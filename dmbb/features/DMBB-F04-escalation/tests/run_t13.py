#!/usr/bin/env python3
"""DMBB-F04 acceptance T-13.1..4 — live jdx9 (apps cmbb + dmbb).

T-13.1 sweep@+10d -> step1 Reminder fires (dmDebt.stage, lastStepSeq=1, NOTIF_PENDING TPL-REMINDER)
T-13.2 sweep@+30d -> step2 Demand fires (lastStepSeq=2); step3 (cum42) not yet due
T-13.3 dispatch -> the escalation notices sent (cmNotif SENT, templates TPL-REMINDER/TPL-DEMAND)
T-13.4 idempotent -> re-sweep@+30d fires nothing new for the case

Usage: JDX9_PASSWORD=admin run_t13.py
"""
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timedelta
import urllib.request

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

BASE = "http://localhost:8089/jw"
PROC = "cmbb:latest:cmCaseEnvelope"
RESULTS = []
RUN = str(int(time.time()))[-6:]


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


class Joget:
    def __init__(self):
        c = JogetClient.from_instance("jdx9")
        c.get("/web/json/console/app/list", params={"pageSize": 1})
        self.s = getattr(c, "session", None) or c._session
        self.s.headers["Referer"] = BASE + "/web/console/home"

    def csrf(self):
        r = self.s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
        return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    def start(self, rid):
        r = self.s.post(f"{BASE}/web/json/workflow/process/start/{PROC}",
                        data={"OWASP_CSRFTOKEN": self.csrf(), "recordId": rid,
                              "var_caseId": rid, "var_assignee": "admin"})
        return r.json()

    def complete(self, sub, pid):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        for a in ([data] if isinstance(data, dict) else data):
            aid = a.get("activityId") or a.get("id", "")
            if sub in ((a.get("activityName") or "") + aid) and a.get("processId", "") == pid:
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return True
        return False


def as_of(days):
    return (datetime.now() + timedelta(days=days)).strftime("%Y-%m-%dT%H:%M:%S")


def sweep(days):
    rid = f"er-{RUN}-{int(time.time()) % 1000}"
    dmbb("cmEscalateRun", {"id": rid, "asOf": as_of(days)})
    time.sleep(4)
    return rid


