#!/usr/bin/env python3
"""Delete a Joget app version from an instance (DEV update cycle, DX9-DELTAS).

Importing a JWA over an EXISTING app only creates a new Git version — new forms
are NOT materialised into the runtime DB. The proven DEV cycle is therefore
delete -> import -> publish. This deletes the app; app_fd_* DATA survives.

Usage:
  JDX9_PASSWORD=... <toolkit-venv-python> delete_app.py <instance> <app_id> [version]
"""
import os
import re
import sys

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402


def main():
    instance = sys.argv[1]
    app_id = sys.argv[2]
    version = sys.argv[3] if len(sys.argv) > 3 else "1"

    client = JogetClient.from_instance(instance)
    client.get("/web/json/console/app/list", params={"pageSize": 1})  # authenticate
    s = getattr(client, "session", None) or client._session

    base = None
    if hasattr(client, "config") and hasattr(client.config, "base_url"):
        base = client.config.base_url
    if not base:
        import yaml
        inst = yaml.safe_load(open(os.path.expanduser("~/.joget/instances.yaml")))
        base = inst["instances"][instance]["tomcat"]["url"].rstrip("/")
        if not base.endswith("/jw"):
            base += "/jw"

    r = s.get(base + "/csrf", headers={"Referer": base + "/web/console/home"})
    token = re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    r = s.post(base + f"/web/console/app/{app_id}/{version}/delete",
               data={"OWASP_CSRFTOKEN": token},
               headers={"Referer": base + "/web/console/home"})
    print("delete:", r.status_code)

    apps = client.get("/web/json/console/app/list", params={"pageSize": 100})
    data = apps.get("data", [])
    ids = [a["id"] for a in (data if isinstance(data, list) else [data])]
    print("apps on instance:", ids)
    print("OK — app removed" if app_id not in ids else "WARN: app still present")
    sys.exit(0)


if __name__ == "__main__":
    main()
