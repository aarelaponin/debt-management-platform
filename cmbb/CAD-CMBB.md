# CAD — CMBB Case Management Building Block
Version: 1.1 | Status: Draft (G1 gate report at end) | FR coverage: M08 WF-FR-001…020 (§4.13) cumulative; GCMF outline §3 (capabilities 1–12); M05 §4.3 satisfied by conformance (see §7 note)
Amendment log:

| Date | Slice | Sections touched |
|---|---|---|
| 2026-06-11 | CMBB-S1+S2+S3 (initial, all three scope cards) | all |
| 2026-06-11 | Bidder-claims enrichment E1–E7 (`docs/design/Bidder-CM-Claims-Analysis.md`) | §2.2 (+mm_task_rule, hold scopes, task flag), §3 (dispatcher guard), §4 (I-9), §7 notes |
| 2026-06-12 | CMBB-F08 build (decisions/holds/links/pending-info) | §2.2 plugin budget **9→11**: HoldConnector built + **DecisionEngine added** (decisions/linkage/pending-info form post-processor — the CAD assumed Joget native multi-stage approval but the one-envelope/no-new-XPDL convention forbids it, so a P3-clean post-processor engine is required); mmCaseType gains `requireDecision` (per-type closure gate; F01 form amended) |
| 2026-06-12 | ADR-001 specialisation model (SPIKE-001 verified) | §4 interface contracts gain the **subject-form extension point** (GCMF §4b content-forms): `mmCaseType.subjectFormId` registers a per-type 1:1 content form rendered on the case; module cases are `cmCase` spine rows extended by module-owned subject tables (the two-app B1 model). Carrier `mmCaseType.subjectFormId` added (F01 form amended). Follow-on CMBB increment: a `CaseSubjectFormElement` to render the per-type subject form inside the single-form-map spine activity (Joget `SubForm.formDefId` is static). |
| 2026-06-12 | CMBB-F09 build (events-kpi) | §2.2 budget **unchanged** — built the three remaining budgeted rows: **EventEmitter** (chain VERIFY + KPI EMIT + Gold PROBE, post-processor), **OutcomeWriteback** (idempotent fact_case_outcomes insert, queue-retry), **GoldMartClient** (sta_v1 JDBC shared lib + cmGoldSnapshot cache). New trigger/register forms cmChainCheck/cmGoldProbe/cmOutcomeRun/cmOutcome/cmGoldSnapshot; clickhouse-jdbc embedded. CMBB feature-complete |

## 1. Component Charter

CMBB realises the Generic Case Management Framework (GCMF/Module 13) as the **case fabric** for all MTCA building blocks: it owns case mechanics — the case spine and child entities, the case-type metamodel, lifecycle execution with guarded transitions, allocation/COI, multi-clock deadlines & SLA, queues and worklists, decisions & approvals, holds, linkage, the document register over Mayan, notifications, and the immutable case event log. It owns **no business meaning**: case-type content (debt escalation rules, instalment products, accounting adjustments) belongs to the specialising blocks (DMBB now; others later) via the GCMF §4 extension points; document binaries belong to Mayan (P15); accounting facts belong to the data product (D-SAD-01). Conformance rule (GCMF §4): a module case FR is satisfied by CMBB configuration or a declared extension point — anything needing a third mechanism is a CMBB gap, fixed here. Dependency position: depends only on platform services (Mayan, notification gateways, `sta_v1` read identities); every case-bearing BB depends on it.

## 2. Data Model

### 2.1 TA-RDM alignment

TA-RDM L2 domain YAMLs are not yet on hand (SAD OPEN-1; conventions per `__Data/_sigtas/TA-RDM-YAML-Guide.md`). Mappings below are **to-verify** against the named candidate domains; the GCMF intends the case model as a candidate L2 extension of the compliance-control domain.

| Component entity | TA-RDM L2 source (file :: entity) | Divergence + reason |
|---|---|---|
| case | 07-compliance-control.yaml :: case (candidate extension) — TO-VERIFY | adds Joget workflow linkage columns; subject_ref as typed reference |
| case_document | 08-document-management.yaml :: document/document_storage — TO-VERIFY | register row only; storage_reference = Mayan UUID (P15.2) |
| case_decision, case_hold, case_deadline, case_link, case_event, case_task, case_note, case_party_role | no L2 source — **candidate L2 extensions** (feed back to reference model) | GCMF §3.1 model is itself the proposal |
| mm_* metamodel, md_* lookups | no L2 source — Joget-operational config, **exempt** | configuration surface, not business data |

