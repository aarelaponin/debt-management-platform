#!/usr/bin/env python3
"""DMBB-WF (ADR-004) acceptance T-28 — multi-tenancy (live jdx9, apps cmbb + dmbb).

T-28.1 a case opened through the envelope process is stamped tenant=MLT (TenantContext
       default, resolved from the opener's profile — invisible, never picked).
T-28.2 tenant ISOLATION: with a TST-tenant workflow present, an MLT case resolves the
       MLT workflow (W-DEFAULT) and a TST case resolves the TST workflow — proving the
       tenant participates in workflow resolution and isolates tenants.

Usage: JDX9_PASSWORD=admin run_t28.py
"""
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timedelta
import urllib.request

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

BASE = "http://localhost:8089/jw"
PROC = "cmbb:latest:cmCaseEnvelope"
RESULTS = []
RUN = str(int(time.time()))[-6:]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


CK = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")
DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def post(form, api, key, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", api), ("api_key", key)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = (json.loads(body).get("errors") or {}) if body.startswith("{") else {"_": body[:120]}
        if errs:
            raise SystemExit(f"validation error on {form}: {body[:300]}")


def cmbb(form, p):
    post(form, "API-cmbb-data", CK, p)


def dmbb(form, p):
    post(form, "API-dmbb-data", DK, p)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


class Joget:
    def __init__(self):
        c = JogetClient.from_instance("jdx9")
        c.get("/web/json/console/app/list", params={"pageSize": 1})
        self.s = getattr(c, "session", None) or c._session
        self.s.headers["Referer"] = BASE + "/web/console/home"

    def csrf(self):
        r = self.s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
        return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    def start(self, rid):
        r = self.s.post(f"{BASE}/web/json/workflow/process/start/{PROC}",
                        data={"OWASP_CSRFTOKEN": self.csrf(), "recordId": rid,
                              "var_caseId": rid, "var_assignee": "admin"})
        return r.json()

    def complete(self, sub, pid):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        for a in ([data] if isinstance(data, dict) else data):
            aid = a.get("activityId") or a.get("id", "")
            if sub in ((a.get("activityName") or "") + aid) and a.get("processId", "") == pid:
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return True
        return False


def as_of(days):
    return (datetime.now() + timedelta(days=days)).strftime("%Y-%m-%dT%H:%M:%S")


def sweep(days):
    rid = f"t28-er-{RUN}-{int(time.time()) % 1000}"
    dmbb("cmEscalateRun", {"id": rid, "asOf": as_of(days)})
    time.sleep(4)
    return rid


def main():
    assert CK and DK
    jg = Joget()

    # ---------- T-28.1 tenant stamped from profile at open (invisible default MLT) ----------
    c1 = f"t28a-{RUN}"
    cmbb("cmCase", {"id": c1, "caseType": "DM", "tin": f"T28A{RUN}", "origin": "MANUAL",
                    "taxType": "INCOME_TAX", "taxpayerName": "Tenant Probe Ltd",
                    "amountAtStake": "100000", "category": "C6"})
    r = jg.start(c1)
    time.sleep(2)
    jg.complete("openCase", r.get("processId"))
    time.sleep(2)
    tn1 = sql(f"SELECT c_tenant FROM app_fd_cmcase WHERE id='{c1}'")
    check("T-28.1 case opened via process is stamped tenant=MLT (profile default, invisible)",
          tn1 == "MLT", f"tenant={tn1!r}")

    # ---------- T-28.2 tenant isolation in workflow resolution ----------
    # a TST-tenant workflow (tax ANY) — must govern ONLY TST cases, never the MLT default.
    dmbb("dmWorkflow", {"id": "W-TENANT-TST", "code": "W-TENANT-TST", "name": "TST tenant workflow",
                        "tenant": "TST", "taxType": "", "segment": "", "industry": "",
                        "categoryFloor": "C2", "validFrom": "2020-01-01", "status": "Active",
                        "version": "1"})
    # two open DM cases, identical except tenant; managed fields seeded by SQL (documented exception)
    for sfx, tenant in (("mlt", "MLT"), ("tst", "TST")):
        cid = f"t28b-{sfx}-{RUN}"
        tin = f"T28B{sfx.upper()}{RUN}"
        cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "MANUAL",
                        "taxType": "INCOME_TAX", "taxpayerName": f"Iso {sfx} Ltd",
                        "amountAtStake": "300000", "category": "C6"})
        dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": "C6", "stage": "Identified",
                        "triggerOrigin": "MANUAL", "consolidatedAmount": "300000", "lastStepSeq": "0"})
        sql(f"UPDATE app_fd_cmcase SET c_currentstate='OPEN', c_taxtype='INCOME_TAX', "
            f"c_category='C6', c_tenant='{tenant}' WHERE id='{cid}'")
        sql(f"UPDATE app_fd_dmdebt SET c_stage='Identified', c_laststepseq='0', "
            f"c_debtcategory='C6' WHERE id='{cid}'")
    sweep(10)
    wfMlt = sql(f"SELECT c_workflowcode FROM app_fd_dmdebt WHERE id='t28b-mlt-{RUN}'")
    wfTst = sql(f"SELECT c_workflowcode FROM app_fd_dmdebt WHERE id='t28b-tst-{RUN}'")
    check("T-28.2 tenant isolation: MLT case→W-DEFAULT, TST case→W-TENANT-TST",
          wfMlt == "W-DEFAULT" and wfTst == "W-TENANT-TST",
          f"mlt_workflow={wfMlt!r} tst_workflow={wfTst!r}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
