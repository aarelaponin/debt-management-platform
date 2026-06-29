# SAD — Solution Architecture Document
## Joget-Based Debt Management Solution (MTCA) — Domain 4 Build

**Version 0.3 Draft · 11 June 2026 · Author:** Aare Lapõnin
**Status:** For review (gate: SAD approval precedes all Stage-0/1 work)
**v0.3 changes:** Lifecycle reframed — the solution is **not interim**: the `sta_v1` contract (Module 05, canonical per the Menhard functional specifications and mandatory for ITCAS) and the Domain-4 build (CMBB/DMBB) are durable; **only the ASA is interim** (retires with legacy). "Interim" wording inherited from Modules 05/08 §1.3 recorded as a position change (D-SAD-13).
**v0.2 changes:** TABB removed from build scope on TARA zoning grounds (accounting = Domain 1 Operational Tax Processing; this project = Domain 4 Case Management). The Accounting Semantics Adaptor (ASA) identified as the missing Domain-2 component the debt quick win depends on; mock repositioned as the ASA reference implementation. Decision log and open items updated accordingly.
**Inputs:** TA Reference Architecture (TARA, esp. Ch. 5–6, 10), Module 05 v1.1 (Taxpayer Accounting — *as data-product design authority, not build scope*), Module 08 v1.1 (Debt Management), Module 13 (GCMF outline), Module 14 (DMS), Mayan-EDMS.md, Data Platform Blueprint v1.0 (Ch. 8, 13.2, 14.2–14.3), `Legacy-DB-Findings_and_Gold-Mock-Architecture.md`, `Architectural_Principles_Joget_DM_v0.1.md` (binding), `JOGET-DELIVERY-WORKFLOW.md` (governing process).

---

## 1. Purpose and Position

This SAD fixes the architecture for the **debt-management solution**: a TARA **Domain 4** (Case Management Platform) build on Joget DX, consuming the MTCA Data Platform (Domain 2). It is a **durable** asset, not an interim one — it is *exit-ready* (BPMN-exportable workflows, open formats, contract-stable interfaces; P10) without being end-dated. Whether ITCAS Phase-2 case modules ever displace it is a future governance decision the architecture keeps cheap, not an assumption baked into it.

**Lifecycle classification (D-SAD-13):**

