#!/usr/bin/env python3
"""One-off: publish a specific app version (deploy_jwa hardcodes version 1, but
delete->import bumps the version, leaving nothing published -> ApiBuilder.getJson
NoSuchElementException -> 400 on all form API). Usage: _publish_v.py <app> <version>"""
import re
import sys

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

app, version = sys.argv[1], sys.argv[2]
BASE = "http://localhost:8089/jw"
client = JogetClient.from_instance("jdx9")
client.get("/web/json/console/app/list", params={"pageSize": 1})
s = getattr(client, "session", None) or client._session
s.headers["Referer"] = BASE + "/web/console/home"


def csrf():
    r = s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
    return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)


r = s.post(f"{BASE}/web/console/app/{app}/{version}/publish",
           data={"OWASP_CSRFTOKEN": csrf()},
           headers={"Referer": BASE + "/web/console/home"})
print(f"publish {app} v{version}:", r.status_code)
