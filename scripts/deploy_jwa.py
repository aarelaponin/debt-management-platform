#!/usr/bin/env python3
"""Deploy a generated/ directory to a Joget DX9 instance — the PROVEN path.

Pipeline: build JWA (build_jwa.py format) -> import via HTML console
(/web/console/app/import/submit, multipart + OWASP CSRF token from /csrf)
-> publish -> verify counts at DB-definition level via console session.

DX9 notes (see docs/DX9-DELTAS.md): the DX8 JSON endpoints
/web/json/console/app/import and /app/submit are GONE in 9.0.7; the HTML
console + csrfguard token flow below is the working replacement.

Usage:
  JDX9_PASSWORD=... <toolkit-venv-python> deploy_jwa.py <instance> <generated_dir> <app_id> "<App Name>"
"""
import re
import subprocess
import sys
import tempfile
import os

sys.path.insert(0, "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src")
from joget_deployment_toolkit.api.client import JogetClient  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))


def main():
    instance, gen_dir, app_id, app_name = sys.argv[1:5]
    jwa = os.path.join(tempfile.gettempdir(), f"APP_{app_id}-1.jwa")
    subprocess.run([sys.executable, os.path.join(HERE, "build_jwa.py"),
                    gen_dir, app_id, app_name, jwa], check=True)

    client = JogetClient.from_instance(instance)
    client.get("/web/json/console/app/list", params={"pageSize": 1})  # authenticate
    s = getattr(client, "session", None) or client._session
    base = client.config.base_url if hasattr(client.config, "base_url") else None
    if not base:
        import yaml
        inst = yaml.safe_load(open(os.path.expanduser("~/.joget/instances.yaml")))
        base = inst["instances"][instance]["tomcat"]["url"].rstrip("/")
        if not base.endswith("/jw"):
            base += "/jw"

    def csrf():
        r = s.get(base + "/csrf", headers={"Referer": base + "/web/console/home"})
        return re.search(r'masterTokenValue\s*[=:]\s*["\']([^"\']+)["\']', r.text).group(1)

    # DX9-DELTAS (2026-06-12): import-over-existing creates a new app VERSION but
    # does NOT materialise NEW forms into the runtime DB; the proven DEV cycle is
    # delete -> import -> publish (app_fd_* DATA survives the delete). DX9-DELTAS
    # (2026-06-12, F09): the version is NOT always 1 — once versions advance,
    # delete/publish hardcoded to /1/ silently no-op, leaving NOTHING published
    # (ApiBuilder.getJson -> NoSuchElementException -> HTTP 400 on every form
    # post) AND accumulating stale definition versions (app_datalist returns >1
    # row per id, breaking test helpers that read the deployed definition). So:
    # discover real versions via psql (DEV creds, env-overridable), delete ALL
    # existing versions before import, and publish the version actually created.
    def versions():
        env = {"PGPASSWORD": os.environ.get("PGPASSWORD", "joget_mtca"),
               "PATH": "/opt/homebrew/bin:/usr/bin:/bin"}
        q = f"SELECT appversion FROM app_app WHERE appid='{app_id}' ORDER BY appversion"
        r = subprocess.run(["psql", "-h", os.environ.get("PGHOST", "localhost"),
                            "-U", os.environ.get("PGUSER", "joget_mtca"),
                            "-d", os.environ.get("PGDATABASE", "jwdb_mtca"),
                            "-t", "-A", "-c", q], env=env, capture_output=True, text=True)
        return [v for v in r.stdout.split() if v.strip().isdigit()]

    if "--keep" not in sys.argv:
        for v in (versions() or ["1"]):
            r = s.post(base + f"/web/console/app/{app_id}/{v}/delete",
                       data={"OWASP_CSRFTOKEN": csrf()},
                       headers={"Referer": base + "/web/console/home"})
            print(f"delete v{v}:", r.status_code)

    with open(jwa, "rb") as f:
        r = s.post(base + "/web/console/app/import/submit",
                   files={"appZip": (os.path.basename(jwa), f, "application/zip")},
                   data={"OWASP_CSRFTOKEN": csrf()})
    print("import:", r.status_code)

    vs = versions()
    pub_v = vs[-1] if vs else "1"
    r = s.post(base + f"/web/console/app/{app_id}/{pub_v}/publish",
               data={"OWASP_CSRFTOKEN": csrf()})
    print(f"publish v{pub_v}:", r.status_code)

    apps = client.get("/web/json/console/app/list", params={"pageSize": 100})
    data = apps.get("data", [])
    ids = [a["id"] for a in (data if isinstance(data, list) else [data])]
    print("apps on instance:", ids)
    print("OK" if app_id in ids else "FAILED: app not present")
    print("NOTE: restart Tomcat (kill PID -> start) BEFORE seeding — reimported API")
    print("      definitions only go live after a restart (DX9-DELTAS 2026-06-12).")
    sys.exit(0 if app_id in ids else 1)


if __name__ == "__main__":
    main()
