# GOLD-MOCK-DESIGN — Mock Accounting Data Product (A2)

**v1.1 · 11 June 2026 · Status: BUILT, LOADED, VERIFIED** on `mtca-clickhouse` (24.8).
**v1.1:** intermediate layer added (ARMS `dbt_arms` precedent + Blueprint Ch.8 `int_*` pattern, user decision 11 Jun); views rewired; invariant suite added; SIGTAS learnings recorded (§7).
**Design authority:** Module 05 (§9.1 conceptual model, §9.2.4 views, STA L1–L3); Module 08 §4.1/BR-DM-001…005 for the debt marts; legacy review for grains/codes/realism. Decisions: SAD D-SAD-01/02/06/11/12.
**Code:** `platform-mock/ddl/01_silver.sql · 02_intermediate.sql · 03_sta_v1_views.sql · 04_users.sql · 05_int_rebuild.sql`; seed: `platform-mock/seed/generate_seed.py` (deterministic, seed=2026) + `load_seed.sh` (idempotent).

## 1. Shape

Three layers in one ClickHouse (staging → intermediate → marts, Blueprint Ch.8):

- **`mtca_ors` (silver, legacy-equivalent grain)** — `silver_taxpayer`, `silver_assessments`, `silver_transactions` (tplg-shaped: `trcd/sbcd`, signed amounts), `silver_receipts` (lcph/lcpd-shaped: `stus 8/9` excluded downstream), `silver_objections`, `silver_suspense` — plus **`asa_trcd_map`**: the ASA-as-data table mapping legacy codes → (semantic, PA/IA/PCA component, **confidence**). And `fact_case_outcomes` (writeback landing, ReplacingMergeTree on case_id+outcome_date+type+code, versioned by `received_at`).
- **`mtca_ors` intermediate (materialized tables, rebuilt by `ddl/05_int_rebuild.sql` after every load — the dbt-run stand-in):** `int_accounting__postings` (transactions ⋈ asa_trcd_map, semantics resolved once), `int_accounting__period_balances` (TIN×tax×year, PA/IA/PCA + due date + disputes), `int_taxpayer__master` (golden record à la ARMS: registry + dispute/credit/suspense rollups). The reused joins live here once; the views above are thin.
- **`sta_v1` (the published product)** — 9 views, the contract per SAD §5.1: `taxpayer_360`, `account_transactions` (L3 + running balance), `taxpayer_balances` (L2: debit/credit/balance, PA/IA/PCA, disputed, **enforceable**, per-row `asa_confidence`), `payment_history`, `assessment_register`, `open_credits_suspense`, `debt_balances` (TIN×tax×year, ageing bands, C1–C6), `debt_priority_queue` (risk_score NULL until SAS; `mock_priority` placeholder), `case_outcomes` (deduplicated face of the writeback).

**The views ARE the ASA reference implementation** (D-SAD-11): every legacy→product mapping decision is explicit SQL over `asa_trcd_map`. The mapping table seeds the ASA Semantic Mapping Specification (Phase C1) — VERIFIED rows = grounded in the legacy review; APPROXIMATED rows (estimated assessments, interest-at-statement-run, adjustments, journals) = the decomposition gaps the spec must resolve with MTCA.

## 2. Key semantics

1. **Enforceable = balance − open disputes** (BR-DM-008); disputes from `silver_objections WHERE status='OPEN'`.
2. **C1–C6 banding** (BR-DM-003/DM-FR-004, thresholds 30/100/1k/20k/200k) computed over the **consolidated owed amount per TIN** = `sumIf(enforceable, > 0)` — debts only; credit year-rows are *not* netted (set-off is an STA operation, not a reporting assumption). Verified: every TIN's total sits inside its band.
3. **PA/IA/PCA decomposition** via the trcd map; payments allocated to P (documented simplification, APPROXIMATED). Confidence is surfaced per row — consumers can see what is solid.
4. **Ageing** from `silver_assessments.min(due_date)` per TIN×tax×year; bands 0-29/30-89/90-179/180-364/365+.
5. **`as_of` on every view** (INT-FR-004 staleness display).
6. **Identities:** `sta_reader/sta_reader_dev` (SELECT only — Joget I-1; JDBC `jdbc:clickhouse://localhost:8123/sta_v1`), `writeback_api/writeback_dev` (INSERT on the fact only — I-2).

