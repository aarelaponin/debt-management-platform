# FIS — CMBB-F09 — Events completion, KPI emission, Gold reads & Outcome writeback
Status: Accepted (T-09.1..8 8/8 PASS on jdx9 + platform-mock ClickHouse, 2026-06-12; 72/72 unit; full regression run_t02..t09 green: 8+7+6+7+7+5+8+8). Built/deployed/tested on the Mac via Desktop Commander; authored+generated in Cowork.
CAD ref: CAD-CMBB §7 row F09 (EventEmitter completion); plugin-budget rows EventEmitter / GoldMartClient / OutcomeWriteback (no budget growth — all three were budgeted from G1).

## 1. Traceability
F09 closes the last open CMBB requirement (WF-FR-020 *full*) and lands the two SAD integration
connectors (I-1 GoldMartClient, I-2 OutcomeWriteback) that every case-bearing BB depends on (P7).
It also discharges the SLA-compliance-% report deferred from F05 (WF-FR-011 close-out note).
The cmEvent hash chain (append) and zero-padded `seq` hardening already shipped (F02 / F07-pull-forward);
F09 adds the **verification** side that makes the trail *tamper-evident*, plus KPI emission.

| Source (verbatim AC where quoted) | Realised by | Test |
|---|---|---|
| WF-FR-020 — "immutable, tamper-evident audit trail … Entries cannot be modified or deleted by any user including administrators. Audit trail queryable by: date range, user, action type, entity." | cmEvent already append-only + hash-chained (F02). **F09: ChainVerifyService** recomputes SHA-256(payload+prevHash) over each case's chain ordered by `seq`, checks seq contiguity + prevHash linkage + hash recomputation → EventEmitter VERIFY writes VERIFIED/BROKEN + firstBadSeq onto `cmChainCheck`, appends CHAIN_VERIFIED / CHAIN_BROKEN. Query-by-dimension already served by DL-list_cmEvent (date/actor/type/case filters). | T-09.1, T-09.2 |
| CAD §2.2 EventEmitter — "WF-FR-004/020 (immutable history; **KPI emission**)"; GCMF §3.5 "typed events + standard dimensions" | EventEmitter EMIT scans CLOSED cases, derives the standard KPI dimensions (caseType, tin, taxType, cycleTimeDays, slaBreached, outcomeCode) from the chain, appends a typed **KPI_EMITTED** cmEvent per case (consumer = OutcomeWriteback / Module 10, GCMF §3.5) | T-09.7 |
| INT-FR-001 / I-1 — "read taxpayer account data (consolidated balance, tax type balances …) via … endpoints"; INT-FR-004 — "display cached data with a 'data as of [timestamp]' indicator … remains functional with read-only cached data during ORS outage" | **GoldMartClient** (shared lib): JDBC `sta_v1` read (`taxpayer_balances` / `taxpayer_360`) → {enforceableBalance, debtCategory, asaConfidence, asOf}; on outage returns the last `cmGoldSnapshot` row with its stale `asOf` and `source=CACHE` (P11 degraded read). Consumed by OutcomeWriteback (enriches the outcome) and exercised standalone. | T-09.3, T-09.5 |
| INT-FR-002 / I-2 — idempotent case-outcome writeback (Blueprint §14.2.2) | **OutcomeWriteback** (ProcessTool, queue-retry): on `cmOutcomeRun` SHIP it scans CLOSED cases with no SHIPPED `cmOutcome`, builds (case_id, outcome_date, tin, outcome_type, outcome_code, amount, officer, detail), INSERTs into ClickHouse `mtca_ors.fact_case_outcomes` (writeback_api). Idempotent on (case_id,outcome_date,outcome_type,outcome_code) — ReplacingMergeTree dedups and the `cmOutcome` ledger skips already-SHIPPED. ClickHouse unreachable → `cmOutcome` QUEUED + RETRY mode re-ships. | T-09.3, T-09.4, T-09.6 |
| WF-FR-011 (F05 close-out: "compliance % dashboard → F09/RPT, recorded") | **DL-list_sla_compliance** (JdbcDataListBinder): per case type, closed-case count, met vs breached (from cmDeadline terminal clock states), compliance % | T-09.8 |