| Class | Components | Rationale |
|---|---|---|
| **Permanent** | `sta_v1` product contract; Data Platform (Domain 2); CMBB + DMBB (Domain 4); Mayan DMS | The contract is **absolute**: Module 05 is the canonical accounting model (Menhard functional specifications) and is mandatory for ITCAS as well — sources change beneath it, the model does not. The Gold layer abstracts all temporal source dependencies away from consumers. |
| **Interim** | **ASA only** | Exists exactly as long as legacy is the accounting system of record; at ITCAS go-live the product re-sources from ITCAS (which must conform to the same Module-05 model) and the ASA is retired. (Module 05's own watermark mechanism "retires at migration" — INT-FR-019 — same pattern.) |
| **External, evolving** | Sources: legacy Informix → ITCAS; SAS VIYA; Keycloak | Consumed through contracts; swaps are invisible to Domain 4. |

**Zoning rule (D-SAD-10).** This project automates *structured back-office case work* — TARA Domain 4. Capabilities belonging to other domains are consumed through contracts, never built here: accounting computation (Domain 1 — Operational Tax Processing, per TARA capability map row 4 "Accounting/Payments/Refunds"), analytical transformation (Domain 2 — Data Platform), risk scoring (Domain 3). Module 05's interim STA is a **Domain 1 system** and is therefore out of this project's build scope; Module 05 remains the **design authority for the accounting data product contract** this project consumes.

**The solution in one paragraph.** Legacy Informix systems remain — for now — the transactional systems of record. The Data Platform consolidates them; within it, the **Accounting Semantics Adaptor (ASA — §3, the one interim component)** transforms legacy accounting data into the published, Module-05-shaped **accounting data product**. On Joget, two building blocks — CMBB (the case fabric) and DMBB (debt management) — read that product over JDBC, run the debt lifecycle, file every document in Mayan EDMS, and return operational data to the platform via idempotent outcome writeback and ingestion as a regular source. Nothing is written back to legacy.

## 2. Component Landscape

```
┌─ DOMAIN 1 · OPERATIONAL TAX PROCESSING (out of scope) ───────────────────────┐
│  Legacy Informix (irdnew, ars, vat, …) + PowerBuilder — systems of record    │
│  [future: interim STA per Module 05, or ITCAS accounting module]             │
└──────────────┬───────────────────────────────────────────────────────────────┘
               │ ingestion (platform-owned)
┌─ DOMAIN 2 · DATA PLATFORM (ORS/ClickHouse) ──────────────────────────────────┐
│  Bronze → Silver → ★ ASA: Accounting Semantics Adaptor (dbt models +         │
│  semantic mapping spec; MISSING TODAY — see §3) → GOLD:                      │
│  accounting data product `sta_v1` (§5.1) + gold_debt marts                   │
│  + fact_case_outcomes (writeback target)                                     │
│  [DEV: MOCKED — the mock IS the ASA reference implementation]                │
└──────┬────────────────────────────────────────────────────▲─────────▲────────┘
       │ ① JDBC reads (versioned views, read-only role)     │ ② REST  │ ③ ingestion
┌─ DOMAIN 4 · CASE MANAGEMENT — THIS BUILD ─────────────────┴─────────┴────────┐
│  Joget DX instance (jdx-mtca-dev), two apps:                                 │
│  ┌─ CMBB — case fabric (GCMF) ────────────────────────────────────────────┐  │
│  │ case spine + mm_* metamodel; shared plugins: TransitionGuard,          │  │
│  │ AllocationEngine, DeadlineEngine, HoldConnector, EventEmitter,         │  │
│  │ MayanConnector, GoldMartClient, OutcomeWriteback                       │  │
│  └───────────────▲────────────────────────────────────────────────────────┘  │
│  ┌─ DMBB ────────┴────────────────────────────────────────────────────────┐  │
│  │ debt cases (C1–C5), reminders, escalation FSM, instalment agreements,  │  │
│  │ enforcement (14 action types), write-off, debtors list, collection MI; │  │
│  │ config-driven calculators for instalment-interest projection &        │  │
│  │ set-off awareness (informational, never authoritative postings)        │  │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└────────┬──────────────────────────────────────────────────────────────────────┘
         │ REST API v4 (MayanConnector)        Notifications: email/SMS gateway
┌─ MAYAN EDMS ────────────────────────┐       Keycloak SSO: Phase 2
│ binaries, versions, OCR, retention  │       SAS VIYA (Domain 3): deferred (§7 I-7)
└─────────────────────────────────────┘
```

**Component charters:**
- **CMBB** owns case mechanics (lifecycle, allocation, COI, deadlines/SLA, pending-info, decisions, holds, linkage, events, queues) and satisfies the Modules 05/08 §5 platform catalogues once, for every future BB (P7). It owns no business meaning.
- **DMBB** owns the debt lifecycle: identification → reminder → demand → escalation → enforcement → resolution (payment / instalment / write-off), debtors-list management, collection MI. It computes **no** balances and no authoritative interest — accounting figures come from the product; its own calculators (instalment projection) are configuration-driven and informational (D-SAD-04).
- **ASA (Domain 2 — external dependency, specified by this project, mocked in DEV)** owns the legacy→STA semantic transformation: §3.
- **Data product** owns the published views (§5.1). **Mayan** owns document binaries (P15); Joget keeps the register.
- **Interim STA / TABB (Domain 1 — not in this project).** If MTCA later builds Module 05's interim STA, it plugs in as a second contributor to the product (the v0.1 "two-plane" analysis is preserved in §3.3 for that day); DMBB is unaffected — its read path never pointed anywhere but the product.

## 3. The Show-Stopper Finding — the Accounting Semantics Adaptor (D-SAD-11)

### 3.1 The gap

The R9 quick-win logic was: *debt management automation once the comprehensive data platform per Blueprint is ready*, with the Gold layer assumed to carry an STA-shaped accounting product per Module 05. The legacy review (`Legacy-DB-Findings…`) shows what the platform will actually ingest: raw legacy accounting — `ars:tpbl/tplg` balances and postings keyed by `trcd/sbcd` codes, receipts by `ptyp`, statements that exist only as monthly RST batch output, interest computed at statement-run time rather than stored per transaction. **Between Bronze-ingested legacy data and the Module-05-shaped product sits an unbuilt, unowned, unspecified transformation: the ASA.**

The ASA is not plumbing; it encodes accounting *meaning*: which `trcd/sbcd` combinations constitute enforceable liability vs adjustment vs journal; how year-of-assessment balances roll; how PA/IA/PCA decomposition is derived (or approximated and flagged) when legacy does not store it; what "disputed" and "collectible" mean in legacy terms (`dudt`, `objectionref`, `ptexceptions`). That knowledge lives in the income-dw SQL estate and in the heads of MTCA accounting/IT veterans — the same capacity gap the mission reporting has flagged since February.

### 3.2 Architectural placement and ownership

The ASA is a **Domain 2 asset**: a specified, version-controlled, tested set of dbt Silver→Gold models (the Blueprint's own `int_payments__reconciled` and `compliance status` models are embryonic pieces of it), with the `sta_v1` view catalogue (§5.1) as its output contract and Module 05 as the contract's design authority. Owner: MTCA Data Unit, with this project supplying the **ASA Semantic Mapping Specification** (legacy tables/codes → product concepts; derivable vs approximated fields; per-field confidence) derived from the legacy review + Module 05. Programme-level: this is a new, named dependency of R9 to be raised with the IMF handler / steering — without an owner and timeline, the debt quick win's data foundation does not materialise.

### 3.2a Adaptor, not accounting module (D-SAD-12)

To enable DM, **the ASA suffices; a Module-05 accounting module must not be built while legacy remains system of record.** (a) Everything DM consumes exists semantically in legacy — the ASA transforms and composes (consolidation, code mapping, ageing, C1–C5 banding) but computes no new accounting facts; the moment it would have to *compute* one, it has crossed into Domain 1 and the design is wrong. (b) An interim STA computing interest/allocations by modernised rules in parallel with legacy creates **two accounting truths** — a P9 violation at programme scale and a legal hazard on enforcement documents. (c) The contract's fields that legacy cannot supply cleanly degrade gracefully and honestly: PA/IA/PCA published with per-field derivation confidence; interest "as at last statement run" surfaced via `as_of` (the same property today's legacy demand notes have). Module 05's role in this programme phase is **contract design authority only**; its build belongs to the legacy-replacement step (ITCAS).

### 3.3 Source evolution and the contract's permanence

The product contract is deliberately **source-agnostic** and outlives every source generation: (1) today — ASA composes legacy data; (2) optionally — a Domain-1 STA build contributes modernised artefacts (kept possible, not planned; two-truths rule D-SAD-12 applies); (3) ITCAS go-live — ITCAS accounting, which per the Menhard recommendations must itself conform to the Module-05 model, feeds the product natively and **the ASA retires**. In all states DMBB's read path is byte-for-byte unchanged — the Gold layer abstracts the temporal dependency away. That invariance is the architectural payoff of D-SAD-01/02 and the reason the Domain-4 build is permanent, not interim.

### 3.4 What this build contributes

The DEV mock seeds synthetic *legacy-shaped* data and derives the `sta_v1` views with explicit SQL — i.e. the mock **is** the ASA reference implementation: every mapping decision it makes is captured in the Semantic Mapping Specification and handed to the Data Unit as a tested starting point, instead of a blank page. This converts the mock from throwaway scaffolding into programme value.

## 4. Accounting Truth (D-SAD-01, restated for v0.2 scope)

One read source: **DMBB reads accounting state exclusively from the product** (`sta_v1` views) — never from legacy, never from any future STA's tables, never computing its own. Within a debt case, figures are stored only as timestamped **snapshots** (case evidence); live figures are re-read (INT-FR-004 staleness banner). Single-writer matrix: platform/ASA owns source-derived facts; DMBB owns case/action/instalment records; CMBB owns case_event; Mayan owns binaries.

## 5. Data Architecture

### 5.1 The accounting data product (contract; designed from Module 05)

Published, versioned ClickHouse views (`sta_v1`); the view schema is the contract (D-SAD-02):

| View | Content / grain | Anchor |
|---|---|---|
| `taxpayer_360` | registry + registration flags per TIN | 05 §9.2.4 |
| `taxpayer_balances` | L1/L2 per TIN (× tax type), debit/credit, PA/IA/PCA (with per-field derivation confidence from ASA), disputed flag | 05 §9.2.4, STA-FR-001…010 |
| `account_transactions` | L3 per TIN × tax type × period, typed transactions, running balance | 05 §9.1 |
| `payment_history` | unified payment stream, identification status | 05 §9.2.4 |
| `assessment_register` | self/authority/default assessments, caution flag | 05 §9.1 |
| `open_credits_suspense` | credits + suspense by kind | 05 §9.1, STA-FR-046 |
| `debt_balances` | enforceable balances, age bands, C1–C5, oldest-debt-age | 08 §4.1, BR-DM-001…005 |
| `debt_priority_queue` | risk-ranked work queue (risk column NULL until SAS) | 08 §4.2 |
| `case_outcomes` | writeback landing fact (②) | Blueprint §14.2.2 |

TTT discipline throughout; code lists from Modules 05/08 with legacy vocabulary as realism input (Module 05 wins on conflict).

### 5.2 The closed loop

**① JDBC reads** of product views · **② REST outcome writeback** (narrow, idempotent — ReplacingMergeTree on case_id+outcome_date) · **③ batch ingestion** of Joget PostgreSQL as a regular platform source (watermark = Joget `dateModified`; hence P3's API-only writes). Nightly ②-vs-③ reconciliation (post-MVP backlog). Full rationale: legacy-findings doc §3.

### 5.3 Entity ownership (CAD input)

- **CMBB:** case, case_task, case_deadline, case_hold, case_decision, case_document (register), case_link, case_event, case_note, case_party_role + `mm_*` metamodel (GCMF §3.1–3.2).
- **DMBB:** debt_case (C1–C5, FSM per 08 §9.1), instalment_agreement + instalment_payment, demand_notice, recovery_action (14 types), write_off, agent, debtors_list — all CMBB case-type specialisations (P7) — plus `md*` carriers for thresholds, escalation calendars, projection rates (P4).
- TA-RDM L2 mapping in each CAD; YAML location outstanding (OPEN-1).

## 6. Application & Instance Topology (D-SAD-03)

One DEV Joget instance, **two apps** (`cmbb`, `dmbb`); `app_fd_*` is instance-scoped, so CMBB's shared plugins and cross-app reads are native (P3); BB isolation by table ownership + interfaces, gate-checked (P14 keeps later separation possible).

| Instance | Purpose | Apps |
|---|---|---|
| `jdx-mtca-dev` | primary DEV (DX8, skills-validated) | cmbb, dmbb |
| `jdx-mtca-dx9` | DX9 compatibility sandbox (workflow §7) | per-batch imports |

Registered via `joget-instance-setup` (PostgreSQL backend — matches flow ③). Docker Compose alongside: ClickHouse (mock product/ASA) + Mayan (per Mayan-EDMS.md, port 8880, pinned s4.11).

## 7. Integration Contracts

| # | Interface | Direction | Mechanism | Contract / notes |
|---|---|---|---|---|
| I-1 | Product reads | platform → Joget | **JDBC** on `sta_v1.*`, read-only role; `GoldMartClient` for plugins; JdbcDataListBinder/Jasper directly | Versioned schemas; <2 s single-TIN (INT-FR-001 substance; "RESTful" letter = deviation D-SAD-02); `as_of` on every screen (INT-FR-004) |
| I-2 | Outcome writeback | Joget → platform | REST POST (OutcomeWriteback plugin on workflow events) | Idempotent; queue-and-retry on outage; payload per Blueprint §14.2.2 |
| I-3 | Joget-as-source | Joget DB → platform | Batch ingestion (platform pipeline; designed now, built later) | `stg_joget__*` staging contract; watermark `dateModified` |
| I-4 | Accounting operations | DMBB → Domain 1 | **Out of scope**: no interim STA exists; set-off execution & authoritative postings remain legacy/manual; DMBB records the *decision* + outcome only | Revisit if/when interim STA (Module 05) is built |
| I-5 | Documents | CMBB → Mayan | REST API v4, token auth, single MayanConnector | P15; types per 05 §9.1 + 08 notices; TTT metadata spine |
| I-6 | Notifications | Joget → gateways | email/SMS per 08 §4.14.6 | Templates = configuration (P4); dispatch + delivery proof → register + case_event |
| I-7 | Risk scoring | SAS VIYA → product | deferred | `debt_priority_queue.risk_score` nullable; no Joget change when SAS lands |
| I-8 | SSO | Keycloak ↔ Joget | OIDC, Phase 2 | DEV: local accounts; artefacts assume no auth implementation (P12/P14) |

## 8. Personas → Userview Topology

One userview per BB (features amend, never multiply). From Module 08 §3.2 (+ CMBB administration):

| Persona | Userview (categories) |
|---|---|
| Debt Management Officer | DMBB: My Queue, Debt Cases, Instalments, Enforcement |
| Team Leader / Supervisor | DMBB: Team Queues, Approvals, Reallocation + CMBB oversight |
| Senior Management / Director | DMBB dashboards (datalists + Jasper; heavy analytics stays in Superset, P2) |
| Policy Administrator (business) | **DMBB-admin** console: DPM domains D1–D11, profiles, dry-run simulation, activation (policy changes run as CMBB cases; never touches Joget Builders — D-SAD-15) |
| Policy Approver | DMBB-admin approval queues (segregated from author role, GCMF §3.4) |
| IT Administrator / Citizen Developer | CMBB: case-type metamodel, parameters, templates (audited + approval-gated, P8) |
| External Auditor | read-only userview, row-level permission classes (P12) |
| Taxpayer | indirect only (receives notices/agreements); portal deferred |

(Module 05's accounting-clerk personas depart with TABB — they return with the Domain-1 project.)

## 9. Documents, Security, NFRs (summary)

**Documents (P15):** Jasper generates (demand notices, instalment agreements, judicial letters, debtor-list extracts; TAS/TCC belong to Domain 1 — out), MayanConnector files with TTT metadata, register row + checksum in CMBB, dispatch via I-6 with delivery proof, required-docs-per-state as transition guards; Mayan config = versioned Python script.
**Security (P12):** DEV perimeter-relaxed; application baseline non-negotiable (API-only data access, server-side validators, secrets in env/instance config, synthetic data only, role/row-level permissions from first userview, decision/execution segregation per GCMF §3.4). Phase 2: Keycloak OIDC, TLS, MITA hosting.
**NFRs (CADs elaborate per 08 §6):** <2 s single-TIN → product point-lookups; portfolio <5 s → pre-aggregated marts (INT-FR-002); escalation independent of officer availability → DeadlineEngine (scheduled); auditability → case_event + audit columns + TRACE.md; degraded-read mode with staleness banner (INT-FR-004); seed sized from legacy row-count ratios.

## 10. Decision Log

| ID | Decision | Basis |
|---|---|---|
| D-SAD-01 | Single accounting read source = the product; DMBB stores timestamped snapshots only | 08 Domain Overview; P9 |
| D-SAD-02 | Reads via JDBC on versioned product views; REST only for writeback/computation services; INT-FR-001 "RESTful" = documented deviation | 05 §9.2.3; P3; 11 Jun |
| D-SAD-03 | One DEV instance, two apps; isolation by ownership + interfaces | P1, P3, P14 |
| D-SAD-04 | DMBB computes no balances/authoritative interest; instalment projection = config-driven, informational | D-SAD-01; 08 §9.1 |
| D-SAD-05 | Mayan = DMS by integration; Module 14 by mapping | P15; 11 Jun |
| D-SAD-06 | Mock = ClickHouse views + writeback endpoint; flow ③ designed only | P1 |
| D-SAD-07 | SAS deferred behind nullable queue column | I-7; P3-evolution |
| D-SAD-08 | Joget Enterprise assumed (Jasper menu); exit-readiness item | P10; **confirm before A3** |
| D-SAD-09 | GCMF realised as CAD-CMBB directly from outline v0.1 | 11 Jun |
| D-SAD-10 | **TABB out of project scope — TARA zoning** (accounting = Domain 1; this build = Domain 4). Module 05 retained as product-contract design authority | TARA Ch. 5–6; user decision 11 Jun |
| D-SAD-11 | **ASA named as the missing Domain-2 component**; this project delivers its Semantic Mapping Specification + reference implementation (the mock); ownership/timeline escalated to programme level | §3; legacy review; 11 Jun |
| D-SAD-12 | **Adaptor, not accounting module**: ASA transforms/composes legacy facts only — never computes new ones; no parallel STA while legacy is system of record (two-truths hazard); Module 05 = contract authority only | §3.2a; P9; 11 Jun |
| D-SAD-15 | **DMBB-admin / DMBB-runtime split**: policy console (DPM domains, validation, dry-run simulation, approval-gated activation — policy changes run as CMBB cases) vs officer runtime. Two-tier rule: admins never open Joget Builders (policy space = console); structural change stays in the delivery pipeline. Both faces are pipeline-generated Joget artefacts (P3/P13) | TARA 10.2, P8; `DM-Configurability-Analysis.md` §2.4; 11 Jun |
| D-SAD-14 | **Debt Policy Model (DPM)** adopted as DMBB's configurability layer: 11 config domains (categorisation, collection strategies, instrument catalogue, relief products, violation/authority/write-off/dispute-hold/insolvency/publication policies, calendars & statutes) with temporal validity and per-domain binding modes; country/archetype profiles as seed-data sets. Derived from cross-country analysis (KGZ/MNE/KSA/capability model + Module 08). Adds EscalationEngine + ReliefProductInterpreter to the DMBB plugin budget. See `DM-Configurability-Analysis.md` | P4, P7, P8; 11 Jun |
| D-SAD-13 | **Lifecycle: solution is durable, ASA is the only interim component.** `sta_v1` contract is absolute (Module 05 canonical per Menhard, mandatory for ITCAS); Gold layer abstracts source evolution; Domain-4 build is exit-ready (P10) but not end-dated. Modules 05/08 §1.3 "interim solution" wording = recorded position change, to amend at next spec revision | §1; user decision 11 Jun |

## 11. Open Items

| ID | Item | Blocks |
|---|---|---|
| OPEN-1 | TA-RDM L2 YAML location | CAD mappings (proceed with "to-verify" marks) |
| OPEN-2 | Joget edition (Enterprise?) for DEV + DX9 sandbox | A3, Jasper |
| OPEN-3 | `ars` schema extract | nothing hard — ASA spec realism |
| OPEN-4 | Notification gateway endpoints for DEV | I-6 (stub acceptable) |
| OPEN-5 | **ASA owner + timeline at MTCA (Data Unit) — programme escalation** (heads-up to IMF handler; candidate input to Mission 06 CD report) | the R9 quick win itself, beyond DEV |

## 12. Next Stages

Stage 0 scope cards per master plan (CMBB-S1 first; DMBB-S1 after CAD-CMBB §7). CAD order: **CMBB → DMBB**. A2 (mock/ASA reference build) proceeds in parallel from §5.1. New deliverable: **ASA Semantic Mapping Specification** (with A2). Master plan to be updated to v0.2 scope (TABB out, ASA in).

*End SAD v0.2 — for review.*
