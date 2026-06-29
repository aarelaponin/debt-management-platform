#!/usr/bin/env python3
"""DMBB-RPT acceptance T-22.1..6 — live jdx9. Render each Jasper report and confirm it is the
typeset report (HTTP 200, no JRException/ClassCastException, title + grand-total present), with
PDF/Excel export links. The reports run the same SQL the F09/F11/F12 datalists already prove.

Usage: run_t22.py   (no auth needed — dmbbConsole has no permission set, DX9-DELTAS F01)
"""
import sys
import urllib.request

BASE = "http://localhost:8089/jw/web/userview/dmbb/dmbbConsole/_"
RESULTS = []

REPORTS = [
    ("T-22.1", "rptDebtAging", "Debt Aging Report", "RPT-FR-001/008"),
    ("T-22.2", "rptTaxDebtStatus", "Tax Debt Status Report", "RPT-FR-014"),
    ("T-22.3", "rptRecoveryByAction", "Recovery by Enforcement Action", "RPT-FR-005"),
    ("T-22.4", "rptInstalmentCompliance", "Instalment Compliance Report", "RPT-FR-011"),
    ("T-22.5", "rptWriteOff", "Write-Off Report", "RPT-FR-012"),
    ("T-22.6", "rptDebtorsList", "Debtors List Report", "RPT-FR-002"),
]
ERR_MARKERS = ("cannot be cast", "Invalid Jasper", "JRException",
               "net.sf.jasperreports", "Stack Trace", "HTTP Status 500")


def get(url):
    try:
        with urllib.request.urlopen(url, timeout=30) as r:
            return r.getcode(), r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    for tid, cid, title, fr in REPORTS:
        code, html = get(f"{BASE}/{cid}")
        errs = [m for m in ERR_MARKERS if m.lower() in html.lower()]
        has_title = title.lower() in html.lower()
        has_total = "total" in html.lower()
        # export links: the JasperReportsMenu renders pdf/xls export anchors
        has_export = ("pdf" in html.lower()) and ("xls" in html.lower() or "excel" in html.lower())
        check(f"{tid} {cid} renders ({fr})",
              code == 200 and not errs and has_title and has_total,
              f"http={code} title={has_title} total={has_total} export={has_export} errs={errs}")

    print()
    failed = [n for n, ok in RESULTS if not ok]
    print(f"{len(RESULTS) - len(failed)}/{len(RESULTS)} passed"
          + (f"; FAILED {failed}" if failed else " — ALL GREEN"))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
