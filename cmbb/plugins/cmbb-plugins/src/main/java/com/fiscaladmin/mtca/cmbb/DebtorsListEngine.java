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
import com.fiscaladmin.mtca.cmbb.service.DebtorsListService;

/**
 * DebtorsListEngine — DMBB-F11 debtors registry extract (DM-FR-058).
 * Form post-processor, one mode:
 *  EXTRACT — dmDebtorsExtract create: build a registry extract from the PUBLISHED dmDebtorPub set.
 * (The DM-FR-057 debtors-list view is the list_debtorsList JDBC datalist — no engine.)
 */
public class DebtorsListEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = DebtorsListEngine.class.getName();

    @Override
    public String getName() {
        return "DMBB Debtors List Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Debtors registry extract generation (DMBB-F11).";
    }

    @Override
    public String getLabel() {
        return "DMBB Debtors List Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/debtorsListEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("DebtorsListEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        DebtorsListService svc = new DebtorsListService(dao, new CaseEventWriter(dao));
        String id = resolveId(dao, properties, "dmDebtorsExtract",
                "e.customProperties.status = ?1 OR e.customProperties.status IS NULL",
                new Object[]{""});
        LogUtil.info(CLASS_NAME, "EXTRACT " + (id == null ? "(none)" : id) + ": "
                + (id == null ? "skip" : svc.extract(id, actor, LocalDateTime.now())));
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
}
