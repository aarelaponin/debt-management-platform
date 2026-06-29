#!/usr/bin/env python3
"""CMBB-F02 acceptance tests T-02.1..5 — scripted against live jdx9.

Mechanics (P3-compliant):
- case/config rows via the app's form API (/api/form/{id}/saveOrUpdate)
- process flow via the workflow JSON API (process/start with recordId +
  var_*, assignment accept/complete; monitoring force-complete as fallback)
- assertions via read-only SQL (psql)
- T-02.2 "remove transition" = API-update the mmTransition row's toState
  away, restore after (no SQL writes on config)
- case ids are unique per run (case-<run>-N): avoids Hibernate stale-cache
  collisions after the SQL fixture wipe of cmCase/cmEvent

Usage: JDX9_PASSWORD=admin run_t02.py
"""
import hashlib
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
    return f"case-{RUN}-{n}"


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return r.stdout.strip()


def api_key():
    return sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")


def form_post(form_id, payload, key, retries=2):
    for attempt in range(retries + 1):
        req = urllib.request.Request(f"{BASE}/api/form/{form_id}/saveOrUpdate",
                                     data=json.dumps(payload).encode(), method="POST")
        for h, v in [("Content-Type", "application/json"), ("Accept", "application/json"),
                     ("api_id", "API-cmbb-data"), ("api_key", key)]:
            req.add_header(h, v)
        try:
            with urllib.request.urlopen(req) as r:
                return r.status
        except urllib.error.HTTPError:
            if attempt == retries:
                raise
            time.sleep(2)


class Joget:
    def __init__(self):
        client = JogetClient.from_instance("jdx9")
        client.get("/web/json/console/app/list", params={"pageSize": 1})
        self.s = getattr(client, "session", None) or client._session
        # DX9 JsonResponseFilter rejects /web/json/* without same-origin Referer
        self.s.headers["Referer"] = BASE + "/web/console/home"

    def csrf(self):
        r = self.s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
        return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    def start(self, record_id, variables=None):
        data = {"OWASP_CSRFTOKEN": self.csrf(), "recordId": record_id}
        for k, v in (variables or {}).items():
            data["var_" + k] = v
        r = self.s.post(f"{BASE}/web/json/workflow/process/start/{PROC}", data=data)
        try:
            return r.json()
        except Exception:
            return {"raw": r.text[:200], "status": r.status_code}

    def assignments(self):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        return [data] if isinstance(data, dict) else data  # single item = dict

    def complete(self, activity_id):
        self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{activity_id}",
                    data={"OWASP_CSRFTOKEN": self.csrf()})
        r = self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{activity_id}",
                        data={"OWASP_CSRFTOKEN": self.csrf()})
        try:
            return r.json()
        except Exception:
            return {"raw": r.text[:200], "status": r.status_code}

    def force_complete(self, process_id, activity_id):
        r = self.s.post(f"{BASE}/web/json/monitoring/running/activity/complete",
                        data={"OWASP_CSRFTOKEN": self.csrf(),
                              "processId": process_id, "activityId": activity_id})
        return r.status_code

    def complete_for_case(self, activity_substr, process_id=None):
        for a in self.assignments():
            aid = a.get("activityId") or a.get("id", "")
            name = (a.get("activityName") or "") + aid
            if activity_substr.lower() in name.lower() and \
                    (process_id is None or a.get("processId", "") == process_id):
                return self.complete(aid)
        return None


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def set_officer_pool(key, active):
    """F03 interaction: with active officer profiles, AllocationEngine assigns
    cases away from admin and the envelope's manual steps leave admin's inbox.
    F02 runs with the pool deactivated (cases stay with the start variable)."""
    for code, unit, skills in [("officer1", "UNIT-A", "VAT;IT"), ("officer2", "UNIT-B", "VAT")]:
        form_post("mdOfficerProfile", {"id": code, "code": code, "name": code,
                                       "unit": unit, "taxTypes": skills,
                                       "maxCapacity": "2", "active": active}, key)


def cleanup(key):
    """Fixture reset: wipe case/event rows (DEV teardown — documented P3
    exception, test rows only); restore seed transition via API upsert."""
    sql("DELETE FROM app_fd_cmevent")
    sql("DELETE FROM app_fd_cmcase")
    set_officer_pool(key, "")
    form_post("mmTransition", {"id": "TEST-OPEN-CLOSED", "caseType": "TEST",
                               "fromState": "OPEN", "toState": "CLOSED", "version": "1"}, key)


