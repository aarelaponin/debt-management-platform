# Plugin Requirement — DeadlineEngine (Output D format)

**Type:** ProcessTool (MultiTools chains) + PluginWebSupport (scheduled sweep endpoint)
**Plugin ID / FQCN:** `com.fiscaladmin.mtca.cmbb.DeadlineEngine` (bundle cmbb-plugins)
**Forcing FR:** WF-FR-010/011/012; CAD case_deadline state machine + plugin budget row.

**Modes (property `mode`):**

## START (tool #3 in guardOpen)
For each active mmSla row of the case type: create cmDeadline
{caseId, clockCode, startedAt=now, dueAt=start+durationDays per calendar
(mmCalendar WORKING skips Sat/Sun + holidays CSV), warnAt/critAt=linear pct of
span, status=RUNNING, escalationLevel=0}. Set cmCase.slaStatus=GREEN. Idempotent
(skip clocks that already exist for caseId+clockCode — re-open keeps history,
new clocks only when none open).

## CLOSE (tool #2 in guardFinal)
Open clocks (RUNNING/PAUSED) of the case → MET. slaStatus stays as-is
(breached cases persist red until resolved — WF-FR-011 — clock already BREACHED
stays BREACHED for the record; case slaStatus recomputed = RED history retained
on events, current status cleared to '-' on closure).

## SWEEP (web service `/web/json/plugin/<FQCN>/service`, admin-only; param asOf optional ISO for tests)
Per open clock:
1. Case terminal → MET.
2. pauseOnHold && envelope(currentState)==OnHold → PAUSED (pausedAt=now);
   PAUSED && !onHold → resume: dueAt/warnAt/critAt += paused duration,
   pausedDays accumulate, RUNNING. Events SLA_PAUSED / SLA_RESUMED.
3. RUNNING: asOf≥critAt && level<1 → escalate L1 (SLA_ESCALATED + NOTIF_PENDING
   to assignee & supervisor {caseRef, taxpayer, amount, slaStatus, daysOverdue};
   priority bump if escalationChain.bumpPriority; optional reassignTo).
   asOf≥dueAt && level<2 → BREACHED + escalate L2 (SLA_BREACHED + notices).
   Levels capped at escalationChain.maxLevels (default 3). Escalations are
   idempotent via escalationLevel (CAD: fired exactly once).
4. Case slaStatus = worst open clock: BREACHED/≥crit→RED, ≥warn→AMBER, else GREEN.

**FormDataDao reads:** mmSla, mmCalendar, mmState, cmCase, cmDeadline
**FormDataDao writes:** cmDeadline, cmCase (slaStatus, priority), cmEvent (via CaseEventWriter)
**Properties:** mode, caseId (#variable.caseId#)
**Security:** web service requires authenticated admin (DEV; production = scheduler service account, runbook item)
**Complexity:** H (CAD)
**Unit tests:** business-day math (weekend/holiday skip), threshold computation, sweep transitions incl. pause/resume arithmetic, escalation idempotency.
