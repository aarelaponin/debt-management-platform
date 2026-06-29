package com.fiscaladmin.mtca.cmbb.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GarnishingConnector — DMBB-F07 bank-garnishing web-service stub (DM-FR-029, BR-DM-034).
 *
 * Builds the freezing-request payload (TIN, taxpayer name, bank details, amount, legal
 * reference, MTCA authority reference) and records a bank response (SUCCESS / PARTIAL /
 * FAILURE). In DEV there is no live bank web service, so the transport is simulated
 * deterministically; the request map and response are what get audited. Live transport
 * (endpoint, TLS 1.2+, timeout BR-DM-034 default 30s sync / 48h async) is bound at deployment.
 */
public class GarnishingConnector {

    public static class Response {
        public final String status;   // SUCCESS | PARTIAL | FAILURE
        public final double amount;   // garnished amount actually frozen
        public final String externalRef;

        public Response(String status, double amount, String externalRef) {
            this.status = status;
            this.amount = amount;
            this.externalRef = externalRef;
        }
    }

    /** Build the BR-DM-034 request payload (the audited record of what was transmitted). */
    public Map<String, String> buildRequest(String tin, String name, String bankRef,
                                            double amount, String legalRef, String authorityRef) {
        Map<String, String> req = new LinkedHashMap<>();
        req.put("tin", tin == null ? "" : tin);
        req.put("taxpayerName", name == null ? "" : name);
        req.put("bankDetails", bankRef == null ? "" : bankRef);
        req.put("amount", String.valueOf(amount));
        req.put("legalReference", legalRef == null ? "" : legalRef);
        req.put("authorityReference", authorityRef == null ? "" : authorityRef);
        return req;
    }

    /**
     * Transmit the freezing request and return the bank response.
     * DEV stub: a non-positive amount FAILS; otherwise the full amount is frozen (SUCCESS).
     * A real binding replaces this body with the bank web-service call.
     */
    public Response transmit(Map<String, String> request) {
        double amount = parse(request.get("amount"));
        String ref = "BNK-" + Integer.toHexString((request.toString()).hashCode()).toUpperCase();
        if (amount <= 0) {
            return new Response("FAILURE", 0, ref);
        }
        return new Response("SUCCESS", amount, ref);
    }

    private static double parse(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
