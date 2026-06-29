package com.fiscaladmin.mtca.cmbb;

import java.time.LocalDateTime;
import java.util.Map;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/**
 * DeadlineEngine — SLA clocks, traffic lights, escalation
 * (CMBB-F05, WF-FR-010..012, PL-DeadlineEngine.md).
 *
 * Modes:
 *  START — tool #3 in guardOpen: create cmDeadline clocks from mmSla config.
 *  CLOSE — tool #2 in guardFinal: open clocks -> MET.
 *  SWEEP — post-processor of cmSweepRun save (cron-able via form API):
 *          pause/resume, thresholds, escalation, case slaStatus. The sweep
 *          row's optional asOf field allows deterministic time-travel (tests).
 */
public class DeadlineEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = DeadlineEngine.class.getName();
    private static final String F_SWEEP = "cmSweepRun";

    @Override
    public String getName() {
        return "CMBB Deadline Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "SLA clocks from mmSla config, traffic-light status, "
                + "threshold escalation (CMBB-F05).";
    }

    @Override
    public String getLabel() {
        return "CMBB Deadline Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/deadlineEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("DeadlineEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        DeadlineService service = new DeadlineService(dao,
                new MmConfigService(dao), new CaseEventWriter(dao));

        if ("SWEEP".equalsIgnoreCase(mode)) {
            String runId = resolveSweepId(dao, properties);
            FormRow run = runId == null ? null : dao.load(F_SWEEP, F_SWEEP, runId);
            LocalDateTime asOf = run == null ? LocalDateTime.now()
                    : DeadlineService.parse(DeadlineService.prop(run, "asOf"),
                            LocalDateTime.now());
            String result = service.sweep(actor, asOf);
            LogUtil.info(CLASS_NAME, "SWEEP asOf=" + asOf + ": " + result);
            if (run != null) {
                run.setProperty("result", result);
                FormRowSet set = new FormRowSet();
                set.add(run);
                dao.saveOrUpdate(F_SWEEP, F_SWEEP, set);
            }
            return null;
        }

        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");
        String caseId = resolveCaseId(getPropertyString("caseId"), assignment);
        if (caseId == null || caseId.isEmpty()) {
            throw new RuntimeException("DeadlineEngine[" + mode + "]: caseId not resolvable");
        }
        if ("CLOSE".equalsIgnoreCase(mode)) {
            int n = service.close(caseId, actor);
            LogUtil.info(CLASS_NAME, "CLOSE case " + caseId + ": " + n + " clock(s) met");
        } else { // START
            int n = service.start(caseId, actor, LocalDateTime.now());
            LogUtil.info(CLASS_NAME, "START case " + caseId + ": " + n + " clock(s) created");
        }
        return null;
    }

    private String resolveCaseId(String configured, WorkflowAssignment assignment) {
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        if (assignment == null) {
            return null;
        }
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        return appService.getOriginProcessId(assignment.getProcessId());
    }

    /** Same defensive chain as AllocationEngine.resolveOrderId. */
    private String resolveSweepId(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        FormRowSet rows = dao.find(F_SWEEP, F_SWEEP,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }
}
