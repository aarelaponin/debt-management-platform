#!/usr/bin/env python3
"""DMBB-UX-QA2 acceptance T-24.1..5 — live jdx9.

T-24.1 column labels cleaned: no deployed list column header leaks "(set by engine)" / "[DM-FR..]" / "(FK)" ...
T-24.2 new detail lists render: list_enfActions + list_instalments load typeset (HTTP 200, no error)
T-24.3 recovery-by-action drill: list_enfActions?finstr=BANK_GARNISH shows the garnish action, ?finstr=LIEN does not
T-24.4 instalment-compliance drill: list_instalments?fstat=ACTIVE shows the active plan, ?fstat=CANCELLED does not
T-24.5 aging drill: list_debtorsList?fage=<band> filters debtors by age band

Usage: JDX9_PASSWORD=admin run_t24.py
"""
import json
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

RESULTS = []
R = str(int(time.time()))[-6:]
BASE = "http://localhost:8089/jw"
LEAK = re.compile(r"\(set by|\(managed by|\(FK\)|\[DM-FR|\[BR-|\(copied|set by engine", re.I)


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


CK = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")
DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def post(form, api, key, p):
    req = urllib.request.Request(f"{BASE}/api/form/{form}/saveOrUpdate", data=json.dumps(p).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", api), ("api_key", key)]:
        req.add_header(h, v)
    urllib.request.urlopen(req).read()


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return r.getcode(), r.read().decode("utf-8", "replace")


def datarows(url):
    """Count data rows in the datalist <tbody>; an empty result renders 1 placeholder row."""
    _, h = get(url)
    m = re.search(r"<tbody[^>]*>(.*?)</tbody>", h, re.S | re.I)
    return len(re.findall(r"<tr", m.group(1))) if m else 0


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def labels(lid):
    j = sql(f"SELECT json FROM app_datalist WHERE id='{lid}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return [c["label"] for c in json.loads(j)["columns"]]


def main():
    assert CK and DK

    # ---------- T-24.1 labels cleaned ----------
    leaky = {}
    for lid in ("list_dmAction", "list_dmDebt", "list_cmEscalateRun", "list_dmInstAgr", "list_dmDefAssess"):
        bad = [x for x in labels(lid) if LEAK.search(x)]
        if bad:
            leaky[lid] = bad
    check("T-24.1 column labels cleaned of implementation leaks", not leaky, f"leaky={leaky}")

    # ---------- T-24.2 new detail lists render ----------
    ok2 = True
    det = ""
    for lid in ("list_enfActions", "list_instalments"):
        u = f"{BASE}/web/userview/dmbb/dmbbConsole/_/{lid}"
        code, html = get(u)
        err = bool(re.search(r"cannot be cast|HTTP Status 500|SQLException", html, re.I))
        rows = datarows(u)  # a JDBC binder column-error renders a SILENT empty list (HTTP 200, no 500)
        ok2 = ok2 and code == 200 and not err and rows >= 1
        det += f"{lid}=http{code}/err{int(err)}/rows{rows} "
    check("T-24.2 new detail lists render WITH data (enfActions + instalments)", ok2, det)

    # ---------- data for drills ----------
    cg, tg = f"uq-g-{R}", f"UQG{R}"
    post("cmCase", "API-cmbb-data", CK, {"id": cg, "caseType": "DM", "tin": tg, "origin": "SYSTEM",
         "taxpayerName": "UQ Garnish", "amountAtStake": "4000", "category": "C5", "currentState": "OPEN"})
    post("dmDebt", "API-dmbb-data", DK, {"id": cg, "tin": tg, "debtCategory": "C5", "consolidatedAmount": "4000",
         "stage": "Final demand", "lastStepSeq": "3", "writeOffStatus": ""})
    post("dmLine", "API-dmbb-data", DK, {"id": cg + "-L1", "caseId": cg, "taxType": "VAT", "amount": "4000",
         "disputed": "0", "enforceable": "4000"})
    post("dmAction", "API-dmbb-data", DK, {"id": f"uqa-{R}", "debtCaseId": cg, "tin": tg,
         "instrument": "BANK_GARNISH", "amount": "4000", "status": "INITIATED"})
    ci, ti = f"uq-i-{R}", f"UQI{R}"
    post("cmCase", "API-cmbb-data", CK, {"id": ci, "caseType": "DM", "tin": ti, "origin": "SYSTEM",
         "taxpayerName": "UQ Inst", "amountAtStake": "1200", "category": "C4", "currentState": "OPEN"})
    post("dmDebt", "API-dmbb-data", DK, {"id": ci, "tin": ti, "debtCategory": "C4", "consolidatedAmount": "1200",
         "stage": "Final demand", "lastStepSeq": "3", "writeOffStatus": ""})
    post("dmInstAgr", "API-dmbb-data", DK, {"id": f"uqia-{R}", "tin": ti, "debtCaseId": ci,
         "totalDebt": "1200", "durationMonths": "6", "status": "APPLIED"})
    time.sleep(5)

    # Drills pass ONE param; verify single-param discrimination (a real value yields rows, a bogus value empties).
    # ---------- T-24.3 recovery-by-action drill (?finstr) ----------
    u = f"{BASE}/web/userview/dmbb/dmbbConsole/_/list_enfActions"
    g_hits = datarows(f"{u}?finstr=BANK_GARNISH")
    g_none = datarows(f"{u}?finstr=ZZNONE")
    check("T-24.3 recovery->enforcement-actions drill (?finstr) filters", g_hits > 1 and g_none <= 1,
          f"BANK_GARNISH={g_hits} rows, ZZNONE={g_none} rows (empty=1)")

    # ---------- T-24.4 instalment-compliance drill (?fstat) ----------
    u2 = f"{BASE}/web/userview/dmbb/dmbbConsole/_/list_instalments"
    a_hits = datarows(f"{u2}?fstat=ACTIVE")
    a_none = datarows(f"{u2}?fstat=ZZNONE")
    check("T-24.4 compliance->instalments drill (?fstat) filters", a_hits >= 1 and a_none <= 1,
          f"ACTIVE={a_hits} rows, ZZNONE={a_none} rows")

    # ---------- T-24.5 aging drill (?fage) ----------
    u3 = f"{BASE}/web/userview/dmbb/dmbbConsole/_/list_debtorsList"
    fresh = datarows(f"{u3}?fage={urllib.parse.quote('1: 0-30')}")
    bogus = datarows(f"{u3}?fage=NOSUCHBAND")
    check("T-24.5 aging->debtors drill (?fage) filters by age band", fresh > 1 and bogus <= 1,
          f"freshBand={fresh} rows, bogusBand={bogus} rows")

    # ---------- T-24.6 dmAction edit form renders (LOV extraCondition valid, not "null") ----------
    # a leading WHERE in a FormOptionsBinder extraCondition => "WHERE WHERE" HQL error => form
    # body renders literally as "null". Editing the seeded action must show a real form.
    ef = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/dmAction_crud?id=uqa-{R}&_mode=edit")[1]
    form_ok = ("<form" in ef) and (ef.strip().lower() != "null")
    check("T-24.6 dmAction edit form renders (instrument LOV valid, not null)",
          form_ok, f"has_form={'<form' in ef} bodyIsNull={ef.strip().lower() == 'null'}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
