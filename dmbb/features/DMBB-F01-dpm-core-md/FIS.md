# FIS — DMBB-F01 — DPM core carriers + MLT seed (D1/D2/D3)
Status: Accepted (T-10.1..4 4/4 PASS on jdx9, 2026-06-12; CMBB regression run_t02..t09 green 8+7+6+7+7+5+8+8). First DMBB feature — created the `dmbb` app (ADR-001 A1×B1); proves two-app isolation (new app + tables, zero CMBB disturbance).
CAD ref: CAD-DMBB §7 row F01 (DPM carriers + MLT seed, D1/D3 base + D2 schema); DPM-1/3 (DM-Configurability-Analysis).

## 0. Packaging (ADR-001 A1×B1)
This is a **config/seed-only** feature: master-data carrier forms + datalists + a DMBB-admin userview + MLT seed.
No case types, no case screen, no plugins, no workflow → it needs **none** of the F02 platform follow-ons
(SubForm/FormGrid/process-start generators, CaseSubjectFormElement). It is generated into `dmbb/generated/`
and deployed as the second Joget app `dmbb` (spine/envelope/engines stay in `cmbb`). The DM **case type**
registration into the CMBB metamodel is DMBB-F02, not here.

## 1. Traceability
| FR / BR | AC (verbatim from M08 §4.x) | Realised by | Test |
|---|---|---|---|
| DM-FR-004 | "automatically classify new debt into categories based on configurable amount thresholds: C1 (<€30), C2 (€30–100), C3 (€100–1,000), C4 (€1,000–20,000), C5 (€20,000–200,000), with an implied C6 for amounts >€200,000. Thresholds must be configurable." | F-mdDebtCat (code, minAmount, maxAmount, band order) + MLT seed C1–C6 (thresholds 30/100/1000/20000/200000) — the **configurable** carrier; the runtime classification + re-evaluation is DMBB-F03 (DebtIdentificationJob). Aligned with `sta_v1.debt_balances`/`debt_priority_queue` banding (GOLD-MOCK §2.2) so GoldMartClient's `debtCategory` matches. | T-10.1 |
| BR-DM-003 | "Debts classified into categories based on consolidated owed amount … Category determines available enforcement actions." | F-mdDebtCat (bands) + F-mdInstrument.minCategory (the "category → available actions" mapping as data) | T-10.1, T-10.2 |
| BR-DM-031 | "enforcement action applicability by category … Table 4 proportional enforcement: C2 — demand notice, refund interception; C3 — plus phone calls, payment arrangements, bank garnishing, publishing name; C4 — plus taxpayer visits, lien on assets, third-party claims, passport seizure, property seizure, bankruptcy demand; C5 — all" | F-mdInstrument (the Malta-14 instrument catalogue **as data, not enum**, each with `minCategory`, `executionMode`, `authorityLevel`, `enabled`) + MLT seed (14 enabled, foreign present-disabled per DPM-3) | T-10.2 |
| DM-FR-009…012 (config part); BR-DM-005…012 | escalation ladder = (segment × category) → ordered steps (DPM D2) | F-mmStrategy (header: segment×category, effective dating, version) + F-mmEscStep (per-step: trigger days, instrument ref, grace, proportionality min, notice template, exit) + MLT base strategy seed — the **schema + base seed**; the EscalationEngine that walks it is DMBB-F04 | T-10.3 |

Every artefact in this folder is named above; the runtime engines that consume these carriers are recorded as later features (F03/F04), never silently dropped (§3).

## 2. Business rules in scope (carrier surface only)
| BR | Enforcement point |
|---|---|
| Code uniqueness on all `code` PK forms | Duplicate-value validator on `code` (Stage-3 generation note) |
| Thresholds configurable without code (DM-FR-004) | mdDebtCat.minAmount/maxAmount are data rows |
| Instrument applicability by category (BR-DM-031) | mdInstrument.minCategory data; cumulative semantics resolved by the consuming engine (F04) |
| DPM effective-dating / versioning / grandfathering (DM-Configurability §C) | mmStrategy.effectiveFrom/To/version columns present; bind-at-case-creation honoured by EscalationEngine (F04) — schema ready here |
| DPM changes are approval-gated (policy-change-as-case, CAD §6) | Deferred to the DMBB-admin activation flow (F05); interim control = role `dm_policy_admin` writes only — assumption A2 |

