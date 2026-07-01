#!/usr/bin/env python3
"""DMBB-DASH2 acceptance T-43.1..5 — live jdx9.

T-43.1 KPI Monitoring (RPT-FR-003): deployed list_kpiArrears yields the 12 arrears KPIs, each RAG-coloured.
T-43.2 KPI targets (RPT-FR-003): mdKpiTarget seeded with the 12 configurable target thresholds.
T-43.3 Multi-stream (RPT-FR-006): the priority + origin SqlChartMenu charts render (ECharts, no error).
T-43.4 Hierarchical (RPT-FR-007): deployed list_debtByOfficer returns the entity-targeted roll-up.
T-43.5 SAS stub (RPT-FR-020): mdRiskSource config carrier deployed (inactive); risk display already graceful.

Usage: JDX9_PASSWORD=admin run_t43.py
"""
import json
import os
import re
import subprocess
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "scripts"))
import uv_auth  # noqa: E402

UV = "http://localhost:8089/jw/web/userview/dmbb/dmbbConsole/_"
RESULTS = []
ERR = ("cannot be cast", "sqlexception", "exception", "http status 500", "no data is available")


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:300]) if r.returncode else r.stdout.strip()


def deployed_sql(list_id):
    j = sql(f"SELECT json FROM app_datalist WHERE id='{list_id}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    d = json.loads(j)
    b = d.get("binder") or d.get("properties", {}).get("binder") or {}
    p = b.get("properties", b)
    return re.sub(r"#requestParam\.[^#]+#", "", p["sql"])


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    # ---------- T-43.1 KPI monitoring: 12 KPIs, RAG-coloured ----------
    base = deployed_sql("list_kpiArrears")
    n = sql(f"SELECT count(*) FROM ({base}) q")
    rags = sql(f"SELECT string_agg(DISTINCT rag, ',') FROM ({base}) q")
    have_cols = sql(f"SELECT count(*) FROM ({base}) q WHERE target_value IS NOT NULL AND rag IS NOT NULL")
    check("T-43.1 KPI Monitoring lists the 12 arrears KPIs, each RAG-coloured (RPT-FR-003)",
          n == "12" and have_cols == "12" and any(x in (rags or "") for x in ("GREEN", "AMBER", "RED")),
          f"rows={n} withTargetAndRag={have_cols} rags={rags}")

    # ---------- T-43.2 configurable KPI targets ----------
    tgts = sql("SELECT count(*) FROM app_fd_mdkpitarget WHERE c_active='true'")
    check("T-43.2 mdKpiTarget seeded with 12 configurable targets (RPT-FR-003)",
          tgts == "12", f"targets={tgts}")

    # ---------- T-43.3 multi-stream charts render ----------
    bad = {}
    for cid in ("chartCasesByPriority", "chartCaseByOrigin"):
        code, html = uv_auth.admin_get(f"{UV}/{cid}")
        errs = [m for m in ERR if m in html.lower()]
        if not (code == 200 and "echart" in html.lower() and not errs):
            bad[cid] = f"http={code} echart={'echart' in html.lower()} errs={errs}"
    check("T-43.3 multi-stream priority + origin charts render (RPT-FR-006)", not bad, f"bad={bad}")

    # ---------- T-43.4 hierarchical entity-targeted roll-up ----------
    baseo = deployed_sql("list_debtByOfficer")
    rows = sql(f"SELECT count(*) FROM ({baseo}) q")
    check("T-43.4 debt-by-officer entity-targeted roll-up returns rows (RPT-FR-007)",
          rows.isdigit() and int(rows) >= 1, f"rows={rows}")

    # ---------- T-43.5 SAS risk-source stub + graceful N/A ----------
    rs = sql("SELECT c_provider, c_active FROM app_fd_mdrisksource WHERE id='RS-SASVIYA'")
    # graceful N/A: the debtors list renders even with no SAS scores (risk column tolerates null)
    dbase = deployed_sql("list_debtorsList")
    dl_rows = sql(f"SELECT count(*) FROM ({dbase}) q")
    check("T-43.5 SAS VIYA risk-source stub deployed (inactive) + risk display graceful (RPT-FR-020)",
          rs.startswith("SAS_VIYA|false") and dl_rows.isdigit() and int(dl_rows) >= 1,
          f"riskSource={rs} debtorsRows={dl_rows}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
