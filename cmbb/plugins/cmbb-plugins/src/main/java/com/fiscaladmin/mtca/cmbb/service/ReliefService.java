package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * ReliefService — DMBB-F06 instalment agreements (DM-FR-021..028, BR-DM-017..029).
 *
 * APPLY (dmInstAgr create): eligibility (category in product set, single-active
 * BR-DM-018, min instalment BR-DM-020) → schedule + projected reduced-rate
 * interest → auto-approve within params (BR-DM-021) else UNDER_REVIEW. On ACTIVE
 * it asserts an ENFORCEMENT_SUPPRESS hold on the linked debt case — exactly what
 * the F05 enforcement gate honours, so enforcement pauses while the plan is live (DM-FR-028).
 *
 * COMPLIANCE (cmInstComplianceRun): evaluate each schedule line vs paid (within
 * grace BR-DM-025); on >= threshold consecutive misses auto-cancel (BR-DM-027) —
 * release the hold and create a recovery debt case for the remaining balance (BR-DM-029).
 */
public class ReliefService {

    public static final String F_AGR = "dmInstAgr";
    public static final String F_LINE = "dmInstLine";
    public static final String F_RELIEF = "mdRelief";
    public static final String F_VIOLATION = "mdViolation";
    public static final String F_PROJRATE = "mdProjRate";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_HOLD = "cmHold";
    private static final String CLASS_NAME = ReliefService.class.getName();
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int evaluated = 0;
        public int cancelled = 0;
        public int atRisk = 0;

