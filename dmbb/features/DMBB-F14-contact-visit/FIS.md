# DMBB-F14 — Contact & Visit tracking + high-value fast-track (FIS)

**Status:** Accepted (run_t41 6/6 on jdx9, 2026-07-01; full cold-start regression t02..t41 GREEN=40)
**Covers:** DM-FR-018 (Must), DM-FR-019 (Should), DM-FR-020 (Should), WF-FR-009 (Must, quick-actions half)

## 1. Scope

Closes the "telephone / visit / fast-track" gap identified in the DM Requirements v1.1
traceability review. Three officer-facing capabilities plus one engine behaviour:

- **DM-FR-019 — telephone-contact tracking.** New `dmContact` form + `list_contacts`
  register: date/time, officer, TIN, person contacted, method (phone/visit/email/SMS/
  letter), outcome code (promise to pay, arrangement agreed, dispute, unable to contact,
  refused, callback), promised amount, agreed follow-up date, notes — linked to the debt
  case via `debtCaseId -> cmCase`. Applicable C3-C5.
- **DM-FR-020 — visit scheduling & recording.** New `dmVisit` form + `list_visits`
  register: scheduled/actual date, field officer, location, purpose, status (SCHEDULED/
  COMPLETED/CANCELLED), persons present, outcome, evidence collected, follow-up actions,
  optional GPS — linked to the case. Applicable C4-C5.
- **DM-FR-018 — high-value fast-track.** At identification, a debt whose `amountAtStake`
  exceeds the configurable high-value threshold is constituted as a **CRITICAL-priority**
  case and given a **TELEPHONE_CONTACT** deadline of `telephoneSlaDays` **business** days
  (default 2). The critical marker is the existing numeric `priority` field (=3, the top
  band; a normal case is unset) — `priorityBand`/`fastTrack` are NOT cmCase form fields so
  `FormDataDao` would drop them on save (DX9-DELTAS 2026-07-01). A `CASE_PRIORITISED` event
  records the trigger. Engine change in `DebtIdentificationService`; no spine form change.
- **WF-FR-009 (quick-actions).** The debtors worklist gains "Call" and "Visit" row
  actions that open `dmContact` / `dmVisit` prefilled from the debtor's open DM case
  (?tin=), alongside the existing Open / Instalment / Payment actions.

## 3. Assumptions / deferred

- Contact/visit records are pure capture this slice (no postProcessor). Writing a
  CONTACT_LOGGED/VISIT_LOGGED cmEvent onto the case timeline is a named follow-up
  (not in scope) — the case link (`debtCaseId`) and the registers already give the
  officer the activity history.
- "Assigned to senior officer" (DM-FR-018) is expressed as the CRITICAL band + telephone
  SLA; case *assignment* remains the AllocationEngine's job (envelope start). The
  `seniorRole` config field records the intended senior group for a later routing rule.
- Full personal-worklist landing (WF-FR-009) already exists (worklist front door,
  task #122); this slice adds only the missing quick-action buttons.

## 4. Configurable carriers

| Marker | Carrier |
|---|---|
| High-value threshold (DM-FR-018) | `mdContactPolicy.highValueThreshold` (EUR), active row |
| Telephone SLA (DM-FR-018) | `mdContactPolicy.telephoneSlaDays` (business days, default 2) |
| Senior-officer group | `mdContactPolicy.seniorRole` (directory group) |
| Contact methods / outcome codes | `dmContact` static SelectBox options |
| Visit purposes / outcomes / status | `dmVisit` static SelectBox options |

## Traceability

| Requirement (verbatim acceptance) | Artefact(s) | Test |
|---|---|---|
| DM-FR-018 — ">high-value threshold -> immediate case creation with 'critical' priority -> SLA: first telephone contact within 2 business days" | `DebtIdentificationService` fast-track; `mdContactPolicy` + seed | run_t41 T-41.6 + `DebtIdentificationServiceTest.fastTracksHighValueDebt` |
| DM-FR-019 — "Contact record created with: timestamp, officer ID, TIN, contact person, contact method, outcome code, notes, follow-up date. Record attached to debt case." | `F-dmContact` + `DL-list_contacts` + UV menu | run_t41 T-41.2 / T-41.4 |
| DM-FR-020 — "Visit scheduled ... visit report completed with: date, location, persons present, outcome, evidence collected, follow-up actions. Report attached to case." | `F-dmVisit` + `DL-list_visits` + UV menu | run_t41 T-41.3 / T-41.4 |
| WF-FR-009 — "Quick-action buttons for common activities (e.g., log call, schedule visit ...)" | `DL-list_debtorsList` Call/Visit row actions | run_t41 T-41.5 |
