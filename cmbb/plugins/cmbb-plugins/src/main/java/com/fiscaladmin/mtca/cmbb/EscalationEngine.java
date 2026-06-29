package com.fiscaladmin.mtca.cmbb;

import java.time.LocalDateTime;
import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;
import com.fiscaladmin.mtca.cmbb.service.EscalationService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;
import com.fiscaladmin.mtca.cmbb.service.StrategyAdminService;

/**
 * EscalationEngine — DMBB-F04 (DM-FR-009..014). Form post-processor on
 * cmEscalateRun (the cron/console trigger row; cmSweepRun pattern). Walks the
 * collection-strategy ladder against open DM cases and emits notices via the F06
 * dispatcher. The run row's optional asOf gives deterministic time-travel (tests).
 */
public class EscalationEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = EscalationEngine.class.getName();
    private static final String F_RUN = "cmEscalateRun";
    private static final String F_CHECK = "cmStrategyCheck";

    @Override
    public String getName() {
        return "DMBB Escalation Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Walks the collection-strategy ladder (reminder->demand->enforcement) "
                + "for open DM cases and issues notices (DMBB-F04).";
    }

    @Override
    public String getLabel() {
        return "DMBB Escalation Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/escalationEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("EscalationEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();

        if ("VALIDATE".equalsIgnoreCase(getPropertyString("mode"))) {
            return validate(dao);
        }

        String runId = resolveRunId(dao, properties, F_RUN);
        FormRow run = runId == null ? null : dao.load(F_RUN, F_RUN, runId);
        LocalDateTime asOf = run == null ? LocalDateTime.now()
                : DeadlineService.parse(DeadlineService.prop(run, "asOf"), LocalDateTime.now());

        EscalationService svc = new EscalationService(dao, new MmConfigService(dao),
                new CaseEventWriter(dao));
        EscalationService.Tally t = svc.sweep(actor, asOf);
        LogUtil.info(CLASS_NAME, "SWEEP asOf=" + asOf + ": " + t);
        if (run != null) {
            run.setProperty("casesScanned", String.valueOf(t.cases));
            run.setProperty("stepsFired", String.valueOf(t.escalated));
            run.setProperty("skippedCount", String.valueOf(t.skipped));
            run.setProperty("result", "SWEEP " + t);
            FormRowSet set = new FormRowSet();
            set.add(run);
            dao.saveOrUpdate(F_RUN, F_RUN, set);
        }
        return null;
    }

    /** VALIDATE mode: strategy-admin consistency check (cmStrategyCheck). */
    private Object validate(FormDataDao dao) {
        StrategyAdminService.Result res = new StrategyAdminService(dao).validate();
        LogUtil.info(CLASS_NAME, "VALIDATE: " + res.summary());
        String checkId = resolveRunId(dao, new java.util.HashMap(), F_CHECK);
        FormRow chk = checkId == null ? null : dao.load(F_CHECK, F_CHECK, checkId);
        if (chk != null) {
            chk.setProperty("valid", res.valid ? "true" : "false");
            chk.setProperty("issueCount", String.valueOf(res.issues.size()));
            chk.setProperty("result", res.summary());
            FormRowSet set = new FormRowSet();
            set.add(chk);
            dao.saveOrUpdate(F_CHECK, F_CHECK, set);
        }
        return null;
    }

    private String resolveRunId(FormDataDao dao, Map properties, String form) {
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
}
