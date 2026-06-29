# Bidder Case-Management Claims — Analysis & Baseline Enrichment

**v1.0 · 11 June 2026 · Source:** `01-preparation/_proposals` — GenTax/FAST (Bidder 2), European Dynamics (Bidder 3, **winner**), Netcompany/SOLON (Bidder 4), GrantThornton/ITAS (Bidder 1).
**Purpose:** test the GCMF/CMBB baseline against four mature products' claims; promote what is generic; record ITCAS-vendor alignment evidence.

## 1. Alignment scorecard (GCMF §2.2 capabilities × bidders)

| GCMF capability | GenTax | ED (vendor) | Netcompany | GT | Our baseline |
|---|---|---|---|---|---|
| Configurable lifecycle + guarded transitions | ✓ multi-stage cases | ✓ BPM designer; states **identical to our envelope** | ✓ Camunda BPMN + Drools | ✓ custom | ✓ CAD §2.3 |
| Allocation (rules/workload/skill/teams) | ✓ auto + bulk | ✓ org-matrix routing, **teams w/ dynamic membership** | ✓ worker profiles (skill+capacity) | ✓ | ✓ F03 (team note added) |
| Deadline/SLA + calendar | ✓ | ✓ **Calendar Service** (holidays/working days) | ✓ | ✓ | ✓ F05 + DPM D11 |
| Queues/worklists + priority | ✓ priority-ordered | ✓ rule-routed worklists, risk-prioritised to-do | ✓ + capacity/load balancing | ✓ | ✓ F04 |
| Decisions & approvals | ✓ | ✓ multi-approval workflows | ✓ | ✓ | ✓ F08 |
| Holds & suppression | ✓ **Stop-Correspondence actively blocks mail** | ✓ (bankruptcy STOPS collection) | ✓ **suppression management** | ✓ | partial → **enriched (E1)** |
| Pending-info / proactive tasks | ✓ **auto work item on missing data** | ✓ | ✓ risk-triggered | — | partial → **enriched (E2)** |
| Documents & correspondence | ✓ **consolidated multi-account notices** | ✓ templates + case docs + merge w/ doc transfer | ✓ approval-workflow correspondence | ✓ | ✓ F06/F07 → **enriched (E3)** |
| Immutable history/audit | ✓ immutable versions | ✓ full trail + historisation | ✓ | ✓ | ✓ cmEvent hash chain |
| Case linkage / consolidation / sub-cases | ✓ | ✓ merge + doc transfer | ✓ **parent-child hierarchies** | ✓ | ✓ case_link → **pattern noted (E4)** |
| Bulk operations | ✓ **bulk error resolution, bulk assignment** | ✓ | ✓ batch processing | — | partial → **enriched (E5)** |
| External case open/close via API | — | ✓ **"receive information from external systems to open cases and send outcomes back"** | ✓ event-driven | ✓ CIP | missing → **added (E6)** |

Debt-side: ED's escalation ladder with statutory wait periods (3-day decision, 2-month seizure window, 3-month notification), bankruptcy-stops-collection, instalment one-active-plan validation, and rule-based debtor identification all confirm DPM D2/D4/D9 and the DMBB scope cards. GT's dual pathways (regular vs enforced) = DPM strategies. Nothing in any proposal contradicts the baseline.

## 2. Enrichments promoted to the generic baseline (CAD-CMBB v1.1)

| # | Enrichment | Source signal | Carrier |
|---|---|---|---|
| E1 | **Suppression as first-class hold scopes**: `CORRESPONDENCE_SUPPRESS` and `ENFORCEMENT_SUPPRESS` join financial holds; NotificationDispatcher and EscalationEngine must check active suppressions before acting | GenTax Stop-Correspondence (active block, not passive list) + Netcompany suppression mgmt | mm_hold_policy scope enum + dispatcher guard |
| E2 | **Task auto-generation rules**: configurable (event/condition → task type, assignee rule, due rule) — generalises the pending-info loop; covers "missing data → work item" | GenTax automatic work items; ED preset-source case initiation | new `mm_task_rule` config-catalogue |
| E3 | **Notice consolidation policy**: per-TIN consolidation across cases/accounts into one notice (already hinted by BR-DM-009 per-tax-type breakdown) | GenTax consolidated multi-account correspondence | mmNotifRule.consolidation_mode |
| E4 | **Sub-case pattern documented**: parent/child via case_link type `parent`, with roll-up display + doc-transfer on consolidation | Netcompany hierarchies; ED merge with documentation transfer | existing case_link + UI pattern note |
| E5 | **Bulk queue actions**: bulk reassign (WF-FR-008 ✓) extended to bulk state actions, guard-checked per row, one event per case | GenTax bulk ops | queue datalist bulk actions + TransitionGuard |
| E6 | **Inbound case API** (open case from external system; outcome callback) — future interface, designed not built | ED explicit claim (their integration pattern at migration) | CAD §4 new contract row I-9 |
| E7 | **Indicator/flag–task coupling** (flag auto-clears when its work item closes) — recorded as a *pattern* on case_task (flag field + auto-clear), not a new entity | GenTax indicator-task coupling | case_task.flag_ref + EventEmitter rule |

Deliberately NOT promoted: GenTax's single-database 360° claim (that's the data product's job, D-SAD-01); Netcompany's Camunda/Drools stack (P3 — we work with Joget's engine); AI prioritisation claims (I-7, SAS lane already reserved).

## 3. Migration-alignment evidence (for the record)

ED's proposal confirms: case states **identical** to our envelope; BPM-configurable case types ≈ our mm metamodel; calendar-aware SLA ≈ DPM D11; rule-based debtor identification ≈ DMBB-S1; instalment approval workflows ≈ DMBB-S3; write-off with evidence ≈ DMBB-S5; "open cases from external systems and send outcomes back" ≈ our I-2/E6 pattern. **The CMBB-on-Joget build is structurally congruent with what ITCAS will deliver** — the migration story (D-SAD-13: durable build, swap-able sources) now has vendor-document evidence behind it. Quote (ED p.186): *"flexibly configurable workflows with multiple approval capacity… case creation process can be configured both manually and automatically."*

*End v1.0.*
