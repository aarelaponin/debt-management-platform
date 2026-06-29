# ADR-004 — Per-tax / segment / industry Workflow Configuration

**Status:** Proposed (design for review — `[confirm]` markers flag decisions to settle before build)
**Date:** 2026-06-19
**Supersedes/extends:** the segment+category strategy selection in `EscalationService` and the per-tax
lifecycle pilot (ADR-003 §5 step 3, "VAT skips Reminder").

## 1. Context & problem

MTCA runs distinct collection workflows for its main taxes — **VAT**, **CIT** (corporate income tax
under the Income Tax Act), and employer salary taxes **FSS + SSC** — and may vary them further by
**taxpayer segment** (e.g. large / SME / individual) and **industry**. Today:

- The collection ladder is **one shared strategy** (`mmStrategy`/`mmEscStep`) selected by taxpayer
  *segment* + debt *category* only — it is **tax-agnostic**. Per-tax variation exists *only* at the
  stage-skipping level (the VAT lifecycle override).
- Configuration is **entity-CRUD scattered**: to define "the VAT workflow" a user must visit the
  strategy screen, the steps screen, the lifecycle, notices, instruments… across many screens.
- There is **no temporal validity** — a workflow cannot have an effective period, so policy change
  over time cannot be modelled or audited.
- Country is **baked into seed values** (MLT), not a first-class tenant scope.

This ADR designs a **feature-oriented, tenant-scoped, effective-dated workflow configuration
capability**: pick a workflow target (tax × segment × industry), and configure its *whole* behaviour
in one place.

## 2. Decisions captured (from review)

1. **Selector space.** A workflow is keyed by any subset of **[tax type, taxpayer segment, industry]**
   — each dimension is a specific value **or `ANY` (wildcard)**. A workflow has a **validity start
   date (required)** and an **optional validity end date**, so workflows change over time. An
   **overlap check** must prevent two workflows competing for the same target at the same time.
2. **Operational vs legislative tiering.** Split today's broad "Admin" into config the *collection
   business* tunes periodically vs config *fixed by legislation* (change-controlled). Draft
   classification in §7.
3. **Multi-tenancy now.** Country/tenant is a first-class scope **built in from this build** —
   pervasive in the model, **resolved from the user profile**, applied as an **invisible filter**,
   never a navigable screen.

## 3. The configuration model (the core)

A **Workflow** is a versioned, effective-dated rule. Its identity is a **selector** + a **validity
window**, and it *bundles* everything that governs a debt's collection journey.

### 3.1 Selector

| Dimension | Source at resolution time | Values |
|---|---|---|
| `tenant` | user profile (always concrete) | e.g. `MLT` |
| `taxType` | the debt's governing tax | `VAT` / `CIT` / `FSS` / `SSC` / `PIT` / **`ANY`** |
| `segment` | taxpayer master (size/type) | e.g. `LARGE` / `SME` / `INDIVIDUAL` / **`ANY`** |
| `industry` | taxpayer master (NACE-style) | e.g. `CONSTRUCTION` / **`ANY`** |

`ANY` means "applies unless a more specific workflow matches". A pure-`ANY` workflow (tax=ANY,
segment=ANY, industry=ANY) is the tenant's **default ladder**.

### 3.2 Validity (effective dating)

- `validFrom` (date, **required**), `validTo` (date, **nullable** = open-ended).
- A debt is governed by the workflow whose window contains the **decision date** (`asOf`). This lets
  policy evolve: supersede a workflow by end-dating it and starting a new one the next day.

### 3.3 Resolution (which workflow applies to a debt)

Given `(tenant, taxType, segment, industry, asOf)`:

1. **Candidates** = active workflows in `tenant` where each selector dimension **matches the debt's
   value OR is `ANY`**, AND `asOf ∈ [validFrom, validTo]`.
2. **Most-specific wins.** Score each candidate by number of concrete (non-`ANY`) dimensions, with a
   fixed precedence to break ties (`tax > segment > industry` — `[confirm]`). Highest score wins.
3. Determinism is guaranteed by the overlap rule below (no two equally-specific candidates can be
   valid at once).

### 3.4 Overlap prevention (the integrity rule)

> For a given **exact selector tuple** `(tenant, taxType, segment, industry)`, the validity windows
> of all workflows with that tuple **must not overlap**.

Different specificity tuples *may* coexist (that is how a default + a VAT-specific override work
together — resolution picks the most specific). Overlap is therefore checked **per-exact-tuple, on the
time axis only**. Enforced at save (reject) and by a batch **VALIDATE** check (reusing the existing
`cmStrategyCheck`/`cmEnfConfigCheck` pattern).

### 3.5 What a Workflow bundles

