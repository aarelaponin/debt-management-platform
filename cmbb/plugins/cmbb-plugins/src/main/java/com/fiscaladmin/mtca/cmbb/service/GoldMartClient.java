package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * GoldMartClient — sta_v1 read client + degraded-read cache (CMBB-F09,
 * I-1 / INT-FR-001/004, CAD §2.2 "shared lib used by BB plugins").
 *
 * LIVE: GoldGateway.read(tin) hits sta_v1; on success the profile is cached to
 *       cmGoldSnapshot (id = tin) and returned with source=LIVE.
 * CACHE: any gateway failure -> return the cmGoldSnapshot row with source=CACHE
 *       and its stale asOf. Never throws for an outage — case work never blocks
 *       on the product (P11). The JDBC details live in JdbcGoldGateway so unit
 *       tests inject a fake/throwing gateway with no live ClickHouse.
 */
public class GoldMartClient {

    public static final String F_SNAP = "cmGoldSnapshot";
    private static final String CLASS_NAME = GoldMartClient.class.getName();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public interface GoldGateway {
        GoldProfile read(String tin) throws Exception;
    }

    public static class GoldProfile {
        public String tin = "";
        public String enforceableBalance = "";
        public String debtCategory = "";
        public String asaConfidence = "";
        public String asOf = "";
        public String source = "LIVE";
    }

    private final FormDataDao dao;
    private final GoldGateway gateway;

    public GoldMartClient(FormDataDao dao, GoldGateway gateway) {
        this.dao = dao;
        this.gateway = gateway;
    }

    public GoldProfile fetchProfile(String tin) {
        dao.updateSchema(F_SNAP, F_SNAP, new FormRowSet());
        try {
            GoldProfile p = gateway.read(tin);
            if (p == null) {
                p = new GoldProfile();
            }
            p.tin = tin;
            p.source = "LIVE";
            upsertSnapshot(p);
            return p;
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "sta_v1 read failed for TIN " + tin
                    + " (" + e.getMessage() + ") — serving cache");
            return fromCache(tin);
        }
    }

    private GoldProfile fromCache(String tin) {
        GoldProfile p = new GoldProfile();
        p.tin = tin;
        p.source = "CACHE";
        FormRow row = dao.load(F_SNAP, F_SNAP, tin);
        if (row != null) {
            p.enforceableBalance = nz(row.getProperty("enforceableBalance"));
            p.debtCategory = nz(row.getProperty("debtCategory"));
            p.asaConfidence = nz(row.getProperty("asaConfidence"));
            p.asOf = nz(row.getProperty("asOf"));
        }
        return p;
    }

    private void upsertSnapshot(GoldProfile p) {
        FormRow row = new FormRow();
        row.setId(p.tin);
        row.setProperty("tin", p.tin);
        row.setProperty("enforceableBalance", nz(p.enforceableBalance));
        row.setProperty("debtCategory", nz(p.debtCategory));
        row.setProperty("asaConfidence", nz(p.asaConfidence));
        row.setProperty("asOf", nz(p.asOf));
        row.setProperty("fetchedAt", LocalDateTime.now().format(TS));
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(F_SNAP, F_SNAP, set);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
