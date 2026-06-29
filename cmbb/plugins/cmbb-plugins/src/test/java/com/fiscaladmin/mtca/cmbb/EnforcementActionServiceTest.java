package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.EnforcementActionService;

/** EnforcementActionService — DMBB-F07 instrument execution / agents / fees / sweeps (T-16 unit). */
public class EnforcementActionServiceTest {

    private static final Pattern EQ =
            Pattern.compile("customProperties\\.(\\w+)\\s*=\\s*\\?(\\d+)");

    private FormDataDao dao;
    private final Map<String, List<FormRow>> store = new LinkedHashMap<>();

    @Before
    public void setUp() {
        store.clear();
        dao = mock(FormDataDao.class);
        when(dao.load(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(2);
            for (FormRow r : rows(inv.getArgument(0))) {
                if (id != null && id.equals(r.getId())) {
                    return r;
                }
            }
            return null;
        });
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> match(inv.getArgument(0), inv.getArgument(2),
                        inv.getArgument(3), (Integer) inv.getArgument(7)));
        when(dao.count(anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> (long) match(inv.getArgument(0), inv.getArgument(2),
                        inv.getArgument(3), null).size());
        doAnswer(inv -> {
            String form = inv.getArgument(0);
            for (FormRow r : (FormRowSet) inv.getArgument(2)) {
                upsert(form, r);
            }
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
        seedInstruments();
        seedFees();
        seedActionLifecycle();
    }

    /** ADR-003: dmAction DM lifecycle the migrated engine guard reads (mirrors seed CSV). */
    private void seedActionLifecycle() {
        String[][] edges = {
            {"INITIATED", "EXECUTED"}, {"INITIATED", "SUBMITTED"}, {"INITIATED", "BLOCKED"},
            {"INITIATED", "REFERRED"}, {"INITIATED", "FAILED"},
            {"SUBMITTED", "CONFIRMED"}, {"SUBMITTED", "FAILED"},
        };
        int i = 0;
        for (String[] e : edges) {
            put("mmEntityTransition",
                    row("entity", "dmAction", "scope", "DM", "fromStatus", e[0], "toStatus", e[1]),
                    "tr-act-dm-" + (++i));
        }
    }

    private EnforcementActionService svc() {
        return new EnforcementActionService(dao, new CaseEventWriter(dao));
    }

    // ---------------- INITIATE ----------------

    @Test
    public void garnishExecutesPostsFeeAndRecovery() {
        debtCase("c1", "T1", "C3");
        put("dmAction", row("debtCaseId", "c1", "tin", "T1", "instrument", "BANK_GARNISH",
                "amount", "2000", "status", "INITIATED"), "act-1");

        String r = svc().initiate("act-1", "officer", LocalDateTime.now());

        assertTrue(r, r.startsWith("EXECUTED"));
        assertEquals("EXECUTED", prop("dmAction", "act-1", "status"));
        assertEquals("SUCCESS", prop("dmAction", "act-1", "responseStatus"));
        assertEquals("2000.0", prop("dmAction", "act-1", "recoveredAmount"));
        assertEquals("50.0", prop("dmAction", "act-1", "costAmount"));
        assertEquals(1, rows("dmCharge").size());
        assertEquals(1, ev("c1", "GARNISH_CONFIRMED"));
        assertEquals(1, ev("c1", "LEGAL_FEE_POSTED"));
    }

    @Test
    public void proportionalGateRejectsLowCategory() {
        debtCase("c2", "T2", "C3");
        put("dmAction", row("debtCaseId", "c2", "tin", "T2", "instrument", "PROPERTY_SEIZURE",
                "amount", "8000", "status", "INITIATED"), "act-2");

        String r = svc().initiate("act-2", "officer", LocalDateTime.now());

        assertTrue(r, r.startsWith("BLOCKED"));
        assertEquals("BLOCKED", prop("dmAction", "act-2", "status"));
        assertEquals(1, ev("c2", "ENFORCEMENT_ACTION_BLOCKED"));
    }

    @Test
    public void enforcementSuppressHoldBlocksDisputeSensitive() {
        debtCase("c3", "T3", "C4");
        put("cmHold", row("caseId", "c3", "scope", "ENFORCEMENT_SUPPRESS", "status", "ACTIVE"), "h3");
        put("dmAction", row("debtCaseId", "c3", "tin", "T3", "instrument", "BANK_GARNISH",
                "amount", "3000", "status", "INITIATED"), "act-3");

        String r = svc().initiate("act-3", "officer", LocalDateTime.now());

        assertTrue(r, r.contains("ENFORCEMENT_SUPPRESS"));
        assertEquals("BLOCKED", prop("dmAction", "act-3", "status"));
    }

    @Test
    public void judicialBankruptcySubmits() {
        debtCase("c4", "T4", "C4");
        put("dmAction", row("debtCaseId", "c4", "tin", "T4", "instrument", "BANKRUPTCY",
                "amount", "25000", "status", "INITIATED"), "act-4");

        String r = svc().initiate("act-4", "officer", LocalDateTime.now());

        assertTrue(r, r.startsWith("SUBMITTED"));
        assertEquals("SUBMITTED", prop("dmAction", "act-4", "status"));
        assertTrue(prop("dmAction", "act-4", "externalRef").startsWith("COURT-"));
        assertEquals("300.0", prop("dmAction", "act-4", "costAmount"));
    }

    // ---------------- AGENT ----------------

    @Test
    public void agentAppointThenReportAccruesCommission() {
        debtCase("c5", "T5", "C4");
        put("dmAgent", row("debtCaseId", "c5", "tin", "T5", "agentId", "AG-9",
                "status", "APPOINTED"), "ap-5");

        String r = svc().appointAgent("ap-5", "sdo", LocalDateTime.now());
        assertTrue(r, r.startsWith("APPOINTED"));
        assertEquals("200.0", prop("dmAgent", "ap-5", "appointmentFee"));
        assertEquals(1, ev("c5", "AGENT_APPOINTED"));

        put("dmAgentRpt", row("agentApptId", "ap-5", "reportDate", "2026-06-10",
                "recoveredAmount", "1000"), "rp-5");
        String r2 = svc().agentReport("rp-5", "agent", LocalDateTime.now());
        assertTrue(r2, r2.contains("commission=100.0"));
        assertEquals("100.0", prop("dmAgentRpt", "rp-5", "commissionAmount"));
        assertEquals("1000.0", prop("dmAgent", "ap-5", "recoveredTotal"));
        assertEquals("100.0", prop("dmAgent", "ap-5", "commissionTotal"));
        assertEquals(1, ev("c5", "AGENT_REPORTED"));
    }

    @Test
    public void agentRejectedBelowC4() {
        debtCase("c6", "T6", "C3");
        put("dmAgent", row("debtCaseId", "c6", "tin", "T6", "agentId", "AG-1",
                "status", "APPOINTED"), "ap-6");

        String r = svc().appointAgent("ap-6", "sdo", LocalDateTime.now());
        assertTrue(r, r.startsWith("REJECTED"));
        assertEquals("REJECTED", prop("dmAgent", "ap-6", "status"));
    }

    // ---------------- SWEEP ----------------

    @Test
    public void publishSweepPublishesEligibleCase() {
        debtCase("c7", "T7", "C3");
        setDebt("c7", "consolidatedAmount", "2000");
        setDebt("c7", "lastStepSeq", "3");
        put("mdPublishRule", row("minCategory", "C3", "minAmount", "1000",
                "registry", "national registry", "active", "true"), "pub-rule");

        EnforcementActionService.Tally t = svc().sweep("PUBLISH", "sdo", LocalDateTime.now());

        assertTrue(t.acted >= 1);
        assertEquals(1, rows("dmDebtorPub").size());
        assertEquals("PUBLISHED", rows("dmDebtorPub").get(0).getProperty("status"));
        assertEquals(1, ev("c7", "DEBTOR_PUBLISHED"));
    }

    @Test
    public void agentAlertSweepFlagsOverdue() {
        debtCase("c8", "T8", "C4");
        put("dmAgent", row("debtCaseId", "c8", "tin", "T8", "agentId", "AG-7",
                "status", "APPOINTED", "lastReportDate", LocalDate.now().minusDays(30).toString()),
                "ap-8");

        EnforcementActionService.Tally t = svc().sweep("AGENT_ALERT", "sup", LocalDateTime.now());

        assertEquals(1, t.acted);
        assertEquals(1, ev("c8", "AGENT_OVERDUE_ALERT"));
    }

    // ---------------- fixtures ----------------

    private void debtCase(String id, String tin, String cat) {
        put("cmCase", row("caseType", "DM", "tin", tin, "category", cat,
                "taxpayerName", "TP " + tin, "currentState", "OPEN"), id);
        put("dmDebt", row("tin", tin, "debtCategory", cat, "consolidatedAmount", "5000",
                "lastStepSeq", "0"), id);
    }

    private void setDebt(String caseId, String field, String value) {
        prop("dmDebt", caseId, null); // ensure list exists
        for (FormRow r : rows("dmDebt")) {
            if (caseId.equals(r.getId())) {
                r.setProperty(field, value);
            }
        }
    }

    private void seedInstruments() {
        instr("BANK_GARNISH", "C3", "ADMINISTRATIVE", "SUPERVISOR", "1000", "TPL-GARNISH", "true", "true");
        instr("PROPERTY_SEIZURE", "C4", "FIELD", "DIRECTOR", "5000", "TPL-SEIZURE", "true", "true");
        instr("BANKRUPTCY", "C4", "JUDICIAL", "DIRECTOR", "20000", "TPL-BANKR", "true", "true");
        instr("PUBLISH_NAME", "C3", "ADMINISTRATIVE", "SUPERVISOR", "1000", "", "false", "true");
        instr("LIEN", "C4", "ADMINISTRATIVE", "SUPERVISOR", "1000", "TPL-LIEN", "true", "true");
    }

    private void instr(String code, String minCat, String mode, String authority, String minAmt,
                       String tpl, String cost, String holdSensitive) {
        put("mdInstrument", row("code", code, "minCategory", minCat, "executionMode", mode,
                "authorityLevel", authority, "proportionalityMinAmount", minAmt, "docTemplate", tpl,
                "costRecorded", cost, "disputeHoldSensitive", holdSensitive, "enabled", "true"),
                "instr-" + code);
    }

    private void seedFees() {
        put("mdLegalFee", row("instrument", "BANK_GARNISH", "feeAmount", "50",
                "chargeCategory", "LEGAL_FEE", "active", "true"), "lf-g");
        put("mdLegalFee", row("instrument", "BANKRUPTCY", "feeAmount", "300",
                "chargeCategory", "LEGAL_FEE", "active", "true"), "lf-b");
        put("mdLegalFee", row("instrument", "", "feeAmount", "40",
                "chargeCategory", "LEGAL_FEE", "active", "true"), "lf-def");
        put("mdAgentFee", row("category", "C4", "appointmentFee", "200", "commissionRate", "0.10",
                "reportingIntervalDays", "7", "active", "true"), "af-c4");
    }

    // ---------------- store ----------------

    private FormRowSet match(String form, String cond, Object[] params, Integer limit) {
        FormRowSet out = new FormRowSet();
        List<String[]> preds = new ArrayList<>();
        if (cond != null) {
            Matcher m = EQ.matcher(cond);
            while (m.find()) {
                preds.add(new String[]{m.group(1), m.group(2)});
            }
        }
        for (FormRow r : rows(form)) {
            boolean ok = true;
            for (String[] pr : preds) {
                Object want = params[Integer.parseInt(pr[1]) - 1];
                if (!String.valueOf(want).equals(r.getProperty(pr[0]))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(r);
            }
            if (limit != null && limit > 0 && out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private List<FormRow> rows(String form) {
        return store.computeIfAbsent(form, k -> new ArrayList<>());
    }

    private void put(String form, FormRow r, String id) {
        r.setId(id);
        rows(form).add(r);
    }

    private void upsert(String form, FormRow r) {
        List<FormRow> list = rows(form);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() != null && list.get(i).getId().equals(r.getId())) {
                list.set(i, r);
                return;
            }
        }
        list.add(r);
    }

    private String prop(String form, String id, String field) {
        for (FormRow r : rows(form)) {
            if (id.equals(r.getId())) {
                return field == null ? "" : r.getProperty(field);
            }
        }
        return null;
    }

    private long ev(String caseId, String type) {
        return rows("cmEvent").stream()
                .filter(e -> type.equals(e.getProperty("eventType"))
                        && caseId.equals(e.getProperty("caseId"))).count();
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
