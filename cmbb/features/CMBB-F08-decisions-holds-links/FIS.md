# FIS — CMBB-F08 — Decisions & Approvals + Holds + Linkage + Pending-Info Loop
Status: Accepted (T-08.1..8 8/8 PASS on jdx9, 2026-06-12; 62/62 unit; full regression F02–F07 green)
CAD ref: CAD-CMBB §7 row F08 (GCMF-sourced — no direct M08 WF-FR)

## 1. Traceability
F08 carries no M08 WF-FR by design (CAD §7 note). Its forcing requirements are GCMF
capabilities §3.3 (capabilities 5/6/7/8/10), DPM D6/D8, and the bidder enrichment E1/E2/E4.
It also closes three stubs the earlier features parked, pending the decision/hold machinery.

| Source | Requirement (paraphrase) | Realised by | Test |
|---|---|---|---|
| GCMF §3.3-7; DPM D6; WF.010–012 | Decision & approval: threshold→authority matrix, single/supervisor/collegial body, reasoned & (e-)signed decisions | F-cmDecision (postProcessor → DecisionEngine DECIDE): resolves required level from mmAuthority (action × amount band), compares approver level, sets outcome Approved/Rejected, collegial quorum, DECISION_PROPOSED/APPROVED/REJECTED events | T-08.4, T-08.5 |
| GCMF §3.3-8; E1; DPM D8 | Holds tied to case state; suppression scopes CORRESPONDENCE_SUPPRESS / ENFORCEMENT_SUPPRESS; hold↔state consistency | F-cmHold (→ HoldConnector ASSERT) + F-cmHoldRelease (→ RELEASE): cmHold {scope, basis, status}, HOLD_ASSERTED/RELEASED events, targetBB stamp | T-08.1, T-08.3 |
| E1 (dispatcher guard) | NotificationDispatcher must check active CORRESPONDENCE_SUPPRESS holds before dispatch | DispatchService.HOLD_CHECK_ENABLED flipped true + updateSchema(cmHold); active suppression → cmNotif SUPPRESSED, no send | T-08.2 |
| GCMF §3.3-10; E4 | Typed case linkage (referral/escalation/parent-child), navigable both ways, validated against permitted target types | F-cmLink (→ DecisionEngine LINK): validates mmLinkType.targetCaseTypes, writes cmLink + reciprocal, CASE_LINKED events | T-08.6 |
| GCMF §3.3-3; DPM (independence) | EXCLUDE_DECISION_MAKER independence control at (re)allocation | AllocationService.coiExcluded EXCLUDE_DECISION_MAKER now reads cmDecision (same TIN / linked case) + updateSchema(cmDecision) | T-08.7 |
| GCMF §3.3-6; REF-FR-031 | Pending-information loop: request → pending/OnHold → response resumes, no re-initiation | F-cmInfoRequest (→ INFO_REQUEST: OnHold + cmTask + NOTIF_PENDING) + F-cmInfoResponse (→ INFO_RESPONSE: resume + close task) | T-08.8 |
| F05 deferred | ClosePhase closure decision gate (was stub requireDecision=false) | ClosePhase: when requireDecision, require a DECISION_APPROVED for the case (updateSchema(cmDecision)) | T-08.4 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| Authority gate: action × amount band → minimum approver level (mmAuthority) | DecisionService.requiredLevel + rank comparison; reasons mandatory (validator + service) |
| Collegial body needs quorum approvals | DecisionService: bodyType=COLLEGIAL → approvalsCount ≥ quorum before Approved |
| Hold↔state consistency: an ACTIVE hold suppresses the configured scope | HoldService writes status; DispatchService reads CORRESPONDENCE_SUPPRESS; (ENFORCEMENT_SUPPRESS consumed by DMBB enforcement) |
| Link only to permitted target case types | LinkService validates against mmLinkType.targetCaseTypes (blank = any) |
| Decision-maker independence | AllocationService excludes officers who decided the TIN's / linked cases when an EXCLUDE_DECISION_MAKER mmCoi rule applies |
| Pending-info parks the case, never re-initiates | PendingInfoService moves to OnHold envelope + opens cmTask; response resumes prior state |

