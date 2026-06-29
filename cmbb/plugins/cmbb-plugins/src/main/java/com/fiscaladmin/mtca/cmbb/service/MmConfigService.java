package com.fiscaladmin.mtca.cmbb.service;

import java.util.HashSet;
import java.util.Set;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Read-side gateway to the CMBB metamodel (mm_*) configuration tables.
 * All lifecycle rules live in data (P4 — tables-in-config); this service is
 * the only place the guard reads them. Field IDs match the F01 form specs
 * exactly (no c_ prefix in Java — FormDataDao rule).
 */
public class MmConfigService {

    public static final String F_CASE_TYPE = "mmCaseType";
    public static final String F_STATE = "mmState";
    public static final String F_TRANSITION = "mmTransition";
    public static final String F_DOC_REQ = "mmDocReq";

    private final FormDataDao dao;

    public MmConfigService(FormDataDao dao) {
        this.dao = dao;
    }

    /** Case type row by code, or null when not registered. */
    public FormRow caseType(String code) {
        FormRowSet rows = dao.find(F_CASE_TYPE, F_CASE_TYPE,
                "WHERE e.customProperties.code = ?1", new Object[]{code},
                "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** True when an mmTransition row permits fromState -> toState for the type. */
    public boolean transitionAllowed(String caseTypeCode, String fromState, String toState) {
        Long n = dao.count(F_TRANSITION, F_TRANSITION,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.fromState = ?2"
                        + " AND e.customProperties.toState = ?3",
                new Object[]{caseTypeCode, fromState, toState});
        return n != null && n > 0;
    }

    /** All transitions leaving fromState for the type. */
    public FormRowSet transitionsFrom(String caseTypeCode, String fromState) {
        return dao.find(F_TRANSITION, F_TRANSITION,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.fromState = ?2",
                new Object[]{caseTypeCode, fromState},
                "dateCreated", Boolean.FALSE, null, null);
    }

    /** First active state of the type carrying the given envelope state, or null. */
    public FormRow stateByEnvelope(String caseTypeCode, String envelopeState) {
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.envelopeState = ?2",
                new Object[]{caseTypeCode, envelopeState},
                "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** Codes of terminal states for the type (isTerminal checkbox = 'true'). */
    public Set<String> terminalStateCodes(String caseTypeCode) {
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.isTerminal = ?2",
                new Object[]{caseTypeCode, "true"},
                "dateCreated", Boolean.FALSE, null, null);
        Set<String> codes = new HashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                codes.add(r.getProperty("code"));
            }
        }
        return codes;
    }

    public static final String F_ALLOC = "mmAlloc";
    public static final String F_COI = "mmCoi";
    public static final String F_OFFICER = "mdOfficerProfile";

    /** First allocation policy row for the type, or null (CMBB-F03). */
    public FormRow allocPolicy(String caseTypeCode) {
        FormRowSet rows = dao.find(F_ALLOC, F_ALLOC,
                "WHERE e.customProperties.caseType = ?1", new Object[]{caseTypeCode},
                "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** COI rules configured for the type (CMBB-F03). */
    public FormRowSet coiRules(String caseTypeCode) {
        return dao.find(F_COI, F_COI,
                "WHERE e.customProperties.caseType = ?1", new Object[]{caseTypeCode},
                "dateCreated", Boolean.FALSE, null, null);
    }

    /** Active officer profiles, ordered by code (deterministic round-robin base). */
    public FormRowSet activeOfficers() {
        return dao.find(F_OFFICER, F_OFFICER,
                "WHERE e.customProperties.active = ?1", new Object[]{"true"},
                "code", Boolean.FALSE, null, null);
    }

    /** Terminal state codes across ALL case types (workload counting). */
    public Set<String> allTerminalCodes() {
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.isTerminal = ?1", new Object[]{"true"},
                "dateCreated", Boolean.FALSE, null, null);
        Set<String> codes = new HashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                codes.add(r.getProperty("code"));
            }
        }
        return codes;
    }

    public static final String F_SLA = "mmSla";
    public static final String F_CALENDAR = "mmCalendar";

    /** All SLA clock definitions for the type (CMBB-F05). */
    public FormRowSet slaRows(String caseTypeCode) {
        return dao.find(F_SLA, F_SLA,
                "WHERE e.customProperties.caseType = ?1", new Object[]{caseTypeCode},
                "clockCode", Boolean.FALSE, null, null);
    }

    /** One SLA clock definition, or null. */
    public FormRow slaRow(String caseTypeCode, String clockCode) {
        FormRowSet rows = dao.find(F_SLA, F_SLA,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.clockCode = ?2",
                new Object[]{caseTypeCode, clockCode}, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** Calendar row by code, or null (CALENDAR-day behaviour). */
    public FormRow calendarRow(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        FormRowSet rows = dao.find(F_CALENDAR, F_CALENDAR,
                "WHERE e.customProperties.code = ?1", new Object[]{code},
                "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** Envelope state of a per-type state code, or "". */
    public String envelopeOf(String caseTypeCode, String stateCode) {
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.code = ?2",
                new Object[]{caseTypeCode, stateCode}, "dateCreated", Boolean.FALSE, 0, 1);
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        String env = rows.get(0).getProperty("envelopeState");
        return env == null ? "" : env;
    }

    /** Required-document rows for (type, state). Empty set when none configured. */
    public FormRowSet requiredDocs(String caseTypeCode, String stateCode) {
        return dao.find(F_DOC_REQ, F_DOC_REQ,
                "WHERE e.customProperties.caseType = ?1 AND e.customProperties.stateCode = ?2"
                        + " AND e.customProperties.required = ?3",
                new Object[]{caseTypeCode, stateCode, "true"},
                "dateCreated", Boolean.FALSE, null, null);
    }

    // ── ADR-003: entity-status state machine (mmEntityState / mmEntityTransition) ──
    public static final String F_ENTITY_STATE = "mmEntityState";
    public static final String F_ENTITY_TRANSITION = "mmEntityTransition";

    /** All transition rows for (entity, scope). */
    public FormRowSet entityTransitions(String entity, String scope) {
        return dao.find(F_ENTITY_TRANSITION, F_ENTITY_TRANSITION,
                "WHERE e.customProperties.entity = ?1 AND e.customProperties.scope = ?2",
                new Object[]{entity, scope}, "dateCreated", Boolean.FALSE, null, null);
    }

    /** True when (entity, scope) has any transition rows configured (used for scope resolution). */
    public boolean entityScopeHasRules(String entity, String scope) {
        Long n = dao.count(F_ENTITY_TRANSITION, F_ENTITY_TRANSITION,
                "WHERE e.customProperties.entity = ?1 AND e.customProperties.scope = ?2",
                new Object[]{entity, scope});
        return n != null && n > 0;
    }

    /** Terminal status codes for (entity, scope). */
    public Set<String> entityTerminalStatuses(String entity, String scope) {
        FormRowSet rows = dao.find(F_ENTITY_STATE, F_ENTITY_STATE,
                "WHERE e.customProperties.entity = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.isTerminal = ?3",
                new Object[]{entity, scope, "true"}, "dateCreated", Boolean.FALSE, null, null);
        Set<String> codes = new HashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                codes.add(r.getProperty("code"));
            }
        }
        return codes;
    }

    /** Initial status code for (entity, scope), or null. */
    public String entityInitialStatus(String entity, String scope) {
        FormRowSet rows = dao.find(F_ENTITY_STATE, F_ENTITY_STATE,
                "WHERE e.customProperties.entity = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.isInitial = ?3",
                new Object[]{entity, scope, "true"}, "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getProperty("code");
    }
}
