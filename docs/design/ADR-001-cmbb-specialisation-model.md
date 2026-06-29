# ADR-001: How specialising modules consume the CMBB generic case framework

**Status:** **Accepted (A1×B1, 2026-06-12)** — verified by SPIKE-001 (cross-app spine union/read/write + engine non-regression + cross-app metamodel seeding proven live on jdx9; B2 retired). Two non-blocking follow-ons scoped: generator extensions (SubForm/FormGrid/process-start menu) and a `CaseSubjectFormElement` for the per-type assignment screen.
**Date:** 2026-06-12
**Deciders:** Aare (architecture owner); applies to DMBB now and every future case-bearing BB (AMBB, APBB, RFBB, …)
**Supersedes/clarifies:** makes the *mechanics* of D-SAD-03 (two apps) and GCMF §4 (specialisation contract) concrete; reconciles them with CMBB as-built and with CAD-DMBB §2.2.

## Context

CMBB is feature-complete (F01–F09): one `cmCase` spine, one `cmCaseEnvelope` XPDL process, the `mm_*`/`md_*` metamodel, nine JVM-global plugins, and the case children (event, task, deadline, hold, decision, link, note, doc, party). DMBB is the first module to *specialise* it. How it does so is not a folder choice — it is the template for ~11 case-bearing modules and it shapes the eventual ITCAS migration. Three governing inputs already exist and must be honoured (or consciously amended):

1. **GCMF §4 + P7 — specialisation, not siblings.** A module adds case types by (a) registering them in the metamodel, (b) **extending the case via the *subject reference* + child tables**, (c) plugging logic into *named extension points* (transition guards, allocation providers, deadline sources, decision consequences, hold scopes, content forms, link types). "Needing a third mechanism is a framework gap to fix in CMBB — never a workaround in the module" (P7). One physical case table; ~5 shared plugins for the whole suite (GCMF §5).
2. **SAD D-SAD-03 — two apps.** "One DEV instance, two apps (`cmbb`, `dmbb`); `app_fd_*` is instance-scoped, so CMBB's shared plugins and cross-app reads are native (P3); BB isolation by table ownership + interfaces (P14 keeps later separation possible)." SAD §5.3 lists `debt_case` as **DMBB-owned** yet "all CMBB case-type specialisations."
3. **CMBB as-built.** Every engine (TransitionGuard, AllocationEngine, DeadlineEngine, CaseEventWriter, DecisionEngine, Hold/Notification/Mayan/Outcome) keys on **`cmCase.id` + `caseType`**. `cmCase` already carries the typed `subjectKind` + `subjectId` reference (built in F02). The envelope's activity→form maps point at `cmCase`.

**The latent tension to resolve.** CAD-DMBB §2.2 lists `dmCase` as a *main (CMBB case type)* table — readable two ways: (A) `dmCase` is a 1:1 **subject extension** of a `cmCase` spine row, or (B) `dmCase` is a **parallel main** case table. (B) orphans every CMBB engine (none of them read `dmCase`) and contradicts GCMF §5 + P7. The platform facts (global `app_fd_*` tables, JVM-global OSGi plugins) are not in question — **what is undecided is the case-spine model and the cross-app case-screen mechanics.**

This ADR settles two axes: **(1) the case-spine model** and **(2) the app/packaging boundary**, and defines the **spike** that must de-risk the cross-app screen before the DMBB build commits.

## Decision (proposed)

**Axis 1 — Shared spine, subject-table specialisation.** Every case in every module is exactly one `cmCase` row (the spine, CMBB-owned), distinguished by `caseType` (DM, IA, WO, DA, …). `cmCase.subjectKind` = case type family; `cmCase.subjectId` → the module's 1:1 **subject form** (DMBB-owned, e.g. `dmDebt`) that holds module attributes; module **child grids** (`dmLine`, …) hang off the case id. All lifecycle, allocation, deadlines, events, holds, decisions, links run on the `cmCase` row **unchanged**. Module behaviour enters only through the §4 extension points (DM guard pack, allocation criteria provider, deadline source, decision consequence, hold scope) shipped as plugins in the shared bundle. **Consequence: CAD-DMBB §2.2 is amended — `dmCase`→`dmDebt` (subject table on the `cmCase` spine); the engines need zero change.**

