#!/usr/bin/env python3
"""CMBB-F05 acceptance T-05.1..5 — live jdx9.
Flow: open a TEST case through the envelope (clocks created by guardOpen
chain), then time-travel sweeps via cmSweepRun rows (asOf field), asserting
clock states, escalation events and queue integration with read-only SQL.

Usage: JDX9_PASSWORD=admin run_t05.py
"""
import datetime
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
CASE = f"sla-{RUN}-1"


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


KEY = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")


def form_post(form_id, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form_id}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("Accept", "application/json"),
                 ("api_id", "API-cmbb-data"), ("api_key", KEY)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = {}
        try:
            errs = json.loads(body).get("errors") or {}
        except ValueError:
            pass
        if errs:
            raise SystemExit(f"form API validation error on {form_id}: {body[:300]}")


def sweep(n, as_of):
    rid = f"sw-{RUN}-{n}"
    form_post("cmSweepRun", {"id": rid, "asOf": as_of})
    time.sleep(4)
    return sql(f"SELECT c_result FROM app_fd_cmsweeprun WHERE id='{rid}'")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


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
                              "var_caseId": record_id, "var_assignee": "admin"})
        return r.json()

    def complete_open_case(self, process_id):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        for a in ([data] if isinstance(data, dict) else data):
            aid = a.get("activityId") or a.get("id", "")
            if "openCase" in ((a.get("activityName") or "") + aid) \
                    and a.get("processId", "") == process_id:
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return True
        return False


def iso(dt):
    return dt.strftime("%Y-%m-%dT%H:%M:%S")


def main():
    assert KEY, "no api key"
    jg = Joget()
    sql("DELETE FROM app_fd_cmevent")
    sql("DELETE FROM app_fd_cmcase")
    sql("DELETE FROM app_fd_cmdeadline") if sql(
        "SELECT to_regclass('app_fd_cmdeadline')") else None
    now = datetime.datetime.now().replace(microsecond=0)

    # ---------- T-05.1: clocks created at case open ----------
    form_post("cmCase", {"id": CASE, "caseType": "TEST", "tin": "100058G",
                         "origin": "MANUAL", "taxType": "VAT",
                         "taxpayerName": "Alpha Ltd", "amountAtStake": "500"})
    r = jg.start(CASE)
    time.sleep(2)
    jg.complete_open_case(r.get("processId"))
    time.sleep(4)
    clock = sql(f"SELECT c_clockcode || '|' || c_status FROM app_fd_cmdeadline "
                f"WHERE c_caseid='{CASE}'")
    sla = sql(f"SELECT c_slastatus FROM app_fd_cmcase WHERE id='{CASE}'")
    check("T-05.1 clock created from mmSla config", clock == "RES|RUNNING" and sla == "GREEN",
          f"{clock} sla={sla}")

    # ---------- T-05.2: traffic lights + escalation via time-travel sweeps ----------
    res = sweep(1, iso(now + datetime.timedelta(days=8)))  # 80% >= warn 75
    amber = sql(f"SELECT c_slastatus FROM app_fd_cmcase WHERE id='{CASE}'")
    check("T-05.2a amber at warn threshold", amber == "AMBER", f"{amber} ({res})")

    res = sweep(2, iso(now + datetime.timedelta(days=9, hours=14)))  # >= crit 90
    red = sql(f"SELECT c_slastatus FROM app_fd_cmcase WHERE id='{CASE}'")
    lvl = sql(f"SELECT c_escalationlevel FROM app_fd_cmdeadline WHERE c_caseid='{CASE}'")
    esc = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{CASE}' "
              "AND c_eventtype='SLA_ESCALATED'")
    notices = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{CASE}' "
                  "AND c_eventtype='NOTIF_PENDING'")
    check("T-05.2b crit escalation L1 + notices to officer & supervisor",
          red == "RED" and lvl == "1" and esc == "1" and notices == "2",
          f"sla={red} lvl={lvl} esc={esc} notices={notices} ({res})")

    res = sweep(3, iso(now + datetime.timedelta(days=11)))  # breach
    st = sql(f"SELECT c_status || '|' || c_escalationlevel FROM app_fd_cmdeadline "
             f"WHERE c_caseid='{CASE}'")
    br = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{CASE}' "
             "AND c_eventtype='SLA_BREACHED'")
    check("T-05.2c breach L2, fired once", st == "BREACHED|2" and br == "1",
          f"{st} breached_events={br} ({res})")

    res = sweep(4, iso(now + datetime.timedelta(days=12)))  # idempotency
    br2 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{CASE}' "
              "AND c_eventtype IN ('SLA_BREACHED','SLA_ESCALATED')")
    red2 = sql(f"SELECT c_slastatus FROM app_fd_cmcase WHERE id='{CASE}'")
    check("T-05.2d no duplicate escalation; red persists", br2 == "2" and red2 == "RED",
          f"escalations={br2} sla={red2} ({res})")

    # ---------- T-05.3: pause on hold, resume extends ----------
    form_post("mmState", {"id": "TEST-HOLD", "caseType": "TEST", "code": "HOLD",
                          "name": "On hold", "envelopeState": "OnHold",
                          "version": "1", "active": "true"})
    case2 = f"sla-{RUN}-2"
    form_post("cmCase", {"id": case2, "caseType": "TEST", "tin": "100061K",
                         "origin": "MANUAL", "taxType": "VAT"})
    r2 = jg.start(case2)
    time.sleep(2)
    jg.complete_open_case(r2.get("processId"))
    time.sleep(4)
    due0 = sql(f"SELECT c_dueat FROM app_fd_cmdeadline WHERE c_caseid='{case2}'")
    form_post("cmCase", {"id": case2, "caseType": "TEST", "tin": "100061K",
                         "origin": "MANUAL", "taxType": "VAT", "currentState": "HOLD",
                         "assignee": "admin", "assignmentStatus": "ASSIGNED"})
    sweep(5, iso(now + datetime.timedelta(days=2)))
    paused = sql(f"SELECT c_status FROM app_fd_cmdeadline WHERE c_caseid='{case2}'")
    form_post("cmCase", {"id": case2, "caseType": "TEST", "tin": "100061K",
                         "origin": "MANUAL", "taxType": "VAT", "currentState": "OPEN",
                         "assignee": "admin", "assignmentStatus": "ASSIGNED"})
    sweep(6, iso(now + datetime.timedelta(days=5)))
    resumed = sql(f"SELECT c_status || '|' || c_pauseddays FROM app_fd_cmdeadline "
                  f"WHERE c_caseid='{case2}'")
    due1 = sql(f"SELECT c_dueat FROM app_fd_cmdeadline WHERE c_caseid='{case2}'")
    check("T-05.3 pause on hold + resume extends due",
          paused == "PAUSED" and resumed == "RUNNING|3" and due1 > due0,
          f"paused={paused} resumed={resumed} due {due0} -> {due1}")

    # ---------- T-05.4: queue integration ----------
    qsql = json.loads(sql("SELECT json FROM app_datalist WHERE id='list_queue' "
                          "AND appid='cmbb'"))["binder"]["properties"]["sql"]
    row = sql(f"SELECT q.sla_status || '|' || q.sla_due FROM ({qsql}) q WHERE q.id='{CASE}'")
    check("T-05.4 queue shows SLA status + countdown source",
          row.startswith("RED|") and "T" in row, row)

    print()
    failed = [n for n, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
