"""DMBB Decision & Approval Service (#6) — P3b: per-user "mine to decide" inbox (live jdx9).

The inbox is the ApprovalInboxBinder (a custom Joget DataListBinder, API-only): it identifies the
logged-in user, resolves their rank level from their DIRECTORY role-groups (directory API + the
mmRoleLevel map — no dir_* SQL), and returns only the Pending requests they may decide (rank gate +
four-eyes + delegation, via the pure ApprovalInbox rule that is exhaustively unit-tested).

This proves the POSITIVE path live: a real Pending request is raised through the gate (a Joget write,
so the binder's form-data read is cache-consistent), the inbox is rendered AS ADMIN — a real
directory user in dm_policy_admin, so the binder resolves admin → DIRECTOR from the directory — and:

T-35.1 admin SEES a DIRECTOR-required request (only because their level is resolved as DIRECTOR);
T-35.2 the row carries requiredLevel=DIRECTOR (the rank the inbox matched the user against);
T-35.3 admin also SEES a lower SUPERVISOR-required request (rank ≥ required), confirming the gate's
       rank rule drives the inbox.

(four-eyes, outranking-exclusion, non-Pending exclusion and delegation are proven by ApprovalInboxTest.)

Usage: JDX9_PASSWORD=admin run_t35.py
"""
import json
import subprocess
import sys
import time
import urllib.request

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

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
                    "taxpayerName": "P3b Inbox Test", "amountAtStake": amount, "category": "C4",
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": "C4", "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0"})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": amount, "disputed": "0", "enforceable": amount})


def submit_plan(agr, tin, cid, total):
    dmbb("dmInstAgr", {"id": agr, "tin": tin, "debtCaseId": cid, "totalDebt": total,
                       "durationMonths": "12", "action": "submit", "status": "APPLIED"})
    time.sleep(6)


def required_of(agr):
    return sql(f"SELECT c_requiredlevel FROM app_fd_cmapproval WHERE c_recordid='{agr}' AND c_status='Pending' LIMIT 1")


def render_inbox_as_admin():
    # Joget caches the rendered userview page per session → use a fresh authenticated session.
    c = JogetClient.from_instance("jdx9")
    c.get("/web/json/console/app/list", params={"pageSize": 1})  # authenticate as admin
    sess = getattr(c, "session", None) or c._session
    b = c.config.base_url.rstrip("/")
    return getattr(sess.get(b + "/web/userview/dmbb/dmbbConsole/_/list_cmApproval_my",
                            headers={"Referer": b + "/web/console/home"}), "text", "") or ""


def main():
    assert CK and DK, "missing api keys"
    # the unified instalment bands (as run_t30): 50000.01+ -> collegial DIRECTOR; 5000.01-20000 -> SUPERVISOR
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE",
                         "slaDays": "2", "maxEscalations": "2"})
    cmbb("mmAuthority", {"id": "AUTH-IA-Q", "actionType": "INSTALMENT_PLAN", "amountMin": "50000.01",
                         "amountMax": "", "level": "DIRECTOR", "bodyType": "COLLEGIAL", "quorum": "2",
                         "slaDays": "2", "maxEscalations": "2"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)

    # clear stale Pending requests left by earlier runs so this run's two dominate page 1 of the
    # inbox (the binder pages; abandoned Pending from prior t30..t34 runs would otherwise bury them).
    sql("UPDATE app_fd_cmapproval SET c_status='Cleared' WHERE c_status='Pending'")
    time.sleep(1)

    # raise two real Pending requests through the gate (Joget writes → cache-consistent reads)
    cD, tD, aD = f"p3bD-{RUN}", f"P35D{RUN}", f"agrD-{RUN}"   # 60000 -> DIRECTOR required
    cS, tS, aS = f"p3bS-{RUN}", f"P35S{RUN}", f"agrS-{RUN}"   # 8000  -> SUPERVISOR required
    make_case(cD, tD, "60000"); submit_plan(aD, tD, cD, "60000")
    make_case(cS, tS, "8000"); submit_plan(aS, tS, cS, "8000")
    reqD, reqS = required_of(aD), required_of(aS)

    html = ""
    seesD = seesS = False
    for _ in range(6):
        html = render_inbox_as_admin()
        seesD, seesS = (aD in html), (aS in html)
        if seesD and seesS:
            break
        time.sleep(8)
    rowD_director = ("column_requiredLevel body_column_2\" style=\"\">DIRECTOR" in html)

    check("T-35.1 inbox renders + admin (DIRECTOR via directory) SEES the DIRECTOR-required request",
          ("list_cmApproval_my" in html) and reqD == "DIRECTOR" and seesD,
          f"requiredOfD={reqD} seesD={seesD} bytes={len(html)}")
    check("T-35.2 the visible request is matched at DIRECTOR (the user's directory-resolved rank)",
          rowD_director, f"directorRowRendered={rowD_director}")
    check("T-35.3 admin also sees a lower SUPERVISOR-required request (rank ≥ required drives the inbox)",
          reqS == "SUPERVISOR" and seesS, f"requiredOfS={reqS} seesS={seesS}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
