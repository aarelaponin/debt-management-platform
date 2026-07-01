#!/usr/bin/env python3
"""DMBB-DASH acceptance T-26.1..5 — native dashboard tier (ADR-002) on live jdx9.

ADR-002 decision: dashboards = enterprise SqlChartMenu (server-side SQL -> Apache ECharts), bound
to the tested MI datalist binders. NO custom HTML/JS, NO client-side datalist scraping.

T-26.1 the 4 chart menus render: HTTP 200, ECharts present, zero SQL/JSP error
T-26.2 charts are native SqlChartMenu in the deployed userview (className), bound to the MI datalists
T-26.3 server-side data source: the chart's bound datalist (list_debtByCategory) renders server-side
        category rows (SqlChartMenu charts that datalist's server-side data; it can only chart what the
        binder returns — the binder SQL is independently tested by run_t21)
T-26.4 the scraping interim is retired: no HtmlPage/CustomHTML/FormMenu dashboard menu remains
T-26.5 Dashboards is the landing category and holds exactly the 4 charts

Usage: JDX9_PASSWORD=admin run_t26.py
"""
import json
import re
import subprocess
import sys
import urllib.request

RESULTS = []
BASE = "http://localhost:8089/jw"
CHARTS = ["chartDebtByCategory", "chartDebtAging", "chartRecoveryByAction", "chartInstalmentCompliance"]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


import os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "scripts"))
import uv_auth


def get(url):
    # #91: console categories are GroupPermission-gated; fetch userview pages as admin (member of
    # all role groups). Anonymous GETs now 302. API POSTs elsewhere keep their own auth (untouched).
    return uv_auth.admin_get(url)


def uv_json():
    j = sql("SELECT json FROM app_userview WHERE id='dmbbConsole' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    # ---------- T-26.1 charts render ----------
    ok1, det = True, ""
    for c in CHARTS:
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{c}")
        ech = "echart" in html.lower()
        err = bool(re.search(r"exception|HTTP Status 500|SQLException|JRException", html, re.I))
        if code != 200 or not ech or err:
            ok1 = False
            det += f"{c}=http{code}/echart{int(ech)}/err{int(err)} "
    check("T-26.1 all 4 SqlChartMenu charts render (ECharts, no error)", ok1, det or "all render")

    # ---------- T-26.2 native SqlChartMenu bound to RAW SQL (datasource=default) ----------
    # The datalist-binder mode renders "No data is available"; charts run their own server-side query.
    uv = uv_json()
    menus = {m["properties"].get("customId"): m for cat in uv["categories"] for m in cat.get("menus", [])}
    bad = {}
    for c in CHARTS:
        m = menus.get(c)
        cls = (m or {}).get("className", "")
        p = (m or {}).get("properties", {})
        if not cls.endswith("SqlChartMenu") or p.get("datasource") != "default" or "SELECT" not in (p.get("query") or "").upper():
            bad[c] = f"{cls.split('.')[-1]}/ds={p.get('datasource')}/q={bool(p.get('query'))}"
    check("T-26.2 charts are native SqlChartMenu on server-side SQL (datasource=default + query)", not bad, f"bad={bad}")

    # ---------- T-26.3 charts render WITH data (no 'No data is available') ----------
    nodata = {}
    for c in CHARTS:
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{c}")
        if "No data is available" in html or "echart" not in html.lower():
            nodata[c] = f"noData={'No data is available' in html} echarts={'echart' in html.lower()}"
    check("T-26.3 all 4 charts render with data (server-side query returned rows)", not nodata, f"empty={nodata}")

    # ---------- T-26.4 the Chart.js SCRAPING interim is retired (the iframe dashboard is fine) ----------
    SCRAPING = {"dashMgmtForm", "dashDeptForm", "dashOfficerForm"}
    leftover = [(m["properties"].get("customId"), m["properties"].get("formId")) for cat in uv["categories"]
                for m in cat.get("menus", [])
                if m["properties"].get("formId") in SCRAPING or m["className"].split(".")[-1] == "HtmlPage"]
    check("T-26.4 Chart.js scraping interim retired (no scraping HtmlPage/FormMenu)", not leftover, f"leftover={leftover}")

    # ---------- T-26.5 Dashboards landing = one-page dashboard + the 4 charts ----------
    cat0 = uv["categories"][0]
    label0 = re.sub(r"<[^>]+>", "", cat0["properties"]["label"]).strip()
    chart_ids = [m["properties"].get("customId") for m in cat0.get("menus", []) if m["className"].endswith("SqlChartMenu")]
    landing = cat0["menus"][0]["properties"].get("formId") if cat0["menus"] else None
    # DMBB-DASH2 extended the Dashboards page (KPI Monitoring + priority/origin charts + by-officer),
    # so the 4 base charts are now a SUBSET of the category's charts, not the exact set.
    check("T-26.5 Dashboards lands on the one-page dashboard + includes the 4 base charts",
          label0 == "Dashboards" and landing == "dashOverviewForm" and set(CHARTS) <= set(chart_ids),
          f"label={label0!r} landing={landing} charts={chart_ids}")

    # ---------- T-26.6 the one-page dashboard renders all 4 charts as embed iframes ----------
    _, ovhtml = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/dashOverviewForm")
    iframes = re.findall(r'<iframe[^>]*src="([^"]+)"', ovhtml)
    embedded = {re.search(r"/_/(\w+)\?embed", s).group(1) for s in iframes if re.search(r"/_/(\w+)\?embed", s)}
    check("T-26.6 one-page dashboard embeds all 4 charts (iframes, contextPath resolved)",
          set(CHARTS) <= embedded and "#request.contextPath#" not in ovhtml,
          f"embedded={sorted(embedded)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