TTT spine: `case` always carries `c_tin`; `c_tax_type` and `c_tax_period` nullable — the case **type** declares party-scoped vs obligation-scoped (GCMF §3.1); enforced by TransitionGuard at creation per `mm_case_type.tt_scope`. Child entities inherit the case FK, not their own TTT (single anchor).
Country handling: single-country deployment (MLT) — country FK **omitted** (recorded per convention).

### 2.2 Entity inventory

| Entity | Joget table (app_fd_) | Kind | PK strategy | Parent | Notes |
|---|---|---|---|---|---|
| case | cmCase | main | IdGenerator, prefix from mm_case_type (e.g. DM-??????) | — | spine; subject_ref typed (kind+id); origin; classification; current_state |
| case_task | cmTask | child grid | uuid | case | work items; queue rows derive from here; `flag_ref` couples an indicator that auto-clears on close (E7) |
| case_deadline | cmDeadline | child grid | uuid | case | named clocks: kind {target/hard}, clock_state {running/paused/met/breached}, pause_reason |
| case_hold | cmHold | child grid | uuid | case | scope, basis, asserted/released, target BB reference |
| case_decision | cmDecision | child grid | uuid | case | type, outcome, reasons, authority level, approval chain ref |
| case_document | cmDoc | child grid | uuid | case | register: class, direction, confidentiality, mayan_uuid, checksum, dispatch + delivery proof (WF-FR-016/019) |
| case_link | cmLink | junction | uuid | — | typed case↔case / case↔decision relations |
| case_event | cmEvent | log/event | uuid | case | append-only; actor/time/type/payload; tamper-evident hash chain (WF-FR-020) |
| case_note | cmNote | child grid | uuid | case | confidentiality-classed |
| case_party_role | cmParty | child grid | uuid | case | representative, counsel, counterparty |
| mm_case_type | mmCaseType | config-catalogue | code | — | id format, tt_scope, owning BB, policies refs (WF-FR-001/002) |
| mm_lifecycle_state | mmState | config-catalogue | code | mm_case_type | incl. custom states, version (in-flight keep version) |
| mm_lifecycle_transition | mmTransition | config-catalogue | uuid | mm_case_type | from/to/guard refs/actor roles |
| mm_allocation_policy | mmAlloc | config-catalogue | code | mm_case_type | criteria, supervisor mode, rebalancing (WF-FR-007) |
| mm_coi_rule | mmCoi | config-catalogue | uuid | mm_case_type | matrix + independence constraints; override authority |
| mm_sla | mmSla | config-catalogue | code | mm_case_type | clocks: source, duration rule, thresholds 75/90%, escalation chain (WF-FR-010) |
| mm_authority_matrix | mmAuthority | config-catalogue | uuid | — | (action × amount band) → level; body type incl. collegial (DPM D6) |
| mm_hold_policy | mmHoldPolicy | config-catalogue | uuid | mm_case_type | permitted holds incl. **suppression scopes** CORRESPONDENCE_SUPPRESS / ENFORCEMENT_SUPPRESS (E1); auto assert/release; dispute-effect rows (DPM D8) |
| mm_task_rule | mmTaskRule | config-catalogue | uuid | mm_case_type | auto task generation: (event/condition) → task type + assignee rule + due rule; generalises pending-info (E2) |
| mm_calendar / mm_statute | mmCalendar / mmStatute | config-catalogue | code | — | working-day mode, holidays; limitation periods per tax type (DPM D11; SIGTAS APPLY_ON_HOLIDAY precedent) |
| mm_doc_requirement | mmDocReq | config-catalogue | uuid | mm_case_type | required document classes per state (guard input) |
| mm_link_type | mmLinkType | MD-lookup | code | — | permitted link types + target case types |
| mm_notification_rule | mmNotifRule | config-catalogue | uuid | mm_case_type | event→template→channel incl. taxpayer preference (WF-FR-013/014); `consolidation_mode` for per-TIN single-notice (E3, BR-DM-009) |
| md_channel / md_decision_type / md_hold_type / md_doc_class | mdChannel / mdDecisionType / mdHoldType / mdDocClass | MD-lookup | code | — | + companion list_* datalists |

### 2.3 State machines

