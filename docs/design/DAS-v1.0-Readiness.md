# Decision & Approval Service (#6) — v1.0 Readiness Gate

**Status:** ✅ **READY** · 2026-07-01 · Gate for closing the DAS as a comprehensive, reusable,
platform-shared approval service (ADR-006). Each row is the ADR §4 "definition of done" mapped to the
phase that delivered it, the live acceptance test, and the commit — every phase ended with the full
regression green.

## Definition-of-done checklist

| # | Capability (ADR-006 §4) | Phase | Live proof | Commit | Status |
|---|---|---|---|---|---|
| 1 | **Two live consumers** through one engine + one authority matrix | P2 | run_t33 (write-off) + run_t30/t31 (instalments) | `d0a41ff` | ✅ |
| 2 | **Zero hard-coded policy** — SLA / max-escalations / effective-dating are config | P1 | run_t37 (config SLA + effective-dated band switch) | `f70ec2d` | ✅ |
| 3 | **Directory-resolved approver identity** (not self-declared) | P3a | run_t34 (no resolvable authority ⇒ blocked) | `d3838d6` | ✅ |
| 4 | **Per-user "mine to decide" inbox** | P3b | run_t35 (admin sees only their eligible requests) | `34483ca` | ✅ |
| 5 | **Delegation bound** — the delegate becomes the required decider | P4 | run_t36 (non-delegate blocked, delegate decides) | `a064bc1` | ✅ |
| 6 | **Lifecycle notified** — assignment / escalation / timeout / delegation | P4 | run_t36 (NOTIF_PENDING → cmAlert dispatched) | `a064bc1` | ✅ |
| 7 | **COI declared + auto-derived** | P5 | run_t32 (declared) + run_t38 (auto, same-taxpayer bar) | `9cf4671` | ✅ |
| 8 | **Authority-matrix validated** (overlap / gap / chain / quorum / level) | P5 | run_t38 (bad config flagged, clean config passes) | `9cf4671` | ✅ |
| 9 | **Oversight dashboard** — turnaround / breaches / escalations / pending-by-authority | P6 | run_t39 (Approvals MI charts render, server-side SQL) | this phase | ✅ |
| 10 | **Documented integration contract** — one effect + matrix rows onboards the next module | P0 / P6 | `DAS-Integration-Contract.md` v1.0 | `4eb4287` | ✅ |
| 11 | **Full regression green at every phase** | all | t02–t39 sweep green after each phase | — | ✅ |

## What "reusable" means here, concretely

A new module becomes an approval consumer with **two seams and zero engine changes** (see
`DAS-Integration-Contract.md`): register one `DecisionEffect` line, and raise `request(…)` from its
lifecycle. Everything else — the authority matrix, directory identity, the inbox, SoD, COI (declared
+ auto), SLA escalation/timeout, delegation binding, notifications, matrix validation, the reasoned
hash-chain audit and the MI dashboard — is inherited. Write-off (P2) is the worked proof: it onboarded
by adding exactly that, and its bespoke approval flow was retired.

## Test surface (approval-specific)

- **Unit:** ApprovalServiceTest (30), AuthorityResolverTest (6), ApprovalInboxTest (7),
  MatrixValidatorTest (9), WriteOffServiceTest (9) — part of the cmbb-plugins 197-test suite, all green.
- **Live acceptance:** run_t30 (routing/topologies), t31 (escalation/timeout/delegation), t32 (declared
  COI), t33 (write-off reuse), t34 (identity safety), t35 (inbox), t36 (delegation binding +
  notifications), t37 (config + effective-dating), t38 (auto-COI + matrix validation), t39 (MI
  dashboard) — inside the full **t02–t39 regression, green**.

## Known deferrals (out of the v1.0 bar, tracked)

- **Per-role landing-page gating** (#60 / #91) — ✅ **DONE (2026-07-01)**: never a directory-resolution
  quirk — a property-name bug in our emitter (`groupId` vs the plugin's `allowedGroupIds`). Fixed;
  the six console categories are now GroupPermission-gated and land each role on its first visible
  category. Live-proven by run_t40 (admin ∈ all groups sees all; anonymous denied). See
  `docs/design/SPIKE-groupperm.md`.
- **cmWriteOffApprove console menu removal** — ✅ **DONE (2026-07-01)**: the vestigial "Write-off
  approvals" menu node has been removed from the deployed `dmbbConsole` (68→67 menus) and the deployed
  userview confirmed clean; full regression t02–t39 **38/38 green**. See ADR-006 "Post-v1.0 cleanup".
- **Extraction to a standalone cross-project JAR** — deliberately deferred (ADR-006 §5): re-opens as a
  future ADR now that ≥2 in-platform consumers have battle-tested the contract.
