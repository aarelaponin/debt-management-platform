#!/usr/bin/env python3
"""DMBB-F09 acceptance T-18.1..7 — live jdx9 (apps cmbb + dmbb).

T-18.1 auto C1 write-off (no human intervention): AUTO_C1 sweep -> C1 case written off + closed (DM-FR-042/BR-DM-036)
T-18.2 evidence guard: an APPROVED request with no evidence -> REJECTED (BR-DM-039)
  (T-18.3/T-18.4 retired: discretionary route+approve+post migrated onto the DAS gate — see run_t33)
T-18.5 statutory bulk: a debt aged past the statutory period -> STATUTORY_BULK writes it off (DM-FR-044/BR-DM-038)
T-18.6 C2 passive: C2_PASSIVE marks a C2 debt passive-collection, case stays open (DM-FR-045)
T-18.7 status preservation: a written-off case + its history persist with status written-off (DM-FR-046)

Usage: JDX9_PASSWORD=admin run_t18.py
"""
import json
import subprocess
import sys
import time
from datetime import datetime
import urllib.request

RESULTS = []
RUN = str(int(time.time()))[-6:]
BASE = "http://localhost:8089/jw"


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


def evc(case, etype):
    return int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' AND c_eventtype='{etype}'"))


def debt_case(cid, tin, cat, amount, first_assessed=""):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "WO " + tin, "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0",
                    "firstAssessedDate": first_assessed, "writeOffStatus": ""})
    # managed fields are read-only → the data API drops them on store; seed directly (fixture).
    sql(f"UPDATE app_fd_cmcase SET c_category='{cat}', c_currentstate='OPEN', c_amountatstake='{amount}' WHERE id='{cid}'")
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='{cat}', c_stage='Identified', c_triggerorigin='TEST', c_consolidatedamount='{amount}', c_laststepseq='0', c_firstassesseddate='{first_assessed}', c_writeoffstatus='' WHERE id='{cid}'")


def run(form_payload):
    dmbb("cmWriteOffRun", form_payload)
    time.sleep(5)


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-18.1 auto C1 ----------
    c1 = f"wo1-{RUN}"
    debt_case(c1, f"T18A{RUN}", "C1", "20")
    run({"id": f"swc1-{RUN}", "sweepMode": "AUTO_C1"})
    wos1 = sql(f"SELECT c_status FROM app_fd_dmwriteoff WHERE c_debtcaseid='{c1}'")
    dds1 = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c1}'")
    cs1 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{c1}'")
    chg = int(sql(f"SELECT count(*) FROM app_fd_dmcharge WHERE c_debtcaseid='{c1}' AND c_chargetype='WRITE_OFF'"))
    check("T-18.1 auto C1 write-off posts + closes case, no approval (DM-FR-042)",
          wos1 == "POSTED" and dds1 == "written-off" and cs1 == "CLOSED" and chg == 1
          and evc(c1, "WRITEOFF_POSTED") >= 1, f"wo={wos1} debt={dds1} case={cs1} charge={chg}")

    # ---------- T-18.2 evidence guard ----------
    c2 = f"wo2-{RUN}"
    debt_case(c2, f"T18B{RUN}", "C3", "500")
    dmbb("dmWriteOff", {"id": f"r2-{RUN}", "debtCaseId": c2, "tin": f"T18B{RUN}", "amount": "500",
                        "woType": "APPROVED", "ground": "WO-MGMT", "status": "SUBMITTED"})
    time.sleep(4)
    st2 = sql(f"SELECT c_status FROM app_fd_dmwriteoff WHERE id='r2-{RUN}'")
    check("T-18.2 APPROVED write-off without evidence REJECTED (BR-DM-039)",
          st2 == "REJECTED" and evc(c2, "WRITEOFF_REJECTED") >= 1, f"status={st2}")

    # NOTE (DAS finalisation P2): the discretionary write-off decision (route + approve + post)
    # was retired from the bespoke cmWriteOffApprove path and migrated onto the Decision &
    # Approval Service. That end-to-end flow is now proven in run_t33 (write-off via the gate).

    # ---------- T-18.5 statutory bulk ----------
    c5 = f"wo5-{RUN}"
    debt_case(c5, f"T18E{RUN}", "C2", "60", first_assessed=str(datetime.now().date().replace(year=datetime.now().year - 6)))
    run({"id": f"swst-{RUN}", "sweepMode": "STATUTORY_BULK"})
    dds5 = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c5}'")
    check("T-18.5 statutory-aged debt bulk written off (DM-FR-044/BR-DM-038)",
          dds5 == "written-off" and evc(c5, "WRITEOFF_POSTED") >= 1, f"debt={dds5}")

    # ---------- T-18.6 C2 passive ----------
    c6 = f"wo6-{RUN}"
    debt_case(c6, f"T18F{RUN}", "C2", "60")
    run({"id": f"swc2-{RUN}", "sweepMode": "C2_PASSIVE"})
    dds6 = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c6}'")
    cs6 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{c6}'")
    check("T-18.6 C2 set to passive collection, case stays open (DM-FR-045)",
          dds6 == "passive-collection" and cs6 == "OPEN", f"debt={dds6} case={cs6}")

    # ---------- T-18.7 status preservation ----------
    hist = int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}'"))
    still = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c1}'")
    check("T-18.7 written-off case + history preserved (DM-FR-046)",
          still == "written-off" and hist >= 1, f"status={still} events={hist}")

    # ---------- T-18.8 dmWriteOff status guarded + audited (ADR-003 migration) ----------
    # auto-C1 case c1 moves to POSTED (>=1 STATUS_CHANGED); rejected case c2 -> REJECTED (>=1).
    sc1 = evc(c1, "STATUS_CHANGED")
    sc2 = evc(c2, "STATUS_CHANGED")
    check("T-18.8 dmWriteOff status guarded + audited (auto-post + reject each >=1 STATUS_CHANGED)",
          sc1 >= 1 and sc2 >= 1, f"auto_case_sc={sc1} rejected_case_sc={sc2}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
