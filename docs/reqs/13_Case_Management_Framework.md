# GENERIC CASE MANAGEMENT FRAMEWORK (GCMF)
## Baseline Specification Outline — The Case Fabric Beneath Modules 01–12

**Status:** Outline for review (v0.1) | **Realisation target:** Case Management Building Block (CMBB) on Joget DX9
**Derived from:** TA Reference Architecture (TARA) Domain 4; cross-analysis of requirements Modules 00–12

---

# 1. PURPOSE AND ARCHITECTURAL POSITION

## 1.1 The claim this framework operationalises

The TA Reference Architecture assigns all structured back-office work to Domain 4: the Case Management Platform "handles all structured back-office work in the tax administration … audit cases, taxpayer service requests, enforcement actions, objections and appeals, and internal administrative processes," realised on a low-code platform where workflows, case types, allocation rules and SLAs are **configuration, not code** — with bespoke code reserved for integrations and specialised logic.

Module 00 makes the same claim from the solution side: *"one case and workflow fabric — audit, debt, appeals, refunds, registration approval, and service requests run as configurable cases on one case-management fabric."*

Today that fabric is implicit: each module specification re-states its own case requirements, and each building block would re-implement them. This document makes the fabric **explicit and buildable**: a Generic Case Management Framework (GCMF), realised once as a Case Management Building Block (CMBB), on which the module-specific building blocks (AMBB, DMBB, APBB, RFBB, …) are *specialisations*, not siblings.

## 1.2 What changes for the suite

- **Module specs stay as they are.** Their case-management FRs become *conformance requirements* against the GCMF plus a small set of module-specific extensions — nothing is lost, but ~40–50 FRs across the suite stop being implemented eleven times.
- **CADs gain a standard dependency.** Every CAD's case-bearing entities derive from the GCMF case model; every state machine is a GCMF case-type configuration.
- **The platform-capability requirements already written are absorbed.** Modules 05 and 08 carry generic platform requirement catalogues (WF.xxx workflow engine, BR.xxx rules, EC.xxx events, PM.xxx monitoring — e.g., WF.001–WF.012 covering BPMN-style design, sub-processes, multi-stage approvals, policy-based assignment). These are GCMF platform requirements that happened to be filed inside two module documents; the GCMF becomes their proper home, referenced from both.

---

# 2. EVIDENCE BASE — THE PATTERN THE MODULES ALREADY SHARE

## 2.1 Case inventory across the suite

| Module | Case types it runs on the fabric |
|---|---|
| 01 Registration | Registration approval; deregistration; amendment review |
| 02 Returns | Non-filer/stop-filer case; return correction/amendment review |
| 04 Refunds | Refund case (REF-FR-029: Originated → … → Paid/Closed, with Under Audit, Pending Information, Cancelled, Recovered) |
| 05 Accounting | Accounting operations cases (adjustments, write-off execution, reconciliation discrepancies) |
| 06 Risk | Risk review/treatment cases (outputs consumed by selection) |
| 07 Audit | Audit case — the richest specialisation: teams, suspension/extension with clock pause (AUD-FR-031…033), authorisation, QA |
| 08 Debt | Debt/collection case; instalment agreement; escalation chains; enforcement actions; write-off |
| 09 Appeals | Dispute case with independence-constrained allocation (APL-FR-014…017), collection hold, multi-tier escalation; litigation case (v1.1 §4.15) with procedural calendar |
| 11 Services | Enquiry/service-request case (TPS-FR-017…020: lifecycle, routing, SLA, escalation); complaints |
| 12 Additional | EOIR request case (deadline-managed, confidentiality-walled), scheme review, guarantee call, disclosure agreement, discrepancy cases (RDT) |

Eleven of thirteen modules run cases; only 03 Payments and 10 Performance are (mostly) caseless — and even they raise exception cases (suspense resolution, KPI breach review).

## 2.2 The recurring pattern (with representative FR evidence)

Every module independently specifies the same nine capabilities. This repetition *is* the framework:

