package com.fiscaladmin.mtca.cmbb.phase;

import java.util.Set;

import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.InvalidTransitionException;
import com.fiscaladmin.mtca.cmbb.service.TenantContext;

/**
 * OPEN phase (PL-TransitionGuard.md): case type registered+active, TTT scope,
 * dedup per type policy, caseRef generation, New->Open transition, and the
 * re-open path (WF-FR-005: terminal -> Open when mmTransition permits, event
 * linked to the closing event hash).
 */
public class OpenPhase implements GuardPhase {

    @Override
    public void run(GuardContext ctx) {
        FormRow c = ctx.caseRow();
        String typeCode = ctx.prop("caseType");
        FormRow type = ctx.mm().caseType(typeCode);
        if (type == null || !"true".equalsIgnoreCase(type.getProperty("active"))) {
            throw new InvalidTransitionException("", "",
                    "case type not registered or inactive: " + typeCode);
        }
        FormRow openState = ctx.mm().stateByEnvelope(typeCode, "Open");
        if (openState == null) {
            throw new InvalidTransitionException("", "",
                    "no Open-envelope state configured for type " + typeCode);
        }
        String openCode = openState.getProperty("code");
        String current = ctx.prop("currentState");
        Set<String> terminals = ctx.mm().terminalStateCodes(typeCode);

        // ---- re-open path (case already in a terminal state) ----
        if (!current.isEmpty() && terminals.contains(current)) {
            if (!ctx.mm().transitionAllowed(typeCode, current, openCode)) {
                throw new InvalidTransitionException(current, openCode,
                        "re-open not permitted: no configured transition "
                                + current + "->" + openCode);
            }
            String closingHash = ctx.events().lastEventHash(ctx.caseId());
            String extra = "\"linkedEventHash\":\"" + CaseEventWriter.esc(closingHash)
                    + "\",\"caseRef\":\"" + CaseEventWriter.esc(ctx.prop("caseRef")) + "\"";
            c.setProperty("currentState", openCode);
            ctx.saveCase();
            ctx.events().append(ctx.caseId(), "CASE_REOPENED", ctx.actor(),
                    current, openCode, "case re-opened", extra);
            return;
        }

        // ---- fresh-open path ----
        FormRow newState = ctx.mm().stateByEnvelope(typeCode, "New");
        String fromCode = current.isEmpty()
                ? (newState != null ? newState.getProperty("code") : "")
                : current;

        if ("OBLIGATION".equalsIgnoreCase(type.getProperty("ttScope"))
                && (ctx.prop("taxType").isEmpty() || ctx.prop("taxPeriod").isEmpty())) {
            throw new InvalidTransitionException(fromCode, openCode,
                    "tax type and period required for obligation-scoped case");
        }

        String policy = type.getProperty("dedupPolicy");
        if (policy == null || policy.isEmpty()) {
            policy = "SKIP_IF_ACTIVE";
        }
        if (!"ALLOW".equalsIgnoreCase(policy) && hasActiveDuplicate(ctx, type, terminals)) {
            throw new InvalidTransitionException(fromCode, openCode,
                    "duplicate case for same taxpayer/tax type/period (policy " + policy + ")");
        }

        if (fromCode != null && !fromCode.isEmpty() && !fromCode.equals(openCode)
                && !ctx.mm().transitionAllowed(typeCode, fromCode, openCode)) {
            throw new InvalidTransitionException(fromCode, openCode,
                    "no configured transition " + fromCode + "->" + openCode);
        }

        if (ctx.prop("caseRef").isEmpty()) {
            c.setProperty("caseRef",
                    ctx.refs().generate(typeCode, type.getProperty("idFormat")));
        }
        // ADR-004 multi-tenancy: stamp the tenant from the opener's profile (invisible scope).
        // Resolved once at open; never shown or picked. Blank-safe (TenantContext defaults).
        if (ctx.prop("tenant").isEmpty()) {
            c.setProperty("tenant", new TenantContext(ctx.dao()).resolve(ctx.actor()));
        }
        c.setProperty("currentState", openCode);
        ctx.saveCase();
        ctx.events().append(ctx.caseId(), "CASE_CREATED", ctx.actor(),
                "", fromCode, "case created (origin " + ctx.prop("origin") + ")", null);
        ctx.events().append(ctx.caseId(), "CASE_OPENED", ctx.actor(),
                fromCode, openCode, "case opened", null);
    }

    /** WF-FR-003 dedup: another non-terminal case of the same type for the same TIN
     *  (and tax type/period when obligation-scoped). */
    private boolean hasActiveDuplicate(GuardContext ctx, FormRow type, Set<String> terminals) {
        boolean obligation = "OBLIGATION".equalsIgnoreCase(type.getProperty("ttScope"));
        String cond = "WHERE e.customProperties.caseType = ?1 AND e.customProperties.tin = ?2"
                + " AND e.id <> ?3";
        Object[] params;
        if (obligation) {
            cond += " AND e.customProperties.taxType = ?4 AND e.customProperties.taxPeriod = ?5";
            params = new Object[]{ctx.prop("caseType"), ctx.prop("tin"), ctx.caseId(),
                    ctx.prop("taxType"), ctx.prop("taxPeriod")};
        } else {
            params = new Object[]{ctx.prop("caseType"), ctx.prop("tin"), ctx.caseId()};
        }
        FormRowSet others = ctx.dao().find(GuardContext.F_CASE, GuardContext.F_CASE,
                cond, params, "dateCreated", Boolean.FALSE, null, null);
        if (others == null) {
            return false;
        }
        for (FormRow other : others) {
            String st = other.getProperty("currentState");
            if (st == null || st.isEmpty() || !terminals.contains(st)) {
                return true; // not (yet) terminal -> active duplicate
            }
        }
        return false;
    }
}
