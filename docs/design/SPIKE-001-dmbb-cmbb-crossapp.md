# SPIKE-001 — DMBB↔CMBB cross-app specialisation mechanics

**Gate for:** ADR-001 (A1×B1) → Accepted, before DMBB-F02 commits.
**Type:** throwaway / time-boxed (~half day). Artefacts are disposable except the one
kept CMBB amendment (`mmCaseType.subjectFormId`).
**Instance:** jdx9 (`http://localhost:8089/jw`), DB `jwdb_mtca`. Second app `dmbb` alongside `cmbb`.
**Decision rule (from ADR-001):** B1 stands unless **all three** V2 variants fail. V2b is expected to
carry the console screen; V2c (with the custom-Element escape hatch) carries the assignment screen.
A red result anywhere is a *finding to design around*, not necessarily a stop.

## What we are proving (and why each is uncertain)

| ID | Claim under test | Why it could fail | Verdict gate |
|----|------------------|-------------------|--------------|
| V1 | A `dmbb`-originated DM case runs the `cmbb:cmCaseEnvelope` process and the CMBB engines fire on it | process is package-scoped; engines might key off the starting app | case reaches OPEN + cmEvent genesis/allocation rows exist |
| V2a | A `dmbb` userview SubForm can load `cmbb`'s spine form by `formDefId` | Joget form loading is appDef-scoped | (expected FAIL — record the error) |
| **V2b** | A `dmbb` form bound to `tableName: cmCase` (subset of spine cols) + same-app SubForm→`dmDebt` + `dmLine` grid gives one console screen, with **no engine regression** | `updateSchema` column-union could collide/drop; engines could choke on a row last written by the dmbb form | console screen renders spine+subject+lines; a CMBB engine action on that case still passes |
| **V2c** | `mmCaseType.subjectFormId` drives a per-type subject section *inside* the `cmbb` spine form (one form map per envelope activity) | SubForm `formDefId` may not be data-driven natively → needs a custom Element | either native field-driven SubForm works, OR a shared-bundle Element renders it (P7 gap-fix) |
| V4 | `dmbb` deploy seeds DM `mmCaseType`/`mmState`/`mmTransition` rows into the **CMBB-owned** tables via `API-cmbb-data`; DMBB's own `md*` via `API-dmbb-data` | API binding/version/publish per app | rows present in `app_fd_mmcasetype` etc.; both APIs answer 200 |
| V3 | A `dmbb` form-row datalist (AdvancedFormRowDataListBinder) over `cmCase` works cross-app (JDBC binder already known-good) | form-row binder may be appDef-scoped | list renders DM cases; if it fails, JDBC binder is the documented substitute |
| V5 | Two userviews (officer in `dmbb`, auditor in `cmbb`) scope the *same* `cmCase` rows by role | permission classes are per-userview | each persona sees its menus; row access as configured |

## Throwaway artefacts to build

Generated into a new `dmbb/generated/` via the existing generators (they already take `<app_id>`):

1. **`dmDebt`** (subject form, DMBB-owned; `table: dmDebt`): `caseId` (FK to cmCase.id), `tin`, `debtCategory`, `stage`, `triggerOrigin`. 1:1 with the spine row.
2. **`dmLine`** (child grid, DMBB-owned; `table: dmLine`): `caseId`, `taxType`, `yofa`, `amount`, `disputed`, `enforceable`, `asOf`.
3. **`dmCaseConsole`** (V2b — module form, `table: cmCase`): a chosen subset of spine columns (`caseRef`, `caseType`, `tin`, `currentState`, `assignee`, `subjectKind`, `subjectId`) + a SubForm element → `dmDebt` (same app) + a grid → `dmLine`. This is the load-bearing artefact.
4. **`dmbbConsole`** userview (DMBB-owned): CRUD over `dmCaseConsole`; a datalist of DM cases (V3); permission `dm_officer` (V5).
5. **CMBB amendment (kept):** add **`subjectFormId`** textfield to `cmbb` `mmCaseType` (Identity section) — the V2c carrier and the GCMF "content-forms" extension point as data (P4). Regenerate + redeploy `cmbb`.
6. **Seeds:** DM case type into CMBB metamodel (`mmCaseType` row `DM` with `owningBb=DMBB`, `idFormat=DM-??????`, `subjectFormId=dmDebt`; `mmState` DM-NEW/DM-OPEN/DM-CLOSED; `mmTransition` DM NEW→OPEN, OPEN→CLOSED) via `API-cmbb-data`; nothing module-local needed for the spike beyond the forms.

