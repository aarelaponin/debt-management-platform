"""Admin-authenticated userview page fetch for the render acceptance tests.

Since #91 was fixed, the dmbb console categories are gated with GroupPermission (per-role landing).
Anonymous GETs of a gated menu page now 302 to login; `admin` is a member of ALL role groups, so an
admin session sees every category. The render tests therefore fetch userview PAGES through this
helper (one login per process via the toolkit JogetClient; JDX9_PASSWORD from env, default "admin").

Scope: this covers userview page GETs ONLY. The tests' form-data API POSTs keep their own
api_id/api_key header auth and are deliberately NOT routed through here, so engine/API behaviour
(incl. the P3 roleAnonymous semantics) is unchanged.
"""
import os
import sys

_TOOLKIT_SRC = "/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/src"
if _TOOLKIT_SRC not in sys.path:
    sys.path.insert(0, _TOOLKIT_SRC)

_sess = None
_base = None


def _session():
    """Lazily log in as admin once per process; return (requests_session, base_url)."""
    global _sess, _base
    if _sess is None:
        os.environ.setdefault("JDX9_PASSWORD", "admin")
        from joget_deployment_toolkit.api.client import JogetClient
        c = JogetClient.from_instance("jdx9")
        c.get("/web/json/console/app/list", params={"pageSize": 1})  # authenticate as admin
        _sess = getattr(c, "session", None) or c._session
        _base = c.config.base_url.rstrip("/")
    return _sess, _base


def admin_get(url, timeout=30):
    """GET a userview page (absolute URL or /path) as admin. Returns (status_code, text)."""
    sess, base = _session()
    if url.startswith("/"):
        url = base + url
    try:
        r = sess.get(url, headers={"Referer": base + "/web/console/home"}, timeout=timeout)
        return r.status_code, (r.text or "")
    except Exception as e:  # never raise from a fetch helper — mirror the tests' get() contract
        return 0, f"ADMIN-GET-ERROR: {e}"
