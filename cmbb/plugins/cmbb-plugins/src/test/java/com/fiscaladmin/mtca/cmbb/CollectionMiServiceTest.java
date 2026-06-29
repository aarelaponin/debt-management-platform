package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.fiscaladmin.mtca.cmbb.service.CollectionMiService;

/** CollectionMiService — DMBB-F12 rollup / suggest / reconcile (T-21 unit). */
public class CollectionMiServiceTest {

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
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
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
    }

    private CollectionMiService svc() {
        return new CollectionMiService(dao, new CaseEventWriter(dao));
    }

    @Test
    public void rollupComputesPlanVsActual() {
        debt("case4a", "C4");
        debt("case4b", "C4");
        put("dmAction", row("debtCaseId", "case4a", "recoveredAmount", "3000"), "a1");
        put("dmAgent", row("debtCaseId", "case4b", "recoveredTotal", "1000"), "g1");
        put("dmPlanTarget", row("planId", "P1", "category", "C4", "targetAmount", "10000"), "t-c4");
        put("dmPlanTarget", row("planId", "P1", "category", "C3", "targetAmount", "5000"), "t-c3");

        CollectionMiService.Tally t = svc().rollup("P1", "mi", LocalDateTime.now());
        assertEquals(2, t.matched);
        assertEquals("4000", prop("dmPlanTarget", "t-c4", "actualAmount"));
        assertEquals("40", prop("dmPlanTarget", "t-c4", "attainmentPct"));
        assertEquals("0", prop("dmPlanTarget", "t-c3", "actualAmount"));
    }

    @Test
    public void suggestSetsTargetFromStock() {
        // two open C4 debts, stock 20000; rate 0.35 -> suggested 7000
        miCase("s1", "C4", "12000");
        miCase("s2", "C4", "8000");
        put("mdCollectionParam", row("category", "C4", "recoveryRate", "0.35"), "cp4");
        put("dmPlanTarget", row("planId", "P2", "category", "C4", "targetAmount", "0"), "t2");

        CollectionMiService.Tally t = svc().suggest("P2", "mi", LocalDateTime.now());
        assertEquals(1, t.matched);
        assertEquals("7000", prop("dmPlanTarget", "t2", "suggestedAmount"));
        assertEquals("7000", prop("dmPlanTarget", "t2", "targetAmount")); // seeded (was 0)
    }

    @Test
    public void reconcileFlagsOrphanWriteoffEvent() {
        // matched: WRITEOFF_POSTED on caseX has a dmCharge WRITE_OFF
        put("cmEvent", row("eventType", "WRITEOFF_POSTED", "caseId", "caseX"), "ev1");
        put("dmCharge", row("debtCaseId", "caseX", "chargeType", "WRITE_OFF"), "ch1");
        // orphan: WRITEOFF_POSTED on caseY with no charge
        put("cmEvent", row("eventType", "WRITEOFF_POSTED", "caseId", "caseY"), "ev2");

        CollectionMiService.Tally t = svc().reconcile("mi", LocalDateTime.now());
        assertEquals(1, t.matched);
        assertEquals(1, t.variance);
    }

    // ---------------- fixtures ----------------

    private void debt(String caseId, String cat) {
        put("dmDebt", row("debtCategory", cat), caseId);
    }

    private void miCase(String id, String cat, String amount) {
        put("cmCase", row("caseType", "DM", "category", cat), id);
        put("dmDebt", row("debtCategory", cat, "consolidatedAmount", amount, "writeOffStatus", ""), id);
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

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
