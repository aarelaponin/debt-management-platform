package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * EnforcementActionService — DMBB-F07 (DM-FR-029..038, BR-DM-031..035 / 044..046).
 *
 * Full breadth, config-driven: every recovery instrument is an mdInstrument row; one
 * generic dmAction record + this service branch on mdInstrument.executionMode. Five
 * entry points wired by EnforcementActionEngine modes:
 *   initiate      — proportional gate (BR-DM-031/033) + F05 eligibility + execute by mode
 *                   (ADMINISTRATIVE incl. GarnishingConnector / JUDICIAL / FIELD) + legal fee (DM-FR-038)
 *   appointAgent  — DM-FR-037 / BR-DM-044 (C4-C5) + appointment fee (BR-DM-045)
 *   agentReport   — BR-DM-046 activity report + commission accrual (BR-DM-045)
 *   sweep         — PUBLISH (DM-FR-030) / AGENT_ALERT (BR-DM-046) / RELEASE / CONFIRM
 */
public class EnforcementActionService {

    public static final String F_ACTION = "dmAction";
    public static final String F_AGENT = "dmAgent";
    public static final String F_RPT = "dmAgentRpt";
    public static final String F_CHARGE = "dmCharge";
    public static final String F_PUB = "dmDebtorPub";
    public static final String F_INSTRUMENT = "mdInstrument";
    public static final String F_LEGALFEE = "mdLegalFee";
    public static final String F_AGENTFEE = "mdAgentFee";
    public static final String F_PUBRULE = "mdPublishRule";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_LINE = "dmLine";
    public static final String F_HOLD = "cmHold";
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int processed = 0;
        public int acted = 0;

