#!/usr/bin/env python3
"""Deploy CMBB-F01 batch to a Joget instance (Stage 4, joget-deploy skill order).

Uses joget-deployment-toolkit (session auth via ~/.joget/instances.yaml) and
the Joget console JSON API: ensure app -> forms -> datalists -> userview.
Run with the toolkit's venv python and JDX9_PASSWORD set, e.g.:
  JDX9_PASSWORD=admin <toolkit>/venv/bin/python scripts/deploy_cmbb_f01.py jdx9 cmbb
"""
import json
import os
import sys

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.client.base import *  # noqa: F401,F403 (ensures package init)
from joget_deployment_toolkit.api.client import JogetClient

GEN = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "cmbb", "generated")


def main():
    instance, app_id = (sys.argv + ["jdx9", "cmbb"])[1:3]
    client = JogetClient.from_instance(instance)
    app_name = "CMBB Case Fabric"

    # 1. ensure app exists
    apps = client.get("/web/json/console/app/list", params={"pageSize": 100})
    ids = [a.get("id") for a in apps.get("data", [])] if isinstance(apps, dict) else []
    if app_id in ids:
        print(f"app {app_id}: exists")
    else:
        ok = False
        for payload in ({"id": app_id, "name": app_name},
                        {"appId": app_id, "appName": app_name}):
            try:
                r = client.post("/web/json/console/app/submit", data=payload)
                print(f"app create response: {r}")
                ok = True
                break
            except Exception as e:
                print(f"app create attempt {payload}: {e}")
        if not ok:
            sys.exit("could not create app — create 'cmbb' once in App Center UI, then rerun")

    results = {"forms": 0, "datalists": 0, "userviews": 0}
    errors = []

    # 2. forms (md* first, then mmCalendar/mmCaseType, then the rest — FIS §5 order)
    order = sorted(os.listdir(f"{GEN}/forms"),
                   key=lambda f: (0 if f.startswith("md") else
                                  1 if f in ("mmCalendar.json", "mmCaseType.json") else 2, f))
    for fn in order:
        d = json.load(open(f"{GEN}/forms/{fn}"))
        p = d["properties"]
        try:
            client.post(f"/web/json/console/app/{app_id}/1/form/submit",
                        data={"id": p["id"], "name": p["name"],
                              "tableName": p["tableName"], "json": json.dumps(d)})
            results["forms"] += 1
            print(f"  + form {p['id']}")
        except Exception as e:
            errors.append(f"form {p['id']}: {e}")

    # 3. datalists
    for fn in sorted(os.listdir(f"{GEN}/datalists")):
        d = json.load(open(f"{GEN}/datalists/{fn}"))
        try:
            client.post(f"/web/json/console/app/{app_id}/1/datalist/submit",
                        data={"id": d["id"], "name": d["name"], "description": "",
                              "json": json.dumps(d)})
            results["datalists"] += 1
            print(f"  + datalist {d['id']}")
        except Exception as e:
            errors.append(f"datalist {d['id']}: {e}")

    # 4. userview
    for fn in sorted(os.listdir(f"{GEN}/userviews")):
        d = json.load(open(f"{GEN}/userviews/{fn}"))
        uid = d["properties"]["id"]
        try:
            client.post(f"/web/json/console/app/{app_id}/1/userview/submit",
                        data={"id": uid, "name": d["properties"]["name"],
                              "description": "", "json": json.dumps(d)})
            results["userviews"] += 1
            print(f"  + userview {uid}")
        except Exception as e:
            errors.append(f"userview {uid}: {e}")

    print(f"\nDEPLOYED: {results}")
    if errors:
        print("ERRORS:")
        [print("  !", e) for e in errors]
        sys.exit(1)


if __name__ == "__main__":
    main()
