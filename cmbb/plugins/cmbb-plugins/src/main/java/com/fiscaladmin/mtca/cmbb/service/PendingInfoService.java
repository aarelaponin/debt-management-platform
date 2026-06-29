package com.fiscaladmin.mtca.cmbb.service;

import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Pending-information loop (CMBB-F08, GCMF §3.3-6 / REF-FR-031).
 * request: parks the case in its OnHold-envelope state (when the type has one),
 * opens a PROVIDE_INFO task and queues a taxpayer notice. response: restores the
 * pre-hold state and closes the task — the case is never re-initiated. The SLA
 * pause-on-hold (DeadlineEngine, F05) handles the clock; no clock logic here.
 */
public class PendingInfoService {

    public static final String F_REQ = "cmInfoRequest";
    public static final String F_RESP = "cmInfoResponse";
    public static final String F_CASE = "cmCase";
    public static final String F_TASK = "cmTask";

    private final FormDataDao dao;
    private final MmConfigService mm;
    private final CaseEventWriter events;

    public PendingInfoService(FormDataDao dao, MmConfigService mm, CaseEventWriter events) {
        this.dao = dao;
        this.mm = mm;
        this.events = events;
    }

    public String request(String reqId, String actor) {
        dao.updateSchema(F_REQ, F_REQ, new FormRowSet());
        dao.updateSchema(F_TASK, F_TASK, new FormRowSet());
        FormRow req = dao.load(F_REQ, F_REQ, reqId);
        if (req == null) {
            return "info request not found: " + reqId;
        }
        if (!DeadlineService.prop(req, "result").isEmpty()) {
            return "no-op: already processed";
        }
        String caseId = caseId(req);
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c == null) {
            req.setProperty("result", "case not found: " + caseId);
            save(F_REQ, req);
            return "case not found: " + caseId;
        }
        String priorState = DeadlineService.prop(c, "currentState");
        String caseType = DeadlineService.prop(c, "caseType");

        FormRow onHold = mm.stateByEnvelope(caseType, "OnHold");
        if (onHold != null) {
            String onHoldCode = onHold.getProperty("code");
            c.setProperty("currentState", onHoldCode);
            save(F_CASE, c);
            events.append(caseId, "STATE_CHANGED", actor, priorState, onHoldCode,
                    "parked for pending information", null);
        }

        FormRow task = new FormRow();
        task.setId(UUID.randomUUID().toString());
        task.setProperty("caseId", caseId);
        task.setProperty("caseRef", DeadlineService.prop(req, "caseRef"));
        task.setProperty("taskType", "PROVIDE_INFO");
        task.setProperty("status", "OPEN");
        task.setProperty("description", DeadlineService.prop(req, "infoNeeded"));
        save(F_TASK, task);

        req.setProperty("priorState", priorState);
        req.setProperty("status", "OPEN");
        req.setProperty("taskId", task.getId());
        req.setProperty("result", "REQUESTED");
        save(F_REQ, req);

        events.append(caseId, "INFO_REQUESTED", actor, "", "",
                "information requested",
                "\"requestId\":\"" + CaseEventWriter.esc(reqId) + "\"");
        events.append(caseId, "NOTIF_PENDING", actor, "", "",
                "pending-information request",
                "\"reason\":\"INFO_REQUESTED\"");
        return "REQUESTED (parked at " + (onHold == null ? priorState : onHold.getProperty("code")) + ")";
    }

    public String respond(String respId, String actor) {
        dao.updateSchema(F_RESP, F_RESP, new FormRowSet());
        FormRow resp = dao.load(F_RESP, F_RESP, respId);
        if (resp == null) {
            return "info response not found: " + respId;
        }
        if (!DeadlineService.prop(resp, "result").isEmpty()) {
            return "no-op: already processed";
        }
        String requestId = DeadlineService.prop(resp, "requestId");
        FormRow req = requestId.isEmpty() ? null : dao.load(F_REQ, F_REQ, requestId);
        if (req == null) {
            resp.setProperty("result", "request not found: " + requestId);
            save(F_RESP, resp);
            return "request not found: " + requestId;
        }
        String caseId = caseId(req);
        String priorState = DeadlineService.prop(req, "priorState");
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c != null && !priorState.isEmpty()) {
            String current = DeadlineService.prop(c, "currentState");
            c.setProperty("currentState", priorState);
            save(F_CASE, c);
            events.append(caseId, "STATE_CHANGED", actor, current, priorState,
                    "resumed after information received", null);
        }

        String taskId = DeadlineService.prop(req, "taskId");
        if (!taskId.isEmpty()) {
            FormRow task = dao.load(F_TASK, F_TASK, taskId);
            if (task != null) {
                task.setProperty("status", "CLOSED");
                save(F_TASK, task);
            }
        }

        req.setProperty("status", "RESPONDED");
        save(F_REQ, req);
        resp.setProperty("result", "RESUMED");
        save(F_RESP, resp);

        events.append(caseId, "INFO_RECEIVED", actor, "", "",
                "information received",
                "\"requestId\":\"" + CaseEventWriter.esc(requestId) + "\"");
        return "RESUMED (" + priorState + ")";
    }

    private String caseId(FormRow req) {
        String c = DeadlineService.prop(req, "caseId");
        return c.isEmpty() ? DeadlineService.prop(req, "caseRef") : c;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
