---
title: "MTCA Taxpayer Accounting (Single Taxpayer Account) — Requirements Specification"
subtitle: "Comprehensive Functional and Technical Requirements for the Interim STA Module"
organization: "Malta Tax and Customs Administration (MTCA)"
version: "1.1"
date: "2026-06-11"
status: "Draft"
classification: "Official — For Internal Use"
---

**Document History**

| Version | Date | Description |
|---|---|---|
| 1.0 | 2026-06-02 | Initial draft — assembled from the source functional design (Menhard, Feb 2026) and counterpart DM specification. |
| 1.1 | 2026-06-11 | Baseline-alignment amendment per Gap Review MTCA-STA-GAP-001: new STA-FR-046–049, INT-FR-019, RPT-FR-022; amended STA-FR-008/039/040/041; new BR-STA-041–044; assumption A-08; OQ-32/OQ-33; data-model additions (tax-type flags, tax-year/YA, suspended-payments revenue subtype); heading-numbering corrections in Chapters 7, 8 and 12. Cross-boundary contracts synchronised with DM spec v1.1. |

# TABLE OF CONTENTS

1. [Executive Summary](#1-executive-summary)
2. [Business Context and Objectives](#2-business-context-and-objectives)
3. [Stakeholder Analysis](#3-stakeholder-analysis)
4. [Functional Requirements](#4-functional-requirements)
5. [Platform Capability Requirements](#5-platform-capability-requirements)
6. [Non-Functional Requirements](#6-non-functional-requirements)
7. [Use Case Model](#7-use-case-model)
8. [Business Rules Catalogue](#8-business-rules-catalogue)
9. [Data Requirements](#9-data-requirements)
10. [Interface Requirements](#10-interface-requirements)
11. [Implementation Considerations](#11-implementation-considerations)
12. [Traceability Matrix](#12-traceability-matrix)
13. [Contractual Safeguards](#13-contractual-safeguards)
14. [Glossary](#14-glossary)
15. [Appendices](#15-appendices)

---

# 1. EXECUTIVE SUMMARY

## 1.1 Purpose

This document provides a comprehensive Requirements Specification and Use Case Model for the **Taxpayer Accounting** component — the **Single Taxpayer Account (STA)** — that the Malta Tax and Customs Administration (MTCA) intends to implement as a quick-win initiative ahead of the Integrated Tax and Customs Administration System (ITCAS). The specification covers functional requirements, platform capability requirements, non-functional requirements, detailed use cases, business rules, data models, interface definitions, and implementation considerations for a solution that delivers immediate operational value while remaining architecturally aligned with the long-term ITCAS vision.

The document is the authoritative reference for solution design, development, testing, and acceptance of the interim STA system. It is the **counterpart to the Debt Management (DM) Requirements Specification**: where the DM specification treats the STA as an upstream data source, this specification defines the STA as the **system-of-record accounting engine** that produces the consolidated balances, enforceable positions, credit balances, and statements that the DM, reporting, and taxpayer-service components consume. It is intended for use by MTCA business stakeholders (principally the Taxpayer Accounting Section and Revenue Accounting), the MTCA IT unit, the platform implementation team, and IMF technical advisors throughout the solution lifecycle.

## 1.2 Scope

The Taxpayer Accounting solution addresses a single, foundational functional domain:

**Single Taxpayer Account (STA)** — A unified, consolidated view of each taxpayer's financial position across all tax types, periods, and charge categories, together with the accounting engine that maintains it. The STA consolidates data from MTCA's nine legacy Informix databases into a three-level hierarchical account structure — consolidated balance (L1), tax-type balance (L2), and transaction-level detail (L3) — following the Taxpayer–Tax Type–Tax Period (TTT) accounting principle and the balance-forward method. Beyond presentation, the STA computes balances, applies payment-allocation rules, calculates interest and penalties, performs cross-tax-type set-off and refund interception, reconciles taxpayer and revenue accounts, generates Tax Account Statements (TAS) and Tax Clearance Certificates (TCC), and supports end-of-period and year-end processing.

The STA is the **producer** of the financial data on which downstream components depend. Debt Management consumes enforceable balances, disputed-amount flags, and credit positions to drive its case lifecycle; reporting consumes revenue and reconciliation data; taxpayer services consume statements and clearance results. Those downstream behaviours are specified in their own documents and are out of scope here.

## 1.3 Solution Positioning

This specification defines an **interim solution** that operates during the period before the ITCAS taxpayer-accounting module becomes operational. It is positioned as follows:

**Data Layer:** The STA reads consolidated taxpayer data from the Operational Reporting System (ORS), built on ClickHouse, which ingests data from all nine legacy Informix databases through a medallion (bronze/silver/gold) ingestion pipeline. The ORS provides the analytical, query-optimised read layer; legacy systems remain the transactional systems of record for tax processing. The STA reads taxpayer accounting data and writes its own computed artefacts (allocations, adjustments, statements, suspense and reconciliation cases) to the platform's own store — never back to legacy Informix.

**Computation and Workflow Layer:** Balance computation, interest and penalty calculation, payment allocation, set-off, reconciliation, statement generation, and the supporting accounting-operations cases (payment correction, suspense resolution, reconciliation confirmation) are implemented on the selected low-code/no-code platform. All workflow definitions are exportable in standard BPMN 2.0 format to facilitate future migration. The specification is **platform-agnostic**: it states what the platform must do, not how any specific product implements it.

**ITCAS Migration Path:** The interim STA is explicitly designed for eventual replacement by the ITCAS accounting module. Architectural decisions throughout prioritise BPMN portability, standard data formats, and API-first integration to minimise migration effort. At ITCAS go-live, the STA data source switches from ORS/ClickHouse to the ITCAS accounting component with no substantial structural change to the account model defined here.

## 1.4 Key Stakeholders

| Stakeholder | Primary Interest |
|---|---|
| Commissioner for Tax and Customs | Strategic oversight; revenue accuracy; performance visibility |
| Director, Taxation Services | Taxpayer accounting accuracy; payment reconciliation; refund timeliness |
| Director, IT / CIO | Technical architecture; integration with ITCAS; platform sustainability |
| Taxpayer Accounting Section | Unified account views; TAS/TCC generation; payment allocation and reconciliation |
| Revenue Accounting / Treasury Liaison | Double-entry integrity; revenue-account reconciliation; fiscal reporting |
| Debt Management Unit (downstream consumer) | Accurate enforceable balances, credit positions, and aging derived from the STA |
| MITA (Malta IT Agency) | Infrastructure provisioning; ClickHouse hosting; SSO; network connectivity |
| European Dynamics (ITCAS Vendor) | Alignment with ITCAS accounting-module design; data-migration compatibility |
| IMF Technical Advisory Team | Compliance with international best practice; KPI framework alignment |
| Ministry for Finance / Treasury | Revenue reconciliation; refund forecasting; fiscal reporting |

## 1.5 Document Conventions

### 1.5.1 Requirement Identification Scheme

| Prefix | Domain | Example |
|---|---|---|
| STA-FR-*nnn* | Single Taxpayer Account — Functional Requirements | STA-FR-001 |
| RPT-FR-*nnn* | Reporting — Functional Requirements (accounting-relevant subset) | RPT-FR-004 |
| WF-FR-*nnn* | Workflow & Case Management — Functional Requirements | WF-FR-001 |
| INT-FR-*nnn* | Integration — Functional Requirements | INT-FR-001 |
| ADM-FR-*nnn* | Administration — Functional Requirements | ADM-FR-001 |
| NFR-*nnn* | Non-Functional Requirements | NFR-001 |
| BR-*xxx*-*nnn* | Business Rules (by domain: STA, RPT, WF, ADM) | BR-STA-001 |

Requirement IDs are preserved unchanged from the combined STA/DM specification to maintain bidirectional traceability between the Taxpayer Accounting and Debt Management documents.

### 1.5.2 MoSCoW Priority Levels

| Priority | Definition | Interim Context |
|---|---|---|
| **Must Have** | Essential for minimum viable solution; system cannot launch without it | Consolidated account views, balance computation, payment allocation, reconciliation, TAS/TCC |
| **Should Have** | Important; expected in the initial release but solution is usable without it | Set-off automation, refund-interest tracking, TAS template variants, self-service TAS |
| **Could Have** | Desirable enhancements if time and resources permit | Multi-currency handling, revenue disbursement by residential/territorial principle, ad-hoc analytics |
| **Won't Have** | Explicitly out of scope for the interim; deferred to ITCAS | Transactional tax processing, full taxpayer portal self-service, write-back to legacy systems |

### 1.5.3 Source References

| Abbreviation | Source Document |
|---|---|
| PDF-§*n.n* | MTCA STA/DM KPI Requirements (Menhard, Feb 2026) — chapter reference |
| PDF-T*n* | MTCA STA/DM KPI Requirements — table reference |
| RFP-§*n.n.n.n.n* | ITCAS RFP — section reference |
| VP-§*n.n* | Vendor Technical Proposal (European Dynamics) — section reference |
| DTP-§*n* | MTCA Digital Transformation Programme — section reference |

### 1.5.4 Use Case Identification

Use cases follow the pattern UC-*DOM*-*nnn* where DOM is the functional domain (STA, RPT, ADM).

---

# 2. BUSINESS CONTEXT AND OBJECTIVES

## 2.1 Problem Statement

MTCA administers Malta's complete tax and customs operations with approximately 700 staff, collecting revenue across corporate income tax (CIT), personal income tax (PIT), value-added tax (VAT), customs and excise duties, stamp duty, and other tax types. Its operational environment presents several challenges that directly impair taxpayer accounting.

**Fragmented Legacy Systems.** Tax operations are supported by over 70 PowerBuilder applications built over two decades against nine Informix databases containing more than 5,170 tables. These systems were developed incrementally and are not integrated.

**No Unified Taxpayer Account.** The absence of a Single Taxpayer Account means MTCA cannot produce a consolidated statement of a taxpayer's liabilities, payments, and balances. Each tax type is managed in isolation; cross-tax-type offsetting is not possible; tax clearance certificates require manual compilation; and a taxpayer who overpays one tax type cannot have that credit applied against liabilities in another without manual intervention.

**Manual Accounting Operations.** Payment allocation across tax types and charge components is performed manually and inconsistently. Unidentified and incompletely identified payments accumulate in suspense accounts with no systematic aging or resolution. Interest and penalty computation is not standardised. Reconciliation between taxpayer accounts and revenue accounts — and with Treasury — is laborious and error-prone.

**Absence of Reliable Revenue and Refund Reporting.** Without a consolidated ledger, revenue collected by tax type and charge component cannot be reported reliably, refund timeliness cannot be measured, and the data underpinning most performance indicators is classified as not-yet-available.

**Declining Technology Platform.** PowerBuilder's ecosystem is in structural decline, increasing maintenance risk and making technical staff harder to retain.

## 2.2 Strategic Objectives

The Taxpayer Accounting solution supports the MTCA Digital Transformation Programme (DTP) 2026–2030 and addresses the following objectives, with the **accounting KPI framework** drawn from the MTCA KPI matrix (PDF-T5).

### 2.2.1 Objective 1: Establish Unified Taxpayer Financial Visibility

Provide a consolidated, real-time view of each taxpayer's financial position across all tax types, enabling accurate Tax Account Statements, clearance assessment, and informed decision-making.

**KPI Targets (Taxpayer Services domain, PDF-T5 Items 1–10):** new registrations for CIT/VAT/PIT tracked and reported (Items 7–9; data available; High); taxpayer satisfaction with MTCA taxation information (Item 4; not yet; Medium); website visits by taxpayers (Item 10; not yet; Medium).

### 2.2.2 Objective 2: Improve Returns Filing and Payment Compliance

Enable systematic monitoring of payment timeliness, refund processing, and revenue collection across all tax types — all of which depend on an accurate consolidated ledger.

**KPI Targets (Returns Filing and Payment domain, PDF-T5 Items 11–23):** % of returns filed on time (Item 11; available; High); % filed digitally (Item 12; available; High); % processed without intervention (Item 13; not yet; High); % of expected returns not filed (Item 14; not yet; High); value of assessed revenue after intervention (Item 15; not yet; High); **% of VAT/CIT/PIT refunds issued within target timeframe (Items 20–22; not yet; High)** — measurable only once the STA tracks refund timeliness and late-refund interest; revenue collected from interventions after review of filed returns (Item 23; not yet; High).

### 2.2.3 Objective 3: Enable Accurate Revenue Accounting and Reconciliation

Maintain double-entry integrity between taxpayer accounts and revenue accounts, reconcile daily, and reconcile with Treasury, providing reliable revenue figures by tax type and charge component (principal, interest, penalties).

### 2.2.4 Objective 4: Provide the Accounting Foundation for Debt Management

Produce the enforceable balances, disputed-amount flags, credit positions, debt aging inputs, and refund-interception controls that the Debt Management component consumes. The STA does not itself manage debt cases, instalment agreements, or enforcement — those are downstream — but no risk-based, proportional debt management is possible without the accurate ledger this specification defines.

### 2.2.5 Objective 5: Liberate Staff Capacity Through Automation

Deliver measurable FTE savings by automating manual accounting operations — payment allocation, suspense resolution, statement generation, and reconciliation — contributing to the DTP's overall Year-1 target of approximately 50 FTE-equivalent savings.

## 2.3 Scope Definition

### 2.3.1 In Scope

**Single Taxpayer Account (STA):** consolidated taxpayer balance view across all tax types at three levels (L1 consolidated, L2 tax-type, L3 transaction); balance-forward accounting with PA/IA/PCA decomposition and disputed-amount tracking; double-entry bookkeeping; payment allocation (charge-type, oldest-period-first, tax-type priority, consolidated); overpayment and suspense handling; interest calculation (daily, intra-period rate changes, instalment and refund interest); penalty and fine calculation; cross-tax-type set-off and refund interception; account and Treasury reconciliation; Tax Account Statement and Tax Clearance Certificate generation; revenue accounting and chart-of-accounts mapping; end-of-period and year-end processing; legacy data migration support; suspended-payment handling for identified taxpayers; automatic account provisioning on new TIN or tax-type registration; and taxpayer account closure accounting.

**STA-relevant Reporting:** revenue dashboard, revenue reconciliation report, tax debt status report (accounting view), report configuration, export, and drill-down following the L1→L2→L3 hierarchy.

**STA-relevant Workflow and Administration:** accounting-operations cases (payment correction, suspense resolution, reconciliation confirmation, adjustment approvals); document generation and storage for statements and certificates; notifications for set-off, statements, and corrections; audit trail; business-rule, template, and rate configuration; user roles and permissions.

**Integration:** read access to ORS/ClickHouse; SSO; email/SMS notification gateways; document generation; and the API contracts that downstream components (DM, reporting, taxpayer portal) use to consume STA data.

### 2.3.2 Out of Scope

**Debt Management:** debt cases, risk-based prioritisation, instalment agreements, enforcement actions, and write-off workflows are specified in the Debt Management Requirements Specification. The STA produces the data these consume but performs none of these functions.

**Transactional Tax Processing:** the interim STA does not replace legacy systems for return filing, assessment processing, or payment receipt recording. Legacy Informix/PowerBuilder systems remain the systems of record; the STA reads from ORS, which mirrors legacy data.

**Taxpayer Portal Self-Service (full):** full taxpayer self-service is deferred to ITCAS; limited read-only account viewing and self-service TAS are classified Should/Could for a later interim phase.

**Risk Scoring and Analytics:** taxpayer risk scores are produced by the analytics platform and consumed by Debt Management; the STA neither computes nor stores them.

**Write-back to Legacy Systems and Real-time Bank Integration:** the STA does not write to legacy Informix databases; real-time bank payment processing and direct debit are ITCAS responsibilities.

### 2.3.3 Future ITCAS Migration

The ITCAS contract (European Dynamics, €68M) follows a phased plan with T0 targeted for March 2026; the taxpayer-accounting module falls within ITCAS Phase 2. When ITCAS Phase 2 is operational, the interim STA is progressively decommissioned: the STA data source switches from ORS/ClickHouse to the ITCAS accounting component; computed-artefact and configuration data migrate to ITCAS; and the interim platform is retained for residual case-management use or decommissioned. This path is detailed in Section 11 and reflected in the MoSCoW prioritisation of individual requirements.

## 2.4 Assumptions and Constraints

### 2.4.1 Assumptions

| ID | Assumption | Impact if Invalid |
|---|---|---|
| A-01 | The ORS (ClickHouse) will be operational and populated with data from all nine Informix databases by Q2 2026 | STA cannot provide consolidated views; launch delayed |
| A-02 | The selected low-code/no-code platform will be licensed and its environment provisioned by Q2 2026 | Accounting-operations workflows cannot be implemented; alternative platform required |
| A-03 | MTCA will allocate dedicated accounting subject-matter experts for requirements validation, UAT, and configuration | Requirements gaps; poor adoption; inaccurate accounting rules |
| A-04 | Legacy Informix systems continue operating unchanged during the interim, providing stable feeds to ORS | Data inconsistencies; STA accuracy compromised |
| A-05 | Interest rates, penalty/fine rates, allocation sequences, and charge-type priorities will be confirmed by MTCA before configuration | Default values used; potential misalignment with policy |
| A-06 | Legal authority for automated cross-tax-type offsetting and refund interception will be established or confirmed | Manual workarounds required; automated features disabled |
| A-07 | All workflow definitions will be created in BPMN 2.0 standard format to ensure portability to ITCAS | Vendor-specific formats create migration risk |
| A-08 | All revenue contributors in scope (including customs traders, NGOs and foreign representations) carry a valid MOD11 TIN before STA go-live; the TIN renumbering programme for populations identified by other identifiers completes for in-scope tax types (PDF §3.2) | STA accounts cannot be provisioned for unnumbered contributors; affected populations excluded from the consolidated view |

### 2.4.2 Constraints

| ID | Constraint | Implication |
|---|---|---|
| C-01 | **Read-only legacy access** — the STA reads from ORS/ClickHouse and never writes to legacy Informix databases | All transactional updates originate in legacy systems; STA reflects data with ORS refresh latency |
| C-02 | **ORS refresh frequency** — freshness depends on ETL scheduling (target near-real-time via CDC, minimum daily batch) | STA balances may not reflect same-day transactions during initial deployment |
| C-03 | **BPMN 2.0 portability** — all workflow definitions exportable in standard BPMN 2.0 | Constrains use of platform-specific extensions |
| C-04 | **MTCA IT capacity** — the IT unit has 6–12 operational staff | Favours low-code/no-code approaches; limits concurrent complexity |
| C-05 | **MITA infrastructure dependency** — ClickHouse hosting and connectivity provided by MITA | Deployment subject to MITA provisioning and change management |
| C-06 | **Budget envelope** — operates within the DTP quick-win allocation, separate from the €68M ITCAS contract | Favours open-source components and phased delivery |

## 2.5 Dependencies

| ID | Dependency | Provider | Target Date | Impact on STA |
|---|---|---|---|---|
| D-01 | ORS ClickHouse cluster operational with production data | MITA / MTCA Data Unit | Q2 2026 | Prerequisite for the STA data layer |
| D-02 | ORS ETL pipelines covering all nine Informix databases | MTCA Data Unit | Q2 2026 | Determines breadth of tax types visible in the STA |
| D-03 | Low-code/no-code platform environment provisioned | MITA | Q2 2026 | Prerequisite for accounting-operations workflows |
| D-04 | Data Catalogue (Apache Atlas) covering STA-relevant tables | MTCA Data Unit | Q1 2026 | Required for accurate data mapping |
| D-05 | ITCAS contract signature (T0) | MTCA / European Dynamics | March 2026 | Determines ITCAS timeline alignment for migration |
| D-06 | MTCA confirmation of rates, allocation sequences, and charge-type priorities | Commissioner / Directors | Q2 2026 | Required for accounting-rule configuration |
| D-07 | Legal framework review for automated offsetting and refund interception | MTCA Legal / Ministry | Q2–Q3 2026 | Determines which automated features can be activated |

---

# 3. STAKEHOLDER ANALYSIS

## 3.1 Stakeholder Register

| # | Stakeholder | Role/Function | Interest in STA | Influence | Key Expectations |
|---|---|---|---|---|---|
| 1 | Commissioner for Tax and Customs | DTP sponsor; strategic oversight | High — needs reliable revenue and compliance visibility | Very High | Accurate revenue data; demonstrable accuracy and FTE savings |
| 2 | Director, Taxation Services | Operational leadership for tax administration | High — accountable for accounting accuracy and refund processing | High | Accurate taxpayer accounts; reduced manual workload; timely refunds |
| 3 | Director, IT / CIO | Technical architecture; ITCAS coordination | High — integration, sustainability, migration path | High | Clean architecture; ITCAS alignment; manageable maintenance; BPMN portability |
| 4 | Taxpayer Accounting Section | Primary STA users | Very High — daily dependency for enquiries, statements, allocation | Medium | Accurate consolidated balances; fast TAS/TCC generation; reliable allocation |
| 5 | Revenue Accounting / Treasury Liaison | Revenue accounts and reconciliation | High — double-entry integrity and Treasury reconciliation | Medium | Zero-variance reconciliation; reliable revenue reporting |
| 6 | Debt Management Unit | Downstream consumer of STA data | High — depends on enforceable balances and aging | Medium | Accurate, timely enforceable balances, credit positions, disputed flags |
| 7 | MITA | Infrastructure provider; ClickHouse and SSO | Medium — provisioning and SLA | High | Clear infrastructure requirements; standard deployment patterns |
| 8 | European Dynamics (ITCAS Vendor) | Future system provider; migration target | Medium — interim must not conflict with ITCAS design | Medium | Standard data formats; documented APIs; clean migration path |
| 9 | Ministry for Finance / Treasury | Revenue reconciliation; fiscal reporting | Medium — accurate revenue data and refund forecasts | Low | Reliable revenue reports; timely reconciliation; refund forecasting |
| 10 | Taxpayers and Tax Practitioners | Recipients of TAS/TCC and account information | Medium — affected by accuracy and timeliness | Low | Accurate statements; clear allocation; fair, transparent balances |
| 11 | IMF Technical Advisory Team | QA; international best-practice alignment | Medium — advisory on design and KPI framework | Low | Compliance with standards; measurable outcomes |
| 12 | External Auditors (NAO) | Oversight and accountability | Low — periodic review of accounting accuracy | Low | Audit trail; transparent adjustment and reconciliation procedures |

## 3.2 Detailed User Personas

### 3.2.1 Taxpayer Accounting Clerk

**Role Description:** Responsible for maintaining taxpayer-account accuracy, processing payment allocations, handling balance enquiries, and generating Tax Account Statements (TAS) and Tax Clearance Certificates (TCC). Serves as first point of contact for taxpayers and practitioners requesting account information, clearance, or payment confirmations.

**Daily Tasks:** Respond to enquiries about account balances. Generate and issue statements and certificates in various formats. Investigate payment discrepancies and unallocated payments. Process refund verifications and inter-tax-type credit transfers. Reconcile accounts at end-of-day or end-of-period. Identify and flag anomalous balances for review.

**Pain Points:** Must access multiple legacy systems to assemble a complete financial picture. TAS generation is slow and error-prone due to manual consolidation. Payment allocation across tax types is not automated. Unidentified payments accumulate in suspense. No standardised process for overpayments and credit balances. High volume of routine enquiries consumes time better spent on complex cases.

**Needs from the STA:** Instant consolidated balance across all tax types from a single screen. One-click TAS/TCC generation with configurable templates (clearance type, language, taxpayer category). Automated payment-allocation trail showing how each payment was distributed. Clear visibility of suspense balances with resolution tools. Credit/debit netting display showing offset potential. Search by TIN, name, document ID, or payment ID.

**Success Criteria:** TAS generation reduced from hours to seconds. Zero manual consolidation for balance enquiries. Allocation rules applied consistently and transparently. Suspense backlog reduced by 80%. Enquiry response time reduced from days to minutes.

### 3.2.2 Revenue / Reconciliation Officer

**Role Description:** Responsible for revenue-account integrity, double-entry reconciliation between taxpayer and revenue accounts, daily reconciliation, and reconciliation with Treasury. Maintains the chart of accounts and oversees end-of-period and year-end processing.

**Daily Tasks:** Run and review daily reconciliation. Investigate and resolve variances exceeding tolerance. Reconcile collections by tax type with Treasury records. Oversee period-end balance roll-over and year-end account opening/closing. Maintain CoA mappings and validate postings.

**Pain Points:** Reconciliation requires extensive manual cross-referencing across disconnected systems. Variances are hard to localise to specific TINs and transactions. Treasury reconciliation is slow, delaying fiscal reporting. No automated exception handling.

**Needs from the STA:** Automated daily reconciliation with exception reporting (affected TINs, amounts, transaction IDs). Configurable tolerance (default €0.01). Treasury reconciliation at daily/weekly frequency with variance flagging. Period-end and year-end processing with zero-variance carry-forward verification. CoA configuration with posting validation and full audit trail.

**Success Criteria:** Daily reconciliation completes within the batch window with zero-variance target for automated postings. Treasury discrepancies flagged within 24 hours. Year-end carry-forward verified to ±€0.01. No untraceable adjustments.

### 3.2.3 Senior Management / Director

**Role Description:** Director or Deputy Director responsible for strategic oversight of revenue and accounting accuracy. Reports to the Commissioner on revenue collection and operational efficiency; not a daily system user but relies on dashboards and periodic reports.

**Daily Tasks:** Review revenue and reconciliation dashboards. Assess revenue trends and refund timeliness. Approve policy changes to rates, allocation sequences, and offset/interception parameters. Prepare briefings for the Commissioner and Ministry.

**Pain Points:** No consolidated revenue dashboard — relies on manually compiled spreadsheets weeks out of date. Cannot quantify the revenue impact of policy or rate changes. Imprecise revenue forecasting for Treasury.

**Needs from the STA:** Revenue dashboard with period-over-period comparison and drill-down to tax type and transaction. Refund-timeliness and reconciliation status at a glance. Exportable reports for external briefings.

**Success Criteria:** Revenue tracking accurate to within one business day. Reconciliation status visible at a glance. Decisions supported by data rather than anecdote.

### 3.2.4 IT Administrator / Citizen Developer

**Role Description:** Member of MTCA's small IT unit (6–12 staff) responsible for configuration, user management, rule and template modification, and first-line support. On the selected low-code platform, also acts as a citizen developer — configuring forms, reports, rules, and rate tables without traditional software development.

**Daily Tasks:** Manage user accounts, roles, and permissions. Configure and update accounting rules, rate tables, allocation sequences, and document templates. Create and modify report templates and dashboards. Monitor data-synchronisation status and reconciliation health. Troubleshoot ORS/legacy data discrepancies. Maintain integration connections (ORS API, notification, document generation).

**Pain Points:** Very small team relative to the systems supported. Limited capacity for concurrent operational, ITCAS-vendor, and maintenance demands. No standardised change-management process for rule modifications.

**Needs from the STA:** Low-code configuration for rate, allocation, and template changes without coding. Clear documentation of data models, API endpoints, and integration patterns. Automated monitoring and alerting for pipeline health and reconciliation. Role-based access administration. Version control for rule and template definitions. Sandbox for testing before production deployment.

**Success Criteria:** Routine configuration changes (rate updates, template edits, allocation tweaks) completed in hours, not weeks. Zero-downtime deployment of configuration changes. System health monitoring automated with proactive alerting. Clear audit trail of all configuration changes.

### 3.2.5 Taxpayer (Indirect — Receives STA Outputs)

**Role Description:** A natural person (NP) or legal person (LP) registered with MTCA for one or more tax obligations. In the interim solution the taxpayer is primarily an indirect stakeholder, affected by the STA's outputs (statements, clearance certificates, allocation notifications, set-off notices) with limited or no direct system access. Full self-service is deferred to ITCAS; a limited read-only account view and self-service TAS are candidate later-phase features.

**Interactions with the STA:** Receives Tax Account Statements and Tax Clearance Certificates generated from STA data. Receives notifications when payments are allocated, credits are set off, or balances change. May request a TAS through existing channels (counter, email, phone), generated by Accounting Clerks using the STA.

**Pain Points (current state):** No visibility of their own consolidated position. Cannot verify whether payments were allocated correctly. Statements sometimes contain inaccurate amounts due to reconciliation issues. No single point of contact for cross-tax-type queries.

**Needs from the STA (reflected in outputs):** Accurate, clearly formatted statements and certificates. Consistent payment allocation reflected in all correspondence. Transparent set-off and credit handling. Communication available in English and Maltese.

**Success Criteria:** Zero inaccurate statements (amounts reconciled to the STA). TAS produced within 24 hours of request (target: seconds on demand). Allocation and set-off clearly explained in every notification.

### 3.2.6 External Auditor (Read-Only)

**Role Description:** Auditor from the National Audit Office (NAO) or an external firm conducting periodic reviews of MTCA's revenue-accounting accuracy, adjustment and reversal procedures, and reconciliation. Requires read-only access for sampling, trend analysis, and compliance verification, governed by formal audit-engagement terms.

**Interactions with the STA:** Reviews account adjustment and reversal trails. Verifies revenue reconciliation between MTCA accounts and Treasury records. Examines suspense-account aging and resolution. Assesses double-entry integrity and period-end processing.

**Pain Points (current state):** Audit evidence must be manually compiled from disconnected systems. No centralised audit trail for accounting decisions. Difficult to perform statistical sampling without consolidated data. Reconciliation requires extensive manual cross-referencing.

**Needs from the STA:** Read-only access to account data, adjustment/reversal logs, and reconciliation trails through a dedicated auditor role. Pre-built audit reports (adjustment summary, suspense aging, reconciliation variance, revenue by tax type). Export for sampling (CSV, Excel). Complete audit trail on every posting, adjustment, and approval.

**Success Criteria:** Audit data extraction in hours rather than weeks. Full audit trail for every accounting action. Adjustment and reversal authorisation chain verifiable from system records alone. Statistical sampling possible directly from system reports.


---

# 4. FUNCTIONAL REQUIREMENTS

This section specifies the functional requirements of the Taxpayer Accounting (Single Taxpayer Account) component. Subsection 4.1 defines the core accounting engine. Subsections 4.2–4.4 define the accounting-relevant reporting, workflow/case-management, and integration requirements. Requirements that are primarily consumed by the downstream Debt Management component (debt-aging dashboards, debtors lists, enforcement workflows) are specified in the Debt Management Requirements Specification and are not repeated here.

## 4.1 Single Taxpayer Account (STA)

The Single Taxpayer Account provides a unified, consolidated view of every taxpayer's financial position across all tax types, tax periods, and charge categories. It serves as the foundational data structure upon which debt management, reporting, and taxpayer services are built. In the interim solution, the STA reads from the ORS/ClickHouse data warehouse; upon ITCAS deployment, the data source will switch to the ITCAS accounting module with no substantial structural changes.

All requirements in this section follow the TTT (Taxpayer / Tax Type / Tax Period) principle as the overriding consistency constraint, and implement the balance forward accounting method without exception.

### 4.1.1 Unified Account View

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-001 | The system shall display a consolidated top-level balance (L1) for each taxpayer showing aggregate debit and credit amounts across all tax types. | Must | Given a taxpayer TIN, the system displays a single consolidated balance comprising total debits and total credits across all registered tax types within 3 seconds. | PDF §3.3.1; ITCAS RFP §3.5.5.4.11.2 | ITCAS Ledger module provides equivalent L1 view. Interim solution reads from ORS; data source switches at ITCAS go-live. |
| STA-FR-002 | The system shall display tax type-level balances (L2) for each taxpayer, showing debit and credit amounts per tax type aggregated across all periods. | Must | For any taxpayer, the system displays individual balances for each registered tax type (CIT, PIT, VAT, stamp duty, etc.) with period-by-period breakdown available on drill-down. | PDF §3.3.1 Table 1; ITCAS RFP §3.5.5.4.11.2 | Maps directly to ITCAS TTT-based account structure per tax type. |
| STA-FR-003 | The system shall display detailed financial transaction records (L3) for each taxpayer, tax type, and tax period, showing individual debit (Dx) and credit (Cx) transactions. | Must | Drill-down from L2 to L3 shows all individual transactions (assessments, payments, refunds, transfers, write-offs, reversals) with date/time, amount, transaction type, and charge type (PA/IA/PCA). | PDF §3.3.1; Vendor Proposal §6.6 | ITCAS Ledger records all transactions at this granularity. |
| STA-FR-004 | The system shall support bidirectional navigation: top-down from consolidated balance to individual transactions, and bottom-up from transactions to consolidated balance. | Must | User can navigate from L1 → L2 → L3 (drill-down) and from any L3 transaction back to its L2 tax type balance and L1 consolidated balance without data loss or inconsistency. | PDF §3.1 | Core ITCAS accounting principle; navigation paths preserved in interim. |
| STA-FR-005 | The system shall provide an interface for MTCA staff to view all details of a taxpayer's account with role-based access controls preventing unauthorised data modification. | Must | Authorised MTCA staff can view complete account details using multiple access keys (TIN, document ID, taxpayer name). Read-only access enforced for view-only roles; audit trail logged for all access events. | Vendor Proposal §6.6; PDF §3.3.2 | ITCAS provides equivalent staff portal with RBAC. |
| STA-FR-006 | The system shall provide a taxpayer self-service portal view allowing taxpayers to view their single account balances and review all transactions across their accounts. | Should | Authenticated taxpayers can view their consolidated balance, per-tax-type balances, and transaction history via the online portal. View-only; no modification capability. | Vendor Proposal §6.6; PDF Annex 1 §1.11 | ITCAS Taxpayer Portal includes full account view. Interim may be read-only dashboard. |
| STA-FR-007 | The system shall support accounts where the concept of tax period does not apply (e.g., customs duties, duty on documents) by using the transaction date as the notional period. | Must | For non-periodic tax types, the system records transactions using the transaction date as the period identifier, and these balances correctly aggregate into L2 and L1 views. | ITCAS RFP §3.5.5.4.11.2; PDF §2.3 | Explicit ITCAS requirement for customs and duty on documents. |
| STA-FR-008 | The system shall provide account access using multiple search keys including taxpayer ID (TIN), document ID, payment ID, taxpayer name, and configured alternate registered identifiers (e.g., social security number, company register number, licence number). | Must | Staff can locate and open a taxpayer account using any of the supported keys, including any alternate identifier registered for the taxpayer. Search returns matching records within 2 seconds. Partial name search supported with disambiguation. Alternate identifier types configurable. | Vendor Proposal §6.6; PDF §3.3.2 Table 2 (additional identifiers) | Standard ITCAS search capability across all modules; identifier types align with the party-identifier reference pattern. |

### 4.1.2 Account Balance Computation

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-009 | The system shall maintain balances by tax type and tax period for each taxpayer, applying the balance forward accounting method: closing balance (debit, credit, or zero) at end of each tax period becomes the opening balance of the next period. | Must | At period close, the system computes the closing balance from all L3 transactions and carries it forward as the opening balance of the subsequent period. Balance integrity verified: opening balance + period transactions = closing balance. | PDF §3.3.1; Annex 1 §1.6; ITCAS RFP §3.5.5.4.11.2 | Core ITCAS accounting principle — balance forward method mandated. |
| STA-FR-010 | The system shall maintain separate balance components for each taxpayer account: principal tax (PA), interest (IA), and penalties and charges (PCA). | Must | Every balance at L1, L2, and L3 levels is decomposable into PA, IA, and PCA components. Summation of components equals total balance at each level. | PDF §3.3.2 Table 2; Annex 1 §1.9 | ITCAS Ledger tracks charge types: tax, interest, fees, penalty separately. |
| STA-FR-011 | The system shall track disputed amounts separately for each charge component (PA, IA, PCA) and exclude them from enforceable balance calculations while including them in total balance displays. | Must | Disputed amounts flagged at transaction level. Enforceable balance = total balance minus disputed amounts. Both total and enforceable balances visible to authorised staff. | PDF §3.3.2 Table 2 | ITCAS objection management integration marks disputed amounts. |
| STA-FR-012 | The system shall implement double-entry bookkeeping: every taxpayer liability posted as debit/credit to taxpayer account AND as credit/debit to the relevant tax-type revenue code account. | Must | For every accounting transaction, the system generates both the taxpayer account entry and the corresponding revenue account contra-entry. Net impact on trial balance is zero. Daily reconciliation report confirms double-entry integrity. | PDF Annex 1 §1.7-1.8; Vendor Proposal §6.6 | ITCAS Ledger implements standard double-entry bookkeeping. |
| STA-FR-013 | The system shall support all required transaction types: liability, payment, refund, transfer (from-to), write-off, and reversal of all transaction types. | Must | Each transaction type can be created, posted, and reversed. Reversals generate equal and opposite entries with audit trail linking to the original transaction. | PDF Annex 1 §1.9; Vendor Proposal §6.6 | All transaction types supported in ITCAS Ledger. |
| STA-FR-014 | The system shall support all required charge types: principal tax, penalty, interest, and surcharges (expenses, fees including legal fees). | Must | Each transaction is tagged with exactly one charge type. Charge types are configurable. Legal fees tracked in a separate sub-account per ITCAS RFP. | PDF Annex 1 §1.9; ITCAS RFP §3.5.5.4.11.2 | ITCAS maintains balances on additional taxes, interests, and fees (legal fees). |
| STA-FR-015 | The system shall support end-of-period processing that automatically rolls over balances applying the balance forward method, including year-end account opening and closing per fiscal year. | Must | At fiscal year-end, all accounts are closed with final balances calculated, and new-year accounts opened with carried-forward balances. Processing completes within the defined batch window. Reconciliation report generated confirming zero-variance carry-forward. | Vendor Proposal §6.6.2; PDF Annex 1 §1.6 | ITCAS supports end-of-period processing per Malta budget legislation. |
| STA-FR-016 | The system shall maintain balances including credits from pre-payments and support allocation of overpaid tax to previous/future years for the same tax type and for other tax types based on a configurable sequence, before a refund is issued. | Must | Overpayment detected → system checks for outstanding liabilities across tax types per priority sequence → auto-allocates credits → remaining credit flagged for refund processing or held in credit account. | ITCAS RFP §3.5.5.4.11.2; PDF Annex 1 §2.2 | Explicit ITCAS requirement for cross-period and cross-tax-type credit allocation. |

### 4.1.3 Payment Allocation Rules

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-017 | When a taxpayer selects a specific tax type and period, the system shall allocate payment in the following sequence: (1) interest due, (2) additional taxes due, (3) principal tax due, (4) fees due. This sequence shall be configurable and standardised across all tax types. | Must | Payment of €1,000 against a period with €200 interest, €100 additional tax, €600 principal tax, and €50 fees results in: interest fully settled, additional tax fully settled, principal tax partially settled (€650 remaining from principal, but €700 available after interest+additional = €300, so €700 to principal, leaving €0 balance and €50 to fees). Allocation rules displayed to taxpayers during payment. | ITCAS RFP §3.5.5.4.11.3; PDF Annex 1 §2.2 | ITCAS defines identical allocation hierarchy. Interim system must match. |
| STA-FR-018 | When a taxpayer indicates only a tax type (no specific period), the system shall allocate payment against the oldest tax debt first, applying the same charge-type sequence (interest → additional tax → principal → fees) within each period. | Must | Payment without period specification applied to oldest outstanding period first, then next-oldest, until funds exhausted. Allocation log generated showing distribution. | ITCAS RFP §3.5.5.4.11.3 | Explicit ITCAS payment allocation rule. |
| STA-FR-019 | When a consolidated payment is received without tax type or period specification, the system shall apply a configurable tax-type priority sequence, and within each tax type apply the charge-type and period-age sequences. | Must | Unspecified payment distributed across tax types per configured priority order. Complete allocation trail recorded. Taxpayer notification generated showing how payment was distributed. | ITCAS RFP §3.5.5.4.11.3; PDF Annex 1 §2.2 | ITCAS configurable rule for consolidated balance payment. |
| STA-FR-020 | Overpayments shall be posted to the taxpayer's general credit account and automatically applied when a new liability is posted. | Must | After all current liabilities cleared, excess amount posted to credit account. When next liability arises, system automatically checks credit balance and applies available credit before generating a payment demand. | PDF Annex 1 §2.2; ITCAS RFP §3.5.5.4.11.2 | ITCAS maintains pre-payment credits per taxpayer. |
| STA-FR-021 | Payments for which a taxpayer cannot be securely identified shall be posted to a tax revenue suspense account. Upon identity resolution, amounts shall be credited to the affected taxpayer's account. | Must | Unidentified payment → posted to suspense with case raised for resolution → assigned to authorised officer → upon resolution, reposted to identified taxpayer's ledger with full audit trail. Suspense account aging report generated daily. | PDF Annex 1 §2.2; Vendor Proposal §6.6 (payment processing items 3-6) | ITCAS suspense processing with case management workflow. |
| STA-FR-022 | The system shall support payment cancellation and reversal workflows for incorrect postings (e.g., payment posted to wrong TIN), with mandatory reason capture, approval workflow, and subsequent correct reposting. | Must | Cancellation request submitted with reason → routed to authorised approver → upon approval, original posting reversed and correct posting created. Both original and corrected transactions visible in audit trail. | Vendor Proposal §6.6 (payment processing item 8) | ITCAS provides payment correction workflow with approval. |

### 4.1.4 Interest Calculation

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-023 | The system shall automatically calculate interest on overdue tax liabilities on a daily basis, using the interest rate valid for each day (supporting rate changes within a calculation period). | Must | For a liability of €10,000 overdue for 90 days with a rate change on day 45 (from 8% to 9%), the system calculates: 45 days at 8% + 45 days at 9%. Interest amount posted to taxpayer's IA component. Calculation flow recorded and available for taxpayer notice. | Vendor Proposal §6.6.1 (items 1-2); PDF §4.1 | ITCAS Ledger computes interest in real-time with daily rate application. |
| STA-FR-024 | The system shall detect valid instalment schedules and apply a lower interest rate when the taxpayer is compliant with an approved instalment agreement. | Must | Taxpayer with active instalment agreement and all payments current → system applies configured lower rate. If instalment payment missed → system reverts to standard rate from the date of non-compliance. | Vendor Proposal §6.6.1 (item 3); PDF §6.2 | ITCAS instalment management controls interest rate application. |
| STA-FR-025 | MTCA administrators shall be able to configure interest rates, their validity periods, and the rules governing interest calculation through a configuration interface. The system shall enforce non-overlapping validity periods. | Must | Admin creates new interest rate with start/end dates. System validates no overlap with existing rates. Rate changes take effect automatically from the configured date. Audit trail for all rate changes. | Vendor Proposal §6.6.1 (config items 1, 4, 6) | ITCAS Configuration Management system provides rate management UI. |
| STA-FR-026 | The system shall track and report interest on late refund payments separately. | Should | When a refund is paid late (beyond the configured standard processing period), the system calculates interest owed to the taxpayer using a configurable refund-interest rate. Amount displayed on taxpayer account and in refund processing reports. | PDF §4.3; KPI #20-22 | ITCAS refund module tracks timeliness. Interim captures late refund interest for reporting. |

### 4.1.5 Penalty and Fine Calculation

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-027 | The system shall automatically calculate penalties when a rule violation is detected (e.g., late filing, late payment) and post penalty amounts to the taxpayer's PCA component. | Must | Late payment detected → system applies configured penalty rate → penalty amount posted to taxpayer account within the same processing cycle. Penalty amount, rule violated, and calculation basis recorded. | Vendor Proposal §6.6.1; PDF Annex 1 §4.4 | ITCAS auto-calculates penalties per configured rules. |
| STA-FR-028 | The system shall automatically calculate fines for configured rule violations and post them to the appropriate tax accounts, separately from penalties. | Must | Fine triggered by configured violation type → calculated per configured rate → posted to taxpayer account with fine classification distinct from penalty classification. | Vendor Proposal §6.6.1 | ITCAS distinguishes fines from penalties in the Ledger. |
| STA-FR-029 | MTCA administrators shall be able to configure penalty rates, fine rates, classification of violation types (fine vs. penalty), and their validity periods through a configuration interface with non-overlapping period enforcement. | Must | Admin can: (1) set penalty rates per violation type, (2) set fine rates per violation type, (3) define which violations trigger fines vs. penalties, (4) set validity periods. System prevents overlapping periods. All changes audited. | Vendor Proposal §6.6.1 (config items 2-6) | ITCAS Configuration Management provides full penalty/fine rule editor. |
| STA-FR-030 | The system shall support manual imposition, waiving, reversal, and write-off of penalties and interests by authorised MTCA officers, with approval workflows for amounts exceeding configured thresholds. | Should | Officer initiates penalty waiver → system routes to approver if amount exceeds threshold → upon approval, reversal entry posted. Audit trail captures original penalty, waiver request, approval, and reversal. | Vendor Proposal §6.6 (penalty/interest management) | ITCAS provides impose/un-impose/waive/reverse functions with RBAC. |
| STA-FR-031 | The system shall support configurable grace periods for penalties, with overdue penalty payments flagged for escalation. | Should | Grace period configured per penalty type (e.g., 15 days). Penalty not paid within grace period → flagged as overdue → escalation trigger activated. | Vendor Proposal §6.6 (penalty management) | ITCAS configures grace periods per penalty type. |

### 4.1.6 Set-off and Offsetting

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-032 | The system shall support automated cross-tax-type offsetting: credit balances in one tax type account shall be automatically applied against debit balances in other tax type accounts per a configurable set-off priority sequence. | Should | Taxpayer has €500 credit in VAT and €300 debit in CIT. System automatically applies €300 from VAT credit to clear CIT debit. Remaining €200 stays in VAT credit account. Notification generated to taxpayer. | PDF Annex 1 §5.1-5.3; ITCAS RFP §3.5.5.4.11.2 | ITCAS provides enhanced configurable set-off tool. Requires legal authorisation (OQ-06). |
| STA-FR-033 | Refund interception: before issuing any refund, the system shall check for existing debts across all tax types and automatically intercept the refund amount to cover outstanding liabilities. | Must | Refund of €2,000 approved for PIT. System detects €800 outstanding CIT debt → intercepts €800 → pays CIT liability → issues net refund of €1,200. Full interception trail recorded. | PDF §5.5; BR-DM-07 | ITCAS refund processing includes debt-check interception. Critical for debt management effectiveness. |

### 4.1.7 Account Reconciliation

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-034 | The system shall provide automated account reconciliation ensuring that payments and other transactions reconcile between taxpayer accounts and revenue accounts. | Must | Daily automated reconciliation process runs comparing taxpayer-side and revenue-side postings. Any discrepancy generates an exception report with affected TINs, amounts, and transaction IDs for manual review. Zero-variance target for automated postings. | ITCAS RFP §3.5.5.4.11.2; PDF Annex 1 §6.1 | ITCAS mandates automated reconciliation. Interim replicates using ORS data. |
| STA-FR-035 | The system shall support reconciliation with Treasury preferably in real time or at least on a daily basis, providing information on taxes transferred between tax types for clearing of taxpayer liabilities. | Should | End-of-day reconciliation report generated showing: total collections by tax type, transfers between types, net position per revenue account. Discrepancies with Treasury flagged within 24 hours. | PDF Annex 1 §6.1; Vendor Proposal §6.6.2 | ITCAS revenue accounting reconciles with government financial system. |
| STA-FR-036 | The system shall support the Reconciliation Act: generating a statement of all outstanding liabilities for a taxpayer and enabling MTCA officers to obtain confirmation of information correctness from taxpayers. | Should | Officer generates reconciliation statement for selected taxpayer → statement sent to taxpayer for confirmation → response tracked → confirmed/disputed amounts updated in account. | Vendor Proposal §6.8 | ITCAS enables reconciliation with taxpayer confirmation workflow. |

### 4.1.8 Statement Generation

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-037 | The system shall generate a Tax Account Statement (TAS) for any taxpayer showing the consolidated position across all tax types with breakdown by tax type, period, and charge component (PA/IA/PCA). | Must | TAS generated on demand showing: L1 consolidated balance, L2 per-tax-type balances, L3 transaction detail for selected period range. Output in electronic form with print capability. Multiple output formats (PDF, on-screen, printable). | PDF §3.3.1; Vendor Proposal §6.6 | ITCAS generates statement of account for all tax types. |
| STA-FR-038 | The system shall generate Tax Clearance Certificates: positive clearance when STA consolidated debit balance ≤ credit balance (no overdue liabilities), and negative clearance listing overdue liabilities by tax type and period with PA/IA/PCA breakdown and overdue-since date. | Must | Positive clearance: system confirms no overdue liabilities, generates certificate with validity period. Negative clearance: lists each overdue item with tax type, period, charge breakdown, and days overdue. Certificate includes operational provisos and legal instructions. | PDF BR-TAS-01, BR-TAS-02, BR-TAS-03 | ITCAS TCC (Tax Compliance Certificate) function. Interim produces equivalent certificates. |
| STA-FR-039 | The system shall support multiple TAS template variants selected on a four-dimension matrix: taxpayer category (NP, LP), statement type (TAS/TCC), **reason for request** (general request, tax clearance, other configured reasons), and language (English, Maltese as minimum). | Should | At least 4 template variants available: NP clearance, LP clearance, detailed statement, summary statement. Each available in English and Maltese. Template resolution applies the full matrix with English fallback (BR-STA-006). Templates and reason codes configurable by authorised administrators. | PDF §4.2 Table 3, §4.3 Figure 3 (step 1a); OQ-20 | ITCAS document generation supports configurable templates and multi-language. |
| STA-FR-040 | The system shall allow taxpayers to request a TAS through the online self-service portal, capturing the **reason for the request**. For general (non-clearance) requests, the generated statement shall include a text block with a link invoking the full STA account view, enabling complete drill-down analysis. | Should | Taxpayer logs in → selects reason, period/tax type → system generates and delivers electronically within 5 minutes. Reason recorded on the statement register; general-request output embeds the STA link block. Request and delivery logged. | PDF §4.2 Table 3, §3.3.3; Vendor Proposal §6.6 | ITCAS Taxpayer Portal provides self-service TAS. |

### 4.1.9 Revenue Accounting

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-041 | The system shall maintain revenue accounts per tax type and fiscal year — including a suspended-payments revenue account (STA-FR-046) — tracking expected revenue (liabilities), collected revenue (payments), and refundable amounts. Additional revenue-account dimensions per tax office (PDF Annex 1 §3.1) are satisfied at reporting level (RPT-FR-004 office breakdown) for the interim; a recorded divergence to be revisited at ITCAS. | Must | Revenue accounts maintained per tax type with: assessed amounts, collected amounts, refund claims, disbursements; suspended-payments account reconciles to the sum of suspended payments. Accounts align to fiscal year with year-end close and opening. | PDF Annex 1 §3.1; Vendor Proposal §6.6.2 | ITCAS Revenue Accounting subsystem manages CoA per tax type and fiscal year. |
| STA-FR-042 | The system shall support a Chart of Accounts (CoA) with the ability to set up, configure, map to tax types and payment methods, validate, modify, deactivate, and expire account structures. | Should | CoA codes mapped to all tax types. Validation prevents invalid postings. Changes to CoA audited. Deactivation prevents new postings while preserving historical data. | Vendor Proposal §6.6.2 | ITCAS Ledger CoA module provides full chart management. Interim may use simplified mapping. |
| STA-FR-043 | The system shall support revenue disbursement per the residential principle (taxpayer address determines municipality/region) and territorial principle (property location for immovable property tax). | Could | Revenue allocated to recipients based on taxpayer address or property location as appropriate per tax type. Configurable allocation rules per jurisdiction. | PDF Annex 1 §3.2-3.3 | ITCAS revenue disbursement configurable per tax type. Deferred to ITCAS if complex. |
| STA-FR-049 | The system shall provide revenue recipients and Treasury with read-only online access to their revenue-account positions (taxes collected per recipient), with view and download capability, refreshed at least daily. | Could | Authenticated recipient view shows collections by tax type and period for that recipient; daily refresh; download to Excel/CSV. Interim may substitute scheduled extracts; full online access deferred to ITCAS (recorded deferral). | PDF Annex 1 §1.11, §6.3 | ITCAS stakeholder portal provides recipient and Treasury account access. |

### 4.1.10 Data Migration and Legacy Support

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-044 | The system shall manage migrated legacy data, supporting transfer of balances and ledgers from existing legacy systems (Informix/PowerBuilder) through the ORS/DWH. | Must | Historical balances imported from DWH with migration audit trail. Migrated data clearly marked as legacy-sourced. Balances verified against source system totals. Discrepancy report generated for any variance > €0.01. | Vendor Proposal §6.6; PDF §3.3.2 | ITCAS includes legacy data migration capability. Interim loads from DWH. |
| STA-FR-045 | The system shall support multi-currency processing for tax types requiring denominated currencies (e.g., corporate tax shareholdings with foreign currency components). | Could | Transactions recorded in original currency with Euro equivalent calculated at applicable exchange rate. Multi-currency balances displayed with both original and Euro amounts. | ITCAS RFP §3.5.5.4.11.2; Vendor Proposal §6.6 | ITCAS supports multi-currency payments. Interim may defer to Euro-only with manual handling. |

### 4.1.11 Suspended Payments

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-046 | The system shall support a suspended state for payments of **identified** taxpayers (e.g., payment under verification, disputed instrument, dishonoured-cheque holding period). Suspended payments shall be posted to the taxpayer account and to the suspended-payments revenue account, excluded from allocation and from enforceable-balance computation until released, and resolved by release-to-allocation or return-to-payer with documented justification. | Must | Suspended payment visible on the taxpayer account with status "suspended" and suspension reason; excluded from BR-STA-011–014 allocation and from the enforceable balance exposed to Debt Management; release triggers standard allocation; return requires STO approval; full audit trail of suspension, release/return and reasons. | PDF Annex 1 (process list: "temporary suspended payments are included"), Annex 1 §4.7 | ITCAS payment processing supports suspended/temporary payment states with dedicated revenue suspense accounts. |

### 4.1.12 Account Provisioning and Closure

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| STA-FR-047 | The system shall automatically provision STA accounts: upon detection of a new TIN in the ORS registration feed, create the taxpayer account; upon detection of a new tax-type registration, create the corresponding L2 sub-account and link the registered tax periods (L3 structures). Malformed or incomplete registrations shall be routed to an exception queue rather than silently skipped. | Must | New TIN in ORS → account created with primary and secondary registration data within the applicable freshness tier; new tax-type registration → L2 sub-account linked with assessment type, periods and applicable flags; exceptions (invalid TIN, missing mandatory registration data) create resolution tasks; provisioning is idempotent (re-processing the same registration creates no duplicates). | PDF §3.3.3 Figure 2 (steps 1–3) | ITCAS registration module provisions accounts natively; interim provisioning logic retires at ITCAS go-live. |
| STA-FR-048 | The system shall support taxpayer account closure (deregistration accounting): a closure case resolving residual balances before closure (collection, refund, or hand-off to Debt Management for write-off per its workflow), after which the account rejects new postings except reversals; closed accounts remain queryable and re-openable under configurable authorisation. | Should | Closure case tracks balance-resolution path per tax type; account state "closed" enforced in all posting services; write-off hand-off recorded with the DM case reference (DM-FR-044 account-closure criteria); reopening requires authorised approval with reason; closed-account data retained per audit retention rules. | PDF Annex 1 (process list: write-off "at closing of taxpayer accounts") | ITCAS taxpayer lifecycle management includes deregistration and account closure. |

## 4.2 Reporting and Analytics (Accounting-Relevant)

The STA produces the revenue and reconciliation data consumed by management reporting. The reporting requirements below are those served directly by the accounting ledger. Debt-aging, debtors-list, collection-performance, instalment-compliance, and write-off reports are owned by the Debt Management component and are specified there.

### 4.2.1 Revenue and Reconciliation Reporting

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| RPT-FR-004 | The system shall provide a **Revenue Dashboard** displaying total revenue collected across all tax types, broken down by: principal tax, penalties, and interest; registration office; business type; and economic sector. Comparison with prior periods shall be supported. | Should | Revenue figures displayed in Euro with configurable period selection (month, quarter, year, custom). Prior period comparison shows absolute and percentage change. Drill-down from summary to tax type to individual transactions. Totals reconcile with Treasury disbursement records. | PDF §8.4.2 (Tables 10–11); BR-REV-01 | ITCAS revenue reporting provides comprehensive breakdowns. Interim dashboard transitions. |
| RPT-FR-007 | All dashboards shall support three hierarchical views: (a) multi-stream for top management, (b) entity-targeted for departmental management (e.g., Debt Management department), and (c) single-stream for case workers on specific case types. | Must | Each user sees the appropriate dashboard level based on their role. Navigation between levels supported with contextual filtering preserved. Dashboard layout adapts to role-based permissions. | PDF §6.3 (Figures 9, 10, 11) | ITCAS role-based dashboard hierarchy. |
| RPT-FR-010 | The system shall generate a **Revenue Reconciliation Report** comparing total revenue collected (principal tax, penalties, interest separately) by tax type against Treasury disbursement records, highlighting any variances. | Should | Revenue figures sourced from STA aggregation. Treasury comparison data imported or manually entered. Variance > configurable threshold (default €1.00) flagged for investigation. Report produced at configurable frequency (default: daily). | PDF Annex 1 §3; BR-REV-01, BR-REV-02 | ITCAS revenue accounting provides automated Treasury reconciliation. |
| RPT-FR-014 | The system shall generate a **Tax Debt Status Report** showing: TIN, registration office, tax type, period, business type, sector, due amount, paid amount, with sub-totals by TIN, type, and period; and grand totals of (due – paid). Sortable by office, type, period, sector, TIN, and amount descending. | Must | Report matches Table 17 specification. All amount fields in Euro. Sub-totals accurate. Grand total verified against STA aggregate. | PDF §8.4.4 (Table 17) | ITCAS taxpayer accounting reports. |
| RPT-FR-022 | The system shall produce a **Refund Liability Forecast** for Treasury, showing approved-but-undisbursed refunds and claimed refunds under review, by tax type and expected disbursement window, to support Treasury cash-flow planning per the configured frequency. | Should | Forecast aggregates refund positions from STA data: approved-undisbursed and under-review amounts by tax type and expected window; exported to Treasury via the Treasury interface (INT-EXT-07); produced at configurable frequency (default weekly); reconciles to refund balances within €0.01. | PDF Annex 1 §6.2 | ITCAS revenue accounting provides Treasury cash forecasting. |

### 4.2.2 Report Configuration, Export and Drill-Down

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| RPT-FR-015 | The system shall provide an interactive report configuration interface allowing users to select: functional area (mandatory), tax type(s), tax period(s), tax year(s), registration type, business type, registration office, annual turnover range, and output format (screen, PDF, Excel, CSV). | Must | Configuration screen matches Table 7/13/16 delimiter specifications. Defaults applied per specification (e.g., tax type default = all). User can preview selection before execution. Saved configurations reusable. Report title configurable (max 200 characters). | PDF §8.5 (Tables 7, 13, 16) | ITCAS MIS configuration interface. |
| RPT-FR-016 | The system shall allow users to save report configurations for reuse, including all parameter settings, layout choices, and scheduling options. Saved configurations shall be shareable across authorised users within the same organisational unit. | Should | Configurations saved with: name, description, creation date, owner. Shared configurations visible to unit members. Version history maintained. Maximum 100 saved configurations per user. | PDF §8.1 | ITCAS report library. |
| RPT-FR-017 | The system shall support scheduled report generation and automated distribution via email to configured recipient lists. | Should | Reports scheduled at: daily, weekly, monthly, or custom intervals. Distribution lists configurable per report. Email includes report as attachment (PDF or Excel). Delivery confirmation logged. Failed deliveries retried and escalated. | PDF §8.1 | ITCAS automated reporting. |
| RPT-FR-018 | All reports and dashboards shall support export to: screen display, PDF, Excel (XLSX), and CSV formats. PDF exports shall include report header with MTCA logo, generation timestamp, and parameter summary. | Must | Export function available on every report and dashboard. PDF formatted with consistent MTCA branding. Excel export preserves formulas for subtotals. CSV export uses UTF-8 encoding with comma delimiter. | PDF §8.5 | ITCAS standard export formats. |
| RPT-FR-019 | All reports shall support drill-down navigation from summary levels to detailed transaction-level data, following the STA hierarchy: consolidated balance → tax type balance → period balance → individual transactions. | Must | Click on any aggregate figure navigates to the next level of detail. Drill-down path preserved in breadcrumb. User can navigate back to any level. Context filters maintained across drill-down levels. | PDF §3.3.1 (L1→L2→L3); §8.1 | ITCAS provides multi-level drill-down in all reports. |
| RPT-FR-021 | The system shall support ad-hoc query capability allowing authorised analysts to construct custom reports using available data fields from the ORS/ClickHouse data platform, without requiring developer intervention. | Could | Query builder interface with: drag-and-drop field selection, filter conditions, grouping, sorting. Results displayable as table or basic chart. Custom queries saveable. Query execution within 30-second timeout. | PDF §8.1 | ITCAS MIS includes self-service analytics via Apache Superset. |

## 4.3 Workflow and Case Management (Accounting Operations)

These workflow and case-management requirements support the STA's accounting-operations cases — payment cancellation/correction, suspense resolution, reconciliation confirmation, and adjustment approvals — and the generation, storage, notification, and auditing of statements and certificates. The same platform capabilities serve the Debt Management component's case lifecycle (specified separately).

### 4.3.1 Case Lifecycle and History

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| WF-FR-001 | The system shall support a configurable case lifecycle with the following standard states: New → Open → In Progress → On Hold → Pending Closure → Closed. Additional custom states shall be configurable per case type without code changes. | Must | Case state transitions governed by configurable rules. Each transition logged with: timestamp, user, previous state, new state, reason. Invalid transitions rejected with user notification. State diagram configurable by administrator. | Vendor Proposal §6.7; PDF §5.2.2 | ITCAS CM uses identical lifecycle states. BPMN 2.0 export enables migration. |
| WF-FR-004 | The system shall maintain a complete case history showing all activities, decisions, state changes, communications, and document generation events for each case, with timestamps and user identification. | Must | Case history displayed in chronological order. Each entry shows: date/time, user, action type, description, outcome. History is immutable (entries cannot be modified or deleted). History exportable to PDF for case review. | Vendor Proposal §6.7; PDF §5.2.2 | ITCAS case audit trail. |
| WF-FR-005 | The system shall support case re-opening when new information or events require further action on a previously closed case, subject to configurable business rules and authorisation levels. | Should | Re-open creates new case activity linked to original case. Re-open reason mandatory. Original case data and history preserved. Re-open requires minimum authorisation level configurable per case type. | Vendor Proposal §6.7 | ITCAS case re-open capability. |

### 4.3.2 Notifications and Templates

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| WF-FR-013 | The system shall support multi-channel notification delivery to taxpayers via: email, SMS, in-app portal notification, and postal letter. Channel selection shall be configurable per notification type and taxpayer preference. | Must | Each notification type has configurable default channel(s). Taxpayer channel preference respected where available. Delivery status tracked per channel: sent, delivered, failed, bounced. Failed delivery triggers fallback to next configured channel. Postal letter generates print-ready PDF queued for dispatch. | Vendor Proposal §6.6.1; PDF §5.2.1 | ITCAS notification management subsystem. |
| WF-FR-014 | The system shall generate configurable notification templates for: payment reminders (pre-due-date), demand notices (1st and 2nd), instalment payment reminders, instalment default notices, enforcement escalation notices, and tax account statements. Templates shall support Maltese and English languages. | Must | Templates configurable by administrator with: merge fields (TIN, name, amounts, dates), static text, MTCA branding. Each template available in Maltese and English. Template versioning with effective dating. Preview function before deployment. | Vendor Proposal §6.6.1, §6.8; PDF §5.2.1, §5.3 | ITCAS document template management (ADG). |
| WF-FR-015 | The system shall send internal alerts to MTCA officers for: new case assignment, SLA warning/breach, instalment default detection, high-risk taxpayer activity, and supervisor approval requests. Internal alerts shall appear in the officer's in-app notification panel and optionally via email. | Must | In-app notification panel shows unread count badge. Alerts categorised by type and priority. Alert preferences configurable per officer (which alerts, which channels). Read/unread status tracked. Alert history retained for 90 days minimum. | Vendor Proposal §6.7 | ITCAS internal notification framework. |
| WF-FR-016 | The system shall log all outbound notifications with: recipient, channel, content summary, send timestamp, delivery status, and any associated case reference. Notification logs shall be searchable and included in case history. | Must | Notification log entry created for every outbound communication. Failed notifications flagged for manual review. Notification linked to originating case. Log searchable by: TIN, date range, channel, status. Retention period: 7 years minimum. | Vendor Proposal §6.6.1 | ITCAS communication audit trail. |

### 4.3.3 Document Generation, Storage and Audit Trail

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| WF-FR-017 | The system shall support automated document generation (ADG) for all standard debt management documents: demand notices, judicial letters, instalment agreements, enforcement orders, write-off approvals, and tax account statements. Documents shall be generated from configurable templates populated with case and taxpayer data. | Must | Documents generated in PDF format. Templates support: merge fields, conditional sections, MTCA letterhead, digital signatures (where applicable). Generated documents automatically attached to case record. Batch generation supported for bulk operations (e.g., monthly demand notices). | PDF §5.2.2 (Figure 8); Vendor Proposal §6.7, §6.8 | ITCAS ADG module provides automated document generation. |
| WF-FR-018 | The system shall provide an electronic document store (EDS) for case-related documents, supporting: upload of scanned documents, attachment of generated documents, document categorisation, version control, and search by document type, case, or taxpayer. | Must | Documents stored with: metadata (type, date, case ref, TIN), version number, upload user. Maximum file size: 25 MB. Supported formats: PDF, DOCX, XLSX, JPG, PNG. Full-text search across document metadata. Documents accessible from case view and taxpayer view. | PDF §5.2.2 (Figure 8); Vendor Proposal §6.7 | ITCAS EDS provides enterprise document management. |
| WF-FR-019 | The system shall track postal delivery of physical documents, recording: dispatch date, delivery method, delivery confirmation (where available), and returned mail. Undeliverable mail shall trigger an alert for address verification. | Should | Postal dispatch recorded in notification log and case history. Delivery confirmation updateable manually or via postal service integration. Returned mail flagged on taxpayer record. Address verification task auto-created for returned mail. | PDF §5.2.1; Vendor Proposal §6.8 | ITCAS postal delivery tracking. |
| WF-FR-020 | The system shall maintain an immutable, tamper-evident audit trail for all case management activities including: case creation, state transitions, data modifications, document generation, notifications sent, user logins, and configuration changes. | Must | Audit trail entries include: timestamp (UTC), user ID, action, entity affected, before/after values for data changes, IP address. Entries cannot be modified or deleted by any user including administrators. Audit trail queryable by: date range, user, action type, entity. Retention period: 10 years minimum. | PDF §5.2.2; Vendor Proposal §6.7 | ITCAS comprehensive audit logging. |

## 4.4 Integration Requirements

This section defines requirements for system integrations between the STA low-code application and its data sources, analytical platforms, legacy systems, future systems, and external services.

### 4.4.1 ORS/ClickHouse Integration

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-001 | The system shall read taxpayer account data (STA structure: L1 consolidated, L2 tax type, L3 transaction detail) from ORS/ClickHouse via RESTful API endpoints, with query response time <2 seconds for single-taxpayer lookups. | Must | API endpoints available for: consolidated balance by TIN, tax type balances by TIN, transaction history by TIN/tax type/period. Response includes: all STA data fields as specified in STA-FR-001 through STA-FR-010. Single-taxpayer query returns within 2 seconds under normal load. API authentication via OAuth 2.0 or API key. | ORS Implementation Plan §Technical Architecture; STA-FR-001–010 | ITCAS taxpayer accounting replaces ORS as data source. API contract preserved. |
| INT-FR-002 | The system shall read debt aging and debt portfolio data from ORS/ClickHouse via aggregation queries, with response time <5 seconds for portfolio-level analytics (e.g., total debt by age band across all taxpayers). | Must | Aggregation queries supported for: debt by age band, debt by tax type, debt by category (C1–C5), debt by enforcement stage. Materialized views in ClickHouse optimised for these access patterns. Results cached with configurable TTL (default: 15 minutes). | ORS Implementation Plan §ClickHouse Core; RPT-FR-001, RPT-FR-008 | ITCAS analytical data warehouse. |
| INT-FR-003 | The system shall support both real-time query access (via GraphQL or REST) and batch data synchronisation (via scheduled ETL) from ORS/ClickHouse, depending on the data freshness requirements of each functional area. | Must | Real-time access for: taxpayer account lookup, balance check, transaction history. Batch sync for: reporting data marts, KPI calculation, debtors list refresh. Batch sync frequency configurable per data set (minimum: every 15 minutes for hot data). Data freshness indicator displayed on all reports and dashboards. | ORS Implementation Plan §Consumers Layer | ITCAS provides transactional data in real time. |
| INT-FR-004 | The system shall handle ORS/ClickHouse unavailability gracefully, displaying cached data with a "data as of [timestamp]" indicator and queuing write operations for retry when connectivity is restored. | Must | System remains functional with read-only cached data during ORS outage. Cache validity period configurable (default: 4 hours). User alerted when viewing stale data. Write operations (case updates, workflow actions) continue to function using local platform storage. Automatic reconciliation upon ORS reconnection. | ORS Implementation Plan §High Availability | ITCAS high-availability architecture. |
| INT-FR-019 | The system shall consume ORS registration and transaction streams idempotently with persisted per-source consumption watermarks, providing a provable completeness check ("all source records up to watermark W have been absorbed"). Watermarks and consumption status are held in the platform's own store and are **not** written back to ORS or legacy systems, deliberately replacing the source design's transfer-status write-back (PDF Figure 2, steps 4 and 8) in a manner consistent with constraint C-01. | Must | Per-source watermark persisted after each successful consumption cycle; re-processing from a watermark produces no duplicate accounts, balances or postings; daily completeness report compares watermark coverage with ORS ingestion monitoring; divergence triggers a reconciliation exception. | PDF §3.3.3 Figure 2 (steps 4, 8); C-01 | ITCAS becomes the single transactional source; watermark mechanism retires at migration. |

### 4.4.2 SAS VIYA Integration

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-005 | The system shall consume taxpayer risk scores from SAS VIYA via REST API, including: overall risk score (0–100), risk category (High/Medium/Low), risk factors contributing to the score, and score calculation timestamp. | Must | Risk score API called on: case creation, case view, debtors list generation. Response parsed and stored locally for display. Score refresh frequency: configurable (default: daily batch + on-demand). API failure handled gracefully — system displays last known score with timestamp. | SAS Strategic Alignment §2, §5; CMP Evaluation §6.2 | ITCAS compliance management integrates with SAS VIYA. API contract preserved. |
| INT-FR-006 | The system shall send case outcome data to SAS VIYA to enable model refinement, including: enforcement actions taken, resolution outcome (paid, instalment, write-off), time to resolution, and recovery amount as percentage of original debt. | Should | Outcome data pushed via REST API or batch file (Parquet format) at configurable frequency (default: weekly). Data schema agreed between STA/DM and SAS VIYA teams. Successful delivery confirmed via API response. Failed deliveries logged and retried. | SAS Strategic Alignment §5 (Credit Collection Module) | ITCAS feeds case outcomes to SAS for model improvement. |
| INT-FR-007 | The system shall support invoking SAS VIYA predictive models on demand for: payment likelihood prediction, optimal enforcement action recommendation, and instalment default probability, displaying results within the case management interface. | Could | Model invocation via REST API with taxpayer context data. Response displayed in case detail panel as advisory information (not auto-actioned). Response time <5 seconds. Model unavailability does not block case processing. | SAS Strategic Alignment §5 (New Analytical Capabilities) | ITCAS advanced analytics integration. |

### 4.4.3 Legacy Informix Coexistence

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-008 | The system shall coexist with legacy Informix/PowerBuilder applications during the transition period, ensuring that data changes made in legacy systems are reflected in ORS/ClickHouse within the configured freshness tier: <5 minutes for hot data (payments, assessments), <1 hour for warm data, <24 hours for cold data. | Must | Data freshness verified against ORS ingestion monitoring (Grafana dashboard). STA application does not connect directly to Informix — all legacy data accessed via ORS/ClickHouse. Data inconsistencies between legacy and ORS flagged via automated reconciliation alerts. | Informix ClickHouse Ingestion Architecture §Phase 3; STA-FR-044 | ITCAS replaces legacy Informix systems. Coexistence ends at ITCAS go-live. |
| INT-FR-009 | The system shall NOT write data back to legacy Informix databases. All workflow state, case data, and configuration shall be stored in the low-code platform's own database, with ORS/ClickHouse serving as the read-only data source for taxpayer accounting data. | Must | Architecture enforces read-only access to ORS/ClickHouse for taxpayer data. Platform database stores: cases, workflow state, notifications, documents, configuration. No JDBC or ODBC write connections to Informix. Data ownership boundaries documented. | Project Brief §Key Constraints; ORS Implementation Plan §Architectural Advantages | ITCAS becomes the transactional system of record. Platform database migrated to ITCAS. |

### 4.4.4 ITCAS Future Migration

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-010 | All workflow definitions created in the low-code platform shall be exportable in BPMN 2.0 standard format, enabling migration to the ITCAS BPM engine without requiring workflow redesign from scratch. | Must | BPMN 2.0 export function available for all workflow definitions. Exported files validate against BPMN 2.0 XML schema. Export includes: process flows, decision points, timer events, escalation rules, user tasks, and service tasks. Round-trip tested: export from platform → import to reference BPMN tool → visual equivalence verified. | Project Brief §Key Constraints; CMP Justification §F.8 | ITCAS BPMN 2.0 compliant BPM engine imports exported workflows. |
| INT-FR-011 | The system shall be designed with a clean separation between: (a) business logic (rules, workflows, calculations), (b) data access (ORS/ClickHouse queries), and (c) presentation (UI/dashboards), to facilitate component-by-component migration to ITCAS. | Must | Architecture follows MVC or equivalent separation pattern. Data access layer uses abstraction (e.g., service interfaces) that can be re-pointed from ORS to ITCAS PostgreSQL. Business rules stored in decision tables exportable as spreadsheets or DMN format. Migration impact assessment producible from system documentation. | Project Brief §Key Constraints; ITCAS Vendor Technical Context §6.1 | ITCAS absorbs business logic and data access. Presentation layer replaced by ITCAS UI. |
| INT-FR-012 | The system shall support parallel operation with ITCAS during the migration period, enabling side-by-side comparison of case processing outcomes to validate ITCAS configuration before full cutover. | Should | Parallel mode routes identical case triggers to both systems. Comparison reports highlight discrepancies in: case assignment, SLA calculation, notification timing, enforcement action selection. Configurable to run for selected case types only. No duplicate taxpayer communications sent during parallel operation. | ITCAS Vendor Technical Context §6.1; PDF OQ-12 | ITCAS migration strategy includes parallel run validation. |

### 4.4.5 External System Integration

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-013 | The system shall support integration with commercial banks for automated bank account garnishment (freezing) requests, via secure web services or file-based exchange as supported by Maltese banking infrastructure. | Should | Garnishment request generated from enforcement case. Request includes: TIN, taxpayer name, bank account identifier, amount to freeze, legal authority reference. Response (confirmation/rejection) processed and recorded in case history. Secure channel (TLS 1.2+ minimum). | PDF §7.1 (item 7); Vendor Proposal §6.8 | ITCAS bank integration for garnishment and payment processing. |
| INT-FR-014 | The system shall support lookup of taxpayer assets via integration with the Land/Property Registry and Motor Vehicle Registry to support enforcement action planning (lien on assets, property seizure). | Could | Registry lookup triggered from enforcement case. Results displayed in case: property details, vehicle details, estimated values. Lookup via API or secure file exchange. Results cached locally with timestamp. Registry unavailability does not block case processing. | PDF §7.1 (items 9–12); Vendor Proposal §6.8 | ITCAS external authority integration. |
| INT-FR-015 | The system shall integrate with MTCA's taxpayer portal to enable taxpayers to: view their tax account statement (TAS), submit instalment applications, and view the status of their debt management cases. Portal integration shall be read-only for taxpayer-facing account data and write-enabled for instalment applications only. | Should | Portal displays: STA balance summary, transaction history, active cases (status only, no internal notes). Instalment application form submittable via portal → creates case in platform. Portal authentication via existing MTCA/MITA SSO. Data refresh frequency: near-real-time for balance, daily for case status. | PDF §3.3.3, §6.2; Vendor Proposal §6.8.2 | ITCAS taxpayer portal replaces interim portal integration. |
| INT-FR-016 | The system shall integrate with MITA's single sign-on (SSO) infrastructure using OAuth 2.0 or SAML 2.0 protocols for MTCA staff authentication, ensuring users do not need separate credentials for the STA platform. | Must | SSO integration tested and operational. User roles and permissions synchronised from MITA directory service. Session timeout configurable (default: 30 minutes of inactivity). SSO failure falls back to local authentication with alert to administrator. | CMP Evaluation §5.2; MITA governance requirements | ITCAS uses same MITA SSO infrastructure. |

### 4.4.6 Notification Channel Integration

| ID | Description | Priority | Acceptance Criteria | Source | ITCAS Alignment |
|----|-------------|----------|---------------------|--------|-----------------|
| INT-FR-017 | The system shall integrate with an email service (SMTP or API-based) for delivery of notifications, reports, and document attachments to taxpayers and MTCA staff. | Must | Email delivery via configurable SMTP server or email API (e.g., MITA email gateway). Support for: HTML and plain-text email, attachments up to 10 MB, delivery receipt tracking. Bounce handling: bounced emails logged, taxpayer email marked as undeliverable after 3 consecutive bounces. | WF-FR-013; Vendor Proposal §6.6.1 | ITCAS email integration. |
| INT-FR-018 | The system shall integrate with an SMS gateway for delivery of short notification messages (payment reminders, SLA alerts) to taxpayers who have opted in to SMS communications. | Should | SMS via configurable gateway API. Message length: ≤160 characters (single SMS) or ≤480 characters (concatenated). Delivery status tracked: sent, delivered, failed. Opt-in/opt-out managed per taxpayer record. SMS content logged in notification history. | WF-FR-013; PDF §5.2.1 | ITCAS SMS integration. |

---

---

# 5. PLATFORM CAPABILITY REQUIREMENTS

This section specifies the platform-agnostic capability requirements the selected low-code/no-code Case Management Platform must provide to support the STA. They define *what* the platform must do, not *how* any product implements it.

## 5.0 Overview

This section specifies the platform capability requirements for the Case Management Platform (CMP) supporting the Taxpayer Accounting (STA) module. These requirements define the technical capabilities that the selected no-code/low-code platform must provide to enable the STA functional requirements documented in Section 4.

All requirements in this section are platform-agnostic — they define *what* the platform must do, not *how* any specific product implements it. The CMP must be a market-validated commercial off-the-shelf (COTS) platform with demonstrated capability in government case management deployments.

**Requirement Summary:**

| Subsection | Prefix | Count | Must | Should |
|---|---|---|---|---|
| 5.1 Forms and Workflow Configuration | CFW | 45 | 43 | 2 |
| 5.2 Workflow Engine | WF | 50 | 49 | 1 |
| 5.3 Business Rules Engine | BR | 15 | 15 | 0 |
| 5.4 Forms Management | FM | 15 | 15 | 0 |
| 5.5 Data Management | DM | 12 | 12 | 0 |
| 5.6 Events and Triggers | EC | 10 | 10 | 0 |
| 5.7 Process Monitoring | PM | 10 | 10 | 0 |
| 5.8 Platform Integration | INT | 20 | 17 | 3 |
| 5.9 Platform Architecture | OA | 10 | 9 | 1 |
| 5.10 General Platform Requirements | GN | 10 | 10 | 0 |
| 5.11 DM Operational Metrics | PRM (subset) | 9 | 8 | 1 |
| **Total** | | **206** | **198** | **8** |

**FTE Liberation Cross-Reference:** Section 5.12 maps platform capabilities to the DM use cases (UC.DM.001–UC.DM.007) and their combined ~8–10 FTE liberation target by December 2026.

---

## 5.1 Forms and Workflow Configuration (CFW.xxx)

This subsection specifies requirements for the platform's form and workflow configuration capabilities, enabling MTCA staff to define document forms, processing rules, and case workflows without programming.

**DM Use Case Mapping:** These capabilities directly enable UC.DM.001 (debt case identification and tracking), UC.DM.002 (demand notice generation), and UC.DM.003 (instalment agreement processing).

### 5.1.1 Form and Workflow Definition

| ID | Priority | Requirement |
|---|---|---|
| CFW.001 | Must | The platform SHALL enable MTCA officials to define fields of document form structure, layout, and processing rules without custom coding. The component must enable configuration of standard action types including: (a) specification of action name and code, (b) specification of risk type applicability, (c) specification of document templates, (d) provision of instruction flows, (e) specification of forms for findings, and (f) specification of forms for case end results. |
| CFW.002 | Must | The platform SHALL support inclusion of data field types: (a) company/people identification, (b) Canonical Data Model fields, (c) calculated fields, (d) attachment uploads, (e) date fields with validation, (f) numeric fields with range validation, (g) dropdown selections, and (h) multi-select checkboxes. |
| CFW.003 | Must | The platform SHALL enable definition of multiple document versions or "document types" for the same business process. |
| CFW.004 | Must | The platform SHALL support defining document processing workflows as Finite State Machines, automatically calculating case status. |
| CFW.005 | Must | The platform SHALL enable configuration of approval workflows for case decisions with multi-stage approval routing. |
| CFW.006 | Must | The platform SHALL support configuration of pre-conditions for actions that validate data completeness. |
| CFW.007 | Must | The platform SHALL enable specification of data sources for form field population, including data sourced from ORS/ClickHouse and legacy Informix. |
| CFW.008 | Must | The platform SHALL support configuration of output documents generated upon case completion. |
| CFW.009 | Must | The platform SHALL enable configuration of business rules that check data consistency and validity. |
| CFW.010 | Must | The platform SHALL support automatic generation of unique reference numbers for cases and documents. |

### 5.1.2 Document Handling and Processing

| ID | Priority | Requirement |
|---|---|---|
| CFW.011 | Must | The platform SHALL enable selection of document templates for generating decisions, notices, and correspondence. |
| CFW.012 | Must | The platform SHALL support configuration of document versioning with ability to view previous versions. |
| CFW.013 | Must | The platform SHALL enable configuration of document archival rules based on case type and retention requirements. |
| CFW.014 | Must | The platform SHALL support notification configuration including: (a) configurable mailing lists or rules-based recipients, and (b) notification text generation using templates and document attributes. |
| CFW.015 | Must | The platform SHALL enable definition of approval workflows that vary based on document and approver attributes. |
| CFW.016 | Must | The platform SHALL support configuration of rule invocation modes: (a) manual, (b) scheduled/automatic, and (c) event-driven. |
| CFW.017 | Must | The platform SHALL enable configuration of document correctness rules validating data consistency constraints. |
| CFW.018 | Must | The platform SHALL support generation of unique payment references linked to specific documents. |
| CFW.019 | Must | The platform SHALL enable configuration of document status transitions with defined valid progressions. |
| CFW.020 | Must | The platform SHALL support instant generation of submission confirmation tokens. |

### 5.1.3 List and Queue Management

| ID | Priority | Requirement |
|---|---|---|
| CFW.021 | Must | The platform SHALL enable configuration of work queues for case assignment with priority and load balancing. |
| CFW.022 | Must | The platform SHALL support configurable list views with: (a) configurable attribute columns, and (b) configurable access rights. |
| CFW.023 | Must | The platform SHALL enable automatic processing of submitted documents using configurable business rules. |
| CFW.024 | Must | The platform SHALL support bulk operations on document lists including bulk status updates and reassignment. |
| CFW.025 | Must | The platform SHALL enable configuration of document list filtering, sorting, and search capabilities. |
| CFW.026 | Must | The platform SHALL support printing of individual documents and document lists. |
| CFW.027 | Must | The platform SHALL support document export and import in XML, JSON, PDF, Excel, and CSV formats. |
| CFW.028 | Must | The platform SHALL provide web service APIs for automated document submission with data quality validation. |

### 5.1.4 Correspondence and Templates

| ID | Priority | Requirement |
|---|---|---|
| CFW.029 | Must | The platform SHALL enable configuration of letter templates with placeholder fields populated from case data. |
| CFW.030 | Must | The platform SHALL support bilingual correspondence generation in Maltese and English. |
| CFW.031 | Must | The platform SHALL enable batch letter generation for bulk correspondence campaigns. |
| CFW.032 | Must | The platform SHALL support electronic delivery of correspondence via email with tracking. |
| CFW.033 | Must | The platform SHALL enable configuration of correspondence approval workflows before issuance. |
| CFW.034 | Should | The platform SHOULD support integration with government postal services for physical mail tracking. |
| CFW.035 | Must | The platform SHALL maintain correspondence history linked to cases with version tracking. |

### 5.1.5 Audit Trail and History

| ID | Priority | Requirement |
|---|---|---|
| CFW.036 | Must | The platform SHALL maintain comprehensive audit trail for all form submissions, workflow transitions, and user actions. |
| CFW.037 | Must | The platform SHALL record username, date, time, IP address, and device for all audited events. |
| CFW.038 | Must | The platform SHALL support audit trail queries by case, user, date range, and action type. |
| CFW.039 | Must | The platform SHALL retain audit trail data for minimum 10 years per government retention requirements. |
| CFW.040 | Must | The platform SHALL prevent modification or deletion of audit trail records. |

### 5.1.6 Multi-Language Support

| ID | Priority | Requirement |
|---|---|---|
| CFW.041 | Must | The platform SHALL support form and workflow configuration in both Maltese and English. |
| CFW.042 | Must | The platform SHALL enable users to switch interface language without losing session context. |
| CFW.043 | Must | The platform SHALL support localised field labels, validation messages, and help text. |
| CFW.044 | Must | The platform SHALL support date, number, and currency formatting per Maltese conventions. |
| CFW.045 | Should | The platform SHOULD support future addition of third languages without system modification. |

---

## 5.2 Workflow Engine (WF.xxx)

This subsection specifies requirements for the platform's workflow management engine, encompassing process modelling, task assignment, flow control, scheduling, error handling, and case management.

**DM Use Case Mapping:** The workflow engine is the central orchestration layer enabling all DM use cases. It directly powers UC.DM.001 (automated case identification workflows), UC.DM.002 (notice generation workflows with timer-based triggers), UC.DM.003 (multi-stage instalment approval workflows), UC.DM.004 (risk-based case routing), and UC.DM.007 (escalation-driven enforcement workflows).

### 5.2.1 Process Modelling

| ID | Priority | Requirement |
|---|---|---|
| WF.001 | Must | The platform SHALL support workflow design using BPMN 2.0 notation or equivalent visual standard. |
| WF.002 | Must | The platform SHALL provide a graphical Workflow Editor with drag-and-drop process design. |
| WF.003 | Must | The platform SHALL support process types: (a) Synchronous — blocking caller, (b) Asynchronous — background execution, and (c) Manual — human task-based. |
| WF.004 | Must | The platform SHALL support grouping of workflows into hierarchical parent-child structures. |
| WF.005 | Must | The platform SHALL support workflow decomposition with sub-process invocation. |
| WF.006 | Must | The platform SHALL enable definition of process start events (manual trigger, scheduled, event-driven). |
| WF.007 | Must | The platform SHALL support multiple end events with distinct outcomes and routing. |
| WF.008 | Must | The platform SHALL enable workflow documentation with annotations and descriptions. |

### 5.2.2 Task and Assignment

| ID | Priority | Requirement |
|---|---|---|
| WF.009 | Must | The platform SHALL support user task definition with configurable forms and instructions. |
| WF.010 | Must | The platform SHALL support declarative modelling of approval management processes with optional reverse functionality. |
| WF.011 | Must | The platform SHALL enable complex multi-stage approval with static and dynamic approval lists organised by: (a) named users, (b) groups, (c) job levels/roles, (d) position, (e) supervisory chain, and (f) rule-based determination. |
| WF.012 | Must | The platform SHALL provide policy-based task assignment defining approval rules based on document attributes. |
| WF.013 | Must | The platform SHALL support task delegation allowing reassignment to another user. |
| WF.014 | Must | The platform SHALL support task escalation when deadlines are exceeded. |
| WF.015 | Must | The platform SHALL enable ad-hoc modification of responsible users during task execution. |
| WF.016 | Must | The platform SHALL support task claiming from group queues. |
| WF.017 | Must | The platform SHALL provide task prioritisation capabilities. |

### 5.2.3 Gateways and Flow Control

| ID | Priority | Requirement |
|---|---|---|
| WF.018 | Must | The platform SHALL support exclusive gateways (XOR) routing to single path based on conditions. |
| WF.019 | Must | The platform SHALL support parallel gateways (AND) enabling concurrent execution paths. |
| WF.020 | Must | The platform SHALL support inclusive gateways (OR) routing to one or more paths based on conditions. |
| WF.021 | Must | The platform SHALL support event-based gateways waiting for multiple possible events. |
| WF.022 | Must | The platform SHALL support complex gateway logic with custom condition expressions. |
| WF.023 | Must | The platform SHALL support loop patterns for iterative processing. |

### 5.2.4 Timer and Scheduling

| ID | Priority | Requirement |
|---|---|---|
| WF.024 | Must | The platform SHALL support timer start events triggering workflows on schedule. |
| WF.025 | Must | The platform SHALL support timer intermediate events creating delays or deadlines. |
| WF.026 | Must | The platform SHALL support timer boundary events triggering escalation on task timeout. |
| WF.027 | Must | The platform SHALL support calendar-aware scheduling respecting Maltese public holidays. |
| WF.028 | Must | The platform SHALL support business hours configuration for SLA calculations. |
| WF.029 | Must | The platform SHALL support cron-style schedule expressions for recurring triggers. |

### 5.2.5 Error Handling

| ID | Priority | Requirement |
|---|---|---|
| WF.030 | Must | The platform SHALL support error boundary events capturing workflow exceptions. |
| WF.031 | Must | The platform SHALL support error sub-processes for exception handling logic. |
| WF.032 | Must | The platform SHALL support compensation handlers for rollback scenarios. |
| WF.033 | Must | The platform SHALL support retry logic with configurable attempt limits and delays. |
| WF.034 | Must | The platform SHALL log all workflow errors with diagnostic information. |

### 5.2.6 Case Management Specifics

| ID | Priority | Requirement |
|---|---|---|
| WF.035 | Must | The platform SHALL support case lifecycle management from creation through resolution to archive. |
| WF.036 | Must | The platform SHALL support milestone tracking within case workflows. |
| WF.037 | Must | The platform SHALL support activity scheduling and tracking within cases. |
| WF.038 | Must | The platform SHALL trigger business rules on case events: (a) lifecycle, (b) milestone, (c) activity, (d) data, (e) document, (f) comment, and (g) user events. |
| WF.039 | Must | The platform SHALL support case templates with pre-configured workflow patterns. |
| WF.040 | Must | The platform SHALL enable case correlation linking related cases. |

### 5.2.7 Notification and Communication

| ID | Priority | Requirement |
|---|---|---|
| WF.041 | Must | The platform SHALL support email notification configuration for workflow events. |
| WF.042 | Should | The platform SHOULD support SMS notification for urgent alerts. |
| WF.043 | Must | The platform SHALL support in-application notification to user task lists. |
| WF.044 | Must | The platform SHALL support notification templates with data placeholders. |
| WF.045 | Must | The platform SHALL support notification recipient determination by role, group, or expression. |

### 5.2.8 Collaboration and Versioning

| ID | Priority | Requirement |
|---|---|---|
| WF.046 | Must | The platform SHALL support workflow versioning with ability to run multiple versions concurrently. |
| WF.047 | Must | The platform SHALL support workflow comparison showing differences between versions. |
| WF.048 | Must | The platform SHALL support workflow migration moving in-flight instances to new versions. |
| WF.049 | Must | The platform SHALL support workflow commenting and annotation for collaboration. |
| WF.050 | Must | The platform SHALL support workflow export in BPMN 2.0 XML format. |

---

## 5.3 Business Rules Engine (BR.xxx)

This subsection specifies requirements for the platform's business rules management capabilities, supporting configurable rule definition, testing, and lifecycle management.

**DM Use Case Mapping:** The business rules engine directly supports UC.DM.001 (debt case identification criteria), UC.DM.002 (demand notice trigger rules and escalation thresholds), UC.DM.004 (risk-based prioritisation rules consuming SAS VIYA scores), and UC.DM.007 (enforcement escalation rules).

| ID | Priority | Requirement |
|---|---|---|
| BR.001 | Must | The platform SHALL provide an Expression Builder with syntax highlighting, error detection, and code completion. |
| BR.002 | Must | The platform SHALL support IF-THEN rule format for conditional logic. |
| BR.003 | Must | The platform SHALL support decision table format for matrix-based rule configuration. |
| BR.004 | Must | The platform SHALL support decision tree format for hierarchical rule structures. |
| BR.005 | Must | The platform SHALL support formula expressions for calculated values. |
| BR.006 | Must | The platform SHALL support rule sets grouping related rules with namespacing. |
| BR.007 | Must | The platform SHALL support rule chaining for sequential rule execution. |
| BR.008 | Must | The platform SHALL support rule prioritisation determining execution order. |
| BR.009 | Must | The platform SHALL support rule versioning with effective date ranges. |
| BR.010 | Must | The platform SHALL support rule localisation for multi-language error messages. |
| BR.011 | Must | The platform SHALL support rule testing with simulation capability. |
| BR.012 | Must | The platform SHALL support rule integration with workflows and forms. |
| BR.013 | Must | The platform SHALL support rule consistency checking preventing conflicting rules. |
| BR.014 | Must | The platform SHALL support rule audit trail showing execution history. |
| BR.015 | Must | The platform SHALL support rule lifecycle management from creation to deactivation. |

---

## 5.4 Forms Management (FM.xxx)

This subsection specifies requirements for the platform's forms management capabilities, enabling visual form design, data binding, validation, and multi-channel rendering.

**DM Use Case Mapping:** Forms management directly enables UC.DM.003 (instalment agreement forms with complex validation), UC.DM.002 (demand notice templates), and UC.DM.005 (management dashboard reporting forms).

| ID | Priority | Requirement |
|---|---|---|
| FM.001 | Must | The platform SHALL provide a drag-and-drop Forms Editor enabling visual form design by MTCA officials. |
| FM.002 | Must | The platform SHALL support form design across layers: (a) Data Objects Management, (b) Controls Management, (c) Layout Design, and (d) Handlers Design. |
| FM.003 | Must | The platform SHALL support comprehensive field types: text, numeric, date, dropdown, checkbox, radio button, file upload, signature, and rich text. |
| FM.004 | Must | The platform SHALL support data binding connecting form fields to business objects. |
| FM.005 | Must | The platform SHALL support client-side validation with immediate feedback. |
| FM.006 | Must | The platform SHALL support server-side validation for security-critical rules. |
| FM.007 | Must | The platform SHALL support conditional visibility showing/hiding fields based on rules. |
| FM.008 | Must | The platform SHALL support conditional enabling/disabling of fields based on context. |
| FM.009 | Must | The platform SHALL support form sections and tabs for complex form organisation. |
| FM.010 | Must | The platform SHALL support repeating sections for variable-length data entry. |
| FM.011 | Must | The platform SHALL support form pre-population from existing data sources. |
| FM.012 | Must | The platform SHALL support multi-channel form rendering: (a) web browser, (b) mobile devices, and (c) REST API. |
| FM.013 | Must | The platform SHALL support form template creation for reusable form patterns. |
| FM.014 | Must | The platform SHALL support form versioning with change tracking. |
| FM.015 | Must | The platform SHALL support form compilation and deployment to production. |

---

## 5.5 Data Management (DM.xxx)

This subsection specifies requirements for the platform's data management capabilities, including business object definition, data relationships, external data integration, and historization.

**DM Use Case Mapping:** Data management is foundational to all DM use cases, particularly UC.DM.001 (debt case entity definition with ORS integration), UC.DM.006 (real-time debt portfolio visibility from ORS/ClickHouse), and UC.DM.003 (instalment agreement data objects).

| ID | Priority | Requirement |
|---|---|---|
| DM.001 | Must | The platform SHALL support definition of business objects (entities) with typed attributes. |
| DM.002 | Must | The platform SHALL support relationships between business objects (one-to-one, one-to-many, many-to-many). |
| DM.003 | Must | The platform SHALL support process data objects carrying state through workflow execution. |
| DM.004 | Must | The platform SHALL support sub-process data passing with argument mapping. |
| DM.005 | Must | The platform SHALL support data associations binding form fields to business object attributes. |
| DM.006 | Must | The platform SHALL support external data source integration for reference data lookup. |
| DM.007 | Must | The platform SHALL support data validation rules at object and attribute level. |
| DM.008 | Must | The platform SHALL support calculated attributes derived from other data elements. |
| DM.009 | Must | The platform SHALL support data historization tracking changes over time. |
| DM.010 | Must | The platform SHALL support integration with legacy Informix databases for read-only data access. |
| DM.011 | Must | The platform SHALL support integration with ORS/ClickHouse for analytical data access. |
| DM.012 | Must | The platform SHALL support master data management for shared reference data. |

---

## 5.6 Events and Triggers (EC.xxx)

This subsection specifies requirements for the platform's event processing capabilities, enabling event detection, correlation, temporal processing, and workflow triggering.

**DM Use Case Mapping:** Event capabilities are critical for UC.DM.001 (automatic debt case creation triggered by payment overdue events from ORS), UC.DM.002 (timer-driven demand notice generation), and UC.DM.007 (escalation triggers based on enforcement action deadlines).

| ID | Priority | Requirement |
|---|---|---|
| EC.001 | Must | The platform SHALL support event detection based on data changes, workflow transitions, and external triggers. |
| EC.002 | Must | The platform SHALL support event correlation grouping related events. |
| EC.003 | Must | The platform SHALL support temporal operators for event processing (within time window, sequence detection). |
| EC.004 | Must | The platform SHALL support sliding window implementations for event aggregation. |
| EC.005 | Must | The platform SHALL support event-driven workflow triggering. |
| EC.006 | Must | The platform SHALL support event filtering based on attribute conditions. |
| EC.007 | Must | The platform SHALL support event transformation mapping external event formats. |
| EC.008 | Must | The platform SHALL support event persistence for replay and audit. |
| EC.009 | Must | The platform SHALL support event subscription management for external consumers. |
| EC.010 | Must | The platform SHALL support dead letter handling for failed event processing. |

---

## 5.7 Process Monitoring (PM.xxx)

This subsection specifies requirements for the platform's process monitoring capabilities, providing operational visibility into workflow execution, case throughput, and performance metrics.

**DM Use Case Mapping:** Process monitoring directly supports UC.DM.005 (automated reporting and management dashboard updates, ~1.8 FTE liberation at 90% automation) and provides the operational metrics foundation for all DM case management activities.

| ID | Priority | Requirement |
|---|---|---|
| PM.001 | Must | The platform SHALL provide standard metrics: process instance counts, completion rates, error rates, cycle times. |
| PM.002 | Must | The platform SHALL support user-defined metrics based on configurable calculations. |
| PM.003 | Must | The platform SHALL provide business indicators and KPIs aligned with MTCA operational goals. |
| PM.004 | Must | The platform SHALL provide dashboard capabilities with configurable visualisations. |
| PM.005 | Must | The platform SHALL support drill-down from aggregate metrics to individual cases. |
| PM.006 | Must | The platform SHALL support real-time monitoring of in-flight workflow instances. |
| PM.007 | Must | The platform SHALL support alerting when metrics breach defined thresholds. |
| PM.008 | Must | The platform SHALL support scheduled report generation and distribution. |
| PM.009 | Must | The platform SHALL support metric export for integration with MTCA management reporting. |
| PM.010 | Must | The platform SHALL support process mining capabilities identifying bottlenecks and improvement opportunities. |

---

## 5.8 Platform Integration (INT.xxx)

This subsection specifies requirements for the platform's integration capabilities with ITCAS Core, ORS/ClickHouse, SAS VIYA, and external systems.

**DM Use Case Mapping:** Integration requirements are essential for UC.DM.004 (SAS VIYA risk score consumption), UC.DM.006 (ORS/ClickHouse real-time debt portfolio visibility), and UC.DM.001 (ITCAS Core taxpayer data access for debt case creation).

### 5.8.1 ITCAS Core Integration

| ID | Priority | Requirement |
|---|---|---|
| INT.001 | Must | The platform SHALL integrate with ITCAS Core via REST APIs for taxpayer data access. |
| INT.002 | Must | The platform SHALL support OAuth 2.0 / OpenID Connect authentication for API access. |
| INT.003 | Must | The platform SHALL support API rate limiting configurable per client application. |
| INT.004 | Must | The platform SHALL provide comprehensive API documentation (OpenAPI/Swagger format). |
| INT.005 | Must | The platform SHALL support API versioning enabling backward compatibility. |

### 5.8.2 ORS/ClickHouse Integration

| ID | Priority | Requirement |
|---|---|---|
| INT.006 | Must | The platform SHALL connect to ORS/ClickHouse for debt portfolio data access. |
| INT.007 | Must | The platform SHALL support real-time data refresh from ORS dashboards. |
| INT.008 | Must | The platform SHALL integrate with Taxpayer 360° view for unified customer profile. |
| INT.009 | Must | The platform SHALL publish case status updates to ORS for dashboard visibility. |
| INT.010 | Must | The platform SHALL support configurable data refresh frequencies. |

### 5.8.3 SAS VIYA Integration

| ID | Priority | Requirement |
|---|---|---|
| INT.011 | Must | The platform SHALL receive risk scores from SAS VIYA via REST API. |
| INT.012 | Must | The platform SHALL support batch import of risk scoring results (hourly frequency). |
| INT.013 | Must | The platform SHALL support real-time risk score lookup for case prioritisation. |
| INT.014 | Must | The platform SHALL support confidence score integration for risk decisions. |
| INT.015 | Should | The platform SHOULD support case recommendation integration from SAS VIYA. |

### 5.8.4 External System Integration

| ID | Priority | Requirement |
|---|---|---|
| INT.021 | Should | The platform SHOULD support integration with banking systems for payment verification. |
| INT.022 | Should | The platform SHOULD support integration with government registries (business, persons). |
| INT.023 | Must | The platform SHALL support SMTP email integration for correspondence delivery. |
| INT.024 | Must | The platform SHALL support LDAP/Active Directory integration for user authentication. |
| INT.025 | Must | The platform SHALL support webhook configuration for external system notification. |

---

## 5.9 Platform Architecture (OA.xxx)

This subsection specifies requirements for the platform's configuration management, deployment pipeline, and version control architecture.

**DM Use Case Mapping:** Architecture requirements are cross-cutting, ensuring that all DM workflows, forms, and rules can be developed, tested, and deployed reliably. They are particularly important for the phased DM deployment (Phase 2: Q3–Q4 2026) where configuration changes must be managed across development, test, and production environments.

| ID | Priority | Requirement |
|---|---|---|
| OA.001 | Must | The platform SHALL maintain a Configuration Management Repository storing all workflow, form, and rule definitions. |
| OA.002 | Must | The platform SHALL support organisation of configuration changes into logical projects or change sets. |
| OA.003 | Must | The platform SHALL provide a deployment pipeline promoting configurations from development through test to production. |
| OA.004 | Must | The platform SHALL support version control for all configuration elements with history and comparison. |
| OA.005 | Must | The platform SHALL enable parallel configuration development by multiple users without conflicts. |
| OA.006 | Must | The platform SHALL provide configuration validation before deployment checking for errors and conflicts. |
| OA.007 | Should | The platform SHOULD support branching and merging of configuration versions for parallel development streams. |
| OA.008 | Must | The platform SHALL maintain separation between configuration metadata and runtime execution data. |
| OA.009 | Must | The platform SHALL support hot deployment of configuration changes without system restart. |
| OA.010 | Must | The platform SHALL provide configuration audit trail showing who changed what and when. |

---

## 5.10 General Platform Requirements (GN.xxx)

This subsection specifies the overarching platform requirements ensuring the Case Management Platform provides integrated BPM, forms, rules, and event capabilities with a strong emphasis on business user configurability.

| ID | Priority | Requirement |
|---|---|---|
| GN.001 | Must | The platform SHALL provide BPM/workflow management capabilities enabling visual design of executable business processes. |
| GN.002 | Must | The platform SHALL provide forms management capabilities enabling drag-and-drop form creation and modification. |
| GN.003 | Must | The platform SHALL provide business rules management capabilities enabling IF-THEN rule and decision table configuration. |
| GN.004 | Must | The platform SHALL provide events configuration capabilities enabling event detection and workflow triggering. |
| GN.005 | Must | The platform SHALL distinguish between business user configuration (forms, simple rules) and technical staff configuration (complex integrations). |
| GN.006 | Must | The platform SHALL enable 80%+ of configuration activities to be performed by trained business users without developer involvement. |
| GN.007 | Must | The platform SHALL provide configuration documentation generated automatically from configured elements. |
| GN.008 | Must | The platform SHALL support configuration testing in isolated environments before production deployment. |
| GN.009 | Must | The platform SHALL provide rollback capability for configuration changes causing production issues. |
| GN.010 | Must | The platform SHALL support configuration export/import for backup and environment promotion. |

---

## 5.11 Operational Metrics (PRM.xxx — Subset)

This subsection includes the Performance Management requirements relevant to STA accounting operations: PRM.006–PRM.010 (operational throughput, cycle-time, quality, and SLA metrics) and PRM.016–PRM.019 (automation and freed-capacity tracking). These metrics measure the efficiency of payment allocation, suspense resolution, statement generation, and reconciliation.

**Use Case Mapping:** These metrics support the accounting-operations use cases (payment allocation, suspense resolution, reconciliation, statement generation) and provide the measurement framework for tracking automation-driven capacity gains.

### 5.11.1 KPI Metrics and Calculation

| ID | Priority | Requirement |
|---|---|---|
| PRM.006 | Must | The platform SHALL calculate workflow throughput metrics (cases processed per period, per user). |
| PRM.007 | Must | The platform SHALL calculate cycle time metrics (average case resolution time by case type). |
| PRM.008 | Must | The platform SHALL calculate quality metrics (error rates, rework rates, first-time-right percentages). |
| PRM.009 | Must | The platform SHALL support custom KPI definition based on configurable formulas. |
| PRM.010 | Must | The platform SHALL calculate SLA compliance metrics for service request resolution. |

### 5.11.2 FTE Liberation Tracking Alignment

| ID | Priority | Requirement |
|---|---|---|
| PRM.016 | Must | The platform SHALL track automation rates showing percentage of cases processed without manual intervention. |
| PRM.017 | Must | The platform SHALL measure task completion times enabling pre/post automation comparison. |
| PRM.018 | Must | The platform SHALL report on freed capacity metrics aligned with the 50 FTE liberation programme. |
| PRM.019 | Should | The platform SHOULD integrate with the programme-level capacity-tracking dashboard. |

---

## 5.12 Automation and Capacity Cross-Reference

The following matrix maps the STA's automatable accounting operations to the platform capability areas that enable them.

| Accounting Operation | Automation | Primary Platform Capabilities |
|---|---|---|
| Consolidated balance assembly and drill-down (L1→L2→L3) | High | INT (ORS/ClickHouse read), DM-data (data source access), PM (dashboards) |
| Payment allocation (charge-type / oldest-period / tax-type sequences) | High | BR (allocation rules), EC (event triggers), WF (posting workflow) |
| Interest and penalty computation (daily, intra-period rate changes) | High | BR (rate rules), EC (scheduled triggers), FM (rate configuration) |
| Cross-tax-type set-off and refund interception | Medium–High | BR (set-off priority rules), WF (approval workflow), EC (refund triggers) |
| Suspense resolution and payment correction cases | Medium | WF (case workflows), FM (forms), CFW (correspondence), BR (approval thresholds) |
| Daily and Treasury reconciliation | High | EC (scheduled batch), BR (tolerance rules), PM (exception monitoring) |
| Statement and certificate generation (TAS/TCC) | High | CFW (templates, correspondence), FM (forms), DM-data (read access) |


## 5.13 External Dependencies

The platform capability requirements in this section assume the availability of the following external systems:

| System | Dependency Type | Capabilities Required |
|---|---|---|
| ORS/ClickHouse | Data source (read) + status publish (write) | Taxpayer accounting views, consolidated balances, transaction detail |
| Analytics platform (downstream) | Data consumer | Consumes STA enforceable balances and credit positions; risk scoring is not an STA function |
| ITCAS Core | API integration | Taxpayer data, tax account data, assessment data |
| Legacy Informix databases | Data source (read-only) | Historical balances and reference data during transition period |
| LDAP/Active Directory | Authentication | User authentication, role mapping |
| SMTP mail server | Correspondence delivery | Email notification, letter delivery tracking |
| Government postal services | Optional integration | Physical mail tracking (CFW.034) |
| Banking systems | Optional integration | Payment verification (INT.021) |
| Government registries | Optional integration | Business and persons registry lookup (INT.022) |

---

*End of Section 5*


---

# 6. NON-FUNCTIONAL REQUIREMENTS

This section defines measurable non-functional requirements covering performance, availability, scalability, security, usability, data quality, auditability, maintainability, and portability.

## 6.1 Performance

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-001 | Single-taxpayer account lookup (STA L1–L3 data retrieval) shall complete within the specified response time under normal operational load (≤100 concurrent users). | Must | Response time ≤2 seconds (95th percentile) | ORS Implementation Plan (60x improvement target) |
| NFR-002 | Workflow actions (case state transition, task assignment, form submission) shall complete within the specified response time. | Must | Response time ≤5 seconds (95th percentile) | Industry standard for transactional workflow |
| NFR-003 | Dashboard and report rendering (including data retrieval from ORS/ClickHouse) shall complete within the specified response time for standard reports. | Must | Initial load ≤10 seconds; refresh ≤5 seconds (95th percentile) | ORS Implementation Plan (Superset <1 second for cached) |
| NFR-004 | Batch operations (bulk case creation, mass notification generation, scheduled report generation) shall process within the specified throughput rate. | Should | ≥500 cases per minute for bulk operations; ≥1,000 notifications per hour | Vendor Proposal §6.8 (bulk processing) |
| NFR-005 | The system shall support a minimum of 150 concurrent users (approximately 20% of MTCA's 700+ staff) without performance degradation below the specified response time targets. | Must | All response time targets met at 150 concurrent users | MTCA staff size (700+); typical concurrent usage 15–20% |
| NFR-006 | Database queries powering operational reports shall complete within the specified time. Ad-hoc analytical queries shall complete within a separate, longer timeout. | Should | Operational queries ≤5 seconds; ad-hoc queries ≤30 seconds | ORS Implementation Plan (ClickHouse performance) |

## 6.2 Availability and Reliability

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-007 | The system shall be available during MTCA business hours (Monday–Friday, 07:00–19:00 CET) with the specified uptime percentage, measured monthly. | Must | 99.5% availability during business hours (≤2.6 hours unplanned downtime per month) | Government service availability standard |
| NFR-008 | Planned maintenance windows shall be scheduled outside business hours with a minimum advance notification period to all users. | Must | ≥48 hours advance notice; maintenance window: Saturday 22:00–Sunday 06:00 CET | MITA operational standards |
| NFR-009 | The system shall recover from unplanned outages within the specified recovery time objective (RTO) and recover data to within the specified recovery point objective (RPO). | Must | RTO ≤4 hours; RPO ≤1 hour (maximum 1 hour of data loss) | Government IT disaster recovery standards |
| NFR-010 | Automated health checks shall monitor system availability and alert administrators within the specified time when any component becomes unavailable. | Should | Alert within ≤5 minutes of component failure | ORS Implementation Plan (Grafana monitoring) |

## 6.3 Security

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-011 | The system shall implement Role-Based Access Control (RBAC) with the following minimum roles: Case Officer, Senior Officer, Supervisor, Manager, Administrator, Auditor (read-only), and System Administrator. Each role shall have configurable permissions for: view, create, edit, approve, and delete operations per entity type. | Must | All system functions gated by role permissions. Permission changes effective immediately. Unauthorised access attempts logged and blocked. Role hierarchy supported (supervisor inherits officer permissions). | MITA security standards; GDPR requirements |
| NFR-012 | All data in transit shall be encrypted using TLS 1.2 or higher. All data at rest containing personally identifiable information (PII) shall be encrypted using AES-256 or equivalent. | Must | TLS 1.2+ for all HTTP, API, and database connections. Encryption at rest for: taxpayer names, addresses, TINs, financial data. Encryption key management per MITA standards. | GDPR Article 32; MITA security standards |
| NFR-013 | The system shall comply with GDPR requirements including: data subject access requests (right of access), right to rectification, data minimisation, purpose limitation, and data protection impact assessment outcomes. | Must | DSAR process: response within 30 calendar days. Data fields classified by sensitivity level. PII access logged. Data retention periods enforced automatically. Privacy impact assessment completed before go-live. | GDPR Articles 15–22; Maltese Data Protection Act |
| NFR-014 | User sessions shall automatically terminate after a configurable period of inactivity. Failed login attempts shall trigger account lockout after a configurable threshold. | Must | Session timeout: configurable (default 30 minutes). Account lockout: after 5 consecutive failed attempts; auto-unlock after 30 minutes or manual unlock by administrator. All authentication events logged. | MITA security standards |
| NFR-015 | The system shall support data masking for sensitive fields (TIN, name, financial amounts) when displayed to users who do not have full-access permissions. Partial masking (e.g., last 4 digits of TIN) shall be configurable per field and per role. | Should | Masking rules configurable per field and per role. Masked data cannot be copied/exported in unmasked form by restricted users. Masking consistent across all views (screen, reports, exports). | GDPR; MITA data classification policy |

## 6.4 Usability

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-016 | The user interface shall comply with WCAG 2.1 Level AA accessibility standards to ensure usability for all MTCA staff. | Should | WCAG 2.1 AA compliance verified by automated testing tool (e.g., axe, WAVE). Manual testing for keyboard navigation and screen reader compatibility. Compliance report produced before go-live. | CMP Evaluation §4.2 (selected platform: partial compliance); EU accessibility directive |
| NFR-017 | The user interface shall support both Maltese and English languages, with the ability to switch language at any time without losing session context. | Must | All system labels, messages, and help text available in both languages. Language preference saved per user. Reports and documents generated in user's selected language or taxpayer's preferred language as appropriate. | PDF §TAS templates; Maltese language requirements |
| NFR-018 | New case officers shall be able to perform core tasks (view taxpayer account, create case, log activity, generate document) after completing a training programme of specified maximum duration. | Should | Training programme ≤3 days for core competency. Task completion rate ≥90% within 5 days of training. Measured via post-training assessment. Contextual help available on all screens. | CMP Evaluation §4 (selected platform: faster learning curve) |
| NFR-019 | The user interface shall be responsive and functional on standard desktop browsers (Chrome, Edge, Firefox — latest two major versions) at minimum resolution of 1280×720. Mobile access shall be supported for supervisory functions (dashboard viewing, approval actions). | Must | Cross-browser compatibility tested. Desktop layout optimised for 1920×1080. Mobile layout functional for: dashboard viewing, case approval, notification review. No plugin or extension requirements. | CMP Evaluation §4.2 (mobile responsiveness) |

## 6.5 Data Quality

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-020 | STA balance calculations shall maintain arithmetic accuracy with zero tolerance for rounding errors in consolidated balances. All financial amounts shall use a minimum of 2 decimal places (Euro cents). | Must | L1 consolidated balance = sum of L2 balances ± €0.00. L2 tax type balance = sum of L3 period balances ± €0.00. Reconciliation checks run automatically on every balance update. Discrepancy > €0.01 triggers alert. | PDF §3.3.1 (L1/L2/L3 hierarchy); BR-ACC-01–05 |
| NFR-021 | Data imported from ORS/ClickHouse shall be validated against defined business rules before display. Invalid or missing data shall be flagged visually to users and logged for investigation. | Must | Validation rules: mandatory field completeness, referential integrity (TIN exists in register), amount reasonableness (no negative tax liabilities unless credit), date validity. Invalid records flagged with specific validation failure reason. Data quality dashboard showing: % records passing validation by source table. | STA-FR-044 (migration); ORS Implementation Plan §ETL layer |
| NFR-022 | Duplicate detection shall prevent creation of duplicate cases for the same taxpayer, tax type, and tax period combination. Near-duplicate detection shall alert users when similar cases exist. | Must | Exact-match duplicate blocked at creation. Near-duplicate alert shows: existing case ID, status, creation date. User can override near-duplicate alert with mandatory justification. Duplicate check response time <2 seconds. | WF-FR-003; Vendor Proposal §6.7 |

## 6.6 Auditability

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-023 | All system activities shall be logged in an immutable audit trail including: user actions, system events, data modifications (before and after values), API calls, and configuration changes. | Must | Audit log entries: timestamp (UTC), user ID, action, entity, before/after values, IP address, session ID. Audit log cannot be modified or deleted by any user. Audit log searchable by: date range, user, action type, entity. Retention: ≥10 years. | WF-FR-020; GDPR; Maltese tax administration law |
| NFR-024 | Audit reports shall be generatable showing: user activity summaries, data access patterns, configuration change history, and exception events, for any specified date range. | Should | Standard audit reports: user login/logout history, data access by user, configuration changes, failed access attempts, SLA breaches. Reports exportable to PDF and Excel. Generation time <60 seconds for 30-day report. | GDPR; internal audit requirements |

## 6.7 Maintainability and Configurability

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-025 | Business rules (debt category thresholds, escalation sequences, SLA durations, notification schedules, interest rates, penalty rates) shall be configurable by authorised administrators through the platform's UI, without requiring code changes or developer intervention. | Must | ≥90% of business rules configurable via UI. Rule changes take effect within the specified activation period (default: immediately, or at next business day start). Rule change history maintained with: old value, new value, change date, changed by. | PDF §5.5 (Table 4); Vendor Proposal §6.6.1 (configurable elements); CMP Evaluation §4.3 |
| NFR-026 | New report templates and notification templates shall be deployable by trained MTCA staff using the platform's visual design tools, without requiring external vendor support. | Should | Template creation via drag-and-drop or visual editor. Average template creation time ≤4 hours for a trained administrator. Template testing in sandbox environment before production deployment. ≥80% of template changes achievable without vendor support. | CMP Evaluation §4.2 (form builder); CMP Justification §D.1 |
| NFR-027 | The system shall support separate environments for development, testing/UAT, and production, with controlled promotion of configurations and customisations between environments. | Must | Minimum 3 environments: DEV, UAT, PROD. Configuration promotion via documented export/import process. Environment parity: UAT mirrors PROD configuration. Promotion audit trail maintained. | MITA infrastructure standards |

## 6.8 Portability and Standards Compliance

| ID | Description | Priority | Measurable Target | Source |
|----|-------------|----------|-------------------|--------|
| NFR-028 | All workflow process definitions shall be exportable in BPMN 2.0 standard XML format, ensuring portability to any BPMN 2.0 compliant engine. | Must | Export validates against BPMN 2.0 XML schema (OMG standard). Exported processes importable into at least 2 independent BPMN tools (e.g., Camunda, Flowable) with visual equivalence. Export includes: process diagrams, decision points, participant lanes, message flows. | Project Brief §Key Constraints; INT-FR-010 |
| NFR-029 | Business rules shall be exportable in a structured, machine-readable format (DMN, spreadsheet, or JSON) to facilitate migration to ITCAS or alternative platforms. | Should | Decision tables exportable as XLSX or DMN 1.3 XML. Conditional rules exportable as JSON or XML. Exported rules include: name, conditions, actions, priority, effective dates. Export completeness: ≥95% of configured rules included. | INT-FR-011; Project Brief §Key Constraints |
| NFR-030 | All APIs exposed by the system shall conform to RESTful design principles with OpenAPI 3.0 specification documentation, enabling integration with ITCAS and other future systems. | Must | OpenAPI 3.0 spec auto-generated and published. API versioning supported. Backward-compatible changes default; breaking changes require version increment. API documentation includes: endpoints, parameters, request/response schemas, authentication, error codes. | CMP Evaluation §5.1; ORS Implementation Plan §Consumers Layer |
| NFR-031 | The system shall not create dependencies on proprietary data formats, protocols, or platform-specific features that would prevent migration to an alternative platform. All data shall be exportable in standard formats (CSV, JSON, XML, SQL). | Must | Full data export in: CSV (for tabular data), JSON (for configuration and hierarchical data), SQL (for relational data). Export includes all: case data, workflow history, documents (as files + metadata), configuration, audit trail. Export documentation: data dictionary with field descriptions and data types. | CMP Justification §F.7; Project Brief §Key Constraints |

---

---

# 7. USE CASE MODEL

This section presents the Single Taxpayer Account use cases (UC-STA-01 to UC-STA-13), the accounting-relevant reporting use case (Revenue Reconciliation), and the administration use cases. Debt-management use cases (UC-DM-xx) are specified in the Debt Management Requirements Specification.

## 7.1 Single Taxpayer Account Use Cases

This section presents the detailed use case specifications for the Single Taxpayer Account (STA) domain. Each use case is fully elaborated with actors, preconditions, step-by-step scenarios, alternative and exception flows, postconditions, and traceability to the functional requirements defined in Section 4.1.

Business rules referenced herein (BR-STA-xxx) are formally defined in Section 8 (Business Rules Catalogue). Forward references are used here for traceability; the rule catalogue provides the authoritative definitions.

**Actor Definitions:**

| Actor | Description |
|-------|-------------|
| **Tax Officer (TO)** | MTCA operational staff responsible for taxpayer account management, payment processing, and reconciliation. Has read/write access to assigned accounts. |
| **Senior Tax Officer (STO)** | Supervisory MTCA staff with approval authority for adjustments, write-offs, and payment corrections above configured thresholds. |
| **System Administrator (SA)** | MTCA IT/configuration staff responsible for configuring rates, rules, templates, and system parameters. |
| **Taxpayer (TP)** | Natural person (NP) or legal person (LP) registered with MTCA, accessing their account via the self-service portal. |
| **System (SYS)** | The STA solution itself, performing automated batch and real-time processing (interest calculation, balance roll-over, reconciliation). |
| **External System (EXT)** | ORS/ClickHouse data warehouse, ITCAS (future), Treasury system, or payment gateway providing data feeds. |

---

### UC-STA-01: View Unified Taxpayer Account

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary), Senior Tax Officer, Taxpayer (via portal) |
| **Priority** | Must |
| **Frequency** | ~500–800/day (most frequent STA interaction) |
| **Related Requirements** | STA-FR-001, STA-FR-002, STA-FR-003, STA-FR-004, STA-FR-005, STA-FR-006, STA-FR-007, STA-FR-008, STA-FR-010, STA-FR-011 |
| **Related Business Rules** | BR-STA-001 (TTT hierarchy), BR-STA-002 (balance computation), BR-STA-003 (disputed amount exclusion) |

**Preconditions:**

- The taxpayer exists in the taxpayer register with a valid TIN.
- The user is authenticated and authorised for account view access (RBAC).
- ORS/ClickHouse data warehouse is available and synchronised within the last 24 hours.
- At least one financial transaction exists for the taxpayer.

**Main Success Scenario:**

1. The Tax Officer enters a search key (TIN, document ID, payment ID, or taxpayer name) in the account search interface.
2. The system validates the search input and queries the ORS/ClickHouse data warehouse.
3. The system returns matching taxpayer record(s). If multiple matches exist, the system displays a disambiguation list with TIN, name, taxpayer type (NP/LP), and registration status.
4. The Tax Officer selects the target taxpayer from the results.
5. The system displays the **Level 1 (L1) consolidated view** showing: taxpayer header (TIN, name, type, registration status), aggregate debit balance, aggregate credit balance, net position, and number of active tax types.
6. The Tax Officer selects a specific tax type to drill down.
7. The system displays the **Level 2 (L2) tax type view** showing: tax type name and code, aggregate balance for the tax type (decomposed into PA, IA, PCA components), disputed amounts (shown separately per BR-STA-003), enforceable balance (total minus disputed), and list of tax periods with individual balances.
8. The Tax Officer selects a specific tax period to drill down further.
9. The system displays the **Level 3 (L3) transaction detail view** showing: opening balance (carried forward from prior period), all individual debit transactions (Dx) and credit transactions (Cx), each with date/time, amount, transaction type (liability, payment, refund, transfer, write-off, reversal), charge type (PA, IA, PCA), document reference, and posting user. The closing balance is computed as: opening balance + Σ(debits) − Σ(credits).
10. The Tax Officer reviews the transaction detail. The system provides a navigation breadcrumb for returning to L2 or L1 views.
11. The system logs the account access event to the audit trail (user, TIN, timestamp, access level reached).

**Alternative Flows:**

- **AF-1: Taxpayer Portal Access.** At step 1, the Taxpayer logs in via the self-service portal. The system automatically displays the Taxpayer's own L1 consolidated view (no search required). The Taxpayer can drill down to L2 and L3 in read-only mode. Navigation and data are identical to the staff view except that modification functions are hidden.
- **AF-2: Non-periodic Tax Type.** At step 7, if the tax type does not use tax periods (e.g., customs duties, duty on documents), the system uses the transaction date as the notional period identifier per STA-FR-007. The L2 view groups transactions by calendar quarter or configurable grouping period instead of tax period.
- **AF-3: Partial Name Search.** At step 1, if the Tax Officer searches by taxpayer name, the system performs a partial-match search. If more than 50 results are returned, the system prompts for additional filtering criteria (taxpayer type, registration tax office, status).
- **AF-4: Bottom-Up Navigation.** The Tax Officer starts from a specific transaction reference (document ID or payment ID). The system locates the transaction at L3, then provides upward navigation to the parent L2 tax type view and L1 consolidated view per STA-FR-004.

**Exception Flows:**

- **EF-1: TIN Not Found.** At step 2, if the search key returns no matches, the system displays "No taxpayer found for the given search criteria" and prompts the Tax Officer to verify the input or try an alternative search key.
- **EF-2: Data Warehouse Unavailable.** At step 2, if ORS/ClickHouse is unreachable, the system displays a service unavailability message with estimated recovery time (if available) and logs the incident. The Tax Officer cannot proceed until the data source is restored.
- **EF-3: Access Denied.** At step 4, if the user's RBAC role does not permit access to the selected taxpayer's account (e.g., restricted TIN list), the system denies access, displays "You are not authorised to view this account," and logs the unauthorised access attempt.
- **EF-4: Balance Inconsistency Detected.** At step 9, if the computed closing balance does not reconcile with the stored closing balance (variance > €0.01), the system flags the account with a reconciliation warning icon, logs the discrepancy for review, and displays both values with a "Balance under review" indicator.

**Postconditions:**

- The taxpayer's account data has been displayed to the authorised user at the requested detail level.
- An audit trail entry has been recorded for the access event.
- No data has been modified (view-only operation).

---

### UC-STA-02: Generate Taxpayer Statement of Account

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary), Taxpayer (via portal), Senior Tax Officer |
| **Priority** | Must |
| **Frequency** | ~100–200/day (on-demand), ~50,000/month (batch for annual campaign) |
| **Related Requirements** | STA-FR-037, STA-FR-038, STA-FR-039, STA-FR-040, STA-FR-010, STA-FR-011 |
| **Related Business Rules** | BR-STA-004 (positive clearance criteria), BR-STA-005 (negative clearance content), BR-STA-006 (statement template selection) |

**Preconditions:**

- The taxpayer exists in the system with at least one tax type registration.
- The user is authenticated and authorised for statement generation.
- Statement templates are configured (at least: NP clearance, LP clearance, detailed statement, summary statement) in English and Maltese.

**Main Success Scenario:**

1. The Tax Officer navigates to the taxpayer account (via UC-STA-01) and selects "Generate Statement."
2. The system presents statement configuration options: statement type (Tax Account Statement or Tax Clearance Certificate), date range (from/to or "as at date"), tax type scope (all types or selected types), charge component filter (all, PA only, IA only, PCA only), output language (English, Maltese), and output format (PDF, on-screen, printable).
3. The Tax Officer selects the desired options and confirms.
4. The system retrieves all relevant account data for the specified scope and date range from ORS/ClickHouse.
5. **For Tax Account Statement (TAS):** The system generates the statement showing L1 consolidated balance, L2 per-tax-type balances, and L3 transaction details for the selected period range. Each balance level is decomposed into PA, IA, and PCA components. Disputed amounts are shown separately.
6. **For Tax Clearance Certificate (TCC):** The system evaluates the taxpayer's consolidated position:
   - If total overdue debit balance ≤ 0 (no overdue liabilities): generate **Positive Clearance** certificate with validity period (configurable, default 30 days), taxpayer details, certification statement, and issue date.
   - If overdue debit balance > 0: generate **Negative Clearance** certificate listing each overdue item by tax type, tax period, charge component (PA/IA/PCA), amount, and overdue-since date. Include operational provisos and legal instructions.
7. The system renders the statement/certificate using the appropriate template variant (NP/LP, language) and presents it to the Tax Officer.
8. The Tax Officer reviews and either delivers to the taxpayer, prints, or saves.
9. The system logs the statement generation event (type, TIN, date range, generating officer, delivery method).

**Alternative Flows:**

- **AF-1: Taxpayer Self-Service Request.** At step 1, the Taxpayer logs in to the self-service portal and requests a statement. The system presents simplified options (statement type, date range, language). The statement is generated and delivered electronically within 5 minutes. No officer involvement required.
- **AF-2: Batch Statement Generation.** A Senior Tax Officer initiates batch generation for a cohort of taxpayers (e.g., all active LPs, all taxpayers with overdue balances). The system queues generation, processes asynchronously, and delivers results as a downloadable archive or queues for postal dispatch.
- **AF-3: Instalment Compliance Override.** At step 6, the taxpayer has overdue liabilities but is compliant with an active instalment agreement. The system applies BR-STA-004 (instalment compliance may qualify as "conditionally positive") and generates a conditional clearance certificate noting the active instalment arrangement.

**Exception Flows:**

- **EF-1: No Data for Period.** At step 4, if no transactions exist for the selected date range, the system informs the Tax Officer and offers to generate a statement showing zero balances or to adjust the date range.
- **EF-2: Template Not Available.** At step 7, if the required template variant (e.g., Maltese LP clearance) is not configured, the system falls back to the default English version and alerts the Tax Officer that the requested language variant is unavailable.
- **EF-3: Generation Timeout.** At step 4, if data retrieval exceeds 30 seconds (complex account with thousands of transactions), the system offers to generate asynchronously and notify the Tax Officer upon completion.

**Postconditions:**

- A statement or certificate has been generated and delivered or made available to the requestor.
- The generation event is recorded in the audit trail with full parameters.
- For TCC: the certificate record is stored with its validity period for future verification.

---

### UC-STA-03: Calculate Interest on Overdue Tax

| Field | Value |
|-------|-------|
| **Actor(s)** | System (primary — automated batch), Tax Officer (manual trigger), System Administrator (rate configuration) |
| **Priority** | Must |
| **Frequency** | Daily batch (all overdue accounts), ~20–50/day manual recalculation |
| **Related Requirements** | STA-FR-023, STA-FR-024, STA-FR-025, STA-FR-026, STA-FR-010, STA-FR-014 |
| **Related Business Rules** | BR-STA-007 (daily interest rate application), BR-STA-008 (rate change within period), BR-STA-009 (instalment lower rate), BR-STA-010 (refund interest) |

**Preconditions:**

- Interest rates are configured with valid, non-overlapping validity periods.
- At least one overdue tax liability exists in the system.
- The daily batch processing window is available (for automated execution).
- ORS/ClickHouse data is synchronised with current liability data.

**Main Success Scenario:**

1. The System initiates the daily interest calculation batch process at the configured time (e.g., 02:00 daily).
2. The System identifies all taxpayer accounts with overdue liabilities (principal tax balance where due date has passed and balance > 0).
3. For each overdue liability, the System:
   a. Determines the start date for interest calculation (the day after the due date, or the last date interest was calculated).
   b. Determines the end date (current date).
   c. For each day in the calculation period, retrieves the applicable interest rate for that day from the rate configuration table.
   d. Calculates daily interest: (outstanding principal amount × daily rate / 365).
   e. If the interest rate changed within the calculation period (BR-STA-008), the System segments the calculation: days at old rate + days at new rate.
   f. Accumulates the total interest amount for the period.
4. The System checks whether the taxpayer has an active, compliant instalment agreement (BR-STA-009).
   - If yes and all instalment payments are current: applies the configured lower instalment interest rate.
   - If instalment payment missed: reverts to the standard rate from the date of non-compliance.
5. The System posts the calculated interest amount as an IA (Interest Amount) debit transaction to the taxpayer's account, with the corresponding contra-entry to the revenue interest account (double-entry per STA-FR-012).
6. The System records the full calculation flow: liability reference, period, daily rates used, number of days at each rate, total interest amount, and posting reference.
7. The System generates a daily interest calculation summary report: total accounts processed, total interest posted, exceptions/errors encountered.

**Alternative Flows:**

- **AF-1: Manual Recalculation.** A Tax Officer triggers interest recalculation for a specific taxpayer account (e.g., after a payment reversal or balance adjustment). The System performs the calculation for the selected account only, using the same algorithm as the batch process. The Tax Officer reviews the result before confirming the posting.
- **AF-2: Disputed Amount Exclusion.** At step 3, if the overdue liability (or portion thereof) is flagged as disputed (per STA-FR-011), the System excludes the disputed amount from the interest calculation base. Interest is calculated only on the undisputed portion.
- **AF-3: Late Refund Interest.** The System detects a refund that was processed beyond the standard processing period. It calculates interest owed to the taxpayer using the configurable refund-interest rate (BR-STA-010) and posts a credit (IA) to the taxpayer's account.

**Exception Flows:**

- **EF-1: Missing Rate Configuration.** At step 3c, if no interest rate is configured for a specific date (gap in validity periods), the System skips that account, logs an error referencing the date gap, and includes the account in the exception report for administrator review.
- **EF-2: Negative Outstanding Amount.** At step 3, if the outstanding amount is negative (credit balance), the System skips interest calculation for that liability. No interest is charged on credit balances.
- **EF-3: Batch Processing Failure.** If the batch process fails mid-execution, the System marks all unprocessed accounts as "pending" and generates an alert. The next batch run resumes from the last successfully processed account. Already-posted interest entries are not duplicated.

**Postconditions:**

- Interest amounts have been calculated and posted to all applicable overdue accounts.
- Double-entry bookkeeping integrity is maintained (taxpayer IA debit = revenue interest account credit).
- The interest calculation log is available for audit and taxpayer notification purposes.
- The daily summary report is available for management review.

---

### UC-STA-04: Process Payment Allocation

| Field | Value |
|-------|-------|
| **Actor(s)** | System (primary — automated), Tax Officer (manual override/correction) |
| **Priority** | Must |
| **Frequency** | ~300–500/day (automated payment processing) |
| **Related Requirements** | STA-FR-017, STA-FR-018, STA-FR-019, STA-FR-020, STA-FR-021, STA-FR-022, STA-FR-012, STA-FR-013 |
| **Related Business Rules** | BR-STA-011 (charge-type priority: interest → additional tax → principal → fees), BR-STA-012 (oldest-period-first), BR-STA-013 (tax-type priority sequence), BR-STA-014 (overpayment handling), BR-STA-015 (suspense posting) |

**Preconditions:**

- A payment has been received from a payment channel (bank, portal, counter) and is awaiting allocation.
- The payment record includes: amount, payer identification (TIN if known), and optionally tax type and/or period specification.
- Payment allocation rules are configured in the system.
- The taxpayer's account exists with current liability data.

**Main Success Scenario:**

1. The System receives a payment notification from the payment gateway/bank interface containing: payment amount, payment reference, date, payer TIN, and any taxpayer-specified allocation instructions (tax type, period).
2. The System validates the payer TIN against the taxpayer register.
3. **Scenario A — Fully specified payment (tax type + period known):**
   a. The System retrieves outstanding liabilities for the specified tax type and period.
   b. The System allocates the payment following the charge-type priority sequence (BR-STA-011): (1) interest due → (2) additional taxes due → (3) principal tax due → (4) fees due.
   c. For each charge type, the System posts a credit transaction (Cx) to the taxpayer account and a corresponding debit to the revenue account (double-entry).
   d. If the payment fully covers all liabilities for the period, the period status is updated to "paid."
4. **Scenario B — Tax type specified, no period:**
   a. The System retrieves all outstanding liabilities for the specified tax type, ordered by oldest period first (BR-STA-012).
   b. Starting with the oldest period, the System allocates using the charge-type sequence (BR-STA-011) until the payment is exhausted or all liabilities are cleared.
   c. Allocation proceeds period-by-period in chronological order.
5. **Scenario C — Consolidated payment (no tax type, no period):**
   a. The System applies the configured tax-type priority sequence (BR-STA-013).
   b. Within each tax type, the System applies oldest-period-first and charge-type priority as in Scenarios A and B.
   c. Allocation continues across tax types until the payment is exhausted.
6. The System generates an allocation log showing: for each liability covered, the tax type, period, charge type, amount allocated, and remaining balance.
7. If the payment exceeds all outstanding liabilities (overpayment), the System posts the excess to the taxpayer's general credit account (BR-STA-014) per STA-FR-020.
8. The System generates a taxpayer notification showing payment receipt confirmation and detailed allocation breakdown.

**Alternative Flows:**

- **AF-1: Unidentified Payer.** At step 2, if the TIN cannot be resolved, the System posts the payment to the tax revenue suspense account (BR-STA-015) per STA-FR-021. A case is created for identity resolution and assigned to an authorised officer. Upon resolution, the officer resubmits the payment with the correct TIN (triggering step 2 again).
- **AF-2: Manual Payment Override.** A Tax Officer overrides the automatic allocation to direct a payment to a specific liability (e.g., taxpayer explicitly requests payment against a specific assessment). The system records the override reason and the officer's identity. The charge-type sequence still applies within the targeted liability.
- **AF-3: Automatic Credit Application.** At step 1, instead of a new payment, a new liability is posted to an account that has an existing credit balance. The System automatically checks the credit account and applies available credit before generating a payment demand, per STA-FR-020.
- **AF-4: Partial Payment.** The payment amount is insufficient to cover even the first charge-type component of the oldest liability. The System allocates the full payment to interest (or whatever the first-priority charge type is) and records the remaining balance on all charge types. The liability remains "partially paid."

**Exception Flows:**

- **EF-1: Duplicate Payment Detection.** At step 1, the System detects a payment with the same reference number, amount, and date as an already-processed payment. The System flags it as a potential duplicate, posts to suspense, and creates a review case for the Tax Officer.
- **EF-2: Payment Amount Mismatch.** The payment amount is negative or zero. The System rejects the payment record, logs the error, and generates an alert for investigation.
- **EF-3: Payment Reversal Required.** After allocation (post step 6), it is discovered the payment was posted to the wrong TIN. See UC-STA-05 for the credit/debit adjustment workflow. The Tax Officer initiates a payment cancellation per STA-FR-022, which reverses all allocation entries, and then reprocesses the payment against the correct TIN.

**Postconditions:**

- The payment has been allocated to taxpayer liabilities following the configured priority rules.
- Double-entry bookkeeping integrity is maintained for all postings.
- The allocation log is recorded and available for audit and taxpayer notification.
- Overpayments are posted to the taxpayer's credit account.
- Unidentified payments are posted to the suspense account with a resolution case created.

---

### UC-STA-05: Process Credit/Debit Adjustment

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary initiator), Senior Tax Officer (approver for amounts above threshold) |
| **Priority** | Must |
| **Frequency** | ~30–60/day |
| **Related Requirements** | STA-FR-013, STA-FR-022, STA-FR-030, STA-FR-012, STA-FR-014 |
| **Related Business Rules** | BR-STA-016 (adjustment approval thresholds), BR-STA-017 (reversal linking), BR-STA-018 (double-entry adjustment) |

**Preconditions:**

- The taxpayer account exists and the Tax Officer has identified the transaction(s) requiring adjustment.
- The user is authenticated with the "adjustment initiation" role.
- A valid reason for the adjustment has been established (e.g., incorrect posting, assessment amendment, penalty waiver, payment reversal).

**Main Success Scenario:**

1. The Tax Officer navigates to the taxpayer account (via UC-STA-01) and selects the transaction requiring adjustment.
2. The Tax Officer selects the adjustment type: credit adjustment (increase taxpayer balance — e.g., refund, assessment reduction, penalty waiver) or debit adjustment (decrease taxpayer balance — e.g., additional assessment, penalty imposition).
3. The Tax Officer enters: adjustment amount, charge type (PA/IA/PCA), tax type, tax period, reason code (from configurable list), free-text justification, and supporting document reference.
4. The system validates the adjustment: amount is positive, charge type is valid, the target period exists, and mandatory fields are populated.
5. The system checks the adjustment amount against the configured approval threshold (BR-STA-016):
   - If below threshold: proceeds directly to posting (step 7).
   - If at or above threshold: routes to approval workflow (step 6).
6. The Senior Tax Officer receives the approval request, reviews the adjustment details and justification, and either approves or rejects.
   - If approved: proceeds to step 7.
   - If rejected: the system notifies the initiating Tax Officer with the rejection reason. The adjustment is cancelled. End of use case.
7. The system posts the adjustment as a debit or credit transaction to the taxpayer account with the corresponding contra-entry to the relevant revenue account (double-entry per STA-FR-012).
8. If the adjustment is a reversal of a prior transaction, the system links the reversal entry to the original transaction (BR-STA-017) and marks the original as "reversed."
9. The system recalculates the affected period balance and updates L2 and L1 aggregates.
10. The system logs the complete adjustment trail: initiator, approver (if applicable), original transaction reference, adjustment details, and timestamp.

**Alternative Flows:**

- **AF-1: Penalty/Interest Waiver.** At step 2, the Tax Officer selects "waive penalty" or "waive interest" (per STA-FR-030). The system creates a credit adjustment for the PCA or IA component respectively. If the waiver amount exceeds the configured threshold, Senior Tax Officer approval is required.
- **AF-2: Payment Cancellation and Reposting.** The adjustment is specifically to reverse an incorrectly posted payment (STA-FR-022). The Tax Officer initiates cancellation with mandatory reason capture. Upon approval, the system reverses all allocation entries generated by the original payment (across all affected periods and charge types) and then reprocesses the payment against the correct account.
- **AF-3: Batch Adjustment.** A Senior Tax Officer initiates a batch adjustment affecting multiple taxpayers (e.g., legislative rate change requiring mass reassessment). The system processes adjustments individually but within a single batch job, with a summary report upon completion.

**Exception Flows:**

- **EF-1: Insufficient Credit for Debit Adjustment.** At step 7, if a debit adjustment would result in a total liability exceeding a configured maximum or create an inconsistent state, the system warns the Tax Officer and requires confirmation before proceeding.
- **EF-2: Already-Reversed Transaction.** At step 1, if the selected transaction has already been reversed, the system prevents a duplicate reversal and displays the existing reversal details.
- **EF-3: Approval Timeout.** At step 6, if the Senior Tax Officer does not respond within the configured SLA (e.g., 48 hours), the system escalates the approval request to the next management level and notifies the initiating Tax Officer of the escalation.

**Postconditions:**

- The adjustment has been posted to the taxpayer account with full audit trail.
- Double-entry integrity is maintained.
- Affected balances (L3, L2, L1) are recalculated and consistent.
- If the adjustment resulted in a credit balance, it is available for future allocation or refund processing.
- The approval chain (if triggered) is fully documented.

---

### UC-STA-06: Reconcile Taxpayer Account

| Field | Value |
|-------|-------|
| **Actor(s)** | System (primary — automated daily), Tax Officer (manual reconciliation), Senior Tax Officer (exception resolution) |
| **Priority** | Must |
| **Frequency** | Daily batch (all accounts), ~10–20/day manual reconciliation |
| **Related Requirements** | STA-FR-034, STA-FR-035, STA-FR-036, STA-FR-012, STA-FR-009 |
| **Related Business Rules** | BR-STA-019 (reconciliation tolerance: €0.01), BR-STA-020 (daily reconciliation window), BR-STA-021 (Treasury reconciliation frequency) |

**Preconditions:**

- All transactions for the reconciliation period have been posted and finalised.
- ORS/ClickHouse data is fully synchronised.
- Revenue accounts are available for cross-reference.
- The daily batch window is available (for automated reconciliation).

**Main Success Scenario:**

1. The System initiates the daily automated reconciliation process at the configured time.
2. The System retrieves all transactions posted during the reconciliation period (previous 24 hours or configured period).
3. **Taxpayer-to-Revenue Reconciliation:** For each taxpayer account transaction, the System verifies that a corresponding contra-entry exists in the relevant revenue account (double-entry validation per STA-FR-012).
4. **Balance Integrity Check:** For each taxpayer and tax type, the System verifies: opening balance + Σ(period transactions) = closing balance per STA-FR-009.
5. **Cross-Tax-Type Totals:** The System verifies that the sum of all L2 tax type balances equals the L1 consolidated balance for each taxpayer.
6. The System compiles reconciliation results:
   - **Clean accounts:** transactions balanced within tolerance (€0.01 per BR-STA-019). No action required.
   - **Discrepancies:** accounts where taxpayer-side and revenue-side postings do not match, or balance computations fail.
7. The System generates the daily reconciliation report containing: total accounts processed, clean accounts count, discrepancy count, total discrepancy value, and detailed exception list (TIN, tax type, transaction IDs, discrepancy amount).
8. For zero-variance results, the System marks the reconciliation as "complete" for the period.
9. For discrepancies, the System creates exception cases assigned to designated Tax Officers for investigation.

**Alternative Flows:**

- **AF-1: Treasury Reconciliation.** In parallel with taxpayer-revenue reconciliation, the System generates a Treasury reconciliation report (per STA-FR-035) showing: total collections by tax type, transfers between types, net position per revenue account, and comparison with Treasury records. Discrepancies flagged within 24 hours.
- **AF-2: Manual Account Reconciliation.** A Tax Officer selects a specific taxpayer account and initiates a manual reconciliation. The System performs the same checks as the automated process but for the selected account only, with results displayed on-screen for immediate review.
- **AF-3: Reconciliation Act Process.** Per STA-FR-036, a Tax Officer generates a reconciliation statement for a specific taxpayer showing all outstanding liabilities. The statement is sent to the taxpayer for confirmation. The taxpayer responds with confirmed/disputed items. The Tax Officer updates the account accordingly (disputed items flagged per STA-FR-011).

**Exception Flows:**

- **EF-1: Orphaned Revenue Entry.** At step 3, a revenue account entry exists without a corresponding taxpayer account entry. The System flags this as a critical discrepancy and creates a high-priority case for investigation.
- **EF-2: Reconciliation Process Timeout.** If the daily reconciliation exceeds the configured batch window, the System completes processing for already-started accounts, marks remaining accounts as "deferred," and schedules continuation in the next available window.
- **EF-3: Systematic Discrepancy Pattern.** The System detects that discrepancies follow a pattern (e.g., all affecting a specific tax type or posting date), generates an alert suggesting a systemic issue, and escalates to the Senior Tax Officer and System Administrator.

**Postconditions:**

- All accounts have been reconciled or flagged for investigation.
- The daily reconciliation report is available for management review.
- Exception cases have been created and assigned for discrepancies.
- Treasury reconciliation data has been generated (if configured).
- The reconciliation status is recorded for audit compliance.

---

### UC-STA-07: View Transaction History with Drill-Down

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary), Senior Tax Officer, Taxpayer (via portal) |
| **Priority** | Must |
| **Frequency** | ~200–400/day |
| **Related Requirements** | STA-FR-003, STA-FR-004, STA-FR-005, STA-FR-008, STA-FR-013, STA-FR-014 |
| **Related Business Rules** | BR-STA-001 (TTT hierarchy), BR-STA-022 (transaction type classification), BR-STA-023 (charge type classification) |

**Preconditions:**

- The taxpayer account exists and has been located (via UC-STA-01 or direct navigation).
- The user is authenticated with appropriate view access.
- At least one transaction exists for the taxpayer.

**Main Success Scenario:**

1. The Tax Officer is viewing a taxpayer account (from UC-STA-01) and selects "Transaction History" from the account menu.
2. The system presents filtering options: date range (from/to), tax type (all or specific), tax period (all or specific), transaction type (liability, payment, refund, transfer, write-off, reversal, or all), charge type (PA, IA, PCA, or all), minimum/maximum amount, and posting user.
3. The Tax Officer applies desired filters and submits the query.
4. The system retrieves matching transactions from ORS/ClickHouse, ordered by transaction date (newest first by default, with sortable columns).
5. The system displays the transaction list with columns: date/time, transaction type, charge type, tax type, tax period, debit amount, credit amount, net impact, document reference, and posting user.
6. The Tax Officer selects an individual transaction for detail view.
7. The system displays the full transaction detail: all fields from step 5 plus — original document reference, source system, posting timestamp, approval chain (if applicable), linked transactions (e.g., the original transaction for a reversal, or the payment for an allocation), calculation flow (for interest and penalty transactions showing the computation basis), and any attached supporting documents.
8. The Tax Officer may navigate to linked transactions (e.g., clicking a reversal's "original transaction" link navigates to the original posting).
9. The system provides export functionality: export the filtered transaction list to CSV or PDF format.

**Alternative Flows:**

- **AF-1: Taxpayer Portal View.** The Taxpayer accesses their own transaction history via the self-service portal with the same filtering options. Internal fields (posting user, approval chain) are hidden. Export to PDF is available.
- **AF-2: Aggregate Summary View.** Instead of individual transactions, the Tax Officer selects "Summary View" which groups transactions by month, tax type, or transaction type, showing aggregate debit/credit amounts per group. Drill-down from summary to individual transactions is supported.
- **AF-3: Audit Trail View.** A Senior Tax Officer selects "Audit Trail" mode which adds columns for: access history (who viewed this transaction), modification history (all changes to the posting), and approval history (full approval chain with timestamps).

**Exception Flows:**

- **EF-1: Large Result Set.** At step 4, if the query returns more than 1,000 transactions, the system paginates the results (configurable page size, default 50) and displays the total count. Performance remains within the 3-second response target per page.
- **EF-2: No Matching Transactions.** At step 4, if no transactions match the filter criteria, the system displays "No transactions found for the selected criteria" and suggests broadening the filters.

**Postconditions:**

- The transaction history has been displayed per the user's filter criteria.
- Any exports have been generated and made available for download.
- Audit trail entries have been recorded for all access events.

---

### UC-STA-08: Process Set-Off (Credit Against Debt)

| Field | Value |
|-------|-------|
| **Actor(s)** | System (primary — automated), Tax Officer (manual set-off initiation), Senior Tax Officer (approval for large amounts) |
| **Priority** | Should |
| **Frequency** | ~50–100/day (automated), ~10–20/day (manual) |
| **Related Requirements** | STA-FR-032, STA-FR-033, STA-FR-016, STA-FR-020, STA-FR-012 |
| **Related Business Rules** | BR-STA-024 (cross-tax-type set-off priority), BR-STA-025 (refund interception rule), BR-STA-026 (minimum set-off threshold), BR-STA-027 (set-off notification) |

**Preconditions:**

- A taxpayer has a credit balance in at least one tax type account and a debit (overdue) balance in at least one other tax type account; or a refund has been approved and outstanding debts exist.
- Cross-tax-type set-off is legally authorised (regulatory prerequisite — see open question OQ-06 in requirements).
- Set-off priority sequence is configured in the system.

**Main Success Scenario:**

1. **Trigger:** The System detects a set-off opportunity through one of: (a) a new credit balance arising from overpayment or approved refund, (b) a scheduled batch set-off processing run, or (c) a Tax Officer manually initiates a set-off review for a specific taxpayer.
2. The System retrieves the taxpayer's complete account position across all tax types, identifying credit balances (sources) and overdue debit balances (targets).
3. The System applies the configured cross-tax-type set-off priority sequence (BR-STA-024) to determine which debts to clear first.
4. Within each target tax type, the System applies the standard charge-type priority (BR-STA-011: interest → additional tax → principal → fees) and oldest-period-first rule (BR-STA-012).
5. The System calculates the proposed set-off transactions: for each source-target pair, the amount to transfer, resulting in a debit to the credit account (source) and a credit to the liability account (target).
6. **For automated set-off:** If the total set-off amount is below the configured approval threshold, the System executes the set-off immediately.
7. **For large amounts or manual initiation:** The System presents the proposed set-off to the Tax Officer (or Senior Tax Officer for approval). The officer reviews and confirms or modifies the allocation.
8. The System posts all set-off transactions with double-entry bookkeeping: each transfer debits the source tax type's credit account and credits the target tax type's liability account, with corresponding revenue account entries.
9. The System generates a notification to the taxpayer detailing: the set-off action, amounts transferred from each source, amounts applied to each target, and the resulting account balances (BR-STA-027).
10. Any remaining credit balance after set-off is retained in the credit account for future allocation or refund processing.

**Alternative Flows:**

- **AF-1: Refund Interception.** At step 1, a refund has been approved for the taxpayer (trigger c from STA-FR-033). Before disbursement, the System checks for outstanding debts across all tax types. If debts exist, the System automatically intercepts the refund amount (up to the total debt), applies it per the set-off priority, and issues a net refund for any remaining amount. The full interception trail is recorded.
- **AF-2: Partial Set-Off.** The credit balance is insufficient to cover all outstanding debts. The System applies the available credit per the priority sequence, clearing debts partially. The remaining debts retain their overdue status and continue to accrue interest.
- **AF-3: Taxpayer-Initiated Transfer.** Via the self-service portal (future capability), the Taxpayer requests a transfer of credit from one tax type to cover a liability in another. The system validates the request, applies it per the set-off rules, and confirms to the Taxpayer.

**Exception Flows:**

- **EF-1: Disputed Liability Target.** At step 3, one or more target liabilities are flagged as disputed. The System excludes disputed amounts from the set-off calculation per STA-FR-011 and BR-STA-003. The Tax Officer is notified of the exclusion.
- **EF-2: Legal Authorisation Not Confirmed.** At step 1, if the legal authorisation for cross-tax-type set-off has not been confirmed for a specific tax type combination, the System flags the proposed set-off as "pending legal confirmation" and does not execute it automatically. A case is created for legal review.
- **EF-3: Concurrent Modification.** At step 8, if another process has modified the taxpayer's account between step 2 (retrieval) and step 8 (posting), the System detects the conflict, aborts the set-off, and re-runs from step 2 with fresh data.

**Postconditions:**

- Credit balances have been applied against outstanding debts per the configured priority sequence.
- All set-off transactions are posted with double-entry integrity.
- The taxpayer has been notified of the set-off action and resulting balances.
- For refund interception: the net refund amount (if any) is queued for disbursement.
- Full audit trail of the set-off is recorded.

---

### UC-STA-09: Roll Over End-of-Period Balances

| Field | Value |
|-------|-------|
| **Actor(s)** | System (primary — automated batch), System Administrator (scheduling and configuration), Senior Tax Officer (verification and sign-off) |
| **Priority** | Must |
| **Frequency** | End of each tax period (varies by tax type: monthly for VAT, quarterly for provisional tax, annually for CIT/PIT), plus annual fiscal year-end for all types |
| **Related Requirements** | STA-FR-009, STA-FR-015, STA-FR-041, STA-FR-012 |
| **Related Business Rules** | BR-STA-028 (balance forward method), BR-STA-029 (year-end processing sequence), BR-STA-030 (closing balance verification), BR-STA-031 (fiscal year calendar) |

**Preconditions:**

- The tax period end date has been reached (per the configured fiscal calendar).
- All transactions for the closing period have been posted and finalised (no pending payments in processing, no pending adjustments).
- The batch processing window is available.
- The System Administrator has confirmed the period-end processing schedule.

**Main Success Scenario:**

1. The System identifies that a tax period end date has been reached for one or more tax types (per the configured fiscal calendar BR-STA-031).
2. The System verifies that all prerequisites are met: no transactions in pending/processing status for the closing period, reconciliation for the period has been completed (UC-STA-06), and no blocking exceptions exist.
3. For each taxpayer and tax type affected:
   a. The System calculates the **closing balance** for the period: opening balance + Σ(all debit transactions) − Σ(all credit transactions), decomposed by charge type (PA, IA, PCA).
   b. The System verifies the closing balance against the running balance (BR-STA-030). If discrepancy > €0.01, the account is flagged for exception processing.
   c. The System records the period closing entry with the final balance.
   d. The System creates the **opening balance** for the new period, equal to the closing balance of the prior period, carrying forward the PA, IA, and PCA decomposition.
4. The System updates the revenue accounts (per STA-FR-041) with period-end totals: expected revenue (total liabilities), collected revenue (total payments), refundable amounts, and carried-forward balances.
5. The System generates the period-end reconciliation report confirming: zero-variance carry-forward for all clean accounts, list of exception accounts with discrepancy details, total balances carried forward by tax type.
6. For **fiscal year-end processing** (annual):
   a. The System performs all period-end steps above for the final period of the fiscal year.
   b. The System closes the fiscal year accounts for all tax types.
   c. The System opens new fiscal year accounts with carried-forward balances.
   d. Revenue accounts are closed and new-year revenue accounts opened per STA-FR-041.
7. The Senior Tax Officer reviews the period-end report and provides sign-off confirmation.
8. The System marks the period as "closed" — no further postings are permitted to the closed period without a formal reopening request.

**Alternative Flows:**

- **AF-1: Partial Period Close.** If some accounts cannot be closed due to pending transactions, the System proceeds with clean accounts and defers the remainder. The deferred accounts are tracked and retried in the next batch window. A partial closure report is generated.
- **AF-2: Period Reopening.** A Senior Tax Officer requests reopening a closed period (e.g., to post a late-discovered adjustment). The System logs the reopening request, resets the period status to "open," allows the adjustment, and then re-executes the closing sequence. All reopenings are audited.
- **AF-3: Non-Periodic Tax Types.** For tax types that do not use tax periods (customs, duty on documents per STA-FR-007), the System performs monthly or quarterly balance snapshots using transaction dates as the period boundary. These snapshots serve as reconciliation checkpoints rather than formal period closings.

**Exception Flows:**

- **EF-1: Balance Variance Detected.** At step 3b, if the closing balance discrepancy exceeds €0.01, the System halts processing for that account, creates an exception case, and continues with the remaining accounts. The exception must be resolved before the account can be closed.
- **EF-2: Pending Transactions Block.** At step 2, if pending transactions exist for the closing period, the System cannot proceed. It generates a list of blocking transactions (payment in processing, unapproved adjustments) and notifies the responsible officers to finalise them before period close can proceed.
- **EF-3: Batch Window Exhaustion.** If the period-end processing cannot complete within the available batch window (e.g., fiscal year-end with hundreds of thousands of accounts), the System pauses, records its progress checkpoint, and resumes in the next available window. Progress is reported to the System Administrator.

**Postconditions:**

- All closing balances have been computed, verified, and recorded for the ended period.
- Opening balances for the new period have been created with correct carry-forward amounts.
- Revenue accounts have been updated with period-end totals.
- The period-end reconciliation report has been generated and reviewed.
- The closed period is locked against further postings (without formal reopening).
- Full audit trail of the roll-over process is recorded.

---

### UC-STA-10: Configure Interest and Penalty Rates

| Field | Value |
|-------|-------|
| **Actor(s)** | System Administrator (primary), Senior Tax Officer (approval for rate changes) |
| **Priority** | Must |
| **Frequency** | ~5–10/year (rate changes are infrequent but critical) |
| **Related Requirements** | STA-FR-025, STA-FR-029, STA-FR-031, STA-FR-024 |
| **Related Business Rules** | BR-STA-032 (non-overlapping validity periods), BR-STA-033 (rate change effective date), BR-STA-034 (grace period configuration) |

**Preconditions:**

- The System Administrator is authenticated with configuration management role.
- A legislative or policy decision has been made to change rates (documented externally).
- The current rate configuration is in a consistent state (no overlapping periods).

**Main Success Scenario:**

1. The System Administrator navigates to the Configuration Management interface and selects "Interest and Penalty Rates."
2. The system displays the current rate configuration: interest rates with validity periods, penalty rates per violation type with validity periods, fine rates per violation type with validity periods, instalment discount rates, refund interest rates, and grace period settings per penalty type.
3. The System Administrator selects the rate type to modify and chooses "Add New Rate" or "Modify Existing."
4. For a new rate, the System Administrator enters: rate value (percentage), validity start date, validity end date (or "indefinite"), applicable tax types (all or specific), and rate classification (standard, instalment, refund).
5. The system validates the entry: confirms no overlap with existing validity periods (BR-STA-032), validates that the start date is in the future or current date, validates rate value is within acceptable bounds (configurable min/max), and checks that no gap exists between the end of the previous rate period and the start of the new one.
6. The system presents a preview showing: the proposed rate alongside existing rates in a timeline view, affected accounts (estimated count of taxpayers with overdue liabilities that would be subject to the new rate), and estimated daily interest impact of the rate change.
7. The System Administrator confirms the configuration change.
8. If the rate change requires Senior Tax Officer approval (configurable policy), the system routes the change for approval. The Senior Tax Officer reviews and approves or rejects.
9. Upon approval (or direct confirmation if no approval required), the system activates the new rate effective from the configured start date.
10. The system logs the configuration change with full audit trail: previous value, new value, effective date, configuring user, approver (if applicable), and timestamp.
11. The system generates a notification to relevant staff about the rate change and its effective date.

**Alternative Flows:**

- **AF-1: Modify Existing Rate.** At step 3, the System Administrator modifies an existing rate (e.g., changing the end date). The system validates that the modification does not create overlapping periods or gaps. Historical rates (already used in calculations) cannot be modified; only future rates can be changed.
- **AF-2: Grace Period Configuration.** The System Administrator configures or modifies grace periods for penalty types (per STA-FR-031). For each penalty type, the grace period (in days) is set. Overdue penalty payments beyond the grace period trigger escalation.
- **AF-3: Violation Type Classification.** The System Administrator configures whether a specific violation triggers a fine or a penalty (per STA-FR-029), with the appropriate rate and calculation method for each.

**Exception Flows:**

- **EF-1: Overlapping Period Detected.** At step 5, the system detects that the proposed rate period overlaps with an existing rate. The system rejects the entry and highlights the conflict, prompting the System Administrator to adjust dates.
- **EF-2: Retroactive Rate Change Attempt.** At step 5, the System Administrator attempts to set a start date in the past. The system warns that retroactive rate changes would require recalculation of all interest posted since the proposed start date and requires explicit Senior Tax Officer authorisation to proceed.

**Postconditions:**

- The new rate configuration is active and will be applied by the interest/penalty calculation engine (UC-STA-03) from the effective date.
- The configuration change is fully audited.
- Relevant staff have been notified of the rate change.
- No overlapping validity periods exist in the rate configuration.

---

### UC-STA-11: Process Payment Cancellation and Correction

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary initiator), Senior Tax Officer (approver) |
| **Priority** | Must |
| **Frequency** | ~10–20/day |
| **Related Requirements** | STA-FR-022, STA-FR-013, STA-FR-012 |
| **Related Business Rules** | BR-STA-035 (cancellation mandatory reason), BR-STA-036 (approval requirement for all cancellations), BR-STA-017 (reversal linking) |

**Preconditions:**

- A payment has been posted to a taxpayer account and an error has been identified (wrong TIN, wrong amount, wrong tax type, duplicate posting, etc.).
- The Tax Officer has verified the error and identified the correct posting details.
- The original payment posting is visible in the taxpayer's transaction history.

**Main Success Scenario:**

1. The Tax Officer navigates to the incorrectly posted payment in the taxpayer's transaction history (via UC-STA-07).
2. The Tax Officer selects "Cancel/Reverse Payment" and is presented with the cancellation form.
3. The Tax Officer enters: reason code (from configurable list: wrong TIN, wrong amount, wrong tax type, duplicate, bank reversal, other), free-text explanation, and if correction is needed, the correct posting details (correct TIN, tax type, period, amount).
4. The system validates the cancellation request: verifies the original payment has not already been reversed, checks that the reason code is populated, and confirms the Tax Officer has cancellation initiation rights.
5. The system routes the cancellation request to the Senior Tax Officer for approval (all cancellations require approval per BR-STA-036).
6. The Senior Tax Officer reviews the cancellation request including: original payment details, reason for cancellation, proposed correction (if any), and impact analysis (which allocations will be reversed).
7. The Senior Tax Officer approves the cancellation.
8. The system reverses the original payment by generating equal and opposite entries for every allocation that was created by the original payment:
   a. For each credit entry posted to the taxpayer's account, a corresponding debit reversal entry is created.
   b. For each debit entry posted to the revenue account, a corresponding credit reversal entry is created.
   c. All reversal entries are linked to the original transaction (BR-STA-017).
9. The system recalculates all affected period balances to reflect the reversal.
10. If a correction is specified (step 3), the system immediately processes the corrected payment as a new payment (invoking UC-STA-04 with the correct details).
11. The system logs the complete cancellation and correction chain: original posting, cancellation request, approval, reversal entries, and corrected posting (if applicable).
12. The system generates notifications to: the initiating Tax Officer (confirmation), and the taxpayer (if the correction affects their visible account balance).

**Alternative Flows:**

- **AF-1: Partial Amount Correction.** The original payment amount was correct but was split incorrectly across tax types. The Tax Officer specifies a partial reversal (reversing only the mis-allocated portion) and provides the correct allocation. The system reverses only the affected allocation entries and reprocesses the freed amount per the corrected instructions.
- **AF-2: Bank-Initiated Reversal.** The payment gateway notifies the system of a bank reversal (bounced payment, chargeback). The system automatically creates a cancellation request with reason "bank reversal," routes for expedited approval, and upon approval, reverses all allocations.

**Exception Flows:**

- **EF-1: Approval Rejected.** At step 7, the Senior Tax Officer rejects the cancellation. The system notifies the Tax Officer with the rejection reason. The original payment remains posted. The Tax Officer may resubmit with additional justification or escalate.
- **EF-2: Cascading Impact.** At step 8, the reversal affects downstream calculations (e.g., interest was calculated on the incorrectly reduced balance). The system identifies all dependent transactions and flags them for recalculation, creating a recalculation case for review.

**Postconditions:**

- The erroneous payment has been fully reversed with all allocation entries unwound.
- If a correction was specified, the corrected payment has been processed and allocated.
- Double-entry integrity is maintained through all reversal and correction entries.
- The complete audit trail links original posting → cancellation → reversal → correction.
- Affected account balances are recalculated and consistent.

---

### UC-STA-12: Manage Suspense Account

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary), System (automated aging alerts) |
| **Priority** | Must |
| **Frequency** | ~20–40/day (resolution actions), daily batch (aging report) |
| **Related Requirements** | STA-FR-021, STA-FR-008 |
| **Related Business Rules** | BR-STA-015 (suspense posting criteria), BR-STA-037 (suspense aging thresholds), BR-STA-038 (suspense resolution workflow) |

**Preconditions:**

- One or more payments exist in the tax revenue suspense account (posted per UC-STA-04, AF-1).
- The Tax Officer is authorised for suspense account management.

**Main Success Scenario:**

1. The Tax Officer navigates to the Suspense Account Management interface.
2. The system displays the suspense account register: list of all unresolved suspense entries, sorted by age (oldest first), showing payment date, amount, payment reference, partial payer information (if any), and days in suspense.
3. The Tax Officer selects an entry for resolution.
4. The Tax Officer investigates the payer identity using available information: payment reference, bank details, partial name, amount pattern matching against known liabilities.
5. The Tax Officer identifies the correct taxpayer and enters their TIN.
6. The system validates the TIN against the taxpayer register and displays the matched taxpayer's account summary for confirmation.
7. The Tax Officer confirms the identity resolution and specifies allocation instructions (tax type, period, or "apply per standard rules").
8. The system removes the payment from the suspense account, posts it to the identified taxpayer's account (invoking UC-STA-04 payment allocation logic), and records the resolution with full audit trail (original suspense entry, resolving officer, resolution date, matched TIN).
9. The system updates the suspense account balance.

**Alternative Flows:**

- **AF-1: Daily Aging Report.** The System generates a daily suspense account aging report showing: entries by age bracket (0–7 days, 8–30 days, 31–90 days, >90 days), total amounts per bracket, and entries exceeding the configured aging threshold (BR-STA-037) flagged for escalation.
- **AF-2: Refund of Unresolvable Payment.** If the Tax Officer cannot identify the payer after exhausting investigation options (and the entry has exceeded the maximum suspense period), the Tax Officer initiates a return/refund process using the payment source details. Senior Tax Officer approval is required for suspense refunds.
- **AF-3: Partial Resolution.** A suspense entry represents a bulk payment for multiple taxpayers. The Tax Officer splits the entry and resolves each portion to the appropriate taxpayer account.

**Exception Flows:**

- **EF-1: Invalid TIN at Resolution.** At step 6, the entered TIN does not match any registered taxpayer. The system rejects the resolution and prompts the Tax Officer to re-verify the identity.
- **EF-2: Payment Already Claimed.** At step 5, the payment amount and reference match a payment that has already been posted to a different taxpayer. The system warns of a potential duplicate and requires the Tax Officer to confirm before proceeding.

**Postconditions:**

- The suspense entry has been resolved and the payment allocated to the correct taxpayer account.
- The suspense account balance has been reduced accordingly.
- Full audit trail of the resolution is recorded.
- The daily aging report reflects the updated suspense position.

---

### UC-STA-13: Generate Tax Clearance Certificate

| Field | Value |
|-------|-------|
| **Actor(s)** | Tax Officer (primary), Taxpayer (via portal request), System (automated batch for regulatory submissions) |
| **Priority** | Must |
| **Frequency** | ~50–100/day (on-demand), ~5,000/quarter (batch for government contract checks) |
| **Related Requirements** | STA-FR-038, STA-FR-039, STA-FR-037, STA-FR-011 |
| **Related Business Rules** | BR-STA-004 (positive clearance: no overdue liabilities after set-off and instalment compliance), BR-STA-005 (negative clearance content requirements), BR-STA-039 (certificate validity period), BR-STA-040 (instalment conditional clearance) |

**Preconditions:**

- The taxpayer exists in the system with a current account position.
- Certificate templates are configured for the appropriate taxpayer type (NP/LP) and languages (English, Maltese).
- The taxpayer's account data is synchronised and reconciled.

**Main Success Scenario:**

1. The Tax Officer receives a clearance certificate request (from counter, phone, or internal process) and navigates to the taxpayer account.
2. The Tax Officer selects "Generate Tax Clearance Certificate."
3. The system retrieves the taxpayer's complete consolidated position across all tax types, including: total overdue debit balances (decomposed by PA, IA, PCA), credit balances available for set-off, active instalment agreements and their compliance status, and disputed amounts.
4. The system evaluates the clearance status applying BR-STA-004:
   - **Positive Clearance:** Consolidated overdue debit balance ≤ 0 (after applying available credits per set-off rules and considering compliant instalment agreements as "not overdue").
   - **Negative Clearance:** Consolidated overdue debit balance > 0 after all set-offs and instalment considerations.
5. **For Positive Clearance:**
   a. The system generates a positive TCC showing: taxpayer details (TIN, name, type), certification statement confirming no overdue tax liabilities, issue date and validity period (per BR-STA-039, default 30 days), certificate reference number, and issuing officer details.
6. **For Negative Clearance:**
   a. The system generates a negative TCC listing each overdue item: tax type, tax period, charge component (PA, IA, PCA), amount, and overdue-since date.
   b. The certificate includes operational provisos (e.g., subject to any pending assessments) and legal instructions (e.g., rights of appeal, payment instructions).
7. The system selects the appropriate template variant based on taxpayer type (NP/LP) and requested language.
8. The Tax Officer reviews the certificate and delivers it (print, email, or portal).
9. The system records the certificate: type (positive/negative), reference number, validity period, generating officer, delivery method, and a snapshot of the account position at generation time.

**Alternative Flows:**

- **AF-1: Taxpayer Portal Self-Service.** The Taxpayer requests a TCC via the self-service portal. The system generates the certificate automatically and delivers it electronically. For positive clearance, delivery is immediate. For negative clearance, the system also displays a summary of outstanding liabilities with payment links.
- **AF-2: Conditional Clearance (Instalment).** Per BR-STA-040, the taxpayer has overdue liabilities but is fully compliant with an active instalment agreement. The system generates a conditional positive clearance noting the active instalment arrangement, the remaining balance, and the next payment date.
- **AF-3: Batch Certificate Generation.** A government agency requests clearance checks for a batch of taxpayers (e.g., all bidders for a public contract). The Senior Tax Officer uploads the TIN list, the system generates certificates for each, and delivers a consolidated report.
- **AF-4: Certificate Verification.** An external party presents a certificate reference number for verification. The Tax Officer enters the reference number, and the system confirms or denies the certificate's validity (checking expiry date, matching details, and confirming no subsequent changes to the account that would invalidate the clearance).

**Exception Flows:**

- **EF-1: Account Under Reconciliation.** At step 3, the taxpayer's account has an unresolved reconciliation exception. The system warns that the certificate may not reflect the final position and offers to generate with a caveat or to defer until reconciliation is complete.
- **EF-2: Disputed Amount Ambiguity.** At step 4, the taxpayer has disputed liabilities. The system generates the clearance based on undisputed amounts only, but includes a note about pending disputes and their potential impact on the clearance status.

**Postconditions:**

- A Tax Clearance Certificate has been generated (positive, negative, or conditional) with a unique reference number.
- The certificate record is stored for future verification, including the account position snapshot.
- The generation event is recorded in the audit trail.
- For positive/conditional clearance: the validity period is tracked and the certificate can be verified against its reference number.

---
## 7.1.1 Use Case Summary and Coverage Matrix

| Use Case | Requirements Covered | Priority | Primary Actor |
|----------|---------------------|----------|---------------|
| UC-STA-01 | STA-FR-001–008, 010, 011 | Must | Tax Officer |
| UC-STA-02 | STA-FR-037–040, 010, 011 | Must | Tax Officer |
| UC-STA-03 | STA-FR-023–026, 010, 014 | Must | System |
| UC-STA-04 | STA-FR-017–022, 012, 013 | Must | System |
| UC-STA-05 | STA-FR-013, 022, 030, 012, 014 | Must | Tax Officer |
| UC-STA-06 | STA-FR-034–036, 012, 009 | Must | System |
| UC-STA-07 | STA-FR-003–005, 008, 013, 014 | Must | Tax Officer |
| UC-STA-08 | STA-FR-032, 033, 016, 020, 012 | Should | System |
| UC-STA-09 | STA-FR-009, 015, 041, 012 | Must | System |
| UC-STA-10 | STA-FR-025, 029, 031, 024 | Must | System Admin |
| UC-STA-11 | STA-FR-022, 013, 012 | Must | Tax Officer |
| UC-STA-12 | STA-FR-021, 008 | Must | Tax Officer |
| UC-STA-13 | STA-FR-038, 039, 037, 011 | Must | Tax Officer |

### Requirements Not Directly Covered by Use Cases (addressed by system architecture)

| Requirement | Coverage |
|-------------|----------|
| STA-FR-042 (Chart of Accounts) | Configuration activity; addressed in system administration procedures and UC-STA-10 (configuration pattern) |
| STA-FR-043 (Revenue disbursement principles) | Backend processing rule applied automatically during payment allocation; no direct user interaction |
| STA-FR-044 (Data migration) | One-time migration activity; addressed in Section 11 (Implementation Considerations) |
| STA-FR-045 (Multi-currency) | Cross-cutting capability applied within UC-STA-04 and UC-STA-01; deferred to ITCAS for full implementation |
| STA-FR-027, 028 (Penalty/fine auto-calculation) | Triggered within UC-STA-03 pattern (system-initiated batch); penalty posting follows same double-entry mechanism |
| STA-FR-046 (Suspended payments) | Handled within the UC-STA-12 suspense-management pattern (suspense kind = suspended); release follows UC-STA-04 allocation |
| STA-FR-047 (Account provisioning) | System-initiated background process driven by the ORS registration feed; exceptions follow the UC-STA-12 resolution pattern |
| STA-FR-048 (Account closure) | Accounting-operations case following the UC-STA-05 approval pattern; DM hand-off per DM-FR-044 |
| STA-FR-049 (Recipient access) | Read-only extract/view; no STA-side workflow (Could; deferred to ITCAS) |
| INT-FR-019 (Consumption watermark) | System architecture mechanism verified through the UC-STA-06 reconciliation pattern |
| RPT-FR-022 (Refund liability forecast) | Scheduled report following the UC-RPT-04 generation pattern |

---

## 7.2 Reporting Use Case — Revenue Reconciliation

### UC-RPT-04: Generate Revenue Reconciliation Report

| Field | Value |
|-------|-------|
| **Actor(s)** | SDO (primary), MGR |
| **Priority** | Should |
| **Frequency** | Daily (automated) + on-demand |
| **Related Requirements** | RPT-FR-010, RPT-FR-004, RPT-FR-017, RPT-FR-018 |
| **Related Business Rules** | BR-RPT-007 (reconciliation variance threshold), BR-RPT-008 (Treasury comparison rules) |

**Preconditions:**

- Revenue data is available from ORS/ClickHouse (STA aggregations).
- Treasury disbursement comparison data is available (imported or manually entered).
- Variance threshold is configured (default €1.00 per BR-RPT-007).

**Main Success Scenario:**

1. The system generates the daily Revenue Reconciliation Report (scheduled per RPT-FR-017).
2. The report compares total revenue collected (PA, IA, PCA separately) by tax type against Treasury records.
3. Variances exceeding the configured threshold are highlighted and flagged for investigation.
4. The report is distributed to the configured recipient list via email.
5. The SDO reviews flagged variances and initiates investigation where required.

**Alternative Flows:**

- **AF-1: On-Demand Generation.** The SDO generates the report for a custom date range.
- **AF-2: Manual Treasury Data Entry.** If automated Treasury data import is not available, the SDO enters Treasury figures manually before generating the comparison.

**Exception Flows:**

- **EF-1: Treasury Data Missing.** If Treasury comparison data is not available, the report generates the MTCA-side revenue totals only and flags that reconciliation is incomplete.

**Postconditions:**

- Revenue reconciliation report generated with variance analysis.
- Flagged variances assigned for investigation.

---

## 7.3 Administration Use Cases


This section presents use case specifications for system configuration and administration activities that support the STA solution.

---

### UC-ADM-01: Configure Business Rules

| Field | Value |
|-------|-------|
| **Actor(s)** | System Administrator (primary), Senior Tax Officer (approval for critical rule changes) |
| **Priority** | Must |
| **Frequency** | ~10–20/year (rule changes are infrequent but critical) |
| **Related Requirements** | NFR-025, NFR-029, INT-FR-011 |
| **Related Business Rules** | BR-ADM-001 (rule change audit requirements), BR-ADM-002 (rule activation policy) |

**Preconditions:**

- The SA is authenticated with business rule configuration permissions.
- A policy or legislative change has been documented requiring rule modification.
- The rule management interface is operational.

**Main Success Scenario:**

1. The SA navigates to the Business Rules Configuration module.
2. The system displays the current rule catalogue organised by domain: STA rules (payment allocation, interest, reconciliation), DM rules (debt thresholds, escalation, enforcement eligibility), reporting rules (KPI calculations, thresholds), and workflow rules (SLA, assignment, triggers).
3. The SA selects a rule to modify or creates a new rule.
4. For each rule, the SA configures: rule name and description, rule logic (decision table, conditional expression, or formula), effective date and end date, applicable scope (tax type, taxpayer type, debt category), and configurable parameter values.
5. The system validates the rule: no conflicts with existing rules in the same scope and effective period, all referenced parameters exist, and decision table completeness (no gaps or overlaps).
6. The SA previews the rule impact: estimated number of affected cases/accounts and before/after comparison for sample cases.
7. If the rule change is classified as critical (e.g., debt category thresholds, payment allocation sequence), the system routes for STO approval.
8. Upon confirmation or approval, the system activates the rule per BR-ADM-002 (immediately or at next business day start, configurable).
9. The system records the rule change with full audit trail per BR-ADM-001: previous logic/values, new logic/values, effective date, configuring user, approver, and justification.
10. The rule is exportable in structured format (DMN, JSON, or spreadsheet) per NFR-029.

**Alternative Flows:**

- **AF-1: Rule Deactivation.** The SA deactivates a rule that is no longer applicable. Active cases that were processed under the old rule retain their outcomes; only new processing uses the updated rules.
- **AF-2: Rule Version Comparison.** The SA compares two versions of a rule side-by-side to review changes over time.

**Exception Flows:**

- **EF-1: Conflicting Rules.** At step 5, if the new rule conflicts with an existing active rule, the system displays the conflict and requires resolution before save.

**Postconditions:**

- Business rule configured, validated, and activated.
- Full audit trail recorded.
- Rule exportable in standard format.

---

### UC-ADM-02: Configure Notification Templates

| Field | Value |
|-------|-------|
| **Actor(s)** | System Administrator (primary) |
| **Priority** | Must |
| **Frequency** | Initial setup + periodic updates (~5–10/year) |
| **Related Requirements** | WF-FR-014, WF-FR-013, WF-FR-017, NFR-017, NFR-026 |
| **Related Business Rules** | BR-ADM-003 (template versioning rules), BR-ADM-004 (merge field validation) |

**Preconditions:**

- The SA is authenticated with template management permissions.
- The template designer is operational.
- Available merge fields are documented and accessible.

**Main Success Scenario:**

1. The SA navigates to the Notification Template Management module.
2. The system displays existing templates organised by type: payment reminders (pre-due-date), demand notices (1st, 2nd, final), instalment payment reminders, instalment default notices, enforcement escalation notices, tax account statements, and internal staff alerts.
3. The SA selects a template to edit or creates a new one.
4. Using the visual template designer per NFR-026, the SA configures: static text content, merge fields (TIN, name, amounts, dates, tax type, period — validated against available data per BR-ADM-004), MTCA branding (logo, letterhead, footer), conditional sections (e.g., instalment information only shown if an instalment exists), and language variant (English and Maltese per NFR-017).
5. The SA previews the template with sample data to verify rendering.
6. The system validates: all merge fields reference valid data sources, both language variants are complete, and required legal text sections are present.
7. The SA saves the template with version control per BR-ADM-003. The previous version is preserved. An effective date is set for the new version.
8. The system records the template change with audit trail.

**Alternative Flows:**

- **AF-1: Batch Template Update.** When legislative changes affect standard legal text across multiple templates, the SA updates the shared text component once and all referencing templates are updated.
- **AF-2: Template Testing.** The SA generates a test notification using the template with test data to verify end-to-end delivery (email rendering, PDF formatting, postal print layout).

**Exception Flows:**

- **EF-1: Invalid Merge Field.** At step 6, if a merge field references a non-existent data element, the system flags the error and prevents save.
- **EF-2: Missing Language Variant.** At step 6, if the Maltese variant is not provided, the system warns and allows save with "incomplete — English only" status.

**Postconditions:**

- Notification template configured, versioned, and active.
- Both language variants available (or flagged as incomplete).
- Audit trail recorded.

---

### UC-ADM-03: Manage User Roles and Permissions

| Field | Value |
|-------|-------|
| **Actor(s)** | System Administrator (primary) |
| **Priority** | Must |
| **Frequency** | ~5–15/month (user onboarding, role changes, offboarding) |
| **Related Requirements** | NFR-011, NFR-014, NFR-015, INT-FR-016 |
| **Related Business Rules** | BR-ADM-005 (role hierarchy rules), BR-ADM-006 (permission inheritance), BR-ADM-007 (separation of duties) |

**Preconditions:**

- The SA is authenticated with user administration permissions.
- MITA SSO directory service is accessible per INT-FR-016.
- Role definitions exist (Case Officer, Senior Officer, Supervisor, Manager, Administrator, Auditor, System Administrator per NFR-011).

**Main Success Scenario:**

1. The SA navigates to the User and Role Management module.
2. For role management: the SA views existing roles with their permissions matrix (view, create, edit, approve, delete per entity type). The SA can create new roles, modify permissions on existing roles (subject to separation of duties per BR-ADM-007), and configure role hierarchy per BR-ADM-005 (e.g., Supervisor inherits Case Officer permissions per BR-ADM-006).
3. For user management: the SA searches for a user from the MITA SSO directory, assigns one or more roles, configures additional user-specific settings (assigned office, specialisation, workload capacity), and optionally configures data masking level per NFR-015.
4. The system validates: no conflicting role assignments violating separation of duties, user exists in the SSO directory, and all mandatory user fields are populated.
5. Permission changes take effect immediately per NFR-011.
6. The system records the role/user change with audit trail: previous roles, new roles, change date, and administering SA.

**Alternative Flows:**

- **AF-1: User Offboarding.** The SA deactivates a user account: all active case assignments are flagged for reassignment, the user's access is revoked immediately, but their audit history is preserved indefinitely.
- **AF-2: Temporary Role Elevation.** The SA grants temporary elevated permissions (e.g., acting supervisor during leave) with an automatic expiry date. The system reverts permissions automatically at expiry.

**Exception Flows:**

- **EF-1: SSO Directory Unavailable.** If MITA SSO is unreachable, the system allows local account management with a warning that SSO synchronisation will occur when connectivity is restored.
- **EF-2: Separation of Duties Violation.** At step 4, if the role combination violates separation of duties (e.g., both case creation and case approval), the system prevents the assignment and displays the policy violation.

**Postconditions:**

- User roles and permissions configured and effective.
- Audit trail recorded.
- SSO synchronisation confirmed (or queued).

---

---

# 8. BUSINESS RULES CATALOGUE

## 8.1 STA Accounting Rules (BR-STA-001 to BR-STA-044)

### 8.1.1 Account Structure and Balance Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-001 | TTT Hierarchy | The STA shall maintain a strict three-level hierarchy: Taxpayer → Tax Type → Tax Period. All queries, displays, and calculations shall respect this hierarchy for drill-down and aggregation. | `L1_balance(TIN) = Σ L2_balance(TIN, TXT)` for all TXT; `L2_balance(TIN, TXT) = Σ L3_balance(TIN, TXT, TXP)` for all TXP | PDF §3.1, §3.3.1; ITCAS RFP §3.5.5.4.11 | N | STA-FR-001–007; UC-STA-01, UC-STA-07 |
| BR-STA-002 | Balance Computation | Consolidated balance at each level is the algebraic sum of all debit and credit transactions. Balance = Opening Balance + Σ(Debits) − Σ(Credits). Each balance is decomposed into PA (Principal Amount), IA (Interest Amount), and PCA (Penalties/Charges Amount). | `balance(PA) = opening_PA + Σ(debit_PA) − Σ(credit_PA)`; same for IA, PCA; `total_balance = balance(PA) + balance(IA) + balance(PCA)` | PDF §3.3.1 | N | STA-FR-002, STA-FR-009; UC-STA-01 |
| BR-STA-003 | Disputed Amount Exclusion | Disputed amounts shall be tracked separately from undisputed amounts at each level. Disputed amounts are excluded from enforceable balance calculations and interest calculations. | `enforceable_balance = total_balance − disputed_amount`; `interest_base = total_PA − disputed_PA` | PDF §2.5; Vendor §6.6 | N | STA-FR-011; UC-STA-01, UC-STA-03 |
| BR-STA-004 | Positive Tax Clearance Criteria | A positive Tax Clearance Certificate is issued when: (a) the consolidated overdue debit balance ≤ 0 after applying available credits via set-off, OR (b) all overdue liabilities are covered by a compliant active instalment agreement. | `IF (overdue_debit − available_credit ≤ 0) OR (all overdue liabilities under compliant instalment) THEN positive_clearance ELSE negative_clearance` | PDF §BR-TAS-01; Vendor §6.6 | P (instalment compliance flag configurable) | STA-FR-038, STA-FR-039; UC-STA-02, UC-STA-13 |
| BR-STA-005 | Negative Tax Clearance Content | A negative TCC shall list each overdue item by: tax type, tax period, charge component (PA/IA/PCA), amount, and overdue-since date. Shall include operational provisos and legal instructions. | Template-driven content with mandatory sections: overdue items table, legal advice, appeal rights, payment instructions | PDF §BR-TAS-02 | Y (template content configurable) | STA-FR-039; UC-STA-02, UC-STA-13 |
| BR-STA-006 | Statement Template Selection | Statement templates are selected based on: taxpayer type (NP/LP), statement type (TAS/TCC), and requested language (English/Maltese). Fallback to English if requested language variant is unavailable. | `template = lookup(taxpayer_type, statement_type, language)`; `IF template IS NULL THEN template = lookup(taxpayer_type, statement_type, 'EN')` | PDF §BR-TAS-03 | Y (template mapping configurable) | STA-FR-037, STA-FR-040; UC-STA-02 |

### 8.1.2 Interest and Penalty Calculation Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-007 | Daily Interest Rate Application | Interest is calculated daily on overdue principal amounts. The rate valid for each day is applied individually, supporting intra-period rate changes. | `daily_interest = outstanding_PA × (annual_rate / 365)`; calculated for each day from (due_date + 1) to current_date | PDF §4.2; Vendor §6.6.1 | P (rates configurable; formula fixed) | STA-FR-023, STA-FR-024; UC-STA-03 |
| BR-STA-008 | Rate Change Within Period | When the interest rate changes during a calculation period, interest is segmented: days at the old rate calculated separately from days at the new rate. | `total_interest = Σ(days_at_rate_n × PA × rate_n / 365)` for each rate period within the calculation window | Vendor §6.6.1 | P (rates configurable) | STA-FR-024; UC-STA-03 |
| BR-STA-009 | Instalment Lower Interest Rate | When a taxpayer has an active, compliant instalment agreement, a lower interest rate is applied to the outstanding balance. Upon non-compliance, the rate reverts to the standard rate from the date of non-compliance. | `IF instalment_active AND instalment_compliant THEN rate = instalment_rate ELSE rate = standard_rate`; reversion date = first missed payment date | PDF §BR-INST-04; Vendor §6.6.1 | Y (instalment rate configurable) | STA-FR-025; UC-STA-03 |
| BR-STA-010 | Refund Interest Calculation | When a refund is processed beyond the standard processing period, interest is owed to the taxpayer. Calculated using the configurable refund interest rate. | `refund_interest = refund_amount × (refund_rate / 365) × days_overdue`; where `days_overdue = processing_date − (due_date + standard_period)` | Vendor §6.6.1 | Y (refund rate and standard period configurable) | STA-FR-026; UC-STA-03 (AF-3) |

### 8.1.3 Payment Allocation Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-011 | Charge-Type Priority Sequence | When allocating a payment to a liability, the system applies payment in the following priority: (1) Interest Amount (IA), (2) Additional tax, (3) Principal Amount (PA), (4) Fees/Surcharges. This sequence is configurable. | `allocation_order = [IA, additional_tax, PA, fees]`; for each charge type in order: `allocated = min(payment_remaining, charge_balance)`; `payment_remaining -= allocated` | PDF §3.2 (Annex 1); Vendor §6.6 | Y (priority sequence configurable) | STA-FR-017, STA-FR-018; UC-STA-04 |
| BR-STA-012 | Oldest-Period-First Allocation | When a payment is applied across multiple tax periods (within a single tax type), the oldest overdue period is satisfied first, progressing chronologically. | `periods = sort(overdue_periods, by=due_date, ascending)`; allocate to each period in order until payment exhausted | PDF §BR-PAY-01, §BR-PAY-02 | Y (allocation direction configurable) | STA-FR-018; UC-STA-04 |
| BR-STA-013 | Tax-Type Priority Sequence | When a consolidated payment (no tax type specified) is received, the system allocates across tax types in a configured priority sequence. Within each tax type, BR-STA-012 and BR-STA-011 apply. | `tax_types = sort(overdue_tax_types, by=configured_priority)`; for each tax type: apply BR-STA-012, then BR-STA-011 | PDF §BR-PAY-03 | Y (tax type priority sequence configurable) | STA-FR-019; UC-STA-04 |
| BR-STA-014 | Overpayment Handling | When a payment exceeds all outstanding liabilities, the excess is posted to the taxpayer's general credit account. The credit is available for future liability set-off or refund processing. | `IF payment > total_liabilities THEN credit_balance += (payment − total_liabilities)`; credit posted to general credit account | PDF §BR-PAY-04 | N | STA-FR-020; UC-STA-04 |
| BR-STA-015 | Suspense Account Posting | Payments where the payer cannot be identified are posted to the tax revenue suspense account. A resolution case is created and assigned for identity resolution. | `IF TIN_unresolvable THEN post_to_suspense(amount, payment_ref)`; create identity resolution case | PDF §BR-PAY-05 | N | STA-FR-021; UC-STA-04 (AF-1), UC-STA-12 |

### 8.1.4 Adjustment and Reversal Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-016 | Adjustment Approval Thresholds | Credit/debit adjustments above a configured amount threshold require Senior Tax Officer approval. Below the threshold, the initiating Tax Officer can process directly. | `IF adjustment_amount ≥ approval_threshold THEN route_to_STO_for_approval ELSE process_directly`; threshold configurable per adjustment type | Vendor §6.6 | Y (thresholds configurable per type) | STA-FR-030; UC-STA-05 |
| BR-STA-017 | Reversal Transaction Linking | Every reversal transaction must be linked to the original transaction it reverses. The original transaction is marked as "reversed." Reversal of an already-reversed transaction is prohibited. | `reversal.original_ref = original.transaction_id`; `original.status = 'reversed'`; `IF original.status == 'reversed' THEN reject_reversal` | Vendor §6.6 | N | STA-FR-022; UC-STA-05, UC-STA-11 |
| BR-STA-018 | Double-Entry Adjustment Posting | Every adjustment posts a debit/credit pair: one entry to the taxpayer account and the corresponding contra-entry to the relevant revenue account. | `post(taxpayer_account, adjustment_amount, direction)`; `post(revenue_account, adjustment_amount, opposite_direction)` | PDF §3.3.1 (BR-ACC-04) | N | STA-FR-012; UC-STA-05 |

### 8.1.5 Reconciliation Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-019 | Reconciliation Tolerance | Reconciliation checks use a tolerance of €0.01. Discrepancies within tolerance are auto-resolved; discrepancies exceeding tolerance generate exception cases. | `IF abs(computed_balance − stored_balance) ≤ 0.01 THEN pass ELSE flag_exception` | PDF §BR-ACC-01; NFR-020 | Y (tolerance configurable) | STA-FR-034; UC-STA-06 |
| BR-STA-020 | Daily Reconciliation Window | Automated reconciliation runs daily within a configured batch window. All transactions posted since the last reconciliation are included. | Batch scheduled at configurable time (default 03:00); scope = all transactions since last successful reconciliation | Vendor §6.6 | Y (schedule configurable) | STA-FR-034; UC-STA-06 |
| BR-STA-021 | Treasury Reconciliation Frequency | Revenue reconciliation with Treasury occurs at the configured frequency (target: daily; minimum: weekly). | `reconciliation_frequency = configurable (daily | weekly)`; comparison generated at scheduled time | PDF §BR-REV-01 | Y (frequency configurable) | STA-FR-035; UC-STA-06 (AF-1) |

### 8.1.6 Transaction Classification Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-022 | Transaction Type Classification | Every financial transaction is classified as one of: liability, payment, refund, transfer (from/to), write-off, or reversal. Classification determines posting rules and reporting categorisation. | `transaction.type ∈ {liability, payment, refund, transfer_from, transfer_to, write_off, reversal}` | PDF §BR-ACC-06 | P (types fixed; display labels configurable) | STA-FR-013; UC-STA-07 |
| BR-STA-023 | Charge Type Classification | Every transaction amount is classified by charge type: PA (principal tax), IA (interest), PCA (penalties/charges/surcharges), or fees. Multiple charge types may apply to a single business event. | `transaction.charge_type ∈ {PA, IA, PCA, fees}`; allocation per BR-STA-011 | PDF §BR-ACC-07 | N | STA-FR-014; UC-STA-07 |

### 8.1.7 Set-Off and Credit Application Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-024 | Cross-Tax-Type Set-Off Priority | When applying credit from one tax type against liability in another, the system follows a configured priority sequence for target tax types. Within each target, BR-STA-011 and BR-STA-012 apply. | `target_tax_types = sort(overdue_tax_types, by=set_off_priority)`; apply credit sequentially per priority | PDF §BR-PAY-06 (Annex 1 §5) | Y (priority sequence configurable) | STA-FR-032; UC-STA-08 |
| BR-STA-025 | Refund Interception Rule | Before disbursing any approved refund, the system checks for outstanding debts across all tax types. If debts exist, the refund is intercepted (up to the total debt amount) and applied per set-off priority. | `IF approved_refund AND outstanding_debt > 0 THEN intercept = min(refund, debt); apply per BR-STA-024; net_refund = refund − intercept` | PDF §BR-DM-07 (§3.5) | N (automatic; interception scope configurable) | STA-FR-033; UC-STA-08 (AF-1) |
| BR-STA-026 | Minimum Set-Off Threshold | Set-off is only executed when the credit amount exceeds a configurable minimum threshold. Below the threshold, the credit is retained for future accumulation. | `IF credit_amount ≥ min_setoff_threshold THEN execute_setoff ELSE retain_credit` | Derived from operational efficiency | Y (threshold configurable) | STA-FR-032; UC-STA-08 |
| BR-STA-027 | Set-Off Notification Requirement | The taxpayer must be notified of every set-off action within a configurable period (default: same business day). Notification includes: amounts transferred, source and target tax types, and resulting balances. | Notification generated automatically after set-off posting; channel per taxpayer preference | PDF §3.1 (Annex 1 §5) | Y (notification timing and template configurable) | STA-FR-032; UC-STA-08 |

### 8.1.8 Period-End and Roll-Over Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-028 | Balance Forward Method | All tax types use the balance forward accounting method: the closing balance of one period becomes the opening balance of the next period, regardless of whether individual transactions within the period are settled. | `opening_balance(TIN, TXT, TXP_new) = closing_balance(TIN, TXT, TXP_previous)` for PA, IA, PCA separately | PDF §3.3.1 (BR-ACC-02, BR-ACC-03) | N | STA-FR-009; UC-STA-09 |
| BR-STA-029 | Year-End Processing Sequence | Fiscal year-end processing follows a defined sequence: (1) complete all period-end processing for the final period, (2) close fiscal year accounts, (3) open new fiscal year accounts, (4) close and open revenue accounts. | Sequential execution; step N+1 begins only after step N completes successfully | Derived from accounting standards | N | STA-FR-041; UC-STA-09 |
| BR-STA-030 | Closing Balance Verification | Before closing a period, the system verifies: computed closing balance = stored running balance ± €0.01. Discrepancies block period close and generate exception cases. | `IF abs(computed_closing − stored_running) > 0.01 THEN block_close; create_exception ELSE proceed` | PDF §3.3.1; NFR-020 | P (tolerance configurable) | STA-FR-009; UC-STA-09 |
| BR-STA-031 | Fiscal Year Calendar | The fiscal calendar defines period-end dates per tax type: monthly (VAT), quarterly (provisional tax), and annually (CIT/PIT). Custom periods are configurable. | Calendar table: `{tax_type, period_type, period_start, period_end}`; non-overlapping periods enforced | Maltese tax legislation | Y (calendar configurable) | STA-FR-015; UC-STA-09 |

### 8.1.9 Rate Configuration Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-032 | Non-Overlapping Validity Periods | Interest, penalty, and fine rates must have non-overlapping validity periods. No two rates for the same rate type and scope may have overlapping date ranges. | `FOR each new_rate: SELECT count(*) FROM rates WHERE type = new_rate.type AND scope = new_rate.scope AND start_date < new_rate.end_date AND end_date > new_rate.start_date; IF count > 0 THEN reject` | Vendor §6.6.1 | N (validation rule; rate values configurable) | STA-FR-025; UC-STA-10 |
| BR-STA-033 | Rate Change Effective Date | New rates can only have future or current-date effective dates. Retroactive rate changes require explicit STO authorisation and trigger recalculation of all affected interest postings. | `IF new_rate.start_date < today THEN require_STO_approval AND trigger_recalculation ELSE activate_normally` | Derived from accounting integrity | P (retroactive flag configurable) | STA-FR-025; UC-STA-10 |
| BR-STA-034 | Grace Period Configuration | Each penalty type has a configurable grace period (in days). Penalties are not applied during the grace period following the due date. | `penalty_applies_from = due_date + grace_period_days + 1`; `IF current_date ≤ due_date + grace_period THEN no_penalty` | Vendor §6.6.1; PDF §4.1 | Y (grace period days configurable per penalty type) | STA-FR-031; UC-STA-10 |

### 8.1.10 Payment Cancellation and Suspense Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-035 | Cancellation Mandatory Reason | Every payment cancellation requires a reason code from a configurable list and a free-text justification. Cancellation without reason is rejected. | `IF cancellation.reason_code IS NULL OR cancellation.justification IS NULL THEN reject` | Vendor §6.6 | P (reason codes configurable; requirement fixed) | STA-FR-022; UC-STA-11 |
| BR-STA-036 | Approval Required for All Cancellations | All payment cancellations require Senior Tax Officer approval, regardless of amount. | `FOR ALL cancellations: route_to_STO_approval`; no amount-based exemption | Derived from financial control | N | STA-FR-022; UC-STA-11 |
| BR-STA-037 | Suspense Account Aging Thresholds | Suspense entries are aged and categorised: 0–7 days (new), 8–30 days (pending), 31–90 days (aged), >90 days (critical). Entries exceeding 90 days trigger escalation to supervisor. | `age_bracket = CASE WHEN days ≤ 7 THEN 'new' WHEN days ≤ 30 THEN 'pending' WHEN days ≤ 90 THEN 'aged' ELSE 'critical' END`; critical entries auto-escalated | Derived from operational standards | Y (thresholds configurable) | STA-FR-021; UC-STA-12 |
| BR-STA-038 | Suspense Resolution Workflow | Suspense entries are resolved by: (1) identity resolution and allocation to taxpayer account, (2) return/refund to payment source, or (3) write-off after maximum suspense period. Each resolution path requires documented justification. | `resolution_type ∈ {allocate_to_TIN, refund_to_source, write_off}`; STO approval required for refund and write-off paths | Derived from financial control | P (resolution options fixed; max period configurable) | STA-FR-021; UC-STA-12 |

### 8.1.11 Tax Clearance Supplementary Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-039 | Certificate Validity Period | Positive TCC certificates are valid for a configurable number of days from issue date (default: 30 days). After expiry, a new certificate must be requested. | `certificate.expiry_date = issue_date + validity_days`; `IF current_date > expiry_date THEN certificate_expired` | PDF §BR-TAS-01 | Y (validity days configurable) | STA-FR-038; UC-STA-13 |
| BR-STA-040 | Instalment Conditional Clearance | A taxpayer with overdue liabilities covered by a compliant instalment agreement may receive a conditional positive clearance. The certificate notes the instalment arrangement, remaining balance, and next payment date. | `IF overdue > 0 AND instalment_active AND instalment_compliant THEN clearance_type = 'conditional_positive'`; certificate template includes instalment section | Derived from BR-STA-004 | Y (conditional clearance enabled/disabled configurable) | STA-FR-038; UC-STA-13 (AF-2) |

### 8.1.12 Suspension, Account Lifecycle and Cross-Entity Rules

| Rule ID | Name | Description | Rule Logic | Source | Config. | Related Reqs / UCs |
|---------|------|-------------|------------|--------|---------|---------------------|
| BR-STA-041 | Suspended-Payment Exclusion and Release | A payment of an identified taxpayer placed in suspended state is posted to the taxpayer account and the suspended-payments revenue account; the covered amount is excluded from payment allocation and from the enforceable balance until release. Release routes the amount through standard allocation (BR-STA-011–014); return-to-payer requires STO approval and documented justification. | `IF payment.status = 'suspended' THEN exclude_from(allocation, enforceable_balance)`; `release → run allocation`; `return → require_STO_approval` | PDF Annex 1 (process list), Annex 1 §4.7 | P (suspension reasons configurable; exclusion fixed) | STA-FR-046 |
| BR-STA-042 | Objection-Allowed Gate | Disputed amounts (STA-FR-011) may only be recorded for tax types whose registration carries `objection_allowed = true`. Attempts to flag disputes on objection-disallowed tax types are rejected with reason. Where no return has been received for a self-assessed tax despite reminders, objection to the default assessment is disallowed per the legal framework (OQ-08). | `IF NOT tax_type.objection_allowed THEN reject_dispute_flag(reason)` | PDF §3.3.2 Table 2 (objection allowed); PDF §5.2.1 CAUTION | P (flags configurable per tax type; gate fixed) | STA-FR-011; OQ-08 |
| BR-STA-043 | Tax Year / Year of Assessment Derivation | Each tax period carries a tax-year attribute. For income taxes (CIT, PIT), the Year of Assessment equals the basis (calendar) year + 1; for other tax types, the tax year equals the calendar year of the period. The tax-year attribute anchors reporting delimiters (RPT-FR-015) and statement groupings; interest computation continues to anchor on statutory due dates. | `IF tax_type ∈ income_taxes THEN tax_year = basis_year + 1 ELSE tax_year = calendar_year(period)` | PDF §6.3.1.1 Table 6 (tax period vs tax year); Maltese YA convention | P (derivation per tax type configurable) | RPT-FR-015; STA-FR-037 |
| BR-STA-044 | Debtor's-Debtor Posting Pattern | Amounts collected from a third party owing money to the debtor (garnishee receipts, DM-FR-032) are posted as a payment on the paying party's side and a transfer-to credit on the debtor's account, both transactions audit-linked to the originating DM enforcement case reference. | `post(payer_account, PAYMENT)`; `post(debtor_account, TRANSFER_TO)`; both with `case_ref = DM_case_id` | PDF Annex 1 (process list: reconciliation between taxpayers — enforced collection from debtor's debtor) | N | STA-FR-013; DM-FR-032 |

---

---

# 9. DATA REQUIREMENTS

## 9.1 Conceptual Data Model

The STA solution operates on a conceptual data model comprising the STA core entities (and referencing downstream entities owned by the Debt Management component) organised around the TTT (Taxpayer / Tax Type / Tax Period) principle. The model is read-oriented — sourcing data from ORS/ClickHouse — with workflow state managed locally in the the selected low-code/no-code platform platform database. The entities below describe the logical model; physical implementation spans two persistence layers (ClickHouse for tax data, the selected platform MySQL/PostgreSQL for workflow state).

### 9.1.1 Core Entities

**Taxpayer** — The central entity representing any natural person (NP) or legal person (LP) registered with MTCA. Every taxpayer is uniquely identified by a TIN (Taxpayer Identification Number) — a discrete, non-informative, self-controlling number using a MOD11 algorithm. NPs performing business activities are treated as LPs. Key attributes include TIN, taxpayer type (TPT: NP/LP), name, postal address, phone, email, contact details, bank details, and for LPs: business name, structure/branches/shareholders, legal representatives, ISIC/NACE classification, and exporter status.

**TaxType** — Represents a category of tax obligation (CIT, PIT, VAT, customs/excise duties, stamp duty, rental income tax, capital gains tax, ECO contributions, etc.). Each TaxType has a code, name, assessment methodology, applicable rates, filing frequency, associated business rules for interest and penalty calculation, and the per-tax-type registration flags `amendments_allowed` and `objection_allowed` (PDF Table 2) governing whether revised liabilities are accepted and whether disputed amounts may arise (BR-STA-042). TaxTypes determine the L2 structure of the STA.

**TaxPeriod** — The period of liability for a specific tax type. For most taxes, this is a calendar period (monthly, quarterly, annually). For customs transactions, the period equals the transaction date. Each period carries a **tax-year attribute**: for income taxes the Year of Assessment (YA = basis year + 1, Maltese convention), otherwise the calendar year (BR-STA-043) — anchoring report delimiters and statement groupings. TaxPeriods govern balance forward accounting — each period has an opening balance (carried from the prior period's closing balance) and a closing balance (sum of opening balance plus all transactions within the period).

**TaxAccount** — The STA record implementing the three-level structure: L1 (consolidated balance across all tax types), L2 (per-tax-type balance), and L3 (per-period transaction detail). Each level maintains separate debit and credit amounts, with balances decomposed into PA (Principal Amount), IA (Interest Amount), and PCA (Penalties and Charges Amount). The TaxAccount enforces double-entry bookkeeping — every transaction posts both to the taxpayer account and to the corresponding revenue code account.

**Transaction** — A financial event recorded against a TaxAccount. Transaction types include: liability (assessment), payment, refund, transfer (from/to), write-off, and reversal of any preceding type. Each transaction records: date/time, type, amount (decomposed into PA/IA/PCA), source process, and the resulting balance impact. Transactions are immutable once posted; corrections are recorded as reversal + new transaction pairs.

**Payment** — A specific transaction type representing funds received. Payments may be fully identified (taxpayer + tax type + period), partially identified (taxpayer known but tax type/period unspecified), or unidentified (posted to suspense). Payment allocation follows configurable business rules determining the sequence in which liabilities are satisfied (e.g., penalties → interest → principal, or vice versa). Overpayments are credited to the taxpayer's general credit account.

**Assessment** — A liability-creating transaction. Assessments may be self-assessed (from filed returns), authority-assessed (from audit/review), or default-assessed (auto-generated for non-filers). Default assessments carry a caution flag to prevent excessively exaggerated amounts that create unrealistic revenue expectations.

**External (Downstream) Entities — owned by Debt Management.** The STA produces the data consumed by the Debt Management component's own entities — *DebtCase*, *InstalmentAgreement*, *InstalmentPayment*, *DemandNotice*, *RecoveryAction*, *WriteOff*, and *Agent*. These are **not** STA entities: the STA exposes enforceable balances, disputed-amount flags, credit positions, and debt-age inputs through its read APIs, and Debt Management maintains the case entities in its own store. Their attributes and lifecycle are defined in the Debt Management Requirements Specification.

**RevenueAccount** — Per tax type and fiscal year, the contra-side of double-entry postings. Tracks assessed (expected) revenue, collected revenue, and refundable amounts, mapped to the Chart of Accounts; includes the dedicated suspended-payments revenue account (STA-FR-046). Per-office account dimensions are an ITCAS-deferred divergence — office breakdown is provided at reporting level for the interim (STA-FR-041). Supports daily and Treasury reconciliation and year-end close/open (STA-FR-041, STA-FR-042).

**CreditAccount / SuspenseEntry** — The general credit account holds taxpayer overpayments pending set-off or refund; suspense entries hold **unidentified** payments pending identity resolution (aged per BR-STA-037, resolved per BR-STA-038) and **suspended** payments of identified taxpayers pending release or return (STA-FR-046, BR-STA-041), distinguished by suspense kind.








**Document** — Generated or received document associated with any entity. Types include: TAS (Tax Account Statement), demand notices, instalment agreements, clearance certificates, judicial letters, and supporting evidence. Documents are generated through the ADG (Automated Document Generation) subsystem and stored in the EDS (Electronic Document System).

**AuditLog** — Immutable record of every system action for compliance and accountability. Captures: timestamp, user, action type, affected entity, before/after values, and IP address. Retention period aligned with statutory requirements.

### 9.1.2 Key Relationships

The entity relationships follow the TTT principle as the overriding structural constraint:

- Taxpayer (1) → TaxAccount (1): Each taxpayer has exactly one STA record
- TaxAccount (1) → TaxType (N): L2 breakdown — one sub-account per registered tax type
- TaxType (1) → TaxPeriod (N): L3 breakdown — one period balance per tax period
- TaxPeriod (1) → Transaction (N): All financial movements within a period
- RevenueAccount (1) → Transaction (N): Contra-side postings for double-entry integrity
- All STA entities (N) → Document (N): Document attachments
- All entities (N) → AuditLog (N): Audit trail entries

## 9.2 ORS Data Access Patterns

The STA solution consumes data from the ORS/ClickHouse platform through a read-only integration pattern. The ORS implements a medallion architecture (Bronze → Silver → Gold) across 9 Informix databases containing 5,170+ tables. The STA application accesses data at the Bronze and Silver layers, with workflow state persisted locally.

### 9.2.1 ClickHouse Data Sources

The following ClickHouse databases and table prefixes are consumed by the STA solution:

| Source Database | CH Prefix | Key Tables for STA/DM | Est. Tables Used |
|---|---|---|---|
| Income Tax Core (1,570 tables) | `it_` | Taxpayer master, assessments, payments, returns, balances | ~80 |
| VAT (928 tables) | `vat_` | VAT returns, payments, refund claims, period balances | ~50 |
| ARS Accounting (461 tables) | `ars_` | Journal entries, payment reconciliation, revenue accounts | ~40 |
| Taxation Shared (219 tables) | `shared_` | Tax rates, code tables, office codes, period definitions | ~30 |
| CTD Property Tax (471 tables) | `ctd_` | Property assessments, stamp duty, transfer duty | ~20 |
| Web Income Tax (788 tables) | `wit_` | E-filed returns, online submissions, validation results | ~15 |
| VAT-Web (419 tables) | `vatweb_` | Online VAT submissions, portal payment records | ~10 |
| Old VAT Charging (270 tables) | `ovt_` | Historical VAT data (archive reference only) | ~5 |
| VIES (44 tables) | `vies_` | EU validation data (cross-reference only) | ~3 |

**Total estimated ClickHouse tables consumed: ~253** (5% of the full 5,170+ table inventory)

### 9.2.2 Query Patterns by Data Freshness Tier

The ORS pipeline classifies tables into four freshness tiers. The STA solution's query patterns align with these tiers:

**Hot Tier (< 5 min freshness, ~310 tables globally)**

Used for: real-time taxpayer account views, payment status checks, current-period balance queries, active debt case monitoring.

Query patterns: point lookups by TIN, filtered by current tax period. Typical query: retrieve all transactions for TIN X, TaxType Y, TaxPeriod Z with < 2 second response time. Supports the Unified Taxpayer Account view (UC-STA-01) and real-time debt case dashboard (UC-RPT-01).

Example consumption:
- `mtca_ors.it_taxpayer` — taxpayer master lookup
- `mtca_ors.it_assessments` — current period liabilities
- `mtca_ors.ars_journal_entries` — recent payment postings
- `mtca_ors.vat_transactions` — VAT period balances

**Warm Tier (< 1 hour freshness, ~775 tables globally)**

Used for: prior-period balance calculations, instalment compliance monitoring, demand notice generation triggers, aged debt analysis.

Query patterns: range scans across tax periods for a single taxpayer or aggregations across taxpayers for a single tax type/period. Supports statement generation (UC-STA-02), interest calculation (UC-STA-03), and aged debtors list (UC-DM-20).

**Cold Tier (4–24 hour freshness, ~2,065 tables globally)**

Used for: reference data lookups (tax rates, penalty rates, office codes, NACE classifications), report parameter population, historical trend analysis.

Query patterns: small lookups by code/ID, typically cached by the application layer. Supports rate configuration (UC-STA-10), report parameterisation (UC-RPT-01 through UC-RPT-05), and enforcement type configuration (UC-DM-16).

**Archive Tier (weekly, ~2,020 tables globally)**

Used for: historical comparison in reports, legacy data reference, audit trail cross-checking.

Query patterns: batch analytical queries run during off-peak hours. Supports year-over-year comparison in the Revenue Collection Report (RPT-FR-008) and historical debt stock analysis.

### 9.2.3 Data Access Interface

All data access from the the selected low-code/no-code platform platform to ClickHouse is via the ClickHouse HTTP REST API or JDBC connector:

**REST API Pattern (preferred for dashboard and real-time queries):**
- Endpoint: `https://<clickhouse-lb>:8443/`
- Authentication: X-ClickHouse-User / X-ClickHouse-Key headers
- Format: JSON (JSONEachRow) for application consumption, TabSeparated for bulk exports
- Connection pooling: managed by the selected platform's built-in datasource configuration

**JDBC Pattern (for complex joins and reporting queries):**
- Driver: `com.clickhouse.jdbc.ClickHouseDriver`
- Connection string: `jdbc:clickhouse://<clickhouse-lb>:8443/mtca_ors?ssl=true`
- Used primarily for the selected platform's built-in report builder and dataset beans

### 9.2.4 Silver/Gold Layer Views

The STA solution benefits from pre-built Silver and Gold layer views in ClickHouse, constructed using dbt models:

**Silver Layer (denormalised, cross-database joins):**
- `taxpayer_360` — unified taxpayer view joining registration data from Income Tax, VAT, FSS, and Property Tax databases
- `taxpayer_balances` — consolidated balance view per TIN across all tax types and periods (the L1/L2 STA view)
- `payment_history` — unified payment stream from all payment channels and tax types
- `assessment_register` — all assessments (self, authority, default) across tax types

**Gold Layer (aggregated, report-ready):**
- `debt_stock_summary` — current debt stock by category (C1–C5), age band, tax type, and office
- `revenue_collection_daily` — daily revenue collection totals by tax type and office
- `kpi_arrears_management` — pre-computed KPI metrics for arrears KPIs #24–35
- `instalment_compliance` — agreement compliance rates for KPIs #26, #29, #30, #34

### 9.2.5 Write-Back Requirements

The STA solution is read-only with respect to ClickHouse. All workflow state is persisted in the the selected platform platform's local database:

| Data Written Locally | Storage | Rationale |
|---|---|---|
| Debt case records (status, assignment, priority) | the selected platform MySQL/PostgreSQL | Workflow state not present in source systems |
| Instalment agreement records | the selected platform MySQL/PostgreSQL | New agreements created in DM workflow |
| Recovery action log | the selected platform MySQL/PostgreSQL | Enforcement actions initiated from DM |
| Demand notice tracking | the selected platform MySQL/PostgreSQL | Generation and delivery status |
| User work queue assignments | the selected platform MySQL/PostgreSQL | Case officer allocation |
| Notification templates and schedules | the selected platform MySQL/PostgreSQL | Configurable by administrators |

**Future write-back to Informix:** When ITCAS is operational (est. 2028–2029), workflow outcomes (e.g., confirmed write-offs, finalised instalment agreements) will need to be posted back to the transactional systems. This is deferred to the ITCAS integration phase and is not in scope for the interim solution. The the selected platform platform's data model is designed to support export via REST API for this future integration.

## 9.3 Data Quality Requirements

Data quality is critical for the STA solution because financial decisions (payment allocation, interest calculation, enforcement escalation) depend on accurate and complete source data. The following requirements define minimum quality standards.

### 9.3.1 Validation Rules

**At ingestion (ORS pipeline responsibility):**
- DQ-VAL-01: TIN format validation — all TINs conform to MOD11 check digit algorithm. Invalid TINs flagged in the pipeline's data quality dashboard. Target: < 0.01% invalid TINs in Hot tier tables.
- DQ-VAL-02: Amount consistency — PA + IA + PCA = total amount for every transaction. Discrepancies flagged. Target: 100% consistency.
- DQ-VAL-03: Date validity — all transaction dates within valid range (no future dates for historical transactions, no dates before 1970-01-01). Target: < 0.1% out-of-range dates.
- DQ-VAL-04: Referential integrity — every transaction references a valid TIN, TaxType code, and TaxPeriod. Orphaned transactions flagged. Target: < 0.05% orphaned records.

**At application layer (the selected platform form validation):**
- DQ-VAL-05: Instalment agreement amounts — total scheduled payments must equal or exceed total outstanding debt plus projected interest. Validation at form submission.
- DQ-VAL-06: Debt category assignment — system-calculated category (C1–C5) based on consolidated balance must match the thresholds defined in business rules BR-DM-001 through BR-DM-005.
- DQ-VAL-07: Enforcement action sequencing — actions must follow the escalation sequence defined for the debt category (Table 4 of the source PDF). Out-of-sequence actions require supervisor override.
- DQ-VAL-08: Write-off authorisation — write-off amounts must not exceed the outstanding balance for the relevant TIN/TaxType/TaxPeriod combination.

### 9.3.2 Reconciliation Rules

- DQ-REC-01: **STA balance reconciliation** — for every taxpayer, the sum of all L3 transaction amounts per tax type must equal the L2 tax-type balance. The sum of all L2 balances must equal the L1 consolidated balance. Reconciliation run daily as a scheduled ClickHouse query. Target: 100% reconciliation (zero tolerance).
- DQ-REC-02: **Payment-to-revenue reconciliation** — total payments recorded in taxpayer accounts must reconcile with total revenue posted to government revenue accounts. Checked daily. Target: < €100 daily variance (attributable to timing differences).
- DQ-REC-03: **Debt stock reconciliation** — total active debt cases in the selected platform must reconcile with total outstanding debit balances in ClickHouse (excluding C1 auto-write-offs). Checked weekly. Target: < 1% variance by count, < 0.5% by value.
- DQ-REC-04: **Instalment payment matching** — payments recorded in the ORS against a TIN under instalment must match the expected schedule in the selected platform. Checked daily. Mismatches trigger compliance alerts per BR-DM-040.

### 9.3.3 Completeness Thresholds

| Data Domain | Completeness Target | Measurement |
|---|---|---|
| Taxpayer registration (TIN, name, TPT) | 100% | All taxpayers with financial transactions must have complete registration data |
| Tax type registration per TIN | ≥ 99.5% | Cross-check: taxpayers with transactions in a tax type must be registered for that type |
| Transaction history (all tax types) | ≥ 99.9% | Row count comparison between Informix source and ClickHouse Bronze layer |
| Payment records | ≥ 99.9% | Reconciliation against bank settlement files |
| Contact information (email or phone) | ≥ 85% | Required for automated notification delivery |
| NACE classification for LPs | ≥ 90% | Required for sector-based reporting and risk analysis |

### 9.3.4 Data Freshness SLAs

| Use Case | Required Freshness | ORS Tier Alignment |
|---|---|---|
| Taxpayer account balance lookup | < 5 minutes | Hot |
| Payment status check | < 5 minutes | Hot |
| Debt case dashboard | < 15 minutes | Hot |
| Report generation (operational) | < 1 hour | Warm |
| Interest calculation batch | < 4 hours | Warm/Cold |
| KPI dashboard update | < 24 hours | Cold |
| Historical trend reports | < 1 week | Archive |

---

> **Scope note.** Entities owned by the downstream Debt Management component (Debt Case, Instalment Agreement, Enforcement Action) are shown here only as external references that consume STA-produced data; their internal attributes and lifecycle are specified in the Debt Management Requirements Specification.


---

# 10. INTERFACE REQUIREMENTS

## 10.1 User Interface Requirements

### 10.1.1 UI Design Principles

- **UI-PRIN-01: Responsive Design** — All screens must render correctly on desktop (1920×1080 minimum), tablet (1024×768), and mobile (375×667) viewports. the selected low-code/no-code platform's responsive form builder provides this natively.
- **UI-PRIN-02: WCAG 2.1 Level AA Compliance** — All screens must meet Web Content Accessibility Guidelines 2.1 at Level AA. This includes sufficient colour contrast (4.5:1 for normal text), keyboard navigability, screen reader compatibility, and visible focus indicators. Note: the selected low-code/no-code platform provides partial WCAG compliance natively; additional CSS customisation required for full AA conformance (ref: CMP Evaluation, §4.2).
- **UI-PRIN-03: Bilingual Support (Maltese/English)** — All user-facing labels, messages, tooltips, and generated documents must be available in both Maltese and English. Language selection stored in user profile with runtime switching. the selected platform supports i18n via resource bundles.
- **UI-PRIN-04: Role-Based Access** — Screen visibility and field editability controlled by user role. Seven roles defined: Case Officer, Senior Officer, Team Lead, Manager, Administrator, Auditor (read-only), and System Administrator.
- **UI-PRIN-05: Consistent Navigation** — Standard navigation pattern: left sidebar for module selection, top bar for breadcrumb/context, main content area with tabbed sub-sections. Consistent action buttons (Save, Submit, Cancel, Print) positioned bottom-right.
- **UI-PRIN-06: Data Density** — Screens designed for productivity: tables with sortable columns, inline search/filter, configurable column visibility, and export-to-Excel capability on all data grids.

### 10.1.2 Screen Inventory — STA Module

| Screen ID | Screen Name | Primary Actor | Key Functions | Source Reqs |
|---|---|---|---|---|
| STA-SCR-01 | Unified Taxpayer Account View | Case Officer | L1/L2/L3 drill-down, balance summary, transaction list, linked debt cases | STA-FR-001–005 |
| STA-SCR-02 | Transaction Detail | Case Officer | Single transaction view: type, amounts (PA/IA/PCA), date, source process, reversal link | STA-FR-006–008 |
| STA-SCR-03 | Account Statement Generator | Case Officer | TAS generation: template selection, period range, language, output format (PDF/screen) | STA-FR-009–013 |
| STA-SCR-04 | Payment Allocation View | Case Officer | Show allocation sequence, manual override (supervisor), suspense resolution | STA-FR-014–020 |
| STA-SCR-05 | Interest Calculation Detail | Case Officer | Rate lookup, daily calculation breakdown, effective date ranges, instalment rate switch | STA-FR-021–025 |
| STA-SCR-06 | Set-Off / Offset View | Senior Officer | Cross-tax-type credit application, confirmation workflow, legal authority check | STA-FR-026–028 |
| STA-SCR-07 | Reconciliation Dashboard | Manager | Balance reconciliation status, variances, drill-to-detail, exception queue | STA-FR-029–033 |
| STA-SCR-08 | Suspense Account Management | Senior Officer | Unidentified payments list, identity resolution form, re-allocation workflow | STA-FR-034–036 |
| STA-SCR-09 | Rate Configuration | Administrator | Interest rates, penalty rates, effective date management, rate history | STA-FR-037–040 |
| STA-SCR-10 | Tax Clearance Certificate | Case Officer | TAS query, positive/negative determination, certificate generation, provisos | STA-FR-041–045 |

### 10.1.4 Screen Inventory — Reporting and Administration

| Screen ID | Screen Name | Primary Actor | Key Functions | Source Reqs |
|---|---|---|---|---|
| RPT-SCR-01 | Collection Status Dashboard | Manager | Real-time KPI tiles: debt stock, collection rate, ageing, instalment compliance | RPT-FR-001–003 |
| RPT-SCR-02 | KPI Monitoring Dashboard | Director | 45 KPIs across 5 functional areas, trend charts, target vs actual, drill-down | RPT-FR-004–005 |
| RPT-SCR-03 | Revenue Reconciliation Report | Manager | Daily/monthly reconciliation, variance analysis, Treasury alignment | RPT-FR-006–008 |
| RPT-SCR-04 | Report Configuration | Any Officer | Parameter selection (tax type, period, office, sector), delimiter configuration, save/load | RPT-FR-009–015 |
| RPT-SCR-05 | Report Viewer | Any Officer | On-screen display, print, export (PDF, Excel, CSV), scheduled delivery | RPT-FR-016–021 |
| WF-SCR-01 | Workflow Configuration | Administrator | Process definition (BPMN), SLA thresholds, escalation rules, notification triggers | WF-FR-001–010 |
| WF-SCR-02 | Work Queue Management | Team Lead | Queue assignment, rebalancing, priority adjustment, performance monitoring | WF-FR-011–015 |
| WF-SCR-03 | Case Reassignment | Team Lead | Bulk reassignment, absence coverage, load balancing across officers | WF-FR-016–020 |
| ADM-SCR-01 | Business Rules Administration | Administrator | Rule catalogue view, rule editing (configurable rules), effective dating, audit trail | BR-* (77 configurable rules) |
| ADM-SCR-02 | Notification Template Manager | Administrator | Template editing (Maltese/English), variable insertion, channel configuration, preview | WF-FR-006–008 |
| ADM-SCR-03 | User & Role Management | System Admin | User accounts, role assignment, permission matrix, access log | NFR-SEC-001–005 |

**Total screen count: 26 screens** (10 STA + 12 DM + 4 RPT/WF/ADM)

### 10.1.5 Key Screen Descriptions

**STA-SCR-01: Unified Taxpayer Account View** — The primary STA screen. Opens with a TIN search. Displays the L1 consolidated balance prominently (total debit, total credit, net position). Below, a tabbed interface shows L2 tax-type balances. Selecting a tax type expands to L3 period-by-period transaction history. Each level shows PA/IA/PCA decomposition. Colour-coded status indicators: green (credit/zero), amber (debit < 30 days), red (debit > 30 days). Right panel shows linked debt cases, active instalment agreements, and recent TAS documents. Actions available: Generate Statement, View Clearance Status, Create Debt Case.

**DM-SCR-01: Debt Case Dashboard** — The primary DM working screen. Top row: KPI summary tiles (total active cases, total debt value, cases approaching SLA, overdue actions). Main content: sortable data grid of assigned cases with columns: TIN, taxpayer name, debt category (C1–C5), total amount, oldest debt age, risk score, current status, next action due date. Filters: category, status, officer assignment, age band, tax type. Quick actions: Open Case, Generate Demand Notice, Log Phone Call, Create Instalment Application.

**DM-SCR-05: Instalment Agreement Form** — Multi-step form following the the selected platform form wizard pattern. Step 1: Taxpayer identification (TIN lookup, auto-populate name/contact). Step 2: Debt summary (auto-populated from ClickHouse — all outstanding balances by tax type/period). Step 3: Schedule configuration (number of instalments, frequency, start date, total amount — system calculates per-instalment amount with interest). Step 4: Approval routing (auto-route based on amount thresholds: within standard parameters → auto-approve; outside parameters → supervisor approval per BR-INST-05). Step 5: Confirmation and taxpayer notification.

## 10.2 System Interface Requirements

### 10.2.1 ORS / ClickHouse REST API

| Interface ID | Direction | Protocol | Purpose | Frequency |
|---|---|---|---|---|
| INT-CH-01 | Read | HTTP/REST | Taxpayer account data (L1/L2/L3 balances, transactions) | Real-time, per user action |
| INT-CH-02 | Read | HTTP/REST | Debt stock queries (aggregate debt by category, age, tax type) | Real-time, dashboard refresh |
| INT-CH-03 | Read | JDBC | Report generation queries (complex joins, aggregations) | On-demand / scheduled |
| INT-CH-04 | Read | HTTP/REST | Reference data (tax rates, codes, periods, offices) | Cached, refreshed every 4 hours |
| INT-CH-05 | Read | HTTP/REST | Payment event polling (new payments for instalment matching) | Every 5 minutes |

**Authentication:** Service account with read-only privileges on `mtca_ors` database. SSL/TLS mandatory. API key rotation every 90 days.

**Error handling:** Circuit breaker pattern with 3 retry attempts, 5-second backoff. Fallback: cached data with staleness indicator displayed to user. ClickHouse health check endpoint polled every 30 seconds.

### 10.2.2 SAS VIYA Scoring API

| Interface ID | Direction | Protocol | Purpose | Frequency |
|---|---|---|---|---|
| INT-SAS-01 | Read | REST/JSON | Risk score retrieval for a specific TIN | On-demand, when case opened |
| INT-SAS-02 | Read | REST/JSON | Batch risk scoring for work queue prioritisation | Daily batch (overnight) |
| INT-SAS-03 | Read | REST/JSON | Risk factor breakdown (contributing factors to score) | On-demand, risk panel display |
| INT-SAS-04 | Write | REST/JSON | Case outcome feedback (for model retraining) | Event-driven, on case closure |

**Data flow:** ClickHouse risk data marts → SAS VIYA → risk score API → the selected low-code/no-code platform. The SAS VIYA API gateway serves as the intermediary. Risk scores are numeric (0–100) with accompanying factor weights.

**SLA:** Score retrieval < 2 seconds for individual TIN, < 30 minutes for full batch.

### 10.2.3 Notification Gateway

| Interface ID | Direction | Protocol | Purpose | Frequency |
|---|---|---|---|---|
| INT-NOT-01 | Write | SMTP | Email notifications (demand notices, instalment reminders, status updates) | Event-driven |
| INT-NOT-02 | Write | SMS Gateway API | SMS reminders (payment due dates, instalment defaults) | Event-driven |
| INT-NOT-03 | Write | REST | In-app notifications (the selected platform notification centre) | Event-driven |
| INT-NOT-04 | Write | REST/Print API | Physical letter generation (demand notices, judicial letters) — queued for postal services | Batch daily |

**Template management:** Notification content driven by templates stored in the selected platform, supporting Maltese/English, with merge variables for taxpayer name, TIN, amounts, dates, and reference numbers.

### 10.2.4 Document Generation (ADG)

| Interface ID | Direction | Protocol | Purpose | Frequency |
|---|---|---|---|---|
| INT-DOC-01 | Write/Read | REST | TAS generation (Tax Account Statement in PDF) | On-demand |
| INT-DOC-02 | Write/Read | REST | Demand notice document generation (PDF, templated) | Event-driven |
| INT-DOC-03 | Write/Read | REST | Instalment agreement document generation (PDF) | On agreement approval |
| INT-DOC-04 | Write/Read | REST | Tax clearance certificate generation (PDF) | On-demand |
| INT-DOC-05 | Read | REST | Document retrieval from EDS (Electronic Document System) | On-demand |

**Technology:** the selected platform's built-in PDF generation for simple documents; external ADG service (Apache FOP or similar) for complex templated documents requiring precise formatting for legal compliance (judicial letters, warrant documents).

### 10.2.5 External System Interfaces

| Interface ID | Direction | Protocol | Purpose | Phase |
|---|---|---|---|---|
| INT-EXT-01 | Read | REST/SFTP | Bank payment file ingestion (settlement files for payment matching) | Phase 1 |
| INT-EXT-02 | Write | REST | Bank account garnishing requests (automated freezing via web services) | Phase 2 |
| INT-EXT-03 | Read | REST | Land/property register lookup (asset identification for enforcement) | Phase 2 |
| INT-EXT-04 | Read | REST | Vehicle register lookup (asset identification) | Phase 2 |
| INT-EXT-05 | Read/Write | REST | Government CRM (MS Dynamics) — complaint/interaction tracking | Phase 3 |
| INT-EXT-06 | Read | REST | Taxpayer portal — self-service TAS requests, instalment applications | Phase 2 |
| INT-EXT-07 | Read/Write | REST | Treasury interface — revenue reconciliation, refund forecasting | Phase 3 |

### 10.2.6 Future ITCAS Integration Points

These interfaces are designed but not implemented in the interim solution. They define the migration pathway:

| Interface ID | Direction | Protocol | Purpose | ITCAS Phase |
|---|---|---|---|---|
| INT-ITCAS-01 | Write | REST | Post confirmed write-offs to ITCAS accounting module | Phase 2 (T0+36 weeks+) |
| INT-ITCAS-02 | Write | REST | Post finalised instalment agreements to ITCAS | Phase 2 |
| INT-ITCAS-03 | Read | REST | Read taxpayer data from ITCAS (replacing ClickHouse reads) | Phase 2 |
| INT-ITCAS-04 | Read | Events/Kafka | Receive real-time transaction events from ITCAS | Phase 2 |
| INT-ITCAS-05 | Bidirectional | REST | Case management synchronisation (ITCAS CM ↔ the selected platform) | Phase 2/3 |

**Migration strategy:** Data source abstraction layer in the selected platform ensures that switching from ClickHouse to ITCAS requires configuration changes (connection strings, query mapping) rather than application code changes. BPMN 2.0 process definitions export from the selected platform and import into ITCAS's BPM engine.

---

---

# 11. IMPLEMENTATION CONSIDERATIONS

## 11.1 Phasing Strategy

The STA solution is implemented across four phases, aligned with ORS readiness milestones and ITCAS vendor timeline. Each phase delivers measurable capability and is gated by explicit go/no-go criteria.

### Phase 1: Core STA Foundation (Q2 2026 — April to June)

**Objective:** Establish the unified taxpayer account view and basic balance enquiry capability using ORS/ClickHouse data.

**Prerequisites:** ORS POC validated (Gate G1, March 2026); Income Tax and VAT databases loaded in ClickHouse; the selected low-code/no-code platform platform deployed.

**Deliverables:**
- STA-SCR-01 (Unified Account View) — L1/L2/L3 drill-down for Income Tax and VAT
- STA-SCR-02 (Transaction Detail) — individual transaction display
- STA-SCR-03 (Account Statement Generator) — basic TAS in PDF
- STA-SCR-10 (Tax Clearance Certificate) — positive/negative clearance
- INT-CH-01 through INT-CH-04 — ClickHouse integration operational
- Core data model (Taxpayer, TaxAccount, TaxType, TaxPeriod, Transaction entities)

**FTE Impact:** ~2–3 FTE reduction through automated TAS generation and self-service clearance checks (currently manual processes).

**Go/No-Go Gate:** ORS Hot tier achieving < 5 min freshness for Income Tax and VAT tables. Minimum 99.5% data accuracy on balance reconciliation.

### Phase 2: Debt Management Workflows (Q3–Q4 2026 — July to December)

**Objective:** Implement the full debt management case lifecycle, instalment agreements, and automated notification workflows.

**Prerequisites:** Phase 1 stable in production; ARS Accounting and remaining databases loaded in ClickHouse; SAS VIYA risk scoring API operational.

**Deliverables:**
- DM-SCR-01 through DM-SCR-12 — all Debt Management screens
- STA-SCR-04 through STA-SCR-09 — remaining STA screens (payment allocation, interest, set-off, reconciliation, suspense, rate config)
- Full BPMN workflow processes: debt case lifecycle, instalment lifecycle, enforcement escalation, write-off approval
- INT-SAS-01 through INT-SAS-04 — SAS VIYA integration
- INT-NOT-01 through INT-NOT-04 — notification gateway
- INT-DOC-01 through INT-DOC-05 — document generation
- INT-EXT-01 (bank payment files), INT-EXT-02 (garnishing), INT-EXT-06 (portal)
- Business rules engine: all 124 business rules configured (77 configurable via UI)

**FTE Impact:** ~8–10 FTE reduction through automated debt case creation, automated demand notice generation, self-service instalment applications, and risk-based work queue prioritisation. This is the primary FTE liberation target for debt management.

**Go/No-Go Gate:** All 9 Informix databases loaded in ClickHouse with > 99% freshness SLA compliance. SAS VIYA scoring API response time < 2 seconds. Minimum 20 debt cases successfully processed end-to-end in UAT.

### Phase 3: Advanced DM and External Integration (Q1 2027 — January to March)

**Objective:** Extend enforcement capabilities with external system integrations and advanced analytical features.

**Deliverables:**
- INT-EXT-03 (property register), INT-EXT-04 (vehicle register) — asset identification for enforcement
- INT-EXT-05 (CRM integration) — complaint/interaction tracking
- INT-EXT-07 (Treasury interface) — real-time revenue reconciliation
- Enhanced risk scoring: payment likelihood prediction, network analysis
- Bulk processing capabilities: batch demand notices, bulk write-offs, mass case reassignment
- Agent (Warrant) management: appointment, case assignment, outcome tracking

**FTE Impact:** ~1–2 FTE additional through automated asset identification and bulk processing.

**Go/No-Go Gate:** Phase 2 stable for 90+ days. External system APIs verified in integration testing. Legal authority confirmed for automated garnishing and name publication.

### Phase 4: Full Reporting and ITCAS Preparation (Q2 2027 — April to June)

**Objective:** Complete the reporting suite and prepare for ITCAS migration.

**Deliverables:**
- RPT-SCR-01 through RPT-SCR-05 — all reporting screens with full KPI coverage
- WF-SCR-01 through WF-SCR-03 — workflow administration
- ADM-SCR-01 through ADM-SCR-03 — administration screens
- Gold-layer ClickHouse views for all 45 KPIs
- BPMN 2.0 export package for ITCAS migration
- Data migration specifications: the selected platform workflow data → ITCAS format
- Operational runbook and knowledge transfer documentation

**FTE Impact:** Reporting automation contributes to the broader ~19 FTE target for ORS Priority Reports.

**Go/No-Go Gate:** All 45 KPIs producing accurate data. BPMN export validated for import compatibility. ITCAS requirements verification (DcP3, June 2026) completed.

### Phase Summary

| Phase | Timeline | Screens | Integrations | FTE Impact | Total Reqs Covered |
|---|---|---|---|---|---|
| 1 | Q2 2026 | 4 STA | 4 CH | 2–3 | ~40 |
| 2 | Q3–Q4 2026 | 6 STA + 12 DM | SAS, notifications, docs, bank, portal | 8–10 | ~130 |
| 3 | Q1 2027 | Enhancements | Property, vehicle, CRM, Treasury | 1–2 | ~15 |
| 4 | Q2 2027 | 4 RPT + 3 WF + 3 ADM | KPI views, ITCAS prep | (included in ORS) | ~8 |
| **Total** | **12 months** | **26 screens** | **~25 interfaces** | **~11–15** | **193** |

## 11.2 ITCAS Migration Strategy

The STA/DM interim solution is explicitly designed as a bridge to the ITCAS integrated platform. The migration strategy ensures that no investment is wasted and that the transition is technically feasible.

### 11.2.1 What Migrates to ITCAS

| Component | Migration Path | ITCAS Target |
|---|---|---|
| **BPMN process definitions** | Export from the selected platform as BPMN 2.0 XML → import into ITCAS BPM engine | ITCAS Case Management (§6.7/6.8) |
| **Business rules** | 77 configurable rules exported as decision tables → reimplemented in ITCAS rules engine | ITCAS Configuration Module |
| **Workflow data** (active cases, agreements) | Database export → transformation → ITCAS import via migration scripts | ITCAS Debt Management (§6.8) |
| **Document templates** | Template content (Maltese/English) → recreated in ITCAS ADG | ITCAS Document Generation |
| **Notification configurations** | Channel/template/schedule → recreated in ITCAS notification system | ITCAS Communications |
| **Report definitions** | Query logic and KPI formulas → reimplemented using ITCAS reporting | ITCAS MIS/Reporting |

### 11.2.2 What Stays / Retires

| Component | Action | Rationale |
|---|---|---|
| **the selected low-code/no-code platform platform** | Retire after ITCAS DM module goes live (~2028) | Interim solution replaced by integrated system |
| **ClickHouse integration layer** | Redirected to ITCAS PostgreSQL data sources | ORS continues as analytical platform, but STA/DM reads switch to ITCAS |
| **the selected platform-local database** (workflow state) | Archived after data migration to ITCAS | Historical reference only |
| **SAS VIYA integration** | Retained — SAS continues as analytical layer on top of ITCAS/ClickHouse | Long-term analytical capability |

### 11.2.3 BPMN Portability

The the selected low-code/no-code platform platform supports BPMN 2.0 process export. The following measures ensure portability:

- All workflow processes modelled using standard BPMN 2.0 constructs (tasks, gateways, events, timers) without the selected platform-proprietary extensions
- Process variables use generic data types (string, number, date, boolean) — no the selected platform-specific bindings
- Integration points abstracted behind service tasks with configurable endpoint URLs — switching from ClickHouse to ITCAS requires endpoint reconfiguration, not process redesign
- Human tasks reference roles (not specific users) — role mapping translatable to any BPM platform
- Decision logic encapsulated in external business rules (not embedded in process flows) — rules independently portable

**Limitation:** the selected platform-specific form bindings (UI layout, field mappings) do not export via BPMN 2.0. Forms must be rebuilt in the ITCAS platform. The interim solution mitigates this by documenting all form specifications in the requirements document (Section 10.1) to serve as rebuilding blueprints.

### 11.2.4 Data Migration

Active workflow data must migrate from the selected platform's local database to ITCAS:

- **Active debt cases:** case records, action histories, assigned officers, current status → mapped to ITCAS case management schema
- **Active instalment agreements:** schedules, payment history, compliance status → mapped to ITCAS instalment module
- **Pending enforcement actions:** in-progress garnishing orders, liens, agent assignments → manual verification during parallel running
- **Historical data:** completed cases, expired agreements → bulk migration for reference (lower priority, can be deferred)

**Parallel running:** A minimum 3-month parallel operation period where both the selected platform and ITCAS DM modules run concurrently on the same data. Case officers work in ITCAS while the selected platform remains available for reference. Cut-over occurs when ITCAS DM achieves functional parity validated by business users.

### 11.2.5 Timeline Alignment

| Milestone | Date (Est.) | STA/DM Implication |
|---|---|---|
| ITCAS T0 (contract start) | Mar 2026 | STA/DM Phase 1 under development concurrently |
| DcP3 (Requirements verification) | Jun 2026 | STA/DM requirements inform ITCAS DM requirements |
| DcP4.1 (Phase 1 design) | Oct 2026 | Confirm ITCAS DM architecture compatible with STA/DM migration |
| ITCAS Phase 2 start (Payments, Enforcement) | T0+36 weeks (~Nov 2026) | STA/DM Phase 2 in production; ITCAS team can reference working system |
| ITCAS Phase 2 UAT | Est. H2 2027 | Begin parallel running preparation |
| ITCAS Phase 2 go-live | Est. H1 2028 | STA/DM migration window opens |
| STA/DM retirement | Est. H2 2028 | Full transition to ITCAS |

## 11.3 Risks and Mitigations

### 11.3.1 Data Quality Risk

**Risk:** Source data in legacy Informix systems contains inconsistencies, missing records, or incorrect balances that propagate through ORS/ClickHouse into the STA solution, leading to incorrect taxpayer account views, erroneous interest calculations, or invalid enforcement actions.

**Likelihood:** High. The source PDF notes that most KPI data is "Not Yet" available, and legacy systems have been operating without integrated reconciliation for decades.

**Impact:** High. Incorrect enforcement actions against taxpayers could cause legal exposure, reputational damage, and taxpayer complaints.

**Mitigations:**
- Implement reconciliation rules DQ-REC-01 through DQ-REC-04 as automated daily checks with exception reporting
- Establish data quality thresholds (Section 9.3.3) as go/no-go criteria for each phase
- Create a data quality dashboard (Grafana) comparing Informix source counts against ClickHouse
- Require manual verification for all enforcement actions > €1,000 during the first 6 months of operation
- Engage the Data Catalogue (Apache Atlas) to document known quality issues per table

### 11.3.2 User Adoption Risk

**Risk:** MTCA staff resist the new STA system due to unfamiliarity with the the selected platform platform, preference for established manual processes, or distrust of automated decisions (particularly automated write-offs and debt categorisation).

**Likelihood:** Medium. MTCA has limited experience with digital workflow systems.

**Impact:** High. Low adoption undermines the 8–10 FTE liberation target and delays realisation of benefits.

**Mitigations:**
- Involve 4–6 case officers as pilot users during UAT for each phase; incorporate their feedback before rollout
- Design screens to match existing mental models (e.g., the TAS format familiar to officers)
- Provide supervisor override for all automated decisions during the first 3 months
- Deliver role-specific training: 2-day hands-on workshops per role, plus self-paced e-learning in Maltese/English
- Measure adoption weekly via system usage dashboards; address drop-offs with targeted coaching

### 11.3.3 Integration Complexity Risk

**Risk:** Multiple system integrations (ClickHouse, SAS VIYA, notification gateway, document generation, external registers) create a complex dependency chain where failure of any component degrades or blocks STA/DM operations.

**Likelihood:** Medium. Each integration is technically straightforward but the combined reliability requirement is demanding.

**Impact:** Medium. Degraded service during integration outages; potential data inconsistency if partial failures are not handled correctly.

**Mitigations:**
- Implement circuit breaker pattern on all external API calls with graceful degradation (cached data + staleness warning)
- ClickHouse health check endpoint polled every 30 seconds; automatic failover to replica if primary unresponsive
- SAS VIYA scoring: cache risk scores locally with 24-hour TTL; display cached score with timestamp if API unavailable
- Notification delivery: asynchronous queue (the selected platform's built-in scheduler) with retry logic; batch failures reported to administrators
- Integration testing environment that simulates each external system's failure modes

### 11.3.4 Platform Limitations Risk

**Risk:** the selected low-code/no-code platform's partial WCAG 2.1 compliance, basic case management capabilities (vs. high-end enterprise platforms), and limited process simulation may constrain advanced functionality or require costly workarounds.

**Likelihood:** Low-Medium. The CMP Evaluation (§4.2) identifies specific gaps: digital signatures require plugins, rule testing/simulation is limited, and case hierarchy (parent-child) support is basic.

**Impact:** Low-Medium. Workarounds increase development effort by an estimated 15–20% for affected features.

**Mitigations:**
- Accept the selected platform limitations for the interim period (12–24 months); plan full capability delivery in ITCAS
- Budget 20% contingency in development effort for WCAG remediation (CSS customisation, ARIA attributes)
- Use the selected platform's plugin marketplace (500+ components) for gaps: digital signatures, advanced charts, bulk processing
- For case hierarchy requirements (debt case → enforcement sub-cases): implement using the selected platform's sub-process feature with parent reference fields rather than native parent-child case types

### 11.3.5 ORS Readiness Timing Risk

**Risk:** The ORS ClickHouse pipeline does not achieve production-ready status on the timeline required for STA/DM Phase 1 (Q2 2026), delaying or blocking the STA solution.

**Likelihood:** Medium. ORS depends on infrastructure procurement, MITA network access, and successful completion of Phase 2/3 pipeline rollout across all 9 databases.

**Impact:** High. STA/DM cannot deliver value without access to taxpayer data in ClickHouse.

**Mitigations:**
- STA/DM Phase 1 requires only Income Tax and VAT databases — align with ORS Phase 2 (weeks 3–6 of ORS timeline), not full Phase 3
- Establish explicit ORS readiness checkpoints in the STA/DM project plan: data availability per database at T-4 weeks before each STA/DM phase
- Contingency: if ClickHouse data for a specific database is delayed, STA/DM can launch with partial tax type coverage and expand incrementally
- ORS Gate G1 (60x performance validation) in March 2026 serves as the go/no-go for STA/DM Phase 1 development start

### 11.3.6 ITCAS Coordination Risk

**Risk:** The STA/DM interim solution and ITCAS vendor implementation create competing or contradictory requirements, duplicate effort, or political tension between the parallel initiatives.

**Likelihood:** Medium. The source PDF explicitly warns of potential "implementer reluctance and potential for disputes" regarding integration between quick-win components and ITCAS.

**Impact:** Medium-High. Wasted investment, confused stakeholders, or contractual disputes with the ITCAS vendor.

**Mitigations:**
- Position STA/DM explicitly as a "proof of concept and operational bridge" that de-risks ITCAS DM requirements by providing a working reference implementation
- Share STA/DM requirements with the ITCAS vendor during DcP3 requirements verification (June 2026) as input to their DM module design
- Establish clear governance: STA/DM owned by the STX/Data Unit; ITCAS DM owned by the vendor; joint review at quarterly steering committee
- Ensure all STA/DM design decisions prioritise ITCAS portability (BPMN 2.0, standard APIs, no proprietary extensions)
- Document lessons learned from STA/DM operation as formal input to ITCAS DM acceptance criteria

### Risk Summary Matrix

| # | Risk | Likelihood | Impact | Severity | Primary Mitigation |
|---|---|---|---|---|---|
| R1 | Data quality | High | High | **Critical** | Automated reconciliation + manual verification threshold |
| R2 | User adoption | Medium | High | **High** | Pilot users + supervisor overrides + role-specific training |
| R3 | Integration complexity | Medium | Medium | **Medium** | Circuit breakers + cached fallbacks + integration test environment |
| R4 | Platform limitations | Low-Medium | Low-Medium | **Low** | Accept for interim + plugin workarounds + ITCAS migration |
| R5 | ORS readiness timing | Medium | High | **High** | Phased database dependency + ORS Gate G1 checkpoint |
| R6 | ITCAS coordination | Medium | Medium-High | **Medium-High** | Governance separation + shared requirements + BPMN portability |

> **Phasing note for the STA.** Phase 1 (Core STA Foundation) is the primary scope of this specification. The later phases listed above (Debt Management workflows, advanced integration, full reporting) consume the STA foundation and are governed by their own specifications; they are retained here to show the STA's place in the overall interim programme and the ITCAS migration timeline.


---

# 12. TRACEABILITY MATRIX

## 12.1 STA Requirements → Use Cases


| Requirement | Use Case(s) | Notes |
|---|---|---|
| STA-FR-001 | UC-STA-01 | Unified account view |
| STA-FR-002 | UC-STA-01 | Consolidated balance display |
| STA-FR-003 | UC-STA-01, UC-STA-07 | TTT drill-down structure |
| STA-FR-004 | UC-STA-01, UC-STA-07 | Balance decomposition (PA, IA, PCA) |
| STA-FR-005 | UC-STA-01, UC-STA-07 | Transaction-level detail |
| STA-FR-006 | UC-STA-01 | Multi-tax-type aggregation |
| STA-FR-007 | UC-STA-01 | Real-time balance refresh |
| STA-FR-008 | UC-STA-01, UC-STA-07, UC-STA-12 | Account status indicators |
| STA-FR-009 | UC-STA-06, UC-STA-09 | Fiscal year boundaries |
| STA-FR-010 | UC-STA-01, UC-STA-02 | Tax type configuration |
| STA-FR-011 | UC-STA-01, UC-STA-02, UC-STA-13 | Taxpayer registration display |
| STA-FR-012 | UC-STA-04, UC-STA-05, UC-STA-06, UC-STA-08, UC-STA-09 | Double-entry posting |
| STA-FR-013 | UC-STA-04, UC-STA-05, UC-STA-07, UC-STA-11 | Credit/debit balance maintenance |
| STA-FR-014 | UC-STA-03, UC-STA-05, UC-STA-07 | Balance-forward presentation |
| STA-FR-015 | UC-STA-09 | End-of-period roll-over |
| STA-FR-016 | UC-STA-08 | Set-off across tax types |
| STA-FR-017 | UC-STA-04 | Payment allocation — FIFO |
| STA-FR-018 | UC-STA-04 | Payment allocation — proportional |
| STA-FR-019 | UC-STA-04 | Payment allocation — directed |
| STA-FR-020 | UC-STA-04, UC-STA-08 | Payment allocation — configurable rules |
| STA-FR-021 | UC-STA-12 | Suspense account management |
| STA-FR-022 | UC-STA-04, UC-STA-05, UC-STA-11 | Payment reversal/correction |
| STA-FR-023 | UC-STA-03 | Interest calculation — daily accrual |
| STA-FR-024 | UC-STA-03, UC-STA-10 | Interest rates — configurable |
| STA-FR-025 | UC-STA-03, UC-STA-10 | Interest calculation — compound/simple |
| STA-FR-026 | UC-STA-03 | Interest calculation — automatic batch |
| STA-FR-027 | UC-STA-03 | Penalty auto-calculation (cross-cutting) |
| STA-FR-028 | UC-STA-03 | Fine auto-calculation (cross-cutting) |
| STA-FR-029 | UC-STA-10 | Penalty rate configuration |
| STA-FR-030 | UC-STA-05 | Credit/debit adjustments |
| STA-FR-031 | UC-STA-10 | Adjustment approval workflow |
| STA-FR-032 | UC-STA-08 | Cross-tax-type offsetting |
| STA-FR-033 | UC-STA-08 | Set-off rules configuration |
| STA-FR-034 | UC-STA-06 | Reconciliation — automated |
| STA-FR-035 | UC-STA-06 | Reconciliation — exception handling |
| STA-FR-036 | UC-STA-06 | Reconciliation — Treasury matching |
| STA-FR-037 | UC-STA-02, UC-STA-13 | TAS generation |
| STA-FR-038 | UC-STA-02, UC-STA-13 | TAS template configuration |
| STA-FR-039 | UC-STA-02, UC-STA-13 | TAS clearance certificate |
| STA-FR-040 | UC-STA-02, UC-STA-13 | TAS multi-period support |
| STA-FR-041 | UC-STA-09 | Revenue accounting per tax type |
| STA-FR-042 | UC-STA-10 | Chart of Accounts (architecture/configuration) |
| STA-FR-043 | — | Revenue disbursement principles (backend rule; no direct UC) |
| STA-FR-044 | — | Data migration (implementation activity; Section 11) |
| STA-FR-045 | UC-STA-01, UC-STA-04 | Multi-currency (cross-cutting; deferred to ITCAS) |

## 12.2 STA Requirements → Business Rules


| Requirement Group | Requirements | Governing Business Rules |
|---|---|---|
| Unified Account / Balance | STA-FR-001–008 | BR-STA-001–005 (account structure, TTT hierarchy, balance decomposition) |
| Double-Entry / Posting | STA-FR-009–014 | BR-STA-006–010 (posting rules, debit/credit matching, fiscal year boundaries) |
| Payment Allocation | STA-FR-017–022 | BR-STA-011–018 (FIFO, proportional, directed allocation, reversal rules) |
| Interest & Penalties | STA-FR-023–029 | BR-STA-019–025 (rate application, accrual periods, compound/simple rules, caps) |
| Credit/Debit Adjustment | STA-FR-030–031 | BR-STA-026–028 (adjustment authority, approval thresholds, audit requirements) |
| Set-Off / Offsetting | STA-FR-032–033 | BR-STA-029–031 (cross-tax-type rules, priority ordering, minimum balance) |
| Reconciliation | STA-FR-034–036 | BR-STA-032–035 (tolerance thresholds, exception handling, Treasury matching) |
| TAS / Statements | STA-FR-037–040 | BR-STA-036–038 (template selection, period coverage, clearance logic) |
| Revenue Accounting | STA-FR-041–043, STA-FR-049 | BR-STA-039–040 (fiscal year close, disbursement principles) |
| Suspension / Account Lifecycle | STA-FR-046–048; INT-FR-019 | BR-STA-041–044 (suspended-payment exclusion and release, objection-allowed gate, tax-year derivation, debtor's-debtor posting) |

## 12.3 Requirements → KPIs


This matrix maps requirements to the 45 KPIs defined in Section 2.2 (sourced from PDF Table 5). KPIs are grouped by strategic objective.

### 12.3.1 Objective 1: Unified Taxpayer Financial Visibility (KPI Items 1–10)

| KPI Item | KPI Description | Contributing Requirements |
|---|---|---|
| Items 7–9 | New registrations (CIT, VAT, PIT) | STA-FR-010, STA-FR-011, RPT-FR-003 |
| Item 4 | Taxpayer satisfaction | STA-FR-037–040 (TAS quality), NFR-016–018 (usability) |
| Item 10 | Website visits | INT-FR-016 (portal integration; Won't Have for interim) |

### 12.3.2 Objective 2: Returns Filing and Payment Compliance (KPI Items 11–23)

| KPI Item | KPI Description | Contributing Requirements |
|---|---|---|
| Items 11–14 | Returns filing rates | RPT-FR-003, RPT-FR-006, INT-FR-001–003 (ORS data access) |
| Item 15 | Revenue from unfiled return interventions | RPT-FR-004, DM-FR-047–049 (default assessment) |
| Items 20–22 | Refund processing timeliness | STA-FR-016 (set-off), RPT-FR-003, RPT-FR-010 |
| Item 23 | Revenue from return review | RPT-FR-003, RPT-FR-004 |

### 12.3.3 Objective 3: Transform Tax Debt Management (KPI Items 24–35)

| KPI Item | KPI Description | Contributing Requirements |
|---|---|---|
| Item 24 | New debt as % of revenue | DM-FR-001–002, RPT-FR-001, RPT-FR-003 |
| Item 25 | Total collectable debt as % of revenue | DM-FR-057, RPT-FR-001, RPT-FR-002 |
| Item 26 | Debt under instalment arrangement | DM-FR-021–028, RPT-FR-011 |
| Item 27 | Debt < 6 months age | DM-FR-004, RPT-FR-008 |
| Item 28 | Collectable debt < 2 years age | DM-FR-004, RPT-FR-001, RPT-FR-008 |
| Item 29 | Instalment compliance rate | DM-FR-026–027, RPT-FR-011 |
| Items 31–33 | Demand notes, agreements, revenue collected | DM-FR-012–013, DM-FR-021, RPT-FR-009 |
| Item 34 | Non-compliance requiring stronger measures | DM-FR-015, DM-FR-031–036, RPT-FR-009 |
| Item 35 | New debt resolved within 6 months | DM-FR-001–005, DM-FR-050, RPT-FR-001 |

### 12.3.4 Objective 4: Audit Performance Monitoring (KPI Items 36–43)

| KPI Item | KPI Description | Contributing Requirements |
|---|---|---|
| Items 36, 38 | Inspections and audits conducted | RPT-FR-003, RPT-FR-013, INT-FR-001 |
| Item 37 | Revenue from inspection actions | RPT-FR-004, RPT-FR-010 |
| Items 39, 41 | Assessments and revenue from compliance | RPT-FR-003, RPT-FR-004, INT-FR-001 |
| Item 42 | Revenue per hour of compliance activity | RPT-FR-013, RPT-FR-003 |

### 12.3.5 Objective 5: Legal Proceedings Monitoring (KPI Items 44–45)

| KPI Item | KPI Description | Contributing Requirements |
|---|---|---|
| Item 44 | Appeals decided in favour of Commissioner | RPT-FR-003, INT-FR-001 (data from legacy/ITCAS) |
| Item 45 | Prosecutions decided in favour of Commissioner | RPT-FR-003, DM-FR-035 |

### 12.3.6 Objective 6: FTE Liberation Through Automation

| Target | Contributing Requirements |
|---|---|
| 8–10 FTE from debt management | DM-FR-009–014 (automated notifications), DM-FR-015 (automated escalation), DM-FR-030 (auto write-off), WF-FR-001–003 (automated workflow), WF-FR-006–007 (work queues) |
| Reporting efficiency | RPT-FR-001–021 (automated dashboards replacing manual compilation) |
| Reconciliation efficiency | STA-FR-034–036 (automated reconciliation replacing manual matching) |

---

## 12.4 Requirements → Source Documents


This matrix traces each requirement domain to its originating source documents. Source abbreviations follow the conventions in Section 1.5.3.

| Requirement Domain | Primary Source | Secondary Sources |
|---|---|---|
| STA-FR-001–011 (Unified Account) | PDF-§3, PDF-§3.1 | RFP-§3.5.5.4.11; VP-§6.6 |
| STA-FR-012–016 (Accounting Engine) | PDF-§3.1, PDF-§3.3 | VP-§6.6.2; RFP-§3.5.5.4.11.2 |
| STA-FR-017–022 (Payment Allocation) | PDF-§3.1, PDF-§3.3.2 | RFP-§5.3.5.4.26–28; VP-§6.6 |
| STA-FR-023–029 (Interest & Penalties) | PDF-§3.1, PDF-§4.4 | VP-§6.6; RFP-§3.5.5.4.11 |
| STA-FR-030–033 (Adjustment & Set-Off) | PDF-§3.2, PDF-§3.3 | VP-§6.6.2 |
| STA-FR-034–036 (Reconciliation) | PDF-§3.3, PDF-§3.3.2 | VP-§6.6.2 |
| STA-FR-037–040 (TAS) | PDF-§3.4, PDF-Annex 1 | VP-§6.6 |
| STA-FR-041–045 (Revenue & Migration) | PDF-Annex 1 §3.1–3.3 | VP-§6.6.2; RFP-§3.5.5.4.11.2 |
| DM-FR-001–005 (Identification) | PDF-§4.1, PDF-§4.2 | VP-§6.8 |
| DM-FR-006–008 (Risk Scoring) | PDF-§4.3, PDF-§4.5 | VP-§6.8.2; STX-A1 (SAS VIYA) |
| DM-FR-009–014 (Notifications) | PDF-§4.4, PDF-§8.4 | VP-§6.8; RFP-§3.5.5 (debt management) |
| DM-FR-015–020 (Escalation) | PDF-§4.4, PDF-§4.5 | VP-§6.8.2 (items 1–30) |
| DM-FR-021–028 (Instalments) | PDF-§5, PDF-§5.1–5.3 | VP-§6.8; RFP-§3.5.5 |
| DM-FR-029–036 (Enforcement Actions) | PDF-§4.5, PDF-§4.6 | VP-§6.8.2 (items 12–20) |
| DM-FR-037–041 (Collection Planning) | PDF-§4.5, PDF-§8.4.4 | VP-§6.8 |
| DM-FR-042–046 (Write-Off) | PDF-§7, PDF-§7.1 | VP-§6.8 |
| DM-FR-047–049 (Default Assessment) | PDF-§4.4 | VP-§6.7 (case management) |
| DM-FR-050–058 (Case Lifecycle) | PDF-§8, PDF-§8.4 | VP-§6.7, VP-§6.8.2 |
| RPT-FR-001–021 | PDF-§6, PDF-T5 | STX-A1 (ORS); DTP-§4 |
| WF-FR-001–020 | PDF-§8, VP-§6.7 | CMP Evaluation; CMP Justification |
| INT-FR-001–018 | STX-A1 (ORS); DTP-§3 | VP-§6.6–6.8; informix_clickhouse_ingestion_architecture.md |
| NFR-001–031 | Project Brief; CMP Justification | DTP-§5; RFP-§3 (general requirements) |

---

---

# 13. CONTRACTUAL SAFEGUARDS
## 13.1 Vendor Lock-in Prevention
| ID | M/P | Requirement |
|---|---|---|
| CS.001 | M | The vendor SHALL deliver Case Management Platform capabilities using a general-purpose COTS low-code platform with proven market adoption (100+ customers). |
| CS.002 | M | The vendor SHALL provide full data export capability in standard formats (JSON, CSV, XML) at any time upon MTCA request. |
| CS.003 | M | The vendor SHALL provide workflow definitions exportable in BPMN 2.0 XML format. |
| CS.004 | M | The vendor SHALL provide form definitions exportable in JSON or equivalent portable format. |
| CS.005 | M | The vendor SHALL provide business rules exportable in documented format enabling reimplementation. |
| CS.006 | M | MTCA reserves the right to substitute the Case Management Platform with alternative solution if platform fails to meet COTS market adoption criteria. |

---

# 14. GLOSSARY

## 14.1 Domain-Specific Terms

| Term | Definition |
|---|---|
| Assessment | The determination of a taxpayer's tax liability for a specific tax type and period, either by self-assessment (taxpayer-filed return) or by authority assessment (MTCA-determined). |
| Balance Decomposition | Separation of a taxpayer's outstanding balance into its constituent components: Principal Amount (PA), Interest Amount (IA), and Penalties/Charges Amount (PCA). |
| Balance-Forward Accounting | An accounting presentation method where the opening balance for each period includes the carried-forward balance from the previous period, plus all transactions in the current period. |
| C1–C5 Debt Categories | Five-tier debt classification based on amount thresholds: C1 (< €30, auto write-off), C2 (€30–€100, minimal collection), C3 (€100–€1,000, standard collection), C4 (€1,000–€20,000, intensive collection), C5 (> €20,000, strategic enforcement). Thresholds subject to MTCA confirmation. |
| Case | A managed unit of work in the debt management workflow, representing a taxpayer's debt situation requiring action. Cases progress through defined lifecycle states (New → Open → In-Progress → On-Hold → Pending Closure → Closed). |
| Charge Component | One of the three elements of a tax balance: Principal Amount (PA), Interest Amount (IA), or Penalties/Charges Amount (PCA). |
| Collectable Debt | Outstanding tax debt that has not been written off and for which there is reasonable expectation of recovery. Excludes statute-barred debt and amounts under legal dispute. |
| Collection Strategy | A configurable set of rules determining the enforcement approach for a given debt profile, based on category (C1–C5), risk score, debt age, and taxpayer characteristics. |
| Consolidated Balance | The L1 (top-level) view in the TTT hierarchy showing a taxpayer's total financial position across all tax types, aggregating all debit and credit balances. |
| Credit Balance | A positive balance in a taxpayer's account indicating an overpayment or refund entitlement. Credits may be applied against debit balances in other tax types via set-off. |
| Debit Balance | A negative balance (from the administration's perspective) indicating tax owed by the taxpayer. Includes principal amounts, accrued interest, and penalties/charges. |
| Debt Aging | The classification of outstanding debt by the number of days since the payment became overdue. Used for reporting, prioritisation, and KPI calculation. |
| Default Assessment | A tax assessment generated by the system when a taxpayer fails to file a required tax return, typically based on estimated liability using available data and configurable estimation rules. |
| Demand Notice | A formal written communication to a taxpayer notifying them of outstanding tax debt and requiring payment within a specified period. Precedes judicial letter in the escalation sequence. |
| Disbursement | The allocation of collected tax revenue to designated recipients (municipalities, regions) based on the residential principle (taxpayer address) or territorial principle (property location). |
| Double-Entry Posting | The accounting method used in the STA where every financial transaction is recorded as both a debit and a credit entry, maintaining accounting balance integrity. |
| Enforcement Action | A formal collection measure applied to recover outstanding tax debt, ranging from demand notices through bank account freezing, asset seizure, departure prohibition, business closure, and prosecution referral. |
| Escalation | The progression of debt collection activities from lower-intensity measures (reminders) to higher-intensity measures (enforcement actions) based on elapsed time, debtor response, and configurable rules. |
| Fiscal Year | The financial reporting period used by MTCA for revenue accounting and period-end processing. Determines the boundaries for balance roll-over and revenue account closure. |
| FSM | Fiscal Services Model — the operational framework governing MTCA's revenue collection and taxpayer accounting processes. |
| FSS | Final Settlement System — the mechanism for reconciling end-of-year tax liabilities for employed individuals, ensuring that tax paid through payroll withholding matches annual obligations. |
| Instalment Agreement | A formal arrangement between MTCA and a taxpayer allowing payment of outstanding debt in scheduled instalments over a defined period, subject to terms and conditions including interest accrual. |
| Judicial Letter | A formal legal document issued after an unresolved demand notice, constituting a legal claim for outstanding tax debt. Escalation from judicial letter leads to warrant issuance. |
| KPI (Key Performance Indicator) | A quantifiable measure of operational performance. This specification references 45 KPIs from the PDF source document, grouped across five functional domains. |
| Levy | A general term for any amount imposed by law on a taxpayer, including taxes, duties, fees, and charges administered by MTCA. |
| NACE Code | The European statistical classification of economic activities (Nomenclature statistique des Activités économiques dans la Communauté Européenne), used to categorise taxpayers by economic sector. |
| Overdue Debt | A tax liability where the payment due date has passed without full payment being received. The threshold for initiating collection actions is typically calculated from the statutory due date. |
| Payment Allocation | The process of applying a received payment to outstanding tax liabilities according to defined rules (FIFO, proportional, directed, or configurable). |
| Proportional Enforcement | The principle that the severity and cost of enforcement actions should be proportional to the size, age, and risk profile of the outstanding debt. Prevents applying expensive enforcement measures to small debts. |
| Risk Score | A numeric score assigned to a debtor or debt case indicating the likelihood and severity of non-payment, calculated using multi-factor models integrating debt amount, age, taxpayer history, compliance patterns, and SAS VIYA predictive analytics. |
| Set-Off | The application of a credit balance in one tax type against a debit balance in another tax type for the same taxpayer, reducing the net amount owed. Subject to legal authority and configurable rules. |
| Single Taxpayer Account (STA) | The consolidated financial account view for each taxpayer across all tax types, providing a unified balance, transaction history, and statement generation capability. |
| SSC | Social Security Contributions — amounts withheld and remitted to the Social Security Administration, displayed for information within the STA where relevant. |
| Suspense Account | A temporary holding account for payments that cannot be immediately allocated to a specific taxpayer or tax type due to missing or ambiguous reference information. |
| Tax Account Statement (TAS) | A formal document presenting a taxpayer's financial position, including balances, transactions, and payment history for specified periods and tax types. Multiple template variants support different taxpayer types and languages. |
| Tax Clearance Certificate (TCC) | A document certifying whether a taxpayer has any outstanding tax obligations. Positive clearance indicates no overdue liabilities; negative clearance lists outstanding amounts. Used for public procurement, business licensing, and other regulatory purposes. |
| Tax Period | The specific time period to which a tax liability relates (e.g., calendar year for income tax, quarter for VAT). Forms the third level of the TTT hierarchy. |
| TTT Principle | Taxpayer–Tax Type–Tax Period — the three-level hierarchical accounting principle that structures all STA data. Every financial transaction is attributed to a specific taxpayer, for a specific tax type, within a specific tax period. |
| Warrant | A legal instrument issued after an unresolved judicial letter, authorising specific enforcement actions against a debtor's assets. Warrants may be iterative (multiple warrants for escalating actions). |
| Work Queue | A prioritised list of debt management cases assigned to individual officers or teams, generated automatically based on workflow rules, risk scores, SLA deadlines, and workload balancing. |
| Write-Off | The removal of an outstanding debt from active collection, either automatically (for amounts below the C1 threshold) or through an approval workflow (for larger amounts). Written-off debts may be reactivated if subsequent payment is received. |

## 14.2 Acronyms

| Acronym | Expansion |
|---|---|
| ADG | Automated Document Generation |
| API | Application Programming Interface |
| BPMN | Business Process Model and Notation (standard version 2.0) |
| C1–C5 | Debt Categories 1 through 5 (by amount threshold) |
| CBC | Country-by-Country (reporting, OECD framework) |
| CDC | Change Data Capture (data synchronisation method) |
| CIT | Corporate Income Tax |
| CM | Case Management |
| CMP | Case Management Platform |
| CoA | Chart of Accounts |
| COTS | Commercial Off-The-Shelf (software) |
| CRM | Customer Relationship Management |
| CRS | Common Reporting Standard (OECD automatic exchange of financial account information) |
| CSV | Comma-Separated Values (data format) |
| DAC | Directive on Administrative Cooperation (EU) |
| DM | Debt Management |
| DMN | Decision Model and Notation (OMG standard) |
| DMO | Debt Management Officer (operational role) |
| DTP | Digital Transformation Programme |
| DWH | Data Warehouse |
| EDS | Electronic Document System |
| ETL | Extract, Transform, Load (data integration process) |
| FATCA | Foreign Account Tax Compliance Act (US) |
| FIFO | First In, First Out (payment allocation method) |
| FR | Functional Requirement |
| FSM | Fiscal Services Model |
| FSS | Final Settlement System |
| FTE | Full-Time Equivalent (staff capacity measure) |
| IA | Interest Amount (charge component) |
| INT | Integration (requirement prefix) |
| ITCAS | Integrated Tax and Customs Administration System |
| JSON | JavaScript Object Notation (data format) |
| KPI | Key Performance Indicator |
| LP | Legal Person (company, partnership, or other legal entity) |
| MAGNET | Malta Government Network |
| MGR | Manager (role in reporting use cases) |
| MIS | Management Information System |
| MITA | Malta Information Technology Agency |
| MoSCoW | Must/Should/Could/Won't (prioritisation method) |
| MTCA | Malta Tax and Customs Administration |
| NACE | Nomenclature statistique des Activités économiques dans la Communauté Européenne |
| NFR | Non-Functional Requirement |
| NP | Natural Person (individual taxpayer) |
| ORS | Operational Reporting System |
| PA | Principal Amount (charge component) |
| PCA | Penalties/Charges Amount (charge component) |
| PIT | Personal Income Tax |
| RBAC | Role-Based Access Control |
| REST | Representational State Transfer (API architecture) |
| RFP | Request for Proposals |
| RPT | Reporting (requirement and use case prefix) |
| SA | System Administrator (role) |
| SAS | Statistical Analysis System (analytics platform, now SAS VIYA) |
| SDO | Senior Debt Officer (supervisory role) |
| SLA | Service Level Agreement |
| SSC | Social Security Contributions |
| SSO | Single Sign-On |
| STA | Single Taxpayer Account |
| STX | Short-Term Expert (IMF advisory role) |
| TAS | Tax Account Statement |
| TCC | Tax Clearance Certificate |
| TIN | Taxpayer Identification Number |
| TTT | Taxpayer–Tax Type–Tax Period (accounting principle) |
| UAT | User Acceptance Testing |
| UC | Use Case |
| VAT | Value Added Tax |
| VP | Vendor Proposal |
| WF | Workflow (requirement and use case prefix) |
| XML | Extensible Markup Language (data format) |

---

---

# 15. APPENDICES

## Appendix A: Source Document References

The following source documents informed the requirements in this specification. Section references indicate the most relevant parts of each document.

| Ref | Document | Key Sections Used | Description |
|---|---|---|---|
| PDF | MTCA STA/DM KPI Requirements (Menhard, February 2026) | §3 (STA), §4 (Debt Management), §5 (Instalments), §6 (Reporting/KPIs), §7 (Write-Off), §8 (Case Management), Table 5 (KPIs), Annex 1 (Revenue Accounting) | Primary requirements source; 58-page document defining MTCA's operational requirements for taxpayer accounting and debt management. Authored by IMF STX. |
| RFP | ITCAS Request for Proposals | §3.5.5.4.11 (Taxpayer Accounting), §3.5.5.4.26–28 (Payment Allocation), §3.5.5 (Debt Management) | ITCAS procurement document defining long-term system capabilities. Used to align interim requirements with ITCAS target architecture. |
| VP | European Dynamics Vendor Technical Proposal | §6.6 (Ledger/Chart of Accounts), §6.6.2 (Accounting Engine), §6.7 (Case Management), §6.8 (Debt Management), §6.8.2 (30 DM Capabilities) | ITCAS vendor's proposed solution design. Used to verify capability coverage and plan ITCAS migration alignment. |
| DTP | MTCA Digital Transformation Programme | §3 (Technology Architecture), §4 (Reporting Strategy), §5 (NFR Standards) | Strategic programme document establishing the framework for all MTCA digital transformation initiatives. |
| CMP-J | CMP Justification Document | §F.7 (Vendor Lock-in), §5.1 (API Requirements) | Justification for the selected low-code/no-code platform platform selection with specific portability and integration requirements. |
| CMP-E | CMP Evaluation (platform evaluation) | §5.1 (Technical Comparison) | Platform evaluation informing non-functional requirements for the workflow layer. |
| ORS-A | Informix-ClickHouse Ingestion Architecture | Full document | Technical architecture for the ORS data layer underpinning STA data access. |
| STX-A1 | STX Appendix 1: ORS Implementation Plan | §Consumers Layer | ORS implementation details informing integration requirements. |

## Appendix B: Requirement Priority Summary

### B.1 Functional Requirements by Domain and Priority

| Domain | Must | Should | Could | Won't | Total |
|---|---|---|---|---|---|
| STA (STA-FR) | 35 | 11 | 3 | 0 | 49 |
| Reporting — accounting subset (RPT-FR) | 5 | 5 | 1 | 0 | 11 |
| Workflow — accounting subset (WF-FR) | 9 | 2 | 0 | 0 | 11 |
| Integration (INT-FR) | 12 | 5 | 2 | 0 | 19 |
| **Functional Total** | **61** | **23** | **6** | **0** | **90** |

> Counts reflect this specification's STA scope. Debt-management functional requirements (DM-FR) and the DM-only reporting/workflow requirements are catalogued in the Debt Management Requirements Specification.

### B.2 Non-Functional Requirements by Priority

| Category | Must | Should | Could | Total |
|---|---|---|---|---|
| Performance | 3 | 1 | 0 | 4 |
| Scalability | 2 | 1 | 0 | 3 |
| Availability | 2 | 1 | 0 | 3 |
| Security | 4 | 1 | 0 | 5 |
| Usability | 2 | 1 | 0 | 3 |
| Maintainability | 2 | 2 | 0 | 4 |
| Data Quality | 2 | 0 | 0 | 2 |
| Portability | 5 | 2 | 0 | 7 |
| **NFR Total** | **22** | **9** | **0** | **31** |

### B.3 Grand Totals

| Category | Must | Should | Could | Won't | Total |
|---|---|---|---|---|---|
| Functional Requirements | 61 | 23 | 6 | 0 | 90 |
| Non-Functional Requirements | 22 | 9 | 0 | 0 | 31 |
| **Grand Total** | **83** | **32** | **6** | **0** | **121** |

### B.4 Supplementary Artefact Counts

| Artefact Type | Count |
|---|---|
| Use Cases | 17 (13 STA + 1 Reporting [Revenue Reconciliation] + 3 Administration) |
| Business Rules | 44 (STA accounting rules, BR-STA-001–044) |
| Data Entities (STA Conceptual Model) | 11 STA entities (downstream DM entities referenced externally) |
| Screen Specifications | 14 (10 STA + 4 Reporting/Administration) |
| System Interfaces | 19 (INT-FR-001–019) |
| Implementation Phases | Phase 1 (Core STA Foundation) is the primary scope; later phases consume the STA |
| Identified Risks | 6 |

## Appendix C: Open Questions and Assumptions Log

The following open questions were identified during requirements analysis. They are carried forward from the source document analysis (PDF §13) and supplemented with questions that arose during requirements elaboration. Each question requires resolution before the affected requirements can be finalised for implementation.

### C.1 Unspecified Numeric Targets

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-01 | KPI target values in Table 5 are marked "set target" without numeric values. | RPT-FR-003, RPT-FR-006, all KPI-linked requirements | **Open** | MTCA management to define baseline and target values for all 45 KPIs. |
| OQ-02 | Debt category thresholds (€30, €100, €1,000, €20,000, €200,000) appear illustrative. Are these final? | DM-FR-003, BR-DM-001–008 | **Open** | MTCA to confirm whether thresholds are final policy values or placeholders. |
| OQ-03 | "nn thousand" for largest debts requiring immediate action — value not specified. | DM-FR-037, BR-DM-008 | **Open** | MTCA to define the C5 sub-threshold for immediate strategic action. |
| OQ-04 | Statutory period for collection before write-off ("n years") not specified. | DM-FR-042, BR-DM-049 | **Open** | MTCA legal team to confirm statutory limitation period. |

### C.2 Legal Dependencies

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-05 | Automated write-off of <€30 debts "may require specific legislative authority." What is the status? | DM-FR-030, BR-DM-049 | **Open** | Legal team to confirm whether enabling legislation is in place or planned. |
| OQ-06 | Cross-tax-type offsetting "requires comprehensive legal support." Current legal position? | STA-FR-032–033, BR-STA-029–031 | **Open** | Legal team to confirm existing authority for cross-tax-type set-off. |
| OQ-07 | New Law on Tax Procedure (Stage 4, T+26 weeks) — timeline marked with "?" suggesting uncertainty. | Multiple DM requirements dependent on enforcement powers | **Open** | Legislative affairs to provide updated timeline for tax procedure law. |
| OQ-08 | Objection disallowed for self-assessed taxes when no return received — requires confirmation. | DM-FR-047–049 | **Open** | Legal team to confirm against current Maltese legal framework. |

### C.3 Data Availability

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-09 | Most KPI data classified as "Not Yet" available. Which data elements are currently missing from DWH? | RPT-FR-001–021, INT-FR-001–003 | **Partially Addressed** | ORS implementation is progressively covering all nine databases. Gap analysis needed for specific KPI data elements. |
| OQ-10 | DWH population deadline (T+14 weeks) — is this realistic? | INT-FR-001–003; Dependency D-01 | **Open** | MTCA Data Unit / MITA to provide updated ORS readiness timeline. |
| OQ-11 | NACE classification — consistently applied across all taxpayer records? | DM-FR-006–008 (risk scoring uses economic sector) | **Open** | Data quality assessment needed for NACE code completeness. |

### C.4 Architecture and Integration

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-12 | Integration approach between quick-win components and ITCAS not yet agreed. | INT-FR-010–011, NFR-025–031 | **Open** | MTCA/European Dynamics governance discussion needed. Critical for migration planning. |
| OQ-13 | Choice between standard accounting package, CM, and MIS vs. bespoke development not finalised. | All STA-FR, WF-FR requirements | **Superseded** | the selected low-code/no-code platform selected as CMP; ClickHouse selected for ORS. Platform decisions resolved. |
| OQ-14 | CM system alignment between PDF reference and the selected low-code/no-code platform selection. | WF-FR-001–020 | **Resolved** | the selected low-code/no-code platform confirmed via CMP evaluation and justification documents. |
| OQ-15 | Whether to explore commercial accounting module. | STA-FR-012–016 | **Superseded** | Interim solution uses ORS read + the selected platform workflow. Full accounting deferred to ITCAS. |

### C.5 Process Gaps

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-16 | Pre-enforcement activities for authority-assessed taxes (CIT assessments, customs duties)? | DM-FR-009–014 | **Open** | MTCA business team to confirm whether notification workflows differ by assessment type. |
| OQ-17 | Default assessment generation rules — detail beyond caution about exaggeration? | DM-FR-047–049, BR-DM-057–058 | **Open** | MTCA to provide specific estimation rules per tax type. |
| OQ-18 | Multi-currency handling in STA — Euro vs. denominated currency accounts. | STA-FR-045 | **Deferred** | Classified as Could Have; full multi-currency deferred to ITCAS. |
| OQ-19 | Dispute handling within STA — disputed amounts tracked but resolution workflow not described. | STA-FR-004 (disputed component tracking) | **Open** | MTCA to define whether dispute resolution is in STA/DM scope or separate workflow. |
| OQ-32 | Non-tax public-debt set-off (PDF Annex 1 footnote 3: vehicle licences, media licences, traffic fines, alimony) — is whole-of-government offsetting in scope at any horizon? | STA-FR-032 (set-off target model extensibility) | **Open** | MTCA / Ministry to confirm policy intent; determines whether the set-off engine's target-account model must extend beyond tax types. |
| OQ-33 | Property-tax taxable subject/object consistency — policy decision on shifting tax burden from immovable property to TIN-identifiable owners (PDF §3.2). | STA-FR-047 (provisioning of property-tax obligations); CTD data mapping | **Open** | MTCA policy unit to decide; affects CTD source mapping and account linkage for property-related liabilities. |
| OQ-20 | TAS template variants — how many, which languages, which categories? | STA-FR-038–040, BR-STA-006 | **Partially Addressed** | v1.1 fixes the template dimensions (taxpayer type × statement type × reason × language); the count of variants per dimension and final wording remain with MTCA. |

### C.6 Document Completeness

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-21 | Chapter 8 (Conclusive Remarks) marked as "preliminary" and incomplete. | General context | **Acknowledged** | Final version of source document to be provided when available. |
| OQ-22 | Annexes 2–6 contain only placeholder text. | Various (annex-referenced requirements) | **Acknowledged** | Full annex content to be provided. OQ-23 specifically for Annex 6. |
| OQ-23 | Annex 6 (MIS reporting sub system) likely contains detailed specifications. | RPT-FR-001–021 | **Open** | Critical for reporting requirements validation. Full annex needed. |
| OQ-24 | PDF figure labels garbled (corrupted text from image extraction). | Process verification | **Acknowledged** | Original diagrams needed; requirements specification has compensated with textual descriptions. |

### C.7 Scope Boundaries

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-25 | Are customs debts included in STA/DM scope? | STA-FR-001 (scope), DM-FR-001 (scope) | **Resolved** | Customs debt excluded from interim DM workflow scope per Section 2.3.2. STA may display customs balances for information only. |
| OQ-26 | Delineation between STA/DM quick-win and ITCAS accounting module. | All STA-FR, NFR-025–031 | **Partially Addressed** | MoSCoW prioritisation and ITCAS alignment column in each requirement table provide delineation. Full migration plan to be developed during ITCAS Phase 2 planning. |

### C.8 Questions Arising During Requirements Elaboration

| ID | Question | Affected Requirements | Status | Action Required |
|---|---|---|---|---|
| OQ-27 | the selected low-code/no-code platform form builder limitations for complex financial calculations (e.g., compound interest with variable rates). | STA-FR-023–026, NFR-019–022 | **Open** | Platform capability validation needed during Phase 1 design. May require custom plugin development. |
| OQ-28 | ClickHouse materialized view refresh timing for STA balance aggregation — can near-real-time be achieved for all nine databases simultaneously? | STA-FR-007, INT-FR-001–003, NFR-001 | **Open** | Performance testing required during ORS deployment. Phased database onboarding may be needed. |
| OQ-29 | Approval workflow delegation during staff absence — configurable in the selected low-code/no-code platform or requires custom development? | DM-FR-024, WF-FR-008–009 | **Open** | Platform capability validation during configuration phase. |
| OQ-30 | Document generation integration — ADG vs. the selected platform built-in forms vs. third-party template engine. | WF-FR-017–019, DM-FR-040 | **Open** | Architecture decision needed before Phase 1 implementation. |
| OQ-31 | SAS VIYA API availability timeline — risk scoring integration depends on SAS deployment. | DM-FR-006–008, INT-FR-005 | **Open** | SAS deployment team to confirm API availability and data format specifications. |

---