#!/usr/bin/env python3
"""DMBB-F07 acceptance T-16.1..8 — live jdx9 (apps cmbb + dmbb).

T-16.1 bank garnishing (ADMINISTRATIVE): dmAction BANK_GARNISH on C3 -> EXECUTED, SUCCESS, recovered + legal fee (DM-FR-029/038, BR-DM-033/035)
T-16.2 proportional gate: PROPERTY_SEIZURE (min C4) on a C3 case -> BLOCKED (BR-DM-031)
T-16.3 hold-sensitive block: BANK_GARNISH on a case with an active ENFORCEMENT_SUPPRESS hold -> BLOCKED (F05/F06 seam)
T-16.4 judicial: BANKRUPTCY (min C4, >=20000) on C4 -> SUBMITTED to court + legal fee (DM-FR-035)
T-16.5 agent appoint + report: dmAgent (C4) -> APPOINTED + fee; dmAgentRpt recovered 1000 -> commission accrued (DM-FR-037, BR-DM-045)
T-16.6 agent gate: dmAgent on a C3 case -> REJECTED (BR-DM-044)
T-16.7 publish + release sweep: PUBLISH lists an eligible C3 debtor; RELEASE removes it on resolution (DM-FR-030)
T-16.8 agent non-report alert: AGENT_ALERT flags an agent past its reporting interval (BR-DM-046)

Usage: JDX9_PASSWORD=admin run_t16.py
"""
import json
import subprocess
import sys
import time
from datetime import datetime, timedelta
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


def fnum(s):
    try:
        return float(s)
    except (TypeError, ValueError):
        return None


def evc(case, etype):
    return int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{case}' AND c_eventtype='{etype}'"))


def debt_case(cid, tin, cat, amount, disputed, step="0"):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "Enf " + tin, "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Final demand",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": step})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": amount, "disputed": disputed, "enforceable": amount})
    # managed fields are read-only → the data API drops them on store (engines set them via
    # FormDataDao in production); seed them directly for the test scenario (fixture exception).
    sql(f"UPDATE app_fd_cmcase SET c_category='{cat}', c_currentstate='OPEN', c_amountatstake='{amount}' WHERE id='{cid}'")
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='{cat}', c_stage='Final demand', c_triggerorigin='TEST', c_consolidatedamount='{amount}', c_laststepseq='{step}' WHERE id='{cid}'")


