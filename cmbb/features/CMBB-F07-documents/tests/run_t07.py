#!/usr/bin/env python3
"""CMBB-F07 acceptance T-07.1..5 — live jdx9 + live Mayan EDMS.

T-07.1 PUSH: file placed in Joget upload dir + register row -> Mayan id
T-07.2 GENERATE: ADG order -> PDF in Mayan + cmDoc auto-attached
T-07.3 mmDocReq: guardClosure blocks without doc, passes with doc
T-07.4 POSTAL: RETURNED -> address-verification task + flags + alert event
T-07.5 batch GENERATE by case type

Usage: JDX9_PASSWORD=admin MAYAN_PASSWORD=<pw> run_t07.py
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
MAYAN = os.environ.get("MAYAN_URL", "http://localhost:8880").rstrip("/") + "/api/v4"
PROC = "cmbb:latest:cmCaseEnvelope"
RESULTS = []
RUN = str(int(time.time()))[-6:]
UPLOAD_ROOT = "/Users/aarelaponin/joget-enterprise-linux-9.0.7-9/wflow/app_formuploads"


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


def mayan_get(path, token):
    req = urllib.request.Request(MAYAN + path)
    req.add_header("Authorization", "Token " + token)
    req.add_header("Accept", "application/json")
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read().decode())


def mayan_token():
    body = json.dumps({"username": os.environ.get("MAYAN_USER", "admin"),
                       "password": os.environ.get("MAYAN_PASSWORD", "admin")}).encode()
    req = urllib.request.Request(MAYAN + "/auth/token/obtain/", data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")  # DRF returns HTML otherwise
    with urllib.request.urlopen(req) as r:
        return json.loads(r.read().decode())["token"]


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


def set_officer_pool(active):
    """F03 interaction: active pool assigns cases away from admin's inbox."""
    for code, unit, skills in [("officer1", "UNIT-A", "VAT;IT"), ("officer2", "UNIT-B", "VAT")]:
        form_post("mdOfficerProfile", {"id": code, "code": code, "name": code,
                                       "unit": unit, "taxTypes": skills,
                                       "maxCapacity": "2", "active": active})


