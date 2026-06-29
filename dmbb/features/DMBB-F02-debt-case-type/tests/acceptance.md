# Acceptance — DMBB-F02 (run_t11.py, live jdx9; apps cmbb + dmbb)

Prereqs: `dmbb` redeployed with F02 forms (dmDebt/dmLine/dmCaseConsole); DM `mmCaseType`/`mmState`/
`mmTransition` seeded via `API-cmbb-data`; both API keys bound. Spine + envelope + engines unchanged in `cmbb`.

| Test | Assertion |
|---|---|
| **T-11.1 DM case runs the envelope** | Create a `cmCase` (caseType=DM, origin=MANUAL) via API-cmbb-data; start `cmCaseEnvelope`; complete `openCase`. Assert `app_fd_cmcase.c_casetype='DM'`, `c_currentstate='OPEN'`, `c_caseref ~ '^DM-\d{6}$'` (TransitionGuard used `mmCaseType.DM.idFormat`). |
| **T-11.2 subject + lines linked (ADR-001)** | Post `dmDebt` (id = case id) + two `dmLine` (caseId = case id) via API-dmbb-data. Assert `app_fd_dmdebt` has one row id=case id with `c_debtcategory` set (1:1); `app_fd_dmline` has 2 rows with `c_caseid`=case id (1:many). |
| **T-11.3 history (DM-FR-005)** | Assert cmEvent chain exists for the case (≥1 genesis + transition events) ordered by `c_seq`; a `cmChainCheck` over the case returns `VERIFIED` (CMBB audit engine runs on the DM case unchanged). |
| **T-11.4 dedup (DM-FR-003)** | A second DM case for the same TIN → openCase blocked (`dedupPolicy=SKIP`): `TRANSITION_REJECTED` event / no second OPEN DM case for that TIN. |

Regression: full CMBB run_t02..t09 + DMBB run_t10 green (F02 adds dmbb forms + DM metamodel rows only; the
shared bundle and `cmCase` spine are unchanged). Record findings in `docs/DX9-DELTAS.md`.
