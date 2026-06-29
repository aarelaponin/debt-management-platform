#!/usr/bin/env python3
"""Deterministic userview generator (P5) — joget-userview-gen shapes.

Builds a userview JSON from the UV-delta spec: categories of CrudMenus over
generated forms + list_* datalists. IDs are uuid5 of stable names (same spec
-> same JSON). Theme/permission left as Joget defaults (empty className) —
theming recorded in DX9-DELTAS.

Usage: gen_userview.py <uv_spec.yml> [more_uv_specs...] <out_dir> <forms_dir> <datalists_dir>
"""
import json
import os
import sys
import uuid

import yaml

NS = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")


def uid(name):
    return str(uuid.uuid5(NS, "cmbb:" + name))


def crud_menu(form_id, label):
    return {"className": "org.joget.plugin.enterprise.CrudMenu", "properties": {
        "id": uid("menu:" + form_id), "label": label,
        "addFormId": form_id, "editFormId": form_id,
        "datalistId": f"list_{form_id}", "customId": f"{form_id}_crud",
        "add-afterSaved": "list", "edit-afterSaved": "list",
        "list-showDeleteButton": "yes", "rowCount": "true",
        "buttonPosition": "bothLeft", "checkboxPosition": "left",
        "selectionType": "multiple", "iconIncluded": False}}


def datalist_menu(dl_id, label):
    return {"className": "org.joget.apps.userview.lib.DataListMenu", "properties": {
        "id": uid("menu:dl:" + dl_id), "customId": dl_id, "label": label, "datalistId": dl_id,
        "rowCount": "", "buttonPosition": "bothLeft", "checkboxPosition": "left",
        "selectionType": "multiple", "iconIncluded": False}}


def jasper_menu(m, base_dir):
    # JasperReportsMenu (Joget Enterprise): the whole .jrxml ships inline. The menu compiles
    # it at runtime, runs its SQL on the profile datasource, renders HTML + PDF/Excel export.
    # Rules: language=java (Groovy 2.4 ClassCastException on JDK21), parameters MUST be a JSON
    # array (never ""), datasource "" = profile DB, export is a ;-joined string. (joget-jasper-report)
    jrxml_path = m["jrxmlFile"] if os.path.isabs(m["jrxmlFile"]) else os.path.join(base_dir, m["jrxmlFile"])
    with open(jrxml_path) as f:
        jrxml = f.read()
    return {"className": "org.joget.plugin.enterprise.JasperReportsMenu", "properties": {
        "id": uid("menu:jasper:" + m["customId"]), "customId": m["customId"], "label": m["label"],
        "datasource": "", "output": "html", "export": m.get("export", "pdf;xls"),
        "parameters": [], "jrxml": jrxml, "iconIncluded": False}}


def category_permission(role):
    """Category access control (task #60): gate a bucket to a directory group via Joget
    GroupPermission. An empty/None role => OPEN (no permission). Per-role landing emerges for
    free: Joget lands a user on the first category they are permitted to see (BUCKET_ORDER).
    `admin` is a member of all three role groups, so admin/superuser still sees every bucket.
    """
    if not role:
        return {"className": "", "properties": {}}
    return {"className": "org.joget.apps.userview.lib.GroupPermission",
            "properties": {"groupId": role, "isExclusive": ""}}


def formmenu(form_id, label):
    # FormMenu renders a form in view mode. Used to host the dashboard forms (a CustomHTML element
    # carrying the Chart.js dashboard) — the working alternative to the script-stripping HtmlPage.
    return {"className": "org.joget.apps.userview.lib.FormMenu", "properties": {
        "id": uid("menu:form:" + form_id), "customId": form_id, "label": label,
        "formId": form_id, "readonly": "true", "readonlyLabel": "",
        "messageShowAfterComplete": "", "iconIncluded": False}}


