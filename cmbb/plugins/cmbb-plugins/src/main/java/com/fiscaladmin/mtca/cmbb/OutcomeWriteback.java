package com.fiscaladmin.mtca.cmbb;

import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.GoldMartClient;
import com.fiscaladmin.mtca.cmbb.service.JdbcGoldGateway;
import com.fiscaladmin.mtca.cmbb.service.JdbcOutcomeGateway;
import com.fiscaladmin.mtca.cmbb.service.OutcomeService;

/**
 * OutcomeWriteback — idempotent case-outcome writeback to the Gold mart
 * (CMBB-F09, I-2 / Blueprint §14.2.2). Form post-processor on cmOutcomeRun.
 *
 * Modes (read from the trigger row's `mode` field, else the plugin property):
 *  SHIP  — ship newly CLOSED cases not yet SHIPPED.
 *  RETRY — re-ship QUEUED ledger rows after a ClickHouse outage.
 * Reads sta_v1 via GoldMartClient for amount/detail enrichment; writes the fact
 * to ClickHouse via JdbcOutcomeGateway; the cmOutcome ledger gives exactly-once.
 */
public class OutcomeWriteback extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = OutcomeWriteback.class.getName();
    private static final String F_RUN = "cmOutcomeRun";
    // DEV defaults — plugin-property `value` defaults are UI-only, not injected
    // at runtime, so getPropertyString returns "" on a post-processor invocation.
    private static final String DEF_READER_URL = "jdbc:clickhouse://localhost:8123/sta_v1";
    private static final String DEF_READER_USER = "sta_reader";
    private static final String DEF_READER_PASS = "sta_reader_dev";
    private static final String DEF_WRITER_URL = "jdbc:clickhouse://localhost:8123/mtca_ors";
    private static final String DEF_WRITER_USER = "writeback_api";
    private static final String DEF_WRITER_PASS = "writeback_dev";

    @Override
    public String getName() {
        return "CMBB Outcome Writeback";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Idempotent case-outcome writeback to ClickHouse fact_case_outcomes (CMBB-F09).";
    }

    @Override
    public String getLabel() {
        return "CMBB Outcome Writeback";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/outcomeWriteback.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("OutcomeWriteback: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String runId = resolveRunId(dao, properties);
        FormRow run = runId == null ? null : dao.load(F_RUN, F_RUN, runId);
        String mode = run != null && notBlank(run.getProperty("mode"))
                ? run.getProperty("mode") : getPropertyString("mode");
        if (mode == null || mode.isEmpty()) {
            mode = "SHIP";
        }

        GoldMartClient gold = new GoldMartClient(dao, new JdbcGoldGateway(
                cfg("readerUrl", DEF_READER_URL), cfg("readerUser", DEF_READER_USER),
                cfg("readerPass", DEF_READER_PASS)));
        OutcomeService svc = new OutcomeService(dao, new JdbcOutcomeGateway(
                cfg("writerUrl", DEF_WRITER_URL), cfg("writerUser", DEF_WRITER_USER),
                cfg("writerPass", DEF_WRITER_PASS)), gold);

        OutcomeService.Tally t = "RETRY".equalsIgnoreCase(mode)
                ? svc.retryQueued(actor) : svc.shipClosed(actor);
        LogUtil.info(CLASS_NAME, mode + ": " + t);
        if (run != null) {
            run.setProperty("shippedCount", String.valueOf(t.shipped));
            run.setProperty("queuedCount", String.valueOf(t.queued));
            run.setProperty("failedCount", String.valueOf(t.failed));
            run.setProperty("result", mode + " " + t);
            FormRowSet set = new FormRowSet();
            set.add(run);
            dao.saveOrUpdate(F_RUN, F_RUN, set);
        }
        return null;
    }

    private String resolveRunId(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        dao.updateSchema(F_RUN, F_RUN, new FormRowSet());
        FormRowSet rows = dao.find(F_RUN, F_RUN,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }

    private String cfg(String name, String def) {
        String v = getPropertyString(name);
        return (v == null || v.isEmpty()) ? def : v;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isEmpty();
    }
}
