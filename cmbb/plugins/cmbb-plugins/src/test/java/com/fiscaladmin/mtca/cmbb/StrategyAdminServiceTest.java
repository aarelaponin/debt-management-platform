package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.StrategyAdminService;

/** StrategyAdminService — DMBB-F05 strategy consistency validation (T-14 unit). */
public class StrategyAdminServiceTest {

    private FormDataDao dao;
    private final List<FormRow> strategies = new ArrayList<FormRow>();
    private final List<FormRow> steps = new ArrayList<FormRow>();
    private final Map<String, Boolean> instrumentEnabled = new HashMap<String, Boolean>();

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        strategies.clear();
        steps.clear();
        instrumentEnabled.clear();
        instrumentEnabled.put("DEMAND", true);
        instrumentEnabled.put("BANK_GARNISH", true);
        strategies.add(row("code", "STD-MLT", "segment", "ALL", "categoryFloor", "C2",
                "version", "1", "active", "true", "effectiveFrom", "2026-01-01", "effectiveTo", ""));
        steps.add(row("strategyCode", "STD-MLT", "seq", "1", "instrument", ""));
        steps.add(row("strategyCode", "STD-MLT", "seq", "2", "instrument", "DEMAND"));
        steps.add(row("strategyCode", "STD-MLT", "seq", "3", "instrument", "BANK_GARNISH"));

        when(dao.find(eq("mmStrategy"), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> rs(strategies));
        when(dao.find(eq("mmEscStep"), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> rs(steps));
        when(dao.load(eq("mdInstrument"), eq("mdInstrument"), anyString())).thenAnswer(inv -> {
            String code = inv.getArgument(2);
            if (!instrumentEnabled.containsKey(code)) {
                return null; // not in catalogue
            }
            return row("code", code, "enabled", instrumentEnabled.get(code) ? "true" : "false");
        });
    }

    @Test
    public void validWhenAllStepsEnabledAndCoverageComplete() {
        StrategyAdminService.Result r = new StrategyAdminService(dao).validate();
        assertTrue(r.summary(), r.valid);
        assertEquals(0, r.issues.size());
    }

    @Test
    public void flagsStepOnDisabledInstrument() {
        instrumentEnabled.put("BANK_GARNISH", false);
        StrategyAdminService.Result r = new StrategyAdminService(dao).validate();
        assertFalse(r.valid);
        assertTrue(r.summary(), r.summary().contains("BANK_GARNISH") && r.summary().contains("disabled"));
    }

    @Test
    public void flagsMissingCategoryCoverage() {
        strategies.get(0).setProperty("categoryFloor", "C4"); // C2, C3 now uncovered
        StrategyAdminService.Result r = new StrategyAdminService(dao).validate();
        assertFalse(r.valid);
        assertTrue(r.summary(), r.summary().contains("C2") && r.summary().contains("C3"));
    }

    private static FormRowSet rs(List<FormRow> rows) {
        FormRowSet s = new FormRowSet();
        s.addAll(rows);
        return s;
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
