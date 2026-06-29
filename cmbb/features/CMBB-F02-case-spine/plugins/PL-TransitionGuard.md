# Plugin Requirement — TransitionGuard (Output D format)

**Type:** ProcessTool (MultiTools pattern A) + FormValidator (case form save)
**Plugin ID / FQCN:** `com.fiscaladmin.mtca.cmbb.TransitionGuard`
**Forcing FR:** WF-FR-001 (enforcement half: "Invalid transitions rejected with user notification. Each transition logged with: timestamp, user, previous state, new state, reason"); CAD-CMBB §2.3 guards.

**Trigger:** tool activities `guardOpen` / `guardClosure` / `guardFinal` in process `cmCaseEnvelope` (property `phase` ∈ OPEN / PRE_CLOSE / CLOSE); validator on cmCase save for state-field edits.

**Logic per phase:**
- OPEN: case type registered & active (mmCaseType); TTT scope satisfied per `tt_scope` (OBLIGATION ⇒ taxType+taxPeriod mandatory); duplicate check per type dedup policy; generate `caseRef` from `mm_case_type.idFormat` (env counter per type — custom ID = plugin trigger per req-analyst rules); set currentState=New→Open; write cmEvent(CASE_CREATED, CASE_OPENED).
- PRE_CLOSE: required documents per state present (mmDocReq ⋈ cmDoc); no OPEN cmTask rows; transition Open/InProgress→PendingClosure valid per mmTransition (type's lifecycle version); write cmEvent(STATE_CHANGED).
- CLOSE: decision/authority satisfied (mmAuthority via cmDecision — F08 dependency stubbed: config flag `requireDecision=false` until F08); currentState→Closed; write cmEvent(CASE_CLOSED).
- ALL phases: invalid transition ⇒ throw with reason (workflow halts, user notified); every accepted transition appends cmEvent {timestamp, actor, prevState, newState, reason} + hash chain (prevHash=hash of previous event row for the case; hash=SHA-256 of payload+prevHash).

**FormDataDao reads:** mmCaseType, mmState, mmTransition, mmDocReq, cmCase, cmTask, cmDoc, cmEvent (last row per case)
**FormDataDao writes:** cmCase (caseRef, currentState), cmEvent (append only — never update/delete)

**Properties (FIS §4 carriers):** `phase`, `caseId` (#variable.caseId#), `requireDecision` (default "false" until F08)
**Complexity:** M
**Build:** Maven/OSGi per joget-plugin-dev; bundle `mtca-cmbb-plugins` (this is the bundle's first plugin; AllocationEngine/DeadlineEngine join it in S2).
