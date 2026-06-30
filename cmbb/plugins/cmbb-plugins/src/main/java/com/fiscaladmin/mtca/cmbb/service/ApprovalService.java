package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * ApprovalService — CMBB Decision &amp; Approval Service (GCMF Core Service #6), minimal slice.
 *
 * <p>Mechanism only (the meaning is config): a module lifecycle <b>requests</b> approval for an
 * action; the service resolves the required authority from the {@code mdApprovalPolicy} matrix
 * (threshold band → role), and either lets the gate open immediately (below the lowest band — no
 * approval required) or raises a {@code Pending} {@code cmApproval}. An authority then
 * <b>decides</b> (approve / reject / return) with a <b>mandatory reason</b>; separation-of-duties
 * (approver ≠ requester) is a blocking filter; the request lifecycle is guarded + audited by
 * {@link StatusManager}; a first-class reasoned {@code APPROVAL_DECISION} record is appended to the
 * case hash-chain; and on {@code Approved} the {@link DecisionEffect} for the action runs once
 * (the gate). No e-signature, no document rendering (AP-D2); a human always decides (AP-D5).
 *
 * <p>Deferred (this slice): chain/quorum/batch topologies, escalation/delegation/timeout, and the
 * Independence &amp; COI service (#3) — SoD here is the four-eyes minimum.
 */
public class ApprovalService {

    public static final String F_APPROVAL = "cmApproval";
    public static final String F_POLICY = "mdApprovalPolicy";
    private static final String ENTITY = "cmApproval";
    private static final List<String> SCOPE = Collections.singletonList("DEFAULT");
    private static final int FETCH_ALL = 100000;
    private static final String CLASS_NAME = ApprovalService.class.getName();

    /** The module's lifecycle step the gate runs when an action is approved (or auto-passes). */
    public interface DecisionEffect {
        String run(String entity, String recordId, String actor, LocalDateTime now);
    }

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final StatusManager status;
    private final Map<String, DecisionEffect> effects;

    public ApprovalService(FormDataDao dao, CaseEventWriter events, Map<String, DecisionEffect> effects) {
        this.dao = dao;
        this.events = events;
        this.effects = effects;
        this.status = new StatusManager(dao, new MmConfigService(dao), events);
    }

    // ---------------- REQUEST ----------------

    /** Raise an approval request (or auto-pass below the lowest band). */
    public String request(String entity, String recordId, String actionType, double materiality,
                          String requester, String caseId, String actor, LocalDateTime now) {
        updateSchemas();
        FormRow live = liveRequest(recordId, actionType);
        if (live != null) {
            return "already pending (" + live.getId() + ")"; // idempotent
        }
        String authority = resolveAuthority(actionType, materiality);
        if (authority == null) {
            event(caseId, "APPROVAL_NOT_REQUIRED", actor,
                    "below approval threshold for " + actionType + " (materiality " + materiality + ")",
                    extra(entity, recordId, actionType, materiality, ""));
            return "AUTO (no approval required) -> " + runEffect(entity, recordId, actionType, actor, now);
        }
        FormRow ap = new FormRow();
        String apId = "AP-" + UUID.randomUUID().toString().substring(0, 8) + "-" + recordId;
        ap.setId(apId);
        ap.setProperty("entity", entity);
        ap.setProperty("recordId", recordId);
        ap.setProperty("actionType", actionType);
        ap.setProperty("materiality", String.valueOf(materiality));
        ap.setProperty("caseId", caseId);
        ap.setProperty("requestedBy", requester);
        ap.setProperty("requiredAuthority", authority);
        ap.setProperty("status", "Pending"); // plain genesis (guarded moves run at decide)
        ap.setProperty("reason", "");
        save(F_APPROVAL, ap);
        event(caseId, "APPROVAL_REQUESTED", actor,
                "approval requested: " + actionType + " -> authority " + authority,
                extra(entity, recordId, actionType, materiality, authority));
        return "PENDING (authority=" + authority + ", id=" + apId + ")";
    }

    /**
     * Resolve the required authority from the matrix: the role of the highest threshold the
     * materiality strictly exceeds; {@code null} when it exceeds none (no approval required).
     */
    String resolveAuthority(String actionType, double materiality) {
        FormRowSet rows = dao.find(F_POLICY, F_POLICY,
                "WHERE e.customProperties.actionType = ?1", new Object[]{actionType},
                null, Boolean.FALSE, 0, FETCH_ALL);
        String best = null;
        double bestThreshold = -1;
        if (rows != null) {
            for (FormRow r : rows) {
                double thr = num(p(r, "threshold"));
                if (materiality > thr && thr >= bestThreshold) {
                    bestThreshold = thr;
                    best = p(r, "authorityRole");
                }
            }
        }
        return (best == null || best.isEmpty()) ? null : best;
    }

    // ---------------- DECIDE ----------------

    /** An authority decides a Pending request. outcome ∈ approve | reject | return. */
    public String decide(String approvalId, String approver, String outcome, String reason,
                         LocalDateTime now) {
        updateSchemas();
        FormRow ap = dao.load(F_APPROVAL, F_APPROVAL, approvalId);
        if (ap == null) {
            return "no approval " + approvalId;
        }
        if (!"Pending".equalsIgnoreCase(p(ap, "status"))) {
            return "already decided (" + p(ap, "status") + ")"; // gate-once
        }
        if (reason == null || reason.trim().isEmpty()) {
            return "reason required"; // AP-5: blank why hollows the record
        }
        String entity = p(ap, "entity");
        String recordId = p(ap, "recordId");
        String actionType = p(ap, "actionType");
        String caseId = p(ap, "caseId");
        double materiality = num(p(ap, "materiality"));
        // separation of duties — approver ≠ requester (four-eyes); blocking
        if (approver != null && approver.equalsIgnoreCase(p(ap, "requestedBy"))) {
            event(caseId, "APPROVAL_SOD_BLOCKED", approver,
                    "separation of duties: approver == requester (" + approver + ")",
                    extra(entity, recordId, actionType, materiality, p(ap, "requiredAuthority")));
            return "SoD: approver == requester";
        }
        String target = "approve".equalsIgnoreCase(outcome) ? "Approved"
                : "return".equalsIgnoreCase(outcome) ? "Returned" : "Rejected";
        ap.setProperty("decision", target);
        ap.setProperty("reason", reason);
        ap.setProperty("decidedBy", approver);
        ap.setProperty("decidedAt", now.toString());
        // guarded + audited Pending -> target (sets status + a STATUS_CHANGED row on the chain)
        status.apply(F_APPROVAL, ap, "status", caseId, target, SCOPE, approver, reason);
        save(F_APPROVAL, ap);
        // first-class reasoned decision record (§14)
        event(caseId, "APPROVAL_DECISION", approver, target + ": " + reason,
                extra(entity, recordId, actionType, materiality, p(ap, "requiredAuthority"))
                        + ",\"outcome\":\"" + CaseEventWriter.esc(target) + "\"");
        if ("Approved".equals(target)) {
            return "APPROVED -> " + runEffect(entity, recordId, actionType, approver, now);
        }
        return target.toUpperCase();
    }

    // ---------------- helpers ----------------

    private String runEffect(String entity, String recordId, String actionType, String actor,
                             LocalDateTime now) {
        DecisionEffect eff = effects == null ? null : effects.get(actionType);
        if (eff == null) {
            return "no DecisionEffect for " + actionType;
        }
        try {
            return eff.run(entity, recordId, actor, now);
        } catch (Exception e) {
            LogUtil.error(CLASS_NAME, e, "DecisionEffect failed for " + actionType + " " + recordId);
            return "effect error: " + e.getMessage();
        }
    }

    private FormRow liveRequest(String recordId, String actionType) {
        FormRowSet rows = dao.find(F_APPROVAL, F_APPROVAL,
                "WHERE e.customProperties.recordId = ?1 AND e.customProperties.actionType = ?2",
                new Object[]{recordId, actionType}, null, Boolean.FALSE, 0, FETCH_ALL);
        if (rows != null) {
            for (FormRow r : rows) {
                if ("Pending".equalsIgnoreCase(p(r, "status"))) {
                    return r;
                }
            }
        }
        return null;
    }

    private String extra(String entity, String recordId, String actionType, double materiality,
                         String authority) {
        return "\"entity\":\"" + CaseEventWriter.esc(entity) + "\""
                + ",\"recordId\":\"" + CaseEventWriter.esc(recordId) + "\""
                + ",\"actionType\":\"" + CaseEventWriter.esc(actionType) + "\""
                + ",\"materiality\":\"" + materiality + "\""
                + ",\"authority\":\"" + CaseEventWriter.esc(authority) + "\"";
    }

    private void event(String caseId, String type, String actor, String reason, String extra) {
        events.append(caseId, type, actor, "", "", reason, extra);
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    private void updateSchemas() {
        dao.updateSchema(F_APPROVAL, F_APPROVAL, new FormRowSet());
    }

    private static String p(FormRow r, String field) {
        if (r == null) {
            return "";
        }
        String v = r.getProperty(field);
        return v == null ? "" : v;
    }

    private static double num(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? 0 : Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
