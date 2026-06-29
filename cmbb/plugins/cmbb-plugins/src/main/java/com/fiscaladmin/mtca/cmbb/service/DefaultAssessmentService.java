package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * DefaultAssessmentService — DMBB-F10 default-assessment tracking (DM-FR-054..056, BR-DM-051..053).
 *
 * assess  — estimate by priority (prior-year ?? industry avg ?? fixed, BR-DM-051) when auto, apply the
 *           reasonableness check (BR-DM-052), record the informational debit; ASSESSED or NEEDS_JUSTIFICATION.
 * replace — a filed return reverses the default, records the variance, signals the risk update (DM-FR-055/BR-DM-053).
 * escalate— no filing past the grace period → create a DM debt case from the amount (DM-FR-056) via ProcessStarter.
 */
public class DefaultAssessmentService {

    public static final String F_ASSESS = "dmDefAssess";
    public static final String F_RETURN = "cmReturnFiled";
    public static final String F_METHOD = "mdEstMethod";
    public static final String F_POLICY = "mdDefAssessPolicy";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_CHARGE = "dmCharge";
    public static final String F_DEBTCAT = "mdDebtCat";
    private static final String CLASS_NAME = DefaultAssessmentService.class.getName();
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int processed = 0;
        public int escalated = 0;