**Axis 2 — Uphold the two-app boundary (D-SAD-03), realised as platform-app + module-app.** `cmbb` is the platform app (spine, metamodel, envelope, shared engines, generic console). `dmbb` is a module app that (i) seeds its case-type/lifecycle rows into the CMBB-owned metamodel tables via the CMBB data API, (ii) owns its subject/content forms, config carriers (`md*`/`mm*` DPM), datalists, seeds, and **module userview**, (iii) ships its engines into the *same* JVM plugin bundle, (iv) starts the `cmbb:cmCaseEnvelope` process for new cases. Reads/writes of the shared spine are native (global tables). This is the repeatable template for AMBB/APBB/RFBB/….

**Gate: a thin spike (below) must prove the cross-app case-screen + process-start before DMBB-F02 commits.** If the spike fails on the unified case screen, the fallback is Axis-2′ (single app) — see Trade-offs.

## Options considered

### Axis 1 — case-spine model

#### A1 — Shared `cmCase` spine + module subject/child tables (recommended)
| Dimension | Assessment |
|-----------|------------|
| GCMF/P7 fidelity | Exact — this *is* §4(b) "extend via subject reference + child tables" |
| Engine reuse | 100% — engines already key on `cmCase`+`caseType`; zero change |
| Complexity | Low-Med — needs the subject-form convention + a cross-app case screen |
| Scales to 11 modules | Yes — one spine, one event chain, one KPI semantic (GCMF §3.5) |

**Pros:** every CMBB capability (SLA, COI, holds, decisions, linkage, audit chain, writeback) applies to debt cases for free; one event/KPI stream; cross-module linkage (refund→audit→debt) is native because all links are `cmCase`↔`cmCase`. **Cons:** the per-type case *screen* spans two apps (spine form in `cmbb`, subject form in `dmbb`) — the one real mechanic to verify.

#### A2 — Parallel module case tables (`dmCase` as its own main)
| Dimension | Assessment |
|-----------|------------|
| GCMF/P7 fidelity | Violates "one physical case table" + "specialisations not siblings" |
| Engine reuse | ~0% — every engine must be generalised to be table-parametric (huge, regresses F01–F09) |
| Complexity | High — forks the spine; duplicates event chain per table |
| Scales to 11 modules | Poorly — N case tables, N event chains, N KPI mappings |

**Pros:** crisp per-module table ownership; case screen is single-app. **Cons:** re-opens all nine engines and the 8 green suites; breaks cross-module linkage and the single audit/KPI stream; precisely the "eleven re-implementations" the GCMF exists to prevent. **Rejected.**

### Axis 2 — app/packaging boundary (assumes A1)

#### B1 — Two apps: platform `cmbb` + module `dmbb` (recommended; upholds D-SAD-03)
| Dimension | Assessment |
|-----------|------------|
| Module isolation | Strong — table ownership + userview/persona/permission per app; independent release & versioning |
| ITCAS migration | Cleanest — per-app BPMN export, per-domain cutover (INT-FR-010/011) |
| Tender posture | "One fabric, per-module building blocks" — the GCMF story made visible |
| Complexity | Med — cross-app process start + cross-app case screen must work (spike) |

**Pros:** scales to 11 modules without a monolith; release/regression isolation per module; matches the SAD and P14 ("topology is configuration"). **Cons:** the cross-app case screen (V2) and cross-app seeding split add wiring; needs the spike.

#### B2 — Single app: `dmbb` folded into `cmbb`
| Dimension | Assessment |
|-----------|------------|
| Module isolation | Weak — one app holds every module's forms/userviews/case types |
| ITCAS migration | Harder — no per-domain seam |
| Complexity | Low *now* — case screen, datalists, subforms all same-app |

**Pros:** simplest mechanically — no cross-app anything; the unified case screen is trivial (same-app subform). **Cons:** `cmbb` bloats with all 11 modules; one regression surface; no release isolation; weakens the fabric/specialisation narrative; reverses D-SAD-03. Viable **only** as the fallback if the B1 spike shows cross-app screens are unworkable. **Kept as fallback.**

