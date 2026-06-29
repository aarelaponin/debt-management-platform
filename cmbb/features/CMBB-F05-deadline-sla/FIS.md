# FIS — CMBB-F05 — Deadline / SLA Engine
Status: Accepted (T-05.1..4 7/7 PASS on jdx9, 2026-06-12; full regression F02/F03/F04 green)
CAD ref: CAD-CMBB §7 row F05

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.3) | Realised by | Test |
|---|---|---|---|
| WF-FR-010 | "SLAs configurable in business days or calendar days. SLA clock pauses when case is On Hold (configurable). Multiple SLA levels per case (e.g., first response SLA, resolution SLA). SLA breach triggers configurable escalation action." | mmSla rows (multiple clocks per type; +pauseOnHold flag) + mmCalendar (CALENDAR/WORKING + holidays) + F-cmDeadline (clock instances) + PL-DeadlineEngine START/SWEEP | T-05.1, T-05.3, U-05 (date math) |
| WF-FR-011 | "Traffic-light indicators update in real time. SLA countdown displayed in hours/days remaining. Breached cases persist as red until resolved. SLA compliance percentage displayed on management dashboards." | SWEEP writes cmCase.slaStatus (GREEN/AMBER/RED, worst clock wins; BREACHED stays RED) + DL-list_queue amendment (sla_due countdown column, RED-first ordering already in BR-WF-004 SQL); *compliance % dashboard = reporting scope (F09/RPT), recorded* | T-05.2, T-05.4 |
| WF-FR-012 | "Escalation triggers at configurable threshold (default: 90% of SLA). Notification includes: case ID, taxpayer, debt amount, SLA status, days overdue. Escalation recorded in case history. Configurable maximum escalation levels (default: 3)." | SWEEP escalation: crit crossing → level 1, breach → level 2 (≤ maxLevels); NOTIF_PENDING to assignee + supervisor with case payload; SLA_ESCALATED/SLA_BREACHED events; priority bump; optional senior reassignment per escalationChain | T-05.2 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| CAD case_deadline state machine (Running→Paused→Running; →Met/Breached terminal; breach escalation fired exactly once) | PL-DeadlineEngine SWEEP (escalationLevel guard makes each escalation idempotent) |
| Clock pause on hold (configurable) | mmSla.pauseOnHold × mmState.envelopeState=OnHold; resume extends dueAt by paused duration |

## 3. Design decisions & assumptions
1. **A1 — invocation (revised at Stage 3)**: PluginWebSupport is jakarta-servlet in 9.0.7 vs javax in the 8.1 compile target (DX9-DELTAS) — SWEEP is instead triggered by saving a `cmSweepRun` row (form postProcessor → DeadlineEngine SWEEP; pattern proven in F03). Production schedule = cron POSTing the row via the form API; every sweep is an auditable record with its result. `asOf` field allows deterministic time-travel in tests (blank = now).
2. **A2 — clock creation**: START mode as tool #3 in guardOpen (after AllocationEngine): one cmDeadline per active mmSla row of the case type; CLOSE mode in guardFinal chain marks open clocks MET. No new XPDL.
3. **A3 — thresholds**: warnAt/critAt = linear pct of the start→due span (calendar-agnostic approximation; exact business-day thresholds recorded as refinement).
4. **A4 — escalation actions**: escalationChain JSON `{"notify":["assignee","supervisor"],"bumpPriority":true,"reassignTo":"","maxLevels":3}`; notification dispatch = F06 (NOTIF_PENDING carries case ref, taxpayer, amount, SLA status, days overdue).
5. **A5 — "real time"**: status recomputed every sweep + on guard transitions; queue reads are live SQL. Sweep cadence is the deployment's cron setting (DEV: on demand).
6. **A6 — compliance % dashboard** deferred to reporting (F09/RPT-FRs) — recorded, not dropped.
7. No blockers — G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Clocks per case type (multiple) | mmSla rows (clockCode, durationDays, calendar) | TEST: RES 10d | WF-FR-010 |
| Warning / critical thresholds | mmSla.warnPct / critPct | 75 / 90 | WF-FR-010/012 |
| Pause on hold | mmSla.pauseOnHold (new checkbox — F01 spec amendment) | true | WF-FR-010 |
| Business vs calendar days + holidays | mmCalendar.workingDayMode / holidays | CALENDAR | WF-FR-010 |
| Escalation chain / max levels / priority bump / senior reassign | mmSla.escalationChain JSON | notify assignee+supervisor, bump, maxLevels 3 | WF-FR-012 |
| Sweep cadence | deployment cron (ops runbook) | on demand (DEV) | WF-FR-011 |

## 5. Generation order
1. F-mmSla amendment (pauseOnHold) → 2. forms/F-cmDeadline → 3. datalists (list_cmDeadline companion; DL-list_queue SQL amendment: sla_due join) → 4. WF spec: guardOpen tools[]+=DeadlineEngine START, guardFinal tools[]+=CLOSE → 5. PL-DeadlineEngine (bundle; PluginWebSupport) + unit tests (date math, sweep transitions, escalation idempotency) → 6. UV delta (SLA category: mmSla/mmCalendar config already in F01 console; add list_cmDeadline) → 7. seeds (mmSla TEST row, MLT calendar) → 8. redeploy + T-05.x + F02/F03/F04 regression.
