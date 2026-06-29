#!/usr/bin/env python3
"""DMBB-F02 acceptance T-11.1..4 — live jdx9 (apps cmbb + dmbb).

T-11.1 DM case runs the cmCaseEnvelope: caseType DM, currentState OPEN, caseRef DM-######
T-11.2 subject + lines linked: dmDebt id=case id (1:1), dmLine caseId=case id (1:many)
T-11.3 history: cmEvent chain + cmChainCheck VERIFIED over the DM case
T-11.4 dedup (dedupPolicy=SKIP): second DM case same TIN blocked

Usage: JDX9_PASSWORD=admin run_t11.py
"""
import json
import re
import subprocess
import sys
import time
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


def post(form_id, api_id, key, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form_id}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", api_id), ("api_key", key)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = (json.loads(body).get("errors") or {}) if body.startswith("{") else {"_": body[:120]}
        if errs:
            raise SystemExit(f"validation error on {form_id}: {body[:300]}")


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


def set_officer_pool(active):
    for code, unit, skills in [("officer1", "UNIT-A", "VAT;IT"), ("officer2", "UNIT-B", "VAT")]:
        cmbb("mdOfficerProfile", {"id": code, "code": code, "name": code, "unit": unit,
                                  "taxTypes": skills, "maxCapacity": "2", "active": active})


def open_dm_case(jg, cid, tin):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "MANUAL",
                    "taxType": "VAT", "taxpayerName": "Debtor Ltd", "amountAtStake": "5000",
                    "category": "C4"})
    r = jg.start(cid)
    time.sleep(2)
    jg.complete("openCase", r.get("processId"))
    time.sleep(3)
    return r.get("processId")


def main():
    assert CK and DK, "missing api keys (cmbb/dmbb)"
    jg = Joget()
    set_officer_pool("")
    for t in ("cmevent", "cmcase", "cmtask", "cmchaincheck", "dmdebt", "dmline"):
        if sql(f"SELECT to_regclass('app_fd_{t}')"):
            sql(f"DELETE FROM app_fd_{t}")

    # ---------- T-11.1 ----------
    c1 = f"dm-{RUN}-1"
    open_dm_case(jg, c1, "100010A")
    ct = sql(f"SELECT c_casetype FROM app_fd_cmcase WHERE id='{c1}'")
    st = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{c1}'")
    ref = sql(f"SELECT c_caseref FROM app_fd_cmcase WHERE id='{c1}'")
    check("T-11.1 DM case runs envelope (DM/OPEN/DM-ref)",
          ct == "DM" and st == "OPEN" and bool(re.match(r"^DM-\d{6}$", ref or "")),
          f"type={ct} state={st} ref={ref}")

    # ---------- T-11.2 ----------
    dmbb("dmDebt", {"id": c1, "tin": "100010A", "debtCategory": "C4", "stage": "Identified",
                    "triggerOrigin": "MANUAL", "consolidatedAmount": "5000"})
    # debtCategory/stage are managed (read-only) → the data API drops them on store; the
    # identification engine sets them in production. Seed directly for this manual fixture.
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='C4', c_stage='Identified', c_consolidatedamount='5000' WHERE id='{c1}'")
    for i, (tt, amt) in enumerate([("VAT", "3000"), ("CIT", "2000")], 1):
        dmbb("dmLine", {"id": f"{c1}-L{i}", "caseId": c1, "taxType": tt, "yofa": "2024",
                        "amount": amt, "disputed": "0", "enforceable": amt})
    sub = sql(f"SELECT c_debtcategory FROM app_fd_dmdebt WHERE id='{c1}'")
    nsub = sql(f"SELECT count(*) FROM app_fd_dmdebt WHERE id='{c1}'")
    nline = sql(f"SELECT count(*) FROM app_fd_dmline WHERE c_caseid='{c1}'")
    check("T-11.2 dmDebt 1:1 (id=case id) + dmLine 1:many linked",
          sub == "C4" and nsub == "1" and nline == "2", f"dmDebt={sub}/{nsub} dmLine={nline}")

    # ---------- T-11.3 ----------
    nev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}'")
    cmbb("cmChainCheck", {"id": f"cc-{RUN}", "caseId": c1})
    time.sleep(3)
    vr = sql(f"SELECT c_result FROM app_fd_cmchaincheck WHERE id='cc-{RUN}'")
    check("T-11.3 history present + chain VERIFIED",
          int(nev) >= 1 and vr == "VERIFIED", f"events={nev} verify={vr}")

    # ---------- T-11.4 ----------
    c2 = f"dm-{RUN}-2"
    open_dm_case(jg, c2, "100010A")  # same TIN -> dedup SKIP
    rej = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c2}' "
              "AND c_eventtype='TRANSITION_REJECTED'")
    st2 = sql(f"SELECT coalesce(c_currentstate,'') FROM app_fd_cmcase WHERE id='{c2}'")
    check("T-11.4 dedup blocks 2nd DM case for same TIN",
          int(rej) >= 1 and st2 != "OPEN", f"rejected_ev={rej} state2={st2}")

    set_officer_pool("true")
    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
