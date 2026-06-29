# SCOPE CARD — DMBB-S4 (enforcement actions)
Module: 08 §4.6 + §4.7
FR slice: DM-FR-029…038 (14 action types, visits, garnishment, seizure, outcomes, costs), DM-FR-039…041 (action configuration, templates)
MoSCoW floor: Must + Should
Archetype target: B
Scope: recovery_action entity; instrument catalogue full (DPM D3, enable-flags); proportionality guards (category→instrument); legal fee posting decisions (BR-DM-010; recorded as decisions, posting stays legacy — I-4); agent assignment; outcome writeback (I-2) live from this slice; DMBB-admin slice 2 (instrument + authority config).
Excluded: bankruptcy/insolvency track (DPM D9 — post-MVP policy rows); foreign claims
Upstream assumed: DMBB-S2; CMBB-S2/S3
Country profile: MLT
DX9 check: inherent
G0: PASS
