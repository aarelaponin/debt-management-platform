# ADR-002: How DMBB delivers the dashboard / KPI-visualisation tier (RPT-FR-003/006/007)

**Status:** **Accepted (2026-06-14)** — Aare accepted as written: Axis 1 = D3 (server-side SQL via `SqlChartMenu`), Axis 2 = P3 (native `DashboardMenu` host), KPI tiles = K2 (thin tile portlet, thresholds in `mdCollectionParam`). SPIKE-002 to confirm the native path through the generator pipeline, then build the three tiers and retire the CustomHTML scraping interim. Supersedes the two interim spikes built this session (HTML-scrape dashboards), now rejected.
**Date:** 2026-06-14
**Deciders:** Aare (architecture owner); applies to DMBB now and to every BB that needs management dashboards (collection MI, AMBB, APBB, …)
**Relates to:** RPT-FR-003 (KPI Monitoring Dashboard), RPT-FR-006 (Multi-Stream Management Dashboard), RPT-FR-007 (role-based hierarchical views), RPT-FR-018 (export); INT-FR-011 (clean separation of business logic / data access / presentation, *presentation replaced by ITCAS*); the standing DMBB-RPT Jasper tier (printable/tabular reports, already shipped).

## Context

The DMBB module is complete and the Jasper **printable/tabular** report tier (6 reports) ships. What is missing is the **interactive dashboard tier**: KPI tiles with traffic-light status and trend (RPT-FR-003), a multi-stream management overview (RPT-FR-006), and a role-based hierarchy of views (RPT-FR-007). The underlying numbers already exist as four JDBC **MI datalists** (`list_debtByCategory`, `list_debtAging`, `list_recoveryByAction`, `list_instalmentCompliance`) computed by SQL over `app_fd_*` in the profile DB — the same SQL the Jasper reports use.

Two governing inputs constrain *how* we build the presentation:

1. **INT-FR-011 — clean separation, presentation is replaceable.** Business logic, data access, and presentation must be separable so the system migrates component-by-component to ITCAS, where *"the presentation layer [is] replaced by ITCAS UI."* The durable, transferable asset is the **data layer (SQL / marts)**, not the pixels.
2. **The requirements repeatedly mark dashboards as INTERIM.** RPT-FR-003's own note: *"ITCAS MIS module subsumes KPI monitoring. Interim metrics and definitions transfer."* So a heavy, bespoke presentation investment is thrown away at ITCAS go-live; an over-built custom dashboard is negative-value.

**What this session discovered (the trigger for this ADR).** A first attempt rendered dashboards as custom HTML+Chart.js hosted in a Joget `HtmlPage` — which silently strips `<script>`, so nothing rendered. A second attempt hosts the same HTML in a `CustomHTML` form element behind a `FormMenu` — which *does* execute, and the dashboard renders. **But its data plane is the problem:** the client-side JS fetches the server-rendered datalist *page* and screen-scrapes the `<table>`. That couples the dashboard to datalist column order/labels/row markup (which the QA rounds just spent days changing), hardcodes KPI targets in JS, and pushes aggregation to the browser. It works, but it is exactly the "too much customisation on top of Joget" smell — fragile by construction.

**The decisive fact.** Joget DX9 Enterprise already ships the dashboard capability natively: **`SqlChartMenu`** (server-side SQL → Apache ECharts; 9 chart types; binder can be raw `query` OR an existing `datalistId`; `datasource`/`url`/`username`/`password`/`driverClassName` so it can target the profile DB now and be **re-pointed at ClickHouse/ITCAS at go-live**; `showTable`/`showExportLinks`/`showFilter`/`showValue`) and **`DashboardMenu`** (a portlet **grid** that composes other userview menus — including SqlChartMenu — into one page, with per-portlet x/y/width/height). So the real decision is **not** "custom JS vs. custom plugin" — it is **"use the platform's own dashboard plugins vs. build anything bespoke at all."**

## Decision (proposed)

**Axis 1 — Data plane: server-side SQL, never client-side scraping.** Charts read their data through Joget's native server-side path — `SqlChartMenu` bound either to the existing MI **datalist** (`binder: datalist`, reusing the already-tested JDBC SQL) or, where a chart needs a shape no datalist provides, to a raw `query` on the profile datasource. The aggregation lives in **SQL** (the transferable asset). No HTML is scraped; no datalist-markup coupling.

