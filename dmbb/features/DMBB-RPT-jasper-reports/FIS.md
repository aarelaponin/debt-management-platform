# FIS — DMBB-RPT — Jasper report visual tier (printable operational reports)
Status: Accepted (T-22.1..6 6/6 PASS on jdx9, 2026-06-13 — all 6 reports render typeset HTML + PDF/Excel, no exceptions; full regression run_t02..t21 green). RPT track over the F09/F11/F12 data tier.
CAD ref: RPT-FR subset (001/002/005/008/011/012/014). Builds on F11 (list_debtorsList), F12 (MI datalists), F09 (list_dmWriteOff).

## 0. Approach — JasperReportsMenu, config-as-code through the existing loop
A datalist is a sortable grid; a *report* adds a titled header with run date, grouped sections, ruled subtotals,
a bottom line, and true PDF/Excel. Joget Enterprise renders these from a `JasperReportsMenu` whose `.jrxml`
ships inline — no GUI designer, no Java build. F11/F12 already proved the exact SQL each report needs as JDBC
datalists; this track promotes that SQL into six typeset reports.

**Platform follow-on (built first):** `gen_userview.py` gains a `jasper` menu emitter — a UV-delta menu
`{type: jasper, customId, label, jrxmlFile}` inlines the `.jrxml` into a JasperReportsMenu (datasource ""
= profile DB, output html, export "pdf;xls", `parameters: []`). So reports stay config-as-code and ship through
the normal gen_userview → build_jwa → deploy_jwa loop, regenerated deterministically (P5). The `.jrxml` files are
the source of truth under the feature's `reports/`.

The six reports (1:1 with the proven datalists):
| Report | RPT-FR | Source SQL (proven as) | Sections / totals |
|---|---|---|---|
| Debt Aging Report | RPT-FR-001/008 | list_debtAging (F12) | age bands 0-30…>365, cases + total debt, grand total |
| Tax Debt Status Report | RPT-FR-014 | list_debtByCategory (F12) | per category: cases, total, avg; grand total |
| Recovery by Action Report | RPT-FR-005 | list_recoveryByAction (F12) | per instrument: actions, executed, exec-rate, recovered; total |
| Instalment Compliance Report | RPT-FR-011 | list_instalmentCompliance (F12) | per plan status: agreements, total debt; grand total |
| Write-Off Report | RPT-FR-012 | list_dmWriteOff (F09) | per write-off type/ground: count, amount; grand total |
| Debtors List Report | RPT-FR-002 | list_debtorsList (F11) | per-debtor rows (TIN/name/debt/age/category/stage); count + total |

## 1. Traceability
| RPT-FR | AC (paraphrase) | Realised by | Test |
|---|---|---|---|
| RPT-FR-001 / 008 | Debt aging across configurable age bands with totals reconciling to balances | rptDebtAging.jrxml (bands + grand total) | T-22.1 |
| RPT-FR-014 | Tax Debt Status — debt amounts by category / tax type | rptTaxDebtStatus.jrxml | T-22.2 |
| RPT-FR-005 | Collection performance — actions by type, recovery amounts | rptRecoveryByAction.jrxml | T-22.3 |
| RPT-FR-011 | Instalment monitoring and compliance | rptInstalmentCompliance.jrxml | T-22.4 |
| RPT-FR-012 | Write-off volumes, reasons, amounts | rptWriteOff.jrxml | T-22.5 |
| RPT-FR-002 | Debtors list — all taxpayers with outstanding balances, printable | rptDebtorsList.jrxml | T-22.6 |

## 2. Design decisions
1. **A1 — reports reuse the datalist SQL, promoted to grouped/totalled form.** Same WHERE semantics (open DM,
   not written-off, not terminal) so a report and its datalist always reconcile.
2. **A2 — jrxml rules (joget-jasper-report).** `language="java"` (Groovy 2.4 throws ClassCastException on JDK21);
   `parameters: []` as a JSON array (never ""); `datasource: ""` = profile PostgreSQL DB; `export: "pdf;xls"`
   (a `;`-string, the one non-array multi-value prop); no checked-exception expressions (no `parse(...)`).
3. **A3 — bottom-line via UNION ALL + sort key.** Grand-total rows are extra SQL rows (`line_type='TOTAL'`,
   high sort) bolded by a `<conditionalStyle>` keyed on `line_type` — the skill's section-subtotal pattern.
4. **A4 — no parameters in v1.** Direct menu click renders the full current picture; an as-of/period date-picker
   header (`#requestParam#` mapping) is a recorded refinement.

## 3. Deferred / assumptions (recorded)
- Interactive dashboard tier (RPT-FR-003 KPI traffic-lights + sparklines, RPT-FR-006/007 multi-stream + role
  hierarchy, drill-down, <10s SLA) — a BI front-end concern Jasper does poorly; out of this tabular-report track.
- Configurable age bands (RPT-FR-001) and as-of/period parameters — recorded refinements (the SQL CASE bands
  and a customHeader date-picker).
- Staff productivity (RPT-FR-013) needs an officer/assignee dimension that is thin in DEV data — recorded.

## 4. Generation order
1. gen_userview follow-on: add the jasper emitter (done first, validated with Debt Aging).
2. Author reports/*.jrxml (6) + UV-delta Reports category.
3. gen_userview ALL dmbb UV-deltas (F01..F12 + RPT) → dmbbConsole gains a **Reports** category.
4. build_jwa + deploy_jwa (no plugin change — JasperReportsMenu is Enterprise-bundled). run_t22 + full regression.