def action(aid, cid, tin, instrument, amount):
    dmbb("dmAction", {"id": aid, "debtCaseId": cid, "tin": tin, "instrument": instrument,
                      "amount": amount, "status": "INITIATED"})
    time.sleep(4)


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-16.1 garnish ----------
    c1, a1 = f"enf1-{RUN}", f"act1-{RUN}"
    debt_case(c1, f"T16A{RUN}", "C3", "2000", "0")
    action(a1, c1, f"T16A{RUN}", "BANK_GARNISH", "2000")
    st = sql(f"SELECT c_status FROM app_fd_dmaction WHERE id='{a1}'")
    rs = sql(f"SELECT c_responsestatus FROM app_fd_dmaction WHERE id='{a1}'")
    rec = fnum(sql(f"SELECT c_recoveredamount FROM app_fd_dmaction WHERE id='{a1}'"))
    cost = fnum(sql(f"SELECT c_costamount FROM app_fd_dmaction WHERE id='{a1}'"))
    charges = int(sql(f"SELECT count(*) FROM app_fd_dmcharge WHERE c_debtcaseid='{c1}' AND c_chargetype='LEGAL_FEE'"))
    check("T-16.1 garnish EXECUTED + recovered + legal fee",
          st == "EXECUTED" and rs == "SUCCESS" and rec == 2000.0 and cost == 50.0
          and charges == 1 and evc(c1, "GARNISH_CONFIRMED") >= 1 and evc(c1, "LEGAL_FEE_POSTED") >= 1,
          f"status={st} resp={rs} recovered={rec} cost={cost} charges={charges}")

    # ---------- T-16.2 proportional gate ----------
    c2, a2 = f"enf2-{RUN}", f"act2-{RUN}"
    debt_case(c2, f"T16B{RUN}", "C3", "8000", "0")
    action(a2, c2, f"T16B{RUN}", "PROPERTY_SEIZURE", "8000")
    st2 = sql(f"SELECT c_status FROM app_fd_dmaction WHERE id='{a2}'")
    check("T-16.2 PROPERTY_SEIZURE on C3 BLOCKED (BR-DM-031)",
          st2 == "BLOCKED" and evc(c2, "ENFORCEMENT_ACTION_BLOCKED") >= 1, f"status={st2}")

    # ---------- T-16.3 hold-sensitive block ----------
    c3, a3 = f"enf3-{RUN}", f"act3-{RUN}"
    debt_case(c3, f"T16C{RUN}", "C4", "3000", "0")
    cmbb("cmHold", {"id": f"h3-{RUN}", "caseId": c3, "caseRef": c3, "scope": "ENFORCEMENT_SUPPRESS",
                    "holdType": "INSTALMENT", "basis": "active instalment (test)",
                    "status": "ACTIVE", "assertedBy": "test"})
    action(a3, c3, f"T16C{RUN}", "BANK_GARNISH", "3000")
    st3 = sql(f"SELECT c_status FROM app_fd_dmaction WHERE id='{a3}'")
    check("T-16.3 garnish BLOCKED by ENFORCEMENT_SUPPRESS hold",
          st3 == "BLOCKED" and evc(c3, "ENFORCEMENT_ACTION_BLOCKED") >= 1, f"status={st3}")

    # ---------- T-16.4 judicial bankruptcy ----------
    c4, a4 = f"enf4-{RUN}", f"act4-{RUN}"
    debt_case(c4, f"T16D{RUN}", "C4", "25000", "0")
    action(a4, c4, f"T16D{RUN}", "BANKRUPTCY", "25000")
    st4 = sql(f"SELECT c_status FROM app_fd_dmaction WHERE id='{a4}'")
    ext4 = sql(f"SELECT c_externalref FROM app_fd_dmaction WHERE id='{a4}'")
    cost4 = fnum(sql(f"SELECT c_costamount FROM app_fd_dmaction WHERE id='{a4}'"))
    check("T-16.4 BANKRUPTCY SUBMITTED to court + legal fee",
          st4 == "SUBMITTED" and ext4.startswith("COURT-") and cost4 == 300.0,
          f"status={st4} ext={ext4} cost={cost4}")

    # ---------- T-16.5 agent appoint + report ----------
    c5, ap5 = f"enf5-{RUN}", f"appt5-{RUN}"
    debt_case(c5, f"T16E{RUN}", "C4", "10000", "0")
    dmbb("dmAgent", {"id": ap5, "debtCaseId": c5, "tin": f"T16E{RUN}", "agentId": "AG-77",
                     "agentName": "Agent 77", "status": "APPOINTED"})
    time.sleep(4)
    appfee = fnum(sql(f"SELECT c_appointmentfee FROM app_fd_dmagent WHERE id='{ap5}'"))
    dmbb("dmAgentRpt", {"id": f"rpt5-{RUN}", "agentApptId": ap5, "reportDate": "2026-06-12",
                        "actionsTaken": "visit + negotiation", "recoveredAmount": "1000"})
    time.sleep(4)
    comm = fnum(sql(f"SELECT c_commissionamount FROM app_fd_dmagentrpt WHERE id='rpt5-{RUN}'"))
    rtot = fnum(sql(f"SELECT c_recoveredtotal FROM app_fd_dmagent WHERE id='{ap5}'"))
    ctot = fnum(sql(f"SELECT c_commissiontotal FROM app_fd_dmagent WHERE id='{ap5}'"))
    check("T-16.5 agent APPOINTED + report accrues commission (BR-DM-045)",
          appfee == 200.0 and comm == 100.0 and rtot == 1000.0 and ctot == 100.0
          and evc(c5, "AGENT_APPOINTED") >= 1 and evc(c5, "AGENT_REPORTED") >= 1,
          f"appFee={appfee} comm={comm} recoveredTot={rtot} commTot={ctot}")

    # ---------- T-16.6 agent gate C3 ----------
    c6, ap6 = f"enf6-{RUN}", f"appt6-{RUN}"
    debt_case(c6, f"T16F{RUN}", "C3", "5000", "0")
    dmbb("dmAgent", {"id": ap6, "debtCaseId": c6, "tin": f"T16F{RUN}", "agentId": "AG-3",
                     "status": "APPOINTED"})
    time.sleep(4)
    st6 = sql(f"SELECT c_status FROM app_fd_dmagent WHERE id='{ap6}'")
    check("T-16.6 agent appointment REJECTED below C4 (BR-DM-044)",
          st6 == "REJECTED", f"status={st6}")

    # ---------- T-16.7 publish + release ----------
    c7 = f"enf7-{RUN}"
    debt_case(c7, f"T16G{RUN}", "C3", "2000", "0", step="3")  # past final demand
    dmbb("cmEnfActionRun", {"id": f"pub-{RUN}", "sweepMode": "PUBLISH"})
    time.sleep(5)
    pub = int(sql(f"SELECT count(*) FROM app_fd_dmdebtorpub WHERE c_debtcaseid='{c7}' AND c_status='PUBLISHED'"))
    # resolve the case then RELEASE
    cmbb("cmCase", {"id": c7, "currentState": "CLOSED"})
    dmbb("cmEnfActionRun", {"id": f"rel-{RUN}", "sweepMode": "RELEASE"})
    time.sleep(5)
    removed = int(sql(f"SELECT count(*) FROM app_fd_dmdebtorpub WHERE c_debtcaseid='{c7}' AND c_status='REMOVED'"))
    check("T-16.7 PUBLISH lists debtor then RELEASE removes on resolution (DM-FR-030)",
          pub >= 1 and removed >= 1 and evc(c7, "DEBTOR_PUBLISHED") >= 1 and evc(c7, "DEBTOR_UNPUBLISHED") >= 1,
          f"published={pub} removed={removed}")

    # ---------- T-16.8 agent non-report alert ----------
    c8, ap8 = f"enf8-{RUN}", f"appt8-{RUN}"
    debt_case(c8, f"T16H{RUN}", "C4", "9000", "0")
    dmbb("dmAgent", {"id": ap8, "debtCaseId": c8, "tin": f"T16H{RUN}", "agentId": "AG-8",
                     "status": "APPOINTED"})
    time.sleep(4)
    asof = (datetime.now() + timedelta(days=30)).strftime("%Y-%m-%dT%H:%M:%S")
    dmbb("cmEnfActionRun", {"id": f"al-{RUN}", "sweepMode": "AGENT_ALERT", "asOf": asof})
    time.sleep(5)
    check("T-16.8 AGENT_ALERT flags agent past reporting interval (BR-DM-046)",
          evc(c8, "AGENT_OVERDUE_ALERT") >= 1, f"alerts={evc(c8, 'AGENT_OVERDUE_ALERT')}")

    # ---------- T-16.9 dmAction status guarded + audited (ADR-003 migration) ----------
    # the garnish EXECUTED (c1, INITIATED->EXECUTED) and the proportional BLOCK
    # (c2, INITIATED->BLOCKED) each emit a STATUS_CHANGED row on their case chain.
    sc1 = evc(c1, "STATUS_CHANGED")
    sc2 = evc(c2, "STATUS_CHANGED")
    check("T-16.9 dmAction status guarded + audited (EXECUTED + BLOCKED → STATUS_CHANGED)",
          sc1 >= 1 and sc2 >= 1, f"executed_case_sc={sc1} blocked_case_sc={sc2}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
