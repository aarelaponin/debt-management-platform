# FIS — DMBB-F05 — Enforcement trigger + strategy-admin consistency
Status: Accepted (T-14.1..3 3/3 PASS on jdx9, 2026-06-13; 84/84 unit; full regression run_t02..t14 green). DMBB slice S2 (F05) — closes S2.
CAD ref: CAD-DMBB §7 row F05 (escalation-admin + enforcement trigger); DM-FR-015..020, BR-DM-030. Builds on F04 + CMBB-F08 holds.

## 0. Approach (no new plugin)
Two capabilities, both folded into the F04 **EscalationEngine** (no new Activator registration):
1. **Enforcement trigger (BR-DM-030)** — a gate inside the ladder walk: an *enforcement* step (a recovery
   instrument, not a reminder/demand notice) fires only when the case is **eligible** — no full-amount
   objection (Σ disputed ≥ Σ enforceable on the debt lines) and no active **ENFORCEMENT_SUPPRESS** hold
   (an approved instalment asserts that hold via F06/F08, so the "no active instalment" condition rides on
   it). Eligible → `ENFORCEMENT_TRIGGERED`; blocked → `ENFORCEMENT_BLOCKED` and the case holds at final demand.
2. **Strategy-admin consistency (DM-FR-015)** — `EscalationEngine` VALIDATE mode (on a `cmStrategyCheck`
   trigger) runs **StrategyAdminService**: every escalation step references an *enabled* instrument, the
   active strategies cover every band C2–C6, and no two active strategies for the same (segment, floor) have
   overlapping effective windows. The dry-run/consistency gate that makes self-service strategy authoring safe.

## 1. Traceability
| FR / BR | AC (paraphrase) | Realised by | Test |
|---|---|---|---|
| BR-DM-030 | enforcement eligible when final demand expired AND no active instalment AND no full-amount objection | EscalationService gate: enforcement step requires no full-amount objection (dmLine Σdisputed ≥ Σenforceable) and no active ENFORCEMENT_SUPPRESS hold (cmHold); final-demand-expired is the step ordering | T-14.1, T-14.2 |
| DM-FR-015 | configurable FSM workflows, manual override; safe authoring | StrategyAdminService validates active strategies (enabled instruments, C2–C6 coverage, no overlapping windows) via cmStrategyCheck VALIDATE | T-14.3, T-14.unit |
| DM-FR-016 | escalate to manual CM when automated stages exhausted | the case stays in the CMBB worklist throughout (allocation F03/CMBB); an enforcement-eligible case is flagged ENFORCEMENT_TRIGGERED for officer action (manual enforcement = F07) | (covered by CMBB queues) |

## 2. Design decisions
1. **A1 — enforcement step heuristic.** A step is enforcement when its `instrument` is set and ≠ `DEMAND` (reminder steps carry no instrument; demand/final carry `DEMAND`). Everything else (BANK_GARNISH, PROPERTY_SEIZURE, …) is a recovery action and gated. A per-instrument `isEnforcement` flag is recorded as a refinement.
2. **A2 — instalment condition via the hold.** Rather than read a not-yet-built `dmInstAgr` (F06), eligibility checks the **ENFORCEMENT_SUPPRESS** hold that an approved instalment (F06) / insolvency will assert — the F08 HoldConnector contract. So F05's gate is forward-compatible with F06.
3. **A3 — objection from the snapshot.** Full-amount objection = Σ `dmLine.disputed` ≥ Σ `dmLine.enforceable` (the case's stored debt lines, D-SAD-04 snapshot) — no recompute.
4. **A4 — VALIDATE reuses the engine.** The strategy check is a second mode on EscalationEngine (cmStrategyCheck) — no new plugin, no Activator change. The full approval-gated *activation-as-case* flow leans on the CMBB policy-change case type (recorded; the consistency gate is the build here).

## 3. Generation order
1. gen_forms: F-cmStrategyCheck → dmbb/generated/forms.
2. gen_datalists (forms) → companion.
3. gen_userview: ALL dmbb UV-deltas (F01..F05) → dmbbConsole gains a **Strategy admin** category.
4. Plugin: EscalationService gate + StrategyAdminService + EscalationEngine VALIDATE + unit tests (built 84/84). No new properties (VALIDATE on the cmStrategyCheck postProcessor).
5. Deploy cmbb (JAR) + dmbb; no new seed (reuses F01 strategy/instruments + F04 templates). run_t14 + full regression.
