# FIS — DMBB-F09 — Write-off (auto-C1 · approved · statutory bulk · status preservation)
Status: Accepted (T-18.1..7 7/7 PASS on jdx9, 2026-06-13; 112/112 unit; full regression run_t02..t17 green). DMBB slice S5 (F09) — opens S5.
CAD ref: CAD-DMBB §7 row F09 (write-off case type + grounds + auto-C1 + statutory bulk + evidence guard); DM-FR-042…046, BR-DM-036…039. Builds on F01 (mdDebtCat C1/C2 bands), F03 (DM case), CMBB-F08 (decisions/holds), F07/F08 (enforcement history the evidence references).

## 0. Approach — write-off as a debt-case outcome record (F06/F07 pattern, not a new case type)
A write-off is an **outcome applied to an existing debt case**, not a case with its own lifecycle (DM-FR-046:
"written-off debts remain in system with status written-off, all case history preserved"). So — exactly as F06
(instalment) and F07 (action) — F09 models it as a **dmWriteOff record** linked to the debt case (`debtCaseId`),
processed by one new engine **WriteOffEngine** (15th). The debt case is closed and its `dmDebt.writeOffStatus`
is stamped; the full cmCase/cmEvent history is preserved (no deletion). This keeps all CMBB engines working over
the same spine row (the CAD's "case type" wording is satisfied by a typed outcome on the DM case — recorded
deviation, consistent with F06 A1).

WriteOffEngine modes:
1. **SUBMIT** (`dmWriteOff` create) — the manual/approved path (DM-FR-043). Evidence guard (BR-DM-039: an
   `APPROVED`-type request must carry enforcement-history summary + evidence ref + rationale, else REJECTED);
   delegation routing (BR-DM-037: approvalLevel by amount — DMO < `dmoMax`, SDO < `sdoMax`, else Director, from
   `mdWoDelegation`); a `C1_AUTO` request needs no approval and posts immediately. Otherwise → `UNDER_REVIEW`.
2. **APPROVE** (`cmWriteOffApprove` create) — an authorised officer's decision (DM-FR-043). On `APPROVE` →
   post the write-off (status `POSTED`, `dmDebt.writeOffStatus = written-off`, close the case, record the
   write-off transaction as an informational `dmCharge` WRITE_OFF + `WRITEOFF_POSTED` event); on `REJECT` →
   `REJECTED`, case returns for alternative action.
3. **SWEEP** (`cmWriteOffRun`, sub-mode, asOf time-travel) —
   - `AUTO_C1` (DM-FR-042 / BR-DM-036): every open DM case with `dmDebt.debtCategory = C1` (<€30) and not yet
     written off → auto-create + post a `C1_AUTO` write-off, "uneconomic to collect", **no human intervention**.
   - `STATUTORY_BULK` (DM-FR-044 / BR-DM-038): debts whose age ≥ `statutoryYears` (from `firstAssessedDate`) and
     collection exhausted → bulk write-off (`STATUTORY` ground), one summary run + per-debt audit entries.
   - `C2_PASSIVE` (DM-FR-045): C2 (€30–100) debts post-automation → `dmDebt.writeOffStatus = passive-collection`
     (monitored for refund interception by STA; DM consumes the outcome); after `statutoryYears` they fall into
     the STATUTORY_BULK sweep.
   Write-off **postings** to taxpayer + revenue accounts are external (SAD I-4) — DMBB records the dmCharge
   hand-off + event; the Write-Off Report (RPT-FR-012) is the `dmWriteOff` datalist grouped by reason/tax/category.

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR / BR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-042 / BR-DM-036 | "Debts in category C1 (<€30) shall be automatically written off … with no manual intervention required. … case closed. Write-off report generated. No human action needed." | WriteOffEngine SWEEP AUTO_C1 → C1_AUTO write-off posted, case closed, dmDebt.writeOffStatus=written-off | T-18.1 |
| DM-FR-043 / BR-DM-037 | "approved write-off … requiring supporting evidence, documented decisions, and approval by authorised officers at appropriate seniority levels based on write-off amount. … Routed to appropriate approver based on amount thresholds." | SUBMIT delegation routing (mdWoDelegation: DMO/SDO/Director) → UNDER_REVIEW; APPROVE posts | T-18.3, T-18.4 |
| BR-DM-039 | "Manual write-off requests must include: enforcement history summary, evidence of inability to collect, decision rationale … Submissions without required evidence are rejected." | SUBMIT evidence guard rejects an APPROVED request missing summary/evidence/rationale | T-18.2 |
| DM-FR-044 / BR-DM-038 | "both individual and bulk write-off operations … all debts older than statutory period … Bulk operation produces summary report and individual audit entries." | SWEEP STATUTORY_BULK over debts with age ≥ statutoryYears → bulk write-off + per-debt events; run summary counts | T-18.5 |
| DM-FR-045 | "Debts in category C2 (€30–100) … retained … for future collection (refund interception) or written off after the statutory collection period expires. … status set to passive collection." | SWEEP C2_PASSIVE sets dmDebt.writeOffStatus=passive-collection; aged C2 then caught by STATUTORY_BULK | T-18.6 |
| DM-FR-046 | "maintain write-off status on debt records, preserving the full history … Written-off debts remain in system with status written-off. All associated case history … preserved." | dmDebt.writeOffStatus + dmWriteOff record; case + cmEvent chain never deleted; reportable via list_dmWriteOff | T-18.1, T-18.7 |

