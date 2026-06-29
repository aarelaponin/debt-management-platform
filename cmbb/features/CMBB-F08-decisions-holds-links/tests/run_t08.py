#!/usr/bin/env python3
"""CMBB-F08 acceptance T-08.1..8 — live jdx9.

T-08.1 hold ASSERT: cmHold -> ACTIVE + HOLD_ASSERTED
T-08.2 suppression: active CORRESPONDENCE_SUPPRESS -> cmNotif SUPPRESSED
T-08.3 hold RELEASE: cmHoldRelease -> RELEASED + dispatch resumes (SENT)
T-08.4 closure decision gate: TESTD (requireDecision) blocked w/o decision, closes w/ approved
T-08.5 authority insufficient: WRITE_OFF above band -> DECISION_REJECTED
T-08.6 case linkage: permitted -> reciprocal + CASE_LINKED; impermissible -> REJECTED
T-08.7 decision-maker independence: EXCLUDE_DECISION_MAKER excludes prior approver
T-08.8 pending-info loop: request parks + task; response resumes + closes task

Usage: JDX9_PASSWORD=admin run_t08.py
Seeds required first (load_md_seed.py on the F08 seed dir): mmAuthority, mmHoldPolicy,
mmLinkType, mdHoldType, mdDecisionType, mmCaseType(TESTD), mmState(TESTD), mmTransition(TESTD).
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
RESULTS = []
RUN = str(int(time.time()))[-6:]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


KEY = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")


def drain(max_s=45):
    """Order-independence guard: wait until no envelope process is still running before this
    suite resets, so a prior suite's still-dispatching notices / late events can't bleed into
    T-08.2/T-08.3's suppression-then-resume assertions. Cold runs return at once."""
    stable = 0
    for _ in range(max_s):
        if sql("SELECT count(*) FROM app_report_process_instance WHERE finishtime IS NULL") == "0":
            stable += 1
            if stable >= 2:
                return
        else:
            stable = 0
        time.sleep(1)


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


def close_case(jg, process_id):
    """Drive a case through closure: workInProgress -> approveClosure -> guardFinal
    (CLOSE). Mirrors run_t02 T-02.5; force_complete is the fallback when the
    approveClosure assignment isn't in the inbox (officer pool inactive keeps it
    with admin)."""
    jg.complete("workInProgress", process_id)
    time.sleep(3)
    if not jg.complete("approveClosure", process_id):
        act = sql("SELECT activityid FROM shkactivities WHERE processid LIKE "
                  "'%cmCaseEnvelope%' ORDER BY activated DESC LIMIT 1")
        if act and not act.startswith("SQL-ERROR"):
            jg.force_complete(process_id, act)
    time.sleep(3)


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


