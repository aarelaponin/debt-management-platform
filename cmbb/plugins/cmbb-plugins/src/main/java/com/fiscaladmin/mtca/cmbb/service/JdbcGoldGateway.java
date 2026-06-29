package com.fiscaladmin.mtca.cmbb.service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * JDBC implementation of GoldMartClient.GoldGateway (single-TIN profile, F09)
 * AND GoldMartScanner.ScanGateway (bulk debt identification, F03) — both read
 * the published sta_v1 product (I-1). Connection url/user/pass come from the
 * consuming plugin's properties. The ClickHouse JDBC driver is embedded in the
 * bundle (Embed-Dependency; webapp packages are not OSGi-exported — DX9-DELTAS F07).
 */
public class JdbcGoldGateway implements GoldMartClient.GoldGateway, GoldMartScanner.ScanGateway {

    private static final String DRIVER = "com.clickhouse.jdbc.ClickHouseDriver";

    // sta_v1.taxpayer_balances: enforceable_balance + worst asa_confidence + as_of per TIN.
    private static final String SQL_BAL =
            "SELECT sum(enforceable_balance) AS bal, min(asa_confidence) AS conf, "
            + "max(toString(as_of)) AS asof FROM sta_v1.taxpayer_balances WHERE tin = ?";
    // sta_v1.debt_priority_queue: consolidated debt_category per TIN (may be absent for non-debtors).
    private static final String SQL_CAT =
            "SELECT any(debt_category) AS cat FROM sta_v1.debt_priority_queue WHERE tin = ?";
    // F03 scan: debtors above the minimum consolidated enforceable amount.
    private static final String SQL_SCAN =
            "SELECT tin, debt_category AS cat, toString(total_enforceable) AS tot, "
            + "toString(oldest_debt_age_days) AS age, toString(tax_types_involved) AS tt "
            + "FROM sta_v1.debt_priority_queue WHERE total_enforceable >= ? ORDER BY tin";
    // F03 lines: the TIN's TINxtaxxyear debt lines.
    private static final String SQL_LINES =
            "SELECT tax_type AS tt, toString(yofa) AS yofa, toString(balance) AS amt, "
            + "toString(disputed_amount) AS disp, toString(enforceable_amount) AS enf, "
            + "toString(pa_amount) AS prin, toString(ia_amount) AS intr, toString(pca_amount) AS pen, "
            + "toString(as_of) AS asof FROM sta_v1.debt_balances WHERE tin = ? ORDER BY tax_type, yofa";

    private final String url;
    private final String user;
    private final String pass;

    public JdbcGoldGateway(String url, String user, String pass) {
        this.url = url;
        this.user = user;
        this.pass = pass;
    }

    /**
     * Open a ClickHouse connection pinned to the lightweight HttpURLConnection transport.
     * The clickhouse-jdbc "-all" jar's pooled path (PoolingHttpClientConnectionManager) needs
     * org.slf4j, which this OSGi bundle does not resolve at class-init; its &lt;clinit&gt; throws
     * NoClassDefFoundError and — critically — POISONS the shaded HTTP-client class for the whole
     * JVM, so once any ClickHouse call (e.g. the F09 degraded-read failover) touches the Apache
     * pool, every later ClickHouse call in that JVM fails. Pinning every connection to
     * HTTP_URL_CONNECTION keeps the Apache/slf4j path unreachable, so the engines stay repeatable
     * across a full regression sweep, not just on a freshly restarted JVM. (DX9-DELTAS)
     */
    private Connection connect() throws java.sql.SQLException {
        Properties props = new Properties();
        if (user != null) props.setProperty("user", user);
        if (pass != null) props.setProperty("password", pass);
        props.setProperty("http_connection_provider", "HTTP_URL_CONNECTION");
        return DriverManager.getConnection(url, props);
    }

    @Override
    public GoldMartClient.GoldProfile read(String tin) throws Exception {
        Class.forName(DRIVER);
        GoldMartClient.GoldProfile p = new GoldMartClient.GoldProfile();
        p.tin = tin;
        try (Connection c = connect()) {
            try (PreparedStatement ps = c.prepareStatement(SQL_BAL)) {
                ps.setString(1, tin);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        p.enforceableBalance = nz(rs.getString("bal"));
                        p.asaConfidence = nz(rs.getString("conf"));
                        p.asOf = nz(rs.getString("asof"));
                    }
                }
            }
            try (PreparedStatement ps = c.prepareStatement(SQL_CAT)) {
                ps.setString(1, tin);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        p.debtCategory = nz(rs.getString("cat"));
                    }
                }
            }
        }
        return p;
    }

    @Override
    public List<GoldMartScanner.GoldDebtor> scan(String minAmount) throws Exception {
        Class.forName(DRIVER);
        List<GoldMartScanner.GoldDebtor> out = new ArrayList<GoldMartScanner.GoldDebtor>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(SQL_SCAN)) {
            ps.setBigDecimal(1, toDec(minAmount));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GoldMartScanner.GoldDebtor d = new GoldMartScanner.GoldDebtor();
                    d.tin = nz(rs.getString("tin"));
                    d.debtCategory = nz(rs.getString("cat"));
                    d.totalEnforceable = nz(rs.getString("tot"));
                    d.oldestAgeDays = nz(rs.getString("age"));
                    d.taxTypes = nz(rs.getString("tt"));
                    out.add(d);
                }
            }
        }
        return out;
    }

    @Override
    public List<GoldMartScanner.GoldLine> lines(String tin) throws Exception {
        Class.forName(DRIVER);
        List<GoldMartScanner.GoldLine> out = new ArrayList<GoldMartScanner.GoldLine>();
        try (Connection c = connect();
             PreparedStatement ps = c.prepareStatement(SQL_LINES)) {
            ps.setString(1, tin);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    GoldMartScanner.GoldLine l = new GoldMartScanner.GoldLine();
                    l.taxType = nz(rs.getString("tt"));
                    l.yofa = nz(rs.getString("yofa"));
                    l.amount = nz(rs.getString("amt"));
                    l.disputed = nz(rs.getString("disp"));
                    l.enforceable = nz(rs.getString("enf"));
                    l.principal = nz(rs.getString("prin"));
                    l.interest = nz(rs.getString("intr"));
                    l.penalty = nz(rs.getString("pen"));
                    l.asOf = nz(rs.getString("asof"));
                    out.add(l);
                }
            }
        }
        return out;
    }

    private static BigDecimal toDec(String s) {
        try {
            return (s == null || s.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
