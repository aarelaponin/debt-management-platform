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
import com.fiscaladmin.mtca.cmbb.service.DebtIdentificationService;
import com.fiscaladmin.mtca.cmbb.service.ReliefService;

/**
 * ReliefService — DMBB-F06 instalment apply/compliance (T-15 unit).
 * Backed by a generic in-memory FormDataDao fake (store keyed by form id),
 * with an AND-equality matcher over the {@code e.customProperties.X = ?n} clauses.
 */
public class ReliefServiceTest {

    private static final Pattern EQ =
            Pattern.compile("customProperties\\.(\\w+)\\s*=\\s*\\?(\\d+)");

    private FormDataDao dao;
    private final Map<String, List<FormRow>> store = new LinkedHashMap<>();
    private final List<String> started = new ArrayList<>();

    @Before
    public void setUp() {
        store.clear();
        started.clear();
        dao = mock(FormDataDao.class);

        when(dao.load(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            String form = inv.getArgument(0);
            String id = inv.getArgument(2);
            for (FormRow r : rows(form)) {
                if (id != null && id.equals(r.getId())) {
                    return r;
                }
            }
            return null;
        });
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> query(inv.getArgument(0), inv.getArgument(2),
                        inv.getArgument(3), (Integer) inv.getArgument(7)));
        doAnswer(inv -> {
            String form = inv.getArgument(0);
            FormRowSet set = inv.getArgument(2);
            for (FormRow r : set) {
                upsert(form, r);
            }
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
        when(dao.count(anyString(), anyString(), any(), any())).thenAnswer(inv ->
                (long) query(inv.getArgument(0), inv.getArgument(2),
                        inv.getArgument(3), null).size());

        // ADR-003: the dmInstLine DM lifecycle the migrated compliance guard reads
        // (mirrors seed/mmEntityTransition.csv) — without it every line move is "illegal".
        put("mmEntityTransition", row("entity", "dmInstLine", "scope", "DM",
                "fromStatus", "pending", "toStatus", "paid"), "tr-line-dm-1");
        put("mmEntityTransition", row("entity", "dmInstLine", "scope", "DM",
                "fromStatus", "pending", "toStatus", "missed"), "tr-line-dm-2");
        put("mmEntityTransition", row("entity", "dmInstLine", "scope", "DM",
                "fromStatus", "missed", "toStatus", "paid"), "tr-line-dm-3");
        // ADR-003 migration #5: the dmInstAgr DM lifecycle the migrated apply/cancel guard reads.
        String[][] agrEdges = {
            {"APPLIED", "ACTIVE"}, {"APPLIED", "UNDER_REVIEW"}, {"APPLIED", "REJECTED"},
            {"UNDER_REVIEW", "ACTIVE"}, {"UNDER_REVIEW", "REJECTED"},
            {"ACTIVE", "COMPLETED"}, {"ACTIVE", "CANCELLED"},
        };
        int ai = 0;
        for (String[] e : agrEdges) {
            put("mmEntityTransition", row("entity", "dmInstAgr", "scope", "DM",
                    "fromStatus", e[0], "toStatus", e[1]), "tr-ia-dm-" + (++ai));
        }
    }

    private ReliefService svc() {
        return new ReliefService(dao, new CaseEventWriter(dao), new ProcessStarterStub());
    }

    // ---------------- APPLY ----------------

    @Test
    public void autoApprovesEligibleAndAssertsHold() {
        put("dmInstAgr", row("status", "APPLIED", "tin", "T1", "debtCaseId", "case-1",
                "totalDebt", "4000", "durationMonths", "12"), "agr-1");
        put("dmDebt", row("debtCategory", "C4"), "case-1");
        put("mdRelief", row("categories", "C3,C4,C5", "minInstalment", "50",
                "autoThreshold", "5000", "autoMaxMonths", "12"), "rel-1");
        put("mdProjRate", row("reducedRate", "0.5"), "pr-1");

        String result = svc().apply("agr-1", "tester", LocalDateTime.now());

        assertTrue(result, result.startsWith("ACTIVE"));
        assertEquals("ACTIVE", prop("dmInstAgr", "agr-1", "status"));
        assertEquals(12, rows("dmInstLine").size());
        FormRow hold = firstWhere("cmHold", "scope", "ENFORCEMENT_SUPPRESS");
        assertNotNull("hold asserted", hold);
        assertEquals("ACTIVE", hold.getProperty("status"));
        assertEquals(1, ev("INSTALMENT_APPROVED"));
        assertEquals(1, ev("HOLD_ASSERTED"));
    }

    @Test
    public void rejectsSecondActivePlan() {
        put("dmInstAgr", row("status", "ACTIVE", "tin", "T1"), "agr-0");
        put("dmInstAgr", row("status", "APPLIED", "tin", "T1", "debtCaseId", "case-1",
                "totalDebt", "1000", "durationMonths", "6"), "agr-1");
        put("dmDebt", row("debtCategory", "C4"), "case-1");
        put("mdRelief", row("categories", "C3,C4,C5", "minInstalment", "10",
                "autoThreshold", "5000", "autoMaxMonths", "12"), "rel-1");

        String result = svc().apply("agr-1", "tester", LocalDateTime.now());

        assertTrue(result, result.startsWith("REJECTED"));
        assertEquals("REJECTED", prop("dmInstAgr", "agr-1", "status"));
        assertEquals(0, rows("dmInstLine").size());
        assertEquals(0, ev("HOLD_ASSERTED"));
        assertEquals(1, ev("INSTALMENT_REJECTED"));
    }

    @Test
    public void rejectsIneligibleCategory() {
        put("dmInstAgr", row("status", "APPLIED", "tin", "T2", "debtCaseId", "case-2",
                "totalDebt", "1000", "durationMonths", "6"), "agr-2");
        put("dmDebt", row("debtCategory", "C1"), "case-2");
        put("mdRelief", row("categories", "C3,C4,C5", "minInstalment", "10",
                "autoThreshold", "5000", "autoMaxMonths", "12"), "rel-1");

        String result = svc().apply("agr-2", "tester", LocalDateTime.now());

        assertTrue(result, result.contains("not eligible"));
        assertEquals("REJECTED", prop("dmInstAgr", "agr-2", "status"));
    }

    // ---------------- COMPLIANCE ----------------

    @Test
    public void defaultCancelsReleasesHoldAndOpensRecovery() {
        put("dmInstAgr", row("status", "ACTIVE", "tin", "T9", "debtCaseId", "case-9",
                "totalDebt", "1200", "monthlyAmount", "100"), "agr-9");
        String pastDue = LocalDate.now().minusDays(10).toString();
        for (int i = 1; i <= 3; i++) {
            put("dmInstLine", row("instAgrId", "agr-9", "seq", String.valueOf(i),
                    "dueDate", pastDue, "expectedAmount", "100", "paidAmount", "0",
                    "status", "pending"), "agr-9-L" + i);
        }
        put("dmDebt", row("debtCategory", "C4"), "case-9");
        put("mdViolation", row("graceDays", "3", "consecutiveMissThreshold", "2"), "vio-1");
        put("cmHold", row("caseId", "case-9", "scope", "ENFORCEMENT_SUPPRESS",
                "status", "ACTIVE"), "hold-9");

        ReliefService.Tally t = svc().compliance("tester", LocalDateTime.now());

        assertEquals(1, t.evaluated);
        assertEquals(1, t.cancelled);
        assertEquals("CANCELLED", prop("dmInstAgr", "agr-9", "status"));
        assertEquals("RELEASED", prop("cmHold", "hold-9", "status"));
        // a recovery DM case was opened for the remaining balance and started
        FormRow recovery = firstWhere("cmCase", "triggerOrigin", null); // any new cmCase
        assertNotNull("recovery case row", recovery);
        assertEquals("DM", recovery.getProperty("caseType"));
        assertEquals("1200.0", recovery.getProperty("amountAtStake"));
        assertEquals(1, started.size());
        assertEquals(1, ev("INSTALMENT_CANCELLED"));
        assertEquals(1, ev("RECOVERY_CASE_CREATED"));
        assertEquals(1, ev("HOLD_RELEASED"));
    }

    @Test
    public void compliantPlanSurvives() {
        put("dmInstAgr", row("status", "ACTIVE", "tin", "T8", "debtCaseId", "case-8",
                "totalDebt", "300"), "agr-8");
        String pastDue = LocalDate.now().minusDays(10).toString();
        for (int i = 1; i <= 2; i++) {
            put("dmInstLine", row("instAgrId", "agr-8", "seq", String.valueOf(i),
                    "dueDate", pastDue, "expectedAmount", "100", "paidAmount", "100",
                    "status", "pending"), "agr-8-L" + i);
        }
        put("mdViolation", row("graceDays", "3", "consecutiveMissThreshold", "2"), "vio-1");

        ReliefService.Tally t = svc().compliance("tester", LocalDateTime.now());

        assertEquals(1, t.evaluated);
        assertEquals(0, t.cancelled);
        assertEquals("ACTIVE", prop("dmInstAgr", "agr-8", "status"));
        assertEquals("COMPLIANT", prop("dmInstAgr", "agr-8", "complianceStatus"));
    }

    // ---------------- fake store ----------------

    private final class ProcessStarterStub implements DebtIdentificationService.ProcessStarter {
        @Override
        public void start(String caseId, String assignee) {
            started.add(caseId);
        }
    }

    private FormRowSet query(String form, String cond, Object[] params, Integer limit) {
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

    private FormRow firstWhere(String form, String field, String value) {
        for (FormRow r : rows(form)) {
            if (value == null || value.equals(r.getProperty(field))) {
                return r;
            }
        }
        return null;
    }

    private long ev(String type) {
        return rows("cmEvent").stream()
                .filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
