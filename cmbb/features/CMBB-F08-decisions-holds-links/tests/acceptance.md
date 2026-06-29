# Acceptance — CMBB-F08 (T-08.1 … T-08.8)
Run on jdx9 (live). Scripted in `run_t08.py` (model: run_t07.py). Read-only SQL asserts;
all writes via the form API. Per-run unique ids. Full regression run_t02..run_t08 after.

| # | Name | Setup | Assertion |
|---|---|---|---|
| T-08.1 | Hold assert | POST cmHold {scope CORRESPONDENCE_SUPPRESS, status blank} on a live case | cmHold row → status ACTIVE; one cmEvent eventType=HOLD_ASSERTED for the case; assertedAt set |
| T-08.2 | Suppression blocks dispatch | with the ACTIVE CORRESPONDENCE_SUPPRESS hold, emit a NOTIF_PENDING (rule match) and run DISPATCH | a cmNotif row for the case has status SUPPRESSED; no SENT row for that event (HOLD_CHECK_ENABLED proven) |
| T-08.3 | Hold release resumes | POST cmHoldRelease {holdId} then re-run DISPATCH on a fresh NOTIF_PENDING | cmHold → RELEASED; HOLD_RELEASED event; new cmNotif status SENT (suppression lifted) |
| T-08.4 | Closure decision gate | case type with requireDecision; attempt guardClosure→CLOSE with NO approved decision, then POST cmDecision (sufficient authority) and retry | first CLOSE rejected (TRANSITION_REJECTED, "decision"); after DECISION_APPROVED, CLOSE succeeds (currentState terminal, CASE_CLOSED) |
| T-08.5 | Authority insufficient | POST cmDecision {actionType WRITE_OFF, amount above band for approverLevel} | decisionStatus REJECTED; DECISION_REJECTED event; no DECISION_APPROVED → close still blocked |
| T-08.6 | Case linkage both-way | POST cmLink {linkType REFERRAL, toCaseRef of a permitted-type case} | forward cmLink result OK + reciprocal row written (reciprocal=true); CASE_LINKED event on both; impermissible target type → result REJECTED, no row |
| T-08.7 | Decision-maker independence | mmCoi EXCLUDE_DECISION_MAKER; an APPROVED cmDecision by officer1 on TIN T; create a new case for T and ASSIGN | officer1 not chosen (CASE_ASSIGNED.officer ≠ officer1) when an alternative exists; ASSIGNMENT considered-list shows officer1 excluded |
| T-08.8 | Pending-info loop | POST cmInfoRequest on an Open case, then cmInfoResponse {requestId} | after request: case currentState = OnHold-envelope state + cmTask PROVIDE_INFO OPEN + INFO_REQUESTED; after response: currentState restored to priorState + task CLOSED + INFO_RECEIVED |

Regression: T-02 (8), T-03 (7), T-04 (6), T-05 (7), T-06 (7), T-07 (5) all green after the F08 deploy.
Unit: HoldServiceTest, DecisionServiceTest, LinkServiceTest (+ pending-info), updated ClosurePhasesTest
and AllocationServiceTest — all green in `mvn clean package`.
