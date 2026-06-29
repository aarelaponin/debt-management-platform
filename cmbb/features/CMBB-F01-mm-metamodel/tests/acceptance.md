# Acceptance tests — CMBB-F01

## T-01.1 — WF-FR-001 (config part): state diagram configurable by administrator
**Covers:** WF-FR-001 AC: "State diagram configurable by administrator."
**Setup:** login as cmbb_admin; cmbbConsole → Case-type administration → Lifecycle State.
**UI check:** create state {caseType=TEST, code=REVIEW, name=Review, envelopeState=InProgress, version=1} → appears in list_mmState; create transition {TEST, OPEN→REVIEW} → appears in list_mmTransition.
**SQL check:**
```sql
SELECT c_code, c_envelopeState FROM app_fd_mmState WHERE c_caseType='TEST' AND c_code='REVIEW';
-- expected: ('REVIEW','InProgress')
SELECT count(*) FROM app_fd_mmTransition WHERE c_caseType='TEST' AND c_fromState='OPEN' AND c_toState='REVIEW';
-- expected: 1
```
**Teardown:** delete the REVIEW state + transition (keep seed at 3 states / 2 transitions).

## T-01.2 — WF-FR-002: new case type addable without code changes
**Covers:** WF-FR-002 AC: "New case types addable by administrator without code changes."
**Setup:** cmbbConsole → Case Type → new.
**UI check:** create {code=PILOT, name=Pilot type, owningBb=CMBB, idFormat=PI-??????, ttScope=OBLIGATION, active=true}; attach one SLA clock {clockCode=RES, source=SLA, kind=TARGET, durationDays=30, warnPct=75, critPct=90, calendar=MLT} — all via forms, no deployment.
**SQL check:**
```sql
SELECT c_idFormat, c_ttScope FROM app_fd_mmCaseType WHERE c_code='PILOT';
-- expected: ('PI-??????','OBLIGATION')
SELECT c_durationDays FROM app_fd_mmSla WHERE c_caseType='PILOT' AND c_clockCode='RES';
-- expected: ('30')
```
**Teardown:** delete PILOT rows (mmSla first, then mmCaseType).

## T-01.3 — seed integrity (MLT profile base)
**Covers:** WF-FR-002 ("independently configurable… SLA parameters, escalation rules" — carriers present and seeded).
**Setup:** post-deploy seed import completed.
**SQL check:**
```sql
SELECT count(*) FROM app_fd_mdChannel;        -- expected: 4
SELECT count(*) FROM app_fd_mdHoldType;       -- expected: 5  (incl. CORR_SUPPRESS, ENF_SUPPRESS — E1)
SELECT count(*) FROM app_fd_mdDocClass;       -- expected: 5
SELECT c_workingDayMode FROM app_fd_mmCalendar WHERE c_code='MLT';  -- expected: ('WORKING')
SELECT count(*) FROM app_fd_mmState WHERE c_caseType='TEST';        -- expected: 3
```
**Notes:** counts are literals; rerunning seed import must not duplicate (import = upsert by code — Stage-4 deployment note).
