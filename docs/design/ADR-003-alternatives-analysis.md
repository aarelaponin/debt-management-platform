# ADR-003 — Alternatives analysis (consequences & drawbacks)

**Companion to** `ADR-003-dmbb-status-state-machine.md` · **Date:** 2026-06-17 · **Status:** for review

This document elaborates, for **every decision** in ADR-003, **every alternative considered**, with its
consequences and drawbacks (and the upside, so the trade-off is balanced). The recommended option is
marked ★ and matches the ADR. Read this to challenge the ADR; the ADR itself stays terse.

Context constraints that colour every option (established this session):
- **Read-only ≠ API-writable** — a `readonly` form field is dropped by the REST data API
  (`saveOrUpdate` runs the form pipeline); only `FormDataDao` (in-process, engines) writes it. Any
  status mechanism must write via `FormDataDao`, and test/integration seeding of a status must not use
  the data API.
- **CMBB already runs a config-driven, per-case-type spine state machine** (`mmState`/`mmTransition` +
  `MmConfigService`) — proven, deployed, green.
- **A reusable generic engine exists** (`joget-status-framework`: guarded transition + audit), but its
  GAM usage hard-codes the transition map in Java.
- **Discipline:** every change runs the full 25-suite cold-start regression; stateful suites must be
  order-independent; generated artefacts are never hand-edited (fix spec/generator).
- **Scale:** ~15 services across the bundle currently set status with raw `setProperty`.

---

## Decision 1 — Should entity statuses be a *guarded state machine* at all? (enforcement approach)

> ADR choice: **A3 (config-driven guarded service).** This is the central decision; the rest refine it.

### A1 — Read-only forms only; no transition guard (the interim already shipped)
- **Consequences:** zero further build; the user-facing defect (typing `CANCELLE`) is already gone;
  engines keep setting status freely; the live PoC stays exactly as it is today (green).
