#!/usr/bin/env python3
"""DMBB-F01 acceptance T-10.1..4 — live jdx9, app `dmbb` (config/seed only).

T-10.1 debt categories C1-C6 with exact configurable thresholds (DM-FR-004/BR-DM-003), Gold-aligned
T-10.2 instrument catalogue: 14 enabled + 4 present-disabled, applicability by minCategory (BR-DM-031/DPM-3)
T-10.3 collection strategy STD-MLT + 5 ordered escalation steps (DPM D2)
T-10.4 dmbb app present (4 forms) + API-dmbb-data write path works; CMBB spine untouched

Usage: JDX9_PASSWORD=admin run_t10.py   (seeds loaded first via load_md_seed on API-dmbb-data)
"""
import json
import subprocess
import sys
import urllib.request

RESULTS = []


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


DK = sql("SELECT apikey FROM api_credential WHERE apiname='dmbb-dev-key'")


def dmbb_post(form_id, payload):
    req = urllib.request.Request(f"http://localhost:8089/jw/api/form/{form_id}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("api_id", "API-dmbb-data"), ("api_key", DK)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        errs = (json.loads(body).get("errors") or {}) if body.startswith("{") else {"_": body[:120]}
        return r.status, errs


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    assert DK, "no dmbb api key bound"

    # ---------- T-10.1 ----------
    bands = dict(line.split("|", 1) for line in sql(
        "SELECT c_code, c_minamount||'/'||coalesce(c_maxamount,'') FROM app_fd_mddebtcat").splitlines() if "|" in line)
    expect = {"C1": "0/30", "C2": "30/100", "C3": "100/1000", "C4": "1000/20000",
              "C5": "20000/200000", "C6": "200000/"}
    gold_cats = set(x for x in ch("SELECT DISTINCT debt_category FROM sta_v1.debt_balances").split() if x)
    check("T-10.1 6 categories, exact thresholds, Gold-aligned",
          bands == expect and gold_cats.issubset(set(expect)),
          f"bands={bands} gold={sorted(gold_cats)}")

    # ---------- T-10.2 ----------
    # scope to the seeded catalogue (MLT/FOREIGN); other suites may leave tagged test rows (e.g. F08 'TEST')
    en = sql("SELECT count(*) FROM app_fd_mdinstrument WHERE c_enabled='true' AND c_countryprofile IN ('MLT','FOREIGN')")
    dis = sql("SELECT count(*) FROM app_fd_mdinstrument WHERE c_enabled='false' AND c_countryprofile IN ('MLT','FOREIGN')")
    bg = sql("SELECT c_mincategory||'/'||c_executionmode||'/'||c_authoritylevel FROM app_fd_mdinstrument WHERE c_code='BANK_GARNISH'")
    ps = sql("SELECT c_mincategory FROM app_fd_mdinstrument WHERE c_code='PROPERTY_SEIZURE'")
    bad = sql("SELECT count(*) FROM app_fd_mdinstrument WHERE c_enabled='true' AND c_mincategory NOT IN ('C2','C3','C4','C5')")
    check("T-10.2 14 enabled + 4 disabled, applicability monotone",
          en == "14" and dis == "4" and bg == "C3/ADMINISTRATIVE/SUPERVISOR" and ps == "C4" and bad == "0",
          f"enabled={en} disabled={dis} bank_garnish={bg} property={ps} out_of_band={bad}")

    # ---------- T-10.3 ----------
    strat = sql("SELECT c_segment||'/'||c_categoryfloor||'/'||c_active FROM app_fd_mmstrategy WHERE c_code='STD-MLT'")
    steps = sql("SELECT string_agg(c_stepname,'>' ORDER BY c_seq::int) FROM app_fd_mmescstep WHERE c_strategycode='STD-MLT'")
    nstep = sql("SELECT count(*) FROM app_fd_mmescstep WHERE c_strategycode='STD-MLT'")
    orphan = sql("SELECT count(*) FROM app_fd_mmescstep s WHERE coalesce(s.c_instrument,'')<>'' "
                 "AND NOT EXISTS (SELECT 1 FROM app_fd_mdinstrument i WHERE i.c_code=s.c_instrument)")
    check("T-10.3 STD-MLT + 5 ordered steps, instruments resolve",
          strat == "ALL/C2/true" and nstep == "5"
          and steps == "Reminder>Demand notice>Final demand>Bank garnishing>Field enforcement"
          and orphan == "0",
          f"strategy={strat} nstep={nstep} orphan_instruments={orphan}")

    # ---------- T-10.4 ----------
    # the 4 F01 DPM forms must exist (later features add more dmbb forms — count is not pinned)
    f01 = sql("SELECT count(*) FROM app_form WHERE appid='dmbb' AND formid IN "
              "('mdDebtCat','mdInstrument','mmStrategy','mmEscStep')")
    cmcols = sql("SELECT count(*) FROM information_schema.columns WHERE table_name='app_fd_cmcase'")
    st, errs = dmbb_post("mdDebtCat", {"id": "C1", "code": "C1", "name": "Up to EUR 30",
                                       "bandOrder": "1", "minAmount": "0", "maxAmount": "30"})
    check("T-10.4 F01 DPM forms present + API write path + CMBB spine intact",
          f01 == "4" and st == 200 and not errs and int(cmcols) >= 28,
          f"f01_forms={f01} post={st} errors={errs} cmcase_cols={cmcols}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
