# PL-HoldConnector — CMBB Hold service (CMBB-F08)
Type: DefaultApplicationPlugin (form post-processor). Bundle: cmbb-plugins.
Forcing: GCMF §3.3-8, DPM D8, enrichment E1. CAD §2.2 plugin-budget row "HoldConnector".

## Modes (property `mode`)
| Mode | Trigger form | Action |
|---|---|---|
| ASSERT | cmHold (runOn: create) | resolve newest unprocessed cmHold (status REQUESTED/blank) → set status ACTIVE, assertedBy/assertedAt, stamp targetBB; append HOLD_ASSERTED; if scope ∈ {CORRESPONDENCE_SUPPRESS, ENFORCEMENT_SUPPRESS} the suppression is now live (consumed by DispatchService / DMBB enforcement) |
| RELEASE | cmHoldRelease (runOn: create) | load cmHold by holdId → set RELEASED, releasedBy/releaseReason; append HOLD_RELEASED; suppression lifts (hold↔state consistency) |

## DAO contract
- updateSchema(cmHold) before first read/write (DX9-DELTAS tx-poison rule).
- Reads: cmHold (by id / newest unprocessed), cmCase (for caseRef/tin).
- Writes: cmHold.status; cmEvent (HOLD_ASSERTED / HOLD_RELEASED, hash-chained via CaseEventWriter).
- No cross-BB write in v1: financial/enforcement scopes record targetBB + event; the actual
  collection/disbursement stay is a DMBB-era HoldConnector→BB call (CAD §4 contract, recorded).

## Service: HoldService (constructor-injected dao, mm, events) — unit-tested on GuardTestHarness.
`assert(holdId, actor)` · `release(releaseId, actor)`. Idempotent: an already-ACTIVE / already-RELEASED
hold is a no-op that still returns a result string. No exception path — failures route to result text.

## Events
HOLD_ASSERTED {scope, holdType, basis, targetBB} · HOLD_RELEASED {holdId, reason}.