def sqlchart_menu(m):
    # Native enterprise SqlChartMenu (ADR-002): server-side data -> Apache ECharts, no custom code.
    # Prefer a raw `sql` (datasource=default, runs on the profile DB) — the datalist-binder mode
    # ('datasource: datalist') renders "No data is available" because SqlChartMenu does not pull a
    # bound datalist's rows reliably. keyName = category/x column alias, value = numeric/y alias.
    # chartHeight/chartWidth are used RAW as CSS in sqlchart.ftl: the container is sized with
    # `width:<chartWidth>; padding-bottom:<chartHeight>; height:0` (the aspect-ratio trick). So they
    # MUST be valid CSS lengths — a bare "320" is invalid and collapses the chart to height 0.
    props = {
        "id": uid("menu:chart:" + m["customId"]), "customId": m["customId"],
        "label": m["label"], "title": m.get("title", m["label"]),
        "chartType": m.get("chartType", "bar"), "library": "echart",
        "keyName": m["keyName"], "value": m["value"], "chartUseAllDataRows": "true",
        "showValue": m.get("showValue", "true"), "showLegend": m.get("showLegend", ""),
        "horizontal": m.get("horizontal", ""),
        "chartWidth": m.get("chartWidth", "100%"), "chartHeight": m.get("chartHeight", "360px"),
        "showTable": "", "showExportLinks": "", "iconIncluded": False}
    if m.get("sql"):
        props["datasource"] = "default"
        props["query"] = m["sql"]
    else:
        props["datasource"] = "datalist"
        props["datalistId"] = m["datalistId"]
    return {"className": "org.joget.plugin.enterprise.SqlChartMenu", "properties": props}


def dashboard_menu(m, base_dir):
    # HtmlMenu (Joget core) carrying an inline dashboard: KPI tiles + Chart.js charts that fetch
    # the live MI datalist pages same-origin (session cookie) and parse their server-rendered
    # tables — no API keys, reuses the deployed datalists. The whole HTML/JS ships in `customHtml`.
    html_path = m["htmlFile"] if os.path.isabs(m["htmlFile"]) else os.path.join(base_dir, m["htmlFile"])
    with open(html_path) as f:
        content = f.read()
    return {"className": "org.joget.apps.userview.lib.HtmlPage", "properties": {
        "id": uid("menu:dash:" + m["customId"]), "customId": m["customId"], "label": m["label"],
        "content": content, "iconIncluded": False}}


# Business-friendly console: collapse the ~18 per-feature categories into 4 buckets so business users
# see WHAT THEY DO, not configuration. Mapping is by the spec category label; unknown -> Operations.
BUCKET_ORDER = ["Dashboards", "Operations", "Automation",
                "Collection settings", "Legal & reference"]
BUCKET_ICON = {"Dashboards": "fa-line-chart", "Operations": "fa-tasks",
               "Automation": "fa-bolt", "Collection settings": "fa-sliders",
               "Legal & reference": "fa-balance-scale"}
# Per-role gating + landing (task #60): each bucket is visible only to its directory group.
# `admin` is a member of all three groups (so superuser/admin checks still see everything);
# a role user lands on the first bucket they may see. Memberships compose the per-role view:
# manager -> dm_manager(+dm_officer); officer -> dm_officer; policy admin -> dm_policy_admin(+dm_officer).
BUCKET_ROLE = {"Dashboards": "dm_manager", "Operations": "dm_officer",
               "Automation": "dm_policy_admin",
               "Collection settings": "dm_collection_admin",  # operational tier (ADR-004 §7)
               "Legal & reference": "dm_legal_admin"}         # legislative tier (ADR-004 §7)
# ADR-004 §7 legislative tier: config determined by law/regulation, change-controlled. Everything
# else currently authored under an Admin-bucket category is operational → Collection settings.
LEGISLATIVE_FORMS = {
    "mdInstrument", "mdWoGround", "mdWoDelegation", "mdWoPolicy", "mdLegalFee",
    "mdAgentFee", "mdEstMethod", "mdDefAssessPolicy", "mdPublishRule", "mdViolation",
}
# Gating is OPT-IN (UV_GATING=1) and OFF by default. The GroupPermission wiring is correct and
# gates anonymous users out, BUT this DEV Joget does NOT resolve `admin`'s seeded group membership
# at render time (a DirectoryManager behaviour — admin/groups are org-less yet admin still isn't
# recognised; confirmed live 2026-06-19, see DX9-DELTAS). With gating ON the whole console goes
# blank. Keep OFF until membership resolution is fixed (create groups/members via the directory
# API, or align the directory manager), then flip UV_GATING=1 — the per-role landing follows for free.
UV_GATING = os.environ.get("UV_GATING") == "1"
BUCKET = {
    "Dashboards": "Dashboards",
    "Debt cases": "Operations", "Instalments": "Operations", "Enforcement": "Operations",
    "Write-off": "Operations", "Default assessments": "Operations", "Debtors list": "Operations",
    "Collection MI": "Operations", "Reports": "Operations", "Detail views": "Operations",
    "Operations — batch runs": "Automation",
    "Workflow configuration": "Collection settings",  # ADR-004 §6 workspace entry
    # The admin categories map to a transient "Admin" bucket; each menu is then routed per the
    # ADR-004 §7 operational/legislative split (LEGISLATIVE_FORMS) into the two real buckets.
    "DM administration": "Admin", "Relief config": "Admin", "Enforcement config": "Admin",
    "Enforcement admin": "Admin", "Write-off config": "Admin",
    "Default assessment config": "Admin", "Collection MI config": "Admin",
}