- **Lifecycle / stages** — which stages this target's debt passes through (the per-tax override that
  today makes VAT skip Reminder), i.e. the scoped `mmEntityState` / `mmEntityTransition`.
- **Escalation ladder** — ordered steps: trigger days, notice template, enforcement instrument,
  grace, proportionality threshold (today's `mmEscStep`, re-parented from `strategyCode` to the
  workflow).
- **References** to operational policy reused across workflows (relief products, MI targets) — by
  reference, not copy, to keep a **single source of truth**.

## 4. Data model

### 4.1 New / changed config carriers

- **`dmWorkflow`** (new; generalises `mmStrategy`): `code`, `tenant`, `taxType`, `segment`,
  `industry`, `validFrom`, `validTo`, `status` (Draft / Active / Superseded), `version`, `name`,
  `description`. The three selector dimensions accept a literal value or `ANY`.
- **`mmEscStep`** — add `workflowCode` (replaces/augments `strategyCode`); steps belong to a workflow.
- **`mmEntityState` / `mmEntityTransition`** — the per-target lifecycle override is bound to the
  workflow (today's `scope=VAT` becomes "the lifecycle of workflows whose tax is VAT"). The proven
  scope-resolution stays; only the *binding* moves under the workflow.

### 4.2 Tenant key

Add a `tenant` column to **all config tables** (`dmWorkflow`, `mmEscStep`, `mmEntityState/Transition`,
`mdInstrument`, `md*`) and to the **case/data tables** (`cmCase`, `dmDebt`, …). A
**`TenantContext`** resolves the current tenant from the logged-in user's profile attribute
(`dir_user_meta` or a custom field) once per request; batch jobs use a system default. Every datalist
SQL and every engine query gains a `WHERE c_tenant = <ctx>` guard. Seed migration sets `tenant = MLT`
on existing rows.

### 4.3 Governing tax of a (consolidated) debt — `[confirm]`

A debt case is consolidated across tax types (lines carry `taxType`); the workflow selector needs a
single **governing tax** per case. Options: (a) the tax of the **largest** line; (b) a `governingTax`
set at identification; (c) the existing `cmCase.taxType`. Proposed default: **(b)** — stamp
`governingTax` on `dmDebt` at identification (dominant by amount), so resolution is stable and
auditable. Confirm.

## 5. Engine change

`EscalationService.resolveStrategy(segment, cat)` → **`resolveWorkflow(tenant, governingTax, segment,
industry, asOf)`** implementing §3.3. The walk then:

1. fires the **workflow's** steps (timings + instruments + notices), and
2. applies the **workflow's lifecycle scope** to decide which stages apply — i.e. the proven
   "consume the step's slot if the stage isn't legal for this tax, else apply" logic from the VAT
   pilot, now driven by the resolved workflow rather than a hard-wired `["DM"]` scope.

This subsumes the pilot: a fully distinct per-tax ladder (own timings/instruments) AND per-tax stage
set are both expressible. Category handling (`categoryFloor`) stays as a workflow eligibility
attribute (debt size is orthogonal to the selector). `[confirm]` whether debt category should also
become a 4th selector dimension.

## 6. Configuration workspace (feature-oriented UI)

Replace scattered CRUD with **one task-oriented workspace**. Entry point: a **Workflows** list
(tenant-scoped) showing selector + validity + status, with overlap warnings inline. Create/Edit opens
a **tabbed workspace** scoped to one workflow:

```
Configure workflow — VAT · Large · (all industries)        valid 2026-07-01 → open   [Active]
┌────────────────────────────────────────────────────────────────────────────────────┐
│ [Definition] [Lifecycle & stages] [Escalation ladder] [Notices] [Enforcement] [Relief]│
└────────────────────────────────────────────────────────────────────────────────────┘
```

- **Definition** — the selector (tax / segment / industry pickers, each with "Any"), validity dates,
  status; live overlap check against existing workflows.
- **Lifecycle & stages** — the ordered stages this workflow's debt passes through (add/remove/reorder;
  this is where "VAT skips Reminder" is expressed visually).
- **Escalation ladder** — a grid of steps: seq, trigger days, notice template, enforcement instrument,
  grace, threshold.
- **Notices** — the templates referenced by the ladder (preview).
- **Enforcement** — which instruments this workflow may use (drawn from the legislative instrument
  catalogue, §7).
- **Relief & instalments** — operational relief parameters that apply to this target.

Each tab edits the underlying config **filtered to this workflow** — the tables remain the single
source of truth; the workspace is a task-first *view*, not a copy. Buildable in Joget via a parent
form with tabbed sections + child `FormGrid`s (steps), hosted as a userview FormMenu.

## 7. Operational vs legislative classification (draft — `[confirm]` the borderlines)

Split criterion: **who owns the change and how often.** *Operational* = tuned by collection management
by business judgement, periodically. *Legislative / reference* = determined by law/regulation, changes
only when the law changes, **change-controlled and audited**.

