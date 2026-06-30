"""DMBB Decision & Approval Service (#6) — P3a: directory-resolved approver identity (live jdx9).

The decide path no longer trusts a self-declared approverLevel for live users: the gate resolves
the approver's authority from their DIRECTORY role-groups (Joget directory API) + the mmRoleLevel
map (AuthorityResolver). The declared field survives only as an explicit automation/test override.

Provable through the form-data API (which runs as `roleAnonymous`, a pseudo-user with no directory
groups → resolves to no authority):

T-34.1 SAFETY — a decision with NO declared level, by an identity with no resolvable directory
       authority, is BLOCKED at the rank gate (you cannot approve by simply omitting the level);
       the request stays Pending and the effect does not fire.
T-34.2 OVERRIDE (back-compat) — the same kind of request, decided WITH a declared level (the
       automation path every existing consumer/test uses), still approves and fires the effect.

(The POSITIVE path — a real logged-in officer whose level is resolved from the directory — is proven
by the AuthorityResolver unit tests and, live, by the P3b inbox slice using logged-in sessions.)

Usage: JDX9_PASSWORD=admin run_t34.py
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


def make_case(cid, tin, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "P3 Identity Test", "amountAtStake": amount, "category": "C4",
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


def apfield(apid, col):
    return sql(f"SELECT c_{col} FROM app_fd_cmapproval WHERE id='{apid}'")


def hold_active(cid):
    return int(sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                   "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='ACTIVE'" % cid))


def decide(apid, payload_extra):
    # four-eyes fixture: requester != approver (roleAnonymous) so SoD never pre-empts the rank gate.
    sql(f"UPDATE app_fd_cmapproval SET c_requestedby='officer-z{RUN}' WHERE id='{apid}'")
    base = {"id": f"d{int(time.time()*1000)%100000}-{apid[-6:]}", "approvalId": apid,
            "outcome": "approve", "reason": "P3 decide"}
    base.update(payload_extra)
    dmbb("cmApprovalDecision", base)
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

    # ---------- T-34.1 SAFETY: no declared level + no directory authority -> blocked ----------
    c1, t1, a1 = f"p3a-{RUN}", f"P34A{RUN}", f"agrA-{RUN}"
    make_case(c1, t1, "8000")
    submit_plan(a1, t1, c1, "8000")
    ap1 = pending_id(a1)
    decide(ap1, {"approverLevel": ""})  # NO declared level -> gate resolves from directory (none)
    st1 = apfield(ap1, "status")
    agr1 = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{a1}'")
    check("T-34.1 no declared level + no resolvable directory authority -> BLOCKED, stays Pending, no effect",
          ap1 != "" and st1 == "Pending" and agr1 != "ACTIVE" and hold_active(c1) == 0,
          f"apId={ap1} status={st1} agr={agr1} hold={hold_active(c1)}")

    # ---------- T-34.2 OVERRIDE (back-compat): declared level still approves ----------
    c2, t2, a2 = f"p3b-{RUN}", f"P34B{RUN}", f"agrB-{RUN}"
    make_case(c2, t2, "8000")
    submit_plan(a2, t2, c2, "8000")
    ap2 = pending_id(a2)
    decide(ap2, {"approverLevel": "SUPERVISOR"})  # declared override (the automation path)
    st2 = apfield(ap2, "status")
    agr2 = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{a2}'")
    check("T-34.2 declared-level override still approves + fires the effect (automation path intact)",
          st2 == "Approved" and agr2 == "ACTIVE" and hold_active(c2) >= 1,
          f"apId={ap2} status={st2} agr={agr2} hold={hold_active(c2)}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
