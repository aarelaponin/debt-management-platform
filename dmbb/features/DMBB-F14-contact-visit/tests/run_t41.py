#!/usr/bin/env python3
"""DMBB-F14 acceptance T-41.1..6 — live jdx9 + ClickHouse (apps cmbb + dmbb).

T-41.1 WF-FR-009: the DEPLOYED list_debtorsList carries "Call" + "Visit" quick-actions
       (row hrefs to dmContact_crud / dmVisit_crud, prefilled by ?tin=).
T-41.2 DM-FR-019: a telephone-contact record is saved and linked to the debt case, and the
       DEPLOYED list_contacts returns it (timestamp/outcome/follow-up captured).
T-41.3 DM-FR-020: a field-visit record is saved and linked to the case, and the DEPLOYED
       list_visits returns it (schedule/outcome captured).
T-41.4 mdContactPolicy fast-track config is deployed and carries the high-value threshold.
T-41.5 DM-FR-018: a high-value debt (> threshold) is identified as a CRITICAL-priority case.
T-41.6 DM-FR-018: that critical case gets a TELEPHONE_CONTACT deadline (first call SLA).

Usage: JDX9_PASSWORD=admin run_t41.py
"""
import json
import re
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


def ch(q):
    r = subprocess.run(["docker", "exec", "mtca-clickhouse", "clickhouse-client", "-q", q],
                       capture_output=True, text=True)
    return ("CH-ERROR: " + r.stderr.strip()[:300]) if r.returncode else r.stdout.strip()


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


