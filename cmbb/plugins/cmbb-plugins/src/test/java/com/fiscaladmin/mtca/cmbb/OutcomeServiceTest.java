package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.GoldMartClient;
import com.fiscaladmin.mtca.cmbb.service.OutcomeService;

/** OutcomeService — I-2 idempotent writeback + queue-retry (CMBB-F09, T-09.3/04/06 unit). */
public class OutcomeServiceTest {

    private FormDataDao dao;
    private GoldMartClient gold;
    private final List<FormRow> cases = new ArrayList<FormRow>();

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        gold = new GoldMartClient(dao, tin -> {
            GoldMartClient.GoldProfile p = new GoldMartClient.GoldProfile();
            p.enforceableBalance = "500.00";
            p.debtCategory = "C4";
            p.asaConfidence = "VERIFIED";
            p.asOf = "2026-06-12T10:00:00";
            return p;
        });
        cases.clear();
        cases.add(closedCase("c-1"));
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> dispatch(inv.getArgument(0)));
        // no ledger row yet by default
        when(dao.load(eq("cmOutcome"), eq("cmOutcome"), anyString())).thenReturn(null);
    }

    private FormRow closedCase(String id) {
        FormRow c = new FormRow();
        c.setId(id);
        c.setProperty("caseType", "TEST");
        c.setProperty("tin", "100058G");
        c.setProperty("taxType", "VAT");
        c.setProperty("currentState", "CLOSED");
        c.setProperty("assignee", "officer1");
        c.setProperty("amountAtStake", "500");
        return c;
    }

    private FormRowSet dispatch(String form) {
        FormRowSet out = new FormRowSet();
        if ("mmState".equals(form)) {
            FormRow s = new FormRow();
            s.setProperty("code", "CLOSED");
            s.setProperty("isTerminal", "true");
            out.add(s);
        } else if ("cmCase".equals(form)) {
            out.addAll(cases);
        } // cmDecision -> empty (RESOLVED); cmOutcome QUEUED scan -> empty
        return out;
    }

    @Test
    public void shipsClosedCaseLiveEnriched() {
        List<OutcomeService.OutcomeRecord> shipped = new ArrayList<OutcomeService.OutcomeRecord>();
        OutcomeService svc = new OutcomeService(dao, shipped::add, gold);
        OutcomeService.Tally t = svc.shipClosed("tester");
        assertEquals(1, t.shipped);
        assertEquals(0, t.queued);
        assertEquals(1, shipped.size());
        assertEquals("500.00", shipped.get(0).amount); // Gold enforceable balance, not amountAtStake
        assertEquals("RESOLVED", shipped.get(0).outcomeCode);
        assertEquals("RESOLUTION", shipped.get(0).outcomeType);
        assertEquals("100058G", shipped.get(0).tin);
    }

    @Test
    public void clickHouseOutageQueues() {
        OutcomeService.OutcomeGateway failing = r -> {
            throw new RuntimeException("clickhouse unreachable");
        };
        OutcomeService svc = new OutcomeService(dao, failing, gold);
        OutcomeService.Tally t = svc.shipClosed("tester");
        assertEquals(0, t.shipped);
        assertEquals(1, t.queued);
    }

    @Test
    public void alreadyShippedIsSkipped() {
        FormRow ledger = new FormRow();
        ledger.setId("c-1-RESOLVED");
        ledger.setProperty("shipStatus", "SHIPPED");
        when(dao.load("cmOutcome", "cmOutcome", "c-1-RESOLVED")).thenReturn(ledger);
        List<OutcomeService.OutcomeRecord> shipped = new ArrayList<OutcomeService.OutcomeRecord>();
        OutcomeService svc = new OutcomeService(dao, shipped::add, gold);
        OutcomeService.Tally t = svc.shipClosed("tester");
        assertEquals(0, t.shipped);
        assertEquals(0, shipped.size());
    }
}
