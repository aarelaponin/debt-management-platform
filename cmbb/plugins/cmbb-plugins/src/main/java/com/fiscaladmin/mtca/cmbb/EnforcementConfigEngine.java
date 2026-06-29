package com.fiscaladmin.mtca.cmbb;

import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;

import com.fiscaladmin.mtca.cmbb.service.DeadlineService;
import com.fiscaladmin.mtca.cmbb.service.EnforcementConfigService;

/**
 * EnforcementConfigEngine — DMBB-F08 enforcement-config console (DM-FR-039/040/041).
 * Form post-processor, two modes:
 *  VALIDATE — cmEnfConfigCheck: consistency-gate the enabled instrument config (testable before production).
 *  PREVIEW  — cmTemplatePreview: render a template against a case with consolidation (preview before generation).
 */
public class EnforcementConfigEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = EnforcementConfigEngine.class.getName();
    private static final String F_CHECK = "cmEnfConfigCheck";
    private static final String F_PREVIEW = "cmTemplatePreview";

    @Override
    public String getName() {
        return "DMBB Enforcement Config Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Config consistency validation + template preview/consolidation (DMBB-F08).";
    }

    @Override
    public String getLabel() {
        return "DMBB Enforcement Config Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/enforcementConfigEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("EnforcementConfigEngine: formDataDao bean not available");
        }
        EnforcementConfigService svc = new EnforcementConfigService(dao);

        if ("PREVIEW".equalsIgnoreCase(getPropertyString("mode"))) {
            String id = resolveId(dao, properties, F_PREVIEW,
                    "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                    new Object[]{""});
            FormRow row = id == null ? null : dao.load(F_PREVIEW, F_PREVIEW, id);
            if (row == null) {
                LogUtil.warn(CLASS_NAME, "PREVIEW: no request resolvable");
                return null;
            }
            EnforcementConfigService.Preview pv = svc.render(
                    DeadlineService.prop(row, "templateCode"),
                    DeadlineService.prop(row, "debtCaseId"),
                    DeadlineService.prop(row, "consolidationMode"));
            row.setProperty("noticeCount", String.valueOf(pv.noticeCount));
            row.setProperty("renderedSubject", pv.subject);
            row.setProperty("renderedBody", pv.body);
            row.setProperty("result", pv.detail);
            save(dao, F_PREVIEW, row);
            LogUtil.info(CLASS_NAME, "PREVIEW " + id + ": " + pv.detail);
            return null;
        }

        // VALIDATE (default)
        String id = resolveId(dao, properties, F_CHECK,
                "e.customProperties.valid = ?1 OR e.customProperties.valid IS NULL",
                new Object[]{""});
        FormRow row = id == null ? null : dao.load(F_CHECK, F_CHECK, id);
        if (row == null) {
            LogUtil.warn(CLASS_NAME, "VALIDATE: no check resolvable");
            return null;
        }
        EnforcementConfigService.Result r = svc.validate(DeadlineService.prop(row, "scope"));
        row.setProperty("valid", String.valueOf(r.valid));
        row.setProperty("issueCount", String.valueOf(r.issueCount));
        row.setProperty("issues", String.join("\n", r.issues));
        row.setProperty("result", "VALIDATE " + r);
        save(dao, F_CHECK, row);
        LogUtil.info(CLASS_NAME, "VALIDATE " + id + ": " + r);
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
}