**Entity: case (generic envelope — WF-FR-001; per-type states are mm configuration *within* this envelope; the envelope is the only XPDL)**
States: New, Open, InProgress, OnHold, PendingClosure, Closed. Terminal: Closed (re-openable per WF-FR-005).

| From | To | Trigger/actor | Guard | Guard realised as |
|---|---|---|---|---|
| (create) | New | system rule / authorised officer (WF-FR-003) | type registered + TTT scope satisfied + no duplicate per type dedup policy | TransitionGuard (plugin) |
| New | Open | allocation completes | assignee resolved; COI clear | AllocationEngine + TransitionGuard |
| Open | InProgress | assigned officer first action | — | workflow route |
| InProgress | OnHold | officer/system | hold policy permits; pending-info or hold active | TransitionGuard reading mm_hold_policy |
| OnHold | InProgress | response received / hold released | resuming clock rules applied | DeadlineEngine resume + route |
| InProgress | PendingClosure | officer proposes outcome | required documents per state present (mm_doc_requirement); open child tasks closed; decision approved per mm_authority_matrix | TransitionGuard |
| PendingClosure | Closed | approver | authority level satisfied | TransitionGuard + decision service |
| Closed | Open | authorised re-open (WF-FR-005) | re-open rule + authority; new activity linked to original | TransitionGuard; case_event records linkage |

**Entity: case_deadline (clock)** — Running → Paused (suspension w/ reason) → Running; Running → Met / Breached (terminal). Breach guard: escalation chain fired exactly once → DeadlineEngine (plugin, scheduled).
**Entity: case_hold** — Requested → Active → Released (terminal). Guards: policy permits (TransitionGuard); assert/release propagated to owning BB → HoldConnector.
**Entity: case_decision** — Proposed → UnderApproval → Approved / Rejected (terminal). Guard: approver level per mm_authority_matrix; collegial body = N approvals → decision service config + TransitionGuard.

## 3. Configuration vs Runtime split

| Concern | Realisation | Generator skill |
|---|---|---|
| Case data capture (spine + children) | Forms | joget-form-gen |
| Metamodel + MD administration | Forms (mm_*/md_*) + list_* datalists, approval-gated (P8) | joget-form-gen / joget-datalist-gen |
| Queues, worklists, history views | Datalists (priority formula columns, SLA traffic lights WF-FR-011) | joget-datalist-gen |
| Navigation/personas | One CMBB userview (admin + oversight) | joget-userview-gen |
| Generic lifecycle envelope | XPDL from §2.3 state machine | joget-workflow-gen |
| Decision documents, notices (ADG WF-FR-017) | JasperReports → filed via MayanConnector | joget-jasper-report |
| Everything below | Plugins (budget) | joget-plugin-dev |

### Plugin budget

| Plugin ID | Type | Forcing FR | DAO reads | DAO writes | Complexity |
|---|---|---|---|---|---|
| TransitionGuard | Validator + ProcessTool | WF-FR-001/005; GCMF §3.3-1 (configured guards, cross-form checks) | mm_*, cmCase children | cmEvent | M |
| AllocationEngine | ProcessTool + ParticipantPlugin | WF-FR-007/008 (rules, workload, COI) | mmAlloc, mmCoi, cmCase, dir_user | cmCase.assignment, cmEvent | H |
| DeadlineEngine | ApplicationPlugin (scheduled) | WF-FR-010/012 (clocks, thresholds, escalation independent of officer) | mmSla, mmCalendar, cmDeadline | cmDeadline, cmTask, cmEvent | H |
| HoldConnector | ProcessTool | GCMF §3.3-7; DPM D8 (assert/release in owning BBs) | mmHoldPolicy, cmHold | cmHold, target-BB interface | M |
| DecisionEngine | ProcessTool (form post-processor) | GCMF §3.3-6/7/10; DPM D6 (decisions, linkage, pending-info — F08 growth, see amendment log) | mmAuthority, mmLinkType, cmCase, cmDecision, cmTask | cmDecision, cmLink, cmTask, cmCase.state, cmEvent | M |
| EventEmitter | ProcessTool + scheduled | WF-FR-004/020 (immutable history; KPI emission) | cmCase children | cmEvent (append-only, hash chain) | M |
| MayanConnector | ProcessTool + API client lib | WF-FR-017/018/019; P15 (REST v4 file/retrieve/metadata) | cmDoc, mmDocReq | cmDoc, cmEvent | M |
| NotificationDispatcher | ProcessTool | WF-FR-013…016 (multi-channel, templates, outbound log); **checks active CORRESPONDENCE_SUPPRESS holds before dispatch (E1)**; consolidation per mmNotifRule (E3) | mmNotifRule, md_channel, cmHold | cmDoc (dispatch proof), cmEvent | M |
| GoldMartClient | shared lib (used by BB plugins) | I-1 / D-SAD-02 (JDBC read of sta_v1) | sta_v1 (external) | — | S |
| OutcomeWriteback | ProcessTool (queue-retry) | I-2 / Blueprint §14.2.2 (idempotent outcomes) | cmCase, cmEvent | fact_case_outcomes (external) | M |

