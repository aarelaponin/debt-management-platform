#!/usr/bin/env python3
"""One-off: delete stale app versions left by deploy_jwa's hardcoded-version-1
delete/publish (versions accumulate -> app_datalist returns >1 row per id ->
test helpers that read the definition get concatenated JSON). Keeps the
published version. Usage: _delete_v.py <app> <version> [<version> ...]"""
import re
import sys

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

app = sys.argv[1]
versions = sys.argv[2:]
BASE = "http://localhost:8089/jw"
client = JogetClient.from_instance("jdx9")
client.get("/web/json/console/app/list", params={"pageSize": 1})
s = getattr(client, "session", None) or client._session
s.headers["Referer"] = BASE + "/web/console/home"


def csrf():
    r = s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
    return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)


for v in versions:
    r = s.post(f"{BASE}/web/console/app/{app}/{v}/delete",
               data={"OWASP_CSRFTOKEN": csrf()},
               headers={"Referer": BASE + "/web/console/home"})
    print(f"delete {app} v{v}:", r.status_code)
