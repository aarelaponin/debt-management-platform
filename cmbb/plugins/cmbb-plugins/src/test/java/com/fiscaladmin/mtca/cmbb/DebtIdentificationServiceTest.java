package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DebtIdentificationService;
import com.fiscaladmin.mtca.cmbb.service.GoldMartScanner;

/** DebtIdentificationService — DMBB-F03 consolidation/categorisation/dedup (T-12 unit). */
public class DebtIdentificationServiceTest {

    private FormDataDao dao;
    private final List<FormRow> workflows = new ArrayList<FormRow>(); // ADR-004 dmWorkflow catalogue
    private final List<FormRow> existingDm = new ArrayList<FormRow>();
    private final Map<String, List<FormRow>> saved = new HashMap<String, List<FormRow>>();
    private final List<String> started = new ArrayList<String>();
    private GoldMartScanner scanner;
    private DebtIdentificationService.ProcessStarter starter;

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        saved.clear();
        existingDm.clear();
        started.clear();
        // ADR-004 catalogue: W-DEFAULT (tax ANY → consolidates) + W-VAT (tax-specific → own case)
        workflows.clear();
        workflows.add(wf("W-DEFAULT", "MLT", "", "C2"));
        workflows.add(wf("W-VAT", "MLT", "VAT", "C2"));
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    String form = inv.getArgument(0);
                    FormRowSet out = new FormRowSet();
                    if ("mmState".equals(form)) {
                        FormRow s = new FormRow();
                        s.setProperty("code", "CLOSED");
                        out.add(s);
                    } else if ("cmCase".equals(form)) {
                        out.addAll(existingDm);
                    } else if ("dmWorkflow".equals(form)) {
                        out.addAll(workflows);
                    }
                    return out; // cmEvent / mdUserTenant -> empty (genesis / default tenant)
                });
        doAnswer(inv -> {
            String form = inv.getArgument(0);
            FormRowSet rs = inv.getArgument(2);
            saved.computeIfAbsent(form, k -> new ArrayList<FormRow>()).addAll(rs);
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));

        scanner = new GoldMartScanner(new GoldMartScanner.ScanGateway() {
            public List<GoldMartScanner.GoldDebtor> scan(String minAmount) {
                return Arrays.asList(debtor("100010A", "C4", "5000"),
                                     debtor("100091L", "C6", "1000000"));
            }

            public List<GoldMartScanner.GoldLine> lines(String tin) {
                return Arrays.asList(line("VAT", "3000"), line("CIT", "2000"));
            }
        });
        starter = (caseId, assignee) -> started.add(caseId);
    }

    private DebtIdentificationService svc() {
        return new DebtIdentificationService(dao, scanner, new CaseEventWriter(dao), starter);
    }

    @Test
    public void splitsTaxSpecificFromConsolidated() {
        // each TIN has a VAT line (W-VAT, tax-specific → own case) and a CIT line
        // (W-DEFAULT, tax-agnostic → consolidated case): 2 cases per TIN × 2 TINs = 4.
        DebtIdentificationService.Tally t = svc().identify("100", "tester");
        assertEquals(4, t.created);
        assertEquals(0, t.skipped);
        assertEquals(4, saved.get("cmCase").size());
        assertEquals(4, saved.get("dmDebt").size());
        assertEquals(4, saved.get("dmLine").size()); // 1 VAT + 1 CIT line per TIN × 2
        assertEquals(4, started.size());
        // the first TIN's two cases: a VAT-pinned case and a consolidated (blank tax) case
        FormRow caseVat = saved.get("cmCase").get(0);   // consolidated bucket is added when CIT seen; VAT seen first
        assertEquals("VAT", caseVat.getProperty("taxType"));
        assertEquals("3000", caseVat.getProperty("amountAtStake")); // the VAT line enforceable
        FormRow caseCons = saved.get("cmCase").get(1);
        assertEquals("", caseCons.getProperty("taxType"));          // consolidated → no single tax
        assertEquals("2000", caseCons.getProperty("amountAtStake")); // the CIT line enforceable
        // the governing workflow is recorded on each dmDebt at birth
        assertEquals("W-VAT", saved.get("dmDebt").get(0).getProperty("workflowCode"));
        assertEquals("W-DEFAULT", saved.get("dmDebt").get(1).getProperty("workflowCode"));
        // dmDebt shares the case id (1:1)
        assertEquals(caseVat.getId(), saved.get("dmDebt").get(0).getId());
    }

    @Test
    public void dedupSkipsTinWithOpenDmCase() {
        FormRow ex = new FormRow();
        ex.setProperty("caseType", "DM");
        ex.setProperty("tin", "100010A");
        ex.setProperty("currentState", "OPEN");
        existingDm.add(ex);
        DebtIdentificationService.Tally t = svc().identify("100", "tester");
        assertEquals(2, t.created);   // only 100091L → its VAT + consolidated cases
        assertEquals(1, t.skipped);   // 100010A already open
        assertEquals(2, started.size());
    }

    @Test
    public void outageCreatesNothing() {
        scanner = new GoldMartScanner(new GoldMartScanner.ScanGateway() {
            public List<GoldMartScanner.GoldDebtor> scan(String m) throws Exception {
                throw new RuntimeException("clickhouse down");
            }

            public List<GoldMartScanner.GoldLine> lines(String tin) {
                return new ArrayList<GoldMartScanner.GoldLine>();
            }
        });
        DebtIdentificationService.Tally t = svc().identify("100", "tester");
        assertEquals(0, t.created);
        assertEquals(0, t.failed);
    }

    private static FormRow wf(String code, String tenant, String taxType, String floor) {
        FormRow r = new FormRow();
        r.setProperty("code", code);
        r.setProperty("tenant", tenant);
        r.setProperty("taxType", taxType);
        r.setProperty("segment", "");
        r.setProperty("industry", "");
        r.setProperty("categoryFloor", floor);
        r.setProperty("validFrom", "2020-01-01");
        r.setProperty("validTo", "");
        r.setProperty("status", "Active");
        r.setProperty("version", "1");
        return r;
    }

    private static GoldMartScanner.GoldDebtor debtor(String tin, String cat, String tot) {
        GoldMartScanner.GoldDebtor d = new GoldMartScanner.GoldDebtor();
        d.tin = tin;
        d.debtCategory = cat;
        d.totalEnforceable = tot;
        return d;
    }

    private static GoldMartScanner.GoldLine line(String tt, String amt) {
        GoldMartScanner.GoldLine l = new GoldMartScanner.GoldLine();
        l.taxType = tt;
        l.amount = amt;
        l.enforceable = amt;
        return l;
    }
}
