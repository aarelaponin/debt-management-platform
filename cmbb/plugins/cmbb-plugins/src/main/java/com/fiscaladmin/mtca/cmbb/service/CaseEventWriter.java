package com.fiscaladmin.mtca.cmbb.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Append-only writer for cmEvent — the tamper-evident case history
 * (WF-FR-004/020). Every accepted or rejected transition appends one row:
 * {timestamp, actor, prevState, newState, reason} as JSON payload, chained by
 * hash = SHA-256(payload + prevHash). Genesis rows carry prevHash = "".
 * Never updates or deletes (immutability is enforced by having no other
 * write path — cmEvent has no edit UI).
 */
public class CaseEventWriter {

    public static final String F_EVENT = "cmEvent";

    private final FormDataDao dao;
    /** Chains consecutive appends within one guard run without re-querying. */
    private String lastHash;
    private String lastHashCaseId;
    private long lastSeq = -1;

    public CaseEventWriter(FormDataDao dao) {
        this.dao = dao;
    }

    /**
     * Appends an event row and returns its hash.
     * extraJson (nullable) is merged into the payload object verbatim —
     * pass pre-escaped JSON members like "\"link\":\"abc\"".
     */
    public String append(String caseId, String eventType, String actor,
                         String prevState, String newState, String reason, String extraJson) {
        String prevHash = lastEventHash(caseId);
        long seq = lastSeq + 1;
        String ts = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
        StringBuilder payload = new StringBuilder("{");
        payload.append("\"timestamp\":\"").append(esc(ts)).append('"');
        payload.append(",\"actor\":\"").append(esc(actor)).append('"');
        payload.append(",\"prevState\":\"").append(esc(prevState)).append('"');
        payload.append(",\"newState\":\"").append(esc(newState)).append('"');
        payload.append(",\"reason\":\"").append(esc(reason)).append('"');
        if (extraJson != null && !extraJson.isEmpty()) {
            payload.append(',').append(extraJson);
        }
        payload.append('}');
        String hash = sha256(payload + prevHash);

        FormRow row = new FormRow();
        row.setId(UUID.randomUUID().toString());
        row.setProperty("caseId", caseId);
        row.setProperty("seq", String.format("%010d", seq));
        row.setProperty("eventType", eventType);
        row.setProperty("actor", actor == null ? "" : actor);
        row.setProperty("eventTime", ts);
        row.setProperty("prevHash", prevHash);
        row.setProperty("hash", hash);
        row.setProperty("payload", payload.toString());
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(F_EVENT, F_EVENT, set);

        lastHash = hash;
        lastHashCaseId = caseId;
        lastSeq = seq;
        return hash;
    }

    /** Hash of the case's latest event, or "" for genesis. */
    public String lastEventHash(String caseId) {
        if (caseId.equals(lastHashCaseId) && lastHash != null) {
            return lastHash;
        }
        // chain order = zero-padded seq: same-MILLISECOND eventTime ties across
        // different writer instances forked the chain (regression T-02.3)
        FormRowSet rows = dao.find(F_EVENT, F_EVENT,
                "WHERE e.customProperties.caseId = ?1", new Object[]{caseId},
                "seq", Boolean.TRUE, 0, 1);
        if (rows == null || rows.isEmpty()) {
            lastSeq = -1;
            return "";
        }
        FormRow prev = rows.get(0);
        lastSeq = parseSeq(prev.getProperty("seq"));
        String h = prev.getProperty("hash");
        return h == null ? "" : h;
    }

    static long parseSeq(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return 0; // legacy rows without seq
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                   .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
