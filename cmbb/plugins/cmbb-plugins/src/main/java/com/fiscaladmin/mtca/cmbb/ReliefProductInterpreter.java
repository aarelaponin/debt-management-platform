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
import com.fiscaladmin.mtca.cmbb.service.JogetProcessStarter;
import com.fiscaladmin.mtca.cmbb.service.ReliefService;

/**
 * ReliefProductInterpreter — DMBB-F06 instalment agreements (DM-FR-021..028).
 * Form post-processor, two modes:
 *  APPLY      — dmInstAgr create: eligibility/schedule/approval + ENFORCEMENT_SUPPRESS hold.
 *  COMPLIANCE — cmInstComplianceRun: evaluate schedules, auto-cancel on default + recovery case.
 */
public class ReliefProductInterpreter extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = ReliefProductInterpreter.class.getName();
    private static final String F_AGR = "dmInstAgr";
    private static final String F_RUN = "cmInstComplianceRun";

    @Override
    public String getName() {
        return "DMBB Relief Product Interpreter";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Instalment eligibility/schedule/approval + compliance & auto-cancel (DMBB-F06).";
    }

    @Override
    public String getLabel() {
        return "DMBB Relief Product Interpreter";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/reliefProductInterpreter.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("ReliefProductInterpreter: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        ReliefService svc = new ReliefService(dao, new CaseEventWriter(dao), new JogetProcessStarter());

        if ("COMPLIANCE".equalsIgnoreCase(getPropertyString("mode"))) {
            String runId = resolveId(dao, properties, F_RUN);
            FormRow run = runId == null ? null : dao.load(F_RUN, F_RUN, runId);
            LocalDateTime asOf = run == null ? LocalDateTime.now()
                    : DeadlineService.parse(DeadlineService.prop(run, "asOf"), LocalDateTime.now());
            ReliefService.Tally t = svc.compliance(actor, asOf);
            LogUtil.info(CLASS_NAME, "COMPLIANCE asOf=" + asOf + ": " + t);
            if (run != null) {
                run.setProperty("evaluatedCount", String.valueOf(t.evaluated));
                run.setProperty("cancelledCount", String.valueOf(t.cancelled));
                run.setProperty("atRiskCount", String.valueOf(t.atRisk));
                run.setProperty("result", "COMPLIANCE " + t);
                saveRow(dao, F_RUN, run);
            }
            return null;
        }

        // APPLY (default)
        String agrId = resolveId(dao, properties, F_AGR);
        if (agrId == null) {
            LogUtil.warn(CLASS_NAME, "APPLY: no dmInstAgr resolvable — skipped");
            return null;
        }
        String result = svc.apply(agrId, actor, LocalDateTime.now());
        LogUtil.info(CLASS_NAME, "APPLY " + agrId + ": " + result);
        return null;
    }

    /** dmInstAgr resolve: a just-created agreement is APPLIED/blank status; runs are result-blank. */
    private String resolveId(FormDataDao dao, Map properties, String form) {
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
        String cond = F_AGR.equals(form)
                ? "WHERE e.customProperties.status = ?1 OR e.customProperties.status IS NULL"
                : "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL";
        Object[] params = F_AGR.equals(form) ? new Object[]{"APPLIED"} : new Object[]{""};
        FormRowSet rows = dao.find(form, form, cond, params, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }

    private void saveRow(FormDataDao dao, String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
