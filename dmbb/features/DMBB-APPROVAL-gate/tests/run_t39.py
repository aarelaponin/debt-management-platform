"""DMBB Decision & Approval Service (#6) — P6: Approvals MI dashboard (live jdx9).

Oversight surface for the DAS: native enterprise SqlChartMenu (server-side SQL -> Apache ECharts,
ADR-002) over the shared cmApproval / cmEvent tables. Four charts in the "Approvals MI" category —
pending-by-authority, outcomes, volume-by-action, and SLA events (escalations/timeouts/delegations).

T-39.1 all 4 chart menus render: HTTP 200, ECharts present, no SQL/JSP error.
T-39.2 they are native SqlChartMenu on server-side SQL (datasource=default + a SELECT query).
T-39.3 the underlying queries return rows (the charts render with data, not "No data is available").

Usage: JDX9_PASSWORD=admin run_t39.py
"""
import json
import re
import subprocess
import sys
import urllib.request

RESULTS = []
BASE = "http://localhost:8089/jw"
CHARTS = ["chartApprovalPending", "chartApprovalOutcomes", "chartApprovalByAction", "chartApprovalBreaches"]


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
    # ---------- T-39.1 charts render ----------
    ok1, det = True, ""
    for c in CHARTS:
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{c}")
        ech = "echart" in html.lower()
        err = bool(re.search(r"exception|HTTP Status 500|SQLException|JRException", html, re.I))
        if code != 200 or not ech or err:
            ok1 = False
            det += f"{c}=http{code}/echart{int(ech)}/err{int(err)} "
    check("T-39.1 all 4 Approvals-MI SqlChartMenu charts render (ECharts, no error)", ok1, det or "all render")

    # ---------- T-39.2 native SqlChartMenu on server-side SQL ----------
    uv = uv_json()
    menus = {m["properties"].get("customId"): m for cat in uv["categories"] for m in cat.get("menus", [])}
    bad = {}
    for c in CHARTS:
        m = menus.get(c)
        cls = (m or {}).get("className", "")
        p = (m or {}).get("properties", {})
        if not cls.endswith("SqlChartMenu") or p.get("datasource") != "default" or "SELECT" not in (p.get("query") or "").upper():
            bad[c] = f"{cls.split('.')[-1]}/ds={p.get('datasource')}/q={bool(p.get('query'))}"
    check("T-39.2 charts are native SqlChartMenu on server-side SQL (datasource=default + query)", not bad, f"bad={bad}")

    # ---------- T-39.3 charts render WITH data ----------
    nodata = {}
    for c in CHARTS:
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{c}")
        if "No data is available" in html or "echart" not in html.lower():
            nodata[c] = f"noData={'No data is available' in html} echarts={'echart' in html.lower()}"
    check("T-39.3 all 4 charts render with data (server-side query returned rows)", not nodata, f"empty={nodata}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
