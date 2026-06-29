#!/usr/bin/env python3
"""Deterministic synthetic seed for the mock accounting data product (P5/P12: no real data).

Generates CSVs in ./out/ at legacy-equivalent grain (silver_*), so the sta_v1 views
exercise the real ASA mapping path. Same RNG seed -> identical output, always.

Usage: python3 generate_seed.py [--taxpayers 300] [--seed 2026]
"""
import argparse
import csv
import os
import random
from datetime import date, timedelta

TODAY = date(2026, 6, 11)          # frozen reference date (determinism)
TAX_TYPES = ["PIT", "CIT", "VAT", "FSS", "SSC"]
LOCALITIES = ["Valletta", "Birkirkara", "Mosta", "Qormi", "Sliema", "Zabbar",
              "Rabat", "Floriana", "Gzira", "Marsa", "Victoria"]
SURNAMES = ["Borg", "Camilleri", "Vella", "Farrugia", "Zammit", "Galea", "Micallef",
            "Grech", "Attard", "Spiteri", "Azzopardi", "Mifsud", "Bugeja", "Caruana"]
FIRST = ["Joseph", "Maria", "John", "Carmen", "Paul", "Antoinette", "George", "Rita",
         "Charles", "Josephine", "David", "Angela", "Mario", "Doris"]
COMPANY_WORDS = ["Mediterranean", "Malta", "Island", "Harbour", "Crown", "Phoenix",
                 "Atlas", "Falcon", "Luzzu", "Calypso"]
COMPANY_KINDS = ["Trading Ltd", "Holdings Ltd", "Services Ltd", "Imports Ltd",
                 "Construction Ltd", "Hospitality Ltd"]
CHANNELS = ["BANK", "PORTAL", "COUNTER", "DD"]


