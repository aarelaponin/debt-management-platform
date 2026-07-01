#!/usr/bin/env python3
"""DMBB-UX-QA acceptance T-23.1..5 — live jdx9. Verifies the list UX remediation against the
DEPLOYED app_datalist / app_userview JSON + a functional drill-down.

T-23.1 sortable: every column on the operational lists is sortable
T-23.2 typed filters: debtors list has a SelectBox category filter; the dmAction CRUD list has TIN+status filters
T-23.3 drill actions present: debtByCategory drills to list_debtorsList; debtorsList has an Open-case row drill
T-23.4 functional drill: list_debtorsList?fcat=C5 shows only the C5 debtor, ?fcat=C3 only the C3 (summary->detail)
T-23.5 orphan removed: the standalone "Debt lines" (list_dmLine) menu is gone from the userview

Usage: run_t23.py
"""
import json
import subprocess
import sys
import time
import urllib.request

RESULTS = []
R = str(int(time.time()))[-6:]
BASE = "http://localhost:8089/jw"


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


CK = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")
DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def deployed_json(table, lid):
    j = sql(f"SELECT json FROM {table} WHERE id='{lid}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def post(form, api, key, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form}/saveOrUpdate", data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", api), ("api_key", key)]:
        req.add_header(h, v)
    urllib.request.urlopen(req).read()


import os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "scripts"))
import uv_auth


def get(url):
    # #91: console categories are GroupPermission-gated; fetch userview pages as admin (member of
    # all role groups). Anonymous GETs now 302. API POSTs elsewhere keep their own auth (untouched).
    return uv_auth.admin_get(url)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def filt_classes(dl):
    return [f["type"]["className"].split(".")[-1] for f in dl.get("filters", [])]


def main():
    assert CK and DK

    # ---------- T-23.1 sortable ----------
    dbt = deployed_json("app_datalist", "list_debtorsList")
    act = deployed_json("app_datalist", "list_dmAction")
    s1 = sum(1 for c in dbt["columns"] if c.get("sortable") == "true")
    s2 = sum(1 for c in act["columns"] if c.get("sortable") == "true")
    check("T-23.1 every column sortable (debtorsList + dmAction)",
          s1 == len(dbt["columns"]) and s2 == len(act["columns"]) and s1 > 0 and s2 > 0,
          f"debtors {s1}/{len(dbt['columns'])}, action {s2}/{len(act['columns'])}")

    # ---------- T-23.2 typed filters ----------
    dbt_f = {f["name"]: f["type"]["className"].split(".")[-1] for f in dbt["filters"]}
    act_f = [f["name"] for f in act["filters"]]
    check("T-23.2 typed filters (debtors category=SelectBox; dmAction has TIN+status)",
          dbt_f.get("category") == "SelectBoxDataListFilterType"
          and "tin" in act_f and "status" in act_f,
          f"debtors={dbt_f} action={act_f}")

    # ---------- T-23.3 drill actions ----------
    cat = deployed_json("app_datalist", "list_debtByCategory")
    cat_drill = any("list_debtorsList" in r["properties"].get("href", "") for r in cat.get("rowActions", []))
    dbt_open = any("dmCaseConsole" in r["properties"].get("href", "") for r in dbt.get("rowActions", []))
    check("T-23.3 drill actions present (summary->detail + detail->record)",
          cat_drill and dbt_open, f"catDrill={cat_drill} openCase={dbt_open}")

    # ---------- T-23.4 functional drill ----------
    post("cmCase", "API-cmbb-data", CK, {"id": f"qa5-{R}", "caseType": "DM", "tin": f"QA5{R}",
         "origin": "SYSTEM", "taxpayerName": "QA Five", "amountAtStake": "50000", "category": "C5", "currentState": "OPEN"})
    post("dmDebt", "API-dmbb-data", DK, {"id": f"qa5-{R}", "tin": f"QA5{R}", "debtCategory": "C5",
         "consolidatedAmount": "50000", "stage": "Final demand", "lastStepSeq": "3", "writeOffStatus": ""})
    post("cmCase", "API-cmbb-data", CK, {"id": f"qa3-{R}", "caseType": "DM", "tin": f"QA3{R}",
         "origin": "SYSTEM", "taxpayerName": "QA Three", "amountAtStake": "500", "category": "C3", "currentState": "OPEN"})
    post("dmDebt", "API-dmbb-data", DK, {"id": f"qa3-{R}", "tin": f"QA3{R}", "debtCategory": "C3",
         "consolidatedAmount": "500", "stage": "Final demand", "lastStepSeq": "3", "writeOffStatus": ""})
    time.sleep(2)
    u = f"{BASE}/web/userview/dmbb/dmbbConsole/_/list_debtorsList"
    _, h5 = get(f"{u}?fcat=C5")
    _, h3 = get(f"{u}?fcat=C3")
    ok = (f"QA5{R}" in h5 and f"QA3{R}" not in h5) and (f"QA3{R}" in h3 and f"QA5{R}" not in h3)
    check("T-23.4 functional summary->detail drill filters correctly",
          ok, f"C5page[QA5={'QA5'+R in h5},QA3={'QA3'+R in h5}] C3page[QA3={'QA3'+R in h3},QA5={'QA5'+R in h3}]")

    # ---------- T-23.5 orphan removed ----------
    uv = deployed_json("app_userview", "dmbbConsole")
    dl_menus = [m["properties"].get("datalistId") for c in uv["categories"] for m in c["menus"]]
    check("T-23.5 orphan 'Debt lines' (list_dmLine) menu removed",
          "list_dmLine" not in dl_menus, f"present={'list_dmLine' in dl_menus}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