## 2. Design decisions
1. **A1 — outcome record, not a parallel case.** dmWriteOff links to the DM cmCase (debtCaseId); the case closes
   but persists with all history (DM-FR-046). Same modelling choice as F06/F07 — keeps the nine CMBB engines and
   the audit chain intact over one spine row.
2. **A2 — auto vs approved split on type + amount.** C1_AUTO posts with no approval (BR-DM-036). Everything else
   is evidence-guarded (BR-DM-039) and delegation-routed (BR-DM-037); the approver's decision is a second trigger
   (cmWriteOffApprove) so the approval is itself an audited event, not a silent status flip.
3. **A3 — postings are informational (SAD I-4).** The write-off transaction to taxpayer + revenue accounts is
   external; DMBB records a `dmCharge` (chargeType WRITE_OFF) + `WRITEOFF_POSTED` event as the hand-off, and never
   recomputes the balance (D-SAD-01/04). Reversal/reactivation-on-payment (UC-DM-18) recorded as deferred.
4. **A4 — deterministic time-travel for statutory aging.** STATUTORY_BULK/C2_PASSIVE read `asOf` (blank = now) and
   `dmDebt.firstAssessedDate`, so the acceptance test ages a debt past the statutory period without waiting.
5. **A5 — Write-Off Report = datalist (RPT-FR-012).** The list_dmWriteOff datalist (reason/taxType/category/amount/
   date) is the DEV report surface; a pixel-perfect Jasper report is a later RPT feature (recorded).

## 3. Deferred / assumptions (recorded)
- Account-closure hand-off from STA (DM-FR-044, STA-FR-048): dmWriteOff carries a `closureRef` field end-to-end
  and emits the outcome event; the STA closure-case round-trip is the STA engine's (external).
- Refund-interception execution on C2 (DM-FR-045, STA-FR-033): DMBB marks passive-collection and consumes the
  interception outcome; the interception itself is the STA accounting engine's.
- Write-off reversal / reactivation on later payment (UC-DM-18) — the status model supports it; the reversal flow
  is a later feature.
- Pixel-perfect Write-Off Report (RPT-FR-012) — datalist now; Jasper later.

## 4. Configurables → carriers
- C1 auto threshold / C2 band (DM-FR-042/045) → `mdWoPolicy` (c1Max, c2Min, c2Max) — aligned to mdDebtCat (F01).
- Approval delegation (BR-DM-037) → `mdWoDelegation` (dmoMax, sdoMax → Director above).
- Statutory period (BR-DM-038) → `mdWoPolicy.statutoryYears`.
- Write-off grounds + evidence requirement (BR-DM-039) → `mdWoGround` (code, requiresEvidence, requiresApproval).
- Debt write-off status (DM-FR-046) → `dmDebt.writeOffStatus` (extended).

## 5. Generation order
1. gen_forms: F-dmWriteOff, F-cmWriteOffRun, F-cmWriteOffApprove, F-mdWoGround, F-mdWoDelegation, F-mdWoPolicy, and F-dmDebt (extended: +writeOffStatus/writeOffRef/firstAssessedDate) → dmbb/generated/forms.
2. gen_datalists (forms) → companions (list_dmWriteOff is the Write-Off Report surface).
3. gen_userview: ALL dmbb UV-deltas (F01..F09) → dmbbConsole gains a **Write-off** category + **Write-off config**.
4. Plugin: WriteOffService (guard/delegation/auto-C1/statutory/preserve) + WriteOffEngine (SUBMIT/APPROVE/SWEEP) + writeOffEngine.json; Activator 14→15; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb; seed mdWoGround/mdWoDelegation/mdWoPolicy (MLT) via API-dmbb-data. run_t18 + full regression run_t02..t17.
