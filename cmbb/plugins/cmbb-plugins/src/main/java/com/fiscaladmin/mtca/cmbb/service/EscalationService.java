package com.fiscaladmin.mtca.cmbb.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * EscalationService — DMBB-F04 (DM-FR-009..014, BR-DM-005..012). Walks the F01
 * collection strategy ladder (mmStrategy + ordered mmEscStep) against each open
 * DM case's elapsed time (since identification), category-gated (BR-DM-006: a
 * case below the strategy's category floor — e.g. C1 — is skipped). For each
 * newly-due step it advances dmDebt.stage / lastStepSeq, appends DEBT_ESCALATED,
 * and emits a NOTIF_PENDING carrying the step's notice template as `reason` so
 * the existing NotificationDispatcher (F06) issues the reminder/demand notice.
 * Idempotent: dmDebt.lastStepSeq gates re-firing. asOf enables time-travel tests.
 */
public class EscalationService {

    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_STEP = "mmEscStep";
    public static final String F_HOLD = "cmHold";
    public static final String F_LINE = "dmLine";
    /** Transitional default ladder: a workflow with no steps of its own inherits it (ADR-004 Phase 1b). */
    private static final String DEFAULT_WORKFLOW = "W-DEFAULT";
    private static final int FETCH_ALL = 100000;

    public static class Tally {
        public int cases = 0;
        public int escalated = 0; // steps fired
        public int skipped = 0;   // below category floor / no strategy

        @Override
        public String toString() {
            return "cases=" + cases + " stepsFired=" + escalated + " skipped=" + skipped;
        }
    }

    private final FormDataDao dao;
    private final MmConfigService mm;
    private final CaseEventWriter events;
    private final StatusManager status; // ADR-003: guarded + audited dmDebt.stage writes
    private final WorkflowService workflows; // ADR-004: per-tax/segment/industry workflow resolution

    public EscalationService(FormDataDao dao, MmConfigService mm, CaseEventWriter events) {
        this.dao = dao;
        this.mm = mm;
        this.events = events;
        // reuse the same config reader + event writer so STATUS_CHANGED joins the case chain
        this.status = new StatusManager(dao, mm, events);
        this.workflows = new WorkflowService(dao);
    }