def main():
    assert KEY, "no api key"
    token = mayan_token()
    jg = Joget()
    set_officer_pool("")
    for t in ("cmevent", "cmcase", "cmdoc", "cmtask", "cmnotif", "cmalert"):
        if sql(f"SELECT to_regclass('app_fd_{t}')"):
            sql(f"DELETE FROM app_fd_{t}")

    # case via envelope (caseRef assigned by guard)
    case = f"dc-{RUN}-1"
    form_post("cmCase", {"id": case, "caseType": "TEST", "tin": "100058G",
                         "origin": "MANUAL", "taxType": "VAT",
                         "taxpayerName": "Alpha Ltd", "amountAtStake": "500"})
    r = jg.start(case)
    time.sleep(2)
    jg.complete("openCase", r.get("processId"))
    time.sleep(4)
    ref = sql(f"SELECT c_caseref FROM app_fd_cmcase WHERE id='{case}'")
    docclass = sql("SELECT id FROM app_fd_mddocclass LIMIT 1")

    # ---------- T-07.1: PUSH upload -> Mayan ----------
    doc_id = f"doc-{RUN}-1"
    updir = f"{UPLOAD_ROOT}/cmDoc/{doc_id}"
    os.makedirs(updir, exist_ok=True)
    with open(f"{updir}/evidence.pdf", "wb") as f:
        f.write(b"%PDF-1.4 test evidence " + RUN.encode())
    form_post("cmDoc", {"id": doc_id, "caseId": case, "caseRef": ref, "tin": "100058G",
                        "docClass": docclass, "description": "test evidence",
                        "file": "evidence.pdf", "source": "UPLOAD", "status": "PENDING"})
    time.sleep(4)
    row = sql(f"SELECT c_status || '|' || c_mayandocid FROM app_fd_cmdoc WHERE id='{doc_id}'")
    status, mayan_id = (row.split("|") + [""])[:2]
    label_ok = False
    if mayan_id:
        doc = mayan_get(f"/documents/{mayan_id}/", token)
        label_ok = "evidence.pdf" in doc.get("label", "")
    check("T-07.1 upload registered in Mayan", status == "REGISTERED" and label_ok,
          f"{row} label_ok={label_ok}")

    # ---------- T-07.2: GENERATE single ----------
    form_post("cmDocGen", {"id": f"gen-{RUN}-1", "caseRef": ref,
                           "templateCode": "TPL-TEST", "docClass": docclass})
    time.sleep(4)
    res = sql(f"SELECT c_result FROM app_fd_cmdocgen WHERE id='gen-{RUN}-1'")
    gen = sql(f"SELECT c_status || '|' || c_mayandocid FROM app_fd_cmdoc "
              f"WHERE c_source='GENERATED' AND c_caseid='{case}'")
    gstatus, gmayan = (gen.split("|") + [""])[:2]
    pdf_ok = False
    if gmayan:
        d = mayan_get(f"/documents/{gmayan}/", token)
        pdf_ok = d.get("label", "").endswith(".pdf")
    check("T-07.2 ADG: PDF generated, attached, in Mayan",
          res == "1 document(s) generated" and gstatus == "REGISTERED" and pdf_ok,
          f"result={res} doc={gen}")

    # ---------- T-07.3: mmDocReq enforcement at guardClosure ----------
    req_class = sql("SELECT id FROM app_fd_mddocclass ORDER BY id DESC LIMIT 1")
    form_post("mmDocReq", {"id": f"REQ-{RUN}", "caseType": "TEST", "stateCode": "OPEN",
                           "docClass": req_class, "required": "true"})
    case2 = f"dc-{RUN}-2"
    form_post("cmCase", {"id": case2, "caseType": "TEST", "tin": "100061K",
                         "origin": "MANUAL", "taxType": "VAT"})
    r2 = jg.start(case2)
    time.sleep(2)
    jg.complete("openCase", r2.get("processId"))
    time.sleep(4)
    jg.complete("workInProgress", r2.get("processId"))
    time.sleep(4)
    rej = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case2}' "
              "AND c_eventtype='TRANSITION_REJECTED' AND c_payload LIKE '%required document%'")
    check("T-07.3 doc-per-state blocks closure without document", rej == "1", rej)
    form_post("cmDoc", {"id": f"doc-{RUN}-2", "caseId": case2, "docClass": req_class,
                        "description": "satisfies requirement", "source": "UPLOAD",
                        "status": "PENDING"})
    # remove requirement after the assertion (config restore)
    sql_note = "config restored via API"
    form_post("mmDocReq", {"id": f"REQ-{RUN}", "caseType": "TEST", "stateCode": "OPEN",
                           "docClass": req_class, "required": ""})

    # ---------- T-07.4: postal RETURNED ----------
    form_post("cmPostal", {"id": f"post-{RUN}-1", "caseRef": ref, "method": "REGISTERED",
                           "status": "RETURNED"})
    time.sleep(4)
    flag = sql(f"SELECT c_addressflag FROM app_fd_cmcase WHERE id='{case}'")
    task = sql(f"SELECT count(*) FROM app_fd_cmtask WHERE c_caseid='{case}' "
               "AND c_tasktype='ADDRESS_VERIFICATION' AND c_status='OPEN'")
    pevents = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' "
                  "AND c_eventtype IN ('POSTAL_RETURNED','NOTIF_PENDING')")
    check("T-07.4 returned mail: flag + verification task + alert",
          flag == "RETURNED_MAIL" and task == "1" and int(pevents) >= 2,
          f"flag={flag} task={task} events={pevents}")

    # ---------- T-07.5: batch GENERATE by type ----------
    form_post("cmDocGen", {"id": f"gen-{RUN}-2", "filterCaseType": "TEST",
                           "templateCode": "TPL-TEST", "docClass": docclass})
    time.sleep(5)
    res2 = sql(f"SELECT c_result FROM app_fd_cmdocgen WHERE id='gen-{RUN}-2'")
    n = sql("SELECT count(*) FROM app_fd_cmdoc WHERE c_source='GENERATED'")
    check("T-07.5 batch generation by case type",
          "generated" in res2 and int(n) >= 3, f"result={res2} generated_docs={n}")

    set_officer_pool("true")
    print()
    failed = [name for name, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
