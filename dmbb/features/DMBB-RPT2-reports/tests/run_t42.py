#!/usr/bin/env python3
"""DMBB-RPT2 acceptance T-42.1..5 — live jdx9.

T-42.1 Collection Status Report (RPT-FR-009) renders (HTTP 200, no JRException, title + per-case columns).
T-42.2 Staff Productivity Report (RPT-FR-013) renders (title + ALL OFFICERS total row).
T-42.3 a saved report configuration (RPT-FR-015/016) persists and lists.
T-42.4 a scheduled report (RPT-FR-017) persists, bound to the config, and lists.
T-42.5 the deployed userview exposes both reports + both config screens.

Usage: JDX9_PASSWORD=admin run_t42.py
"""
import json
import os
import subprocess
import sys
import time
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "scripts"))
import uv_auth  # noqa: E402

BASE = "http://localhost:8089/jw"
UV = BASE + "/web/userview/dmbb/dmbbConsole/_"
RUN = str(int(time.time()))[-6:]
RESULTS = []
ERR_MARKERS = ("cannot be cast", "invalid jasper", "jrexception",
               "net.sf.jasperreports", "stack trace", "http status 500")


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:300]) if r.returncode else r.stdout.strip()


DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def post(form, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", "API-dmbb-data"), ("api_key", DK)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = (json.loads(body).get("errors") or {}) if body.startswith("{") else {"_": body[:120]}
        if errs:
            raise SystemExit(f"validation error on {form}: {body[:300]}")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    assert DK, "missing dmbb key"

    # ---------- T-42.1 Collection Status Report renders ----------
    code, html = uv_auth.admin_get(f"{UV}/rptCollectionStatus")
    errs = [m for m in ERR_MARKERS if m in html.lower()]
    ok = code == 200 and not errs and "collection status report" in html.lower() \
        and "enforcement stage" in html.lower()
    check("T-42.1 Collection Status Report renders (RPT-FR-009)", ok,
          f"http={code} errs={errs}")

    # ---------- T-42.2 Staff Productivity Report renders ----------
    code, html = uv_auth.admin_get(f"{UV}/rptStaffProductivity")
    errs = [m for m in ERR_MARKERS if m in html.lower()]
    ok = code == 200 and not errs and "staff productivity report" in html.lower() \
        and "all officers" in html.lower()
    check("T-42.2 Staff Productivity Report renders with totals (RPT-FR-013)", ok,
          f"http={code} errs={errs}")

    # ---------- T-42.3 saved report configuration ----------
    cfg = f"RC-{RUN}"
    post("mdReportConfig", {"id": cfg, "code": cfg, "name": "Weekly collection status",
                            "reportId": "rptCollectionStatus", "period": "THIS_MONTH",
                            "format": "PDF", "owner": "dm_manager", "active": "true",
                            "filters": "category=C4"})
    saved = sql(f"SELECT c_reportid FROM app_fd_mdreportconfig WHERE id='{cfg}'")
    check("T-42.3 saved report configuration persists (RPT-FR-015/016)",
          saved == "rptCollectionStatus", f"reportId={saved}")

    # ---------- T-42.4 scheduled report bound to the config ----------
    sch = f"RS-{RUN}"
    post("mdReportSchedule", {"id": sch, "code": sch, "reportConfig": cfg,
                              "frequency": "WEEKLY", "format": "PDF", "active": "true",
                              "recipients": "manager@mtca.example"})
    bound = sql(f"SELECT c_reportconfig, c_frequency FROM app_fd_mdreportschedule WHERE id='{sch}'")
    check("T-42.4 scheduled report persists, bound to the config (RPT-FR-017)",
          bound == f"{cfg}|WEEKLY", f"row={bound}")

    # ---------- T-42.5 userview exposes reports + config screens ----------
    uvjson = sql("SELECT json FROM app_userview WHERE id='dmbbConsole' AND appid='dmbb' "
                 "ORDER BY appversion DESC LIMIT 1")
    present = all(x in uvjson for x in
                  ("rptCollectionStatus", "rptStaffProductivity", "mdReportConfig", "mdReportSchedule"))
    check("T-42.5 deployed userview exposes both reports + both config screens",
          present, f"len={len(uvjson)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
