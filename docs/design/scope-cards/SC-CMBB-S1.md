# SCOPE CARD — CMBB-S1 (case fabric core)
Module: 13 GCMF (outline v0.1) + Module 08 §4.13.1/§4.13.6
FR slice: WF-FR-001…005 (case lifecycle), WF-FR-020 (audit trail); GCMF §3.1 case object model + §3.2 case-type metamodel (mm_*)
MoSCoW floor: Must
Archetype target: B (config-driven case types)
Scope: case spine (case, case_task, case_event, case_note) + mm_case_type/mm_lifecycle MD forms + lifecycle engine + TransitionGuard plugin. MASTER-DATA-FIRST slice.
Excluded: allocation/COI/deadlines (S2), decisions/holds/docs/linkage (S3) — dependency order
Upstream assumed: jdx9 + mock product (DONE); none else
Country profile: MLT (profiles per DPM-5)
DX9 check: inherent (jdx9 IS DX9); record deltas in docs/DX9-DELTAS.md
G0: PASS — no blocker questions; SAD v0.3 approved; GCMF outline is the spec (D-SAD-09)
