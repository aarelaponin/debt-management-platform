# CAD — DMBB Debt Management Building Block
Version: 1.0 | Status: Draft (G1 gate report at end) | FR coverage: M08 DM-FR-001…058 (§4.1–§4.11) + RPT-FR-001…014 subset (§4.12) cumulative
Amendment log:

| Date | Slice | Sections touched |
|---|---|---|
| 2026-06-11 | DMBB-S1…S5 (initial, all five scope cards) | all |
| 2026-06-12 | ADR-001 (specialisation model, A1×B1, SPIKE-001 verified) | §2.2: **`dmCase` → `dmDebt`** — a debt case is ONE `cmCase` spine row (CMBB-owned: state, assignment, events, deadlines, holds, decisions, links) extended 1:1 by the DMBB-owned **`dmDebt` subject form** (`subjectId`) + `dmLine` child grid (GCMF §4b). The CMBB engines run on the `cmCase` row unchanged. Likewise `dmInstAgr`/`dmWriteOff`/`dmDefAssess` are `cmCase` rows of their case types + their own subject tables, not parallel case spines. Case-type registration carries `mmCaseType.subjectFormId` (new CMBB carrier). Two-app packaging (B1): `dmbb` owns subject/content/config forms, datalists, userview, engines (shared bundle), seeds; spine + envelope + metamodel stay in `cmbb`. |

## 1. Component Charter

