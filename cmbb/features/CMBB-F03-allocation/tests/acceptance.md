# Acceptance tests — CMBB-F03

Fixture (all runs): seed/mdOfficerProfile.csv (officer1/officer2, maxCapacity=2 each);
mmAlloc row `TEST-DEFAULT` {caseType=TEST, criteria: {"strategy":"ROUND_ROBIN","filters":["WORKLOAD","SPECIALISATION"],"warnPct":90}};
dir users officer1/officer2 in group cmbb_user (DEV directory seed).
Case fixture rows carry taxType=VAT so SPECIALISATION matches both officers.
Scripted runner: tests/run_t03.py (same mechanics as F02 run_t02.py).

## T-03.1 — WF-FR-007: rule-based assignment, logged in history
**Covers:** "Assignment rules configurable without code changes... Assignment logged in case history."
**Setup:** open two TEST cases (distinct TINs, taxType=VAT) through the envelope.
**UI check:** both cases show an assignee; no code/deploy step occurred between policy edit and effect.
**SQL check:**
```sql
SELECT c_assignee, c_assignmentstatus FROM app_fd_cmcase WHERE id IN ('<c1>','<c2>');
-- expected: officer1/officer2 (round-robin, one each), both 'ASSIGNED'
SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='CASE_ASSIGNED';  -- expected: 2
```

## T-03.2 — BR-WF-005: officer at capacity skipped
**Setup:** officer1 already holds 2 open cases (maxCapacity=2); open a third case.
**SQL check:** third case `c_assignee='officer2'`; a CAPACITY_WARNING event exists for officer1 at ≥90%.

## T-03.3 — WF-FR-007: unassignable → supervisor queue + alert event
**Setup:** fill both officers to capacity; open a fifth case.
**SQL check:**
```sql
SELECT c_assignee, c_assignmentstatus FROM app_fd_cmcase WHERE id='<c5>';
-- expected: ('', 'UNASSIGNED')
SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='<c5>' AND c_eventtype='ASSIGNMENT_FAILED';  -- 1
SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='<c5>' AND c_eventtype='NOTIF_PENDING';      -- 1
```
**UI check:** case appears in `Queue: Unassigned cases (supervisor)` (dm_supervisor-gated).
**Note:** workflow does NOT halt — assignment failure is routed state (FIS A4).

## T-03.4 — WF-FR-008: single reassignment, mandatory reason, audit
**Setup:** cmReassign row {caseRef=<c1 ref>, toOfficer=officer2, reason="leave coverage"}.
**UI check:** saving without reason is rejected by validator.
**SQL check:**
```sql
SELECT c_assignee FROM app_fd_cmcase WHERE c_caseref='<c1 ref>';  -- 'officer2'
SELECT c_payload FROM app_fd_cmevent WHERE c_eventtype='CASE_REASSIGNED' AND c_payload LIKE '%<c1 ref>%';
-- payload contains "from":"officer1","to":"officer2","reason":"leave coverage"
SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%<c1 ref>%';
-- expected: 2 (original + new assignee — WF-FR-008 notification, dispatched by F06)
```

## T-03.5 — WF-FR-008 + BR-WF-007: bulk reassignment with distribution
**Setup:** officer1 holds 2 open TEST cases; cmReassign {filterOfficer=officer1, filterCaseType=TEST, toOfficer=blank, reason="org change"}.
**SQL check:** both cases reassigned off officer1; targets respect capacity (officer2 ends ≤ maxCapacity); cmReassign.result = '2 case(s) reassigned'; 2× CASE_REASSIGNED events.

## T-03.6 — CAD §2.3: COI exclusion honoured at assignment
**Setup:** mmCoi row {caseType=TEST, ruleType=EXCLUDE_UNIT, expression=UNIT-A}; open a new case with both officers free.
**SQL check:** assignee = officer2 (officer1's unit excluded); CASE_ASSIGNED payload `candidatesConsidered` reflects the exclusion.
**Teardown:** remove mmCoi row (API), restore capacities.

**Status note:** T-03.x require PL-AllocationEngine deployed + gen extensions (postProcessor, multi-tool guardOpen); run after F03 Stage 3/4.
