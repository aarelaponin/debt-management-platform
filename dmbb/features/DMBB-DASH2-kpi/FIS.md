# DMBB-DASH2 — KPI Monitoring dashboard, multi-stream extension, hierarchical view, SAS stub (FIS)

**Status:** Draft (Slice 4 of the v1.1 gap closure)
**Covers:** RPT-FR-003 (Must), RPT-FR-006 (Should, extension), RPT-FR-007 (supporting), RPT-FR-020 (Must, stub)

## 1. Scope

Completes the DMBB dashboard tier (native SqlChartMenu / datalists per ADR-002; the existing
DMBB-DASH shipped 4 multi-stream charts).

- **RPT-FR-003 KPI Monitoring Dashboard** — `list_kpiArrears`: the 12 Arrears Management KPIs
  (PDF-T5 items 24-35), each with current value (computed from the DM data model), target,
  variance %, and a RAG **Status** (GREEN ≥ target / AMBER within 10% / RED beyond, direction-
  aware). Thresholds are configurable in `mdKpiTarget` (seeded with the 12 targets + direction).
- **RPT-FR-006 Multi-Stream extension** — two more SqlChartMenu charts on the Dashboards page:
  **open cases by priority band** (High/Medium/Low from the numeric priority) and **case mix by
  origin** (channel), complementing the existing debt/aging/recovery/compliance charts.
- **RPT-FR-007 Hierarchical views** — `list_debtByOfficer` supplies the **entity-targeted**
  roll-up level between the multi-stream overview and the single-stream debtors list; together
  with the existing category→debtors and instrument→actions drills this realises the 3-level
  hierarchy (multi-stream → entity-targeted → single-stream).
- **RPT-FR-020 SAS VIYA risk scores (STUB)** — `mdRiskSource` config carrier records the SAS
  VIYA REST endpoint/refresh contract; the scheduled consumer + score upsert are **deferred**
  (SAS VIYA not available in the PoC). Risk scores already display on the debtors list and the
  Collection Status report, and absent scores render "-"/N/A with no error (the unavailability
  clause), so the display half is satisfied now; the ingestion half wires at go-live.

## 3. Assumptions / deferred

- **Trend arrow + 12-month sparkline** (RPT-FR-003) require a KPI-history snapshot that accrues
  over time; the current-value + target + variance + RAG structure ships now, and the history
  table/snapshot job is a go-live add (a period accumulation, not a code gap).
- **Revenue-ratio denominators** (KPIs 1-2, "% of revenue") need STA collections, which live
  outside DM; a within-DM proxy is computed now and the true denominator wires to STA at go-live.
- **RPT-FR-020 ingestion** (SAS REST poll + score write) is a deployment wiring; this slice ships
  the config carrier + the already-graceful N/A display.
- KPI RAG is rendered as a datalist **Status** column (native, zero-custom-code — joget-dashboard-gen
  §3 K1). Big-number RAG *cards* (K2 CustomHTML+JSON) are a later cosmetic upgrade.

## Traceability

| Requirement | Artefact(s) | Test |
|---|---|---|
| RPT-FR-003 (KPI Monitoring, 12 KPIs, RAG, config targets) | `list_kpiArrears` + `mdKpiTarget` + seed | run_t43 T-43.1 / T-43.2 |
| RPT-FR-006 (multi-stream: priority, origin) | `chartCasesByPriority`, `chartCaseByOrigin` | run_t43 T-43.3 |
| RPT-FR-007 (hierarchical / entity-targeted) | `list_debtByOfficer` + existing drills | run_t43 T-43.4 |
| RPT-FR-020 (SAS VIYA risk scores — stub) | `mdRiskSource` + seed; graceful N/A display | run_t43 T-43.5 |