- **Drawbacks:** no validation that a status move is legal — an engine bug or a future REST/integration
  caller can still write `REJECTED → ACTIVE` or an unknown value; no audit of who/what/why changed a
  status (only the immutable `cmEvent` business events exist, not a status-transition trail); the
  "every tax has its own workflow" requirement is unmet; legal-defensibility of an enforcement action
  is weaker (you can't prove the case was in a state that *permitted* it).
- **When right:** if DMBB stays a short-lived PoC and statuses are only ever set by the (trusted) engines.

### A2 — Hard-coded Java transition map (reuse GAM `StatusManager` verbatim)
- **Consequences:** fastest *correct* path to guarded transitions + audit (the framework already does
  the validation, write, audit, `InvalidTransitionException`); strong compile-time safety; unit-testable.
- **Drawbacks:** **directly violates the per-tax-type-configurable requirement** — every workflow tweak
  (a new tax's ladder, a changed step) is a Java edit + `mvn` + redeploy + full regression; business/
  policy users can never adjust a lifecycle; diverges from CMBB's own data-driven `mm*` pattern (two
  competing philosophies in one codebase); the map duplicates knowledge that partly already lives in
  `mmTransition`.
- **When right:** if lifecycles were truly fixed and identical across taxes — they are not, per MTCA.

### A3 ★ — Config-driven guarded service (extend `mm*` + reuse framework mechanism)
- **Consequences:** one consistent, data-driven lifecycle model across spine and subjects; satisfies
  per-tax-type config in data (no redeploy for a workflow change); reuses the deployed `MmConfigService`
  read pattern and the framework's guard/audit mechanism; transitions are validated and audited; an
  illegal move is rejected at the source (engine call), not just hidden in the UI.
- **Drawbacks:** more build than A1/A2 (carriers + a shared service + per-entity seeds + migrating ~15
  call-sites); a runtime config lookup per transition (vs. a compile-time map) — small cost, mitigated
  by caching; **config can be wrong** (a missing/!active transition row blocks a legitimate move) — so
  it needs a "config consistency" validator like the F08 enforcement-config check; the team must keep
  seed data correct as a first-class artefact.
- **When right:** the MTCA reality — durable solution, per-tax workflows, audit/legal needs. ✔

### A4 — Full BPM/workflow per status (model each lifecycle as an XPDL/Joget process)
- **Consequences:** richest modelling (parallel paths, timers, human tasks per state); visual designer.
- **Drawbacks:** massive over-engineering for record-status fields; **breaks the architecture** — DMBB
  deliberately uses ONE `cmCaseEnvelope` process and "no new XPDL ever" (skill rule); a process per
  subject entity multiplies engines/audit surfaces and process-instance bloat; far slower to build and
  operate; the user asked for configurable *status workflows*, not orchestration.
- **When right:** never here; this is for human-routed, multi-actor orchestration, which the envelope
  already provides at the case level.

### A5 — Enforce at the database layer (Postgres CHECK constraints / triggers on `app_fd_*`)
- **Consequences:** DB-guaranteed legality regardless of caller; very strong invariant.
- **Drawbacks:** violates P3 (all writes via the Joget API/DAO; the DB schema is Joget-owned and
  regenerated); transitions would need triggers that encode the same map — now in *three* places
  (DB, config, code); no business-friendly config; brittle across Joget schema rebuilds; opaque errors
  surface as DAO exceptions that mark the JTA tx rollback-only (a known footgun here).
- **When right:** never in a Joget-owned schema; acceptable only for a hand-owned database.

---

## Decision 2 — Where does the transition map live / which engine enforces it? (mechanism)

> ADR choice: **B2 — config tables fed into the `joget-status-framework` mechanism.**
> (D1=A3 settles *that* it's config-driven; D2 settles *how* the engine is built.)

### B1 — Hard-coded Java map registered into the framework (GAM shape)
- **Consequences:** simplest reuse of the framework; compile-time validated map.
- **Drawbacks:** same as A2 — not configurable; redeploy per change. (Listed again because it is the
  default the framework invites; rejecting it is the whole point of D-SM-02.)

### B2 ★ — Config tables → built into the framework's transition API at runtime
- **Consequences:** reuses the framework's proven *mechanism* (validation, audit row, valid-next,
  `InvalidTransitionException`) while the *map* is data; one read path (`MmConfigService`-style); the
  framework's tests/semantics carry over.
- **Drawbacks:** an impedance layer — the framework expects a registered map; feeding it from config
  means either (i) loading config into the framework's registry at startup/first-use and **refreshing
  it when config changes** (cache-invalidation complexity), or (ii) bypassing the registry and querying
  config per call (then the framework is mostly just the audit/exception helpers). Either way you carry
  a dependency on `global.govstack.statusframework` (an external bundle) and must embed/version it.
- **When right:** you value the framework's audit/exception ergonomics and cross-project consistency.

### B3 — Native CMBB `StatusManager` reading `mm*` directly; do NOT use `joget-status-framework`
- **Consequences:** no external bundle dependency; the service is a thin sibling of `MmConfigService`
  (which already does exactly this kind of config read); full control of audit shape (e.g. straight to
  `cmEvent`); nothing to refresh — it reads config live each call.
- **Drawbacks:** re-implements the framework's validation/audit/valid-next (modest duplication, but it
  *is* duplication); loses cross-project consistency with the GAM/registration codebases; you own the
  edge cases the framework already solved.
- **When right:** if the framework's registry/refresh model fights the "live config read" you want — in
  which case B3 is arguably *simpler* than B2 and more consistent with `MmConfigService`. **This is the
  strongest contender against B2; worth deciding explicitly.**

### B4 — Leave it distributed in each engine (status quo)
- **Consequences:** no new component.
- **Drawbacks:** the problem itself — 15 copies of implicit rules, no guard, no audit, no config. Non-starter.

> **Note for sign-off:** B2 vs B3 is a real fork. B2 maximises reuse/consistency; B3 maximises
> simplicity and fit with the existing `MmConfigService` live-read style. The ADR leans B2 for reuse,
> but B3 may be the lower-risk build. Recommend deciding this alongside open-question #1.

---

## Decision 3 — How is per-tax-type flexibility keyed? (scope model)

> ADR choice: **C4 — layered most-specific-wins `taxType ▸ caseType ▸ DEFAULT`.**

