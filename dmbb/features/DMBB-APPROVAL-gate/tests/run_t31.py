"""DMBB Decision & Approval Service (#6) depth slice 2 — escalation / timeout / delegation (live jdx9).

T-31.1 an overdue Pending request escalates one rank per sweep (SUPERVISOR -> MANAGER -> DIRECTOR)
T-31.2 after the escalations are exhausted, the next sweep times it out (auto-Rejected; never approved)
T-31.3 delegating a request records the hand-off; the delegate then decides under the rank gate -> ACTIVE

Sweeps time-travel via the cmApprovalSweep `asOf` field (the request's SLA deadline is now + 2 days).

Usage: JDX9_PASSWORD=admin run_t31.py
"""
import json
import subprocess
import sys
import time
import urllib.request
from datetime import datetime, timedelta

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


def make_case(cid, tin, cat, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "Approval Test", "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0"})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": amount, "disputed": "0", "enforceable": amount})


def submit_plan(agr, tin, cid, total, months):
    dmbb("dmInstAgr", {"id": agr, "tin": tin, "debtCaseId": cid, "totalDebt": total,
                       "durationMonths": months, "action": "submit", "status": "APPLIED"})
    time.sleep(6)


def pending_id(agr):
    return sql(f"SELECT id FROM app_fd_cmapproval WHERE c_recordid='{agr}' AND c_status='Pending' LIMIT 1")


def field(apid, col):
    return sql(f"SELECT c_{col} FROM app_fd_cmapproval WHERE id='{apid}'")


def hold_active(cid):
    return int(sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                   "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='ACTIVE'" % cid))


def sweep(sid, asof_date):
    dmbb("cmApprovalSweep", {"id": sid, "asOf": asof_date})
    time.sleep(5)


def main():
    assert CK and DK, "missing api keys"
    # self-seed (idempotent): the unified authority matrix + the cmApproval lifecycle (as run_t30)
    # explicit slaDays/maxEscalations so the escalation arithmetic is deterministic regardless of
    # any config another test left on this shared band (the form API merge-updates rows).
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE",
                         "slaDays": "2", "maxEscalations": "2"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)
    d5 = (datetime.now() + timedelta(days=5)).strftime("%Y-%m-%d")
    d10 = (datetime.now() + timedelta(days=10)).strftime("%Y-%m-%d")
    d15 = (datetime.now() + timedelta(days=15)).strftime("%Y-%m-%d")

    # ---------- T-31.1 escalation: each sweep past the deadline bumps the required rank ----------
    cE, tE, aE = f"apE-{RUN}", f"T31E{RUN}", f"agrE-{RUN}"
    make_case(cE, tE, "C4", "8000")
    submit_plan(aE, tE, cE, "8000", "12")  # SINGLE SUPERVISOR band -> Pending
    apIdE = pending_id(aE)
    lvl0 = field(apIdE, "requiredlevel")
    sweep(f"sw1-{RUN}", d5)
    lvl1, esc1, st1 = field(apIdE, "requiredlevel"), field(apIdE, "escalations"), field(apIdE, "status")
    sweep(f"sw2-{RUN}", d10)
    lvl2, esc2 = field(apIdE, "requiredlevel"), field(apIdE, "escalations")
    escEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cE}' "
                "AND c_eventtype='APPROVAL_ESCALATED'")
    check("T-31.1 overdue request escalates one rank per sweep: SUPERVISOR->MANAGER->DIRECTOR",
          lvl0 == "SUPERVISOR" and st1 == "Pending" and lvl1 == "MANAGER" and esc1 == "1"
          and lvl2 == "DIRECTOR" and esc2 == "2" and int(escEv) >= 2,
          f"level0={lvl0} level1={lvl1} esc1={esc1} level2={lvl2} esc2={esc2} escalatedEv={escEv}")

    # ---------- T-31.2 timeout: the sweep after the last escalation auto-rejects (never approves) -----
    sweep(f"sw3-{RUN}", d15)
    stE = field(apIdE, "status")
    toEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cE}' "
               "AND c_eventtype='APPROVAL_TIMEOUT'")
    agrE = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aE}'")
    check("T-31.2 escalations exhausted -> timeout auto-Rejected, agreement not activated, no hold",
          stE == "Rejected" and int(toEv) >= 1 and agrE != "ACTIVE" and hold_active(cE) == 0,
          f"status={stE} timeoutEv={toEv} agr={agrE} hold={hold_active(cE)}")

    # ---------- T-31.3 delegation: hand off then the delegate decides under the rank gate -> ACTIVE ----
    cD, tD, aD = f"apD-{RUN}", f"T31D{RUN}", f"agrD-{RUN}"
    make_case(cD, tD, "C4", "8000")
    submit_plan(aD, tD, cD, "8000", "12")
    apIdD = pending_id(aD)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdD}'")  # four-eyes fixture
    dmbb("cmApprovalDelegate", {"id": f"del-{RUN}", "approvalId": apIdD,
                                "delegateTo": "deputy", "reason": "please cover for me"})
    time.sleep(6)
    delTo = field(apIdD, "delegatedto")
    delEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cD}' "
                "AND c_eventtype='APPROVAL_DELEGATED'")
    stD1 = field(apIdD, "status")
    dmbb("cmApprovalDecision", {"id": f"decd-{RUN}", "approvalId": apIdD, "approverLevel": "SUPERVISOR",
                                "outcome": "approve", "reason": "deciding as delegated"})
    time.sleep(6)
    stD2 = field(apIdD, "status")
    agrD = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aD}'")
    check("T-31.3 delegate records the hand-off; the delegate then decides (rank ok) -> ACTIVE + hold",
          delTo == "deputy" and int(delEv) >= 1 and stD1 == "Pending"
          and stD2 == "Approved" and agrD == "ACTIVE" and hold_active(cD) >= 1,
          f"delegatedTo={delTo} delegatedEv={delEv} statusAfterDelegate={stD1} final={stD2} agr={agrD} hold={hold_active(cD)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
