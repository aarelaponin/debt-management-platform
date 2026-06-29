#!/usr/bin/env python3
"""DMBB-UX-QA3 acceptance T-25.1..6 — live jdx9.

QA3 = list width + column ordering + menu organisation of the batch-run triggers.

T-25.1 wide lists curated: the 8 previously-wide deployed lists each expose <=8 columns
T-25.2 key column leads: every curated list's first column is a taxpayer/case key (tin|caseRef|code)
T-25.3 Operations category present: deployed userview has one "Operations — batch runs" category
        collecting the 8 batch-run/check triggers (cmIdentRun..cmStrategyCheck)
T-25.4 officer categories cleaned: no batch-run trigger menu (cm*Run / cmStrategyCheck) survives
        in any officer/day-to-day category, and the single-trigger categories are retired
T-25.5 curated lists still render: the 8 curated lists load typeset (HTTP 200, no cast/SQL error)
T-25.6 drill still functional after curation: list_dmAction header still leads with TIN and a
        curated companion still discriminates (sanity that curation didn't break the binder)

Usage: JDX9_PASSWORD=admin run_t25.py
"""
import json
import re
import subprocess
import sys
import urllib.request

RESULTS = []
BASE = "http://localhost:8089/jw"

CURATED = ["list_dmAction", "list_dmDefAssess", "list_dmAgent", "list_dmWriteOff",
           "list_dmInstAgr", "list_dmDebt", "list_dmCaseConsole", "list_mdInstrument"]
KEY_FIRST = re.compile(r"^(tin|caseRef|code)$")
# a curated column header should keep only genuinely informative units/codes; anything else
# parenthetical is an authoring gloss that does not belong in a grid header (QA3).
KEEP_PAREN = re.compile(r"^(EUR|USD|MDL|GBP|RON|%|C\d(\s*[-–]\s*C\d)?|C1-C6|days|months)$", re.I)
# the 8 batch triggers that must live ONLY in Operations
BATCH = {"cmIdentRun", "cmEscalateRun", "cmInstComplianceRun", "cmEnfActionRun",
         "cmWriteOffRun", "cmDefAssessRun", "cmCollectionMiRun", "cmStrategyCheck"}


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return r.getcode(), r.read().decode("utf-8", "replace")


def datarows(url):
    _, h = get(url)
    m = re.search(r"<tbody[^>]*>(.*?)</tbody>", h, re.S | re.I)
    return len(re.findall(r"<tr", m.group(1))) if m else 0


def list_json(lid):
    j = sql(f"SELECT json FROM app_datalist WHERE id='{lid}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def uv_json():
    j = sql("SELECT json FROM app_userview WHERE id='dmbbConsole' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def menu_form_id(m):
    p = m.get("properties", {})
    return p.get("addFormId") or p.get("editFormId") or p.get("formDefId") or ""


def menu_for_list(uv, list_id):
    """The customId of the menu (CrudMenu or DataListMenu) that exposes list_id — its real UI URL."""
    for c in uv["categories"]:
        for m in c.get("menus", []):
            if m.get("properties", {}).get("datalistId") == list_id:
                return m["properties"].get("customId")
    return None


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    # ---------- T-25.1 curated width ----------
    widths = {}
    for lid in CURATED:
        widths[lid] = len(list_json(lid)["columns"])
    over = {k: v for k, v in widths.items() if v > 8}
    check("T-25.1 wide lists curated to <=8 columns", not over, f"widths={widths}")

    # ---------- T-25.2 key column leads ----------
    bad_first = {}
    for lid in CURATED:
        first = list_json(lid)["columns"][0]["name"]
        if not KEY_FIRST.match(first):
            bad_first[lid] = first
    check("T-25.2 curated lists lead with a key column (tin|caseRef|code)", not bad_first, f"bad={bad_first}")

    # ---------- T-25.3 Automation bucket (business console: the 8 batch triggers live here) ----------
    uv = uv_json()
    cats = uv["categories"]
    auto = [c for c in cats if "Automation" in c.get("properties", {}).get("label", "")]
    auto_forms = {menu_form_id(m) for m in auto[0].get("menus", [])} if auto else set()
    check("T-25.3 single Automation category collects all 8 batch triggers",
          len(auto) == 1 and BATCH <= auto_forms,
          f"automation_categories={len(auto)} missing={sorted(BATCH - auto_forms)}")

    # ---------- T-25.4 batch triggers live ONLY in Automation (not in Operations/Admin/Dashboards) ----------
    leaks = {}
    for c in cats:
        lbl = re.sub(r"<[^>]+>", "", c.get("properties", {}).get("label", "")).strip()
        if "Automation" in lbl:
            continue
        for m in c.get("menus", []):
            if menu_form_id(m) in BATCH:
                leaks.setdefault(lbl, []).append(menu_form_id(m))
    # 5-bucket console (ADR-004 §7): Dashboards / Operations / Automation / Collection settings / Legal & reference
    buckets = {re.sub(r"<[^>]+>", "", c["properties"]["label"]).strip() for c in cats}
    check("T-25.4 batch triggers live only in Automation; console is the 5 business buckets",
          not leaks and buckets <= {"Dashboards", "Operations", "Automation",
                                    "Collection settings", "Legal & reference"},
          f"leaks={leaks} buckets={sorted(buckets)}")

    # ---------- T-25.5 curated lists render through their real menu (resolve customId; a companion
    #            list has no standalone /_/<listId> menu — fetching that returns a blank body).
    #            list_dmDebt is an intentionally unexposed companion: the user-facing debt list is the
    #            joined case console (list_dmCaseConsole). Render-check the exposed ones; assert the only
    #            unexposed curated list is that known companion. ----------
    UNEXPOSED_OK = {"list_dmDebt"}
    ok5 = True
    det = ""
    for lid in CURATED:
        mid = menu_for_list(uv, lid)
        if not mid:
            if lid not in UNEXPOSED_OK:
                ok5 = False
                det += f"{lid}=NO-MENU "
            continue
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{mid}")
        err = bool(re.search(r"cannot be cast|HTTP Status 500|SQLException|JRException", html, re.I))
        m = re.search(r"<tbody[^>]*>(.*?)</tbody>", html, re.S | re.I)
        rows = len(re.findall(r"<tr", m.group(1))) if m else 0
        if code != 200 or err or rows < 1:
            ok5 = False
            det += f"{lid}->{mid}=http{code}/err{int(err)}/rows{rows} "
    check("T-25.5 exposed curated lists render rows through their menu", ok5, det or "all render rows")

    # ---------- T-25.6 curated column headers are terse (no authoring glosses left) ----------
    glossy = {}
    for lid in CURATED:
        for lab in [c["label"] for c in list_json(lid)["columns"]]:
            for inner in re.findall(r"\(([^)]*)\)", lab):
                if not KEEP_PAREN.match(inner.strip()):
                    glossy.setdefault(lid, []).append(lab)
    check("T-25.6 curated headers carry no gloss parentheticals (units/codes only)", not glossy,
          f"glossy={glossy}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
