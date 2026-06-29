package com.fiscaladmin.mtca.cmbb.service;

import java.util.HashMap;
import java.util.Map;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;

/**
 * Joget implementation of DebtIdentificationService.ProcessStarter — starts the
 * CMBB cmCaseEnvelope for an auto-created DM case, bound to the case record
 * (DMBB-F03; the create-and-start capability F02 deferred here). The envelope
 * lives in the published `cmbb` app even though this runs in the `dmbb` context,
 * so the process def is resolved explicitly via AppService.
 */
public class JogetProcessStarter implements DebtIdentificationService.ProcessStarter {

    public static final String CMBB_APP = "cmbb";
    public static final String ENVELOPE = "cmCaseEnvelope";

    @Override
    public void start(String caseId, String assignee) throws Exception {
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        WorkflowManager wm = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        AppDefinition cmbb = appService.getPublishedAppDefinition(CMBB_APP);
        if (cmbb == null) {
            throw new IllegalStateException("cmbb app not published — cannot start envelope");
        }
        WorkflowProcess proc = appService.getWorkflowProcessForApp(
                CMBB_APP, cmbb.getVersion().toString(), ENVELOPE);
        if (proc == null) {
            throw new IllegalStateException("cmCaseEnvelope process not found in cmbb");
        }
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("caseId", caseId);
        vars.put("assignee", assignee == null ? "admin" : assignee);
        // processStart(processDefId, processId, variables, startUsername, recordId, startManually)
        wm.processStart(proc.getId(), null, vars, null, caseId, false);
    }
}
