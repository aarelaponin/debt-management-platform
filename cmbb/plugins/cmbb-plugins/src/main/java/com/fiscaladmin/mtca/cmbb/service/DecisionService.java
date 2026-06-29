package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Decision &amp; approval service (CMBB-F08, GCMF §3.3-7 / DPM D6).
 * On a cmDecision the required authority level is resolved from the mmAuthority
 * matrix (actionType × amount band) and rank-compared against the approver's
 * level; collegial bodies additionally need a quorum of approvals. Reasoned
 * grounds are mandatory. Outcome + events (DECISION_PROPOSED / APPROVED /
 * REJECTED) are written idempotently. Constructor-injected for unit tests.
 */
public class DecisionService {

    public static final String F_DECISION = "cmDecision";
    public static final String F_AUTH = "mmAuthority";
    static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Authority ranking (DEV controlled list; deployment may externalise — FIS A2). */
    private static final List<String> RANKS = Arrays.asList(
            "OFFICER", "SENIOR", "SUPERVISOR", "MANAGER", "DIRECTOR", "COMMISSIONER");

    private final FormDataDao dao;
    private final CaseEventWriter events;

    public DecisionService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
    }

    /** 1-based rank of a level name, or 0 when unknown/blank. */
    static int rank(String level) {
        int i = RANKS.indexOf(level == null ? "" : level.trim().toUpperCase());
        return i < 0 ? 0 : i + 1;
    }

    /** mmAuthority row for (actionType, amount in band), or null. */
    FormRow requiredRow(String actionType, double amount) {
        FormRowSet rows = dao.find(F_AUTH, F_AUTH,
                "WHERE e.customProperties.actionType = ?1", new Object[]{actionType},
                "dateCreated", Boolean.FALSE, null, null);
        if (rows == null) {
            return null;
        }
        for (FormRow r : rows) {
            double min = parseDouble(DeadlineService.prop(r, "amountMin"), 0);
            String maxStr = DeadlineService.prop(r, "amountMax");
            double max = maxStr.isEmpty() ? Double.MAX_VALUE : parseDouble(maxStr, Double.MAX_VALUE);
            if (amount >= min && amount <= max) {
                return r;
            }
        }
        return null;
    }

    /** Process a cmDecision: resolve authority, set outcome, write events. */
    public String decide(String decisionId, String actor, LocalDateTime now) {
        dao.updateSchema(F_DECISION, F_DECISION, new FormRowSet());
        FormRow d = dao.load(F_DECISION, F_DECISION, decisionId);
        if (d == null) {
            return "decision not found: " + decisionId;
        }
        if (!DeadlineService.prop(d, "result").isEmpty()) {
            return "no-op: already processed";
        }
        String caseId = caseId(d);
        String actionType = DeadlineService.prop(d, "actionType");
        double amount = parseDouble(DeadlineService.prop(d, "amount"), 0);
        String approverLevel = DeadlineService.prop(d, "approverLevel");
        String bodyType = DeadlineService.prop(d, "bodyType");
        int approvals = (int) DeadlineService.parseLong(DeadlineService.prop(d, "approvalsCount"), 0);
        String reasons = DeadlineService.prop(d, "reasons");

        events.append(caseId, "DECISION_PROPOSED", actor, "", "",
                "decision proposed: " + actionType,
                "\"decisionId\":\"" + CaseEventWriter.esc(decisionId) + "\""
                        + ",\"actionType\":\"" + CaseEventWriter.esc(actionType) + "\""
                        + ",\"amount\":\"" + CaseEventWriter.esc(DeadlineService.prop(d, "amount")) + "\"");

        String status;
        String result;
        if (reasons.isEmpty()) {
            status = "REJECTED";
            result = "REJECTED: reasoned grounds mandatory";
        } else {
            FormRow auth = requiredRow(actionType, amount);
            String requiredLevel = auth == null ? "OFFICER" : DeadlineService.prop(auth, "level");
            int quorum = auth == null ? 0
                    : (int) DeadlineService.parseLong(DeadlineService.prop(auth, "quorum"), 0);
            boolean authorityOk = rank(approverLevel) >= rank(requiredLevel);
            boolean collegialOk = !"COLLEGIAL".equalsIgnoreCase(bodyType)
                    || approvals >= Math.max(quorum, 1);
            if (authorityOk && collegialOk) {
                status = "APPROVED";
                result = "APPROVED by " + approverLevel + " (required " + requiredLevel + ")";
            } else if (!authorityOk) {
                status = "REJECTED";
                result = "REJECTED: " + approverLevel + " below required " + requiredLevel;
            } else {
                status = "REJECTED";
                result = "REJECTED: collegial quorum " + quorum + " not met (" + approvals + ")";
            }
        }
        d.setProperty("decisionStatus", status);
        d.setProperty("result", result);
        d.setProperty("decidedAt", ISO.format(now));
        save(d);

        String eventType = "APPROVED".equals(status) ? "DECISION_APPROVED" : "DECISION_REJECTED";
        events.append(caseId, eventType, actor, "", "", result,
                "\"decisionId\":\"" + CaseEventWriter.esc(decisionId) + "\""
                        + ",\"actionType\":\"" + CaseEventWriter.esc(actionType) + "\"");
        return result;
    }

    /** Closure gate (ClosePhase): an APPROVED decision exists for the case. */
    public boolean hasApprovedDecision(String caseId) {
        dao.updateSchema(F_DECISION, F_DECISION, new FormRowSet());
        Long n = dao.count(F_DECISION, F_DECISION,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.decisionStatus = ?2",
                new Object[]{caseId, "APPROVED"});
        return n != null && n > 0;
    }

    private String caseId(FormRow d) {
        String c = DeadlineService.prop(d, "caseId");
        return c.isEmpty() ? DeadlineService.prop(d, "caseRef") : c;
    }

    private void save(FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(F_DECISION, F_DECISION, set);
    }

    static double parseDouble(String s, double dflt) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return dflt;
        }
    }
}