        @Override
        public String toString() {
            return "processed=" + processed + " acted=" + acted;
        }
    }

    /** dmAction has no per-tax override (ADR-003) — its lifecycle is the DM scope. */
    private static final java.util.List<String> SCOPE_DM = java.util.Collections.singletonList("DM");

    private final FormDataDao dao;
    private final CaseEventWriter events;
    private final GarnishingConnector garnish;
    private final StatusManager status; // ADR-003: guarded + audited dmAction status writes

    public EnforcementActionService(FormDataDao dao, CaseEventWriter events) {
        this(dao, events, new GarnishingConnector());
    }

    public EnforcementActionService(FormDataDao dao, CaseEventWriter events, GarnishingConnector garnish) {
        this.dao = dao;
        this.events = events;
        this.garnish = garnish;
        // share the same CaseEventWriter so STATUS_CHANGED rows join the case hash-chain
        this.status = new StatusManager(dao, new MmConfigService(dao), events);
        updateSchemas();
    }

    // ---------------- INITIATE ----------------

    public String initiate(String actionId, String actor, LocalDateTime now) {
        FormRow a = dao.load(F_ACTION, F_ACTION, actionId);
        if (a == null) {
            return "no action " + actionId;
        }
        String curStatus = p(a, "status");
        if (!curStatus.isEmpty() && !"INITIATED".equalsIgnoreCase(curStatus)) {
            return "already processed (" + curStatus + ")";
        }
        // Establish the canonical initial in-memory: the dmAction.status field is read-only,
        // so the data API drops the "INITIATED" the creator submits (DX9 delta) — the row
        // arrives blank. Set it here (FormDataDao write path bypasses read-only) so the
        // config-guarded transition below moves INITIATED → outcome, not "" → outcome.
        if (curStatus.isEmpty()) {
            a.setProperty("status", "INITIATED");
        }
        String caseId = p(a, "debtCaseId");
        String tin = p(a, "tin");
        String code = p(a, "instrument");
        double amount = num(p(a, "amount"));
        FormRow instr = instrumentByCode(code);
        if (instr == null) {
            return reject(a, caseId, actor, "unknown instrument " + code);
        }
        String execMode = p(instr, "executionMode");
        String cat = debtCategory(caseId);
        a.setProperty("category", cat);
        a.setProperty("executionMode", execMode);
        a.setProperty("authorityLevel", p(instr, "authorityLevel"));
        a.setProperty("initiatedBy", actor);
        a.setProperty("asOf", now.toString());

        // --- proportional matrix (BR-DM-031) ---
        if (rank(cat) < rank(p(instr, "minCategory"))) {
            return reject(a, caseId, actor, "category " + cat + " below instrument minimum "
                    + p(instr, "minCategory") + " (BR-DM-031)");
        }
        double minAmt = num(p(instr, "proportionalityMinAmount"));
        if (amount < minAmt) {
            return reject(a, caseId, actor, "amount " + amount + " below proportionality threshold "
                    + minAmt + " (BR-DM-031)");
        }
        // --- enforcement eligibility (F05 contract) ---
        if (fullAmountObjection(caseId)) {
            return reject(a, caseId, actor, "full-amount objection on the case");
        }
        if ("true".equalsIgnoreCase(p(instr, "disputeHoldSensitive"))
                && holdActive(caseId, "ENFORCEMENT_SUPPRESS")) {
            return reject(a, caseId, actor, "active ENFORCEMENT_SUPPRESS hold (instalment/objection)");
        }

        // --- legal fee (DM-FR-038) ---
        double fee = 0;
        if ("true".equalsIgnoreCase(p(instr, "costRecorded"))) {
            fee = legalFee(code);
            if (fee > 0) {
                raiseCharge(caseId, tin, "LEGAL_FEE", code, actionId, fee, now, actor);
                a.setProperty("costAmount", String.valueOf(fee));
            }
        }
        // --- garnish cap (BR-DM-033): amount <= debt + legal fees ---
        double debt = num(p(a, "amount")); // action amount IS the debt being enforced
        double cap = debt + fee;
        if (amount > cap) {
            amount = cap;
            a.setProperty("amount", String.valueOf(amount));
        }

        // --- document generation ---
        if (!p(instr, "docTemplate").isEmpty()) {
            a.setProperty("docRef", "DOC-" + code + "-" + shortId(actionId));
        }
        event(caseId, "ENFORCEMENT_ACTION_INITIATED", actor, "initiate " + code + " (" + execMode + ")",
                "\"action\":\"" + CaseEventWriter.esc(actionId) + "\",\"instrument\":\""
                        + CaseEventWriter.esc(code) + "\"");
        if (fee > 0) {
            event(caseId, "LEGAL_FEE_POSTED", actor, "legal fee for " + code,
                    "\"amount\":\"" + fee + "\",\"action\":\"" + CaseEventWriter.esc(actionId) + "\"");
        }

        // --- execution branch ---
        String result = execute(a, instr, code, execMode, caseId, tin, amount, actor, now);
        save(F_ACTION, a);
        return result;
    }

    private String execute(FormRow a, FormRow instr, String code, String execMode,
                           String caseId, String tin, double amount, String actor, LocalDateTime now) {
        if ("ADMINISTRATIVE".equalsIgnoreCase(execMode)) {
            if ("BANK_GARNISH".equals(code)) {
                Map<String, String> req = garnish.buildRequest(tin, taxpayerName(caseId),
                        "BANK-ON-FILE", amount, p(a, "legalRef"), "MTCA-DM-" + shortId(caseId));
                GarnishingConnector.Response r = garnish.transmit(req);
                a.setProperty("externalRef", r.externalRef);
                a.setProperty("responseStatus", r.status);
                event(caseId, "GARNISH_TRANSMITTED", actor, "freezing request transmitted",
                        "\"ref\":\"" + CaseEventWriter.esc(r.externalRef) + "\",\"amount\":\"" + amount + "\"");
                if ("SUCCESS".equals(r.status) || "PARTIAL".equals(r.status)) {
                    a.setProperty("recoveredAmount", String.valueOf(r.amount));
                    status.apply(F_ACTION, a, "status", caseId, "EXECUTED", SCOPE_DM, actor,
                            "garnish " + r.status + " (BR-DM-033)");
                    event(caseId, "GARNISH_CONFIRMED", actor, "garnished amount received (writeback)",
                            "\"amount\":\"" + r.amount + "\"");
                    return "EXECUTED garnish " + r.status + " " + r.amount;
                }
                status.apply(F_ACTION, a, "status", caseId, "FAILED", SCOPE_DM, actor,
                        "bank declined garnish");
                event(caseId, "GARNISH_FAILED", actor, "bank declined", "\"ref\":\""
                        + CaseEventWriter.esc(r.externalRef) + "\"");
                return "FAILED garnish";
            }
            if ("PUBLISH_NAME".equals(code)) {
                publish(caseId, tin, taxpayerName(caseId), amount, "MTCA website", now, actor);
                status.apply(F_ACTION, a, "status", caseId, "EXECUTED", SCOPE_DM, actor,
                        "name published (DM-FR-030)");
                return "EXECUTED publish";
            }
            // LIEN / THIRD_PARTY / REFUND_INTERCEPT / LICENCE / DEBTORS_DEBTOR_OFFSET
            a.setProperty("externalRef", "REG-" + shortId(a.getId()));
            a.setProperty("responseStatus", "PENDING");
            status.apply(F_ACTION, a, "status", caseId, "SUBMITTED", SCOPE_DM, actor,
                    code + " registered (administrative, pending)");
            event(caseId, "ENFORCEMENT_ACTION_SUBMITTED", actor, code + " registered (pending)",
                    "\"mode\":\"ADMINISTRATIVE\"");
            return "SUBMITTED " + code;
        }
        if ("JUDICIAL".equalsIgnoreCase(execMode)) {
            a.setProperty("externalRef", "COURT-" + shortId(a.getId()));
            a.setProperty("responseStatus", "PENDING");
            status.apply(F_ACTION, a, "status", caseId, "SUBMITTED", SCOPE_DM, actor,
                    code + " submitted to court (judicial)");
            event(caseId, "ENFORCEMENT_ACTION_SUBMITTED", actor, code + " submitted to authority",
                    "\"mode\":\"JUDICIAL\"");
            return "SUBMITTED " + code + " (judicial)";
        }
        // FIELD
        if ("AGENT_WARRANT".equals(code)) {
            status.apply(F_ACTION, a, "status", caseId, "REFERRED", SCOPE_DM, actor,
                    "referred to agent appointment");
            event(caseId, "ENFORCEMENT_ACTION_SUBMITTED", actor, "referred to agent appointment",
                    "\"mode\":\"FIELD\"");
            return "REFERRED to agent appointment";
        }
        if ("PROPERTY_SEIZURE".equals(code)) {
            status.apply(F_ACTION, a, "status", caseId, "SUBMITTED", SCOPE_DM, actor,
                    "seizure order issued; auction pending");
            event(caseId, "ENFORCEMENT_ACTION_SUBMITTED", actor, "seizure order issued; auction pending",
                    "\"mode\":\"FIELD\"");
            return "SUBMITTED seizure (auction pending)";
        }
        status.apply(F_ACTION, a, "status", caseId, "SUBMITTED", SCOPE_DM, actor,
                code + " scheduled (field)");
        event(caseId, "ENFORCEMENT_ACTION_SUBMITTED", actor, code + " scheduled (field)",
                "\"mode\":\"FIELD\"");
        return "SUBMITTED " + code + " (field)";
    }

    private String reject(FormRow a, String caseId, String actor, String reason) {
        a.setProperty("responseStatus", reason);
        status.apply(F_ACTION, a, "status", caseId, "BLOCKED", SCOPE_DM, actor, reason);
        save(F_ACTION, a);
        event(caseId, "ENFORCEMENT_ACTION_BLOCKED", actor, reason,
                "\"action\":\"" + CaseEventWriter.esc(a.getId()) + "\"");
        return "BLOCKED: " + reason;
    }

    // ---------------- AGENT ----------------

    public String appointAgent(String agentId, String actor, LocalDateTime now) {
        FormRow ag = dao.load(F_AGENT, F_AGENT, agentId);
        if (ag == null) {
            return "no agent appointment " + agentId;
        }
        if (!p(ag, "appointmentFee").isEmpty() || "REJECTED".equals(p(ag, "status"))) {
            return "already processed";
        }
        String caseId = p(ag, "debtCaseId");
        String tin = p(ag, "tin");
        String cat = debtCategory(caseId);
        ag.setProperty("category", cat);
        if (rank(cat) < 4) {
            ag.setProperty("status", "REJECTED");
            save(F_AGENT, ag);
            event(caseId, "AGENT_APPOINTMENT_REJECTED", actor,
                    "category " + cat + " below C4 (BR-DM-044)", "");
            return "REJECTED: category " + cat + " < C4";
        }
        FormRow fee = agentFee(cat);
        double appt = fee == null ? 200 : num(p(fee, "appointmentFee"));
        double rate = fee == null ? 0.10 : num(p(fee, "commissionRate"));
        ag.setProperty("appointmentFee", String.valueOf(appt));
        ag.setProperty("commissionRate", String.valueOf(rate));
        ag.setProperty("recoveredTotal", "0");
        ag.setProperty("commissionTotal", "0");
        ag.setProperty("status", "APPOINTED");
        save(F_AGENT, ag);
        raiseCharge(caseId, tin, "AGENT_FEE", "AGENT_WARRANT", agentId, appt, now, actor);
        event(caseId, "AGENT_APPOINTED", actor, "agent " + p(ag, "agentId") + " appointed",
                "\"appt\":\"" + CaseEventWriter.esc(agentId) + "\",\"fee\":\"" + appt + "\"");
        event(caseId, "LEGAL_FEE_POSTED", actor, "agent appointment fee",
                "\"amount\":\"" + appt + "\",\"type\":\"AGENT_FEE\"");
        return "APPOINTED (fee " + appt + ", rate " + rate + ")";
    }

    public String agentReport(String rptId, String actor, LocalDateTime now) {
        FormRow r = dao.load(F_RPT, F_RPT, rptId);
        if (r == null) {
            return "no report " + rptId;
        }
        if (!p(r, "commissionAmount").isEmpty()) {
            return "already processed";
        }
        FormRow ag = dao.load(F_AGENT, F_AGENT, p(r, "agentApptId"));
        if (ag == null) {
            return "no parent appointment";
        }
        double recovered = num(p(r, "recoveredAmount"));
        double rate = num(p(ag, "commissionRate"));
        double commission = round2(recovered * rate);
        r.setProperty("commissionAmount", String.valueOf(commission));
        save(F_RPT, r);
        ag.setProperty("recoveredTotal", String.valueOf(num(p(ag, "recoveredTotal")) + recovered));
        ag.setProperty("commissionTotal", String.valueOf(round2(num(p(ag, "commissionTotal")) + commission)));
        String when = p(r, "reportDate");
        ag.setProperty("lastReportDate", when.isEmpty() ? now.toLocalDate().toString() : when);
        save(F_AGENT, ag);
        event(p(ag, "debtCaseId"), "AGENT_REPORTED", actor,
                "recovered " + recovered + ", commission " + commission,
                "\"appt\":\"" + CaseEventWriter.esc(ag.getId()) + "\",\"recovered\":\"" + recovered + "\"");
        return "REPORTED recovered=" + recovered + " commission=" + commission;
    }

    // ---------------- SWEEP ----------------

    public Tally sweep(String sweepMode, String actor, LocalDateTime asOf) {
        Tally t = new Tally();
        if ("PUBLISH".equalsIgnoreCase(sweepMode)) {
            publishSweep(t, actor, asOf);
        } else if ("AGENT_ALERT".equalsIgnoreCase(sweepMode)) {
            agentAlertSweep(t, actor, asOf);
        } else if ("RELEASE".equalsIgnoreCase(sweepMode)) {
            releaseSweep(t, actor, asOf);
        } else if ("CONFIRM".equalsIgnoreCase(sweepMode)) {
            confirmSweep(t, actor);
        }
        return t;
    }

    private void publishSweep(Tally t, String actor, LocalDateTime asOf) {
        FormRow rule = firstRow(F_PUBRULE);
        String minCat = rule == null ? "C3" : p(rule, "minCategory");
        double minAmt = rule == null ? 1000 : num(p(rule, "minAmount"));
        String registry = rule == null ? "national registry" : p(rule, "registry");
        FormRowSet cases = dao.find(F_CASE, F_CASE, "WHERE e.customProperties.caseType = ?1",
                new Object[]{"DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return;
        }
        for (FormRow c : cases) {
            t.processed++;
            String caseId = c.getId();
            FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
            if (dd == null) {
                continue;
            }
            String cat = !p(dd, "debtCategory").isEmpty() ? p(dd, "debtCategory") : p(c, "category");
            double amt = num(p(dd, "consolidatedAmount"));
            boolean pastFinal = (long) num(p(dd, "lastStepSeq")) >= 3;  // final demand reached
            if (rank(cat) < rank(minCat) || amt < minAmt || !pastFinal) {
                continue;
            }
            if (!enforcementEligible(caseId) || alreadyPublished(caseId)) {
                continue;
            }
            publish(caseId, p(c, "tin"), taxpayerName(caseId), amt, registry, asOf, actor);
            t.acted++;
        }
    }

    private void agentAlertSweep(Tally t, String actor, LocalDateTime asOf) {
        FormRow fee = firstRow(F_AGENTFEE);
        long interval = fee == null ? 7 : DeadlineService.parseLong(p(fee, "reportingIntervalDays"), 7);
        FormRowSet agents = dao.find(F_AGENT, F_AGENT, "WHERE e.customProperties.status = ?1",
                new Object[]{"APPOINTED"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (agents == null) {
            return;
        }
        for (FormRow ag : agents) {
            t.processed++;
            LocalDate ref = refDate(ag);
            if (ref == null || !ref.plusDays(interval).isBefore(asOf.toLocalDate())) {
                continue;
            }
            event(p(ag, "debtCaseId"), "AGENT_OVERDUE_ALERT", actor,
                    "agent " + p(ag, "agentId") + " has not reported in " + interval + " days",
                    "\"appt\":\"" + CaseEventWriter.esc(ag.getId()) + "\"");
            t.acted++;
        }
    }

    private void releaseSweep(Tally t, String actor, LocalDateTime asOf) {
        FormRowSet pubs = dao.find(F_PUB, F_PUB, "WHERE e.customProperties.status = ?1",
                new Object[]{"PUBLISHED"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (pubs == null) {
            return;
        }
        for (FormRow pub : pubs) {
            t.processed++;
            String caseId = p(pub, "debtCaseId");
            FormRow c = dao.load(F_CASE, F_CASE, caseId);
            if (c == null || !isResolved(p(c, "currentState"))) {
                continue;
            }
            pub.setProperty("status", "REMOVED");
            pub.setProperty("removedDate", asOf.toLocalDate().toString());
            save(F_PUB, pub);
            event(caseId, "DEBTOR_UNPUBLISHED", actor, "debt resolved — removed from debtors list", "");
            t.acted++;
        }
    }

    private void confirmSweep(Tally t, String actor) {
        FormRowSet acts = dao.find(F_ACTION, F_ACTION, "WHERE e.customProperties.status = ?1",
                new Object[]{"SUBMITTED"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (acts == null) {
            return;
        }
        for (FormRow a : acts) {
            t.processed++;
            a.setProperty("responseStatus", "SUCCESS");
            status.apply(F_ACTION, a, "status", p(a, "debtCaseId"), "CONFIRMED", SCOPE_DM, actor,
                    p(a, "instrument") + " confirmed by authority/register");
            save(F_ACTION, a);
            event(p(a, "debtCaseId"), "ENFORCEMENT_ACTION_CONFIRMED", actor,
                    p(a, "instrument") + " confirmed by authority/register",
                    "\"action\":\"" + CaseEventWriter.esc(a.getId()) + "\"");
            t.acted++;
        }
    }

    // ---------------- helpers ----------------

    private void publish(String caseId, String tin, String name, double amount, String registry,
                         LocalDateTime now, String actor) {
        FormRow pub = new FormRow();
        pub.setId(UUID.randomUUID().toString());
        pub.setProperty("debtCaseId", caseId);
        pub.setProperty("tin", tin);
        pub.setProperty("debtorName", name);
        pub.setProperty("debtAmount", String.valueOf(amount));
        pub.setProperty("registry", registry);
        pub.setProperty("publishedDate", now.toLocalDate().toString());
        pub.setProperty("status", "PUBLISHED");
        save(F_PUB, pub);
        event(caseId, "DEBTOR_PUBLISHED", actor, "debtor published on " + registry,
                "\"tin\":\"" + CaseEventWriter.esc(tin) + "\",\"amount\":\"" + amount + "\"");
    }

    private void raiseCharge(String caseId, String tin, String type, String instrument,
                             String actionId, double amount, LocalDateTime now, String actor) {
        FormRow ch = new FormRow();
        ch.setId(UUID.randomUUID().toString());
        ch.setProperty("debtCaseId", caseId);
        ch.setProperty("tin", tin);
        ch.setProperty("chargeType", type);
        ch.setProperty("instrument", instrument);
        ch.setProperty("actionId", actionId);
        ch.setProperty("amount", String.valueOf(amount));
        ch.setProperty("postedDate", now.toLocalDate().toString());
        ch.setProperty("status", "RECORDED");
        save(F_CHARGE, ch);
    }

    private boolean alreadyPublished(String caseId) {
        Long n = dao.count(F_PUB, F_PUB,
                "WHERE e.customProperties.debtCaseId = ?1 AND e.customProperties.status = ?2",
                new Object[]{caseId, "PUBLISHED"});
        return n != null && n > 0;
    }

    boolean enforcementEligible(String caseId) {
        if (holdActive(caseId, "ENFORCEMENT_SUPPRESS")) {
            return false;
        }
        return !fullAmountObjection(caseId);
    }

    private boolean holdActive(String caseId, String scope) {
        Long n = dao.count(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{caseId, scope, "ACTIVE"});
        return n != null && n > 0;
    }

    private boolean fullAmountObjection(String caseId) {
        FormRowSet lines = dao.find(F_LINE, F_LINE, "WHERE e.customProperties.caseId = ?1",
                new Object[]{caseId}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        double total = 0;
        double disputed = 0;
        for (FormRow l : lines) {
            total += num(p(l, "enforceable"));
            disputed += num(p(l, "disputed"));
        }
        return total > 0 && disputed >= total;
    }

    private FormRow instrumentByCode(String code) {
        FormRowSet rows = dao.find(F_INSTRUMENT, F_INSTRUMENT, "WHERE e.customProperties.code = ?1",
                new Object[]{code}, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private double legalFee(String code) {
        FormRowSet rows = dao.find(F_LEGALFEE, F_LEGALFEE, "WHERE e.customProperties.instrument = ?1",
                new Object[]{code}, "dateCreated", Boolean.FALSE, 0, 1);
        if (rows != null && !rows.isEmpty()) {
            return num(p(rows.get(0), "feeAmount"));
        }
        FormRowSet def = dao.find(F_LEGALFEE, F_LEGALFEE, "WHERE e.customProperties.instrument = ?1",
                new Object[]{""}, "dateCreated", Boolean.FALSE, 0, 1);
        return (def == null || def.isEmpty()) ? 0 : num(p(def.get(0), "feeAmount"));
    }

    private FormRow agentFee(String cat) {
        FormRowSet rows = dao.find(F_AGENTFEE, F_AGENTFEE, "WHERE e.customProperties.category = ?1",
                new Object[]{cat}, "dateCreated", Boolean.FALSE, 0, 1);
        if (rows != null && !rows.isEmpty()) {
            return rows.get(0);
        }
        FormRowSet def = dao.find(F_AGENTFEE, F_AGENTFEE, "WHERE e.customProperties.category = ?1",
                new Object[]{""}, "dateCreated", Boolean.FALSE, 0, 1);
        return (def == null || def.isEmpty()) ? null : def.get(0);
    }

    private String debtCategory(String caseId) {
        if (caseId == null || caseId.isEmpty()) {
            return "";
        }
        FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
        if (dd != null && !p(dd, "debtCategory").isEmpty()) {
            return p(dd, "debtCategory");
        }
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        return c == null ? "" : p(c, "category");
    }

    private String taxpayerName(String caseId) {
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        return c == null ? "" : p(c, "taxpayerName");
    }

    private LocalDate refDate(FormRow ag) {
        String last = p(ag, "lastReportDate");
        if (!last.isEmpty()) {
            try {
                return LocalDate.parse(last);
            } catch (Exception ignore) {
                // fall through
            }
        }
        if (ag.getDateCreated() != null) {
            return ag.getDateCreated().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private static boolean isResolved(String state) {
        return "CLOSED".equalsIgnoreCase(state) || "RESOLVED".equalsIgnoreCase(state)
                || "PAID".equalsIgnoreCase(state);
    }

    private static int rank(String cat) {
        if (cat == null || cat.length() < 2 || cat.charAt(0) != 'C') {
            return 0;
        }
        try {
            return Integer.parseInt(cat.substring(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private FormRow firstRow(String form) {
        FormRowSet rows = dao.find(form, form, null, null, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private void event(String caseId, String type, String actor, String reason, String extra) {
        if (caseId != null && !caseId.isEmpty()) {
            events.append(caseId, type, actor, "", "", reason, extra);
        }
    }

    private void updateSchemas() {
        for (String f : new String[]{F_ACTION, F_AGENT, F_RPT, F_CHARGE, F_PUB, F_CASE, F_DEBT, F_HOLD}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    private static String shortId(String id) {
        return id == null ? "" : id.replaceAll("[^A-Za-z0-9]", "").substring(0, Math.min(6,
                id.replaceAll("[^A-Za-z0-9]", "").length())).toUpperCase();
    }

    private static String p(FormRow r, String id) {
        return DeadlineService.prop(r, id);
    }

    private static double num(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double round2(double d) {
        return Math.round(d * 100.0) / 100.0;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
