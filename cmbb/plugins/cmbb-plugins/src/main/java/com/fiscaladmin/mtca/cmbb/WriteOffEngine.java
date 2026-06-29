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
import com.fiscaladmin.mtca.cmbb.service.WriteOffService;

/**
 * WriteOffEngine — DMBB-F09 write-off (DM-FR-042..046). Form post-processor, three modes:
 *  SUBMIT  — dmWriteOff create: evidence guard + delegation; C1_AUTO posts, else UNDER_REVIEW.
 *  APPROVE — cmWriteOffApprove create: an officer's decision posts or rejects.
 *  SWEEP   — cmWriteOffRun: AUTO_C1 / STATUTORY_BULK / C2_PASSIVE.
 */
public class WriteOffEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = WriteOffEngine.class.getName();

    @Override
    public String getName() {
        return "DMBB Write-Off Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Auto-C1 / approved / statutory write-off with status preservation (DMBB-F09).";
    }

    @Override
    public String getLabel() {
        return "DMBB Write-Off Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/writeOffEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("WriteOffEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String mode = getPropertyString("mode");
        WriteOffService svc = new WriteOffService(dao, new CaseEventWriter(dao));
        LocalDateTime now = LocalDateTime.now();

        if ("APPROVE".equalsIgnoreCase(mode)) {
            String id = resolveId(dao, properties, "cmWriteOffApprove",
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            log("APPROVE", id, id == null ? "skip" : svc.approve(id, actor, now));
            return null;
        }
        if ("SWEEP".equalsIgnoreCase(mode)) {
            String runId = resolveId(dao, properties, "cmWriteOffRun",
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            FormRow run = runId == null ? null : dao.load("cmWriteOffRun", "cmWriteOffRun", runId);
            if (run == null) {
                LogUtil.warn(CLASS_NAME, "SWEEP: no run resolvable");
                return null;
            }
            String sweepMode = DeadlineService.prop(run, "sweepMode");
            LocalDateTime asOf = DeadlineService.parse(DeadlineService.prop(run, "asOf"), now);
            WriteOffService.Tally t = svc.sweep(sweepMode, actor, asOf);
            run.setProperty("processedCount", String.valueOf(t.processed));
            run.setProperty("writtenOffCount", String.valueOf(t.writtenOff));
            run.setProperty("result", sweepMode + " " + t);
            save(dao, "cmWriteOffRun", run);
            LogUtil.info(CLASS_NAME, "SWEEP " + sweepMode + " asOf=" + asOf + ": " + t);
            return null;
        }

        // SUBMIT (default)
        String id = resolveId(dao, properties, "dmWriteOff",
                "e.customProperties.status = ?1 OR e.customProperties.status IS NULL",
                new Object[]{"SUBMITTED"});
        log("SUBMIT", id, id == null ? "skip" : svc.submit(id, actor, now));
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
