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
import com.fiscaladmin.mtca.cmbb.service.CollectionMiService;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;

/**
 * CollectionMiEngine — DMBB-F12 collection MI (DM-FR-047/048, CAD §132). Form post-processor on
 * cmCollectionMiRun; dispatches on the row's runMode: ROLLUP (plan-vs-actual), SUGGEST (target
 * setting), RECONCILE (②/③ writeback completeness). The DM-FR-048 reports are JDBC datalists — no engine.
 */
public class CollectionMiEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = CollectionMiEngine.class.getName();
    private static final String F_RUN = "cmCollectionMiRun";

    @Override
    public String getName() {
        return "DMBB Collection MI Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Collection plan rollup / target suggest / ②-③ reconciliation (DMBB-F12).";
    }

    @Override
    public String getLabel() {
        return "DMBB Collection MI Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/collectionMiEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("CollectionMiEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        CollectionMiService svc = new CollectionMiService(dao, new CaseEventWriter(dao));
        LocalDateTime now = LocalDateTime.now();

        String runId = resolveId(dao, properties, F_RUN,
                "e.customProperties.result = ?1 OR e.customProperties.result IS NULL",
                new Object[]{""});
        FormRow run = runId == null ? null : dao.load(F_RUN, F_RUN, runId);
        if (run == null) {
            LogUtil.warn(CLASS_NAME, "no run resolvable");
            return null;
        }
        String runMode = DeadlineService.prop(run, "runMode");
        String planId = DeadlineService.prop(run, "planId");
        LocalDateTime asOf = DeadlineService.parse(DeadlineService.prop(run, "asOf"), now);

        CollectionMiService.Tally t;
        if ("SUGGEST".equalsIgnoreCase(runMode)) {
            t = svc.suggest(planId, actor, now);
        } else if ("RECONCILE".equalsIgnoreCase(runMode)) {
            t = svc.reconcile(actor, asOf);
        } else {
            t = svc.rollup(planId, actor, now);
        }
        run.setProperty("matchedCount", String.valueOf(t.matched));
        run.setProperty("varianceCount", String.valueOf(t.variance));
        run.setProperty("result", runMode + " " + t);
        save(dao, F_RUN, run);
        LogUtil.info(CLASS_NAME, runMode + " " + runId + ": " + t);
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
