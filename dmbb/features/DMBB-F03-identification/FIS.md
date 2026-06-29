# FIS — DMBB-F03 — Debt identification & auto case creation
Status: Accepted (T-12.1..4 4/4 PASS on jdx9 + ClickHouse, 2026-06-12; 75/75 unit; full regression run_t02..t12 green). Closes DMBB slice S1. Built the deferred create-and-start capability (WorkflowManager.processStart from a plugin) — auto-creates consolidated DM cases from Gold.
CAD ref: CAD-DMBB §7 row F03 (DebtIdentificationJob + consolidation + categorisation); DM-FR-001/002, BR-DM-001/002/004.

## 0. Packaging & approach (ADR-001 A1×B1)
**DebtIdentificationJob** is a new engine in the shared `cmbb-plugins` bundle (it orchestrates the CMBB
spine + envelope, so it belongs with the engines, JVM-global). It is fired by a `cmIdentRun` trigger-row
form (the DeadlineEngine/cmSweepRun cron pattern) living in the `dmbb` app. It **reads** `sta_v1` via the
F09 **GoldMartClient** (extended here with `scanDebtors`/`fetchLines`), **consolidates per TIN** (BR-DM-004),
**categorises** with the F01 bands (the Gold `debt_category` is already those bands — A1), **dedups**
(BR-DM-002), and **creates + starts** the DM case (cmCase + dmDebt + dmLine + `cmCaseEnvelope`). This is
the **CaseStarter** capability that F02 deferred here — via `WorkflowManager.processStart(procId, null,
{caseId,assignee}, null, caseId, false)`, the process def resolved from `appService.getWorkflowProcessForApp("cmbb",…,"cmCaseEnvelope")`.

## 1. Traceability
| FR / BR | AC (verbatim from M08 §4.x) | Realised by | Test |
|---|---|---|---|
| DM-FR-001 | "monitor … and identify debt within 24h of due date … create a debt management case" | DebtIdentificationJob scans `sta_v1.debt_priority_queue` (GoldMartClient) and creates a DM case per new debtor; the 24h cadence = the cron POSTing `cmIdentRun` (scheduled at deployment) | T-12.1 |
| DM-FR-002 | "automatic case creation triggered by configurable rules … duplicate case detection prevents duplicate cases for same taxpayer" | minAmount threshold (plugin property, BR-DM-002) + dedup: skip a TIN with an existing non-terminal DM case | T-12.1, T-12.3 |
| BR-DM-001 | debt detection triggers (configurable) | `cmIdentRun.mode=IDENTIFY` + minAmount property; trigger origin stamped `GOLD` on dmDebt | T-12.1 |
| BR-DM-003 (category) | category by consolidated amount | `dmDebt.debtCategory` / `cmCase.category` = Gold `debt_category` (= F01 bands, A1) | T-12.2 |
| BR-DM-004 (consolidation) | "case consolidation rules … one case per TIN" | one DM case per TIN consolidating all `debt_balances` lines into `dmLine`; `cmCase.amountAtStake` = total enforceable | T-12.2 |
| ADR-001 create-and-start | a module case = spine row + subject/child, in the lifecycle | cmCase(DM) + dmDebt(1:1) + dmLine(1:many) + `cmCaseEnvelope` started; CMBB engines run | T-12.1, T-12.2 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| BR-DM-002 dedup + min threshold | DebtIdentificationJob: skip TIN with non-terminal DM case; skip total < minAmount |
| BR-DM-004 consolidation | one DM case per TIN; all tax-type/year debt lines as dmLine children; amountAtStake = Σ enforceable |
| BR-DM-003 categorisation | category copied from Gold (sta_v1 bands == F01 mdDebtCat bands, A1) — no recompute (D-SAD-04) |
| P11 degraded read | GoldMartClient outage → scan returns empty (no cases created), never throws into the run |

## 3. Design decisions & assumptions
1. **A1 — category comes from Gold, not recomputed.** sta_v1's `debt_category` is computed from the same thresholds F01 seeds; the job copies it (snapshot, D-SAD-04). No double truth.
2. **A2 — create via FormDataDao (P3-clean).** The job writes cmCase/dmDebt/dmLine/cmEvent via `dao.saveOrUpdate` — the same Joget data API every engine uses (AllocationEngine writes cmCase.assignee, CaseEventWriter writes cmEvent). Then `processStart` binds the record to the envelope. caseRef + OPEN state are assigned by the existing TransitionGuard at openCase (the job does not duplicate guard logic).
3. **A3 — trigger-row + cron, not PluginWebSupport** (jakarta/javax, DX9-DELTAS F05). `cmIdentRun` (mode IDENTIFY, minAmount, result fields) is POSTed by cron / console; the run row is the audit record (createdCount/skippedCount/detail).
4. **A4 — GoldMartClient extension, reused by all BBs (P7).** `scanDebtors(minAmount)` (debt_priority_queue) + `fetchLines(tin)` (debt_balances) added to the shared lib + JDBC gateway; degraded-read semantics unchanged.
5. **A5 — connection config = plugin properties** (the F09 A4 convention); DEV defaults `jdbc:clickhouse://localhost:8123/sta_v1`, sta_reader.
6. The spike-era empty `app_fd_dmdebt`/`dmline` + the F02 `DM` type are the live carriers; the job upserts onto them.

## 4. Generation order
1. gen_forms.py: F-cmIdentRun → dmbb/generated/forms (postProcessor → DebtIdentificationJob mode IDENTIFY).
2. gen_datalists.py (forms) → list_cmIdentRun.
3. gen_userview.py: ALL dmbb UV-deltas (F01+F02+F03) → dmbbConsole gains an admin "Identification runs" menu.
4. Plugin: extend GoldMartClient (+scanDebtors/fetchLines) + JdbcGoldGateway; new **DebtIdentificationJob** engine + DebtIdentificationService + properties JSON + Activator (9→10); unit tests (consolidation/categorisation/dedup with a fake gateway + a fake process-starter). mvn package.
5. Deploy: cmbb (new plugin JAR) + dmbb (cmIdentRun); restart; no new MD seed. run_t12 + full regression.
