# FIS — DMBB-F10 — Default-assessment tracking (non-filers → estimate → replace / escalate)
Status: Accepted (T-19.1..5 5/5 PASS on jdx9, 2026-06-13; 119/119 unit; full regression run_t02..t18 green). DMBB slice S5 (F10).
CAD ref: CAD-DMBB §7 row F10 (default-assessment tracking + estimation refs + replacement); DM-FR-054…056, BR-DM-051…053. Builds on F03 (DM case + ProcessStarter), F01 (mdDebtCat), CMBB spine.

## 0. Approach — assessment record + one engine (F06/F07/F09 pattern)
A default assessment is a record about a non-filer's estimated liability, tracked through estimate → (return
filed → replace) or (no filing → escalate to a debt case). Same modelling choice as F06/F07/F09: a **dmDefAssess
record** + one engine **DefaultAssessmentEngine** (16th), three modes wrapping **DefaultAssessmentService**:
1. **ASSESS** (`dmDefAssess` create, DM-FR-054 / BR-DM-051/052) — if the amount is auto, estimate by the
   configured **priority** (BR-DM-051: prior-year return ?? industry average ?? fixed formula), recording the
   method + basis. Apply the **reasonableness check** (BR-DM-052: estimate ≤ priorYear × `reasonablenessMultiplier`,
   default 150%); over the threshold without a DMO justification → `NEEDS_JUSTIFICATION`, else `ASSESSED`. The
   posting to the taxpayer account (debit) is external (SAD I-4) — recorded as an informational dmCharge ASSESSMENT.
2. **REPLACE** (`cmReturnFiled` create, DM-FR-055 / BR-DM-053) — the taxpayer files in response: reverse the
   default (status `REPLACED`), record the filed amount and the variance (filed − estimated), and emit a
   `RISK_PROFILE_UPDATE` event carrying the variance (the SAS risk update is external — DM publishes the signal).
3. **ESCALATE** (`cmDefAssessRun`, DM-FR-056) — no filing after the configurable grace period
   (`mdDefAssessPolicy.nonFilingGraceDays`): create a **DM debt case** from the default-assessment amount
   (cmCase + dmDebt + the F03 ProcessStarter → cmCaseEnvelope) and route it into the standard enforcement
   workflow; the assessment goes `ESCALATED` with the new `debtCaseRef`. asOf time-travel for the grace clock.

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR / BR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-054 | "generation of administrative (default) assessments — either auto-generated using configurable estimation rules or manually created … with: estimated amount, estimation method, basis for estimate … legal reference. Assessment amount subject to configurable reasonableness checks. Posted to taxpayer account as debit." | dmDefAssess + DefaultAssessmentEngine ASSESS (estimate + method + basis + reasonableness + informational debit) | T-19.1, T-19.3 |
| BR-DM-051 | "estimated using (in priority order): (1) prior year return data, (2) industry average … (3) configurable fixed formula. Estimation method is documented with the assessment." | ASSESS estimation priority over mdEstMethod (PRIOR_YEAR → INDUSTRY_AVG → FIXED); method + basis stamped | T-19.1, T-19.3 |
| BR-DM-052 | "must pass a reasonableness check: no more than a configurable multiplier of the prior year amount (default: 150%). Amounts exceeding the threshold require DMO justification." | ASSESS flags estimate > priorYear × reasonablenessMultiplier → NEEDS_JUSTIFICATION unless a justification is supplied | T-19.2 |
| DM-FR-055 / BR-DM-053 | "the submitted return shall replace the default assessment … default assessment reversed → actual return amount posted → difference reconciled → risk profile updated based on variance. Audit trail shows default → actual transition." | cmReturnFiled → REPLACE: status REPLACED, filedAmount + variance recorded, RISK_PROFILE_UPDATE event | T-19.4 |
| DM-FR-056 | "If no filing occurs … create a new enforced collection case on the basis of the default assessment amount and route it to the debt management workflow … debt case auto-created using default assessment balance → enters standard enforcement workflow." | cmDefAssessRun ESCALATE: DM cmCase + dmDebt from the assessment amount, started via ProcessStarter; status ESCALATED + debtCaseRef | T-19.5 |

## 2. Design decisions
1. **A1 — record + engine, reuse the F03 starter.** No new case type for the *assessment*; ESCALATE creates a
   standard DM debt case (the assessment becomes the debt basis) and hands it to the existing envelope — the
   debt then flows through F03→F09 unchanged.
2. **A2 — estimation is config-driven priority (BR-DM-051).** mdEstMethod carries the ordered methods + the fixed
   fallback amount + a representative industry-average amount (DEV); the service walks priority: a supplied
   priorYearAmount wins, else industry average, else fixed. The chosen method + basis are persisted (auditable).
3. **A3 — reasonableness is a soft gate (BR-DM-052).** Over-threshold without justification parks the assessment
   at NEEDS_JUSTIFICATION (not rejected) — a DMO supplies the justification and re-submits; with justification it
   proceeds. Mirrors the "avoid exaggerated estimates" caution without blocking legitimate high estimates.
4. **A4 — postings + risk update are external signals (SAD I-4 / D-SAD-07).** The debit posting (DM-FR-054) and
   the risk-profile update (BR-DM-053) are recorded as a dmCharge + a RISK_PROFILE_UPDATE event; the ledger and
   the SAS risk engine consume them. DM never recomputes balances or risk scores.
5. **A5 — deterministic grace clock.** ESCALATE reads asOf + the assessment's assessedDate vs nonFilingGraceDays,
   so the acceptance test ages an assessment past the grace period without waiting.

## 3. Deferred / assumptions (recorded)
- Real industry-average estimation (by sector + size) — DEV uses a representative `industryAvgAmount` on
  mdEstMethod; the sector/size lookup is an external data feed (recorded).
- Reliability verification of the filed return (DM-FR-055 "subject the filed return to reliability verification") —
  that is the Returns/risk domain; F10 records the variance signal it consumes.
- The actual debit/credit ledger postings + SAS risk recompute are external (SAD I-4 / D-SAD-07).

## 4. Configurables → carriers
- Estimation methods + priority + fixed/industry amounts (BR-DM-051) → `mdEstMethod`.
- Reasonableness multiplier (BR-DM-052) + non-filing grace period (DM-FR-056) + legal ref → `mdDefAssessPolicy`.
- Estimation method / basis / status on each assessment → `dmDefAssess` fields.

## 5. Generation order
1. gen_forms: F-dmDefAssess, F-cmReturnFiled, F-cmDefAssessRun, F-mdEstMethod, F-mdDefAssessPolicy → dmbb/generated/forms.
2. gen_datalists (forms) → companions.
3. gen_userview: ALL dmbb UV-deltas (F01..F10) → dmbbConsole gains a **Default assessments** category + config.
4. Plugin: DefaultAssessmentService + DefaultAssessmentEngine (ASSESS/REPLACE/ESCALATE) + defaultAssessmentEngine.json; Activator 15→16; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb; seed mdEstMethod/mdDefAssessPolicy (MLT) via API-dmbb-data. run_t19 + full regression run_t02..t18.
