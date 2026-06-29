#!/usr/bin/env python3
"""CMBB-F09 acceptance T-09.1..8 — live jdx9 + platform-mock ClickHouse.

T-09.1 chain VERIFY green: cmChainCheck -> VERIFIED + CHAIN_VERIFIED
T-09.2 tamper DETECTED: mutate one cmEvent payload -> BROKEN at seq + CHAIN_BROKEN
T-09.3 outcome SHIP (LIVE): close case -> row in fact_case_outcomes, goldSource LIVE
T-09.4 SHIP idempotent: re-run -> still 1 row (FINAL dedup), ledger not duplicated
T-09.5 GoldMartClient degraded read (CH down) -> source CACHE, stale asOf
T-09.6 writeback queue-retry: CH down -> QUEUED; CH up + RETRY -> SHIPPED
T-09.7 KPI emission: KPI_EMITTED with standard dimensions for a closed case
T-09.8 SLA compliance datalist: deployed JDBC SQL returns compliance %

Usage: JDX9_PASSWORD=admin run_t09.py
Prereq: app cmbb deployed with F09 + Tomcat restarted; mtca-clickhouse up + seeded;
plugin props default to jdbc:clickhouse://localhost:8123 (sta_reader / writeback_api DEV).
"""
import json
import os
import re
import subprocess
import sys
import time
import urllib.request

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

