# Acceptance — DMBB-F03 (run_t12.py, live jdx9 + ClickHouse; apps cmbb + dmbb)

Prereqs: cmbb redeployed with DebtIdentificationJob; dmbb with cmIdentRun; ClickHouse up + Gold seeded;
DM case type registered (F02). Job reads `sta_v1` via GoldMartClient; creates DM cases via FormDataDao + starts the envelope.

| Test | Assertion |
|---|---|
| **T-12.1 auto-identification creates DM cases** | POST `cmIdentRun` (minAmount small). For ≥2 distinct Gold debtors above threshold, assert a `cmCase` (caseType=DM, origin=SYSTEM) exists per TIN with `c_category` set; `cmIdentRun.createdCount` ≥ 2; each case has a started envelope (a running process / openCase assignment). |
| **T-12.2 consolidation + categorisation + lines** | For one identified TIN (e.g. 100091L, a C6 debtor): exactly **one** DM case; `dmDebt` row (id=case id) with `c_debtcategory` = the Gold `debt_category` and `c_consolidatedamount` = Gold total enforceable; `dmLine` rows = the TIN's `sta_v1.debt_balances` lines (count + amounts match). |
| **T-12.3 dedup (BR-DM-002/004)** | POST a second `cmIdentRun`. No new DM case for an already-identified TIN (`skippedCount` ≥ the prior createdCount); still one DM case per TIN. |
| **T-12.4 lifecycle + history** | Complete `openCase` for an identified case → currentState OPEN, caseRef `^DM-\d{6}$` (guard assigned), CASE_IDENTIFIED + transition events present; `cmChainCheck` VERIFIED. |

Regression: full CMBB run_t02..t09 + DMBB run_t10/run_t11 green (the shared bundle gains one engine + GoldMartClient
methods; the spine/envelope and existing engines are unchanged). Record findings in `docs/DX9-DELTAS.md`.
