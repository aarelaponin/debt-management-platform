#!/usr/bin/env python3
"""DMBB-F10 acceptance T-19.1..5 — live jdx9 (apps cmbb + dmbb).

T-19.1 auto-estimate from prior year + reasonableness OK (DM-FR-054/BR-DM-051)
T-19.2 reasonableness flag: estimate > prior x 1.5 with no justification -> NEEDS_JUSTIFICATION; with justification -> ASSESSED (BR-DM-052)
T-19.3 fallback estimation: no prior-year -> industry average (BR-DM-051 priority)
T-19.4 return replacement: a filed return reverses the default + records variance + risk signal (DM-FR-055/BR-DM-053)
T-19.5 escalation: an assessment with no filing past the grace period -> a DM debt case is created (DM-FR-056)

Usage: JDX9_PASSWORD=admin run_t19.py
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


DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def dmbb(form, p):
    req = urllib.request.Request(f"{BASE}/api/form/{form}/saveOrUpdate",
                                 data=json.dumps(p).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", "API-dmbb-data"), ("api_key", DK)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = (json.loads(body).get("errors") or {}) if body.startswith("{") else {"_": body[:120]}
        if errs:
            raise SystemExit(f"validation error on {form}: {body[:300]}")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def da(field, aid):
    return sql(f"SELECT c_{field} FROM app_fd_dmdefassess WHERE id='{aid}'")


def ev(anchor, etype):
    return int(sql(f"SELECT count(*) FROM app_fd_cmevent WHERE c_caseid='{anchor}' AND c_eventtype='{etype}'"))


def main():
    assert DK, "missing dmbb api key"

    # ---------- T-19.1 auto prior-year ----------
    a1 = f"da1-{RUN}"
    dmbb("dmDefAssess", {"id": a1, "tin": f"T19A{RUN}", "taxType": "VAT", "period": "2024",
                         "priorYearAmount": "1000", "status": "DRAFT"})
    time.sleep(4)
    check("T-19.1 auto-estimate from prior year, reasonableness OK (DM-FR-054/BR-DM-051)",
          da("estimatedamount", a1) == "1000" and da("estimationmethod", a1) == "PRIOR_YEAR"
          and da("reasonablenessflag", a1) == "OK" and da("status", a1) == "ASSESSED"
          and ev(f"DA-{a1}", "DEFAULT_ASSESSED") >= 1,
          f"est={da('estimatedamount', a1)} method={da('estimationmethod', a1)} status={da('status', a1)}")

    # ---------- T-19.2 reasonableness ----------
    a2 = f"da2-{RUN}"
    dmbb("dmDefAssess", {"id": a2, "tin": f"T19B{RUN}", "priorYearAmount": "1000",
                         "estimatedAmount": "2000", "status": "DRAFT"})
    time.sleep(4)
    s2 = da("status", a2)
    a2b = f"da2b-{RUN}"
    dmbb("dmDefAssess", {"id": a2b, "tin": f"T19B2{RUN}", "priorYearAmount": "1000",
                         "estimatedAmount": "2000", "justification": "new line doubled turnover",
                         "status": "DRAFT"})
    time.sleep(4)
    s2b = da("status", a2b)
    check("T-19.2 over-threshold needs justification, then passes (BR-DM-052)",
          s2 == "NEEDS_JUSTIFICATION" and da("reasonablenessflag", a2) == "REVIEW" and s2b == "ASSESSED",
          f"noJust={s2} withJust={s2b}")

    # ---------- T-19.3 industry-average fallback ----------
    a3 = f"da3-{RUN}"
    dmbb("dmDefAssess", {"id": a3, "tin": f"T19C{RUN}", "status": "DRAFT"})
    time.sleep(4)
    check("T-19.3 no prior-year falls back to industry average (BR-DM-051 priority)",
          da("estimationmethod", a3) == "INDUSTRY_AVG" and da("estimatedamount", a3) == "800",
          f"method={da('estimationmethod', a3)} est={da('estimatedamount', a3)}")

    # ---------- T-19.4 return replacement ----------
    a4 = f"da4-{RUN}"
    dmbb("dmDefAssess", {"id": a4, "tin": f"T19D{RUN}", "priorYearAmount": "1000", "status": "DRAFT"})
    time.sleep(4)
    dmbb("cmReturnFiled", {"id": f"rf4-{RUN}", "defAssessId": a4, "filedAmount": "1200",
                           "filedDate": "2026-06-12"})
    time.sleep(4)
    check("T-19.4 filed return replaces default, variance + risk signal (DM-FR-055/BR-DM-053)",
          da("status", a4) == "REPLACED" and da("variance", a4) == "200"
          and ev(f"DA-{a4}", "ASSESSMENT_REPLACED") >= 1 and ev(f"DA-{a4}", "RISK_PROFILE_UPDATE") >= 1,
          f"status={da('status', a4)} variance={da('variance', a4)}")

    # ---------- T-19.5 escalation to a debt case ----------
    a5 = f"da5-{RUN}"
    dmbb("dmDefAssess", {"id": a5, "tin": f"T19E{RUN}", "taxType": "VAT", "priorYearAmount": "900",
                         "status": "DRAFT"})
    time.sleep(4)
    asof = (datetime.now() + timedelta(days=60)).strftime("%Y-%m-%dT%H:%M:%S")  # past 30d grace
    dmbb("cmDefAssessRun", {"id": f"esc-{RUN}", "asOf": asof})
    time.sleep(5)
    ref5 = da("debtcaseref", a5)
    newcase = int(sql(f"SELECT count(*) FROM app_fd_dmdebt WHERE c_tin='T19E{RUN}' "
                      "AND c_triggerorigin='DEFAULT_ASSESSMENT'"))
    cat5 = sql(f"SELECT c_debtcategory FROM app_fd_dmdebt WHERE c_tin='T19E{RUN}' "
               "AND c_triggerorigin='DEFAULT_ASSESSMENT' LIMIT 1")
    check("T-19.5 no-filing assessment escalates to a categorised DM debt case (DM-FR-056)",
          da("status", a5) == "ESCALATED" and ref5 != "" and newcase >= 1 and cat5 == "C3",
          f"status={da('status', a5)} ref={ref5} newDebt={newcase} category={cat5}")

    # ---------- T-19.6 dmDefAssess status guarded + audited (ADR-003 migration) ----------
    # a1 DRAFT->ASSESSED chains under DA-a1; a5's ASSESSED->ESCALATED chains under the new case ref5.
    sc1 = ev(f"DA-{a1}", "STATUS_CHANGED")
    sc5 = ev(ref5, "STATUS_CHANGED")
    check("T-19.6 dmDefAssess status guarded + audited (ASSESSED + ESCALATED → STATUS_CHANGED)",
          sc1 >= 1 and sc5 >= 1, f"assessed_sc={sc1} escalated_case_sc={sc5}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
