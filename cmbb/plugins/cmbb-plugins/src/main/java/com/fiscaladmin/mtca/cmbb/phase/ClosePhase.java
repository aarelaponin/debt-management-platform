package com.fiscaladmin.mtca.cmbb.phase;

import java.util.Set;

import org.joget.apps.form.model.FormRow;

import com.fiscaladmin.mtca.cmbb.service.DecisionService;
import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;

/**
 * CLOSE phase: decision/authority gate (CMBB-F08 — when requireDecision is set
 * for the type, an APPROVED cmDecision must exist for the case), then move the
 * case to its terminal state per mmTransition and append CASE_CLOSED.
 */
public class ClosePhase implements GuardPhase {

    @Override
    public void run(GuardContext ctx) {
        FormRow c = ctx.caseRow();
        String typeCode = ctx.prop("caseType");
        String current = ctx.prop("currentState");

        // F08: requireDecision is per case TYPE (mmCaseType.requireDecision) so a
        // single envelope process does not force decisions on every type; the
        // plugin property is a global override (FIS A4). A global true would break
        // the F02–F07 TEST regression — hence the type flag.
        boolean needDecision = ctx.requireDecision();
        if (!needDecision) {
            FormRow type = ctx.mm().caseType(typeCode);
            needDecision = type != null
                    && "true".equalsIgnoreCase(type.getProperty("requireDecision"));
        }
        if (needDecision) {
            // an APPROVED decision (authority already proven at decision time,
            // FIS A2) must exist for the case before it may close.
            DecisionService decisions = new DecisionService(ctx.dao(), ctx.events());
            if (!decisions.hasApprovedDecision(ctx.caseId())) {
                throw new InvalidTransitionException(current, "",
                        "closure decision required: no approved decision for this case");
            }
        }

        Set<String> terminals = ctx.mm().terminalStateCodes(typeCode);
        if (terminals.isEmpty()) {
            throw new InvalidTransitionException(current, "",
                    "no terminal state configured for type " + typeCode);
        }
        if (terminals.contains(current)) {
            return; // already closed — idempotent
        }
        String target = null;
        for (String terminal : terminals) {
            if (ctx.mm().transitionAllowed(typeCode, current, terminal)) {
                target = terminal;
                break;
            }
        }
        if (target == null) {
            throw new InvalidTransitionException(current, String.join("|", terminals),
                    "no configured transition from " + current + " to a terminal state");
        }
        c.setProperty("currentState", target);
        ctx.saveCase();
        ctx.events().append(ctx.caseId(), "CASE_CLOSED", ctx.actor(),
                current, target, "case closed", null);
    }
}
