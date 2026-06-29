package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * WriteOffService — DMBB-F09 write-off (DM-FR-042..046, BR-DM-036..039).
 *
 * submit  — manual/approved path: evidence guard (BR-DM-039) + delegation routing (BR-DM-037);
 *           a C1_AUTO request posts immediately, an approved one waits at UNDER_REVIEW.
 * approve — an authorised officer's decision posts or rejects (DM-FR-043).
 * sweep   — AUTO_C1 (DM-FR-042/BR-DM-036), STATUTORY_BULK (DM-FR-044/BR-DM-038), C2_PASSIVE (DM-FR-045).
 * Posting closes the case and stamps dmDebt.writeOffStatus while preserving all history (DM-FR-046);
 * the account posting is external (SAD I-4) — DMBB records a dmCharge WRITE_OFF + WRITEOFF_POSTED event.
 */
public class WriteOffService {

    public static final String F_WO = "dmWriteOff";
    public static final String F_APPROVE = "cmWriteOffApprove";
    public static final String F_GROUND = "mdWoGround";
    public static final String F_DELEGATION = "mdWoDelegation";
    public static final String F_POLICY = "mdWoPolicy";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_CHARGE = "dmCharge";
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int processed = 0;
        public int writtenOff = 0;

        @Override
        public String toString() {
            return "processed=" + processed + " writtenOff=" + writtenOff;
        }
    }

    /** dmWriteOff has no per-tax override (ADR-003) — its lifecycle is the DM scope. */
    private static final java.util.List<String> SCOPE_DM = java.util.Collections.singletonList("DM");

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final StatusManager status; // ADR-003: guarded + audited dmWriteOff status writes

    public WriteOffService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
        // share the same CaseEventWriter so STATUS_CHANGED rows join the case hash-chain
        this.status = new StatusManager(dao, new MmConfigService(dao), events);
        for (String f : new String[]{F_WO, F_GROUND, F_DELEGATION, F_POLICY, F_CASE, F_DEBT, F_CHARGE}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    // ---------------- SUBMIT ----------------

    public String submit(String woId, String actor, LocalDateTime now) {
        FormRow wo = dao.load(F_WO, F_WO, woId);
        if (wo == null) {
            return "no write-off " + woId;
        }
        String curStatus = p(wo, "status");
        if (!curStatus.isEmpty() && !"SUBMITTED".equalsIgnoreCase(curStatus)) {
            return "already processed (" + curStatus + ")";
        }
        // dmWriteOff.status is read-only → the data API drops the creator's "SUBMITTED" and the
        // row arrives blank; establish the canonical initial so the guarded moves below have a
        // valid from-state (FormDataDao write path bypasses read-only).
        if (curStatus.isEmpty()) {
            wo.setProperty("status", "SUBMITTED");
        }
        String caseId = p(wo, "debtCaseId");
        String type = p(wo, "woType");
        double amount = num(p(wo, "amount"));
        wo.setProperty("asOf", now.toString());

        if ("C1_AUTO".equalsIgnoreCase(type)) {
            return post(wo, caseId, actor, now, "auto C1 (uneconomic to collect)");
        }

        // --- evidence guard (BR-DM-039) ---
        FormRow ground = groundByCode(p(wo, "ground"));
        boolean needsEvidence = "APPROVED".equalsIgnoreCase(type)
                || (ground != null && "true".equalsIgnoreCase(p(ground, "requiresEvidence")));
        if (needsEvidence && (p(wo, "enforcementHistorySummary").isEmpty()
                || p(wo, "evidenceRef").isEmpty() || p(wo, "rationale").isEmpty())) {
            status.apply(F_WO, wo, "status", caseId, "REJECTED", SCOPE_DM, actor,
                    "missing evidence (BR-DM-039)");
            save(F_WO, wo);
            event(caseId, "WRITEOFF_REJECTED", actor,
                    "missing evidence (history/evidence/rationale) (BR-DM-039)",
                    "\"wo\":\"" + CaseEventWriter.esc(woId) + "\"");
            return "REJECTED: insufficient evidence (BR-DM-039)";
        }

        // --- delegation routing (BR-DM-037) ---
        String level = delegationLevel(amount);
        wo.setProperty("approvalLevel", level);
        status.apply(F_WO, wo, "status", caseId, "UNDER_REVIEW", SCOPE_DM, actor,
                "routed to " + level + " (BR-DM-037)");
        save(F_WO, wo);
        event(caseId, "WRITEOFF_SUBMITTED", actor, "routed to " + level + " (BR-DM-037)",
                "\"wo\":\"" + CaseEventWriter.esc(woId) + "\",\"amount\":\"" + amount + "\"");
        return "UNDER_REVIEW (" + level + ")";
    }

    // ---------------- APPROVE ----------------

    public String approve(String approveId, String actor, LocalDateTime now) {
        FormRow a = dao.load(F_APPROVE, F_APPROVE, approveId);
        if (a == null) {
            return "no approval " + approveId;
        }
        if (!p(a, "result").isEmpty()) {
            return "already processed";
        }
        FormRow wo = dao.load(F_WO, F_WO, p(a, "writeOffId"));
        if (wo == null) {
            a.setProperty("result", "no write-off " + p(a, "writeOffId"));
            save(F_APPROVE, a);
            return "no write-off";
        }
        if (!"UNDER_REVIEW".equalsIgnoreCase(p(wo, "status"))) {
            a.setProperty("result", "write-off not under review (" + p(wo, "status") + ")");
            save(F_APPROVE, a);
            return "not under review";
        }
        String caseId = p(wo, "debtCaseId");
        String approver = p(a, "approver");
        if ("APPROVE".equalsIgnoreCase(p(a, "decision"))) {
            wo.setProperty("approver", approver);
            String r = post(wo, caseId, actor, now, "approved by " + approver);
            a.setProperty("result", r);
            save(F_APPROVE, a);
            return r;
        }
        wo.setProperty("approver", approver);
        status.apply(F_WO, wo, "status", caseId, "REJECTED", SCOPE_DM, actor,
                "rejected by " + approver);
        save(F_WO, wo);
        event(caseId, "WRITEOFF_REJECTED", actor, "rejected by " + approver,
                "\"wo\":\"" + CaseEventWriter.esc(wo.getId()) + "\"");
        a.setProperty("result", "REJECTED");
        save(F_APPROVE, a);
        return "REJECTED by " + approver;
    }

    // ---------------- SWEEP ----------------

    public Tally sweep(String mode, String actor, LocalDateTime asOf) {
        Tally t = new Tally();
        FormRow policy = firstRow(F_POLICY);
        FormRowSet cases = dao.find(F_CASE, F_CASE, "WHERE e.customProperties.caseType = ?1",
                new Object[]{"DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return t;
        }
        long statYears = policy == null ? 5 : DeadlineService.parseLong(p(policy, "statutoryYears"), 5);
        for (FormRow c : cases) {
            String caseId = c.getId();
            FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
            if (dd == null) {
                continue;
            }
            String woStatus = p(dd, "writeOffStatus");
            if ("written-off".equalsIgnoreCase(woStatus)) {
                continue; // already written off (preserved, DM-FR-046)
            }
            String cat = p(dd, "debtCategory");
            t.processed++;
            if ("AUTO_C1".equalsIgnoreCase(mode) && "C1".equalsIgnoreCase(cat)) {
                autoWriteOff(c, dd, "C1_AUTO", "WO-UNECONOMIC",
                        "auto C1 (uneconomic to collect)", actor, asOf);
                t.writtenOff++;
            } else if ("STATUTORY_BULK".equalsIgnoreCase(mode)
                    && agedOut(dd, statYears, asOf)) {
                autoWriteOff(c, dd, "STATUTORY", "WO-STATUTORY",
                        "statutory period expired (" + statYears + "y)", actor, asOf);
                t.writtenOff++;
            } else if ("C2_PASSIVE".equalsIgnoreCase(mode) && "C2".equalsIgnoreCase(cat)
                    && !"passive-collection".equalsIgnoreCase(woStatus)) {
                dd.setProperty("writeOffStatus", "passive-collection");
                save(F_DEBT, dd);
                event(caseId, "WRITEOFF_PASSIVE", actor,
                        "C2 set to passive collection (DM-FR-045)", "");
                t.writtenOff++;
            }
        }
        return t;
    }

    private boolean agedOut(FormRow dd, long statYears, LocalDateTime asOf) {
        LocalDate fad = parseDate(p(dd, "firstAssessedDate"));
        return fad != null && ChronoUnit.YEARS.between(fad, asOf.toLocalDate()) >= statYears;
    }

    private void autoWriteOff(FormRow c, FormRow dd, String type, String ground,
                              String reason, String actor, LocalDateTime asOf) {
        String caseId = c.getId();
        FormRow wo = new FormRow();
        String woId = UUID.randomUUID().toString();
        wo.setId(woId);
        wo.setProperty("debtCaseId", caseId);
        wo.setProperty("tin", p(c, "tin"));
        wo.setProperty("amount", p(dd, "consolidatedAmount"));
        wo.setProperty("woType", type);
        wo.setProperty("ground", ground);
        wo.setProperty("rationale", reason);
        wo.setProperty("asOf", asOf.toString());
        wo.setProperty("status", "SUBMITTED"); // canonical initial so post()'s guard has a from-state
        save(F_WO, wo);
        post(wo, caseId, actor, asOf, reason);
    }

    // ---------------- post ----------------

    private String post(FormRow wo, String caseId, String actor, LocalDateTime now, String reason) {
        double amount = num(p(wo, "amount"));
        status.apply(F_WO, wo, "status", caseId, "POSTED", SCOPE_DM, actor, reason);
        wo.setProperty("postedDate", now.toLocalDate().toString());
        save(F_WO, wo);

        FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
        if (dd != null) {
            dd.setProperty("writeOffStatus", "written-off");
            dd.setProperty("writeOffRef", wo.getId());
            save(F_DEBT, dd);
        }
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c != null && !"CLOSED".equalsIgnoreCase(p(c, "currentState"))) {
            c.setProperty("currentState", "CLOSED");
            save(F_CASE, c);
        }
        // informational hand-off record for the external ledger posting (SAD I-4)
        FormRow ch = new FormRow();
        ch.setId(UUID.randomUUID().toString());
        ch.setProperty("debtCaseId", caseId);
        ch.setProperty("tin", p(wo, "tin"));
        ch.setProperty("chargeType", "WRITE_OFF");
        ch.setProperty("instrument", p(wo, "woType"));
        ch.setProperty("actionId", wo.getId());
        ch.setProperty("amount", String.valueOf(amount));
        ch.setProperty("postedDate", now.toLocalDate().toString());
        ch.setProperty("status", "RECORDED");
        save(F_CHARGE, ch);

        event(caseId, "WRITEOFF_POSTED", actor, reason,
                "\"wo\":\"" + CaseEventWriter.esc(wo.getId()) + "\",\"amount\":\"" + amount
                        + "\",\"type\":\"" + CaseEventWriter.esc(p(wo, "woType")) + "\"");
        return "POSTED " + p(wo, "woType") + " " + amount;
    }

    // ---------------- helpers ----------------

    private String delegationLevel(double amount) {
        FormRow d = firstRow(F_DELEGATION);
        double dmoMax = d == null ? 1000 : num(p(d, "dmoMax"));
        double sdoMax = d == null ? 20000 : num(p(d, "sdoMax"));
        if (amount < dmoMax) {
            return "DMO";
        }
        if (amount < sdoMax) {
            return "SDO";
        }
        return "DIRECTOR";
    }

    private FormRow groundByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        FormRowSet rows = dao.find(F_GROUND, F_GROUND, "WHERE e.customProperties.code = ?1",
                new Object[]{code}, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
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

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
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

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