#### B3 — Separate apps with *no* shared spine (cross-app process invocation per module)
Each module re-declares the envelope and links by reference. Reintroduces A2's spine fork at the process layer. **Rejected** (duplicate XPDL, against no-new-XPDL).

## Trade-off analysis

- **The decision is A1 (settled by governance) × {B1 vs B2} (the genuine call).** A1 is effectively mandated by P7 + GCMF §5 + the as-built engines; choosing A2 would mean rewriting the platform. So the live question is **packaging: two apps (B1) vs one app (B2)** — i.e. *how much module isolation do we buy, against how much cross-app wiring do we pay.*
- **The cost of B1 concentrates in exactly one mechanic: the per-type case screen** (officer sees `cmCase` spine fields + `dmDebt` + `dmLine` on one page, presented from the `dmbb` userview). Everything else in B1 is already proven native: global tables (cross-app data), JVM plugins (cross-app logic), JDBC datalists on `app_fd_cmcase` (cross-app reporting), process start by global id (F09 already starts `cmbb:latest:cmCaseEnvelope` from outside the app).
- **P14 makes B1→B2 reversible but not B2→B1.** Starting two-app keeps the seam; collapsing later is mechanical. Starting one-app and splitting 11 modules later is a migration. This asymmetry favours B1 *if* the spike passes.
- **Linkage and KPI are A1 dividends regardless of B1/B2:** because all cases are `cmCase`, cross-module links and the single event/KPI stream work either way.

## Load-bearing facts — known vs must-verify (the spike)

**Known (no spike):** `app_fd_*` are instance-global (SAD §6; observed on `app_fd_cmcase`); OSGi plugins are JVM-global (one `cmbb-plugins` bundle already serves everything); JDBC datalist binders read any global table; the envelope process is startable by global id from outside the app (F09 `run_t*`).

