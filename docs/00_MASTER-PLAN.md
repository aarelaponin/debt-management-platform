# MASTER PLAN — Joget-Based Debt Management Solution
**MTCA · ITCAS workstream 03 · v0.2 (11 June 2026)**

> **v0.2 rescope (per SAD v0.2):** TABB removed from build scope — accounting is TARA Domain 1, this build is Domain 4 (D-SAD-10). The **Accounting Semantics Adaptor (ASA)** identified as the missing Domain-2 dependency of the R9 quick win; this project delivers its mapping specification and reference implementation (the mock), not an accounting module (D-SAD-11/12). TABB/Phase-C content retained below struck-through-in-spirit for traceability; authoritative scope = SAD v0.2.

**Governing process:** `docs/JOGET-DELIVERY-WORKFLOW.md` (Stages 0–4, gates G0–G4).
**Governing principles:** `docs/Architectural_Principles_Joget_DM_v0.1.md` (P1–P15 + SOLID; P3 "with the platform" and P15 "Mayan is the document engine" are the project-specific anchors).
**Requirements base:** `docs/reqs/` — Module 05 (Taxpayer Accounting, STA-FR/RPT-FR/WF-FR + §5 platform catalogue), Module 08 (Debt Management, DM-FR ×58 / RPT-FR ×21 + §5 platform catalogue), Module 13 (GCMF outline v0.1), Module 14 (DMS summary spec).
**Data platform contract:** `05-data-platform/MTCA_Data_Platform_Blueprint_v1.0.docx` — Ch. 8 (dbt marts), Ch. 13.2 (debt NRT path), Ch. 14.2/14.3 (CMP integration contract).

---

## 1. Decisions taken (planning session, 11 Jun 2026)

| # | Decision | Consequence |
|---|---|---|
| D1 | **Architecture first.** A Solution Architecture Document (SAD) precedes all CADs. | New Phase A before Stage 1. |
| D2 | **Mock Gold platform.** A mock of the Data Platform Gold layer is built exactly as the Blueprint specifies it, so Joget integrates against the real contract from day one. | New workstream `platform-mock/`. |
| D3 | **CAD-CMBB directly from GCMF outline v0.1** + the WF/BR/EC/PM catalogues in Modules 05/08 §5. GCM-FR spec back-filled later if needed for tender posture. | Faster Stage 1; CAD is the de-facto GCMF spec. |
| D4 | **Mayan EDMS is the DMS.** Module 14 is realised by integration, not built. CMBB's document service is a thin register over Mayan's REST API. | DMS plugin = Mayan connector; Mayan runs in Docker for DEV. |
| D5 | **New local DEV instances** via `joget-instance-setup`; DX9 sandbox for the compatibility gate (workflow §7). | Phase A includes environment setup. |
| D6 | **TABB out of scope — TARA zoning** (Domain 1 vs Domain 4); Module 05 stays as the data-product contract authority. | Build = CMBB + DMBB only; one DEV instance, two apps (D-SAD-03/10). |
| D7a | **Lifecycle: nothing here is interim except the ASA.** The `sta_v1` contract is absolute (Module 05 = canonical Menhard model, mandatory for ITCAS); the Gold layer abstracts source evolution (legacy → ITCAS); CMBB/DMBB are durable Domain-4 assets, exit-ready but not end-dated (D-SAD-13). | "Interim" wording in Modules 05/08 §1.3 to be amended at next revision. |
| D7 | **Adaptor, not accounting module.** The ASA (Domain 2, dbt) transforms legacy accounting into the Module-05-shaped product; never computes new accounting facts (two-truths hazard). This project writes the ASA Semantic Mapping Spec; the mock is its reference implementation. | New deliverable in Phase A; programme escalation OPEN-5 (R9 dependency). |

## 2. Building blocks and dependency order

