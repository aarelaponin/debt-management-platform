package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * WorkflowService — ADR-004. Resolves the collection workflow that governs a
 * case from the dmWorkflow carrier, and validates the workflow catalogue.
 *
 * <p>A workflow is selected over the dimension space (tenant × taxType × segment
 * × industry); a blank selector dimension means ANY. Among the Active workflows
 * whose selector matches, whose validity window [validFrom, validTo] contains the
 * resolution instant, and whose categoryFloor admits the case's debt band, the
 * MOST SPECIFIC wins — specificity weighted tax &gt; segment &gt; industry
 * (ADR-004 §model), with the highest version breaking ties.</p>
 *
 * <p>Overlap prevention is a config-authoring guard: two Active workflows that
 * share the exact same selector tuple must not have overlapping validity windows
 * (otherwise resolution would be ambiguous). {@link #findOverlaps()} surfaces any
 * such collision for the VALIDATE trigger.</p>
 */
public class WorkflowService {

    public static final String F_WORKFLOW = "dmWorkflow";
    /** Tenant assumed when a case carries none (resolved from the profile by TenantContext). */
    public static final String DEFAULT_TENANT = TenantContext.DEFAULT_TENANT;
    private static final int FETCH_ALL = 100000;
    private static final List<String> BANDS = Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6");
    // specificity weights — tax dominates segment dominates industry (ADR-004 §model)
    private static final int W_TAX = 4;
    private static final int W_SEGMENT = 2;
    private static final int W_INDUSTRY = 1;

    private final FormDataDao dao;

    public WorkflowService(FormDataDao dao) {
        this.dao = dao;
    }

    /**
     * The governing workflow for a case, or null when none is applicable (no
     * matching Active workflow in window, or the case is below every floor).
     */
    public FormRow resolve(String tenant, String taxType, String segment,
            String industry, String category, LocalDateTime asOf) {
        dao.updateSchema(F_WORKFLOW, F_WORKFLOW, new FormRowSet());
        FormRowSet rows = dao.find(F_WORKFLOW, F_WORKFLOW,
                "WHERE e.customProperties.status = ?1", new Object[]{"Active"},
                "version", Boolean.TRUE, 0, FETCH_ALL);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String effTenant = firstNonBlank(tenant, DEFAULT_TENANT);
        String asOfDate = (asOf == null ? LocalDateTime.now() : asOf).toLocalDate().toString();
        int catRank = rank(category);
        FormRow best = null;
        int bestScore = -1;
        long bestVersion = -1;
        for (FormRow w : rows) {
            String wTenant = DeadlineService.prop(w, "tenant");
            String wTax = DeadlineService.prop(w, "taxType");
            String wSeg = DeadlineService.prop(w, "segment");
            String wInd = DeadlineService.prop(w, "industry");
            if (!dimMatch(wTenant, effTenant) || !dimMatch(wTax, taxType)
                    || !dimMatch(wSeg, segment) || !dimMatch(wInd, industry)) {
                continue;
            }
            if (!inWindow(DeadlineService.prop(w, "validFrom"),
                    DeadlineService.prop(w, "validTo"), asOfDate)) {
                continue;
            }
            if (rank(DeadlineService.prop(w, "categoryFloor")) > catRank) {
                continue; // case below this workflow's floor (BR-DM-006)
            }
            int score = (isSet(wTax) ? W_TAX : 0) + (isSet(wSeg) ? W_SEGMENT : 0)
                    + (isSet(wInd) ? W_INDUSTRY : 0);
            long version = DeadlineService.parseLong(DeadlineService.prop(w, "version"), 0);
            if (score > bestScore || (score == bestScore && version > bestVersion)) {
                bestScore = score;
                bestVersion = version;
                best = w;
            }
        }
        return best;
    }

    /**
     * Ambiguity report: pairs of Active workflows that share the exact selector
     * tuple (tenant|taxType|segment|industry) and have overlapping validity
     * windows. Empty list = catalogue is unambiguous.
     */
    public List<String> findOverlaps() {
        List<String> issues = new ArrayList<String>();
        dao.updateSchema(F_WORKFLOW, F_WORKFLOW, new FormRowSet());
        FormRowSet rows = dao.find(F_WORKFLOW, F_WORKFLOW,
                "WHERE e.customProperties.status = ?1", new Object[]{"Active"},
                "code", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null) {
            return issues;
        }
        List<FormRow> active = new ArrayList<FormRow>(rows);
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                FormRow a = active.get(i);
                FormRow b = active.get(j);
                if (selectorKey(a).equals(selectorKey(b)) && windowsOverlap(a, b)) {
                    issues.add("overlapping workflows for selector [" + selectorKey(a) + "]: "
                            + DeadlineService.prop(a, "code") + " & " + DeadlineService.prop(b, "code"));
                }
            }
        }
        return issues;
    }

    /** Selector dimension matches: a blank workflow dimension is ANY; else case-insensitive equal. */
    private static boolean dimMatch(String workflowDim, String caseValue) {
        if (!isSet(workflowDim)) {
            return true; // ANY
        }
        return workflowDim.equalsIgnoreCase(caseValue == null ? "" : caseValue.trim());
    }

    private static boolean inWindow(String validFrom, String validTo, String asOfDate) {
        return leq(validFrom, asOfDate) && leq(asOfDate, validTo);
    }

    private static boolean windowsOverlap(FormRow a, FormRow b) {
        String af = DeadlineService.prop(a, "validFrom"), at = DeadlineService.prop(a, "validTo");
        String bf = DeadlineService.prop(b, "validFrom"), bt = DeadlineService.prop(b, "validTo");
        return leq(af, bt) && leq(bf, at);
    }

    /** x <= y with blank x = -inf and blank y = +inf (ISO date strings compare lexically). */
    private static boolean leq(String x, String y) {
        if (x == null || x.isEmpty() || y == null || y.isEmpty()) {
            return true;
        }
        return x.compareTo(y) <= 0;
    }

    private static String selectorKey(FormRow w) {
        return norm(DeadlineService.prop(w, "tenant")) + "|" + norm(DeadlineService.prop(w, "taxType"))
                + "|" + norm(DeadlineService.prop(w, "segment")) + "|" + norm(DeadlineService.prop(w, "industry"));
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private static boolean isSet(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String firstNonBlank(String a, String b) {
        return isSet(a) ? a : (b == null ? "" : b);
    }

    private static int rank(String band) {
        int i = BANDS.indexOf(band == null ? "" : band.trim().toUpperCase());
        return i < 0 ? 0 : i + 1;
    }
}
