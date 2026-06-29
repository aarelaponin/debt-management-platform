package com.fiscaladmin.mtca.cmbb.service;

/**
 * Thrown by a guard phase when a state transition is not permitted by the
 * mm_* configuration (WF-FR-001: "Invalid transitions rejected with user
 * notification"). Pattern adapted from the status-framework precedents
 * (gam-framework / joget-status-framework) — see Plugin-Dev-Baseline.md.
 */
public class InvalidTransitionException extends RuntimeException {

    private final String fromState;
    private final String toState;

    public InvalidTransitionException(String fromState, String toState, String reason) {
        super(reason);
        this.fromState = fromState;
        this.toState = toState;
    }

    public String getFromState() {
        return fromState == null ? "" : fromState;
    }

    public String getToState() {
        return toState == null ? "" : toState;
    }
}
