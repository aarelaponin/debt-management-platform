# Legacy Income-Tax DB Review → Mock Gold Design Inputs & Closed-Loop Data Architecture

**v0.1 · 11 June 2026 · Input to:** SAD (A1) and GOLD-MOCK-DESIGN (A2)
**Source reviewed:** `04-db-legacy/` — the live income-dw ETL estate (~45 job folders of Informix SQL + cron shells) and the schema-extraction package (`_request-for-schema/tables_irdnew.xlsx`: full `irdnew` schema, 13,890 columns / ~1,325 tables, with row counts in `Q2.UNL`).

---

## 1. What the legacy estate tells us

### 1.1 Two databases carry the accounting truth

Exactly as the Blueprint's intermediate-model chapter assumes, the accounting picture is a **join across two Informix databases**:

| DB | Role | Key tables (verified) |
|---|---|---|
| **`ars`** (ARS Accounting) | Balances, transaction ledger, receipting | `tpbl`, `tplg`, `atlg`, `lcph`, `lcpd` |
| **`irdnew`** (Income Tax Core) | Registry, statements, refunds, enforcement, penalty/interest params | `taxpayer`, `statement`, `refundsdue`, `refund`, `enforcement`, `penaltyparam`, `adminpenalty`, `objectionref`, `fssarrear` |

### 1.2 The verified core tables (mock-relevant grain and columns)

**`ars:tpbl` — year-by-year taxpayer balance.** Grain: TIN × year of assessment × currency.
`tpcd` (TIN, e.g. `588291M`), `yofa` (year of assessment), `curr`, `amnt` (signed balance), `dudt` (due date — `dudt IS NOT NULL` is the estate-wide filter for *established/collectible*). **This is the natural ancestor of `gold_debt.fact_debt_balances` and STA Level 2/3 rollups.**

**`ars:tplg` — accounting transaction log.** Grain: posting. `tpcd`, `yofa`, `trcd` (transaction code), `sbcd` (sub-code), `amnt`. Codes observed in live business rules: `trcd 20 + sbcd 'G'` = flushed TRIS payments; `trcd 47/49 + sbcd A/B/C/D/X` = receipt adjustments; `trcd 98` = journal; `230/231` = estimated assessments (per survey of tcaust/statement jobs). **Ancestor of the TAS transaction detail and `fact_payment_transactions`.**

**`ars:lcph` + `ars:lcpd` — receipt header/detail.** Join on `ukey`. Header: `rcno` (receipt no), `rcdt` (receipt date), `uscd` (cash-office/user section), `stus` (status; `stus NOT IN (8,9)` = exclude rejected/deleted — used everywhere). Detail: `tpcd`, `ptyp` (payment type), `yerr` (year), `amnt`. **Live `ptyp` code list (verbatim from revenuedw_v2.sql):** `PT, SA, SAPS, ST, CT, CGT, PAYE, SSC, SSC1, SSC2, SSC3, PYSS, TAS, ATLR, PETS, PETT, PTSE, PEET, PEED, PEES, LFE, LFT, OTET, OTTT, ATET` (+ derived `TRSi` for TRIS-routed `ST`). This is the real-world "tax type / payment type" dimension.

**`irdnew` registry & operations:**
- `taxpayer`: `tax_serial` (internal PK), `tax_tpref` CHAR(9) (public TIN), `tax_tpform` (`I`/`C` — individual/company), `tax_firstret/lastret`, `tax_dodeath`, `tax_deletion`, `tax_security`.
- `statement`: `sta_docref`, `sta_traref`, `sta_indicator`, `sta_date`, `sta_detail`, `sta_amount` — statement lines are **batch-produced** by the monthly "refresh statement" process (`process.pro_type='RST'`, controlled via `processcontrol` and `parameter` `SKIPRST` — see REFRESH_STATEMENT/). Statements are *derived*, not authored.
- `refundsdue`: `rdu_taxref`, `rdu_year`, `rdu_amount`, `rdu_ybysetoff`, `rdu_setoff`, `rdu_balance`, `rdu_duedate`, `rdu_paid` — **set-off before refund is already structural** (year-by-year setoff columns).
- `enforcement`: `enf_enftype`, `enf_runno`, `enf_claimno`, `enf_taxref`, `enf_tpref`, `enf_due`, `enf_barcode` — enforcement claims are generated in **runs** with claim numbers and barcoded notices; PT enforcement applies minimum thresholds and an exceptions table (`ptexceptions`).
- `penaltyparam`(+`detail`): per year × tpform: first-period penalty, next-period min/percent, interest-free date, interest period — **interest/penalty regimes are parameter tables, not code** (a ready-made template for our `md*` carriers, P4).
- `adminpenalty`, `objectionref`, `fssarrear` — administrative penalties, objections (hold-relevant), FSS employer arrears.