| # | Capability | Recurs as (examples) |
|---|---|---|
| 1 | **Configurable status lifecycle** with rejected invalid transitions, audited transitions | REF-FR-029; TPS-FR-018; audit/debt/appeals lifecycles; every CAD state machine |
| 2 | **Allocation** by skill/office/type/workload, supervisor assignment, re-allocation with justification + approval, history preserved | AUD-FR-015…017; APL-FR-017; debt collector assignment; TARA 10.4 |
| 3 | **Conflict-of-interest / independence control** at (re)allocation, blocking with authorised override | AUD-FR-018; APL-FR-015/016 (independence from original decision-maker as a *fairness control*); TARA COI matrix |
| 4 | **Deadline/SLA engine**: per-type targets, elapsed tracking, at-risk/breach alerts, escalation independent of officer availability | REF-FR-030; AUD-FR-031; TPS-FR-020; APL-FR-067 (litigation calendar); ICX request deadlines; TARA 10.6 |
| 5 | **Suspension/extension with clock semantics**: grounds, decision, notification, deadline-clock pause/extend within limits | AUD-FR-032/033; appeals suspension of proceedings; debt instalment holds |
| 6 | **Pending-information loop**: request → pending state → provision resumes, no re-initiation | REF-FR-031; appeals information requests; audit document requests; services |
| 7 | **Decision & approval**: thresholds, authority levels, multi-level and batch approval, signed (e-sign) reasoned decisions | Refund approvals; audit decisions; appeals decisions; SCH awards; GSM calls; VDP agreements; WF.010–WF.012 |
| 8 | **Holds & financial segregation tied to case state**: disputed-amount collection hold; disbursement hold pending audit; stay securities | APL §4.5; REF-FR-017; LIT/APL-FR-068 hold consistency; debt-side enforcement stays |
| 9 | **Immutable case history + document/evidence record** | REF-FR-032; every module's audit-trail FRs; TARA 10.5 (versioned, searchable, confidentiality-marked documents) |

Plus four cross-cutting recurrences: **case linkage** (refund→audit referral, appeal→litigation, MAR→debt, scheme→clawback→debt), **queue & prioritisation** (deadline/amount/risk/age), **QA sampling & oversight** (audit §4.11, appeals §4.11, refunds §4.11), and **KPI event emission** to Module 10.

## 2.3 What is genuinely module-specific (and must NOT be genericised)

The framework's discipline is as much about exclusion as inclusion. These stay in the specialisations: audit working papers and findings structure; debt enforcement-action catalogue and instalment mechanics; the appeals independence *policy content* (the GCMF provides the enforcement mechanism; Module 09 provides the rule); refund set-off; ICX confidentiality *policy* and schemas; scheme eligibility/calculation; litigation counsel workflow content; VDP relief calculation. The GCMF carries mechanisms; modules carry meaning.

---

# 3. THE FRAMEWORK — OUTLINE

## 3.1 Case object model (TA-RDM anchored)

The generic model, to be specified as both L2 canonical entities (candidate extension of the TA-RDM compliance-control domain, 07-compliance-control.yaml) and CMBB Joget tables:

```
case ──────────────┐ the spine: one row per case of any type
├─ case_type (FK → case-type metamodel, §3.2)
├─ party anchor: TIN (always); tax_type + tax_period where the case is
│  obligation-scoped (TTT discipline — registration/service cases may be
│  party-scoped only; the type declares which)
├─ subject reference: the business object the case is about (assessment id,
│  refund id, debt id, decision id, request id) — typed reference, not free text
├─ origin: how it arose (risk selection, taxpayer initiation, system detection,
│  escalation from another case, external request)
├─ classification: category / sub-category / priority / amount-at-stake
├─ current_state + state history (immutable, actor/time/reason per transition)
├─ assignment: current owner (officer/team/unit) + assignment history
└─ terminal outcome: typed result + linkage to executed consequences

case_task          work items within a case (who, what, due, status)
case_deadline      every clock on the case: statutory, SLA, procedural;
                   kind = {target | hard}, clock state = {running | paused},
                   pause reason linkage (suspension semantics)
case_hold          financial/process holds the case asserts elsewhere
                   (collection hold, disbursement hold, offset exclusion),
                   with scope, basis, release linkage
case_decision      signed decisions: type, outcome, reasons, authority level,
                   approvals chain, e-signature, document reference
case_document      evidence/correspondence register (store-agnostic reference,
                   version, confidentiality class, direction in/out)
case_link          typed case-to-case and case-to-decision relations
                   (referral, escalation, continuation, consolidation, parent)
case_event         the immutable event log (every action; feeds audit trail,
                   KPI emission, and the Module 10 semantic layer)
case_note          officer annotations (confidentiality-classed)
case_party_role    additional parties on the case (representative, counsel,
                   counterparty administration, issuer)
```

Conventions carried over from the suite: code-as-key case ids per type (`AUD-…`, `RC-…`), Joget `app_fd_*` realisation, NULL/ISO-3 country handling, SCD-style history never overwritten.

## 3.2 Case-type metamodel (the configuration surface)

A case type is a configuration record set — the GCMF equivalent of "tax officers configure workflows, case types, and business rules using visual tools" (TARA 10.2). Per case type:

