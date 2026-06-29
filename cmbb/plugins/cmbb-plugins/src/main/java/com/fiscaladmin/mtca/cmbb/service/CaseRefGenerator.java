package com.fiscaladmin.mtca.cmbb.service;

import org.joget.apps.form.dao.FormDataDao;

/**
 * Generates the human caseRef from mm_case_type.idFormat (FIS A1).
 * Format convention: literal prefix + run of '?' as zero-padded counter,
 * e.g. "TT-??????" -> TT-000001. Counter = count of already-referenced cases
 * of the type + 1, advanced past collisions (DEV-grade sequencing; a
 * dedicated sequence carrier is a recorded hardening item).
 */
public class CaseRefGenerator {

    public static final String F_CASE = "cmCase";

    private final FormDataDao dao;

    public CaseRefGenerator(FormDataDao dao) {
        this.dao = dao;
    }

    public String generate(String caseTypeCode, String idFormat) {
        int q = idFormat.indexOf('?');
        String prefix = (q < 0) ? idFormat : idFormat.substring(0, q);
        int width = 0;
        for (int i = 0; i < idFormat.length(); i++) {
            if (idFormat.charAt(i) == '?') {
                width++;
            }
        }
        if (width == 0) {
            width = 6;
        }
        Long existing = dao.count(F_CASE, F_CASE,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.caseRef <> ?2",
                new Object[]{caseTypeCode, ""});
        long seq = (existing == null ? 0 : existing) + 1;
        String candidate = format(prefix, seq, width);
        // collision guard (counts can lag deletions/imports)
        while (countRef(candidate) > 0) {
            seq++;
            candidate = format(prefix, seq, width);
        }
        return candidate;
    }

    private long countRef(String caseRef) {
        Long n = dao.count(F_CASE, F_CASE,
                "WHERE e.customProperties.caseRef = ?1", new Object[]{caseRef});
        return n == null ? 0 : n;
    }

    private static String format(String prefix, long seq, int width) {
        return prefix + String.format("%0" + width + "d", seq);
    }
}