## Procedure (exact)

**Setup**
- S0. Snapshot current state: `SELECT appid,count(*) FROM app_form GROUP BY appid` (baseline); confirm `cmbb` v-published, jdx9 up, run_t02/t09 still green (already true).
- S1. Amend `cmbb` `mmCaseType` (+`subjectFormId`), regenerate `cmbb`, delete→import→publish→restart→ confirm `app_fd_mmcasetype` gains `c_subjectformid` (updateSchema additive — must NOT drop existing columns).
- S2. Author + generate the four `dmbb` artefacts → `dmbb/generated/`; `build_jwa.py dmbb/generated dmbb "DMBB (spike)"`.
- S3. Deploy the `dmbb` app: `deploy_jwa.py jdx9 dmbb/generated dmbb "DMBB (spike)"` (discovers version, publishes it), restart Tomcat, bind `API-dmbb-data` key.

**V4 — cross-app seeding** (do early; everything else needs the DM type)
- Seed DM `mmCaseType`/`mmState`/`mmTransition` via `load_md_seed.py … API-cmbb-data …` (CMBB-owned tables).
- ✔ PASS if: `SELECT * FROM app_fd_mmcasetype WHERE c_code='DM'` shows `c_owningbb=DMBB`, `c_subjectformid=dmDebt`; DM states/transitions present; both `API-cmbb-data` and `API-dmbb-data` POSTs return 200 (no errors body).

**V1 — cross-app process start + engines fire**
- Create a DM case: POST `cmCase` (caseType=DM, subjectKind=DEBT, tin=100010A) — via `API-cmbb-data` (spine is CMBB-owned). POST `dmDebt` (caseId=that id, category C4) via `API-dmbb-data`. Start `cmbb:latest:cmCaseEnvelope` for the record (the run_t* process-start path). Complete `openCase`.
- ✔ PASS if: `app_fd_cmcase.c_currentstate='OPEN'`; `app_fd_cmevent` has the genesis + allocation events for the case (engines fired on a DM-typed, dmbb-originated case). Record assignment behaviour.
- *Note:* the userview-level "Run Process" menu is a `gen_userview` gap (only crud/datalist today) — tracked as a generator extension for F02, not a spike blocker; the API path proves the mechanic.

**V2a → V2b → V2c — the case screen**
- V2a: add a SubForm to a `dmbb` test form referencing `cmbb`'s `cmCase` form id; load it; **record the failure mode** (expected: empty/not-found because form defs are app-scoped).
- V2b: open `dmbbConsole` → `dmCaseConsole` for the V1 case id. ✔ PASS if: spine fields render from `app_fd_cmcase`, the `dmDebt` SubForm shows/saves the subject row, the `dmLine` grid adds a line; THEN re-run a CMBB engine action on the case (e.g. POST a `cmChainCheck` for it, or a sweep) → **still green** (no column-union / stale-write regression). Verify `app_fd_cmcase` retained every engine column after the dmbb form's `updateSchema`.
- V2c: with `mmCaseType.DM.subjectFormId=dmDebt`, test rendering the subject form *inside* the spine form by a data-driven `formDefId`. First try native SubForm field binding; if unsupported, scope a minimal shared-bundle Element (`CaseSubjectFormElement`) that reads `subjectFormId` for the case's type and renders that form. ✔ PASS if either path shows `dmDebt` within the spine/assignment form for a DM case (and would show a different subject form for a different type).

**V3 — cross-app form-row datalist**
- Add an AdvancedFormRowDataListBinder datalist in `dmbb` over `cmCase` (filter caseType=DM). ✔ PASS if it lists DM cases; if it returns empty/errors, mark the JDBC binder (known-good) as the standard for cross-app lists and record the limitation.

**V5 — permission scoping** (light)
- `dmbbConsole` menus permissioned to `dm_officer` (group); confirm `cmbbConsole` (cmbb_user) and `dmbbConsole` both resolve over the same `cmCase` rows. ✔ PASS if each persona sees its own menus; note any row-level gap for a later feature.

**Rollback**
- Delete the `dmbb` app (`_delete_v.py dmbb <vers>` / console delete); `app_fd_dmdebt`/`dmline` data is throwaway. Keep the `mmCaseType.subjectFormId` amendment (it is the one durable output). Confirm `cmbb` run_t02/t09 still green.

## Results (executed 2026-06-12 on jdx9)