**Must-verify before DMBB-F02 commits (thin spike on jdx9, mirrors the Mayan spike precedent):**
- **V1 — cross-app process start + ownership:** a `dmbb` userview menu starts `cmbb:cmCaseEnvelope` for a new DM case; the case runs end-to-end and the CMBB engines fire; confirm monitoring/assignment behave when the starting app ≠ the process app. *Named fallback (so V1 can only choose, not fail):* DM case-creation form in `dmbb` + postProcessor that starts the envelope — the proven trigger-row pattern (cmSweepRun/cmDispatchRun precedent; lst `wf-activator` precedent for form-save→process-start).
- **V2 — the unified case screen (the crux), tested as THREE variants in preference order:**
  - **V2a — cross-app SubForm:** `dmbb` userview, SubForm referencing `cmbb`'s spine form by formDefId. Test it, but expect failure — Joget form loading is appDef-scoped.
  - **V2b — module form over the shared spine TABLE (expected winner for the console screen):** `dmbb` defines its OWN case form with `tableName: cmCase` (a chosen subset of spine fields) + same-app SubForm to `dmDebt` + `dmLine` grid. Rationale this is near-certain: `app_fd_*` tables are instance-global, forms bind by tableName, Joget's updateSchema UNIONS columns across form definitions, and every engine reads rows via FormDataDao without caring which form definition wrote them. No cross-app form mechanics at all. Verify: column union is clean, engine behaviour unchanged when a row is edited via the dmbb form.
  - **V2c — the ASSIGNMENT screen (app-boundary-independent — see Review note 1):** the envelope has ONE form map per activity, so per-type screens inside the process need a dynamic mechanism regardless of B1/B2. Test: add **`subjectFormId` to `mmCaseType`** (registers GCMF's "content forms" extension point as data, P4) and render the per-type subject form inside the `cmbb` spine form — via SubForm with field-driven formDefId if Joget supports it, else a small custom Element in the shared bundle (a legitimate P7 "framework gap fixed in CMBB once, all 11 modules benefit").
  Decide where the composite case form is assembled per surface: console (V2b) vs assignment (V2c).
- **V3 — cross-app form-row datalist binding** (not just JDBC) to `cmCase`, if any module list needs it.
- **V4 — cross-app seeding:** `dmbb` deploy seeds DM `mm_case_type`/`mm_state`/`mm_transition` rows into the **CMBB-owned** metamodel via `API-cmbb-data`, while DMBB's own `md*` go via `API-dmbb-data`. Confirm both and the publish/version flow (now that deploy_jwa discovers versions, ADR-aware).
- **V5 — permission/persona scoping** across the two apps for the same underlying case rows (officer in `dmbb` userview vs auditor in `cmbb` console).

Decision rule: B2 only if **all three** V2 variants fail. Since V2b barely can (global tables + tableName binding are proven platform facts) and V2c has the custom-Element escape hatch, B2 is effectively retired as a live option — kept only as the P14-asymmetry fallback. V1 has a named fallback (trigger-row start) and V3/V4/V5 are expected to pass with at most wiring notes.

## Consequences

**Easier:** every future module is a thin "register types + subject forms + config + engines + userview" package on a stable fabric; cross-module linkage, audit, SLA, KPI are free; ITCAS migration has per-app seams; the tender story is demonstrable.
**Harder:** a module's case screen is a two-app composition (B1) — one new convention to establish once, in the spike; seeding is split across two API definitions; regression must run both apps' suites once DMBB has acceptance tests.
**Revisit:** the V2 outcome (where the composite case form lives); whether the `mm_case_type` "owner" is CMBB with module-contributed rows (assumed) or module-replicated; CAD-DMBB §2.2 wording (`dmCase`→`dmDebt`) and CAD-CMBB §4 (add the subject-form extension point explicitly).

## Review notes (2026-06-12, against CMBB as-built)

1. **The "B2 makes the case screen trivial" claim is only half true — and the correction strengthens B1.** The envelope process has ONE form map per activity (openCase/workInProgress/approveClosure → the `cmbb` cmCase form) for ALL case types. Per-type *assignment* screens therefore need a dynamic mechanism (V2c) whether there is one app or two; B2 only simplifies the *console* screen — which V2b solves anyway. Most of B2's advantage evaporates.
2. **A1 is as-built fact, not merely governance.** Every engine hardcodes `F_CASE = "cmCase"` and keys on row id + caseType (dedup, workload counts, SLA clocks, event chain, writeback). A2 would be a rewrite of nine engines and an invalidation of eight green acceptance suites.
3. **Honest cut on B1's "independent release & versioning":** true at the app-artefact level (forms/lists/userviews/seeds per app), but the plugin bundle is shared — an engine change ships JVM-wide to all modules. The mitigation is the existing discipline: regression = ALL apps' suites on every bundle change. State this plainly; isolation claims will be probed.
4. **Ops doubling under B1** (mechanical, but plan it): `dmbb/generated/`, a second `deploy_jwa` target and delete→import→publish cycle, a second API definition + bound credential (`API-dmbb-data`), and the two-app regression loop. Belongs in the `joget-feature-loop` skill template update (action item 4).
5. **`mmCaseType.subjectFormId` is the one CMBB amendment worth pulling into the spike** — it turns the entire specialisation story into configuration (type registration carries its content form), and it is the data carrier V2c needs.

## Action items
1. [ ] **Decide B1 vs B2** (two-app vs one-app) — your call; A1 (shared spine) is taken as governance-settled unless you want to challenge it.
2. [ ] If B1: run the **thin spike (V1–V5, V2 as a/b/c)** on jdx9 as a throwaway `dmbb` app — prove V2b (module form over `tableName: cmCase`) for the console screen and V2c (`mmCaseType.subjectFormId` + dynamic subject section) for the assignment screen, with the V1 trigger-row fallback on standby.
3. [ ] Record the outcome as ADR-001 *Accepted*; amend CAD-DMBB §2.2 (`dmDebt` subject table) and CAD-CMBB §4 (subject-form extension point).
4. [ ] Update `joget-feature-loop` + `joget-component-architect` skills with the chosen module-onboarding template (so AMBB/APBB/… inherit it).
5. [ ] Then proceed to **DMBB-F01-dpm-core-md** (DPM config carriers + MLT seed) under the agreed model.