| Config item (table) | Tier | Note |
|---|---|---|
| Workflow definition + escalation ladder (`dmWorkflow`, `mmEscStep`) | **Operational** | the core knob; tuned per target |
| Per-target lifecycle / stages (`mmEntityState/Transition`) | **Operational** | workflow design |
| Relief products & instalment params (`mdRelief`, `mdProjRate`) | **Operational** | collection policy |
| Notice templates & rules (`mdNoticeRule`, templates) | **Operational** | wording/channels |
| Collection MI targets/params (`mdCollectionParam`) | **Operational** | management targets |
| Enforcement instruments + authority levels (`mdInstrument`) | **Legislative** | which legal instruments exist |
| Write-off grounds (`mdWoGround`) | **Legislative** | statutory grounds |
| Write-off delegation thresholds (`mdWoDelegation`) | **Legislative** | governance/authority |
| Write-off statutory policy / periods (`mdWoPolicy`) | **Legislative** | statutory time bars |
| Legal & agent fee schedules (`mdLegalFee`, `mdAgentFee`) | **Legislative** | set by regulation |
| Default-assessment methods & policy (`mdEstMethod`, `mdDefAssessPolicy`) | **Legislative** | statutory estimation basis |
| Publish (name-and-shame) rules (`mdPublishRule`) | **Legislative** | resolved review-2: legal basis governs |
| Violation thresholds (`mdViolation`) | **Legislative** | resolved review-2 |
| Debt category bands C1–C6 (`mdDebtCat`) | **Operational** | resolved review-2 |