### 1.3 The operating pattern we are replacing

The whole income-dw estate is cron-driven Informix SQL that lands extracts in a reporting DB (`dbadm1@servrevarc1_tcp`, with a `reportexecution` control table). Batches are tiered (`batch_1..5 low/med/high`, daily/evening/weekly/monthly) — a hand-rolled ancestor of the Blueprint's Hot/Warm/Cold tiering. The ~19 FTE ORS reporting burden lives here. **Implication:** the mock Gold layer should mirror the Blueprint's replacement of this estate, not the estate itself — but its *content* (balances per `tpcd × yofa`, receipts by `ptyp`, enforcement runs) is the authoritative vocabulary.

---

## 2. Consequences for the mock Gold design (A2)

**Design authority note (decided 11 Jun):** the accounting data product is designed **from the Module 05 specification** (STA-FR catalogue, §9.1 conceptual model, §9.2.4 view definitions) — this legacy review supplies *contextual vocabulary and realism* (code lists, grains, formats, row-count ratios), never requirements. Where legacy shape and Module 05 disagree, Module 05 wins.

1. **Seed at "silver-equivalent" grain, derive marts.** The mock seeds synthetic data shaped like the real upstream — `balances (tpcd, tax_type, yofa, amnt, dudt)`, `transactions (tpcd, yofa, trcd, sbcd, amnt, date)`, `receipts (rcno, rcdt, stus; tpcd, ptyp, yerr, amnt)` — and builds `gold_debt.*` and the STA L1–L3 marts from them with ClickHouse SQL (standing in for dbt). This makes the demo credible, keeps mart logic honest (rollups computed, not hand-typed), and rehearses the real migration.
2. **Real code lists.** `ptyp` values as the tax/payment-type dimension; `trcd/sbcd` as transaction-type codes; `stus 8/9` exclusion; `tax_tpform I/C`; TIN format `^\d{1,6}[A-Z]$` (e.g. `588291M`). Synthetic TINs follow the format; no real data leaves the source.
3. **STA levels map cleanly:** L1 = sum over tax types of `tpbl`-style rows (`dudt is not null`); L2 = per tax type; L3 = `tplg`/receipt detail per period with running balance — exactly Blueprint §14.2.3.
4. **Debt ageing** comes from `dudt` → age bands; **collectibility filters** replicate the estate's rules (established = `dudt not null`, exclusions à la `ptexceptions`, minimum-amount thresholds as configurable parameters).
5. **Set-off, objections, instalment-relevant holds** have legacy anchors (`refundsdue.rdu_setoff`, `objectionref`) — the mock exposes a small `open_setoffs/holds` mart so TABB/DMBB can exercise hold logic against data.
6. **Penalty/interest parameters**: model the `penaltyparam` structure as the seed for TABB's `md*` interest/penalty configuration — same shape, our carrier (P4).

---

## 3. The closed-loop data architecture (the SAD §data-model section, drafted)

The debt module is **not only a consumer**: it creates transactional data (cases, actions, instalments, enforcement steps, write-offs) in Joget/PostgreSQL, and that data must return to the data platform. The loop, with the single-writer rule (P9) applied at each hop:

```
   Informix (irdnew, ars, …)  ──ingestion──►  Bronze ──dbt──► Silver ──► GOLD marts
        ▲   (legacy writes continue)                                        │
        │                                                                   │ ① READ (REST gateway:
        │                                                                   │   /debt-priority/queue,
   PowerBuilder est.                                                        ▼   /taxpayer/{tin}/debt-profile)
                                                              ┌──────────────────────────┐
                                                              │  JOGET (CMBB/TABB/DMBB)  │
                                                              │  PostgreSQL app_fd_* =   │
                                                              │  system of record for    │
                                                              │  cases/actions/instal-   │
                                                              │  ments/enforcement/      │
                                                              │  write-off decisions     │
                                                              └────────┬───────────┬─────┘
                  ② WRITEBACK (NRT, narrow): POST outcomes ────────────┘           │
                     → gold_debt.fact_case_outcomes                                │
                     (idempotent: ReplacingMergeTree on case_id+outcome_date)      │
                                                                                   │
                  ③ INGESTION (batch, wide): Joget PostgreSQL becomes a ───────────┘
                     platform SOURCE like any other: app_fd_* → Bronze →
                     stg_joget__* → int_debt__case_history → marts
```

