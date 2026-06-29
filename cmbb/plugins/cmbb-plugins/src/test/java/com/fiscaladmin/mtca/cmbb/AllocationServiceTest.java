package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.AllocationService;
import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/** AllocationEngine core — T-03.1..6 logic at unit level (WF-FR-007/008). */
public class AllocationServiceTest {

    private GuardTestHarness h;
    private AllocationService svc;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        h.seedTestType();
        h.caseRow = null; // allocation tests use the cases map exclusively
        h.officers.add(GuardTestHarness.row("code", "officer1", "name", "Officer One",
                "unit", "UNIT-A", "taxTypes", "VAT;IT", "maxCapacity", "2", "active", "true"));
        h.officers.add(GuardTestHarness.row("code", "officer2", "name", "Officer Two",
                "unit", "UNIT-B", "taxTypes", "VAT", "maxCapacity", "2", "active", "true"));
        h.allocPolicies.add(GuardTestHarness.row("caseType", "TEST", "code", "TEST-DEFAULT",
                "criteria", "{\"strategy\":\"ROUND_ROBIN\",\"filters\":[\"WORKLOAD\",\"SPECIALISATION\"],\"warnPct\":90}"));
        svc = new AllocationService(h.dao, new MmConfigService(h.dao), new CaseEventWriter(h.dao));
    }

    private FormRow newCase(String id, String tin) {
        FormRow c = GuardTestHarness.row("caseType", "TEST", "tin", tin, "origin", "MANUAL",
                "currentState", "OPEN", "caseRef", "TT-" + id, "taxType", "VAT", "assignee", "");
        c.setId(id);
        h.cases.put(id, c);
        return c;
    }

    @Test
    public void roundRobinAlternatesAndLogs() { // T-03.1
        newCase("a1", "100058G");
        newCase("a2", "100059H");
        String first = svc.assign("a1", "tester");
        String second = svc.assign("a2", "tester");
        assertEquals("officer1", first);
        assertEquals("officer2", second);
        assertEquals("ASSIGNED", h.cases.get("a1").getProperty("assignmentStatus"));
        long assignedEvents = h.events.stream()
                .filter(e -> "CASE_ASSIGNED".equals(e.getProperty("eventType"))).count();
        assertEquals(2, assignedEvents);
    }

    @Test
    public void officerAtCapacitySkipped() { // T-03.2 / BR-WF-005
        // officer1 already holds 2 open cases (capacity 2)
        for (int i = 0; i < 2; i++) {
            FormRow busy = newCase("busy" + i, "1000" + i);
            busy.setProperty("assignee", "officer1");
        }
        newCase("c3", "100061K");
        assertEquals("officer2", svc.assign("c3", "tester"));
    }

    @Test
    public void capacityWarningAtThreshold() { // BR-WF-005 90% alert
        h.officers.get(0).setProperty("maxCapacity", "1");
        h.officers.get(1).setProperty("active", "");
        newCase("w1", "100058G");
        assertEquals("officer1", svc.assign("w1", "tester"));
        assertTrue(h.events.stream()
                .anyMatch(e -> "CAPACITY_WARNING".equals(e.getProperty("eventType"))));
    }

    @Test
    public void unassignableRoutedToSupervisorQueue() { // T-03.3
        for (FormRow o : h.officers) {
            o.setProperty("maxCapacity", "0");
        }
        FormRow c = newCase("u1", "100062L");
        assertNull(svc.assign("u1", "tester"));
        assertEquals("UNASSIGNED", c.getProperty("assignmentStatus"));
        assertEquals("", c.getProperty("assignee"));
        assertTrue(h.events.stream()
                .anyMatch(e -> "ASSIGNMENT_FAILED".equals(e.getProperty("eventType"))));
        assertTrue(h.events.stream()
                .anyMatch(e -> "NOTIF_PENDING".equals(e.getProperty("eventType"))
                        && e.getProperty("payload").contains("SUPERVISOR_UNASSIGNED")));
    }

    @Test
    public void coiUnitExclusionHonoured() { // T-03.6
        h.coiRules.add(GuardTestHarness.row("caseType", "TEST",
                "ruleType", "EXCLUDE_UNIT", "expression", "UNIT-A"));
        newCase("k1", "100063M");
        assertEquals("officer2", svc.assign("k1", "tester"));
    }

    @Test
    public void decisionMakerExcludedAtAllocation() { // T-08.7 / GCMF §3.3-3 (F08)
        h.coiRules.add(GuardTestHarness.row("caseType", "TEST",
                "ruleType", "EXCLUDE_DECISION_MAKER", "expression", ""));
        FormRow d = GuardTestHarness.row("tin", "100070P", "decidedBy", "officer1",
                "decisionStatus", "APPROVED");
        d.setId("dec-1");
        h.decisionRows.put("dec-1", d);
        newCase("dm1", "100070P");
        assertEquals("officer2", svc.assign("dm1", "tester")); // officer1 (decision-maker) excluded
    }

    @Test
    public void manualPreAssignmentRespected() {
        FormRow c = newCase("m1", "100064N");
        c.setProperty("assignee", "officer2");
        assertEquals("officer2", svc.assign("m1", "tester"));
        assertTrue(h.events.stream()
                .anyMatch(e -> e.getProperty("payload").contains("\"policy\":\"MANUAL\"")));
    }

    @Test
    public void singleReassignWithAuditAndNotices() { // T-03.4
        FormRow c = newCase("r1", "100058G");
        c.setProperty("assignee", "officer1");
        c.setProperty("caseRef", "TT-000001");
        FormRow order = GuardTestHarness.row("caseRef", "TT-000001",
                "toOfficer", "officer2", "reason", "leave coverage");
        order.setId("ord-1");
        h.reassignOrders.put("ord-1", order);

        String result = svc.reassign("ord-1", "supervisor");

        assertEquals("1 case(s) reassigned", result);
        assertEquals("officer2", c.getProperty("assignee"));
        FormRow reassigned = h.events.stream()
                .filter(e -> "CASE_REASSIGNED".equals(e.getProperty("eventType")))
                .findFirst().orElseThrow();
        String payload = reassigned.getProperty("payload");
        assertTrue(payload.contains("\"from\":\"officer1\""));
        assertTrue(payload.contains("\"to\":\"officer2\""));
        assertTrue(payload.contains("leave coverage"));
        long notices = h.events.stream()
                .filter(e -> "NOTIF_PENDING".equals(e.getProperty("eventType"))).count();
        assertEquals(2, notices); // original + new assignee (WF-FR-008)
    }

    @Test
    public void reassignWithoutReasonRejected() { // BR-WF-006
        FormRow order = GuardTestHarness.row("caseRef", "TT-000001", "toOfficer", "officer2",
                "reason", "");
        order.setId("ord-2");
        h.reassignOrders.put("ord-2", order);
        assertTrue(svc.reassign("ord-2", "supervisor").startsWith("REJECTED"));
        assertEquals(0, h.events.size());
    }

    @Test
    public void bulkRedistributionRespectsCapacity() { // T-03.5 / BR-WF-007
        for (int i = 0; i < 2; i++) {
            FormRow c = newCase("b" + i, "10007" + i);
            c.setProperty("assignee", "officer1");
        }
        FormRow order = GuardTestHarness.row("filterOfficer", "officer1",
                "filterCaseType", "TEST", "toOfficer", "", "reason", "org change");
        order.setId("ord-3");
        h.reassignOrders.put("ord-3", order);

        String result = svc.reassign("ord-3", "supervisor");

        assertEquals("2 case(s) reassigned", result);
        assertEquals("officer2", h.cases.get("b0").getProperty("assignee"));
        assertEquals("officer2", h.cases.get("b1").getProperty("assignee"));
    }
}
