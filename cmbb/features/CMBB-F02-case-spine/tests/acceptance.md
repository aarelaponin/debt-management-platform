# Acceptance tests — CMBB-F02

## T-02.1 — WF-FR-003: manual case creation with validation + dedup
**Covers:** WF-FR-003 AC: "Manual creation includes mandatory fields validation. Duplicate case detection prevents creation of duplicate cases for same taxpayer/tax type/period combination."
**Setup:** seed present (TEST case type, OBLIGATION? — TEST is PARTY-scoped; create via console an OBLIG variant or use TEST). Start process cmCaseEnvelope as cmbb_user with {caseType=TEST, tin=100058G, origin=MANUAL}.
**UI check:** case saves; guardOpen runs; caseRef assigned (TT-000001 pattern); second identical creation rejected with duplicate reason.
**SQL check:**
```sql
SELECT c_caseRef, c_currentState FROM app_fd_cmCase WHERE c_tin='100058G' AND c_caseType='TEST';
-- expected: 1 row, ('TT-000001','OPEN')
-- note: currentState stores the per-type state CODE (mmState.code, seed: NEW/OPEN/CLOSED);
-- the envelope name resolves via mmState.envelopeState (design decision at plugin build)
SELECT count(*) FROM app_fd_cmEvent WHERE c_eventType IN ('CASE_CREATED','CASE_OPENED');
-- expected: 2
```

## T-02.2 — WF-FR-001: invalid transition rejected with notification
**Covers:** "Invalid transitions rejected with user notification."
**Setup:** TEST lifecycle has no OPEN→CLOSED-direct… (seed has OPEN→CLOSED v1 — adjust: remove that transition row first via console, keep NEW→OPEN only).
**UI check:** attempting closure path (guardClosure) without a configured transition → workflow halts with reason shown; case stays in prior state.
**SQL check:**
```sql
SELECT c_currentState FROM app_fd_cmCase WHERE c_caseRef='TT-000001';  -- unchanged
SELECT count(*) FROM app_fd_cmEvent WHERE c_eventType='TRANSITION_REJECTED';  -- expected: 1
```

## T-02.3 — WF-FR-004/020: chronological, tamper-evident history
**Covers:** case history chronological; hash chain intact.
**SQL check:**
```sql
SELECT c_eventType, c_prevHash, c_hash FROM app_fd_cmEvent
WHERE c_caseId=(SELECT id FROM app_fd_cmCase WHERE c_caseRef='TT-000001')
ORDER BY "dateCreated";
-- expected: first row c_prevHash='' (genesis); every row N>1: c_prevHash = row N-1's c_hash
```
Verification script recomputes SHA-256(payload+prevHash) per row and compares.

## T-02.4 — WF-FR-003: TTT scope enforcement
**Setup:** create case type OBLIG (ttScope=OBLIGATION) via console; start case without taxType.
**UI check:** guardOpen rejects with "tax type and period required for obligation-scoped case".

## T-02.5 — WF-FR-005: re-open linked to original
**Setup:** complete a case to Closed; re-open as supervisor.
**SQL check:**
```sql
SELECT count(*) FROM app_fd_cmEvent WHERE c_eventType='CASE_REOPENED'
AND c_payload LIKE '%TT-000001%';  -- expected: 1; payload carries link to closing event hash
```
**Status note:** T-02.x require PL-TransitionGuard deployed; run after plugin build.
