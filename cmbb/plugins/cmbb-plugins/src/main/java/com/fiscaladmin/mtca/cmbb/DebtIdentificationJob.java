package com.fiscaladmin.mtca.cmbb;

import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DebtIdentificationService;
import com.fiscaladmin.mtca.cmbb.service.GoldMartScanner;
import com.fiscaladmin.mtca.cmbb.service.JdbcGoldGateway;
import com.fiscaladmin.mtca.cmbb.service.JogetProcessStarter;

/**
 * DebtIdentificationJob — DMBB-F03 (DM-FR-001/002, BR-DM-001..004). Form
 * post-processor on cmIdentRun (the cron/console trigger row). Scans sta_v1 via
 * GoldMartScanner, consolidates/categorises/dedups, and creates+starts a DM case
 * per new debtor. Lives in the shared cmbb-plugins bundle (orchestrates the CMBB
 * spine + envelope). No new XPDL.
 */
public class DebtIdentificationJob extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = DebtIdentificationJob.class.getName();
    private static final String F_RUN = "cmIdentRun";
    private static final String DEF_URL = "jdbc:clickhouse://localhost:8123/sta_v1";
    private static final String DEF_USER = "sta_reader";
    private static final String DEF_PASS = "sta_reader_dev";

    @Override
    public String getName() {
        return "DMBB Debt Identification Job";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Scans sta_v1 and auto-creates+starts consolidated DM debt cases (DMBB-F03).";
    }

    @Override
    public String getLabel() {
        return "DMBB Debt Identification Job";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/debtIdentificationJob.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("DebtIdentificationJob: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String runId = resolveRunId(dao, properties);
        FormRow run = runId == null ? null : dao.load(F_RUN, F_RUN, runId);
        String minAmount = run != null && notBlank(run.getProperty("minAmount"))
                ? run.getProperty("minAmount") : cfg("minAmount", "0");

        GoldMartScanner scanner = new GoldMartScanner(new JdbcGoldGateway(
                cfg("readerUrl", DEF_URL), cfg("readerUser", DEF_USER), cfg("readerPass", DEF_PASS)));
        DebtIdentificationService svc = new DebtIdentificationService(
                dao, scanner, new CaseEventWriter(dao), new JogetProcessStarter());

        DebtIdentificationService.Tally t = svc.identify(minAmount, actor);
        LogUtil.info(CLASS_NAME, "IDENTIFY (minAmount=" + minAmount + "): " + t);
        if (run != null) {
            run.setProperty("createdCount", String.valueOf(t.created));
            run.setProperty("skippedCount", String.valueOf(t.skipped));
            run.setProperty("failedCount", String.valueOf(t.failed));
            run.setProperty("result", "IDENTIFY " + t);
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