```
                  ┌───────────────────────────────┐
                  │ 04 PLATFORM-MOCK (Gold layer)  │  ClickHouse + mock REST API
                  └───────┬───────────────────────┘  (Blueprint contract)
                          │ reads (STA L1–L3, TAS, debt marts)   writes back (fact_case_outcomes)
┌──────────┐      ┌───────┴────────┐      ┌────────────────┐
│ 01 CMBB  │◄─────│ (ASA: Domain 2) │◄────│ 02 DMBB        │
│ case     │      │ taxpayer       │      │ debt           │
│ fabric   │      │ accounting/STA │      │ management     │
└────┬─────┘      └────────────────┘      └────────────────┘
     │ documents/notices via REST
┌────┴─────┐
│ Mayan DMS│  (external component, Docker)
└──────────┘
```

- **CMBB — Case Management Building Block.** Realises the GCMF: case spine + child entities, case-type metamodel (`mm_*`), the ~5 shared plugins (TransitionGuard, AllocationEngine, DeadlineEngine, HoldConnector, EventEmitter) + a **MayanConnector** for the document service. Everything downstream specialises it.
- **~~TABB~~ → out of scope (v0.2, D-SAD-10/12).** Accounting is TARA Domain 1; while legacy remains system of record, no interim accounting module is built (two-truths hazard). Module 05 remains the **contract design authority** for the data product. In its place, the **ASA (Domain 2, external dependency)** adapts legacy accounting to the `sta_v1` product — this project specifies it and the mock reference-implements it.
- **DMBB — Debt Management Building Block** (Module 08). Debt identification & case creation, risk classification, reminders, escalation chains, instalment agreements, enforcement actions, write-off, debtors list, collection MI. All cases are CMBB case types; all accounting state is read from the `sta_v1` product views (JDBC, D-SAD-01/02); instalment-interest projection is config-driven and informational (D-SAD-04); all notices go through Mayan.
- **04 PLATFORM-MOCK.** ClickHouse (Docker) carrying the **accounting data product designed from Module 05** (§9.1 conceptual model, §9.2.4 views: `taxpayer_balances`, `payment_history`, `assessment_register`, `taxpayer_360`) plus the Blueprint's `gold_debt` marts (`fact_debt_balances`, `fact_payment_transactions`, `dim_taxpayer_debt_profile`, `fact_case_outcomes`) and STA L1–L3 / TAS rollups. **Read access = JDBC on versioned product views** (read-only role) — Joget-native for datalists/Jasper/plugins; the only REST piece is the **outcome-writeback** endpoint (Blueprint §14.2.2, idempotent). Seeded with synthetic Maltese taxpayers (legacy review supplies realistic code lists/grains; Module 05 is the design authority). **Mock in data, real in contract** — when the real platform lands, only the connection string changes.

**STA boundary — RESOLVED at SAD v0.2 (D-SAD-01/10/12):** single accounting read source = the `sta_v1` product (ASA-composed from legacy); no accounting module in this build; DMBB stores timestamped snapshots only.

## 3. Phases

### Phase A — Architecture & foundations (now)
| Step | Output | Gate |
|---|---|---|
| A1 | **SAD** — component landscape above, interface contracts (I-1…I-8 per SAD), STA boundary decision, persona/userview topology, instance topology, NFR strategy | SAD reviewed by you |
| A2 | **Mock Gold design + build** — DDL, seed generator, mock API; smoke-queried | contract = Blueprint §13.2.2/§14.2 |
| A3 | **Environments** — `jdx-mtca-dev` (apps: cmbb, dmbb — D-SAD-03), `jdx-mtca-dx9` sandbox, Mayan + ClickHouse Docker compose | instances respond; DX9-DELTAS.md started |

