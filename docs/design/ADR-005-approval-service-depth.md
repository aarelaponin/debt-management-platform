# ADR-005 — Decision & Approval Service (#6) depth: one authority matrix, routed topologies

**Status:** Accepted (build in progress — slice 1 = chain + quorum routing)
**Date:** 2026-06-30
**Extends:** the minimal #6 gate (ApprovalService over `mdApprovalPolicy`, run_t30 5/5) and the
CMBB-F08 decision service (`DecisionService` over `mmAuthority`, run_t08).

## 1. Context & problem

The minimal #6 gate routes an action to **one role** above a **single threshold**
(`mdApprovalPolicy`: `actionType → threshold → authorityRole`, `topology` recorded but unused) and a
human decides with four-eyes separation-of-duties. That is enough for the instalment gate but cannot
express the real authority rules MTCA needs:

- **Bands**, not a single threshold (different authority at different materiality).
- **Rank**, not just a role name (an approver must be *at least* the required level).
- **Chains** — sequential sign-off (e.g. supervisor *then* director).
- **Quorum** — collegial N-of-M (e.g. two directors must both approve).

Meanwhile CMBB-F08 already carries a **richer** authority matrix, `mmAuthority`
(`actionType × amount band → level + bodyType + quorum`), and `DecisionService` already implements
**rank-gating** (a controlled level ladder OFFICER<…<COMMISSIONER) and **collegial quorum** — but
only for `cmDecision`, evaluated single-shot (the vote count is handed to it, not accumulated), and
without the gate machinery (Pending→Approved lifecycle, effect-fires-once) the #6 service provides.

So the platform has **two overlapping notions of "who may approve what"**: the thin `mdApprovalPolicy`
(dmbb) and the rich `mmAuthority` (cmbb). Deepening the gate while leaving both in place would deepen
the divergence.

## 2. Decision

**Unify the authority matrix on `mmAuthority`; retire `mdApprovalPolicy`.** The #6 `ApprovalService`
becomes the **orchestration layer** (request → route → accumulate decisions → fire effect once) over
the **same** authority matrix F08 reads. Rationale:

1. **Single source of truth.** "Who may approve which action at what materiality" is one question and
   must have one authoritative table. Two matrices drift.
2. **Correct layer.** The #6 service lives in the shared **cmbb spine** bundle and is reused across
   modules (instalments today, write-off next) and apps. Its core config therefore belongs in the
   spine (`mmAuthority`), not in the dmbb consumer app (`mdApprovalPolicy`).
3. **Reuse, not duplication.** `mmAuthority` already carries bands + `level` + `bodyType` + `quorum`,
   and `DecisionService.rank()` already ranks the level ladder. The deepened gate reuses both.
4. **Expressiveness.** Bands + rank + bodyType + quorum natively support single / chain / quorum;
   a single threshold→role cannot.

F08's `cmDecision` path is untouched: changes to `mmAuthority` are **additive** (new INSTALMENT rows,
an optional `chain` column F08 ignores), so run_t08 stays green.

## 3. The routing model

`ApprovalService.resolveRoute(actionType, materiality)` reads the `mmAuthority` band whose
`[amountMin, amountMax]` contains the materiality and returns a **Route**:

| Topology | mmAuthority shape | Gate behaviour |
|---|---|---|
| **(none)** | no band matches the materiality | **auto-pass** — no approval, effect runs immediately |
| **SINGLE** | `bodyType` ≠ COLLEGIAL, no `chain` | one decision by an approver ranked ≥ `level` → effect |
| **CHAIN** | `chain` = ordered CSV of levels (e.g. `SUPERVISOR,DIRECTOR`) | sequential: each step needs an approver ranked ≥ that step's level; effect runs **only after the last step** |
| **QUORUM** | `bodyType` = COLLEGIAL, `quorum` = N | **N distinct** approvers each ranked ≥ `level`; a repeat voter is ignored; effect runs at the **Nth** |

Invariant rules that hold for every topology: **mandatory reason**; **separation of duties**
(no approver may be the requester; in a chain, each step is a distinct approver); a **reject at any
step rejects the whole request**; the **DecisionEffect fires exactly once**, only on full completion;
every step writes a reasoned `APPROVAL_DECISION` to the case hash-chain; the request lifecycle is
guarded + audited by `StatusManager` (`Pending → Approved | Rejected | Returned`).

### 3.1 Route state on `cmApproval`

The request row carries the route and its progress (all engine-managed):
`routeKind` (SINGLE/CHAIN/QUORUM), `requiredLevel`, `chain` (CSV), `currentStep` (chain cursor),
`quorum` (N), `approvalsCount` (quorum tally), `voters` (CSV of distinct approver ids, for dedup).

### 3.2 The approver's level

Each decision (`cmApprovalDecision`) carries `approverLevel` — exactly as F08's `cmDecision` does —
so the rank gate is `rank(approverLevel) ≥ rank(requiredLevel)` (reusing `DecisionService.rank()`).
At the live UI the level is the approver's profile level; in tests it is supplied on the decision.

## 4. Consequences

- **Matrix admin moves to the cmbb console.** `mmAuthority` is a cmbb form; the dmbb console cannot
  CRUD it. The dmbb **Approvals** bucket therefore becomes **inbox + Decide** only (the operational
  surface); the authority matrix is administered in the cmbb (spine) console alongside the rest of the
  decision config — a cleaner operational/spine separation.
- `mdApprovalPolicy` (form + seed + the "Approval policy" menu) is **removed**; its instalment band is
  re-expressed as `mmAuthority` rows. `run_t30` seeds `mmAuthority`.
- **Slice plan.** Slice 1 (this build): chain + quorum routing + rank gate on the unified matrix.
  Slice 2: escalation / delegation / timeout (reusing the F05 DeadlineEngine clock + a sweep).
  Slice 3: Conflict-of-Interest (#3), reusing F08's `EXCLUDE_DECISION_MAKER`. Then migrate write-off
  off its bespoke approval onto the gate.

## 5. Alternatives considered

- **Extend `mdApprovalPolicy`** with level/bodyType/quorum/chain. Rejected: keeps two authority
  matrices (drift), and puts the shared service's core config in a consumer app (wrong layer).
- **Fold #6 into F08's `DecisionService`.** Rejected: `DecisionService` is a single-shot evaluator
  for `cmDecision`, not a gate; it has no Pending lifecycle, no effect-once, no vote accumulation. The
  #6 service is the orchestration the platform was missing; it consumes the same matrix instead.
