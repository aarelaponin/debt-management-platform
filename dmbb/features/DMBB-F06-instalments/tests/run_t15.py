#!/usr/bin/env python3
"""DMBB-F06 acceptance T-15.1..4 — live jdx9 (apps cmbb + dmbb).

T-15.1 eligible first-time small plan -> ACTIVE (auto-approved), 12 schedule lines, ENFORCEMENT_SUPPRESS hold asserted
T-15.2 enforcement BLOCKED while the plan is active (F05 gate honours the instalment hold) -> ENFORCEMENT_BLOCKED, holds at final demand
T-15.3 a second plan for the same taxpayer -> REJECTED (BR-DM-018 single active plan)
T-15.4 a defaulted plan -> auto-CANCELLED, hold RELEASED, recovery debt case opened (BR-DM-027/029)

Usage: JDX9_PASSWORD=admin run_t15.py
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


def make_debt_case(cid, tin, cat, amount, disputed):
    """An open DM debt case with one enforceable line (disputed controls full-objection)."""
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM",
                    "taxType": "VAT", "taxpayerName": "IA Test", "amountAtStake": amount,
                    "category": cat, "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "0"})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": amount, "disputed": disputed, "enforceable": amount})


def apply_plan(agr, tin, cid, total, months):
    dmbb("dmInstAgr", {"id": agr, "tin": tin, "debtCaseId": cid, "totalDebt": total,
                       "durationMonths": months, "status": "APPLIED"})
    time.sleep(5)


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-15.1 eligible -> ACTIVE + hold ----------
    caseA, tinA, agrA = f"iaA-{RUN}", f"T15A{RUN}", f"agrA-{RUN}"
    make_debt_case(caseA, tinA, "C4", "4000", "0")   # no objection: disputed 0 < enforceable
    apply_plan(agrA, tinA, caseA, "4000", "12")
    statusA = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{agrA}'")
    linesA = sql(f"SELECT count(*) FROM app_fd_dminstline WHERE c_instagrid='{agrA}'")
    holdA = sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='ACTIVE'" % caseA)
    apprA = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseA}' "
                "AND c_eventtype='INSTALMENT_APPROVED'")
    check("T-15.1 eligible first plan auto-approves with hold + 12 lines",
          statusA == "ACTIVE" and linesA == "12" and int(holdA) >= 1 and int(apprA) >= 1,
          f"status={statusA} lines={linesA} hold={holdA} approved={apprA}")

    # ---------- T-15.2 enforcement blocked by the active plan (F05 gate) ----------
    rid = f"er-{RUN}"
    asof60 = (datetime.now() + timedelta(days=60)).strftime("%Y-%m-%dT%H:%M:%S")
    dmbb("cmEscalateRun", {"id": rid, "asOf": asof60})  # cum r7 d21 f42 garnish56 (<=60)
    time.sleep(6)
    stageA = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{caseA}'")
    seqA = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{caseA}'")
    blkA = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseA}' "
               "AND c_eventtype='ENFORCEMENT_BLOCKED'")
    trgA = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseA}' "
               "AND c_eventtype='ENFORCEMENT_TRIGGERED'")
    check("T-15.2 active plan blocks enforcement at final demand (hold honoured by F05)",
          stageA == "Final demand" and seqA == "3" and int(blkA) >= 1 and trgA == "0",
          f"stage={stageA} seq={seqA} blocked={blkA} triggered={trgA}")

    # ---------- T-15.3 second plan same taxpayer -> REJECTED ----------
    agrA2 = f"agrA2-{RUN}"
    apply_plan(agrA2, tinA, caseA, "1000", "6")
    statusA2 = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{agrA2}'")
    rejA = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseA}' "
               "AND c_eventtype='INSTALMENT_REJECTED'")
    check("T-15.3 second active plan rejected (BR-DM-018)",
          statusA2 == "REJECTED" and int(rejA) >= 1, f"status={statusA2} rejected={rejA}")

    # ---------- T-15.4 default -> cancel -> recovery ----------
    caseD, tinD, agrD = f"iaD-{RUN}", f"T15D{RUN}", f"agrD-{RUN}"
    make_debt_case(caseD, tinD, "C4", "300", "0")
    apply_plan(agrD, tinD, caseD, "300", "3")
    pre = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{agrD}'")
    runD = f"cr-{RUN}"
    asof70 = (datetime.now() + timedelta(days=70)).strftime("%Y-%m-%dT%H:%M:%S")
    dmbb("cmInstComplianceRun", {"id": runD, "asOf": asof70})  # lines 1(+30d)&2(+60d) past due+grace, unpaid
    time.sleep(6)
    statusD = sql(f"SELECT c_status FROM app_fd_dminstagr WHERE id='{agrD}'")
    holdActiveD = sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                      "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='ACTIVE'" % caseD)
    holdRelD = sql("SELECT count(*) FROM app_fd_cmhold WHERE c_caseid='%s' "
                   "AND c_scope='ENFORCEMENT_SUPPRESS' AND c_status='RELEASED'" % caseD)
    recov = sql(f"SELECT count(*) FROM app_fd_dmdebt WHERE c_tin='{tinD}' "
                "AND c_triggerorigin='INSTALMENT_DEFAULT'")
    canEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseD}' "
                "AND c_eventtype='INSTALMENT_CANCELLED'")
    recEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseD}' "
                "AND c_eventtype='RECOVERY_CASE_CREATED'")
    check("T-15.4 defaulted plan cancels, releases hold, opens recovery (BR-DM-027/029)",
          pre == "ACTIVE" and statusD == "CANCELLED" and int(holdActiveD) == 0
          and int(holdRelD) >= 1 and int(recov) >= 1 and int(canEv) >= 1 and int(recEv) >= 1,
          f"pre={pre} status={statusD} holdActive={holdActiveD} holdRel={holdRelD} "
          f"recovery={recov} cancelled={canEv} recoveryEv={recEv}")

    # ---------- T-15.5 line-status writes now guarded + audited (ADR-003 migration) ----------
    # the two past-due lines (1:+30d, 2:+60d <= 70) move pending->missed via StatusManager,
    # each emitting a STATUS_CHANGED row on the case hash-chain; line 3 (+90d) stays pending.
    smEv = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseD}' "
               "AND c_eventtype='STATUS_CHANGED'")
    missed = sql(f"SELECT count(*) FROM app_fd_dminstline WHERE c_instagrid='{agrD}' "
                 "AND c_status='missed'")
    pend = sql(f"SELECT count(*) FROM app_fd_dminstline WHERE c_instagrid='{agrD}' "
               "AND c_status='pending'")
    check("T-15.5 dmInstLine status guarded + audited (2 missed STATUS_CHANGED, 1 still pending)",
          int(smEv) >= 2 and int(missed) == 2 and int(pend) == 1,
          f"statusChanged={smEv} missed={missed} pending={pend}")

    # ---------- T-15.6 dmInstAgr status guarded + audited (ADR-003 migration #5) ----------
    # caseA: agrA auto-approves (APPLIED->ACTIVE) and the 2nd plan rejects (APPLIED->REJECTED)
    # — two agreement-level STATUS_CHANGED rows on the case chain.
    agrSc = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{caseA}' "
                "AND c_eventtype='STATUS_CHANGED'")
    check("T-15.6 dmInstAgr status guarded + audited (approve + reject → 2 STATUS_CHANGED)",
          int(agrSc) >= 2, f"caseA_statusChanged={agrSc}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
