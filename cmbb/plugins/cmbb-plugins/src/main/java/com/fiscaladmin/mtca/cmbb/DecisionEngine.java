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
import com.fiscaladmin.mtca.cmbb.service.DecisionService;
import com.fiscaladmin.mtca.cmbb.service.LinkService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;
import com.fiscaladmin.mtca.cmbb.service.PendingInfoService;

/**
 * DecisionEngine — decisions &amp; approvals, case linkage, pending-information
 * loop (CMBB-F08, GCMF §3.3-6/7/10, DPM D6). Form post-processor modes:
 *  DECIDE (cmDecision), LINK (cmLink), INFO_REQUEST (cmInfoRequest),
 *  INFO_RESPONSE (cmInfoResponse). Budget growth recorded in CAD (FIS A1).
 */
public class DecisionEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = DecisionEngine.class.getName();

    @Override
    public String getName() {
        return "CMBB Decision Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Authority-gated decisions, typed case linkage and the "
                + "pending-information loop (CMBB-F08).";
    }

    @Override
    public String getLabel() {
        return "CMBB Decision Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/decisionEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        String mode = getPropertyString("mode");
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("DecisionEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        CaseEventWriter events = new CaseEventWriter(dao);

        String form = formFor(mode);
        String recordId = resolveRecordId(dao, properties, form);
        if (recordId == null) {
            LogUtil.warn(CLASS_NAME, mode + ": no record resolvable — skipped");
            return null;
        }
        String result;
        if ("DECIDE".equalsIgnoreCase(mode)) {
            result = new DecisionService(dao, events).decide(recordId, actor, LocalDateTime.now());
        } else if ("LINK".equalsIgnoreCase(mode)) {
            result = new LinkService(dao, events).link(recordId, actor);
        } else if ("INFO_REQUEST".equalsIgnoreCase(mode)) {
            result = new PendingInfoService(dao, new MmConfigService(dao), events)
                    .request(recordId, actor);
        } else if ("INFO_RESPONSE".equalsIgnoreCase(mode)) {
            result = new PendingInfoService(dao, new MmConfigService(dao), events)
                    .respond(recordId, actor);
        } else {
            throw new RuntimeException("DecisionEngine: unknown mode '" + mode + "'");
        }
        LogUtil.info(CLASS_NAME, mode + " " + recordId + ": " + result);
        return null;
    }

    private String formFor(String mode) {
        if ("DECIDE".equalsIgnoreCase(mode)) {
            return DecisionService.F_DECISION;
        }
        if ("LINK".equalsIgnoreCase(mode)) {
            return LinkService.F_LINK;
        }
        if ("INFO_REQUEST".equalsIgnoreCase(mode)) {
            return PendingInfoService.F_REQ;
        }
        if ("INFO_RESPONSE".equalsIgnoreCase(mode)) {
            return PendingInfoService.F_RESP;
        }
        throw new RuntimeException("DecisionEngine: unknown mode '" + mode + "'");
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
