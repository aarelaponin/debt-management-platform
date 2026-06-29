# Plugin Requirement — AllocationEngine (Output D format)

**Type:** ProcessTool (MultiTools, chained after TransitionGuard in `guardOpen`) + form post-processor (REASSIGN)
**Plugin ID / FQCN:** `com.fiscaladmin.mtca.cmbb.AllocationEngine` (joins existing bundle `cmbb-plugins`)
**Forcing FR:** WF-FR-007 (rule-based assignment, workload, supervisor routing, history), WF-FR-008 (single/bulk reassignment), BR-WF-005/006/007; CAD §2.3 New→Open guard "assignee resolved; COI clear".

**Modes (property `mode`):**

## ASSIGN (tool #2 in guardOpen)
1. Load case (caseId fallback = origin process id, as TransitionGuard).
2. Skip if `assignee` already set (manual pre-assignment respected) — still log CASE_ASSIGNED.
3. Load mmAlloc policy for case type (first active row; criteria JSON `{strategy, filters[], warnPct}`).
4. Candidate set: mdOfficerProfile rows `active=true`
   - filter SPECIALISATION: case.taxType ∈ profile.taxTypes (skip filter when case or profile blank)
   - filter GEOGRAPHY: case geography attr ∈ profile.geography (blank-tolerant, same rule)
   - filter WORKLOAD (BR-WF-005): openCases(officer) < maxCapacity, where openCases = count cmCase assignee=officer AND currentState not terminal
   - filter COI (mmCoi rows for type): EXCLUDE_UNIT → drop candidates whose profile.unit matches expression; MATRIX → drop candidates named/matched by expression; EXCLUDE_DECISION_MAKER → no-op until F08 (logged)
5. Selection: ROUND_ROBIN over surviving candidates (cursor = count of CASE_ASSIGNED events per type, mod n — deterministic, restart-safe).
6. Write: cmCase.assignee + assignmentStatus=ASSIGNED; set process variable `assignee` (WorkflowManager.processVariable) so caseOfficer participant resolves; append cmEvent(CASE_ASSIGNED, payload {officer, policy, candidatesConsidered}).
7. Capacity warning (BR-WF-005): selected officer at ≥ warnPct% of maxCapacity → cmEvent(CAPACITY_WARNING, payload {officer, openCases, maxCapacity}).
8. No candidate → cmCase.assignmentStatus=UNASSIGNED (assignee stays empty), cmEvent(ASSIGNMENT_FAILED, reason) + cmEvent(NOTIF_PENDING, alertType=SUPERVISOR_UNASSIGNED) — supervisor queue = DL-list_unassigned; in-app alert consumed by F06. **Engine never throws** (assignment failure is routed state, not workflow halt).

## REASSIGN (post-processor of cmReassign save)
1. Load cmReassign row (recordId property).
2. Resolve scope: caseRef set → that case; else bulk = cmCase WHERE assignee=filterOfficer [AND caseType=filterCaseType] [AND category=filterCategory], non-terminal only.
3. Target: toOfficer set → all to that officer (capacity check: warn-only, supervisor decision overrides); blank → redistribute per BR-WF-007: round-robin over ASSIGN candidate filter (workload + specialisation).
4. Per case: prevOfficer=assignee; cmCase.assignee=newOfficer; append cmEvent(CASE_REASSIGNED, payload {from, to, reason, orderId}) + 2× cmEvent(NOTIF_PENDING) for original and new assignee (WF-FR-008 notification — dispatched by F06).
5. Write back cmReassign.result = "n case(s) reassigned" (or failure reason).
6. Reason blank → impossible (form validator), but engine re-checks and aborts with result=REJECTED (defence in depth).

**FormDataDao reads:** cmCase, mmAlloc, mmCoi, mdOfficerProfile, cmEvent (round-robin cursor), cmReassign
**FormDataDao writes:** cmCase (assignee, assignmentStatus), cmEvent (append-only via CaseEventWriter — same hash chain), cmReassign (result)
**Properties:** `mode` (ASSIGN/REASSIGN), `caseId` (#variable.caseId#, ASSIGN), `recordId` (REASSIGN)
**Reuses:** CaseEventWriter, MmConfigService (extend with mmAlloc/mmCoi/mdOfficerProfile readers), GuardContext patterns; unit tests on GuardTestHarness (extend dispatch with new tables).
**Complexity:** H (CAD budget row confirmed)
