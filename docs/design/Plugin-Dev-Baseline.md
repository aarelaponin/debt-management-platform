# Plugin Development Baseline — reuse survey & decisions

**v1.0 · 12 June 2026 · Sources surveyed:** `gam-bank/_plugins` (production, runs on jdx8 = 9.0.7), `lst-frm-prj/plugins` + `lst-frm-prj/api-builder`, `joget/jw-community` (+ local `~/.m2`), `joget/api-builder`.
**Purpose:** TransitionGuard (CMBB-F02) and every later engine start from these proven assets, not from scratch.

## 1. What exists and what we take

| Asset | Where | Reuse decision |
|---|---|---|
| **Project scaffold** (pom: `packaging bundle`, maven-bundle-plugin 5.1.9, compiler 3.13.0, surefire 3.2.5, explicit `Import-Package` list + `DynamicImport-Package: *`, `Bundle-Activator`) | `gam-bank/_plugins/statement-importer/pom.xml` | **Copy as template.** The Import-Package list is the hard-won part — take verbatim, extend only if a new org.joget package is needed |
| **Activator pattern** (BundleActivator registering plugin classes as services) | `com.fiscaladmin.gam.Activator` | Copy; register CMBB plugins in one bundle |
| **ProcessTool shape** (`DefaultApplicationPlugin.execute()` with steps/loaders/persisters decomposition — SOLID in practice) | `gl-preparator/GlPreparator` | Follow the decomposition; TransitionGuard = guard-steps + event-persister |
| **Status/transition framework** — `Status`, `EntityType`, `canTransition`, `transition(dao,…)`, `InvalidTransitionException`, `TransitionAuditEntry` | TWO variants: `gam-framework/status` (static manager, transitions in code) and `lst joget-status-framework` (interface-based, pluggable) | **Adapt the lst interface shape, but back it with mm_* tables** (gam's in-code transition matrix would violate P4 — CMBB's lifecycles are configuration). The audit-entry concept maps to cmEvent + hash chain |
| **Mockito test pattern** (FormDataDao mocked; end-to-end tests per consolidation/persister) | `statement-importer/src/test` | Copy the test style; TransitionGuard gets guard-matrix unit tests against mocked mm rows |
| **wf-activator** — Post-Processing Tool that auto-starts `{serviceId}_submission` process on form save, passing form data as workflow variables | `lst-frm-prj/plugins/wf-activator` | **Strong candidate for F02 wiring**: case form save → start cmCaseEnvelope automatically (instead of officer manually launching the process). Evaluate at integration; convention-based naming fits our metamodel |
| **Joget dependencies in local m2** | `wflow-core` 5.0.0 / **8.1-SNAPSHOT** / 8.1.7 / **9.0-SNAPSHOT** | Build against **8.1-SNAPSHOT**: proven combination — GAM's 8.1-SNAPSHOT bundles run on Joget 9.0.7 today (form-creator-api-8.1-SNAPSHOT on jdx8). 9.0-SNAPSHOT available if an API gap appears |
| **jw-community source** | `joget/jw-community` | Reference for exact API signatures (FormDataDao, WorkflowManager) — read, never modify |
| **api-builder source** | `joget/api-builder` + `lst-frm-prj/api-builder` (incl. `apibuilder_sample_plugin`) | Reference for custom API endpoints (relevant when the I-9 inbound case API or richer data APIs are built); explains the AppFormAPI behaviour we already use |

## 2. Decisions

1. **Bundle:** one Maven project `cmbb-plugins` (artifact `cmbb-plugins`, package `com.fiscaladmin.mtca.cmbb`), all CMBB engines in one bundle (TransitionGuard now; Allocation/Deadline/etc. join in S2/S3 — mirrors GAM's per-concern bundles but ours are one fabric). Version `8.1-SNAPSHOT` for dependency coherence with the proven stack.
2. **Location:** `cmbb/plugins/cmbb-plugins/` — plugin **source** is authored (not generated), so it does NOT live under `generated/` (deviation from workflow §6 layout, recorded: the never-hand-edit rule applies to generated artefacts; Java source is spec-driven but hand-written).
3. **TransitionGuard internals:** lst-style `Status`/`EntityType` interfaces + an `MmTableTransitionModel` implementation reading mmState/mmTransition/mmDocReq via FormDataDao (cached per request); guard phases per PL-TransitionGuard.md; cmEvent writes through a single `CaseEventWriter` (genesis/prevHash/SHA-256 chain).
4. **Testing:** unit tests with mocked FormDataDao (transition matrix, TTT scope, dedup, hash chain); container test = T-02.x acceptance on jdx9.
5. **Build:** `mvn clean package` per project; JAR upload via Manage Plugins (or `wflow/app_plugins` + restart) — add to deploy runbook at first plugin deployment.

## 3. Time saved (honest estimate)

Scaffold + OSGi headers (the classic week-one swamp): eliminated. Transition framework semantics: ~70% conceptually done, the mm-table backing is the new work. Test harness: pattern ready. Net: TransitionGuard becomes a ~1–2 day implementation instead of ~4–5.
