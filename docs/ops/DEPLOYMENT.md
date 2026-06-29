# MTCA DM — Deployment Process

**v1.0 · 11 June 2026 · Governs:** every Stage-3 → Stage-4 transition (workflow §4–5); executed with the `joget-deploy` skill.

## 1. Principles

1. **Generated artefacts are never hand-edited** (P3/P5). A fix goes to the FIS spec or the generator skill → regenerate → redeploy. Hand edits break regeneration and are treated as corruption.
2. **Every deployment is a batch with a manifest**: what artefacts, from which FIS folder, at which spec version, to which instance.
3. **Pre-flight before import, verification after** — no silent deploys.

## 2. Standard import order (per release slice)

```
1. MD seed data (CSV per md_*/mm_* form)        ← profiles incl. DPM policy seeds
2. Forms              (dependency order: MD forms → independent → FK-dependent → parents with grids)
3. Datalists
4. Userviews
5. Workflow package   (XPDL + the three packageActivity*Map blocks)
6. Plugin JARs        (OSGi bundles via Manage Plugins)
7. Jasper menus/reports
```
(Workflow §5 order; the FIS §5 generation order of each feature mirrors it.)

## 3. Pre-flight (gate G3 → G4 entry)

- Cross-artefact invariants (workflow §4): every referenced `formDefId` exists in batch or instance; FK lookups point at earlier MD forms; all workflow tool/participant mappings resolve; plugin property names match FIS §4 carriers.
- Validation scripts per artefact class (skills' own validators).
- Target instance healthy (ENVIRONMENT.md §4 smoke).
- DB structure precondition for fresh instances: Joget PG structure incl. `shk*` loaded (ENVIRONMENT.md §3.2).

## 4. Verification (gate G4)

1. **UI/process checks** — each FR's acceptance criterion exercised through the userview (tests/acceptance.md).
2. **DB checks** — read-only SQL against `app_fd_*` per `joget-db-inspect`/gam-db-inspect pattern: never trust the screen alone for financial state.
3. **Integration smoke** — `GoldMartClient` reads a known seeded TIN from `sta_v1`; MayanConnector files + retrieves a test document; OutcomeWriteback posts idempotently (repeat POST → no duplicate).
4. **TRACE.md updated** — per-FR status flip; FIS status → Deployed/Accepted.

## 5. DX9 note

jdx9 IS the DX9 target — every deployment exercises the DX9 gate implicitly. Any artefact-class incompatibility found (generator skills are DX8-validated) is recorded in `docs/DX9-DELTAS.md` and fixed in the *skill* (or a post-processing step), never in the generated output (workflow §7). DX8 cross-check, if ever needed for diagnosis: jdx2 (8.1.6).

## 6. Environment promotion

Single DEV instance for now (jdx9). When an INT/demo instance is added: same process, MD seed first, profiles select the country/archetype; promotion = re-running the same batch manifests in order — never copying databases.
