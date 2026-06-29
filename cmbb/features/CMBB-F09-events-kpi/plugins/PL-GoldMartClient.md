# PL-GoldMartClient — sta_v1 read client (shared lib) (CMBB-F09)
Type: service / shared library (NOT a separately registered plugin). Bundle: cmbb-plugins.
Forcing: INT-FR-001 (read taxpayer account data), INT-FR-004 (degraded reads), I-1 / D-SAD-02. CAD §2.2 budget row "GoldMartClient" (shared lib used by BB plugins, P7 — built once here so no BB re-implements it).

## Contract
`GoldMartClient(dao, jdbcUrl, user, pass).fetchProfile(tin) -> GoldProfile`
- `GoldProfile { tin, enforceableBalance, debtCategory, asaConfidence, asOf, source }` where source ∈ {LIVE, CACHE}.
- LIVE path: JDBC SELECT against `sta_v1.taxpayer_balances` (SUM enforceable_balance per TIN, worst asa_confidence) + `sta_v1.debt_priority_queue`/`debt_balances` for debt_category + `as_of`. On success, upserts `cmGoldSnapshot` (id=tin) with the values + fetchedAt=now.
- CACHE path (INT-FR-004): any SQLException / connect failure → read back the cmGoldSnapshot row for the TIN, return it with `source=CACHE` and its stored (stale) `asOf`. No snapshot + outage → GoldProfile with source=CACHE, balance null, asOf="" (caller decides; OutcomeWriteback ships amount 0 with a detail note).
- Never throws to the caller for an outage — case work never blocks on the product (P11). A genuine bug (bad SQL) still surfaces in logs.

## Consumed by
- OutcomeWriteback (enriches amount/detail at ship time).
- EventEmitter PROBE (the test/ops affordance).
- DMBB later (BR-DM-001 Gold-driven auto-creation) — same lib, no re-impl.

## DAO / JDBC
- updateSchema(cmGoldSnapshot) before first cache read.
- ClickHouse views run invoker-rights — sta_reader has SELECT on sta_v1 AND mtca_ors (04_users.sql); the DEV creds are A4 defaults. Driver embedded (shared with OutcomeWriteback).
- ClickHouse gotchas (GOLD-MOCK §6): qualified columns need explicit aliases in any CTE we add; we only SELECT from the published views so this is already handled view-side.

## Tested by
- GoldMartClientTest: inject a fake `java.sql.Connection`/ResultSet (or a thin ResultProvider interface) → assert LIVE parse; inject a thrower → assert CACHE fallback reads cmGoldSnapshot. No live ClickHouse in unit tests.
