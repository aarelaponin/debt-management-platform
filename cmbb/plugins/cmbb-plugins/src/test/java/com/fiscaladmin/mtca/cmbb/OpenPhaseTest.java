package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.phase.OpenPhase;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;

/** OPEN phase: T-02.1 (creation, caseRef, dedup), T-02.4 (TTT scope), T-02.5 (re-open). */
public class OpenPhaseTest {

    private GuardTestHarness h;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        h.seedTestType();
    }

    @Test
    public void happyPathAssignsRefStateAndTwoChainedEvents() {
        new OpenPhase().run(h.ctx(false));

        assertEquals("TT-000001", h.caseRow.getProperty("caseRef"));
        assertEquals("OPEN", h.caseRow.getProperty("currentState"));
        assertEquals(2, h.events.size());
        FormRow created = h.events.get(0);
        FormRow opened = h.events.get(1);
        assertEquals("CASE_CREATED", created.getProperty("eventType"));
        assertEquals("CASE_OPENED", opened.getProperty("eventType"));
        assertEquals("", created.getProperty("prevHash"));                       // genesis
        assertEquals(created.getProperty("hash"), opened.getProperty("prevHash")); // chain
    }

    @Test
    public void obligationScopeWithoutTaxTypeRejected() {
        h.typeRow.setProperty("ttScope", "OBLIGATION");
        try {
            new OpenPhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("tax type and period required"));
        }
        assertEquals(0, h.events.size()); // phase writes nothing on rejection
    }

    @Test
    public void activeDuplicateRejectedUnderSkipIfActive() {
        FormRow dup = GuardTestHarness.row("caseType", "TEST", "tin", "100058G",
                "currentState", "OPEN", "caseRef", "TT-000001");
        h.otherCases.add(dup);
        try {
            new OpenPhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("duplicate case"));
        }
    }

    @Test
    public void closedDuplicateDoesNotBlockCreation() {
        FormRow closedDup = GuardTestHarness.row("caseType", "TEST", "tin", "100058G",
                "currentState", "CLOSED", "caseRef", "TT-000001");
        h.otherCases.add(closedDup);
        new OpenPhase().run(h.ctx(false));
        assertEquals("TT-000002", h.caseRow.getProperty("caseRef"));
    }

    @Test
    public void missingTransitionRejected() {
        h.transitions.clear(); // no NEW->OPEN configured
        try {
            new OpenPhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("no configured transition"));
            assertEquals("NEW", e.getFromState());
            assertEquals("OPEN", e.getToState());
        }
    }

    @Test
    public void reopenLinksClosingEventAndCarriesCaseRef() {
        h.caseRow.setProperty("currentState", "CLOSED");
        h.caseRow.setProperty("caseRef", "TT-000001");
        h.transitions.add(new String[]{"TEST", "CLOSED", "OPEN"});
        FormRow closing = GuardTestHarness.row("caseId", "case-1",
                "eventType", "CASE_CLOSED", "hash", "abc123");
        h.events.add(closing);

        new OpenPhase().run(h.ctx(false));

        assertEquals("OPEN", h.caseRow.getProperty("currentState"));
        FormRow reopened = h.events.get(h.events.size() - 1);
        assertEquals("CASE_REOPENED", reopened.getProperty("eventType"));
        assertTrue(reopened.getProperty("payload").contains("TT-000001"));   // T-02.5
        assertTrue(reopened.getProperty("payload").contains("abc123"));      // link to closing event
    }

    @Test
    public void reopenWithoutConfiguredTransitionRejected() {
        h.caseRow.setProperty("currentState", "CLOSED");
        try {
            new OpenPhase().run(h.ctx(false));
            fail("expected rejection");
        } catch (InvalidTransitionException e) {
            assertTrue(e.getMessage().contains("re-open not permitted"));
        }
    }
}
