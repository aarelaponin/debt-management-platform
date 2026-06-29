# FIS — DMBB-F06 — Instalment agreements (relief product)
Status: Accepted (T-15.1..4 4/4 PASS on jdx9, 2026-06-13; 89/89 unit; full regression run_t02..t14 green). DMBB slice S3 (F06) — opens S3.
CAD ref: CAD-DMBB §7 row F06 (instalments); DM-FR-021..028, BR-DM-017..029. Builds on F03 (debt case + ProcessStarter), F05 (enforcement gate ↔ ENFORCEMENT_SUPPRESS hold), CMBB-F08 holds.

## 0. Approach (one new engine)
A single new extension-point plugin **ReliefProductInterpreter** (12th engine, shared cmbb bundle), with two
modes, wrapping a pure **ReliefService**:
1. **APPLY** (`dmInstAgr` create) — eligibility → schedule → approval. Eligibility: debt category in the relief
   product's set (`mdRelief.categories`, MLT = C3–C5), the taxpayer has no other active plan (BR-DM-018), and the
   computed instalment is at/above the product minimum (BR-DM-020). On pass it builds the `dmInstLine` schedule
   (one row/month, due `now+i`) with projected reduced-rate interest (`mdProjRate`, D-SAD-04 informational) and
   either **auto-approves within params** (BR-DM-021: total < `autoThreshold`, duration ≤ `autoMaxMonths`, first
   application → `ACTIVE`) or routes to authority (`UNDER_REVIEW`, BR-DM-022). On `ACTIVE` it asserts an
   **ENFORCEMENT_SUPPRESS** hold on the linked debt case — exactly the hold the F05 enforcement gate honours, so
   enforcement pauses while the plan is live (DM-FR-028).
2. **COMPLIANCE** (`cmInstComplianceRun`) — evaluate each ACTIVE plan's lines vs paid within grace (`mdViolation.graceDays`,
   BR-DM-025); flag `AT_RISK` on any miss; on ≥ `consecutiveMissThreshold` consecutive misses **auto-cancel**
   (BR-DM-027) → release the hold and **open a recovery debt case** for the remaining balance (BR-DM-029) via the
   F03 `ProcessStarter` (cmCaseEnvelope), `triggerOrigin = INSTALMENT_DEFAULT`.

## 1. Traceability
| FR / BR | AC (paraphrase) | Realised by | Test |
|---|---|---|---|
| DM-FR-021 / BR-DM-017 | taxpayer may apply to pay a debt by instalments | F-dmInstAgr (CrudMenu over dmInstAgr) → ReliefProductInterpreter APPLY builds schedule | T-15.1 |
| BR-DM-018 | only one active instalment plan per taxpayer | ReliefService.hasActivePlan(tin) — reject if any ACTIVE/APPROVED/UNDER_REVIEW plan exists | T-15.3 |
| BR-DM-020 | instalment not below product minimum | per-line = (debt+interest)/duration must be ≥ mdRelief.minInstalment, else REJECTED | T-15.unit |
| BR-DM-021 | auto-approve small first-time plans within params | total < autoThreshold AND duration ≤ autoMaxMonths AND first application → ACTIVE + approvalRef=AUTO | T-15.1 |
| DM-FR-028 / BR-DM-024 | enforcement paused while a plan is active | ACTIVE plan asserts ENFORCEMENT_SUPPRESS hold on debtCaseId → F05 gate emits ENFORCEMENT_BLOCKED | T-15.2 |
| BR-DM-025 | a missed instalment is detected after a grace period | COMPLIANCE evaluates lines with dueDate+graceDays < asOf | T-15.4, T-15.unit |
| BR-DM-027 | repeated default cancels the plan | ≥ consecutiveMissThreshold consecutive misses → status CANCELLED | T-15.4 |
| BR-DM-029 | a cancelled plan returns the debt to recovery | releaseHold + createRecoveryCase (new DM cmCase + dmDebt remaining, ProcessStarter) | T-15.4 |

## 2. Design decisions
1. **A1 — IA as a standalone case-linked form, not a subform.** A plan is its own `dmInstAgr` row keyed by
   `debtCaseId` (the cmCase id), not a subform of the debt case — one debt case can carry a history of plans
   (rejected, cancelled, active) and the CrudMenu listing is the officer's worklist. The schedule is a child
   `dmInstLine` grid (foreignKey `instAgrId`).
2. **A2 — the hold is the integration seam with F05.** APPLY asserts / cancel releases the **ENFORCEMENT_SUPPRESS**
   hold (CMBB-F08 cmHold). F05's gate already honours it (`holdActive(caseId,"ENFORCEMENT_SUPPRESS")`), so F06 needs
   no change to F05 — the contract designed forward in F05 A2 is now exercised. T-15.2 is the live proof.
3. **A3 — reduced-rate interest is projected, not posted.** `mdProjRate.reducedRate` (%/month) yields an
   informational `totalInterest` folded into the instalment (D-SAD-04 — the engine never writes back to Gold or
   recomputes the debt; the authoritative balance stays the snapshot). Refinement: a real interest accrual line is
   an enforcement/accounting concern (out of F06 scope).
4. **A4 — recovery via the F03 ProcessStarter.** Default cancellation reuses `JogetProcessStarter` (the F03
   capability) to start a fresh cmCaseEnvelope on the recovery case — no new workflow, the remaining balance becomes
   a new DM case `triggerOrigin = INSTALMENT_DEFAULT`. Idempotent: COMPLIANCE only acts on ACTIVE plans, a CANCELLED
   plan is skipped on re-run.
5. **A5 — deterministic time-travel.** COMPLIANCE reads `asOf` (blank = now) exactly like the F04 sweep, so the
   acceptance test can age a schedule into default without sleeping.

## 3. Generation order
1. gen_forms: F-dmInstAgr (+ dmInstLine child grid), F-dmInstLine, F-cmInstComplianceRun → dmbb/generated/forms.
2. gen_datalists (forms) → list_dmInstAgr, list_dmInstLine, list_cmInstComplianceRun.
3. gen_userview: ALL dmbb UV-deltas (F01..F06) → dmbbConsole gains an **Instalments** category.
4. Plugin: ReliefService + ReliefProductInterpreter (APPLY/COMPLIANCE) + reliefProductInterpreter.json; Activator 11→12; ReliefServiceTest (5 unit). Build cmbb bundle.
5. Deploy cmbb (JAR) + dmbb; seed mdRelief/mdViolation/mdProjRate (MLT) via API-dmbb-data. run_t15 + full regression run_t02..t14.
