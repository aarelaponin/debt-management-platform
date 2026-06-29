package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * OutcomeService — idempotent case-outcome writeback to the Gold mart
 * (CMBB-F09, I-2 / Blueprint §14.2.2). SHIP scans CLOSED cases with no SHIPPED
 * cmOutcome and inserts a fact row into ClickHouse via the injected
 * OutcomeGateway; on gateway failure the ledger row is QUEUED (no throw into the
 * case flow, P11) and RETRY re-ships it. Exactly-once at the Joget side via the
 * cmOutcome ledger (id = caseId-outcomeCode); ReplacingMergeTree dedups CH-side.
 */
public class OutcomeService {

    public static final String F_CASE = "cmCase";
    public static final String F_OUTCOME = "cmOutcome";
    public static final String F_DECISION = "cmDecision";
    public static final String F_STATE = "mmState";
    private static final String CLASS_NAME = OutcomeService.class.getName();
    private static final int FETCH_ALL = 100000;

    public interface OutcomeGateway {
        void insert(OutcomeRecord r) throws Exception;
    }

    public static class OutcomeRecord {
        public String caseId = "";
        public String outcomeDate = "";
        public String tin = "";
        public String outcomeType = "RESOLUTION";
        public String outcomeCode = "RESOLVED";
        public String amount = "0";
        public String officer = "";
        public String detail = "";
    }

    /** {shipped, queued, failed}. */
    public static class Tally {
        public int shipped = 0;
        public int queued = 0;
        public int failed = 0;

        @Override
        public String toString() {
            return "shipped=" + shipped + " queued=" + queued + " failed=" + failed;
        }
    }

    private final FormDataDao dao;
    private final OutcomeGateway gateway;
    private final GoldMartClient gold;

    public OutcomeService(FormDataDao dao, OutcomeGateway gateway, GoldMartClient gold) {
        this.dao = dao;
        this.gateway = gateway;
        this.gold = gold;
    }

    public Tally shipClosed(String actor) {
        dao.updateSchema(F_OUTCOME, F_OUTCOME, new FormRowSet());
        dao.updateSchema(F_CASE, F_CASE, new FormRowSet());
        Tally t = new Tally();
        Set<String> terminal = terminalStateCodes();
        FormRowSet cases = dao.find(F_CASE, F_CASE, null, null,
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return t;
        }
        for (FormRow c : cases) {
            String state = nz(c.getProperty("currentState"));
            if (!terminal.contains(state)) {
                continue;
            }
            String caseId = c.getId();
            String code = resolveOutcomeCode(dao, caseId);
            String ledgerId = caseId + "-" + code;
            FormRow existing = dao.load(F_OUTCOME, F_OUTCOME, ledgerId);
            if (existing != null && "SHIPPED".equals(existing.getProperty("shipStatus"))) {
                continue; // exactly-once
            }
            OutcomeRecord rec = build(c, code);
            GoldMartClient.GoldProfile gp = gold.fetchProfile(rec.tin);
            applyGold(rec, gp);
            shipOne(t, ledgerId, rec, gp, actor);
        }
        return t;
    }

    public Tally retryQueued(String actor) {
        dao.updateSchema(F_OUTCOME, F_OUTCOME, new FormRowSet());
        Tally t = new Tally();
        FormRowSet queued = dao.find(F_OUTCOME, F_OUTCOME,
                "WHERE e.customProperties.shipStatus = ?1", new Object[]{"QUEUED"},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (queued == null) {
            return t;
        }
        for (FormRow row : queued) {
            OutcomeRecord rec = new OutcomeRecord();
            rec.caseId = nz(row.getProperty("caseId"));
            rec.outcomeDate = nz(row.getProperty("outcomeDate"));
            rec.tin = nz(row.getProperty("tin"));
            rec.outcomeType = nz(row.getProperty("outcomeType"));
            rec.outcomeCode = nz(row.getProperty("outcomeCode"));
            rec.amount = nz(row.getProperty("amount"));
            rec.officer = nz(row.getProperty("officer"));
            rec.detail = nz(row.getProperty("detail"));
            try {
                gateway.insert(rec);
                row.setProperty("shipStatus", "SHIPPED");
                save(row);
                t.shipped++;
            } catch (Exception e) {
                LogUtil.warn(CLASS_NAME, "retry insert failed for " + rec.caseId + ": " + e.getMessage());
                t.queued++;
            }
        }
        return t;
    }

    private void shipOne(Tally t, String ledgerId, OutcomeRecord rec,
                         GoldMartClient.GoldProfile gp, String actor) {
        String status;
        try {
            gateway.insert(rec);
            status = "SHIPPED";
            t.shipped++;
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "writeback insert failed for " + rec.caseId
                    + ": " + e.getMessage() + " — queued");
            status = "QUEUED";
            t.queued++;
        }
        FormRow row = new FormRow();
        row.setId(ledgerId);
        row.setProperty("caseId", rec.caseId);
        row.setProperty("tin", rec.tin);
        row.setProperty("outcomeType", rec.outcomeType);
        row.setProperty("outcomeCode", rec.outcomeCode);
        row.setProperty("amount", rec.amount);
        row.setProperty("officer", rec.officer);
        row.setProperty("outcomeDate", rec.outcomeDate);
        row.setProperty("shipStatus", status);
        row.setProperty("goldSource", gp.source);
        row.setProperty("asOf", nz(gp.asOf));
        row.setProperty("detail", rec.detail);
        save(row);
    }

    private OutcomeRecord build(FormRow c, String code) {
        OutcomeRecord rec = new OutcomeRecord();
        rec.caseId = c.getId();
        rec.tin = nz(c.getProperty("tin"));
        rec.outcomeType = "RESOLUTION";
        rec.outcomeCode = code;
        rec.amount = orZero(c.getProperty("amountAtStake"));
        rec.officer = nz(c.getProperty("assignee"));
        rec.outcomeDate = LocalDate.now().toString();
        return rec;
    }

    private void applyGold(OutcomeRecord rec, GoldMartClient.GoldProfile gp) {
        if (gp.enforceableBalance != null && !gp.enforceableBalance.isEmpty()) {
            rec.amount = gp.enforceableBalance;
        }
        rec.detail = "cat=" + nz(gp.debtCategory) + ";conf=" + nz(gp.asaConfidence)
                + ";asOf=" + nz(gp.asOf) + ";src=" + gp.source;
    }

    private Set<String> terminalStateCodes() {
        dao.updateSchema(F_STATE, F_STATE, new FormRowSet());
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.isTerminal = ?1", new Object[]{"true"},
                "code", Boolean.FALSE, 0, FETCH_ALL);
        Set<String> codes = new HashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                String code = r.getProperty("code");
                if (code != null && !code.isEmpty()) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    /** Outcome code from the latest APPROVED cmDecision actionType, else RESOLVED. */
    public static String resolveOutcomeCode(FormDataDao dao, String caseId) {
        dao.updateSchema(F_DECISION, F_DECISION, new FormRowSet());
        FormRowSet rows = dao.find(F_DECISION, F_DECISION,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.decisionStatus = ?2",
                new Object[]{caseId, "APPROVED"}, "dateCreated", Boolean.TRUE, 0, 1);
        if (rows == null || rows.isEmpty()) {
            return "RESOLVED";
        }
        String action = rows.get(0).getProperty("actionType");
        if (action == null || action.isEmpty() || "CLOSE_CASE".equals(action)) {
            return "RESOLVED";
        }
        return action.toUpperCase();
    }

    private void save(FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(F_OUTCOME, F_OUTCOME, set);
    }

    private static String orZero(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
