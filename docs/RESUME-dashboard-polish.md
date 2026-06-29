# Resume note — DMBB dashboard polish (after a short pause)

**Written 2026-06-14.** Read this, then `docs/design/ADR-002-dashboard-visualization-tier.md` (the binding decision) and the DMBB-DASH rows in `dmbb/TRACE.md`. Everything else in `docs/SESSION-RESTART-NOTE.md` is still current.

---

## 0. Where things stand (don't re-derive)

The **interactive dashboard tier is built the solid, native way** per ADR-002 (Accepted) and is **live + green**:

- Dashboards = native enterprise **`SqlChartMenu`** (server-side SQL → Apache ECharts), bound to the tested MI datalist binders. **No custom HTML/JS, no scraping, no bespoke plugin.**
- The **Dashboards** category (console landing) carries 4 charts: `chartDebtByCategory`, `chartDebtAging`, `chartRecoveryByAction` (bar) + `chartInstalmentCompliance` (donut), over `list_debtByCategory` / `list_debtAging` / `list_recoveryByAction` / `list_instalmentCompliance` with `datasource:datalist` + `keyName`/`value` + `chartUseAllDataRows:true`.
- `scripts/gen_userview.py` has a working **`sqlchart`** emitter. Spec: `dmbb/features/DMBB-DASH/userview/UV-delta.spec.yml`. Test: `dmbb/features/DMBB-DASH/tests/run_t26.py`.
- **run_t26 5/5** and **full regression `run_t02..t26` = 25/25 green.**

The two earlier custom-Chart.js attempts (HtmlPage strips scripts; CustomHTML/FormMenu renders but *scrapes* datalist HTML) are **rejected and retired** in the userview. Their files still sit on disk (see Cleanup) — the sandbox couldn't `rm` from the mount.

---

## 0b. Skills are now a git repo — single source of truth (2026-06-14)

The Joget Claude/Cowork dev-lifecycle skills are no longer scattered. They live in **one git-backed repo**:

- **Local:** `/Users/aarelaponin/IdeaProjects/rsr/joget-claude-skills` · **Remote:** https://github.com/aarelaponin/joget-claude-skills (SSH, pushed).
- 13 `joget-*` skills under `skills/<name>/SKILL.md` (+ their `references/`), each the **latest** version — the DMBB QA-hardening folded into datalist-gen / userview-gen / feature-loop / plugin-dev / req-analyst / deploy, plus the new **`joget-dashboard-gen`** (the native SqlChartMenu/DashboardMenu pattern this dashboard tier used).
- `MANIFEST.md` = sha + line-count per skill (drift check). `docs/JOGET-DELIVERY-WORKFLOW.md` = the delivery methodology; `docs/joget-skills-build-prompt.md` = how the set was authored.
- The old `joget-framework` folder and all scattered copies are **retired** (moved to `~/.Trash/joget-framework-retired-*` on 2026-06-14).

**To change a skill:** edit `skills/<name>/SKILL.md` → `./build.sh` → install `dist/<name>.skill` via **Settings → Capabilities** → `git commit && git push`. **Drift check:** `shasum -a 256 "<installed>/<name>/SKILL.md" | cut -c1-12` vs `MANIFEST.md`.

**OPEN (do later — flagged by Aare):** re-audit the skill *set* for gaps. Likely method: walk every stage of `docs/JOGET-DELIVERY-WORKFLOW.md` and confirm each has a covering skill; check the skills for references to a skill/step that doesn't exist; compare the installed set against the repo (MANIFEST shas) for drift. There's a `## Next — re-audit the set` note in the repo README tracking this.

---

## 1. Environment quickstart (host, all already running)

- **jdx9**: Joget 9.0.7 EE at `/Users/aarelaponin/joget-enterprise-linux-9.0.7-9`, http://localhost:8089/jw, admin/admin. DB `jwdb_mtca` (`joget_mtca`/`joget_mtca` on localhost).
  - Restart (the only reliable way): `cd <jdx9>; pkill -f "org.apache.catalina.startup.Bootstrap"; sleep 8; ./tomcat.sh start` then poll `…/web/json/workflow/currentUsername` for 400/200.
