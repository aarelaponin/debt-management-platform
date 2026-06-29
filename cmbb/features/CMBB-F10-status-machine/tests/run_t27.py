#!/usr/bin/env python3
"""DMBB-SM foundation acceptance (T-27) — config-driven status state machine (ADR-003).

The StatusManager logic is proven by the JUnit StatusManagerTest. This live suite proves the
CONFIG is correctly DEPLOYED + SEEDED into the Joget mm* tables (the data the dormant service reads):
the lifecycles, terminals, initials and the per-tax (VAT) wholesale shadow are all present and correct.
Read-only over app_fd_* (P3). Engines are NOT yet migrated (foundation increment).
"""
import subprocess
import sys

RESULTS = []


def sql(q):
    r = subprocess.run(["psql", "-h", "localhost", "-U", "joget_mtca", "-d", "jwdb_mtca", "-t", "-A", "-c", q],
                       env={"PGPASSWORD": "joget_mtca", "PATH": "/opt/homebrew/bin:/usr/bin:/bin"},
                       capture_output=True, text=True)
    return ("ERR:" + r.stderr.strip()[:160]) if r.returncode else r.stdout.strip()


def n(q):
    v = sql(q)
    try:
        return int(v)
    except ValueError:
        return -1


def st(entity, scope, frm):
    rows = sql("SELECT c_tostatus FROM app_fd_mmentitytransition WHERE c_entity='%s' AND c_scope='%s'"
               " AND c_fromstatus='%s' ORDER BY c_tostatus" % (entity, scope, frm))
    return set(x for x in rows.split("\n") if x)


def check(name, cond, detail=""):
    RESULTS.append((name, bool(cond)))
    print(("PASS " if cond else "FAIL ") + name + ("  -- " + str(detail) if detail else ""))


def main():
    # carriers deployed?
    tbls = n("SELECT count(*) FROM information_schema.tables WHERE table_name IN "
             "('app_fd_mmentitystate','app_fd_mmentitytransition')")
    check("T-27.0 carriers deployed (mmEntityState + mmEntityTransition tables)", tbls == 2, "tables=%s" % tbls)

    # T-27.1 legal allowed / illegal absent
    legal = n("SELECT count(*) FROM app_fd_mmentitytransition WHERE c_entity='dmWriteOff' AND c_scope='DM'"
              " AND c_fromstatus='SUBMITTED' AND c_tostatus='UNDER_REVIEW'")
    illegal = n("SELECT count(*) FROM app_fd_mmentitytransition WHERE c_entity='dmWriteOff' AND c_scope='DM'"
                " AND c_fromstatus='REJECTED' AND c_tostatus='ACTIVE'")
    check("T-27.1 legal transition seeded, illegal absent (dmWriteOff DM)", legal == 1 and illegal == 0,
          "legal=%s illegal=%s" % (legal, illegal))

    # T-27.2 validNext(dmAction DM INITIATED) — incl. FAILED (garnish-decline path, engine-migrated)
    nx = st("dmAction", "DM", "INITIATED")
    check("T-27.2 validNext(dmAction,DM,INITIATED) = {EXECUTED,SUBMITTED,BLOCKED,REFERRED,FAILED}",
          nx == {"EXECUTED", "SUBMITTED", "BLOCKED", "REFERRED", "FAILED"}, "got=%s" % sorted(nx))

    # T-27.3 terminal has no outgoing
    term = n("SELECT count(*) FROM app_fd_mmentitystate WHERE c_entity='dmWriteOff' AND c_scope='DM'"
             " AND c_code='POSTED' AND c_isterminal='true'")
    outgoing = len(st("dmWriteOff", "DM", "POSTED"))
    check("T-27.3 terminal dmWriteOff/POSTED flagged + no outgoing transition",
          term == 1 and outgoing == 0, "isTerminal=%s outgoing=%s" % (term, outgoing))

    # T-27.4 per-tax wholesale shadow: VAT skips Reminder (real escalation stage names)
    vat = n("SELECT count(*) FROM app_fd_mmentitytransition WHERE c_entity='dmDebt' AND c_scope='VAT'"
            " AND c_fromstatus='Identified' AND c_tostatus='Demand notice'")
    dm = n("SELECT count(*) FROM app_fd_mmentitytransition WHERE c_entity='dmDebt' AND c_scope='DM'"
           " AND c_fromstatus='Identified' AND c_tostatus='Demand notice'")
    dm_rem = n("SELECT count(*) FROM app_fd_mmentitytransition WHERE c_entity='dmDebt' AND c_scope='DM'"
               " AND c_fromstatus='Identified' AND c_tostatus='Reminder'")
    check("T-27.4 per-tax shadow: VAT Identified->Demand notice present; DM goes via Reminder",
          vat == 1 and dm == 0 and dm_rem == 1, "vat=%s dm_direct=%s dm_reminder=%s" % (vat, dm, dm_rem))

    # T-27.5 every entity DM lifecycle has exactly one initial
    bad = sql("SELECT c_entity || ':' || count(*) FROM app_fd_mmentitystate WHERE c_scope='DM'"
              " AND c_isinitial='true' GROUP BY c_entity HAVING count(*) <> 1")
    check("T-27.5 each entity (scope DM) has exactly one initial status", bad == "", "violations=%s" % bad)

    print()
    failed = [x for x, ok in RESULTS if not ok]
    print("%d/%d passed" % (len(RESULTS) - len(failed), len(RESULTS)) + (" — ALL GREEN" if not failed else "; FAILED %s" % failed))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
