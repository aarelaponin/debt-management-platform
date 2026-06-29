#!/usr/bin/env python3
"""CMBB-F03 acceptance tests T-03.1..6 — scripted against live jdx9.
Same mechanics as F02 run_t02.py (form API + workflow JSON API + read-only
SQL). Officer capacity is adjusted via API upserts between tests.

Usage: JDX9_PASSWORD=admin run_t03.py
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


def cid(n):
    return f"al-{RUN}-{n}"


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return r.stdout.strip()


KEY = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")


def drain(max_s=45):
    """Order-independence guard: wait until no envelope process is still running, so a
    prior suite's late CASE_ASSIGNED events cannot land AFTER this suite's reset and skew
    the round-robin cursor (which is a global cmEvent CASE_ASSIGNED count). Cold/isolated
    runs return immediately; only a tight back-to-back sweep actually waits here."""
    stable = 0
    for _ in range(max_s):
        running = sql("SELECT count(*) FROM app_report_process_instance WHERE finishtime IS NULL")
        if running == "0":
            stable += 1
            if stable >= 2:
                return
        else:
            stable = 0
        time.sleep(1)


def form_post(form_id, payload, retries=2):
    for attempt in range(retries + 1):
        req = urllib.request.Request(f"{BASE}/api/form/{form_id}/saveOrUpdate",
                                     data=json.dumps(payload).encode(), method="POST")
        for h, v in [("Content-Type", "application/json"), ("Accept", "application/json"),
                     ("api_id", "API-cmbb-data"), ("api_key", KEY)]:
            req.add_header(h, v)
        try:
            with urllib.request.urlopen(req) as r:
                return r.status
        except urllib.error.HTTPError:
            if attempt == retries:
                raise
            time.sleep(2)


def officer(code, cap, active="true", unit=None):
    form_post("mdOfficerProfile", {"id": code, "code": code, "name": code,
                                   "unit": unit or ("UNIT-A" if code == "officer1" else "UNIT-B"),
                                   "taxTypes": "VAT;IT" if code == "officer1" else "VAT",
                                   "maxCapacity": str(cap), "active": active})


class Joget:
    def __init__(self):
        client = JogetClient.from_instance("jdx9")
        client.get("/web/json/console/app/list", params={"pageSize": 1})
        self.s = getattr(client, "session", None) or client._session
        self.s.headers["Referer"] = BASE + "/web/console/home"

    def csrf(self):
        r = self.s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
        return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    def start(self, record_id):
        r = self.s.post(f"{BASE}/web/json/workflow/process/start/{PROC}",
                        data={"OWASP_CSRFTOKEN": self.csrf(), "recordId": record_id,
                              "var_caseId": record_id})
        try:
            return r.json()
        except Exception:
            return {"raw": r.text[:160]}

    def assignments(self):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        return [data] if isinstance(data, dict) else data

    def complete_open_case(self, process_id):
        for a in self.assignments():
            aid = a.get("activityId") or a.get("id", "")
            if "openCase" in ((a.get("activityName") or "") + aid) \
                    and a.get("processId", "") == process_id:
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                                   data={"OWASP_CSRFTOKEN": self.csrf()}).status_code
        return None


def open_case(jg, n, tin):
    form_post("cmCase", {"id": cid(n), "caseType": "TEST", "tin": tin,
                         "origin": "MANUAL", "taxType": "VAT"})
    r = jg.start(cid(n))
    time.sleep(2)
    jg.complete_open_case(r.get("processId"))
    time.sleep(3)
    return sql(f"SELECT c_assignee || '|' || coalesce(c_assignmentstatus,'') "
               f"FROM app_fd_cmcase WHERE id='{cid(n)}'")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    assert KEY, "no api key"
    jg = Joget()
    # fixture reset (DEV teardown; unique ids avoid stale-cache). drain() first so a prior
    # suite's still-running envelopes can't emit CASE_ASSIGNED after we zero the table.
    drain()
    sql("DELETE FROM app_fd_cmevent")
    sql("DELETE FROM app_fd_cmcase")
    officer("officer1", 2)
    officer("officer2", 2)
    form_post("mmCoi", {"id": "COI-TEST-UNIT", "caseType": "TEST",
                        "ruleType": "EXCLUDE_UNIT", "expression": "NO-SUCH-UNIT"})

    # ---------- T-03.1: round-robin + history ----------
    # Assert round-robin ALTERNATION (two consecutive opens land on the two distinct
    # candidates), not an absolute officer1-then-officer2 order — the engine cursor is a
    # global CASE_ASSIGNED count whose starting parity must not be baked into the test.
    a1 = open_case(jg, 1, "100058G")
    a2 = open_case(jg, 2, "100059H")
    pair = {a1, a2}
    check("T-03.1 round-robin assignment",
          pair == {"officer1|ASSIGNED", "officer2|ASSIGNED"}, f"{a1} / {a2}")
    n = sql("SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='CASE_ASSIGNED'")
    check("T-03.1 assignment logged in history", n == "2", n)

    # ---------- T-03.2: capacity skip (BR-WF-005) ----------
    officer("officer1", 1)  # officer1 now at 1/1 — at capacity
    a3 = open_case(jg, 3, "100061K")
    warn = sql("SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='CAPACITY_WARNING' "
               "AND c_payload LIKE '%officer2%'")
    check("T-03.2 officer at capacity skipped", a3 == "officer2|ASSIGNED" and warn == "1",
          f"{a3} warn={warn}")

    # ---------- T-03.3: unassignable -> supervisor queue ----------
    a4 = open_case(jg, 4, "100062L")  # officer1 1/1, officer2 2/2 -> nobody
    failed = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(4)}' "
                 "AND c_eventtype='ASSIGNMENT_FAILED'")
    alert = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(4)}' "
                "AND c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%SUPERVISOR_UNASSIGNED%'")
    check("T-03.3 unassignable routed UNASSIGNED + alert",
          a4 == "|UNASSIGNED" and failed == "1" and alert == "1",
          f"{a4} failed={failed} alert={alert}")

    # ---------- T-03.4: single reassignment ----------
    ref1 = sql(f"SELECT c_caseref FROM app_fd_cmcase WHERE id='{cid(1)}'")
    form_post("cmReassign", {"id": f"ord-{RUN}-1", "caseRef": ref1,
                             "toOfficer": "officer2", "reason": "leave coverage"})
    time.sleep(4)
    res = sql(f"SELECT c_result FROM app_fd_cmreassign WHERE id='ord-{RUN}-1'")
    if not res:  # postProcessor may not fire on API path — record finding
        print("  (postProcessor idle on API save — checking)")
    asg = sql(f"SELECT c_assignee FROM app_fd_cmcase WHERE id='{cid(1)}'")
    audit = sql("SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='CASE_REASSIGNED' "
                f"AND c_payload LIKE '%{ref1}%' AND c_payload LIKE '%leave coverage%'")
    notices = sql("SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='NOTIF_PENDING' "
                  f"AND c_payload LIKE '%{ref1}%'")
    check("T-03.4 single reassign + audit + 2 notices",
          asg == "officer2" and audit == "1" and notices == "2",
          f"assignee={asg} audit={audit} notices={notices} result={res}")

    # ---------- T-03.5: bulk redistribution (BR-WF-007) ----------
    officer("officer1", 10)
    form_post("cmReassign", {"id": f"ord-{RUN}-2", "filterOfficer": "officer2",
                             "filterCaseType": "TEST", "toOfficer": "",
                             "reason": "org change"})
    time.sleep(4)
    res2 = sql(f"SELECT c_result FROM app_fd_cmreassign WHERE id='ord-{RUN}-2'")
    left = sql("SELECT count(*) FROM app_fd_cmcase WHERE c_assignee='officer2'")
    check("T-03.5 bulk reassignment off officer2", "reassigned" in res2 and left == "0",
          f"result={res2} remaining={left}")

    # ---------- T-03.6: COI exclusion ----------
    form_post("mmCoi", {"id": "COI-TEST-UNIT", "caseType": "TEST",
                        "ruleType": "EXCLUDE_UNIT", "expression": "UNIT-A"})
    officer("officer1", 10)
    officer("officer2", 10)
    a6 = open_case(jg, 6, "100063M")
    check("T-03.6 COI unit exclusion", a6 == "officer2|ASSIGNED", a6)
    form_post("mmCoi", {"id": "COI-TEST-UNIT", "caseType": "TEST",
                        "ruleType": "EXCLUDE_UNIT", "expression": "NO-SUCH-UNIT"})

    print()
    failed_tests = [name for name, c, _ in RESULTS if not c]
    print(f"{len(RESULTS) - len(failed_tests)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed_tests}" if failed_tests else " — ALL GREEN"))
    sys.exit(1 if failed_tests else 0)


if __name__ == "__main__":
    main()
