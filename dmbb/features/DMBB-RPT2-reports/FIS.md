# DMBB-RPT2 — Collection Status + Staff Productivity reports & report configuration (FIS)

**Status:** Draft (Slice 3 of the v1.1 gap closure)
**Covers:** RPT-FR-009 (Must), RPT-FR-013 (Should), RPT-FR-015/016/017 (supporting)

## 1. Scope

Extends the DMBB reporting tier (6 Jasper reports already shipped, DMBB-RPT) with the two
missing predefined reports and the report configuration/scheduling capability.

- **RPT-FR-009 Collection Status Report** — `rptCollectionStatus.jrxml`: one row per active DM
  case with TIN, taxpayer, category, enforcement stage, assigned officer, days since last
  action (from the case's latest `cmEvent`), next scheduled action, risk score and SLA status.
  Overdue next-action rows are highlighted red (BR-RPT-004). PDF/Excel export via the
  JasperReportsMenu. Landscape.
- **RPT-FR-013 Staff Productivity Report** — `rptStaffProductivity.jrxml`: per debt officer —
  cases assigned, cases resolved (terminal state), total recovered (from `dmPayment`), average
  days to resolution, and SLA breaches; with an ALL OFFICERS total row, ranked by recovered.
- **RPT-FR-015/016 Report configuration + save** — `mdReportConfig` form + `list_mdReportConfig`:
  a named, reusable configuration (report + filter set + reporting period + output format) that
  officers save and re-run. The form IS the configuration interface; the row IS the saved config.
- **RPT-FR-017 Scheduled report generation** — `mdReportSchedule` form + `list_mdReportSchedule`:
  binds a saved config to a cadence (frequency/cron) + recipients + format. `lastRunAt` is stamped
  by the scheduler.

## 3. Assumptions / deferred

- The two reports reuse the same open-DM / not-written-off / not-terminal WHERE semantics as the
  datalists and the other Jasper reports, so a report and its list always reconcile.
- **Scheduled EXECUTION and email DELIVERY** (RPT-FR-017) are a deployment wiring, not a code
  artefact: the cadence + recipients are captured in `mdReportSchedule`; the actual scheduled run
  + SMTP send are configured on the platform scheduler/SMTP gateway at deployment (already in the
  open ledger: "postal/SMS/SMTP gateway config at deployment"). This slice ships the config carrier.
- In DEV the case fields `assignee`/`slaStatus`/`nextActionDue` are largely unset (allocation +
  deadline population happen through the live flow), so the reports render with `-` placeholders;
  the report STRUCTURE (columns, totals, highlight rule) is what this slice delivers.

## Traceability

| Requirement | Artefact(s) | Test |
|---|---|---|
| RPT-FR-009 (Collection Status Report) | `rptCollectionStatus.jrxml` + Reports menu | run_t42 T-42.1 |
| RPT-FR-013 (Staff Productivity Report) | `rptStaffProductivity.jrxml` + Reports menu | run_t42 T-42.2 |
| RPT-FR-015/016 (config interface + save) | `mdReportConfig` + `list_mdReportConfig` + menu | run_t42 T-42.3 / T-42.5 |
| RPT-FR-017 (scheduled generation) | `mdReportSchedule` + `list_mdReportSchedule` + menu | run_t42 T-42.4 / T-42.5 |
