package com.fiscaladmin.mtca.cmbb.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * StrategyAdminService — DMBB-F05 (DM-FR-015, DMBB-admin). The consistency
 * validation that makes self-service strategy authoring safe (the "activation-as-case"
 * dry-run gate): every escalation step references an ENABLED instrument; the active
 * strategies cover every debt band C2..C6; and no two active strategies for the same
 * (segment, categoryFloor) have overlapping effective windows. Pure read; the engine
 * writes the verdict back onto the trigger row.
 */
public class StrategyAdminService {

    public static final String F_STRATEGY = "mmStrategy";
    public static final String F_STEP = "mmEscStep";
    public static final String F_INSTRUMENT = "mdInstrument";
    private static final int FETCH_ALL = 100000;
    private static final List<String> COVER = Arrays.asList("C2", "C3", "C4", "C5", "C6");
    private static final List<String> BANDS = Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6");

    public static class Result {
        public boolean valid = true;
        public final List<String> issues = new ArrayList<String>();

        public String summary() {
            return valid ? "VALID" : "INVALID(" + issues.size() + "): " + String.join("; ", issues);
        }
    }

    private final FormDataDao dao;

    public StrategyAdminService(FormDataDao dao) {
        this.dao = dao;
    }

    public Result validate() {
        Result r = new Result();
        FormRowSet strategies = dao.find(F_STRATEGY, F_STRATEGY,
                "WHERE e.customProperties.active = ?1", new Object[]{"true"},
                "code", Boolean.FALSE, 0, FETCH_ALL);
        List<FormRow> active = strategies == null ? new ArrayList<FormRow>()
                : new ArrayList<FormRow>(strategies);

        // 1. every step references an ENABLED instrument
        for (FormRow s : active) {
            String code = DeadlineService.prop(s, "code");
            FormRowSet steps = dao.find(F_STEP, F_STEP,
                    "WHERE e.customProperties.strategyCode = ?1", new Object[]{code},
                    "seq", Boolean.FALSE, 0, FETCH_ALL);
            if (steps == null) {
                continue;
            }
            for (FormRow step : steps) {
                String instr = DeadlineService.prop(step, "instrument");
                if (instr.isEmpty()) {
                    continue; // a reminder step needs no instrument
                }
                FormRow md = dao.load(F_INSTRUMENT, F_INSTRUMENT, instr);
                if (md == null) {
                    r.issues.add(code + " step " + DeadlineService.prop(step, "seq")
                            + ": instrument '" + instr + "' not in catalogue");
                } else if (!"true".equals(DeadlineService.prop(md, "enabled"))) {
                    r.issues.add(code + " step " + DeadlineService.prop(step, "seq")
                            + ": instrument '" + instr + "' is disabled");
                }
            }
        }

        // 2. category coverage C2..C6 (some active strategy whose floor rank <= band rank)
        Set<String> covered = new HashSet<String>();
        for (FormRow s : active) {
            int floor = rank(DeadlineService.prop(s, "categoryFloor"));
            for (String band : COVER) {
                if (rank(band) >= floor && floor > 0) {
                    covered.add(band);
                }
            }
        }
        for (String band : COVER) {
            if (!covered.contains(band)) {
                r.issues.add("no active strategy covers category " + band);
            }
        }

        // 3. no overlapping effective windows for the same (segment, categoryFloor)
        for (int i = 0; i < active.size(); i++) {
            for (int j = i + 1; j < active.size(); j++) {
                FormRow a = active.get(i);
                FormRow b = active.get(j);
                if (DeadlineService.prop(a, "segment").equalsIgnoreCase(DeadlineService.prop(b, "segment"))
                        && DeadlineService.prop(a, "categoryFloor")
                                .equalsIgnoreCase(DeadlineService.prop(b, "categoryFloor"))
                        && overlaps(a, b)) {
                    r.issues.add("overlapping effective windows: "
                            + DeadlineService.prop(a, "code") + " & " + DeadlineService.prop(b, "code"));
                }
            }
        }

        // 4. ADR-004 workflow catalogue: no two Active workflows sharing the exact selector
        // tuple may have overlapping validity windows (else workflow resolution is ambiguous).
        r.issues.addAll(new WorkflowService(dao).findOverlaps());

        r.valid = r.issues.isEmpty();
        return r;
    }

    /** [from,to] windows overlap; blank from = -inf, blank to = +inf. */
    private static boolean overlaps(FormRow a, FormRow b) {
        String af = DeadlineService.prop(a, "effectiveFrom"), at = DeadlineService.prop(a, "effectiveTo");
        String bf = DeadlineService.prop(b, "effectiveFrom"), bt = DeadlineService.prop(b, "effectiveTo");
        return leq(af, bt) && leq(bf, at);
    }

    /** x <= y with blank x = -inf and blank y = +inf (ISO date strings compare lexically). */
    private static boolean leq(String x, String y) {
        if (x == null || x.isEmpty() || y == null || y.isEmpty()) {
            return true;
        }
        return x.compareTo(y) <= 0;
    }

    private static int rank(String band) {
        int i = BANDS.indexOf(band == null ? "" : band.trim().toUpperCase());
        return i < 0 ? 0 : i + 1;
    }
}
