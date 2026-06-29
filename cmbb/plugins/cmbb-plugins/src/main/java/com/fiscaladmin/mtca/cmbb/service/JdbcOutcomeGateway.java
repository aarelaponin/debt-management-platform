package com.fiscaladmin.mtca.cmbb.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

/**
 * JDBC implementation of OutcomeService.OutcomeGateway — inserts a fact row into
 * ClickHouse mtca_ors.fact_case_outcomes (CMBB-F09, I-2). writeback_api creds
 * (INSERT,SELECT on the fact) come from the OutcomeWriteback plugin properties.
 * ReplacingMergeTree(received_at) ORDER BY (case_id,outcome_date,outcome_type,
 * outcome_code) dedups, so a duplicate insert cannot create a second logical row.
 * Driver embedded in the bundle (Embed-Dependency, DX9-DELTAS F07).
 */
public class JdbcOutcomeGateway implements OutcomeService.OutcomeGateway {

    private static final String DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";
    private static final String SQL =
            "INSERT INTO mtca_ors.fact_case_outcomes "
            + "(case_id, outcome_date, tin, outcome_type, outcome_code, amount, officer, detail) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private final String url;
    private final String user;
    private final String pass;

    public JdbcOutcomeGateway(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    @Override
    public void insert(OutcomeService.OutcomeRecord r) throws Exception {
        Class.forName(DRIVER);
        // Pin the lightweight HttpURLConnection transport. The clickhouse-jdbc "-all" jar's
        // INSERT path otherwise engages the shaded Apache HttpClient5 pool, whose
        // PoolingHttpClientConnectionManager.<clinit> references org.slf4j.LoggerFactory —
        // a package this OSGi bundle does not statically import and DynamicImport-Package:* does
        // not resolve at class-init, so it throws NoClassDefFoundError and marks the JTA tx
        // rollback-only (CMBB-F09 run_t09 → HTTP 500). The read gateways already use this
        // provider and work; aligning the writer makes the slf4j path unreachable. (DX9-DELTAS)
        Properties props = new Properties();
        if (user != null) props.setProperty("user", user);
        if (pass != null) props.setProperty("password", pass);
        props.setProperty("http_connection_provider", "HTTP_URL_CONNECTION");
        try (Connection c = DriverManager.getConnection(url, props);
             PreparedStatement ps = c.prepareStatement(SQL)) {
            ps.setString(1, r.caseId);
            ps.setDate(2, Date.valueOf(r.outcomeDate));
            ps.setString(3, r.tin);
            ps.setString(4, r.outcomeType);
            ps.setString(5, r.outcomeCode);
            ps.setBigDecimal(6, toDecimal(r.amount));
            ps.setString(7, r.officer);
            ps.setString(8, r.detail);
            ps.executeUpdate();
        }
    }

    private static BigDecimal toDecimal(String s) {
        try {
            return (s == null || s.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
