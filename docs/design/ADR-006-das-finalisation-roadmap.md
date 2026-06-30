# ADR-006 — Finalising the Decision & Approval Service into a comprehensive, platform-shared component

**Status:** Accepted (roadmap — phase build in progress).
**Progress:** P0 ✅ (spike + integration contract, `4eb4287`) · P1 ✅ (config-ify SLA/escalation/effective-dating, `f70ec2d`) · P2 ✅ (write-off is the 2nd live consumer — `WRITE_OFF` effect = `WriteOffService.applyApproved`, `submit()` raises the gate request, bespoke `cmWriteOffApprove` engine path retired; run_t33 4/4 + trimmed run_t18 6/6 + full regression t02–t33+t37 **33/33 green**). P3–P6 pending.
**Deferred (cosmetic, P6 cleanup):** the live `cmWriteOffApprove` userview menu node is retired in the UV-delta spec but not yet removed from the deployed `dmbbConsole` (it is now a harmless no-op — its post-processor branch is gone). Remove it on the next faithful dmbb userview regen with the verified UV-delta ORDER (the stale ORDER in `RESUME-dashboard-polish.md` predates DMBB-APPROVAL-gate + DMBB-WF and must not be used as-is).
**Date:** 2026-06-30
**Builds on:** ADR-005 (DAS depth — routing, escalation/timeout/delegation, COI). The engine, the
unified `mmAuthority` matrix, `mmCoi`, and the lifecycle are live and green (regression 2..32).

## 1. Context

The DAS is a working, reusable-by-design approval gate with a broad capability surface, but it is
"comprehensive in mechanism, early in adoption": one live consumer (instalments), some policy still
in code constants, a self-declared approver identity, no per-user inbox, no notifications, declared-
only COI, and no oversight surface. This ADR records the decisions that finalise it.

## 2. Decisions

1. **Reuse boundary = platform-shared service.** The DAS stays inside the CMBB spine bundle; it is
   **not** extracted into a standalone cross-project JAR. "Reusable" therefore means a *hardened,
   documented integration contract* plus *≥2 live consumers* proving it — not portability beyond the
   platform. A new module becomes a consumer by adding one `DecisionEffect` registration and the
   `mmAuthority`/`mmCoi` rows for its action — **no engine change**.
2. **v1.0 scope = all four capability clusters:** production-readiness core (config-ify all policy
   params + directory-resolved identity + per-user inbox); workflow completeness (delegation binding
   + lifecycle notifications); governance & integrity (auto-derived COI + matrix validation); proof &
   oversight (write-off as the 2nd consumer + an approvals MI dashboard).
3. **Sequencing = reuse-proof-early + risk-first.** Migrate write-off as soon as the contract is firm
   (phase 2) so the 2nd consumer validates the contract before more is built on it; spike the one real
   unknown — Joget directory/group-membership resolution behind a per-user inbox (the twice-deferred
   #91) — in phase 0 rather than discovering it late.

## 3. The roadmap (each phase ends green: unit + a live run_tNN + full regression + commit)

| Phase | Delivers | Proof | Risk |
|---|---|---|---|
| 0 · De-risk + contract backbone | Spike the directory/group-membership path for a per-user inbox; formalise the consumer integration contract (one documented effect-registration point + an integration guide) | Spike memo; contract doc; regression stays green | Spike may surface a Joget workaround |
| 1 · Config-ify policy | SLA length, max-escalations, and effective-dating moved from code constants into `mmAuthority` (resolveRoute honours the row valid at request time) | run_t30/t31 seed SLA from config + an effective-dated band-switch case | Low |
| 2 · Reuse proof — write-off | Migrate DMBB-F09's bespoke `cmWriteOffApprove` onto the gate (register `WRITE_OFF` effect, raise the request from submit, retire the bespoke path) | run_t33 + F09's test + regression — **2nd live consumer** | Medium |
| 3 · Identity + inbox | Directory/`mdOfficerProfile`-resolved approver level/role (not self-declared); per-user "mine to decide" inbox; closes #91/#60/#90 | run_t34 (request shows only to an eligible approver; ineligible cannot decide) | **Highest** (gated by phase-0 spike) |
| 4 · Workflow completeness | Delegation binding (the delegate becomes the required decider); notifications on assignment / escalation / timeout via the F06 dispatcher | run_t35 (delegate-only decides; cmNotif emitted) | Low–medium |
| 5 · Governance & integrity | Auto-derived COI (exclude whoever already approved a decision on the same TIN) on top of declared COI; `mmAuthority` validation (overlap / gap / ascending chain / quorum / level-exists) | run_t36 (auto-COI block + matrix-check flags a bad config) | Low |
| 6 · Oversight + close | Approvals MI dashboard (turnaround, breaches, escalations, pending-by-authority); integration guide + config reference; v1.0 readiness gate | Dashboard renders live; readiness checklist green | Low |

## 4. Definition of done — "DAS v1.0, comprehensive platform-shared service"

Two live consumers (instalments + write-off) through one engine and one authority matrix; zero
hard-coded policy params with effective-dated config; directory-resolved approver identity and a
per-user inbox; delegation bound and the lifecycle notified; COI both declared and auto-derived with a
validated matrix; an oversight dashboard; and a documented integration contract so the *next* module
onboards by adding one effect + matrix rows. Full regression green at every phase.

## 5. Alternatives considered

- **Extract to a standalone cross-project JAR now** (like joget-form-prefill). Deferred, not rejected:
  re-opens as a future ADR once ≥2 in-platform consumers have battle-tested the contract — extraction
  is cheap then and risky now.
- **Defer write-off to last.** Rejected: proving reuse early is the point; a contract validated by a
  real 2nd consumer up front prevents building phases 3–6 on an unproven seam.
