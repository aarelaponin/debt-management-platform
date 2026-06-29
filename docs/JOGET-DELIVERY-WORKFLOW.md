# JOGET DELIVERY WORKFLOW
## From Requirements Specification to Running Joget DX9 Application

**Purpose:** The repeatable workflow that takes any module of the requirements suite (Modules 00–12) through component architecture design into actionable, generation-ready specifications for every feature, and on to validated Joget artefacts.

**Operating principle:** Every stage produces a typed artefact that the next stage consumes without re-interpretation. Claude skills are the execution engines; this workflow defines what flows between them, in what format, and what "done" means at each gate.

---

## 0. THE PIPELINE AT A GLANCE

```
Requirements Suite                Architecture            Feature Specs              Generation                 Validation & Deploy
(Modules 00–12)                   (per Building Block)    (per Feature)              (per Artefact)             (per Release Slice)

┌──────────────────┐   Stage 1   ┌──────────────────┐   Stage 2   ┌─────────────┐   Stage 3   ┌────────────┐   Stage 4   ┌──────────────┐
│ FR catalogue      │──────────►│ Component         │──────────►│ Feature      │──────────►│ Joget       │──────────►│ Deployed +    │
│ (MoSCoW, AC,      │            │ Architecture      │            │ Implementa-  │            │ artefacts   │            │ acceptance-   │
│ BRs, UCs, NFRs)   │            │ Document (CAD)    │            │ tion Specs   │            │ (JSON/XPDL/ │            │ tested slice  │
│                   │            │ + Data Model      │            │ (FIS)        │            │ JAR/JRXML)  │            │ + traceability│
└──────────────────┘            └──────────────────┘            └─────────────┘            └────────────┘            └──────────────┘
        │                               │                              │                          │                          │
   joget-req-analyst              TA-RDM L2 anchor              templates §6             joget-form-gen              joget-instance-setup
   (gap analysis,                 (canonical entities,          + skill spec             joget-datalist-gen          validation scripts
   completeness checks)           TTT spine)                    formats                  joget-userview-gen          gam-db-inspect pattern
                                                                                         joget-workflow-gen          (DB-level verification)
                                                                                         joget-plugin-dev
                                                                                         joget-jasper-report
```

**The two layers this workflow adds** (everything else already exists as skills or documents):

1. **Stage 1 — Component Architecture Document (CAD):** one per building block, answering *what the component is*: its entities, state machines, configuration vs runtime split, and interfaces. Without this layer, form specs get generated from FRs directly and the data model fragments.
2. **Stage 2 — Feature Implementation Spec (FIS):** one per feature, answering *what to build for this FR slice*: the exact set of forms, datalists, userview deltas, workflows, plugins, and reports — in the YAML formats the generation skills consume — plus traceability and acceptance tests.

---

## 1. STAGE 0 — SCOPING AND INTAKE

**Input:** A module specification (e.g., `04_Refunds_...md`) or a capability slice of Module 12.
**Executor:** You + `joget-req-analyst` (Steps 1–3 of its protocol: parse, completeness analysis, clarification round).
**Output:** A **Scope Card** — half a page.

### Scope Card template

```markdown
# SCOPE CARD — <module>-<slice-id>
Module: 04 Refunds Management
FR slice: REF-FR-001…014 (origination, intake, set-off)        ← contiguous FR ranges
MoSCoW floor: Must + Should                                     ← what's in this slice
Archetype target: B (automated, risk-integrated)                ← drives config depth
Excluded FRs + reason: REF-FR-0xx (Could; deferred to slice 3)
Upstream modules assumed present: 01 (TIN), 03, 05
Country profile: generic | <ISO-3>                              ← TA-RDM convention
DX9 compatibility check: required (see §7)
```

**Gate G0 (Definition of Ready):** every FR in the slice has an owner module, no BLOCKER-class open questions remain from `joget-req-analyst`'s completeness analysis, and upstream dependencies are deployed or stubbed.

**Slicing rules:**
- Slice along FR *sections* (the 4.x subsections), never through the middle of one.
- A slice should yield 3–10 features (Stage 2). Larger → split; this matches the sub-stepping pattern you already use for large prompts.
- Master-data-only slices are legitimate first slices (they unblock everything downstream).

---

## 2. STAGE 1 — COMPONENT ARCHITECTURE DESIGN (per Building Block)

