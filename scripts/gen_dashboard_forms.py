#!/usr/bin/env python3
"""Wrap each generated dashboard HTML (gen_dashboards.py output) in a Joget Form carrying a single
CustomHTML element, so it can be exposed via a FormMenu. This is the host that actually executes the
dashboard's <script>/<canvas> — Joget's HtmlPage menu does not (DX9-DELTAS 2026-06-14). CustomHTML
renders its `value` raw (template emits ${value!}, unescaped); requiredSanitize is left OFF so the
Chart.js script survives.

Usage: gen_dashboard_forms.py <dash_html_dir> <out_forms_dir>
"""
import json
import os
import sys
import uuid

NS = uuid.UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")

# dashboard html file (without .html) -> (formId, form name, menu label)
FORMS = {
    "dash_overview": ("dashOverviewForm", "DM - Management Dashboard", "Management dashboard"),
}


def uid(name):
    return str(uuid.uuid5(NS, "cmbb:" + name))


def build_form(form_id, name, html):
    custom = {"className": "org.joget.apps.form.lib.CustomHTML", "properties": {
        "id": form_id + "Html", "value": html, "label": "",
        "autoPopulate": "", "requiredSanitize": ""}}   # sanitize OFF: keep <script>/<canvas>
    column = {"className": "org.joget.apps.form.model.Column",
              "properties": {"width": "100%"}, "elements": [custom]}
    section = {"className": "org.joget.apps.form.model.Section",
               "properties": {"id": "dashSection", "label": ""}, "elements": [column]}
    return {"className": "org.joget.apps.form.model.Form", "properties": {
        "id": form_id, "name": name, "tableName": "dmDashboard",
        "loadBinder": {"className": "", "properties": {}},
        "storeBinder": {"className": "", "properties": {}},
        "description": "", "noPermissionMessage": "", "id_field_id": ""},
        "elements": [section]}


def main():
    html_dir, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    for stem, (form_id, name, _label) in FORMS.items():
        with open(os.path.join(html_dir, stem + ".html")) as f:
            html = f.read()
        form = build_form(form_id, name, html)
        with open(os.path.join(out_dir, form_id + ".json"), "w") as f:
            json.dump(form, f, indent=2)
        print(f"  + {form_id}.json  (CustomHTML {len(html)} chars)")
    print(f"{len(FORMS)} dashboard forms generated -> {out_dir}")


if __name__ == "__main__":
    main()