| Configuration block | Defines |
|---|---|
| Identity | type code, name, owning module/BB, id format, subject-reference type, party-scope (TTT vs party-only) |
| Lifecycle | states, transitions, guards (rule refs), terminal states, version (in-flight cases keep their version — TARA 10.3 workflow versioning) |
| Allocation policy | criteria (skill, office, type, workload), supervisor mode, re-allocation rules (justification + approval), team support |
| Independence/COI policy | COI matrix reference; independence constraints (e.g., "exclude original decision-maker and unit"); override authority |
| Deadlines & SLAs | named clocks: source (statutory/SLA/procedural), duration or date rule, kind, at-risk thresholds, alert/escalation chain; suspension grounds and clock effect; extension limits + authority |
| Pending-information policy | request templates, pending state mapping, response handling, auto-actions on expiry (e.g., 10-day timer → auto-close, per TARA 10.3) |
| Decision & approval policy | decision types, threshold→authority matrix, batch approval eligibility, e-sign requirement, reason mandatory |
| Hold policy | which holds this type may assert, on what scope, auto-assert/release rules |
| Document policy | required document classes per state, confidentiality default, retention class |
| Linkage policy | permitted link types and target case types (e.g., refund → audit referral) |
| Queue & priority | priority formula inputs (deadline proximity, amount, risk, age), queue definitions |
| QA & oversight | sampling rules, four-eyes points, oversight roles |
| Security | case-level access classes (incl. hard walls — ICX confidentiality), field/document-level restrictions |
| KPI events | which events emit, with which dimensions, to Module 10 |

## 3.3 Core services (the engine, built once)

1. **Lifecycle engine** — executes the configured state machine; rejects invalid transitions; writes the immutable history. (Realises pattern §2.2-1.)
2. **Allocation engine** — skill/workload/priority routing, supervisor and team modes, re-allocation with justification+approval, full history. (§2.2-2; TARA 10.4 incl. workload rebalancing.)
3. **Independence & COI service** — evaluates the COI matrix and independence constraints at every (re)allocation; blocks; authorised-override path with audit. (§2.2-3.)
4. **Deadline & SLA engine** — multi-clock per case, statutory vs SLA semantics, pause/resume tied to suspension decisions, at-risk and breach alerts with escalation **independent of assignee availability**, calendar surfaces (the litigation procedural calendar is this engine, not a special one). (§2.2-4/5.)
5. **Pending-information service** — request issue, state parking, response intake (portal/officer), resumption, expiry auto-actions. (§2.2-6.)
6. **Decision & approval service** — threshold routing, multi-level and batch/package approval, e-signed reasoned decisions, decision documents. (§2.2-7; absorbs WF.010–WF.012.)
7. **Hold service** — asserts/releases holds in the owning modules (collection in 08, disbursement in 04/03, offset exclusions in 05) through their interfaces; guarantees hold↔case-state consistency (the APL/LIT/GSM consistency rule generalised). (§2.2-8.)
8. **Task service** — work items, worklists/queues per the priority formula, reassignment.
9. **Document & evidence service** — case file register over the platform DMS: versioning, confidentiality classes, full-text search, in/out correspondence linkage. (TARA 10.5.)
10. **Linkage service** — typed case graph: referrals, escalations (incl. cross-BB: appeal→litigation, refund→audit, scheme clawback→debt), consolidation; navigable both ways.
11. **Audit-trail & event service** — every action to case_event; tamper-evident; the single source for reconstruction and for KPI emission to Module 10. (§2.2-9.)
12. **QA & oversight service** — sampling, four-eyes checkpoints, oversight dashboards (central oversight of regional offices, per refunds/appeals §4.11 patterns).

## 3.4 Security and independence model

Case-level security as a first-class GCMF concern: access classes per case type; role + unit scoping; **hard walls** for confidentiality-restricted types (ICX exchange cases visible only to EOI roles, with purpose-recorded access logging); restriction inheritance to documents and notes; and the independence constraint engine shared with allocation. Decision/execution segregation (the Module 12 / MTCA governance principle) is enforced structurally: decision authority and execution tasks are distinct task types that the type configuration may not assign to one role.

## 3.5 Events and measurement

Every lifecycle transition, allocation, deadline state change, decision and hold emits a typed event with standard dimensions (case type, module, office, officer, party segment, amounts, durations). Module 10's KPI catalogue consumes these directly — case aging, SLA compliance, backlog, throughput per TARA 10.6 become *one* semantic-layer mapping instead of eleven.

---

# 4. SPECIALISATION CONTRACT — WHAT EACH MODULE ADDS