def main():
    key = api_key()
    assert key, "no api key"
    jg = Joget()
    cleanup(key)

    # ---------- T-02.1: creation, caseRef, events ----------
    form_post("cmCase", {"id": cid(1), "caseType": "TEST", "tin": "100058G",
                         "origin": "MANUAL"}, key)
    r = jg.start(cid(1), {"caseId": cid(1), "assignee": "admin"})
    print("start case 1:", json.dumps(r)[:140])
    time.sleep(2)
    c = jg.complete_for_case("openCase", r.get("processId"))
    print("complete openCase:", json.dumps(c)[:140] if c else "NO ASSIGNMENT")
    time.sleep(3)
    row = sql(f"SELECT c_caseref || '|' || c_currentstate FROM app_fd_cmcase WHERE id='{cid(1)}'")
    check("T-02.1 caseRef+state", row == "TT-000001|OPEN", row)
    n = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(1)}' "
            "AND c_eventtype IN ('CASE_CREATED','CASE_OPENED')")
    check("T-02.1 events created+opened", n == "2", n)

    # ---------- T-02.1 dedup ----------
    form_post("cmCase", {"id": cid(2), "caseType": "TEST", "tin": "100058G",
                         "origin": "MANUAL"}, key)
    r2 = jg.start(cid(2), {"caseId": cid(2), "assignee": "admin"})
    time.sleep(2)
    jg.complete_for_case("openCase", r2.get("processId"))
    time.sleep(3)
    rej = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(2)}' "
              "AND c_eventtype='TRANSITION_REJECTED' AND c_payload LIKE '%duplicate%'")
    st = sql(f"SELECT coalesce(c_currentstate,'') FROM app_fd_cmcase WHERE id='{cid(2)}'")
    check("T-02.1 duplicate rejected", rej == "1" and st == "", f"rej={rej} state={st}")

    # ---------- T-02.2: closure transition removed -> rejected ----------
    form_post("cmCase", {"id": cid(3), "caseType": "TEST", "tin": "100061K",
                         "origin": "MANUAL"}, key)
    r3 = jg.start(cid(3), {"caseId": cid(3), "assignee": "admin"})
    time.sleep(2)
    jg.complete_for_case("openCase", r3.get("processId"))
    time.sleep(3)
    form_post("mmTransition", {"id": "TEST-OPEN-CLOSED", "caseType": "TEST",
                               "fromState": "OPEN", "toState": "NOWHERE", "version": "1"}, key)
    jg.complete_for_case("workInProgress", r3.get("processId"))
    time.sleep(3)
    rej3 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(3)}' "
               "AND c_eventtype='TRANSITION_REJECTED'")
    st3 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{cid(3)}'")
    check("T-02.2 invalid transition rejected, state unchanged",
          rej3 == "1" and st3 == "OPEN", f"rej={rej3} state={st3}")
    form_post("mmTransition", {"id": "TEST-OPEN-CLOSED", "caseType": "TEST",
                               "fromState": "OPEN", "toState": "CLOSED", "version": "1"}, key)

    # ---------- T-02.4: TTT scope ----------
    form_post("mmCaseType", {"id": "OBLIG", "code": "OBLIG", "name": "Obligation test type",
                             "owningBb": "CMBB", "idFormat": "OB-??????",
                             "ttScope": "OBLIGATION", "active": "true"}, key)
    for st_id, code, env, term in [("OBLIG-NEW", "NEW", "New", ""),
                                   ("OBLIG-OPEN", "OPEN", "Open", ""),
                                   ("OBLIG-CLOSED", "CLOSED", "Closed", "true")]:
        form_post("mmState", {"id": st_id, "caseType": "OBLIG", "code": code,
                              "name": code.title(), "envelopeState": env,
                              "isTerminal": term, "version": "1", "active": "true"}, key)
    form_post("mmTransition", {"id": "OBLIG-NEW-OPEN", "caseType": "OBLIG",
                               "fromState": "NEW", "toState": "OPEN", "version": "1"}, key)
    form_post("cmCase", {"id": cid(4), "caseType": "OBLIG", "tin": "100058G",
                         "origin": "MANUAL"}, key)  # no taxType/taxPeriod
    r4 = jg.start(cid(4), {"caseId": cid(4), "assignee": "admin"})
    time.sleep(2)
    jg.complete_for_case("openCase", r4.get("processId"))
    time.sleep(3)
    rej4 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(4)}' "
               "AND c_eventtype='TRANSITION_REJECTED' AND c_payload LIKE '%tax type and period%'")
    check("T-02.4 TTT scope enforced", rej4 == "1", rej4)

    # ---------- T-02.5: close case 1 fully, then re-open ----------
    jg.complete_for_case("workInProgress", r.get("processId"))
    time.sleep(3)
    done = jg.complete_for_case("approveClosure", r.get("processId"))
    if done is None:
        act = sql("SELECT activityid FROM shkactivities WHERE processid LIKE '%cmCaseEnvelope%' "
                  "ORDER BY activated DESC LIMIT 1")
        jg.force_complete(r.get("processId"), act)
    time.sleep(3)
    st1 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{cid(1)}'")
    closed = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(1)}' "
                 "AND c_eventtype='CASE_CLOSED'")
    check("T-02.5a case closed", st1 == "CLOSED" and closed == "1",
          f"state={st1} closed={closed}")

    form_post("mmTransition", {"id": "TEST-CLOSED-OPEN", "caseType": "TEST",
                               "fromState": "CLOSED", "toState": "OPEN", "version": "1"}, key)
    r5 = jg.start(cid(1), {"caseId": cid(1), "assignee": "admin"})
    time.sleep(2)
    jg.complete_for_case("openCase", r5.get("processId"))
    time.sleep(3)
    reop = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid(1)}' "
               "AND c_eventtype='CASE_REOPENED' AND c_payload LIKE '%TT-000001%'")
    check("T-02.5b re-open linked", reop == "1", reop)

    # ---------- T-02.3: hash chain over all of case 1's events ----------
    rows = sql(f"SELECT id || '|' || c_prevhash || '|' || c_hash FROM app_fd_cmevent "
               f"WHERE c_caseid='{cid(1)}' ORDER BY c_seq")
    ok, prev = True, ""
    lines = rows.splitlines() if rows else []
    for line in lines:
        rid, prevhash, h = line.split("|")
        payload = sql(f"SELECT c_payload FROM app_fd_cmevent WHERE id='{rid}'")
        if prevhash != prev:
            ok = False
        if hashlib.sha256((payload + prevhash).encode()).hexdigest() != h:
            ok = False
        prev = h
    check("T-02.3 hash chain intact + recomputable", ok and len(lines) >= 4,
          f"{len(lines)} events")

    set_officer_pool(key, "true")  # restore for F03/F04 runs
    print()
    failed = [name for name, c, _ in RESULTS if not c]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
