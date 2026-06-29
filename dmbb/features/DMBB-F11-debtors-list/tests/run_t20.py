#!/usr/bin/env python3
"""DMBB-F11 acceptance T-20.1..4 — live jdx9 (apps cmbb + dmbb).

T-20.1 debtors list (DM-FR-057): the DEPLOYED list_debtorsList JDBC query returns open DM debtors with the right total + category
T-20.2 exclusion (DM-FR-057): a written-off debt case is NOT in the debtors list
T-20.3 registry extract (DM-FR-058): EXTRACT over PUBLISHED dmDebtorPub -> count/total/payload, GENERATED + event
T-20.4 extract excludes resolved (DM-FR-058): a REMOVED dmDebtorPub entry is not in the extract payload

Usage: JDX9_PASSWORD=admin run_t20.py
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


def deployed_sql():
    j = sql("SELECT json FROM app_datalist WHERE id='list_debtorsList' AND appid='dmbb' "
            "ORDER BY appversion DESC LIMIT 1")
    d = json.loads(j)
    # binder is the datalist's binder; sql lives in its properties
    binder = d.get("binder") or d.get("properties", {}).get("binder") or {}
    props = binder.get("properties", binder)
    # UX-QA: the deployed SQL now carries #requestParam.X# filter guards that Joget substitutes at render
    # time (empty when no param). Running the SQL raw in psql, simulate the no-param default → strip them to ''.
    import re
    return re.sub(r"#requestParam\.[^#]+#", "", props["sql"])


def make_debt_case(cid, tin, cat, amount, writeoff=""):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "DL " + tin, "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Final demand",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount, "lastStepSeq": "3",
                    "writeOffStatus": writeoff})
    # managed fields are read-only → the data API drops them on store; seed directly (fixture).
    sql(f"UPDATE app_fd_cmcase SET c_category='{cat}', c_currentstate='OPEN', c_amountatstake='{amount}' WHERE id='{cid}'")
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='{cat}', c_stage='Final demand', c_triggerorigin='TEST', c_consolidatedamount='{amount}', c_laststepseq='3', c_writeoffstatus='{writeoff}' WHERE id='{cid}'")


def main():
    assert CK and DK, "missing api keys"
    base = deployed_sql()
    assert base and "app_fd_cmcase" in base, "could not read deployed list_debtorsList SQL"

    def listed(tin, col="*"):
        return sql(f"SELECT {col} FROM ({base}) q WHERE q.tin='{tin}'")

    # ---------- T-20.1 list shows open debtors ----------
    a, b = f"dlA-{RUN}", f"dlB-{RUN}"
    ta, tb = f"T20A{RUN}", f"T20B{RUN}"
    make_debt_case(a, ta, "C3", "1500")
    make_debt_case(b, tb, "C4", "5000")
    cnt_a = listed(ta, "count(*)")
    debt_a = listed(ta, "total_debt")
    cat_b = listed(tb, "category")
    check("T-20.1 debtors list returns open DM debtors with total + category (DM-FR-057)",
          cnt_a == "1" and debt_a == "1500" and cat_b == "C4",
          f"countA={cnt_a} debtA={debt_a} catB={cat_b}")

    # ---------- T-20.2 written-off excluded ----------
    c = f"dlC-{RUN}"
    tc = f"T20C{RUN}"
    make_debt_case(c, tc, "C3", "700", writeoff="written-off")
    cnt_c = listed(tc, "count(*)")
    check("T-20.2 written-off debt excluded from the list (DM-FR-057)",
          cnt_c == "0", f"countC={cnt_c}")

    # ---------- T-20.3 registry extract ----------
    p1, p2 = f"T20P1{RUN}", f"T20P2{RUN}"
    dmbb("dmDebtorPub", {"id": f"pub1-{RUN}", "debtCaseId": a, "tin": p1, "debtorName": "Pub One",
                         "debtAmount": "1000", "registry": "national", "status": "PUBLISHED"})
    dmbb("dmDebtorPub", {"id": f"pub2-{RUN}", "debtCaseId": b, "tin": p2, "debtorName": "Pub Two",
                         "debtAmount": "500", "registry": "national", "status": "PUBLISHED"})
    pr = f"T20PR{RUN}"
    dmbb("dmDebtorPub", {"id": f"pub3-{RUN}", "debtCaseId": c, "tin": pr, "debtorName": "Resolved",
                         "debtAmount": "800", "registry": "national", "status": "REMOVED"})
    xid = f"ext-{RUN}"
    dmbb("dmDebtorsExtract", {"id": xid, "registry": "national debt registry", "format": "CSV"})
    time.sleep(4)
    st = sql(f"SELECT c_status FROM app_fd_dmdebtorsextract WHERE id='{xid}'")
    cnt = int(sql(f"SELECT c_debtorcount FROM app_fd_dmdebtorsextract WHERE id='{xid}'") or "0")
    payload = sql(f"SELECT c_payload FROM app_fd_dmdebtorsextract WHERE id='{xid}'")
    ev = int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='EXTRACT-{xid}' "
                 "AND c_eventtype='DEBTORS_EXTRACT_GENERATED'"))
    check("T-20.3 registry extract generated from PUBLISHED debtors (DM-FR-058)",
          st == "GENERATED" and cnt >= 2 and p1 in payload and p2 in payload and ev >= 1,
          f"status={st} count={cnt} p1in={p1 in payload} p2in={p2 in payload}")

    # ---------- T-20.4 removed excluded from extract ----------
    check("T-20.4 resolved (REMOVED) debtor excluded from the extract (DM-FR-058)",
          pr not in payload, f"resolvedTinInPayload={pr in payload}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
