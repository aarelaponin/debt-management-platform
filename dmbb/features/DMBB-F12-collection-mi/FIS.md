# FIS — DMBB-F12 — Collection MI (plans · targets · reports · ②/③ reconciliation) — MODULE CLOSER
Status: Accepted (T-21.1..4 4/4 PASS on jdx9, 2026-06-13; 125/125 unit; full regression run_t02..t20 green). DMBB slice S5 (F12) — CLOSES the DMBB module (F01–F12 complete).
CAD ref: CAD-DMBB §7 row F12 (plans/targets + dashboards + reports + ②/③ reconciliation); DM-FR-047…053, BR-DM-040…043, RPT-FR-001…014 subset. Builds on every prior DMBB feature (the data it reports on) + CMBB-F09 (OutcomeWriteback ③, KPI).

## 0. Approach — the closer: new = plans/targets + MI reports + ②/③ reconciliation; the rest is cross-reference
Of F12's FRs, **five are already delivered** and are recorded here as cross-references (no new build):
DM-FR-049 (extract from ORS/ClickHouse) = CMBB-F09 GoldMartClient; DM-FR-050 (audit logs/timestamps) = the
cmEvent hash chain (CMBB-F02/F09); DM-FR-051 (notifications/alerts) = CMBB-F06 NotificationDispatcher +
DMBB-F04/F07 NOTIF_PENDING/alerts; DM-FR-052 (document upload) = CMBB-F07 (Mayan EDS); DM-FR-053 (case closure)
= CMBB-F08 ClosePhase + DMBB-F09 write-off close. The **new** work, via one engine **CollectionMiEngine** (18th)
/ **CollectionMiService**:
1. **Collection plans + targets (DM-FR-047 / BR-DM-042)** — `dmCollectionPlan` (scope local/national, period,
   status) with a `dmPlanTarget` child grid (target recovery amount per debt category / tax type). **ROLLUP**
   mode (`cmCollectionMiRun`) computes **plan-vs-actual**: actual recovered per target = Σ `dmAction.recoveredAmount`
   + Σ `dmAgent.recoveredTotal` + Σ instalment `dmInstLine.paidAmount` for that category, with attainment %.
2. **Target-setting (BR-DM-043)** — **SUGGEST** mode: suggested target = current debt stock for the category
   (Σ open, non-written-off `dmDebt.consolidatedAmount`) × the configured recovery rate (`mdCollectionParam`),
   the "historical_rate × current_stock" methodology (capacity/seasonal factors recorded as refinements).
