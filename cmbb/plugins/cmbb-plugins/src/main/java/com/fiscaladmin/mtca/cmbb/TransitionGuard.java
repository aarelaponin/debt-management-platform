package com.fiscaladmin.mtca.cmbb;

import java.util.Map;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.phase.ClosePhase;
import com.fiscaladmin.mtca.cmbb.phase.GuardPhase;
import com.fiscaladmin.mtca.cmbb.phase.OpenPhase;
import com.fiscaladmin.mtca.cmbb.phase.PreClosePhase;
import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.CaseRefGenerator;
import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/**
 * TransitionGuard — the CMBB lifecycle enforcement plugin (WF-FR-001,
 * PL-TransitionGuard.md). Runs as a Process Tool on the guard activities of
 * cmCaseEnvelope (guardOpen / guardClosure / guardFinal) with property
 * phase = OPEN / PRE_CLOSE / CLOSE. Lifecycle rules are read exclusively
 * from the mm_* configuration tables (P4); every accepted or rejected
 * transition is appended to the hash-chained cmEvent history (WF-FR-020).
 * Rejections write a TRANSITION_REJECTED event, then throw — halting the
 * workflow activity so the user sees the reason (WF-FR-001).
 */
public class TransitionGuard extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = TransitionGuard.class.getName();

    @Override
    public String getName() {
        return "CMBB Transition Guard";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Enforces case lifecycle transitions from mm_* configuration; "
                + "appends tamper-evident cmEvent history (CMBB-F02, WF-FR-001).";
    }

    @Override
    public String getLabel() {
        return "CMBB Transition Guard";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/transitionGuard.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String phase = getPropertyString("phase");
        boolean requireDecision = "true".equalsIgnoreCase(getPropertyString("requireDecision"));

        WorkflowAssignment assignment = (WorkflowAssignment) properties.get("workflowAssignment");
        String caseId = resolveCaseId(getPropertyString("caseId"), assignment);
        if (caseId == null || caseId.isEmpty()) {
            LogUtil.error(CLASS_NAME, null, "TransitionGuard[" + phase + "]: no caseId resolvable");
            throw new RuntimeException("TransitionGuard: caseId not resolvable");
        }

        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("TransitionGuard: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();

        CaseEventWriter events = new CaseEventWriter(dao);
        GuardContext ctx = new GuardContext(dao, new MmConfigService(dao), events,
                new CaseRefGenerator(dao), caseId, actor, requireDecision);

        GuardPhase guardPhase = phaseFor(phase);
        try {
            guardPhase.run(ctx);
            LogUtil.info(CLASS_NAME, "TransitionGuard[" + phase + "] accepted, case " + caseId);
        } catch (InvalidTransitionException e) {
            // The rejection event commits in its own transaction before the
            // throw halts the activity — WF-FR-001 log + notification.
            events.append(caseId, "TRANSITION_REJECTED", actor,
                    e.getFromState(), e.getToState(), e.getMessage(), null);
            LogUtil.warn(CLASS_NAME, "TransitionGuard[" + phase + "] rejected case "
                    + caseId + ": " + e.getMessage());
            throw new RuntimeException("Transition rejected: " + e.getMessage(), e);
        }
        return null;
    }

    /** Property value when configured; falls back to the origin process id
     *  (form-workflow binding makes it the cmCase record id). */
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

    private GuardPhase phaseFor(String phase) {
        if ("OPEN".equalsIgnoreCase(phase)) {
            return new OpenPhase();
        }
        if ("PRE_CLOSE".equalsIgnoreCase(phase)) {
            return new PreClosePhase();
        }
        if ("CLOSE".equalsIgnoreCase(phase)) {
            return new ClosePhase();
        }
        throw new RuntimeException("TransitionGuard: unknown phase '" + phase + "'");
    }
}
