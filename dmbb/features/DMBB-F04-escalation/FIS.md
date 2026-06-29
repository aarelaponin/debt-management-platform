# FIS — DMBB-F04 — Escalation engine (reminders → demand → final demand → enforcement)
Status: Accepted (T-13.1..4 4/4 PASS on jdx9, 2026-06-13; 79/79 unit; full regression run_t02..t13 green). DMBB slice S2 (F04).
CAD ref: CAD-DMBB §7 row F04 (EscalationEngine + notices via Mayan); DM-FR-009…014, BR-DM-005…012.

## 0. Approach
**EscalationEngine** (new engine in `cmbb-plugins`, 11th) is a `cmEscalateRun` trigger-row sweep (the
cmSweepRun/DeadlineEngine pattern). It walks the F01 collection strategy (`mmStrategy` + ordered
`mmEscStep`) against each open DM case's **elapsed time since identification** (`cmCase.dateCreated`,
time-travelled by the run's `asOf`), category-gated (BR-DM-006), advancing `dmDebt.stage`/`lastStepSeq`
and emitting a `NOTIF_PENDING` cmEvent whose `reason` is the step's notice template — so the **existing F06
NotificationDispatcher** issues the reminder/demand/final-demand/enforcement notice. No new XPDL, no new
notification machinery — F04 is configuration (the F01 ladder) + one ladder-walking engine + DM notice seeds.

## 1. Traceability
| FR / BR | AC (paraphrase) | Realised by | Test |
|---|---|---|---|
| DM-FR-010 / BR-DM-005 | first reminder after due+Nd; second after response period | EscalationService fires `mmEscStep` 1 (Reminder, triggerDays 7) when cumulative days ≤ elapsed; advances stage | T-13.1 |
| DM-FR-012 / BR-DM-008 | demand notice after reminder period; demanded = total − disputed | step 2 (Demand) fires at cumulative 21d; `dmDebt.stage`=Demand | T-13.2 |
| DM-FR-013 / BR-DM-011 | final demand demand+21d; proof of delivery for enforcement | step 3 (Final demand) fires at cumulative 42d; enforcement steps (4/5) gated behind it | (ladder ordering) |
| BR-DM-006 | reminders C2–C5; C1 → no reminder (write-off) | category gate: a case below the strategy `categoryFloor` (C2) is skipped — C1 fires nothing | T-13.unit (categoryFloorSkipsC1) |
| DM-FR-009/014 | multi-channel notices, delivery tracked | each fired step → `NOTIF_PENDING(reason=template)` → F06 dispatcher renders `mmNotifRule(DM,template)` → channel → `cmNotif` (SENT/QUEUED_PRINT) | T-13.3 |
| (idempotency) | a step is issued once | `dmDebt.lastStepSeq` gates re-firing; re-sweep at the same asOf fires nothing | T-13.4 |

## 2. Design decisions
1. **A1 — cumulative trigger days.** `mmEscStep.triggerDays` are relative to the previous step (F01 seed: 7/14/21/14/30); the engine accumulates them and fires every step whose cumulative offset ≤ elapsed (catch-up), one notice each.
2. **A2 — base = identification time.** elapsed = `asOf − cmCase.dateCreated`. The auto-identification time is the clock origin (the debt was overdue at identification). A per-line due-date refinement is recorded for later (dmLine carries due dates).
3. **A3 — category gate at the strategy floor.** A case below `mmStrategy.categoryFloor` (C2) resolves no strategy → skipped (BR-DM-006 C1). The finer 2nd-reminder-C3+ rule is a future per-step `minCategory` (the ladder currently has one reminder step).
4. **A4 — notices reuse F06.** The engine only emits `NOTIF_PENDING(reason=<step.noticeTemplate>)`; the dispatcher matches `mmNotifRule(caseType=DM, eventType=<template>)`. New seeds: `mdTemplate` (TPL-REMINDER/DEMAND/FINAL/GARNISH/SEIZURE) + `mmNotifRule` (DM × each), loaded via `API-cmbb-data` (CMBB-owned tables).
5. **A5 — `dmDebt.lastStepSeq`** added (F02 form amended; additive `updateSchema` union).

## 3. Generation order
1. gen_forms: F-cmEscalateRun (+ regenerate amended dmDebt) → dmbb/generated/forms.
2. gen_datalists (forms) → companions.
3. gen_userview: ALL dmbb UV-deltas (F01..F04) → dmbbConsole gains an **Escalation** category.
4. Plugin: EscalationService + EscalationEngine + properties + Activator (10→11) + EscalationServiceTest; mvn (built 79/79).
5. Deploy cmbb (JAR) + dmbb; seed mdTemplate + mmNotifRule via API-cmbb-data; run_t13 + full regression.
