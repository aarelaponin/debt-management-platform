package com.fiscaladmin.mtca.cmbb.service;

import java.util.Collections;
import java.util.List;

import org.joget.commons.util.LogUtil;

/**
 * GoldMartScanner — bulk sta_v1 reads for debt identification (DMBB-F03, I-1).
 * Separate from GoldMartClient's single-TIN profile so the F09 GoldGateway stays
 * a single-method (lambda) interface. Degraded-read: any failure → empty list,
 * never throws into the identification run (P11 / INT-FR-004).
 */
public class GoldMartScanner {

    private static final String CLASS_NAME = GoldMartScanner.class.getName();

    /** A debtor row from sta_v1.debt_priority_queue (consolidated per TIN). */
    public static class GoldDebtor {
        public String tin = "";
        public String debtCategory = "";
        public String totalEnforceable = "0";
        public String oldestAgeDays = "0";
        public String taxTypes = "0";
    }

    /** A debt line from sta_v1.debt_balances (TIN x tax x year). */
    public static class GoldLine {
        public String taxType = "";
        public String yofa = "";
        public String amount = "0";
        public String disputed = "0";
        public String enforceable = "0";
        // Legal-correctness itemisation (STA Gold PA/IA/PCA decomposition): principal,
        // interest and penalty/costs of this line. principal+interest+penalty == amount.
        public String principal = "0";
        public String interest = "0";
        public String penalty = "0";
        public String asOf = "";
    }

    public interface ScanGateway {
        List<GoldDebtor> scan(String minAmount) throws Exception;
        List<GoldLine> lines(String tin) throws Exception;
    }

    private final ScanGateway gateway;

    public GoldMartScanner(ScanGateway gateway) {
        this.gateway = gateway;
    }

    public List<GoldDebtor> scan(String minAmount) {
        try {
            return gateway.scan(minAmount == null || minAmount.isEmpty() ? "0" : minAmount);
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "debt_priority_queue scan failed (" + e.getMessage()
                    + ") — no cases identified this run");
            return Collections.emptyList();
        }
    }

    public List<GoldLine> lines(String tin) {
        try {
            return gateway.lines(tin);
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "debt_balances read failed for TIN " + tin
                    + " (" + e.getMessage() + ")");
            return Collections.emptyList();
        }
    }
}
