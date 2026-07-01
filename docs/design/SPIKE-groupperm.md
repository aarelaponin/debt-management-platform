# SPIKE (#91) — why userview GroupPermission gating denied everyone

**Date:** 2026-07-01 · **Question:** why does gating a userview category with
`org.joget.apps.userview.lib.GroupPermission` hide the category from **`admin`** (a valid,
active member of the target group) — blanking the whole console when `UV_GATING=1`?

**Answer:** it was never a DirectoryManager / group-membership-resolution problem. It is a
**property-name mismatch in our own `gen_userview` emitter.** The DX9 `GroupPermission` plugin
reads its allowed-groups list from the property **`allowedGroupIds`** (a `;`-delimited
multiselect). Our emitter wrote the list under **`groupId`** (plus a non-existent `isExclusive`).
So `getPropertyString("allowedGroupIds")` returned empty → zero tokens → the match loop never
ran → `authorized = false` for **every** user, `admin` included. Deny-all looked like "gating
works for anonymous" but was actually "gating denies everyone".

## Evidence (read-only, live jdx9)

1. **Bytecode of the running plugin.** Disassembled the loaded copy
   (`…/webapps/jw/WEB-INF/classes/org/joget/apps/userview/lib/GroupPermission.class`, the exploded
   copy takes precedence over `wflow-core-9.0.7.jar`). `isAuthorize()` body:
   ```
   groups = directoryManager.getGroupByUsername(currentUser.getUsername());
   for token in new StringTokenizer(getPropertyString("allowedGroupIds"), ";"):
       for g in groups: if g.getId().equals(token) -> authorized = true
   ```
   The only property it reads is `allowedGroupIds`. No `groupId`, no `isExclusive`.
2. **The plugin's own property definition** (`properties/userview/groupPermission.json` in
   `wflow-core-9.0.7.jar`) declares exactly two properties: `orgId` (config-UI group-picker
   filter only) and `allowedGroupIds` (`type: multiselect`). Confirms the runtime read.
3. **Our emitter was wrong.** `scripts/gen_userview.py :: category_permission()` emitted
   `{"groupId": role, "isExclusive": ""}` — neither name exists on the plugin.
4. **The directory resolves fine — same call P3 already relies on.** `getGroupByUsername` is the
   exact method `AuthorityResolver` (DAS P3) uses to resolve `admin` → DIRECTOR (run_t34/t35
   green, 2026-06-30). Live DB confirms the data it reads: `dir_group` holds the org-less role
   groups (`id == name`), `admin` is an **active** member of all of them via `dir_user_group`.
   The emitted role value (`dm_manager`, …) already equals `dir_group.id`, so once it lands in
   `allowedGroupIds`, `g.getId().equals("dm_manager")` is true.

## Why the 2026-06-19 "DirectoryManager quirk" conclusion was wrong

That probe deployed with gating on, saw `admin` denied, and inferred Joget wasn't resolving
`admin`'s flat-group membership. But the deny was upstream of any membership check: with an empty
`allowedGroupIds` the plugin returns `false` **before** comparing against the resolved groups.
P3 (a week later) then proved the very same `getGroupByUsername("admin")` resolves the groups
correctly — which is only consistent with the property-name mismatch, not a resolution failure.

## Fix (Option A — implemented)

- `category_permission()` emits `{"className":"…GroupPermission","properties":{"allowedGroupIds": role}}`
  (single group id; multiple would be `id1;id2`). `groupId`/`isExclusive` removed.
- `UV_GATING` made durable (default ON, `UV_GATING=0` disables) so faithful regens keep gating.
- Directory seed completed: the emitter's six `BUCKET_ROLE` groups now all exist —
  `dm_collection_admin` and `dm_legal_admin` were referenced but never seeded; added them with
  `admin` membership (the seed invariant is `admin ∈ all roles`, so the superuser and the
  admin-based render tests still see every category).
- Deployed via a **surgical permission-block edit** of the on-disk generated userview (add the
  `GroupPermission` block per category, keyed by bucket→role), not a full `gen_userview` regen —
  this sidesteps the UV-delta ORDER trap (DX9-DELTAS 2026-06-29) and keeps blast radius to the
  permission blocks only.

## Live signature that proves the fix (run_t40)

The three states are distinguishable by what `admin` vs anonymous see:

| state | admin sees | anonymous sees |
|---|---|---|
| broken (property bug) | 0 categories | 0 |
| open (`UV_GATING=0`) | all | all |
| **fixed gating** | **all** (member of all) | **0** (denied) |

`admin sees all AND anonymous denied` is unique to the fixed-gating state, so run_t40 asserts
exactly that. A role user (`officer1` ∈ `dm_officer` only) then lands on Operations.
