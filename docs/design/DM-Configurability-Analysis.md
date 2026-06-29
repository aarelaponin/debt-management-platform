# Debt Management Configurability — Cross-Country Analysis & Config Layer Design

**v0.1 · 11 June 2026 · Input to:** CAD-DMBB (Stage 1) and CAD-CMBB metamodel extensions
**Question answered:** what should be abstracted out of the debt activities into a configuration layer, so that (a) one build serves countries with different rules, and (b) one country's rule changes over time never require redeployment.

**Evidence base** (`ta-ref-arch/__10_Processes`): Kyrgyzstan — 8 debt-collection BPMN process models (debtor contact, deferral/instalment, enforced collection, preferential regime, bankruptcy, judicial enforcement, write-off, restructuring) + overpayment offset; Montenegro — TO-BE BPM2 + D5-1 functional specification (collection chapters); Saudi Arabia — TRMS process inventory (collection-relevant rows); Somaliland — ITAS Release-1 scope ("configurable building blocks… arrears"); the TA capability model (`tax-capabilities.docx` Ch. 8, OECD-grounded) as the canonical baseline; Malta Module 08 as the home requirement set.

---

## 1. What varies across countries — the comparative matrix

| # | Variability dimension | Malta (M08) | Kyrgyzstan | Montenegro | Capability model (canonical) |
|---|---|---|---|---|---|
| 1 | **Debt categorisation** | C1–C5 bands (amount-based, BR-DM-001…005) | by debt type + statute age | risk-based prioritisation, case size markers | age × size × collectability × taxpayer risk |
| 2 | **Escalation ladder** | reminder → demand → escalation FSM → enforcement | извещение → решение → требование → предписание → акт → court (45–60 days, multi-instrument) | reminder → demand → 7 enforcement instruments, **"order of garnishing type to be defined in the system"** | reminder (n days after due, "as set in the system") → demand → collection case |
| 3 | **Enforcement instrument set** | 14 action types | + travel ban, cash-register seizure (80%, without notice), property arrest in parallel tracks | 7 instruments incl. licence measures | salary deduction, bank direct debit, seizure & sale, **debtor's-debtor offset**; administrative vs field vs judicial |
| 4 | **Relief products** (instalment/deferral) | instalment agreement (Module 08 §4.5) | **four distinct products**: deferral, instalment, restructuring, preferential regime (50% penalty amnesty) | "reprogram" with broad eligibility | payment agreement; violation ⇒ **immediate cancellation** |
| 5 | **Relief approval authority** | officer/supervisor thresholds | **collegial commission**; bank-guarantee **collateral required** | "under relevant conditions" (configurable) | unspecified — system-routed |
| 6 | **Relief violation policy** | compliance states (at-risk / non-compliant) | cure periods (10 days) | automatic violation monitoring | immediate cancellation |
| 7 | **Interest during relief** | reduced rate on agreement | statutory, unchanged | **unspecified** (flagged gap) | n/a |
| 8 | **Write-off** | auto low-value + decided (DM-FR-030) | **automatic at statute-of-limitations expiry** | grounds + authority levels | bulk + individual, "according to system-set limits" |
| 9 | **Dispute suspensive effect** | hold via case state | appeal windows (30 d) suspend specific instruments | **explicitly unresolved in spec: "objection does (does not?) hold enforcement (check needed!)"** | every step risk-assessed; suspension semantics country-specific |
| 10 | **Insolvency interplay** | out of DM scope | dedicated bankruptcy process; segregated ledger account (КЛС 14th account); collection halts | legal phase engagement | legal service engagement trigger |
| 11 | **Debtor publication** | website / registry, configurable (DM-FR-030/058) | not present | not present | not present (country-optional) |
| 12 | **Segment differentiation** | taxpayer category in C-bands | — | case size markers, risk profiling | **SOE/large-taxpayer separate handling**; strategy per segment |
| 13 | **Collection scope** | tax debt | tax + social contributions | tax | some countries: **all public revenues** (centralised) |
| 14 | **Authority matrix** | supervisor approvals | commission for relief; UGNS head for instruments | per-instrument levels | proportionality model (Table 5) |

Saudi confirms the *component* decomposition rather than rules: collection case lives in case management; instalments (reschedule / late / **revoke**) live in accounting; reminders and write-off are standard capabilities. Somaliland confirms the commercial thesis: arrears delivered as a **configuration-first building block**.

