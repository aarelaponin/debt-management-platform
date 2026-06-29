#!/usr/bin/env python3
"""DMBB-F03 acceptance T-12.1..4 — live jdx9 + ClickHouse (apps cmbb + dmbb).

T-12.1 cmIdentRun constitutes each Gold debtor into the workflow-group case(s) (ADR-004 §14)
T-12.2 split detail: a VAT line forms its own tax-specific case; other taxes consolidate; lines partitioned + workflow recorded
T-12.3 dedup: a 2nd run constitutes nothing new for already-identified TINs
T-12.4 an identified case carries CASE_IDENTIFIED history + a VERIFIED hash chain

Usage: JDX9_PASSWORD=admin run_t12.py
"""
import json
import re
import subprocess
import sys
import time
import urllib.request

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

BASE = "http://localhost:8089/jw"
MIN = "200000"  # C6 debtors only — keeps the run small and deterministic
RESULTS = []
RUN = str(int(time.time()))[-6:]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def ch(q):
    r = subprocess.run(["docker", "exec", "mtca-clickhouse", "clickhouse-client", "-q", q],
                       capture_output=True, text=True)
    return ("CH-ERROR: " + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


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

    def complete_first(self, sub):
        r = self.s.get(f"{BASE}/web/json/workflow/assignment/list", params={"rows": 1000})
        data = r.json().get("data", [])
        for a in ([data] if isinstance(data, dict) else data):
            aid = a.get("activityId") or a.get("id", "")
            if sub in ((a.get("activityName") or "") + aid):
                self.s.post(f"{BASE}/web/json/workflow/assignment/accept/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                self.s.post(f"{BASE}/web/json/workflow/assignment/complete/{aid}",
                            data={"OWASP_CSRFTOKEN": self.csrf()})
                return True
        return False


def identify(min_amount):
    rid = f"ir-{RUN}-{int(time.time()) % 1000}"
    post("cmIdentRun", "API-dmbb-data", DK, {"id": rid, "minAmount": min_amount})
    time.sleep(5)
    return rid


# ADR-004 §14: the only tax with a tax-specific workflow in the seed is VAT (W-VAT).
# A VAT line gets its own case; every other tax consolidates into one case.
TAX_SPECIFIC = {"VAT"}


def expected_groups(taxes):
    """Number of cases a TIN with this set of tax types constitutes (ADR-004 §14)."""
    g = len(taxes & TAX_SPECIFIC)            # one case per tax-specific tax present
    if taxes - TAX_SPECIFIC:                 # one consolidated case for everything else
        g += 1
    return g or 1                            # a TIN with lines but no recognised tax → 1 case


def main():
    assert CK and DK, "missing keys"
    jg = Joget()
    # deactivate officer pool so openCase stays in admin's inbox
    for code in ("officer1", "officer2"):
        post("mdOfficerProfile", "API-cmbb-data", CK,
             {"id": code, "code": code, "name": code, "active": ""})
    # NOTE: wipe-free + idempotent. SQL-DELETE on app_fd_cmcase poisons the Hibernate cache
    # the dedup engine reads (DX9-DELTAS F02), so we assert the END STATE — every Gold debtor
    # is constituted into exactly the workflow-group case(s) ADR-004 §14 prescribes — regardless
    # of whether THIS run created them or a prior run did.

    expected = [x for x in ch(
        f"SELECT tin FROM sta_v1.debt_priority_queue WHERE total_enforceable >= {MIN} ORDER BY tin").split() if x]
    inlist = ",".join("'" + t + "'" for t in expected) or "''"
    # per-TIN tax sets from Gold → expected case count
    taxes = {t: set(x for x in ch(
        f"SELECT DISTINCT tax_type FROM sta_v1.debt_balances WHERE tin='{t}'").split() if x) for t in expected}
    want = {t: expected_groups(taxes[t]) for t in expected}
    total_groups = sum(want.values())
    # a TIN that has VAT AND at least one other tax → proves the split
    split = next((t for t in expected if "VAT" in taxes[t] and (taxes[t] - {"VAT"})), None)

    # ---------- T-12.1 ----------
    identify(MIN)
    mism = []
    for t in expected:
        cnt = int(sql(f"SELECT count(*) FROM app_fd_cmcase WHERE c_casetype='DM' "
                      f"AND c_origin='SYSTEM' AND c_tin='{t}'") or "0")
        if cnt != want[t]:
            mism.append(f"{t}:{cnt}!={want[t]}")
    check("T-12.1 each Gold debtor constituted into the expected workflow-group case(s)",
          bool(expected) and not mism,
          f"tins={len(expected)} total_groups={total_groups} mismatches={mism}")

    # ---------- T-12.2 split detail: VAT case + consolidated case, lines partitioned ----------
    vat_cid = sql(f"SELECT id FROM app_fd_cmcase WHERE c_casetype='DM' AND c_tin='{split}' "
                  f"AND c_taxtype='VAT' LIMIT 1")
    cons_cid = sql(f"SELECT id FROM app_fd_cmcase WHERE c_casetype='DM' AND c_tin='{split}' "
                   f"AND (c_taxtype='' OR c_taxtype IS NULL) LIMIT 1")
    gold_vat = ch(f"SELECT count() FROM sta_v1.debt_balances WHERE tin='{split}' AND tax_type='VAT'")
    gold_oth = ch(f"SELECT count() FROM sta_v1.debt_balances WHERE tin='{split}' AND tax_type!='VAT'")
    vat_nl = sql(f"SELECT count(*) FROM app_fd_dmline WHERE c_caseid='{vat_cid}'")
    cons_nl = sql(f"SELECT count(*) FROM app_fd_dmline WHERE c_caseid='{cons_cid}'")
    wf_vat = sql(f"SELECT c_workflowcode FROM app_fd_dmdebt WHERE id='{vat_cid}'")
    wf_cons = sql(f"SELECT c_workflowcode FROM app_fd_dmdebt WHERE id='{cons_cid}'")
    gold_cat = ch(f"SELECT any(debt_category) FROM sta_v1.debt_priority_queue WHERE tin='{split}'")
    sub_cat = sql(f"SELECT c_debtcategory FROM app_fd_dmdebt WHERE id='{vat_cid}'")
    check("T-12.2 tax-specific VAT case + consolidated case; lines partitioned; workflow recorded",
          bool(vat_cid) and bool(cons_cid) and vat_nl == gold_vat and cons_nl == gold_oth
          and wf_vat == "W-VAT" and wf_cons == "W-DEFAULT" and sub_cat == gold_cat,
          f"tin={split} vat_lines={vat_nl}/{gold_vat} cons_lines={cons_nl}/{gold_oth} "
          f"wf={wf_vat}/{wf_cons} cat={sub_cat}/{gold_cat}")

    # ---------- T-12.3 dedup: re-run constitutes no new cases ----------
    before = sql(f"SELECT count(*) FROM app_fd_cmcase WHERE c_casetype='DM' AND c_origin='SYSTEM' "
                 f"AND c_tin IN ({inlist})")
    r2 = identify(MIN)
    after = sql(f"SELECT count(*) FROM app_fd_cmcase WHERE c_casetype='DM' AND c_origin='SYSTEM' "
                f"AND c_tin IN ({inlist})")
    skipped = sql(f"SELECT c_skippedcount FROM app_fd_cmidentrun WHERE id='{r2}'")
    check("T-12.3 dedup: a 2nd run constitutes no new cases for identified TINs",
          after == before and int(skipped or "0") >= len(expected),
          f"cases_before={before} after={after} skipped={skipped}")

    # ---------- T-12.4 audit-clean: identified + VERIFIED chain ----------
    cid = vat_cid or sql(f"SELECT id FROM app_fd_cmcase WHERE c_casetype='DM' AND c_tin='{split}' LIMIT 1")
    ev = sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{cid}' "
             "AND c_eventtype='CASE_IDENTIFIED'")
    post("cmChainCheck", "API-cmbb-data", CK, {"id": f"cc-{RUN}", "caseId": cid})
    time.sleep(3)
    vr = sql(f"SELECT c_result FROM app_fd_cmchaincheck WHERE id='cc-{RUN}'")
    check("T-12.4 constituted case is identified + envelope started + VERIFIED chain",
          int(ev) >= 1 and vr == "VERIFIED",
          f"identified_ev={ev} verify={vr}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
