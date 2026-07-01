# ADR-006 — Finalising the Decision & Approval Service into a comprehensive, platform-shared component

**Status:** ✅ **DELIVERED** — DAS v1.0 complete (P0–P6 all shipped green; see `DAS-v1.0-Readiness.md`).
**Progress:** P0 ✅ (spike + integration contract, `4eb4287`) · P1 ✅ (config-ify SLA/escalation/effective-dating, `f70ec2d`) · P2 ✅ (write-off is the 2nd live consumer — `WRITE_OFF` effect = `WriteOffService.applyApproved`, `submit()` raises the gate request, bespoke `cmWriteOffApprove` engine path retired; run_t33 4/4 + trimmed run_t18 6/6 + full regression t02–t33+t37 **33/33 green**). P3a ✅ (directory-resolved approver identity — `AuthorityResolver` resolves the deciding officer's rank level from their Joget **directory** role-groups via the directory API + the `mmRoleLevel` map, **API-only**, no `dir_*` SQL; the self-declared `approverLevel` is retired to an automation/test override; 6 resolver unit tests + run_t34 2/2 + full regression **34/34 green**). P3b ✅ (per-user "mine to decide" inbox — `ApprovalInboxBinder`, a custom Joget `DataListBinder` that resolves the logged-in user's level through the same `AuthorityResolver` and returns only the requests they may decide via the pure, unit-tested `ApprovalInbox` rule (rank gate, four-eyes, delegation); wired as the lead menu of the Approvals category; **API-only**, no `dir_*` SQL; 7 inbox unit tests + run_t35 3/3 live — rendered as admin (DIRECTOR resolved from the directory) it surfaces a DIRECTOR-required and a SUPERVISOR-required request; full regression t02–t35+t37 **35/35 green**). **P3 complete.** P5 ✅ (governance & integrity — **auto-derived COI**: when an `mmCoi` `EXCLUDE_DECISION_MAKER` rule is in force, one person may not decide more than one request for the same taxpayer, derived from the decision record; and **authority-matrix validation**: the `cmAuthorityCheck` trigger runs `MatrixValidator` over an action's `mmAuthority` bands — level-exists / ascending-chain / quorum / overlap / gap — so a bad config is caught before it routes; 12 new unit tests + run_t38 3/3 live + full regression t02–t38 **37/37 green**). P4 ✅ (workflow completeness — **delegation now binds**: once a request is delegated, only the named delegate may decide it, enforced in `decide()`; and the lifecycle emits **notifications** (NOTIF_PENDING → the F06 dispatcher → cmAlert) on assignment/escalation/timeout/delegation, recipients resolved via the mmRoleLevel reverse map; 4 new unit tests + run_t36 3/3 live (non-delegate blocked, bound delegate decides, cmAlert dispatched) + run_t31 updated for the bound semantics; full regression t02–t36+t37 **36/36 green**). P5–P6 pending.
**Post-v1.0 cleanup — DONE (2026-07-01):** the vestigial `cmWriteOffApprove` "Write-off approvals" userview menu node (retired in the UV-delta spec since P2, a harmless no-op after the effect migration) has now been surgically removed from the deployed `dmbbConsole` (order-preserving edit of the gitignored `generated/userviews/dmbbConsole.json` — 68→67 menus, 7 categories; the stale ORDER in `RESUME-dashboard-polish.md` was **not** used, per its own warning). dmbb re-imported + published, Tomcat restarted, deployed userview confirmed clean, and **full regression t02–t39 38/38 green** (the userview-touching t25/t29/t39 all green). Closes the P2 "retire the bespoke path" deferral end-to-end.
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
