# MTCA Debt Management — Architectural Principles

**Version 0.1 Draft | June 2026 | Author:** Aare Lapõnin
**Lineage:** Adapted from *ARMS Architectural Principles v0.4* (April 2026). The ARMS document governs a custom-built data platform (Python/Spring Boot/React); this adaptation governs a **low-code delivery on Joget DX** with Mayan EDMS as the document store and the MTCA Data Platform (Blueprint v1.0) as the analytical substrate. Principles that survived unchanged are restated briefly; principles that changed carry the reasoning; ARMS-only principles (ratio-scale measurement, React state discipline) are dropped or replaced.

**Scope:** the CMBB / TABB / DMBB building blocks, the platform mock, the Mayan integration, all custom plugins, and all tooling in `15-ITCAS/03_debt_management/`. Governing process remains `docs/JOGET-DELIVERY-WORKFLOW.md`.

**How to use:** every "should we do X or Y?" must be resolvable by reference to these principles. On conflict, lower number wins. P3 is the principle this adaptation exists for — when in doubt, read P3 first.

---

## Architectural Principles

### P1. Business Value Before Technical Perfection *[validation phase]*

Unchanged from ARMS. The engagement context sharpens it: this build is the IMF R9 quick win (debt-management automation, 8–10 FTE-equivalent from workflow automation per Blueprint §14.3), and its credibility depends on demonstrable working slices, not on platform elegance.

**Implications:** deliver vertical slices a debt officer can click through; accept documented debt that doesn't block validation; "good enough to prove the concept" is the Phase-1 bar. The mock Gold platform exists precisely so value delivery is not hostage to the real platform's timeline.

**Anti-patterns:** building the full case-type metamodel before one debt case runs end-to-end; polishing Superset-grade dashboards inside Joget when a datalist answers the question.

### P2. Technology Follows Workload

The ARMS principle, with the workload table rewritten for this stack. Do not force one technology across workloads — and equally, do not import a technology for a workload the platform already serves.

| Workload | Technology | Rationale |
| --- | --- | --- |
| Case management, forms, worklists, navigation, approvals | **Joget DX configuration** (forms, datalists, userviews, XPDL workflow) | This is what the CMP *is*. TARA Domain 4: "configuration, not code." |
| Cross-form logic, calculations, integrations, scheduled jobs | **Joget plugins** (Java/OSGi, typed per joget-plugin-dev) | The platform's official extension mechanism — and the only one (P3). |
| Pixel-perfect documents (notices, statements, decisions) | **JasperReports** via JasperReportsMenu | Joget Enterprise's recommended reporting path. |
| Document storage, versioning, OCR, retention | **Mayan EDMS** via REST API v4 | Purpose-built DMS; do not rebuild it in Joget (P3, P13). |
| Analytical data (STA balances, debt marts, KPIs) | **Data platform Gold layer** (ClickHouse + dbt; mocked per Blueprint contract) | Joget consumes; it does not re-aggregate nine databases. |
| Spec tooling, generators, seed/validation scripts | **Python** + the joget-* generation skills | The delivery pipeline's ecosystem. |

**Boundary rule:** a service at a boundary belongs to the workload it primarily serves. The outcome-writeback plugin is a Joget plugin (it fires on Joget workflow events) even though it writes to ClickHouse; the seed-data generator is Python even though it feeds Joget tables.

### P3. Work *With* the Platform, Never Against It  ⟵ the load-bearing principle

We chose Joget; therefore we build the way Joget is designed to be built. The platform's conventions, APIs, and extension points are the contract. Anything that bypasses them — however expedient — creates an artefact the platform cannot manage, upgrade, or migrate, and is a defect even when it works.

**The mechanism ladder.** For any requirement, mechanisms are tried in this order, and a step down requires a recorded justification (the CAD plugin budget is that record):

1. **Standard configuration** — form elements, validators, load/store binders, datalist columns and formatters, userview menus and permissions, workflow routes, deadlines and participants.
2. **Standard plugins and platform facilities** — bundled plugins, environment variables, hash variables, BeanShell *only* for thin glue (a few lines, no business rules).
3. **Custom plugin of an official type** — ApplicationPlugin, ProcessTool, FormLoadBinder/FormStoreBinder, Validator, ParticipantPlugin, DatalistBinder, scheduler plugin… built per joget-plugin-dev, named, typed, FR-justified, complexity-rated.
4. **There is no step 4.** A need that no plugin type can serve is an architecture failure to be escalated (wrong component boundary, or a GCMF gap), not hacked around.

