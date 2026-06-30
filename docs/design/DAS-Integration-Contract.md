# DAS Integration Contract — how a module becomes an approval consumer

**Status:** v1.0 · 2026-06-30 · The DAS is a **platform-shared service** in the CMBB spine
(ADR-006). A module plugs in through **two seams** and **zero engine changes**: it (1) raises a
request from its lifecycle, and (2) registers a `DecisionEffect` for its action. The matrix, COI,
SLA, escalation, delegation, lifecycle and audit are the gate's, not the consumer's.

## Seam 1 — raise a request

```
ApprovalEffects.service(dao).request(
    entity,      // the consumer's form id, e.g. "dmInstAgr" / "dmWriteOff"
    recordId,    // the row awaiting approval
    actionType,  // the key into mmAuthority, e.g. "INSTALMENT_PLAN" / "WRITE_OFF"
    materiality, // the amount that selects the authority band (double)
    requester,   // who is asking (for four-eyes SoD)
    caseId,      // the case anchor (events + COI subject resolution)
    actor, now)
```

Behaviour: resolves the `mmAuthority` band for `(actionType, materiality)`. **No band → auto-pass**:
the effect runs immediately and `APPROVAL_NOT_REQUIRED` is logged. **A band → a `Pending`
`cmApproval`** routed per the band's topology, and `APPROVAL_REQUESTED` is logged. Idempotent: a
second request for the same `(recordId, actionType)` while one is pending is a no-op.

## Seam 2 — register a DecisionEffect

One line in `ApprovalEffects.registry(dao)` — the single registration point:

```
effects.put(ACTION_WRITEOFF, (entity, recordId, actor, now) ->
        new WriteOffService(dao, …).approve(recordId, actor, now));
```

The effect is the consumer's "what happens when approved". Contract: it runs **exactly once**, only
on full approval (or auto-pass); it returns a short result string; it **must tolerate re-entry**
(be idempotent on its own row) even though the gate guards fire-once via the `Pending` lifecycle.

## What the consumer configures (no code)

- **`mmAuthority`** rows — `actionType × [amountMin,amountMax] → level + bodyType(SINGLE/CHAIN/
  COLLEGIAL) + quorum`. This *is* the policy: who, how many, in what shape, per amount band.
  (From P1: SLA + max-escalations + effective-dating live here too.)
- **`mmCoi`** rows (optional) — `EXCLUDE_APPROVER` rules (`approver|tin`, `*` wildcards) to bar
  conflicted approvers for the action's case type.
- The **`cmApproval` lifecycle** (`mmEntityTransition` Pending→Approved/Rejected/Returned) is shared
  and already seeded — consumers do not re-seed it.

## What the gate guarantees (so the consumer doesn't reimplement it)

Authority resolution by band; SINGLE / sequential-CHAIN / collegial-QUORUM routing with distinct
voters; rank gate; four-eyes SoD; mandatory reasons; conflict-of-interest exclusion; SLA escalation
+ timeout (auto-reject, never auto-approve); delegation; effect-fires-once; and a reasoned audit
trail on the case hash-chain — `APPROVAL_REQUESTED / _NOT_REQUIRED / _DECISION / _SOD_BLOCKED /
_RANK_BLOCKED / _COI_BLOCKED / _ESCALATED / _TIMEOUT / _DELEGATED`.

## Onboard a new action — the recipe (write-off is the worked example, P2)

1. Add the effect line in `ApprovalEffects.registry` (`ACTION_WRITEOFF → WriteOffService.approve`).
2. Call `request(…, "WRITE_OFF", amount, …)` from the action's submit (replace any bespoke approval).
3. Seed `mmAuthority` bands for `WRITE_OFF` (and `mmCoi` if needed).
4. Surface the request in the Approvals inbox (automatic — the inbox is action-agnostic).
5. Acceptance: a `run_tNN` proving auto-pass / routed / approved-effect, then full regression green.

No new approval flow, no new forms beyond the consumer's own row carrier, no engine edit.