def tin_for(i, rng):
    return f"{100000 + i * 7 + rng.randint(0, 4)}{rng.choice('MGHABL')}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--taxpayers", type=int, default=300)
    ap.add_argument("--seed", type=int, default=2026)
    args = ap.parse_args()
    rng = random.Random(args.seed)
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")
    os.makedirs(out, exist_ok=True)

    taxpayers, assessments, txns, receipts, objections, suspense = [], [], [], [], [], []
    rcno, aid, oid = 50_000_000, 0, 0

    for i in range(args.taxpayers):
        tin = tin_for(i, rng)
        is_company = rng.random() < 0.35
        name = (f"{rng.choice(COMPANY_WORDS)} {rng.choice(COMPANY_WORDS)} {rng.choice(COMPANY_KINDS)}"
                if is_company else f"{rng.choice(FIRST)} {rng.choice(SURNAMES)}")
        taxpayers.append([tin, "C" if is_company else "I", name,
                          date(rng.randint(1995, 2024), rng.randint(1, 12), rng.randint(1, 28)),
                          "ACTIVE" if rng.random() < 0.95 else "DEREGISTERED",
                          rng.choice(LOCALITIES),
                          f"{tin.lower()}@example.mt", f"+356 21{rng.randint(100000, 999999)}"])

        # debtor profile: ~55% clean, rest spread across C1..C6 (DM-FR-004 bands)
        profile = rng.choices(["clean", "C1", "C2", "C3", "C4", "C5", "C6"],
                              weights=[55, 6, 8, 12, 12, 5, 2])[0]
        types = rng.sample(TAX_TYPES, k=(rng.randint(2, 4) if is_company else rng.randint(1, 2)))
        if is_company and "PIT" in types:
            types[types.index("PIT")] = "CIT"

        for tt in types:
            for yofa in range(rng.randint(2020, 2023), 2026):
                base = {"C1": (5, 28), "C2": (20, 90), "C3": (80, 900), "C4": (800, 18000),
                        "C5": (8000, 60000), "C6": (60000, 250000)}.get(profile, (100, 8000))
                liab = round(rng.uniform(*base) / max(1, 2026 - yofa), 2)
                if liab < 1:
                    continue
                kind = rng.choices(["SELF", "AUTHORITY", "DEFAULT"], weights=[80, 12, 8])[0]
                due = date(yofa, 6, 30) if tt in ("PIT", "CIT") else date(yofa, rng.choice([3, 6, 9, 12]), 15)
                aid += 1
                assessments.append([f"A{aid:08d}", tin, tt, yofa, str(yofa), kind,
                                    1 if kind == "DEFAULT" and rng.random() < 0.5 else 0,
                                    liab, due, f"{due - timedelta(days=30)} 09:00:00"])
                txns.append([tin, tt, yofa, str(yofa),
                             230 if kind == "DEFAULT" else 10, "",
                             liab, due - timedelta(days=30), f"A{aid:08d}"])
                # penalties/interest on overdue debtors
                if profile != "clean" and rng.random() < 0.7:
                    pen = round(liab * rng.uniform(0.05, 0.15), 2)
                    txns.append([tin, tt, yofa, str(yofa), 70, "", pen,
                                 due + timedelta(days=40), "PEN"])
                if profile != "clean" and rng.random() < 0.8:
                    intr = round(liab * 0.006 * max(1, (TODAY - due).days // 30), 2)
                    if intr > 0.5:
                        txns.append([tin, tt, yofa, str(yofa), 60, "", intr,
                                     date(TODAY.year, TODAY.month, 1), "RST"])
                # payments: clean pay fully; debtors partially by age
                pay_share = 1.0 if profile == "clean" else rng.uniform(0.0, 0.75)
                if yofa >= 2025 and profile != "clean":
                    pay_share = min(pay_share, 0.4)
                paid = round(liab * pay_share, 2)
                if paid > 0.5:
                    rcno += 1
                    pdate = due + timedelta(days=rng.randint(-20, 60))
                    receipts.append([rcno, pdate, rng.choices([0, 8], weights=[97, 3])[0],
                                     tin, tt if tt != "PIT" else "SA", yofa, paid,
                                     rng.choice(CHANNELS)])
                    txns.append([tin, tt, yofa, str(yofa), 20, "G", -paid, pdate, f"R{rcno}"])

        # disputes on ~8% of debtors (BR-DM-008 exclusion path)
        if profile not in ("clean", "C1") and rng.random() < 0.18:
            oid += 1
            tt = rng.choice(types)
            objections.append([f"O{oid:06d}", tin, tt, rng.randint(2021, 2024),
                               round(rng.uniform(50, 5000), 2),
                               rng.choices(["OPEN", "REJECTED", "UPHELD"], weights=[60, 25, 15])[0],
                               TODAY - timedelta(days=rng.randint(10, 400))])

        # credits/suspense sprinkle
        if rng.random() < 0.06:
            suspense.append([f"S{i:06d}", tin if rng.random() < 0.6 else "",
                             rng.choice(["CREDIT", "UNIDENTIFIED", "SUSPENDED"]),
                             round(rng.uniform(20, 2000), 2),
                             TODAY - timedelta(days=rng.randint(5, 300)), f"REF{i}"])

    files = {
        "silver_taxpayer.csv": (["tin", "tpform", "name", "reg_date", "status", "locality",
                                 "email", "phone"], taxpayers),
        "silver_assessments.csv": (["assessment_id", "tin", "tax_type", "yofa", "period", "kind",
                                    "caution_flag", "amount", "due_date", "assessed_at"], assessments),
        "silver_transactions.csv": (["tin", "tax_type", "yofa", "period", "trcd", "sbcd",
                                     "amount", "txn_date", "source_ref"], txns),
        "silver_receipts.csv": (["rcno", "rcdt", "stus", "tin", "ptyp", "yerr", "amount",
                                 "channel"], receipts),
        "silver_objections.csv": (["objection_id", "tin", "tax_type", "yofa", "amount",
                                   "status", "filed_date"], objections),
        "silver_suspense.csv": (["entry_id", "tin", "kind", "amount", "received_date",
                                 "reference"], suspense),
    }
    for fn, (hdr, rows) in files.items():
        with open(os.path.join(out, fn), "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(hdr)
            w.writerows(rows)
        print(f"  {fn}: {len(rows)} rows")
    print("Seed complete (deterministic, seed=%d)." % args.seed)


if __name__ == "__main__":
    main()