**Input:** Scope Card(s) + the module specification + TA-RDM L2 domain YAMLs.
**Executor:** You + Claude (architecture session); `joget-req-analyst` mapping rules (§2 of the skill) applied at entity level.
**Output:** One **Component Architecture Document (CAD)** per building block. Written once per building block, then *amended* per slice — not rewritten.

### 2.1 CAD content (template)

```markdown
# CAD — <BB-ID> <Building Block Name>                    e.g. CAD-RFBB
Version / Status / FR coverage: <module §4 ranges>

## 1. Component Charter
One paragraph: what this component owns, what it explicitly does not own
(boundary statements lifted from the module's §2.3.3), and its place in the
module dependency graph.

## 2. Data Model (the anchor section)
### 2.1 TA-RDM alignment
| Component entity | TA-RDM L2 source | Divergence + reason |
|---|---|---|
| refund_case | 06-payment-refund.yaml :: refund | adds case-workflow columns |
TTT discipline: every transactional entity carries TIN + tax_type + tax_period
FKs (or documents why not). Country handling per L2 convention: NULL
country_code = universal; ISO 3166-1 alpha-3 = specific.

### 2.2 Entity inventory
| Entity | Joget table | Kind | PK strategy | Parent | Notes |
|---|---|---|---|---|---|
| refund_case | app_fd_refundCase | main | IdGenerator RC-?????? | — | case spine |
| refund_setoff_item | app_fd_refundSetoff | child grid | uuid | refund_case | |
| md_refund_type | app_fd_mdRefundType | MD.xx lookup | code | — | config catalogue |
Kind ∈ {main, child, junction, MD-lookup, config-catalogue, log/event}.

### 2.3 State machines
One per case-bearing entity. States, transitions, transition guards (which
become workflow routes or plugin validations), terminal states.
DRAFT → SUBMITTED → OFFSET_APPLIED → {FAST_TRACK | COMPLIANCE_REVIEW} → …

## 3. Configuration vs Runtime split
| Concern | Realisation | Generator skill |
|---|---|---|
| Data capture | Forms | joget-form-gen |
| Listing/worklists | Datalists | joget-datalist-gen |
| Navigation/personas | Userview categories+menus | joget-userview-gen |
| Process & approvals | XPDL + activity maps | joget-workflow-gen |
| Rules that cross forms, calculations, integrations, batch | Plugins (typed) | joget-plugin-dev |
| Pixel documents (decisions, certificates, statements) | JasperReports | joget-jasper-report |
Rule: everything is configuration until it hits a plugin trigger from
joget-req-analyst §"What Needs a Custom Plugin". Plugins are listed by type
(ApplicationPlugin / FormLoadBinder / FormStoreBinder / Validator / …) and
justified — each plugin entry must name the FR that forces it.

## 4. Interface contracts
| Interface | Direction | Mechanism | Contract |
|---|---|---|---|
| Posting to 05 ledger | out | plugin → FormDataDao / API | posting record schema |
| Risk routing from 06 | in | API / shared table | score + reason codes |
External integrations: endpoint, auth, payload schema, failure behaviour.

## 5. Userview & persona map
Persona (from module §3.2) → userview category → menus. Permissions model.

## 6. NFR realisation notes
Only the NFRs this BB must actively design for (volumes, deadline engines,
audit-trail depth) and how — one line each.

## 7. Feature decomposition (feeds Stage 2)
| Feature ID | Name | FR coverage | Entities | Depends on |
|---|---|---|---|---|
| RFBB-F01 | Refund type catalogue + MD set | REF-FR-001 | md_* | — |
| RFBB-F02 | Claim intake (portal+officer) | REF-FR-006…009 | refund_case | F01 |
| RFBB-F03 | Mandatory set-off engine | REF-FR-010…014 | setoff_item | F02, 05-IF |
```

### 2.2 CAD design rules

1. **TA-RDM is the source of entity truth.** Component entities derive from the L2 canonical domains; the CAD records every divergence. This is what keeps Daftaria's per-module data models converging on one warehouse-loadable model (L3) instead of drifting per implementation.
2. **One state machine per case-bearing entity, drawn before any workflow spec.** Workflow XPDL is *generated from* the state machine, never invented at generation time.
3. **The plugin list is a budget.** Each plugin is named, typed, FR-justified, and complexity-rated (the joget-req-analyst Output-D format). A feature whose plugin count grows during Stage 2 goes back to Stage 1.
4. **Feature decomposition is the CAD's contract with Stage 2.** Features are vertical (entity + UI + process + rules for one FR cluster), 1–5 days of build each, and dependency-ordered using the req-analyst build-order rule: MD forms → independent forms → FK-dependent forms → parents with grids → workflows → plugins → reports.

