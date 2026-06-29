package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DeadlineService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/** DeadlineEngine core — WF-FR-010..012 at unit level (T-05 logic + U-05 date math). */
public class DeadlineServiceTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 6, 1, 9, 0, 0);
    private GuardTestHarness h;
    private DeadlineService svc;
    private FormRow c;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        h.seedTestType();
        h.caseRow = null;
        h.slaRows.add(GuardTestHarness.row("caseType", "TEST", "clockCode", "RES",
                "durationDays", "10", "warnPct", "75", "critPct", "90",
                "calendar", "", "pauseOnHold", "true",
                "escalationChain", "{\"notify\":[\"assignee\",\"supervisor\"],\"bumpPriority\":true,\"maxLevels\":3}"));
        c = GuardTestHarness.row("caseType", "TEST", "tin", "100058G", "currentState", "OPEN",
                "caseRef", "TT-000001", "assignee", "officer1", "taxpayerName", "Alpha Ltd",
                "amountAtStake", "500", "priority", "0", "slaStatus", "");
        c.setId("d-1");
        h.cases.put("d-1", c);
        svc = new DeadlineService(h.dao, new MmConfigService(h.dao), new CaseEventWriter(h.dao));
    }

    @Test
    public void startCreatesClockFromConfig() { // T-05.1
        assertEquals(1, svc.start("d-1", "tester", T0));
        assertEquals(1, h.deadlines.size());
        FormRow d = h.deadlines.values().iterator().next();
        assertEquals("RUNNING", d.getProperty("status"));
        assertEquals("2026-06-11T09:00:00", d.getProperty("dueAt")); // +10 calendar days
        assertEquals("GREEN", c.getProperty("slaStatus"));
        // idempotent
        assertEquals(0, svc.start("d-1", "tester", T0));
    }

    @Test
    public void trafficLightsAcrossThresholds() { // T-05.2 / WF-FR-011
        svc.start("d-1", "tester", T0);
        svc.sweep("tester", T0.plusDays(5));
        assertEquals("GREEN", c.getProperty("slaStatus"));
        svc.sweep("tester", T0.plusDays(8)); // 80% >= 75 warn
        assertEquals("AMBER", c.getProperty("slaStatus"));
        svc.sweep("tester", T0.plusDays(9).plusHours(13)); // >= 90% crit
        assertEquals("RED", c.getProperty("slaStatus"));
    }

    @Test
    public void escalationAtCritThenBreachOnceEach() { // T-05.2 / WF-FR-012
        svc.start("d-1", "tester", T0);
        svc.sweep("tester", T0.plusDays(9).plusHours(13)); // crit -> L1
        FormRow d = h.deadlines.values().iterator().next();
        assertEquals("1", d.getProperty("escalationLevel"));
        long l1Notices = h.events.stream().filter(e ->
                "NOTIF_PENDING".equals(e.getProperty("eventType"))).count();
        assertEquals(2, l1Notices); // assignee + supervisor
        assertTrue(h.events.stream().anyMatch(e ->
                "SLA_ESCALATED".equals(e.getProperty("eventType"))));
        assertEquals("1", c.getProperty("priority")); // bumped

        svc.sweep("tester", T0.plusDays(11)); // breach -> L2 + BREACHED
        assertEquals("BREACHED", d.getProperty("status"));
        assertEquals("2", d.getProperty("escalationLevel"));
        assertTrue(h.events.stream().anyMatch(e ->
                "SLA_BREACHED".equals(e.getProperty("eventType"))));
        assertEquals("RED", c.getProperty("slaStatus"));

        int eventsBefore = h.events.size();
        svc.sweep("tester", T0.plusDays(12)); // idempotent — fired exactly once
        long extraEscalations = h.events.stream().skip(eventsBefore).filter(e ->
                e.getProperty("eventType").startsWith("SLA_")).count();
        assertEquals(0, extraEscalations);
        assertEquals("RED", c.getProperty("slaStatus")); // persists red (WF-FR-011)
    }

    @Test
    public void pauseOnHoldAndResumeExtendsDue() { // T-05.3 / WF-FR-010
        h.states.add(GuardTestHarness.row("caseType", "TEST", "code", "HOLD",
                "envelopeState", "OnHold"));
        svc.start("d-1", "tester", T0);
        c.setProperty("currentState", "HOLD");
        svc.sweep("tester", T0.plusDays(2));
        FormRow d = h.deadlines.values().iterator().next();
        assertEquals("PAUSED", d.getProperty("status"));

        svc.sweep("tester", T0.plusDays(4)); // still on hold — stays paused, no escalation
        assertEquals("PAUSED", d.getProperty("status"));

        c.setProperty("currentState", "OPEN");
        svc.sweep("tester", T0.plusDays(5)); // resume after 3 paused days
        assertEquals("RUNNING", d.getProperty("status"));
        assertEquals("2026-06-14T09:00:00", d.getProperty("dueAt")); // 11th + 3d
        assertTrue(h.events.stream().anyMatch(e ->
                "SLA_RESUMED".equals(e.getProperty("eventType"))));
    }

    @Test
    public void closedCaseClocksMet() {
        svc.start("d-1", "tester", T0);
        assertEquals(1, svc.close("d-1", "tester"));
        assertEquals("MET", h.deadlines.values().iterator().next().getProperty("status"));
        assertEquals("-", c.getProperty("slaStatus"));
    }

    @Test
    public void workingDayCalendarSkipsWeekendsAndHolidays() { // U-05
        FormRow cal = GuardTestHarness.row("code", "MLT", "workingDayMode", "WORKING",
                "holidays", "2026-06-08");
        // Mon 1 Jun 09:00 + 5 working days: Tue2,Wed3,Thu4,Fri5,(skip Sat6,Sun7,hol Mon8),Tue9
        LocalDateTime due = DeadlineService.addDays(LocalDateTime.of(2026, 6, 1, 9, 0), 5, cal);
        assertEquals(LocalDateTime.of(2026, 6, 9, 9, 0), due);
        // calendar mode: straight addition
        assertEquals(LocalDateTime.of(2026, 6, 6, 9, 0),
                DeadlineService.addDays(LocalDateTime.of(2026, 6, 1, 9, 0), 5, null));
    }

    @Test
    public void seniorReassignmentWhenConfigured() { // WF-FR-012 optional action
        h.slaRows.get(0).setProperty("escalationChain",
                "{\"notify\":[\"assignee\"],\"bumpPriority\":false,\"reassignTo\":\"senior1\",\"maxLevels\":3}");
        svc.start("d-1", "tester", T0);
        svc.sweep("tester", T0.plusDays(11)); // breach
        assertEquals("senior1", c.getProperty("assignee"));
        assertEquals("0", c.getProperty("priority")); // bump disabled
        assertTrue(h.events.stream().anyMatch(e ->
                "CASE_REASSIGNED".equals(e.getProperty("eventType"))
                        && e.getProperty("payload").contains("senior1")));
    }
}
