#!/usr/bin/env python3
"""DMBB-F08 acceptance T-17.1..4 — live jdx9 (apps cmbb + dmbb).

T-17.1 VALIDATE the live config -> VALID (every enabled instrument's docTemplate resolves + costRecorded has a fee) (DM-FR-039)
T-17.2 VALIDATE catches a dangling docTemplate on a temp enabled instrument -> INVALID with the issue (DM-FR-039 testability)
T-17.3 PREVIEW separate-per-tax-type: a 2-tax-type case renders merge fields, noticeCount=2 (DM-FR-040 + DM-FR-041 SEPARATE)
T-17.4 PREVIEW consolidated: same case, CONSOLIDATED_ALL -> noticeCount=1, total amount merged (DM-FR-041)

Usage: JDX9_PASSWORD=admin run_t17.py
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


def main():
    assert CK and DK, "missing api keys"

    # ---------- T-17.1 VALIDATE clean ----------
    chk1 = f"chk1-{RUN}"
    dmbb("cmEnfConfigCheck", {"id": chk1, "scope": ""})
    time.sleep(4)
    v1 = sql(f"SELECT c_valid FROM app_fd_cmenfconfigcheck WHERE id='{chk1}'")
    i1 = sql(f"SELECT c_issuecount FROM app_fd_cmenfconfigcheck WHERE id='{chk1}'")
    iss1 = sql(f"SELECT c_issues FROM app_fd_cmenfconfigcheck WHERE id='{chk1}'")
    check("T-17.1 live enforcement config validates clean (DM-FR-039)",
          v1 == "true" and i1 == "0", f"valid={v1} issues={i1} :: {iss1[:160]}")

    # ---------- T-17.2 VALIDATE catches a dangling template ----------
    bad = f"ZZTEST{RUN}"
    dmbb("mdInstrument", {"id": bad, "code": bad, "name": "Temp bad", "minCategory": "C3",
                          "executionMode": "ADMINISTRATIVE", "docTemplate": "TPL-NOEXIST",
                          "costRecorded": "false", "enabled": "true", "countryProfile": "TEST"})
    chk2 = f"chk2-{RUN}"
    dmbb("cmEnfConfigCheck", {"id": chk2, "scope": ""})
    time.sleep(4)
    v2 = sql(f"SELECT c_valid FROM app_fd_cmenfconfigcheck WHERE id='{chk2}'")
    i2 = int(sql(f"SELECT c_issuecount FROM app_fd_cmenfconfigcheck WHERE id='{chk2}'") or "0")
    iss2 = sql(f"SELECT c_issues FROM app_fd_cmenfconfigcheck WHERE id='{chk2}'")
    check("T-17.2 VALIDATE flags a dangling docTemplate (testable before production)",
          v2 == "false" and i2 >= 1 and bad in iss2, f"valid={v2} issues={i2} txt={iss2[:120]}")
    dmbb("mdInstrument", {"id": bad, "enabled": "false"})  # cleanup: disable temp instrument

    # ---------- preview case (2 tax types) ----------
    cP = f"prev-{RUN}"
    cmbb("cmCase", {"id": cP, "caseType": "DM", "caseRef": f"DM-{RUN}", "tin": f"T17{RUN}",
                    "origin": "SYSTEM", "taxpayerName": "Preview Co", "amountAtStake": "1500",
                    "currentState": "OPEN"})
    dmbb("dmLine", {"id": f"{cP}-L1", "caseId": cP, "taxType": "VAT", "yofa": "2024",
                    "amount": "1000", "disputed": "0", "enforceable": "1000"})
    dmbb("dmLine", {"id": f"{cP}-L2", "caseId": cP, "taxType": "INCOME", "yofa": "2024",
                    "amount": "500", "disputed": "0", "enforceable": "500"})

    # ---------- T-17.3 PREVIEW separate ----------
    pv1 = f"pv1-{RUN}"
    dmbb("cmTemplatePreview", {"id": pv1, "templateCode": "TPL-DEMAND", "debtCaseId": cP,
                               "consolidationMode": ""})
    time.sleep(4)
    n1 = sql(f"SELECT c_noticecount FROM app_fd_cmtemplatepreview WHERE id='{pv1}'")
    body1 = sql(f"SELECT c_renderedbody FROM app_fd_cmtemplatepreview WHERE id='{pv1}'")
    check("T-17.3 PREVIEW separate-per-tax-type renders merge fields (DM-FR-040/041)",
          n1 == "2" and "Preview Co" in body1 and f"DM-{RUN}" in body1 and "#" not in body1,
          f"notices={n1} body={body1[:100]}")

    # ---------- T-17.4 PREVIEW consolidated ----------
    pv2 = f"pv2-{RUN}"
    dmbb("cmTemplatePreview", {"id": pv2, "templateCode": "TPL-DEMAND", "debtCaseId": cP,
                               "consolidationMode": "CONSOLIDATED_ALL"})
    time.sleep(4)
    n2 = sql(f"SELECT c_noticecount FROM app_fd_cmtemplatepreview WHERE id='{pv2}'")
    body2 = sql(f"SELECT c_renderedbody FROM app_fd_cmtemplatepreview WHERE id='{pv2}'")
    check("T-17.4 PREVIEW consolidated single notice with total (DM-FR-041)",
          n2 == "1" and "1500" in body2, f"notices={n2} body={body2[:100]}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
