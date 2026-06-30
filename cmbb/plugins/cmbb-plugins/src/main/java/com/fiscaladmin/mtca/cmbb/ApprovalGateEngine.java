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

import com.fiscaladmin.mtca.cmbb.service.ApprovalEffects;
import com.fiscaladmin.mtca.cmbb.service.ApprovalService;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;

/**
 * ApprovalGateEngine — the DECIDE half of the Decision &amp; Approval Service (#6). Form
 * post-processor on {@code cmApprovalDecision} create: reads the approver's approvalId + outcome
 * (approve / reject / return) + reason and calls {@link ApprovalService#decide} — which runs
 * separation-of-duties, the guarded + audited status transition, the first-class reasoned record,
 * and (on approve) the gate's DecisionEffect exactly once. The REQUEST half is raised from the
 * consuming lifecycle (e.g. the instalment submit), not here.
 */
public class ApprovalGateEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = ApprovalGateEngine.class.getName();
    private static final String F_DECISION = "cmApprovalDecision";

    @Override
    public String getName() {
        return "DMBB Approval Gate Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Decision & Approval Service #6 — decide (approve/reject/return) + gate.";
    }

    @Override
    public String getLabel() {
        return "DMBB Approval Gate Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "[]";
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("ApprovalGateEngine: formDataDao bean not available");
        }
        String approver = WorkflowUtil.getCurrentUsername();
        String decId = resolveDecision(dao, properties);
        if (decId == null) {
            LogUtil.warn(CLASS_NAME, "DECIDE: no cmApprovalDecision resolvable — skipped");
            return null;
        }
        FormRow dec = dao.load(F_DECISION, F_DECISION, decId);
        if (dec == null) {
            return null;
        }
        String approvalId = DeadlineService.prop(dec, "approvalId");
        String outcome = DeadlineService.prop(dec, "outcome");
        String reason = DeadlineService.prop(dec, "reason");
        ApprovalService svc = ApprovalEffects.service(dao);
        String result = svc.decide(approvalId, approver, outcome, reason, LocalDateTime.now());
        LogUtil.info(CLASS_NAME, "DECIDE " + approvalId + " (" + outcome + "): " + result);
        dec.setProperty("result", result);
        FormRowSet set = new FormRowSet();
        set.add(dec);
        dao.saveOrUpdate(F_DECISION, F_DECISION, set);
        return null;
    }

    /** Resolve the just-created decision: configured recordId, else the latest blank-result row. */
    private String resolveDecision(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        dao.updateSchema(F_DECISION, F_DECISION, new FormRowSet());
        FormRowSet rows = dao.find(F_DECISION, F_DECISION,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }
}