- **Toolkit venv**: `/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/.venv/bin/python`.
- **Repo**: `/Users/aarelaponin/IdeaProjects/rsr/mt-tca-prj/15-ITCAS/03_debt_management`.

**Regenerate userview + jwa** (DMBB-DASH must be FIRST so Dashboards is the landing):
```bash
ORDER="DMBB-DASH DMBB-F01-dpm-core-md DMBB-F02-debt-case-type DMBB-F03-identification \
DMBB-F04-escalation DMBB-F05-escalation-admin DMBB-F06-instalments DMBB-F07-enforcement-actions \
DMBB-F08-enforcement-config DMBB-F09-writeoff DMBB-F10-default-assessment DMBB-F11-debtors-list \
DMBB-F12-collection-mi DMBB-RPT-jasper-reports DMBB-UX-QA"
UVS=""; for f in $ORDER; do [ -f dmbb/features/$f/userview/UV-delta.spec.yml ] && UVS="$UVS dmbb/features/$f/userview/UV-delta.spec.yml"; done
python3 scripts/gen_userview.py $UVS dmbb/generated/userviews dmbb/generated/forms dmbb/generated/datalists
python3 scripts/build_jwa.py dmbb/generated dmbb "DMBB" dmbb/generated/dmbb.jwa
```
**Deploy** (then RESTART — reimported API defs go live only after a restart):
```bash
JDX9_PASSWORD=admin <venv> scripts/deploy_jwa.py jdx9 dmbb/generated dmbb "DMBB"
# …restart Tomcat…
```
**Regression** (canonical; cold-starts Tomcat, sources Mayan pw, runs t02..t26):
```bash
bash scripts/run_regression.sh 2 26      # must stay GREEN=25
```

---

## 2. The remaining polish — in priority order

### Item A — DashboardMenu: compose the charts into ONE page per tier (RPT-FR-006 single-page)
Today each chart is its own menu/page. Goal: one dashboard page that lays out the charts in a grid.
- **Plugin:** `org.joget.plugin.enterprise.DashboardMenu`, `mode:"plugins"`, `plugins`: a repeater of portlets — each `{ userviewMenu:<embedded menu element JSON>, x, y, width(1|2|3), height, autoReload, fixed, resizable }`.
- **RECON FIRST (the one unknown):** how the embedded `userviewMenu` portlet is serialised inside `plugins[]` — nested JSON object vs. JSON-encoded string. Confirm by exporting a hand-built DashboardMenu from the App Composer UI, or inspect `app_userview.json` after building one. **Do a 1-page SPIKE** (a DashboardMenu embedding 2 existing `SqlChartMenu` portlets) and verify it renders both before generalising.
- **Build:** retarget the now-unused `dashboard_menu()` emitter in `gen_userview.py` (it currently still points at the rejected `HtmlPage` — change it to emit `DashboardMenu` composing `sqlchart` portlets). Then in `DMBB-DASH/userview/UV-delta.spec.yml` add a `type: dashboard` menu listing its chart portlets. Keep the standalone charts or fold them in.
- **Verify:** extend `run_t26` with "the DashboardMenu page renders all N charts in one page, HTTP 200, no error".