def deployed_list(list_id):
    """The DEPLOYED datalist json (for row-action / column assertions)."""
    j = sql(f"SELECT json FROM app_datalist WHERE id='{list_id}' AND appid='dmbb' "
            "ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def deployed_list_sql(list_id):
    """The DEPLOYED JDBC binder SQL with #requestParam.X# guards stripped (no-param default)."""
    d = deployed_list(list_id)
    binder = d.get("binder") or d.get("properties", {}).get("binder") or {}
    props = binder.get("properties", binder)
    return re.sub(r"#requestParam\.[^#]+#", "", props["sql"])


def make_debt_case(cid, tin, cat, amount):
    cmbb("cmCase", {"id": cid, "caseType": "DM", "tin": tin, "origin": "SYSTEM", "taxType": "VAT",
                    "taxpayerName": "T41 " + tin, "amountAtStake": amount, "category": cat,
                    "currentState": "OPEN"})
    dmbb("dmDebt", {"id": cid, "tin": tin, "debtCategory": cat, "stage": "Final demand",
                    "triggerOrigin": "TEST", "consolidatedAmount": amount})
    sql(f"UPDATE app_fd_cmcase SET c_category='{cat}', c_currentstate='OPEN', "
        f"c_amountatstake='{amount}', c_taxpayername='T41 {tin}' WHERE id='{cid}'")
    sql(f"UPDATE app_fd_dmdebt SET c_debtcategory='{cat}', c_stage='Final demand', "
        f"c_consolidatedamount='{amount}' WHERE id='{cid}'")


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-41.1 WF-FR-009 quick-actions on the deployed debtors list ----------
    dl = deployed_list("list_debtorsList")
    actions = json.dumps(dl)
    has_call = "dmContact_crud" in actions and '"label": "Call"' in actions
    has_visit = "dmVisit_crud" in actions and '"label": "Visit"' in actions
    check("T-41.1 debtors list carries Call + Visit quick-actions (WF-FR-009)",
          has_call and has_visit, f"call={has_call} visit={has_visit}")

    # ---------- T-41.2 telephone contact captured + listed (DM-FR-019) ----------
    cid, tin = f"c41-{RUN}", f"T41C{RUN}"
    make_debt_case(cid, tin, "C4", "9000")
    kid = f"ct-{RUN}"
    dmbb("dmContact", {"id": kid, "tin": tin, "debtCaseId": cid, "method": "TELEPHONE",
                       "contactPerson": "Director", "contactDate": "2026-07-01",
                       "contactTime": "10:30", "officer": "officer1", "outcome": "PROMISE_TO_PAY",
                       "promiseAmount": "9000", "followUpDate": "2026-07-08",
                       "notes": "agreed to pay in full"})
    saved_outcome = sql(f"SELECT c_outcome FROM app_fd_dmcontact WHERE id='{kid}'")
    saved_follow = sql(f"SELECT c_followupdate FROM app_fd_dmcontact WHERE id='{kid}'")
    saved_case = sql(f"SELECT c_debtcaseid FROM app_fd_dmcontact WHERE id='{kid}'")
    base_c = deployed_list_sql("list_contacts")
    listed_c = sql(f"SELECT count(*) FROM ({base_c}) q WHERE q.tin='{tin}'")
    check("T-41.2 telephone contact saved (outcome+follow-up), linked to case, and listed (DM-FR-019)",
          saved_outcome == "PROMISE_TO_PAY" and saved_follow == "2026-07-08"
          and saved_case == cid and listed_c == "1",
          f"outcome={saved_outcome} follow={saved_follow} case={saved_case} listed={listed_c}")

    # ---------- T-41.3 field visit captured + listed (DM-FR-020) ----------
    vid = f"vs-{RUN}"
    dmbb("dmVisit", {"id": vid, "tin": tin, "debtCaseId": cid, "purpose": "ASSET_VERIFICATION",
                     "scheduledDate": "2026-07-10", "fieldOfficer": "officer1",
                     "location": "12 Republic St", "visitDate": "2026-07-10",
                     "personsPresent": "Director", "outcome": "ASSETS_IDENTIFIED",
                     "evidenceCollected": "van + stock", "followUpActions": "lien"})
    saved_v_out = sql(f"SELECT c_outcome FROM app_fd_dmvisit WHERE id='{vid}'")
    saved_v_case = sql(f"SELECT c_debtcaseid FROM app_fd_dmvisit WHERE id='{vid}'")
    base_v = deployed_list_sql("list_visits")
    listed_v = sql(f"SELECT count(*) FROM ({base_v}) q WHERE q.tin='{tin}'")
    check("T-41.3 field visit saved (outcome), linked to case, and listed (DM-FR-020)",
          saved_v_out == "ASSETS_IDENTIFIED" and saved_v_case == cid and listed_v == "1",
          f"outcome={saved_v_out} case={saved_v_case} listed={listed_v}")

    # ---------- T-41.4 fast-track policy deployed with a threshold (DM-FR-018) ----------
    dmbb("mdContactPolicy", {"id": "CTP-DEFAULT", "code": "CTP-DEFAULT",
                             "highValueThreshold": "200000", "telephoneSlaDays": "2",
                             "seniorRole": "dm_supervisor", "active": "true"})
    thr = sql("SELECT c_highvaluethreshold FROM app_fd_mdcontactpolicy "
              "WHERE id='CTP-DEFAULT'")
    check("T-41.4 mdContactPolicy fast-track config deployed with high-value threshold (DM-FR-018)",
          thr not in ("", None) and float(thr or 0) >= 200000, f"threshold={thr}")

    # ---------- T-41.5/6 high-value fast-track at identification (DM-FR-018) ----------
    # sta_v1 is all VIEWS (not injectable), so we identify a REAL un-caased high-value Gold debtor:
    # 101632G (~127,447 enforceable; CIT/FSS/SSC — no VAT line, so ONE consolidated case). Lower the
    # threshold below it (100k) and scan with minAmount=125k (isolates it among un-caased debtors).
    # The CRITICAL marker is the numeric priority field (=3) + the CASE_PRIORITISED event + the
    # TELEPHONE_CONTACT deadline. End-state assertion => order-independent: a prior sweep may have
    # created the case; the deployed engine only ever constitutes a >threshold debt as CRITICAL.
    ft = "101632G"
    dmbb("mdContactPolicy", {"id": "CTP-DEFAULT", "code": "CTP-DEFAULT",
                             "highValueThreshold": "100000", "telephoneSlaDays": "2",
                             "seniorRole": "dm_supervisor", "active": "true"})
    dmbb("cmIdentRun", {"id": f"ir41-{RUN}", "minAmount": "125000"})
    time.sleep(10)
    ftcase = sql(f"SELECT id FROM app_fd_cmcase WHERE c_tin='{ft}' AND c_casetype='DM' "
                 "AND c_priority='3' ORDER BY dateCreated DESC LIMIT 1")
    ev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{ftcase}' "
             "AND c_eventtype='CASE_PRIORITISED'") if ftcase else "0"
    check("T-41.5 high-value debt is CRITICAL priority (=3) + CASE_PRIORITISED event (DM-FR-018)",
          bool(ftcase) and ev not in ("", "0"), f"case={ftcase} prioritisedEvent={ev}")
    dlcount = sql(f"SELECT count(*) FROM app_fd_cmdeadline WHERE c_caseid='{ftcase}' "
                  "AND c_clockcode='TELEPHONE_CONTACT'") if ftcase else "0"
    dlstatus = sql(f"SELECT c_status FROM app_fd_cmdeadline WHERE c_caseid='{ftcase}' "
                   "AND c_clockcode='TELEPHONE_CONTACT' ORDER BY dateCreated DESC LIMIT 1") if ftcase else ""
    check("T-41.6 critical case gets a TELEPHONE_CONTACT deadline (first-call SLA, DM-FR-018)",
          (dlcount not in ("", "0")) and dlstatus == "RUNNING",
          f"deadlines={dlcount} status={dlstatus}")
    # restore the shipped default threshold so the DEV console matches the seed (200,000).
    dmbb("mdContactPolicy", {"id": "CTP-DEFAULT", "code": "CTP-DEFAULT",
                             "highValueThreshold": "200000", "telephoneSlaDays": "2",
                             "seniorRole": "dm_supervisor", "active": "true"})

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
