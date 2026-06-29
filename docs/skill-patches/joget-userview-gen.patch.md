# Patch — joget-userview-gen

Anchored to current sections (§4 Categories/menus + decision table, §5 Permissions, §11 pitfalls,
§12 wrong tool).

---

## §4 Menu type decision table — ADD rows

> | The user wants… | Use | Notes |
> |---|---|---|
> | a printable/tabular report | `JasperReportsMenu` | inline jrxml, `language="java"` |
> | an interactive chart | `SqlChartMenu` (enterprise) | server-side SQL→ECharts; bind to a datalist or raw query — see `joget-dashboard-gen` |
> | a multi-chart dashboard page | `DashboardMenu` (enterprise) | portlet grid composing chart menus |
> | a custom HTML/JS widget page | `FormMenu` over a form with a `CustomHTML` element | **NOT** `HtmlPage` (strips scripts) and there is **no** `HtmlMenu` plugin in DX9 |

## §4 — ADD: menu organisation house style

> **Separate day-to-day from batch/admin.** Officer categories hold the records an officer works;
> cron/admin **batch-run / sweep / check trigger** menus belong in ONE admin category (e.g. "Operations
> — batch runs"), not scattered across officer categories. **Retire single-trigger categories** (a
> category whose only menu is one batch trigger) — fold the trigger into Operations. (DMBB UX-QA3.)

## §5 Permissions — REPLACE the section's guidance with the verified mechanics

> **Category gating uses `org.joget.apps.userview.lib.GroupPermission`** with `groupId = <directory
> group id>` (absent/blank = open). Joget renders only the categories the user may see and **lands them
> on the first such category** → category ORDER + gating gives **per-role landing pages**.
>
> **⚠ Two hard prerequisites or gating silently hides everything:**
> 1. **Create the groups + memberships via the Joget directory API, not raw `dir_user_group` INSERTs.**
>    In DMBB, seeded admin membership via raw SQL + a restart was **not recognised** by GroupPermission,
>    so every gated category vanished for admin.
> 2. **The test/demo super-user (`admin`) must be a member of every gated group**, or admin-based render
>    acceptance tests see empty pages and fail. Give per-role acceptance tests **role-user logins** to
>    verify gating actually restricts.
>
> Until both hold, emit OPEN permissions (keep the GroupPermission shape commented for when they do).

## §11 Common pitfalls — ADD

> - **There is no `HtmlMenu` in DX9.** The custom-HTML menu is `HtmlPage` (property `content`), but it
>   is a Quill rich-text page and **does not render `<script>`/`<canvas>`** at runtime — a Chart.js page
>   in HtmlPage shows nothing. For scripted content use `FormMenu` + a `CustomHTML` form element
>   (`value` rendered raw via `${value!}`; leave `requiredSanitize` OFF). For charts, prefer native
>   `SqlChartMenu` (no custom code at all).
> - **A reimported userview is cached until a Tomcat restart** — after delete→import→publish the running
>   app serves the OLD userview until restart. Verify renders only after restarting.
> - **`DataListMenu` customId = the datalist id** gives a list a clean `/_/<listId>` URL; a CrudMenu's
>   list is reached at `/_/<crud customId>`. Keep this in mind when wiring drill `href`s.

## §12 When this skill is the wrong tool — ADD
> For dashboards / KPI tiles / charts, hand off to **`joget-dashboard-gen`** (native SqlChartMenu /
> DashboardMenu). Do not hand-roll Chart.js in a userview menu.
