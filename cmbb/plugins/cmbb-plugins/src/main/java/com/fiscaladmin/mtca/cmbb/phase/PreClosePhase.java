package com.fiscaladmin.mtca.cmbb.phase;

import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;

/**
 * PRE_CLOSE phase: no open tasks, required documents per current state
 * present, and a configured closure path exists from the current state.
 * When the type has a PendingClosure-envelope state, the case is moved there
 * (STATE_CHANGED event); minimal lifecycles without one (e.g. the TEST seed)
 * validate the path only and leave the move to CLOSE.
 */
public class PreClosePhase implements GuardPhase {

    @Override
    public void run(GuardContext ctx) {
        FormRow c = ctx.caseRow();
        String typeCode = ctx.prop("caseType");
        String current = ctx.prop("currentState");

        // 1. no open tasks (cmTask.status = OPEN)
        Long openTasks = ctx.dao().count(GuardContext.F_TASK, GuardContext.F_TASK,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.status = ?2",
                new Object[]{ctx.caseId(), "OPEN"});
        if (openTasks != null && openTasks > 0) {
            throw new InvalidTransitionException(current, "",
                    openTasks + " open task(s) must be completed before closure");
        }

        // 2. required documents for the current state (mmDocReq x cmDoc).
        // cmDoc arrives with F03 (Mayan register) — when the carrier table is
        // not deployed yet, configured requirements cannot be satisfied.
        FormRowSet reqs = ctx.mm().requiredDocs(typeCode, current);
        if (reqs != null && !reqs.isEmpty()) {
            // reads never auto-create tables and a missing relation poisons the
            // tx (DX9-DELTAS) — cmDoc form ships with F07, ensure table exists
            ctx.dao().updateSchema(GuardContext.F_DOC, GuardContext.F_DOC, new FormRowSet());
            for (FormRow req : reqs) {
                String docClass = req.getProperty("docClass");
                Long n = ctx.dao().count(GuardContext.F_DOC, GuardContext.F_DOC,
                        "WHERE e.customProperties.caseId = ?1 AND e.customProperties.docClass = ?2",
                        new Object[]{ctx.caseId(), docClass});
                long present = n == null ? 0 : n;
                if (present == 0) {
                    throw new InvalidTransitionException(current, "",
                            "required document missing for state " + current + ": " + docClass);
                }
            }
        }

        // 3. closure path: prefer a PendingClosure-envelope state; otherwise a
        // terminal state must be reachable from the current state.
        FormRow pending = ctx.mm().stateByEnvelope(typeCode, "PendingClosure");
        if (pending != null) {
            String pendingCode = pending.getProperty("code");
            if (!ctx.mm().transitionAllowed(typeCode, current, pendingCode)) {
                throw new InvalidTransitionException(current, pendingCode,
                        "no configured transition " + current + "->" + pendingCode);
            }
            c.setProperty("currentState", pendingCode);
            ctx.saveCase();
            ctx.events().append(ctx.caseId(), "STATE_CHANGED", ctx.actor(),
                    current, pendingCode, "pending closure", null);
        } else {
            boolean reachable = false;
            String target = "";
            for (String terminal : ctx.mm().terminalStateCodes(typeCode)) {
                if (ctx.mm().transitionAllowed(typeCode, current, terminal)) {
                    reachable = true;
                    target = terminal;
                    break;
                }
            }
            if (!reachable) {
                throw new InvalidTransitionException(current, target,
                        "no configured closure transition from state " + current);
            }
            // path validated; state move happens in CLOSE
        }
    }
}
