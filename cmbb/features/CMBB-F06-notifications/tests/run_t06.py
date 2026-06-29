#!/usr/bin/env python3
"""CMBB-F06 acceptance T-06.1..5 — live jdx9.
Engines already emit NOTIF_PENDING; this run creates fresh ones (reassignment),
configures a taxpayer rule + bilingual templates, dispatches via cmDispatchRun
rows, and SQL-asserts alerts, rendered notifications, fallback and the log.

Usage: run_t06.py
"""
import json
import subprocess
import sys
import time
import urllib.request

BASE = "http://localhost:8089/jw"
RESULTS = []
RUN = str(int(time.time()))[-6:]


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
        try:
            errs = json.loads(body).get("errors") or {}
        except ValueError:
            errs = {}
        if errs:
            raise SystemExit(f"validation error on {form_id}: {body[:300]}")


def dispatch(n):
    rid = f"dr-{RUN}-{n}"
    form_post("cmDispatchRun", {"id": rid})
    time.sleep(4)
    return sql(f"SELECT c_result FROM app_fd_cmdispatchrun WHERE id='{rid}'")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    assert KEY, "no api key"
    for t in ("cmevent", "cmcase", "cmnotif", "cmalert"):
        if sql(f"SELECT to_regclass('app_fd_{t}')"):
            sql(f"DELETE FROM app_fd_{t}")

    # fixture: case + reassignment (emits CASE_REASSIGNED + 2 NOTIF_PENDING)
    case = f"nt-{RUN}-1"
    form_post("cmCase", {"id": case, "caseType": "TEST", "tin": "100058G",
                         "origin": "MANUAL", "currentState": "OPEN",
                         "taxpayerName": "Alpha Ltd", "amountAtStake": "500",
                         "caseRef": "TT-900001", "assignee": "officer1",
                         "assignmentStatus": "ASSIGNED"})
    form_post("cmReassign", {"id": f"ord-{RUN}", "caseRef": "TT-900001",
                             "toOfficer": "officer2", "reason": "coverage"})
    time.sleep(4)
    pend = sql("SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='NOTIF_PENDING'")
    check("fixture: NOTIF_PENDING emitted", pend == "2", pend)

    # taxpayer rule + bilingual templates (rule channels exercise fallback later)
    form_post("mmNotifRule", {"id": f"rule-{RUN}", "caseType": "TEST",
                              "eventType": "NOTIF_PENDING", "template": "TPL-TEST",
                              "channelDefault": "EMAIL"})

    # ---------- T-06.1: dispatch -> alerts + idempotency ----------
    r1 = dispatch(1)
    alerts = sql("SELECT count(*) FROM app_fd_cmalert WHERE c_isread IS DISTINCT FROM 'true'")
    check("T-06.1a internal alerts created (both reassignment parties)",
          alerts == "2", f"alerts={alerts} ({r1})")
    r2 = dispatch(2)
    alerts2 = sql("SELECT count(*) FROM app_fd_cmalert")
    check("T-06.1b idempotent re-dispatch", alerts2 == "2" and "skippedHandled=2" in r2,
          f"alerts={alerts2} ({r2})")

    # ---------- T-06.2: template render with merge fields ----------
    rendered = sql("SELECT c_summary FROM app_fd_cmnotif WHERE c_status='SENT' LIMIT 1")
    check("T-06.2 merge fields rendered (caseRef + taxpayer)",
          "TT-900001" in rendered and "Alpha Ltd" in rendered, rendered)

    # ---------- T-06.3: channel fallback ----------
    form_post("mmNotifRule", {"id": f"rule-{RUN}", "caseType": "TEST",
                              "eventType": "NOTIF_PENDING", "template": "TPL-TEST",
                              "channelDefault": "SIMFAIL,EMAIL"})
    form_post("cmReassign", {"id": f"ord2-{RUN}", "caseRef": "TT-900001",
                             "toOfficer": "officer1", "reason": "back"})
    time.sleep(4)
    dispatch(3)
    fb = sql("SELECT string_agg(c_channel || ':' || c_status, ',' ORDER BY datecreated) "
             "FROM app_fd_cmnotif n WHERE n.c_channel IN ('SIMFAIL','EMAIL') "
             "AND n.c_eventid IN (SELECT id FROM app_fd_cmevent WHERE "
             "c_payload LIKE '%ord2-" + RUN + "%' AND c_eventtype='NOTIF_PENDING') ")
    check("T-06.3 fallback after FAILED", "SIMFAIL:FAILED" in fb and "EMAIL:SENT" in fb, fb)

    # ---------- T-06.4: outbound log fields + linkage ----------
    log = sql("SELECT count(*) FROM app_fd_cmnotif WHERE c_tin='100058G' "
              "AND c_eventid <> '' AND c_sentat <> ''")
    check("T-06.4 log rows carry TIN, eventId, timestamp", int(log) >= 3, log)

    # ---------- T-06.5: alert panel definition + unread data ----------
    dl = sql("SELECT json FROM app_datalist WHERE id='list_cmAlert_my' AND appid='cmbb'")
    has_panel = "JdbcDataListBinder" in dl and "#currentUser.username#" in dl
    panel_sql = json.loads(dl)["binder"]["properties"]["sql"].replace(
        "#currentUser.username#", "officer2")
    unread = sql(f"SELECT count(*) FROM ({panel_sql}) q WHERE q.is_read = ''")
    check("T-06.5 alert panel scoped + unread badge data", has_panel and int(unread) >= 1,
          f"panel={has_panel} unread={unread}")

    print()
    failed = [n for n, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
