#!/usr/bin/env python3
"""DMBB Decision & Approval Service (#6) acceptance — live jdx9 (apps cmbb + dmbb).

T-30.1 instalment under the band (<=5000) -> auto-pass (no approval) -> ACTIVE + hold
T-30.2 instalment over the band (>5000)  -> Pending approval routed to dm_supervisor, NO hold
T-30.3 approve the Pending request (approver != requester) -> ACTIVE + hold + reasoned record
T-30.4 self-approval blocked (approver == requester) -> SoD, request stays Pending, not activated
T-30.5 reject the Pending request -> blocked, agreement not activated, no hold

Usage: JDX9_PASSWORD=admin run_t30.py
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


def hold_active(cid):
    return int(sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                   "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='ACTIVE'" % cid))


def main():
    assert CK and DK, "missing api keys"
    # self-seed (idempotent): the unified authority matrix (mmAuthority — ADR-005) and the cmApproval
    # lifecycle. Three INSTALMENT_PLAN bands, one per topology, exercised by materiality below:
    # 5000.01-20000 -> SINGLE SUPERVISOR; 20000.01-50000 -> CHAIN SUPERVISOR->DIRECTOR;
    # 50000.01-inf -> QUORUM 2 x DIRECTOR.
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE"})
    cmbb("mmAuthority", {"id": "AUTH-IA-C", "actionType": "INSTALMENT_PLAN", "amountMin": "20000.01",
                         "amountMax": "50000", "level": "SUPERVISOR,DIRECTOR", "bodyType": "CHAIN"})
    cmbb("mmAuthority", {"id": "AUTH-IA-Q", "actionType": "INSTALMENT_PLAN", "amountMin": "50000.01",
                         "amountMax": "", "level": "DIRECTOR", "bodyType": "COLLEGIAL", "quorum": "2"})
    # mdRelief: rely on the standard seeded product REL-MLT-STD (C3,C4,C5, minInstalment 50),
    # exactly as run_t15 does — do not seed a competing row (firstRow ambiguity).
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)

    # ---------- T-30.1 under the band -> auto-pass -> ACTIVE + hold ----------
    cL, tL, aL = f"apL-{RUN}", f"T30L{RUN}", f"agrL-{RUN}"
    make_case(cL, tL, "C4", "4000")
    submit_plan(aL, tL, cL, "4000", "12")
    sL = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aL}'")
    apL = sql(f"SELECT count(*) FROM app_fd_cmapproval WHERE c_recordid='{aL}'")
    naEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cL}' "
               "AND c_eventtype='APPROVAL_NOT_REQUIRED'")
    check("T-30.1 under band auto-passes -> ACTIVE + hold, no approval raised",
          sL == "ACTIVE" and hold_active(cL) >= 1 and int(apL) == 0 and int(naEv) >= 1,
          f"status={sL} hold={hold_active(cL)} approvals={apL} notReqEv={naEv}")

    # ---------- T-30.2 over the band -> Pending, NO hold ----------
    cH, tH, aH = f"apH-{RUN}", f"T30H{RUN}", f"agrH-{RUN}"
    make_case(cH, tH, "C4", "8000")
    submit_plan(aH, tH, cH, "8000", "12")
    sH = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aH}'")
    apId = pending_id(aH)
    auth = sql(f"SELECT c_requiredauthority FROM app_fd_cmapproval WHERE id='{apId}'")
    reqEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cH}' "
                "AND c_eventtype='APPROVAL_REQUESTED'")
    check("T-30.2 over band -> Pending (SUPERVISOR), agreement not activated, NO hold",
          bool(apId) and not apId.startswith("SQL") and auth == "SUPERVISOR"
          and sH != "ACTIVE" and hold_active(cH) == 0 and int(reqEv) >= 1,
          f"approval={apId} authority={auth} agrStatus={sH} hold={hold_active(cH)} reqEv={reqEv}")

    # ---------- T-30.3 approve (approver != requester) -> ACTIVE + hold + reasoned record ----------
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-x' WHERE id='{apId}'")  # four-eyes fixture
    dmbb("cmApprovalDecision", {"id": f"dec-ok-{RUN}", "approvalId": apId, "approverLevel": "SUPERVISOR",
                                "outcome": "approve", "reason": "within delegated authority"})
    time.sleep(6)
    apStat = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apId}'")
    sH2 = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aH}'")
    decEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cH}' "
                "AND c_eventtype='APPROVAL_DECISION'")
    check("T-30.3 approve -> agreement ACTIVE + hold + reasoned APPROVAL_DECISION",
          apStat == "Approved" and sH2 == "ACTIVE" and hold_active(cH) >= 1 and int(decEv) >= 1,
          f"approval={apStat} agrStatus={sH2} hold={hold_active(cH)} decisionEv={decEv}")

    # ---------- T-30.4 self-approval blocked (approver == requester) ----------
    cS, tS, aS = f"apS-{RUN}", f"T30S{RUN}", f"agrS-{RUN}"
    make_case(cS, tS, "C4", "9000")
    submit_plan(aS, tS, cS, "9000", "12")
    apIdS = pending_id(aS)  # requestedBy == the api user (== the decider) by default
    dmbb("cmApprovalDecision", {"id": f"dec-sod-{RUN}", "approvalId": apIdS, "approverLevel": "DIRECTOR",
                                "outcome": "approve", "reason": "trying to self-approve"})
    time.sleep(6)
    apStatS = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdS}'")
    sodEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cS}' "
                "AND c_eventtype='APPROVAL_SOD_BLOCKED'")
    sS = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aS}'")
    check("T-30.4 self-approval blocked (SoD), request stays Pending, not activated",
          apStatS == "Pending" and int(sodEv) >= 1 and sS != "ACTIVE" and hold_active(cS) == 0,
          f"approval={apStatS} sodEv={sodEv} agrStatus={sS} hold={hold_active(cS)}")

    # ---------- T-30.5 reject -> blocked ----------
    cR, tR, aR = f"apR-{RUN}", f"T30R{RUN}", f"agrR-{RUN}"
    make_case(cR, tR, "C4", "7000")
    submit_plan(aR, tR, cR, "7000", "12")
    apIdR = pending_id(aR)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-y' WHERE id='{apIdR}'")
    dmbb("cmApprovalDecision", {"id": f"dec-rej-{RUN}", "approvalId": apIdR, "approverLevel": "SUPERVISOR",
                                "outcome": "reject", "reason": "duration too long for this band"})
    time.sleep(6)
    apStatR = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdR}'")
    sR = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aR}'")
    check("T-30.5 reject -> request Rejected, agreement not activated, no hold",
          apStatR == "Rejected" and sR != "ACTIVE" and hold_active(cR) == 0,
          f"approval={apStatR} agrStatus={sR} hold={hold_active(cR)}")

    # ---------- T-30.6 chain route: first step advances the cursor; no effect yet ----------
    # (Completion needs a distinct 2nd approver, which one API identity / roleAnonymous cannot
    # represent live; the full supervisor->director->effect completion is unit-tested. Here we
    # prove the live wiring: CHAIN routing, the rank-gated first step, the cursor advancing to the
    # next level, persisted route state, and NO premature effect.)
    cC, tC, aC = f"apC-{RUN}", f"T30C{RUN}", f"agrC-{RUN}"
    make_case(cC, tC, "C4", "30000")
    submit_plan(aC, tC, cC, "30000", "12")
    apIdC = pending_id(aC)
    kindC = sql(f"SELECT c_routekind FROM app_fd_cmapproval WHERE id='{apIdC}'")
    lvl0 = sql(f"SELECT c_requiredlevel FROM app_fd_cmapproval WHERE id='{apIdC}'")
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdC}'")  # four-eyes fixture
    dmbb("cmApprovalDecision", {"id": f"dec-c1-{RUN}", "approvalId": apIdC, "approverLevel": "SUPERVISOR",
                                "outcome": "approve", "reason": "step 1 ok"})
    time.sleep(6)
    midStat = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdC}'")
    lvl1 = sql(f"SELECT c_requiredlevel FROM app_fd_cmapproval WHERE id='{apIdC}'")
    midAgr = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aC}'")
    check("T-30.6 chain: supervisor step advances the cursor to DIRECTOR; Pending, no effect yet",
          kindC == "CHAIN" and lvl0 == "SUPERVISOR" and midStat == "Pending"
          and lvl1 == "DIRECTOR" and midAgr != "ACTIVE" and hold_active(cC) == 0,
          f"kind={kindC} level0={lvl0} midStatus={midStat} level1={lvl1} midAgr={midAgr} hold={hold_active(cC)}")

    # ---------- T-30.7 quorum route: vote accumulates; a duplicate vote is ignored; no effect yet --------
    # (Reaching quorum needs a 2nd distinct director — unit-tested. Live proves: QUORUM routing,
    # the 1st vote accumulating to 1/2, the same identity's repeat vote being ignored, Pending.)
    cQ, tQ, aQ = f"apQ-{RUN}", f"T30Q{RUN}", f"agrQ-{RUN}"
    make_case(cQ, tQ, "C4", "70000")
    submit_plan(aQ, tQ, cQ, "70000", "12")
    apIdQ = pending_id(aQ)
    kindQ = sql(f"SELECT c_routekind FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    quoQ = sql(f"SELECT c_quorum FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdQ}'")
    dmbb("cmApprovalDecision", {"id": f"dec-q1-{RUN}", "approvalId": apIdQ, "approverLevel": "DIRECTOR",
                                "outcome": "approve", "reason": "vote 1"})
    time.sleep(6)
    cnt1 = sql(f"SELECT c_approvalscount FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    stat1 = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    # the same identity voting again must NOT count
    dmbb("cmApprovalDecision", {"id": f"dec-qd-{RUN}", "approvalId": apIdQ, "approverLevel": "DIRECTOR",
                                "outcome": "approve", "reason": "voting again"})
    time.sleep(6)
    cntDup = sql(f"SELECT c_approvalscount FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    statQ = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdQ}'")
    agrQ = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aQ}'")
    check("T-30.7 quorum: 1st vote -> 1/2, duplicate vote ignored, Pending, no premature effect",
          kindQ == "QUORUM" and quoQ == "2" and cnt1 == "1" and stat1 == "Pending"
          and cntDup == "1" and statQ == "Pending" and agrQ != "ACTIVE" and hold_active(cQ) == 0,
          f"kind={kindQ} quorum={quoQ} count1={cnt1} afterDup={cntDup} status1={stat1} agr={agrQ}")

    # ---------- T-30.8 rank gate: an officer cannot approve a supervisor band ----------
    cK, tK, aK = f"apK-{RUN}", f"T30K{RUN}", f"agrK-{RUN}"
    make_case(cK, tK, "C4", "8000")
    submit_plan(aK, tK, cK, "8000", "12")
    apIdK = pending_id(aK)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdK}'")
    dmbb("cmApprovalDecision", {"id": f"dec-k-{RUN}", "approvalId": apIdK, "approverLevel": "OFFICER",
                                "outcome": "approve", "reason": "trying to wave it through"})
    time.sleep(6)
    statK = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdK}'")
    rankEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cK}' "
                 "AND c_eventtype='APPROVAL_RANK_BLOCKED'")
    agrK = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aK}'")
    check("T-30.8 rank gate: officer below the supervisor band is blocked, request stays Pending",
          statK == "Pending" and int(rankEv) >= 1 and agrK != "ACTIVE" and hold_active(cK) == 0,
          f"approvalStatus={statK} rankBlockedEv={rankEv} agr={agrK} hold={hold_active(cK)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
