package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.ChainVerifyService;

/** ChainVerifyService — WF-FR-020 tamper-evidence (CMBB-F09, T-09.1/09.2 unit). */
public class ChainVerifyServiceTest {

    private FormDataDao dao;
    private ChainVerifyService svc;

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        svc = new ChainVerifyService(dao);
    }

    private FormRow ev(long seq, String payload, String prevHash) {
        FormRow r = new FormRow();
        r.setId("e" + seq);
        r.setProperty("seq", String.format("%010d", seq));
        r.setProperty("payload", payload);
        r.setProperty("prevHash", prevHash);
        r.setProperty("hash", CaseEventWriter.sha256(payload + prevHash));
        return r;
    }

    private void stubChain(FormRow... rows) {
        FormRowSet rs = new FormRowSet();
        for (FormRow r : rows) {
            rs.add(r);
        }
        when(dao.find(eq("cmEvent"), eq("cmEvent"), anyString(), any(Object[].class),
                anyString(), any(), any(), any())).thenReturn(rs);
    }

    @Test
    public void intactChainVerifies() {
        FormRow e0 = ev(0, "{\"a\":0}", "");
        FormRow e1 = ev(1, "{\"a\":1}", e0.getProperty("hash"));
        stubChain(e0, e1);
        ChainVerifyService.Result r = svc.verify("case-1");
        assertTrue(r.ok);
        assertEquals(2, r.events);
        assertEquals(-1, r.firstBadSeq);
    }

    @Test
    public void tamperedPayloadBreaksAtSeq() {
        FormRow e0 = ev(0, "{\"a\":0}", "");
        FormRow e1 = ev(1, "{\"a\":1}", e0.getProperty("hash"));
        e1.setProperty("payload", "{\"a\":1,\"x\":\"tampered\"}"); // stored hash no longer matches
        stubChain(e0, e1);
        ChainVerifyService.Result r = svc.verify("case-1");
        assertFalse(r.ok);
        assertEquals(1, r.firstBadSeq);
        assertTrue(r.reason, r.reason.contains("hash"));
    }

    @Test
    public void brokenLinkageDetected() {
        FormRow e0 = ev(0, "{\"a\":0}", "");
        FormRow e1 = ev(1, "{\"a\":1}", "deadbeef"); // prevHash not e0.hash
        stubChain(e0, e1);
        ChainVerifyService.Result r = svc.verify("case-1");
        assertFalse(r.ok);
        assertEquals(1, r.firstBadSeq);
        assertTrue(r.reason, r.reason.contains("linkage"));
    }

    @Test
    public void emptyChainIsTriviallyIntact() {
        when(dao.find(eq("cmEvent"), eq("cmEvent"), anyString(), any(Object[].class),
                anyString(), any(), any(), any())).thenReturn(new FormRowSet());
        assertTrue(svc.verify("case-x").ok);
    }
}