Navigation: today's single **Admin** section splits into **"Collection settings"** (operational —
fronted by the Workflow workspace) and **"Legal & reference"** (legislative — read-mostly,
change-controlled). The *enforcement* of who may edit each tier is the parked roles work (#60/#91):
operational → collection-manager role, legislative → policy/legal-admin role. The **taxonomy and
labelling ship now**; the gating lands when roles resume. This split is the natural home for that
gating.

## 8. Multi-tenancy model (summary)

- Tenant = country, a first-class column on config + data (§4.2), resolved from the user profile by
  `TenantContext`, applied as an invisible `WHERE` everywhere. No "Country Profile" menu.
- Single-country users only ever see and edit their tenant; the system is **tenant-ready** even though
  only `MLT` deploys today. Cross-cutting — it touches every datalist and engine query, so it is its
  own delivery slice (Phase 1) rather than bolted onto the UI.

## 9. Migration & backward-compatibility

- The existing `STD-MLT` strategy → a workflow `(tenant=MLT, tax=ANY, segment=ANY, industry=ANY,
  validFrom=2026-01-01, validTo=open)` — the default ladder. Its `mmEscStep` rows re-parent to it.
- The VAT lifecycle override → a workflow `(tenant=MLT, tax=VAT, …)` carrying the Reminder-skipping
  stage set. Resolution + the §5 walk reproduce today's behaviour exactly (regression stays green).
- Seed sets `tenant=MLT` on all existing config + data rows.
- Generated artefacts are regenerated from specs (P5) — never hand-edited.

## 10. Delivery phasing (build AFTER sign-off)

- **Phase 1 — model + tenant + engine (headless).** `dmWorkflow`, tenant key + `TenantContext`,
  `resolveWorkflow` (selector + validity + specificity), overlap VALIDATE. Migrate seed. Acceptance:
  resolution picks the right workflow by specificity + date; overlap is rejected; VAT still skips
  Reminder; full regression green. **No UI yet** — config via the existing/seed path.
- **Phase 2 — the configuration workspace.** The tabbed feature-oriented UI (§6) over the Phase-1
  model. Acceptance: create/edit a workflow end-to-end; overlap warned; live render checks.
- **Phase 3 — navigation tiering.** Operational vs legislative split (§7). Role enforcement deferred
  to the roles track (#91).

## 11. Open decisions to confirm (`[confirm]`)

1. **Governing tax** of a consolidated debt (§4.3) — dominant-by-amount stamped at identification?
2. **Specificity tie-break precedence** (§3.3) — `tax > segment > industry`?
3. **Debt category** (§5) — keep as workflow eligibility, or promote to a 4th selector dimension?
4. **Borderline classifications** (§7) — publish rules, violation thresholds, debt categories.
5. **Segment & industry vocabularies** — confirm the value lists (segment: LARGE/SME/INDIVIDUAL?;
   industry: NACE codes or a coarser local list?).

## 12. Risks

- **Tenant key is cross-cutting** — touches every query; mitigated by doing it as one Phase-1 slice
  with the regression harness as the safety net.
- **Resolution ambiguity** — fully prevented only if the overlap rule is enforced on save AND audited
  in batch; ship both.
- **UI composition in Joget** — a tabbed multi-child workspace is more than CRUD; prototype the tab
  host early (Phase 2) before committing the full set.
- **Governing-tax choice** changes which workflow a consolidated debt gets — get §4.3 confirmed before
  Phase 1.

## 13. Review-2 resolutions (2026-06-19)

**(D2) Specificity tie-break** — confirmed `tax > segment > industry`.
**(D4) Classification** — `mdPublishRule` + `mdViolation` → **Legislative**; `mdDebtCat` (category bands) →
**Operational** (table in §7 updated).
**(D5) Segment & industry are MTCA-owned LOVs** — see §15.
**(D1) Case constitution is workflow-driven at identification** — supersedes the "governing tax" idea in
§4.3; see §14.

## 14. Case constitution (supersedes §4.3)

The selector doesn't just *route* an existing case — it determines **how a taxpayer's debt lines are
grouped into cases at identification** (DMBB-F03). So the workflow model now governs case *formation*,
not only escalation.

Key facts:
- **`segment` and `industry` are taxpayer-level** (constant across all of a taxpayer's lines) → they
  never *split* lines; they only *select* which workflow applies.
- **`taxType` is line-level.** Whether it splits depends on whether a matching workflow uses it.

**Rule:** *if the workflow that applies to a tax uses the tax dimension, that tax's lines form their own
single-tax case (principal + interest + penalties + periods for that one tax); taxes matched only by a
tax-`ANY` workflow consolidate together into one multi-tax case.*

**Identification algorithm** for taxpayer `T` owing taxes `{t1…tn}`, as-of date `d`:
1. For each `ti`, resolve the most-specific active workflow `W(ti)` (selector + validity, §3.3).
2. Taxes whose `W` has **`tax=ANY`** → their lines **consolidate into one case** under that shared workflow.
3. Each tax whose `W` is **tax-keyed** (`tax=ti`) → its **own single-tax case** under `W(ti)`.

Example — taxpayer owes VAT, CIT, FSS; a VAT-specific workflow exists, the rest match only the default:
→ one **VAT** case (single-tax) + one **consolidated** case for CIT+FSS. Matches the decision exactly.

Consequence: the Workflow object is consumed by **both** identification (F03 — grouping) and escalation
(F04 — ladder + stages). The resolver is shared.

## 15. Segment & industry vocabularies (D5)

- `segment` and `industry` are **reference LOVs maintained by MTCA** as master data
  (`mdSegment`, `mdIndustry` — operational tier), not hard-coded by DMBB.
- The **taxpayer master carries the assigned segment + industry**, populated by MTCA's own taxpayer-
  classification process. **DMBB consumes these values; it does not derive or infer them.**
- The workflow Definition tab's segment/industry pickers populate from these LOVs.
- **Dependency / assumption:** MTCA assigns segment & industry to taxpayers **consistently** upstream.
  If a taxpayer lacks a value, every selector dimension that uses it simply doesn't match → the debt
  falls through to a less-specific (or the default `ANY`) workflow. No crash, graceful degradation.

## 16. (D3) Debt category as a selector dimension — analysis

**Question:** make `debtCategory` (C1–C6) a 4th selector dimension, or keep it as a workflow
eligibility floor / per-step threshold (as today)?

**Pros of making it a selector**
- One uniform mechanism — all targeting is "selector + validity"; nothing special-cased.
- Declarative category-specific ladders (e.g. C5–C6 large debts → faster/harsher) without per-step logic.

**Cons of making it a selector**
- **Category is dynamic.** Tax, segment and industry are *stable for a case's life* (tax fixed per case;
  segment/industry are taxpayer attributes). Debt category tracks the **balance**, which changes as
  payments/penalties land — so a case could cross a band mid-life and would have to *re-resolve and
  switch workflow*, which conflicts with §14 (a case is constituted once under one workflow).
- **Breaks case-constitution stability.** Selectors decide grouping at identification; a moving
  selector means the grouping isn't stable.
- **Combinatorial blow-up + overlap-check cost** — tax×segment×industry×category multiplies the rule
  space and the per-tuple overlap matrix.

**Recommendation (proposed):** **keep `debtCategory` out of the selector.** Model it as
(a) a **workflow eligibility floor** (`categoryFloor`, as today — "this workflow only applies at C3+")
and (b) optional **per-step category thresholds** in the ladder (a step fires only at/above a category).
This preserves expressiveness (category-graded escalation) while keeping selectors **stable** so case
constitution and resolution stay deterministic. **CONFIRMED (review-2): category stays a
workflow eligibility floor + optional per-step threshold — NOT a selector dimension.**

---
**Design status: ACCEPTED (2026-06-19).** All `[confirm]` decisions resolved. Cleared for Phase 1 build.
