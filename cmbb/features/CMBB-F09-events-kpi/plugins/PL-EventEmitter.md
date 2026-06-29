# PL-EventEmitter — CMBB Audit-chain verification + KPI emission (CMBB-F09)
Type: DefaultApplicationPlugin (form post-processor). Bundle: cmbb-plugins.
Forcing: WF-FR-020 (full, tamper-evidence), WF-FR-004 / GCMF §3.5 (KPI emission). CAD §2.2 budget row "EventEmitter" (no growth — budgeted from G1). No new XPDL.

## Modes (property `mode`)
| Mode | Trigger form | Action |
|---|---|---|
| VERIFY | cmChainCheck (create) | ChainVerifyService recomputes SHA-256(payload+prevHash) over the case's cmEvent rows ordered by `seq`; checks (a) seq contiguity from 0, (b) each prevHash == prior row hash, (c) recomputed hash == stored hash. caseId set = that case; blank = every distinct caseId. Writes result/checkedCount/firstBadCaseId/firstBadSeq/detail back onto the trigger row; appends **CHAIN_VERIFIED** (clean) or **CHAIN_BROKEN** (with firstBadSeq) to the first checked case. |
| EMIT | (reuse cmChainCheck or cron) | For each CLOSED case with no prior KPI_EMITTED, derive standard dimensions from the chain (caseType, tin, taxType, cycleTimeDays = CLOSED.eventTime − genesis.eventTime, slaBreached = any cmDeadline BREACHED, outcomeCode = latest APPROVED cmDecision actionType else RESOLVED); append **KPI_EMITTED** cmEvent carrying those dimensions as payload (GCMF §3.5 consumer = OutcomeWriteback / Module 10). |
| PROBE | cmGoldProbe (create) | Calls GoldMartClient.fetchProfile(tin); writes enforceableBalance/debtCategory/asaConfidence/asOf/source + result back onto the probe row. Exercises the I-1 shared lib incl. the INT-FR-004 cache path. |

## DAO contract
- updateSchema(cmChainCheck, cmGoldProbe, cmEvent, cmCase, cmDeadline, cmDecision, cmGoldSnapshot) before first read (tx-poison rule, DX9-DELTAS F06).
- Reads: cmEvent (ORDER BY seq), cmCase, cmDeadline, cmDecision; sta_v1 via GoldMartClient.
- Writes: cmChainCheck / cmGoldProbe result fields; cmEvent (CHAIN_VERIFIED / CHAIN_BROKEN / KPI_EMITTED) via CaseEventWriter; cmGoldSnapshot via GoldMartClient.
- VERIFY/EMIT are otherwise read-only over the chain — they never mutate cmEvent (immutability, WF-FR-020).

## Services (constructor-injected) — unit-tested on GuardTestHarness
- ChainVerifyService(dao): verify(caseId) → {ok, firstBadSeq, reason}; verifyAll() → aggregate. Reuses CaseEventWriter.sha256 / payload assembly so a recompute matches the original byte-for-byte.
- GoldMartClient (shared lib, see PL-GoldMartClient): fetchProfile(tin).

## Notes
- The recompute MUST reproduce the exact payload string CaseEventWriter wrote (it reads stored `payload` + `prevHash` verbatim and hashes `payload + prevHash`), so verification is independent of field ordering — it trusts the stored payload, only re-deriving the hash and the linkage. A row whose stored `hash` ≠ sha256(stored payload + stored prevHash), or whose prevHash ≠ prior hash, is the tamper point.