## 3. Design decisions & assumptions
1. **A1 — category↔Gold alignment.** mdDebtCat bands are seeded to **exactly** the `sta_v1` banding (C1<30, C2<100, C3<1000, C4<20000, C5<200000, C6≥200000) so the category a DM case is assigned (F03) equals the `debtCategory` GoldMartClient reads from the product. A mismatch would split truth — avoided by construction.
2. **A2 — applicability lives on the instrument, not the category.** Table 4 is cumulative (C4 includes C3's actions). Rather than a category→list, each instrument carries `minCategory` (the lowest band at which it applies). "Available actions for category X" = instruments with rank(minCategory) ≤ rank(X). Cleaner, single source, matches DPM D3 "instruments as data".
3. **A3 — instruments as data with enable-flags (DPM-3).** Ship Malta-14 `enabled=true`; foreign instruments (travel-ban-foreign, register-seizure, licence-measure, debtors-debtor-offset) seeded `enabled=false`, present for range/demo. `countryProfile=MLT`.
4. **A4 — escalation step is a standalone form, not a child grid.** mmEscStep references `strategyCode` (like mmTransition→caseType in CMBB-F01), so F01 stays clear of the FormGrid generator extension (that arrives with the case-content screens at F02). Steps ordered by `seq`.
5. **A5 — references are codes (TextFields), validated by the engines later.** mmEscStep.instrument and .strategyCode are code strings; referential validation (no step on a disabled instrument; full category coverage) is the DMBB-admin consistency check (F05), per the DPM analysis. Matches CMBB-F01 A1.
6. **A6 — expressions as text.** triggerEvent / exitCondition stored as short text interpreted by EscalationEngine (F04); never BeanShell (P3).
7. No blockers — gate G2 may pass on spec review. The `dmbb` app + `API-dmbb-data` credential are created at deploy (Stage 4); the DM `mmCaseType` row the spike left is harmless and will be owned by F02.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default (MLT seed) | Source |
|---|---|---|---|
| Debt category bands (configurable thresholds) | mdDebtCat rows | C1–C6 @ 30/100/1000/20000/200000 | DM-FR-004, BR-DM-003 |
| Enforcement instrument catalogue + applicability | mdInstrument rows | Malta-14 enabled + 4 foreign disabled | BR-DM-031, DPM D3 |
| Collection strategy (segment × category) | mmStrategy rows | STD-MLT (ALL segments, from C2) | DPM D2, BR-DM-005… |
| Escalation steps per strategy | mmEscStep rows | reminder→demand→final demand→enforcement (MLT day offsets) | DPM D2, BR-DM-005…012 |

## 5. Generation order
1. gen_forms.py: F-mdDebtCat, F-mdInstrument, F-mmStrategy, F-mmEscStep → dmbb/generated/forms.
2. gen_datalists.py (forms dir) → companion list_* for each.
3. gen_userview.py: dmbb UV-delta → `dmbbConsole` (DM Administration category, CRUD over the four carriers; role dm_policy_admin).
4. build_jwa.py dmbb/generated dmbb "DMBB" — no workflow, no plugins.
5. Deploy: create the `dmbb` app (deploy_jwa discovers version, publishes), restart Tomcat, **bind `API-dmbb-data` key** (unique per app — DX9-DELTAS SPIKE-001), load MLT seeds via load_md_seed (API-dmbb-data).
6. tests/run_t10.py (T-10.1..3, literal SQL asserts) + cmbb regression run_t02..t09 must stay green (shared bundle/spine untouched).
