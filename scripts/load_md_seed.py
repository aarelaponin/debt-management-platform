#!/usr/bin/env python3
"""Load MD/mm seed CSVs into a deployed Joget app via the API Builder data path.

P3-compliant: rows go through /api/form/{formId}/saveOrUpdate (FormService),
so audit columns and table creation are Joget's own. Idempotent: explicit `id`
per row (code, or composite for child rows) makes saveOrUpdate an upsert.

Usage: load_md_seed.py <base_url> <api_id> <api_key> <seed_dir>
"""
import csv
import json
import os
import sys
import urllib.request


def post(base, api_id, api_key, form_id, payload):
    req = urllib.request.Request(
        f"{base}/api/form/{form_id}/saveOrUpdate",
        data=json.dumps(payload).encode(), method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    req.add_header("api_id", api_id)
    req.add_header("api_key", api_key)
    with urllib.request.urlopen(req) as r:
        return r.status, r.read().decode()[:200]


def row_id(form_id, row):
    if "id" in row and row["id"]:
        return row["id"]  # explicit id wins (e.g. multilingual rows sharing a code)
    if "code" in row and row["code"]:
        return row["code"]
    parts = [row.get(k, "") for k in ("caseType", "fromState", "toState", "stateCode",
                                      "clockCode", "eventType", "holdType") if row.get(k)]
    return "-".join(parts) or None


def main():
    base, api_id, api_key, seed_dir = sys.argv[1:5]
    base = base.rstrip("/")
    total, errs = 0, []
    for fn in sorted(os.listdir(seed_dir)):
        if not fn.endswith(".csv"):
            continue
        form_id = fn[:-4]
        with open(os.path.join(seed_dir, fn)) as f:
            for row in csv.DictReader(f):
                payload = {k: v for k, v in row.items() if v != ""}
                rid = row_id(form_id, row)
                if rid:
                    payload["id"] = rid
                try:
                    status, _ = post(base, api_id, api_key, form_id, payload)
                    total += 1
                except Exception as e:
                    errs.append(f"{form_id}: {row} -> {e}")
        print(f"  {form_id}: done")
    print(f"rows submitted: {total}")
    if errs:
        print("ERRORS:")
        [print("  !", e) for e in errs[:10]]
        sys.exit(1)


if __name__ == "__main__":
    main()
