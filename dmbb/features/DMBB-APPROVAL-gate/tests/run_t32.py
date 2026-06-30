"""DMBB Decision & Approval Service (#6) depth slice 3 — conflict of interest (live jdx9).

T-32.1 an approver barred from a subject by an mmCoi EXCLUDE_APPROVER rule cannot decide it
       (blocked beyond four-eyes SoD; the request stays Pending, the agreement is not activated)
T-32.2 a request for an unbarred subject is decided normally -> ACTIVE + hold

Reuses the F08 mmCoi register (caseType + ruleType + expression). The bar `*|COITIN-BAR` excludes
every approver from that one taxpayer's requests; expression tokens are `approver|tin` with `*`
wildcards. Seeded via SQL so the test does not depend on the mmCoi select accepting a new value.

Usage: JDX9_PASSWORD=admin run_t32.py
"""
import json
import subprocess
import sys
import time
import urllib.request

RESULTS = []
RUN = str(int(time.time()))[-6:]
BASE = "http://localhost:8089/jw"
COI_TIN = "COITIN-BAR"


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
                    "taxpayerName": "COI Test", "amountAtStake": amount, "category": cat,
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
    # authority matrix (SINGLE band) + cmApproval lifecycle (as run_t30)
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    # COI bar: every approver is excluded from taxpayer COITIN-BAR's requests
    sql("INSERT INTO app_fd_mmcoi (id, datecreated, datemodified, c_casetype, c_ruletype, c_expression) "
        "VALUES ('coi-ia-bar', now(), now(), 'DM', 'EXCLUDE_APPROVER', '*|" + COI_TIN + "') "
        "ON CONFLICT (id) DO UPDATE SET c_ruletype='EXCLUDE_APPROVER', c_expression='*|" + COI_TIN + "'")
    time.sleep(2)

    # ---------- T-32.1 a barred approver cannot decide (beyond SoD) ----------
    cB, aB = f"apCOI-{RUN}", f"agrCOI-{RUN}"
    make_case(cB, COI_TIN, "C4", "8000")
    submit_plan(aB, COI_TIN, cB, "8000", "12")
    apIdB = pending_id(aB)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdB}'")  # SoD passes
    dmbb("cmApprovalDecision", {"id": f"decCOI-{RUN}", "approvalId": apIdB, "approverLevel": "DIRECTOR",
                                "outcome": "approve", "reason": "trying to approve a conflicted case"})
    time.sleep(6)
    stB = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdB}'")
    coiEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cB}' "
                "AND c_eventtype='APPROVAL_COI_BLOCKED'")
    agrB = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aB}'")
    check("T-32.1 conflicted approver blocked (COI), request stays Pending, agreement not activated",
          stB == "Pending" and int(coiEv) >= 1 and agrB != "ACTIVE" and hold_active(cB) == 0,
          f"status={stB} coiBlockedEv={coiEv} agr={agrB} hold={hold_active(cB)}")

    # ---------- T-32.2 control: an unbarred subject is decided normally ----------
    cOk, tOk, aOk = f"apOK-{RUN}", f"T32OK{RUN}", f"agrOK-{RUN}"
    make_case(cOk, tOk, "C4", "8000")
    submit_plan(aOk, tOk, cOk, "8000", "12")
    apIdOk = pending_id(aOk)
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z' WHERE id='{apIdOk}'")
    dmbb("cmApprovalDecision", {"id": f"decOK-{RUN}", "approvalId": apIdOk, "approverLevel": "SUPERVISOR",
                                "outcome": "approve", "reason": "no conflict here"})
    time.sleep(6)
    stOk = sql(f"SELECT c_status FROM app_fd_cmapproval WHERE id='{apIdOk}'")
    agrOk = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{aOk}'")
    check("T-32.2 unbarred subject decided normally -> Approved -> ACTIVE + hold",
          stOk == "Approved" and agrOk == "ACTIVE" and hold_active(cOk) >= 1,
          f"status={stOk} agr={agrOk} hold={hold_active(cOk)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
