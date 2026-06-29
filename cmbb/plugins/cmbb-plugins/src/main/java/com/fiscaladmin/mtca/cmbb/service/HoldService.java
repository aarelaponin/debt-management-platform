package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Hold service core (CMBB-F08, GCMF §3.3-8 / DPM D8 / enrichment E1).
 * Asserts and releases the holds a case carries, keeping hold ↔ case-state
 * consistency. Suppression scopes (CORRESPONDENCE_SUPPRESS / ENFORCEMENT_SUPPRESS)
 * are recognised here and consumed by DispatchService and DMBB enforcement.
 * Financial scopes (COLLECTION / DISBURSEMENT / OFFSET) are recorded with their
 * owning BB (targetBB) + event; the cross-BB stay call is a DMBB-era integration
 * over the HoldConnector contract (CAD §4). Constructor-injected for unit tests.
 */
public class HoldService {

    public static final String F_HOLD = "cmHold";
    public static final String F_RELEASE = "cmHoldRelease";
    public static final String F_CASE = "cmCase";
    static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final FormDataDao dao;
    private final CaseEventWriter events;

    public HoldService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
    }

    /** Activate the referenced cmHold. Idempotent: an ACTIVE/RELEASED hold is a no-op. */
    public String assertHold(String holdId, String actor, LocalDateTime now) {
        // reads/writes never auto-create tables; a missing relation poisons the
        // JTA tx (DX9-DELTAS) — ensure the carrier exists before first touch.
        dao.updateSchema(F_HOLD, F_HOLD, new FormRowSet());
        FormRow hold = dao.load(F_HOLD, F_HOLD, holdId);
        if (hold == null) {
            return "hold not found: " + holdId;
        }
        String status = DeadlineService.prop(hold, "status");
        if ("ACTIVE".equals(status) || "RELEASED".equals(status)) {
            return "no-op: hold already " + status;
        }
        String caseId = caseId(hold);
        String scope = DeadlineService.prop(hold, "scope");
        hold.setProperty("status", "ACTIVE");
        hold.setProperty("assertedBy", actor == null ? "" : actor);
        hold.setProperty("assertedAt", ISO.format(now));
        hold.setProperty("result", "ASSERTED");
        save(F_HOLD, hold);
        events.append(caseId, "HOLD_ASSERTED", actor, "", "",
                "hold asserted: " + scope,
                "\"holdId\":\"" + CaseEventWriter.esc(holdId) + "\""
                        + ",\"scope\":\"" + CaseEventWriter.esc(scope) + "\""
                        + ",\"holdType\":\"" + CaseEventWriter.esc(DeadlineService.prop(hold, "holdType")) + "\""
                        + ",\"basis\":\"" + CaseEventWriter.esc(DeadlineService.prop(hold, "basis")) + "\""
                        + ",\"targetBB\":\"" + CaseEventWriter.esc(DeadlineService.prop(hold, "targetBB")) + "\"");
        return "ACTIVE " + scope;
    }

    /** Release the cmHold referenced by a cmHoldRelease row. Idempotent. */
    public String release(String releaseId, String actor, LocalDateTime now) {
        dao.updateSchema(F_HOLD, F_HOLD, new FormRowSet());
        FormRow rel = dao.load(F_RELEASE, F_RELEASE, releaseId);
        if (rel == null) {
            return "release order not found: " + releaseId;
        }
        String holdId = DeadlineService.prop(rel, "holdId");
        String reason = DeadlineService.prop(rel, "releaseReason");
        String result;
        FormRow hold = holdId.isEmpty() ? null : dao.load(F_HOLD, F_HOLD, holdId);
        if (hold == null) {
            result = "hold not found: " + holdId;
        } else if ("RELEASED".equals(DeadlineService.prop(hold, "status"))) {
            result = "no-op: hold already RELEASED";
        } else {
            String caseId = caseId(hold);
            String scope = DeadlineService.prop(hold, "scope");
            hold.setProperty("status", "RELEASED");
            hold.setProperty("releasedBy", actor == null ? "" : actor);
            hold.setProperty("releasedAt", ISO.format(now));
            hold.setProperty("releaseReason", reason);
            save(F_HOLD, hold);
            events.append(caseId, "HOLD_RELEASED", actor, "", "",
                    "hold released: " + scope,
                    "\"holdId\":\"" + CaseEventWriter.esc(holdId) + "\""
                            + ",\"reason\":\"" + CaseEventWriter.esc(reason) + "\"");
            result = "RELEASED " + scope;
        }
        rel.setProperty("result", result);
        save(F_RELEASE, rel);
        return result;
    }

    /** Active holds of a scope for a case (read helper, e.g. for downstream guards). */
    public boolean hasActiveScope(String caseId, String scope) {
        Long n = dao.count(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{caseId, scope, "ACTIVE"});
        return n != null && n > 0;
    }

    private String caseId(FormRow hold) {
        String c = DeadlineService.prop(hold, "caseId");
        return c.isEmpty() ? DeadlineService.prop(hold, "caseRef") : c;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
