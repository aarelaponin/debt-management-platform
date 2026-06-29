package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

import com.fiscaladmin.mtca.cmbb.service.EnforcementConfigService;

/** EnforcementConfigService — DMBB-F08 VALIDATE + template render/consolidate (T-17 unit). */
public class EnforcementConfigServiceTest {

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
        doAnswer(inv -> null).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
    }

    private EnforcementConfigService svc() {
        return new EnforcementConfigService(dao);
    }

    // ---------------- VALIDATE ----------------

    @Test
    public void validatePassesOnConsistentConfig() {
        instr("BANK_GARNISH", "C3", "ADMINISTRATIVE", "TPL-GARNISH", "true", "true");
        template("TPL-GARNISH", "true");
        put("mdLegalFee", row("instrument", "BANK_GARNISH", "feeAmount", "50"), "lf-g");

        EnforcementConfigService.Result r = svc().validate("");
        assertTrue(r.toString(), r.valid);
        assertEquals(0, r.issueCount);
    }

    @Test
    public void validateCatchesDanglingTemplate() {
        instr("BANKRUPTCY", "C4", "JUDICIAL", "TPL-MISSING", "false", "true");

        EnforcementConfigService.Result r = svc().validate("");
        assertFalse(r.valid);
        assertEquals(1, r.issueCount);
        assertTrue(r.issues.get(0).contains("TPL-MISSING"));
    }

    @Test
    public void validateCatchesCostWithoutFee() {
        instr("LIEN", "C4", "ADMINISTRATIVE", "", "true", "true"); // costRecorded, no fee, no template
        EnforcementConfigService.Result r = svc().validate("");
        assertFalse(r.valid);
        assertTrue(r.issues.get(0).contains("mdLegalFee"));
    }

    @Test
    public void validateIgnoresDisabledInstruments() {
        instr("TRAVEL_BAN_FOREIGN", "C5", "JUDICIAL", "TPL-MISSING", "false", "false"); // enabled=false
        EnforcementConfigService.Result r = svc().validate("");
        assertTrue(r.valid);
        assertEquals(0, r.issueCount);
    }

    // ---------------- RENDER (DM-FR-040/041) ----------------

    @Test
    public void renderSeparatePerTaxType() {
        previewCase();
        EnforcementConfigService.Preview pv = svc().render("TPL-DEMAND", "c1", "");
        assertEquals(2, pv.noticeCount);                       // VAT + INCOME, SEPARATE default
        assertTrue(pv.body, pv.body.contains("ACME Ltd"));     // #taxpayerName# merged
        assertTrue(pv.body, pv.body.contains("DM-000123"));    // #caseRef# merged
        assertFalse(pv.body.contains("#"));                    // no unresolved tokens
    }

    @Test
    public void renderConsolidatedAllSumsAmount() {
        previewCase();
        EnforcementConfigService.Preview pv = svc().render("TPL-DEMAND", "c1", "CONSOLIDATED_ALL");
        assertEquals(1, pv.noticeCount);
        assertTrue(pv.body, pv.body.contains("1500"));         // 1000 + 500 total
    }

    @Test
    public void mergeLeavesUnknownTokens() {
        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("taxpayerName", "Joe");
        assertEquals("Hi Joe #x#", EnforcementConfigService.merge("Hi #taxpayerName# #x#", ctx));
    }

    // ---------------- fixtures ----------------

    private void previewCase() {
        put("cmCase", row("caseType", "DM", "caseRef", "DM-000123", "taxpayerName", "ACME Ltd",
                "tin", "T100", "amountAtStake", "1500", "currentState", "OPEN"), "c1");
        put("dmLine", row("caseId", "c1", "taxType", "VAT", "enforceable", "1000"), "c1-L1");
        put("dmLine", row("caseId", "c1", "taxType", "INCOME", "enforceable", "500"), "c1-L2");
        put("mdNoticeRule", row("caseType", "DM", "consolidationMode", "SEPARATE", "active", "true"), "nr");
        put("mdTemplate", row("code", "TPL-DEMAND", "active", "true", "version", "1",
                "subject", "Demand #caseRef#",
                "body", "Dear #taxpayerName#, demand for #amountAtStake# (#taxType#) on #caseRef#."), "t1");
    }

    private void instr(String code, String minCat, String mode, String tpl, String cost, String enabled) {
        put("mdInstrument", row("code", code, "name", code, "minCategory", minCat,
                "executionMode", mode, "docTemplate", tpl, "costRecorded", cost,
                "taxpayerType", "ALL", "instructions", "do it", "enabled", enabled), "i-" + code);
    }

    private void template(String code, String active) {
        put("mdTemplate", row("code", code, "active", active, "version", "1",
                "subject", "S", "body", "B"), "t-" + code);
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

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
