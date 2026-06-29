package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DecisionService;

/** DecisionService — CMBB-F08 authority gate + collegial quorum (T-08.4/08.5 unit). */
public class DecisionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 10, 0);
    private GuardTestHarness h;
    private DecisionService svc;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        svc = new DecisionService(h.dao, new CaseEventWriter(h.dao));
        h.authorities.add(GuardTestHarness.row("actionType", "CLOSE_CASE",
                "amountMin", "0", "amountMax", "", "level", "OFFICER", "quorum", ""));
        h.authorities.add(GuardTestHarness.row("actionType", "WRITE_OFF",
                "amountMin", "0", "amountMax", "5000", "level", "SUPERVISOR", "quorum", ""));
        h.authorities.add(GuardTestHarness.row("actionType", "WRITE_OFF",
                "amountMin", "5000.01", "amountMax", "", "level", "DIRECTOR", "quorum", "2"));
    }

    private long eventCount(String type) {
        return h.events.stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private FormRow seedDecision(String id, String action, String amount,
                                 String level, String body, String approvals, String reasons) {
        FormRow d = GuardTestHarness.row("caseId", "case-1", "caseRef", "TT-1", "tin", "T",
                "actionType", action, "amount", amount, "approverLevel", level,
                "bodyType", body, "approvalsCount", approvals, "reasons", reasons, "result", "");
        d.setId(id);
        h.decisionRows.put(id, d);
        return d;
    }

    @Test
    public void officerApprovesCaseClosure() { // T-08.4
        FormRow d = seedDecision("d-1", "CLOSE_CASE", "0", "OFFICER", "SINGLE", "", "ok to close");
        String r = svc.decide("d-1", "tester", NOW);
        assertTrue(r.startsWith("APPROVED"));
        assertEquals("APPROVED", d.getProperty("decisionStatus"));
        assertEquals(1, eventCount("DECISION_PROPOSED"));
        assertEquals(1, eventCount("DECISION_APPROVED"));
        assertTrue(svc.hasApprovedDecision("case-1"));
    }

    @Test
    public void insufficientAuthorityRejected() { // T-08.5
        FormRow d = seedDecision("d-1", "WRITE_OFF", "50000", "SUPERVISOR", "SINGLE", "", "write off");
        String r = svc.decide("d-1", "tester", NOW);
        assertTrue(r.contains("below required DIRECTOR"));
        assertEquals("REJECTED", d.getProperty("decisionStatus"));
        assertEquals(1, eventCount("DECISION_REJECTED"));
        assertFalse(svc.hasApprovedDecision("case-1"));
    }

    @Test
    public void collegialQuorumEnforced() { // DPM D6
        seedDecision("d-1", "WRITE_OFF", "50000", "DIRECTOR", "COLLEGIAL", "1", "panel");
        assertTrue(svc.decide("d-1", "tester", NOW).contains("quorum"));
        assertFalse(svc.hasApprovedDecision("case-1"));

        seedDecision("d-2", "WRITE_OFF", "50000", "DIRECTOR", "COLLEGIAL", "2", "panel");
        assertTrue(svc.decide("d-2", "tester", NOW).startsWith("APPROVED"));
        assertTrue(svc.hasApprovedDecision("case-1"));
    }

    @Test
    public void reasonsMandatory() {
        FormRow d = seedDecision("d-1", "CLOSE_CASE", "0", "OFFICER", "SINGLE", "", "");
        assertTrue(svc.decide("d-1", "tester", NOW).contains("mandatory"));
        assertEquals("REJECTED", d.getProperty("decisionStatus"));
    }

    @Test
    public void alreadyProcessedIsNoOp() {
        FormRow d = seedDecision("d-1", "CLOSE_CASE", "0", "OFFICER", "SINGLE", "", "ok");
        svc.decide("d-1", "tester", NOW);
        long before = h.events.size();
        assertTrue(svc.decide("d-1", "tester", NOW).contains("no-op"));
        assertEquals(before, h.events.size());
    }
}
