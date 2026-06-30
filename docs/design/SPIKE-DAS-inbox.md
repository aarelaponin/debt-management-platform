# SPIKE (DAS P0a) — per-user "mine to decide" inbox & approver identity

**Date:** 2026-06-30 · **Question:** can the per-user approval inbox + the decide-time rank gate
resolve the current user's authority **without** the unresolved GroupPermission render blocker (#91)?

**Answer: yes.** Phase 3 is feasible with proven patterns; the GroupPermission issue is side-stepped.

## Findings (live jdx9)

1. **`#currentUser.username#` resolves in a JDBC datalist binder.** Proven by the F04 worklist:
   `… WHERE c.c_assignee = '#currentUser.username#'`. So a per-user inbox is a *datalist filtered in
   SQL on the logged-in user* — it does **not** need category GroupPermission (the thing that doesn't
   resolve, #91). The render blocker only ever affected per-role *landing/category gating*.
2. **Directory model:** `dir_user_group` maps user→group (admin ∈ all five; officer1/officer2 ∈
   `dm_officer`,`cmbb_user`). Groups are **role groups**: `dm_officer / dm_supervisor / dm_manager /
   dm_policy_admin / cmbb_user`.
3. **`mdOfficerProfile` has no level** (cols: code, name, unit, active, alertPrefs, taxTypes,
   geography, maxCapacity). So an approver's *rank level* is not stored per officer.
4. **The bridge to build:** the authority matrix speaks **rank levels** (OFFICER<…<COMMISSIONER); the
   directory speaks **role groups**. They must be bridged by a small **role-group → level map** (new
   config, e.g. `dm_officer→OFFICER, dm_supervisor→SUPERVISOR, dm_manager→MANAGER, dm_policy_admin→
   DIRECTOR`), carrying the rank ordinal so SQL/engine can compare.

## Chosen approach for Phase 3

- **Approver identity (decide path).** `ApprovalGateEngine` resolves the approver's level from their
  `dir_user_group` membership via the role→level map (highest mapped level), instead of trusting the
  self-declared `approverLevel`. Keep the declared field as an **explicit override** only for
  automation/tests (the live system resolves; tests with the single API identity supply it).
- **Per-user inbox.** A JDBC datalist `list_cmApproval_my`: Pending `cmApproval` where the current
  user's resolved level (join `dir_user_group` → role→level map) `≥ requiredLevel`, **and**
  `requestedBy ≠ currentUser`, **and** not COI-barred, **and** (if `delegatedTo` set, `= currentUser`).
- **Testing.** Decide-path level resolution: `run_t34` seeds the test identity's `dir_user_group`
  row + the role→level map, asserts the gate resolves the level (no longer needs the declared field).
  Inbox rendering: assert the binder SQL returns the right rows for a substituted username, and/or a
  logged-in puppeteer session (reuse the #106 capture harness) shows only eligible rows.

## Scope note

Per-role **landing pages** (#60 / #90, the GroupPermission category gating) stay deferred — they are
**not** required for the per-user inbox and are out of the DAS v1.0 bar. The inbox does not depend on
them.
