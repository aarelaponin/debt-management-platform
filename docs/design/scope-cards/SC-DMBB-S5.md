# SCOPE CARD — DMBB-S5 (write-off, MI, debtors list)
Module: 08 §4.8–§4.11 + §4.12 subset
FR slice: DM-FR-042…046 (write-off), DM-FR-047…053 (collection planning/MI), DM-FR-054…056 (default assessment integration), DM-FR-057…058 (debtors list); RPT-FR-001…007 dashboards subset + RPT-FR-008…014 operational reports subset
MoSCoW floor: Must (Should for RPT subset)
Archetype target: B
Scope: write_off as CMBB case type with grounds catalogue (DPM D7, auto low-value per BR); collection MI datalists + Jasper reports (heavy analytics stays Superset, P2); default-assessment caution handling (reads assessment_register.kind/caution); debtors_list with publication policy (DPM D10); ②/③ reconciliation check (nightly).
Excluded: RPT-FR-015…021 (report config mgmt + analytics — post-MVP); KPI semantic layer (Module 10)
Upstream assumed: DMBB-S1…S4
Country profile: MLT
DX9 check: inherent
G0: PASS