**Gate G1 (Architecture Done):** entity inventory complete with TA-RDM mapping; state machines for all case entities; plugin budget approved; feature decomposition covers 100% of the slice's FRs (checked mechanically — see §5 traceability).

---

## 3. STAGE 2 — FEATURE IMPLEMENTATION SPECS (per Feature)

**Input:** CAD §7 row + the FRs it covers + module BRs/UCs/AC.
**Executor:** Claude with `joget-req-analyst` (Output C/D formats) — this is the stage where "execute the prompt" sessions live.
**Output:** One **FIS** per feature — a folder, not a file, because its sections are the *direct inputs* to the generation skills.

### 3.1 FIS folder layout

```
features/RFBB-F03-setoff-engine/
├── FIS.md                        # the spec head: traceability, AC→tests, decisions
├── forms/
│   ├── F-refundCase.spec.yml     # joget-form-gen input format (extends existing form OK)
│   └── F-setoffItem.spec.yml
├── datalists/
│   └── DL-list_refundCase.spec.yml    # joget-datalist-gen input format
├── userview/
│   └── UV-delta.spec.yml         # joget-userview-gen input — DELTA against the BB userview
├── workflow/
│   └── WF-refundSetoff.spec.yml  # joget-workflow-gen YAML (process, tools, deadlines)
├── plugins/
│   └── PL-SetoffEngine.md        # joget-plugin-dev requirement spec (Output-D format)
├── reports/
│   └── RPT-setoffStatement.spec.yml   # joget-jasper-report spec (if any)
└── tests/
    └── acceptance.md             # FR acceptance criteria → executable checks
```

### 3.2 FIS.md head (template)

```markdown
# FIS — RFBB-F03 — Mandatory Set-Off Engine
Status: Draft | Specified | Generated | Deployed | Accepted
CAD ref: CAD-RFBB §7 row F03

## 1. Traceability
| FR | AC (verbatim from module) | Realised by | Test |
|---|---|---|---|
| REF-FR-010 | "A credit is offset before refund; only net refundable…" | PL-SetoffEngine + WF route | T-03.1 |
| REF-FR-011 | "Offset follows configured priority…" | md_setoff_priority + PL | T-03.2 |
Every FR row names artefacts in this folder. No orphan FRs, no orphan artefacts.

## 2. Business rules in scope
BR-REF-004 (offset before refund, hard), BR-REF-005 (priority, configurable)
→ each BR states WHERE it is enforced: validator / plugin / workflow guard /
  DB constraint. A BR enforced "by officer practice" is a spec failure.

## 3. Design decisions & assumptions
Numbered, in the joget-req-analyst Q/A/Assumption convention, so unresolved
items are visible at the gate.

## 4. Configuration parameters introduced
| Parameter | Carrier (MD form / env variable / plugin property) | Default |
[Configurable] markers from the FRs land here — every one becomes a real,
named carrier or is explicitly deferred.

## 5. Generation order
1. forms/F-setoffItem → 2. forms/F-refundCase (grid ref) → 3. datalists →
4. workflow → 5. plugin build → 6. userview delta → 7. report
```

### 3.3 FIS authoring rules

1. **Specs are written in the skills' preferred YAML.** A form spec that joget-form-gen can't consume verbatim is not done. Same for datalist/userview/workflow specs.
2. **Acceptance criteria are copied verbatim from the module FR table** into §1, then translated into checks in `tests/acceptance.md`. The module AC is the contract; the test is its executable form (UI check, or SQL against `app_fd_*` per the gam-db-inspect verification pattern).
3. **[Configurable] is not decoration.** Every configurability marker in the FR slice must surface in FIS §4 as a parameter with a named carrier. This is what makes the country-archetype claim real.
4. **Userview specs are deltas.** The building block owns one userview; features amend it. Prevents the N-userviews-per-module drift.
5. **Open questions block the gate, assumptions don't.** Same triage as joget-req-analyst: blockers stop, documented defaults proceed.

