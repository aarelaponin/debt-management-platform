package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.HoldService;

/** HoldService — CMBB-F08 assert/release + suppression scope (T-08.1/08.3 unit). */
public class HoldServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 10, 0);
    private GuardTestHarness h;
    private HoldService svc;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        svc = new HoldService(h.dao, new CaseEventWriter(h.dao));
    }

    private long eventCount(String type) {
        return h.events.stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private FormRow seedHold(String id, String scope, String status) {
        FormRow hold = GuardTestHarness.row("caseId", "case-1", "caseRef", "TT-1",
                "scope", scope, "holdType", "INFO_PENDING", "basis", "dispute",
                "targetBB", "CMBB", "status", status);
        hold.setId(id);
        h.holds.put(id, hold);
        return hold;
    }

    @Test
    public void assertActivatesAndSuppresses() {
        FormRow hold = seedHold("hold-1", "CORRESPONDENCE_SUPPRESS", "");
        String r = svc.assertHold("hold-1", "tester", NOW);
        assertTrue(r.contains("ACTIVE"));
        assertEquals("ACTIVE", hold.getProperty("status"));
        assertEquals("tester", hold.getProperty("assertedBy"));
        assertEquals(1, eventCount("HOLD_ASSERTED"));
        assertTrue(svc.hasActiveScope("case-1", "CORRESPONDENCE_SUPPRESS"));
    }

    @Test
    public void assertIsIdempotent() {
        seedHold("hold-1", "COLLECTION", "");
        svc.assertHold("hold-1", "tester", NOW);
        String r2 = svc.assertHold("hold-1", "tester", NOW);
        assertTrue(r2.contains("no-op"));
        assertEquals(1, eventCount("HOLD_ASSERTED"));
    }

    @Test
    public void releaseLiftsScope() {
        seedHold("hold-1", "CORRESPONDENCE_SUPPRESS", "ACTIVE");
        FormRow rel = GuardTestHarness.row("holdId", "hold-1", "caseId", "case-1",
                "releaseReason", "dispute resolved");
        rel.setId("rel-1");
        h.holdReleases.put("rel-1", rel);
        String r = svc.release("rel-1", "tester", NOW);
        assertTrue(r.contains("RELEASED"));
        assertEquals("RELEASED", h.holds.get("hold-1").getProperty("status"));
        assertEquals(1, eventCount("HOLD_RELEASED"));
        assertFalse(svc.hasActiveScope("case-1", "CORRESPONDENCE_SUPPRESS"));
    }

    @Test
    public void releaseUnknownHoldIsSafe() {
        FormRow rel = GuardTestHarness.row("holdId", "nope", "releaseReason", "x");
        rel.setId("rel-1");
        h.holdReleases.put("rel-1", rel);
        String r = svc.release("rel-1", "tester", NOW);
        assertTrue(r.contains("not found"));
        assertEquals(0, eventCount("HOLD_RELEASED"));
    }
}
