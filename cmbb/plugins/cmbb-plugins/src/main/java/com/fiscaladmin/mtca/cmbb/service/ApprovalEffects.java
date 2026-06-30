package com.fiscaladmin.mtca.cmbb.service;

import java.util.HashMap;
import java.util.Map;

import org.joget.apps.form.dao.FormDataDao;

/**
 * Wiring for the Decision &amp; Approval Service (#6): the {@code actionType → DecisionEffect}
 * registry (the module-supplied "meaning"), and a configured {@link ApprovalService} that both
 * the request side (the relief engine's submit) and the decide side (the gate post-processor)
 * construct identically — so the gate fires the same effect whether the action auto-passes below
 * the band or is approved.
 *
 * <p>First effect: {@code INSTALMENT_PLAN → ReliefService.apply} (finalise the instalment:
 * eligibility + approval bookkeeping + the ENFORCEMENT_SUPPRESS hold). Second effect:
 * {@code WRITE_OFF → WriteOffService.applyApproved} (post the approved write-off) — the reuse
 * proof that a structurally different action (collegial quorum, a different effect) drives the
 * same gate. Later actions (waiver, hold-release) add a line here, not a new approval flow.
 */
public final class ApprovalEffects {

    public static final String ACTION_INSTALMENT = "INSTALMENT_PLAN";
    public static final String ACTION_WRITEOFF = "WRITE_OFF";

    private ApprovalEffects() {
    }

    public static Map<String, ApprovalService.DecisionEffect> registry(final FormDataDao dao) {
        Map<String, ApprovalService.DecisionEffect> effects = new HashMap<String, ApprovalService.DecisionEffect>();
        effects.put(ACTION_INSTALMENT, (entity, recordId, actor, now) ->
                new ReliefService(dao, new CaseEventWriter(dao), new JogetProcessStarter())
                        .apply(recordId, actor, now));
        effects.put(ACTION_WRITEOFF, (entity, recordId, actor, now) ->
                new WriteOffService(dao, new CaseEventWriter(dao))
                        .applyApproved(recordId, actor, now));
        return effects;
    }

    /** A ready ApprovalService backed by the live registry. */
    public static ApprovalService service(FormDataDao dao) {
        return new ApprovalService(dao, new CaseEventWriter(dao), registry(dao));
    }
}