        @Override
        public String toString() {
            return "evaluated=" + evaluated + " cancelled=" + cancelled + " atRisk=" + atRisk;
        }
    }

    /** dmInstAgr / dmInstLine have no per-tax override (ADR-003) — DM scope. */
    private static final java.util.List<String> SCOPE_DM = java.util.Collections.singletonList("DM");

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final DebtIdentificationService.ProcessStarter starter; // recovery-case start (BR-DM-029)
    private final StatusManager status; // ADR-003: guarded + audited dmInstAgr/dmInstLine status writes

    public ReliefService(FormDataDao dao, CaseEventWriter events,
                         DebtIdentificationService.ProcessStarter starter) {
        this.dao = dao;
        this.events = events;
        this.starter = starter;
        // share the same CaseEventWriter so STATUS_CHANGED rows join the case hash-chain
        this.status = new StatusManager(dao, new MmConfigService(dao), events);
    }

    // ---------------- APPLY ----------------

    public String apply(String agrId, String actor, LocalDateTime now) {
        updateSchemas();
        FormRow agr = dao.load(F_AGR, F_AGR, agrId);
        if (agr == null) {
            return "no agreement " + agrId;
        }
        if (!"".equals(p(agr, "status")) && !"APPLIED".equalsIgnoreCase(p(agr, "status"))) {
            return "already processed (" + p(agr, "status") + ")"; // idempotent
        }
        // dmInstAgr.status is read-only → the data API drops the creator's "APPLIED"; establish the
        // canonical initial so the guarded moves below (approve/reject) have a valid from-state.
        if (p(agr, "status").isEmpty()) {
            agr.setProperty("status", "APPLIED");
        }
        String tin = p(agr, "tin");
        String debtCaseId = p(agr, "debtCaseId");
        double totalDebt = num(p(agr, "totalDebt"));
        int duration = (int) DeadlineService.parseLong(p(agr, "durationMonths"), 12);
        FormRow relief = firstRow(F_RELIEF);
        FormRow projrate = firstRow(F_PROJRATE);

        // --- resolve case + outstanding from the TIN (the worklist "Set up instalment
        //     plan" action passes only the TIN; the officer never types the case id or the
        //     amount — they are read from the debtor's primary open DM debt case) ---
        if (debtCaseId.isEmpty() && !tin.isEmpty()) {
            FormRow primary = resolvePrimaryCase(tin);
            if (primary != null) {
                debtCaseId = primary.getId();
                agr.setProperty("debtCaseId", debtCaseId);
            }
        }
        if (totalDebt == 0 && !debtCaseId.isEmpty()) {
            FormRow dd0 = dao.load(F_DEBT, F_DEBT, debtCaseId);
            if (dd0 != null) {
                totalDebt = num(p(dd0, "consolidatedAmount"));
                agr.setProperty("totalDebt", String.valueOf(totalDebt));
            }
        }

        // --- eligibility ---
        String cat = debtCategory(debtCaseId);
        String allowed = relief == null ? "C3,C4,C5" : p(relief, "categories");
        if (!allowed.toUpperCase().contains(cat.toUpperCase()) || cat.isEmpty()) {
            return reject(agr, actor, debtCaseId, "category " + cat + " not eligible (need " + allowed + ")");
        }
        if (hasActivePlan(tin, agrId)) {
            return reject(agr, actor, debtCaseId, "taxpayer already has an active instalment (BR-DM-018)");
        }
        double minInst = relief == null ? 0 : num(p(relief, "minInstalment"));
        double monthly = duration > 0 ? totalDebt / duration : totalDebt;
        // projected reduced-rate interest (informational, D-SAD-04)
        double reducedRate = projrate == null ? 0 : num(p(projrate, "reducedRate")); // % per month
        double totalInterest = round2(totalDebt * (reducedRate / 100.0) * (duration / 2.0));
        double perLine = round2((totalDebt + totalInterest) / Math.max(1, duration));
        if (perLine < minInst) {
            return reject(agr, actor, debtCaseId,
                    "instalment " + perLine + " below minimum " + minInst + " (BR-DM-020)");
        }

        // --- schedule ---
        buildSchedule(agrId, duration, perLine, now);
        agr.setProperty("monthlyAmount", String.valueOf(perLine));
        agr.setProperty("totalInterest", String.valueOf(totalInterest));
        agr.setProperty("consecutiveMissed", "0");

        // --- approval (BR-DM-021 auto-approve within params, else route) ---
        double autoThreshold = relief == null ? 5000 : num(p(relief, "autoThreshold"));
        int autoMax = relief == null ? 12 : (int) DeadlineService.parseLong(p(relief, "autoMaxMonths"), 12);
        boolean firstApp = countAgrForTin(tin, agrId) == 0;
        if (totalDebt < autoThreshold && duration <= autoMax && firstApp) {
            agr.setProperty("approvalRef", "AUTO");
            agr.setProperty("complianceStatus", "COMPLIANT");
            status.apply(F_AGR, agr, "status", debtCaseId, "ACTIVE", SCOPE_DM, actor,
                    "auto-approved (BR-DM-021)");
            save(F_AGR, agr);
            assertHold(debtCaseId, agrId, actor);
            event(debtCaseId, "INSTALMENT_APPROVED", actor,
                    "auto-approved (BR-DM-021)", "\"agr\":\"" + CaseEventWriter.esc(agrId)
                            + "\",\"monthly\":\"" + perLine + "\"");
            return "ACTIVE (auto-approved, hold asserted)";
        }
        agr.setProperty("complianceStatus", "");
        status.apply(F_AGR, agr, "status", debtCaseId, "UNDER_REVIEW", SCOPE_DM, actor,
                "routed to authority (BR-DM-022)");
        save(F_AGR, agr);
        event(debtCaseId, "INSTALMENT_UNDER_REVIEW", actor,
                "routed to authority (BR-DM-022)", "\"agr\":\"" + CaseEventWriter.esc(agrId) + "\"");
        return "UNDER_REVIEW (outside auto-approval params)";
    }

    private void buildSchedule(String agrId, int duration, double perLine, LocalDateTime now) {
        for (int i = 1; i <= duration; i++) {
            FormRow l = new FormRow();
            l.setId(agrId + "-L" + i);
            l.setProperty("instAgrId", agrId);
            l.setProperty("seq", String.valueOf(i));
            l.setProperty("dueDate", now.toLocalDate().plusMonths(i).toString());
            l.setProperty("expectedAmount", String.valueOf(perLine));
            l.setProperty("paidAmount", "0");
            l.setProperty("status", "pending");
            save(F_LINE, l);
        }
    }

    private String reject(FormRow agr, String actor, String debtCaseId, String reason) {
        agr.setProperty("complianceStatus", reason);
        status.apply(F_AGR, agr, "status", debtCaseId, "REJECTED", SCOPE_DM, actor, reason);
        save(F_AGR, agr);
        event(debtCaseId, "INSTALMENT_REJECTED", actor, reason,
                "\"agr\":\"" + CaseEventWriter.esc(agr.getId()) + "\"");
        return "REJECTED: " + reason;
    }

    // ---------------- COMPLIANCE ----------------

    public Tally compliance(String actor, LocalDateTime asOf) {
        updateSchemas();
        Tally t = new Tally();
        FormRow violation = firstRow(F_VIOLATION);
        long grace = violation == null ? 3 : DeadlineService.parseLong(p(violation, "graceDays"), 3);
        long cancelAt = violation == null ? 2
                : DeadlineService.parseLong(p(violation, "consecutiveMissThreshold"), 2);
        FormRowSet agrs = dao.find(F_AGR, F_AGR, "WHERE e.customProperties.status = ?1",
                new Object[]{"ACTIVE"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (agrs == null) {
            return t;
        }
        for (FormRow agr : agrs) {
            t.evaluated++;
            int consecutive = evaluateLines(agr.getId(), p(agr, "debtCaseId"), actor, grace, asOf);
            agr.setProperty("consecutiveMissed", String.valueOf(consecutive));
            if (consecutive >= cancelAt) {
                cancel(agr, actor, asOf);
                t.cancelled++;
            } else {
                agr.setProperty("complianceStatus", consecutive > 0 ? "AT_RISK" : "COMPLIANT");
                save(F_AGR, agr);
                if (consecutive > 0) {
                    t.atRisk++;
                }
            }
        }
        return t;
    }

    /** Mark due lines paid/missed; return the trailing run of consecutive misses. */
    private int evaluateLines(String agrId, String debtCaseId, String actor,
                              long grace, LocalDateTime asOf) {
        FormRowSet lines = dao.find(F_LINE, F_LINE,
                "WHERE e.customProperties.instAgrId = ?1", new Object[]{agrId},
                "seq", Boolean.FALSE, 0, FETCH_ALL);
        int consecutive = 0;
        if (lines == null) {
            return 0;
        }
        for (FormRow l : lines) {
            LocalDate due = parseDate(p(l, "dueDate"));
            if (due == null || due.plusDays(grace).isAfter(asOf.toLocalDate())) {
                continue; // not yet due (within grace)
            }
            boolean paid = num(p(l, "paidAmount")) >= num(p(l, "expectedAmount"));
            String target = paid ? "paid" : "missed";
            // ADR-003: route through the config-driven guard — an illegal move throws
            // InvalidTransitionException (nothing written); each accepted move is audited
            // as a STATUS_CHANGED row on the case hash-chain. No-op (same status) is skipped.
            if (!target.equals(p(l, "status"))) {
                status.transition(F_LINE, F_LINE, "status", l.getId(), debtCaseId, target,
                        java.util.Collections.singletonList("DM"), actor,
                        "instalment line " + (paid ? "settled" : "missed within grace") + " (BR-DM-025)");
            }
            consecutive = paid ? 0 : consecutive + 1;
        }
        return consecutive;
    }

    private void cancel(FormRow agr, String actor, LocalDateTime asOf) {
        String debtCaseId = p(agr, "debtCaseId");
        agr.setProperty("complianceStatus", "DEFAULTED");
        status.apply(F_AGR, agr, "status", debtCaseId, "CANCELLED", SCOPE_DM, actor,
                "auto-cancelled on default (BR-DM-027)");
        releaseHold(debtCaseId, actor);
        double paid = sumPaid(agr.getId());
        double remaining = Math.max(0, num(p(agr, "totalDebt")) - paid);
        String recoveryId = createRecoveryCase(agr, remaining, actor);
        agr.setProperty("recoveryCaseRef", recoveryId);
        save(F_AGR, agr);
        event(debtCaseId, "INSTALMENT_CANCELLED", actor,
                "auto-cancelled on default (BR-DM-027)", "\"agr\":\"" + CaseEventWriter.esc(agr.getId())
                        + "\",\"remaining\":\"" + remaining + "\"");
        event(debtCaseId, "RECOVERY_CASE_CREATED", actor, "recovery case from cancelled instalment",
                "\"recoveryCase\":\"" + CaseEventWriter.esc(recoveryId)
                        + "\",\"remaining\":\"" + remaining + "\"");
    }

    private String createRecoveryCase(FormRow agr, double remaining, String actor) {
        String caseId = UUID.randomUUID().toString();
        String tin = p(agr, "tin");
        String cat = debtCategory(p(agr, "debtCaseId"));
        FormRow c = new FormRow();
        c.setId(caseId);
        c.setProperty("caseType", "DM");
        c.setProperty("tin", tin);
        c.setProperty("origin", "ESCALATION");
        c.setProperty("category", cat);
        c.setProperty("amountAtStake", String.valueOf(remaining));
        c.setProperty("subjectKind", "DEBT");
        c.setProperty("subjectId", caseId);
        save(F_CASE, c);
        FormRow dd = new FormRow();
        dd.setId(caseId);
        dd.setProperty("tin", tin);
        dd.setProperty("debtCategory", cat);
        dd.setProperty("stage", "Recovery");
        dd.setProperty("triggerOrigin", "INSTALMENT_DEFAULT");
        dd.setProperty("consolidatedAmount", String.valueOf(remaining));
        dd.setProperty("lastStepSeq", "0");
        save(F_DEBT, dd);
        try {
            if (starter != null) {
                starter.start(caseId, "admin");
            }
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "recovery-case start failed for " + caseId + ": " + e.getMessage());
        }
        return caseId;
    }

    // ---------------- holds ----------------

    private void assertHold(String debtCaseId, String agrId, String actor) {
        if (debtCaseId == null || debtCaseId.isEmpty()) {
            return;
        }
        FormRow h = new FormRow();
        h.setId(UUID.randomUUID().toString());
        h.setProperty("caseId", debtCaseId);
        h.setProperty("scope", "ENFORCEMENT_SUPPRESS");
        h.setProperty("holdType", "INSTALMENT");
        h.setProperty("basis", "active instalment " + agrId);
        h.setProperty("targetBB", "DMBB");
        h.setProperty("status", "ACTIVE");
        h.setProperty("assertedBy", actor);
        save(F_HOLD, h);
        event(debtCaseId, "HOLD_ASSERTED", actor, "enforcement suppressed by instalment",
                "\"scope\":\"ENFORCEMENT_SUPPRESS\",\"agr\":\"" + CaseEventWriter.esc(agrId) + "\"");
    }

    private void releaseHold(String debtCaseId, String actor) {
        FormRowSet holds = dao.find(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{debtCaseId, "ENFORCEMENT_SUPPRESS", "ACTIVE"},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (holds == null) {
            return;
        }
        for (FormRow h : holds) {
            h.setProperty("status", "RELEASED");
            save(F_HOLD, h);
        }
        event(debtCaseId, "HOLD_RELEASED", actor, "instalment cancelled — enforcement resumes",
                "\"scope\":\"ENFORCEMENT_SUPPRESS\"");
    }

    // ---------------- helpers ----------------

    private boolean hasActivePlan(String tin, String exceptId) {
        FormRowSet rows = dao.find(F_AGR, F_AGR, "WHERE e.customProperties.tin = ?1",
                new Object[]{tin}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null) {
            return false;
        }
        for (FormRow a : rows) {
            if (a.getId().equals(exceptId)) {
                continue;
            }
            String s = p(a, "status");
            if ("ACTIVE".equals(s) || "APPROVED".equals(s) || "UNDER_REVIEW".equals(s)) {
                return true;
            }
        }
        return false;
    }

    private long countAgrForTin(String tin, String exceptId) {
        FormRowSet rows = dao.find(F_AGR, F_AGR, "WHERE e.customProperties.tin = ?1",
                new Object[]{tin}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null) {
            return 0;
        }
        long n = 0;
        for (FormRow a : rows) {
            if (!a.getId().equals(exceptId)) {
                n++;
            }
        }
        return n;
    }

    private double sumPaid(String agrId) {
        FormRowSet lines = dao.find(F_LINE, F_LINE, "WHERE e.customProperties.instAgrId = ?1",
                new Object[]{agrId}, "seq", Boolean.FALSE, 0, FETCH_ALL);
        double s = 0;
        if (lines != null) {
            for (FormRow l : lines) {
                s += num(p(l, "paidAmount"));
            }
        }
        return s;
    }

    /** The debtor's primary open DM debt case (the one with the largest outstanding) —
     *  used when an instalment is launched from the worklist with only the TIN. */
    private FormRow resolvePrimaryCase(String tin) {
        FormRowSet cases = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.tin = ?1 AND e.customProperties.caseType = ?2",
                new Object[]{tin, "DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return null;
        }
        FormRow best = null;
        double bestAmt = -1;
        for (FormRow c : cases) {
            FormRow dd = dao.load(F_DEBT, F_DEBT, c.getId());
            double amt = (dd == null) ? 0 : num(p(dd, "consolidatedAmount"));
            if (amt > bestAmt) {
                bestAmt = amt;
                best = c;
            }
        }
        return best;
    }

    private String debtCategory(String debtCaseId) {
        if (debtCaseId == null || debtCaseId.isEmpty()) {
            return "";
        }
        FormRow dd = dao.load(F_DEBT, F_DEBT, debtCaseId);
        if (dd != null && !p(dd, "debtCategory").isEmpty()) {
            return p(dd, "debtCategory");
        }
        FormRow c = dao.load(F_CASE, F_CASE, debtCaseId);
        return c == null ? "" : p(c, "category");
    }

    private FormRow firstRow(String form) {
        FormRowSet rows = dao.find(form, form, null, null, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private void event(String caseId, String type, String actor, String reason, String extra) {
        if (caseId != null && !caseId.isEmpty()) {
            events.append(caseId, type, actor, "", "", reason, extra);
        }
    }

    private void updateSchemas() {
        for (String f : new String[]{F_AGR, F_LINE, F_CASE, F_DEBT, F_HOLD}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    private static String p(FormRow r, String id) {
        return DeadlineService.prop(r, id);
    }

    private static double num(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
