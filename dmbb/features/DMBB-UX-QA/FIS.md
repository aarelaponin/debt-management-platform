# FIS — DMBB-UX-QA — list usability remediation (sort · filter · drill-down) + orphan removal
Status: Accepted (T-23.1..5 5/5 PASS on jdx9, 2026-06-13; full regression run_t02..t22 green). Cross-cutting QA pass over the datalist/userview tier.
Trigger: user QA review found (1) an orphan "Debt lines" menu, (2) no column sorting, (3) thin/free-text filters, (4) no drill-down — and correctly noted these are standard Joget features. This FIS also **corrects earlier over-stated claims**: several F11/F12 FIS/TRACE rows described lists as "filterable and sortable" / "drill-down to debtor list" before those affordances were actually generated. They are now real and tested.

## 0. What was actually wrong (verified against the deployed JSON, not assumed)
- `list_dmLine` ("Debt lines") was exposed as a standalone DataListMenu, but `dmLine` is a 1:many child of a
  debt case (`caseId` hidden, no taxpayer column) — meaningless on its own; it already renders nested in the
  dmCaseConsole subform/grid.
- `sortable:"true"` was set ONLY on lookup/date columns → `0 of 9` sortable on list_debtorsList, `0 of 4` on the
  MI summaries. Almost nothing sorted.
- Filters existed only where hand-specced, and were free-text (type "C4" exactly). Most lists had none.
- `actions: []` and `rowActions: []` on EVERY datalist → no row opened its record, no summary cell drilled to
  the rows beneath it.

## 1. The fix — generator-driven, deterministic (fix the generator, regenerate; never hand-edit generated)
1. **Sortable everywhere** — `gen_datalists.py` now emits every column with `sortable:"true"` (both
   form-companion and custom lists). Verified 9/9 and 18/18 on the deployed lists.
2. **Typed filters** — the datalist spec/generator gained filter `type`: `select`
   (`SelectBoxDataListFilterType`, inline options) and `text` (`TextFieldDataListFilterType`). Form-row binders
   apply filters natively; JDBC lists read the filter value via `#requestParam.<param>#` in the SQL (the verified
   JDBC mechanism), so a filter dropdown and a drill link feed the same SQL param. Form-companion lists
   auto-get a "contains" filter on the common operational columns (TIN / status / category / instrument / type / agent).
3. **Drill-down** — `gen_datalists.py` gained `rowActions` + per-column `drill` (HyperlinkDataListAction, the
   live-verified shape). Two kinds wired: **summary→detail** (the "Debt by category" Category cell + a row action
   link to `list_debtorsList?fcat=<category>` — click a summary, see exactly the rows beneath it) and
   **detail→record** (the debtors-list "Open case" row action → `dmCaseConsole?id=<case>`). `gen_userview.py`
   gives DataListMenus a `customId` so drill URLs are clean (`/_/list_debtorsList`).
4. **Orphan removed** — the standalone "Debt lines" menu dropped from the F02 UV-delta.

## 2. Traceability
| Concern | Realised by | Test |
|---|---|---|
| Sortable columns on every list | gen_datalists col() always `sortable:"true"` | T-23.1 |
| Typed filters (dropdown + native contains) | filter_entry select/text; form-companion auto-filters (tin/status/…) | T-23.2 |
| Summary→detail + detail→record drill present | rowActions/drill (HyperlinkDataListAction) on debtByCategory + debtorsList | T-23.3 |
| Functional drill filters the detail | debtorsList `#requestParam.fcat#` guard; `?fcat=C5` shows only C5 | T-23.4 |
| Orphan "Debt lines" gone | F02 UV-delta menu removed | T-23.5 |

## 3. Design decisions
1. **A1 — JDBC filter/drill via `#requestParam#`.** The verified JDBC filter mechanism: the SQL guards each
   filter optionally (`('#requestParam.fcat#'='' OR col='#requestParam.fcat#')`); the filter dropdown and any
   inbound drill both set that param. One mechanism for filter + drill. (Test harnesses that run the deployed SQL
   raw must strip `#requestParam.X#` → '' to simulate the no-param default — run_t20 updated.)
2. **A2 — spike before volume.** The whole pattern (sortable + SelectBox + summary-drill + record-drill) was
   validated end-to-end on the debtByCategory→debtorsList pair (functional `?fcat=` filter confirmed) before
   rolling out — same de-risking as the Jasper track.
3. **A3 — honest scope.** Date-range filter plugin (`org.joget.lst.DateRangeFilterType`) is NOT installed on
   jdx9, so date-range filters are deferred (recorded); text/select cover the controlled vocabularies. Drills are
   wired where a real detail target exists (category/record); summaries without a detail target (aging band,
   recovery-by-action) get sortable + select filter, drill deferred.

## 4. Deferred / recorded
- Date-range filters (plugin not installed); numeric-range filters.
- Aging-band → debtors drill (band-label↔range mapping) — sortable + render only for now.
- Per-list bespoke filter sets beyond the auto TIN/status/category/instrument/type/agent (add via custom DL spec when needed).

## 5. Generation
gen_datalists (all forms + custom dirs) → sortable/filters/drill; gen_userview (orphan removed, datalist customIds);
deploy_jwa. No plugin change. run_t23 + full regression run_t02..t22.
