# Patch — joget-deploy

Anchored to "Step 1 — Import order (hard rule)", "Step 2 — Post-import confirmation",
"Re-deployment and regeneration discipline".

---

## Step 1 — Import order — ADD: the restart is part of the cycle

> The full DEV cycle is **delete → import → publish → RESTART Tomcat → seed/verify**. Reimported
> **userview AND API definitions are cached** — the running app keeps serving the OLD ones until a
> restart, so both seeding (API data path) and **userview rendering** are stale until then. `tomcat.sh
> restart` is unreliable here: `pkill -f org.apache.catalina.startup.Bootstrap; sleep 8; ./tomcat.sh
> start`, then poll `…/web/json/workflow/currentUsername` for 400/200 before testing.

## Step 2 — Post-import confirmation — ADD: render-verify the right URL
> When confirming a list/menu rendered, use the **menu's real URL**: a form-companion datalist is at
> `/_/<crud customId>` (NOT `/_/<listId>` — that returns a blank 200). A `SqlChartMenu` page loads its
> data via AJAX, so "rendered" = HTTP 200 + ECharts present + zero error; assert the **bound datalist**
> for data, not the chart page.

## Re-deployment and regeneration discipline — ADD: clean-JVM regression
> For a trustworthy regression sweep, **cold-start Tomcat first** (clean JVM). A warm JVM masked a real
> ClickHouse-client poisoning in DMBB (an engine that passed once then 500'd on the second run in the
> same JVM). The canonical runner (`scripts/run_regression.sh`, `RESTART=1`) cold-starts, sources the
> Mayan credentials, and runs the full suite; two consecutive green cold-start sweeps is the
> order-independence bar.
