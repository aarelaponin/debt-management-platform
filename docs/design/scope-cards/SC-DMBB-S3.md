# SCOPE CARD — DMBB-S3 (instalment agreements)
Module: 08 §4.5
FR slice: DM-FR-021…028 (application→approval→schedule→monitoring→default→cancellation)
MoSCoW floor: Must + Should
Archetype target: B
Scope: instalment_agreement + instalment_payment (SIGTAS INST_ASSESS-validated shape); relief product config (DPM D4) + violation policy (D5); approval via CMBB authority matrix (D6); payment matching from sta_v1.payment_history; interest projection calculator (config-driven, informational — D-SAD-04); agreement document via Mayan.
Excluded: deferral/restructuring/amnesty products (catalogue rows enabled later — DPM-4)
Upstream assumed: DMBB-S1/S2; CMBB-S3
Country profile: MLT
DX9 check: inherent
G0: PASS
