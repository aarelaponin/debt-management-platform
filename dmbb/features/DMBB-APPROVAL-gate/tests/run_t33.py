"""DMBB Decision & Approval Service (#6) — P2 reuse proof: WRITE-OFF via the gate (live jdx9).

Write-off is the gate's SECOND live consumer (the first is INSTALMENT_PLAN). A discretionary
write-off no longer uses the bespoke cmWriteOffApprove flow: submit() raises a WRITE_OFF gate
request routed off the unified mmAuthority bands, and the gate's WRITE_OFF effect
(WriteOffService.applyApproved) posts the write-off when the decision completes.

T-33.1 single supervisor (500 -> AUTH-WO-S): submit -> wo UNDER_REVIEW + a Pending WRITE_OFF
       request (SUPERVISOR/SINGLE); one approval -> Approved -> effect posts: wo POSTED, debt
       written-off, case CLOSED, a WRITE_OFF charge recorded.
T-33.2 collegial quorum (25000 -> AUTH-WO-D): submit -> a Pending WRITE_OFF request
       (DIRECTOR/QUORUM 2); one valid vote accumulates (1 of 2) and the request stays Pending —
       the effect does NOT fire and the write-off stays UNDER_REVIEW (completion-at-2 is covered
       by the gate unit tests; one API identity cannot supply a distinct 2nd voter).

Usage: JDX9_PASSWORD=admin run_t33.py
"""
import json
import subprocess
import sys
import time
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


def ap_field(apid, col):
    return sql(f"SELECT c_{col} FROM app_fd_cmapproval WHERE id='{apid}'")


def pending_ap(record):
    return sql(f"SELECT id FROM app_fd_cmapproval WHERE c_recordid='{record}' AND c_status='Pending' LIMIT 1")


def debt_case(cid, tin, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "WriteOff Gate Test", "amountAtStake": amount, "category": "C4",
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": "C4", "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0",
                    "writeOffStatus": ""})


def submit_wo(woid, cid, tin, amount):
    dmbb("dmWriteOff", {"id": woid, "debtCaseId": cid, "tin": tin, "amount": amount,
                        "woType": "APPROVED", "ground": "WO-MGMT",
                        "enforcementHistorySummary": "5 enforcement steps exhausted",
                        "evidenceRef": "EV-" + woid, "rationale": "insolvent — uneconomic to pursue",
                        "status": "SUBMITTED"})
    time.sleep(6)


def decide(apid, level, outcome, reason):
    # four-eyes fixture: the gate blocks self-approval, so make the requester != the API identity.
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-{RUN}' WHERE id='{apid}'")
    dmbb("cmApprovalDecision", {"id": f"dec-{apid[-8:]}", "approvalId": apid,
                                "approverLevel": level, "outcome": outcome, "reason": reason})
    time.sleep(6)


def main():
    assert CK and DK, "missing api keys"
    # self-seed (idempotent): the WRITE_OFF authority bands + the cmApproval lifecycle, with
    # explicit slaDays/maxEscalations so this test is immune to shared-band config drift.
    cmbb("mmAuthority", {"id": "AUTH-WO-S", "actionType": "WRITE_OFF", "amountMin": "0",
                         "amountMax": "5000", "level": "SUPERVISOR", "bodyType": "SINGLE",
                         "slaDays": "2", "maxEscalations": "2"})
    cmbb("mmAuthority", {"id": "AUTH-WO-D", "actionType": "WRITE_OFF", "amountMin": "5000.01",
                         "amountMax": "", "level": "DIRECTOR", "bodyType": "COLLEGIAL", "quorum": "2",
                         "slaDays": "2", "maxEscalations": "2"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)

    # ---------- T-33.1 single supervisor: submit -> gate -> approve -> posts ----------
    c1, t1, w1 = f"wog1-{RUN}", f"T33A{RUN}", f"woG1-{RUN}"
    debt_case(c1, t1, "500")
    submit_wo(w1, c1, t1, "500")
    rev1 = sql(f"SELECT c_status FROM app_fd_dmwriteoff WHERE id='{w1}'")
    ap1 = pending_ap(w1)
    routed = (ap_field(ap1, "actiontype"), ap_field(ap1, "requiredlevel"), ap_field(ap1, "routekind")) if ap1 else ("", "", "")
    reqEv = evc(c1, "APPROVAL_REQUESTED")
    decide(ap1, "SUPERVISOR", "approve", "within delegated authority")
    apStat1 = ap_field(ap1, "status")
    wos1 = sql(f"SELECT c_status FROM app_fd_dmwriteoff WHERE id='{w1}'")
    dds1 = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c1}'")
    cs1 = sql(f"SELECT c_currentstate FROM app_fd_cmcase WHERE id='{c1}'")
    chg1 = int(sql(f"SELECT count(*) FROM app_fd_dmcharge WHERE c_debtcaseid='{c1}' AND c_chargetype='WRITE_OFF'"))
    check("T-33.1 submit routes WRITE_OFF to the gate (SUPERVISOR/SINGLE), Pending request raised",
          rev1 == "UNDER_REVIEW" and routed == ("WRITE_OFF", "SUPERVISOR", "SINGLE") and int(reqEv) >= 1,
          f"woStatus={rev1} routed={routed} reqEv={reqEv}")
    check("T-33.1 gate approval fires the WRITE_OFF effect: posted + written-off + closed + charge",
          apStat1 == "Approved" and wos1 == "POSTED" and dds1 == "written-off"
          and cs1 == "CLOSED" and chg1 == 1 and evc(c1, "WRITEOFF_POSTED") >= 1,
          f"ap={apStat1} wo={wos1} debt={dds1} case={cs1} charge={chg1}")

    # ---------- T-33.2 collegial quorum: one vote accumulates; effect does NOT fire ----------
    c2, t2, w2 = f"wog2-{RUN}", f"T33B{RUN}", f"woG2-{RUN}"
    debt_case(c2, t2, "25000")
    submit_wo(w2, c2, t2, "25000")
    ap2 = pending_ap(w2)
    routed2 = (ap_field(ap2, "requiredlevel"), ap_field(ap2, "routekind"), ap_field(ap2, "quorum")) if ap2 else ("", "", "")
    decide(ap2, "DIRECTOR", "approve", "first director vote")
    apStat2 = ap_field(ap2, "status")
    cnt2 = ap_field(ap2, "approvalscount")
    wos2 = sql(f"SELECT c_status FROM app_fd_dmwriteoff WHERE id='{w2}'")
    dds2 = sql(f"SELECT c_writeoffstatus FROM app_fd_dmdebt WHERE id='{c2}'")
    check("T-33.2 high-value WRITE_OFF routes to DIRECTOR collegial quorum 2",
          routed2 == ("DIRECTOR", "QUORUM", "2"), f"routed={routed2}")
    check("T-33.2 one valid vote accumulates (1 of 2); still Pending, effect not fired, wo UNDER_REVIEW",
          apStat2 == "Pending" and cnt2 == "1" and wos2 == "UNDER_REVIEW" and dds2 != "written-off",
          f"ap={apStat2} count={cnt2} wo={wos2} debt={dds2}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