## 3. Design decisions & assumptions
1. **A1 — two new plugins (budget growth, recorded).** CAD §2.2 anticipated **HoldConnector** (the hold interface — ASSERT/RELEASE). F08 adds a second engine, **DecisionEngine** (modes DECIDE / LINK / INFO_REQUEST / INFO_RESPONSE), because the CAD's "decisions = workflow approval pattern, no plugin" assumed Joget native multi-stage approval, which our **one-envelope / no-new-XPDL** convention (DX9-DELTAS, §5) forbids. A form post-processor engine is therefore the only P3-clean trigger for decision/link/pending-info form actions. Budget grows 9→11; recorded as a CAD amendment. No new XPDL.
2. **A2 — decision authority as ranks.** mmAuthority.level and cmDecision.approverLevel are role names; DecisionService ranks them (OFFICER<SENIOR<SUPERVISOR<MANAGER<DIRECTOR<COMMISSIONER, unknown=0). Required level = the mmAuthority row matching actionType with amountMin ≤ amount ≤ amountMax (blank amountMax = ∞). approverRank ≥ requiredRank ⇒ APPROVED, else REJECTED (insufficient authority). Ranking is a controlled list in code (DEV); deployment may externalise to mdAuthorityLevel later (OPEN).
3. **A3 — collegial bodies (DPM D6).** Single-form-create cannot collect N signatures interactively; v1 reads cmDecision.approvalsCount against mmAuthority.quorum. A true multi-signature ballot UI is deferred (recorded) — sufficient for the gate and the DMBB write-off / instalment-committee cases.
4. **A4 — the closure gate.** ClosePhase enforces requireDecision (TransitionGuard property, set per case type at DMBB). The gate requires a **DECISION_APPROVED** event for the case; authority adequacy is already proven at decision time (A2), so the gate is an existence check (cheap, idempotent). Cases without requireDecision are unaffected — F02–F07 regression unchanged.
5. **A5 — hold scopes & owning BB.** CMBB owns the cmHold register and the suppression scopes it consumes itself (CORRESPONDENCE_SUPPRESS → dispatcher). Financial scopes (COLLECTION/DISBURSEMENT/OFFSET) and ENFORCEMENT_SUPPRESS are **asserted here, consumed by the owning BB** (DMBB enforcement, refunds) via the HoldConnector contract (CAD §4) — the cross-BB write is a DMBB-era integration; F08 records the hold + targetBB and emits the event.
6. **A6 — pending-info pause.** INFO_REQUEST moves the case to its OnHold-envelope state (when the type has one) and opens a cmTask(PROVIDE_INFO); the SLA pause-on-hold already implemented in DeadlineEngine (F05, mmSla.pauseOnHold) handles the clock. INFO_RESPONSE restores the pre-hold state recorded on the request row. No clock logic duplicated.
7. **A7 — linkage reciprocity.** A link writes the forward cmLink and, when the target case is resolvable by caseRef, a reciprocal row (linkType + "_OF" suffix or the configured inverse) so the graph is navigable both ways (GCMF §3.3-10). Cross-BB links to not-yet-existing target cases store the caseRef only (toCaseId blank) — resolved lazily.
8. No platform blockers anticipated; cmDecision/cmHold/cmLink/cmInfoRequest/cmInfoResponse are standard child tables. G2 may pass on spec review.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source |
|---|---|---|---|
| Authority matrix (action × band → level, body, quorum) | mmAuthority rows (F01 form) | seed: CLOSE_CASE, WRITE_OFF bands | GCMF §3.3-7 |
| Hold policies + suppression scopes | mmHoldPolicy rows (F01 form) | seed: TEST CORRESPONDENCE_SUPPRESS | E1 |
| Hold types | mdHoldType rows (F01 form) | seed: DISPUTE, BANKRUPTCY, INFO_PENDING | GCMF |
| Decision types | mdDecisionType rows (F01 form) | seed: CLOSURE, WRITE_OFF | GCMF |
| Link types + permitted targets | mmLinkType rows (F01 form) | seed: PARENT, REFERRAL, CONTINUATION | E4 |
| Authority level ranking | DecisionService (code) | OFFICER..COMMISSIONER | A2 |
| requireDecision per type | TransitionGuard CLOSE phase property | false (set true at DMBB write-off type) | A4 |

## 5. Generation order
1. gen_forms.py: F-cmDecision (DECIDE), F-cmHold (ASSERT), F-cmHoldRelease (RELEASE), F-cmLink (LINK), F-cmInfoRequest (INFO_REQUEST), F-cmInfoResponse (INFO_RESPONSE) — all postProcessor runOn: create.
2. gen_datalists.py: companion list_* for each + DL-list_cmHold (active-first), DL-list_cmDecision (case filter), DL-list_cmLink.
3. gen_userview.py: ALL UV-delta files in feature order (F01..F08) → cmbbConsole gains a **Decisions & Holds** category.
4. Plugin bundle: HoldConnector + DecisionEngine + HoldService/DecisionService/LinkService/PendingInfoService + properties JSON + Activator registration; flip DispatchService HOLD_CHECK_ENABLED, real ClosePhase gate, AllocationService EXCLUDE_DECISION_MAKER; `mvn clean package`; copy JAR.
5. build_jwa.py → deploy_jwa.py (delete→import→publish) → load_md_seed.py (mmAuthority, mmHoldPolicy, mmLinkType, mdHoldType, mdDecisionType).
6. tests/run_t08.py (T-08.1..8) + ALWAYS full regression run_t02..run_t08. Update TRACE.md + DX9-DELTAS for any import finding.
