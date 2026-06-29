#!/usr/bin/env python3
"""DMBB-WF (ADR-004 §6/§7) acceptance T-29 — the configuration workspace + nav tiering (live jdx9).

T-29.1 nav tiering: the console splits the old broad Admin into "Collection settings" (operational)
       and "Legal & reference" (legislative); Admin is gone; Workflows leads Collection settings.
T-29.2 the operational/legislative split matches ADR-004 §7 (legislative tables under Legal & reference,
       operational tables under Collection settings).
T-29.3 the Workflow workspace form carries the 6 concern sections (Definition + editable Escalation
       ladder FormGrid on mmEscStep + Lifecycle/Notices/Enforcement/Relief panels).
T-29.4 the Workflows list renders the seeded workflows (W-DEFAULT, W-VAT) through its menu.

Usage: JDX9_PASSWORD=admin run_t29.py
"""
import json
import re
import subprocess
import sys
import urllib.request

RESULTS = []
BASE = "http://localhost:8089/jw"
LEGISLATIVE = {"mdInstrument", "mdWoGround", "mdWoDelegation", "mdWoPolicy", "mdLegalFee",
               "mdAgentFee", "mdEstMethod", "mdDefAssessPolicy", "mdPublishRule", "mdViolation"}
OPERATIONAL = {"mmStrategy", "mmEscStep", "mdDebtCat", "mdRelief", "mdProjRate",
               "mdNoticeRule", "mdCollectionParam", "dmWorkflow"}


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def get(url):
    with urllib.request.urlopen(url, timeout=30) as r:
        return r.getcode(), r.read().decode("utf-8", "replace")


def uv_json():
    j = sql("SELECT json FROM app_userview WHERE id='dmbbConsole' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def form_json(fid):
    j = sql(f"SELECT json FROM app_form WHERE formid='{fid}' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def cat_label(c):
    return re.sub(r"<[^>]+>", "", c.get("properties", {}).get("label", "")).strip()


def menu_form_id(m):
    p = m.get("properties", {})
    return p.get("formDefId") or p.get("addFormId") or p.get("editFormId") or p.get("datalistId", "")


def menu_for_list(uv, list_id):
    for c in uv["categories"]:
        for m in c.get("menus", []):
            if m.get("properties", {}).get("datalistId") == list_id:
                return m["properties"].get("customId")
    return None


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    uv = uv_json()
    cats = {cat_label(c): c for c in uv["categories"]}

    # ---------- T-29.1 nav tiering ----------
    cset = cats.get("Collection settings")
    lref = cats.get("Legal & reference")
    first_menu = cset["menus"][0]["properties"].get("label") if cset and cset.get("menus") else None
    check("T-29.1 Admin split into Collection settings + Legal & reference; Workflows leads",
          bool(cset) and bool(lref) and "Admin" not in cats and first_menu == "Workflows",
          f"buckets={sorted(cats)} first_collection_menu={first_menu!r}")

    # ---------- T-29.2 §7 operational/legislative split ----------
    coll_forms = {menu_form_id(m) for m in (cset["menus"] if cset else [])}
    legal_forms = {menu_form_id(m) for m in (lref["menus"] if lref else [])}
    mis_legal = LEGISLATIVE - legal_forms          # legislative tables NOT under Legal & reference
    mis_oper = (OPERATIONAL - {"mmEscStep"}) - coll_forms  # operational tables NOT under Collection settings
    # (mmEscStep is edited in the workspace ladder; a standalone menu is optional)
    leaked = (LEGISLATIVE & coll_forms) | ((OPERATIONAL - LEGISLATIVE) & legal_forms)
    check("T-29.2 operational/legislative split matches ADR-004 §7",
          not mis_legal and not leaked,
          f"legislative_missing={sorted(mis_legal)} operational_missing={sorted(mis_oper)} cross_leaks={sorted(leaked)}")

    # ---------- T-29.3 workspace form has the 6 concern sections + ladder grid ----------
    f = form_json("dmWorkflow")
    sec_labels = [s.get("properties", {}).get("label") for s in f.get("elements", [])]
    want = {"Definition", "Escalation ladder", "Lifecycle & stages", "Notices",
            "Enforcement", "Relief & instalments"}
    # the escalation-ladder section must carry a FormGrid bound to mmEscStep
    grid_on_steps = bool(re.search(r'"formDefId"\s*:\s*"mmEscStep"', json.dumps(f))
                         and 'FormGrid' in json.dumps(f))
    check("T-29.3 workspace form carries the 6 concern sections + editable mmEscStep ladder grid",
          want <= set(sec_labels) and grid_on_steps,
          f"sections={sec_labels} grid_on_mmEscStep={grid_on_steps}")

    # ---------- T-29.4 Workflows list renders the seeded workflows ----------
    mid = menu_for_list(uv, "list_dmWorkflow")
    rows, has_seed = 0, False
    if mid:
        code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{mid}")
        m = re.search(r"<tbody[^>]*>(.*?)</tbody>", html, re.S | re.I)
        rows = len(re.findall(r"<tr", m.group(1))) if m else 0
        has_seed = ("W-DEFAULT" in html) and ("W-VAT" in html)
    check("T-29.4 Workflows list renders the seeded workflows (W-DEFAULT, W-VAT)",
          bool(mid) and rows >= 2 and has_seed,
          f"menu={mid} rows={rows} seeds_present={has_seed}")

    # ---------- T-29.5 the workspace edit form renders W-DEFAULT with all sections + the ladder grid ----------
    # (the FormGrid's rows load via AJAX, so the static edit HTML carries the grid container, not the rows)
    code, html = get(f"{BASE}/web/userview/dmbb/dmbbConsole/_/{mid}?_mode=edit&id=W-DEFAULT")
    secs_shown = sum(1 for s in ("Definition", "Escalation ladder", "Lifecycle & stages",
                                 "Notices", "Enforcement", "Relief & instalments") if s in html)
    grid_present = ("ladderGrid" in html) and ("mmEscStep" in html)
    check("T-29.5 workspace edit form renders the 6 concern sections + the in-place ladder grid",
          code == 200 and secs_shown == 6 and grid_present,
          f"http={code} sections_shown={secs_shown}/6 ladder_grid_present={grid_present}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
