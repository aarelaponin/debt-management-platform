# FIS — CMBB-F10 Status state machine (foundation)

**Status:** Draft (Stage 2) · **ADR:** ADR-003 (Accepted) · **Date:** 2026-06-17
**Scope of THIS increment (ADR §5 step 1 — low risk, no engine migration):** the config carriers + a
**dormant** `StatusManager`. No engine is rewired yet, so the existing engines/suites are untouched and
the full regression must stay GREEN unchanged. Engine migration (replacing `setProperty(status,…)` with
`StatusManager.transition(…)`) happens in later per-entity passes.

## 1. Principles (from ADR-003 §0)
- **Joget-native, no databases:** lifecycle config + audit live in Joget `mm*`/`cm*` forms, read/written
  only via `FormDataDao` (P3). No external bundle, no new DB/table-by-hand, no SQL, no new process.
- **Configurable:** lifecycles are data (`mmEntityState`/`mmEntityTransition`), editable via normal Joget
  admin screens — no redeploy to change a workflow.
- **Per-tax-type:** `scope` resolved **most-specific-first `taxType ▸ caseType ▸ DEFAULT`**, **wholesale
  shadowing** (the most-specific scope that has ANY rows wins entirely — predictable, testable).

## 2. Artefacts
- Forms (CMBB-owned metamodel): `F-mmEntityState`, `F-mmEntityTransition` (this folder).
- Service (shared `cmbb-plugins`): `StatusManager` + `MmConfigService` entity-transition lookups
  (Stage 3). Dormant — exposes `canTransition(entity,scope,from,to)`, `validNext(entity,scope,from)`,
  `transition(dao,entity,recordId,target,scope,actor,reason)` (writes status via FormDataDao + appends a
  `STATUS_CHANGED` `cmEvent`; throws `InvalidTransitionException`).
- Seeds: `seed/mmEntityState.csv`, `seed/mmEntityTransition.csv` (baseline `scope=DM` lifecycles + one
  `scope=VAT` override to prove C4). Loaded via `API-cmbb-data`.
- Tests: `tests/run_t27.py` — config-driven assertions on the dormant service (legal allowed, illegal
  rejected, terminal has no outgoing, VAT scope shadows DM), no engine touched.

## 3. Baseline lifecycles (scope = DM) — the seed

| Entity | Initial | Transitions (from → to) | Terminal |
|---|---|---|---|
| `dmInstLine` | pending | pending→paid, pending→missed | paid, missed |
| `dmAction` | INITIATED | INITIATED→EXECUTED, INITIATED→SUBMITTED, INITIATED→BLOCKED, INITIATED→REFERRED, SUBMITTED→CONFIRMED, SUBMITTED→FAILED | EXECUTED, CONFIRMED, BLOCKED, FAILED, REFERRED |
| `dmWriteOff` | SUBMITTED | SUBMITTED→UNDER_REVIEW, SUBMITTED→POSTED, UNDER_REVIEW→APPROVED, UNDER_REVIEW→REJECTED, APPROVED→POSTED | POSTED, REJECTED |
| `dmDefAssess` | DRAFT | DRAFT→ASSESSED, DRAFT→NEEDS_JUSTIFICATION, NEEDS_JUSTIFICATION→ASSESSED, ASSESSED→REPLACED, ASSESSED→ESCALATED | REPLACED, ESCALATED |
| `dmInstAgr` | APPLIED | APPLIED→ACTIVE, APPLIED→UNDER_REVIEW, APPLIED→REJECTED, UNDER_REVIEW→ACTIVE, UNDER_REVIEW→REJECTED, ACTIVE→COMPLETED, ACTIVE→CANCELLED | COMPLETED, CANCELLED, REJECTED |
| `dmDebt` (stage) | Identified | Identified→Reminder, Reminder→Demand, Demand→Final demand, Final demand→Enforcement | (none — ladder end) |

**Per-tax proof (scope = VAT, `dmDebt`):** VAT skips the informal reminder — Identified→Demand,
Demand→Final demand, Final demand→Enforcement. Because shadowing is wholesale, the VAT row-set fully
replaces the DM ladder for VAT debts; PIT etc. keep the DM baseline.

## 4. Acceptance (run_t27, dormant service)
- T-27.1 `canTransition(dmWriteOff, DM, SUBMITTED, UNDER_REVIEW)` = true; `(…, REJECTED, ACTIVE)` = false.
- T-27.2 `validNext(dmAction, DM, INITIATED)` = {EXECUTED,SUBMITTED,BLOCKED,REFERRED}.
- T-27.3 terminal state has empty `validNext` (e.g. `dmWriteOff/POSTED`).
- T-27.4 scope shadowing: `canTransition(dmDebt, VAT, Identified, Demand)` = true while
  `(dmDebt, DM, Identified, Demand)` = false (DM requires Reminder first).
- T-27.5 `transition(...)` on a legal move writes the status (FormDataDao) + appends one `STATUS_CHANGED`
  `cmEvent`; an illegal move throws and writes nothing.
- Full regression run_t02..t26 unchanged GREEN (engines untouched).
