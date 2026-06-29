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
import com.fiscaladmin.mtca.cmbb.service.DefaultAssessmentService;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;
import com.fiscaladmin.mtca.cmbb.service.JogetProcessStarter;

/**
 * DefaultAssessmentEngine — DMBB-F10 default-assessment tracking (DM-FR-054..056).
 * Form post-processor, three modes:
 *  ASSESS   — dmDefAssess create: estimate (BR-DM-051) + reasonableness (BR-DM-052) + informational debit.
 *  REPLACE  — cmReturnFiled create: a filed return reverses the default + records variance (DM-FR-055).
 *  ESCALATE — cmDefAssessRun: no filing past grace → create a DM debt case (DM-FR-056).
 */
public class DefaultAssessmentEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = DefaultAssessmentEngine.class.getName();

    @Override
    public String getName() {
        return "DMBB Default Assessment Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Default assessment estimate / replace / escalate (DMBB-F10).";
    }

    @Override
    public String getLabel() {
        return "DMBB Default Assessment Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/defaultAssessmentEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("DefaultAssessmentEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String mode = getPropertyString("mode");
        DefaultAssessmentService svc = new DefaultAssessmentService(
                dao, new CaseEventWriter(dao), new JogetProcessStarter());
        LocalDateTime now = LocalDateTime.now();

        if ("REPLACE".equalsIgnoreCase(mode)) {
            String id = resolveId(dao, properties, "cmReturnFiled",
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            log("REPLACE", id, id == null ? "skip" : svc.replace(id, actor, now));
            return null;
        }
        if ("ESCALATE".equalsIgnoreCase(mode)) {
            String runId = resolveId(dao, properties, "cmDefAssessRun",
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            FormRow run = runId == null ? null : dao.load("cmDefAssessRun", "cmDefAssessRun", runId);
            if (run == null) {
                LogUtil.warn(CLASS_NAME, "ESCALATE: no run resolvable");
                return null;
            }
            LocalDateTime asOf = DeadlineService.parse(DeadlineService.prop(run, "asOf"), now);
            DefaultAssessmentService.Tally t = svc.escalate(actor, asOf);
            run.setProperty("processedCount", String.valueOf(t.processed));
            run.setProperty("escalatedCount", String.valueOf(t.escalated));
            run.setProperty("result", "ESCALATE " + t);
            save(dao, "cmDefAssessRun", run);
            LogUtil.info(CLASS_NAME, "ESCALATE asOf=" + asOf + ": " + t);
            return null;
        }

        // ASSESS (default)
        String id = resolveId(dao, properties, "dmDefAssess",
                "e.customProperties.status = ?1 OR e.customProperties.status IS NULL",
                new Object[]{"DRAFT"});
        log("ASSESS", id, id == null ? "skip" : svc.assess(id, actor, now));
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

    private void save(FormDataDao dao, String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    private void log(String mode, String id, String result) {
        LogUtil.info(CLASS_NAME, mode + " " + (id == null ? "(none)" : id) + ": " + result);
    }
}