    public Tally sweep(String actor, LocalDateTime asOf) {
        dao.updateSchema(F_CASE, F_CASE, new FormRowSet());
        dao.updateSchema(F_DEBT, F_DEBT, new FormRowSet());
        dao.updateSchema(F_HOLD, F_HOLD, new FormRowSet());
        dao.updateSchema(F_LINE, F_LINE, new FormRowSet());
        Tally t = new Tally();
        Set<String> terminals = mm.allTerminalCodes();
        FormRowSet cases = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.caseType = ?1", new Object[]{"DM"},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return t;
        }
        for (FormRow c : cases) {
            if (terminals.contains(DeadlineService.prop(c, "currentState"))) {
                continue;
            }
            t.cases++;
            FormRow dd = dao.load(F_DEBT, F_DEBT, c.getId());
            if (dd == null) {
                continue;
            }
            String cat = firstNonBlank(DeadlineService.prop(dd, "debtCategory"),
                    DeadlineService.prop(c, "category"));
            // ADR-004: resolve the governing workflow over (tenant × taxType × segment × industry)
            // within its validity window; null = no applicable workflow or below the floor (BR-DM-006).
            FormRow workflow = workflows.resolve(DeadlineService.prop(c, "tenant"),
                    DeadlineService.prop(c, "taxType"), DeadlineService.prop(c, "segment"),
                    DeadlineService.prop(c, "industry"), cat, asOf);
            if (workflow == null) {
                t.skipped++; // below category floor (BR-DM-006) or no active workflow
                continue;
            }
            t.escalated += walk(c, dd, workflow, actor, asOf);
        }
        return t;
    }

    private int walk(FormRow c, FormRow dd, FormRow workflow, String actor, LocalDateTime asOf) {
        String wfCode = DeadlineService.prop(workflow, "code");
        // ADR-004: record the governing workflow on the debt (observable resolution + 1d foundation).
        if (!wfCode.equals(DeadlineService.prop(dd, "workflowCode"))) {
            dd.setProperty("workflowCode", wfCode);
            save(F_DEBT, dd);
        }
        FormRowSet steps = dao.find(F_STEP, F_STEP,
                "WHERE e.customProperties.workflowCode = ?1",
                new Object[]{wfCode},
                "seq", Boolean.FALSE, 0, FETCH_ALL);
        if (steps == null || steps.isEmpty()) {
            // Transitional (ADR-004 Phase 1b): a workflow that has not yet been given its own
            // ladder inherits the default ladder. Per-tax divergence still comes from the
            // lifecycle scope below, so behaviour is unchanged until a bespoke ladder is authored.
            steps = dao.find(F_STEP, F_STEP,
                    "WHERE e.customProperties.workflowCode = ?1",
                    new Object[]{DEFAULT_WORKFLOW},
                    "seq", Boolean.FALSE, 0, FETCH_ALL);
        }
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        // dmDebt.stage is read-only → the data API drops the creator's "Identified"; establish the
        // canonical initial so the first guarded stage transition has a valid from-state.
        if (DeadlineService.prop(dd, "stage").isEmpty()) {
            dd.setProperty("stage", "Identified");
        }
        long elapsed = elapsedDays(c, asOf);
        long lastSeq = DeadlineService.parseLong(DeadlineService.prop(dd, "lastStepSeq"), 0);
        // Per-tax scope (ADR-003 §5 step 3): a VAT case resolves to its own lifecycle override,
        // every other tax falls through to the DM ladder. The lifecycle config decides which
        // stages apply, so the engine keeps a SINGLE strategy template across all taxes.
        java.util.List<String> scope = java.util.Arrays.asList(DeadlineService.prop(c, "taxType"), "DM");
        long cum = 0;
        int fired = 0;
        boolean enforcementTriggered = false;
        for (FormRow step : steps) {
            cum += DeadlineService.parseLong(DeadlineService.prop(step, "triggerDays"), 0);
            long seq = DeadlineService.parseLong(DeadlineService.prop(step, "seq"), 0);
            if (seq <= lastSeq) {
                continue;
            }
            if (cum > elapsed) {
                break; // cumulative — no later step is due either
            }
            String prevStage = DeadlineService.prop(dd, "stage");
            String stepName = DeadlineService.prop(step, "stepName");
            // ADR-003 per-tax lifecycle: only apply the step if the case's tax scope permits
            // stage -> stepName. If not (e.g. VAT skips the Reminder, or a record's stage is
            // already ahead of its counter), CONSUME the step's time-slot — advance the counter,
            // send NO notice, do NOT regress the stage — and continue; the next lifecycle-legal
            // step then applies in its own slot. One strategy template, per-tax behaviour from config.
            if (!status.canTransition(F_DEBT, scope, prevStage, stepName)) {
                lastSeq = seq;
                dd.setProperty("lastStepSeq", String.valueOf(seq));
                save(F_DEBT, dd);
                continue;
            }
            // BR-DM-030 enforcement gate: an enforcement step (recovery instrument,
            // not a reminder/demand notice) requires eligibility — no full-amount
            // objection and no active ENFORCEMENT_SUPPRESS hold (an active instalment
            // asserts that hold, F06/F08). Blocked => hold at final demand, stop.
            if (isEnforcementStep(step)) {
                if (!enforcementEligible(c.getId())) {
                    events.append(c.getId(), "ENFORCEMENT_BLOCKED", actor, "", "",
                            "enforcement step " + seq + " held (BR-DM-030: objection/hold)",
                            "\"seq\":" + seq);
                    break;
                }
                if (!enforcementTriggered) {
                    events.append(c.getId(), "ENFORCEMENT_TRIGGERED", actor, "", "",
                            "enforcement eligible (BR-DM-030)", "\"seq\":" + seq);
                    enforcementTriggered = true;
                }
            }
            // sets dd.stage in-memory + audits STATUS_CHANGED; persisted by the save below.
            status.apply(F_DEBT, dd, "stage", c.getId(), stepName, scope, actor,
                    "escalation step " + seq + " (" + stepName + ")");
            dd.setProperty("lastStepSeq", String.valueOf(seq));
            save(F_DEBT, dd);
            String instrument = DeadlineService.prop(step, "instrument");
            String notice = DeadlineService.prop(step, "noticeTemplate");
            events.append(c.getId(), "DEBT_ESCALATED", actor, prevStage, stepName,
                    "escalation step " + seq + " (" + stepName + ")",
                    "\"seq\":" + seq + ",\"instrument\":\"" + CaseEventWriter.esc(instrument)
                            + "\",\"notice\":\"" + CaseEventWriter.esc(notice) + "\"");
            if (!notice.isEmpty()) {
                events.append(c.getId(), "NOTIF_PENDING", actor, "", "", "debt notice",
                        "\"reason\":\"" + CaseEventWriter.esc(notice) + "\"");
            }
            lastSeq = seq;
            fired++;
        }
        return fired;
    }

    /** An enforcement step uses a recovery instrument (not a blank reminder or the DEMAND notice). */
    private static boolean isEnforcementStep(FormRow step) {
        String instr = DeadlineService.prop(step, "instrument");
        return !instr.isEmpty() && !"DEMAND".equals(instr);
    }

    /** BR-DM-030: eligible unless a full-amount objection or an active ENFORCEMENT_SUPPRESS hold. */
    boolean enforcementEligible(String caseId) {
        if (holdActive(caseId, "ENFORCEMENT_SUPPRESS")) {
            return false;
        }
        return !fullAmountObjection(caseId);
    }

    private boolean holdActive(String caseId, String scope) {
        Long n = dao.count(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{caseId, scope, "ACTIVE"});
        return n != null && n > 0;
    }

    /** Disputed >= enforceable across the case's debt lines (and there is debt). */
    private boolean fullAmountObjection(String caseId) {
        FormRowSet lines = dao.find(F_LINE, F_LINE,
                "WHERE e.customProperties.caseId = ?1", new Object[]{caseId},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (lines == null || lines.isEmpty()) {
            return false;
        }
        double total = 0;
        double disputed = 0;
        for (FormRow l : lines) {
            total += num(DeadlineService.prop(l, "enforceable"));
            disputed += num(DeadlineService.prop(l, "disputed"));
        }
        return total > 0 && disputed >= total;
    }

    private static double num(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long elapsedDays(FormRow c, LocalDateTime asOf) {
        LocalDateTime base = asOf;
        try {
            if (c.getDateCreated() != null) {
                base = c.getDateCreated().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        } catch (Exception ignored) { /* fall back to asOf -> elapsed 0 */ }
        long d = Duration.between(base, asOf).toDays();
        return Math.max(0, d);
    }

    private static String firstNonBlank(String a, String b) {
        return (a == null || a.isEmpty()) ? (b == null ? "" : b) : a;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
