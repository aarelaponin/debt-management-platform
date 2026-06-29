# Patch — joget-req-analyst

Anchored to Step 4 (Generate Implementation Specs) and Output G (Acceptance Tests).

---

## Step 4 / Output G — STRENGTHEN: acceptance criteria must be functional & testable

> **Write every acceptance criterion as a behaviour a script can exercise and check — never as
> artefact existence.** This is the source-side fix for the DMBB over-claim defect (lists specified as
> "filterable and sortable / drill-down to the debtor list" generated none of it, and acceptance only
> confirmed the artefact deployed). Two rules:
>
> 1. **Each `T-nn.x` states the action AND the observable result**, e.g.
>    - ❌ "The debtors list is filterable and sortable." (untestable claim)
>    - ✅ "GET `…/_/list_debtorsList?fcat=C5` returns only C5 rows; `?fcat=ZZ` returns an empty list;
>      every column carries `sortable:true` in the deployed datalist json."
>    - ✅ "The Debt-by-category cell links (HyperlinkDataListAction) to `…/_/list_debtorsList?fcat=<cell>`
>      and the target shows that category's debtors."
> 2. **Do not assert a capability the generator does not emit.** If the FR wants sort/filter/drill, the
>    spec's §1 traceability must name the datalist column/filter/action that backs it, so the generated
>    test and the artefact agree. A claim with no emitting artefact is an OPEN QUESTION, not a pass.
>
> **For list-bearing FRs**, the FIS should additionally record: which columns are shown (`listColumns`
> if the form is wide), which are filterable (and the single `#requestParam#` each drill/filter uses —
> only one applies per URL), and the drill target. **For dashboard/KPI FRs (RPT-FR-003/006/007)**, route
> to `joget-dashboard-gen` (native SqlChartMenu/DashboardMenu) and record the visualisation choice as an
> ADR rather than specifying bespoke HTML.
