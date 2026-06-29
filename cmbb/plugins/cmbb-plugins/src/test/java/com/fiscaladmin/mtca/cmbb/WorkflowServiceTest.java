package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.WorkflowService;

/** WorkflowService — ADR-004 selector/validity/specificity resolution + overlap VALIDATE (unit). */
public class WorkflowServiceTest {

    private FormDataDao dao;
    private final List<FormRow> workflows = new ArrayList<FormRow>();
    private final LocalDateTime now = LocalDateTime.of(2026, 6, 25, 0, 0);

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        workflows.clear();
        workflows.add(wf("W-DEFAULT", "MLT", "", "", "", "C2", "2020-01-01", "", "Active", "1"));
        workflows.add(wf("W-VAT", "MLT", "VAT", "", "", "C2", "2020-01-01", "", "Active", "1"));
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    FormRowSet out = new FormRowSet();
                    if ("dmWorkflow".equals(inv.getArgument(0))) {
                        out.addAll(workflows);
                    }
                    return out;
                });
    }

    private WorkflowService svc() {
        return new WorkflowService(dao);
    }

    @Test
    public void taxSpecificWorkflowBeatsDefault() {
        FormRow w = svc().resolve("MLT", "VAT", "", "", "C6", now);
        assertNotNull(w);
        assertEquals("W-VAT", w.getProperty("code"));
    }

    @Test
    public void nonVatFallsBackToDefault() {
        FormRow w = svc().resolve("MLT", "INCOME_TAX", "", "", "C6", now);
        assertNotNull(w);
        assertEquals("W-DEFAULT", w.getProperty("code"));
    }

    @Test
    public void blankCaseTenantResolvesViaDefaultTenant() {
        // a case with no tenant yet (pre Phase 1c) still resolves the MLT default workflow
        FormRow w = svc().resolve("", "INCOME_TAX", "", "", "C6", now);
        assertNotNull(w);
        assertEquals("W-DEFAULT", w.getProperty("code"));
    }

    @Test
    public void belowFloorYieldsNoWorkflow() {
        assertNull(svc().resolve("MLT", "INCOME_TAX", "", "", "C1", now));
    }

    @Test
    public void outsideValidityWindowIsExcluded() {
        // both workflows start 2020-01-01 → an asOf in 2019 matches neither
        assertNull(svc().resolve("MLT", "VAT", "", "", "C6", LocalDateTime.of(2019, 1, 1, 0, 0)));
    }

    @Test
    public void segmentSpecificBeatsTaxWhenMoreDimensionsMatch() {
        // a workflow keyed on (tax=VAT, segment=LARGE) outscores the tax-only W-VAT for a LARGE VAT case
        workflows.add(wf("W-VAT-LARGE", "MLT", "VAT", "LARGE", "", "C2", "2020-01-01", "", "Active", "1"));
        FormRow w = svc().resolve("MLT", "VAT", "LARGE", "", "C6", now);
        assertEquals("W-VAT-LARGE", w.getProperty("code"));
    }

    @Test
    public void higherVersionBreaksSpecificityTie() {
        workflows.add(wf("W-VAT-V2", "MLT", "VAT", "", "", "C2", "2020-01-01", "", "Active", "2"));
        FormRow w = svc().resolve("MLT", "VAT", "", "", "C6", now);
        assertEquals("W-VAT-V2", w.getProperty("code"));
    }

    @Test
    public void noOverlapForDistinctSelectorTuples() {
        // W-DEFAULT and W-VAT have different selector tuples → no ambiguity
        assertEquals(0, svc().findOverlaps().size());
    }

    @Test
    public void overlapDetectedForSameTupleOverlappingWindows() {
        workflows.add(wf("W-VAT-DUP", "MLT", "VAT", "", "", "C2", "2020-06-01", "", "Active", "1"));
        List<String> issues = svc().findOverlaps();
        assertEquals(1, issues.size()); // W-VAT (open-ended) overlaps W-VAT-DUP (same tuple)
    }

    @Test
    public void noOverlapForSameTupleDisjointWindows() {
        // close W-VAT in 2021, open W-VAT-NEXT in 2022 → same tuple but disjoint → no overlap
        workflows.get(1).setProperty("validTo", "2021-12-31");
        workflows.add(wf("W-VAT-NEXT", "MLT", "VAT", "", "", "C2", "2022-01-01", "", "Active", "1"));
        assertEquals(0, svc().findOverlaps().size());
    }

    private static FormRow wf(String code, String tenant, String tax, String seg, String ind,
            String floor, String from, String to, String status, String version) {
        FormRow r = new FormRow();
        r.setProperty("code", code);
        r.setProperty("tenant", tenant);
        r.setProperty("taxType", tax);
        r.setProperty("segment", seg);
        r.setProperty("industry", ind);
        r.setProperty("categoryFloor", floor);
        r.setProperty("validFrom", from);
        r.setProperty("validTo", to);
        r.setProperty("status", status);
        r.setProperty("version", version);
        return r;
    }
}