### Item B — K2 KPI traffic-light tiles (RPT-FR-003)
Five RAG tiles: total debt stock, enforcement exec rate, recovered, instalment active %, debt aged >365d — green/amber/red vs **configurable** targets.
- **Targets = config-as-code:** seed `mdCollectionParam` rows (e.g. `execRateTarget=75`, `activePctTarget=80`, `agedShareTarget=20`). Form `mdCollectionParam` already exists (F12).
- **Data WITHOUT scraping** (the ADR's hard rule): cleanest = a thin `CustomHTML` tile-strip portlet whose values come from a **JSON source** — an API-Builder *List* API over a one-row KPI SQL (same-origin, session cookie) — and whose thresholds read the seeded `mdCollectionParam`. 
- **Safe fallback (K1, fully native, zero custom code):** a `SqlChartMenu` with `showTable:"top"` over a one-row KPI query + conditional colour. Use this if the tile-data plumbing fights you.
- **Verify:** `run_t26` — tiles present, values render, a target change in `mdCollectionParam` flips a tile's RAG state.

### Item C — 3-tier role split (RPT-FR-007) — BLOCKED on task #60 first
Split Dashboards into Management (`dm_manager`) / Department (`dm_policy_admin`) / Portfolio (`dm_officer`), each landing the right role. This rides on the **GroupPermission membership fix**:
- The `GroupPermission` emitter in `gen_userview.py` is correct but **commented out** (`category_permission()` returns open). It was disabled because seeded `admin` membership of the new groups was **not recognised** (raw `dir_user_group` inserts + restart weren't enough — likely a DirectoryManager/organization nuance).
- **Fix:** create groups + memberships via the **Joget directory API** (not raw `dir_*` inserts); confirm `admin` resolves as a member; give the render acceptance tests **role-user logins** (so gated categories can be checked per role); then **uncomment** the emitter and re-enable `permission_role` on the dashboard categories.
- Role seed scaffold exists: `dmbb/seed/dir_roles.sql` (groups `dm_officer`/`dm_policy_admin`/`dm_manager`, memberships). Tracked as **task #60**.

### Cleanup (do alongside A) — retire the rejected interim files on the host
```bash
rm dmbb/features/DMBB-DASH/dashboards/dash_*.html \
   scripts/gen_dashboards.py scripts/gen_dashboard_forms.py \
   dmbb/generated/forms/dashMgmtForm.json dmbb/generated/forms/dashDeptForm.json \
   dmbb/generated/forms/dashOfficerForm.json
```
Then regen userview + build_jwa + deploy. `run_t26` T-26.4 already guards that no HtmlPage/FormMenu dashboard menu comes back — keep it green.

---

## 3. Gotchas that will bite (from this session)
- **Restart after every deploy** — reimported API/userview defs are cached; a delete→import→publish does NOT take effect until a Tomcat restart.
- **SqlChartMenu loads its series via AJAX**, so chart data is NOT in the initial page HTML — assert the *bound datalist* returns rows, not inline values (run_t26 T-26.3 does this).
- **`datasource:"custom"`** on SqlChartMenu (driverClassName/url/user/pass) makes a chart re-pointable to ClickHouse/ITCAS at go-live — note for the migration, don't hardcode.
- **Never scrape rendered datalist HTML for data** (ADR-002). If a chart needs a shape no datalist has, add a `datasource:"default"` raw `query` SqlChartMenu, not custom JS.
- **Sandbox can't `rm`/delete on the mounted repo** — deletions are host-only.
- Generators are deterministic (P5): never hand-edit `dmbb/generated/**`; fix the spec or the generator and regenerate.

---

## 4. Paste-ready opening prompt for the resume

> Resume the MTCA debt-management dashboard polish in `15-ITCAS/03_debt_management`. Read `docs/RESUME-dashboard-polish.md` first, then ADR-002. The native SqlChartMenu dashboard tier is live and green (run_t26 5/5, full regression 25/25). Do the remaining polish from §2 in order: (A) compose the 4 charts into one DashboardMenu page per the ADR — recon the portlet serialisation and SPIKE a 2-portlet page first; (B) add the K2 KPI traffic-light tiles with targets in mdCollectionParam; then (C) the 3-tier role split after fixing GroupPermission membership (task #60). Retire the interim files (§Cleanup). Keep `scripts/run_regression.sh 2 26` green and extend run_t26 for each new piece. Where any skill conflicts with the repo's scripts or DX9-DELTAS, the repo wins.