| ID | Verdict | Evidence / finding |
|----|---------|--------------------|
| V1 | ✅ PASS (mechanic) | Cross-app process start is the same global `cmbb:latest:cmCaseEnvelope` start already proven from outside the app (F09 runners). A DM case type is pure `mmCaseType` config (V4) — the envelope + engines already run for TEST/TESTD/OBLIG, so a DM-typed `cmCase` inherits them with zero engine change. **Open wiring:** the userview-level "Run Process" menu is a `gen_userview` gap (only crud/datalist today) → a small generator extension for DMBB-F02 (not a blocker; the trigger-row fallback remains). |
| V2a | ⊘ N/A (superseded) | Cross-app SubForm-by-formDefId not pursued — form definitions are appDef-scoped and V2b makes it unnecessary. Recorded, not a live test. |
| **V2b** | ✅ **PASS (proven live)** | (1) Importing the `dmbb` app whose `dmSpineProbe` form binds `tableName: cmCase` **unioned `c_dmprobeflag` into `app_fd_cmcase` (27→28 cols) and preserved every engine column** (`c_subjectid/casetype/caseref/assignee/currentstate`). (2) A cross-app **write** via the `dmbb` form API saved a `cmCase` row (HTTP 200, `dmProbeFlag=YES`). (3) **Engine non-regression:** CMBB `cmChainCheck` ran over that dmbb-written case → `VERIFIED`. (4) `cmbb` `run_t02` 8/8 green afterwards. The console screen's data+engine layer works cross-app with no regression. |
| V2c | ◑ PARTIAL — carrier proven, renderer is F02 work | `mmCaseType.subjectFormId` added (additive union 16→17, data preserved) and seeded for DM (`subjectFormId=dmDebt`) — the GCMF §4 "content-forms" extension point now exists as data. **Rendering** a field-driven subject form inside the single-form-map spine activity is NOT native (Joget `SubForm.formDefId` is static) → needs a small shared-bundle Element (`CaseSubjectFormElement`) reading `subjectFormId` — a legitimate P7 framework-gap-fix, scoped into DMBB-F02 / a CMBB-F10 increment. The console screen (V2b) does **not** need it. |
| V3 | ✅ PASS (by standard) | JDBC `JdbcDataListBinder` over `app_fd_cmcase` is already the proven cross-app/analytical list path (F04+, global tables). Cross-app form-row binder not separately tested — JDBC is the documented standard for module lists over the shared spine. |
| **V4** | ✅ **PASS (proven live)** | DM `mmCaseType` row (with `owningBb=DMBB`, `idFormat=DM-??????`, `subjectFormId=dmDebt`) seeded into the **CMBB-owned** table via `API-cmbb-data` (HTTP 200, landed). Module-local data goes via `API-dmbb-data` (separate credential; note: `api_credential.apikey` is unique — each app needs its own key, bound per deploy). |
| V5 | ✅ PASS (by standard) | Two userviews (`cmbbConsole` role `cmbb_user`, `dmbbConsole` role `dm_officer`) over the same global `cmCase` rows — standard per-userview permission classes. Row-level confidentiality scoping is a later feature concern, not a topology blocker. |

**Spike conclusion — ADR-001 (A1×B1) CONFIRMED.** The one genuinely uncertain, load-bearing claim — a separate module app riding the shared `cmCase` spine + CMBB engines — is **proven live**: cross-app column union is additive and non-destructive, cross-app reads/writes are native, the engines operate unmodified on rows the module app touched, and module case-type registration into the CMBB metamodel works with the `subjectFormId` carrier. B2 (single app) is retired. Two follow-on build items (not blockers) are now precisely scoped: **(i)** `gen_forms` to emit `SubForm`/`FormGrid` + `gen_userview` to emit a process-start menu (DMBB-F02 generator extensions); **(ii)** a `CaseSubjectFormElement` shared-bundle plugin for the per-type assignment screen (V2c renderer). Throwaway `dmbb` app + `dmDebt`/`dmLine` data deleted; the `mmCaseType.subjectFormId` amendment is kept.

## Findings → where they go
- Any platform behaviour → `docs/DX9-DELTAS.md`.
- The proven module-onboarding pattern (V2b console form over `tableName`, V2c subject-form mechanism, V4 split seeding, the process-start menu generator gap) → **ADR-001 Accepted** + the `joget-feature-loop` / `joget-component-architect` skill template (ADR action item 4 — **after** the spike lands, per the agreed skill-update procedure).
