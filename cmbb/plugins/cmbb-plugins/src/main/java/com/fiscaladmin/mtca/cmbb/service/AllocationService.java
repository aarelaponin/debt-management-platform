package com.fiscaladmin.mtca.cmbb.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Core allocation logic (CMBB-F03, WF-FR-007/008, BR-WF-005/006/007),
 * constructor-injected for unit testing. Policy comes from mmAlloc rows,
 * officer attributes from mdOfficerProfile, exclusions from mmCoi — all
 * configuration (P4). Assignment failure is routed state, never an exception.
 */
public class AllocationService {

    public static final String F_CASE = "cmCase";
    public static final String F_REASSIGN = "cmReassign";
    public static final String F_DECISION = "cmDecision";

    private final FormDataDao dao;
    private final MmConfigService mm;
    private final CaseEventWriter events;

    public AllocationService(FormDataDao dao, MmConfigService mm, CaseEventWriter events) {
        this.dao = dao;
        this.mm = mm;
        this.events = events;
    }

    /** ASSIGN result: chosen officer username, or null when routed UNASSIGNED. */
    public String assign(String caseId, String actor) {
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c == null) {
            throw new IllegalStateException("cmCase row not found: " + caseId);
        }
        // F08: EXCLUDE_DECISION_MAKER reads cmDecision — ensure the relation
        // exists before any read (a missing table poisons the JTA tx; DX9-DELTAS).
        dao.updateSchema(F_DECISION, F_DECISION, new FormRowSet());
        String typeCode = prop(c, "caseType");
        String existing = prop(c, "assignee");
        if (!existing.isEmpty()) { // manual pre-assignment respected, still logged
            c.setProperty("assignmentStatus", "ASSIGNED");
            save(F_CASE, c);
            events.append(caseId, "CASE_ASSIGNED", actor, "", "",
                    "pre-assigned to " + existing,
                    "\"officer\":\"" + CaseEventWriter.esc(existing) + "\",\"policy\":\"MANUAL\"");
            return existing;
        }
        FormRow policy = mm.allocPolicy(typeCode);
        String criteria = policy == null ? "" : nz(policy.getProperty("criteria"));
        List<String> filters = jsonList(criteria, "filters");
        int warnPct = jsonInt(criteria, "warnPct", 90);

        Set<String> terminals = mm.allTerminalCodes();
        List<FormRow> candidates = new ArrayList<FormRow>();
        FormRowSet officers = mm.activeOfficers();
        StringBuilder considered = new StringBuilder();
        if (officers != null) {
            for (FormRow o : officers) {
                considered.append(considered.length() > 0 ? "," : "").append(prop(o, "code"));
                if (filters.contains("SPECIALISATION") && !specialisationOk(c, o)) {
                    continue;
                }
                if (filters.contains("WORKLOAD")
                        && openCases(prop(o, "code"), terminals) >= capacity(o)) {
                    continue;
                }
                if (coiExcluded(typeCode, o, c)) {
                    continue;
                }
                candidates.add(o);
            }
        }
        if (candidates.isEmpty()) { // WF-FR-007: supervisor queue + alert, no halt
            c.setProperty("assignmentStatus", "UNASSIGNED");
            save(F_CASE, c);
            events.append(caseId, "ASSIGNMENT_FAILED", actor, "", "",
                    "no matching officer (considered: " + considered + ")", null);
            events.append(caseId, "NOTIF_PENDING", actor, "", "",
                    "supervisor alert: unassigned case",
                    "\"alertType\":\"SUPERVISOR_UNASSIGNED\"");
            return null;
        }
        // deterministic round-robin: global CASE_ASSIGNED count modulo candidates
        long cursor = count("cmEvent", "WHERE e.customProperties.eventType = ?1",
                new Object[]{"CASE_ASSIGNED"});
        FormRow chosen = candidates.get((int) (cursor % candidates.size()));
        String officer = prop(chosen, "code");

        c.setProperty("assignee", officer);
        c.setProperty("assignmentStatus", "ASSIGNED");
        save(F_CASE, c);
        events.append(caseId, "CASE_ASSIGNED", actor, "", "",
                "assigned by policy", "\"officer\":\"" + CaseEventWriter.esc(officer)
                        + "\",\"policy\":\"" + (policy == null ? "DEFAULT" : prop(policy, "code"))
                        + "\",\"candidatesConsidered\":" + candidates.size());

