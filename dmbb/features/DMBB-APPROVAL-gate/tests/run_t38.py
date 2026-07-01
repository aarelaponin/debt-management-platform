"""DMBB Decision & Approval Service (#6) — P5: auto-COI + authority-matrix validation (live jdx9).

P5 hardens governance integrity:
  * Auto-derived COI — when an mmCoi EXCLUDE_DECISION_MAKER rule is in force, one person may not
    decide more than one request for the SAME taxpayer (derived from the decision record, no
    separate declaration).
  * Authority-matrix validation — a cmAuthorityCheck trigger validates an action's mmAuthority bands
    (level-exists / ascending chain / quorum / overlap / gap) so a bad config is caught before it routes.

T-38.1 auto-COI: an approver who decided one request for a taxpayer is BLOCKED on a second request
       for the same taxpayer; the request stays Pending.
T-38.2 matrix-check flags a bad config (overlapping bands) -> valid=false, issues listed.
T-38.3 matrix-check passes a clean config -> valid=true, no issues.

Usage: JDX9_PASSWORD=admin run_t38.py
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
                    "taxpayerName": "P5 Test", "amountAtStake": amount, "category": "C4",
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


def decide(apid, level, reason):
    dmbb("cmApprovalDecision", {"id": f"d{int(time.time()*1000)%100000}-{apid[-6:]}",
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
    # the auto-COI rule — seed through the data API (a Joget write) so the mmCoi cache is invalidated;
    # a raw-SQL insert would be missed by a stale cache left by the earlier DM tests.
    cmbb("mmCoi", {"id": "coi-dm-decmaker", "caseType": "DM",
                   "ruleType": "EXCLUDE_DECISION_MAKER", "expression": "*"})
    if sql("SELECT c_ruletype FROM app_fd_mmcoi WHERE id='coi-dm-decmaker'") != "EXCLUDE_DECISION_MAKER":
        # the select dropped the out-of-list value -> set it, then re-write via the API to bust the cache
        sql("UPDATE app_fd_mmcoi SET c_ruletype='EXCLUDE_DECISION_MAKER', c_expression='*' WHERE id='coi-dm-decmaker'")
        cmbb("mmCoi", {"id": "coi-dm-decmaker", "caseType": "DM", "expression": "*"})
    time.sleep(2)

    # ---------- T-38.1 auto-COI: same identity can't decide two requests for one taxpayer ----------
    tin = f"TINP5{RUN}"
    c1, a1 = f"p5a-{RUN}", f"agr5a-{RUN}"
    c2, a2 = f"p5b-{RUN}", f"agr5b-{RUN}"
    make_case(c1, tin, "8000"); submit_plan(a1, tin, c1, "8000")
    make_case(c2, tin, "8000"); submit_plan(a2, tin, c2, "8000")
    ap1, ap2 = pending_id(a1), pending_id(a2)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-x{RUN}' WHERE id IN ('{ap1}','{ap2}')")
    decide(ap1, "SUPERVISOR", "first decision for the taxpayer")
    st1 = field(ap1, "status")
    decide(ap2, "SUPERVISOR", "second decision for the same taxpayer")
    st2 = field(ap2, "status")
    # the instalment resolver groups by taxpayer, so both requests land on the same case; query the
    # auto-COI block on the request's actual case.
    ap2case = field(ap2, "caseid")
    autocoi = int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{ap2case}' AND c_eventtype='APPROVAL_AUTOCOI_BLOCKED'"))
    check("T-38.1 auto-COI: same decision-maker blocked on a 2nd request for the same taxpayer (stays Pending)",
          st1 == "Approved" and st2 == "Pending" and autocoi >= 1,
          f"first={st1} second={st2} autoCoiEv={autocoi}")
    # auto-COI is existence-based per case type, so remove the DM rule now it has been exercised —
    # otherwise it would silently apply to every other DM approval test.
    sql("DELETE FROM app_fd_mmcoi WHERE id='coi-dm-decmaker'")

    # ---------- T-38.2 matrix-check flags a bad config (overlap) ----------
    cmbb("mmAuthority", {"id": f"BAD1-{RUN}", "actionType": f"TEST_BAD{RUN}", "amountMin": "0",
                         "amountMax": "10000", "level": "SUPERVISOR", "bodyType": "SINGLE"})
    cmbb("mmAuthority", {"id": f"BAD2-{RUN}", "actionType": f"TEST_BAD{RUN}", "amountMin": "5000",
                         "amountMax": "20000", "level": "MANAGER", "bodyType": "SINGLE"})
    time.sleep(1)
    chkBad = f"chkbad-{RUN}"
    dmbb("cmAuthorityCheck", {"id": chkBad, "actionType": f"TEST_BAD{RUN}"})
    time.sleep(4)
    badValid = sql(f"SELECT c_valid FROM app_fd_cmauthoritycheck WHERE id='{chkBad}'")
    badIssues = sql(f"SELECT c_issues FROM app_fd_cmauthoritycheck WHERE id='{chkBad}'")
    check("T-38.2 matrix-check flags a bad config: overlapping bands -> valid=false, issues listed",
          badValid == "false" and "overlap" in badIssues.lower(),
          f"valid={badValid} issues={badIssues[:120]}")

    # ---------- T-38.3 matrix-check passes a clean config ----------
    cmbb("mmAuthority", {"id": f"GOOD1-{RUN}", "actionType": f"TEST_GOOD{RUN}", "amountMin": "0",
                         "amountMax": "5000", "level": "SUPERVISOR", "bodyType": "SINGLE"})
    cmbb("mmAuthority", {"id": f"GOOD2-{RUN}", "actionType": f"TEST_GOOD{RUN}", "amountMin": "5000.01",
                         "amountMax": "", "level": "DIRECTOR", "bodyType": "COLLEGIAL", "quorum": "2"})
    time.sleep(1)
    chkGood = f"chkgood-{RUN}"
    dmbb("cmAuthorityCheck", {"id": chkGood, "actionType": f"TEST_GOOD{RUN}"})
    time.sleep(4)
    goodValid = sql(f"SELECT c_valid FROM app_fd_cmauthoritycheck WHERE id='{chkGood}'")
    check("T-38.3 matrix-check passes a clean, contiguous, well-formed config -> valid=true",
          goodValid == "true", f"valid={goodValid}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
