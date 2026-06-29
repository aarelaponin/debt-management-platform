#!/usr/bin/env python3
"""DMBB-F05 acceptance T-14.1..3 — live jdx9 (apps cmbb + dmbb).

T-14.1 enforcement triggers for a clean case (no objection) -> ENFORCEMENT_TRIGGERED, step advances to garnishing
T-14.2 enforcement BLOCKED for a full-amount-objection case -> ENFORCEMENT_BLOCKED, holds at final demand
T-14.3 strategy consistency check -> VALID (seeded STD-MLT + Malta-14 instruments are consistent)

Usage: JDX9_PASSWORD=admin run_t14.py
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


def make_case(cid, tin, disputed):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM",
                    "taxType": "VAT", "taxpayerName": "Enf Test", "amountAtStake": "1000",
                    "category": "C6", "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": "C6", "stage": "Identified",
                    "triggerOrigin": "TEST", "consolidatedAmount": "1000", "lastStepSeq": "0"})
    dmbb("dmLine", {"id": f"{cid}-L1", "caseId": cid, "taxType": "VAT", "yofa": "2024",
                    "amount": "1000", "disputed": disputed, "enforceable": "1000"})


def sweep(days):
    rid = f"er-{RUN}-{int(time.time()) % 1000}"
    asof = (datetime.now() + timedelta(days=days)).strftime("%Y-%m-%dT%H:%M:%S")
    dmbb("cmEscalateRun", {"id": rid, "asOf": asof})
    time.sleep(5)


def main():
    assert CK and DK
    a = f"enfA-{RUN}"  # clean
    b = f"enfB-{RUN}"  # full-amount objection
    make_case(a, f"T14A{RUN}", "0")
    make_case(b, f"T14B{RUN}", "1000")
    sweep(60)  # cum: r7, d21, f42, garnish56 (<=60); seizure86 (>60)

    # ---------- T-14.1 ----------
    stage_a = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{a}'")
    seq_a = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{a}'")
    trig_a = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{a}' AND c_eventtype='ENFORCEMENT_TRIGGERED'")
    check("T-14.1 clean case: enforcement triggers (garnishing step)",
          stage_a == "Bank garnishing" and seq_a == "4" and int(trig_a) >= 1,
          f"stage={stage_a} seq={seq_a} triggered={trig_a}")

    # ---------- T-14.2 ----------
    stage_b = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{b}'")
    seq_b = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{b}'")
    blk_b = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{b}' AND c_eventtype='ENFORCEMENT_BLOCKED'")
    trig_b = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{b}' AND c_eventtype='ENFORCEMENT_TRIGGERED'")
    check("T-14.2 full-objection case: enforcement BLOCKED at final demand",
          stage_b == "Final demand" and seq_b == "3" and int(blk_b) >= 1 and trig_b == "0",
          f"stage={stage_b} seq={seq_b} blocked={blk_b} triggered={trig_b}")

    # ---------- T-14.3 ----------
    cid = f"sc-{RUN}"
    dmbb("cmStrategyCheck", {"id": cid})
    time.sleep(4)
    valid = sql(f"SELECT c_valid FROM app_fd_cmstrategycheck WHERE id='{cid}'")
    issues = sql(f"SELECT c_issuecount FROM app_fd_cmstrategycheck WHERE id='{cid}'")
    check("T-14.3 strategy consistency check returns VALID",
          valid == "true" and issues == "0", f"valid={valid} issues={issues}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
