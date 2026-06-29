# PL-OutcomeWriteback — CMBB case-outcome writeback to the Gold mart (CMBB-F09)
Type: DefaultApplicationPlugin (form post-processor, queue-retry). Bundle: cmbb-plugins.
Forcing: INT-FR-002 / I-2 / Blueprint §14.2.2 (idempotent outcomes). CAD §2.2 budget row "OutcomeWriteback". No new XPDL.

## Modes (property `mode`)
| Mode | Trigger form | Action |
|---|---|---|
| SHIP | cmOutcomeRun (create) | Scan CLOSED cases (cmCase.currentState terminal) with no SHIPPED cmOutcome. For each, build the fact (case_id, outcome_date=today, tin, outcome_type=RESOLUTION, outcome_code per A5, amount+detail from GoldMartClient.fetchProfile(tin), officer=assignee); INSERT into ClickHouse mtca_ors.fact_case_outcomes (writeback_api JDBC); write cmOutcome ledger row id=caseId-outcomeCode shipStatus=SHIPPED. ClickHouse unreachable → cmOutcome shipStatus=QUEUED (no throw into the case flow, P11). Tally shipped/queued/failed onto the trigger row. |
| RETRY | cmOutcomeRun (create) | Re-attempt every cmOutcome shipStatus=QUEUED; on success flip to SHIPPED. |

## Idempotency (Blueprint §14.2.2)
- Ledger key cmOutcome.id = `caseId-outcomeCode` → SHIP skips a case already SHIPPED (exactly-once at the Joget side).
- ClickHouse ReplacingMergeTree(received_at) ORDER BY (case_id,outcome_date,outcome_type,outcome_code) dedups duplicates (at-least-once-safe on the CH side). Re-running SHIP after a partial failure cannot create a second logical row; `case_outcomes FINAL` shows one.

## DAO / JDBC contract
- updateSchema(cmOutcomeRun, cmOutcome, cmCase, cmDecision, cmGoldSnapshot) before first read.
- Reads: cmCase, cmDecision (outcome code), sta_v1 via GoldMartClient.
- Writes: cmOutcome ledger (FormDataDao, P3); ClickHouse fact_case_outcomes (JDBC, external — NOT app_fd_, so P3 does not apply); cmOutcomeRun tallies.
- JDBC driver embedded in the bundle (clickhouse-jdbc, Embed-Dependency — webapp packages not OSGi-exported, DX9-DELTAS F07). INSERT uses a parameterised PreparedStatement; connection url/user/pass are plugin properties (A4).

## Services (constructor-injected) — unit-tested on GuardTestHarness
- OutcomeService(dao, ClickHouseGateway, GoldMartClient): shipClosed(actor) / retryQueued(actor). ClickHouseGateway is an interface (insertOutcome(record) throws on failure) so tests inject a fake that records / throws — no live CH needed for unit tests.
- GoldMartClient: see PL-GoldMartClient.
