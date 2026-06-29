#!/usr/bin/env python3
"""Deploy the GAM prefill demo to jdx8 — a thin variant of scripts/deploy_jwa.py.

Why this exists: jdx8's `url` in ~/.joget/instances.yaml is missing the `/jw` context
(every other 9.0.x instance has it), so JogetClient.from_instance("jdx8") builds the
wrong base and the console calls 404. That file is sysadmin-owned, so instead of editing
it we construct the client from an explicit base via from_credentials, and point version
discovery at the GAM DB (jwdb_gam). Same delete->import->publish flow otherwise.
"""
import os
import re
import subprocess
import sys
import tempfile

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

HERE = "/Users/aarelaponin/IdeaProjects/rsr/mt-tca-prj/10-workstreams/itcas-programme/03_debt_management/scripts"
GEN = "/Users/aarelaponin/IdeaProjects/rsr/mt-tca-prj/10-workstreams/itcas-programme/03_debt_management/shared-plugins/joget-form-prefill/demo-app-gam/generated"
APP_ID = "prefillgam"
APP_NAME = "GAM Prefill Demo"
BASE = "http://localhost:8088/jw"

jwa = os.path.join(tempfile.gettempdir(), f"APP_{APP_ID}-jdx8.jwa")
subprocess.run([sys.executable, os.path.join(HERE, "build_jwa.py"), GEN, APP_ID, APP_NAME, jwa], check=True)

client = JogetClient.from_credentials(BASE, "admin", "admin")
client.get("/web/json/console/app/list", params={"pageSize": 1})  # authenticate
s = getattr(client, "session", None) or client._session


def csrf():
    r = s.get(BASE + "/csrf", headers={"Referer": BASE + "/web/console/home"})
    return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)


def versions():
    env = {"PGPASSWORD": "joget_gam", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"}
    q = f"SELECT appversion FROM app_app WHERE appid='{APP_ID}' ORDER BY appversion"
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_gam", "-d", "jwdb_gam",
                        "-t", "-A", "-c", q], env=env, capture_output=True, text=True)
    return [v for v in r.stdout.split() if v.strip().isdigit()]


for v in (versions() or ["1"]):
    r = s.post(BASE + f"/web/console/app/{APP_ID}/{v}/delete",
               data={"OWASP_CSRFTOKEN": csrf()},
               headers={"Referer": BASE + "/web/console/home"})
    print(f"delete v{v}:", r.status_code)

with open(jwa, "rb") as f:
    r = s.post(BASE + "/web/console/app/import/submit",
               files={"appZip": (os.path.basename(jwa), f, "application/zip")},
               data={"OWASP_CSRFTOKEN": csrf()})
print("import:", r.status_code)

vs = versions()
pub_v = vs[-1] if vs else "1"
r = s.post(BASE + f"/web/console/app/{APP_ID}/{pub_v}/publish", data={"OWASP_CSRFTOKEN": csrf()})
print(f"publish v{pub_v}:", r.status_code)

apps = client.get("/web/json/console/app/list", params={"pageSize": 100})
data = apps.get("data", [])
ids = [a["id"] for a in (data if isinstance(data, list) else [data])]
print("apps on jdx8:", ids)
print("OK" if APP_ID in ids else "FAILED: app not present")
sys.exit(0 if APP_ID in ids else 1)