def main():
    *spec_paths, out_dir, forms_dir, dl_dir = sys.argv[1:]
    specs = []
    for p in spec_paths:
        d = yaml.safe_load(open(p))["userview_delta"]
        base = os.path.dirname(os.path.abspath(p))
        for c in d["categories"]:
            c["_dir"] = base          # resolve jasper jrxmlFile relative to this spec's dir
        specs.append(d)
    spec = specs[0]
    for extra in specs[1:]:
        spec["categories"] += extra["categories"]
    uv_id, uv_name = spec["userviewId"], spec["name"]
    bucket_menus = {b: [] for b in BUCKET_ORDER}
    for cat in spec["categories"]:
        base_dir = cat.get("_dir", ".")
        bucket = BUCKET.get(cat["label"], "Operations")
        for m in cat["menus"]:
            t = m.get("type")
            if t == "datalist":
                mm = datalist_menu(m["datalistId"], m["label"])
            elif t == "jasper":
                mm = jasper_menu(m, base_dir)
            elif t == "dashboard":
                mm = dashboard_menu(m, base_dir)
            elif t == "formmenu":
                mm = formmenu(m["formId"], m["label"])
            elif t == "sqlchart":
                mm = sqlchart_menu(m)
            else:
                mm = crud_menu(m["formDefId"], m["label"])
            # ADR-004 §7: route the transient "Admin" bucket per-menu into the two tiers.
            dest = bucket
            if dest == "Admin":
                fid = m.get("formDefId") or m.get("datalistId") or ""
                dest = "Legal & reference" if fid in LEGISLATIVE_FORMS else "Collection settings"
            bucket_menus[dest].append(mm)
    categories = []
    for b in BUCKET_ORDER:
        if not bucket_menus[b]:
            continue
        categories.append({"className": "org.joget.apps.userview.model.UserviewCategory",
            "menus": bucket_menus[b], "properties": {
                "hide": "", "permission": category_permission(BUCKET_ROLE.get(b) if UV_GATING else None),
                "comment": "", "id": "category-" + uid("bucket:" + b),
                "label": f"<i class=\"fa {BUCKET_ICON[b]}\"></i> {b}",
                "iconIncluded": True}})
    uv = {"className": "org.joget.apps.userview.model.Userview",
          "categories": categories,
          "properties": {"logoutText": "Logout", "welcomeMessage": "#date.EEE, d MMM yyyy#",
                         "name": uv_name, "description": "", "footerMessage": "MTCA CMBB",
                         "id": uv_id},
          "setting": {"properties": {"tempDisablePermissionChecking": "",
                       "userviewDescription": "", "userviewId": uv_id,
                       "hideThisUserviewInAppCenter": "", "userview_thumbnail": "",
                       "userview_category": "",
                       "theme": {"className": "", "properties": {}},
                       "permission": {"className": "", "properties": {}},
                       "userviewName": uv_name}}}
    os.makedirs(out_dir, exist_ok=True)
    out = os.path.join(out_dir, uv_id + ".json")
    json.dump(uv, open(out, "w"), indent=2)
    forms = {f.replace(".json", "") for f in os.listdir(forms_dir)}
    dls = {f.replace(".json", "") for f in os.listdir(dl_dir)}
    errs = []
    for cat in categories:
        for m in cat["menus"]:
            p = m["properties"]
            for k in ("addFormId", "editFormId", "formId"):
                if k in p and p[k] not in forms: errs.append(f"{p['label']}: {k}->{p[k]} missing")
            if p.get("datalistId") and p["datalistId"] not in dls: errs.append(f"{p['label']}: datalist missing")
    n = sum(len(c["menus"]) for c in categories)
    print(f"userview {uv_id}: {len(categories)} categories, {n} menus -> {out}")
    print("reference check:", errs if errs else "all menu refs resolve — PASS")
    sys.exit(1 if errs else 0)


if __name__ == "__main__":
    main()
