# FIS — DMBB-F02 — Debt case type (spine specialisation) + lines + history
Status: Accepted (T-11.1..4 4/4 PASS on jdx9, 2026-06-12; DMBB run_t10 4/4 + full CMBB regression run_t02..t09 green). ADR-001 A1×B1 realised in a working feature: the DM case is a `cmCase` spine row (caseRef DM-000001) extended by the DMBB-owned `dmDebt` subject + `dmLine` child grid, running the full CMBB envelope + engines. No new plugin.
CAD ref: CAD-DMBB §7 row F02 (debt case type + lines + manual creation + history); amended §2.2 (`dmDebt` subject-on-spine).

## 0. Scope & deliberate deferrals (recorded, not dropped)
F02 delivers the **debt case-type structure** and proves it runs the CMBB envelope + engines end to end:
register the `DM` case type into the CMBB metamodel; the `dmDebt` 1:1 subject form (shares the `cmCase` id);
the `dmLine` 1:many child grid; a `dmCaseConsole` composite screen (spine subset + `dmDebt` subform + `dmLine`
grid, via the F02 `gen_forms` SubForm/FormGrid emitters); DM history (cmEvent, already from CMBB).
- **Deferred — `CaseStarter` (auto-create+start the envelope on creation):** the programmatic create-and-start
  is genuinely required by **DMBB-F03 (DebtIdentificationJob, BR-DM-001)** which auto-creates debt cases — so the
  WorkflowManager.processStart capability is built there, not duplicated here. F02 manual creation creates the
  `cmCase`+`dmDebt`+`dmLine`; the envelope is started by the same path all CMBB acceptance uses (process API) —
  run_t11 proves the DM type runs the full lifecycle. (DM-FR-003 "manual creation available" = the console add;
  one-click create-and-start UX rides on F03's starter.)
- **Deferred — `CaseSubjectFormElement` (per-type subject form on the in-workflow activity screen):** the
  composite **console** screen (V2b) needs no custom element; the in-workflow polymorphic screen lands when an
  officer first acts on debt content mid-lifecycle (F04 enforcement). `mmCaseType.subjectFormId=dmDebt` is seeded now.
So **F02 needs no new plugin** — forms + cross-app case-type registration only.

## 1. Traceability
| FR / BR | AC (verbatim from M08 §4.x) | Realised by | Test |
|---|---|---|---|
| DM-FR-003 (manual) | "manual creation shall be available to authorised officers … mandatory fields validation. Duplicate case detection prevents creation of duplicate cases for same taxpayer/tax type/period." | `dmCaseConsole` (CrudMenu add over `tableName: cmCase`, caseType=DM) creating the spine row + `dmDebt` subform + `dmLine` grid; dedup via the existing TransitionGuard (`mmCaseType.dedupPolicy=SKIP`) at openCase | T-11.1, T-11.4 |
| DM-FR-004 (classify) | category bands (configured in F01) | `dmDebt.debtCategory` + `cmCase.category` carry the band; the runtime assignment job is F03 | T-11.2 |
| DM-FR-005 (history) | "case history and audit trail (cross-cutting)" | CMBB cmEvent hash chain runs for the DM case unchanged (genesis + transitions); DL-list_cmEvent already filters | T-11.3 |
| WF-FR-001/003 (lifecycle, config) | the DM case type runs the single `cmCaseEnvelope` with DM `mm_lifecycle` config | DM `mmCaseType`/`mmState`/`mmTransition` seed (envelope states NEW/OPEN/CLOSED; debt *stages* = F04) registered into the **CMBB-owned** metamodel via `API-cmbb-data` | T-11.1, T-11.3 |
| ADR-001 specialisation | a module case = spine row + subject/child tables | `dmDebt` (id = cmCase id, 1:1) + `dmLine` (`caseId` FK, 1:many); CMBB engines operate on the `cmCase` row | T-11.1, T-11.2 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| Duplicate case prevention (DM-FR-003) | `mmCaseType.DM.dedupPolicy=SKIP` → TransitionGuard rejects a second open DM case for the same TIN (proven in F02/F09 dedup) |
| Category carried, not computed here | `dmDebt.debtCategory` is a stored snapshot; runtime classification + re-eval = F03 (GoldMartClient) |
| Subject/line linkage | `dmDebt` shares the case id (SubForm 1:1); `dmLine.caseId` = case id (FormGrid foreignKey) |

## 3. Design decisions & assumptions
1. **A1 — DM stages are F04, envelope states are F02.** F02 seeds the generic envelope states (NEW/OPEN/CLOSED, mirroring TEST) so the DM case can be created → opened → worked → closed and exercise the engines. The debt-specific stages (Identified→Reminder→Demand→…) are `mm_lifecycle` config added by F04 (EscalationEngine). No new XPDL either way.
2. **A2 — explicit seed ids (multi-type rule, DX9-DELTAS F08).** DM state/transition rows carry explicit ids (`DM-NEW`, `DM-OPEN-CLOSED`, …) so they don't collide with TEST's `NEW`/`OPEN`/`CLOSED` (which `load_md_seed` derives from `code`). The DM registration seeds load via `API-cmbb-data` (CMBB owns `mm_case_type`/`mm_state`/`mm_transition`); the `dmDebt`/`dmLine` content forms are dmbb-owned (`API-dmbb-data`).
3. **A3 — one composite console form.** `dmCaseConsole` (over `cmCase`) serves list + add + view: spine subset (caseRef/caseType/tin/taxType/origin/category/amountAtStake/currentState/assignee) + `dmDebt` SubForm + `dmLine` FormGrid. Managed fields (caseRef/currentState/assignee) are engine-set; the officer fills tin/taxType/category/amount + debt detail. caseType is a single-option select pinned to DM.
4. **A4 — no auto-start in F02 (see §0).** Creating via the console writes the `cmCase` row; lifecycle start is the standard process-start path (officer action / F03 job / run_t11 harness). Recorded deferral, not a silent gap.
5. The spike left a harmless `DM` `mmCaseType` row + empty `app_fd_dmdebt` — F02 owns them via API-upsert (never SQL-delete; Hibernate stale-object rule).

## 4. Generation order
1. gen_forms.py: F-dmDebt, F-dmLine, F-dmCaseConsole (uses the new `subform`/`grid` element types) → dmbb/generated/forms.
2. gen_datalists.py (forms) → companions (list_dmDebt, list_dmLine, list_dmCaseConsole over cmCase).
3. gen_userview.py: ALL dmbb UV-delta files (F01 + F02) → `dmbbConsole` gains a **Debt cases** category (CRUD over dmCaseConsole).
4. build_jwa.py dmbb/generated dmbb "DMBB" — no plugin, no workflow.
5. Deploy: cmbb unchanged (no plugin); deploy `dmbb`, restart, seed DM `mmCaseType`/`mmState`/`mmTransition` via `API-cmbb-data` (CMBB-owned) — `dmDebt`/`dmLine`/console are in the dmbb JWA.
6. tests/run_t11.py (T-11.1..4) + full CMBB regression run_t02..t09 + DMBB run_t10.
