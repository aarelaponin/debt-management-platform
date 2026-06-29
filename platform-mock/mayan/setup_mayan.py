#!/usr/bin/env python3
"""Mayan EDMS reference configuration for the MTCA DM build (P15.6: config as code).

Creates (idempotently):
  - document types  (TA-RDM ref_document_type catalogue + Module 08 DM instruments)
  - metadata types  (TTT spine + registry fields, per Mayan-EDMS.md §10.3)
  - type<->metadata attachments (required/optional)
  - 'Taxpayers' index (TIN -> tax type -> period), per §10.4

Usage:
  MAYAN_URL=http://localhost:8880 MAYAN_USER=admin MAYAN_PASS=... python3 setup_mayan.py
Stdlib only (urllib) - no dependencies.
"""
import json
import os
import sys
import urllib.request

BASE = os.environ.get("MAYAN_URL", "http://localhost:8880").rstrip("/") + "/api/v4"
USER = os.environ.get("MAYAN_USER", "admin")
PASS = os.environ.get("MAYAN_PASS", "")

# --- TA-RDM catalogue (Mayan-EDMS.md §10.2) + DM instruments (Module 08, DM-FR-040) ---
DOCUMENT_TYPES = [
    "TAX_RETURN", "ASSESSMENT_NOTICE", "PAYMENT_RECEIPT", "REFUND_DECISION",
    "AUDIT_REPORT", "COLLECTION_NOTICE", "APPEAL_DECISION", "CORRESPONDENCE",
    "CERTIFICATE", "LICENSE", "REGISTRATION", "APPLICATION",
    "SUPPORTING_DOCUMENT", "INTERNAL_MEMO",
    # DM-specific instruments
    "DEMAND_NOTICE", "INSTALMENT_AGREEMENT", "JUDICIAL_LETTER",
    "GARNISHMENT_ORDER", "WRITE_OFF_DECISION",
]

# Types that carry the full TTT spine (tin + tax_type + tax_period required)
TTT_REQUIRED = {
    "TAX_RETURN", "ASSESSMENT_NOTICE", "PAYMENT_RECEIPT", "REFUND_DECISION",
    "COLLECTION_NOTICE", "DEMAND_NOTICE", "INSTALMENT_AGREEMENT",
    "JUDICIAL_LETTER", "GARNISHMENT_ORDER", "WRITE_OFF_DECISION",
}
# Types that are taxpayer-facing (tin required) but not period-scoped
TIN_ONLY = {"CERTIFICATE", "LICENSE", "REGISTRATION", "APPLICATION",
            "CORRESPONDENCE", "SUPPORTING_DOCUMENT", "AUDIT_REPORT", "APPEAL_DECISION"}

METADATA_TYPES = [
    # name, label, validation regex (or ""), lookup (or "")
    ("tin", "TIN", r"^\d{1,8}[A-Z]?$", ""),
    ("tax_type_code", "Tax type", "",
     "VAT,CIT,PIT,FSS,SSC,CGT,PT,SA,TAS,PAYE,OTHER"),
    ("tax_period_code", "Tax period", r"^\d{4}(-(0[1-9]|1[0-2]|Q[1-4]))?$", ""),
    ("document_number", "Document number", r"^DOC-\d{4}-\d{8}$", ""),
    ("document_direction", "Direction", "", "INBOUND,OUTBOUND,INTERNAL"),
    ("source_reference", "Source reference", "", ""),
    ("case_id", "Case id", "", ""),
]

TOKEN = None


def api(method, path, payload=None):
    url = BASE + path
    data_bytes = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data_bytes, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept", "application/json")
    if TOKEN:
        req.add_header("Authorization", f"Token {TOKEN}")
    import time
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req) as r:
                body = r.read().decode()
                try:
                    return r.status, json.loads(body) if body else {}
                except ValueError:
                    return r.status, {"raw": body[:200]}
        except urllib.error.HTTPError as e:
            body = e.read().decode()
            try:
                data = json.loads(body or "{}")
            except ValueError:
                data = {"raw": body[:200]}
            if e.code == 429:
                time.sleep(1.2)
                req2 = urllib.request.Request(url, data=data_bytes, method=method)
                for k, v in req.header_items():
                    req2.add_header(k, v)
                req = req2
                continue
            return e.code, data
    return 429, {"detail": "throttled after retries"}


def get_all(path):
    items, page = [], BASE and path + "?page_size=100"
    while page:
        status, data = api("GET", page)
        if status != 200:
            return items
        items += data.get("results", [])
        nxt = data.get("next")
        page = nxt.split("/api/v4")[1] if nxt else None
    return items


