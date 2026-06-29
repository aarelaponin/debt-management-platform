package com.fiscaladmin.mtca.cmbb.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * ChainVerifyService — verifies the cmEvent tamper-evident hash chain
 * (WF-FR-020 full, CMBB-F09). Read-only: it NEVER mutates cmEvent.
 *
 * For a case it reads every event ordered by zero-padded seq and checks:
 *  (a) seq contiguity from 0 (no inserted/deleted rows),
 *  (b) prevHash linkage (each row's prevHash == prior row's hash),
 *  (c) hash recompute: sha256(stored payload + stored prevHash) == stored hash
 *      (a mutated payload changes the recomputed hash -> BROKEN at that seq).
 * Recompute reuses CaseEventWriter.sha256 and hashes the STORED payload verbatim,
 * so the check is independent of field ordering — it trusts the bytes that were
 * written and only re-derives the hash + the linkage.
 */
public class ChainVerifyService {

    public static final String F_EVENT = "cmEvent";
    public static final String F_CASE = "cmCase";
    private static final int FETCH_ALL = 100000;

    private final FormDataDao dao;

    public ChainVerifyService(FormDataDao dao) {
        this.dao = dao;
    }

    public static class Result {
        public boolean ok = true;
        public long firstBadSeq = -1;
        public String reason = "";
        public int events = 0;
    }

    /** Verify one case's chain. An empty chain is trivially intact. */
    public Result verify(String caseId) {
        dao.updateSchema(F_EVENT, F_EVENT, new FormRowSet());
        FormRowSet rows = dao.find(F_EVENT, F_EVENT,
                "WHERE e.customProperties.caseId = ?1", new Object[]{caseId},
                "seq", Boolean.FALSE, 0, FETCH_ALL);
        Result r = new Result();
        if (rows == null || rows.isEmpty()) {
            return r;
        }
        String prevHash = "";
        long expectedSeq = 0;
        for (FormRow row : rows) {
            r.events++;
            long seq = CaseEventWriter.parseSeq(row.getProperty("seq"));
            String payload = nz(row.getProperty("payload"));
            String storedPrev = nz(row.getProperty("prevHash"));
            String storedHash = nz(row.getProperty("hash"));
            if (seq != expectedSeq) {
                return bad(r, seq, "seq gap (expected " + expectedSeq + ")");
            }
            if (!storedPrev.equals(prevHash)) {
                return bad(r, seq, "prevHash linkage broken");
            }
            String recomputed = CaseEventWriter.sha256(payload + storedPrev);
            if (!recomputed.equals(storedHash)) {
                return bad(r, seq, "hash mismatch (payload tampered)");
            }
            prevHash = storedHash;
            expectedSeq = seq + 1;
        }
        return r;
    }

    private Result bad(Result r, long seq, String why) {
        r.ok = false;
        r.firstBadSeq = seq;
        r.reason = why;
        return r;
    }

    /** Distinct caseIds across all cases (for the verify-all / EMIT scans). */
    public List<String> allCaseIds() {
        dao.updateSchema(F_CASE, F_CASE, new FormRowSet());
        FormRowSet rows = dao.find(F_CASE, F_CASE, null, null,
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        LinkedHashSet<String> ids = new LinkedHashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                if (r.getId() != null && !r.getId().isEmpty()) {
                    ids.add(r.getId());
                }
            }
        }
        return new ArrayList<String>(ids);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
