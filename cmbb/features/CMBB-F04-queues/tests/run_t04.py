#!/usr/bin/env python3
"""CMBB-F04 acceptance T-04.1..5 — definition + deployed-SQL assertions.
Fixtures via form API; the queue SQL under test is extracted from the
DEPLOYED datalist definitions (app_datalist) and run read-only.

Usage: run_t04.py
"""
import datetime
import json
import subprocess
import sys
import time
import urllib.request

BASE = "http://localhost:8089/jw"
RESULTS = []
RUN = str(int(time.time()))[-6:]


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca",
                        "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    if r.returncode != 0:
        return "SQL-ERROR: " + r.stderr.strip()[:200]
    return r.stdout.strip()


KEY = sql("SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'")


def form_post(form_id, payload):
    req = urllib.request.Request(f"{BASE}/api/form/{form_id}/saveOrUpdate",
                                 data=json.dumps(payload).encode(), method="POST")
    for h, v in [("Content-Type", "application/json"), ("Accept", "application/json"),
                 ("api_id", "API-cmbb-data"), ("api_key", KEY)]:
        req.add_header(h, v)
    with urllib.request.urlopen(req) as r:
        body = r.read().decode()
        try:
            errs = json.loads(body).get("errors") or {}
        except ValueError:
            errs = {}
        if errs:
            raise SystemExit(f"form API validation error on {form_id}: {body[:300]}")
        return r.status


def deployed_sql(list_id):
    raw = sql(f"SELECT json FROM app_datalist WHERE id='{list_id}' AND appid='cmbb'")
    return json.loads(raw)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond), detail))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    assert KEY, "no api key"
    sql("DELETE FROM app_fd_cmevent")
    sql("DELETE FROM app_fd_cmcase")
    today = datetime.date.today()

    def case(n, **kw):
        row = {"id": f"q-{RUN}-{n}", "caseType": "TEST", "origin": "MANUAL",
               "currentState": "OPEN", "tin": f"Q{n}-{RUN}"}
        row.update({k: str(v) for k, v in kw.items()})
        # verify-and-retry: first writes can race Hibernate schema updates
        for attempt in range(4):
            form_post("cmCase", row)
            time.sleep(1.5)
            if sql(f"SELECT count(*) FROM app_fd_cmcase WHERE id='{row['id']}'") == "1":
                return row["id"]
        raise SystemExit(f"fixture row {row['id']} not persisted after retries")

    a = case("A", riskScore=90, amountAtStake=100, taxpayerName="Alpha Ltd",
             assignee="officer1", nextActionDue=(today - datetime.timedelta(days=1)).isoformat())
    b = case("B", riskScore=50, amountAtStake=999, taxpayerName="Beta Ltd",
             assignee="officer1", nextActionDue=(today + datetime.timedelta(days=1)).isoformat())
    c = case("C", amountAtStake=500, taxpayerName="Gamma Ltd",
             assignee="officer2", nextActionDue=(today + datetime.timedelta(days=10)).isoformat())

    # ---------- T-04.1: deployed queue SQL — order + priority bands ----------
    qdef = deployed_sql("list_queue")
    qsql = qdef["binder"]["properties"]["sql"]
    out = sql(f"SELECT case_ref || '|' || priority FROM ({qsql}) q WHERE q.id LIKE 'q-{RUN}-%'")
    # case_ref blank for API-created rows (no envelope run) -> assert via id order instead
    out_ids = sql(f"SELECT q.id || '|' || q.priority FROM ({qsql}) q WHERE q.id LIKE 'q-{RUN}-%'")
    expected = [f"{a}|H", f"{b}|M", f"{c}|M"]
    check("T-04.1 BR-WF-004 order + priority bands",
          out_ids.splitlines() == expected, out_ids.replace("\n", " ; "))
    cols = [col["name"] for col in qdef["columns"]]
    check("T-04.1 queue columns complete",
          {"tin", "taxpayer", "debt_amount", "risk_score", "priority",
           "days_in_queue", "sla_status"} <= set(cols), ",".join(cols))

    # ---------- T-04.2: binder + filters ----------
    filters = {f["name"] for f in qdef.get("filters", [])}
    check("T-04.2 JDBC binder + filters",
          qdef["binder"]["className"].endswith("JdbcDataListBinder")
          and {"case_type", "category", "sla_status", "unit"} <= filters,
          ",".join(sorted(filters)))

    # ---------- T-04.3: worklist scoping ----------
    wdef = deployed_sql("list_worklist")
    wsql = wdef["binder"]["properties"]["sql"].replace("#currentUser.username#", "officer1")
    rows = sql(f"SELECT q.id FROM ({wsql}) q WHERE q.id LIKE 'q-{RUN}-%'")
    got = set(rows.splitlines())
    check("T-04.3 worklist scoped to current user", got == {a, b}, rows.replace("\n", ","))

    # ---------- T-04.4: red/amber highlighting ----------
    flags = sql(f"SELECT q.id || '|' || q.due_flag FROM ({wsql}) q WHERE q.id LIKE 'q-{RUN}-%'")
    fmap = dict(line.split("|", 1) for line in flags.splitlines())
    check("T-04.4 overdue red / <24h amber",
          "OVERDUE" in fmap.get(a, "") and "DUE" in fmap.get(b, ""),
          {k[-1]: ("OVERDUE" if "OVERDUE" in v else "DUE" if "DUE" in v else "none")
           for k, v in fmap.items()})

    # ---------- T-04.5: UV — worklist first in Queues ----------
    uv = sql("SELECT json FROM app_userview WHERE appid='cmbb' AND id='cmbbConsole'")
    u = json.loads(uv)
    queues = [cat for cat in u["categories"]
              if "Queues" in cat["properties"].get("label", "")]
    first_menu = queues[0]["menus"][0]["properties"] if queues else {}
    check("T-04.5 worklist is first Queues menu",
          first_menu.get("datalistId") == "list_worklist",
          first_menu.get("datalistId", "no Queues category"))

    print()
    failed = [n for n, ok, _ in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed" +
          (f"; FAILED: {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