Nine entries vs GCMF's "~5 shared plugins": the four engines match the sketch; MayanConnector/NotificationDispatcher/GoldMartClient/OutcomeWriteback are the SAD I-1/I-2/I-5/I-6 **integration connectors** — counted here once so no BB ever re-implements them (P7). Budget rule in force: growth during Stage 2 returns the feature here.

## 4. Interface contracts

| Interface | Direction | Mechanism | Contract |
|---|---|---|---|
| Case-type registration (BB→CMBB) | in | mm_* form data (config) | GCMF §4 extension points; BB CAD declares conformance |
| Hold assertion/release (CMBB→BB) | out | HoldConnector → BB interface (in-instance FormDataDao via declared service forms) | hold {scope, basis, case_id, assert/release}; consistency rule: hold state ≡ case state |
| Transition-guard providers (BB→CMBB) | in | named guard refs in mmTransition → BB plugin classes | guard(caseContext) → pass/fail + reason (LSP contract) |
| Documents (CMBB→Mayan) | out | MayanConnector, REST v4, token env | upload+metadata (TTT spine) → uuid+checksum to cmDoc; retrieve by uuid (I-5) |
| Notifications (CMBB→gateways) | out | NotificationDispatcher → email/SMS stubs (I-6) | template+channel+recipient; delivery status back to cmDoc/cmEvent |
| Accounting reads (BB plugins→product) | out | GoldMartClient JDBC, sta_reader (I-1) | sta_v1 views; `as_of` surfaced (INT-FR-004) |
| Outcome writeback (CMBB→platform) | out | OutcomeWriteback REST/JDBC writeback_api (I-2) | fact_case_outcomes key (case_id,date,type,code); idempotent; queue-retry |
| KPI events (CMBB→Module 10) | out (future) | cmEvent export | typed events + standard dimensions (GCMF §3.5); consumer TBD |
| I-9 Inbound case API (external→CMBB) | in (future, designed only) | Joget API/plugin endpoint | open case {type, TIN, subject_ref, origin=external} → case_id; outcome callback on closure — ED integration pattern (E6); not built until a consumer exists |

## 5. Userview & persona map

| Persona (SAD §8) | Category | Menus | Permission |
|---|---|---|---|
| IT Administrator / Citizen Developer | Case-type administration | mm_* CRUD (approval-gated), md_* CRUD, profile seeds | role cmbb_admin |
| Policy Approver | Approvals | pending config approvals queue | role cmbb_approver (≠ author, GCMF §3.4) |
| Team Leader / Supervisor | Oversight | team queues, reallocation (WF-FR-008), SLA dashboard (WF-FR-011) | role supervisor |
| External Auditor | Audit | case history search, event log, notification log (read-only) | role auditor, row-level read |

(Officer-facing worklists ship in the **DMBB** userview — CMBB provides the datalist definitions; features amend, never multiply.)

## 6. NFR realisation notes

| NFR | Design response |
|---|---|
| Audit immutability (M08 §6.6, WF-FR-020) | cmEvent append-only (no edit forms, store-binder denies update), hash-chained, all writes via Joget API (P3) |
| SLA independence from officer availability | DeadlineEngine scheduled (server-side), not session-bound |
| Volumes (12-officer team, ~10⁴ cases/yr) | standard app_fd_ tables suffice; queue datalists paginate; no partitioning needed |
| Degraded reads (INT-FR-004) | GoldMartClient caches last snapshot + as_of banner; case work never blocks on product outage (P11) |
| Config change traceability (P8) | mm_* edits run as CMBB cases (policy-change case type) → cmEvent |

## 7. Feature decomposition

