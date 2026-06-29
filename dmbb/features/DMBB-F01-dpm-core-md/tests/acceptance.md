# Acceptance — DMBB-F01 (run_t10.py, live jdx9, app `dmbb`)

Prereqs: `dmbb` app deployed (created on first deploy; delete→import→publish→**restart**),
`API-dmbb-data` key bound (unique per app), MLT seeds loaded via `load_md_seed … API-dmbb-data …`.
Config/seed-only feature — no process, no plugin. Helpers: `sql()` (psql jwdb_mtca, read-only asserts),
`dmbb_post()` (form API on `API-dmbb-data`, body-error check). Per-run ids where a write is exercised.

| Test | Assertion (literal SQL on the deployed `dmbb` tables) |
|---|---|
| **T-10.1 debt categories (DM-FR-004/BR-DM-003)** | `app_fd_mddebtcat` has 6 rows C1–C6; thresholds exact: C1 min 0/max 30, C2 30/100, C3 100/1000, C4 1000/20000, C5 20000/200000, C6 min 200000/max NULL. Matches the `sta_v1` banding (A1) — cross-check `ch("SELECT DISTINCT debt_category FROM sta_v1.debt_balances")` ⊆ {C1..C6}. |
| **T-10.2 instrument catalogue (BR-DM-031/DPM-3)** | `app_fd_mdinstrument`: **14 enabled** (`c_enabled='true'`) + 4 present-disabled (`c_enabled='false'`); spot-check `BANK_GARNISH` minCategory=C3 executionMode=ADMINISTRATIVE authority=SUPERVISOR enabled=true; `PROPERTY_SEIZURE` minCategory=C4; foreign `DEBTORS_DEBTOR_OFFSET` enabled=false. Applicability monotonic: every enabled instrument's `minCategory` ∈ {C2..C5}. |
| **T-10.3 strategy + steps (DPM D2)** | `app_fd_mmstrategy` has `STD-MLT` (segment ALL, categoryFloor C2, active true, version 1); `app_fd_mmescstep` has 5 rows for `STD-MLT` ordered seq 1..5; step 1 Reminder (no instrument), steps 2–3 DEMAND, step 4 BANK_GARNISH, step 5 PROPERTY_SEIZURE; every step's `c_instrument` (where set) exists in `mdInstrument` (referential spot-check). |
| **T-10.4 app isolation / write path** | `dmbb` app present (`SELECT count(*) FROM app_form WHERE appid='dmbb'` = 4); a `dmbb_post('mdDebtCat', …)` upsert returns 200 with empty `errors` (the `API-dmbb-data` key works); CMBB tables untouched (`app_fd_cmcase` column count unchanged). |

Regression after run_t10: `cmbb` `run_t02..t09` all green (DMBB-F01 adds a separate app + its own tables only;
the shared `cmbb-plugins` bundle and `cmCase` spine are untouched, so CMBB regression must be unaffected).
Record any platform finding in `docs/DX9-DELTAS.md`.
