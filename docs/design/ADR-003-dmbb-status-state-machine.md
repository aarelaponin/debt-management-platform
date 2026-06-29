# ADR-003 — DMBB entity-status state machine (config-driven, per-tax-type)

**Status:** **Accepted** 2026-06-17 (priority = configurability, maintainability, Joget-native/no-DB;
recommended set in §0). Build under way: foundation increment = carriers + dormant `StatusManager`
(§5 step 1). open-Q3 (which taxes need distinct workflows) is a later seed-sizing input, not a blocker. · **Author:** (DMBB QA round)
**Supersedes/relates:** ADR-001 (CMBB specialisation), the form-quality QA round (read-only/LOV, 2026-06-17)
**Decision sought:** how DMBB entity statuses become the result of a *guarded, configurable* state
machine — reusing what already exists — with each MTCA tax type able to carry its own workflow.

---

## 0. Recommended decision set — priority: configurability, maintainability, **Joget-native (no databases)**

> Governing constraints (user, 2026-06-17): (1) maximise **configurability** — lifecycles editable as
> data, no redeploy; (2) maximise **maintainability** — fewest moving parts, consistent with existing
> patterns; (3) **as Joget-native as possible, no databases** — config and audit live in Joget *forms*
> (the `mm*`/`cm*` metamodel), accessed only via the Joget API / `FormDataDao` (P3) — never a separate
> store, raw SQL table, or DB trigger; logic ships as a plugin in the **existing** `cmbb-plugins`
> bundle, not a third-party bundle.

