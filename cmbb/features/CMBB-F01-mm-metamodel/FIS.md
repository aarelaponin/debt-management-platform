# FIS — CMBB-F01 — Case-Type Metamodel + MD Set
Status: Accepted
CAD ref: CAD-CMBB §7 row F01

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.1) | Realised by | Test |
|---|---|---|---|
| WF-FR-001 (config part) | "Case state transitions governed by configurable rules. Each transition logged with: timestamp, user, previous state, new state, reason. Invalid transitions rejected with user notification. State diagram configurable by administrator." | F-mmState, F-mmTransition, DL-list_bulk, UV-delta (admin CRUD); *logging/rejection enforcement = F02 (TransitionGuard)* | T-01.1 |
| WF-FR-002 | "Each case type independently configurable with: required fields, optional fields, associated workflow, document templates, SLA parameters, escalation rules. New case types addable by administrator without code changes. Minimum 10 case types supported." | F-mmCaseType + policy forms (F-mmSla, F-mmAlloc, F-mmHoldPolicy, F-mmNotifRule, F-mmDocReq, F-mmTaskRule, F-mmCoi, F-mmAuthority, F-mmCalendar, F-mmStatute, F-mmLinkType) + MD forms; UV-delta | T-01.2, T-01.3 |

Every artefact file in this folder is named above; both FRs covered; enforcement-half of WF-FR-001 is explicitly F02's (CAD slice map).

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| (config-surface feature — behavioural BRs land in F02+) Code uniqueness on all `code` PK forms | Duplicate-value validator on `code` field, every mm/md form (Stage-3 generation note) |
| mm changes are approval-gated (P8/CAD §6) | Deferred to F08 (policy-change case type); interim: role `cmbb_admin` only writes — documented assumption A2 |

## 3. Design decisions & assumptions
1. **A1:** State/transition references use state *codes* (TextFields) rather than cascading lookups — keeps F01 free of cascade complexity; referential validation arrives with TransitionGuard (F02). Acceptable for admin-facing config forms.
2. **A2:** Config approval gating (policy-change-as-case) ships in F08; until then `cmbb_admin` role restriction is the control. Recorded, not silent.
3. **A3:** `mmAuthority` is global (not per case type) per CAD/DPM D6; `actionType` strings are the join key used by the decision service (F08).
4. **A4:** Criteria/rule expressions stored as JSON/text in TextAreas — interpreted by the engines (F03/F05), never by BeanShell (P3).
5. **A5:** TEST case type seeded solely for acceptance; flagged inactive-able; DM case types arrive with DMBB-F01/F02 seeds.
6. No blockers — gate G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Case states / transitions per type | mmState / mmTransition rows | envelope six states (TEST seed) | WF-FR-001 |
| Case type attributes (id format, scope, policies) | mmCaseType + policy form rows | TEST seed | WF-FR-002 |
| SLA thresholds | mmSla.warnPct/critPct | 75 / 90 | WF-FR-010 (schema ready; data in F05) |
| Calendars / statutory periods | mmCalendar / mmStatute rows | MLT calendar seed | DPM D11 |
| Channels, hold types, doc classes, decision types, link types | md* seeds | per seed/ CSVs | WF-FR-013/017; DPM D8 |

(This feature *is* the carrier surface — the [Configurable] markers of later features point at these tables.)

## 5. Generation order
1. forms/F-md*.spec.yml (4, no dependencies) → 2. forms/F-mmCalendar, F-mmCaseType → 3. remaining F-mm* (lookup mmCaseType/mdHoldType/mdDocClass/mdChannel/mmCalendar) → 4. datalists/DL-list_bulk → 5. userview/UV-delta (cmbbConsole) → 6. seed CSV import (md* → mmCalendar → mmCaseType → mmState → mmTransition).
