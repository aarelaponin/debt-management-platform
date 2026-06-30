"""DAS finalisation P1 — config-ified SLA + effective-dating (live jdx9).

T-37.1 the decision SLA comes from mmAuthority config (not a code constant): a band with slaDays=7
       stamps the request's deadline 7 days out.
T-37.2 effective-dating: when two bands overlap for the same action/amount, the request resolves to
       the one effective today (a band whose effectiveTo is in the past is skipped).

Usage: JDX9_PASSWORD=admin run_t37.py
"""
import json
import subprocess
import sys
import time
import urllib.request
from datetime import date, timedelta

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


def cmbb(f, p):
    post(f, "API-cmbb-data", CK, p)


def dmbb(f, p):
    post(f, "API-dmbb-data", DK, p)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def make_case(cid, tin, cat, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "P1 Test", "amountAtStake": amount, "category": cat,
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


def main():
    assert CK and DK, "missing api keys"
    # SINGLE band carries a 7-day SLA (config); plus two overlapping high bands for effective-dating
    cmbb("mmAuthority", {"id": "AUTH-IA-S", "actionType": "INSTALMENT_PLAN", "amountMin": "5000.01",
                         "amountMax": "20000", "level": "SUPERVISOR", "bodyType": "SINGLE", "slaDays": "7"})
    cmbb("mmAuthority", {"id": "AUTH-IA-OLD", "actionType": "INSTALMENT_PLAN", "amountMin": "100000.01",
                         "amountMax": "200000", "level": "OFFICER", "bodyType": "SINGLE",
                         "effectiveTo": "2000-01-01"})
    cmbb("mmAuthority", {"id": "AUTH-IA-NEW", "actionType": "INSTALMENT_PLAN", "amountMin": "100000.01",
                         "amountMax": "200000", "level": "DIRECTOR", "bodyType": "SINGLE",
                         "effectiveFrom": "2020-01-01"})
    for fr, to in [("Pending", "Approved"), ("Pending", "Rejected"), ("Pending", "Returned")]:
        cmbb("mmEntityTransition", {"id": f"tr-ap-{to.lower()}", "entity": "cmApproval",
                                    "scope": "DEFAULT", "fromStatus": fr, "toStatus": to})
    time.sleep(2)

    # ---------- T-37.1 SLA from config ----------
    cS, tS, aS = f"apP1S-{RUN}", f"T37S{RUN}", f"agrP1S-{RUN}"
    make_case(cS, tS, "C4", "8000")
    submit_plan(aS, tS, cS, "8000", "12")
    apIdS = pending_id(aS)
    dl = sql(f"SELECT c_deadline FROM app_fd_cmapproval WHERE id='{apIdS}'")
    want = (date.today() + timedelta(days=7)).isoformat()
    check("T-37.1 decision SLA comes from mmAuthority config (slaDays=7 -> deadline +7d)",
          bool(apIdS) and dl.startswith(want),
          f"approval={apIdS} deadline={dl} expectedDatePrefix={want}")

    # ---------- T-37.2 effective-dating picks the current band ----------
    cD, tD, aD = f"apP1D-{RUN}", f"T37D{RUN}", f"agrP1D-{RUN}"
    make_case(cD, tD, "C4", "150000")
    submit_plan(aD, tD, cD, "150000", "12")
    apIdD = pending_id(aD)
    lvl = sql(f"SELECT c_requiredlevel FROM app_fd_cmapproval WHERE id='{apIdD}'")
    check("T-37.2 effective-dating: the current band (DIRECTOR) wins over the expired one (OFFICER)",
          bool(apIdD) and lvl == "DIRECTOR",
          f"approval={apIdD} requiredLevel={lvl}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