**Axis 2 — Presentation host: platform-native, no bespoke code.** Compose the charts with `DashboardMenu` portlets into one dashboard page per tier. Zero custom Java, zero custom JS, shipped config-as-code through the existing `gen_userview → build_jwa → deploy_jwa` pipeline (a `sqlchart` / `dashboard` emitter added to `gen_userview.py`, exactly as the `jasper` emitter was added). **Reject** both (a) the `CustomHTML`/Chart.js scraping interim and (b) a bespoke `DmbbDashboardMenu` Java plugin.

**KPI traffic-light tiles (the one native gap).** `SqlChartMenu` does charts, not single-number-with-RAG-colour tiles. Fill the gap with the **smallest** possible custom surface: one thin `CustomHTML` portlet whose values come from a **SQL/JSON data source** (an API-Builder list API or a one-row datalist), NOT from scraping — and drive its green/amber/red thresholds + targets from **`mdCollectionParam`** (config-as-code), satisfying RPT-FR-003's "configurable target thresholds." If even that is deemed too much, KPIs degrade to a `SqlChartMenu` table (`showTable`) with conditional colour — fully native. The tile component is bounded, data-clean, and replaceable.

**Role-based hierarchy (RPT-FR-007)** rides on the existing (currently-disabled) `GroupPermission` category gating once the directory-membership recognition issue (DX9-DELTAS 2026-06-14) is fixed — it is orthogonal to *how* a dashboard renders and is tracked separately (task #60). Until then all tiers are visible to all DM staff.

**Why this is the solid ground.** It invests in the **durable** layer (SQL/marts, which transfer to ITCAS), keeps presentation **native and throwaway-friendly** (honouring INT-FR-011 + "ITCAS subsumes"), removes the fragile scraping data plane, adds **no** maintained custom code, and ships through the same proven generator pipeline as every other DMBB artefact.

## Options considered

### Axis 1 — data plane (how a chart gets its numbers)
- **D1 — Client scrapes the rendered datalist HTML** (this session's interim). Reuses datalists with no new SQL, but couples to presentation markup; breaks on any column/label/format change; aggregation in the browser. **Reject** (fragile; violates separation).
- **D2 — Custom JSON API (API Builder / plugin endpoint) feeding custom JS.** Clean data, but reintroduces a bespoke front-end and an auth surface (api-key-in-JS), and a plugin JSON endpoint is barred (`PluginWebSupport` jakarta/javax mismatch, DX9-DELTAS). **Reject for charts** (custom front-end); **keep as the data source for the KPI-tile gap only.**
- **D3 — Native `SqlChartMenu`, server-side SQL, binder = datalist or raw query (recommended).** Aggregation in SQL; datasource re-pointable to ITCAS; zero front-end code. **Recommend.**

### Axis 2 — presentation host (what renders the page)
- **P1 — `CustomHTML` + `FormMenu`, custom HTML/JS** (interim). Works, but bespoke and only as good as its data plane (D1). **Reject as the strategic answer; retire once P3 lands.**
- **P2 — Bespoke `DmbbDashboardMenu` Java plugin** (server-side SQL + config + Chart.js render). Full control and "the Joget way" for a *reusable product component* — but it is heavy presentation code that ITCAS **throws away**, and RPT dashboards are explicitly interim. Over-investment against INT-FR-011. **Reject** (wrong side of the build/replace line).
- **P3 — Native `DashboardMenu` + `SqlChartMenu` portlets (recommended).** No custom code; config-as-code; native export (RPT-FR-018). **Recommend.**
- **P4 — Jasper (already shipped) for printable/tabular.** Complementary, not a dashboard. **Keep** as the print/export tier; dashboards are the interactive complement.

### KPI traffic-light tiles
- **K1 — `SqlChartMenu` table (`showTable`) with conditional colour.** Fully native; tiles look like a coloured row, not big-number cards. Acceptable, plain.
- **K2 — One thin `CustomHTML` tile portlet fed by a SQL/JSON source, thresholds from `mdCollectionParam` (recommended).** Smallest bespoke surface, data-clean, config-driven, matches RPT-FR-003 exactly. **Recommend.**
- **K3 — Defer tiles to ITCAS.** Cheapest; loses the headline RAG KPIs. Fallback only.

## Trade-off analysis

| Criterion (weight) | D1+P1 scrape interim | P2 bespoke plugin | **D3+P3 native (+K2 tiles)** |
|---|---|---|---|
| Data integrity / no presentation coupling | ✗ scrapes markup | ✓ server-side | ✓ server-side SQL |
| Custom code to maintain | medium (JS) | high (Java+JS) | **near-zero** (one tile portlet) |
| Config-as-code via existing pipeline | ✓ | ✓ (+ plugin build) | ✓ (no plugin build) |
| Honours INT-FR-011 (replaceable presentation) | ✗ bespoke | ✗ heavy, discarded at ITCAS | **✓ native, throwaway-friendly** |
| Re-pointable to ClickHouse/ITCAS at go-live | ✗ | ~ (recode) | **✓ datasource swap** |
| Native export (RPT-FR-018) | ✗ build it | ~ build it | **✓ built-in** |
| Effort to first dashboard | low (done) | high | **low–medium** |

## Load-bearing facts — known vs must-verify (a thin spike before committing the tier)

**Known (verified this session):** `SqlChartMenu` + `DashboardMenu` exist in `jw-enterprise-plugins-9.0.7`; `SqlChartMenu` exposes `query`, `binder`/`datalistId`, `datasource`+JDBC creds, 9 chart types, ECharts, `showTable`/`showExportLinks`/`showFilter`; the profile datasource holds `app_fd_*`; the Jasper tier already drives ECharts-equivalent SQL through `datasource:""`. The MI SQL is tested (run_t21).

**Must-verify (SPIKE-002, ~1 menu):** (1) a `SqlChartMenu` bound to an existing `list_*` datalist renders server-side with no custom code; (2) `datasource:""` resolves the profile DB for a raw `query` (as Jasper does); (3) `DashboardMenu` composes ≥2 `SqlChartMenu` portlets into one page in a grid; (4) these ship cleanly through `gen_userview → build_jwa → deploy_jwa` and survive a delete-import-publish. Only after SPIKE-002 passes do we author all charts + the three tier dashboards.

## Consequences

- The scraping interim (`gen_dashboards.py`, `gen_dashboard_forms.py`, the `dashboard`/`formmenu` emitters, `DMBB-DASH` CustomHTML dashboards) is **retired**. The generator scaffolding and the tier/role taxonomy are reused; the data plane and host are replaced. (Keep the files until SPIKE-002 proves the native path, then delete.)
- `gen_userview.py` gains a native **`sqlchart`** emitter and a **`dashboard`** (portlet-grid) emitter; charts reuse the four MI datalist binders.
- KPI thresholds/targets become **`mdCollectionParam`** rows (config-as-code) — no recompile to retune RAG bands (RPT-FR-003 "configurable thresholds").
- ITCAS migration story stays clean: at go-live the SQL/marts transfer and the native presentation is dropped — the project never maintains bespoke dashboard code it will discard.
- 12-month sparklines (RPT-FR-003) need a **time-series** the DEV marts don't yet hold; the native line-chart renders whatever history the mart exposes, and true 12-month trend lands when the DW time-series does (recorded, not faked).

## Action items

1. ~~Aare to accept/adjust this ADR~~ — **DONE 2026-06-14** (accepted: D3 + P3 + K2).
2. ~~SPIKE-002~~ — **DONE/PASSED 2026-06-14**: a native `SqlChartMenu` bound to `list_debtByCategory` renders server-side via ECharts through the gen→build→deploy pipeline; `gen_userview.py` `sqlchart` emitter added. (DashboardMenu 2-portlet composition deferred into step 3.)
3. ~~Extend `gen_userview.py` with `sqlchart`~~ — **DONE**: the **Dashboards** category (landing) carries 4 SqlChartMenu charts over the MI datalists. **STILL OPEN:** `dashboard` (DashboardMenu portlet-grid) emitter to compose the charts into one page per tier; seed KPI targets into `mdCollectionParam`; add the K2 tile portlet.
4. **PARTIAL**: `run_t26` 5/5 PASS (4 charts render server-side, native SqlChartMenu, interim retired, Dashboards is landing) + full regression; **STILL OPEN:** retire the interim files on the host (can't `rm` from the sandbox mount — listed in TRACE), and the 3-tier role split (RPT-FR-007) once the task-#60 GroupPermission fix lands.

**Status of the tier:** the ADR's *core decision is implemented and validated* — a native, server-side, no-custom-code multi-stream dashboard (RPT-FR-006) is live. The one-page-per-tier composition (DashboardMenu), KPI traffic-light tiles (K2), and role gating (RPT-FR-007) are the next, well-scoped increment on this solid base.
