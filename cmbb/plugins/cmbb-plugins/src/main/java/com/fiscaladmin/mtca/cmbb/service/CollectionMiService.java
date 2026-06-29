package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * CollectionMiService — DMBB-F12 collection MI (DM-FR-047/048, BR-DM-042/043, CAD §132 ②/③).
 *
 * rollup    — plan-vs-actual: actual recovered per target category = Σ dmAction.recoveredAmount
 *             + Σ dmAgent.recoveredTotal, with attainment % (DM-FR-047).
 * suggest   — suggested target = current category stock × mdCollectionParam.recoveryRate (BR-DM-043).
 * reconcile — ②/③ writeback completeness: every WRITEOFF_POSTED event (②) has its dmCharge WRITE_OFF (③);
 *             orphans are variances (CAD §132).
 */
public class CollectionMiService {

    public static final String F_PLAN = "dmCollectionPlan";
    public static final String F_TARGET = "dmPlanTarget";
    public static final String F_PARAM = "mdCollectionParam";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_ACTION = "dmAction";
    public static final String F_AGENT = "dmAgent";
    public static final String F_CHARGE = "dmCharge";
    public static final String F_EVENT = "cmEvent";
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int matched = 0;
        public int variance = 0;

        @Override
        public String toString() {
            return "matched=" + matched + " variance=" + variance;
        }
    }

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final Map<String, String> catCache = new HashMap<>();

    public CollectionMiService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
        for (String f : new String[]{F_PLAN, F_TARGET, F_PARAM, F_CASE, F_DEBT, F_ACTION, F_AGENT, F_CHARGE}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    // ---------------- ROLLUP (DM-FR-047) ----------------

    public Tally rollup(String planId, String actor, LocalDateTime now) {
        Tally t = new Tally();
        Map<String, Double> recovered = recoveredByCategory();
        FormRowSet targets = targetsOf(planId);
        if (targets == null) {
            return t;
        }
        double planTarget = 0;
        double planActual = 0;
        for (FormRow tg : targets) {
            String cat = p(tg, "category");
            double target = num(p(tg, "targetAmount"));
            double actual = recovered.getOrDefault(cat, 0.0);
            tg.setProperty("actualAmount", fmt(actual));
            tg.setProperty("attainmentPct", target > 0 ? fmt(round2(100.0 * actual / target)) : "0");
            save(F_TARGET, tg);
            planTarget += target;
            planActual += actual;
            t.matched++;
        }
        event("PLAN-" + planId, "COLLECTION_ROLLUP", actor,
                "plan-vs-actual: target " + fmt(planTarget) + " actual " + fmt(planActual),
                "\"target\":\"" + fmt(planTarget) + "\",\"actual\":\"" + fmt(planActual) + "\"");
        return t;
    }

    // ---------------- SUGGEST (BR-DM-043) ----------------

    public Tally suggest(String planId, String actor, LocalDateTime now) {
        Tally t = new Tally();
        Map<String, Double> stock = stockByCategory();
        FormRowSet targets = targetsOf(planId);
        if (targets == null) {
            return t;
        }
        for (FormRow tg : targets) {
            String cat = p(tg, "category");
            double rate = recoveryRate(cat);
            double suggested = round2(stock.getOrDefault(cat, 0.0) * rate);
            tg.setProperty("suggestedAmount", fmt(suggested));
            if (p(tg, "targetAmount").isEmpty() || num(p(tg, "targetAmount")) == 0) {
                tg.setProperty("targetAmount", fmt(suggested));
            }
            save(F_TARGET, tg);
            t.matched++;
        }
        event("PLAN-" + planId, "COLLECTION_TARGET_SUGGESTED", actor,
                "targets suggested from stock x recovery-rate (BR-DM-043)", "");
        return t;
    }

    // ---------------- RECONCILE (CAD §132 ②/③) ----------------

    public Tally reconcile(String actor, LocalDateTime asOf) {
        Tally t = new Tally();
        // ② financially-terminal events
        FormRowSet woEvents = dao.find(F_EVENT, F_EVENT, "WHERE e.customProperties.eventType = ?1",
                new Object[]{"WRITEOFF_POSTED"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (woEvents == null) {
            return t;
        }
        for (FormRow ev : woEvents) {
            String caseId = p(ev, "caseId");
            // ③ outcome hand-off record
            Long charges = dao.count(F_CHARGE, F_CHARGE,
                    "WHERE e.customProperties.debtCaseId = ?1 AND e.customProperties.chargeType = ?2",
                    new Object[]{caseId, "WRITE_OFF"});
            if (charges != null && charges > 0) {
                t.matched++;
            } else {
                t.variance++;
            }
        }
        return t;
    }

    // ---------------- aggregations ----------------

    private Map<String, Double> recoveredByCategory() {
        Map<String, Double> m = new HashMap<>();
        FormRowSet actions = dao.find(F_ACTION, F_ACTION, null, null, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (actions != null) {
            for (FormRow a : actions) {
                double rec = num(p(a, "recoveredAmount"));
                if (rec != 0) {
                    add(m, categoryOf(p(a, "debtCaseId")), rec);
                }
            }
        }
        FormRowSet agents = dao.find(F_AGENT, F_AGENT, null, null, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (agents != null) {
            for (FormRow ag : agents) {
                double rec = num(p(ag, "recoveredTotal"));
                if (rec != 0) {
                    add(m, categoryOf(p(ag, "debtCaseId")), rec);
                }
            }
        }
        return m;
    }

    private Map<String, Double> stockByCategory() {
        Map<String, Double> m = new HashMap<>();
        FormRowSet cases = dao.find(F_CASE, F_CASE, "WHERE e.customProperties.caseType = ?1",
                new Object[]{"DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return m;
        }
        for (FormRow c : cases) {
            FormRow dd = dao.load(F_DEBT, F_DEBT, c.getId());
            if (dd == null || "written-off".equalsIgnoreCase(p(dd, "writeOffStatus"))) {
                continue;
            }
            String cat = !p(dd, "debtCategory").isEmpty() ? p(dd, "debtCategory") : p(c, "category");
            add(m, cat, num(p(dd, "consolidatedAmount")));
        }
        return m;
    }

    private String categoryOf(String caseId) {
        if (caseId == null || caseId.isEmpty()) {
            return "";
        }
        if (catCache.containsKey(caseId)) {
            return catCache.get(caseId);
        }
        FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
        String cat = "";
        if (dd != null && !p(dd, "debtCategory").isEmpty()) {
            cat = p(dd, "debtCategory");
        } else {
            FormRow c = dao.load(F_CASE, F_CASE, caseId);
            cat = c == null ? "" : p(c, "category");
        }
        catCache.put(caseId, cat);
        return cat;
    }

    private double recoveryRate(String cat) {
        FormRowSet rows = dao.find(F_PARAM, F_PARAM, "WHERE e.customProperties.category = ?1",
                new Object[]{cat}, "dateCreated", Boolean.FALSE, 0, 1);
        if (rows != null && !rows.isEmpty()) {
            return num(p(rows.get(0), "recoveryRate"));
        }
        FormRowSet def = dao.find(F_PARAM, F_PARAM, "WHERE e.customProperties.category = ?1",
                new Object[]{""}, "dateCreated", Boolean.FALSE, 0, 1);
        return (def == null || def.isEmpty()) ? 0.30 : num(p(def.get(0), "recoveryRate"));
    }

    private FormRowSet targetsOf(String planId) {
        return dao.find(F_TARGET, F_TARGET, "WHERE e.customProperties.planId = ?1",
                new Object[]{planId}, "dateCreated", Boolean.TRUE, 0, FETCH_ALL);
    }

    private static void add(Map<String, Double> m, String key, double v) {
        m.merge(key == null ? "" : key, v, Double::sum);
    }

    private void event(String anchor, String type, String actor, String reason, String extra) {
        events.append(anchor, type, actor, "", "", reason, extra);
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