**The data-access rule (the user's phrasing is the norm):** *normal logic always uses the Joget API; raw SQL only within Joget's own recommendations.*

- All business reads/writes of form data go through **FormDataDao / FormService / AppService / WorkflowManager**. This keeps the platform's column mangling, audit columns (`dateCreated`, `dateModified`, `createdBy`…), grid parent-child wiring, and form events intact.
- **Raw SQL is legitimate exactly where Joget itself points at SQL:** `JdbcDataListBinder` for reporting datalists and joins the form binder cannot express; JasperReports datasource queries; database-level *read-only* verification in acceptance tests (the gam-db-inspect pattern: never trust the screen alone for financial state). These are consumption paths, not write paths.
- **Writes to `app_fd_*` tables outside the Joget API are forbidden. No exceptions.** A row inserted by hand-rolled JDBC is invisible to form events, audit columns, and cache — it is corruption with a delay.
- Workflow state changes go through the workflow engine (assignment completion, process variables), never by updating state columns directly.
- External stores are not Joget: the Gold mart is read via its REST/JDBC contract, Mayan via its REST API — the rule governs *Joget's* data, not the integrations'.

**Further with-the-grain rules:**

- Generated artefacts (forms, datalists, userviews, XPDL, JRXML) are **never hand-edited** — fixes go to the spec or the generator skill, then regenerate (workflow §7 owns this).
- No modification of Joget core JARs, platform database tables (`wf_*`, `app_*` system tables), or theme internals; styling stays within the userview theme contract.
- Upgrades must survive us: anything we add is an app-level artefact or an OSGi bundle, removable without trace.
- ID/naming conventions follow the suite: camelCase form ids matching table names, `md*`/`mm_*` master-data forms with `list_*` companions, code-as-key case ids (`DM-……`).

**Anti-patterns this prevents:** a "quick" JDBC `INSERT INTO app_fd_debtCase` in a ProcessTool; a 300-line BeanShell block implementing the escalation rules (that is a plugin in the wrong carrier — and a tables-in-code P4 violation when it embeds thresholds); a cron job outside Joget mutating case states; editing generated form JSON by hand "just to fix a label"; a datalist built on a hand-written view that duplicates what the form binder provides.

### P4. Configuration Over Code

ARMS P4, amplified: on a low-code platform this principle is the product. System behaviour is controlled by configuration — MD forms, the case-type metamodel, environment variables, plugin properties — not code changes.

**The tables-in-configuration rule (non-negotiable, no validation-phase exception):** tables of any kind — escalation step chains, risk-class thresholds, payment-allocation priority orders, interest/penalty rates, notice templates per escalation step, enforcement-action catalogues, code-to-label lists — live in MD/config forms (`md*`, `mm_*`) or named plugin properties, never as Java constants, BeanShell literals, or SQL CASE ladders. The `[Configurable]` markers in the FR catalogues are this rule's inventory: every one surfaces in a FIS §4 as a parameter with a named carrier.

**The line:** configuration is "what" (which steps, which thresholds, which templates); plugin code is "how" (the engine that walks the steps). New "what" must never require a deployment.

**Anti-pattern:** `if (daysOverdue > 30) sendReminder()` with `30` in the code — the escalation calendar is MTCA's policy, not our constant.

### P5. Deterministic Generation

ARMS P5, transposed from dbt models to Joget artefacts. The spec-to-artefact pipeline (FIS YAML → joget-form-gen / datalist-gen / userview-gen / workflow-gen / jasper-report) must be deterministic: the same spec always yields the same artefact. No LLM-improvised JSON at generation time — judgement is spent in Stages 1–2 (CAD, FIS), generation is mechanical (workflow Stage 3).

**Implications:** every generated artefact traces to its spec file; specs and generated outputs are both version-controlled; regeneration is always safe because hand-edits are banned (P3); the DX9 compatibility gate fixes deltas in the *skill*, never in the output.

### P6. Transparency and Auditability

Unchanged in intent; the carriers change. Every case action, financial posting, and configuration change must be traceable — this is a tax administration handling enforcement against taxpayers.

**Implications:** the immutable `case_event` log is the single reconstruction source (GCMF §3.3-11); state transitions carry actor/time/reason and are never overwritten (SCD discipline, no deletes — Retired, not dropped); TRACE.md keeps the FR → artefact → test → instance thread per slice (the tender-evidence posture); Joget's audit columns stay intact because all writes go through the API (P3); outcome writeback to `fact_case_outcomes` is idempotent (ReplacingMergeTree on case_id + outcome_date) so retries cannot double-count.

### P7. One Case Fabric (GCMF), Specialisation by Contract

*Replaces ARMS P7 (ratio-scale measurement), which is ARMS-specific.* All case behaviour is built **once** in CMBB; TABB and DMBB are specialisations, not siblings. A module case requirement is satisfied either by GCMF configuration (cite the metamodel block) or by a declared extension point (transition guard, allocation criteria provider, deadline source, decision consequence, hold scope, case-content form, link type). **Needing a third mechanism is a framework gap to fix in CMBB — never a workaround in the module.**

**Implications:** the ~5 shared plugins (TransitionGuard, AllocationEngine, DeadlineEngine, HoldConnector, EventEmitter) replace 8–11 per-module reimplementations; the Modules 05/08 §5 platform catalogues (CFW/WF/BR/EC/PM…) are satisfied once by CMBB and mapped, not rebuilt per module; every CAD declares GCMF conformance.

**Anti-pattern:** DMBB growing its own deadline logic because "debt escalation is special" — escalation calendars are deadline-engine *configuration*.

### P8. Governed Self-Service

ARMS P8, retargeted from risk analysts to tax officers and administrators. The case-type metamodel (`mm_*`) is the self-service surface: case types, lifecycles, SLAs, escalation chains, allocation rules, notice templates, and thresholds are adjustable by authorised MTCA staff through forms — without IT tickets or deployments. This is the TARA 10.2 claim ("tax officers configure workflows, case types and business rules using visual tools") made real, and it is what liberates the FTEs the engagement promised.

**Implication:** configuration changes are themselves audited and approval-gated (a changed escalation chain is a policy act). Build the approval gate into the `mm_*` forms from the start.

### P9. Single Source of Truth Per Concept

ARMS P9 with this project's authority table:

| Concept | Source of truth | Derived copies |
| --- | --- | --- |
| Feature behaviour | FIS spec YAMLs | Generated Joget artefacts |
| Component shape | CAD (entities, state machines, plugin budget) | FIS folders, generated workflow |
| STA balances, debt stock, ageing, TAS lines | **Gold marts** (mock now, platform later) | Joget-side displays and case snapshots |
| Case state, tasks, deadlines, holds | **Joget** (`app_fd_*` via API) | Gold `fact_case_outcomes` (via writeback), dashboards |
| Document binaries, versions, OCR text | **Mayan** | — (Joget holds references only) |
| Document registry row (which doc, for which case/TIN) | Joget `case_document` register | Mayan metadata mirror (TTT spine) |
| MD/config content | `md*`/`mm_*` form data, seeded from CSV in Git | runtime caches |
| Requirements & acceptance criteria | `docs/reqs/` modules (verbatim AC) | FIS traceability tables, tests |

**The boundary this table locks (the SAD confirms it):** Joget never *recomputes* a taxpayer balance — it reads the mart and acts on it; the platform never *owns* case state — it receives outcomes. A number that exists on both sides has exactly one writer.

**Anti-patterns:** caching debt balances in a Joget table and "refreshing" them by plugin logic until they drift from the mart; a debt case storing a copy of taxpayer master data that registration owns; hand-declared notice content drifting from the template that generated it.

### P10. Open Standards and Exit-Readiness

*Adapted from ARMS P10 (sovereignty).* MTCA's context is contractual rather than geopolitical: the DM specification (§13.3) requires platform substitution rights, the Blueprint (§14.3.2) requires BPMN 2.0-exportable workflow definitions, open-format data export, API-first integration, and Keycloak SSO. **Exit-readiness is a property, not a plan** (SAD D-SAD-13): the Domain-4 build is a durable asset; whether ITCAS Phase-2 case modules ever displace it is a future governance decision that this principle keeps cheap. Joget DX (and its Enterprise edition where used, e.g. JasperReportsMenu) is a deliberate commercial-platform exception, mitigated by exit-readiness, and consistent with MITA EA-07's open-source *preference*.

**Implications:** all data in PostgreSQL/MySQL with documented schemas (the `app_fd_*` convention is queryable and exportable); workflow logic documented as CAD state machines (platform-neutral) with XPDL as the generated realisation; business rules live in configuration (P4), which exports as data; everything else in the stack is open source (Mayan GPL, ClickHouse/dbt/Keycloak Apache-2.0, PostgreSQL); no SaaS dependencies; integration only through documented REST contracts.

**Anti-pattern:** burying a business rule in a proprietary platform feature with no exported representation — the rule must always exist in a form ED's migration team can read.

### P11. Isolated Failure, Graceful Degradation

ARMS P11 mapped to this topology. The Blueprint (§13.2.4) already sets the tone: stale data beats no data.

**Implications:** Gold mock/platform unreachable → case work continues on the last-read snapshot, clearly timestamped as such; never block a case officer on an analytics outage. Mayan unreachable → cases proceed, document operations queue and retry; a notice that cannot be stored is not silently "sent". Writeback failure → queue locally, retry idempotently (P6); never lose an outcome, never double-post one. One BB's plugin failure must not take down the shared CMBB engines.

### P12. Perimeter vs Application Security

ARMS P12's two-layer split holds verbatim; the content maps to this stack.

**Perimeter (validation-phase, relaxable):** DEV instances on the local machine/LAN; basic auth; Keycloak SSO (OIDC) is the Phase-2 target per Blueprint §14.3.2 — designed for, not built now.

**Application security (baseline from Day 1, never relaxable):**
- All form-data access via the Joget API (P3) — which is also the SQL-injection defence; the *only* raw-SQL surfaces (reporting binders, Jasper) use parameterised queries, never string-concatenated user input.
- Server-side validators are authoritative; client-side JS validation is convenience.
- Secrets (Mayan tokens, mart credentials, DB passwords) in environment variables / instance configuration — never in Git, never in form data, never in generated artefacts.
- Row-level and menu-level permissions per persona from the first userview — not bolted on later; decision/execution segregation per GCMF §3.4 enforced structurally.
- No taxpayer-identifying data in logs or in the repo; seed data is synthetic.
- Mayan ACLs mirror the case confidentiality classes (the register's class is the source, P9).

### P13. Innovate on Core Competence, Be Boring Everywhere Else

ARMS P13 with the line redrawn. **Core competence here:** the GCMF case-type metamodel (one fabric, eleven modules), the STA-boundary design, the escalation/instalment/enforcement domain logic, and the spec-to-artefact delivery pipeline itself. That is where we go beyond the conventional answer. **Support:** Docker Compose for Mayan/ClickHouse, bash + standard tooling, vanilla Joget theming, the stock Mayan feature set, plain `joget-instance-setup` conventions. The test for any support-area choice: "is this the shape of tool a professionally-run shop already understands?" — and for any core-competence choice: "is this better than what a conventional tax administration would build?"

**Anti-patterns:** writing a custom document previewer when Mayan renders previews; a bespoke scheduler beside Joget's; conversely — settling for eleven per-module case implementations because "that's how Joget apps are usually built" (failure of ambition on exactly the point this architecture exists to prove).

### P14. Portable, Environment-Free Artefacts

ARMS P14 (cloud-ready), restated for a low-code estate. Deployment topology is configuration, not architecture: the same artefacts must run on the laptop DEV instances, a MITA-hosted AKS cluster (Blueprint §14.3.2), and the DX9 target.

**Implications:** no hardcoded hostnames, ports, filesystem paths, or instance names inside forms, BeanShell, plugin code, or Jasper SQL — endpoints and credentials come from environment variables and plugin properties; instance differences live in `~/.joget/instances.yaml` and `.env` files, not in artefacts; Mayan and ClickHouse run containerised from Day 1; the DX8→DX9 delta is handled in skills (workflow §7), keeping artefacts platform-version-portable.

### P15. Mayan Is the Document Engine; Joget Keeps the Register  *(new)*

The document principle, fixing the Mayan-EDMS.md §10.5 pattern as law:

1. **Binaries live in Mayan, only in Mayan.** No file-upload field whose content is the authoritative copy of a case document; no BLOBs in `app_fd_*`.
2. **Joget keeps the register.** The CMBB `case_document` row is the registry of record: document number, case/TIN linkage, class, direction, confidentiality — plus `storage_reference = <Mayan UUID>` and checksum. Mayan mirrors the TTT spine (tin / tax_type_code / tax_period_code) as metadata so its own indexes (the per-TIN 360° tree) stay navigable.
3. **Integration is REST API v4 only** — token-authenticated, via one **MayanConnector** plugin (the single integration point, ISP/DIP below). Never against Mayan's PostgreSQL.
4. **Division of lifecycles:** *case* lifecycle is Joget workflow; *document* lifecycle (DRAFT → UNDER_REVIEW → APPROVED → DISTRIBUTED → ARCHIVED) is Mayan workflow; the connector keeps the register row's status in sync. Required-documents-per-state checks are CMBB transition guards reading the register — not Mayan logic.
5. **Generated documents** (notices, statements — Jasper output) are filed to Mayan at creation with full metadata; dispatch/delivery-proof events land in the register and `case_event`.
6. **Mayan configuration is code-managed:** document types, metadata types, and indexes are created by a versioned Python setup script against the API (reproducible, like everything else in this delivery).
7. Uniqueness and registry semantics are enforced on the Joget side (Mayan metadata has no unique constraints) — P9 applied to documents.

**Anti-patterns:** uploading a signed instalment agreement into a Joget file field "for now"; a second ad-hoc Mayan client inside a different plugin; officers filing documents directly in Mayan's UI for case documents (the register would not know they exist).

---

## Module Design Principles (SOLID)

Binding on **everything we code**: custom plugins (Java/OSGi), the Mayan setup script, the mock-platform API, seed generators, validation scripts. Not binding on Joget, Mayan, or ClickHouse internals — those are consumed through their interfaces (and for Joget, *only* through its interfaces, per P3).

- **SRP.** One plugin, one job. The DeadlineEngine changes when clock semantics change, not when an escalation calendar changes (that's configuration, P4). A ProcessTool that both computes a balance snapshot *and* generates a notice is two tools.
- **OCP.** New behaviour = new configuration or a new registered extension (a new allocation criteria provider, a new deadline source), not an `if (caseType.equals("DEBT"))` branch inside a shared CMBB engine. The GCMF extension points (P7) are this project's named extension surface.
- **LSP.** Every implementation of an extension point honours the contract: every transition guard takes the case context and returns pass/fail+reason; every allocation provider returns the same candidate shape. A guard that silently mutates the case violates the fabric.
- **ISP.** Consumers get narrow interfaces: DMBB depends on the deadline engine's "register clock / pause / resume" surface, not on CMBB's full internals; the Gold client exposes `getDebtProfile(tin)` / `getPriorityQueue(...)`, not "run SQL".
- **DIP.** Business plugins depend on interfaces — `GoldMartClient`, `DocumentStore`, `CaseEventSink` — with the concrete REST/Mayan/ClickHouse clients behind them. This is what makes the mock→real platform switch a configuration change (P14) and lets plugin logic be unit-tested without a running stack.

---

## Applying Principles to Open Questions

| Question | Resolution | Governing principles |
| --- | --- | --- |
| Read the accounting data product: REST gateway or SQL? | **SQL (JDBC) against published, versioned data-product views** (Module 05 §9.2.3 JDBC pattern, §9.2.4 views: `taxpayer_balances`, `payment_history`, `assessment_register`, `taxpayer_360`) — Joget-native for datalists/Jasper/dataset beans; the view schema is the contract. REST remains for **writes** (outcome writeback) and computation-wrapping services (risk-scored queue behind SAS). Plugin access behind `GoldMartClient` either way. INT-FR-001's "RESTful" letter: recorded SAD deviation, spec amended next revision. | P3, P9, DIP, P14 |
| Escalation rules: workflow routes, plugin, or configuration? | **Configuration** (`mm_*` chains) executed by the shared engines; workflow routes carry only the state machine | P3, P4, P7 |
| Where may raw SQL appear? | JdbcDataListBinder for reporting datalists; Jasper datasources; read-only acceptance/verification SQL. **Nowhere else; never a write.** | P3 |
| BeanShell or plugin? | BeanShell only for thin glue (≲ a dozen lines, no rules, no SQL); anything more is a typed plugin in the CAD budget | P3, P4 |
| Documents in Joget file fields? | Only as transient capture before filing to Mayan; the Mayan copy is authoritative the moment it exists | P15, P9 |
| One Mayan client per BB? | No — one **MayanConnector** plugin, shared | P15, ISP, P13 |
| Hand-fix a generated artefact? | Never — fix spec or skill, regenerate | P3, P5 |
| STA balance computed in Joget? | No — read from Gold; Joget owns case state and accounting *operations* + writeback (boundary locked in the SAD) | P9 |
| Keycloak now? | Phase 2; designed-for now (no auth assumptions baked into artefacts) | P1, P12 |
| Joget Community or Enterprise? | Enterprise assumed where Jasper menus are used; record as exit-readiness item | P10 — **open, confirm at SAD** |
| TA-RDM L2 YAMLs | Needed for CAD mappings | **open — location to be provided** |

## Principle Lifecycle

Review at each phase boundary: P1 and the perimeter half of P12 are *[validation phase]* and tighten when the user base grows past the DEV team; P10's Enterprise exception is re-examined at ITCAS Phase-2 migration planning. P3, P15, the application-security half of P12, the tables-in-configuration rule of P4, and SOLID have **no lifecycle** — they are not phase-relaxable. Enforcement vehicle: gate G1 checks the plugin budget (P3/P4), gate G2 checks carriers and traceability (P4/P6), gate G3 checks regeneration cleanliness (P5), gate G4 checks the audit thread (P6).

---

*End of v0.1. Supersedes nothing; companion to `00_MASTER-PLAN.md`. On approval, the SAD (A1) cites these principles as its decision basis.*