**Gate G2 (Spec Done):** traceability table complete both ways; every BR has an enforcement point; every [Configurable] has a carrier; generation order stated; zero blocker questions.

---

## 4. STAGE 3 — GENERATION (per Artefact)

**Input:** A gated FIS folder.
**Executor:** The generation skills, invoked in the FIS §5 order. This stage is mechanical by design — all judgement was spent in Stages 1–2.

| Artefact | Skill | Validation before handoff |
|---|---|---|
| Form JSON | `joget-form-gen` | Importable without manual correction (skill's own contract) |
| Datalist JSON | `joget-datalist-gen` | `scripts/validate_datalist.py` — binder formDefId exists, columns match table |
| Userview JSON | `joget-userview-gen` | `scripts/validate_userview.py` — no menus pointing at missing forms/datalists |
| Workflow XPDL + activity maps | `joget-workflow-gen` | Generated from the CAD state machine; route conditions match guards |
| Plugin JAR | `joget-plugin-dev` | Build per skill (Maven/OSGi); FormDataDao reads/writes match FIS PL spec |
| Jasper JRXML + menu | `joget-jasper-report` | SQL validated against the entity inventory tables |

**Cross-artefact invariants (checked at the end of every generation batch):**
- Every `formDefId` referenced by a datalist, userview menu, or workflow map exists in the batch or in the already-deployed BB.
- Every FK lookup in a form points at an MD form that is generated *earlier* in the order.
- Every workflow tool/participant mapping resolves (the three `packageActivity*Map` blocks are complete).
- Plugin property names match the FIS §4 parameter carriers exactly.

**Gate G3 (Generated):** all artefacts produced, all validation scripts green, invariants pass.

---

## 5. STAGE 4 — DEPLOY, VERIFY, TRACE (per Release Slice)

**Executor:** `joget-instance-setup` (environments), Joget import, then verification.

1. **Environment:** instances registered in `~/.joget/instances.yaml` per the instance-setup skill; one DEV instance per building block stream, one INT instance for cross-module slices.
2. **Import order:** MD form data first (seed CSVs), then forms → datalists → userviews → workflow package → plugin JARs → Jasper menu, per FIS §5.
3. **Acceptance execution:** run `tests/acceptance.md`. Two check classes:
   - **UI/process checks** — the FR's acceptance criterion exercised through the userview.
   - **DB checks** — SQL against `app_fd_*` tables verifying state, postings, and trails (the gam-db-inspect pattern: never trust the screen alone for financial state).
4. **Traceability ledger update** — the one register that spans the whole programme:

```markdown
# TRACE.md (per module, machine-checkable)
| FR | Feature | Artefacts | Tests | Status | Instance |
|---|---|---|---|---|---|
| REF-FR-010 | RFBB-F03 | F-refundCase, PL-SetoffEngine, WF-refundSetoff | T-03.1 ✓ | Accepted | jdx-refunds-dev |
```

**Gate G4 (Accepted):** all slice FRs Accepted in TRACE.md; NFR spot-checks (the module's §6) done for the touched paths; FIS statuses flipped to Deployed/Accepted.

The completed TRACE.md is also your **tender evidence**: "requirement REF-FR-010 is met" is demonstrable as FR → artefact → test → instance, which is precisely the compliance-matrix posture evaluators ask for.

---

## 6. REPOSITORY LAYOUT AND NAMING

```
<bb-id>/                              e.g. rfbb/
├── CAD-RFBB.md
├── TRACE.md
├── features/
│   ├── RFBB-F01-md-catalogue/        (FIS folders, §3.1 layout)
│   ├── RFBB-F02-claim-intake/
│   └── RFBB-F03-setoff-engine/
├── generated/                        Stage-3 outputs, never hand-edited
│   ├── forms/ datalists/ userviews/ workflow/ reports/
│   └── plugins/<plugin-id>/          Maven project per joget-plugin-dev
└── seed/                             MD seed data (CSV per MD form)
```

**Naming conventions** (consistent with the suite and the skills):
- Building blocks: the module's BB ID (RGBB, RPBB, PPBB, RFBB, AMBB, APBB, TSBB; TRBB/ICBB from Module 12).
- Features: `<BB>-Fnn-<slug>`. Forms: camelCase matching table name. MD forms: `mdXxx` + companion `list_mdXxx`. Workflows: `wf<Verb><Noun>`. Plugins: PascalCase plugin ID per joget-plugin-dev.
- Spec files carry the target skill in the prefix: `F-`, `DL-`, `UV-`, `WF-`, `PL-`, `RPT-`.

---

## 7. DX9 COMPATIBILITY GATE

The generation skills are written and validated against **DX 8.x**; the target platform is **DX9**. Until the skills are revalidated, every Stage-3 batch passes a DX9 gate:

1. Import each artefact class into a DX9 sandbox instance once per skill-version; record incompatibilities in a living `DX9-DELTAS.md` (element class names, theme classes — e.g., Dx8TrimedaTheme equivalents — userview property changes, OSGi/Java level for plugins).
2. Where a delta exists, fix it in the **skill** (or a post-processing step), never by hand-editing generated output — hand edits break regeneration.
3. When a skill is confirmed clean on DX9 across all its element types, mark it validated and drop the per-batch check for that skill.

This gate is cheap insurance: one sandbox import per artefact class, and the deltas file converges quickly to empty or to a short mechanical patch list.

---

## 8. EXECUTION PROMPTS (reusable, one per stage)

These assume the working pattern already in use: project knowledge holds the module specs, CAD, and prior FIS folders; the prompt names the slice and the rest is inferred.

**P1 — Architecture session (Stage 1):**
> Create/extend CAD-<BB> for scope card <module>-<slice>. Derive the entity inventory from the module FRs and map each entity to its TA-RDM L2 source, recording divergences. Draw the state machine for every case-bearing entity. Apply joget-req-analyst mapping rules for form/MD/plugin classification; produce the plugin budget in Output-D format and the feature decomposition table. Stop at gate G1 and list anything failing it.

**P2 — Feature specification (Stage 2):**
> Produce the FIS folder for <BB>-Fnn per the workflow §3 templates. Write form/datalist/userview/workflow specs in the exact input YAML of the corresponding skills. Build the two-way traceability table from the module FR rows (verbatim AC), assign an enforcement point to every BR, and a carrier to every [Configurable]. Translate AC into tests/acceptance.md with UI and app_fd_* SQL checks. Flag blockers; default the rest with documented assumptions.

**P3 — Generation batch (Stage 3):**
> Generate all artefacts for <BB>-Fnn from its FIS folder in the FIS §5 order, using the corresponding skills. Run each skill's validation script and the §4 cross-artefact invariants. Output to <bb>/generated/. Report the G3 checklist.

**P4 — Deploy and verify (Stage 4):**
> Deploy <BB>-Fnn to <instance> in the §5 import order, seed MD data, execute tests/acceptance.md (UI + SQL), and update TRACE.md with per-FR status. Report G4 and any NFR spot-check findings.

---

## 9. WORKED MICRO-EXAMPLE (one FR through the pipeline)

**REF-FR-010** — *"The system shall, before any refund, offset the credit against the taxpayer's outstanding liabilities, refunding only the net remaining amount."* (Must)

| Stage | Artefact produced |
|---|---|
| 0 | Scope card 04-S2 (REF-FR-010…014), archetype B |
| 1 | CAD-RFBB: entities `refund_case`, `refund_setoff_item`, `md_setoff_priority`; state OFFSET_APPLIED added to the case state machine; plugin budget gains `SetoffEngine` (ApplicationPlugin; FR-justified: cross-form reads of the 05 ledger — beyond CalculationField); interface contract `05-ledger-read` + `05-posting-write` |
| 2 | FIS RFBB-F03: `F-setoffItem.spec.yml` (child grid form), `WF-refundSetoff.spec.yml` (route guard "net > 0 → continue; net = 0 → set-off-only decision" from the state machine), `PL-SetoffEngine.md` (FormDataDao reads: 05 balances; writes: setoff items + case net), parameter `setoff_priority` carried by `md_setoff_priority`; test T-03.1: create credit 100 with arrears 40 → SQL asserts setoff row 40, case net 60, ledger arrears cleared |
| 3 | Generated form JSON, XPDL + maps, plugin JAR; validators green; invariant check confirms `F-setoffItem` precedes `F-refundCase` grid reference |
| 4 | Imported to jdx-refunds-dev; T-03.1 passes UI + SQL; TRACE.md row REF-FR-010 → Accepted |

One FR, one auditable thread, no interpretation left between stages.

---

*End of Joget Delivery Workflow.*
