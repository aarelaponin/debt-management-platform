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

import com.fiscaladmin.mtca.cmbb.service.DispatchService;

/**
 * NotificationDispatcher — multi-channel notifications, internal alerts,
 * outbound log (CMBB-F06, WF-FR-013..016). Triggered as post-processor of
 * cmDispatchRun (cron-able via form API; result written back).
 */
public class NotificationDispatcher extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = NotificationDispatcher.class.getName();
    private static final String F_RUN = "cmDispatchRun";

    @Override
    public String getName() {
        return "CMBB Notification Dispatcher";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Consumes NOTIF_PENDING events: internal alerts + rule/template "
                + "driven multi-channel notifications with outbound log (CMBB-F06).";
    }

    @Override
    public String getLabel() {
        return "CMBB Notification Dispatcher";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/notificationDispatcher.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("NotificationDispatcher: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String result = new DispatchService(dao).dispatch(actor, LocalDateTime.now());
        LogUtil.info(CLASS_NAME, "DISPATCH: " + result);

        String runId = resolveRunId(dao, properties);
        if (runId != null) {
            FormRow run = dao.load(F_RUN, F_RUN, runId);
            if (run != null) {
                run.setProperty("result", result);
                FormRowSet set = new FormRowSet();
                set.add(run);
                dao.saveOrUpdate(F_RUN, F_RUN, set);
            }
        }
        return null;
    }

    private String resolveRunId(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        FormRowSet rows = dao.find(F_RUN, F_RUN,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }
}
