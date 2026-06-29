package com.fiscaladmin.mtca.cmbb;

import java.util.List;
import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.ChainVerifyService;
import com.fiscaladmin.mtca.cmbb.service.GoldMartClient;
import com.fiscaladmin.mtca.cmbb.service.JdbcGoldGateway;
import com.fiscaladmin.mtca.cmbb.service.OutcomeService;

/**
 * EventEmitter — audit-chain verification + KPI emission + Gold probe
 * (CMBB-F09, WF-FR-020 full / WF-FR-004 / GCMF §3.5, I-1). Form post-processor.
 *
 * Modes:
 *  VERIFY — cmChainCheck post-processor: ChainVerifyService re-hashes each
 *           case's cmEvent chain (one case if caseId set, else all), writes the
 *           verdict back + appends CHAIN_VERIFIED / CHAIN_BROKEN; then emits a
 *           KPI_EMITTED event (standard dimensions) for any CLOSED case missing
 *           one. Read-only over cmEvent (immutability, WF-FR-020).
 *  PROBE  — cmGoldProbe post-processor: GoldMartClient.fetchProfile(tin) (I-1),
 *           writes the profile + LIVE/CACHE source back (INT-FR-004 cache path).
 */
public class EventEmitter extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = EventEmitter.class.getName();
    private static final String F_CHECK = "cmChainCheck";
    private static final String F_PROBE = "cmGoldProbe";
    private static final String F_CASE = "cmCase";
    private static final String F_EVENT = "cmEvent";
    private static final int FETCH_ALL = 100000;
    // DEV defaults — applied when the post-processor invocation carries no
    // configured property (plugin-property `value` defaults are UI-only and are
    // NOT injected at runtime, so getPropertyString returns "" here).
    private static final String DEF_READER_URL = "jdbc:clickhouse://localhost:8123/sta_v1";
    private static final String DEF_READER_USER = "sta_reader";
    private static final String DEF_READER_PASS = "sta_reader_dev";

    @Override
    public String getName() {
        return "CMBB Event Emitter";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Audit-chain verification, KPI emission and Gold-mart probe (CMBB-F09).";
    }

    @Override
    public String getLabel() {
        return "CMBB Event Emitter";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/eventEmitter.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("EventEmitter: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        if ("PROBE".equalsIgnoreCase(mode)) {
            return probe(dao, properties, actor);
        }
        return verify(dao, properties, actor);
    }

    // ---- VERIFY + KPI ----
    private Object verify(FormDataDao dao, Map properties, String actor) {
        String runId = resolveRecordId(dao, properties, F_CHECK);
        FormRow run = runId == null ? null : dao.load(F_CHECK, F_CHECK, runId);
        String only = run == null ? "" : nz(run.getProperty("caseId"));

        ChainVerifyService cvs = new ChainVerifyService(dao);
        List<String> caseIds = only.isEmpty() ? cvs.allCaseIds() : java.util.Collections.singletonList(only);

        int checked = 0;
        boolean allOk = true;
        String firstBadCase = "";
        long firstBadSeq = -1;
        String firstReason = "";
        for (String caseId : caseIds) {
            ChainVerifyService.Result r = cvs.verify(caseId);
            checked++;
            if (!r.ok && allOk) {
                allOk = false;
                firstBadCase = caseId;
                firstBadSeq = r.firstBadSeq;
                firstReason = r.reason;
            }
        }

        CaseEventWriter events = new CaseEventWriter(dao);
        String verdict = allOk ? "VERIFIED" : "BROKEN";
        String stampCase = allOk ? (caseIds.isEmpty() ? null : caseIds.get(0)) : firstBadCase;
        if (stampCase != null && !stampCase.isEmpty()) {
            if (allOk) {
                events.append(stampCase, "CHAIN_VERIFIED", actor, "", "",
                        "chain intact (" + checked + " case(s))", null);
            } else {
                events.append(stampCase, "CHAIN_BROKEN", actor, "", "",
                        firstReason, "\"firstBadSeq\":\"" + CaseEventWriter.esc(
                                String.format("%010d", Math.max(firstBadSeq, 0))) + "\"");
            }
        }

        int kpis = emitKpis(dao, actor);

        String detail = verdict + "; checked=" + checked + "; kpiEmitted=" + kpis
                + (allOk ? "" : "; firstBad=" + firstBadCase + "@" + firstBadSeq + " (" + firstReason + ")");
        LogUtil.info(CLASS_NAME, "VERIFY " + detail);
        if (run != null) {
            run.setProperty("result", verdict);
            run.setProperty("checkedCount", String.valueOf(checked));
            run.setProperty("firstBadCaseId", firstBadCase);
            run.setProperty("firstBadSeq", firstBadSeq < 0 ? "" : String.format("%010d", firstBadSeq));
            run.setProperty("detail", detail);
            saveRow(dao, F_CHECK, run);
        }
        return null;
    }

    /** Emit one KPI_EMITTED per CLOSED case that has none (GCMF §3.5 dimensions). */
    private int emitKpis(FormDataDao dao, String actor) {
        dao.updateSchema(F_CASE, F_CASE, new FormRowSet());
        dao.updateSchema(F_EVENT, F_EVENT, new FormRowSet());
        java.util.Set<String> terminal = terminalStateCodes(dao);
        FormRowSet cases = dao.find(F_CASE, F_CASE, null, null,
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return 0;
        }
        CaseEventWriter events = new CaseEventWriter(dao);
        int n = 0;
        for (FormRow c : cases) {
            if (!terminal.contains(nz(c.getProperty("currentState")))) {
                continue;
            }
            String caseId = c.getId();
            Long existing = dao.count(F_EVENT, F_EVENT,
                    "WHERE e.customProperties.caseId = ?1 AND e.customProperties.eventType = ?2",
                    new Object[]{caseId, "KPI_EMITTED"});
            if (existing != null && existing > 0) {
                continue;
            }
            String dims = kpiDims(dao, c, caseId);
            events.append(caseId, "KPI_EMITTED", actor, "", "", "case KPI", dims);
            n++;
        }
        return n;
    }

    private String kpiDims(FormDataDao dao, FormRow c, String caseId) {
        String caseType = nz(c.getProperty("caseType"));
        String tin = nz(c.getProperty("tin"));
        String taxType = nz(c.getProperty("taxType"));
        long cycle = cycleTimeDays(dao, caseId);
        boolean breached = slaBreached(dao, caseId);
        String code = OutcomeService.resolveOutcomeCode(dao, caseId);
        StringBuilder b = new StringBuilder();
        b.append("\"caseType\":\"").append(CaseEventWriter.esc(caseType)).append('"');
        b.append(",\"tin\":\"").append(CaseEventWriter.esc(tin)).append('"');
        b.append(",\"taxType\":\"").append(CaseEventWriter.esc(taxType)).append('"');
        b.append(",\"cycleTimeDays\":\"").append(cycle).append('"');
        b.append(",\"slaBreached\":\"").append(breached).append('"');
        b.append(",\"outcomeCode\":\"").append(CaseEventWriter.esc(code)).append('"');
        return b.toString();
    }

    private long cycleTimeDays(FormDataDao dao, String caseId) {
        FormRowSet rows = dao.find(F_EVENT, F_EVENT,
                "WHERE e.customProperties.caseId = ?1", new Object[]{caseId},
                "seq", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        String first = rows.get(0).getProperty("eventTime");
        String last = rows.get(rows.size() - 1).getProperty("eventTime");
        try {
            java.time.LocalDateTime a = java.time.LocalDateTime.parse(first);
            java.time.LocalDateTime b = java.time.LocalDateTime.parse(last);
            return java.time.Duration.between(a, b).toDays();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean slaBreached(FormDataDao dao, String caseId) {
        try {
            dao.updateSchema("cmDeadline", "cmDeadline", new FormRowSet());
            Long n = dao.count("cmDeadline", "cmDeadline",
                    "WHERE e.customProperties.caseId = ?1 AND e.customProperties.status = ?2",
                    new Object[]{caseId, "BREACHED"});
            return n != null && n > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private java.util.Set<String> terminalStateCodes(FormDataDao dao) {
        dao.updateSchema("mmState", "mmState", new FormRowSet());
        FormRowSet rows = dao.find("mmState", "mmState",
                "WHERE e.customProperties.isTerminal = ?1", new Object[]{"true"},
                "code", Boolean.FALSE, 0, FETCH_ALL);
        java.util.Set<String> codes = new java.util.HashSet<String>();
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

    // ---- PROBE ----
    private Object probe(FormDataDao dao, Map properties, String actor) {
        String runId = resolveRecordId(dao, properties, F_PROBE);
        FormRow run = runId == null ? null : dao.load(F_PROBE, F_PROBE, runId);
        if (run == null) {
            LogUtil.warn(CLASS_NAME, "PROBE: no cmGoldProbe row resolvable — skipped");
            return null;
        }
        String tin = nz(run.getProperty("tin"));
        GoldMartClient gold = new GoldMartClient(dao, new JdbcGoldGateway(
                cfg("readerUrl", DEF_READER_URL), cfg("readerUser", DEF_READER_USER),
                cfg("readerPass", DEF_READER_PASS)));
        GoldMartClient.GoldProfile p = gold.fetchProfile(tin);
        run.setProperty("enforceableBalance", nz(p.enforceableBalance));
        run.setProperty("debtCategory", nz(p.debtCategory));
        run.setProperty("asaConfidence", nz(p.asaConfidence));
        run.setProperty("asOf", nz(p.asOf));
        run.setProperty("source", p.source);
        run.setProperty("result", "OK");
        saveRow(dao, F_PROBE, run);
        LogUtil.info(CLASS_NAME, "PROBE tin=" + tin + " source=" + p.source
                + " bal=" + p.enforceableBalance);
        return null;
    }

    // ---- shared ----
    private String resolveRecordId(FormDataDao dao, Map properties, String form) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        dao.updateSchema(form, form, new FormRowSet());
        FormRowSet rows = dao.find(form, form,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }

    private String cfg(String name, String def) {
        String v = getPropertyString(name);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private void saveRow(FormDataDao dao, String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
