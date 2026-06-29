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
import com.fiscaladmin.mtca.cmbb.service.EnforcementActionService;

/**
 * EnforcementActionEngine — DMBB-F07 enforcement actions (DM-FR-029..038).
 * Form post-processor, four modes:
 *  INITIATE      — dmAction create: gate + execute a recovery instrument by executionMode.
 *  AGENT_APPOINT — dmAgent create: appoint a recovery agent (DM-FR-037 / BR-DM-044).
 *  AGENT_REPORT  — dmAgentRpt create: log activity + accrue commission (BR-DM-045/046).
 *  SWEEP         — cmEnfActionRun: PUBLISH / AGENT_ALERT / RELEASE / CONFIRM (DM-FR-030).
 */
public class EnforcementActionEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = EnforcementActionEngine.class.getName();

    @Override
    public String getName() {
        return "DMBB Enforcement Action Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Recovery-instrument execution, agents, fees and publication (DMBB-F07).";
    }

    @Override
    public String getLabel() {
        return "DMBB Enforcement Action Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/enforcementActionEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("EnforcementActionEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String mode = getPropertyString("mode");
        EnforcementActionService svc = new EnforcementActionService(dao, new CaseEventWriter(dao));
        LocalDateTime now = LocalDateTime.now();

        if ("AGENT_APPOINT".equalsIgnoreCase(mode)) {
            String id = resolveId(dao, properties, "dmAgent",
                    "(e.customProperties.appointmentFee = ?1 OR e.customProperties.appointmentFee IS NULL)"
                            + " AND (e.customProperties.status IS NULL OR e.customProperties.status = ?2)",
                    new Object[]{"", "APPOINTED"});
            log("AGENT_APPOINT", id, id == null ? "skip" : svc.appointAgent(id, actor, now));
            return null;
        }
        if ("AGENT_REPORT".equalsIgnoreCase(mode)) {
            String id = resolveId(dao, properties, "dmAgentRpt",
                    "e.customProperties.commissionAmount = ?1 OR e.customProperties.commissionAmount IS NULL",
                    new Object[]{""});
            log("AGENT_REPORT", id, id == null ? "skip" : svc.agentReport(id, actor, now));
            return null;
        }
        if ("SWEEP".equalsIgnoreCase(mode)) {
            String runId = resolveId(dao, properties, "cmEnfActionRun",
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            FormRow run = runId == null ? null : dao.load("cmEnfActionRun", "cmEnfActionRun", runId);
            if (run == null) {
                LogUtil.warn(CLASS_NAME, "SWEEP: no run resolvable");
                return null;
            }
            String sweepMode = DeadlineService.prop(run, "sweepMode");
            LocalDateTime asOf = DeadlineService.parse(DeadlineService.prop(run, "asOf"), now);
            EnforcementActionService.Tally t = svc.sweep(sweepMode, actor, asOf);
            run.setProperty("processedCount", String.valueOf(t.processed));
            run.setProperty("actedCount", String.valueOf(t.acted));
            run.setProperty("result", sweepMode + " " + t);
            saveRow(dao, "cmEnfActionRun", run);
            LogUtil.info(CLASS_NAME, "SWEEP " + sweepMode + " asOf=" + asOf + ": " + t);
            return null;
        }

        // INITIATE (default)
        String id = resolveId(dao, properties, "dmAction",
                "e.customProperties.status = ?1 OR e.customProperties.status IS NULL",
                new Object[]{"INITIATED"});
        log("INITIATE", id, id == null ? "skip" : svc.initiate(id, actor, now));
        return null;
    }

    private String resolveId(FormDataDao dao, Map properties, String form, String cond, Object[] params) {
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
        FormRowSet rows = dao.find(form, form, "WHERE " + cond, params, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }

    private void saveRow(FormDataDao dao, String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    private void log(String mode, String id, String result) {
        LogUtil.info(CLASS_NAME, mode + " " + (id == null ? "(none)" : id) + ": " + result);
    }
}
