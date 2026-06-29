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
import com.fiscaladmin.mtca.cmbb.service.DebtIdentificationService;
import com.fiscaladmin.mtca.cmbb.service.DefaultAssessmentService;

/** DefaultAssessmentService — DMBB-F10 estimate / replace / escalate (T-19 unit). */
public class DefaultAssessmentServiceTest {

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
        put("mdDefAssessPolicy", row("reasonablenessMultiplier", "1.5", "nonFilingGraceDays", "30"), "pol");
        put("mdDebtCat", row("code", "C2", "bandOrder", "2", "minAmount", "30", "maxAmount", "100"), "b2");
        put("mdDebtCat", row("code", "C3", "bandOrder", "3", "minAmount", "100", "maxAmount", "1000"), "b3");
        put("mdDebtCat", row("code", "C4", "bandOrder", "4", "minAmount", "1000", "maxAmount", "20000"), "b4");
        seedAssessLifecycle();
    }

    /** ADR-003: dmDefAssess DM lifecycle the migrated engine guard reads (mirrors seed CSV). */
    private void seedAssessLifecycle() {
        String[][] edges = {
            {"DRAFT", "ASSESSED"}, {"DRAFT", "NEEDS_JUSTIFICATION"}, {"DRAFT", "REPLACED"},
            {"NEEDS_JUSTIFICATION", "ASSESSED"}, {"NEEDS_JUSTIFICATION", "REPLACED"},
            {"ASSESSED", "REPLACED"}, {"ASSESSED", "ESCALATED"},
        };
        int i = 0;
        for (String[] e : edges) {
            put("mmEntityTransition",
                    row("entity", "dmDefAssess", "scope", "DM", "fromStatus", e[0], "toStatus", e[1]),
                    "tr-da-dm-" + (++i));
        }
    }

    private DefaultAssessmentService svc() {
        return new DefaultAssessmentService(dao, new CaseEventWriter(dao), new Starter());
    }

    private final class Starter implements DebtIdentificationService.ProcessStarter {
        @Override
        public void start(String caseId, String assignee) {
            started.add(caseId);
        }
    }

    // ---------------- ASSESS ----------------

    @Test
    public void autoEstimateFromPriorYear() {
        method("INDUSTRY_AVG", "industryAvgAmount", "800");
        method("FIXED", "fixedAmount", "500");
        put("dmDefAssess", row("tin", "T1", "priorYearAmount", "1000", "status", "DRAFT"), "da1");
        String r = svc().assess("da1", "off", LocalDateTime.now());
        assertTrue(r, r.startsWith("ASSESSED"));
        assertEquals("1000", prop("dmDefAssess", "da1", "estimatedAmount"));
        assertEquals("PRIOR_YEAR", prop("dmDefAssess", "da1", "estimationMethod"));
        assertEquals("OK", prop("dmDefAssess", "da1", "reasonablenessFlag"));
        assertEquals(1, ev("DEFAULT_ASSESSED"));
    }

    @Test
    public void industryAverageWhenNoPriorYear() {
        method("INDUSTRY_AVG", "industryAvgAmount", "800");
        method("FIXED", "fixedAmount", "500");
        put("dmDefAssess", row("tin", "T2", "status", "DRAFT"), "da2");
        svc().assess("da2", "off", LocalDateTime.now());
        assertEquals("800", prop("dmDefAssess", "da2", "estimatedAmount"));
        assertEquals("INDUSTRY_AVG", prop("dmDefAssess", "da2", "estimationMethod"));
    }

    @Test
    public void fixedFormulaWhenNoPriorOrIndustry() {
        method("FIXED", "fixedAmount", "500");
        put("dmDefAssess", row("tin", "T3", "status", "DRAFT"), "da3");
        svc().assess("da3", "off", LocalDateTime.now());
        assertEquals("500", prop("dmDefAssess", "da3", "estimatedAmount"));
        assertEquals("FIXED", prop("dmDefAssess", "da3", "estimationMethod"));
    }

    @Test
    public void reasonablenessFlagsOverThreshold() {
        put("dmDefAssess", row("tin", "T4", "priorYearAmount", "1000", "estimatedAmount", "2000",
                "status", "DRAFT"), "da4");
        String r = svc().assess("da4", "off", LocalDateTime.now());
        assertTrue(r, r.startsWith("NEEDS_JUSTIFICATION"));
        assertEquals("REVIEW", prop("dmDefAssess", "da4", "reasonablenessFlag"));

        put("dmDefAssess", row("tin", "T4b", "priorYearAmount", "1000", "estimatedAmount", "2000",
                "justification", "new machinery doubled turnover", "status", "DRAFT"), "da4b");
        String r2 = svc().assess("da4b", "off", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("ASSESSED"));
    }

    // ---------------- REPLACE ----------------

    @Test
    public void replaceRecordsVarianceAndRiskSignal() {
        put("dmDefAssess", row("tin", "T5", "estimatedAmount", "1000", "status", "ASSESSED"), "da5");
        put("cmReturnFiled", row("defAssessId", "da5", "filedAmount", "1200"), "rf5");
        String r = svc().replace("rf5", "off", LocalDateTime.now());
        assertTrue(r, r.contains("variance 200"));
        assertEquals("REPLACED", prop("dmDefAssess", "da5", "status"));
        assertEquals("200", prop("dmDefAssess", "da5", "variance"));
        assertEquals(1, ev("ASSESSMENT_REPLACED"));
        assertEquals(1, ev("RISK_PROFILE_UPDATE"));
    }

    // ---------------- ESCALATE ----------------

    @Test
    public void escalateCreatesDebtCaseAfterGrace() {
        put("dmDefAssess", row("tin", "T6", "taxType", "VAT", "estimatedAmount", "900",
                "status", "ASSESSED", "assessedDate", LocalDate.now().minusDays(60).toString()), "da6");
        DefaultAssessmentService.Tally t = svc().escalate("sys", LocalDateTime.now());
        assertEquals(1, t.escalated);
        assertEquals("ESCALATED", prop("dmDefAssess", "da6", "status"));
        assertEquals(1, rows("cmCase").size());
        assertEquals("DM", rows("cmCase").get(0).getProperty("caseType"));
        assertEquals("DEFAULT_ASSESSMENT", rows("dmDebt").get(0).getProperty("triggerOrigin"));
        assertEquals("C3", rows("dmDebt").get(0).getProperty("debtCategory")); // 900 -> C3 band
        assertEquals(1, started.size());
    }

    @Test
    public void escalateSkipsWithinGrace() {
        put("dmDefAssess", row("tin", "T7", "estimatedAmount", "900", "status", "ASSESSED",
                "assessedDate", LocalDate.now().minusDays(10).toString()), "da7");
        DefaultAssessmentService.Tally t = svc().escalate("sys", LocalDateTime.now());
        assertEquals(0, t.escalated);
        assertEquals("ASSESSED", prop("dmDefAssess", "da7", "status"));
    }

    // ---------------- fixtures ----------------

    private void method(String code, String field, String value) {
        put("mdEstMethod", row("code", code, field, value, "active", "true"), "m-" + code);
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

    private long ev(String type) {
        return rows("cmEvent").stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
