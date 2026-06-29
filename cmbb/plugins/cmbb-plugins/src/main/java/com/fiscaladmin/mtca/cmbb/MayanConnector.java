package com.fiscaladmin.mtca.cmbb;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.commons.util.LogUtil;
import org.joget.commons.util.SetupManager;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DocumentService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;
import com.fiscaladmin.mtca.cmbb.service.RestMayanClient;

/**
 * MayanConnector — document register, ADG and postal tracking
 * (CMBB-F07, WF-FR-017..019). Form post-processor modes:
 *  PUSH (cmDoc), GENERATE (cmDocGen), POSTAL (cmPostal).
 */
public class MayanConnector extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = MayanConnector.class.getName();

    @Override
    public String getName() {
        return "CMBB Mayan Connector";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Document register with Mayan EDMS binary store, automated "
                + "document generation, postal tracking (CMBB-F07).";
    }

    @Override
    public String getLabel() {
        return "CMBB Mayan Connector";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/mayanConnector.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("MayanConnector: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        File uploadRoot = new File(SetupManager.getBaseDirectory(), "app_formuploads");
        DocumentService service = new DocumentService(dao, new RestMayanClient(),
                new CaseEventWriter(dao), new MmConfigService(dao), uploadRoot);

        String form = "PUSH".equalsIgnoreCase(mode) ? DocumentService.F_DOC
                : "GENERATE".equalsIgnoreCase(mode) ? DocumentService.F_GEN
                : DocumentService.F_POSTAL;
        String recordId = resolveRecordId(dao, properties, form);
        if (recordId == null) {
            LogUtil.warn(CLASS_NAME, mode + ": no record resolvable — skipped");
            return null;
        }
        String result = "PUSH".equalsIgnoreCase(mode) ? service.push(recordId, actor)
                : "GENERATE".equalsIgnoreCase(mode)
                        ? service.generate(recordId, actor, LocalDateTime.now())
                        : service.postal(recordId, actor);
        LogUtil.info(CLASS_NAME, mode + " " + recordId + ": " + result);
        return null;
    }

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
        // newest unprocessed row of the mode's form
        String cond = DocumentService.F_DOC.equals(form)
                ? "WHERE e.customProperties.mayanDocId = ?1 OR e.customProperties.mayanDocId IS NULL"
                : DocumentService.F_GEN.equals(form)
                        ? "WHERE e.customProperties.result = ?1 OR e.customProperties.result IS NULL"
                        : "WHERE e.customProperties.processed = ?1 OR e.customProperties.processed IS NULL";
        org.joget.apps.form.model.FormRowSet rows = dao.find(form, form, cond,
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return rows == null || rows.isEmpty() ? null : rows.get(0).getId();
    }
}
