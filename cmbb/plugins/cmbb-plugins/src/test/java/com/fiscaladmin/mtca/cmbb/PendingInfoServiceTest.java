package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;
import com.fiscaladmin.mtca.cmbb.service.PendingInfoService;

/** PendingInfoService — CMBB-F08 park/resume loop (T-08.8 unit). */
public class PendingInfoServiceTest {

    private GuardTestHarness h;
    private PendingInfoService svc;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        svc = new PendingInfoService(h.dao, new MmConfigService(h.dao), new CaseEventWriter(h.dao));
        h.states.add(GuardTestHarness.row("caseType", "TEST", "code", "AWAIT_INFO",
                "envelopeState", "OnHold"));
        FormRow c = GuardTestHarness.row("caseType", "TEST", "caseRef", "TT-1",
                "currentState", "INPROGRESS", "tin", "T");
        c.setId("case-1");
        h.cases.put("case-1", c);
    }

    private long eventCount(String type) {
        return h.events.stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    @Test
    public void requestParksThenResponseResumes() { // T-08.8
        FormRow req = GuardTestHarness.row("caseId", "case-1", "caseRef", "TT-1",
                "infoNeeded", "bank statements", "result", "");
        req.setId("req-1");
        h.infoRequests.put("req-1", req);

        String r1 = svc.request("req-1", "tester");
        assertTrue(r1.contains("REQUESTED"));
        assertEquals("AWAIT_INFO", h.cases.get("case-1").getProperty("currentState"));
        assertEquals("INPROGRESS", req.getProperty("priorState"));
        assertEquals(1, h.tasks.size());
        FormRow task = h.tasks.values().iterator().next();
        assertEquals("PROVIDE_INFO", task.getProperty("taskType"));
        assertEquals("OPEN", task.getProperty("status"));
        assertEquals(1, eventCount("INFO_REQUESTED"));
        assertEquals(1, eventCount("NOTIF_PENDING"));

        FormRow resp = GuardTestHarness.row("requestId", "req-1",
                "responseSummary", "provided", "result", "");
        resp.setId("resp-1");
        h.infoResponses.put("resp-1", resp);

        String r2 = svc.respond("resp-1", "tester");
        assertTrue(r2.contains("RESUMED"));
        assertEquals("INPROGRESS", h.cases.get("case-1").getProperty("currentState"));
        assertEquals("CLOSED", task.getProperty("status"));
        assertEquals("RESPONDED", req.getProperty("status"));
        assertEquals(1, eventCount("INFO_RECEIVED"));
    }
}