| # | Decision | Recommended | One-line rationale (config / maintainability / native) |
|---|---|---|---|
| D1 | State machine at all? | **A3** guarded, config-driven service | Only option that is configurable *and* validated+audited. A4 (BPM) and **A5 (DB triggers — excluded by "no databases")** are out. |
| D2 | Engine / where the map lives | **B3** native service in `cmbb-plugins`, reading `mm*` via `FormDataDao` | Most Joget-native + most maintainable: **no external `joget-status-framework` bundle**, no registry-refresh cache to own; mirrors the existing `MmConfigService` (its `transitionAllowed()` ≈ `canTransition()`). Equally configurable as B2. **B2 dropped.** |
| D3 | Per-tax-type keying | **C4** layered `taxType ▸ caseType ▸ DEFAULT`, **wholesale shadowing** | Max configurability with least duplication (shared baseline + thin per-tax overrides); shadowing keeps the effective map predictable/testable. Ship a `validNext` "effective-lifecycle" preview so config stays inspectable. C5 (rules engine) rejected — too much to govern/maintain. |
| D4 | Config carrier | **E1** Joget forms `mmEntityState` + `mmEntityTransition` | Native form-data config, admin-editable via the existing datalist/userview tooling; keeps the green spine carriers untouched (vs E2's migration risk); E4 (JSON blob) rejected — not Joget-form-configurable. |
| D5 | Audit | **F1** reuse the native `cmEvent` SHA-256 chain (`STATUS_CHANGED`) | One native, tamper-evident audit spine already verified by `ChainVerifyService`; no second/external store. Evolve to F4 (add a queryable projection) only at production scale. |
| D6 | Adoption | **G1** incremental, one entity per feature-loop pass, lowest-risk first | Most maintainable rollout; isolates defects; keeps the baseline green each pass (the established discipline here). |

**Net shape:** a small native service (`StatusManager`) in `cmbb-plugins` that reads the lifecycle from
Joget `mm*` forms via `FormDataDao`, validates each transition (most-specific scope wins), writes the
new status via `FormDataDao` (bypasses read-only — engines only), and appends a `STATUS_CHANGED`
`cmEvent`. Lifecycles — including per-tax variants — are pure Joget form data; admins edit them through
ordinary Joget screens. **No external bundle, no extra database, no raw SQL, no new process.**

The only open input is business, not architecture: **open-Q3** — which tax types actually need a
distinct workflow (to size the `scope=<tax>` seed rows) and who owns that config.

---

---

## 1. Context & problem

The form-quality round made every status field **read-only in the UI** (a user can no longer type
`CANCELLE` over `CANCELLED`). That closes the *user* hole. It does **not** make status a state machine:

- DMBB engines set status with raw `FormDataDao` writes — e.g. `setProperty("status", "EXECUTED")`,
  `setProperty("stage", "Final demand")`, `setProperty("writeOffStatus", "written-off")` — across
  **~15 services** (Escalation, Relief/instalment, EnforcementAction, WriteOff, DefaultAssessment,
  CollectionMi, DebtIdentification, the CMBB phases, …).
- There is **no validation** that a transition is legal (an engine bug could move `REJECTED → ACTIVE`),
  **no audit** of who/what/why changed a status, and **no configurability**: the allowed transitions
  are implicit in imperative Java, not data.
- MTCA requirement (user, 2026-06-17): **every tax type has its own standard workflow** — the lifecycle
  must be **configurable per tax type / case type**, not hard-coded.

The subject/child entities and their status fields in scope:

| Entity (table) | Status field | Indicative lifecycle (today, implicit in code) |
|---|---|---|
| `cmCase` (spine) | `currentState` | NEW → OPEN → … → CLOSED — **already config-driven (see §2)** |
| `dmDebt` | `stage` | Identified → Reminder → Demand → Final demand → Enforcement |
| `dmInstAgr` | `status` + `complianceStatus` | APPLIED → UNDER_REVIEW → ACTIVE → COMPLETED / CANCELLED / REJECTED ; compliance OK → DEFAULTED |
| `dmAction` | `status` | INITIATED → EXECUTED / SUBMITTED / BLOCKED / FAILED / REFERRED |
| `dmAgent` | `status` | APPOINTED → REPORTING → CLOSED / ALERTED |
| `dmWriteOff` | `status` | SUBMITTED → UNDER_REVIEW → APPROVED / REJECTED → POSTED |
| `dmDefAssess` | `status` | DRAFT → ASSESSED / NEEDS_JUSTIFICATION → REPLACED / ESCALATED |
| `dmInstLine` | `status` | pending → paid / missed |

## 2. What already exists to reuse (do NOT reinvent)

**(a) CMBB already has a config-driven state machine — for the case spine.** The `mm*` metamodel
(`CMBB-F01`) plus `MmConfigService` are exactly the pattern we want, already proven on `cmCase`:

- `mmCaseType` (code, name) — the registered case types (DM, …).
- `mmState` (caseType, code, name, **envelopeState**, **isTerminal**, version, active) — per-type states.
- `mmTransition` (**caseType**, **fromState**, **toState**, guardRefs, actorRoles, version) — per-type
  allowed transitions.
- `MmConfigService.transitionAllowed(caseType, from, to)` reads `mmTransition`; `terminalStateCodes()`,
  `stateByEnvelope()`, etc. — **all lifecycle rules live in data (P4)**. The CMBB TransitionGuard
  enforces `cmCase.currentState` moves against this table.

So **per-case-type, data-driven transitions are already a solved, deployed pattern here.** The gap is
only that the *subject/child* statuses (`stage`, `status`, …) don't go through an equivalent.

**(b) The generic transition engine exists** — `joget-status-framework`
(`global.govstack.statusframework`: `StatusFramework` core + `EntityType`/`Status`/`TransitionAuditEntry`/
`InvalidTransitionException`). The GAM `StatusManager` shows the reuse shape: define a transition map,
`register()` it with the framework, then `transition(dao, entity, id, target, by, reason)` validates +
writes + audits, throwing `InvalidTransitionException` on an illegal move. **But** GAM's map is
**hard-coded in Java** — which conflicts with the per-tax-type-configurable requirement.

**Reuse decision:** take the framework's *mechanism* (guarded transition + audit row + valid-next API)
but **feed it from the `mm*` config tables**, not a Java map — i.e. marry §2(a) and §2(b). This keeps
one consistent, data-driven lifecycle model across spine *and* subject entities.

## 3. Decision

Introduce a **generalised, config-driven entity-status state machine** for DMBB, built by extending the
existing `mm*` metamodel pattern and enforced by one shared service. Concretely:

### 3.1 Config model — extend the metamodel to entities (data, not code)

Add an **entity dimension** so the same model covers subject/child statuses, scoped for per-tax-type
flexibility. Two carriers (mirroring `mmState`/`mmTransition`):

- **`mmEntityState`** — `entity` (dmDebt|dmInstAgr|dmAction|dmWriteOff|dmDefAssess|dmAgent|dmInstLine),
  `scope` (see §3.3), `code`, `name`, `isInitial`, `isTerminal`, `active`, `version`.
- **`mmEntityTransition`** — `entity`, `scope`, `fromStatus`, `toStatus`, `guardRefs` (optional
  pre-conditions, e.g. `noOpenObjection`, `evidencePresent`), `actorRoles`, `version`.

(Alternative considered: add an `entity` column to the existing `mmState`/`mmTransition` and default it
to `cmCase`. Rejected for clarity — the spine lifecycle and the subject lifecycles read differently
enough that separate carriers keep both simple and avoid migrating the working spine config. Decision
open for review.)

### 3.2 Enforcement service — `StatusManager` (CMBB, config-backed)

A single shared service in the `cmbb-plugins` bundle, modelled on `MmConfigService` + the
`joget-status-framework` API:

```
StatusManager.transition(dao, entity, recordId, targetStatus, scope, actor, reason)
  1. load current status of the record
  2. assert mmEntityTransition permits (entity, scope, current → target)   // config lookup
  3. evaluate guardRefs (if any)                                            // pluggable pre-conditions
  4. write the new status via FormDataDao (bypasses readonly — engines only)
  5. append an audit row  (see §3.4)
  throws InvalidTransitionException on an illegal move
StatusManager.canTransition(entity, scope, from, to) / validNext(entity, scope, from)  // for UI + tests
```

Engines stop calling `setProperty("status", …)` directly and call `StatusManager.transition(…)`.
Initial creation uses `isInitial` from `mmEntityState` (the form `value:` default stays as the seed).

### 3.3 Per-tax-type flexibility (the core requirement) — `scope` + most-specific-wins

`scope` is the lifecycle key. Resolution is **most-specific-first** so a tax type can override the
default workflow *in data* without touching code:

```
scope lookup order:  taxType (e.g. "VAT")  ▸  caseType ("DM")  ▸  "DEFAULT"
```

- Most entities will define one `scope=DM` (or `DEFAULT`) lifecycle.
- Where a tax genuinely differs (e.g. VAT enforcement skips a step PIT requires), seed
  `mmEntityTransition` rows with `scope=VAT`; the resolver picks them over the `DM` baseline.
- This mirrors the DPM profile pattern already used for `mmStrategy`/`mmEscStep` (country/archetype
  seed sets) — same "configuration as seed data" philosophy (P4, D-SAD-14).

### 3.4 Audit

Every transition writes an immutable audit row — reuse the existing **`cmEvent`** hash-chain
(CMBB-F02/F09, already SHA-256 verified) with an event type `STATUS_CHANGED` carrying
`entity/recordId/from/to/actor/reason`, OR a dedicated `cmStatusEvent` table mirroring
`TransitionAuditEntry`. **Decision:** reuse `cmEvent` — it already gives tamper-evidence and the
`ChainVerifyService`, and keeps one audit spine. (Open for review.)

### 3.5 UI

Statuses are already read-only (done). Follow-on (not required for this ADR): expose **valid next
transitions** as buttons/actions driven by `StatusManager.validNext(...)`, so an officer's allowed
moves are themselves config-driven — no free text ever.

## 4. Why this over the alternatives

| Option | Verdict |
|---|---|
| **Hard-coded Java map (GAM `StatusManager` as-is)** | Rejected — violates the per-tax-type-configurable requirement; re-deploy for every workflow tweak. |
| **Leave statuses to engines + read-only forms only** | Rejected as the end state — no transition validation, no audit; an engine bug or a future integration can still set an illegal status. (It is, however, the acceptable *interim* already shipped.) |
| **Extend the `mm*` config pattern + shared guarded service (this ADR)** | Chosen — reuses the deployed, proven, data-driven CMBB lifecycle pattern; satisfies per-tax-type config; one audit spine; incremental adoption. |

## 5. Adoption plan (incremental, one entity at a time — each its own feature-loop pass)

1. **Build the carriers + service:** `mmEntityState`/`mmEntityTransition` forms + seeds; `StatusManager`
   (+ unit tests on the `GuardTestHarness`); no engine change yet (the service is dormant).
2. **Migrate entity-by-entity**, lowest-risk first (`dmInstLine` → `dmAction` → `dmWriteOff` →
   `dmDefAssess` → `dmInstAgr` → `dmDebt.stage`): replace each engine's `setProperty(status,…)` with
   `StatusManager.transition(…)`; seed that entity's `mmEntityState`/`mmEntityTransition`; add an
   acceptance check that an **illegal transition is rejected** and a legal one audits. Full regression
   after each.
   - ✅ **`dmInstLine` (2026-06-17, DONE):** `ReliefService.evaluateLines` now routes the due-line
     outcome through `StatusManager.transition` (scope `["DM"]`): `pending→paid/missed` guarded +
     audited as `STATUS_CHANGED` on the case chain; no-op (same status) skipped; added `missed→paid`
     (late settlement — the engine recomputes status each run, so this move must be legal). Initial
     `pending` creation in `apply()` stays a direct seed (no from-state to guard). Unit guard: the
     `ReliefServiceTest` fake DAO now stubs `count` + seeds the dmInstLine lifecycle (129 JUnit green,
     incl. illegal-rejection via `StatusManagerTest`). Live: run_t15 **T-15.5** asserts 2 missed
     `STATUS_CHANGED` events + 1 still-pending line. Full cold-start regression **t02..t27 GREEN=26**.
   - ✅ **`dmAction` (2026-06-17, DONE):** `EnforcementActionService` (initiate/execute/reject + the
     confirm sweep) routes all 10 status writes through a new `StatusManager.apply(entity,row,…)` —
     a **row-based** guard variant that validates + sets the field on the caller's already-loaded row
     (so the engine's single batched `saveOrUpdate` still persists it) and audits `STATUS_CHANGED`;
     `transition()` is for the load-write-in-one case. Moves: `INITIATED→EXECUTED/SUBMITTED/BLOCKED/
     REFERRED/FAILED`, `SUBMITTED→CONFIRMED`. Added seed `INITIATED→FAILED` (garnish decline). Key
     subtlety: `dmAction.status` is **read-only**, so the data API drops the creator's `INITIATED`
     and the row arrives blank — `initiate()` now establishes the canonical `INITIATED` in-memory
     before the guarded transition (else `""→outcome` is correctly illegal). `EnforcementActionServiceTest`
     fake DAO: broadened the `find` matcher (`anyInt`→`any`, for the `null` start/limit) + seeded the
     dmAction lifecycle. Live: run_t16 **T-16.9** asserts EXECUTED + BLOCKED both emit `STATUS_CHANGED`.
     Full cold-start regression **t02..t27 GREEN=26** (run_t16 9/9, run_t27 6/6). `dmAgent` left raw
     (separate entity, not in scope).
   - ✅ **`dmWriteOff` (2026-06-18, DONE):** `WriteOffService` (submit/approve/post/sweep) routes all
     status writes through `StatusManager.apply`: `SUBMITTED→UNDER_REVIEW/REJECTED/POSTED` (auto-C1),
     `UNDER_REVIEW→POSTED/REJECTED`. `post()` is a shared terminal-write reached from three states;
     the initial `SUBMITTED` is established at both creation points (submit() normalises the read-only
     dropped value; the sweep's `autoWriteOff` sets it on the new row) so `post()` always has a valid
     from-state. Seed reconciled to engine reality: added `SUBMITTED→REJECTED` (evidence-missing at
     submit) + `UNDER_REVIEW→POSTED` (approve posts in one step — the engine never sets an intermediate
     `APPROVED`; the pre-existing `UNDER_REVIEW→APPROVED`/`APPROVED→POSTED` edges are kept but unused).
     `WriteOffServiceTest` fake DAO: `find` matcher `anyInt→any` + seeded lifecycle. Live: run_t18
     **T-18.8** (approve chain 2× + reject 1× STATUS_CHANGED). Full cold-start regression
     **t02..t27 GREEN=26** (run_t18 8/8, run_t27 6/6).
   - ✅ **`dmDefAssess` (2026-06-18, DONE):** `DefaultAssessmentService` (assess/replace/escalate) routes
     all status writes through `StatusManager.apply`: `DRAFT→ASSESSED/NEEDS_JUSTIFICATION`,
     `NEEDS_JUSTIFICATION→ASSESSED`, `{DRAFT,NEEDS_JUSTIFICATION,ASSESSED}→REPLACED`, `ASSESSED→ESCALATED`.
     This entity is **pre-debt** (no case until escalation) — the audit chains under the assessment's
     synthetic `DA-<id>` anchor, switching to the new debt case on escalate (`debtCaseRef` set before
     the guarded move). `assess()` normalises the read-only-dropped `DRAFT`; the `NEEDS_JUSTIFICATION`
     write is no-op-guarded (re-assess without justification). Design choice: `replace()` has no status
     guard, so `DRAFT→REPLACED` + `NEEDS_JUSTIFICATION→REPLACED` are seeded (return filed before/instead
     of assessment) but `ESCALATED→REPLACED` is deliberately NOT — the guard now blocks replacing an
     already-escalated assessment (a debt case exists). `DefaultAssessmentServiceTest` fake DAO: `find`
     matcher `anyInt→any` + seeded lifecycle. Live: run_t19 **T-19.6** (ASSESSED under `DA-id` + ESCALATED
     under the new case). Full cold-start regression **t02..t27 GREEN=26** (run_t19 6/6, run_t27 6/6).
   - ✅ **`dmInstAgr` (2026-06-18, DONE):** `ReliefService` (apply/reject/cancel) routes the agreement
     status writes through `StatusManager.apply`: `APPLIED→ACTIVE` (auto-approve), `APPLIED→UNDER_REVIEW`
     (routed to authority), `APPLIED→REJECTED` (ineligible / second active plan / below-minimum), and
     `ACTIVE→CANCELLED` (default). `apply()` normalises the read-only-dropped `APPLIED`. The engine never
     sets `COMPLETED` (the seeded `ACTIVE→COMPLETED` is aspirational, harmless). `ReliefServiceTest`
     fake DAO already had the count stub + `any()` matcher (from #1); added the dmInstAgr lifecycle seed.
     Live: run_t15 **T-15.6** (auto-approve + reject → 2 agreement STATUS_CHANGED on caseA). Full
     cold-start regression **t02..t27 GREEN=26** (run_t15 6/6, run_t27 6/6). Five of six entities done;
     only `dmDebt.stage` remains.
   - ✅ **`dmDebt.stage` (2026-06-19, DONE — capstone):** `EscalationService.walk` guards the subject
     stage ladder `Identified → Reminder → Demand notice → Final demand → Bank garnishing → Field
     enforcement`. **Seed reconciliation:** the foundation seeded idealised names (`Demand`,
     `Enforcement`); the real strategy `stepName`s (mmEscStep) are `Demand notice` / `Bank garnishing` /
     `Field enforcement` — reconciled both `mmEntityState` + `mmEntityTransition` (DM + VAT) to the real
     names (the old idealised seed had been dormant, so live cases already used the real names).
     **Scope = `["DM"]`, not per-tax:** the engine selects its strategy by segment+category (tax-agnostic),
     so the same ladder fires for every tax; a per-tax chain would wrongly reject a VAT case going through
     Reminder. The VAT override stays seeded + config-tested (run_t27 **T-27.4**) as the proven capability,
     wired to the engine only once strategy selection becomes tax-aware (this completes step 3's config
     half; the engine half is the future pilot). **Guard-exposed latent bug + fix:** the global sweep hit
     leftover SQL-fixture cases at `Final demand`/`seq=0`; the old unguarded write silently REGRESSED them
     (`Final demand → Reminder`), the guard threw and aborted the whole batch. Fixed `walk` to pre-check
     `canTransition` and skip an inconsistent case rather than regress it — turning a silent corruption
     into a safe no-op and making the sweep batch-resilient. Live: run_t13 **T-13.5**. Full cold-start
     regression **t02..t27 GREEN=26** (run_t13 5/5, run_t27 6/6). **All six entities migrated.**
3. ✅ **Per-tax-type pilot (DONE, 2026-06-19):** config half — `scope=VAT` seeded + proven by run_t27
   T-27.4 and `StatusManagerTest`. **Engine half** — `EscalationService.walk` now resolves scope
   `[caseTaxType, "DM"]` and *consumes* (skips, without regressing) any strategy step whose stage is
   not legal in the case's scope lifecycle. A VAT case therefore skips the Reminder and escalates
   straight to Demand notice, driven entirely by the lifecycle config — no new strategy template,
   no schema change. Proven live by run_t13 **T-13.6** and unit `vatSkipsReminderViaLifecycle`.
   open-Q3 (which other taxes need a distinct ladder) is now just a matter of seeding more
   `scope=<tax>` overrides — no further engine work.
4. **UI follow-on:** valid-next actions.

## 6. Risks / constraints (carry from this round)

- **Read-only ≠ API-writable** (DX9-DELTA, 2026-06-17): the data API drops read-only fields; engines via
  `FormDataDao` are unaffected. `StatusManager` writes through `FormDataDao`, so it is fine; **tests that
  seed a status must use the engine path or a SQL fixture**, never the data API.
- **Order-independent regression:** any new stateful suite must `drain()` + assert relatively (skill rule).
- **Scope creep:** keep `StatusManager` in the shared `cmbb-plugins` bundle (CMBB owns case mechanics, P7);
  DMBB contributes only config rows + the per-entity transition seeds.
- **No new XPDL:** this is status-on-records, not a new process — the single `cmCaseEnvelope` is untouched.

## 7. Decision log

| ID | Decision | Basis |
|---|---|---|
| D-SM-01 | Entity statuses become a guarded, **config-driven** state machine, extending the `mm*` metamodel pattern already used for the case spine. | §2(a); P4 |
| D-SM-02 | **Native config-backed `StatusManager` in the existing `cmbb-plugins` bundle (B3)** — NOT the external `joget-status-framework`. Reason: that framework is enum-typed with a `register()`-once static map (fights data-driven, live-editable config → forces a refresh/cache-invalidation), is an external embedded bundle (extra version/classloader risk — cf. the clickhouse slf4j delta), and carries its own audit shape (competes with `cmEvent`). We barely need it: `MmConfigService.transitionAllowed()` already IS `canTransition()`, so `StatusManager` is ~3 small methods over existing native services. Keep the framework as a *reference* (transition-map / validNext / InvalidTransition shape), not a dependency. | configurability + maintainability + Joget-native (user 2026-06-17); §2(b) |
| D-SM-03 | Lifecycle keyed by `scope` with **taxType ▸ caseType ▸ DEFAULT** most-specific-wins, so each MTCA tax can carry its own workflow in data. | user 2026-06-17; D-SAD-14 |
| D-SM-04 | Audit via the existing `cmEvent` hash-chain (`STATUS_CHANGED`). | §3.4 (open for review) |
| D-SM-05 | Adopt incrementally, one entity per feature-loop pass, lowest-risk first; full regression each. | skill; risk |

## 8. Open questions for sign-off
1. Separate `mmEntityState`/`mmEntityTransition` carriers vs. an `entity` column on `mmState`/`mmTransition`? (§3.1)
2. Audit via `cmEvent` `STATUS_CHANGED` vs. a dedicated `cmStatusEvent`? (§3.4)
3. Which tax types actually need a distinct workflow at MTCA (to size the `scope=<tax>` seeds), and who owns that config? (§3.3)
4. Priority order of entity migration if different from §5.
