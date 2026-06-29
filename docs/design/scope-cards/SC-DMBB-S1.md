# SCOPE CARD — DMBB-S1 (debt identification & case creation)
Module: 08 §4.1 + §4.2
FR slice: DM-FR-001…005 (identification, triggers, manual creation, categorisation, history), DM-FR-006…008 (risk/classification — category part)
MoSCoW floor: Must
Archetype target: B (risk-integrated later; categorisation now)
Scope: debt_case as CMBB case type; identification job reading sta_v1.debt_priority_queue/debt_balances (GoldMartClient, I-1); BR-DM-001 triggers (configurable, DPM D1); BR-DM-002 dedup/threshold; C1–C6 assignment (BR-DM-003); case consolidation (BR-DM-004); per-TIN debt history.
Excluded: DM-FR-006/007 SAS risk score consumption (risk_score NULL — D-SAD-07); reminders (S2)
Upstream assumed: CMBB-S1/S2 deployed; mock product (DONE)
Country profile: MLT (DPM profile seed)
DX9 check: inherent
G0: PASS