## 3. Seed profile (synthetic, deterministic — P5/P12)

300 Maltese taxpayers (35% companies), TIN format `\d{6}[MGHABL]`; tax types PIT/CIT/VAT/FSS/SSC; years 2020–2025; ~2.8k assessments, ~7.2k postings, ~2.7k receipts (3% rejected), 23 objections, 14 suspense entries. Debtor mix ≈45% across C1–C6. Regenerate: `python3 seed/generate_seed.py && ./seed/load_seed.sh` (truncates + reloads silver; views are stateless).

## 4. Verification executed (all green, 11 Jun)

| Check | Result |
|---|---|
| Invariant suite (run after every load): postings==txns · no UNMAPPED codes · L2≡L3 every TIN/tax-type · PA+IA+PCA≡balance · category bands valid | ✓ all PASS |
| Band integrity: min/max consolidated total per category within thresholds | ✓ (C1…C6 populated) |
| L2 ≡ L3: `sum(taxpayer_balances.balance)` = `sum(account_transactions.amount)` per TIN | ✓ (2304.93 = 2304.93, TIN 100058G) |
| Single-TIN profile (the GoldMartClient call): balance + PA/IA/PCA + disputed + confidence | ✓ |
| `sta_reader`: SELECT on product ✓ / INSERT denied ✓ | ✓ |
| Writeback idempotency: same (case_id, date, type, code) twice → 1 deduplicated row, latest wins | ✓ (2 raw → 1 FINAL) |

## 5. Known mock-vs-real deltas (carry into ASA spec / real platform)

1. Views are plain (computed per query) — the real platform materialises marts via dbt with tiered refresh; contract identical.
2. `mock_priority` is a placeholder formula; the real ranking arrives with SAS (I-7) — `risk_score` column already present, NULL.
3. Interest rows are synthetic accruals; real interest semantics = the RST statement-run logic, an APPROXIMATED item for the ASA spec.
4. The writeback REST endpoint (small service in front of `fact_case_outcomes`) is not yet built — Joget can write via `writeback_api` JDBC in DEV; build the REST facade with the OutcomeWriteback plugin slice (keeps the Blueprint §14.2.2 contract).
5. ClickHouse views run with invoker rights — reader needs silver SELECT too (encoded in 03_users.sql; the real platform will use definer views or materialised marts).

## 6. ClickHouse gotchas encoded in the DDL (for the next person)

`--` comments are illegal inside `INSERT VALUES`; the 24.8 analyzer requires **explicit aliases** on qualified columns selected in views/CTEs (`t.tin AS tin`), else outer `WHERE tin=` fails; `clickhouse-client --query "INSERT …"` still reads stdin — always `< /dev/null` in scripts.

## 7. The duplication incident (why the intermediate layer paid off on day one)

First int rebuild showed postings = 4× transactions — the `asa_trcd_map` INSERT in `01_silver.sql` ran on every apply, quadrupling the map and silently inflating every join-multiplied aggregate (C6 read €22.4M instead of €4.2M). **The previous view-only design had no row-count seam to catch this.** Fix: `TRUNCATE` before the map INSERT; the postings==txns invariant now guards it permanently.

## 8. SIGTAS cross-check (ta-ref-arch/__Data/_sigtas — COTS precedent)

`COLLECTION_CASE.DEBTOR_CLASS_CODE` ≈ our C-banding (classification-on-case is mature practice); `INST_ASSESS` (amount original/owing/used, due-date-with-grace, per-instalment interest) validates the DMBB instalment entity; `PENALTY`/`INTEREST` as parameter tables with `APPLY_ON_HOLIDAY_FL`/`GRACE_PERIOD`/`TRUNCATION` confirms P4 and enriches DPM D11 (calendar semantics); `OBJECTION` decomposes disputes into `OBJ_TAX/OBJ_PEN/OBJ_INT` — **future refinement: decompose `disputed_amount` by P/I/C** (carry to ASA spec). The folder also holds the **TA-RDM YAML guide + schema + example** (conventions for OPEN-1; full L2 domain files still to be located).

