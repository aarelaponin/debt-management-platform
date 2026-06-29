# PL-DecisionEngine — CMBB Decision / Linkage / Pending-Info (CMBB-F08)
Type: DefaultApplicationPlugin (form post-processor). Bundle: cmbb-plugins.
Forcing: GCMF §3.3-6/7/10, DPM D6. **Budget growth** (9→11) recorded as CAD amendment — the
CAD assumed Joget native multi-stage approval (no plugin); our one-envelope / no-new-XPDL
convention (DX9-DELTAS, §5) forbids it, so a P3-clean form post-processor is the only trigger.

## Modes (property `mode`)
| Mode | Trigger form | Action |
|---|---|---|
| DECIDE | cmDecision (create) | resolve required authority level from mmAuthority (actionType × amount band) → rank-compare approverLevel → set decisionStatus APPROVED / REJECTED; collegial: approvalsCount ≥ quorum; append DECISION_PROPOSED then DECISION_APPROVED / DECISION_REJECTED |
| LINK | cmLink (create) | validate linkType against mmLinkType.targetCaseTypes; resolve toCaseId by caseRef; write reciprocal cmLink when target present; append CASE_LINKED (both rows) |
| INFO_REQUEST | cmInfoRequest (create) | record priorState; move case to its OnHold-envelope state (if any); open cmTask(PROVIDE_INFO, OPEN); append INFO_REQUESTED + NOTIF_PENDING (taxpayer) |
| INFO_RESPONSE | cmInfoResponse (create) | load request by requestId; restore priorState; close the PROVIDE_INFO task; append INFO_RECEIVED |

## DAO contract
- updateSchema(cmDecision, cmLink, cmTask, cmInfoRequest) before first read of each (tx-poison rule).
- Reads: mmAuthority, mmLinkType, cmCase, cmDecision, cmInfoRequest, cmTask.
- Writes: cmDecision.decisionStatus/result; cmLink (+ reciprocal); cmTask; cmCase.currentState
  (info park/resume); cmEvent (DECISION_*, CASE_LINKED, INFO_*, NOTIF_PENDING).

## Services (constructor-injected) — unit-tested on GuardTestHarness
- DecisionService: requiredLevel(actionType, amount) → mmAuthority row; rank(level); decide(decisionId, actor).
  Rank order: OFFICER<SENIOR<SUPERVISOR<MANAGER<DIRECTOR<COMMISSIONER (unknown=0). reasons mandatory.
- LinkService: link(linkId, actor) — permitted-target validation, reciprocal write.
- PendingInfoService: request(reqId, actor), respond(respId, actor) — park/resume + task lifecycle.

## Consumed-by
- ClosePhase (TransitionGuard CLOSE, requireDecision): existence of DECISION_APPROVED for the case.
- AllocationService (EXCLUDE_DECISION_MAKER): cmDecision.decidedBy for the case's TIN / linked cases.
