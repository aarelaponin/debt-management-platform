# Patch — joget-datalist-gen

Anchored to the current sections (§5 Columns, §6 Filters, §7 Sort, §8 house style, §12 pitfalls,
§13 wrong tool). All additive.

---

## §5 Columns — ADD: every column sortable + cleaned labels + `listColumns` curation

> **Sortable is not optional.** Emit `"sortable":"true"` on **every** column, not just lookup/date
> columns. A list the spec calls "sortable" must back it on all columns. (DMBB UX-QA1: 0/9 columns
> were sortable while the FIS claimed "sortable".)
>
> **Clean column labels** — form-field labels carry authoring annotations that are noise as a grid
> header. Strip `[...]` always, and strip **every** `(...)` parenthetical EXCEPT a small units/codes
> allowlist. Keep `(EUR)`, `(%)`, `(C1-C6)`, `(days)`, `(months)`; drop FK refs `(mdInstrument)`,
> provenance `(set by engine)`, glosses `(debt being enforced)`, enum hints `(SUCCESS/PARTIAL/FAILURE)`.
> ```python
> _KEEP_PAREN = re.compile(r"^(EUR|USD|MDL|GBP|RON|%|C\d(\s*[-–]\s*C\d)?|C1-C6|days|months)$", re.I)
> def clean_label(label):
>     s = re.sub(r"\s*\[[^\]]*\]", "", label or "")
>     s = re.sub(r"\s*\(([^)]*)\)",
>                lambda m: m.group(0) if _KEEP_PAREN.match(m.group(1).strip()) else "", s)
>     return re.sub(r"\s{2,}", " ", s).strip().rstrip(";,").strip()
> ```
>
> **Curate wide companion lists with a `listColumns` form hint.** A form with N fields becomes an
> N-column wall (DMBB had an 18-column list). Accept an ordered `listColumns: [id, ...]` subset on the
> form spec and emit exactly those columns, key column first (`tin` / `caseRef` / `code`). Absent the
> hint, behaviour is unchanged. Target ≤ ~8 columns for a companion list.

## §6 Filters — ADD: typed filters + the one-`requestParam`-per-URL law

> **Emit typed filters, not free text.** Use `SelectBoxDataListFilterType` for enumerable columns
> (status, category, instrument) and the native `TextFieldDataListFilterType` for free text (TIN).
> Form-companion lists should auto-filter on the obvious keys (tin/status/category/instrument/type/agent).
>
> **JDBC filter + drill ride `#requestParam.X#` SQL guards**, written optional so an absent param shows
> all: `('#requestParam.fcat#' = '' OR col = '#requestParam.fcat#')`. A summary→detail drill is a
> `HyperlinkDataListAction` passing `?fcat=<cell value>` to the same param.
>
> **⚠ One requestParam per URL.** A JdbcDataListBinder reliably substitutes only ONE `#requestParam#`
> per request — combining two filters/drills in one URL leaves the second resolving to `''` (its guard
> no-ops). **Design single-param drills**; do not promise AND-ed multi-filter on a JDBC list.

## §7 Sort — ADD (one line)
> Default-sortable is set per-column in §5 (every column). This section governs the default sort order
> only.

## §8 House-style patterns — ADD: drill-down is part of the contract

> **Drill-downs are emitted, not described.** A summary list drills to its detail list via a
> `HyperlinkDataListAction` (`href` = `#request.contextPath#/web/userview/<app>/<uv>/_/<menuId>`,
> `hrefColumn`/`hrefParam`); a detail list drills to the record (`Open case → <caseForm>_crud?id=`).
> If the FIS says "drill-down", the generated list MUST carry the action — verify in the acceptance test.

## §12 Common pitfalls — ADD three

> - **A form-companion datalist has NO standalone `/_/<listId>` URL.** It is reached only through the
>   CrudMenu that owns it, at `/_/<crud customId>` (e.g. `/_/dmAction_crud`). Fetching `/_/list_dmAction`
>   returns a **blank 200 body** (no error, no rows). Only a `DataListMenu` gives a list its own
>   `/_/<listId>` URL. An acceptance test that render-checks a companion list MUST resolve its menu from
>   the deployed `app_userview` json (`menus[].datalistId == listId → customId`), not guess the URL.
> - **Don't over-claim in the spec.** If a column isn't sortable, a filter isn't wired, or no
>   `HyperlinkDataListAction` is emitted, the FIS/TRACE may not say "sortable / filterable / drill-down".
>   The generator and the spec text must agree.
> - **Charting a list ≠ scraping a list.** If a chart needs this list's data, bind a native
>   `SqlChartMenu` to the datalist (`datasource:datalist`) — never fetch the rendered list HTML and parse
>   the table client-side (it couples to column order/labels/markup). See `joget-dashboard-gen`.
