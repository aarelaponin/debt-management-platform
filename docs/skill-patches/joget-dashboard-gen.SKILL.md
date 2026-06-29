---
name: joget-dashboard-gen
description: >-
  Build the interactive dashboard / KPI-visualisation tier for a Joget DX 8/9 Enterprise app the
  NATIVE way — enterprise SqlChartMenu (server-side SQL → Apache ECharts) and DashboardMenu (portlet
  grid), plus a thin CustomHTML tile for RAG KPI cards. Use WHENEVER the user wants a dashboard, KPI
  monitoring, traffic-light tiles, sparklines, a management/multi-stream overview, charts over their
  data, or mentions RPT-FR-003/006/007 — "build a dashboard", "KPI tiles", "chart this", "management
  overview", "debt-aging dashboard". It exists to STOP the two anti-patterns the DMBB build hit: custom
  Chart.js in an HtmlPage (which strips scripts) and a dashboard that scrapes rendered datalist HTML for
  its data. Charts must read server-side SQL; never hand-roll a charting front-end. Enterprise-only
  (SqlChartMenu/DashboardMenu live in jw-enterprise-plugins; not Community).
---

# Joget DX Dashboard Generation Skill

## 0. The decision (ADR-002) — read before building anything
Dashboards are **native, server-side, no-custom-code**, and treated as **interim/replaceable**
presentation (the data layer is the durable asset). Rationale: the platform already has the
capability, and migration constraints (presentation replaced downstream) make bespoke dashboard code
negative-value. So:

- **Charts** → enterprise **`SqlChartMenu`** (SQL runs server-side; Apache ECharts renders).
- **One page per tier** → **`DashboardMenu`** (portlet grid composing chart menus).
- **RAG KPI tiles** → the smallest custom surface: one **`CustomHTML`** tile strip fed by a SQL/JSON
  source (never scraping), thresholds in a config table (e.g. `mdCollectionParam`). Native fallback: a
  `SqlChartMenu` with `showTable` + conditional colour.
- **Reject:** custom Chart.js in `HtmlPage` (strips `<script>`), and scraping a rendered datalist's
  `<table>` for data (couples to markup; breaks on every column/label change).

Record the visualisation choice as an ADR; route list/report work elsewhere (see §6).

## 1. Charts — `SqlChartMenu`
`className: org.joget.plugin.enterprise.SqlChartMenu`. Data binding (`datasource`):
- `datalist` + `datalistId` — **reuse an existing tested JDBC datalist binder** (preferred: no SQL
  duplication). Add `chartUseAllDataRows:"true"` so it charts every row, not just the current page.
- `default` + `query` — raw SQL on the profile DB (`app_fd_*`), like Jasper's `datasource:""`.
- `custom` + `driverClassName`/`url`/`username`/`password` — an arbitrary JDBC source; **re-pointable
  to ClickHouse / the target DW at go-live** (note this for migration; don't hardcode prod creds).

Axis mapping: `keyName` = category / x column, `value` = numeric / y column. `chartType` ∈
bar/donut/pie/line/area/xy; `library:"echart"` (Apache ECharts). Optional: `title`, `showValue`,
`showLegend`, `horizontal`, `chartHeight`, `showTable` (`top`/`bottom`), `showExportLinks`,
`showFilter` (RPT-FR-018 export comes free with `showTable`).

Minimal portable spec the generator should accept:
```yaml
- type: sqlchart
  customId: chartDebtByCategory
  label: Debt stock by category
  title: Debt stock by category (€)
  datalistId: list_debtByCategory   # datasource: datalist
  keyName: category
  value: total_debt
  chartType: bar
```

## 2. One page per tier — `DashboardMenu`
`className: org.joget.plugin.enterprise.DashboardMenu`, `mode:"plugins"`, `plugins:` a repeater of
portlets, each `{ userviewMenu:<embedded menu element JSON>, x, y, width(1|2|3), height, autoReload,
fixed, resizable }`. Compose by nesting the `SqlChartMenu` definitions as portlets.

> **⚠ SPIKE this first.** The exact serialisation of the embedded `userviewMenu` portlet (nested object
> vs. JSON-encoded string) is the one unknown — confirm by exporting a hand-built DashboardMenu from the
> App Composer, or inspecting `app_userview.json`, then SPIKE a 2-portlet page before generalising. If it
> fights you, a **category of `SqlChartMenu` menus** is a fully-native fallback (charts as separate pages
> rather than one grid).

## 3. RAG KPI tiles (RPT-FR-003)
Big-number cards with green/amber/red vs configurable targets — `SqlChartMenu` doesn't do these.
- **Preferred (K2):** one `CustomHTML` tile-strip portlet. Its values come from a **JSON source** (an
  API-Builder *List* API over a one-row KPI SQL, fetched same-origin with the session cookie) — NOT from
  scraping a datalist page. Targets/thresholds read a config table (`mdCollectionParam`): seed rows like
  `execRateTarget=75`, `activePctTarget=80`, `agedShareTarget=20`. RAG rule: green ≥ target, amber within
  10%, red beyond.
- **Fallback (K1, zero custom code):** a `SqlChartMenu` `showTable:"top"` over a one-row KPI query with
  conditional colour — a coloured row instead of cards.
- `CustomHTML` renders `value` raw (`${value!}`); leave `requiredSanitize` OFF so scripts survive. There
  is **no `HtmlMenu`** in DX9 and `HtmlPage` strips scripts — host scripted HTML only via `FormMenu` +
  `CustomHTML`.

## 4. Generation (config-as-code)
Emit through the normal `gen_userview → build_jwa → deploy_jwa` pipeline, exactly like Jasper. Add a
`sqlchart` emitter (and a `dashboard` portlet emitter) to `gen_userview.py`; charts reference the MI
datalists by id. Never hand-edit generated json (P5).

## 5. Acceptance (what "renders" means here)
- A chart page = HTTP 200 + ECharts present + **zero** error (`exception|HTTP Status 500|SQLException`).
- **`SqlChartMenu` loads its series via AJAX** — the category data is NOT in the initial page HTML.
  Assert the **bound datalist** returns rows (the chart can only chart what the binder returns; the
  binder SQL is tested separately), not inline chart values.
- A reimported userview is cached until a **Tomcat restart** — render-verify only after restarting.

## 6. When this skill is the wrong tool
- **Printable / tabular / PDF-Excel report** → `joget-jasper-report` (pixel-laid-out, not a dashboard).
- **A plain list / grid / filterable table** → `joget-datalist-gen`.
- **The page is custom HTML/JS that is NOT a chart** → `joget-userview-gen` (FormMenu + CustomHTML).
- **Community edition** (no enterprise plugins) → SqlChartMenu/DashboardMenu are unavailable; fall back
  to Jasper + datalists, or a CustomHTML page.