3. **MI reports (DM-FR-048 / RPT subset)** — pre-defined JDBC datalists (like F11's list_debtorsList):
   `list_debtByCategory` (RPT-FR-014 tax debt status), `list_debtAging` (RPT-FR-001/008 aging bands),
   `list_recoveryByAction` (RPT-FR-005 collection performance), `list_instalmentCompliance` (RPT-FR-011).
   Ad-hoc query = the datalists' combinable filters; export = native CSV/Excel. Pixel-perfect Jasper dashboards
   (RPT-FR-001/003/005/006 visual tier) recorded as a later RPT/Jasper feature.
4. **②/③ reconciliation (CAD §132)** — **RECONCILE** mode: the writeback-completeness check — every
   financially-relevant DMBB event (② the operational record: WRITEOFF_POSTED, GARNISH_CONFIRMED, AGENT_REPORTED)
   must have its outcome hand-off record (③: the `dmCharge` ledger row / recovery). The run tallies matched vs
   orphaned and flags variances; the live Gold cross-check against `fact_case_outcomes` (CMBB-F09 OutcomeWriteback)
   binds at deployment where the ClickHouse writeback runs (recorded).

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR / BR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-047 / BR-DM-042 | "compilation of local and national debt collection plans, incorporating: current debt stock analysis, resource allocation, target recovery amounts by category … Plan vs. actual tracking." | dmCollectionPlan + dmPlanTarget (per category/tax type) + CollectionMiEngine ROLLUP plan-vs-actual + attainment % | T-21.1 |
| BR-DM-043 | "Recovery targets are set using: historical recovery rates by enforcement action type and category, current debt stock analysis … target = historical_rate × current_stock × capacity_factor × seasonal_adjustment" | CollectionMiEngine SUGGEST: current category stock × mdCollectionParam.recoveryRate (capacity/seasonal = recorded refinements) | T-21.2 |
| DM-FR-048 | "generate management information … pre-defined reports include: total debt by category, debt aging analysis, recovery rate by action type … instalment compliance rate. Ad-hoc query allows filtering by any … attribute. Reports exportable." | MI JDBC datalists (list_debtByCategory / list_debtAging / list_recoveryByAction / list_instalmentCompliance) with combinable filters + native CSV/Excel | T-21.3 |
| CAD §132 (②/③) | "every terminal/financially-relevant event emits; nightly ②-vs-③ reconciliation" | CollectionMiEngine RECONCILE: financially-relevant events (②) vs dmCharge/recovery records (③); matched/orphaned tally + variance | T-21.4 |
| DM-FR-049 | extract debt info from ORS/ClickHouse for analysis/reporting/risk | **ALREADY DELIVERED — CMBB-F09 GoldMartClient (sta_v1 JDBC read, INT-FR-004 cache) + OutcomeWriteback** | (CMBB-F09) |
| DM-FR-050 | comprehensive immutable logs/timestamps of all activities | **ALREADY DELIVERED — cmEvent SHA-256 hash chain (CMBB-F02/F09 ChainVerifyService)** | (CMBB-F02/F09) |
| DM-FR-051 | notify participants of status changes; alert on pending actions | **ALREADY DELIVERED — CMBB-F06 NotificationDispatcher + DMBB-F04/F07 NOTIF_PENDING/AGENT_OVERDUE_ALERT** | (CMBB-F06) |
| DM-FR-052 | upload/attach documents to cases | **ALREADY DELIVERED — CMBB-F07 documents (Mayan EDS, cmDoc)** | (CMBB-F07) |
| DM-FR-053 | clear (close) cases when all debts resolved; archive | **ALREADY DELIVERED — CMBB-F08 ClosePhase + DMBB-F09 write-off close (currentState→CLOSED, history preserved)** | (CMBB-F08/F09) |

## 2. Design decisions
1. **A1 — the closer reuses, it doesn't rebuild.** F12 explicitly maps the 5 already-delivered FRs to their
   features (100% slice coverage) and only builds the genuinely missing pieces — plans/targets, the MI report
   datalists, and the ②/③ reconciliation. This is the module-completion gate, not a re-implementation.
2. **A2 — reports are JDBC datalists (F11 precedent).** Same JdbcDataListBinder pattern as list_debtorsList;
   the "dashboard" visual tier (charts) is a Jasper/HTML front-end concern recorded for the RPT track. Ad-hoc
   query = combinable datalist filters.
3. **A3 — plan-vs-actual aggregates the recorded recoveries.** Actual = the financial outcomes the prior
   features already record (dmAction.recoveredAmount F07, dmAgent.recoveredTotal F07, dmInstLine.paidAmount F06).
   No new financial source — F12 only rolls them up against targets.
4. **A4 — ②/③ = completeness, not balance.** DMBB never holds the authoritative balance (D-SAD-01); the
   reconciliation checks **completeness** (every financial event has its ③ record), the invariant CAD §132 asks
   for. The full DMBB-vs-Gold value reconciliation binds to fact_case_outcomes at deployment (CMBB-F09 writeback).

## 3. Deferred / assumptions (recorded)
- Pixel-perfect dashboards (RPT-FR-001/003/005/006/007 visual tier, traffic-light KPIs, sparklines, drill-down,
  <10s load) — the datalists are the data tier; the Jasper/HTML dashboard render + role-hierarchy (RPT-FR-007)
  is a later RPT feature.
- Live Gold value reconciliation against `fact_case_outcomes` (full ②/③) — binds at deployment where CMBB-F09
  OutcomeWriteback populates ClickHouse; F12 ships the completeness framework + the DMBB-side check.
- Capacity/seasonal factors in target setting (BR-DM-043) — recorded; SUGGEST uses stock × recovery-rate now.
- Resource allocation / quarterly milestones (BR-DM-042) — plan carries the fields; the optimiser is out of scope.

## 4. Configurables → carriers
- Recovery rate per category (BR-DM-043) → `mdCollectionParam`.
- Plan scope/period/targets (DM-FR-047) → `dmCollectionPlan` + `dmPlanTarget`.
- Aging bands (RPT-FR-001) → the list_debtAging SQL CASE bands (admin-configurable refinement recorded).

## 5. Generation order
1. gen_forms: F-dmCollectionPlan (+ dmPlanTarget grid), F-dmPlanTarget, F-cmCollectionMiRun, F-mdCollectionParam → dmbb/generated/forms.
2. gen_datalists (forms) → companions; gen_datalists (custom datalists dir) → the 4 MI JDBC datalists.
3. gen_userview: ALL dmbb UV-deltas (F01..F12) → dmbbConsole gains a **Collection MI** category + config.
4. Plugin: CollectionMiService + CollectionMiEngine (ROLLUP/SUGGEST/RECONCILE) + collectionMiEngine.json; Activator 17→18; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb; seed mdCollectionParam (MLT) via API-dmbb-data. run_t21 + full regression run_t02..t20. **Mark DMBB module COMPLETE.**
