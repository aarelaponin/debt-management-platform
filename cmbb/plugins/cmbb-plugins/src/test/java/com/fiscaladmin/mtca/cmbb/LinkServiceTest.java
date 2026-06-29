package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.joget.apps.form.model.FormRow;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.LinkService;

/** LinkService — CMBB-F08 typed linkage + reciprocity + target validation (T-08.6 unit). */
public class LinkServiceTest {

    private GuardTestHarness h;
    private LinkService svc;

    @Before
    public void setUp() {
        h = new GuardTestHarness();
        svc = new LinkService(h.dao, new CaseEventWriter(h.dao));
        h.linkTypes.add(GuardTestHarness.row("code", "REFERRAL", "name", "Referral",
                "targetCaseTypes", "TEST"));
        h.linkTypes.add(GuardTestHarness.row("code", "STRICT", "name", "Strict",
                "targetCaseTypes", "AUDIT"));
        FormRow target = GuardTestHarness.row("caseType", "TEST", "caseRef", "TT-TO");
        target.setId("case-to");
        h.cases.put("case-to", target);
    }

    private long eventCount(String type) {
        return h.events.stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private FormRow seedLink(String id, String linkType, String toRef) {
        FormRow link = GuardTestHarness.row("fromCaseId", "case-from", "fromCaseRef", "TT-FROM",
                "linkType", linkType, "toCaseRef", toRef, "result", "");
        link.setId(id);
        h.links.put(id, link);
        return link;
    }

    @Test
    public void permittedLinkWritesReciprocal() { // T-08.6
        FormRow link = seedLink("link-1", "REFERRAL", "TT-TO");
        String r = svc.link("link-1", "tester");
        assertTrue(r.contains("reciprocal"));
        assertEquals("case-to", link.getProperty("toCaseId"));
        assertEquals("true", link.getProperty("reciprocal"));
        assertEquals(2, h.links.size()); // forward + reciprocal
        assertEquals(2, eventCount("CASE_LINKED"));
    }

    @Test
    public void impermissibleTargetRejected() { // T-08.6 negative
        FormRow link = seedLink("link-1", "STRICT", "TT-TO");
        String r = svc.link("link-1", "tester");
        assertTrue(r.startsWith("REJECTED"));
        assertEquals(1, h.links.size()); // no reciprocal
        assertEquals(0, eventCount("CASE_LINKED"));
    }

    @Test
    public void targetNotYetPresentLinksOneWay() {
        seedLink("link-1", "REFERRAL", "TT-UNKNOWN");
        String r = svc.link("link-1", "tester");
        assertTrue(r.contains("target not yet present"));
        assertEquals(1, h.links.size());
        assertEquals(1, eventCount("CASE_LINKED"));
    }

    @Test
    public void alreadyProcessedIsNoOp() {
        seedLink("link-1", "REFERRAL", "TT-TO");
        svc.link("link-1", "tester");
        int after = h.links.size();
        assertTrue(svc.link("link-1", "tester").contains("no-op"));
        assertEquals(after, h.links.size());
    }
}