A module building block specialises the GCMF by: (a) registering its case types in the metamodel, (b) extending the case with module entities via the subject reference and child tables, (c) plugging module logic into named extension points. The framework defines the extension points; modules fill them:

| Extension point | Filled by (examples) |
|---|---|
| Transition guards | Audit: "findings approved before decision"; Refunds: "set-off applied before approval"; VDP: "agreement accepted before posting" |
| Allocation criteria providers | Audit skill taxonomy; debt portfolio segmentation; appeals tier authority |
| Independence rule content | Appeals: original decision-maker exclusion; ICX: EOI-unit-only |
| Deadline sources | Statutory audit duration; refund statutory deadline (drives interest); EOIR instrument deadlines; court procedural dates |
| Decision types & consequences | Audit assessment → reassessment posting; appeal outcome → outcome implementation; GSM call → realisation posting |
| Hold scopes | Disputed amount (09), pre-refund disbursement (04), enforcement stay (08/GSM) |
| Case-content forms | Working papers (07), enforcement actions (08), submissions/hearings (09), disclosure content (VDP) |
| Link types | refund→audit referral; appeal→litigation continuation; MAR→debt foreign-claim; scheme→clawback→debt |

Conformance rule for module specs: a module case FR is satisfied either by GCMF configuration (cite the metamodel block) or by a declared extension (cite the extension point) — anything needing a third mechanism is a GCMF gap to be fixed in the framework, not worked around in the module.

---

# 5. JOGET DX9 REALISATION SKETCH — CMBB

| GCMF element | Joget realisation | New plugin? |
|---|---|---|
| Case spine + child entities | Forms/tables per §3.1 (joget-form-gen); one physical `case` table + per-type subject tables | No |
| Case-type metamodel | MD/config forms (`mm_case_type`, `mm_lifecycle`, `mm_sla`, `mm_allocation`, …) — consistent with the `mm_*` pattern already in APBB §5 | No |
| Lifecycle engine | Joget workflow (XPDL from state machines, joget-workflow-gen) + a **TransitionGuard** validator plugin evaluating configured guards | Yes (1) |
| Allocation + COI/independence | **AllocationEngine** ApplicationPlugin (skill/workload/priority + COI matrix + independence rules) — the suite's first genuinely shared plugin | Yes (1) |
| Deadline/SLA engine | **DeadlineEngine** scheduled plugin (multi-clock, pause/resume, alerts/escalation) — replaces per-BB deadline logic incl. the APBB deadline engine noted in the delivery workflow | Yes (1) |
| Decision & approval | Workflow approval patterns (WF.010–012) + e-sign decision documents (joget-jasper-report for decision instruments) | No |
| Holds | **HoldConnector** plugin posting holds via module interfaces (08/05/04) | Yes (1) |
| Documents, tasks, queues, worklists | Native forms/datalists/userviews; datalist queues with priority formula columns | No |
| Event log + KPI emission | Append-only `case_event` form table + emitter plugin to the Module 10 pipeline | Yes (1) |
| Security walls | Userview/datalist/form permission classes + row-level filters; ICX wall as a permission class | No (config) |

Plugin budget for the whole framework: **~5 shared plugins** — replacing what would otherwise be 8–11 per-module reimplementations, which is precisely TARA 10.7's "everything is configuration, not code" argument made concrete.

---

# 6. DELIVERY PATH AND IMPACT

1. **CAD-CMBB first.** Run `joget-component-architect` on this outline + the absorbed WF/BR/EC/PM platform requirements (Modules 05/08 §5) to produce CAD-CMBB; its §7 decomposes into ~6–8 features (case spine + metamodel; lifecycle+guards; allocation+COI; deadline engine; decisions; holds+linkage; documents+events; oversight).
2. **Full GCMF requirements spec** follows the house format (this outline becomes its §1–4 skeleton; FR prefix `GCM-FR-nnn`), and Modules 05/08 §5 platform catalogues are referenced out to it in their next revision.
3. **Module BBs declare conformance.** Existing CADs (starting with the RFBB retro-CAD) add a "GCMF conformance" section mapping their case FRs to metamodel blocks/extension points per §4's rule.
4. **Skills impact:** `joget-component-architect` gains a CAD section for GCMF conformance; `joget-workflow-gen` consumes lifecycle configurations; the delivery workflow's Stage 1 rule "one state machine per case-bearing entity" becomes "one GCMF case-type configuration per case-bearing entity."
5. **Tender posture:** the GCMF is the concrete answer to TARA Domain 4 alignment — evaluators asking "show your case management platform" get one framework document plus per-module conformance tables instead of eleven assertions.

---

*End of GCMF outline v0.1. Next step on approval: GCM-FR requirements specification + CAD-CMBB.*
