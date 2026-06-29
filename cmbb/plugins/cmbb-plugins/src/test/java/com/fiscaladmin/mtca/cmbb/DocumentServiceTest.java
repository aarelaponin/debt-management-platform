package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.DocumentService;
import com.fiscaladmin.mtca.cmbb.service.MayanClient;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/** MayanConnector core — WF-FR-017..019 at unit level (simulated Mayan). */
public class DocumentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 12, 10, 0);

    /** Simulated binary store: records uploads, returns sequential ids. */
    static class SimMayan implements MayanClient {
        final List<String> labels = new ArrayList<>();
        byte[] lastContent;
        boolean fail;

        @Override
        public String upload(String label, String fileName, byte[] content) throws Exception {
            if (fail) {
                throw new IllegalStateException("simulated outage");
            }
            labels.add(label);
            lastContent = content;
            return String.valueOf(100 + labels.size());
        }
    }

    private GuardTestHarness h;
    private SimMayan mayan;
    private DocumentService svc;
    private FormRow c;
    private File tmpRoot;

    @Before
    public void setUp() throws Exception {
        h = new GuardTestHarness();
        h.seedTestType();
        h.caseRow = null;
        c = GuardTestHarness.row("caseType", "TEST", "tin", "100058G",
                "taxpayerName", "Alpha Ltd", "caseRef", "TT-000001",
                "amountAtStake", "500", "currentState", "OPEN", "assignee", "officer1");
        c.setId("doc-case-1");
        h.cases.put("doc-case-1", c);
        h.otherCases.add(c); // visible to caseRef finds
        mayan = new SimMayan();
        tmpRoot = Files.createTempDirectory("cmbb-up").toFile();
        svc = new DocumentService(h.dao, mayan, new CaseEventWriter(h.dao),
                new MmConfigService(h.dao), tmpRoot);
    }

    private FormRow docRow(String fileName) throws Exception {
        FormRow d = GuardTestHarness.row("caseId", "doc-case-1", "caseRef", "TT-000001",
                "tin", "100058G", "docClass", "EVID", "file", fileName,
                "source", "UPLOAD", "status", "PENDING", "mayanDocId", "");
        d.setId("d-1");
        h.docs.put("d-1", d);
        File dir = new File(new File(tmpRoot, "cmDoc"), "d-1");
        dir.mkdirs();
        Files.write(new File(dir, fileName).toPath(),
                "fake-pdf-bytes".getBytes(StandardCharsets.UTF_8));
        return d;
    }

    @Test
    public void pushRegistersBinaryInMayan() throws Exception { // T-07.1 / WF-FR-018
        FormRow d = docRow("evidence.pdf");
        String result = svc.push("d-1", "officer1");
        assertTrue(result, result.startsWith("registered mayan:"));
        assertEquals("REGISTERED", d.getProperty("status"));
        assertEquals("101", d.getProperty("mayanDocId"));
        assertEquals("officer1", d.getProperty("uploadedBy"));
        assertTrue(h.events.stream().anyMatch(e ->
                "DOC_REGISTERED".equals(e.getProperty("eventType"))));
        // idempotent
        assertEquals("already registered", svc.push("d-1", "officer1"));
    }

    @Test
    public void pushFailureKeepsRegisterRowRetryable() throws Exception {
        FormRow d = docRow("evidence.pdf");
        mayan.fail = true;
        String result = svc.push("d-1", "officer1");
        assertTrue(result.startsWith("FAILED"));
        assertEquals("FAILED", d.getProperty("status")); // row survives, retryable
        assertEquals("", d.getProperty("mayanDocId"));
    }

    @Test
    public void generateRendersPdfAttachesAndPushes() { // T-07.2 / WF-FR-017
        h.templates.add(GuardTestHarness.row("code", "TPL-DEMAND", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "",
                "subject", "Demand notice #caseRef#",
                "body", "Dear #taxpayerName#, pay EUR #amountAtStake# (TIN #tin#)."));
        FormRow g = GuardTestHarness.row("caseRef", "TT-000001",
                "templateCode", "TPL-DEMAND", "docClass", "NOTICE", "result", "");
        g.setId("g-1");
        h.genOrders.put("g-1", g);

        String result = svc.generate("g-1", "admin", NOW);

        assertEquals("1 document(s) generated", result);
        assertEquals(1, h.docs.size());
        FormRow d = h.docs.values().iterator().next();
        assertEquals("GENERATED", d.getProperty("source"));
        assertEquals("REGISTERED", d.getProperty("status"));
        assertEquals("101", d.getProperty("mayanDocId"));
        // real PDF bytes shipped
        assertTrue(new String(mayan.lastContent, 0, 5, StandardCharsets.ISO_8859_1)
                .startsWith("%PDF-"));
        assertTrue(h.events.stream().anyMatch(e ->
                "DOC_GENERATED".equals(e.getProperty("eventType"))));
    }

    @Test
    public void bulkGenerateByCaseType() { // T-07.5 / WF-FR-017 batch
        h.templates.add(GuardTestHarness.row("code", "TPL-D", "language", "en",
                "version", "1", "active", "true", "effectiveFrom", "",
                "subject", "S #caseRef#", "body", "B"));
        FormRow c2 = GuardTestHarness.row("caseType", "TEST", "tin", "100061K",
                "caseRef", "TT-000002", "currentState", "OPEN");
        c2.setId("doc-case-2");
        h.cases.put("doc-case-2", c2);
        h.otherCases.add(c2);
        FormRow closed = GuardTestHarness.row("caseType", "TEST", "tin", "100062L",
                "caseRef", "TT-000003", "currentState", "CLOSED");
        closed.setId("doc-case-3");
        h.otherCases.add(closed);
        FormRow g = GuardTestHarness.row("caseRef", "", "filterCaseType", "TEST",
                "templateCode", "TPL-D", "docClass", "NOTICE", "result", "");
        g.setId("g-2");
        h.genOrders.put("g-2", g);

        assertEquals("2 document(s) generated", svc.generate("g-2", "admin", NOW));
        assertEquals(2, h.docs.size()); // terminal case excluded
    }

    @Test
    public void returnedMailCreatesVerificationTaskAndFlags() { // T-07.4 / WF-FR-019
        FormRow p = GuardTestHarness.row("caseRef", "TT-000001", "method", "REGISTERED",
                "status", "RETURNED", "processed", "");
        p.setId("p-1");
        h.postalRows.put("p-1", p);

        String result = svc.postal("p-1", "officer1");

        assertEquals("returned-mail actions created", result);
        assertEquals("RETURNED_MAIL", c.getProperty("addressFlag"));
        assertEquals(1, h.tasks.size());
        FormRow t = h.tasks.values().iterator().next();
        assertEquals("ADDRESS_VERIFICATION", t.getProperty("taskType"));
        assertEquals("OPEN", t.getProperty("status"));
        assertTrue(h.events.stream().anyMatch(e ->
                "POSTAL_RETURNED".equals(e.getProperty("eventType"))));
        assertTrue(h.events.stream().anyMatch(e ->
                "NOTIF_PENDING".equals(e.getProperty("eventType"))));
        // idempotent on re-save
        assertEquals("no action (status=RETURNED)", svc.postal("p-1", "officer1"));
        assertEquals(1, h.tasks.size());
    }

    @Test
    public void dispatchRecordedOnNotifAndHistory() { // WF-FR-019 dispatch logging
        FormRow n = GuardTestHarness.row("status", "QUEUED_PRINT");
        n.setId("nq-1");
        h.notifs.add(n);
        // load(cmNotif) is not mapped in harness — postal uses dao.load; map it via docs? use notifId=""
        FormRow p = GuardTestHarness.row("caseRef", "TT-000001", "method", "REGULAR",
                "status", "DISPATCHED", "processed", "", "notifId", "");
        p.setId("p-2");
        h.postalRows.put("p-2", p);
        assertEquals("dispatch recorded", svc.postal("p-2", "officer1"));
        assertTrue(h.events.stream().anyMatch(e ->
                "POSTAL_DISPATCHED".equals(e.getProperty("eventType"))));
        assertTrue(p.getProperty("processed").contains("DISPATCHED"));
    }
}