### C1 — Single global lifecycle (no scope; one map for all)
- **Consequences:** simplest config; one set of states/transitions to maintain and test.
- **Drawbacks:** **fails the requirement outright** — cannot express a VAT-specific ladder vs. a PIT one;
  forces the union of all tax behaviours into one map (either too permissive or wrong for some taxes).
- **When right:** only if all taxes truly shared one lifecycle (they don't).

### C2 — Per-case-type only (scope = caseType, e.g. `DM`)
- **Consequences:** matches today's spine pattern exactly (`mmTransition.caseType`); minimal new concept;
  enough if "the debt workflow" is uniform across taxes.
- **Drawbacks:** can't vary by tax *within* debt management; if MTCA's VAT debt path differs from PIT,
  you'd have to fork the case type (`DM_VAT`, `DM_PIT`) — proliferating case types and duplicating all
  the other case-type config (allocation, SLA, docs) just to vary status transitions. Ugly.
- **When right:** if per-tax differences turn out to be nil — then C2 is the least machinery.

### C3 — Per-tax-type only (scope = taxType)
- **Consequences:** directly models "every tax its own workflow".
- **Drawbacks:** every tax must define a full lifecycle even when most are identical (no shared baseline
  → mass duplication and drift); no place for a cross-tax default; debt cases that aren't tax-specific
  (consolidated multi-tax) have no clear scope; doesn't reuse the spine's case-type keying.
- **When right:** if taxes share almost nothing — unlikely; most of the ladder is common.

### C4 ★ — Layered most-specific-wins `taxType ▸ caseType ▸ DEFAULT`
- **Consequences:** one shared baseline (`DEFAULT`/`DM`) plus thin per-tax overrides only where a tax
  genuinely differs; mirrors the DPM country/archetype profile pattern already used for
  `mmStrategy`/`mmEscStep`; minimal duplication; new tax onboarding = add only its deltas.
- **Drawbacks:** resolution logic is more complex than a single key (must look up in order and merge or
  shadow); **merge semantics must be decided** — does a `taxType` scope *replace* the case-type
  lifecycle wholesale, or *override individual transitions*? (Wholesale = simpler, predictable;
  per-transition merge = flexible but can yield surprising effective maps that are hard to reason about
  and test.) Harder to answer "what is the effective lifecycle for VAT?" without a resolver/preview tool.
- **When right:** the MTCA reality (common core + per-tax exceptions). ✔ Recommend **wholesale shadowing**
  (most-specific scope that exists wins entirely) over per-transition merge, for predictability — and a
  `validNext`/effective-lifecycle preview to make the resolved map inspectable.

### C5 — Full rule/decision engine (conditions, not just a scope key)
- **Consequences:** maximal expressiveness (transition allowed if category≥C3 AND amount>X AND …).
- **Drawbacks:** big complexity jump; config becomes a rule language to author, test and govern; opaque
  to business users; overkill — most of this is already handled by the separate `guardRefs`
  pre-conditions (ADR §3.1) which give conditional gating without a general rules engine.
- **When right:** if transitions depended on rich runtime predicates beyond tax/type — they mostly
  don't; `guardRefs` covers the few that do.

---

## Decision 4 — Config carrier shape (data model for the lifecycle)

> ADR choice: **E1 — new `mmEntityState` / `mmEntityTransition` carriers.**

### E1 ★ — Separate `mmEntityState` + `mmEntityTransition` (entity + scope dimensions)
- **Consequences:** clean separation from the spine carriers; the spine config (`mmState`/`mmTransition`)
  stays untouched and green; the entity model can differ where it needs to (e.g. `scope`, `isInitial`)
  without disturbing the spine.
- **Drawbacks:** two more forms/tables/datalists/admin menus to generate, seed and maintain; some
  concept duplication with `mmState`/`mmTransition`; two query surfaces in `MmConfigService`.
- **When right:** when spine vs. subject lifecycles read differently enough that one shape would be
  awkward — which is the case (spine has `envelopeState`; subjects have `entity`/`scope`). ✔

### E2 — Add an `entity` column to the existing `mmState` / `mmTransition` (default `cmCase`)
- **Consequences:** one set of carriers for everything; less new scaffolding; reuses the deployed admin
  screens and `MmConfigService` queries.
- **Drawbacks:** **migrates the working spine config** (every existing row needs `entity=cmCase` +
  re-test) — risk against a green baseline; mixes the spine's `envelopeState`/`isTerminal` semantics
  with subject semantics in one table (nullable columns that apply to only some rows — a smell); the
  `scope` vs `caseType` keys would have to coexist on the spine rows too. Tighter coupling, more blast
  radius.
- **When right:** if you strongly prefer one table and accept the spine-migration risk.

### E3 — One flat `mmStatusTransition` table only (no separate state catalogue)
- **Consequences:** simplest — just the edges (entity, scope, from, to, guards); states are implied by
  appearing in transitions.
- **Drawbacks:** no place for state metadata (`isInitial`, `isTerminal`, display name) — and DMBB needs
  terminal/initial semantics (e.g. write-off `POSTED` is terminal; the read-only "no edits after
  terminal" rule; initial seed). You'd re-derive these heuristically (a state with no outgoing edge =
  terminal?) which is fragile. Loses the `mmState` analogue the spine relies on.
- **When right:** trivial lifecycles with no terminal/initial logic — not DMBB.

### E4 — Encode each entity's lifecycle as a JSON blob in one config row
- **Consequences:** very compact; whole lifecycle edited as one document; easy to ship as a profile.
- **Drawbacks:** not relational — can't query "which transitions leave X", can't FK-validate, no
  per-row admin UI, no datalist; diff/merge of per-tax overrides becomes string surgery; violates the
  tables-in-config (P4) grain the rest of the platform uses; harder to validate.
- **When right:** if lifecycles were owned entirely by developers in version control, not configured.

---

## Decision 5 — Audit mechanism

> ADR choice: **F1 — reuse the `cmEvent` SHA-256 hash-chain (`STATUS_CHANGED`).**

### F1 ★ — `cmEvent` hash-chain, event type `STATUS_CHANGED`
- **Consequences:** one tamper-evident audit spine; reuses the existing `ChainVerifyService` (already
  acceptance-tested, VERIFIED); legally strong (immutable, ordered by zero-padded `seq`); no new table.
- **Drawbacks:** `cmEvent` is keyed to a **case** (`caseId`) — subject/child status changes must carry
  the owning case id (fine for case-bound entities, but `dmInstLine`/agent rows must resolve their case);
  mixing fine-grained status events into the business-event stream can make the case history noisy;
  querying "status history of this dmAction" means filtering the chain rather than a purpose-built table;
  high-frequency status churn inflates the chain.
- **When right:** when tamper-evidence and a single audit spine matter most (tax-debt/enforcement). ✔

### F2 — Dedicated `cmStatusEvent` table (à la `TransitionAuditEntry`)
- **Consequences:** purpose-built, easily queried per entity/record; clean separation from business
  events; matches the framework's native audit shape.
- **Drawbacks:** a second audit store to secure and reconcile; **not tamper-evident** unless you also
  hash-chain it (re-implementing what `cmEvent` already gives); two places auditors must look; more
  schema to own.
- **When right:** if you need heavy status-history *querying/reporting* and tamper-evidence is secondary.

### F3 — Rely on Joget's `dateModified` / built-in form change tracking only
- **Consequences:** zero build.
- **Drawbacks:** captures only the *latest* change, not the trail; no who/why; not tamper-evident;
  unacceptable for enforcement-grade auditability. Non-starter for legal correctness.
- **When right:** never for this domain.

### F4 — Both: `cmEvent` for tamper-evidence **and** a `cmStatusEvent` projection for querying
- **Consequences:** best of both — immutable proof plus a fast queryable view; the projection can be
  rebuilt from the chain.
- **Drawbacks:** most build and the most to keep consistent (the projection must never diverge from the
  chain — needs a reconcile like the F12 ②/③ check); premature for a PoC.
- **When right:** production scale with real reporting needs — a likely *later* evolution of F1.

---

## Decision 6 — Adoption / migration approach

> ADR choice: **G1 — incremental, one entity per feature-loop pass, lowest-risk first.**

### G1 ★ — Incremental, entity-by-entity, lowest-risk first (`dmInstLine` → … → `dmDebt.stage`)
- **Consequences:** each step is small, fully regressed, and reversible; the service is dormant until an
  entity opts in; defects are isolated to one entity per pass; the green baseline is never far away.
- **Drawbacks:** the system is **temporarily heterogeneous** — some entities guarded, some not, for
  several passes (must be documented so no one assumes uniform enforcement); more total
  build/deploy/regression cycles (slower wall-clock); the shared `StatusManager` lands before its first
  consumer (a pass that ships unused code).
- **When right:** the established discipline here (every UX-QA round worked this way) and the safe choice
  against a green, demoed baseline. ✔

### G2 — Big-bang: migrate all ~15 services + all entities in one release
- **Consequences:** uniform enforcement immediately; one deploy; no heterogeneous interim.
- **Drawbacks:** a large, correlated change across the whole engine set + Gold/seed + 8 entities, then
  one enormous regression — if it goes red, the cause is hard to localise (the skill's exact warning:
  "three real platform bugs were only visible in sequence"); high chance of a multi-failure sweep like
  the one we just untangled; risky against a live PoC; hard to review.
- **When right:** a greenfield build or a hard cut-over window with slack — not an iterative QA round.

### G3 — Strangler: new code uses `StatusManager`; leave existing engines as-is
- **Consequences:** least disruption to working engines; new features born correct.
- **Drawbacks:** the existing 15 call-sites — the actual problem — stay unguarded indefinitely; two
  patterns coexist permanently; the requirement (statuses are *always* a state machine) is never fully
  met. Partial by design.
- **When right:** if the existing engines were considered frozen/legacy — they aren't; they're the core.

### G4 — Spine-first (re-base `cmCase.currentState` onto the new model) then subjects
- **Consequences:** unifies spine + subjects under one mechanism earliest; conceptually cleanest.
- **Drawbacks:** touches the **most critical, already-green** machinery (the envelope/TransitionGuard)
  first — highest blast radius for the least new value (the spine already has its config-driven guard);
  risks destabilising every suite at once.
- **When right:** only if consolidating the two mechanisms were the primary goal — it isn't; the spine
  already works.

---

## Cross-cutting drawbacks that apply whatever is chosen (A3 family)

1. **Config-as-data shifts risk to seeds.** A wrong/missing/`active=false` transition row silently blocks
   a legal move. Mitigation: a config-consistency validator (F08 pattern: every entity has an initial
   and ≥1 terminal; no orphan states; the engines' actual moves are all covered) run as a gate.
2. **Test seeding can't use the data API for statuses** (read-only drop). Mitigation: tests drive the
   engine or seed via SQL fixture; bake this into the acceptance template.
3. **Performance:** a config lookup per transition. Mitigation: cache the resolved map per (entity,scope)
   with invalidation on config change; transitions are not hot-path at PoC volume.
4. **Governance:** lifecycles become business-editable config — needs the approval-gated admin path
   (D-SAD-15 policy-change-as-case), or an unreviewed edit could mis-route enforcement.
5. **Effective-map opacity (C4):** with layered scopes, "what is VAT's real lifecycle" needs a resolver/
   preview tool, else config is hard to reason about.

## Recommended set (recap) — priority: configurability, maintainability, Joget-native (no databases)
D1 **A3** · D2 **B3** (native `MmConfigService`-style service in the existing `cmbb-plugins`; **B2 dropped** —
an external bundle + registry-refresh is less Joget-native and less maintainable) · D3 **C4** with
*wholesale shadowing* + an effective-map preview · D4 **E1** (Joget forms) · D5 **F1** native `cmEvent`
chain (→ **F4** only at production scale) · D6 **G1**.

**Excluded by the "Joget-native, no databases" constraint:** A5 (DB triggers/constraints), B2 (external
`joget-status-framework` bundle), E4 (JSON-blob config), F2/F3 (separate or non-tamper-evident audit
stores). All config and audit stay in Joget `mm*`/`cm*` forms, read/written only via `FormDataDao`.

The only remaining open input is **business, not architecture**: open-Q3 — which tax types need a
distinct workflow (to size the `scope=<tax>` seed rows) and who owns that config.