def main():
    global TOKEN
    if not PASS:
        sys.exit("Set MAYAN_PASS (and optionally MAYAN_URL, MAYAN_USER).")
    status, data = api("POST", "/auth/token/obtain/", {"username": USER, "password": PASS})
    if status != 200:
        sys.exit(f"Auth failed ({status}): {data}")
    TOKEN = data["token"]
    print("Authenticated.")

    # 1. Document types
    existing_dt = {d["label"]: d["id"] for d in get_all("/document_types/")}
    for label in DOCUMENT_TYPES:
        if label in existing_dt:
            print(f"  = document type {label}")
            continue
        status, d = api("POST", "/document_types/", {"label": label})
        if status in (200, 201):
            existing_dt[label] = d["id"]
            print(f"  + document type {label}")
        else:
            print(f"  ! document type {label}: {status} {d}")

    # 2. Metadata types
    existing_mt = {m["name"]: m["id"] for m in get_all("/metadata_types/")}
    for name, label, regex, lookup in METADATA_TYPES:
        if name in existing_mt:
            print(f"  = metadata {name}")
            continue
        payload = {"name": name, "label": label}
        if regex:
            payload["validation"] = "mayan.apps.metadata.validators.RegularExpressionValidator"
            payload["validation_arguments"] = json.dumps({"pattern": regex})
        if lookup:
            payload["lookup"] = ", ".join(lookup.split(","))
        status, m = api("POST", "/metadata_types/", payload)
        if status == 400 and "validation" in m:
            payload.pop("validation", None); payload.pop("validation_arguments", None)
            status, m = api("POST", "/metadata_types/", payload)
            if status in (200, 201):
                print(f"  + metadata {name} (validator dropped - set via UI later)")
        if status in (200, 201):
            existing_mt[name] = m["id"]
            print(f"  + metadata {name}")
        else:
            print(f"  ! metadata {name}: {status} {m}")

    # 3. Attach metadata to document types
    for label, dt_id in existing_dt.items():
        if label not in set(DOCUMENT_TYPES):
            continue
        attached = {a["metadata_type"]["name"]: a for a in
                    get_all(f"/document_types/{dt_id}/metadata_types/")}
        wanted = {"document_number": False, "document_direction": False,
                  "source_reference": False, "case_id": False}
        if label in TTT_REQUIRED:
            wanted.update({"tin": True, "tax_type_code": True, "tax_period_code": False})
        elif label in TIN_ONLY:
            wanted.update({"tin": True})
        for name, required in wanted.items():
            if name in attached or name not in existing_mt:
                continue
            status, r = api("POST", f"/document_types/{dt_id}/metadata_types/",
                            {"metadata_type_id": existing_mt[name], "required": required})
            mark = "+" if status in (200, 201) else "!"
            print(f"  {mark} {label} <- {name}{' (required)' if required else ''}"
                  + ("" if status in (200, 201) else f" {status} {r}"))

    # 4. Taxpayers index (TIN -> tax type -> period)
    index_base = "/index_templates"
    st, probe = api("GET", index_base + "/?page_size=1")
    if st != 200:
        print(f"  ! no index API found ({st}) - create 'Taxpayers' index via UI per Mayan-EDMS.md SS10.4")
    idx = {i["label"]: i for i in get_all(index_base + "/")} if st == 200 else {}
    if "Taxpayers" not in idx:
        status, i = api("POST", index_base + "/", {"label": "Taxpayers", "slug": "taxpayers"})
        if status in (200, 201):
            idx["Taxpayers"] = i
            print("  + index Taxpayers")
        else:
            print(f"  ! index Taxpayers: {status} {i} (create via UI: System > Indexes)")
    if "Taxpayers" in idx:
        index_id = idx["Taxpayers"]["id"]
        # link all taxpayer-facing types to the index (4.11: .../document_types/add/)
        linked = {d["id"] for d in get_all(f"{index_base}/{index_id}/document_types/")}
        for label in (TTT_REQUIRED | TIN_ONLY):
            dt_id = existing_dt.get(label)
            if dt_id and dt_id not in linked:
                api("POST", f"{index_base}/{index_id}/document_types/add/",
                    {"document_type": dt_id})
        # template nodes: root -> tin -> tax_type -> period
        # (4.11: root node id comes from the template detail, not the nodes list)
        nodes = get_all(f"{index_base}/{index_id}/nodes/")
        if len(nodes) < 1:
            _, detail = api("GET", f"{index_base}/{index_id}/")
            root_id = detail.get("index_template_root_node_id")
            exprs = ["{{ document.metadata_value_of.tin }}",
                     "{{ document.metadata_value_of.tax_type_code }}",
                     "{{ document.metadata_value_of.tax_period_code }}"]
            parent = root_id
            for e in exprs:
                status, n = api("POST", f"{index_base}/{index_id}/nodes/",
                                {"expression": e, "parent": parent, "enabled": True,
                                 "link_documents": e == exprs[0] or e == exprs[-1]})
                if status in (200, 201):
                    parent = n["id"]
                    print(f"  + index node {e}")
                else:
                    print(f"  ! index node {e}: {status} {n} (add via UI if needed)")
            api("POST", f"{index_base}/{index_id}/rebuild/")
            print("  > index rebuild triggered")

    print("Done.")


if __name__ == "__main__":
    main()