        @Override
        public String toString() {
            return "processed=" + processed + " escalated=" + escalated;
        }
    }

    /** dmDefAssess has no per-tax override (ADR-003) — its lifecycle is the DM scope. */
    private static final java.util.List<String> SCOPE_DM = java.util.Collections.singletonList("DM");

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final DebtIdentificationService.ProcessStarter starter;
    private final StatusManager status; // ADR-003: guarded + audited dmDefAssess status writes

    public DefaultAssessmentService(FormDataDao dao, CaseEventWriter events,
                                    DebtIdentificationService.ProcessStarter starter) {
        this.dao = dao;
        this.events = events;
        this.starter = starter;
        // share the same CaseEventWriter so STATUS_CHANGED rows join the same chain
        this.status = new StatusManager(dao, new MmConfigService(dao), events);
        for (String f : new String[]{F_ASSESS, F_METHOD, F_POLICY, F_CASE, F_DEBT, F_CHARGE, F_DEBTCAT}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    // ---------------- ASSESS (DM-FR-054, BR-DM-051/052) ----------------

    public String assess(String assessId, String actor, LocalDateTime now) {
        FormRow a = dao.load(F_ASSESS, F_ASSESS, assessId);
        if (a == null) {
            return "no assessment " + assessId;
        }
        String curStatus = p(a, "status");
        if (!curStatus.isEmpty() && !"DRAFT".equalsIgnoreCase(curStatus)
                && !"NEEDS_JUSTIFICATION".equalsIgnoreCase(curStatus)) {
            return "already processed (" + curStatus + ")";
        }
        // dmDefAssess.status is read-only → the data API drops the creator's "DRAFT" and the row
        // arrives blank; establish the canonical initial so the guarded moves below have a from-state.
        if (curStatus.isEmpty()) {
            a.setProperty("status", "DRAFT");
        }
        double priorYear = num(p(a, "priorYearAmount"));
        double estimated = num(p(a, "estimatedAmount"));
        String method = p(a, "estimationMethod");
        String basis;

        if (estimated <= 0) {
            // BR-DM-051 priority: prior-year ?? industry average ?? fixed formula
            if (priorYear > 0) {
                estimated = priorYear;
                method = "PRIOR_YEAR";
                basis = "prior-year return " + fmt(priorYear);
            } else {
                double industry = methodAmount("INDUSTRY_AVG", "industryAvgAmount");
                if (industry > 0) {
                    estimated = industry;
                    method = "INDUSTRY_AVG";
                    basis = "industry average " + fmt(industry);
                } else {
                    estimated = methodAmount("FIXED", "fixedAmount");
                    method = "FIXED";
                    basis = "fixed formula " + fmt(estimated);
                }
            }
            a.setProperty("estimatedAmount", fmt(estimated));
            a.setProperty("estimationMethod", method);
            a.setProperty("basis", basis);
        } else if (method.isEmpty()) {
            a.setProperty("estimationMethod", "MANUAL");
            a.setProperty("basis", "officer-entered " + fmt(estimated));
        }

        a.setProperty("assessedDate", now.toLocalDate().toString());
        a.setProperty("asOf", now.toString());

        // --- reasonableness check (BR-DM-052) ---
        double mult = reasonablenessMultiplier();
        boolean overThreshold = priorYear > 0 && estimated > priorYear * mult;
        if (overThreshold) {
            a.setProperty("reasonablenessFlag", "REVIEW");
            if (p(a, "justification").isEmpty()) {
                // skip the no-op when re-assessed and still without justification
                if (!"NEEDS_JUSTIFICATION".equals(p(a, "status"))) {
                    status.apply(F_ASSESS, a, "status", anchor(a), "NEEDS_JUSTIFICATION", SCOPE_DM,
                            actor, "estimate over prior-year x " + mult + " (BR-DM-052)");
                }
                save(F_ASSESS, a);
                eventTin(a, "DEFAULT_ASSESS_REVIEW", actor,
                        "estimate " + fmt(estimated) + " > prior-year x " + mult + " (BR-DM-052)");
                return "NEEDS_JUSTIFICATION (over reasonableness threshold)";
            }
        } else {
            a.setProperty("reasonablenessFlag", "OK");
        }

        status.apply(F_ASSESS, a, "status", anchor(a), "ASSESSED", SCOPE_DM, actor,
                "default assessment " + fmt(estimated) + " (" + p(a, "estimationMethod") + ")");
        save(F_ASSESS, a);
        // informational debit posting (external ledger; SAD I-4)
        raiseCharge(a, "DEFAULT_ASSESSMENT", estimated, now);
        eventTin(a, "DEFAULT_ASSESSED", actor,
                "default assessment " + fmt(estimated) + " (" + p(a, "estimationMethod") + ")");
        return "ASSESSED " + fmt(estimated) + " (" + p(a, "estimationMethod") + ")";
    }

    // ---------------- REPLACE (DM-FR-055, BR-DM-053) ----------------

    public String replace(String returnId, String actor, LocalDateTime now) {
        FormRow r = dao.load(F_RETURN, F_RETURN, returnId);
        if (r == null) {
            return "no return " + returnId;
        }
        if (!p(r, "result").isEmpty()) {
            return "already processed";
        }
        FormRow a = dao.load(F_ASSESS, F_ASSESS, p(r, "defAssessId"));
        if (a == null) {
            r.setProperty("result", "no assessment " + p(r, "defAssessId"));
            save(F_RETURN, r);
            return "no assessment";
        }
        double filed = num(p(r, "filedAmount"));
        double estimated = num(p(a, "estimatedAmount"));
        double variance = round2(filed - estimated);
        a.setProperty("filedAmount", fmt(filed));
        a.setProperty("variance", fmt(variance));
        // guarded: replacing an ESCALATED assessment is rejected (a debt case already exists)
        status.apply(F_ASSESS, a, "status", anchor(a), "REPLACED", SCOPE_DM, actor,
                "filed " + fmt(filed) + " replaces default " + fmt(estimated));
        save(F_ASSESS, a);
        // reverse the informational debit + post the filed amount (external)
        raiseCharge(a, "ASSESSMENT_REVERSAL", -estimated, now);
        eventTin(a, "ASSESSMENT_REPLACED", actor,
                "filed " + fmt(filed) + " replaces default " + fmt(estimated) + " (variance " + fmt(variance) + ")");
        eventTin(a, "RISK_PROFILE_UPDATE", actor,
                "variance " + fmt(variance) + " on default-assessment replacement (D-SAD-07)");
        r.setProperty("result", "REPLACED variance=" + fmt(variance));
        save(F_RETURN, r);
        return "REPLACED (variance " + fmt(variance) + ")";
    }

    // ---------------- ESCALATE (DM-FR-056) ----------------

    public Tally escalate(String actor, LocalDateTime asOf) {
        Tally t = new Tally();
        long grace = nonFilingGraceDays();
        FormRowSet rows = dao.find(F_ASSESS, F_ASSESS, "WHERE e.customProperties.status = ?1",
                new Object[]{"ASSESSED"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null) {
            return t;
        }
        for (FormRow a : rows) {
            t.processed++;
            LocalDate assessed = parseDate(p(a, "assessedDate"));
            if (assessed == null
                    || ChronoUnit.DAYS.between(assessed, asOf.toLocalDate()) < grace) {
                continue; // still within the filing grace period
            }
            String caseId = createDebtCase(a, actor, asOf);
            // set the ref first so the audit (and anchor) records the escalation on the NEW case chain
            a.setProperty("debtCaseRef", caseId);
            status.apply(F_ASSESS, a, "status", caseId, "ESCALATED", SCOPE_DM, actor,
                    "no filing in " + grace + "d — debt case created");
            save(F_ASSESS, a);
            eventTin(a, "DEFAULT_ASSESS_ESCALATED", actor,
                    "no filing in " + grace + "d — debt case created", "");
            t.escalated++;
        }
        return t;
    }

    private String createDebtCase(FormRow a, String actor, LocalDateTime asOf) {
        String caseId = UUID.randomUUID().toString();
        String tin = p(a, "tin");
        double amount = num(p(a, "estimatedAmount"));
        FormRow c = new FormRow();
        c.setId(caseId);
        String cat = categorize(amount);  // DM-FR-056 "applicable debt category"
        c.setProperty("caseType", "DM");
        c.setProperty("tin", tin);
        c.setProperty("origin", "SYSTEM");
        c.setProperty("taxType", p(a, "taxType"));
        c.setProperty("amountAtStake", fmt(amount));
        c.setProperty("category", cat);
        c.setProperty("subjectKind", "DEBT");
        c.setProperty("subjectId", caseId);
        save(F_CASE, c);
        FormRow dd = new FormRow();
        dd.setId(caseId);
        dd.setProperty("tin", tin);
        dd.setProperty("debtCategory", cat);
        dd.setProperty("stage", "Identified");
        dd.setProperty("triggerOrigin", "DEFAULT_ASSESSMENT");
        dd.setProperty("consolidatedAmount", fmt(amount));
        dd.setProperty("lastStepSeq", "0");
        save(F_DEBT, dd);
        try {
            if (starter != null) {
                starter.start(caseId, "admin");
            }
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "escalation start failed for " + caseId + ": " + e.getMessage());
        }
        return caseId;
    }

    // ---------------- helpers ----------------

    /** Band the amount into a debt category from mdDebtCat (DM-FR-056 applicable category). */
    private String categorize(double amount) {
        FormRowSet bands = dao.find(F_DEBTCAT, F_DEBTCAT, null, null, "bandOrder", Boolean.TRUE, 0, FETCH_ALL);
        if (bands == null) {
            return "";
        }
        String best = "";
        for (FormRow b : bands) {
            double min = num(p(b, "minAmount"));
            String maxStr = p(b, "maxAmount");
            double max = maxStr.isEmpty() ? Double.MAX_VALUE : num(maxStr);
            if (amount >= min && amount < max) {
                return p(b, "code");
            }
            if (amount >= min) {
                best = p(b, "code"); // fall back to the highest band whose floor we cleared
            }
        }
        return best;
    }

    private double methodAmount(String code, String field) {
        FormRowSet rows = dao.find(F_METHOD, F_METHOD, "WHERE e.customProperties.code = ?1",
                new Object[]{code}, "priority", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? 0 : num(p(rows.get(0), field));
    }

    private double reasonablenessMultiplier() {
        FormRow policy = firstRow(F_POLICY);
        double m = policy == null ? 1.5 : num(p(policy, "reasonablenessMultiplier"));
        return m <= 0 ? 1.5 : m;
    }

    private long nonFilingGraceDays() {
        FormRow policy = firstRow(F_POLICY);
        return policy == null ? 30 : DeadlineService.parseLong(p(policy, "nonFilingGraceDays"), 30);
    }

    private void raiseCharge(FormRow a, String type, double amount, LocalDateTime now) {
        FormRow ch = new FormRow();
        ch.setId(UUID.randomUUID().toString());
        ch.setProperty("debtCaseId", "");
        ch.setProperty("tin", p(a, "tin"));
        ch.setProperty("chargeType", type);
        ch.setProperty("instrument", "DEFAULT_ASSESSMENT");
        ch.setProperty("actionId", a.getId());
        ch.setProperty("amount", fmt(amount));
        ch.setProperty("postedDate", now.toLocalDate().toString());
        ch.setProperty("status", "RECORDED");
        save(F_CHARGE, ch);
    }

    private FormRow firstRow(String form) {
        FormRowSet rows = dao.find(form, form, null, null, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** Default assessments are not (yet) on a case; chain the event under a synthetic TIN-scoped id. */
    private void eventTin(FormRow a, String type, String actor, String reason) {
        eventTin(a, type, actor, reason, "");
    }

    /** The cmEvent chain key: the debt case once escalated, else a synthetic TIN-scoped DA id. */
    private String anchor(FormRow a) {
        String r = p(a, "debtCaseRef");
        return r.isEmpty() ? "DA-" + a.getId() : r;
    }

    private void eventTin(FormRow a, String type, String actor, String reason, String extra) {
        String anchor = anchor(a);
        String ex = extra.isEmpty()
                ? "\"tin\":\"" + CaseEventWriter.esc(p(a, "tin")) + "\",\"assessment\":\""
                        + CaseEventWriter.esc(a.getId()) + "\""
                : extra;
        events.append(anchor, type, actor, "", "", reason, ex);
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

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(round2(d));
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
