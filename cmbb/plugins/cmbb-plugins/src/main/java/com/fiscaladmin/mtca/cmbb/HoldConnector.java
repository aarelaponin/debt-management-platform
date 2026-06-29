package com.fiscaladmin.mtca.cmbb;

import java.time.LocalDateTime;
import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.HoldService;

/**
 * HoldConnector — case holds &amp; suppression (CMBB-F08, GCMF §3.3-8, E1).
 * Form post-processor modes: ASSERT (cmHold), RELEASE (cmHoldRelease).
 * CAD §2.2 plugin-budget row "HoldConnector".
 */
public class HoldConnector extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = HoldConnector.class.getName();

    @Override
    public String getName() {
        return "CMBB Hold Connector";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Asserts and releases case holds; suppression scopes gate "
                + "downstream engines (CMBB-F08).";
    }

    @Override
    public String getLabel() {
        return "CMBB Hold Connector";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/holdConnector.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("HoldConnector: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        HoldService service = new HoldService(dao, new CaseEventWriter(dao));

        boolean release = "RELEASE".equalsIgnoreCase(mode);
        String form = release ? HoldService.F_RELEASE : HoldService.F_HOLD;
        String recordId = resolveRecordId(dao, properties, form);
        if (recordId == null) {
            LogUtil.warn(CLASS_NAME, mode + ": no record resolvable — skipped");
            return null;
        }
        String result = release
                ? service.release(recordId, actor, LocalDateTime.now())
                : service.assertHold(recordId, actor, LocalDateTime.now());
        LogUtil.info(CLASS_NAME, mode + " " + recordId + ": " + result);
        return null;
    }

    /** Configured id / map id / newest unprocessed (result blank) row of the form. */
    private String resolveRecordId(FormDataDao dao, Map properties, String form) {
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
        FormRowSet rows = dao.find(form, form,
                "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return rows == null || rows.isEmpty() ? null : rows.get(0).getId();
    }
}
