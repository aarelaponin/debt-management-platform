# FIS — DMBB-F08 — Enforcement configuration console (action types · templates · consolidation)
Status: Accepted (T-17.1..4 4/4 PASS on jdx9, 2026-06-13; 104/104 unit; full regression run_t02..t16 green). DMBB slice S4 (F08) — closes S4.
CAD ref: CAD-DMBB §7 row F08 (instrument/template/fee admin console); DM-FR-039, 040, 041. Builds on F07 (the config it administers: mdInstrument, mdLegalFee, mdAgentFee, mdPublishRule) + CMBB-F06 (mdTemplate).

## 0. Approach — admin console + two safe-authoring engine modes
F07 already reads all the enforcement config; F08 is the **administration + safety** layer over it (DM-FR-039
"testable before production deployment"). No runtime behaviour changes — F08 adds the curated admin userview
and one new engine **EnforcementConfigEngine** (14th) with two modes wrapping a pure **EnforcementConfigService**:
1. **VALIDATE** (`cmEnfConfigCheck`, DM-FR-039) — the dry-run/consistency gate that makes self-service config
   safe: every *enabled* `mdInstrument` must carry its DM-FR-039 attributes (name+code, debt-size range
   `proportionalityMinAmount`/`maxAmount`, `taxpayerType`, instructions), any `docTemplate` it names must resolve
   to an **active** `mdTemplate`, and any `costRecorded` instrument must have an `mdLegalFee` (specific or default).
   Returns `valid` + `issueCount` + `issues`. Mirrors F05's StrategyAdminService VALIDATE.
2. **PREVIEW** (`cmTemplatePreview`, DM-FR-040 + DM-FR-041) — render an `mdTemplate` against a case's data with
   merge fields populated (taxpayer, case, amount, dates), honouring the **consolidation rule** (DM-FR-041):
   `SEPARATE` (default — one notice per tax type, with each tax type's enforceable sum), `CONSOLIDATED_ALL`
   (one notice, total across tax types), or `CONSOLIDATED_SELECTED`. Writes the rendered subject/body + notice
   count back to the trigger row — the "preview before generation" requirement. Batch generation is recorded as
   deferred (the preview proves the merge + consolidation contract).

DM-FR-039 also asks for the five **action-type attributes**; `mdInstrument` already carries name+code,
debt-size *lower* bound and document template, so F08 **extends** it with the missing three: `maxAmount`
(debt-size upper bound), `taxpayerType`, and `instructions`. F08 also **completes the enforcement-document
template set** (DM-FR-040): it seeds the seven `mdTemplate` rows referenced by enabled instruments but not yet
present (TPL-LIEN, TPL-3P, TPL-DPO, TPL-BANKR, TPL-WARRANT, TPL-JUDICIAL, TPL-INSTALMENT) — so VALIDATE is green.

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-039 | "configuration of standard debt collection action types … each action type assigned attributes: name and code, applicable debt size range, applicable taxpayer type, document template, and specific instructions. … Each action type testable before production deployment." | mdInstrument extended (+maxAmount/taxpayerType/instructions) exposed in the admin console; EnforcementConfigEngine VALIDATE (cmEnfConfigCheck) is the "testable before production" gate | T-17.1, T-17.2 |
| DM-FR-040 | "user-defined templates for demand notes, judicial letters, garnishment orders, and other enforcement documents, with merge fields populated from taxpayer and case data. … Templates versioned. Preview before generation." | mdTemplate admin (versioned, the 7 missing enforcement templates seeded) + EnforcementConfigEngine PREVIEW renders merge fields against case data (cmTemplatePreview) | T-17.3 |
| DM-FR-041 | "Demand notes and legal notices … generated either per tax type separately or consolidated across multiple tax types … Configuration allows: separate notices per tax type (default), consolidated notice across selected tax types, or fully consolidated single notice. Option selectable per enforcement action or per batch operation." | mdNoticeRule (consolidationMode) read by PREVIEW; SEPARATE → one notice per dmLine taxType, CONSOLIDATED_ALL → single notice with total; override selectable on the trigger | T-17.3, T-17.4 |

## 2. Design decisions
1. **A1 — admin layer, no runtime change.** F08 ships a console + VALIDATE/PREVIEW; the F07 engines and the
   live config rows are untouched. CMBB/DMBB regression must stay green (no behavioural delta).
2. **A2 — VALIDATE is the BPM "testable before production" gate (DM-FR-039).** Rather than a Joget BPM-tool
   round-trip, the consistency check is the testability mechanism: it catches a dangling docTemplate, a
   costRecorded instrument with no fee, or a missing attribute *before* an officer runs the instrument. Same
   pattern as F05 strategy validation; reuses the cm*Check trigger-row convention.
3. **A3 — PREVIEW renders, it does not send.** DM-FR-040 "preview before generation": PREVIEW returns the
   merged subject/body + notice count; it never creates a cmNotif or document (that is F04/F07 + Mayan at run
   time). Merge context = case + taxpayer + per-tax-type lines + dates.
4. **A4 — consolidation is data (DM-FR-041).** `mdNoticeRule.consolidationMode` (per caseType/taxType) is the
   default; the trigger may override per-action. SEPARATE iterates distinct `dmLine.taxType`; CONSOLIDATED_ALL
   sums. Legal-compliance of each format is a template-content concern (the templates carry the right wording).
5. **A5 — extend, don't fork mdInstrument.** The three new DM-FR-039 attributes are additive columns
   (`updateSchema` append-only, SPIKE-001) — the F01 seed rows keep working; VALIDATE treats the new attributes
   as recommended (a missing `taxpayerType`/`instructions` is an issue, not a hard error, so the existing MLT
   set validates while flagging the authoring gap).

## 3. Deferred / assumptions (recorded)
- Batch generation (DM-FR-040 "Batch generation supported") — PREVIEW proves the single-case merge + the
  consolidation count; the bulk run is a later job over the same service.
- Template editor UI / WYSIWYG (DM-FR-040) — the mdTemplate CRUD form + merge-field reference is the DEV editor;
  a rich editor is a deployment-time front-end concern.
- Version control of action types / "available in workflow designer" (DM-FR-039) — the app is git-versioned by
  the deploy pipeline; the Joget BPM workflow-designer surface is out of scope (the envelope is the only process).

## 4. Configurables → carriers
- Action-type attributes (DM-FR-039) → `mdInstrument` (extended: maxAmount, taxpayerType, instructions).
- Enforcement document templates (DM-FR-040) → `mdTemplate` (CMBB-F06; F08 seeds the 7 missing).
- Consolidation mode (DM-FR-041) → `mdNoticeRule` (caseType/taxType → SEPARATE/CONSOLIDATED_ALL/CONSOLIDATED_SELECTED).
- Validation scope → EnforcementConfigEngine VALIDATE (cmEnfConfigCheck); preview → PREVIEW (cmTemplatePreview).

## 5. Generation order
1. gen_forms: F-mdInstrument (extended), F-mdNoticeRule, F-cmEnfConfigCheck, F-cmTemplatePreview → dmbb/generated/forms.
2. gen_datalists (forms) → companions.
3. gen_userview: ALL dmbb UV-deltas (F01..F08) → dmbbConsole gains an **Enforcement config** admin category (check + preview + the MD lists).
4. Plugin: EnforcementConfigService (VALIDATE + render/consolidate) + EnforcementConfigEngine + enforcementConfigEngine.json; Activator 13→14; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb; seed the 7 mdTemplate rows (API-cmbb-data) + mdNoticeRule (API-dmbb-data). run_t17 + full regression run_t02..t16.
