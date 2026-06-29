package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.GoldMartClient;

/** GoldMartClient — I-1 read + INT-FR-004 degraded cache (CMBB-F09, T-09.3/09.5 unit). */
public class GoldMartClientTest {

    private FormDataDao dao;

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
    }

    @Test
    public void liveReadCachesSnapshot() {
        GoldMartClient.GoldGateway gw = tin -> {
            GoldMartClient.GoldProfile p = new GoldMartClient.GoldProfile();
            p.enforceableBalance = "123.45";
            p.debtCategory = "C4";
            p.asaConfidence = "VERIFIED";
            p.asOf = "2026-06-12T10:00:00";
            return p;
        };
        GoldMartClient c = new GoldMartClient(dao, gw);
        GoldMartClient.GoldProfile p = c.fetchProfile("100058G");
        assertEquals("LIVE", p.source);
        assertEquals("123.45", p.enforceableBalance);
        assertEquals("C4", p.debtCategory);
        assertEquals("100058G", p.tin);
        verify(dao).saveOrUpdate(eq("cmGoldSnapshot"), eq("cmGoldSnapshot"), any(FormRowSet.class));
    }

    @Test
    public void outageServesCacheWithStaleAsOf() {
        GoldMartClient.GoldGateway gw = tin -> {
            throw new RuntimeException("connection refused");
        };
        FormRow snap = new FormRow();
        snap.setId("100058G");
        snap.setProperty("enforceableBalance", "999.00");
        snap.setProperty("debtCategory", "C5");
        snap.setProperty("asaConfidence", "APPROXIMATED");
        snap.setProperty("asOf", "2026-06-01T08:00:00");
        when(dao.load("cmGoldSnapshot", "cmGoldSnapshot", "100058G")).thenReturn(snap);

        GoldMartClient c = new GoldMartClient(dao, gw);
        GoldMartClient.GoldProfile p = c.fetchProfile("100058G");
        assertEquals("CACHE", p.source);
        assertEquals("999.00", p.enforceableBalance);
        assertEquals("2026-06-01T08:00:00", p.asOf);
    }

    @Test
    public void outageWithoutCacheIsSafe() {
        GoldMartClient.GoldGateway gw = tin -> {
            throw new RuntimeException("down");
        };
        when(dao.load("cmGoldSnapshot", "cmGoldSnapshot", "999999X")).thenReturn(null);
        GoldMartClient c = new GoldMartClient(dao, gw);
        GoldMartClient.GoldProfile p = c.fetchProfile("999999X");
        assertEquals("CACHE", p.source);
        assertEquals("", p.enforceableBalance);
    }
}