### Phase B — CMBB (the fabric)
Stage 0→4 per workflow. Slices (≈ GCMF §6.1's 6–8 features regrouped):
- **CMBB-S1:** case-type metamodel (`mm_*` MD forms) + case spine + lifecycle engine + TransitionGuard. *Master-data-first slice.*
- **CMBB-S2:** allocation + COI + deadline/SLA engine + queues/worklists.
- **CMBB-S3:** decisions & approvals + holds + linkage + Mayan document service + case events/KPI emission.

### Phase C — ASA workstream (replaces TABB per v0.2; runs with A2, deepens during B)
- **C1: ASA Semantic Mapping Specification** — legacy tables/codes (`tpbl/tplg/lcph/lcpd`, `trcd/sbcd/ptyp`, RST logic, `objectionref`, exceptions) → `sta_v1` product concepts; per-field derivable/approximated/confidence verdicts (PA/IA/PCA, interest as-at).
- **C2: Reference implementation** = the mock's view SQL, captured model-by-model against C1 (handover asset for the MTCA Data Unit).
- **C3: Programme escalation** — ASA owner + timeline at MTCA (SAD OPEN-5; candidate for Mission 06 CD report / heads-up to IMF handler).

### Phase D — DMBB (the target)
- **DMBB-S1:** debt identification + case creation + risk classification (§4.1–4.2; reads `/debt-priority/queue`).
- **DMBB-S2:** notifications/reminders + escalation workflows (§4.3–4.4; notices via Mayan).
- **DMBB-S3:** instalment agreement lifecycle (§4.5).
- **DMBB-S4:** enforcement actions + configuration (§4.6–4.7).
- **DMBB-S5:** write-off, default assessment, debtors list, collection MI dashboards (§4.8–4.11, 4.12 subset).

Each slice: Scope Card (G0) → CAD new/amend (G1) → FIS folder per feature (G2) → generation batch (G3) → deploy+verify+TRACE.md (G4). Module 05/08 §5 platform catalogues (CFW/WF/BR/FM/DM/EC/PM/INT/…) are **CMBB conformance requirements**, not per-module builds — they get a one-time mapping table in CAD-CMBB.

## 4. Repository layout (this folder)

```
03_debt_management/
├── docs/
│   ├── 00_MASTER-PLAN.md            ← this file
│   ├── JOGET-DELIVERY-WORKFLOW.md
│   ├── reqs/                        (frozen requirement inputs, Modules 05/08/13/14)
│   ├── design/
│   │   ├── SAD-DM-Solution.md       A1
│   │   ├── GOLD-MOCK-DESIGN.md      A2
│   │   └── scope-cards/             Stage 0 outputs
│   └── DX9-DELTAS.md                living compatibility log (workflow §7)
├── cmbb/      ├── CAD-CMBB.md ├── TRACE.md ├── features/ ├── generated/ └── seed/
├── dmbb/      (same layout)
└── platform-mock/
    ├── ddl/  seed/  api/  docker-compose.yml
```

Rule: `docs/` holds documents only; the four component folders hold buildable artefacts (specs-as-code in `features/` count as build inputs, so they live with their component).

## 5. Risks / watch items

1. **STA boundary** — wrong call here fragments the data model; locked at SAD review (above).
2. **DX9 gate** — all generator skills are DX8-validated; every Stage-3 batch passes the §7 sandbox check until skills are marked clean.
3. **TA-RDM L2 YAMLs are not in this repo** — CAD entity mapping needs them (GCMF anchors to `07-compliance-control.yaml`, `08-document-management.yaml`). **Action: provide location, or CADs record mappings as "to-verify".**
4. **Mayan capability fit** — required-document-classes-per-state and confidentiality classes must map to Mayan cabinets/tags/ACLs; verified in A1 with a thin spike, before CMBB-S3 hard-wires the connector.
5. **Module 05/08 §5 catalogues** are platform requirements: satisfy once in CMBB, map, don't re-implement — scope discipline at every G1.
6. **Writeback contract** (`fact_case_outcomes`, ReplacingMergeTree on case_id+outcome_date) — idempotency must be honoured by the Joget plugin from the first DMBB slice that resolves cases.

## 6. Immediate next actions

1. **A1 — SAD** (next session): run the architecture session; resolve the STA boundary; produce `docs/design/SAD-DM-Solution.md`.
2. **A2 — Mock Gold design** alongside: `docs/design/GOLD-MOCK-DESIGN.md` + DDL + seed + API.
3. You provide: location of **TA-RDM L2 YAMLs** (risk #3).
4. **A3 — environments** once A1/A2 are drafted.

*End of master plan v0.1.*
