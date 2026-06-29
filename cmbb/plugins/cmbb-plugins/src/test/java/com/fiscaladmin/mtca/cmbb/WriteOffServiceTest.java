package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
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
import com.fiscaladmin.mtca.cmbb.service.WriteOffService;

/** WriteOffService — DMBB-F09 auto-C1 / approved / statutory write-off (T-18 unit). */
public class WriteOffServiceTest {

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
        // policy / delegation defaults
        put("mdWoPolicy", row("c1Max", "30", "c2Min", "30", "c2Max", "100", "statutoryYears", "5"), "wp");
        put("mdWoDelegation", row("dmoMax", "1000", "sdoMax", "20000"), "wd");
        put("mdWoGround", row("code", "WO-MGMT", "requiresEvidence", "true", "requiresApproval", "true"), "g1");
        seedWriteOffLifecycle();
    }

    /** ADR-003: dmWriteOff DM lifecycle the migrated engine guard reads (mirrors seed CSV). */
    private void seedWriteOffLifecycle() {
        String[][] edges = {
            {"SUBMITTED", "UNDER_REVIEW"}, {"SUBMITTED", "POSTED"}, {"SUBMITTED", "REJECTED"},
            {"UNDER_REVIEW", "POSTED"}, {"UNDER_REVIEW", "REJECTED"},
        };
        int i = 0;
        for (String[] e : edges) {
            put("mmEntityTransition",
                    row("entity", "dmWriteOff", "scope", "DM", "fromStatus", e[0], "toStatus", e[1]),
                    "tr-wo-dm-" + (++i));
        }
    }

    private WriteOffService svc() {
        return new WriteOffService(dao, new CaseEventWriter(dao));
    }

    // ---------------- SUBMIT / APPROVE ----------------

    @Test
    public void submitC1AutoPostsImmediately() {
        debtCase("c1", "T1", "C1", "20");
        put("dmWriteOff", row("debtCaseId", "c1", "tin", "T1", "amount", "20",
                "woType", "C1_AUTO", "status", "SUBMITTED"), "wo1");
        String r = svc().submit("wo1", "sys", LocalDateTime.now());
        assertTrue(r, r.startsWith("POSTED"));
        assertEquals("written-off", prop("dmDebt", "c1", "writeOffStatus"));
        assertEquals("CLOSED", prop("cmCase", "c1", "currentState"));
        assertEquals(1, ev("c1", "WRITEOFF_POSTED"));
    }

    @Test
    public void evidenceGuardRejectsApprovedWithoutEvidence() {
        debtCase("c2", "T2", "C3", "500");
        put("dmWriteOff", row("debtCaseId", "c2", "tin", "T2", "amount", "500",
                "woType", "APPROVED", "ground", "WO-MGMT", "status", "SUBMITTED"), "wo2");
        String r = svc().submit("wo2", "dmo", LocalDateTime.now());
        assertTrue(r, r.startsWith("REJECTED"));
        assertEquals("REJECTED", prop("dmWriteOff", "wo2", "status"));
        assertEquals(1, ev("c2", "WRITEOFF_REJECTED"));
    }

    @Test
    public void submitApprovedRoutesByDelegation() {
        debtCase("c3", "T3", "C4", "500");
        put("dmWriteOff", row("debtCaseId", "c3", "tin", "T3", "amount", "500", "woType", "APPROVED",
                "ground", "WO-MGMT", "enforcementHistorySummary", "5 actions", "evidenceRef", "EV-1",
                "rationale", "insolvent", "status", "SUBMITTED"), "wo3");
        String r = svc().submit("wo3", "dmo", LocalDateTime.now());
        assertTrue(r, r.contains("DMO"));
        assertEquals("UNDER_REVIEW", prop("dmWriteOff", "wo3", "status"));
        assertEquals("DMO", prop("dmWriteOff", "wo3", "approvalLevel"));

        debtCase("c3b", "T3B", "C5", "25000");
        put("dmWriteOff", row("debtCaseId", "c3b", "tin", "T3B", "amount", "25000", "woType", "APPROVED",
                "ground", "WO-MGMT", "enforcementHistorySummary", "x", "evidenceRef", "EV-2",
                "rationale", "y", "status", "SUBMITTED"), "wo3b");
        svc().submit("wo3b", "sdo", LocalDateTime.now());
        assertEquals("DIRECTOR", prop("dmWriteOff", "wo3b", "approvalLevel"));
    }

    @Test
    public void approvePostsWriteOff() {
        debtCase("c4", "T4", "C4", "500");
        put("dmWriteOff", row("debtCaseId", "c4", "tin", "T4", "amount", "500",
                "woType", "APPROVED", "approvalLevel", "DMO", "status", "UNDER_REVIEW"), "wo4");
        put("cmWriteOffApprove", row("writeOffId", "wo4", "decision", "APPROVE",
                "approver", "boss"), "ap4");
        String r = svc().approve("ap4", "boss", LocalDateTime.now());
        assertTrue(r, r.startsWith("POSTED"));
        assertEquals("POSTED", prop("dmWriteOff", "wo4", "status"));
        assertEquals("written-off", prop("dmDebt", "c4", "writeOffStatus"));
        assertEquals("CLOSED", prop("cmCase", "c4", "currentState"));
    }

    @Test
    public void approveRejectReturnsCase() {
        debtCase("c5", "T5", "C4", "500");
        put("dmWriteOff", row("debtCaseId", "c5", "tin", "T5", "amount", "500",
                "woType", "APPROVED", "status", "UNDER_REVIEW"), "wo5");
        put("cmWriteOffApprove", row("writeOffId", "wo5", "decision", "REJECT",
                "approver", "boss"), "ap5");
        svc().approve("ap5", "boss", LocalDateTime.now());
        assertEquals("REJECTED", prop("dmWriteOff", "wo5", "status"));
        assertEquals("OPEN", prop("cmCase", "c5", "currentState")); // not closed
    }

    // ---------------- SWEEP ----------------

    @Test
    public void autoC1SweepWritesOffAllC1() {
        debtCase("s1", "S1", "C1", "20");
        debtCase("s2", "S2", "C3", "500"); // not C1 — untouched
        WriteOffService.Tally t = svc().sweep("AUTO_C1", "sys", LocalDateTime.now());
        assertEquals(1, t.writtenOff);
        assertEquals("written-off", prop("dmDebt", "s1", "writeOffStatus"));
        assertEquals("CLOSED", prop("cmCase", "s1", "currentState"));
        assertEquals("", nz(prop("dmDebt", "s2", "writeOffStatus")));
    }

    @Test
    public void statutoryBulkSweepWritesOffAgedDebt() {
        debtCase("s3", "S3", "C2", "60");
        setDebt("s3", "firstAssessedDate", LocalDate.now().minusYears(6).toString());
        WriteOffService.Tally t = svc().sweep("STATUTORY_BULK", "sys", LocalDateTime.now());
        assertEquals(1, t.writtenOff);
        assertEquals("written-off", prop("dmDebt", "s3", "writeOffStatus"));
    }

    @Test
    public void c2PassiveSweepMarksStatus() {
        debtCase("s4", "S4", "C2", "60");
        WriteOffService.Tally t = svc().sweep("C2_PASSIVE", "sys", LocalDateTime.now());
        assertEquals(1, t.writtenOff);
        assertEquals("passive-collection", prop("dmDebt", "s4", "writeOffStatus"));
        assertEquals("OPEN", prop("cmCase", "s4", "currentState")); // passive ≠ closed
    }

    // ---------------- fixtures ----------------

    private void debtCase(String id, String tin, String cat, String amount) {
        put("cmCase", row("caseType", "DM", "tin", tin, "category", cat,
                "taxpayerName", "TP " + tin, "currentState", "OPEN"), id);
        put("dmDebt", row("tin", tin, "debtCategory", cat, "consolidatedAmount", amount,
                "writeOffStatus", ""), id);
    }

    private void setDebt(String caseId, String field, String value) {
        for (FormRow r : rows("dmDebt")) {
            if (caseId.equals(r.getId())) {
                r.setProperty(field, value);
            }
        }
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
                return r.getProperty(field);
            }
        }
        return null;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
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