def main():
    assert CK and DK
    jg = Joget()
    for code in ("officer1", "officer2"):
        cmbb("mdOfficerProfile", {"id": code, "code": code, "name": code, "active": ""})

    # a fresh open DM case (C6) created now. Synthetic TIN (NOT a Gold debtor) so
    # this suite never collides with F03's identified set (run_t12 dedup check).
    c1 = f"esc-{RUN}-1"
    tin = f"T13{RUN}"
    # Income Tax has no per-tax lifecycle override → the default (DM) escalation ladder applies
    # (Reminder -> Demand notice -> ...). The VAT skip-Reminder path is covered separately in T-13.6.
    cmbb("cmCase", {"id": c1, "caseType": "DM", "tin": tin, "origin": "MANUAL",
                    "taxType": "INCOME_TAX", "taxpayerName": "Big Debtor Ltd", "amountAtStake": "1094635",
                    "category": "C6"})
    r = jg.start(c1)
    time.sleep(2)
    jg.complete("openCase", r.get("processId"))
    time.sleep(2)
    dmbb("dmDebt", {"id": c1, "tin": tin, "debtCategory": "C6", "stage": "Identified",
                    "triggerOrigin": "MANUAL", "consolidatedAmount": "1094635", "lastStepSeq": "0"})

    # ---------- T-13.1 ----------
    sweep(10)
    stage1 = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{c1}'")
    seq1 = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{c1}'")
    np1 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' "
              "AND c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%TPL-REMINDER%'")
    esc1 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' AND c_eventtype='DEBT_ESCALATED'")
    check("T-13.1 sweep@+10d fires Reminder (step1) + notice pending",
          stage1 == "Reminder" and seq1 == "1" and int(np1) >= 1 and esc1 == "1",
          f"stage={stage1} seq={seq1} reminder_pending={np1} escalated={esc1}")

    # ---------- T-13.2 ----------
    sweep(30)
    stage2 = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{c1}'")
    seq2 = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{c1}'")
    np2 = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' "
              "AND c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%TPL-DEMAND%'")
    check("T-13.2 sweep@+30d advances to Demand notice (step2)",
          stage2 == "Demand notice" and seq2 == "2" and int(np2) >= 1,
          f"stage={stage2} seq={seq2} demand_pending={np2}")

    # ---------- T-13.3 ----------
    cmbb("cmDispatchRun", {"id": f"disp-{RUN}", "mode": "DISPATCH"})
    time.sleep(4)
    sent = sql(f"SELECT count(*) FROM app_fd_cmnotif WHERE c_caseid='{c1}' "
               "AND c_status='SENT' AND c_template IN ('TPL-REMINDER','TPL-DEMAND')")
    check("T-13.3 dispatcher sends the escalation notices (cmNotif SENT)",
          int(sent) >= 2, f"sent_notices={sent}")

    # ---------- T-13.4 ----------
    esc_before = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' AND c_eventtype='DEBT_ESCALATED'")
    sweep(30)  # same asOf -> nothing new for c1
    esc_after = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' AND c_eventtype='DEBT_ESCALATED'")
    seq4 = sql(f"SELECT c_laststepseq FROM app_fd_dmdebt WHERE id='{c1}'")
    check("T-13.4 idempotent: re-sweep@+30d fires nothing new for the case",
          esc_after == esc_before and seq4 == "2",
          f"escalated_before={esc_before} after={esc_after} seq={seq4}")

    # ---------- T-13.5 dmDebt.stage guarded + audited (ADR-003 migration #6) ----------
    # the two stage moves (Identified->Reminder, Reminder->Demand notice) each emit a
    # STATUS_CHANGED row on the case chain via the config-driven DM-scope guard.
    stageSc = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{c1}' "
                  "AND c_eventtype='STATUS_CHANGED'")
    check("T-13.5 dmDebt.stage guarded + audited (2 stage moves → STATUS_CHANGED)",
          int(stageSc) >= 2, f"statusChanged={stageSc}")

    # ---------- T-13.6 per-tax pilot: a VAT case SKIPS the Reminder (ADR-003 §5 step 3) ----------
    # Same strategy/timing, but the VAT lifecycle override has no Identified->Reminder, so the
    # Reminder slot is consumed silently and the case escalates straight to Demand notice. Driven
    # entirely by the per-tax lifecycle config — the engine keeps a single strategy template.
    cV, tinV = f"escV-{RUN}", f"T13V{RUN}"
    cmbb("cmCase", {"id": cV, "caseType": "DM", "tin": tinV, "origin": "MANUAL",
                    "taxType": "VAT", "taxpayerName": "VAT Debtor Ltd", "amountAtStake": "500000",
                    "category": "C6"})
    dmbb("dmDebt", {"id": cV, "tin": tinV, "debtCategory": "C6", "stage": "Identified",
                    "triggerOrigin": "MANUAL", "consolidatedAmount": "500000", "lastStepSeq": "0"})
    # managed/read-only fields are dropped by the data API → set via SQL (documented fixture exception)
    sql(f"UPDATE app_fd_cmcase SET c_currentstate='OPEN', c_taxtype='VAT', c_category='C6' WHERE id='{cV}'")
    sql(f"UPDATE app_fd_dmdebt SET c_stage='Identified', c_laststepseq='0', c_debtcategory='C6' WHERE id='{cV}'")
    sweep(25)  # Demand-notice slot (cum21) due; the Reminder slot (cum7) is skipped for VAT
    stageV = sql(f"SELECT c_stage FROM app_fd_dmdebt WHERE id='{cV}'")
    remV = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cV}' "
               "AND c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%TPL-REMINDER%'")
    dnV = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cV}' "
              "AND c_eventtype='NOTIF_PENDING' AND c_payload LIKE '%TPL-DEMAND%'")
    scV = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cV}' AND c_eventtype='STATUS_CHANGED'")
    check("T-13.6 VAT case skips Reminder → straight to Demand notice (per-tax lifecycle, no DM Reminder)",
          stageV == "Demand notice" and int(remV) == 0 and int(dnV) >= 1 and int(scV) == 1,
          f"stage={stageV} reminderNotices={remV} demandNotices={dnV} statusChanged={scV}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
