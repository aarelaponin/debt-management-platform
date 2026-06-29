package com.fiscaladmin.mtca.cmbb;

import java.util.Map;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.AllocationService;
import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/**
 * AllocationEngine — rule-based case assignment + reassignment
 * (CMBB-F03, WF-FR-007/008, BR-WF-005/006/007, PL-AllocationEngine.md).
 *
 * Modes:
 *  ASSIGN   — tool #2 in guardOpen (after TransitionGuard): policy/COI/workload
 *             selection; sets cmCase.assignee + process variable `assignee`;
 *             failure = routed UNASSIGNED state, NEVER a workflow halt.
 *  REASSIGN — post-processor of cmReassign save: single/bulk per order row.
 */
public class AllocationEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = AllocationEngine.class.getName();

    @Override
    public String getName() {
        return "CMBB Allocation Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Configurable case assignment (workload, specialisation, COI, "
                + "round-robin) and supervised reassignment (CMBB-F03).";
    }

    @Override
    public String getLabel() {
        return "CMBB Allocation Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/allocationEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("AllocationEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        AllocationService service = new AllocationService(dao,
                new MmConfigService(dao), new CaseEventWriter(dao));

        if ("REASSIGN".equalsIgnoreCase(mode)) {
            String orderId = resolveOrderId(dao, properties);
            if (orderId == null) {
                LogUtil.warn(CLASS_NAME, "REASSIGN: no order record resolvable — skipped");
                return null;
            }
            String result = service.reassign(orderId, actor);
            LogUtil.info(CLASS_NAME, "REASSIGN order " + orderId + ": " + result);
            return null;
        }

        // ASSIGN (default)
        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");
        String caseId = resolveCaseId(getPropertyString("caseId"), assignment);
        if (caseId == null || caseId.isEmpty()) {
            throw new RuntimeException("AllocationEngine: caseId not resolvable");
        }
        String officer = service.assign(caseId, actor);
        if (officer != null && assignment != null) {
            // caseOfficer participant resolves via workflow variable `assignee`
            WorkflowManager wm = (WorkflowManager)
                    AppUtil.getApplicationContext().getBean("workflowManager");
            if (wm != null) {
                wm.processVariable(assignment.getProcessId(), "assignee", officer);
            }
        }
        LogUtil.info(CLASS_NAME, "ASSIGN case " + caseId + " -> "
                + (officer == null ? "UNASSIGNED (supervisor queue)" : officer));
        return null;
    }

    /** Same fallback chain as TransitionGuard. */
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

    /**
     * Post-processor record resolution (enterprise contract not in community
     * source — defensive chain): explicit property → properties-map recordId →
     * newest cmReassign row without a result.
     */
    private String resolveOrderId(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        FormRowSet rows = dao.find(AllocationService.F_REASSIGN, AllocationService.F_REASSIGN,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        FormRow row = rows.get(0);
        return row.getId();
    }
}
