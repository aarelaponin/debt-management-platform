"""DMBB Decision & Approval Service (#6) — P4: delegation binding + lifecycle notifications (live jdx9).

P4 closes two workflow gaps:
  * Delegation now BINDS — once a Pending request is delegated, ONLY the named delegate may decide it.
  * The lifecycle is no longer silent: it queues NOTIF_PENDING notifications (recipient + alertType)
    on assignment, escalation, timeout and delegation, which the F06 dispatcher turns into cmAlerts.

T-36.1 delegation binding: a request delegated to "deputy" cannot be decided by anyone else (the API
       identity, not the delegate, is blocked, request stays Pending); re-delegated to the API
       identity, that identity (now the delegate) decides it through -> Approved + effect.
T-36.2 notifications emitted: an APPROVAL_ASSIGNED notification to the role on submit, and an
       APPROVAL_DELEGATED notification to the named delegate on delegation.
T-36.3 end-to-end: the F06 dispatcher (cmDispatchRun) turns the queued notifications into a cmAlert
       addressed to the assigned role.

Usage: JDX9_PASSWORD=admin run_t36.py
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
    return ("SQL-ERROR: " + r.stderr.strip()[:300]) if r.returncode else r.stdout.strip()


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


def make_case(cid, tin, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "P4 Test", "amountAtStake": amount, "category": "C4",
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": "C4", "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0"})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": amount, "disputed": "0", "enforceable": amount})


def submit_plan(agr, tin, cid, total):
    dmbb("dmInstAgr", {"id": agr, "tin": tin, "debtCaseId": cid, "totalDebt": total,
                       "durationMonths": "12", "action": "submit", "status": "APPLIED"})
    time.sleep(6)


def pending_id(agr):
    return sql(f"SELECT id FROM app_fd_cmapproval WHERE c_recordid='{agr}' AND c_status='Pending' LIMIT 1")


def field(apid, col):
    return sql(f"SELECT c_{col} FROM app_fd_cmapproval WHERE id='{apid}'")


def evc(case, etype, like=""):
    extra = f" AND c_payload LIKE '%{like}%'" if like else ""
    return int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' "
                   f"AND c_eventtype='{etype}'{extra}"))


def delegate(apid, to, reason):
    dmbb("cmApprovalDelegate", {"id": f"del{int(time.time()*1000)%100000}-{apid[-6:]}",
                                "approvalId": apid, "delegateTo": to, "reason": reason})
    time.sleep(6)


def decide(apid, level, reason):
    dmbb("cmApprovalDecision", {"id": f"dec{int(time.time()*1000)%100000}-{apid[-6:]}",
                                "approvalId": apid, "approverLevel": level,
                                "outcome": "approve", "reason": reason})
    time.sleep(6)


def main():
    assert CK and DK, "missing api keys"
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE",
                         "slaDays": "2", "maxEscalations": "2"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)

    c, t, a = f"p4-{RUN}", f"P36{RUN}", f"agr4-{RUN}"
    make_case(c, t, "8000")
    submit_plan(a, t, c, "8000")
    ap = pending_id(a)
    # four-eyes fixture: requester != the API identity (roleAnonymous) so SoD never pre-empts
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-x{RUN}' WHERE id='{ap}'")

    # ---------- T-36.2 (assignment notification, captured before any decision) ----------
    assigned = evc(c, "NOTIF_PENDING", "APPROVAL_ASSIGNED")
    assignedRole = evc(c, "NOTIF_PENDING", "dm_supervisor")

    # ---------- T-36.1 delegation binding ----------
    delegate(ap, f"deputy{RUN}", "please cover")          # bound to deputy
    delegatedNotif = evc(c, "NOTIF_PENDING", "APPROVAL_DELEGATED")
    decide(ap, "SUPERVISOR", "trying as non-delegate")    # API identity != deputy -> blocked
    blocked = field(ap, "status")
    blockEv = evc(c, "APPROVAL_DELEGATE_BLOCKED")
    delegate(ap, "roleAnonymous", "I'll take it")         # re-bind to the API identity
    decide(ap, "SUPERVISOR", "deciding as the delegate")  # now the delegate -> proceeds
    finalStatus = field(ap, "status")
    agr = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{a}'")

    check("T-36.1 delegation binds: a non-delegate is blocked (Pending), the delegate then decides -> Approved + effect",
          blocked == "Pending" and blockEv >= 1 and finalStatus == "Approved" and agr == "ACTIVE",
          f"afterNonDelegate={blocked} blockEv={blockEv} final={finalStatus} agr={agr}")
    check("T-36.2 lifecycle notifications queued: APPROVAL_ASSIGNED to the role on submit + APPROVAL_DELEGATED on delegate",
          assigned >= 1 and assignedRole >= 1 and delegatedNotif >= 1,
          f"assigned={assigned} toRole={assignedRole} delegated={delegatedNotif}")

    # ---------- T-36.3 dispatcher turns the queued notification into a cmAlert ----------
    cmbb("cmDispatchRun", {"id": f"disp-{RUN}"})
    time.sleep(6)
    alerts = int(sql(f"SELECT count(*) FROM app_fd_cmalert WHERE c_caseid='{c}'"))
    check("T-36.3 the F06 dispatcher turns the queued notification into a cmAlert for the case",
          alerts >= 1, f"cmAlerts_for_case={alerts}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
