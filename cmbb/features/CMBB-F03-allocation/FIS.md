# FIS — CMBB-F03 — Allocation Engine + COI + Reassignment
Status: Accepted (T-03.1..6 7/7 PASS on jdx9, 2026-06-12)
CAD ref: CAD-CMBB §7 row F03

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.2) | Realised by | Test |
|---|---|---|---|
| WF-FR-007 | "Assignment rules configurable without code changes. Assignment considers current officer workload (open case count). Unassignable cases (no matching officer) routed to supervisor queue with alert. Assignment logged in case history." | PL-AllocationEngine ASSIGN mode (chained after TransitionGuard in guardOpen MultiTools) + mmAlloc rows + F-mdOfficerProfile + cmEvent(CASE_ASSIGNED/ASSIGNMENT_FAILED) + DL-list_unassigned | T-03.1, T-03.2, T-03.3 |
| WF-FR-008 | "Single and bulk reassignment supported. Reason field mandatory. Original and new assignment recorded. Notification sent to both original and new assignee. Bulk reassignment supports selection by: officer, case type, debt category, and custom filter combinations." | F-cmReassign (postProcessor → PL-AllocationEngine REASSIGN mode) + cmEvent(CASE_REASSIGNED, payload carries from/to/reason); *notification dispatch = F06 (NOTIF_PENDING events emitted now, consumed there)* | T-03.4, T-03.5 |
| (CAD §2.3 New→Open guard: "assignee resolved; COI clear") | GCMF independence constraint | PL-AllocationEngine COI filter reading mmCoi (EXCLUDE_UNIT, MATRIX now; EXCLUDE_DECISION_MAKER stubbed to F08 — needs decision linkage) | T-03.6 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| BR-WF-005 (workload capacity: skip at capacity; supervisor alert at 90%) | PL-AllocationEngine ASSIGN: candidate filter `openCases < maxCapacity`; emits cmEvent(CAPACITY_WARNING) at ≥90% |
| BR-WF-006 (reassignment authorisation: SDO; bulk needs justification; audit all) | F-cmReassign: userview menu gated to dm_supervisor group; `reason` required validator; every reassignment appends cmEvent |
| BR-WF-007 (bulk distribution: round-robin over filtered officers WHERE workload<capacity AND specialisation match) | PL-AllocationEngine REASSIGN bulk path reuses the ASSIGN candidate filter + round-robin cursor |

## 3. Design decisions & assumptions
1. **A1 — no new XPDL** (CAD: envelope is the only process). AllocationEngine joins the existing `guardOpen` activity as the SECOND tool in the MultiTools `tools[]` array (sequential: guard validates, engine assigns). Requires gen_workflow.py support for multi-tool activities — Stage-3 item.
2. **A2 — officer attributes carrier**: Joget directory has no specialisation/geography/capacity. New MD form `mdOfficerProfile` (username = id) carries them; AllocationEngine joins profile × dir_user at runtime. Officer user accounts themselves are directory seeds (DEV: SQL into dir_user/dir_user_group, as bootstrap).
3. **A3 — reassignment trigger**: F-cmReassign form save runs AllocationEngine (REASSIGN) as form **post-processor** (Joget-native postProcessor property) — no extra process, P3-clean. Generator note: gen_forms.py must emit postProcessor config (Stage-3 extension).
4. **A4 — "supervisor queue with alert"**: route = `cmCase.assignmentStatus=UNASSIGNED` + assignee empty + cmEvent(ASSIGNMENT_FAILED); DL-list_unassigned (gated to dm_supervisor) is the queue. The in-app *alert* itself is WF-FR-015 (F06) — F03 emits the event it will consume. Same for reassignment notifications (NOTIF_PENDING).
5. **A5 — assignee workflow variable**: engine writes cmCase.assignee AND sets process variable `assignee` (WorkflowManager) so the existing caseOfficer participant resolves without map changes.
6. **A6 — debt category in bulk filters**: cmCase.category field exists (F02); custom filter combinations = the four spec'd fields ANDed; richer expressions deferred (recorded, not silent).
7. No blockers — G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Allocation criteria (skill/workload/geography/round-robin weights) | mmAlloc.criteria (JSON, per case type) | `{"strategy":"ROUND_ROBIN","filters":["WORKLOAD","SPECIALISATION"]}` | WF-FR-007 |
| Max open cases per officer | mdOfficerProfile.maxCapacity (per officer) | 20 | BR-WF-005 |
| Capacity warning threshold | mmAlloc.criteria `warnPct` | 90 | BR-WF-005 |
| Specialisation / geography per officer | mdOfficerProfile.taxTypes / geography | — | WF-FR-007 |
| COI rules per case type | mmCoi rows (F01 carrier) | none seeded | CAD §2.3 |
| Reassignment authorisation level | userview permission (dm_supervisor group) — P (partially configurable per BR-WF-006) | dm_supervisor | BR-WF-006 |

## 5. Generation order
1. forms/F-mdOfficerProfile → 2. forms/F-cmReassign (postProcessor) → 3. F-cmCase amendment (assignmentStatus field — amend F02 spec file, regenerate) → 4. datalists (list_mdOfficerProfile, list_cmReassign, DL-list_unassigned over cmCase) → 5. workflow plugin-map delta (guardOpen tools[] += AllocationEngine; gen_workflow.py multi-tool support) → 6. plugins/PL-AllocationEngine (extend cmbb-plugins bundle) → 7. UV delta (Allocation category: officer profiles, reassignment, unassigned queue) → 8. redeploy + seeds (officer profiles + dir users) + T-03.x.