| Feature ID | Name | FR coverage | Entities | Depends on |
|---|---|---|---|---|
| CMBB-F01-mm-metamodel | Case-type metamodel + MD set + seeds | WF-FR-001/002 (config surface) | all mm_*, md_* | — |
| CMBB-F02-case-spine | Case spine forms, lifecycle envelope XPDL, TransitionGuard, case history, re-open | WF-FR-001…005; WF-FR-020 (history part) | case, case_task, case_note, case_event | F01 |
| CMBB-F03-allocation | AllocationEngine + COI + reassignment | WF-FR-007/008 | case (assignment), mmAlloc, mmCoi | F02 |
| CMBB-F04-queues | Unit/type queues + personal worklist + priority columns | WF-FR-006/009 | datalists over case/case_task | F02 |
| CMBB-F05-deadline-sla | DeadlineEngine, SLA config, traffic lights, escalation | WF-FR-010…012 | case_deadline, mmSla, mmCalendar | F02 |
| CMBB-F06-notifications | NotificationDispatcher, templates, internal alerts, outbound log | WF-FR-013…016 | mmNotifRule, md_channel, cmDoc(dispatch), cmEvent | F02 |
| CMBB-F07-documents | cmDoc register + MayanConnector + ADG (Jasper) + postal tracking + doc-per-state reqs | WF-FR-017…019 | case_document, mmDocReq | F02; Mayan (done) |
| CMBB-F08-decisions-holds-links | Decision/approval service + authority matrix + HoldConnector + linkage + pending-info loop | GCMF §3.3-5/6/7/10; DPM D6/D8 (no direct WF-FR — GCMF-sourced, consumed by DMBB S3/S4) | case_decision, case_hold, case_link, mmAuthority, mmHoldPolicy, mmLinkType | F02 |
| CMBB-F09-events-kpi | EventEmitter completion: hash chain, KPI emission, OutcomeWriteback, GoldMartClient lib | WF-FR-020 (full); I-1/I-2 | case_event | F02 |

Slice map: **S1** = F01, F02 · **S2** = F03, F04, F05 · **S3** = F06, F07, F08, F09.
Enrichment placement (E1–E7, see Bidder-CM-Claims-Analysis): E1→F08 (hold scopes) + F06 (dispatcher guard); E2→F02 (mm_task_rule + generation); E3→F06; E4→F08 (pattern note); E5→F04 (bulk actions, guard-checked); E6→§4 I-9 (designed only); E7→F02. No new plugins — budget unchanged (growth absorbed in existing engines' configuration surface).
Completeness: WF-FR-001…020 all covered (001✓F01/F02, 002✓F01, 003✓F02, 004✓F02, 005✓F02, 006✓F04, 007✓F03, 008✓F03, 009✓F04, 010–012✓F05, 013–016✓F06, 017–019✓F07, 020✓F02+F09). F08 carries no M08 WF-FR by design — it realises GCMF capabilities that DMBB scope cards S3/S4 depend on; forcing requirement = GCMF §3.3 + DPM D6/D8 (recorded, not orphaned).
M05 §4.3 (accounting-operations case FRs) conformance: satisfied by F01/F02/F06/F07 configuration when a Domain-1 STA build arrives — no CMBB change anticipated (GCMF §4 rule).

---

## Gate G1 — Architecture Done

- [x] Entity inventory complete; every entity TA-RDM-mapped **to-verify (OPEN-1)** or exempt-flagged — mappings provisional until L2 YAMLs located; not gate-blocking (CADs proceed per SAD OPEN-1 note)
- [x] State machines: case (envelope), case_deadline, case_hold, case_decision — all case-bearing entities covered
- [x] Plugin budget: 9 entries, each typed + FR/interface-justified; growth rule stated
- [x] Interface contracts cover all SAD boundaries the slices touch (I-1/I-2/I-5/I-6 + BB extension points)
- [x] Feature decomposition covers 100% of WF-FR-001…020; F08's GCMF sourcing documented
- [x] Every [Configurable] has a carrier category: mm_* config-catalogue (lifecycles, SLA, allocation, authority, holds, calendars, notification rules), md_* lookups (channels, classes), plugin properties (gateway endpoints, Mayan token env)

**Stage-2 hand-off (FIS-ready, in order):** CMBB-F01-mm-metamodel → CMBB-F02-case-spine (then S2: F03, F04, F05; S3: F06, F07, F08, F09).
