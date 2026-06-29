# Patch — joget-feature-loop

Anchored to current sections (Stage 3 Generate+build, Stage 4 Deploy+accept, "Conventions that fail
silently when violated", References).

---

## Stage 4 — Deploy + accept — ADD: acceptance must be FUNCTIONAL (the over-claim guard)

> **An acceptance test proves BEHAVIOUR, not artefact existence.** The costliest DMBB QA finding was
> lists the FIS called "filterable / sortable / drill-down" that emitted none of it — the tests passed
> because they only checked the artefact deployed. For every capability the FIS claims, the test must
> exercise it and check the result:
> - "sortable" → the column carries `sortable:true` in the deployed json (and ideally a sorted fetch).
> - "filterable" → `?param=<real>` returns the filtered rows; `?param=<bogus>` empties the list.
> - "drill-down" → the summary cell's `HyperlinkDataListAction` href, followed, yields the detail rows.
> - "renders" → the page returns a `<tbody>` with `<tr>` rows (a blank 200 body is a FAIL, not a pass).
>
> **Resolve the real menu URL.** A form-companion list has no `/_/<listId>` URL — fetch it via its
> CrudMenu customId (resolve from `app_userview` json). `SqlChartMenu` loads its series via AJAX, so
> assert the **bound datalist** returns rows, not inline chart data.

## "Conventions that fail silently" — ADD: order-independent stateful suites

> **A stateful suite must be order-independent.** Two failure modes seen in DMBB:
> 1. **Global-counter coupling.** The round-robin allocator's cursor is `count(cmEvent CASE_ASSIGNED)`;
>    a suite that zeroes the table with raw SQL still races a prior suite's still-running envelope
>    emitting an event after the reset. Fix: `drain()` running process-instances to 0
>    (`app_report_process_instance WHERE finishtime IS NULL`) BEFORE the reset, and assert **relative**
>    outcomes (round-robin *alternation* `{a1,a2}=={officer1,officer2}`) not absolute order tied to the
>    counter's starting parity.
> 2. **External-service env.** `run_t07` 400s unless `MAYAN_USER`/`MAYAN_PASSWORD` are exported (it
>    falls back to admin/admin). The canonical runner sources the live autoadmin pw.
>
> **Use the canonical regression runner** (`scripts/run_regression.sh`): it cold-starts Tomcat for a
> clean JVM (a warm JVM hid a real ClickHouse-client poisoning — see joget-plugin-dev), sources the
> Mayan pw, and runs `run_t02..tNN`. Two consecutive cold-start sweeps = the order-independence proof.

## Stage 4 — REINFORCE the cycle (one line)
> The DEV cycle is **delete → import → publish → RESTART Tomcat → seed**. Userview and reimported API
> definitions are cached and do NOT take effect until the restart (not just seeding — rendering too).

## References — ADD
> - `joget-dashboard-gen` — when a feature ships a dashboard/KPI tier, build it native (SqlChartMenu/
>   DashboardMenu), never custom Chart.js, and record the visualisation decision as an ADR (see ADR-002).
