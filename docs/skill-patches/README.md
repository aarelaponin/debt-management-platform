# Joget lifecycle skill patch pack — DMBB QA lessons (2026-06-14)

Distilled from the DMBB build + the three UX-QA rounds + the dashboard/architecture work. Each
`*.patch.md` is a set of **paste-ready, additive** edits anchored to a section of that skill's current
`SKILL.md`; `joget-dashboard-gen.SKILL.md` is a brand-new skill draft.

## How to apply (important)
These patches target the **skill source** you maintain (the plugin behind Settings → Capabilities) —
NOT the read-only skill cache a running session sees. Apply them by editing your `joget-*` plugin's
`SKILL.md` files (and adding the new skill folder), then re-publishing the plugin. Editing the cache
in a session does not persist.

Most of the *granular* facts already live in this repo's `docs/DX9-DELTAS.md` (the running ledger the
skills point at). The point of these patches is to lift the **repeatable patterns** up into the skills
so the next building block (AMBB/APBB/RFBB…) never re-learns them.

## What each patch carries
| Skill | Lesson theme |
|---|---|
| `joget-datalist-gen` | emit real (not claimed) sortable/filter/drill; clean labels; `listColumns` curation; one-`requestParam`-per-URL; companion lists have no `/_/<listId>` URL |
| `joget-userview-gen` | menu organisation (Operations category, retire single-trigger); GroupPermission needs directory-API membership; native dashboard/chart menu types |
| `joget-feature-loop` | **functional** acceptance (the over-claim root cause); order-independent stateful suites (`drain` + relative asserts); canonical regression runner |
| `joget-plugin-dev` | ClickHouse JDBC slf4j/Apache-HC5 trap → pin `http_connection_provider` |
| `joget-req-analyst` | write acceptance criteria as functional, testable assertions |
| `joget-deploy` | restart after import for cached userview/API defs; cold-start for a clean JVM in regression |
| `joget-dashboard-gen` (**new**) | the native `SqlChartMenu`/`DashboardMenu` dashboard pattern + ADR habit |

## The one meta-lesson (worth a line in every skill's intro)
**Never claim a capability an artefact does not back, and prove behaviour functionally.** The single
costliest QA finding was lists described as "filterable / sortable / drill-down" that emitted none of
it, passing because acceptance only checked the artefact existed. Generators must emit the capability;
acceptance tests must exercise it.