**Two meta-observations:**
1. The capability model itself writes "as set in the system" / "defined within the system" at every numeric or ordering decision — the canonical model is *already* a configuration schema in prose. Montenegro's spec does the same 15+ times. The industry baseline expects exactly the layer we're designing.
2. The deepest variability is **not** parameters but **policy semantics**: the suspensive effect of disputes (#9), relief-product shape (#4–7), and write-off automaticity (#8). These need typed policy objects, not just thresholds.

---

## 2. Design — the Debt Policy Model (DPM)

A declarative configuration layer, owned by DMBB, executed by the CMBB engines plus two DMBB interpreters. Everything below is `md_*`/`mm_*` form data (P4: tables-in-configuration; P8: maintained by authorised officers with approval gates; GCMF §3.2 metamodel extended, not bypassed — P7).

### 2.1 The eleven configuration domains

| # | Domain | Carrier (forms) | Executed by | Covers matrix rows |
|---|---|---|---|---|
| D1 | **Debt categorisation** — bands by amount/age/risk/segment, with category → consequences mapping | `md_debt_category`, `md_category_rule` | category assignment at case creation/refresh (GoldMartClient + rule eval) | 1, 12 |
| D2 | **Collection strategy** — *the centrepiece*: (segment × category) → ordered escalation steps; per step: trigger (days/event), instrument ref, grace days, proportionality guard (min amount), notice template, exit conditions | `mm_collection_strategy`, `mm_escalation_step` | **EscalationEngine** (DMBB plugin) walking the strategy against CMBB DeadlineEngine clocks | 2, 3 |
| D3 | **Instrument catalogue** — enforcement actions as *data, not enum*: Malta's 14 + foreign types (travel ban, register seizure, licence measures, debtor's-debtor offset) present but disabled; per instrument: legal basis, execution mode (administrative/field/judicial), proportionality threshold, authority level, document template, cost recording, dispute-hold sensitivity | `md_enforcement_instrument` | EscalationEngine + case forms | 3, 14 |
| D4 | **Relief products** — instalment/deferral/restructuring/amnesty as *typed products*: eligibility rules (debt range, compliance history), security requirements (none/guarantee/lien), duration limits, down-payment %, instalment frequency set, **interest treatment** (statutory/reduced/none + rate ref), grandfathering | `md_relief_product`, `md_eligibility_rule` | **ReliefProductInterpreter** (DMBB plugin) + CMBB decision service | 4, 7 |
| D5 | **Relief violation policy** — per product: tolerance (n missed / days late), cure period, consequence (warn / suspend / immediate cancel + full reinstatement) | `md_violation_policy` | DeadlineEngine clocks + EscalationEngine | 6 |
| D6 | **Authority matrix** — (action type × amount band) → approval level; body type: single officer / supervisor / **collegial commission** (quorum, members-by-role) | `mm_authority_matrix` | CMBB decision & approval service | 5, 14 |
| D7 | **Write-off policy** — grounds catalogue; per ground: evidence classes required, authority by amount, **mode: automatic (statute expiry / de-minimis) vs decided**, reinstatement rules | `md_writeoff_ground` | CMBB decisions + scheduled auto-write-off job | 8 |
| D8 | **Dispute interaction policy** — per (tax type × instrument): objection effect = {full suspension / no new instruments / continue}; security-for-suspension option; resume rules | `md_dispute_hold_policy` | CMBB **HoldConnector** + TransitionGuards | 9 |
| D9 | **Insolvency policy** — triggers to legal track, instrument freeze set, ledger segregation flag, claim-filing deadlines | `md_insolvency_policy` | case-type transition + holds | 10 |
| D10 | **Publication policy** — debtors-list thresholds, exclusions, cadence, channels/recipients | `md_publication_policy` | scheduled job + Jasper extract | 11 |
| D11 | **Calendars & statutes** — working/calendar-day mode, holiday calendar, statutory windows (appeal, cure), statute of limitations per tax type | `md_legal_calendar`, `md_statute` | CMBB DeadlineEngine (deadline *sources*, GCMF §4 extension point) | 2, 8, 9 |

### 2.2 Temporal validity — rules change over time (the second half of the question)

Every policy record in D1–D11 carries `effective_from` / `effective_to` / `version` / `approved_by`. Two binding modes, configurable **per domain** (this is itself a policy decision that varies):

- **Bind-at-case-creation (grandfathering):** in-flight cases keep their strategy/product version — the GCMF/TARA 10.3 workflow-versioning rule generalised to policy. Default for D2 (strategies) and D4 (relief products — an agreed instalment plan never mutates retroactively).
- **Bind-at-execution:** each step evaluates the policy valid *today*. Default for D6 (authority), D8 (dispute holds), D11 (calendars) — legal effect must follow current law.

Policy changes are themselves audited, approval-gated configuration acts (P8), and `case_event` records the policy version used at every step (P6) — an enforcement action is forever reconstructible against the rule that authorised it.

### 2.3 What is NOT configuration (the exclusion discipline, GCMF §2.3)

The case spine, double-entry of snapshots, the writeback contract, document filing mechanics, and the *engines* themselves are code/config of the fabric — fixed. A country profile selects and parameterises; it never injects logic. If a country's rule cannot be expressed in D1–D11, that is a DPM schema gap to fix in the model (one new column/table), not a BeanShell patch (P3, P7-conformance rule).

### 2.4 DMBB-admin / DMBB-runtime split (the consumption model)

The DPM splits DMBB into two faces, both realised as Joget artefacts from the same pipeline (P3, P13):