DMBB owns the **debt lifecycle**: identification → reminder → demand → escalation → enforcement → resolution (payment / instalment / write-off), plus debtors-list management and collection MI — implemented as **CMBB case-type specialisations** (GCMF §4): DMBB registers case types, fills extension points (guards, allocation criteria, deadline sources, decision consequences, hold scopes), and owns the Debt Policy Model carriers (DPM D1–D5, D7, D9, D10). It explicitly does **not** own: balance/interest computation (read from `sta_v1`, D-SAD-01/04 — its calculators are informational projections only); accounting postings (write-off and garnishing *decisions* are recorded and written back; postings remain legacy/manual in the interim, SAD I-4); objection *handling* (Appeals domain — DMBB consumes `disputed_amount` from the product, exactly BR-DM-048's "consumed from the STA accounting engine"); risk scores (SAS, D-SAD-07 — `risk_score` arrives via the queue view); case mechanics (CMBB). Dependencies: CMBB S1–S3 deployed; `sta_v1` product (mock live); Mayan (live).

## 2. Data Model

### 2.1 TA-RDM alignment

Same OPEN-1 caveat as CAD-CMBB; mappings to-verify. M08 §9.1 is the immediate source.

| Component entity | TA-RDM L2 source | Divergence + reason |
|---|---|---|
| debt_case | 07-compliance-control.yaml :: collection case (candidate) — TO-VERIFY | CMBB case specialisation; consolidated per TIN (BR-DM-004), category C1–C6 (SIGTAS DEBTOR_CLASS_CODE precedent) |
| instalment_agreement / instalment_line | 06-payment-refund.yaml :: payment arrangement (candidate) — TO-VERIFY | line shape per SIGTAS INST_ASSESS (original/owing/paid, due_with_grace, per-line interest) |
| recovery_action | no L2 source — candidate extension | enforcement action with typed instrument ref, costs, agent |
| write_off | 06-payment-refund.yaml :: write-off txn (candidate) — TO-VERIFY | DMBB holds the *case/decision*; the posting is the accounting side's |
| agent, debtors_list_entry, default_assess_track | no L2 source — candidate extensions | operational registers |
| md_*/mm_* DPM carriers | exempt (configuration) | DPM §2.1 |

TTT spine: `debt_case` carries TIN (always); tax_type/period live on **debt_line** rows (a consolidated case spans multiple TTT) — divergence recorded: spine at TIN grain, TTT at line grain. Country: MLT, FK omitted.

### 2.2 Entity inventory

| Entity | Joget table (app_fd_) | Kind | PK strategy | Parent | Notes |
|---|---|---|---|---|---|
| debt_case | dmCase | main (CMBB case type) | CMBB IdGenerator DM-?????? | — | consolidated per TIN; category; stage; trigger origin (BR-DM-001) |
| debt_line | dmLine | child grid | uuid | debt_case | TIN×tax×yofa **snapshot**: amount, disputed, enforceable, as_of (D-SAD-04 — evidence, never recomputed) |
| instalment_agreement | dmInstAgr | main (CMBB case type) | CMBB IdGenerator IA-?????? | — | product ref (D4), totals incl. projected interest, approval ref, compliance_status |
| instalment_line | dmInstLine | child grid | uuid | instalment_agreement | seq, due_date, due_with_grace, expected, paid, variance, status {pending/paid/late-compliant/missed} (BR-DM-024/025) |
| recovery_action | dmAction | child grid | uuid | debt_case | instrument ref, authorised_by, executed, outcome {pending/successful/unsuccessful/escalated}, costs, legal_fee_decision (BR-DM-010) |
| write_off | dmWriteOff | main (CMBB case type) | CMBB IdGenerator WO-?????? | — | ground ref (D7), evidence refs (BR-DM-039), mode {auto-C1/statutory-bulk/decided} |
| agent | dmAgent | main | uuid | — | warrant officer register (BR-DM-044), fee schedule ref, status |
| agent_report | dmAgentRpt | child grid | uuid | agent | activity reporting interval watchdog (BR-DM-046) |
| debtors_list_entry | dmDebtors | log/event | uuid | — | publication run snapshots (DM-FR-057/058, D10) |
| default_assess_track | dmDefAssess | main (CMBB case type) | CMBB IdGenerator DA-?????? | — | links assessment_register row; estimation method, reasonableness flag, replacement variance (BR-DM-051…053) |
| md_debt_category | mdDebtCat | config-catalogue (D1) | code | — | C1–C6 thresholds (BR-DM-003), consequences mapping (reminders per BR-DM-006, instruments per BR-DM-031) |
| mm_collection_strategy / mm_escalation_step | mmStrategy / mmEscStep | config-catalogue (D2) | code/uuid | — | (segment×category)→ordered steps: trigger days, instrument, grace, guards, template; effective_from/to + version (DPM §2.2 grandfathering) |
| md_enforcement_instrument | mdInstrument | config-catalogue (D3) | code | — | Malta-14 enabled + foreign disabled; legal basis, execution mode, proportionality (BR-DM-031/033), fee schedule (BR-DM-010), suppression sensitivity (E1) |
| md_relief_product / md_eligibility_rule | mdRelief / mdEligRule | config-catalogue (D4) | code/uuid | — | instalment product: C3–C5 (BR-DM-017), single-active (BR-DM-018), min amount (020), auto-approve params (021), duration limits |
| md_violation_policy | mdViolation | config-catalogue (D5) | uuid | — | grace 3bd (BR-DM-025), consecutive-miss cancel (027), rate-reversion triggers (026/028) |
| md_writeoff_ground | mdWoGround | config-catalogue (D7) | code | — | uneconomic-C1 auto (036), statutory expiry bulk (038), uncollectable-decided + evidence classes (039) |
| md_publication_policy | mdPubPolicy | config-catalogue (D10) | uuid | — | thresholds, exclusions, cadence, recipients (DM-FR-058) |
| md_projection_rate | mdProjRate | config-catalogue | code | — | reduced/standard rates for **informational** projection (BR-DM-019/026; authoritative interest stays with accounting) |
| md_agent_fee_schedule | mdAgentFee | config-catalogue | code | — | appointment fee + commission by category (BR-DM-045) |
| md_estimation_method | mdEstMethod | config-catalogue | code | — | prior-year / industry-average / formula + reasonableness multiplier (BR-DM-051/052) |
| (authority rows) | → CMBB mmAuthority data | config rows, not entity | — | — | instalments DMO≤20k/SDO≤100k/Director (BR-DM-022); write-off DMO≤1k/SDO≤20k/Director (BR-DM-037) |

### 2.3 State machines

**Entity: debt_case** (within the CMBB envelope; stages = mm_lifecycle config for case type DM; executed by EscalationEngine + TransitionGuard)
Stages: Identified → Reminder1 → [Reminder2 (C3–C5 only)] → Demand → FinalDemand → Enforcement → terminal {ResolvedPaid, InstalmentCovered, WrittenOff} ∥ overlay states {Suspended-Disputed, Suspended-Insolvency, Suppressed}.

| From | To | Trigger/actor | Guard | Guard realised as |
|---|---|---|---|---|
| (create) | Identified | DebtIdentificationJob / officer (DM-FR-001/003) | BR-DM-001 trigger enabled; BR-DM-002 no duplicate + ≥min threshold; category assigned | DebtIdentificationJob + TransitionGuard |
| Identified | Reminder1 | strategy step timer (BR-DM-005: due+7d) | category ∈ reminder-eligible (BR-DM-006: C2+); no suppression (E1); not excluded (BR-DM-016) | EscalationEngine + DM guard pack |
| Reminder1 | Reminder2 | +14d response expiry | C3–C5 only (BR-DM-006) | EscalationEngine |
| Reminder1/2 | Demand | response expiry (BR-DM-008) | demanded = total − disputed (BR-DM-048/049); high-value bypass straight to Demand (BR-DM-012) | EscalationEngine |
| Demand | FinalDemand | +21d (BR-DM-011) | delivery proof required for enforcement validity | EscalationEngine + cmDoc check |
| FinalDemand | Enforcement | expiry (BR-DM-030) | no active compliant agreement; no full-amount objection; no insolvency | DM guard pack (reads sta_v1 + dmInstAgr + cmHold) |
| any | InstalmentCovered | agreement approved & active | covers case debt | ReliefProductInterpreter → CMBB hold (enforcement stay) |
| any | Suspended-* | objection full / insolvency (BR-DM-016d) | per D8/D9 policy | HoldConnector + TransitionGuard |
| Enforcement | ResolvedPaid / WrittenOff | payment observed in product / write-off posted | balance≤0 at refresh / WO case closed | DebtIdentificationJob refresh + linkage |

**Entity: instalment_agreement** — Applied → UnderReview → DraftDecision ⇄ Negotiation (≤3 rounds, 14d response, expiry on silence — BR-DM-023) → Approved (auto if BR-DM-021; else mmAuthority per BR-DM-022) → Active → {Completed | AtRisk (grace, late-compliant — BR-DM-024/025) | Defaulted → Cancelled (2 consecutive misses / manual / new-debt trigger — BR-DM-027)}. On Cancelled: rate reversion (026/028, informational; authoritative = accounting) + **auto recovery case** with history + enforcement worklist (BR-DM-029) → ReliefProductInterpreter.
**Entity: recovery_action** — Planned → Authorised (mmAuthority; proportionality per BR-DM-031; garnishing eligibility BR-DM-033) → Executed (GarnishingConnector for bank instruments — BR-DM-034) → OutcomeRecorded {successful/unsuccessful/escalated} (+costs, agent ref). Funds posting is NOT here (BR-DM-035 = accounting side; DMBB records outcome + writeback).
**Entity: write_off** — Proposed (evidence complete — BR-DM-039) → UnderApproval (BR-DM-037 levels) → Approved → Recorded ∥ Auto paths: C1 immediate (BR-DM-036), statutory bulk (BR-DM-038) — both still Recorded with ground + event trail.

## 3. Configuration vs Runtime split

| Concern | Realisation | Generator skill |
|---|---|---|
| All §2.2 md_*/mm_* carriers + seeds (MLT profile) | Forms + list_* + CSV seeds | joget-form-gen / joget-datalist-gen |
| Case content forms (debt, instalment, action, write-off, agent) | Forms (CMBB case types) | joget-form-gen |
| Officer queues, drill-downs, MI lists | Datalists (sta_v1 via JdbcDataListBinder where analytical) | joget-datalist-gen |
| DMBB userview (officer + admin console) | Userview | joget-userview-gen |
| Type-specific stages | mm config inside CMBB envelope (NO new XPDL) | seed data |
| Notices, agreements, debtor extracts, MI reports | JasperReports → Mayan | joget-jasper-report |

### Plugin budget (DMBB-specific; all CMBB engines reused, not re-budgeted)

| Plugin ID | Type | Forcing FR | DAO reads | DAO writes | Complexity |
|---|---|---|---|---|---|
| DebtIdentificationJob | ApplicationPlugin (scheduled) | DM-FR-001/002 (24h batch, external read), BR-DM-001…004 | sta_v1 (GoldMartClient), dmCase | dmCase, dmLine, cmEvent | M |
| EscalationEngine | ApplicationPlugin (scheduled) | DM-FR-009…020; BR-DM-005…012/030 (strategy walk = cross-form, timed, batch) | mmStrategy/mmEscStep, mdInstrument, mdDebtCat, cmDeadline, cmHold | dmCase stage, cmTask, notice requests, cmEvent | H |
| ReliefProductInterpreter | ProcessTool + Validator | DM-FR-021…028; BR-DM-017…029 (eligibility, schedule validation, auto-approval, compliance vs payment_history, auto-cancel→recovery case) | mdRelief/mdEligRule/mdViolation, sta_v1.payment_history, dmInstAgr/Line | dmInstAgr/Line, dmCase, cmEvent | H |
| DmGuardProviders | Validator pack (CMBB extension points) | BR-DM-016/048/049 exclusions; doc/state guards | sta_v1, dmInstAgr, cmHold | — | S |
| InterestProjectionCalc | FormLoadBinder | D-SAD-04; BR-DM-019 (informational schedule on agreement) | mdProjRate, dmInstLine | (display only) | S |
| GarnishingConnector | ProcessTool (stub in DEV) | DM-FR-032/BR-DM-034 (bank web services TLS) | dmAction | dmAction outcome, cmEvent | M |
| DebtorsListPublisher | ApplicationPlugin (scheduled) | DM-FR-057/058 (publication runs, recipients) | mdPubPolicy, sta_v1.debt_balances | dmDebtors, export file, cmEvent | S |

Seven plugins, each FR-forced (batch/external/cross-form triggers). Budget rule in force.

## 4. Interface contracts

| Interface | Direction | Mechanism | Contract |
|---|---|---|---|
| Product reads | in | GoldMartClient JDBC (I-1) | debt_priority_queue (identification), debt_balances (lines/refresh), payment_history (instalment matching), assessment_register (default assessments), taxpayer_360 |
| Outcome writeback | out | CMBB OutcomeWriteback (I-2) | events: case opened/stage-change/resolution; action executed+outcome; instalment approved/defaulted/cancelled; write-off recorded — keyed per fact_case_outcomes |
| Enforcement stay / suppression | internal | CMBB HoldConnector + mm_hold_policy (E1/D8/D9) | instalment-active → ENFORCEMENT stay; disputed-full → suspension; insolvency → stop (ED ladder precedent) |
| Bank garnishing | out | GarnishingConnector REST (stub DEV) | BR-DM-034 payload {TIN, name, bank, amount, legal ref, authority ref}; timeout configurable; funds posting NOT ours (BR-DM-035) |
| Debtors list publication | out | file/export + optional endpoint | per mdPubPolicy recipients (DM-FR-058) |
| Agent reporting | in (interim: officer entry) | dmAgentRpt form | BR-DM-046 interval watchdog via mm_task_rule (E2) |

## 5. Userview & persona map

| Persona | Category | Menus | Permission |
|---|---|---|---|
| Debt Management Officer | My work / Debt cases / Instalments / Enforcement | personal worklist (WF-FR-009 def from CMBB), case CRUD, agreement processing, action execution | role dm_officer |
| Team Leader / Supervisor | Team / Approvals | team queues, reallocation, instalment + write-off approvals (mmAuthority), SLA dashboard | role dm_supervisor |
| Policy Administrator | DM administration (DMBB-admin) | DPM consoles D1–D5/D7/D10, strategy designer + dry-run + activation (policy-change case) | role dm_policy_admin |
| Senior Management | MI | dashboards, collection plan vs actual, debtors list runs | role dm_mgmt (read) |
| External Auditor | Audit | case/event/action history (read-only) | auditor |

## 6. NFR realisation notes

| NFR | Design response |
|---|---|
| Identification within 24h of due date (DM-FR-001) | DebtIdentificationJob daily (configurable); mock product is fresh-on-query |
| Officer capacity limits (BR-DM-041) | CMBB AllocationEngine capacity criteria (worker-capacity pattern, Netcompany precedent) |
| Enforcement legal validity | FinalDemand requires delivery proof (cmDoc) before Enforcement transition |
| Snapshot honesty (INT-FR-004) | dmLine rows carry as_of; screens re-read product live; never recompute (D-SAD-04) |
| Writeback completeness | every terminal/financially-relevant event emits; nightly ②-vs-③ reconciliation (F12) |

## 7. Feature decomposition

| Feature ID | Name | FR coverage | Entities | Depends on |
|---|---|---|---|---|
| DMBB-F01-dpm-core-md | DPM carriers + MLT seed (D1/D3 base) | DM-FR-004 (config), BR-DM-003/031 | mdDebtCat, mdInstrument, mmStrategy/mmEscStep schema | CMBB-F01 |
| DMBB-F02-debt-case-type | Debt case type + lines + manual creation + history | DM-FR-003/004/005 | dmCase, dmLine + mm registration | F01, CMBB-F02 |
| DMBB-F03-identification | DebtIdentificationJob + consolidation + categorisation | DM-FR-001/002; BR-DM-001/002/004 | dmCase/dmLine | F02, product |
| DMBB-F04-reminders-demands | EscalationEngine (reminder→final demand) + notices via Mayan | DM-FR-009…014; BR-DM-005…012 | mmStrategy data, cmDoc | F03, CMBB-F05/F06/F07 |
| DMBB-F05-escalation-admin | DMBB-admin strategy console (dry-run, activation-as-case) + enforcement trigger | DM-FR-015…020; BR-DM-030 | mm consoles | F04, CMBB-F08 |
| DMBB-F06-instalments | Agreement case type + ReliefProductInterpreter + negotiation workflow + compliance + auto-cancel→recovery | DM-FR-021…028; BR-DM-017…029 | dmInstAgr/Line, mdRelief/mdViolation, InterestProjectionCalc | F02, CMBB-F08 |
| DMBB-F07-enforcement-actions | recovery_action + instrument execution + GarnishingConnector stub + costs + agents | DM-FR-029…038; BR-DM-031…035/044…046 | dmAction, dmAgent(+Rpt), mdAgentFee | F05, F06 |
| DMBB-F08-enforcement-config | Instrument/template/fee admin console | DM-FR-039…041 | mdInstrument console, templates | F07 |
| DMBB-F09-writeoff | Write-off case type + grounds + auto-C1 + statutory bulk + evidence guard | DM-FR-042…046; BR-DM-036…039 | dmWriteOff, mdWoGround | F02, CMBB-F08 |
| DMBB-F10-default-assess | Default-assessment tracking + estimation refs + replacement | DM-FR-054…056; BR-DM-051…053 | dmDefAssess, mdEstMethod | F02 |
| DMBB-F11-debtors-list | Publisher + policy + extracts | DM-FR-057/058 | dmDebtors, mdPubPolicy | F03 |
| DMBB-F12-collection-mi | Plans/targets + dashboards + reports + ②/③ reconciliation | DM-FR-047…053; BR-DM-040…043; RPT-FR-001…014 subset | datalists/Jasper | F03…F09 |

Slice map: **S1**=F01,F02,F03 · **S2**=F04,F05 · **S3**=F06 · **S4**=F07,F08 · **S5**=F09,F10,F11,F12.

> ✅ **DELIVERY COMPLETE 2026-06-13 — all of S1–S5 (F01–F12) Accepted on jdx9.** 18 shared engines, dmbb = 44 forms/49 datalists/17 UV categories, 125/125 unit + run_t02..t21 full regression green. Realised DMBB engine set (all in the shared cmbb-plugins bundle, ADR-001): DebtIdentificationJob, EscalationEngine, ReliefProductInterpreter, EnforcementActionEngine, EnforcementConfigEngine, WriteOffEngine, DefaultAssessmentEngine, DebtorsListEngine, CollectionMiEngine (+ JogetProcessStarter / GoldMartScanner / GarnishingConnector helpers). See dmbb/TRACE.md for the per-FR ledger.
Completeness: DM-FR-001…058 covered. DM-FR-006/007 (SAS risk score consumption) **partial by design** — categorisation delivered (F02/F03); score column flows through untouched until SAS lands (D-SAD-07, scope card exclusion). RPT-FR-015…021 excluded per SC-DMBB-S5. WF-FR-001…020 are CMBB's (CAD-CMBB).

---

## Gate G1 — Architecture Done

- [x] Entity inventory complete; TA-RDM mappings to-verify (OPEN-1, consistent with CAD-CMBB); DPM carriers exempt-flagged
- [x] State machines: debt_case (staged within CMBB envelope), instalment_agreement, recovery_action, write_off
- [x] Plugin budget: 7 DMBB plugins, each FR-forced; CMBB engines reused without re-budget
- [x] Interface contracts cover product reads, writeback events, holds/suppression, bank stub, publication, agents
- [x] Feature decomposition covers 100% of DM-FR slice (2 partials documented + justified)
- [x] Every [Configurable] has a carrier: the DPM tables (D1–D5/D7/D10) + mmAuthority data rows + plugin properties (bank endpoint, schedules)

**Stage-2 hand-off (FIS-ready, dependency order):** DMBB-F01 → F02 → F03 (slice S1) — after CMBB-F01/F02 are generated and deployed.