**The three flows, and why both ② and ③ exist:**

- **① Read path — SQL against the published data product (decided 11 Jun).** Joget reads the accounting data product via **JDBC on versioned ClickHouse views** (Module 05 §9.2.3 JDBC pattern; §9.2.4 views `taxpayer_balances`, `payment_history`, `assessment_register`, `taxpayer_360` + the debt marts). Joget-native consumption: `JdbcDataListBinder` for queues/grids, Jasper datasources for statements, `GoldMartClient` (JDBC, read-only role) for plugin logic. The view schema — versioned, with freshness SLOs — is the contract; no REST hop on reads. REST is reserved for writes (②) and computation-wrapping services (e.g. SAS-scored priority queue, when it exists). INT-FR-001's "RESTful endpoints" wording: recorded as a SAD deviation satisfied in substance (defined contract, auth, <2s), spec to be amended. Joget never recomputes a balance (P9); displays carry the view's `as_of` timestamp (P11 stale-beats-none, INT-FR-004).
- **② Outcome writeback — narrow and near-real-time.** Only the *outcome events* the Blueprint contract names (case resolutions, officer actions, state transitions): they feed dashboards and SAS VIYA model-retraining within minutes (§14.2.2). This is an **event interface, not replication** — it carries facts the platform needs *now*, keyed and idempotent.
- **③ Source ingestion — wide and batch.** The Joget PostgreSQL is onboarded as a regular platform source (the same ingestion pipeline that reads Informix reads `app_fd_*`; watermark = Joget's `dateModified` audit column — another reason all writes must go through the Joget API, P3, or the watermark lies). This yields the *complete* analytical history — instalment schedules, escalation paths, case durations — without inflating the writeback API, and it is additive (P3-evolution): new DMBB entities become new staging models, no contract change.

**Single-writer matrix (the rule that keeps the loop sane):**

| Data | Writer | Everyone else |
|---|---|---|
| Taxpayer balances, TAS lines, debt stock, ageing | Data platform (from legacy sources) | Joget reads ①; snapshots are case evidence, never re-published |
| Case/action/instalment/enforcement records | Joget (`app_fd_*` via Joget API only) | Platform reads ②/③; never writes back into Joget tables |
| `fact_case_outcomes` | Writeback plugin ② | Platform treats as append-only fact; ③ may *reconcile* but not rewrite |
| Case evidence documents | Mayan (P15) | Metadata only in marts (never content — Blueprint rule) |

**Reconciliation note (carried to TABB-S3):** flows ② and ③ overlap by design — ③ is also the audit that ② lost nothing. A nightly singular test (dbt-style) compares `fact_case_outcomes` against the ingested case history; discrepancies raise a data-quality incident, mirroring the DQ framework's contracts-vs-monitoring split.

**Mock scope (A2):** implement ① fully (ClickHouse with the Module-05 §9.2.4 product views + debt marts, read-only consumer role — no REST layer needed for reads) and ② fully (writeback REST endpoint + ClickHouse table, idempotency tested). For ③, *design* the staging contract (`stg_joget__case`, `stg_joget__case_event` model stubs reading a Joget dump) and defer the pipeline — it exercises nothing in Joget that ② doesn't, and the real ingestion service is the platform team's asset (P1, P13).

---

## 4. Items for the SAD decision log

1. STA boundary (already flagged) — **this review strengthens the "read, don't rebuild" position**: balances are batch-computed today (RST) and will be platform-computed tomorrow; nothing invites Joget into that business.
2. Tax-type dimension: adopt the `ptyp`-derived list as the seed for `md_tax_type`, normalised (e.g. SSC1/2/3 → SSC with sub-type) — decision on normalisation depth at SAD review.
3. Writeback transport: REST gateway (per principles open-questions table) — confirmed fit; the legacy estate's `reportexecution` control-table pattern suggests adding a simple run-manifest to the mock API for testability.
4. The `_request-for-schema` package (full irdnew schema + row counts) doubles as test-realism input: row-count ratios (e.g. `accesslog` 50M rows vs `taxpayer` ~400k) can size the seed sensibly. The **ars schema is not in the package** — column knowledge comes from query usage; if an ars schema extract exists, request it (low priority; usage-derived columns suffice for a mock).

*End v0.1.*