        long open = openCases(officer, terminals);
        long cap = capacity(chosen);
        if (cap > 0 && open * 100 >= cap * warnPct) { // BR-WF-005 90% alert
            events.append(caseId, "CAPACITY_WARNING", actor, "", "",
                    "officer " + officer + " at " + open + "/" + cap,
                    "\"officer\":\"" + CaseEventWriter.esc(officer) + "\",\"openCases\":" + open
                            + ",\"maxCapacity\":" + cap);
        }
        return officer;
    }

    /** REASSIGN per cmReassign order row; returns result message written back. */
    public String reassign(String orderId, String actor) {
        FormRow order = dao.load(F_REASSIGN, F_REASSIGN, orderId);
        if (order == null) {
            return "order not found: " + orderId;
        }
        String reason = prop(order, "reason");
        String result;
        if (reason.isEmpty()) { // BR-WF-006 defence in depth (validator is first line)
            result = "REJECTED: reason mandatory";
        } else {
            result = doReassign(order, reason, actor);
        }
        order.setProperty("result", result);
        save(F_REASSIGN, order);
        return result;
    }

    private String doReassign(FormRow order, String reason, String actor) {
        String caseRef = prop(order, "caseRef");
        List<FormRow> scope = new ArrayList<FormRow>();
        Set<String> terminals = mm.allTerminalCodes();
        if (!caseRef.isEmpty()) {
            FormRowSet rows = dao.find(F_CASE, F_CASE,
                    "WHERE e.customProperties.caseRef = ?1", new Object[]{caseRef},
                    "dateCreated", Boolean.FALSE, 0, 1);
            if (rows != null) {
                scope.addAll(rows);
            }
        } else { // bulk: officer [+ caseType] [+ category] (WF-FR-008)
            String cond = "WHERE e.customProperties.assignee = ?1";
            List<Object> params = new ArrayList<Object>();
            params.add(prop(order, "filterOfficer"));
            if (!prop(order, "filterCaseType").isEmpty()) {
                cond += " AND e.customProperties.caseType = ?" + (params.size() + 1);
                params.add(prop(order, "filterCaseType"));
            }
            if (!prop(order, "filterCategory").isEmpty()) {
                cond += " AND e.customProperties.category = ?" + (params.size() + 1);
                params.add(prop(order, "filterCategory"));
            }
            FormRowSet rows = dao.find(F_CASE, F_CASE, cond, params.toArray(),
                    "dateCreated", Boolean.FALSE, null, null);
            if (rows != null) {
                for (FormRow r : rows) {
                    if (!terminals.contains(prop(r, "currentState"))) {
                        scope.add(r);
                    }
                }
            }
        }
        if (scope.isEmpty()) {
            return "0 case(s) matched";
        }
        String target = prop(order, "toOfficer");
        List<FormRow> targets = new ArrayList<FormRow>();
        if (target.isEmpty()) { // BR-WF-007 redistribution
            FormRowSet officers = mm.activeOfficers();
            String from = prop(order, "filterOfficer");
            if (officers != null) {
                for (FormRow o : officers) {
                    if (!prop(o, "code").equals(from)
                            && openCases(prop(o, "code"), terminals) < capacity(o)) {
                        targets.add(o);
                    }
                }
            }
            if (targets.isEmpty()) {
                return "REJECTED: no eligible target officers (all at capacity)";
            }
        }
        int n = 0;
        for (FormRow c : scope) {
            String from = prop(c, "assignee");
            String to = target.isEmpty()
                    ? prop(targets.get(n % targets.size()), "code") : target;
            c.setProperty("assignee", to);
            c.setProperty("assignmentStatus", "ASSIGNED");
            save(F_CASE, c);
            String link = "\"from\":\"" + CaseEventWriter.esc(from) + "\",\"to\":\""
                    + CaseEventWriter.esc(to) + "\",\"orderId\":\""
                    + CaseEventWriter.esc(order.getId()) + "\",\"caseRef\":\""
                    + CaseEventWriter.esc(prop(c, "caseRef")) + "\"";
            events.append(c.getId(), "CASE_REASSIGNED", actor, "", "", reason, link);
            // WF-FR-008: notify both parties — dispatched by F06
            events.append(c.getId(), "NOTIF_PENDING", actor, "", "",
                    "reassignment notice (original assignee)",
                    "\"recipient\":\"" + CaseEventWriter.esc(from) + "\"," + link);
            events.append(c.getId(), "NOTIF_PENDING", actor, "", "",
                    "reassignment notice (new assignee)",
                    "\"recipient\":\"" + CaseEventWriter.esc(to) + "\"," + link);
            n++;
        }
        return n + " case(s) reassigned";
    }

    // ---------- helpers ----------

    long openCases(String officer, Set<String> terminals) {
        FormRowSet rows = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.assignee = ?1", new Object[]{officer},
                "dateCreated", Boolean.FALSE, null, null);
        long n = 0;
        if (rows != null) {
            for (FormRow r : rows) {
                if (!terminals.contains(prop(r, "currentState"))) {
                    n++;
                }
            }
        }
        return n;
    }

    private boolean specialisationOk(FormRow c, FormRow officer) {
        String taxType = prop(c, "taxType");
        String skills = prop(officer, "taxTypes");
        if (taxType.isEmpty() || skills.isEmpty()) {
            return true; // blank-tolerant per FIS
        }
        return Arrays.asList(skills.split("[,;]")).contains(taxType);
    }

    private boolean coiExcluded(String typeCode, FormRow officer, FormRow caseRow) {
        FormRowSet rules = mm.coiRules(typeCode);
        if (rules == null) {
            return false;
        }
        for (FormRow r : rules) {
            String ruleType = prop(r, "ruleType");
            String expr = prop(r, "expression");
            List<String> tokens = Arrays.asList(expr.split("[,;]"));
            if ("EXCLUDE_UNIT".equals(ruleType) && tokens.contains(prop(officer, "unit"))) {
                return true;
            }
            if ("MATRIX".equals(ruleType) && (tokens.contains(prop(officer, "code"))
                    || tokens.contains(prop(officer, "unit")))) {
                return true;
            }
            // F08: independence from the original decision-maker (GCMF §3.3-3) —
            // exclude an officer who made an APPROVED decision on this taxpayer's
            // case(s). Decisions are keyed by TIN (the case spine anchor).
            if ("EXCLUDE_DECISION_MAKER".equals(ruleType) && decidedFor(caseRow, officer)) {
                return true;
            }
        }
        return false;
    }

    /** True when the officer has an APPROVED cmDecision for this case's TIN. */
    private boolean decidedFor(FormRow caseRow, FormRow officer) {
        String tin = prop(caseRow, "tin");
        String code = prop(officer, "code");
        if (tin.isEmpty() || code.isEmpty()) {
            return false;
        }
        Long n = dao.count(F_DECISION, F_DECISION,
                "WHERE e.customProperties.tin = ?1 AND e.customProperties.decidedBy = ?2"
                        + " AND e.customProperties.decisionStatus = ?3",
                new Object[]{tin, code, "APPROVED"});
        return n != null && n > 0;
    }

    private long capacity(FormRow officer) {
        try {
            return Long.parseLong(prop(officer, "maxCapacity"));
        } catch (NumberFormatException e) {
            return 20; // FIS §4 default
        }
    }

    private long count(String form, String cond, Object[] params) {
        Long n = dao.count(form, form, cond, params);
        return n == null ? 0 : n;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    static String prop(FormRow r, String id) {
        String v = r.getProperty(id);
        return v == null ? "" : v.trim();
    }

    static String nz(String s) {
        return s == null ? "" : s;
    }

    /** Minimal extractors for the controlled criteria JSON (no JSON lib dep). */
    static List<String> jsonList(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(nz(json));
        List<String> out = new ArrayList<String>();
        if (m.find()) {
            for (String item : m.group(1).split(",")) {
                String v = item.trim().replaceAll("^\"|\"$", "");
                if (!v.isEmpty()) {
                    out.add(v);
                }
            }
        } else {
            out.addAll(Arrays.asList("WORKLOAD", "SPECIALISATION")); // FIS default
        }
        return out;
    }

    static int jsonInt(String json, String key, int dflt) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(nz(json));
        return m.find() ? Integer.parseInt(m.group(1)) : dflt;
    }
}
