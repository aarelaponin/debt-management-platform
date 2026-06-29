package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DispatchService;

/** NotificationDispatcher core — WF-FR-013..016 at unit level. */
public class DispatchServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 10, 0);
    private GuardTestHarness h;
    private DispatchService svc;
    private FormRow c;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        h.seedTestType();
        h.caseRow = null;
        c = GuardTestHarness.row("caseType", "TEST", "tin", "100058G",
                "taxpayerName", "Alpha Ltd", "caseRef", "TT-000001",
                "amountAtStake", "500", "currentState", "OPEN", "priority", "1");
        c.setId("n-1");
        h.cases.put("n-1", c);
        h.officers.add(GuardTestHarness.row("code", "officer1", "name", "Officer One",
                "active", "true", "alertPrefs", ""));
        svc = new DispatchService(h.dao);
    }

    private FormRow pendingEvent(String recipient, String reason) {
        new CaseEventWriter(h.dao).append("n-1", "NOTIF_PENDING", "tester", "", "",
                reason, "\"recipient\":\"" + recipient + "\"");
        return h.events.get(h.events.size() - 1);
    }

    @Test
    public void internalAlertCreatedAndIdempotent() { // T-06.1 / WF-FR-015/016
        pendingEvent("officer1", "new case assignment");
        String r1 = svc.dispatch("tester", NOW);
        assertEquals(1, h.alerts.size());
        FormRow a = h.alerts.get(0);
        assertEquals("officer1", a.getProperty("recipient"));
        assertEquals("", a.getProperty("isRead")); // unread
        assertTrue(r1.contains("alerts=1"));

        String r2 = svc.dispatch("tester", NOW); // idempotent
        assertEquals(1, h.alerts.size());
        assertTrue(r2.contains("skippedHandled=1"));
    }

    @Test
    public void officerPreferenceSuppressesAlert() { // WF-FR-015 preferences
        h.officers.get(0).setProperty("alertPrefs", "SLA escalation");
        pendingEvent("officer1", "SLA escalation notice");
        svc.dispatch("tester", NOW);
        assertEquals(1, h.alerts.size()); // marker row, pre-read
        assertEquals("true", h.alerts.get(0).getProperty("isRead"));
        assertTrue(h.alerts.get(0).getProperty("message").contains("suppressed"));
    }

    @Test
    public void templateRenderWithMergeFieldsAndVersioning() { // T-06.2 / WF-FR-014
        h.templates.add(GuardTestHarness.row("code", "TPL-REMIND", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "2026-01-01",
                "subject", "OLD #caseRef#"));
        h.templates.add(GuardTestHarness.row("code", "TPL-REMIND", "language", "en",
                "version", "2", "active", "true", "effectiveFrom", "2026-06-01",
                "subject", "Reminder #caseRef# for #taxpayerName#: EUR #amountAtStake# (#date#)"));
        h.templates.add(GuardTestHarness.row("code", "TPL-REMIND", "language", "en",
                "version", "3", "active", "true", "effectiveFrom", "2027-01-01",
                "subject", "FUTURE")); // not yet effective
        h.notifRules.add(GuardTestHarness.row("caseType", "TEST",
                "eventType", "NOTIF_PENDING", "template", "TPL-REMIND",
                "channelDefault", "EMAIL"));
        pendingEvent("", "reminder");
        svc.dispatch("tester", NOW);
        assertEquals(1, h.notifs.size());
        String subject = h.notifs.get(0).getProperty("summary");
        assertEquals("Reminder TT-000001 for Alpha Ltd: EUR 500 (2026-06-12)", subject);
        assertEquals("SENT", h.notifs.get(0).getProperty("status"));
    }

    @Test
    public void channelFallbackOnFailure() { // T-06.3 / WF-FR-013
        h.templates.add(GuardTestHarness.row("code", "TPL-X", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "",
                "subject", "S #caseRef#"));
        h.notifRules.add(GuardTestHarness.row("caseType", "TEST",
                "eventType", "NOTIF_PENDING", "template", "TPL-X",
                "channelDefault", "SIMFAIL,EMAIL"));
        pendingEvent("", "notice");
        svc.dispatch("tester", NOW);
        assertEquals(2, h.notifs.size()); // FAILED then SENT — every attempt logged
        assertEquals("FAILED", h.notifs.get(0).getProperty("status"));
        assertEquals("SIMFAIL", h.notifs.get(0).getProperty("channel"));
        assertEquals("SENT", h.notifs.get(1).getProperty("status"));
        assertEquals("EMAIL", h.notifs.get(1).getProperty("channel"));
    }

    @Test
    public void letterQueuedForPrint() { // WF-FR-013 postal
        h.templates.add(GuardTestHarness.row("code", "TPL-L", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "", "subject", "L"));
        h.notifRules.add(GuardTestHarness.row("caseType", "TEST",
                "eventType", "NOTIF_PENDING", "template", "TPL-L",
                "channelDefault", "LETTER"));
        pendingEvent("", "demand notice");
        svc.dispatch("tester", NOW);
        assertEquals("QUEUED_PRINT", h.notifs.get(0).getProperty("status"));
    }

    @Test
    public void outboundLogCarriesLinkageAndFields() { // T-06.4 / WF-FR-016
        h.templates.add(GuardTestHarness.row("code", "TPL-X", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "", "subject", "S"));
        h.notifRules.add(GuardTestHarness.row("caseType", "TEST",
                "eventType", "NOTIF_PENDING", "template", "TPL-X",
                "channelDefault", "EMAIL"));
        FormRow ev = pendingEvent("", "notice");
        svc.dispatch("tester", NOW);
        FormRow n = h.notifs.get(0);
        assertEquals("n-1", n.getProperty("caseId"));
        assertEquals(ev.getId(), n.getProperty("eventId"));
        assertEquals("100058G", n.getProperty("tin"));
        assertEquals("2026-06-12T10:00:00", n.getProperty("sentAt"));
    }
}
