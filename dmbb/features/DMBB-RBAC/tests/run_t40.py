#!/usr/bin/env python3
"""DMBB-RBAC acceptance T-40 — per-role userview gating works (#91 fixed), live jdx9.

Background: the console categories are gated with the native GroupPermission (per-role landing).
#91 was a property-name bug in gen_userview's emitter — it wrote `groupId` but the DX9 plugin reads
`allowedGroupIds`, so the allow-list was always empty → deny-all (admin included). Fixed by emitting
`allowedGroupIds`; the directory already resolves admin's flat group membership (same getGroupByUsername
DAS P3 uses). See docs/design/SPIKE-groupperm.md.

The decisive signature that distinguishes the FIXED state from both the broken (deny-all: admin=0)
and the open (no gating: anon=all) states is: admin (member of all role groups) sees every category,
while anonymous is denied — proven LIVE here.

T-40.1 config: every deployed category carries GroupPermission with a non-empty `allowedGroupIds`
       equal to its bucket's role (no category left OPEN; no empty allow-list = the #91 bug).
T-40.2 gating real: an anonymous GET of a representative menu in each category is DENIED (302), not
       served — so the categories are genuinely access-controlled.
T-40.3 admin sees all: an admin session (admin is a member of all six role groups) gets 200 + real
       content for a representative menu in every category.
T-40.4 per-role landing (derived from the deployed config + live directory memberships): a single-
       group user (officer1 in dm_officer only) would see exactly {Operations}; admin sees all.

Usage: JDX9_PASSWORD=admin run_t40.py
"""
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.request

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "..", "..", "scripts"))
import uv_auth  # noqa: E402

BASE = "http://localhost:8089/jw"
UV_MENU = BASE + "/web/userview/dmbb/dmbbConsole/_/"
RESULTS = []
EXPECT_ROLE = {
    "Dashboards": "dm_manager", "Operations": "dm_officer", "Automation": "dm_policy_admin",
    "Approvals": "dm_supervisor", "Collection settings": "dm_collection_admin",
    "Legal & reference": "dm_legal_admin", "Approvals MI": "dm_manager",
}
CONTENT = ("dataList", "datalist", "chart", "<form", "<table", "userview")


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("SQL-ERR:" + r.stderr.strip()[:200]) if r.returncode else r.stdout.strip()


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def uv_json():
    j = sql("SELECT json FROM app_userview WHERE id='dmbbConsole' AND appid='dmbb' ORDER BY appversion DESC LIMIT 1")
    return json.loads(j)


def cat_label(c):
    return re.sub(r"<[^>]+>", "", c.get("properties", {}).get("label", "")).strip()


def first_menu_id(c):
    for m in c.get("menus", []):
        p = m.get("properties", {})
        mid = p.get("customId") or p.get("id")
        if mid:
            return mid
    return None


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None  # do not follow — a 302 to login is the "denied" signal


_anon = urllib.request.build_opener(_NoRedirect)


def anon_status(mid):
    try:
        r = _anon.open(UV_MENU + mid, timeout=30)
        return r.getcode(), r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, ""


def main():
    uv = uv_json()
    cats = uv["categories"]

    # T-40.1 — config: every category gated with the correct non-empty allowedGroupIds
    bad = []
    for c in cats:
        label = cat_label(c)
        perm = c.get("properties", {}).get("permission", {}) or {}
        gids = (perm.get("properties", {}) or {}).get("allowedGroupIds", "")
        want = EXPECT_ROLE.get(label)
        if perm.get("className") != "org.joget.apps.userview.lib.GroupPermission" or not gids or gids != want:
            bad.append(f"{label!r}: class={perm.get('className', '')!r} gids={gids!r} want={want!r}")
    check("T-40.1 every category gated with correct non-empty allowedGroupIds",
          not bad and len(cats) >= 7, "; ".join(bad) if bad else f"{len(cats)} categories all gated")

    reps = {cat_label(c): first_menu_id(c) for c in cats}

    # T-40.2 — anonymous is DENIED each category's page (302, not served)
    anon_bad = []
    for label, mid in reps.items():
        if not mid:
            continue
        code, body = anon_status(mid)
        served = code == 200 and any(k in body for k in CONTENT)
        if served or code not in (301, 302, 303, 401, 403):
            anon_bad.append(f"{label}:{mid}->{code}")
    check("T-40.2 anonymous denied every gated category (302, not served)",
          not anon_bad, "denied all" if not anon_bad else "leaked=" + ", ".join(anon_bad))

    # T-40.3 — admin (member of all role groups) is SERVED each category's page
    admin_bad = []
    for label, mid in reps.items():
        if not mid:
            continue
        code, body = uv_auth.admin_get(UV_MENU + mid)
        if code != 200 or not any(k in body for k in CONTENT):
            admin_bad.append(f"{label}:{mid}->{code}/{len(body)}b")
    check("T-40.3 admin sees content in every gated category",
          not admin_bad, "all served" if not admin_bad else "denied=" + ", ".join(admin_bad))

    # T-40.4 — per-role landing derived from deployed config + live directory memberships
    cat_role = {cat_label(c): (c.get("properties", {}).get("permission", {}).get("properties", {}) or {}).get("allowedGroupIds", "")
                for c in cats}

    def visible_for(user):
        groups = set(filter(None, sql(f"SELECT groupid FROM dir_user_group WHERE userid='{user}'").splitlines()))
        return {label for label, role in cat_role.items() if role in groups}, groups

    off_vis, off_groups = visible_for("officer1")
    adm_vis, adm_groups = visible_for("admin")
    # officer1 is in dm_officer (+ the base cmbb_user group, which gates no category) → its VISIBLE
    # set is exactly {Operations}; admin is in all six role groups → sees every category.
    off_ok = off_vis == {"Operations"} and "dm_officer" in off_groups
    adm_ok = adm_vis == set(cat_role.keys()) and len(adm_vis) >= 7
    check("T-40.4 per-role landing: officer1 sees only Operations; admin sees all",
          off_ok and adm_ok,
          f"officer1={sorted(off_vis)} (groups={sorted(off_groups)}); admin sees {len(adm_vis)}/{len(cat_role)}")

    npass = sum(1 for _, ok in RESULTS if ok)
    total = len(RESULTS)
    print(f"\n{npass}/{total} passed — " + ("ALL GREEN" if npass == total else "FAILED"))
    sys.exit(0 if npass == total else 1)


if __name__ == "__main__":
    main()
