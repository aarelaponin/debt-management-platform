#!/usr/bin/env python3
"""DMBB-F12 acceptance T-21.1..4 — live jdx9 (apps cmbb + dmbb). MODULE CLOSER.

T-21.1 plan-vs-actual rollup (DM-FR-047): a plan target's actual recovered is filled from real recoveries
T-21.2 target suggestion (BR-DM-043): suggested target = current category stock x recovery rate
T-21.3 MI report (DM-FR-048): the deployed list_debtByCategory JDBC report returns category totals
T-21.4 ②/③ reconciliation (CAD §132): every WRITEOFF_POSTED event has its dmCharge WRITE_OFF (0 variances)

Usage: JDX9_PASSWORD=admin run_t21.py
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


def fnum(s):
    try:
        return float(s)
    except (TypeError, ValueError):
        return None


def make_debt_case(cid, tin, cat, amount, writeoff=""):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "MI " + tin, "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Final demand",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "3",
                    "writeOffStatus": writeoff})
    # managed fields are read-only → the data API drops them on store; seed directly (fixture).
    sql(f"UPDATE app_fd_cmcase SET c_category='{cat}', c_currentstate='OPEN', c_amountatstake='{amount}' WHERE id='{cid}'")
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='{cat}', c_stage='Final demand', c_triggerorigin='TEST', c_consolidatedamount='{amount}', c_laststepseq='3', c_writeoffstatus='{writeoff}' WHERE id='{cid}'")


def mirun(rid, mode, plan="", asof=""):
    dmbb("cmCollectionMiRun", {"id": rid, "runMode": mode, "planId": plan, "asOf": asof})
    time.sleep(4)


def deployed_sql(dl):
    j = sql(f"SELECT json FROM app_datalist WHERE id='{dl}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    d = json.loads(j)
    binder = d.get("binder") or d.get("properties", {}).get("binder") or {}
    return (binder.get("properties", binder))["sql"]


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-21.1 plan rollup (real C5 recovery via garnish) ----------
    c5 = f"mi5-{RUN}"
    make_debt_case(c5, f"T21A{RUN}", "C5", "3000")
    dmbb("dmAction", {"id": f"act5-{RUN}", "debtCaseId": c5, "tin": f"T21A{RUN}",
                      "instrument": "BANK_GARNISH", "amount": "3000", "status": "INITIATED"})
    time.sleep(4)  # garnish executes -> recoveredAmount 3000
    plan1 = f"plan1-{RUN}"
    dmbb("dmCollectionPlan", {"id": plan1, "code": f"PL1{RUN}", "name": "Q3 plan",
                              "scope": "NATIONAL", "period": "2026-Q3", "status": "DRAFT"})
    tgt1 = f"tg1-{RUN}"
    dmbb("dmPlanTarget", {"id": tgt1, "planId": plan1, "category": "C5", "targetAmount": "10000"})
    mirun(f"roll-{RUN}", "ROLLUP", plan=plan1)
    actual = fnum(sql(f"SELECT c_actualamount FROM app_fd_dmplantarget WHERE id='{tgt1}'"))
    attain = fnum(sql(f"SELECT c_attainmentpct FROM app_fd_dmplantarget WHERE id='{tgt1}'"))
    check("T-21.1 plan-vs-actual rollup fills actual + attainment (DM-FR-047)",
          actual is not None and actual >= 3000 and attain is not None and attain >= 30,
          f"actual={actual} attain={attain}")

    # ---------- T-21.2 target suggestion ----------
    plan2 = f"plan2-{RUN}"
    dmbb("dmCollectionPlan", {"id": plan2, "code": f"PL2{RUN}", "scope": "NATIONAL",
                              "period": "2026-Q4", "status": "DRAFT"})
    tgt2 = f"tg2-{RUN}"
    dmbb("dmPlanTarget", {"id": tgt2, "planId": plan2, "category": "C5", "targetAmount": "0"})
    # add open C5 stock
    make_debt_case(f"mi5b-{RUN}", f"T21B{RUN}", "C5", "10000")
    mirun(f"sug-{RUN}", "SUGGEST", plan=plan2)
    sug = fnum(sql(f"SELECT c_suggestedamount FROM app_fd_dmplantarget WHERE id='{tgt2}'"))
    check("T-21.2 target suggested from stock x recovery rate (BR-DM-043)",
          sug is not None and sug >= 4000, f"suggested={sug} (C5 stock x 0.40)")

    # ---------- T-21.3 MI report datalist ----------
    base = deployed_sql("list_debtByCategory")
    c5row = sql(f"SELECT total_debt FROM ({base}) q WHERE q.category='C5'")
    check("T-21.3 MI report list_debtByCategory returns category totals (DM-FR-048)",
          fnum(c5row) is not None and fnum(c5row) >= 10000, f"C5 total_debt={c5row}")

    # ---------- T-21.4 ②/③ reconciliation ----------
    c1 = f"mi1-{RUN}"
    make_debt_case(c1, f"T21C{RUN}", "C1", "20")
    dmbb("cmWriteOffRun", {"id": f"woc1-{RUN}", "sweepMode": "AUTO_C1"})  # posts WRITEOFF_POSTED + dmCharge
    time.sleep(5)
    mirun(f"rec-{RUN}", "RECONCILE")
    matched = int(sql(f"SELECT c_matchedcount FROM app_fd_cmcollectionmirun WHERE id='rec-{RUN}'") or "0")
    variance = int(sql(f"SELECT c_variancecount FROM app_fd_cmcollectionmirun WHERE id='rec-{RUN}'") or "-1")
    check("T-21.4 ②/③ reconciliation: every WRITEOFF_POSTED has its dmCharge (CAD §132)",
          matched >= 1 and variance == 0, f"matched={matched} variance={variance}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
