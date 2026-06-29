# Acceptance tests — CMBB-F04

Queues are read surfaces: tests assert (a) the deployed datalist definitions
(binder class, filters) and (b) the behaviour of the ACTUAL deployed SQL,
extracted from app_datalist and executed read-only. Runner: tests/run_t04.py.

## T-04.1 — WF-FR-006: queue columns, priority bands, BR-WF-004 ordering
**Setup:** 3 open cases via API: A{risk 90, amount 100}, B{risk 50, amount 999},
C{risk blank, amount 500}; bands H≥70/M≥40/L≥0 seeded.
**SQL check (executing the deployed list_queue SQL):** row order = A, B, C
(risk 90 > 50 > default 40); priorities = H, M, M; days_in_queue ≥ 0; columns
include tin/taxpayer/debt_amount/risk_score/sla_status.

## T-04.2 — WF-FR-006: filters configured
**Definition check:** list_queue JSON in app_datalist: binder =
JdbcDataListBinder; filters = {case_type, category, sla_status, unit}.

## T-04.3 — WF-FR-009: personal worklist scoping
**SQL check:** deployed list_worklist SQL with #currentUser.username# →
'officer1' returns exactly officer1's open cases; terminal cases excluded.

## T-04.4 — WF-FR-009: red/amber highlighting
**Setup:** officer1 cases with nextActionDue = yesterday / tomorrow / +10d.
**SQL check:** due_flag contains 'OVERDUE' / 'DUE' / '' respectively.

## T-04.5 — UV: worklist is the officers' entry menu
**Definition check:** cmbbConsole JSON: category "Queues" exists; its first
menu is list_worklist. *(Per-role start-page selection is a userview theme
setting — tracked with the theme OPEN item, not silently dropped.)*