def main():
    assert KEY, "no api key"
    jg = Joget()
    drain()  # let any prior suite's envelopes finish before we reset (order-independence)
    set_officer_pool("")  # keep cases in admin inbox for lifecycle steps
    for t in ("cmevent", "cmcase", "cmtask", "cmnotif", "cmalert",
              "cmhold", "cmholdrelease", "cmdecision", "cmlink",
              "cminforequest", "cminforesponse"):
        if sql(f"SELECT to_regclass('app_fd_{t}')"):
            sql(f"DELETE FROM app_fd_{t}")

    # notification rule so a NOTIF_PENDING actually dispatches a taxpayer notice
    form_post("mmNotifRule", {"id": f"NR-{RUN}", "caseType": "TEST",
                              "eventType": "NOTIF_PENDING", "template": "TPL-TEST",
                              "channelDefault": "EMAIL"})

    case = f"h-{RUN}-1"
    _, ref = open_case(jg, case, "TEST", "100058G")

    # ---------- T-08.1: hold assert ----------
    hold_id = f"hold-{RUN}-1"
    form_post("cmHold", {"id": hold_id, "caseId": case, "caseRef": ref, "tin": "100058G",
                         "holdType": "INFO_PENDING", "scope": "CORRESPONDENCE_SUPPRESS",
                         "basis": "audit dispute", "targetBB": "CMBB"})
    time.sleep(3)
    hstat = sql(f"SELECT c_status FROM app_fd_cmhold WHERE id='{hold_id}'")
    hev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' "
              "AND c_eventtype='HOLD_ASSERTED'")
    check("T-08.1 hold asserted ACTIVE + event", hstat == "ACTIVE" and hev == "1",
          f"status={hstat} events={hev}")

    # ---------- T-08.2: suppression blocks dispatch ----------
    form_post("cmInfoRequest", {"id": f"ir-{RUN}-1", "caseId": case, "caseRef": ref,
                                "tin": "100058G", "infoNeeded": "bank statements"})
    time.sleep(3)
    form_post("cmDispatchRun", {"id": f"disp-{RUN}-1", "mode": "DISPATCH"})
    time.sleep(3)
    supp = sql(f"SELECT count(*) FROM app_fd_cmnotif WHERE c_caseid='{case}' "
               "AND c_status='SUPPRESSED'")
    sent_during_hold = sql(f"SELECT count(*) FROM app_fd_cmnotif WHERE c_caseid='{case}' "
                           "AND c_status='SENT'")
    check("T-08.2 active suppression blocks correspondence",
          int(supp) >= 1 and sent_during_hold == "0", f"suppressed={supp} sent={sent_during_hold}")

    # ---------- T-08.3: release resumes dispatch ----------
    form_post("cmHoldRelease", {"id": f"hr-{RUN}-1", "holdId": hold_id, "caseId": case,
                                "caseRef": ref, "releaseReason": "dispute resolved"})
    time.sleep(3)
    rstat = sql(f"SELECT c_status FROM app_fd_cmhold WHERE id='{hold_id}'")
    rev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' "
              "AND c_eventtype='HOLD_RELEASED'")
    form_post("cmInfoRequest", {"id": f"ir-{RUN}-2", "caseId": case, "caseRef": ref,
                                "tin": "100058G", "infoNeeded": "more"})
    time.sleep(3)
    form_post("cmDispatchRun", {"id": f"disp-{RUN}-2", "mode": "DISPATCH"})
    time.sleep(3)
    sent_after = sql(f"SELECT count(*) FROM app_fd_cmnotif WHERE c_caseid='{case}' "
                     "AND c_status='SENT'")
    check("T-08.3 release lifts suppression, dispatch resumes",
          rstat == "RELEASED" and rev == "1" and int(sent_after) >= 1,
          f"status={rstat} released_ev={rev} sent_after={sent_after}")

    # ---------- T-08.4: closure decision gate (TESTD) ----------
    # case A: no decision -> closure blocked at guardFinal (CLOSE)
    ca = f"d-{RUN}-A"
    pa, _ = open_case(jg, ca, "TESTD", "100061K")
    close_case(jg, pa)
    time.sleep(2)
    blocked = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{ca}' "
                  "AND c_eventtype='TRANSITION_REJECTED' AND c_payload LIKE '%decision%'")
    # case B: approved decision first -> closure proceeds
    cb = f"d-{RUN}-B"
    pb, refb = open_case(jg, cb, "TESTD", "100062L")
    form_post("cmDecision", {"id": f"dec-{RUN}-B", "caseId": cb, "caseRef": refb, "tin": "100062L",
                             "decisionType": "CLOSURE", "actionType": "CLOSE_CASE", "amount": "0",
                             "outcome": "ACCEPT", "approverLevel": "OFFICER", "decidedBy": "admin",
                             "bodyType": "SINGLE", "reasons": "case resolved, ok to close"})
    time.sleep(3)
    dstat = sql(f"SELECT c_decisionstatus FROM app_fd_cmdecision WHERE id='dec-{RUN}-B'")
    close_case(jg, pb)
    time.sleep(2)
    bstate = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{cb}'")
    closed = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cb}' "
                 "AND c_eventtype='CASE_CLOSED'")
    check("T-08.4 closure gate: blocked w/o decision, closes w/ approved",
          blocked == "1" and dstat == "APPROVED" and bstate == "CLOSED" and closed == "1",
          f"blocked={blocked} decision={dstat} state={bstate} closed={closed}")

    # ---------- T-08.5: authority insufficient ----------
    form_post("cmDecision", {"id": f"dec-{RUN}-WO", "caseId": case, "caseRef": ref, "tin": "100058G",
                             "decisionType": "WRITE_OFF", "actionType": "WRITE_OFF", "amount": "50000",
                             "outcome": "ACCEPT", "approverLevel": "SUPERVISOR", "decidedBy": "officer1",
                             "bodyType": "SINGLE", "reasons": "request write-off"})
    time.sleep(3)
    wstat = sql(f"SELECT c_decisionstatus FROM app_fd_cmdecision WHERE id='dec-{RUN}-WO'")
    wrej = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' "
               "AND c_eventtype='DECISION_REJECTED'")
    check("T-08.5 insufficient authority rejected",
          wstat == "REJECTED" and int(wrej) >= 1, f"status={wstat} rejected_ev={wrej}")

    # ---------- T-08.6: case linkage both-way + validation ----------
    tgt = f"lnk-{RUN}-T"
    _, tref = open_case(jg, tgt, "TEST", "100063M")
    form_post("cmLink", {"id": f"link-{RUN}-1", "fromCaseId": case, "fromCaseRef": ref,
                         "linkType": "REFERRAL", "toCaseRef": tref})
    time.sleep(3)
    lres = sql(f"SELECT c_result FROM app_fd_cmlink WHERE id='link-{RUN}-1'")
    recip = sql(f"SELECT count(*) FROM app_fd_cmlink WHERE c_result='RECIPROCAL' "
                f"AND c_fromcaseref='{tref}'")
    linked = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_eventtype='CASE_LINKED'")
    check("T-08.6 linkage writes reciprocal + events",
          lres == "OK" and recip == "1" and int(linked) >= 2,
          f"result={lres} reciprocal={recip} linked_ev={linked}")

    # ---------- T-08.7: decision-maker independence at allocation ----------
    set_officer_pool("true")
    form_post("mmCoi", {"id": f"COI-{RUN}", "caseType": "TEST",
                        "ruleType": "EXCLUDE_DECISION_MAKER", "expression": ""})
    form_post("cmDecision", {"id": f"dec-{RUN}-DM", "caseId": case, "caseRef": ref, "tin": "100099Z",
                             "decisionType": "CLOSURE", "actionType": "CLOSE_CASE", "amount": "0",
                             "outcome": "ACCEPT", "approverLevel": "OFFICER", "decidedBy": "officer1",
                             "bodyType": "SINGLE", "reasons": "prior decision by officer1"})
    time.sleep(3)
    cdm = f"dm-{RUN}-1"
    pdm, _ = open_case(jg, cdm, "TEST", "100099Z")
    time.sleep(2)
    assignee = sql(f"SELECT c_assignee FROM app_fd_cmcase WHERE id='{cdm}'")
    check("T-08.7 decision-maker excluded at allocation",
          assignee and assignee != "officer1", f"assignee={assignee} (expected not officer1)")
    set_officer_pool("")

    # ---------- T-08.8: pending-info loop ----------
    cp = f"pi-{RUN}-1"
    _, pref = open_case(jg, cp, "TEST", "100064N")
    form_post("cmInfoRequest", {"id": f"req-{RUN}-P", "caseId": cp, "caseRef": pref,
                                "tin": "100064N", "infoNeeded": "provide ledger"})
    time.sleep(3)
    task_open = sql(f"SELECT count(*) FROM app_fd_cmtask WHERE c_caseid='{cp}' "
                    "AND c_tasktype='PROVIDE_INFO' AND c_status='OPEN'")
    req_ev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cp}' "
                 "AND c_eventtype='INFO_REQUESTED'")
    form_post("cmInfoResponse", {"id": f"resp-{RUN}-P", "requestId": f"req-{RUN}-P",
                                 "caseId": cp, "caseRef": pref, "responseSummary": "ledger provided"})
    time.sleep(3)
    task_closed = sql(f"SELECT count(*) FROM app_fd_cmtask WHERE c_caseid='{cp}' "
                      "AND c_tasktype='PROVIDE_INFO' AND c_status='CLOSED'")
    recv_ev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cp}' "
                  "AND c_eventtype='INFO_RECEIVED'")
    req_status = sql(f"SELECT c_status FROM app_fd_cminforequest WHERE id='req-{RUN}-P'")
    check("T-08.8 pending-info request parks, response resumes",
          task_open == "1" and req_ev == "1" and task_closed == "1"
          and recv_ev == "1" and req_status == "RESPONDED",
          f"open={task_open} req_ev={req_ev} closed={task_closed} recv={recv_ev} status={req_status}")

    set_officer_pool("true")
    print()
    failed = [name for name, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
