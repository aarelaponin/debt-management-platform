package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.phase.ClosePhase;
import com.fiscaladmin.mtca.cmbb.phase.PreClosePhase;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;

/** PRE_CLOSE and CLOSE phases: T-02.2 (invalid transition rejected), closure flow. */
public class ClosurePhasesTest {

    private GuardTestHarness h;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        h.seedTestType();
        h.caseRow.setProperty("currentState", "OPEN");
        h.caseRow.setProperty("caseRef", "TT-000001");
    }

    @Test
    public void preCloseValidatesPathWithoutMovingMinimalLifecycle() {
        new PreClosePhase().run(h.ctx(false));
        // TEST has no PendingClosure state: path validated, no move, no event
        assertEquals("OPEN", h.caseRow.getProperty("currentState"));
        assertEquals(0, h.events.size());
    }

    @Test
    public void preCloseRejectedWhenClosureTransitionRemoved() { // T-02.2
        h.transitions.removeIf(t -> "OPEN".equals(t[1]) && "CLOSED".equals(t[2]));
        try {
            new PreClosePhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("no configured closure transition"));
        }
        assertEquals("OPEN", h.caseRow.getProperty("currentState")); // unchanged
    }

    @Test
    public void preCloseRejectedWithOpenTasks() {
        h.openTasks = 2;
        try {
            new PreClosePhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("open task"));
        }
    }

    @Test
    public void preCloseMovesToPendingClosureWhenConfigured() {
        h.states.add(GuardTestHarness.row("caseType", "TEST", "code", "PENDING",
                "envelopeState", "PendingClosure"));
        h.transitions.add(new String[]{"TEST", "OPEN", "PENDING"});
        new PreClosePhase().run(h.ctx(false));
        assertEquals("PENDING", h.caseRow.getProperty("currentState"));
        FormRow ev = h.events.get(h.events.size() - 1);
        assertEquals("STATE_CHANGED", ev.getProperty("eventType"));
    }

    @Test
    public void closeMovesToTerminalAndWritesCaseClosed() {
        new ClosePhase().run(h.ctx(false));
        assertEquals("CLOSED", h.caseRow.getProperty("currentState"));
        FormRow ev = h.events.get(h.events.size() - 1);
        assertEquals("CASE_CLOSED", ev.getProperty("eventType"));
        assertTrue(ev.getProperty("payload").contains("\"prevState\":\"OPEN\""));
        assertTrue(ev.getProperty("payload").contains("\"newState\":\"CLOSED\""));
    }

    @Test
    public void closeRejectedWithoutTerminalPath() {
        h.transitions.removeIf(t -> "CLOSED".equals(t[2]));
        try {
            new ClosePhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("terminal"));
        }
    }

    @Test
    public void closeRejectedWhenDecisionRequiredButNoneApproved() { // F08
        try {
            new ClosePhase().run(h.ctx(true)); // requireDecision=true, no APPROVED decision
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("decision"));
        }
        assertEquals("OPEN", h.caseRow.getProperty("currentState")); // unchanged
    }

    @Test
    public void closeSucceedsWhenDecisionRequiredAndApproved() { // F08 / T-08.4
        FormRow d = GuardTestHarness.row("caseId", "case-1", "decisionStatus", "APPROVED");
        d.setId("dec-1");
        h.decisionRows.put("dec-1", d);
        new ClosePhase().run(h.ctx(true));
        assertEquals("CLOSED", h.caseRow.getProperty("currentState"));
        FormRow ev = h.events.get(h.events.size() - 1);
        assertEquals("CASE_CLOSED", ev.getProperty("eventType"));
    }

    @Test
    public void closeRejectedWhenTypeRequiresDecision() { // F08 per-type flag
        h.typeRow.setProperty("requireDecision", "true");
        try {
            new ClosePhase().run(h.ctx(false)); // plugin property false; type flag true
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("decision"));
        }
        assertEquals("OPEN", h.caseRow.getProperty("currentState"));
    }

    @Test
    public void closeIsIdempotentWhenAlreadyTerminal() {
        h.caseRow.setProperty("currentState", "CLOSED");
        new ClosePhase().run(h.ctx(false));
        assertEquals("CLOSED", h.caseRow.getProperty("currentState"));
        assertEquals(0, h.events.size());
    }
}
