# Acceptance — CMBB-F09 (run_t09.py, live jdx9 + platform-mock ClickHouse)

Prereqs: app cmbb deployed with F09 (delete→import→publish→**restart Tomcat**); plugin JAR with
EventEmitter + OutcomeWriteback hot-deployed; platform-mock ClickHouse up and seeded
(`sta_v1` views resolve, `mtca_ors.fact_case_outcomes` exists); plugin properties point at
`jdbc:clickhouse://localhost:8123` with sta_reader / writeback_api DEV creds. No new mm/md seeds.
Helpers reused from run_t08: `sql()` (psql jwdb_mtca), `form_post()` (body-error check), `Joget`
(CSRF, process start, assignment complete, officer-pool toggle). Add `ch(q)` =
`docker exec mtca-clickhouse clickhouse-client -q "<q>"`. Per-run unique ids (`RUN`).

| Test | Setup | Assertion |
|---|---|---|
| **T-09.1 chain verify GREEN** | Open + drive a TEST case through a few transitions (events accrue). POST cmChainCheck{caseId}. | cmChainCheck.result=VERIFIED, checkedCount≥1; one CHAIN_VERIFIED cmEvent on the case; firstBadSeq blank. |
| **T-09.2 tamper DETECTED** | On a throwaway case, `UPDATE app_fd_cmevent SET c_payload = c_payload||' ' WHERE c_caseid=<case> AND c_seq='0000000002'` (documented fixture exception, §3 A3). POST cmChainCheck{caseId}. | result=BROKEN, firstBadSeq=0000000002 (or the next link whose prevHash no longer matches); one CHAIN_BROKEN cmEvent. Proves tamper-evidence (WF-FR-020). |
| **T-09.3 outcome SHIP (LIVE)** | Open + close a TEST case for a TIN present in the Gold seed (e.g. 100058G). POST cmOutcomeRun{mode:SHIP}. | `ch("SELECT count() FROM sta_v1.case_outcomes WHERE case_id='<case>'")` = 1; cmOutcome row shipStatus=SHIPPED, goldSource=LIVE, amount = the TIN's enforceable balance (non-blank), asOf non-blank; cmOutcomeRun.shippedCount≥1. |
| **T-09.4 SHIP idempotent** | POST cmOutcomeRun{mode:SHIP} again (same closed case). | `ch("SELECT count() FROM sta_v1.case_outcomes WHERE case_id='<case>'")` still = 1 (FINAL dedup); only one cmOutcome row for the key; second run shippedCount for this case = 0 (skipped as already SHIPPED). |
| **T-09.5 GoldMartClient degraded read** | Ensure a cmGoldSnapshot exists for the TIN (run a successful cmGoldProbe first). Then POST cmGoldProbe{tin} with the engine property URL pointed at an unreachable port (or stop CH for this step). | cmGoldProbe.source=CACHE, result=OK, asOf = the stale snapshot's asOf, balance non-blank (served from cache). Case/probe flow does not error (P11 / INT-FR-004). |
| **T-09.6 writeback queue-retry** | With CH writer unreachable, close a case + POST cmOutcomeRun{SHIP}. Restore CH, POST cmOutcomeRun{RETRY}. | After SHIP: cmOutcome shipStatus=QUEUED, cmOutcomeRun.queuedCount≥1, `ch(... case_outcomes ...)`=0. After RETRY: shipStatus=SHIPPED, `ch(...)`=1. |
| **T-09.7 KPI emission** | On a closed case, POST cmChainCheck (EMIT path) or a cmOutcomeRun. | One KPI_EMITTED cmEvent for the case; payload contains caseType, tin, taxType, cycleTimeDays, slaBreached, outcomeCode (assert substrings in c_payload). |
| **T-09.8 SLA compliance datalist** | After the above closed cases exist. Run the **deployed** DL-list_sla_compliance SQL via `sql()` (F04 precedent — assert the SQL the binder runs). | Returns ≥1 row; for TEST: closed_total ≥ shipped count, compliance_pct between 0 and 100, met+breached = closed_total. |

Regression after run_t09: `for t in 02 03 04 05 06 07 08; do run_t$t.py; done` — all green
(F09 adds only trigger-row post-processors and read-only datalists; the F02–F08 envelope is untouched,
so regression must stay 8+7+6+7+7+5+8). Record any import/runtime finding in docs/DX9-DELTAS.md.