No orphans: every artefact file below is named by ≥1 row; every F09 requirement above has a test.

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| Audit immutability (M08 §6.6) | No edit/delete UI on cmEvent (F02); ChainVerifyService proves the chain unbroken; a mutated payload changes the recomputed hash → BROKEN at the mutated seq |
| Idempotent outcome writeback (Blueprint §14.2.2) | OutcomeService key = (case_id,outcome_date,outcome_type,outcome_code); cmOutcome ledger = exactly-once shipping; ClickHouse ReplacingMergeTree = at-least-once-safe dedup |
| Degraded reads never block case work (P11 / INT-FR-004) | GoldMartClient try/connect → on failure serve cmGoldSnapshot cache with stale asOf + source=CACHE; OutcomeWriteback still ships (amount/detail from cache), writeback failure queues, does not throw into the case flow |
| KPI events carry standard dimensions (GCMF §3.5) | EventEmitter EMIT fixes the dimension set (caseType, tin, taxType, cycleTimeDays, slaBreached, outcomeCode) |

## 3. Design decisions & assumptions
1. **A1 — three budgeted plugins, no growth.** EventEmitter, GoldMartClient and OutcomeWriteback are CAD §2.2 budget rows from G1 (unlike F08's amendment). F09 builds them: **EventEmitter** and **OutcomeWriteback** are registered `DefaultApplicationPlugin`s (form post-processors, the proven P3-clean trigger); **GoldMartClient** is a service/lib (CAD "shared lib used by BB plugins"), not separately registered — consumed by OutcomeWriteback and exercised through a `cmGoldProbe` trigger handled by EventEmitter (mode PROBE). After F09 every budgeted plugin is built. **No new XPDL** — all three are trigger-row post-processors, not envelope tools, so the F02–F08 envelope and its regression are untouched (chosen over a guardFinal tool precisely to keep regression risk zero; the cmSweepRun/cmDispatchRun pattern, F05/F06).
2. **A2 — verification is on demand / cron, not per-write.** Re-hashing the whole chain on every append is wasteful; instead `cmChainCheck` (POSTable by cron or console) verifies one case (caseId set) or all cases (blank). This mirrors cmSweepRun (F05). VERIFY is read-only over cmEvent plus the trigger-row writeback + two possible cmEvents.
3. **A3 — tamper test uses the documented fixture exception.** Proving "entries cannot be modified" requires *attempting* a modification. T-09.2 issues a raw `UPDATE app_fd_cmevent SET c_payload=…` (the documented read-only-assertion/fixture exception to P3, §5) on a throwaway test case, then shows VERIFY flags BROKEN at that seq. This demonstrates tamper-*evidence*; the platform's immutability (no edit path) is structural (F02).
4. **A4 — ClickHouse connection config = plugin properties.** Reader (`sta_v1`, sta_reader) and writer (`mtca_ors.fact_case_outcomes`, writeback_api) JDBC URL/user/password are EventEmitter/OutcomeWriteback plugin properties (CAD: "gateway endpoints, Mayan token env" → plugin properties). DEV defaults: `jdbc:clickhouse://localhost:8123` (HTTP), sta_reader/sta_reader_dev, writeback_api/writeback_dev. The ClickHouse JDBC driver is **embedded** in the bundle (Embed-Dependency; the openpdf precedent, DX9-DELTAS F07 — webapp-internal packages are not OSGi-exported).
5. **A5 — outcome_type/outcome_code mapping.** A closed case ships `outcome_type=RESOLUTION`, `outcome_code` from the closing decision/outcome (CLOSED default `RESOLVED`; WRITE_OFF / INSTALMENT / PAID resolved from the latest APPROVED cmDecision actionType when present — DMBB will set these). `amount` and `detail` (enforceable balance, debt category, asaConfidence, asOf, source) come from GoldMartClient at ship time. `officer` = case assignee.
6. **A6 — queue-retry, not a REST facade.** SAD §4 names an OutcomeWriteback REST endpoint; GOLD-MOCK §5.4 explicitly says "Joget can write via writeback_api JDBC in DEV; build the REST facade … later". F09 ships the **JDBC writeback** + the queue/ledger semantics (the Blueprint §14.2.2 contract); the REST facade is recorded as deferred (OPEN).
7. **A7 — GoldMartClient cache carrier.** The last good read per TIN is written to `cmGoldSnapshot` (id = tin) via FormDataDao (P3-clean). INT-FR-004 outage path reads it back. Cache validity / staleness banner is surfaced by `asOf` + `source`; a hard TTL is config-deferred (OPEN — default "serve any cache").
8. **A8 — SLA compliance % is read-only.** DL-list_sla_compliance is a pure JdbcDataListBinder over cmCase ⋈ cmDeadline (no plugin); "met" = a non-breached terminal/closed clock, "breached" = cmDeadline.clockState BREACHED ever. Matches the F04 queue-datalist precedent (assert the deployed SQL).
9. No platform blockers anticipated beyond ClickHouse reachability from the JVM; cmChainCheck/cmOutcomeRun/cmOutcome/cmGoldSnapshot are standard trigger/register tables. G2 may pass on spec review.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source |
|---|---|---|---|
| sta_v1 reader JDBC (url/user/pass) | EventEmitter + OutcomeWriteback plugin properties | jdbc:clickhouse://localhost:8123 / sta_reader / sta_reader_dev | I-1, GOLD-MOCK §2.6 |
| fact_case_outcomes writer JDBC (url/user/pass) | OutcomeWriteback plugin properties | jdbc:clickhouse://localhost:8123 / writeback_api / writeback_dev | I-2, 04_users.sql |
| KPI dimension set | EventEmitter (code) | caseType, tin, taxType, cycleTimeDays, slaBreached, outcomeCode | GCMF §3.5 |
| Outcome code mapping (resolution → code) | OutcomeService (code) | CLOSED→RESOLVED; decision actionType override | A5 |
| Gold cache TTL / staleness banner | cmGoldSnapshot.asOf (no hard TTL yet) | serve any cache | INT-FR-004 (A7, OPEN) |

## 5. Generation order
1. gen_forms.py: F-cmChainCheck (EventEmitter VERIFY), F-cmGoldProbe (EventEmitter PROBE), F-cmOutcomeRun (OutcomeWriteback SHIP/RETRY), F-cmOutcome (register/ledger, no postProcessor), F-cmGoldSnapshot (cache, no postProcessor) — trigger forms postProcessor runOn: create.
2. gen_datalists.py: companion list_* for each new form + custom DL-list_sla_compliance (jdbc binder), DL-list_cmOutcome (shipped/queued), DL-list_kpi (cmEvent KPI_EMITTED face).
3. gen_userview.py: ALL UV-delta files in feature order (F01..F09) → cmbbConsole gains an **Audit & KPIs** category.
4. Plugin bundle: EventEmitter + OutcomeWriteback plugins; ChainVerifyService + GoldMartClient + OutcomeService services; properties JSON; Activator registration (7→9); add embedded clickhouse-jdbc dependency to pom; unit tests on GuardTestHarness (ChainVerifyServiceTest, OutcomeServiceTest, GoldMartClientTest with a mock/injected connection). `mvn clean package` — all green; copy JAR.
5. build_jwa.py → deploy_jwa.py (delete→import→publish→**restart Tomcat**) → load_md_seed.py (no new mm/md seeds required; F09 reads existing config + ClickHouse). Ensure platform-mock ClickHouse is up + seeded.
6. tests/run_t09.py (T-09.1..8) + ALWAYS full regression run_t02..run_t09. Update FIS Status + TRACE.md + DX9-DELTAS for any new finding; update SESSION-RESTART-NOTE (next = DMBB).

## 6. Acceptance summary (detail in tests/acceptance.md)
T-09.1 chain VERIFY green · T-09.2 tamper → BROKEN at seq · T-09.3 outcome SHIP → row in fact_case_outcomes, goldSource=LIVE · T-09.4 SHIP idempotent (1 row after re-run) · T-09.5 GoldMartClient degraded read → source=CACHE, stale asOf · T-09.6 writeback queue-retry → QUEUED then SHIPPED · T-09.7 KPI_EMITTED with standard dimensions · T-09.8 DL-list_sla_compliance returns compliance %.