- **DMBB-runtime** — officer userviews (queues, cases, instalments, enforcement) + the interpreting engines. Reads policy; never edits it.
- **DMBB-admin** — the policy console over D1–D11: strategy designer, instrument catalogue (enable-flags), relief-product definitions, authority matrix, calendars, profile management — plus the activation controls that make self-service safe: **consistency validation** (referential checks across domains: no step on a disabled instrument, full category coverage, no overlapping effective windows), **dry-run simulation** (which open cases a new policy version would affect), and **approval-gated activation**. A policy change is itself a CMBB case (proposed → reviewed → approved → effective), so policy governance runs on our own fabric and lands in `case_event`.

**The two-tier rule:** business administrators work the *policy space* exclusively in DMBB-admin and never open a Joget Builder; *structural* change (new states in a case FSM, new entities, new engines) stays in the governed delivery pipeline (spec → generate → deploy), because process definitions are deployed artefacts. The cross-country matrix justifies the cut: everything that varies by country or changes over time (§1) sits in the policy space; what is structural is stable across all profiles. Roles are segregated accordingly: policy-author, policy-approver, runtime roles — no overlap between author and approver (GCMF §3.4 segregation).

### 2.5 The gap protocol — when the runtime cannot be configured as needed

**Invariant: a gap is closed by widening the configuration surface, never by special-casing the engine.** The engine gains a *general* capability; the requesting rule becomes a row in it. A fix that embeds the specific rule in code fails gate G1 (P4 tables-in-configuration; OCP).

| Step | Mechanism |
|---|---|
| Detection | **Policy Gap Request** — a CMBB case type (tracked, allocated, SLA'd). Off-system workarounds prohibited. |
| Triage (architect, recorded) | **A** expressible after all → config guidance, close. **B** parameter gap → extend domain schema + interpreter (pipeline release). **C** domain gap → new DPM domain via CAD amendment, then pipeline. **D** structural gap (not policy: new states/entities/process) → full Stage 0–4 slice. |
| Versioning | Two speeds: **DPM model version** (B/C bump it; pipeline-released) vs **policy version** (admin-released in console). |
| Interim handling | Manual procedure, but *visible*: manual decisions recorded on the case with a `policy_gap` reference in `case_event` — auditable now, migratable to config at closure. Production BeanShell hot-patches: never (P3). |
| Closure | Gap case closes only when the rule is expressed *as configuration* and the interim manual cases are flagged for review. TRACE links gap → release. |

This is the DPM's analogue of the GCMF conformance rule ("a third mechanism is a framework gap to fix in the framework, not a workaround in the module") — applied at the policy level.

### 2.6 Country/archetype profiles

A **profile** = one consistent valuation of D1–D11 (+ enabled instrument subset). Three shippable profiles fall out of the analysis: **MLT** (Malta per Module 08 — the build target), **GENERIC-OECD** (capability-model defaults: reminder→demand→case, immediate cancellation, proportionality table), **KGZ-like** (commission approvals, collateralised relief, multi-notice ladder, auto statute write-off — demonstrates range). Profiles are seed-data sets (`seed/profiles/<ISO3>/*.csv`), aligning with the workflow's country-profile convention and the TA-RDM NULL/ISO-3 rule. The DX9 demo story: switch profile, same build, different administration.

---

## 3. Impact on the build

1. **CAD-CMBB:** D6, D8, D11 land as metamodel blocks (authority matrix, hold policy, deadline sources) — they are generic case concerns the GCMF already anticipated; confirm in CAD that their schemas accommodate the DM valuations.
2. **CAD-DMBB:** D1–D5, D7, D9, D10 are DMBB-owned config entities; **EscalationEngine** and **ReliefProductInterpreter** join the plugin budget (2 plugins, FR-justified by DM-FR escalation/instalment ranges; they *read* DPM tables and *drive* CMBB services — they contain no rules themselves).
3. **Module 08 fit:** Malta's FRs already gesture at this (configurable FSM workflows, DM-FR-040 templates, C1–C5 as BR-driven bands, DM-FR-058 configurable publication) — the DPM makes the `[Configurable]` markers systematic instead of ad hoc. The FIS rule "every [Configurable] gets a named carrier" now has its carrier taxonomy.
4. **Mock/demo:** seed the MLT profile; keep one KGZ-like strategy row to prove the engine is data-driven.
5. **Tender posture:** the DPM + profiles is the concrete answer to "does your platform adapt to our legislation?" — demonstrated, not asserted.

## 4. Decisions proposed (to confirm)

| ID | Proposal |
|---|---|
| DPM-1 | Adopt the Debt Policy Model (D1–D11) as the DMBB configurability layer; carriers as §2.1 |
| DPM-2 | Temporal validity + per-domain binding mode as §2.2; policy version recorded in case_event |
| DPM-3 | Instrument catalogue as data with enable-flags; ship Malta-14 enabled, foreign instruments present-disabled |
| DPM-4 | Relief products as typed catalogue (instalment first; deferral/restructuring/amnesty as catalogue rows enabled later) |
| DPM-5 | Profiles as seed-data sets; MLT is the build target, GENERIC-OECD documented, KGZ-like for range demonstration |
| DPM-6 | Gap protocol per §2.5: Policy Gap Request as CMBB case type; A/B/C/D triage; two-speed versioning (model vs policy); visible manual interim; closure only as configuration |

*End v0.1.*
