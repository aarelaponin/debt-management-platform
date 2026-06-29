package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.fiscaladmin.mtca.cmbb.service.DebtorsListService;

/** DebtorsListService — DMBB-F11 registry extract (T-20 unit). */
public class DebtorsListServiceTest {

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
        doAnswer(inv -> {
            String form = inv.getArgument(0);
            for (FormRow r : (FormRowSet) inv.getArgument(2)) {
                upsert(form, r);
            }
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
    }

    private DebtorsListService svc() {
        return new DebtorsListService(dao, new CaseEventWriter(dao));
    }

    @Test
    public void extractCountsPublishedExcludesRemoved() {
        pub("T1", "Alpha Ltd", "1000", "PUBLISHED");
        pub("T2", "Beta Ltd", "500", "PUBLISHED");
        pub("T3", "Gamma Ltd", "800", "REMOVED"); // resolved — excluded
        put("dmDebtorsExtract", row("registry", "national", "format", "CSV"), "x1");

        String r = svc().extract("x1", "sdo", LocalDateTime.now());
        assertTrue(r, r.startsWith("GENERATED"));
        assertEquals("2", prop("dmDebtorsExtract", "x1", "debtorCount"));
        assertEquals("1500", prop("dmDebtorsExtract", "x1", "totalAmount"));
        assertEquals("GENERATED", prop("dmDebtorsExtract", "x1", "status"));
        String payload = prop("dmDebtorsExtract", "x1", "payload");
        assertTrue(payload, payload.contains("T1") && payload.contains("T2"));
        assertTrue(payload, !payload.contains("T3"));
        assertEquals(1, ev("DEBTORS_EXTRACT_GENERATED"));
    }

    @Test
    public void extractJsonFormat() {
        pub("T9", "Delta Ltd", "1200", "PUBLISHED");
        put("dmDebtorsExtract", row("registry", "api", "format", "JSON"), "x2");
        svc().extract("x2", "sdo", LocalDateTime.now());
        String payload = prop("dmDebtorsExtract", "x2", "payload");
        assertTrue(payload, payload.startsWith("[") && payload.endsWith("]") && payload.contains("T9"));
    }

    @Test
    public void extractIsIdempotent() {
        pub("T1", "Alpha", "100", "PUBLISHED");
        put("dmDebtorsExtract", row("format", "CSV", "status", "GENERATED"), "x3");
        String r = svc().extract("x3", "sdo", LocalDateTime.now());
        assertTrue(r, r.startsWith("already processed"));
    }

    // ---------------- fixtures ----------------

    private void pub(String tin, String name, String amount, String status) {
        put("dmDebtorPub", row("tin", tin, "debtorName", name, "debtAmount", amount,
                "status", status), "p-" + tin);
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