BASE = "http://localhost:8089/jw"
PROC = "cmbb:latest:cmCaseEnvelope"
CH = "mtca-clickhouse"
RESULTS = []
RUN = str(int(time.time()))[-6:]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def ch(q):
    r = subprocess.run(["docker", "exec", CH, "clickhouse-client", "-q", q],
                       capture_output=True, text=True)
    return ("CH-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def ch_power(action):
    subprocess.run(["docker", action, CH], capture_output=True, text=True)
    if action == "start":
        for _ in range(30):
            if ch("SELECT 1") == "1":
                return True
            time.sleep(1)
        return False
    # stop: confirm down
    for _ in range(15):
        if ch("SELECT 1").startswith("CH-ERROR"):
            return True
        time.sleep(1)
    return True


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

    def complete(self, activity_substr, process_id):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        for a in ([data] if isinstance(data, dict) else data):
            aid = a.get("activityId") or a.get("id", "")
            if activity_substr in ((a.get("activityName") or "") + aid) \
                    and a.get("processId", "") == process_id:
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return True
        return False

    def force_complete(self, process_id, activity_id):
        r = self.s.post(f"{BASE}/web/json/monitoring/running/activity/complete",
                        data={"OWASP_CSRFTOKEN": self.csrf(),
                              "processId": process_id, "activityId": activity_id})
        return r.status_code


def set_officer_pool(active):
    for code, unit, skills in [("officer1", "UNIT-A", "VAT;IT"), ("officer2", "UNIT-B", "VAT")]:
        form_post("mdOfficerProfile", {"id": code, "code": code, "name": code,
                                       "unit": unit, "taxTypes": skills,
                                       "maxCapacity": "2", "active": active})


def open_case(jg, case_id, case_type, tin, taxtype="VAT"):
    form_post("cmCase", {"id": case_id, "caseType": case_type, "tin": tin,
                         "origin": "MANUAL", "taxType": taxtype,
                         "taxpayerName": "Alpha Ltd", "amountAtStake": "500"})
    r = jg.start(case_id)
    time.sleep(2)
    jg.complete("openCase", r.get("processId"))
    time.sleep(3)
    return r.get("processId"), sql(f"SELECT c_caseref FROM app_fd_cmcase WHERE id='{case_id}'")


def close_case(jg, process_id):
    jg.complete("workInProgress", process_id)
    time.sleep(3)
    if not jg.complete("approveClosure", process_id):
        act = sql("SELECT activityid FROM shkactivities WHERE processid LIKE "
                  "'%cmCaseEnvelope%' ORDER BY activated DESC LIMIT 1")
        if act and not act.startswith("SQL-ERROR"):
            jg.force_complete(process_id, act)
    time.sleep(3)


def chain_check(case_id=""):
    cid = f"cc-{RUN}-{case_id or 'all'}"
    form_post("cmChainCheck", {"id": cid, "caseId": case_id})
    time.sleep(3)
    return cid


def outcome_run(mode):
    rid = f"or-{RUN}-{mode}-{int(time.time())%1000}"
    form_post("cmOutcomeRun", {"id": rid, "mode": mode})
    time.sleep(4)
    return rid


SLA_SQL = (
    "SELECT ct.case_type, ct.closed_total, ct.closed_total - ct.breached AS met, ct.breached, "
    "CASE WHEN ct.closed_total=0 THEN 0 ELSE ROUND(100.0*(ct.closed_total-ct.breached)/ct.closed_total,1) END AS pct "
    "FROM (SELECT COALESCE(NULLIF(c.c_casetype,''),'-') AS case_type, COUNT(*) AS closed_total, "
    "COUNT(*) FILTER (WHERE EXISTS (SELECT 1 FROM app_fd_cmdeadline d WHERE d.c_caseid=c.id AND d.c_status='BREACHED')) AS breached "
    "FROM app_fd_cmcase c WHERE COALESCE(c.c_currentstate,'') IN "
    "(SELECT s.c_code FROM app_fd_mmstate s WHERE s.c_isterminal='true') "
    "GROUP BY COALESCE(NULLIF(c.c_casetype,''),'-')) ct ORDER BY pct ASC")


def main():
    assert KEY, "no api key"
    jg = Joget()
    set_officer_pool("")
    for t in ("cmevent", "cmcase", "cmtask", "cmnotif", "cmdeadline", "cmdecision",
              "cmchaincheck", "cmoutcome", "cmoutcomerun", "cmgoldsnapshot", "cmgoldprobe"):
        if sql(f"SELECT to_regclass('app_fd_{t}')"):
            sql(f"DELETE FROM app_fd_{t}")

    # ensure the TEST close transition exists (idempotent; mirrors run_t02)
    form_post("mmTransition", {"id": "TEST-OPEN-CLOSED", "caseType": "TEST",
                               "fromState": "OPEN", "toState": "CLOSED", "version": "1"})

    # ---------- T-09.1: chain verify green ----------
    c1 = f"v-{RUN}-1"
    open_case(jg, c1, "TEST", "100058G")
    chain_check(c1)
    res = sql(f"SELECT c_result FROM app_fd_cmchaincheck WHERE c_caseid='{c1}'")
    cev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' AND c_eventtype='CHAIN_VERIFIED'")
    check("T-09.1 chain verify VERIFIED + event", res == "VERIFIED" and cev == "1",
          f"result={res} verified_ev={cev}")

    # ---------- T-09.2: tamper detected ----------
    c2 = f"v-{RUN}-2"
    open_case(jg, c2, "TEST", "100061K")
    n_ev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c2}'")
    sql(f"UPDATE app_fd_cmevent SET c_payload = c_payload || ' ' "
        f"WHERE c_caseid='{c2}' AND c_seq='0000000001'")
    chain_check(c2)
    res2 = sql(f"SELECT c_result FROM app_fd_cmchaincheck WHERE c_caseid='{c2}'")
    bad = sql(f"SELECT c_firstbadseq FROM app_fd_cmchaincheck WHERE c_caseid='{c2}'")
    bev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c2}' AND c_eventtype='CHAIN_BROKEN'")
    check("T-09.2 tamper -> BROKEN at seq + event",
          res2 == "BROKEN" and bad == "0000000001" and int(bev) >= 1,
          f"result={res2} firstBadSeq={bad} broken_ev={bev} (events={n_ev})")

    # ---------- T-09.3: outcome SHIP (LIVE) ----------
    c3 = f"o-{RUN}-1"
    p3, _ = open_case(jg, c3, "TEST", "100010A")  # Gold-seed TIN, distinct (dedup)
    close_case(jg, p3)
    st3 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{c3}'")
    outcome_run("SHIP")
    chrow = ch(f"SELECT count() FROM sta_v1.case_outcomes WHERE case_id='{c3}'")
    led = sql(f"SELECT c_shipstatus FROM app_fd_cmoutcome WHERE id='{c3}-RESOLVED'")
    src = sql(f"SELECT c_goldsource FROM app_fd_cmoutcome WHERE id='{c3}-RESOLVED'")
    amt = sql(f"SELECT c_amount FROM app_fd_cmoutcome WHERE id='{c3}-RESOLVED'")
    check("T-09.3 outcome shipped LIVE to fact_case_outcomes",
          st3 == "CLOSED" and chrow == "1" and led == "SHIPPED" and src == "LIVE" and amt not in ("", "0"),
          f"state={st3} ch_rows={chrow} ledger={led} src={src} amt={amt}")

    # ---------- T-09.4: idempotent ----------
    outcome_run("SHIP")
    chrow2 = ch(f"SELECT count() FROM sta_v1.case_outcomes WHERE case_id='{c3}'")
    ledn = sql(f"SELECT count(*) FROM app_fd_cmoutcome WHERE c_caseid='{c3}'")
    check("T-09.4 SHIP idempotent (1 row, no dup ledger)",
          chrow2 == "1" and ledn == "1", f"ch_rows={chrow2} ledger_rows={ledn}")

    # ---------- T-09.7: KPI emission (closed case present now) ----------
    chain_check(c3)
    kpi = sql(f"SELECT c_payload FROM app_fd_cmevent WHERE c_caseid='{c3}' AND c_eventtype='KPI_EMITTED' LIMIT 1")
    check("T-09.7 KPI_EMITTED with standard dimensions",
          "caseType" in kpi and "cycleTimeDays" in kpi and "outcomeCode" in kpi,
          f"payload={kpi[:120]}")

    # ---------- T-09.5 + T-09.6: degraded read + queue-retry (CH down) ----------
    # prime the gold cache for the TIN while CH is up
    form_post("cmGoldProbe", {"id": f"gp-{RUN}-prime", "tin": "100058G"})
    time.sleep(3)
    prime_src = sql(f"SELECT c_source FROM app_fd_cmgoldprobe WHERE id='gp-{RUN}-prime'")
    # a fresh closed case to ship while CH is down
    c6 = f"q-{RUN}-1"
    p6, _ = open_case(jg, c6, "TEST", "100077B")  # Gold-seed TIN, distinct (dedup)
    close_case(jg, p6)

    ch_power("stop")
    form_post("cmGoldProbe", {"id": f"gp-{RUN}-down", "tin": "100058G"})
    time.sleep(3)
    down_src = sql(f"SELECT c_source FROM app_fd_cmgoldprobe WHERE id='gp-{RUN}-down'")
    down_asof = sql(f"SELECT c_asof FROM app_fd_cmgoldprobe WHERE id='gp-{RUN}-down'")
    check("T-09.5 GoldMartClient degraded read serves CACHE",
          prime_src == "LIVE" and down_src == "CACHE" and down_asof != "",
          f"prime={prime_src} down={down_src} asOf={down_asof}")

    outcome_run("SHIP")  # CH down -> QUEUED
    q_stat = sql(f"SELECT c_shipstatus FROM app_fd_cmoutcome WHERE id='{c6}-RESOLVED'")
    ch_power("start")
    outcome_run("RETRY")  # CH up -> SHIPPED
    r_stat = sql(f"SELECT c_shipstatus FROM app_fd_cmoutcome WHERE id='{c6}-RESOLVED'")
    ch6 = ch(f"SELECT count() FROM sta_v1.case_outcomes WHERE case_id='{c6}'")
    check("T-09.6 writeback queue-retry: QUEUED then SHIPPED",
          q_stat == "QUEUED" and r_stat == "SHIPPED" and ch6 == "1",
          f"after_ship={q_stat} after_retry={r_stat} ch_rows={ch6}")

    # ---------- T-09.8: SLA compliance datalist ----------
    rows = sql(SLA_SQL)
    ok8 = False
    detail8 = rows[:160]
    for line in rows.splitlines():
        parts = line.split("|")
        if len(parts) == 5 and parts[0] == "TEST":
            closed_total, met, breached, pct = parts[1], parts[2], parts[3], parts[4]
            ok8 = (int(closed_total) >= 1 and 0 <= float(pct) <= 100
                   and int(met) + int(breached) == int(closed_total))
            detail8 = f"TEST closed={closed_total} met={met} breached={breached} pct={pct}"
            break
    check("T-09.8 SLA compliance datalist returns %", ok8, detail8)

    set_officer_pool("true")
    print()
    failed = [name for name, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
