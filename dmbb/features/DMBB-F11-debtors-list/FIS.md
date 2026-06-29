# FIS — DMBB-F11 — Debtors list (officer view + registry extract)
Status: Accepted (T-20.1..4 4/4 PASS on jdx9, 2026-06-13; 122/122 unit; full regression run_t02..t19 green). DMBB slice S5 (F11).
CAD ref: CAD-DMBB §7 row F11 (debtors-list publisher + policy + extracts); DM-FR-057, 058. Builds on F03 (DM case + dmDebt), F07 (dmDebtorPub publish/RELEASE sweep + mdPublishRule), CMBB spine.

## 0. Approach — a JDBC list view + a registry-extract engine (reuse F07's publish machinery)
DM-FR-057 is fundamentally a **view**: a live, filterable/sortable list of every taxpayer with outstanding
debt. So F11's Must is a **JDBC datalist** `list_debtorsList` joining the spine (`app_fd_cmcase`) to the DMBB
subject (`app_fd_dmdebt`), with native Joget CSV/Excel export. DM-FR-058 (publish to website / national
registry) is **already** shipped by F07 (the `dmDebtorPub` publish/RELEASE sweep over `mdPublishRule`); F11
adds only the missing piece — a **registry extract** (the "send the list … via file transfer or API" hand-off)
via one new engine **DebtorsListEngine** (17th) / **DebtorsListService**, mode EXTRACT:
1. **list_debtorsList (DM-FR-057)** — JdbcDataListBinder: TIN, taxpayer, total debt, debt age (oldest =
   case age), category (C1–C5), enforcement stage, risk score, assigned officer; excludes written-off
   (F09) and terminal cases; ORDER BY debt desc. Combinable filters on category / enforcement stage /
   tax type. Export is the datalist's native CSV/Excel. Amount = the `dmDebt` snapshot (refreshed from ORS
   by F03/F09 — "real-time refresh from balance data").
2. **EXTRACT (`dmDebtorsExtract` create, DM-FR-058)** — gather the currently `PUBLISHED` `dmDebtorPub`
   entries (the website-published list F07 maintains), produce a registry extract: `debtorCount`,
   `totalAmount`, and a delimited `payload` (TIN|name|amount per line) tagged with the registry + format;
   status `GENERATED`, `DEBTORS_EXTRACT_GENERATED` event. The actual file transfer / API push to the
   national registry is external (deployment binding) — the extract is the audited hand-off record.

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-057 | "maintain a debtors list viewable by authorised MTCA officers, showing all taxpayers with outstanding debts, filterable and sortable by: debt amount, debt age, tax type … enforcement stage, and risk score. Debtors list displays: TIN, taxpayer name, total debt, debt age (oldest), category (C1–C5), enforcement stage, risk score, assigned officer. … Export to Excel/CSV supported. Real-time refresh from taxpayer balance data." | list_debtorsList JDBC datalist (join cmCase+dmDebt; columns + combinable filters; native CSV/Excel export; amount from the ORS-refreshed dmDebt snapshot) | T-20.1, T-20.2 |
| DM-FR-058 | "configurable to automatically publish a list of qualifying debtors on the MTCA website and/or send the list to any external national centralised debt registry. … List generated automatically at configured intervals. Published list updated when debtor resolves debt. Integration with national registry via file transfer or API." | website publish = F07 (dmDebtorPub PUBLISH/RELEASE over mdPublishRule); F11 DebtorsListEngine EXTRACT builds the registry extract (count/total/payload) from the PUBLISHED set, excluding resolved/REMOVED entries | T-20.3, T-20.4 |

## 2. Design decisions
1. **A1 — the list is a JDBC view, not a stored table.** No new entity for DM-FR-057; the truth is the live
   join of cmCase+dmDebt. The datalist is the report surface (and the export). Filter ids = SQL aliases
   (GAM JdbcDataListBinder precedent, same as F04 list_queue).
2. **A2 — don't duplicate F07.** The website-publication lifecycle (qualify → publish → remove on resolution)
   is F07's `dmDebtorPub` + `mdPublishRule` + the PUBLISH/RELEASE sweep. F11 reuses it verbatim and only adds
   the registry **extract** (DM-FR-058's "external national registry … file transfer or API") — the piece F07
   didn't build. mdPublishRule already carries the criteria (min amount/age, prior notice, registry).
3. **A3 — extract is an audited hand-off (SAD I-4 style).** EXTRACT records the count/total/payload + event;
   the transport (SFTP/REST to the registry) is bound at deployment. "Updated when debtor resolves" falls out
   of reading only PUBLISHED dmDebtorPub (F07's RELEASE sweep flips resolved ones to REMOVED).
4. **A4 — debt age = case age (oldest).** DM-FR-057 "debt age (oldest)" — the DM case's `dateCreated` is the
   oldest-debt anchor (identification consolidates per TIN at case creation); per-line assessment dates are a
   later refinement (recorded).

## 3. Deferred / assumptions (recorded)
- Filters on **taxpayer type** and **economic sector** (DM-FR-057) — those attributes live in Registration,
  not in DMBB; the list exposes the columns we hold (category, stage, tax type, risk, amount, age) and the
  sector/type filters are a registration-join refinement (recorded). Risk score is shown when present
  (SAS-fed via the queue view, D-SAD-07).
- The actual file transfer / API push of the extract to the national registry is external (deployment binding).
- Scheduled interval generation (DM-FR-058 "at configured intervals") = a cron POST of dmDebtorsExtract (the
  trigger-row pattern); the cadence is the schedule, not new code.

## 4. Configurables → carriers
- Publication criteria (DM-FR-058) → `mdPublishRule` (F07 — min amount/age, prior notice, registry).
- Extract registry + format → `dmDebtorsExtract` fields (per-run).
- List columns/filters (DM-FR-057) → the `list_debtorsList` datalist definition.

## 5. Generation order
1. gen_forms: F-dmDebtorsExtract → dmbb/generated/forms.
2. gen_datalists (forms) → list_dmDebtorsExtract; gen_datalists (custom datalists dir) → list_debtorsList (JDBC).
3. gen_userview: ALL dmbb UV-deltas (F01..F11) → dmbbConsole gains a **Debtors list** category (datalist menu + extract crud).
4. Plugin: DebtorsListService + DebtorsListEngine (EXTRACT) + debtorsListEngine.json; Activator 16→17; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb (no new seed — reuses F07 mdPublishRule). run_t20 + full regression run_t02..t19.
