
**MALTA TAX AND CUSTOMS ADMINISTRATION**

────────────────────────────────────────

**Debt Management (DM) Module — Requirements Specification**

Requirements Specification

────────────────────────────────────────

| **Document ID** | MTCA-DM-SPEC-001 |
| --- | --- |
| **Version** | 1.1.0 |
| **Date** | 2026-06-11 |
| **Status** | Draft — Assembled from extraction workflow |
| **Author** | MTCA Digital Transformation Advisory (IMF STX) |
| **Classification** | Internal — MTCA |
| **Project** | MTCA Debt Management Automation |
| **Scope** | DM-only extraction — STA requirements excluded per extraction rules |

**Document History**

| Version | Date | Description |
|---|---|---|
| 1.0.0 | 2026-02-19 | Initial draft — assembled from extraction workflow. |
| 1.1.0 | 2026-06-11 | Cross-boundary alignment with the Taxpayer Accounting (STA) Requirements Specification v1.1 per Gap Review MTCA-STA-GAP-001: enforceable-balance definition (BR-DM-048) consumes the STA-provided enforceable balance; instalment compliance-status publication to the STA (DM-FR-026) and interest-posting boundary note (DM-FR-025); cross-references for third-party-claim posting (DM-FR-032 → BR-STA-044), account-closure write-off hand-off (DM-FR-044 → STA-FR-048), refund-interception execution (DM-FR-045 → STA-FR-033), and suspense triggers (DM-FR-002); STA Accounting Engine added to External Dependencies and Appendix D. No DM requirement renumbered. |

# Table of Contents

	Table of Contents	2

	1. EXECUTIVE SUMMARY	12

	1.1 Purpose	12

	1.2 Scope	12

	1.3 Solution Positioning	12

	1.4 External Dependencies	13

	1.5 Key Stakeholders	13

	1.6 Document Conventions	14

	1.6.1 Requirement Identification Scheme	14

	1.6.2 MoSCoW Priority Levels	15

	1.6.3 Source References	15

	1.6.4 Use Case Identification	16

	2. BUSINESS CONTEXT AND OBJECTIVES	17

	2.1 Problem Statement	17

	2.2 Strategic Objectives	17

	2.2.1 Primary Objective: Transform Tax Debt Management	17

	2.2.2 Primary Objective: Liberate Staff Capacity Through Automation	18

	2.2.3 Supporting Objective: Enable Debt-Related Performance Reporting	18

	2.3 Scope Definition	19

	2.3.1 In Scope	19

	2.3.2 Out of Scope	19

	2.3.3 Future ITCAS Migration	20

	2.4 Assumptions and Constraints	20

	2.4.1 Assumptions	20

	2.4.2 Constraints	21

	2.5 Dependencies	22

	3. STAKEHOLDER ANALYSIS	23

	3.1 Stakeholder Register	23

	3.2 Detailed User Personas	24

	3.2.1 Debt Management Officer	24

	3.2.2 Team Leader / Supervisor	24

	3.2.3 Senior Management / Director	25

	3.2.4 IT Administrator	25

	3.2.5 Taxpayer (Indirect — Receives DM Outputs)	26

	3.2.6 External Auditor (Read-Only)	26

	4. Functional Requirements — Debt Management (DM)	27

	Domain Overview	27

	4.1 Debt Identification and Case Creation	27

	4.2 Risk Scoring and Classification	28

	4.3 Proactive Notifications and Reminders	29

	4.4 Escalation Workflows	30

	4.5 Instalment Agreement Lifecycle	31

	4.6 Enforcement Actions	32

	4.7 Enforcement Action Configuration	34

	4.8 Write-Off	35

	4.9 Collection Planning and Management Information	36

	4.10 Default Assessment Integration	37

	4.11 Debtors List Management	38

	External Dependencies	38

	4. Functional Requirements — Reporting, Workflow & Case Management, Integration	40

	RPT-FR Inclusion/Exclusion Classification	40

	4.12 Reporting and Analytics	41

	4.12.1 Dashboards	41

	4.12.2 Operational Reports	43

	4.12.3 Report Configuration and Management	44

	4.12.4 Analytics	44

	4.13 Workflow and Case Management	45

	4.13.1 Case Lifecycle Management	45

	4.13.2 Work Queue Management	46

	4.13.3 SLA Monitoring	47

	4.13.4 Notifications and Alerts	47

	4.13.5 Document Management	48

	4.13.6 Audit Trail	49

	4.14 Integration Requirements	49

	4.14.1 ORS/ClickHouse Integration	49

	4.14.2 SAS VIYA Integration	50

	4.14.3 Legacy Informix Coexistence	51

	4.14.4 ITCAS Future Migration	52

	4.14.5 External System Integration	52

	4.14.6 Notification Channel Integration	53

	External Dependencies Summary	53

	Section 5: Platform Capability Requirements	55

	5.0 Overview	55

	5.1 Forms and Workflow Configuration (CFW.xxx)	55

	5.1.1 Form and Workflow Definition	55

	5.1.2 Document Handling and Processing	56

	5.1.3 List and Queue Management	57

	5.1.4 Correspondence and Templates	57

	5.1.5 Audit Trail and History	58

	5.1.6 Multi-Language Support	58

	5.2 Workflow Engine (WF.xxx)	58

	5.2.1 Process Modelling	59

	5.2.2 Task and Assignment	59

	5.2.3 Gateways and Flow Control	60

	5.2.4 Timer and Scheduling	60

	5.2.5 Error Handling	60

	5.2.6 Case Management Specifics	61

	5.2.7 Notification and Communication	61

	5.2.8 Collaboration and Versioning	61

	5.3 Business Rules Engine (BR.xxx)	62

	5.4 Forms Management (FM.xxx)	63

	5.5 Data Management (DM.xxx)	63

	5.6 Events and Triggers (EC.xxx)	64

	5.7 Process Monitoring (PM.xxx)	65

	5.8 Platform Integration (INT.xxx)	66

	5.8.1 ITCAS Core Integration	66

	5.8.2 ORS/ClickHouse Integration	66

	5.8.3 SAS VIYA Integration	66

	5.8.4 External System Integration	67

	5.9 Platform Architecture (OA.xxx)	67

	5.10 General Platform Requirements (GN.xxx)	68

	5.11 DM Operational Metrics (PRM.xxx — Subset)	68

	5.11.1 KPI Metrics and Calculation	69

	5.11.2 FTE Liberation Tracking Alignment	69

	5.12 FTE Liberation Cross-Reference Matrix	69

	5.13 External Dependencies	70

	6. Non-Functional Requirements	71

	6.1 Performance	71

	6.2 Availability and Reliability	71

	6.3 Security	72

	6.3.1 Authentication and Access Control	72

	6.3.2 Data Protection and Encryption	73

	6.3.3 Security Standards and Testing	73

	6.4 Usability	74

	6.4.1 Accessibility and Internationalisation	74

	6.4.2 User Experience	74

	6.4.3 Documentation and Help	74

	6.5 Data Quality	75

	6.5.1 Data Accuracy and Validation	75

	6.5.2 Data Integrity and Management	75

	6.5.3 Data Archiving and Retention	75

	6.6 Auditability	76

	6.7 Maintainability and Configurability	76

	6.7.1 Business Rule Configuration	76

	6.7.2 Reporting Capabilities	77

	6.7.3 Environment and Deployment Management	77

	6.7.4 Solution Architecture and Vendor Requirements	77

	6.8 Portability and Standards Compliance	78

	Requirements Summary	78

	7. Use Case Model — Part 1: Debt Management Use Cases UC-DM-01 to UC-DM-10	80

	7.1 Use Case Overview	80

	7.1.1 Actor Definitions	80

	7.1.2 Use Case Summary (UC-DM-01 to UC-DM-10)	80

	7.2 Debt Management Use Cases	81

	UC-DM-01: Identify and Create Debt Case (Automated)	81

	UC-DM-02: Generate Payment Reminder	83

	UC-DM-03: Issue Demand Notice	84

	UC-DM-04: Issue Final/Immediate Demand Notice	86

	UC-DM-05: Classify and Rank Debt Cases by Risk	87

	UC-DM-06: Create Instalment Agreement	88

	UC-DM-07: Review and Approve Instalment Agreement	90

	UC-DM-08: Monitor Instalment Agreement Compliance	92

	UC-DM-09: Cancel Instalment Agreement and Create Recovery Case	93

	UC-DM-10: Escalate Debt Case for Enforcement	94

	Extraction Summary	96

	7. Use Case Model — Part 2: Debt Management Use Cases UC-DM-11 to UC-DM-20 + Reporting, Workflow, Administration	97

	7.3 Debt Management Use Cases (continued)	97

	UC-DM-11: Process Bank Account Freezing Request	97

	UC-DM-12: Process Write-Off	98

	UC-DM-13: Manage Debt Recovery Worklist	100

	UC-DM-14: Generate Debt Collection Plan	101

	UC-DM-15: Appoint Agent (Warrant)	102

	UC-DM-16: Manage Objection to Debt	104

	UC-DM-17: Process Default Assessment (Non-Filer)	105

	UC-DM-18: Close Debt Recovery Case	107

	UC-DM-19: Configure Enforcement Action Types	108

	UC-DM-20: Track and Record Enforcement Activities	110

	7.4 Reporting Use Cases	111

	UC-RPT-01: View Debt Aging Dashboard	111

	UC-RPT-02: Generate Debt Collection Status Report	112

	UC-RPT-03: View KPI Monitoring Dashboard	113

	UC-RPT-05: View Debtors List with Filtering	114

	7.5 Workflow and Case Management Use Cases	115

	UC-WF-01: Configure Debt Management Workflow	115

	UC-WF-02: Manage Work Queue	116

	UC-WF-03: Reassign or Delegate Case	117

	7.6 Administration Use Cases	118

	UC-ADM-01: Configure Business Rules	118

	UC-ADM-02: Configure Notification Templates	119

	UC-ADM-03: Manage User Roles and Permissions	120

	7.7 Use Case Summary and Coverage Matrix	122

	7.7.1 Debt Management Use Cases (UC-DM-01 to UC-DM-20) — Complete	122

	7.7.2 Reporting Use Cases	123

	7.7.3 Workflow and Case Management Use Cases	123

	7.7.4 Administration Use Cases	124

	7.7.5 Requirements Not Covered by Specific Use Cases	124

	7.7.6 DM Functional Requirements Coverage Confirmation	125

	Extraction Summary	125

	8. BUSINESS RULES CATALOGUE	127

	8.1 Debt Management Rules (BR-DM-001 to BR-DM-060)	127

	8.1.1 Debt Detection and Case Creation Rules	127

	8.1.2 Reminder and Demand Rules	128

	8.1.3 Risk Scoring and Classification Rules	130

	8.1.4 Instalment Agreement Rules	130

	8.1.5 Instalment Compliance and Cancellation Rules	132

	8.1.6 Enforcement Escalation Rules	134

	8.1.7 Bank Garnishing Rules	135

	8.1.8 Write-Off Rules	135

	8.1.9 Worklist and SLA Rules	136

	8.1.10 Collection Planning Rules	137

	8.1.11 Agent (Warrant) Rules	137

	8.1.12 Objection Management Rules	138

	8.1.13 Default Assessment Rules	139

	8.1.14 Case Closure and Archival Rules	140

	8.1.15 Enforcement Configuration and Activity Rules	140

	8.2 Reporting Rules (BR-RPT-001 to BR-RPT-010)	141

	8.3 Workflow Rules (BR-WF-001 to BR-WF-007)	144

	8.4 Administration Rules (BR-ADM-001 to BR-ADM-007)	146

	8.5 Business Rules Summary	147

	8.6 External Dependencies — STA Business Rules	148

	9. DATA REQUIREMENTS	150

	9.1 Conceptual Data Model	150

	9.1.1 Internal DM Entities	150

	9.1.2 External Data Entities (Accessed via ORS/ClickHouse)	155

	9.1.3 Key Relationships (DM-Centric View)	155

	9.2 ORS Data Access Patterns	156

	9.2.1 ClickHouse Data Sources Consumed by DM	157

	9.2.2 Query Patterns by Data Freshness Tier	157

	9.2.3 Silver/Gold Layer Views Consumed by DM	158

	9.2.4 Data Access Interface	158

	9.3 Data Quality Requirements	158

	9.3.1 Validation Rules	159

	9.3.2 Reconciliation Rules	160

	9.3.3 Completeness Thresholds	160

	9.3.4 Data Freshness SLAs (DM-Relevant)	160

	9.4 Write-Back Requirements	161

	9.4.1 Data Written to Case Management Platform Database	161

	9.4.2 Future Write-Back to Transactional Systems	161

	Cross-References	162

	External Dependencies	162

	10. INTERFACE REQUIREMENTS	164

	10.1 User Interface — Design Principles	164

	10.2 User Interface — DM Screens	164

	10.2.1 Screen Inventory	164

	10.2.2 Key Screen Descriptions	165

	10.3 User Interface — DM Reporting Screens	166

	10.3.1 Screen Inventory	166

	10.3.2 Key Reporting Screen Descriptions	166

	10.3.3 Workflow and Administration Screens	166

	10.3.4 Total Screen Summary	167

	10.4 System Interfaces	167

	10.4.1 ORS / ClickHouse REST API	167

	10.4.2 SAS VIYA Scoring API	168

	10.4.3 Notification Gateway	168

	10.4.4 Document Generation (ADG)	169

	10.4.5 External System Interfaces	169

	10.5 Future ITCAS Integration Points	170

	Cross-References	170

	External Dependencies	171

	Section 11: Implementation Considerations	172

	11.1 Prerequisites	172

	11.1.1 ORS / ClickHouse Readiness	172

	11.1.2 Platform Provisioning	172

	11.1.3 Phase 1 Foundation (Prerequisite Dependency)	172

	11.2 Phasing Strategy	173

	11.2.1 Phase 2: Debt Management Workflows (Q3–Q4 2026 — July to December)	173

	11.2.2 Phase 3: Advanced DM and External Integration (Q1 2027 — January to March)	174

	11.2.3 Phase 4: Full Reporting and ITCAS Preparation (Q2 2027 — April to June)	175

	11.2.4 Phase Summary	175

	11.3 ITCAS Migration Strategy	175

	11.3.1 What Migrates to ITCAS	176

	11.3.2 What Stays / Retires	176

	11.3.3 BPMN Portability	176

	11.3.4 Data Migration	177

	11.3.5 Timeline Alignment with ITCAS	177

	11.4 Risks and Mitigations	177

	11.4.1 Risk Summary Matrix	177

	11.4.2 R1: Data Quality Propagation (Critical)	178

	11.4.3 R2: User Adoption Resistance (High)	178

	11.4.4 R3: Integration Complexity (Medium)	179

	11.4.5 R4: Platform Limitations (Low)	179

	11.4.6 R5: ORS Readiness Timing (High)	179

	11.4.7 R6: ITCAS Coordination (Medium–High)	180

	11.5 FTE Liberation Targets and Tracking	180

	11.5.1 Aggregate FTE Target	180

	11.5.2 FTE Liberation Breakdown by Automation Area	180

	11.5.3 Quarterly FTE Liberation Checkpoints	181

	11.5.4 Measurement Methodology	181

	11.5.5 Arrears Management KPIs	181

	11.5.6 Reallocation Strategy	182

	11.6 External Dependencies Summary	182

	12. Traceability Matrix	184

	12.1 DM Requirements → Use Cases	184

	12.1.1 Debt Management Functional Requirements	184

	12.1.2 Reporting, Workflow, Integration → Use Cases	186

	12.1.3 NFR Coverage Summary	187

	12.2 DM Requirements → Business Rules	188

	12.2.1 DM Functional Requirements → Business Rules	188

	12.2.2 RPT/WF/ADM Requirements → Business Rules	188

	12.3 DM Requirements → KPIs	189

	12.3.1 Objective 3: Transform Tax Debt Management (KPI Items 24–35)	189

	12.4 DM Requirements → CMP Platform Requirements	189

	12.5 DM Requirements → Source Documents	190

	Source Document Key	191

	12.6 Requirements → ITCAS Future Capability Mapping (DM Scope)	191

	13. Contractual Safeguards	193

	13.1 Vendor Lock-in Prevention	193

	13.2 Data Portability	193

	13.3 Platform Substitution Rights	194

	13.4 BPMN Portability Guarantee	194

	13.5 Day 1 Platform Availability Requirements	195

	13.5.1 Day 1 Requirements	195

	13.5.2 Day 1 Acceptance Test Scenarios	196

	13.5.3 Dependencies on Day 1 Requirements	196

	13.6 Safeguard Summary Matrix	197

	14. Glossary	198

	14.1 Domain-Specific Terms	198

	14.2 Acronyms	199

	15. Appendices	202

	Appendix A: Requirement Priority Summary (DM Scope)	202

	A.1 Functional Requirements by Domain and Priority	202

	A.2 Non-Functional Requirements by Priority	202

	A.3 CMP Platform Capability Requirements by Priority	202

	A.4 Contractual and Day 1 Requirements	203

	A.5 Grand Totals (DM Specification)	203

	A.6 Supplementary Artefact Counts (DM Scope)	203

	Appendix B: Open Questions and Assumptions (DM-Relevant)	204

	B.1 Unspecified Numeric Targets	204

	B.2 Legal Dependencies	204

	B.3 Data Availability	205

	B.4 Architecture and Integration	205

	B.5 Process Gaps	205

	B.6 Document Completeness	206

	B.7 DM-Specific Questions Arising During Requirements Elaboration	206

	B.8 Assumptions	207

	Appendix C: CMP Platform Requirements Cross-Reference	207

	C.1 Case Framework (CFW) — 45 Requirements	207

	C.2 Workflow (WF) — 50 Requirements	208

	C.3 Business Rules (BR) — 15 Requirements	208

	C.4 Forms Management (FM) — 15 Requirements	208

	C.5 Data Management (DM) — 12 Requirements	209

	C.6 External Connectors (EC) — 10 Requirements	209

	C.7 Process Monitoring (PM) — 10 Requirements	209

	C.8 Integration (INT) — 20 Requirements	209

	C.9 Operational Analytics (OA) — 10 Requirements	209

	C.10 General (GN) — 10 Requirements	210

	C.11 Performance Metrics (PRM subset) — 9 Requirements	210

	Appendix D: Source Document References	210

# 1. EXECUTIVE SUMMARY

## 1.1 Purpose

This document provides a comprehensive Requirements Specification and Use Case Model for the Debt Management (DM) solution that the Malta Tax and Customs Administration (MTCA) intends to implement as a quick-win initiative ahead of the Integrated Tax and Customs Administration System (ITCAS). The specification covers functional requirements, non-functional requirements, detailed use cases, business rules, data models, interface definitions, and implementation considerations for a case management solution that will deliver immediate operational value in tax debt recovery while remaining architecturally aligned with the long-term ITCAS vision.

The document serves as the authoritative reference for solution design, development, testing, and acceptance of the DM system. It is intended for use by MTCA business stakeholders, the MTCA IT unit, the platform implementation team, and IMF technical advisors throughout the solution lifecycle.

## 1.2 Scope

The DM solution addresses workflow-driven processes for managing tax arrears from initial detection through pre-enforcement, enforced collection, instalment agreements, and write-off. The DM component implements risk-based, proportional enforcement across five debt categories (C1–C5), automated escalation logic, and case management capabilities.

The solution relies on taxpayer financial data sourced from the Operational Reporting System (ORS), which provides consolidated taxpayer account views built on ClickHouse. The ORS ingests data from MTCA's nine legacy Informix databases and delivers the Single Taxpayer Account (STA) views that the DM solution reads — including consolidated balances, tax-type breakdowns, and transaction-level detail. The DM solution does not implement taxpayer accounting logic; it consumes these views as external data inputs.

The solution also encompasses performance management reporting based on 12 Arrears Management KPIs (PDF-T5 Items 24–35) and additional operational KPIs relevant to debt collection, management dashboards, and FTE liberation targets.

## 1.3 Solution Positioning

This specification defines an **interim solution** that operates during the period before ITCAS debt management modules become operational. The interim solution is positioned as follows:

**Data Layer:** The DM solution reads consolidated taxpayer data from the Operational Reporting System (ORS), which is built on ClickHouse and ingests data from all nine legacy Informix databases. The ORS provides the analytical data layer, including Single Taxpayer Account views (consolidated balances, tax-type breakdowns, transaction histories). Legacy systems remain the transactional systems of record for tax processing.

**Workflow Layer:** Debt management workflows, case management, instalment agreement processing, and automated escalation are implemented on the selected Case Management Platform (CMP) — an open-source or commercially licensed low-code platform selected for its BPMN 2.0 compliance, citizen developer enablement, and low vendor lock-in risk. All workflow definitions are exportable in standard BPMN 2.0 format to facilitate future migration.

**Analytics Layer:** SAS VIYA provides advanced analytics, risk scoring, and AI-driven compliance capabilities that feed into debt management prioritisation and taxpayer risk profiling.

**ITCAS Migration Path:** The interim solution is explicitly designed for eventual replacement by the ITCAS debt management and case management modules (Phase 2, estimated T0+36 to T0+126 weeks). Architectural decisions throughout this specification prioritise BPMN portability, standard data formats, and API-first integration to minimise migration effort. Requirements that exceed the interim solution's scope are classified as "Won't Have (deferred to ITCAS)" and documented for traceability.

## 1.4 External Dependencies

The DM solution depends on the following external systems and data sources that are specified and managed outside this document:

| **Dependency** | **Provider** | **What DM Consumes** | **Reference** |  |
| --- | --- | --- | --- | --- |
| **ORS / ClickHouse** | MTCA Data Unit / MITA | Consolidated taxpayer balances (L1, L2, L3); payment histories; registration data; aged debt views | ORS Implementation Plan (STX-A1) |  |
| **STA Views in ORS** | ORS analytical layer | Single Taxpayer Account views — the DM solution reads taxpayer financial position from these views but does not compute balances | STA view definitions in ORS data model |  |
| **SAS VIYA** | MTCA IT / SAS | Risk scores for debt prioritisation; taxpayer compliance profiles; predictive analytics for collection strategy | SAS Strategic Alignment (STX-A7) |  |
| **Legacy Informix Databases** | MTCA (9 databases) | Source data for ORS ingestion — DM does not read from Informix directly; all legacy data is consumed via ORS | Data Catalogue (Apache Atlas) |  |
| **ITCAS (European Dynamics)** | European Dynamics | Future migration target — BPMN 2.0 workflow definitions and data will transfer to ITCAS modules | ITCAS Contract / RFP |  |
| **MITA Infrastructure** | MITA | ClickHouse cluster hosting; network connectivity (MAGNET); deployment environment for the Case Management Platform | MITA SLA |  |

## 1.5 Key Stakeholders

| **Stakeholder** | **Primary Interest** |  |
| --- | --- | --- |
| Commissioner for Tax and Customs | Strategic oversight; performance visibility; compliance improvement |  |
| Director, Taxation Services | Operational debt recovery; FTE productivity; enforcement effectiveness |  |
| Director, IT / CIO | Technical architecture; integration with ITCAS; platform sustainability |  |
| Debt Management Unit | Daily workflow; case management; enforcement tools; instalment processing |  |
| MITA (Malta IT Agency) | Infrastructure provisioning; ClickHouse hosting; network connectivity |  |
| European Dynamics (ITCAS Vendor) | Alignment with ITCAS case management design; data migration compatibility |  |
| IMF Technical Advisory Team | Compliance with international best practice; KPI framework alignment |  |
| Ministry for Finance / Treasury | Revenue reconciliation; refund forecasting; fiscal reporting |  |

## 1.6 Document Conventions

### 1.6.1 Requirement Identification Scheme

All requirements in this document carry a unique identifier following the pattern:

| **Prefix** | **Domain** | **Example** |  |
| --- | --- | --- | --- |
| DM-FR-*nnn* | Debt Management — Functional Requirements | DM-FR-001 |  |
| RPT-FR-*nnn* | Reporting — Functional Requirements (DM-relevant) | RPT-FR-001 |  |
| WF-FR-*nnn* | Workflow & Integration — Functional Requirements | WF-FR-001 |  |
| ADM-FR-*nnn* | Administration — Functional Requirements | ADM-FR-001 |  |
| NFR-*nnn* | Non-Functional Requirements | NFR-001 |  |
| BR-DM-*nnn* | Business Rules — Debt Management | BR-DM-001 |  |
| BR-RPT-*nnn* | Business Rules — Reporting | BR-RPT-001 |  |
| BR-WF-*nnn* | Business Rules — Workflow | BR-WF-001 |  |
| BR-ADM-*nnn* | Business Rules — Administration | BR-ADM-001 |  |

Additionally, requirements originating from the Case Management Platform (CMP) specification carry these prefixes:

| **Prefix** | **Domain** | **Example** |  |
| --- | --- | --- | --- |
| CFW-*nnn* | Core Framework Capabilities | CFW-001 |  |
| WF-*nnn* | Workflow Engine Requirements | WF-001 |  |
| FM-*nnn* | Form Management | FM-001 |  |
| EC-*nnn* | External Communications | EC-001 |  |
| PM-*nnn* | Platform Management | PM-001 |  |
| INT-*nnn* | Integration Capabilities | INT-001 |  |
| CS-*nnn* | Contractual Safeguards | CS-001 |  |
| D1-*nnn* | Day 1 Requirements | D1-001 |  |
| UC.DM-*nnn* | CMP Use Cases — Debt Management | UC.DM-001 |  |

### 1.6.2 MoSCoW Priority Levels

Each requirement is assigned a MoSCoW priority that reflects its criticality within the interim solution context:

| **Priority** | **Definition** | **Interim Context** |  |
| --- | --- | --- | --- |
| **Must Have** | Essential for minimum viable solution; system cannot launch without it | Core debt management workflow, case creation and assignment, priority reports |  |
| **Should Have** | Important functionality expected in the initial release but solution is usable without it | Advanced escalation automation, secondary reports, instalment template variants |  |
| **Could Have** | Desirable enhancements that improve the solution if time and resources permit | Predictive analytics integration, mobile dashboards, bulk processing optimisations |  |
| **Won't Have** | Explicitly out of scope for the interim solution; deferred to ITCAS implementation | Full taxpayer portal self-service, multi-currency accounting, customs debt workflows |  |

### 1.6.3 Source References

Requirements are traced to their originating source using the following abbreviations:

| **Abbreviation** | **Source Document** |  |
| --- | --- | --- |
| PDF-§*n.n* | MTCA STA/DM KPI Requirements (Menhard, Feb 2026) — chapter reference |  |
| PDF-T*n* | MTCA STA/DM KPI Requirements — table reference |  |
| RFP-§*n.n.n.n.n* | ITCAS RFP — section reference |  |
| VP-§*n.n* | Vendor Technical Proposal (European Dynamics) — section reference |  |
| DTP-§*n* | MTCA Digital Transformation Programme — section reference |  |
| STX-A*n* | STX Appendix — initiative reference (e.g., A1 = ORS, A2 = PowerBuilder Migration) |  |
| CMP-§*n* | Case Management Platform Requirements — section reference |  |

### 1.6.4 Use Case Identification

Use cases follow the pattern UC-*DOM*-*nnn* where DOM is the functional domain (DM, RPT, WF, ADM). CMP-originated use cases follow the pattern UC.*DOM*.*nnn*.

# 2. BUSINESS CONTEXT AND OBJECTIVES

## 2.1 Problem Statement

MTCA administers Malta's complete tax and customs operations with approximately 700 staff, managing revenue collection across corporate income tax (CIT), personal income tax (PIT), value-added tax (VAT), customs and excise duties, stamp duty, and other tax types. The administration's current operational environment presents several critical challenges that directly impair the effectiveness of debt management operations.

**Fragmented Legacy Systems.** MTCA's tax operations are supported by over 70 PowerBuilder applications built over two decades, running against nine Informix databases containing more than 5,170 tables. These systems were developed incrementally and are not integrated: there is no unified taxpayer view, no consolidated account balance, and no way to determine a taxpayer's total debt position across tax types without manual cross-referencing of multiple systems. This fragmentation directly undermines effective debt management, as officers cannot assess a debtor's complete financial exposure from a single interface.

**No Consolidated Debtor View.** The absence of consolidated taxpayer financial data means that debt management officers must consult multiple legacy systems to understand a taxpayer's total liabilities. Cross-tax-type debt aggregation is not possible without manual intervention. This gap is being addressed by the Operational Reporting System (ORS), built on ClickHouse, which provides consolidated Single Taxpayer Account (STA) views that the DM solution will consume as an external data source.

**Manual Debt Management.** Tax debt collection is largely manual and reactive. There are an estimated 5,000 overdue cases at any given time, yet there is no systematic risk-based prioritisation, no automated escalation, and no workflow-driven case management. Enforcement actions are applied inconsistently, and there is no proportional framework linking debt size to appropriate collection measures. The critical 60-day window for effective debt recovery is frequently missed.

**Absence of Performance Reporting.** MTCA does not currently report operational performance measures in its annual report. Of the 12 identified Arrears Management KPIs (PDF-T5 Items 24–35), the majority are classified as "Not Yet" available, meaning the underlying data either does not exist in accessible form or has never been systematically collected. Without measurable performance data, management cannot assess the effectiveness of debt recovery strategies or benchmark against international standards.

**Declining Technology Platform.** PowerBuilder's ecosystem is in structural decline, with a shrinking developer community and limited vendor investment. This creates increasing maintenance risk and makes it progressively more difficult to attract and retain technical staff, further constraining the capacity available for debt management system improvements.

## 2.2 Strategic Objectives

The DM solution directly supports the MTCA Digital Transformation Programme (DTP) 2026–2030, addressing the following strategic objectives with associated KPI targets.

### 2.2.1 Primary Objective: Transform Tax Debt Management

Implement systematic, risk-based, proportional debt management with automated workflows, reducing the stock of overdue debt and improving recovery rates within the critical 60-day window.

**KPI Targets — Arrears Management (PDF-T5 Items 24–35):**

| **#** | **KPI** | **PDF-T5 Item** | **Data Availability** | **Priority** |  |
| --- | --- | --- | --- | --- | --- |
| 1 | Annual new tax debt as percentage of total revenue collections | Item 24 | Not Yet | High |  |
| 2 | Total collectable debt as percentage of total revenue collections | Item 25 | Not Yet | High |  |
| 3 | Percentage of tax debt under instalment arrangement | Item 26 | Not Yet | High |  |
| 4 | Percentage of debt under 6 months of age | Item 27 | Not Yet | High |  |
| 5 | Percentage of collectable debt under 2 years of age | Item 28 | Not Yet | High |  |
| 6 | Percentage of taxpayers complying with agreed instalment arrangements | Item 29 | Not Yet | High |  |
| 7 | Value of demand notes issued | Item 31 | Not Yet | High |  |
| 8 | Value of instalment agreements made | Item 32 | Not Yet | High |  |
| 9 | Revenue collected from debt recovery actions | Item 33 | Not Yet | High |  |
| 10 | Percentage of instalment non-compliance requiring garnishment or stronger measures | Item 34 | Not Yet | High |  |
| 11 | Percentage of new debt resolved within six months | Item 35 | Not Yet | High |  |
| 12 | Number of demand notes and agreements as volume metric | Item 30 | Not Yet | High |  |

All 12 KPIs are classified as High priority and represent the core performance measurement framework for the DM solution.

### 2.2.2 Primary Objective: Liberate Staff Capacity Through Automation

Deliver measurable FTE savings through automation of manual debt management processes, targeting **8–10 FTE liberation from debt management alone by Q3 2026**, contributing to the DTP's overall target of approximately 50 FTE equivalent savings in Year 1.

FTE liberation from DM automation is expected from:

- Automated demand notice generation and dispatch (replacing manual letter creation)

- Automated case creation and work queue prioritisation (replacing manual triage)

- Instalment compliance monitoring with automated default detection (replacing manual tracking)

- Automated escalation triggers based on business rules (replacing manual case review)

- Automated KPI computation and report generation (replacing manual spreadsheet compilation)

### 2.2.3 Supporting Objective: Enable Debt-Related Performance Reporting

The DM solution provides reporting infrastructure that enables measurement and monitoring of debt recovery effectiveness, supporting management decision-making. The reporting scope encompasses:

- All 12 Arrears Management KPIs (Section 2.2.1 above)

- Debt aging analysis (by category, tax type, sector, age band)

- Case workload distribution and officer performance metrics

- Enforcement action effectiveness tracking

- Instalment agreement compliance rates

- Management dashboards at three levels (strategic, departmental, operational)

Additionally, the DM solution supports reporting on related functional areas where debt management intersects with broader compliance activities:

- **Field inspections and audit** (PDF-T5 Items 36–43): Revenue collected from compliance activities, number of inspections conducted, and assessment outcomes — reported as context for debt pipeline analysis.

- **Appeals and litigation** (PDF-T5 Items 44–45): Percentage of appeals and prosecutions decided in favour of the Commissioner — relevant for informing enforcement strategy calibration.

These related KPIs are included in the reporting scope but are not primary DM functional requirements; they draw on data sourced from ORS views of inspection, audit, and legal proceedings records.

## 2.3 Scope Definition

### 2.3.1 In Scope

The following capabilities are within the scope of this specification:

**Debt Management (DM) — Core:** Pre-enforcement workflow (notifications, reminders, default assessments); enforced collection workflow across five debt categories (C1–C5); instalment agreement creation, approval, monitoring, and default handling; proportional enforcement escalation; case creation, assignment, and lifecycle management; demand notice generation; write-off processing.

**Reporting — DM-Relevant:** Operational reports (arrears recovery, enforcement activity, instalment compliance); KPI dashboards for arrears management domain; aged debt analysis; case workload reporting; management dashboards at three levels (multi-stream, entity-targeted, single-stream).

**Workflow and Case Management:** BPMN 2.0 workflow definitions for all DM processes; integration with ORS/ClickHouse via REST APIs for reading taxpayer data; integration with SAS VIYA for risk scoring; automated document generation for demand notices and instalment correspondence; configurable business rules engine.

**Administration:** User role and permission management; audit trail logging; report configuration and saving; system parameter configuration (thresholds, rates, templates).

### 2.3.2 Out of Scope

The following capabilities are explicitly excluded from the DM solution:

**Taxpayer Accounting and Balance Computation:** The DM solution does not compute taxpayer balances, allocate payments, calculate interest or penalties, or maintain the Single Taxpayer Account. These functions are provided by the ORS data layer (consolidated views from legacy systems) and will eventually be replaced by the ITCAS accounting module. The DM solution reads taxpayer financial data from ORS as an external data source.

**Tax Return Processing and Assessment:** Legacy Informix/PowerBuilder systems remain the systems of record for return filing, assessment processing, and payment receipt recording. The DM solution reads the outcomes of these processes via ORS but does not perform transactional tax processing.

**Taxpayer Portal Self-Service (full):** While the specification captures requirements for future taxpayer self-service (instalment applications, account viewing), these are classified as "Won't Have" for the interim solution. Limited interaction may be considered for a later phase.

**Full Multi-Currency Accounting:** The ITCAS vendor proposal addresses multi-currency requirements. The interim DM solution operates in Euro only.

**Customs Debt Management:** While customs operations share the same ORS data layer, customs debt management follows distinct EU-mandated procedures and is managed through separate channels. Customs debt is excluded from the DM workflow scope.

**Automated Payment Receipt and Bank Integration:** Real-time bank payment processing, direct debit management, and payment gateway integration are ITCAS responsibilities. The DM solution reflects payments already recorded in legacy systems as surfaced through ORS.

**Tax Account Statement (TAS) Generation:** TAS generation is a taxpayer accounting function served by the STA views in ORS. While DM users may access TAS data for context, the TAS generation capability itself is outside the DM solution scope.

**Revenue Reconciliation and Fiscal Reporting:** Revenue reconciliation between MTCA accounts and Treasury records is a taxpayer accounting function. The DM solution reports on debt recovery revenue but does not perform fiscal reconciliation.

### 2.3.3 Future ITCAS Migration

The ITCAS contract (European Dynamics, €68M) follows a phased implementation plan with T0 targeted for March 2026. The debt management and case management modules fall within Phase 2 (T0+36 to T0+126 weeks, approximately Q4 2026 to Q1 2029). When ITCAS Phase 2 achieves operational status, the interim DM solution will be progressively decommissioned:

- DM data source will switch from ORS/ClickHouse views to the ITCAS accounting component

- DM workflow definitions (BPMN 2.0) will be migrated to the ITCAS case management and BPM engine

- Report definitions and KPI configurations will be transferred to the ITCAS MIS module

- The interim Case Management Platform will be retained for non-DM case management use cases or decommissioned

This migration path is documented in the Implementation Considerations section and is reflected in the MoSCoW prioritisation of individual requirements.

## 2.4 Assumptions and Constraints

### 2.4.1 Assumptions

| **ID** | **Assumption** | **Impact if Invalid** |  |
| --- | --- | --- | --- |
| A-01 | The ORS (ClickHouse) will be operational and populated with data from all nine Informix databases by Q2 2026, including consolidated STA views | DM cannot access taxpayer financial data; solution launch is delayed |  |
| A-02 | The selected Case Management Platform licensing will be procured and deployment environment provisioned by Q2 2026 | DM workflows cannot be implemented; alternative platform required |  |
| A-03 | MTCA will allocate dedicated business staff (minimum 2–3 subject matter experts from the Debt Management Unit) for requirements validation, UAT, and ongoing configuration | Requirements gaps; poor adoption; inaccurate business rules |  |
| A-04 | Legacy Informix systems will continue operating unchanged during the interim period, providing stable data feeds to ORS | Data inconsistencies; taxpayer data consumed by DM is inaccurate |  |
| A-05 | Debt category thresholds (€30, €100, €1,000, €20,000, €200,000) and KPI numeric targets will be confirmed by MTCA management before DM workflow configuration | Default values used; potential misalignment with policy intent |  |
| A-06 | Legal authority for automated write-off of debts below the C1 threshold will be established or confirmed | Manual workarounds required; automated write-off feature disabled |  |
| A-07 | All workflow definitions will be created in BPMN 2.0 standard format to ensure portability to ITCAS | Vendor-specific formats create migration risk |  |
| A-08 | SAS VIYA will be available for risk scoring integration via standard APIs | Risk-based debt prioritisation relies on manual assessment |  |

### 2.4.2 Constraints

| **ID** | **Constraint** | **Implication** |  |
| --- | --- | --- | --- |
| C-01 | **Read-only data access** — The DM solution reads taxpayer financial data from ORS/ClickHouse and does not write to legacy Informix databases | All transactional updates (payments, assessments) originate in legacy systems; DM reflects data with ORS refresh latency |  |
| C-02 | **ORS refresh frequency** — Data freshness depends on ETL pipeline scheduling (target: near-real-time via CDC, minimum daily batch) | Taxpayer balances in DM views may not reflect same-day transactions during initial deployment |  |
| C-03 | **BPMN 2.0 portability** — All workflow definitions must be exportable in standard BPMN 2.0 format | Constrains use of platform-specific extensions; may limit certain advanced automation features |  |
| C-04 | **MTCA IT capacity** — MTCA's IT unit has 6–12 operational staff; scaling to support both DM solution and ITCAS vendor engagement requires careful resource planning | Limits complexity of concurrent development; favours low-code/no-code approaches |  |
| C-05 | **MITA infrastructure dependency** — ClickHouse cluster hosting and network connectivity (MAGNET) are provided by MITA | Deployment timeline subject to MITA provisioning schedules and change management procedures |  |
| C-06 | **Budget envelope** — The DM solution must operate within the DTP quick-win budget allocation, separate from the €68M ITCAS contract | Favours open-source components and phased delivery |  |

## 2.5 Dependencies

| **ID** | **Dependency** | **Provider** | **Target Date** | **Impact on DM** |  |
| --- | --- | --- | --- | --- | --- |
| D-01 | ORS ClickHouse cluster operational with production data | MITA / MTCA Data Unit | Q2 2026 | Prerequisite for DM access to taxpayer financial data |  |
| D-02 | ORS ETL pipelines covering all nine Informix databases | MTCA Data Unit | Q2 2026 | Determines breadth of tax types visible to DM for debt identification |  |
| D-03 | STA views in ORS providing consolidated taxpayer balances | MTCA Data Unit | Q2 2026 | DM reads debtor financial position from these views |  |
| D-04 | Case Management Platform deployment environment provisioned | MITA | Q2 2026 | Prerequisite for DM workflow implementation |  |
| D-05 | Case Management Platform licence procured | MTCA Procurement | Q1 2026 | Blocks workflow development if delayed |  |
| D-06 | SAS VIYA API access for risk scoring | MTCA IT / SAS | Q3 2026 | Risk-based prioritisation unavailable without this |  |
| D-07 | Data Catalogue (Apache Atlas) covering DM-relevant tables | MTCA Data Unit | Q1 2026 | Required for accurate data mapping; catalogue targeted for March 2026 completion |  |
| D-08 | ITCAS contract signature (T0) | MTCA / European Dynamics | March 2026 | Determines ITCAS timeline alignment for migration planning |  |
| D-09 | MTCA management confirmation of KPI targets and debt thresholds | Commissioner / Directors | Q2 2026 | Required for report configuration and workflow rule parameterisation |  |
| D-10 | Legal framework review for automated write-off authority | MTCA Legal / Ministry | Q2–Q3 2026 | Determines which automated DM features can be activated |  |

# 3. STAKEHOLDER ANALYSIS

## 3.1 Stakeholder Register

| **#** | **Stakeholder** | **Role/Function** | **Interest in DM Solution** | **Influence** | **Key Expectations** |  |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | Commissioner for Tax and Customs | DTP Programme Sponsor; strategic oversight | High — needs consolidated performance visibility to report to Government | Very High | Real-time KPI dashboards; demonstrable debt recovery improvement; FTE savings evidence |  |
| 2 | Director, Taxation Services | Operational leadership for all tax administration | High — accountable for debt recovery and enforcement effectiveness | High | Actionable debt management tools; reduced manual workload; measurable collection improvement |  |
| 3 | Director, IT / CIO | Technical architecture ownership; ITCAS coordination | High — responsible for integration, platform sustainability, migration path | High | Clean architecture; ITCAS alignment; manageable maintenance burden; BPMN portability |  |
| 4 | Debt Management Unit (Team Leaders + Officers) | Primary users of DM workflows | Very High — daily operational dependency | Medium | Intuitive case management; automated escalation; clear work queues; reduced manual steps |  |
| 5 | MITA | Infrastructure provider; ClickHouse hosting | Medium — responsible for provisioning and SLA | High | Clear infrastructure requirements; standard deployment patterns; manageable SLA commitments |  |
| 6 | European Dynamics (ITCAS Vendor) | Future system provider; migration target | Medium — interim solution must not conflict with ITCAS design | Medium | Standard data formats; documented APIs; clean migration path; no architectural conflicts |  |
| 7 | Ministry for Finance / Treasury | Revenue reconciliation; fiscal reporting | Medium — needs accurate debt recovery revenue data | Low | Reliable revenue reports; timely reconciliation data |  |
| 8 | Taxpayers and Tax Practitioners | Recipients of demand notices and instalment correspondence | Medium — affected by accuracy and timeliness of debt management actions | Low | Accurate demand notices; fair instalment options; transparent enforcement |  |
| 9 | IMF Technical Advisory Team | Quality assurance; international best practice alignment | Medium — advisory role on design and KPI framework | Low | Compliance with international standards; measurable outcomes; transferable methodology |  |
| 10 | External Auditors (NAO) | Oversight and accountability | Low — periodic review of debt management practices | Low | Audit trail availability; transparent write-off procedures; compliant record-keeping |  |

**Note on excluded stakeholders:** The Taxpayer Accounting Section is not a primary stakeholder for the DM solution. Their core functions (balance enquiries, Tax Account Statement generation, payment reconciliation) are served by the STA views in the ORS, which is a separate system. The DM solution reads from these same ORS views but does not replicate taxpayer accounting functions. Director, Customs is also excluded as customs debt management follows distinct EU-mandated procedures outside the DM solution scope.

## 3.2 Detailed User Personas

### 3.2.1 Debt Management Officer

**Role Description:** A front-line officer within the Debt Management Unit responsible for managing a portfolio of overdue taxpayer accounts. Works cases from initial demand notice through enforcement escalation, instalment negotiation, and case closure. Typically handles 150–250 active cases simultaneously across multiple tax types.

**Daily Tasks:** Review assigned work queue and prioritise cases by age, value, and risk score. Issue demand notices and follow-up reminders. Contact taxpayers by telephone to negotiate payment or instalment arrangements. Process instalment agreement applications and monitor compliance with agreed schedules. Escalate non-responsive cases to enforcement actions (garnishment, lien, third-party claims). Document all actions and communications in the case file. Prepare write-off recommendations for uncollectable debts.

**Pain Points:** No unified view of a taxpayer's total debt across tax types — must consult multiple legacy systems. No automated prioritisation — must manually identify which cases need attention. Case history scattered across paper files and disconnected electronic records. Demand notices and correspondence generated manually using word processing templates. No systematic tracking of enforcement action effectiveness. Instalment agreement monitoring entirely manual — defaults often detected late.

**Needs from DM Solution:** A single screen showing consolidated taxpayer debt position across all tax types (sourced from ORS/STA views). Automated work queue prioritised by debt age, value, and risk score. One-click demand notice generation with auto-populated taxpayer details. Instalment agreement workflow with automated compliance monitoring and default alerting. Case timeline showing full history of all actions, communications, and status changes. Automated escalation triggers when cases exceed age or value thresholds.

**Success Criteria:** Time to issue first demand notice after debt arises reduced from days/weeks to same-day automatic. Instalment defaults detected within 24 hours of missed payment date. Case load managed through prioritised queue without manual triage. All enforcement actions traceable with full audit trail. Reduction in time spent on administrative tasks by at least 40%.

### 3.2.2 Team Leader / Supervisor

**Role Description:** Supervises a team of 12 Debt Management Officers. Responsible for work allocation, quality assurance, escalation decisions, and team performance reporting. Approves instalment agreements above standard thresholds and authorises enforcement actions that require supervisory sign-off (property seizure, bankruptcy demands, passport seizure).

**Daily Tasks:** Monitor team work queues and redistribute cases for balanced workload. Review and approve instalment agreement proposals that exceed officer authority. Authorise high-value enforcement actions. Conduct quality reviews on case handling. Prepare weekly and monthly performance reports for directorate management. Identify training needs and process improvement opportunities. Handle taxpayer escalation complaints.

**Pain Points:** No real-time visibility into team workload distribution or case status. Performance reporting requires manual compilation from multiple sources. Cannot easily identify bottlenecks or officers struggling with case volumes. Approval workflows are paper-based or email-based, creating delays. No benchmarking data to compare team performance against targets or other teams.

**Needs from DM Solution:** Team dashboard showing aggregate case status, workload distribution, and performance metrics. Configurable approval workflows that route cases requiring supervisory action. Alert mechanisms for cases approaching critical thresholds (60-day window, escalation deadlines). Drill-down capability from team metrics to individual officer and case-level detail. Automated generation of periodic performance reports aligned with KPI framework.

**Success Criteria:** Real-time visibility into every case in the team's portfolio. Approval turnaround reduced from days to hours. Performance reports generated automatically with no manual data compilation. Clear identification of underperforming areas or process bottlenecks. Team KPIs (resolution rate, collection rate, average case age) visible at a glance.

### 3.2.3 Senior Management / Director

**Role Description:** Director or Deputy Director responsible for strategic oversight of tax administration functions. Reports to the Commissioner on revenue collection performance, compliance trends, and operational efficiency. Makes policy decisions regarding enforcement thresholds, staffing allocation, and process changes. Typically not a daily system user but relies on dashboards and periodic reports.

**Daily Tasks:** Review strategic dashboard for key performance indicators. Assess revenue collection trends and debt stock movement. Review escalation cases requiring directorate-level authorisation. Prepare briefings for the Commissioner and Ministry. Evaluate effectiveness of enforcement strategies and adjust resource allocation. Approve policy changes to debt thresholds, write-off criteria, or instalment parameters.

**Pain Points:** No consolidated performance dashboard — relies on manually compiled spreadsheets that are often weeks out of date. Cannot quantify the impact of policy changes or staffing decisions on collection outcomes. Limited visibility into debt stock composition (by age, value, tax type, debtor category). Revenue forecasting for Treasury is imprecise. No way to benchmark MTCA performance against international standards.

**Needs from DM Solution:** Strategic dashboard showing aggregate arrears management KPIs. Trend analysis with period-over-period comparison. Debt stock composition analysis (by category, age, tax type, sector). Revenue collection tracking against targets. Drill-down capability from strategic overview to departmental and case-level detail. Exportable reports for external stakeholder briefings.

**Success Criteria:** All 12 Arrears Management KPIs reportable through the system. Dashboard refreshed in real time or near-real-time. Debt stock movement visible at a glance with aging analysis. Revenue collection tracking accurate to within one business day. Strategic decisions supported by data rather than anecdote.

### 3.2.4 IT Administrator

**Role Description:** Member of MTCA's small IT unit (19 staff) responsible for system configuration, user management, workflow modification, and first-line technical support. In the context of the Case Management Platform, also acts as a citizen developer — configuring forms, reports, and workflow adjustments without traditional software development. Bridges the gap between business requirements and technical implementation.

**Daily Tasks:** Manage user accounts, roles, and permissions. Configure and update workflow definitions based on business requirements. Create and modify report templates and dashboard layouts. Monitor system performance and data synchronisation status. Troubleshoot data discrepancy issues between ORS views and displayed debt data. Maintain integration connections (ORS API, SAS VIYA, document generation). Support business users with platform usage questions.

**Pain Points:** Very small team relative to the number of systems being supported. Limited capacity to handle concurrent demands from operational users, ITCAS vendor engagement, and platform maintenance. Legacy PowerBuilder applications require specialised skills that are difficult to find. Change requests from business units often exceed available development capacity. No standardised change management process for workflow modifications.

**Needs from DM Solution:** Low-code configuration interface that enables workflow changes without traditional coding. Clear documentation of all data models, API endpoints, and integration patterns. Automated monitoring and alerting for data pipeline health and system performance. Role-based access control administration interface. Version control for workflow definitions. Sandbox environment for testing configuration changes before production deployment.

**Success Criteria:** Routine workflow modifications (threshold changes, template updates, rule adjustments) completed within hours, not weeks. Zero-downtime deployment of configuration changes. System health monitoring automated with proactive alerting. New report types configurable by IT staff without vendor support. Clear audit trail of all configuration changes.

### 3.2.5 Taxpayer (Indirect — Receives DM Outputs)

**Role Description:** A natural person or legal entity registered with MTCA for one or more tax obligations. In the DM solution, the taxpayer is primarily an indirect stakeholder — affected by the system's outputs (demand notices, instalment correspondence, enforcement actions) but with no direct system access. Full self-service portal access is deferred to ITCAS; the interim DM solution does not include a taxpayer-facing interface.

**Interactions with DM Solution:** Receives system-generated demand notices, reminders, and instalment correspondence. Submits instalment agreement applications through existing channels (paper, counter) — processed by DM Officers using the DM workflow. Receives enforcement action notifications. May request information about their debt status through existing channels — DM Officers access the debtor's position via ORS views within the DM solution.

**Needs from DM Solution (reflected in system outputs):** Accurate, clearly formatted demand notices with correct amounts reconciled to taxpayer's actual debt position. Consistent payment allocation reflected in all correspondence. Timely processing of instalment agreement applications. Fair and transparent enforcement escalation. Clear communication of rights and obligations in all system-generated documents.

**Success Criteria:** Zero inaccurate demand notices issued (amounts reconciled to ORS data). Instalment applications acknowledged within 3 business days and decided within 10 business days. All correspondence available in English and Maltese.

### 3.2.6 External Auditor (Read-Only)

**Role Description:** Auditor from the National Audit Office (NAO) or external audit firm conducting periodic reviews of MTCA's debt management practices, write-off procedures, and enforcement effectiveness. Requires read-only access to system data for audit sampling, trend analysis, and compliance verification. Access is periodic and governed by formal audit engagement terms.

**Interactions with DM Solution:** Reviews debt case samples for procedural compliance. Examines write-off authorisation trails and supporting documentation. Analyses aged debt reports for reasonableness and trend consistency. Assesses adequacy of enforcement escalation decisions.

**Pain Points (current state):** Audit evidence must be manually compiled from multiple disconnected systems. No centralised audit trail for debt management decisions. Difficult to perform statistical sampling without consolidated data. Write-off documentation is inconsistent and sometimes incomplete.

**Needs from DM Solution:** Read-only access to case data, audit trails, and decision logs through a dedicated auditor role. Pre-built audit reports (debt aging, write-off summary, enforcement activity, case lifecycle analysis). Export capability for audit sampling (CSV, Excel). Complete audit trail on every case action, workflow decision, and approval.

**Success Criteria:** Audit data extraction completed in hours rather than weeks. Full audit trail available for every debt management case. Write-off authorisation chain verifiable from system records alone. Statistical sampling possible directly from system reports. No material findings related to missing documentation or untraceable decisions.

# 4. Functional Requirements — Debt Management (DM)

## Domain Overview

The Debt Management domain covers the end-to-end lifecycle of tax debt from initial identification through reminder, demand, escalation, enforcement, and resolution (payment, instalment agreement, or write-off). The solution implements a risk-based, proportional enforcement strategy aligned with the four OECD strategic principles: proactive compliance support, post-due-date escalation, enforcement via data-driven strategy, and collection gap management through write-off.

All debt management activities depend on **taxpayer balance and transaction data sourced from ORS/ClickHouse** (via the STA view in ORS) as the single source of truth for taxpayer balances and transaction history. Enforcement actions are proportional to debt category thresholds (C1–C5) and are driven by configurable Finite State Machine (FSM) workflows in the Case Management Platform.

> **External dependency:** The DM module reads taxpayer account data from the ORS (Operational Reporting System) built on ClickHouse, which replicates and consolidates data from the legacy Informix databases. STA-specific functional requirements (STA-FR-xxx) are documented separately and are not part of this specification.

## 4.1 Debt Identification and Case Creation

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-001 | The system shall automatically identify taxpayers with outstanding debt by monitoring taxpayer balances (sourced from ORS/ClickHouse) for liabilities past their due date, and flag accounts with the status "debt outstanding" and/or "debt payable". | Must | Within 24 hours of a liability passing its due date, the system flags the taxpayer's account with the appropriate debt status. Flagging applies across all tax types. No manual intervention required for detection. | Vendor Proposal §6.8.2 (item 1); PDF §5.1.2 | ITCAS Arrears Management auto-detects overdue liabilities from Ledger. |  |
| DM-FR-002 | The system shall automatically create debt management cases based on pre-defined parameters, events, and taxpayer actions, including: dishonoured payments, cancelled instalment agreements, suspense account non-resolution (unidentified-payment suspense per STA STA-FR-021; note that *suspended identified payments* per STA STA-FR-046 do not trigger debt cases — they reduce the enforceable balance instead), and assessment non-payment. | Must | For each trigger event, a new debt case is created with: case ID, TIN, debt amount, tax type(s), age of debt, trigger event, creation timestamp. Case assigned to worklist. Multiple trigger events for the same taxpayer consolidated into a single case unless configured otherwise. | Vendor Proposal §6.7, §6.8; PDF §5.2.1 | ITCAS Case Management auto-generates debt cases from events. |  |
| DM-FR-003 | The system shall support manual case creation by authorised MTCA officers for situations not covered by automated triggers. | Must | Officer can create a debt case specifying: TIN, tax type(s), period(s), reason, priority. Case enters standard workflow upon creation. Audit trail records creating officer and justification. | Vendor Proposal §6.7 | ITCAS CM supports both automatic and manual case creation. |  |
| DM-FR-004 | The system shall automatically classify new debt into categories based on configurable amount thresholds: C1 (<€30), C2 (€30–100), C3 (€100–1,000), C4 (€1,000–20,000), C5 (€20,000–200,000), with an implied C6 for amounts >€200,000. Thresholds must be configurable. | Must | Each new debt classified into the correct category based on the consolidated owed amount. Category determines available enforcement actions. Category re-evaluated when balance changes (payments received, additional liabilities posted). Threshold values configurable by administrators without code changes. | PDF §5.5 Table 4; PDF §5.6 | ITCAS workflow rules apply proportional enforcement. Interim implements same category logic. |  |
| DM-FR-005 | The system shall retain a complete debt history for each taxpayer at database level, including all previous debt cases, enforcement actions, outcomes, and resolution methods. | Must | Full debt history accessible per TIN showing: all historical debt cases with timeline, actions taken, amounts recovered, and case outcomes. History persists after case closure. Query by TIN returns complete chronological record. | Vendor Proposal §6.8.2 (item 4) | ITCAS retains complete debt history in taxpayer electronic file. |  |

## 4.2 Risk Scoring and Classification

There is a centralised risk management improval initiative (so called SAS project), which is working on implementation of risk identification, assessment and evaluation. The requirements below will be allocated to this centralised component.

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-006 | The system shall rank debt cases by configurable risk criteria including: size of debt, age of debt, number of revenue types involved, taxpayer compliance history, taxpayer type (NP/LP), and economic sector. | Must | Debt cases ranked and scored per weighted criteria. Risk score visible on case dashboard. Sorting by risk score enabled on worklists. Weights configurable by administrators. | Vendor Proposal §6.8.2 (item 9); PDF §5.1 | ITCAS runs risk analysis profiles for debt case prioritisation. |  |
| DM-FR-007 | The system shall generate debt risk profiles for individual taxpayers and aggregate profiles for taxpayer segments, supporting data-driven enforcement strategy. | Must | Individual risk profile shows: current debt amount, historical debt patterns, compliance track record, enforcement response history, payment behaviour. Aggregate profiles available by segment (NP/LP, economic sector, region). | Vendor Proposal §6.8.2 (item 10); PDF §5.1 | ITCAS risk profiling integrated with SAS VIYA analytics. |  |
| DM-FR-008 | The system shall support configurable business rules for automatic inclusion/exclusion of debtors with certain attributes from enforcement actions (e.g., size/type of debt, existence of valid instalment schedule, pending disputes). | Must | Rules configured specifying attributes for inclusion/exclusion. Debtors with pending objections excluded from standard enforcement. Debtors with active instalment agreements excluded while compliant. Rules auditable and version-controlled. | Vendor Proposal §6.6.1; PDF §5.5 | ITCAS enforcement rules exclude objection/instalment cases per configuration. |  |

## 4.3 Proactive Notifications and Reminders

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-009 | The system shall generate proactive notifications (nudges) to taxpayers approaching due dates for filing and payment, sent via configurable channels (email, SMS, in-app notification) at configurable lead times before the due date. | Must | Notifications sent at configured intervals (e.g., 14 days, 7 days, 1 day before due date). Channel selection per taxpayer preference or default. Delivery status tracked. Templates configurable per tax type and notification type. | PDF §5.1 (Strategic Principle 1); Vendor Proposal §6.8.2 (item 6) | ITCAS Notification Service provides multi-channel notifications. |  |
| DM-FR-010 | After a liability passes its due date, the system shall automatically generate a first payment reminder to the liable party specifying the overdue amount and the period within which payment must be made. Applicable to debt categories C2–C5. | Must | Reminder generated within configurable days of due date (e.g., 7 days). Reminder includes: TIN, tax type, period, overdue amount broken down by PA/IA/PCA, payment deadline, payment methods. Reminder delivery recorded. | Vendor Proposal §6.8.2 (items 2-3); PDF §5.5 Table 4 | ITCAS auto-generates payment reminders per configured rules. |  |
| DM-FR-011 | The system shall generate a second reminder notice for debt categories C3–C5 if the first reminder is not responded to within the configured period. | Must | Second reminder triggered if no payment/response received within configured period after first reminder. Second reminder includes escalation warning. Delivery tracked and recorded on case. | PDF §5.5 Table 4 | ITCAS supports multi-stage reminder workflow. |  |
| DM-FR-012 | The system shall automatically generate and issue a demand notice (formal demand for payment) after the reminder period expires without payment, for debt categories C2–C5. | Must | Demand notice generated using configured template. Includes: full debt breakdown, legal basis, consequences of non-payment, appeal rights, payment deadline. Notice issued via configured channels. Physical delivery recording supported. | Vendor Proposal §6.8.2 (items 3, 5); ITCAS RFP (demand notes per tax type) | ITCAS generates demand notes using configurable templates. |  |
| DM-FR-013 | The system shall issue a final/immediate demand notice after the specified period following the initial demand, for cases where debt remains outstanding. | Must | Final demand generated with configurable escalation warning. Includes legal consequences of continued non-payment. Deadline for response before enforcement action. Delivery status tracked with proof of delivery required for enforcement validity. | Vendor Proposal §6.8.2 (item 5); PDF §5.3 | ITCAS supports multi-stage demand with final notice. |  |
| DM-FR-014 | The system shall send notifications, demands, and legal notices via defined channels (in-app, email, postal mail) and provide capabilities to record the physical delivery status of postal notices and letters. | Must | For each notice: channel used, date sent, delivery status (delivered/undelivered/returned), delivery date recorded. For postal notices: delivery confirmation from postal service integrated or manually entered. Undelivered notices flagged for alternative action. | Vendor Proposal §6.8.2 (item 6); ITCAS RFP | ITCAS notification service with delivery tracking. |  |

## 4.4 Escalation Workflows

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-015 | The system shall implement configurable debt management workflows using a Finite State Machine (FSM) approach, enabling automatic progression through enforcement stages based on configurable rules, time periods, and taxpayer responses. | Must | Workflow states and transitions configurable without code changes. Each state has: entry actions, time-based transitions, event-based transitions, exit actions. Workflow execution logged. Manual override by authorised officers supported. | Vendor Proposal §6.8.1, §6.8.2 (item 15); PDF §5.2.2 | ITCAS BPM tool configures FSM-based debt collection workflows. Interim BPMN 2.0 workflows must be portable. |  |
| DM-FR-016 | If debt remains outstanding after automated demand/reminder stages, the system shall escalate the case to manual processing within the Case Management Platform, assigning it to the appropriate worklist based on debt category, risk score, and officer specialisation. | Must | Escalation trigger fires → case moved to manual CM queue → assigned per workload balancing and specialisation rules → officer receives notification. Case includes full history of automated actions taken. | Vendor Proposal §6.8.2 (item 7); PDF §5.4 | ITCAS CM handles escalated debt cases with configurable routing. |  |
| DM-FR-017 | The system shall continuously monitor the consolidated period balance across all tax types (sourced from ORS/ClickHouse) and automatically re-evaluate debt category classification when the balance changes (due to additional liabilities, partial payments, or credits). | Must | Balance change triggers category re-evaluation. Category upgrade (debt increased) → additional enforcement actions become available. Category downgrade (partial payment) → current enforcement level maintained but no further escalation until warranted. Re-evaluation logged. | PDF §7.2 (Figure 12); Vendor Proposal §6.8 | ITCAS monitors balances and adjusts enforcement dynamically. |  |
| DM-FR-018 | For the largest debts (>€nn thousand, configurable threshold), the system shall trigger an immediate high-priority case requiring telephone contact within 2 business days of debt arising. | Must | Debt exceeding high-value threshold → immediate case creation with "critical" priority → assigned to senior officer → SLA: first telephone contact within 2 business days. SLA compliance tracked and reported. | PDF §5.6 | ITCAS workflow rules support priority-based immediate escalation. Threshold configurable. |  |
| DM-FR-019 | The system shall support telephone contact tracking: recording date, time, officer, taxpayer contacted, outcome, and agreed follow-up actions. Telephone contact is applicable for debt categories C3–C5. | Should | Contact record created with: timestamp, officer ID, TIN, contact person, contact method (phone/visit), outcome code (promise to pay, dispute, unable to contact, etc.), notes, follow-up date. Record attached to debt case. | PDF §5.5 Table 4 | ITCAS CM supports activity logging on cases. |  |
| DM-FR-020 | The system shall support scheduling and recording of taxpayer visits by enforcement officers for debt categories C4–C5, with visit outcomes captured and linked to the debt case. | Should | Visit scheduled → assigned to field officer → visit report completed with: date, location, persons present, outcome, evidence collected, follow-up actions. Report attached to case. GPS/location optional. | PDF §5.5 Table 4 | ITCAS CM supports field activity tracking. |  |

## 4.5 Instalment Agreement Lifecycle

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-021 | The system shall support a configurable instalment agreement process allowing taxpayers to apply for structured payment plans covering all outstanding debts across their accounts. Applications accepted via online portal and over-the-counter (paper, entered by MTCA officer). | Must | Application captures: TIN, proposed schedule (amounts and dates), all outstanding debts covered. System validates completeness. Portal and counter applications follow identical workflow. Application status visible to taxpayer in real time. | Vendor Proposal §6.8.2 (items 16-17); PDF §6.2 | ITCAS instalment module with portal submission. |  |
| DM-FR-022 | The system shall prevent a taxpayer from applying for more than one active instalment plan. A new plan may only be submitted after the previous one is cancelled (automatically or manually by authorised MTCA officer). | Must | Submission attempt with active plan → system rejects with message indicating existing active plan. Cancelled plan → new submission permitted. Validation applies to both portal and counter submissions. | Vendor Proposal §6.8 (instalment validation); PDF BR-INST-01 | ITCAS enforces single-plan-per-taxpayer rule. |  |
| DM-FR-023 | The system shall support configurable approval workflows for instalment agreement applications: standard plans within parameters auto-approved or fast-tracked; plans outside standard parameters routed to senior officer for approval. | Must | Parameters defined for auto-approval (e.g., debt <€5,000, standard duration ≤12 months). Applications outside parameters → routed to designated approver with full taxpayer profile. Approval/rejection recorded with reasons. | Vendor Proposal §6.8.2 (item 17); ITCAS RFP; PDF BR-INST-05 | ITCAS configurable approval workflows for instalment plans. |  |
| DM-FR-024 | During the draft decision stage, MTCA officials shall be able to review and alter proposed payment schedules and assemble alternative schedules. The system shall allow sending the MTCA-provided draft to the taxpayer for review and approval. | Must | Officer views application → modifies schedule → generates draft decision → sends to taxpayer via configured channel → taxpayer reviews and accepts/rejects → response captured in system. Version history of all schedule changes maintained. | Vendor Proposal §6.8 (approval workflow) | ITCAS provides draft decision review and taxpayer approval workflow. |  |
| DM-FR-025 | The system shall automatically calculate the interest component of instalment agreements and apply a lower interest rate during the period of compliance with the approved schedule. **Boundary:** DM computes the *planned* interest component of the agreement schedule (the contractual figures shown to the taxpayer); the *actual* interest posting to the taxpayer account is performed by the STA accounting engine at the reduced rate based on the compliance status DM publishes (DM-FR-026; STA spec STA-FR-024, BR-STA-009). Planned and posted interest must reconcile per agreement. | Must | Interest calculated per instalment based on outstanding principal, applicable reduced rate, and payment dates. Interest component visible on each instalment. Total interest across agreement displayed. Rate reverts to standard if compliance breaks. Plan-vs-posted reconciliation per agreement within €0.01; variances raise a reconciliation exception. | Vendor Proposal §6.8.2 (item 18); PDF BR-INST-04; STA spec BR-STA-009 | ITCAS automatically calculates instalment interest per configured rates. |  |
| DM-FR-026 | The system shall monitor instalment agreement compliance by tracking each scheduled payment against actual payments received, and issue alerts for non-compliance (late or missed payments). The system shall **publish the agreement status and compliance flag to the STA accounting engine** on every status change, for use in reduced-interest posting (STA BR-STA-009) and conditional tax clearance (STA BR-STA-004/040). | Must | Each instalment payment date monitored. Payment received ≥ scheduled amount by due date = compliant. Missed or partial payment → alert generated to case officer and taxpayer within configurable period (e.g., 3 business days). Compliance status dashboard per agreement. Status-change events (active/compliant, non-compliant from date, cancelled, completed) delivered to the STA interface within the same processing cycle; delivery logged. | Vendor Proposal §6.8.2 (item 19); PDF KPI #29, #30; STA spec STA-FR-024, BR-STA-009/040 | ITCAS monitors instalments and generates non-compliance alerts. |  |
| DM-FR-027 | Upon cancellation of an instalment agreement (due to non-compliance or taxpayer request), the system shall automatically create a new debt recovery case with the outstanding balance and route it to the enforcement workflow. | Must | Agreement cancelled → system calculates remaining debt (principal + accrued interest at standard rate) → new recovery case created → assigned to worklist → lower interest rate revoked from cancellation date. Full agreement history preserved on case. | Vendor Proposal §6.8.2 (item 20); PDF BR-INST-03 | ITCAS auto-creates recovery case from cancelled instalment. |  |
| DM-FR-028 | An active instalment agreement shall prevent the creation or progression of enforcement actions against the taxpayer, provided payments remain current. | Must | Active compliant agreement → enforcement workflow paused/blocked for covered debts. New debts not covered by agreement → separate enforcement permitted. Non-compliance → block lifted and enforcement resumes. | PDF BR-INST-02 | ITCAS pauses enforcement during compliant instalment agreements. |  |

## 4.6 Enforcement Actions

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-029 | The system shall support bank account garnishing (freezing) through automated generation of freezing requests sent to banks via web services integration. Applicable to debt categories C3–C5. | Must | Officer or automated workflow initiates garnish → system generates freezing request with: TIN, bank details, debt amount, legal reference → request transmitted via web service → bank response (success/failure/partial) recorded → garnished amounts posted to taxpayer account upon receipt. | Vendor Proposal §6.8.1; PDF §5.5 Table 4 | ITCAS integrates with banks for automated freezing requests via web services. |  |
| DM-FR-030 | The system shall support publishing of debtor names and details on the MTCA website and/or national centralised debt registry, for debt categories C3–C5 where configured criteria are met. | Should | Publication criteria configurable (minimum debt amount, minimum age, prior notice given). Publication list generated automatically. Debtor removed from list upon debt resolution. Publication and removal dates recorded. | Vendor Proposal §6.8; PDF §5.5 Table 4 | ITCAS configurable to auto-publish debtor list. |  |
| DM-FR-031 | The system shall support lien on assets by integrating with real estate and automobile registers to identify and register encumbrances against taxpayer assets. Applicable to debt categories C4–C5. | Should | System queries asset registers → displays identified assets → officer selects assets for lien → lien registration request generated → confirmation recorded. Lien release upon debt resolution processed through same integration. | Vendor Proposal §6.8.1; PDF §5.5 Table 4 | ITCAS integrates with asset registers for enforcement. |  |
| DM-FR-032 | The system shall support third-party claims (garnishment orders directed at third parties owing money to the debtor). Applicable to debt categories C4–C5. | Should | Officer identifies third party → generates garnishment order using configured template → tracks third-party response → amounts received posted to taxpayer account **via the STA debtor's-debtor posting pattern (STA BR-STA-044: payer-side payment + transfer-to credit on the debtor's account, both audit-linked to this enforcement case reference)**. Third-party compliance monitored. | PDF §5.5 Table 4; §7.1; PDF Annex 1 (reconciliation between taxpayers); STA spec BR-STA-044 | ITCAS enforcement supports third-party claims. |  |
| DM-FR-033 | The system shall support departure prohibition (passport seizure) enforcement for debt categories C4–C5, generating the appropriate legal documents and tracking the order lifecycle. | Should | Legal document generated from configured template → submitted to relevant authority → confirmation recorded → restriction lifted upon debt resolution or court order → removal request generated. Status tracked on case timeline. | PDF §5.5 Table 4; Vendor Proposal §6.8.2 (item 23 — departure prohibition) | ITCAS supports departure prohibition as enforcement classification. |  |
| DM-FR-034 | The system shall support property seizure and auction sale enforcement for debt categories C4–C5, including: asset identification, seizure order generation, auction scheduling, sale proceeds recording, and distribution to debt accounts. | Should | Seizure process: identify assets → generate seizure order → record seizure → schedule auction → record sale proceeds → distribute to debt accounts → close enforcement action. Each step logged with dates and responsible parties. | PDF §5.5 Table 4; Vendor Proposal §6.8 (organising/selling/destruction of assets) | ITCAS supports asset seizure with ledger posting of proceeds. |  |
| DM-FR-035 | The system shall support bankruptcy/liquidation demand generation: initiating proceedings, sending requests to court, and tracking case outcomes. | Should | Officer initiates → system generates court submission documents → submission recorded → court response tracked → if bankruptcy granted, taxpayer status updated → remaining debt handled per bankruptcy rules. | Vendor Proposal §6.8; PDF §5.5 Table 4 | ITCAS can end enforced collection with bankruptcy request to court. |  |
| DM-FR-036 | The system shall support temporary closure of business as an enforcement action, generating the necessary documentation and tracking compliance. | Could | Closure order generated → served on taxpayer → compliance monitored → business re-opening permitted upon debt resolution or court order. Status tracked on case timeline. | Vendor Proposal §6.8.2 (item 23 — temporary closure) | ITCAS supports temporary closure as enforcement classification. |  |
| DM-FR-037 | The system shall support appointment of an Agent (Warrant) for debt recovery, tracking the agent's activities, recovered amounts, and commission/fees. | Should | Agent appointed with defined scope → agent activities logged → recovered amounts posted to taxpayer account → agent fees/commission calculated and tracked separately. | Vendor Proposal §6.8.2 (item 30) | ITCAS supports warrant appointment for debt recovery. |  |
| DM-FR-038 | The system shall track legal fees applicable to enforcement workflows and post them to the taxpayer's ledger as a separate charge type, linked to the relevant enforcement case. | Must | Legal fees per enforcement action posted to taxpayer account with: fee type, amount, associated case ID, date posted. Fees visible in taxpayer statement under separate charge category. Total legal fees per case and per taxpayer reportable. | ITCAS RFP; Vendor Proposal §6.8 | ITCAS tracks legal fees in separate taxpayer account per the RFP. |  |

## 4.7 Enforcement Action Configuration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-039 | The system shall allow configuration of standard debt collection action types using a BPM tool, with each action type assigned attributes: name and code, applicable debt size range, applicable taxpayer type, document template, and specific instructions. | Must | Administrator creates/edits action types with all five attributes. Actions available in workflow designer. Changes version-controlled. Each action type testable before production deployment. | Vendor Proposal §6.8 (BPM configuration); PDF §5.5 | ITCAS BPM tool configures action types with full attribute set. |  |
| DM-FR-040 | The system shall support user-defined templates for demand notes, judicial letters, garnishment orders, and other enforcement documents, with merge fields populated from taxpayer and case data. | Must | Templates created/edited by authorised staff using a template editor. Merge fields auto-populated from: taxpayer registration data, taxpayer balances (from ORS), case details, dates. Templates versioned. Preview before generation. Batch generation supported for bulk operations. | ITCAS RFP (user-defined templates); Vendor Proposal §6.8.2 (item 6) | ITCAS ADG (Automatic Document Generation) module. |  |
| DM-FR-041 | Demand notes and legal notices shall be generated either per tax type separately or consolidated across multiple tax types in a single notice, based on configurable rules. | Must | Configuration allows: separate notices per tax type (default), consolidated notice across selected tax types, or fully consolidated single notice. Option selectable per enforcement action or per batch operation. Both formats comply with legal requirements. | ITCAS RFP (per tax type or consolidated) | ITCAS supports both separate and consolidated demand notes. |  |

## 4.8 Write-Off

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-042 | Debts in category C1 (<€30) shall be automatically written off as "uneconomic to collect" with no manual intervention required. | Must | Debt flagged as C1 → system automatically processes write-off → taxpayer account updated → write-off transaction posted to both taxpayer and revenue accounts → case closed. Write-off report generated for audit purposes. No human action needed. | PDF §5.6 (C1 handling); BR-DM-01 | ITCAS write-off rules configurable. May require legislative authority (OQ-05). |  |
| DM-FR-043 | The system shall support approved write-off processing for larger debts, requiring supporting evidence, documented decisions, and approval by authorised officers at appropriate seniority levels based on write-off amount. | Must | Write-off request submitted with: supporting evidence, decision rationale, amount. Routed to appropriate approver based on amount thresholds (configurable delegation levels). Approved write-off posted to accounts. Rejected write-off returned to case for alternative action. | Vendor Proposal §6.8.2 (item 11, 25); PDF §7.3 | ITCAS supports approved write-off with evidence attachment and approval workflow. |  |
| DM-FR-044 | The system shall support both individual and bulk write-off operations (per system-defined limits, annual balancing, or account closure criteria). Account-closure write-offs are received as hand-offs from the STA account-closure case (STA spec STA-FR-048); the write-off outcome and DM case reference are returned to the closure case so the account can complete closure. | Should | Individual: single case write-off with approval. Bulk: batch selection by criteria (e.g., all C1 debts, all debts older than statutory period) → batch approval → bulk posting. Bulk operation produces summary report and individual audit entries. Closure-originated write-offs carry the STA closure-case reference end to end. | Vendor Proposal §6.8.2 (item 25); STA spec STA-FR-048 | ITCAS supports individual and bulk write-off processing. |  |
| DM-FR-045 | Debts in category C2 (€30–100) that remain uncollected after automated actions shall be retained in the taxpayer account for future collection (refund interception) or written off after the statutory collection period expires. | Must | C2 debts post-automation: status set to "passive collection" → monitored for refund interception (executed by the STA accounting engine per STA spec STA-FR-033/BR-STA-025; DM consumes the interception outcome) → after statutory period (configurable years) → eligible for bulk write-off. No manual enforcement actions consumed on C2 debts. | PDF §5.6; BR-DM-10; STA spec STA-FR-033 | ITCAS supports passive collection and time-based write-off eligibility. |  |
| DM-FR-046 | The system shall maintain write-off status on debt records, preserving the full history of the original debt, enforcement actions taken, and write-off decision for audit and reporting purposes. | Must | Written-off debts remain in system with status "written-off". All associated case history, documents, and approval records preserved. Written-off amounts reportable by: tax type, period, category, reason, officer, date. | Vendor Proposal §6.6; §6.8.2 (item 26) | ITCAS records and maintains debts with write-off status. |  |

## 4.9 Collection Planning and Management Information

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-047 | The system shall support compilation of local and national debt collection plans, incorporating: current debt stock analysis, resource allocation, target recovery amounts by category, and enforcement strategy by taxpayer segment. | Should | Collection plans created with: scope (local/national), target amounts, resource requirements, strategy per debt category, timeline. Plan vs. actual tracking. Plans configurable by area/region/national. | Vendor Proposal §6.8.2 (item 12); PDF §5.7 | ITCAS supports collection plan compilation. |  |
| DM-FR-048 | The system shall generate management information regarding debt status, debt recovery case status, and enforcement action outcomes, including pre-defined reports and ad-hoc query capability. | Must | Pre-defined reports include: total debt by category, debt aging analysis, recovery rate by action type, case officer productivity, instalment compliance rate. Ad-hoc query allows filtering by any debt/case/taxpayer attribute. Reports exportable. | Vendor Proposal §6.8.2 (items 13, 29); PDF §8.4.4 | ITCAS Reporting Engine generates debt management reports. Interim uses ORS. |  |
| DM-FR-049 | The system shall extract debt information from the ORS/ClickHouse data warehouse for analysis, reporting, and risk profiling purposes. | Must | System connects to ORS/ClickHouse, extracts current and historical debt data, refreshes at configurable intervals (minimum daily). Extracted data supports all debt reporting and risk analysis functions. Data lineage tracked. | Vendor Proposal §6.8.2 (item 14) | ITCAS extracts from its own DWH. Interim reads from ORS/ClickHouse. |  |
| DM-FR-050 | The system shall maintain comprehensive logs and timestamps of all debt management and enforcement process activities, supporting complete audit trails. | Must | Every action (automated or manual) logged with: timestamp, actor (system/officer ID), action type, case ID, TIN, before/after state, outcome. Logs immutable and accessible for audit queries. Retention per legal requirements. | Vendor Proposal §6.8.2 (items 21, 26) | ITCAS audit trail covers all debt management activities. |  |
| DM-FR-051 | The system shall notify participants (officers, supervisors, taxpayers) of process status changes and updates based on pre-defined notification rules, and alert participants to actions pending from their end. | Must | Status change → appropriate parties notified via configured channels. Officer receives alert for cases requiring action. Supervisor alerted for SLA breaches. Taxpayer notified of case progress. Notification rules configurable per workflow stage. | Vendor Proposal §6.8.2 (item 22) | ITCAS Notification Service sends event-driven notifications. |  |
| DM-FR-052 | The system shall support upload and attachment of relevant documents to debt management and recovery cases, including scanned correspondence, evidence, court documents, and officer reports. | Must | Documents uploaded and linked to case with: document type, description, upload date, uploading officer. Supported formats: PDF, DOCX, XLSX, images (JPG, PNG). Maximum file size configurable. Documents retrievable from case record. | Vendor Proposal §6.8.2 (item 27) | ITCAS Electronic Document System (EDS) provides document management. |  |
| DM-FR-053 | The system shall support clearing (closing) of debt recovery cases when all debts are resolved (paid, written off, or otherwise concluded), with final status recorded and case archived. | Must | Case eligible for closure when: all associated debt balances resolved (zero or credit), all enforcement actions concluded, all documents attached. Closure recorded with: date, reason, final outcome, resolving officer. Closed cases searchable but read-only. | Vendor Proposal §6.8.2 (item 28) | ITCAS CM case closure with archival. |  |

## 4.10 Default Assessment Integration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-054 | For non-filers who do not respond to filing reminders, the system shall support generation of administrative (default) assessments — either auto-generated using configurable estimation rules or manually created by authorised officers — with amounts that follow the principle of avoiding excessively exaggerated estimates. | Should | Default assessment generated with: estimated amount, estimation method, basis for estimate (e.g., prior year, industry average), legal reference. Assessment amount subject to configurable reasonableness checks. Posted to taxpayer account as debit. | PDF §5.3 (steps 2-4); BR-DM-08 | ITCAS assessment management supports auto and manual default assessments. |  |
| DM-FR-055 | When a taxpayer files a return in response to a default assessment, the submitted return shall replace the default assessment. The system shall subject the filed return to reliability verification and update the taxpayer's risk profile accordingly. | Should | Filed return received → default assessment reversed → actual return amount posted → difference reconciled → risk profile updated based on variance between default and actual. Audit trail shows default → actual transition. | PDF §5.3 (step 3) | ITCAS returns processing replaces default assessments with filed returns. |  |
| DM-FR-056 | If no filing occurs despite the default assessment, the system shall create a new enforced collection case on the basis of the default assessment amount and route it to the debt management workflow. | Must | Configurable period expires → no filing received → debt case auto-created using default assessment balance → enters standard enforcement workflow for the applicable debt category. | PDF §5.3 (step 4) | ITCAS auto-creates enforcement case from unresponded default assessment. |  |

## 4.11 Debtors List Management

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| DM-FR-057 | The system shall maintain a debtors list viewable by authorised MTCA officers, showing all taxpayers with outstanding debts, filterable and sortable by: debt amount, debt age, tax type, taxpayer type, economic sector, enforcement stage, and risk score. | Must | Debtors list displays: TIN, taxpayer name, total debt, debt age (oldest), category (C1–C5), enforcement stage, risk score, assigned officer. Filters combinable. Export to Excel/CSV supported. Real-time refresh from taxpayer balance data (sourced from ORS). | PDF §8.4.4; Vendor Proposal §6.8.2 (item 8) | ITCAS provides configurable debtor views in CM and reporting. |  |
| DM-FR-058 | The system shall be configurable to automatically publish a list of qualifying debtors on the MTCA website and/or send the list to any external national centralised debt registry. | Could | Publication criteria configurable (minimum amount, minimum enforcement stage reached, prior notice given). List generated automatically at configured intervals. Published list updated when debtor resolves debt. Integration with national registry via file transfer or API. | Vendor Proposal §6.8 | ITCAS configurable for public debtor list publication. |  |

## External Dependencies

This section documents external systems and data sources referenced by DM functional requirements but not specified within this module.

| **Dependency** | **Description** | **Referenced By** |  |
| --- | --- | --- | --- |
| **ORS/ClickHouse** | Operational Reporting System providing consolidated taxpayer balances, transaction history, and STA views. Primary data source for debt identification, balance monitoring, and reporting. | DM-FR-001, 004, 017, 040, 049, 057 |  |
| **STA View in ORS** | Single Taxpayer Account view replicated in ORS from legacy Informix databases. Provides balance-of-period, payment history, and liability data consumed by DM processes. | DM-FR-001, 010, 017, 054 |
| **STA Accounting Engine (Module 05)** | The Taxpayer Accounting component specified in the STA Requirements Specification (counterpart document). Provides: the enforceable balance (total − disputed − suspended payments − held amounts), credit positions, disputed flags, debt aging inputs, refund-interception execution (STA-FR-033) and the debtor's-debtor posting pattern (BR-STA-044). Consumes from DM: instalment agreement status and compliance flag (DM-FR-026) and account-closure write-off outcomes (DM-FR-044). | BR-DM-048; DM-FR-002, 025, 026, 032, 044, 045 |  |
| **SAS VIYA** | Advanced analytics platform used for risk profiling, predictive scoring, and segment analysis supporting data-driven enforcement strategy. | DM-FR-007 |  |
| **Legacy Informix Databases** | Source systems for taxpayer registration, assessment, and payment data replicated into ORS. Nine databases with 5,170+ tables. | Indirect — via ORS |  |
| **ITCAS (future)** | European Dynamics' Integrated Tax and Customs Administration System. BPMN 2.0 workflow portability and eventual migration are design constraints for all DM workflows. | All DM-FR-xxx (ITCAS Alignment column) |  |
| **MTCA Online Portal** | Taxpayer self-service portal for instalment applications and notification delivery. | DM-FR-021, 024 |  |
| **Bank Web Services** | External bank interfaces for garnishment/freezing requests and payment confirmation. | DM-FR-029 |  |
| **Asset Registers** | Real estate and automobile registers for lien enforcement. | DM-FR-031 |  |
| **National Debt Registry** | External centralised debt registry for debtor list publication. | DM-FR-030, 058 |  |

# 4. Functional Requirements — Reporting, Workflow & Case Management, Integration

## RPT-FR Inclusion/Exclusion Classification

The following table classifies all 21 RPT-FR requirements from the source. Per extraction rule #4 (include DM-relevant reporting: debt aging, collection status, DM KPIs, debtors list) and rule #27 (exclude STA-only reports: revenue reconciliation, TAS generation), two requirements are excluded.

| **ID** | **Title** | **Decision** | **Rationale** |  |
| --- | --- | --- | --- | --- |
| RPT-FR-001 | Debt Aging Dashboard | ✅ INCLUDED | Core DM — debt aging by age band, category, taxpayer type |  |
| RPT-FR-002 | Debtors List Dashboard | ✅ INCLUDED | Core DM — all taxpayers with outstanding balances |  |
| RPT-FR-003 | KPI Monitoring Dashboard | ✅ INCLUDED | Core DM — 12 arrears management KPIs (items 24–35) |  |
| RPT-FR-004 | Revenue Dashboard | ❌ EXCLUDED | STA-only — total revenue across all tax types, Treasury comparison. Not DM-specific (rule #27) |  |
| RPT-FR-005 | Collection Performance Dashboard | ✅ INCLUDED | Core DM — collection activities, staff productivity, recovery rates |  |
| RPT-FR-006 | Multi-Stream Management Dashboard | ✅ INCLUDED | DM-relevant — debt stock, enforcement pipeline, instalment compliance overview |  |
| RPT-FR-007 | Hierarchical Dashboard Views | ✅ INCLUDED | Supporting capability — applies to all DM dashboards |  |
| RPT-FR-008 | Debt Aging Report | ✅ INCLUDED | Core DM — operational debt aging report |  |
| RPT-FR-009 | Collection Status Report | ✅ INCLUDED | Core DM — active case status by enforcement stage |  |
| RPT-FR-010 | Revenue Reconciliation Report | ❌ EXCLUDED | STA-only — revenue vs Treasury disbursement reconciliation (rule #27) |  |
| RPT-FR-011 | Instalment Compliance Report | ✅ INCLUDED | Core DM — instalment monitoring and compliance |  |
| RPT-FR-012 | Write-Off Report | ✅ INCLUDED | Core DM — write-off volumes, reasons, amounts |  |
| RPT-FR-013 | Staff Productivity Report | ✅ INCLUDED | DM-relevant — DM officer performance metrics |  |
| RPT-FR-014 | Tax Debt Status Report | ✅ INCLUDED | Core DM — debt amounts by TIN, tax type, period |  |
| RPT-FR-015 | Report Configuration Interface | ✅ INCLUDED | Supporting capability — enables all DM reporting |  |
| RPT-FR-016 | Save Report Configurations | ✅ INCLUDED | Supporting capability |  |
| RPT-FR-017 | Scheduled Report Generation | ✅ INCLUDED | Supporting capability |  |
| RPT-FR-018 | Report Export Formats | ✅ INCLUDED | Supporting capability |  |
| RPT-FR-019 | Drill-down Navigation | ✅ INCLUDED | Supporting capability — drill-down into debt data (STA hierarchy adapted per rule #30) |  |
| RPT-FR-020 | SAS VIYA Risk Score Display | ✅ INCLUDED | Core DM — risk scores in debtor/case views |  |
| RPT-FR-021 | Ad-hoc Query Capability | ✅ INCLUDED | Supporting capability — analyst self-service on ORS/ClickHouse |  |

**Summary:** 19 included, 2 excluded.

## 4.12 Reporting and Analytics

The reporting layer reads data from **ORS/ClickHouse** and presents it through Apache Superset dashboards and the Case Management Platform's built-in reporting components. This section covers dashboards, operational reports, report configuration, and analytics capabilities relevant to debt management.

> **External dependency:** All reporting data is sourced from ORS/ClickHouse. Taxpayer balance and transaction data originates from the STA view in ORS. Risk scores are consumed from SAS VIYA via REST API.

### 4.12.1 Dashboards

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| RPT-FR-001 | The system shall provide a **Debt Aging Dashboard** displaying the distribution of outstanding tax debt across configurable age bands (default: 0–30, 31–60, 61–90, 91–180, 181–365, >365 days), with breakdowns by tax type, debt category (C1–C5), and taxpayer type (NP/LP). | Must | Dashboard loads in <10 seconds. Age bands configurable by administrator. Totals reconcile with consolidated balances sourced from ORS. Drill-down from age band to individual debtor list. Visual representation includes bar chart and summary table. | PDF §8.4.4 (Table 18); KPI #27, #28 | ITCAS reporting module provides debt aging analytics. Interim dashboard migrates to ITCAS. |  |
| RPT-FR-002 | The system shall provide a **Debtors List Dashboard** showing all taxpayers with outstanding balances, filterable by: debt amount range, debt age, tax type, taxpayer type, economic sector (NACE), registration office, enforcement stage, risk score, and assigned officer. | Must | Dashboard displays: TIN, name, total debt, oldest debt age, category (C1–C5), enforcement stage, risk score, assigned officer. All filters combinable. Sortable by any column. Supports pagination for large result sets (>1,000 records). Export to Excel/CSV. | PDF §8.4.4 (Tables 15–17); DM-FR-057 | ITCAS CM provides configurable debtor views. Dashboard design portable. |  |
| RPT-FR-003 | The system shall provide a **KPI Monitoring Dashboard** displaying the 12 arrears management KPIs (items 24–35 from the MTCA KPI matrix) with current values, targets, trend indicators, and period-over-period comparison. | Must | Each KPI displays: current value, target value, variance, trend arrow (improving/declining/stable), sparkline for 12-month trend. Traffic light colour coding: green (≥target), amber (within 10% of target), red (>10% below target). Configurable target thresholds. Dashboard refreshes automatically at configurable intervals. | PDF §6.3 (Table 5, items 24–35); KPIs #24–35 | ITCAS MIS module subsumes KPI monitoring. Interim metrics and definitions transfer. |  |
| RPT-FR-005 | The system shall provide a **Collection Performance Dashboard** showing collection activities, outcomes, and staff productivity metrics including: number of actions taken by type, resolution rates, average resolution time, and collection amounts recovered per officer and per team. | Should | Metrics displayed by: individual officer, team, office, and aggregate. Time period selectable. Ranking of officers/teams by productivity. Drill-down from metric to underlying cases. Benchmark comparison against team averages. | PDF §6.3; KPIs #31–35 | ITCAS case analytics provides collection performance metrics. |  |
| RPT-FR-006 | The system shall provide a **Multi-Stream Management Dashboard** (Tier 1) for senior management showing a cross-functional overview of: total debt stock, new debt arising, debt resolved, enforcement pipeline, instalment compliance, and write-off volumes across all case streams. | Should | Single-page overview with: total open cases by priority (High/Medium/Low), case mix by origin (channel), SLA compliance trend, resolved vs new cases ratio. Interactive filters for date range and functional area. | PDF §6.3 (Figure 9) | ITCAS provides enterprise-level management dashboards. |  |
| RPT-FR-007 | All dashboards shall support three hierarchical views: (a) multi-stream for top management, (b) entity-targeted for departmental management (e.g., Debt Management department), and (c) single-stream for case workers on specific case types. | Must | Each user sees the appropriate dashboard level based on their role. Navigation between levels supported with contextual filtering preserved. Dashboard layout adapts to role-based permissions. | PDF §6.3 (Figures 9, 10, 11) | ITCAS role-based dashboard hierarchy. |  |

### 4.12.2 Operational Reports

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| RPT-FR-008 | The system shall generate a **Debt Aging Report** showing outstanding debt by age bands (0–30, 31–60, 61–90, 91–180, 181–365, >365 days) with sub-totals by TIN, tax type, and tax period; grand totals of (due – paid); sortable by office, tax type, period, sector, TIN, and amount descending. Report shall include instalment case dates and enforcement case dates where applicable. | Must | Report matches Table 18 specification. Age bands match system-calculated debt age using original due date. Sub-totals and grand totals verified against balances sourced from ORS. Comparison with previous year included. Instalment and enforcement dates displayed for active cases. | PDF §8.4.4 (Table 18) | ITCAS reporting module provides configurable debt aging reports. |  |
| RPT-FR-009 | The system shall generate a **Collection Status Report** showing for each active debt case: TIN, taxpayer name, debt category, current enforcement stage, assigned officer, days since last action, next scheduled action, and risk score. | Must | Report filterable by: debt category, enforcement stage, officer, office, tax type. Cases with overdue next-action highlighted. Sortable by any column. Export to Excel/CSV/PDF. | Vendor Proposal §6.8; PDF §5.2.2 | ITCAS CM reporting provides case status views. |  |
| RPT-FR-011 | The system shall generate an **Instalment Compliance Report** showing: number and value of active instalment agreements, compliance rate (% of taxpayers making timely payments), non-compliance actions triggered, and instalment agreements cancelled in the reporting period. | Must | Report displays: total active agreements, total value under instalment, on-time payment rate, late payments count/value, cancellations, and new enforcement cases from cancelled agreements. Filterable by tax type, debt category, and time period. | KPIs #26, #29, #30, #34; PDF §5.2.2 | ITCAS instalment monitoring subsumes this reporting. |  |
| RPT-FR-012 | The system shall generate a **Write-Off Report** listing all debts written off in the reporting period, grouped by: write-off reason (automatic C1, statutory expiry, management decision), tax type, taxpayer type, and amount. The report shall include cumulative write-off totals. | Should | Each write-off entry shows: TIN, taxpayer name, tax type, original debt amount, write-off amount, write-off date, reason, authorising officer (if manual). Totals by category. Year-to-date cumulative total. | PDF §5.2.2; Vendor Proposal §6.8.2 (items 23–25); DM-FR-041–046 | ITCAS write-off reporting. |  |
| RPT-FR-013 | The system shall generate a **Staff Productivity Report** showing for each debt management officer: cases assigned, cases resolved, total amounts recovered, average days to resolution, and cases breaching SLA. | Should | Report covers configurable time period. Metrics calculated from case audit trail. Comparison against team averages shown. Officers ranked by selected metric. Drill-down from officer to individual case list. | PDF §6.3 | ITCAS workforce analytics. |  |
| RPT-FR-014 | The system shall generate a **Tax Debt Status Report** showing: TIN, registration office, tax type, period, business type, sector, due amount, paid amount, with sub-totals by TIN, type, and period; and grand totals of (due – paid). Sortable by office, type, period, sector, TIN, and amount descending. | Must | Report matches Table 17 specification. All amount fields in Euro. Sub-totals accurate. Grand total verified against aggregate data sourced from ORS. | PDF §8.4.4 (Table 17) | ITCAS taxpayer accounting reports. |  |

### 4.12.3 Report Configuration and Management

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| RPT-FR-015 | The system shall provide an interactive report configuration interface allowing users to select: functional area (mandatory), tax type(s), tax period(s), tax year(s), registration type, business type, registration office, annual turnover range, and output format (screen, PDF, Excel, CSV). | Must | Configuration screen matches Table 7/13/16 delimiter specifications. Defaults applied per specification (e.g., tax type default = all). User can preview selection before execution. Saved configurations reusable. Report title configurable (max 200 characters). | PDF §8.5 (Tables 7, 13, 16) | ITCAS MIS configuration interface. |  |
| RPT-FR-016 | The system shall allow users to save report configurations for reuse, including all parameter settings, layout choices, and scheduling options. Saved configurations shall be shareable across authorised users within the same organisational unit. | Should | Configurations saved with: name, description, creation date, owner. Shared configurations visible to unit members. Version history maintained. Maximum 100 saved configurations per user. | PDF §8.1 | ITCAS report library. |  |
| RPT-FR-017 | The system shall support scheduled report generation and automated distribution via email to configured recipient lists. | Should | Reports scheduled at: daily, weekly, monthly, or custom intervals. Distribution lists configurable per report. Email includes report as attachment (PDF or Excel). Delivery confirmation logged. Failed deliveries retried and escalated. | PDF §8.1 | ITCAS automated reporting. |  |
| RPT-FR-018 | All reports and dashboards shall support export to: screen display, PDF, Excel (XLSX), and CSV formats. PDF exports shall include report header with MTCA logo, generation timestamp, and parameter summary. | Must | Export function available on every report and dashboard. PDF formatted with consistent MTCA branding. Excel export preserves formulas for subtotals. CSV export uses UTF-8 encoding with comma delimiter. | PDF §8.5 | ITCAS standard export formats. |  |
| RPT-FR-019 | All reports shall support drill-down navigation from summary levels to detailed transaction-level data, following the hierarchy: consolidated balance → tax type balance → period balance → individual transactions (data sourced from ORS/ClickHouse). | Must | Click on any aggregate figure navigates to the next level of detail. Drill-down path preserved in breadcrumb. User can navigate back to any level. Context filters maintained across drill-down levels. | PDF §3.3.1 (L1→L2→L3); §8.1 | ITCAS provides multi-level drill-down in all reports. |  |

### 4.12.4 Analytics

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| RPT-FR-020 | The system shall consume risk scores from SAS VIYA via REST API and display them alongside taxpayer and debt case data in dashboards and reports. | Must | Risk score (0–100) displayed on debtor list, case detail, and relevant reports. Scores refreshed at configurable intervals (default: daily). Score unavailability handled gracefully with "N/A" display and no system error. | SAS Strategic Alignment §2; CMP evaluation §6.2 | ITCAS compliance management integrates with SAS VIYA risk scoring. |  |
| RPT-FR-021 | The system shall support ad-hoc query capability allowing authorised analysts to construct custom reports using available data fields from the ORS/ClickHouse data platform, without requiring developer intervention. | Could | Query builder interface with: drag-and-drop field selection, filter conditions, grouping, sorting. Results displayable as table or basic chart. Custom queries saveable. Query execution within 30-second timeout. | PDF §8.1 | ITCAS MIS includes self-service analytics via Apache Superset. |  |

## 4.13 Workflow and Case Management

This section defines requirements for the workflow engine, case lifecycle management, work queue management, task routing, SLA monitoring, notifications, and document management capabilities provided by the Case Management Platform (the selected no-code/low-code platform).

> **Context:** Workflow and case management requirements are primarily driven by the debt management domain. The Case Management Platform executes DM workflows while reading taxpayer data from ORS/ClickHouse. All workflow definitions shall be exportable in BPMN 2.0 format for eventual migration to the ITCAS BPM engine.

### 4.13.1 Case Lifecycle Management

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-001 | The system shall support a configurable case lifecycle with the following standard states: New → Open → In Progress → On Hold → Pending Closure → Closed. Additional custom states shall be configurable per case type without code changes. | Must | Case state transitions governed by configurable rules. Each transition logged with: timestamp, user, previous state, new state, reason. Invalid transitions rejected with user notification. State diagram configurable by administrator. | Vendor Proposal §6.7; PDF §5.2.2 | ITCAS CM uses identical lifecycle states. BPMN 2.0 export enables migration. |  |
| WF-FR-002 | The system shall support the following case types for debt management: debt recovery, instalment agreement, write-off, default assessment, and enforcement action. Each case type shall have configurable attributes, workflows, and document templates. | Must | Each case type independently configurable with: required fields, optional fields, associated workflow, document templates, SLA parameters, escalation rules. New case types addable by administrator without code changes. Minimum 10 case types supported. | Vendor Proposal §6.7, §6.8 | ITCAS supports configurable case types. Case type definitions exportable via BPMN 2.0. |  |
| WF-FR-003 | The system shall support both automatic and manual case creation. Automatic creation shall be triggered by configurable business rules (e.g., debt exceeding threshold, missed payment, instalment default). Manual creation shall be available to authorised officers. | Must | Automatic case creation triggers configurable without code changes. Trigger conditions include: debt amount thresholds, debt age thresholds, payment events, instalment events. Manual creation includes mandatory fields validation. Duplicate case detection prevents creation of duplicate cases for same taxpayer/tax type/period combination. | Vendor Proposal §6.7; DM-FR-003, DM-FR-004 | ITCAS automatic and manual case creation. |  |
| WF-FR-004 | The system shall maintain a complete case history showing all activities, decisions, state changes, communications, and document generation events for each case, with timestamps and user identification. | Must | Case history displayed in chronological order. Each entry shows: date/time, user, action type, description, outcome. History is immutable (entries cannot be modified or deleted). History exportable to PDF for case review. | Vendor Proposal §6.7; PDF §5.2.2 | ITCAS case audit trail. |  |
| WF-FR-005 | The system shall support case re-opening when new information or events require further action on a previously closed case, subject to configurable business rules and authorisation levels. | Should | Re-open creates new case activity linked to original case. Re-open reason mandatory. Original case data and history preserved. Re-open requires minimum authorisation level configurable per case type. | Vendor Proposal §6.7 | ITCAS case re-open capability. |  |

### 4.13.2 Work Queue Management

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-006 | The system shall maintain work queues for each organisational unit and each case type, displaying cases awaiting action sorted by priority (based on risk score, debt amount, and debt age). | Must | Work queue shows: case ID, TIN, taxpayer name, debt amount, risk score, priority (High/Medium/Low), days in queue, SLA status. Default sort by priority descending, then by queue entry date ascending. Queue refreshes in real time. Filter by case type, debt category, SLA status. | PDF §5.2.2; Vendor Proposal §6.7 | ITCAS work list generation and prioritisation. |  |
| WF-FR-007 | The system shall support configurable rules for automatic case assignment to officers based on: workload balancing, officer specialisation (tax type expertise), debt category, geographic assignment, and round-robin distribution. | Must | Assignment rules configurable without code changes. Assignment considers current officer workload (open case count). Unassignable cases (no matching officer) routed to supervisor queue with alert. Assignment logged in case history. | Vendor Proposal §6.7; PDF §5.7 | ITCAS routing based on business rules. |  |
| WF-FR-008 | The system shall allow supervisors to manually reassign cases between officers, with mandatory reason capture and full audit trail. Bulk reassignment shall be supported for officer leave coverage or organisational changes. | Must | Single and bulk reassignment supported. Reason field mandatory. Original and new assignment recorded. Notification sent to both original and new assignee. Bulk reassignment supports selection by: officer, case type, debt category, and custom filter combinations. | Vendor Proposal §6.7; PDF §5.7 | ITCAS case reassignment. |  |
| WF-FR-009 | The system shall display a personal work list for each officer showing their assigned cases with: case ID, taxpayer name, debt amount, priority, next action due date, SLA countdown, and case status. | Must | Personal work list is the default landing page for case officers. Overdue actions highlighted in red. Actions due within 24 hours highlighted in amber. List sortable and filterable. Quick-action buttons for common activities (e.g., log call, schedule visit, generate letter). | PDF §6.3 (Figure 11) | ITCAS personal task list. |  |

### 4.13.3 SLA Monitoring

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-010 | The system shall support configurable SLA definitions per case type and debt category, specifying: target resolution time, escalation thresholds (warning at 75%, critical at 90% of SLA), and escalation actions (notification, reassignment, management alert). | Must | SLAs configurable in business days or calendar days. SLA clock pauses when case is On Hold (configurable). Multiple SLA levels per case (e.g., first response SLA, resolution SLA). SLA breach triggers configurable escalation action. | Vendor Proposal §6.7; PDF §5.1.2 | ITCAS SLA management framework. |  |
| WF-FR-011 | The system shall display SLA compliance status on all work queues, case lists, and management dashboards using traffic-light indicators: green (within SLA), amber (warning threshold reached), red (SLA breached). | Must | Traffic-light indicators update in real time. SLA countdown displayed in hours/days remaining. Breached cases persist as red until resolved. SLA compliance percentage displayed on management dashboards. | PDF §6.3 (Figures 9–11) | ITCAS SLA visualisation. |  |
| WF-FR-012 | The system shall automatically escalate cases that breach SLA thresholds by: sending notification to the assigned officer and their supervisor, increasing case priority, and optionally reassigning to a senior officer based on configurable rules. | Must | Escalation triggers at configurable threshold (default: 90% of SLA). Notification includes: case ID, taxpayer, debt amount, SLA status, days overdue. Escalation recorded in case history. Configurable maximum escalation levels (default: 3). | Vendor Proposal §6.7; PDF §5.1.2 | ITCAS automated escalation. |  |

### 4.13.4 Notifications and Alerts

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-013 | The system shall support multi-channel notification delivery to taxpayers via: email, SMS, in-app portal notification, and postal letter. Channel selection shall be configurable per notification type and taxpayer preference. | Must | Each notification type has configurable default channel(s). Taxpayer channel preference respected where available. Delivery status tracked per channel: sent, delivered, failed, bounced. Failed delivery triggers fallback to next configured channel. Postal letter generates print-ready PDF queued for dispatch. | Vendor Proposal §6.6.1; PDF §5.2.1 | ITCAS notification management subsystem. |  |
| WF-FR-014 | The system shall generate configurable notification templates for: payment reminders (pre-due-date), demand notices (1st and 2nd), instalment payment reminders, instalment default notices, enforcement escalation notices, and tax account statements. Templates shall support Maltese and English languages. | Must | Templates configurable by administrator with: merge fields (TIN, name, amounts, dates), static text, MTCA branding. Each template available in Maltese and English. Template versioning with effective dating. Preview function before deployment. | Vendor Proposal §6.6.1, §6.8; PDF §5.2.1, §5.3 | ITCAS document template management (ADG). |  |
| WF-FR-015 | The system shall send internal alerts to MTCA officers for: new case assignment, SLA warning/breach, instalment default detection, high-risk taxpayer activity, and supervisor approval requests. Internal alerts shall appear in the officer's in-app notification panel and optionally via email. | Must | In-app notification panel shows unread count badge. Alerts categorised by type and priority. Alert preferences configurable per officer (which alerts, which channels). Read/unread status tracked. Alert history retained for 90 days minimum. | Vendor Proposal §6.7 | ITCAS internal notification framework. |  |
| WF-FR-016 | The system shall log all outbound notifications with: recipient, channel, content summary, send timestamp, delivery status, and any associated case reference. Notification logs shall be searchable and included in case history. | Must | Notification log entry created for every outbound communication. Failed notifications flagged for manual review. Notification linked to originating case. Log searchable by: TIN, date range, channel, status. Retention period: 7 years minimum. | Vendor Proposal §6.6.1 | ITCAS communication audit trail. |  |

### 4.13.5 Document Management

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-017 | The system shall support automated document generation (ADG) for all standard debt management documents: demand notices, judicial letters, instalment agreements, enforcement orders, write-off approvals, and tax account statements. Documents shall be generated from configurable templates populated with case and taxpayer data. | Must | Documents generated in PDF format. Templates support: merge fields, conditional sections, MTCA letterhead, digital signatures (where applicable). Generated documents automatically attached to case record. Batch generation supported for bulk operations (e.g., monthly demand notices). | PDF §5.2.2 (Figure 8); Vendor Proposal §6.7, §6.8 | ITCAS ADG module provides automated document generation. |  |
| WF-FR-018 | The system shall provide an electronic document store (EDS) for case-related documents, supporting: upload of scanned documents, attachment of generated documents, document categorisation, version control, and search by document type, case, or taxpayer. | Must | Documents stored with: metadata (type, date, case ref, TIN), version number, upload user. Maximum file size: 25 MB. Supported formats: PDF, DOCX, XLSX, JPG, PNG. Full-text search across document metadata. Documents accessible from case view and taxpayer view. | PDF §5.2.2 (Figure 8); Vendor Proposal §6.7 | ITCAS EDS provides enterprise document management. |  |
| WF-FR-019 | The system shall track postal delivery of physical documents, recording: dispatch date, delivery method, delivery confirmation (where available), and returned mail. Undeliverable mail shall trigger an alert for address verification. | Should | Postal dispatch recorded in notification log and case history. Delivery confirmation updateable manually or via postal service integration. Returned mail flagged on taxpayer record. Address verification task auto-created for returned mail. | PDF §5.2.1; Vendor Proposal §6.8 | ITCAS postal delivery tracking. |  |

### 4.13.6 Audit Trail

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| WF-FR-020 | The system shall maintain an immutable, tamper-evident audit trail for all case management activities including: case creation, state transitions, data modifications, document generation, notifications sent, user logins, and configuration changes. | Must | Audit trail entries include: timestamp (UTC), user ID, action, entity affected, before/after values for data changes, IP address. Entries cannot be modified or deleted by any user including administrators. Audit trail queryable by: date range, user, action type, entity. Retention period: 10 years minimum. | PDF §5.2.2; Vendor Proposal §6.7 | ITCAS comprehensive audit logging. |  |

## 4.14 Integration Requirements

This section defines requirements for system integrations between the Case Management Platform and its data sources, analytical platforms, legacy systems, future systems, and external services.

> **Architectural context:** The DM Case Management Platform is an independent application that reads taxpayer accounting data from ORS/ClickHouse (sourced from ORS). It does not connect directly to legacy Informix databases. All workflow state and case data are stored in the platform's own database. This architecture supports clean migration to ITCAS when it becomes the transactional system of record.

### 4.14.1 ORS/ClickHouse Integration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-001 | The system shall read taxpayer account data (consolidated balance, tax type balances, transaction detail — sourced from ORS/ClickHouse) via RESTful API endpoints, with query response time <2 seconds for single-taxpayer lookups. | Must | API endpoints available for: consolidated balance by TIN, tax type balances by TIN, transaction history by TIN/tax type/period. Response includes: all taxpayer account data fields as specified in the STA view in ORS. Single-taxpayer query returns within 2 seconds under normal load. API authentication via OAuth 2.0 or API key. | ORS Implementation Plan §Technical Architecture; STA view in ORS | ITCAS taxpayer accounting replaces ORS as data source. API contract preserved. |  |
| INT-FR-002 | The system shall read debt aging and debt portfolio data from ORS/ClickHouse via aggregation queries, with response time <5 seconds for portfolio-level analytics (e.g., total debt by age band across all taxpayers). | Must | Aggregation queries supported for: debt by age band, debt by tax type, debt by category (C1–C5), debt by enforcement stage. Materialized views in ClickHouse optimised for these access patterns. Results cached with configurable TTL (default: 15 minutes). | ORS Implementation Plan §ClickHouse Core; RPT-FR-001, RPT-FR-008 | ITCAS analytical data warehouse. |  |
| INT-FR-003 | The system shall support both real-time query access (via GraphQL or REST) and batch data synchronisation (via scheduled ETL) from ORS/ClickHouse, depending on the data freshness requirements of each functional area. | Must | Real-time access for: taxpayer account lookup, balance check, transaction history. Batch sync for: reporting data marts, KPI calculation, debtors list refresh. Batch sync frequency configurable per data set (minimum: every 15 minutes for hot data). Data freshness indicator displayed on all reports and dashboards. | ORS Implementation Plan §Consumers Layer | ITCAS provides transactional data in real time. |  |
| INT-FR-004 | The system shall handle ORS/ClickHouse unavailability gracefully, displaying cached data with a "data as of [timestamp]" indicator and queuing write operations for retry when connectivity is restored. | Must | System remains functional with read-only cached data during ORS outage. Cache validity period configurable (default: 4 hours). User alerted when viewing stale data. Write operations (case updates, workflow actions) continue to function using local platform storage. Automatic reconciliation upon ORS reconnection. | ORS Implementation Plan §High Availability | ITCAS high-availability architecture. |  |

### 4.14.2 SAS VIYA Integration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-005 | The system shall consume taxpayer risk scores from SAS VIYA via REST API, including: overall risk score (0–100), risk category (High/Medium/Low), risk factors contributing to the score, and score calculation timestamp. | Must | Risk score API called on: case creation, case view, debtors list generation. Response parsed and stored locally for display. Score refresh frequency: configurable (default: daily batch + on-demand). API failure handled gracefully — system displays last known score with timestamp. | SAS Strategic Alignment §2, §5; CMP Evaluation §6.2 | ITCAS compliance management integrates with SAS VIYA. API contract preserved. |  |
| INT-FR-006 | The system shall send case outcome data to SAS VIYA to enable model refinement, including: enforcement actions taken, resolution outcome (paid, instalment, write-off), time to resolution, and recovery amount as percentage of original debt. | Should | Outcome data pushed via REST API or batch file (Parquet format) at configurable frequency (default: weekly). Data schema agreed between DM and SAS VIYA teams. Successful delivery confirmed via API response. Failed deliveries logged and retried. | SAS Strategic Alignment §5 (Credit Collection Module) | ITCAS feeds case outcomes to SAS for model improvement. |  |
| INT-FR-007 | The system shall support invoking SAS VIYA predictive models on demand for: payment likelihood prediction, optimal enforcement action recommendation, and instalment default probability, displaying results within the case management interface. | Could | Model invocation via REST API with taxpayer context data. Response displayed in case detail panel as advisory information (not auto-actioned). Response time <5 seconds. Model unavailability does not block case processing. | SAS Strategic Alignment §5 (New Analytical Capabilities) | ITCAS advanced analytics integration. |  |

### 4.14.3 Legacy Informix Coexistence

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-008 | The system shall coexist with legacy Informix/PowerBuilder applications during the transition period, ensuring that data changes made in legacy systems are reflected in ORS/ClickHouse within the configured freshness tier: <5 minutes for hot data (payments, assessments), <1 hour for warm data, <24 hours for cold data. | Must | Data freshness verified against ORS ingestion monitoring (Grafana dashboard). The Case Management Platform does not connect directly to Informix — all legacy data accessed via ORS/ClickHouse. Data inconsistencies between legacy and ORS flagged via automated reconciliation alerts. | Informix ClickHouse Ingestion Architecture §Phase 3; STA view in ORS | ITCAS replaces legacy Informix systems. Coexistence ends at ITCAS go-live. |  |
| INT-FR-009 | The system shall NOT write data back to legacy Informix databases. All workflow state, case data, and configuration shall be stored in the Case Management Platform's own database, with ORS/ClickHouse serving as the read-only data source for taxpayer accounting data. | Must | Architecture enforces read-only access to ORS/ClickHouse for taxpayer data. Platform database stores: cases, workflow state, notifications, documents, configuration. No JDBC or ODBC write connections to Informix. Data ownership boundaries documented. | Project Brief §Key Constraints; ORS Implementation Plan §Architectural Advantages | ITCAS becomes the transactional system of record. Platform database migrated to ITCAS. |  |

### 4.14.4 ITCAS Future Migration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-010 | All workflow definitions created in the Case Management Platform shall be exportable in BPMN 2.0 standard format, enabling migration to the ITCAS BPM engine without requiring workflow redesign from scratch. | Must | BPMN 2.0 export function available for all workflow definitions. Exported files validate against BPMN 2.0 XML schema. Export includes: process flows, decision points, timer events, escalation rules, user tasks, and service tasks. Round-trip tested: export from platform → import to reference BPMN tool → visual equivalence verified. | Project Brief §Key Constraints; CMP Justification §F.8 | ITCAS BPMN 2.0 compliant BPM engine imports exported workflows. |  |
| INT-FR-011 | The system shall be designed with a clean separation between: (a) business logic (rules, workflows, calculations), (b) data access (ORS/ClickHouse queries), and (c) presentation (UI/dashboards), to facilitate component-by-component migration to ITCAS. | Must | Architecture follows MVC or equivalent separation pattern. Data access layer uses abstraction (e.g., service interfaces) that can be re-pointed from ORS to ITCAS PostgreSQL. Business rules stored in decision tables exportable as spreadsheets or DMN format. Migration impact assessment producible from system documentation. | Project Brief §Key Constraints; ITCAS Vendor Technical Context §6.1 | ITCAS absorbs business logic and data access. Presentation layer replaced by ITCAS UI. |  |
| INT-FR-012 | The system shall support parallel operation with ITCAS during the migration period, enabling side-by-side comparison of case processing outcomes to validate ITCAS configuration before full cutover. | Should | Parallel mode routes identical case triggers to both systems. Comparison reports highlight discrepancies in: case assignment, SLA calculation, notification timing, enforcement action selection. Configurable to run for selected case types only. No duplicate taxpayer communications sent during parallel operation. | ITCAS Vendor Technical Context §6.1; PDF OQ-12 | ITCAS migration strategy includes parallel run validation. |  |

### 4.14.5 External System Integration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-013 | The system shall support integration with commercial banks for automated bank account garnishment (freezing) requests, via secure web services or file-based exchange as supported by Maltese banking infrastructure. | Should | Garnishment request generated from enforcement case. Request includes: TIN, taxpayer name, bank account identifier, amount to freeze, legal authority reference. Response (confirmation/rejection) processed and recorded in case history. Secure channel (TLS 1.2+ minimum). | PDF §7.1 (item 7); Vendor Proposal §6.8 | ITCAS bank integration for garnishment and payment processing. |  |
| INT-FR-014 | The system shall support lookup of taxpayer assets via integration with the Land/Property Registry and Motor Vehicle Registry to support enforcement action planning (lien on assets, property seizure). | Could | Registry lookup triggered from enforcement case. Results displayed in case: property details, vehicle details, estimated values. Lookup via API or secure file exchange. Results cached locally with timestamp. Registry unavailability does not block case processing. | PDF §7.1 (items 9–12); Vendor Proposal §6.8 | ITCAS external authority integration. |  |
| INT-FR-015 | The system shall integrate with MTCA's taxpayer portal to enable taxpayers to: view their tax account statement (data sourced from ORS), submit instalment applications, and view the status of their debt management cases. Portal integration shall be read-only for taxpayer-facing account data and write-enabled for instalment applications only. | Should | Portal displays: balance summary sourced from ORS, transaction history, active cases (status only, no internal notes). Instalment application form submittable via portal → creates case in platform. Portal authentication via existing MTCA/MITA SSO. Data refresh frequency: near-real-time for balance, daily for case status. | PDF §3.3.3, §6.2; Vendor Proposal §6.8.2 | ITCAS taxpayer portal replaces interim portal integration. |  |
| INT-FR-016 | The system shall integrate with MITA's single sign-on (SSO) infrastructure using OAuth 2.0 or SAML 2.0 protocols for MTCA staff authentication, ensuring users do not need separate credentials for the Case Management Platform. | Must | SSO integration tested and operational. User roles and permissions synchronised from MITA directory service. Session timeout configurable (default: 30 minutes of inactivity). SSO failure falls back to local authentication with alert to administrator. | CMP Evaluation §5.2; MITA governance requirements | ITCAS uses same MITA SSO infrastructure. |  |

### 4.14.6 Notification Channel Integration

| **ID** | **Description** | **Priority** | **Acceptance Criteria** | **Source** | **ITCAS Alignment** |  |
| --- | --- | --- | --- | --- | --- | --- |
| INT-FR-017 | The system shall integrate with an email service (SMTP or API-based) for delivery of notifications, reports, and document attachments to taxpayers and MTCA staff. | Must | Email delivery via configurable SMTP server or email API (e.g., MITA email gateway). Support for: HTML and plain-text email, attachments up to 10 MB, delivery receipt tracking. Bounce handling: bounced emails logged, taxpayer email marked as undeliverable after 3 consecutive bounces. | WF-FR-013; Vendor Proposal §6.6.1 | ITCAS email integration. |  |
| INT-FR-018 | The system shall integrate with an SMS gateway for delivery of short notification messages (payment reminders, SLA alerts) to taxpayers who have opted in to SMS communications. | Should | SMS via configurable gateway API. Message length: ≤160 characters (single SMS) or ≤480 characters (concatenated). Delivery status tracked: sent, delivered, failed. Opt-in/opt-out managed per taxpayer record. SMS content logged in notification history. | WF-FR-013; PDF §5.2.1 | ITCAS SMS integration. |  |

## External Dependencies Summary

The following external systems are referenced by requirements in this section:

| **External System** | **Referenced By** | **Integration Type** |  |
| --- | --- | --- | --- |
| ORS/ClickHouse | INT-FR-001–004, RPT-FR-001–002, RPT-FR-008, RPT-FR-014, RPT-FR-019, RPT-FR-021 | REST/GraphQL API (real-time), ETL (batch) |  |
| SAS VIYA | INT-FR-005–007, RPT-FR-020 | REST API (risk scores, predictions, outcome feedback) |  |
| Legacy Informix | INT-FR-008–009 | Indirect via ORS only (read-only) |  |
| ITCAS (future) | INT-FR-010–012 | BPMN 2.0 export, parallel run, API contract migration |  |
| Commercial Banks | INT-FR-013 | Web services / file exchange (garnishment) |  |
| Land/Property Registry | INT-FR-014 | API / file exchange (asset lookup) |  |
| Motor Vehicle Registry | INT-FR-014 | API / file exchange (asset lookup) |  |
| MTCA Taxpayer Portal | INT-FR-015 | Read-only data feed + instalment application intake |  |
| MITA SSO | INT-FR-016 | OAuth 2.0 / SAML 2.0 |  |
| MITA Email Gateway | INT-FR-017 | SMTP / email API |  |
| SMS Gateway | INT-FR-018 | SMS API |  |

# Section 5: Platform Capability Requirements

## 5.0 Overview

This section specifies the platform capability requirements for the Case Management Platform (CMP) supporting the Debt Management module. These requirements define the technical capabilities that the selected no-code/low-code platform must provide to enable the DM functional requirements documented in Section 4.

All requirements in this section are platform-agnostic — they define *what* the platform must do, not *how* any specific product implements it. The CMP must be a market-validated commercial off-the-shelf (COTS) platform with demonstrated capability in government case management deployments.

**Requirement Summary:**

| **Subsection** | **Prefix** | **Count** | **Must** | **Should** |  |
| --- | --- | --- | --- | --- | --- |
| 5.1 Forms and Workflow Configuration | CFW | 45 | 43 | 2 |  |
| 5.2 Workflow Engine | WF | 50 | 49 | 1 |  |
| 5.3 Business Rules Engine | BR | 15 | 15 | 0 |  |
| 5.4 Forms Management | FM | 15 | 15 | 0 |  |
| 5.5 Data Management | DM | 12 | 12 | 0 |  |
| 5.6 Events and Triggers | EC | 10 | 10 | 0 |  |
| 5.7 Process Monitoring | PM | 10 | 10 | 0 |  |
| 5.8 Platform Integration | INT | 20 | 17 | 3 |  |
| 5.9 Platform Architecture | OA | 10 | 9 | 1 |  |
| 5.10 General Platform Requirements | GN | 10 | 10 | 0 |  |
| 5.11 DM Operational Metrics | PRM (subset) | 9 | 8 | 1 |  |
| **Total** |  | **206** | **198** | **8** |  |

## 5.1 Forms and Workflow Configuration (CFW.xxx)

This subsection specifies requirements for the platform's form and workflow configuration capabilities, enabling MTCA staff to define document forms, processing rules, and case workflows without programming.

**DM Use Case Mapping:** These capabilities directly enable UC.DM.001 (debt case identification and tracking), UC.DM.002 (demand notice generation), and UC.DM.003 (instalment agreement processing).

### 5.1.1 Form and Workflow Definition

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CFW.021 | Must | The platform SHALL enable configuration of work queues for case assignment with priority and load balancing. |
| CFW.022 | Must | The platform SHALL support configurable list views with: (a) configurable attribute columns, and (b) configurable access rights. |
| CFW.023 | Must | The platform SHALL enable automatic processing of submitted documents using configurable business rules. |
| CFW.024 | Must | The platform SHALL support bulk operations on document lists including bulk status updates and reassignment. |
| CFW.025 | Must | The platform SHALL enable configuration of document list filtering, sorting, and search capabilities. |
| CFW.026 | Must | The platform SHALL support printing of individual documents and document lists. |
| CFW.027 | Must | The platform SHALL support document export and import in XML, JSON, PDF, Excel, and CSV formats. |
| CFW.028 | Must | The platform SHALL provide web service APIs for automated document submission with data quality validation. |

### 5.1.4 Correspondence and Templates

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CFW.029 | Must | The platform SHALL enable configuration of letter templates with placeholder fields populated from case data. |
| CFW.030 | Must | The platform SHALL support bilingual correspondence generation in Maltese and English. |
| CFW.031 | Must | The platform SHALL enable batch letter generation for bulk correspondence campaigns. |
| CFW.032 | Must | The platform SHALL support electronic delivery of correspondence via email with tracking. |
| CFW.033 | Must | The platform SHALL enable configuration of correspondence approval workflows before issuance. |
| CFW.034 | Should | The platform SHOULD support integration with government postal services for physical mail tracking. |
| CFW.035 | Must | The platform SHALL maintain correspondence history linked to cases with version tracking. |

### 5.1.5 Audit Trail and History

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CFW.036 | Must | The platform SHALL maintain comprehensive audit trail for all form submissions, workflow transitions, and user actions. |
| CFW.037 | Must | The platform SHALL record username, date, time, IP address, and device for all audited events. |
| CFW.038 | Must | The platform SHALL support audit trail queries by case, user, date range, and action type. |
| CFW.039 | Must | The platform SHALL retain audit trail data for minimum 10 years per government retention requirements. |
| CFW.040 | Must | The platform SHALL prevent modification or deletion of audit trail records. |

### 5.1.6 Multi-Language Support

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CFW.041 | Must | The platform SHALL support form and workflow configuration in both Maltese and English. |
| CFW.042 | Must | The platform SHALL enable users to switch interface language without losing session context. |
| CFW.043 | Must | The platform SHALL support localised field labels, validation messages, and help text. |
| CFW.044 | Must | The platform SHALL support date, number, and currency formatting per Maltese conventions. |
| CFW.045 | Should | The platform SHOULD support future addition of third languages without system modification. |

## 5.2 Workflow Engine (WF.xxx)

This subsection specifies requirements for the platform's workflow management engine, encompassing process modelling, task assignment, flow control, scheduling, error handling, and case management.

**DM Use Case Mapping:** The workflow engine is the central orchestration layer enabling all DM use cases. It directly powers UC.DM.001 (automated case identification workflows), UC.DM.002 (notice generation workflows with timer-based triggers), UC.DM.003 (multi-stage instalment approval workflows), UC.DM.004 (risk-based case routing), and UC.DM.007 (escalation-driven enforcement workflows).

### 5.2.1 Process Modelling

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.001 | Must | The platform SHALL support workflow design using BPMN 2.0 notation or equivalent visual standard. |
| WF.002 | Must | The platform SHALL provide a graphical Workflow Editor with drag-and-drop process design. |
| WF.003 | Must | The platform SHALL support process types: (a) Synchronous — blocking caller, (b) Asynchronous — background execution, and (c) Manual — human task-based. |
| WF.004 | Must | The platform SHALL support grouping of workflows into hierarchical parent-child structures. |
| WF.005 | Must | The platform SHALL support workflow decomposition with sub-process invocation. |
| WF.006 | Must | The platform SHALL enable definition of process start events (manual trigger, scheduled, event-driven). |
| WF.007 | Must | The platform SHALL support multiple end events with distinct outcomes and routing. |
| WF.008 | Must | The platform SHALL enable workflow documentation with annotations and descriptions. |

### 5.2.2 Task and Assignment

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.018 | Must | The platform SHALL support exclusive gateways (XOR) routing to single path based on conditions. |
| WF.019 | Must | The platform SHALL support parallel gateways (AND) enabling concurrent execution paths. |
| WF.020 | Must | The platform SHALL support inclusive gateways (OR) routing to one or more paths based on conditions. |
| WF.021 | Must | The platform SHALL support event-based gateways waiting for multiple possible events. |
| WF.022 | Must | The platform SHALL support complex gateway logic with custom condition expressions. |
| WF.023 | Must | The platform SHALL support loop patterns for iterative processing. |

### 5.2.4 Timer and Scheduling

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.024 | Must | The platform SHALL support timer start events triggering workflows on schedule. |
| WF.025 | Must | The platform SHALL support timer intermediate events creating delays or deadlines. |
| WF.026 | Must | The platform SHALL support timer boundary events triggering escalation on task timeout. |
| WF.027 | Must | The platform SHALL support calendar-aware scheduling respecting Maltese public holidays. |
| WF.028 | Must | The platform SHALL support business hours configuration for SLA calculations. |
| WF.029 | Must | The platform SHALL support cron-style schedule expressions for recurring triggers. |

### 5.2.5 Error Handling

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.030 | Must | The platform SHALL support error boundary events capturing workflow exceptions. |
| WF.031 | Must | The platform SHALL support error sub-processes for exception handling logic. |
| WF.032 | Must | The platform SHALL support compensation handlers for rollback scenarios. |
| WF.033 | Must | The platform SHALL support retry logic with configurable attempt limits and delays. |
| WF.034 | Must | The platform SHALL log all workflow errors with diagnostic information. |

### 5.2.6 Case Management Specifics

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.035 | Must | The platform SHALL support case lifecycle management from creation through resolution to archive. |
| WF.036 | Must | The platform SHALL support milestone tracking within case workflows. |
| WF.037 | Must | The platform SHALL support activity scheduling and tracking within cases. |
| WF.038 | Must | The platform SHALL trigger business rules on case events: (a) lifecycle, (b) milestone, (c) activity, (d) data, (e) document, (f) comment, and (g) user events. |
| WF.039 | Must | The platform SHALL support case templates with pre-configured workflow patterns. |
| WF.040 | Must | The platform SHALL enable case correlation linking related cases. |

### 5.2.7 Notification and Communication

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.041 | Must | The platform SHALL support email notification configuration for workflow events. |
| WF.042 | Should | The platform SHOULD support SMS notification for urgent alerts. |
| WF.043 | Must | The platform SHALL support in-application notification to user task lists. |
| WF.044 | Must | The platform SHALL support notification templates with data placeholders. |
| WF.045 | Must | The platform SHALL support notification recipient determination by role, group, or expression. |

### 5.2.8 Collaboration and Versioning

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| WF.046 | Must | The platform SHALL support workflow versioning with ability to run multiple versions concurrently. |
| WF.047 | Must | The platform SHALL support workflow comparison showing differences between versions. |
| WF.048 | Must | The platform SHALL support workflow migration moving in-flight instances to new versions. |
| WF.049 | Must | The platform SHALL support workflow commenting and annotation for collaboration. |
| WF.050 | Must | The platform SHALL support workflow export in BPMN 2.0 XML format. |

## 5.3 Business Rules Engine (BR.xxx)

This subsection specifies requirements for the platform's business rules management capabilities, supporting configurable rule definition, testing, and lifecycle management.

**DM Use Case Mapping:** The business rules engine directly supports UC.DM.001 (debt case identification criteria), UC.DM.002 (demand notice trigger rules and escalation thresholds), UC.DM.004 (risk-based prioritisation rules consuming SAS VIYA scores), and UC.DM.007 (enforcement escalation rules).

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.4 Forms Management (FM.xxx)

This subsection specifies requirements for the platform's forms management capabilities, enabling visual form design, data binding, validation, and multi-channel rendering.

**DM Use Case Mapping:** Forms management directly enables UC.DM.003 (instalment agreement forms with complex validation), UC.DM.002 (demand notice templates), and UC.DM.005 (management dashboard reporting forms).

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.5 Data Management (DM.xxx)

This subsection specifies requirements for the platform's data management capabilities, including business object definition, data relationships, external data integration, and historization.

**DM Use Case Mapping:** Data management is foundational to all DM use cases, particularly UC.DM.001 (debt case entity definition with ORS integration), UC.DM.006 (real-time debt portfolio visibility from ORS/ClickHouse), and UC.DM.003 (instalment agreement data objects).

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.6 Events and Triggers (EC.xxx)

This subsection specifies requirements for the platform's event processing capabilities, enabling event detection, correlation, temporal processing, and workflow triggering.

**DM Use Case Mapping:** Event capabilities are critical for UC.DM.001 (automatic debt case creation triggered by payment overdue events from ORS), UC.DM.002 (timer-driven demand notice generation), and UC.DM.007 (escalation triggers based on enforcement action deadlines).

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.7 Process Monitoring (PM.xxx)

This subsection specifies requirements for the platform's process monitoring capabilities, providing operational visibility into workflow execution, case throughput, and performance metrics.

**DM Use Case Mapping:** Process monitoring directly supports UC.DM.005 (automated reporting and management dashboard updates, ~1.8 FTE liberation at 90% automation) and provides the operational metrics foundation for all DM case management activities.

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.8 Platform Integration (INT.xxx)

This subsection specifies requirements for the platform's integration capabilities with ITCAS Core, ORS/ClickHouse, SAS VIYA, and external systems.

**DM Use Case Mapping:** Integration requirements are essential for UC.DM.004 (SAS VIYA risk score consumption), UC.DM.006 (ORS/ClickHouse real-time debt portfolio visibility), and UC.DM.001 (ITCAS Core taxpayer data access for debt case creation).

### 5.8.1 ITCAS Core Integration

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| INT.001 | Must | The platform SHALL integrate with ITCAS Core via REST APIs for taxpayer data access. |
| INT.002 | Must | The platform SHALL support OAuth 2.0 / OpenID Connect authentication for API access. |
| INT.003 | Must | The platform SHALL support API rate limiting configurable per client application. |
| INT.004 | Must | The platform SHALL provide comprehensive API documentation (OpenAPI/Swagger format). |
| INT.005 | Must | The platform SHALL support API versioning enabling backward compatibility. |

### 5.8.2 ORS/ClickHouse Integration

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| INT.006 | Must | The platform SHALL connect to ORS/ClickHouse for debt portfolio data access. |
| INT.007 | Must | The platform SHALL support real-time data refresh from ORS dashboards. |
| INT.008 | Must | The platform SHALL integrate with Taxpayer 360° view for unified customer profile. |
| INT.009 | Must | The platform SHALL publish case status updates to ORS for dashboard visibility. |
| INT.010 | Must | The platform SHALL support configurable data refresh frequencies. |

### 5.8.3 SAS VIYA Integration

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| INT.011 | Must | The platform SHALL receive risk scores from SAS VIYA via REST API. |
| INT.012 | Must | The platform SHALL support batch import of risk scoring results (hourly frequency). |
| INT.013 | Must | The platform SHALL support real-time risk score lookup for case prioritisation. |
| INT.014 | Must | The platform SHALL support confidence score integration for risk decisions. |
| INT.015 | Should | The platform SHOULD support case recommendation integration from SAS VIYA. |

### 5.8.4 External System Integration

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| INT.021 | Should | The platform SHOULD support integration with banking systems for payment verification. |
| INT.022 | Should | The platform SHOULD support integration with government registries (business, persons). |
| INT.023 | Must | The platform SHALL support SMTP email integration for correspondence delivery. |
| INT.024 | Must | The platform SHALL support LDAP/Active Directory integration for user authentication. |
| INT.025 | Must | The platform SHALL support webhook configuration for external system notification. |

## 5.9 Platform Architecture (OA.xxx)

This subsection specifies requirements for the platform's configuration management, deployment pipeline, and version control architecture.

**DM Use Case Mapping:** Architecture requirements are cross-cutting, ensuring that all DM workflows, forms, and rules can be developed, tested, and deployed reliably. They are particularly important for the phased DM deployment (Phase 2: Q3–Q4 2026) where configuration changes must be managed across development, test, and production environments.

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.10 General Platform Requirements (GN.xxx)

This subsection specifies the overarching platform requirements ensuring the Case Management Platform provides integrated BPM, forms, rules, and event capabilities with a strong emphasis on business user configurability.

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
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

## 5.11 DM Operational Metrics (PRM.xxx — Subset)

This subsection includes the Performance Management requirements relevant to Debt Management operations. Per extraction rule #29, only PRM.006–PRM.010 (DM operational metrics) and PRM.016–PRM.019 (FTE liberation tracking) are included.

**DM Use Case Mapping:** These metrics directly support UC.DM.005 (automated reporting and management dashboard updates) and provide the measurement framework for tracking progress toward the ~8–10 FTE liberation target.

### 5.11.1 KPI Metrics and Calculation

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| PRM.006 | Must | The platform SHALL calculate workflow throughput metrics (cases processed per period, per user). |
| PRM.007 | Must | The platform SHALL calculate cycle time metrics (average case resolution time by case type). |
| PRM.008 | Must | The platform SHALL calculate quality metrics (error rates, rework rates, first-time-right percentages). |
| PRM.009 | Must | The platform SHALL support custom KPI definition based on configurable formulas. |
| PRM.010 | Must | The platform SHALL calculate SLA compliance metrics for service request resolution. |

### 5.11.2 FTE Liberation Tracking Alignment

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| PRM.016 | Must | The platform SHALL track automation rates showing percentage of cases processed without manual intervention. |
| PRM.017 | Must | The platform SHALL measure task completion times enabling pre/post automation comparison. |
| PRM.018 | Must | The platform SHALL report on freed capacity metrics aligned with the 50 FTE liberation programme. |
| PRM.019 | Should | The platform SHOULD integrate with the Resource Liberation tracking dashboard defined in Sub-task 3.3. |

## 5.12 FTE Liberation Cross-Reference Matrix

The following matrix maps DM use cases and their FTE liberation targets to the platform capability areas that enable them.

| **UC ID** | **Use Case** | **FTE Target** | **Automation** | **Primary Platform Capabilities** |  |
| --- | --- | --- | --- | --- | --- |
| UC.DM.001 | Automated identification and tracking of debt cases from ORS debt portfolio data | ~2.7 FTE | 90% | EC (event triggers), WF (case creation workflows), DM (business objects), INT.006–010 (ORS integration) |  |
| UC.DM.002 | Automated generation of demand notices based on configurable templates and triggers | ~1.9 FTE | 95% | CFW (templates, correspondence), WF (timer workflows), BR (trigger rules), FM (notice forms) |  |
| UC.DM.003 | Instalment agreement processing with configurable approval workflows | ~1.4 FTE | 70% | WF (approval workflows), FM (agreement forms), BR (validation rules), CFW (document handling) |  |
| UC.DM.004 | Risk-based case prioritisation using SAS VIYA risk scores | ~1.3 FTE | 85% | INT.011–015 (SAS VIYA), BR (prioritisation rules), WF (routing), PM (monitoring) |  |
| UC.DM.005 | Automated reporting and management dashboard updates | ~1.8 FTE | 90% | PM (dashboards, KPIs), PRM.006–010 (operational metrics), PRM.016–019 (FTE tracking) |  |
| UC.DM.006 | Integration with ORS/ClickHouse for real-time debt portfolio visibility | — | — | INT.006–010 (ORS integration), DM.010–011 (data source access) |  |
| UC.DM.007 | Automated enforcement action tracking with configurable escalation rules | ~0.9 FTE | 60% | WF (escalation workflows), EC (timer events), BR (escalation rules), CFW (enforcement forms) |  |
|  | **Total** | **~8–10 FTE** |  |  |  |

## 5.13 External Dependencies

The platform capability requirements in this section assume the availability of the following external systems:

| **System** | **Dependency Type** | **Capabilities Required** |
| --- | --- | --- |
| ORS/ClickHouse | Data source (read) + status publish (write) | Debt portfolio views, Taxpayer 360° view, dashboard visibility |
| SAS VIYA | Data source (read) | Risk scores, confidence scores, case recommendations |
| ITCAS Core | API integration | Taxpayer data, tax account data, assessment data |
| Legacy Informix databases | Data source (read-only) | Historical debt data, reference data during transition period |
| LDAP/Active Directory | Authentication | User authentication, role mapping |
| SMTP mail server | Correspondence delivery | Email notification, letter delivery tracking |
| Government postal services | Optional integration | Physical mail tracking (CFW.034) |
| Banking systems | Optional integration | Payment verification (INT.021) |
| Government registries | Optional integration | Business and persons registry lookup (INT.022) |

# 6. Non-Functional Requirements

This section defines measurable non-functional requirements for the Debt Management system covering performance, availability, scalability, security, usability, data quality, auditability, maintainability, and portability. Requirements are drawn from two sources and merged where both specify the same concern, retaining the more specific or stricter metric.

**Source key:**

- **S1** — STA/DM Requirements Specification (Section 5)

- **CMP** — Case Management Platform Requirements (Groups G01–G12)

- **Merged** — requirement synthesised from both sources; stricter target retained

## 6.1 Performance

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-001 | Data lookups (debt case details, taxpayer balance retrieval from ORS/ClickHouse) shall complete within the specified response time under normal operational load. | Must | Response time ≤2 seconds (95th percentile) at ≤150 concurrent users | S1 NFR-001 (adapted: STA→ORS) |
| NFR-002 | Workflow actions (case state transition, task assignment, form submission) shall complete within the specified response time. | Must | Response time ≤3 seconds for standard operations; ≤5 seconds for complex multi-step transitions (95th percentile) | Merged: S1 NFR-002 (≤5s) + CMP G08.002 (≤3s for standard ops); CMP target retained for standard operations |
| NFR-003 | Dashboard and report rendering (including data retrieval from ORS/ClickHouse) shall complete within the specified response time. | Must | Initial load ≤10 seconds; refresh ≤5 seconds (95th percentile); cached queries ≤1 second | Merged: S1 NFR-003 + CMP G08.003 (≤10s for complex reports); targets aligned |
| NFR-004 | Batch operations (bulk case creation, mass notification generation, scheduled report generation) shall process within the specified throughput rate. | Should | ≥500 cases per minute for bulk operations; ≥1,000 notifications per hour | S1 NFR-004 |
| NFR-005 | The system shall support a minimum number of concurrent users without performance degradation below the specified response time targets. | Must | All response time targets met at 200 concurrent users (approximately 28% of MTCA's 700+ staff) | Merged: S1 NFR-005 (150 users) + CMP G08.001 (200 users); CMP's higher target retained |
| NFR-006 | Database queries powering operational reports shall complete within the specified time. Ad-hoc analytical queries shall complete within a separate, longer timeout. | Should | Operational queries ≤5 seconds; ad-hoc queries ≤30 seconds | S1 NFR-006 |

## 6.2 Availability and Reliability

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-007 | The system shall be available during MTCA business hours (Monday–Friday, 07:00–19:00 CET) with the specified uptime percentage, measured monthly. | Must | 99.9% availability during business hours (≤0.7 hours unplanned downtime per month) | Merged: S1 NFR-007 (99.5%) + CMP G09.001 (99.9%); CMP's stricter target retained |
| NFR-008 | Planned maintenance windows shall be scheduled outside business hours with a minimum advance notification period to all users. | Must | ≥48 hours advance notice; maintenance window: Saturday 22:00–Sunday 06:00 CET | S1 NFR-008 |
| NFR-009 | The system shall recover from unplanned outages within the specified recovery time objective (RTO) and recover data to within the specified recovery point objective (RPO). | Must | RTO ≤4 hours; RPO ≤1 hour (maximum 1 hour of data loss) | Merged: S1 NFR-009 + CMP G10.002 (RTO 4h) + CMP G10.003 (RPO 1h); targets aligned |
| NFR-010 | Automated health checks shall monitor system availability and alert administrators within the specified time when any component becomes unavailable. | Should | Alert within ≤5 minutes of component failure | S1 NFR-010 |
| NFR-010A | The system shall support automated daily backups with verified restore capability. | Must | Daily automated backups; backup integrity verification; documented restore procedures tested quarterly | CMP G10.001 |

## 6.3 Security

### 6.3.1 Authentication and Access Control

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-011 | The system shall implement Role-Based Access Control (RBAC) with configurable permissions at both function and data level. Minimum roles: Case Officer, Senior Officer, Supervisor, Manager, Administrator, Auditor (read-only), and System Administrator. Each role shall have configurable permissions for view, create, edit, approve, and delete operations per entity type. | Must | All system functions gated by role permissions. Permission changes effective immediately. Unauthorised access attempts logged and blocked. Role hierarchy supported (supervisor inherits officer permissions). Fine-grained permissions at function and data level. | Merged: S1 NFR-011 + CMP G11.002 (RBAC) + CMP G11.003 (fine-grained permissions) |
| NFR-011A | The system shall integrate with MTCA identity management infrastructure for centralised user authentication. | Must | Integration with Active Directory/LDAP. User provisioning and deprovisioning synchronised with corporate directory. | CMP G11.001 |
| NFR-011B | The system shall support single sign-on (SSO) with MTCA corporate authentication infrastructure. | Must | SSO integration with MTCA authentication service. Users authenticate once and access the system without re-entering credentials. | CMP G11.004 |
| NFR-011C | The system shall support multi-factor authentication (MFA) for privileged operations and remote access. | Must | MFA enabled for administrator roles and configurable for other roles. MFA methods: TOTP, push notification, or hardware token as supported by MTCA infrastructure. | CMP G11.005 |
| NFR-014 | User sessions shall automatically terminate after a configurable period of inactivity. Failed login attempts shall trigger account lockout after a configurable threshold. | Must | Session timeout: configurable (default 30 minutes). Account lockout: after 5 consecutive failed attempts; auto-unlock after 30 minutes or manual unlock by administrator. All authentication events logged. | S1 NFR-014 |

### 6.3.2 Data Protection and Encryption

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-012 | All data in transit shall be encrypted using TLS 1.2 or higher. All data at rest containing personally identifiable information (PII) shall be encrypted using AES-256 or equivalent. | Must | TLS 1.2+ for all HTTP, API, and database connections. Encryption at rest for: taxpayer names, addresses, TINs, financial data. Encryption key management per MITA standards. | Merged: S1 NFR-012 + CMP G12.002 (AES-256 at rest) + CMP G12.003 (TLS 1.2 in transit); targets aligned |
| NFR-013 | The system shall comply with GDPR requirements including: data subject access requests (right of access), right to rectification, data minimisation, purpose limitation, and data protection impact assessment outcomes. | Must | DSAR process: response within 30 calendar days. Data fields classified by sensitivity level. PII access logged. Data retention periods enforced automatically. Privacy impact assessment completed before go-live. | Merged: S1 NFR-013 + CMP G12.007 (GDPR compliance) |
| NFR-015 | The system shall support data masking for sensitive fields (TIN, name, financial amounts) when displayed to users who do not have full-access permissions. Partial masking (e.g., last 4 digits of TIN) shall be configurable per field and per role. | Should | Masking rules configurable per field and per role. Masked data cannot be copied/exported in unmasked form by restricted users. Masking consistent across all views (screen, reports, exports). | S1 NFR-015 |

### 6.3.3 Security Standards and Testing

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-015A | The system shall comply with ISO 27001 information security principles. | Must | Security controls aligned with ISO 27001 Annex A. Compliance documented and verifiable. | CMP G12.001 |
| NFR-015B | The system shall implement OWASP security best practices for web application security. | Must | OWASP Top 10 vulnerabilities addressed. Input validation, output encoding, CSRF protection, and secure session management implemented. Security testing against OWASP testing guide. | CMP G12.004 |
| NFR-015C | The system shall undergo security penetration testing at least annually and after significant changes. | Must | Annual penetration test by independent security assessor. Remediation of critical/high findings within 30 days. Test results documented and available for audit. | CMP G12.006 |

## 6.4 Usability

### 6.4.1 Accessibility and Internationalisation

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-016 | The user interface shall comply with WCAG 2.1 Level AA accessibility standards to ensure usability for all MTCA staff. | Must | WCAG 2.1 AA compliance verified by automated testing tool (e.g., axe, WAVE). Manual testing for keyboard navigation and screen reader compatibility. Compliance report produced before go-live. | Merged: S1 NFR-016 (Should) + CMP G06.004 (Must); CMP's Must priority retained |
| NFR-017 | The user interface shall support both Maltese and English languages, with the ability to switch language at any time without losing session context. | Must | All system labels, messages, and help text available in both languages. Language preference saved per user. Reports and documents generated in user's selected language or taxpayer's preferred language as appropriate. | Merged: S1 NFR-017 + CMP G06.003 |

### 6.4.2 User Experience

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-018 | New case officers shall be able to perform core tasks (view debt case, create case, log activity, generate document) after completing a training programme of specified maximum duration. | Should | Training programme ≤3 days for core competency. Task completion rate ≥90% within 5 days of training. Measured via post-training assessment. Contextual help available on all screens. | S1 NFR-018 |
| NFR-019 | The user interface shall be responsive and functional on standard desktop browsers (Chrome, Edge, Firefox — latest two major versions) at minimum resolution of 1280×720. Mobile access shall be supported for supervisory functions (dashboard viewing, approval actions). | Must | Cross-browser compatibility tested. Desktop layout optimised for 1920×1080. Mobile-responsive design for tablet access. No plugin or extension requirements. | Merged: S1 NFR-019 + CMP G06.001 (web-based) + CMP G06.002 (mobile-responsive) |
| NFR-019A | The system shall provide consistent user experience across all modules and functional areas. | Must | Consistent navigation patterns, terminology, colour scheme, and interaction paradigms across all system modules. | CMP G06.005 |

### 6.4.3 Documentation and Help

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-019B | The system shall include an online help system with searchable content and user manuals for end users and administrators. | Must | Online help accessible from all screens. Content searchable by keyword. User manuals and administrator guides maintained and versioned. | CMP G07.001 + G07.002 |
| NFR-019C | The system shall include configuration documentation for all configurable elements and API documentation in OpenAPI/Swagger format. | Must | All configurable parameters documented with: description, default value, valid range, impact. API documentation auto-generated and published. | CMP G07.003 + G07.004 |

## 6.5 Data Quality

### 6.5.1 Data Accuracy and Validation

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-020 | Financial calculations within the debt management system shall maintain arithmetic accuracy with zero tolerance for rounding errors. All financial amounts shall use a minimum of 2 decimal places (Euro cents). | Must | Debt balance calculations accurate to ± €0.00. Instalment plan totals equal original debt amount. Reconciliation checks run automatically. Discrepancy > €0.01 triggers alert. | S1 NFR-020 (adapted: balance references contextualised for DM) |
| NFR-021 | Data imported from ORS/ClickHouse shall be validated against defined business rules before display. Invalid or missing data shall be flagged visually to users and logged for investigation. | Must | Validation rules: mandatory field completeness, referential integrity (TIN exists in register), amount reasonableness, date validity. Invalid records flagged with specific validation failure reason. Data quality dashboard showing: % records passing validation by source table. | S1 NFR-021 |
| NFR-022 | Duplicate detection shall prevent creation of duplicate debt cases for the same taxpayer, tax type, and tax period combination. Near-duplicate detection shall alert users when similar cases exist. | Must | Exact-match duplicate blocked at creation. Near-duplicate alert shows: existing case ID, status, creation date. User can override near-duplicate alert with mandatory justification. Duplicate check response time <2 seconds. | S1 NFR-022 |

### 6.5.2 Data Integrity and Management

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-022A | The system shall ensure data integrity through transaction management and constraint enforcement. | Must | ACID-compliant transactions for all data modifications. Referential integrity enforced at database level. Concurrent update conflicts handled gracefully with user notification. | CMP G04.001 |
| NFR-022B | The system shall support efficient query processing with indexing and optimisation. | Must | Frequently-accessed queries optimised with appropriate indexing. Query execution plans reviewed during performance testing. Slow query detection and logging enabled. | CMP G04.002 |

### 6.5.3 Data Archiving and Retention

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-022C | The system shall support data archiving based on configurable retention policies, maintaining a minimum period of online historical data per government requirements. | Must | 7 years of online historical data. Archiving policies configurable per entity type. Retention periods enforced automatically. | CMP G03.001 + G03.002 |
| NFR-022D | Archived data shall remain retrievable for audit and legal purposes. | Must | Archived data accessible via dedicated retrieval interface. Retrieval response time ≤30 seconds for individual records. Archived data format preserves full fidelity. | CMP G03.003 |

## 6.6 Auditability

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-023 | All system activities shall be logged in an immutable audit trail including: user actions, system events, data modifications (before and after values), API calls, and configuration changes. Security-sensitive operations shall be logged with enhanced detail. | Must | Audit log entries: timestamp (UTC), user ID, action, entity, before/after values, IP address, session ID. Audit log cannot be modified or deleted by any user. Audit log searchable by: date range, user, action type, entity. Retention: ≥10 years. Security-sensitive operations (permission changes, data exports, PII access) logged with additional context. | Merged: S1 NFR-023 + CMP G12.005 (security audit logging) |
| NFR-024 | Audit reports shall be generatable showing: user activity summaries, data access patterns, configuration change history, and exception events, for any specified date range. | Should | Standard audit reports: user login/logout history, data access by user, configuration changes, failed access attempts, SLA breaches. Reports exportable to PDF and Excel. Generation time <60 seconds for 30-day report. | S1 NFR-024 |

## 6.7 Maintainability and Configurability

### 6.7.1 Business Rule Configuration

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-025 | Business rules (debt category thresholds, escalation sequences, SLA durations, notification schedules, interest rates, penalty rates) shall be configurable by authorised administrators through the platform's UI, without requiring code changes or developer intervention. | Must | ≥90% of business rules configurable via UI. Rule changes take effect within the specified activation period (default: immediately, or at next business day start). Rule change history maintained with: old value, new value, change date, changed by. | S1 NFR-025 |
| NFR-026 | New report templates and notification templates shall be deployable by trained MTCA staff using the platform's visual design tools, without requiring external vendor support. | Should | Template creation via drag-and-drop or visual editor. Average template creation time ≤4 hours for a trained administrator. Template testing in sandbox environment before production deployment. ≥80% of template changes achievable without vendor support. | S1 NFR-026 |

### 6.7.2 Reporting Capabilities

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-026A | The system shall provide report builder capability for ad-hoc report creation and dashboard capabilities with configurable visualisations. | Must | End-user report builder for operational reporting. Configurable dashboards with drill-down capability. Report scheduling and automated distribution. | CMP G05.001 + G05.002 + G05.003 |
| NFR-026B | The system shall support report export in PDF, Excel, and CSV formats, and provide integration capability with enterprise BI tools. | Must (export) / Should (BI integration) | PDF, Excel, CSV export from all reports. Integration with Power BI or equivalent enterprise BI tools for advanced analytics. | CMP G05.004 + G05.005 |

### 6.7.3 Environment and Deployment Management

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-027 | The system shall support separate environments for development, testing/UAT, and production, with controlled promotion of configurations and customisations between environments. | Must | Minimum 3 environments: DEV, UAT, PROD. Configuration promotion via documented export/import process. Environment parity: UAT mirrors PROD configuration. Promotion audit trail maintained. | S1 NFR-027 |

### 6.7.4 Solution Architecture and Vendor Requirements

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-027A | The Case Management Platform shall be a COTS product with demonstrated market adoption and independent analyst validation or proven open-source community adoption. | Must | Demonstrated market adoption across multiple organisations. Independent analyst validation (Gartner, Forrester) or open-source community adoption (100+ organisations). | CMP G01.001 + G01.002 |
| NFR-027B | The platform shall provide modular architecture with separation of concerns between presentation, business logic, and data layers. | Must | Modular deployment of specific components. Integrated data dictionary for metadata management. Master data management for shared reference data. Clear architectural separation of presentation, logic, and data tiers. | CMP G01.003 + G01.004 + G01.005 + G01.006 |
| NFR-027C | The vendor shall provide a clear product roadmap, annual releases with documented upgrade paths, migration tools, backward compatibility for at least 2 major versions, and minimum 12 months advance notice before feature deprecation. | Must | Annual releases with upgrade path documentation. Migration tools provided for major version upgrades. Backward compatibility maintained ≥2 major versions. ≥12 months notice before feature deprecation. | CMP G02.001 + G02.002 + G02.003 + G02.004 + G02.005 |

## 6.8 Portability and Standards Compliance

| **ID** | **Description** | **Priority** | **Measurable Target** | **Source** |
| --- | --- | --- | --- | --- |
| NFR-028 | All workflow process definitions shall be exportable in BPMN 2.0 standard XML format, ensuring portability to any BPMN 2.0 compliant engine. | Must | Export validates against BPMN 2.0 XML schema (OMG standard). Exported processes importable into at least 2 independent BPMN tools (e.g., Camunda, Flowable) with visual equivalence. Export includes: process diagrams, decision points, participant lanes, message flows. | S1 NFR-028 |
| NFR-029 | Business rules shall be exportable in a structured, machine-readable format (DMN, spreadsheet, or JSON) to facilitate migration to ITCAS or alternative platforms. | Should | Decision tables exportable as XLSX or DMN 1.3 XML. Conditional rules exportable as JSON or XML. Exported rules include: name, conditions, actions, priority, effective dates. Export completeness: ≥95% of configured rules included. | S1 NFR-029 |
| NFR-030 | All APIs exposed by the system shall conform to RESTful design principles with OpenAPI 3.0 specification documentation, enabling integration with ITCAS and other future systems. | Must | OpenAPI 3.0 spec auto-generated and published. API versioning supported. Backward-compatible changes default; breaking changes require version increment. API documentation includes: endpoints, parameters, request/response schemas, authentication, error codes. | S1 NFR-030 |
| NFR-031 | The system shall not create dependencies on proprietary data formats, protocols, or platform-specific features that would prevent migration to an alternative platform. All data shall be exportable in standard formats (CSV, JSON, XML, SQL). | Must | Full data export in: CSV (for tabular data), JSON (for configuration and hierarchical data), SQL (for relational data). Export includes all: case data, workflow history, documents (as files + metadata), configuration, audit trail. Export documentation: data dictionary with field descriptions and data types. | S1 NFR-031 |

## Requirements Summary

| **Subsection** | **Source 1 Only** | **CMP Only** | **Merged** | **Total** |  |
| --- | --- | --- | --- | --- | --- |
| 6.1 Performance | 3 | 0 | 3 | 6 |  |
| 6.2 Availability and Reliability | 2 | 1 | 2 | 5 |  |
| 6.3 Security | 2 | 6 | 3 | 11 |  |
| 6.4 Usability | 1 | 3 | 3 | 7 |  |
| 6.5 Data Quality | 3 | 4 | 0 | 7 |  |
| 6.6 Auditability | 1 | 0 | 1 | 2 |  |
| 6.7 Maintainability | 3 | 3 | 0 | 6 |  |
| 6.8 Portability | 4 | 0 | 0 | 4 |  |
| **Total** | **19** | **17** | **12** | **48** |  |

**MoSCoW Breakdown:** 41 Must / 6 Should / 1 Must+Should (NFR-026B) = 48 requirements total

**Merge decisions (12 merged requirements):**

- **NFR-002**: S1 ≤5s + CMP G08.002 ≤3s → retained both targets (≤3s standard, ≤5s complex)

- **NFR-003**: S1 targets + CMP G08.003 ≤10s → aligned, no conflict

- **NFR-005**: S1 150 users + CMP G08.001 200 users → CMP's higher target (200) retained

- **NFR-007**: S1 99.5% + CMP G09.001 99.9% → CMP's stricter target (99.9%) retained

- **NFR-009**: S1 RTO/RPO + CMP G10.002/G10.003 → aligned, no conflict

- **NFR-011**: S1 RBAC + CMP G11.002/G11.003 → merged with fine-grained permissions added

- **NFR-012**: S1 encryption + CMP G12.002/G12.003 → aligned, no conflict

- **NFR-013**: S1 GDPR + CMP G12.007 → S1 more detailed, retained

- **NFR-016**: S1 (Should) + CMP G06.004 (Must) → elevated to Must per CMP

- **NFR-017**: S1 bilingual + CMP G06.003 → aligned, no conflict

- **NFR-019**: S1 browser + CMP G06.001/G06.002 → merged with mobile-responsive added

- **NFR-023**: S1 audit trail + CMP G12.005 → merged with security-sensitive logging added

**External dependencies referenced:** ORS/ClickHouse (data source), MITA infrastructure (AD/LDAP, SSO, security standards), ITCAS (future migration target), SAS VIYA (analytics integration via G05.005)

**Platform-agnostic language applied:** Zero references to any specific vendor platform. All references use "the Case Management Platform" or "the selected platform."

# 7. Use Case Model — Part 1: Debt Management Use Cases UC-DM-01 to UC-DM-10

## 7.1 Use Case Overview

This section presents the detailed use case specifications for the Debt Management (DM) domain. Each use case covers the full debt lifecycle from identification through recovery and write-off, aligned with the four OECD strategic principles: proactive compliance support, post-due-date escalation, data-driven enforcement strategy, and collection gap management.

Business rules referenced herein (BR-DM-xxx) are formally defined in Section 8 (Business Rules Catalogue). Forward references are used for traceability; the rule catalogue provides the authoritative definitions.

### 7.1.1 Actor Definitions

| **Actor** | **Abbreviation** | **Description** |
| --- | --- | --- |
| **Debt Management Officer** | DMO | MTCA operational staff responsible for debt case management, enforcement actions, and debtor communications. Has read/write access to assigned debt cases. |
| **Senior Debt Officer** | SDO | Supervisory MTCA staff with approval authority for instalment agreements, write-offs, and high-value enforcement actions above configured thresholds. |
| **Enforcement Officer** | EO | MTCA field staff responsible for taxpayer visits, property seizure, and on-site enforcement activities. |
| **System Administrator** | SA | MTCA IT/configuration staff responsible for configuring workflows, thresholds, templates, and escalation rules. |
| **Taxpayer** | TP | Natural person (NP) or legal person (LP) with outstanding tax debt, interacting via the self-service portal or in-person at MTCA offices. |
| **System** | SYS | The Case Management Platform performing automated debt detection, classification, escalation, notification generation, and compliance monitoring. |
| **External System** | EXT | ORS/ClickHouse data warehouse, bank systems (for garnishing), asset registers (real estate, automobile), postal service (for delivery tracking), or national debt registry. |

### 7.1.2 Use Case Summary (UC-DM-01 to UC-DM-10)

| **ID** | **Name** | **Primary Actor(s)** | **Priority** |  |
| --- | --- | --- | --- | --- |
| UC-DM-01 | Identify and Create Debt Case (Automated) | System, DMO | Must |  |
| UC-DM-02 | Generate Payment Reminder | System | Must |  |
| UC-DM-03 | Issue Demand Notice | System, DMO | Must |  |
| UC-DM-04 | Issue Final/Immediate Demand Notice | System, SDO | Must |  |
| UC-DM-05 | Classify and Rank Debt Cases by Risk | System, DMO, SDO | Must |  |
| UC-DM-06 | Create Instalment Agreement | Taxpayer, DMO | Must |  |
| UC-DM-07 | Review and Approve Instalment Agreement | DMO, SDO, Taxpayer | Must |  |
| UC-DM-08 | Monitor Instalment Agreement Compliance | System, DMO | Must |  |
| UC-DM-09 | Cancel Instalment Agreement and Create Recovery Case | System, DMO, Taxpayer | Must |  |
| UC-DM-10 | Escalate Debt Case for Enforcement | System, DMO, SDO | Must |  |

## 7.2 Debt Management Use Cases

### UC-DM-01: Identify and Create Debt Case (Automated)

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary), Debt Management Officer (secondary — manual creation) |  |
| **Priority** | Must |  |
| **Frequency** | ~200–500/day (automated), ~20–50/day (manual) |  |
| **Related Requirements** | DM-FR-001, DM-FR-002, DM-FR-003, DM-FR-004, DM-FR-005, DM-FR-017 |  |
| **Related Business Rules** | BR-DM-001 (debt detection triggers), BR-DM-002 (case creation rules), BR-DM-003 (category thresholds C1–C5), BR-DM-004 (case consolidation rules) |  |

**Preconditions:**

- Taxpayer account data is available in ORS/ClickHouse, synchronised within the last 24 hours.

- Debt detection rules are configured (due date monitoring, trigger events, category thresholds).

- Debt category thresholds are defined: C1 (<€30), C2 (€30–100), C3 (€100–1,000), C4 (€1,000–20,000), C5 (€20,000–200,000).

- Worklist assignment rules are configured for case routing.

**Main Success Scenario:**

- The system executes the scheduled debt detection batch process (configurable frequency, minimum daily).

- The system queries ORS/ClickHouse for all taxpayer accounts with liabilities past their due date that do not already have an active debt case.

- For each identified account, the system evaluates trigger conditions per BR-DM-001: liability past due date, dishonoured payment, cancelled instalment agreement, suspense account non-resolution, or assessment non-payment.

- The system checks whether an active debt case already exists for the taxpayer. If yes, it evaluates whether the new trigger should be consolidated into the existing case per BR-DM-004 or create a separate case.

- For each new debt case, the system creates a case record with: auto-generated case ID, TIN, consolidated debt amount across all overdue tax types, individual tax type and period breakdown, age of oldest debt, trigger event(s), creation timestamp, and source (automated).

- The system classifies the debt into the appropriate category (C1–C5) per BR-DM-003 based on the consolidated owed amount.

- For C1 debts (<€30), the system routes directly to the automatic write-off process (UC-DM-12) without entering the debt management workflow.

- For C2–C5 debts, the system assigns the case to the appropriate worklist based on category, risk criteria, and officer specialisation per configured routing rules.

- The system flags the taxpayer account in ORS with the status "debt outstanding" and/or "debt payable."

- The system logs the case creation event with full audit details (timestamp, trigger, amounts, classification, assignment).

- The assigned Debt Management Officer receives a notification of the new case in their worklist.

**Alternative Flows:**

- **AF-1: Manual Case Creation.** At step 1, a DMO identifies a situation not covered by automated triggers (e.g., intelligence-based, cross-border debt notification). The DMO navigates to the case creation screen, enters: TIN, tax type(s), period(s), reason/justification, and priority. The system validates the TIN, confirms no duplicate case exists, creates the case, classifies it, and assigns it per standard rules. Audit trail records the creating officer and justification.

- **AF-2: Multiple Trigger Consolidation.** At step 4, the taxpayer already has an active debt case. The system consolidates the new trigger into the existing case: updates the debt amount, recalculates the category classification, adds the new trigger event to the case history, and re-evaluates risk score. The assigned officer receives a notification of the case update.

- **AF-3: High-Value Immediate Escalation.** At step 6, if the debt exceeds the high-value threshold (configurable, e.g., >€200,000), the system creates the case with "critical" priority and triggers immediate escalation per UC-DM-10, bypassing the standard reminder workflow. SLA requires first telephone contact within 2 business days.

- **AF-4: Category Re-evaluation on Balance Change.** Post-creation, when the taxpayer's consolidated balance changes (additional liabilities posted, partial payment received, credit applied), the system re-evaluates the debt category per DM-FR-017. Category upgrade makes additional enforcement actions available; category downgrade maintains current enforcement level but prevents further escalation.

**Exception Flows:**

- **EF-1: ORS/ClickHouse Data Unavailable.** At step 2, if ORS/ClickHouse is unreachable, the system logs the failure, retries at configured intervals, and alerts the System Administrator. No cases are created until data availability is restored.

- **EF-2: Duplicate Case Detection.** At step 4, the system detects an identical case (same TIN, same tax type/period, same trigger) already exists. The system skips creation and logs the duplicate detection event.

- **EF-3: Invalid TIN (Manual Creation).** At step AF-1, if the DMO enters a TIN that does not exist in the taxpayer register, the system displays "TIN not found" and prevents case creation.

**Postconditions:**

- A new debt case is created with a unique case ID, classified into the correct category (C1–C5).

- The taxpayer account is flagged with the appropriate debt status in ORS.

- The case is assigned to a worklist or routed to the write-off process (C1).

- An audit trail entry records the case creation event.

### UC-DM-02: Generate Payment Reminder

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary) |  |
| **Priority** | Must |  |
| **Frequency** | ~300–600/day (batch), triggered by debt detection |  |
| **Related Requirements** | DM-FR-009, DM-FR-010, DM-FR-011, DM-FR-014, DM-FR-051 |  |
| **Related Business Rules** | BR-DM-005 (reminder timing rules), BR-DM-006 (reminder applicability by category), BR-DM-007 (notification channel selection) |  |

**Preconditions:**

- A debt case exists for the taxpayer in category C2–C5 (C1 excluded — auto write-off).

- The debt case is in the "new" or "first reminder pending" workflow state.

- Reminder templates are configured for each tax type and language (English, Maltese).

- Notification channels are configured (email, SMS, postal mail, in-app).

**Main Success Scenario:**

- The system identifies all debt cases eligible for a first payment reminder: cases in category C2–C5 where the configured post-due-date period has elapsed (e.g., 7 days after due date) and no reminder has been issued.

- For each eligible case, the system selects the appropriate reminder template based on tax type, taxpayer type (NP/LP), and language preference.

- The system populates the template with case-specific data: TIN, taxpayer name, tax type(s), period(s), overdue amount broken down by PA (principal arrears), IA (interest arrears), and PCA (penalty/charge arrears), payment deadline, and available payment methods.

- The system determines the delivery channel(s) per BR-DM-007: taxpayer's registered communication preference, or default channel if no preference is set.

- The system generates and dispatches the reminder via the selected channel(s).

- The system records the reminder generation event on the debt case: reminder number (1st), date/time sent, channel used, template version, and delivery status (sent/pending).

- The system updates the case workflow state to "first reminder sent" and sets a timer for the response period (configurable, e.g., 14 days).

- The system logs the event to the audit trail.

**Alternative Flows:**

- **AF-1: Second Reminder (C3–C5 Only).** After the first reminder response period expires without payment or taxpayer response, the system generates a second reminder for cases in categories C3–C5 per DM-FR-011. The second reminder includes escalation warning language (e.g., "failure to respond may result in enforcement action"). The case workflow state updates to "second reminder sent" with a new response timer.

- **AF-2: Proactive Pre-Due-Date Nudge.** For taxpayers approaching their due date (not yet overdue), the system generates a proactive notification per DM-FR-009 at configurable lead times (e.g., 14 days, 7 days, 1 day before due date). These are informational nudges, not formal reminders, and do not create debt cases.

- **AF-3: Multi-Channel Delivery.** Per BR-DM-007, the system sends the reminder via multiple channels simultaneously (e.g., email + SMS for high-priority cases, email + postal for cases without confirmed electronic delivery).

- **AF-4: Payment Received During Reminder Period.** If full payment is received before the response period expires, the system detects the balance change, cancels the pending timer, updates the case to "resolved by payment," and closes the case.

**Exception Flows:**

- **EF-1: Delivery Failure.** At step 5, if the primary channel fails (email bounce, invalid phone number), the system falls back to the next configured channel. If all channels fail, the system flags the case for manual intervention and alerts the assigned DMO.

- **EF-2: Template Not Available.** At step 2, if the specific template variant is not configured (e.g., Maltese LP reminder for a specific tax type), the system falls back to the default English template and logs the template gap for administrator attention.

- **EF-3: Taxpayer Under Active Objection.** At step 1, if the taxpayer has a pending objection against the assessed amount per BR-DM-008, the system excludes the case from the reminder batch and flags it as "reminder suspended — objection pending."

**Postconditions:**

- A payment reminder has been generated and dispatched to the taxpayer.

- The debt case workflow state has been updated to reflect the reminder stage.

- Delivery status has been recorded on the case.

- A response period timer has been set for escalation monitoring.

### UC-DM-03: Issue Demand Notice

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary), Debt Management Officer (review and manual dispatch) |  |
| **Priority** | Must |  |
| **Frequency** | ~100–200/day |  |
| **Related Requirements** | DM-FR-012, DM-FR-014, DM-FR-040, DM-FR-041, DM-FR-038, DM-FR-051 |  |
| **Related Business Rules** | BR-DM-008 (demand notice timing), BR-DM-009 (demand notice content rules), BR-DM-010 (legal fee applicability) |  |

**Preconditions:**

- A debt case exists in category C2–C5 with the reminder period expired and no payment received.

- The case workflow state is "reminder period expired" or equivalent.

- Demand notice templates are configured with appropriate legal text, merge fields, and language variants.

- Legal fee schedule is configured per DM-FR-038.

**Main Success Scenario:**

- The system identifies all debt cases where the reminder period (first or second, as applicable per category) has expired without payment or taxpayer response.

- For each eligible case, the system determines the demand notice type per BR-DM-009: separate notice per tax type or consolidated notice across multiple tax types, per DM-FR-041 configuration.

- The system selects the appropriate demand notice template and populates it with: TIN, taxpayer name and address, full debt breakdown by tax type, period, and charge component (PA/IA/PCA), legal basis for the demand, consequences of non-payment, taxpayer's appeal rights, payment deadline, and payment methods.

- If legal fees are applicable per BR-DM-010, the system calculates the fee amount and includes it in the demand notice. The fee is posted to the taxpayer's ledger as a separate charge type linked to the enforcement case per DM-FR-038.

- The system generates the demand notice document (PDF) and queues it for dispatch via configured channels (postal mail as primary for legal validity, with electronic copy via email/portal).

- The system records the demand notice on the debt case: notice type, date generated, amount demanded, legal fee (if applicable), dispatch channel, delivery status.

- The system updates the case workflow state to "demand notice issued" and sets a response period timer (configurable, e.g., 21 days).

- The system sends a notification to the assigned DMO that a demand notice has been issued.

**Alternative Flows:**

- **AF-1: Manual Review Before Dispatch.** For cases above a configurable threshold (e.g., C4–C5), the system generates the demand notice in draft state and routes it to the assigned DMO for review before dispatch. The DMO reviews the notice content, approves or modifies, and then confirms dispatch.

- **AF-2: Partial Payment After Reminder.** If a partial payment was received during the reminder period, the system recalculates the demanded amount reflecting the partial payment. The demand notice shows: original amount, payment received, remaining balance demanded.

- **AF-3: Consolidated Demand Across Tax Types.** Per DM-FR-041, the system generates a single consolidated demand notice covering all overdue tax types for the taxpayer, with a per-tax-type breakdown within the notice.

**Exception Flows:**

- **EF-1: Objection Filed During Reminder Period.** At step 1, if the taxpayer filed a valid objection during the reminder period, the system excludes the disputed portion from the demand. If the entire amount is under objection, the demand notice is suspended pending objection resolution. Undisputed amounts proceed normally.

- **EF-2: Active Instalment Agreement.** At step 1, if the taxpayer has an active, compliant instalment agreement covering the debts, the system skips the demand notice per DM-FR-028 and logs the exclusion reason.

- **EF-3: Address Unavailable.** At step 5, if the taxpayer's postal address is not available or marked as invalid, the system flags the case for manual intervention and attempts electronic delivery only. The DMO is alerted to resolve the address issue.

**Postconditions:**

- A formal demand notice has been generated and dispatched (or queued for dispatch).

- Any applicable legal fees have been posted to the taxpayer's ledger.

- The case workflow state reflects the demand notice stage.

- A response period timer is active for escalation monitoring.

### UC-DM-04: Issue Final/Immediate Demand Notice

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary), Senior Debt Officer (approval for immediate demands) |  |
| **Priority** | Must |  |
| **Frequency** | ~50–100/day |  |
| **Related Requirements** | DM-FR-013, DM-FR-014, DM-FR-018, DM-FR-038, DM-FR-040 |  |
| **Related Business Rules** | BR-DM-011 (final demand timing), BR-DM-012 (immediate demand triggers), BR-DM-013 (enforcement warning content) |  |

**Preconditions:**

- A demand notice was previously issued and the response period has expired without payment (for final demand), OR a high-value debt has been identified requiring immediate action (for immediate demand).

- The case is in category C2–C5 (final demand) or exceeds the high-value threshold (immediate demand).

- Final/immediate demand templates are configured with escalation language and legal consequences.

**Main Success Scenario:**

- The system identifies debt cases where the initial demand notice response period has expired without payment.

- The system generates a final demand notice per DM-FR-013, which includes: full debt breakdown including any accrued interest since the initial demand, explicit statement of legal consequences of continued non-payment (enforcement actions that will be taken), deadline for response (configurable, e.g., 10 days), and proof of delivery requirement for enforcement validity.

- The system dispatches the final demand via channels that support proof of delivery (registered postal mail, or electronic with read receipt).

- The system records delivery status and proof of delivery details on the case.

- The system updates the case workflow state to "final demand issued" and sets the enforcement eligibility timer.

- If any additional legal fees apply, they are posted to the taxpayer's ledger.

- The system alerts the assigned DMO and supervisor that the case is approaching enforcement stage.

**Alternative Flows:**

- **AF-1: Immediate Demand for High-Value Debts.** When a debt exceeds the configurable high-value threshold (per DM-FR-018), the system generates an immediate demand notice bypassing the standard reminder/demand sequence. A Senior Debt Officer reviews and approves the immediate demand. The case is simultaneously escalated for telephone contact within 2 business days.

- **AF-2: Taxpayer Responds with Instalment Request.** During the final demand response period, the taxpayer submits an instalment agreement application. The system suspends the enforcement countdown and routes the application to the instalment approval workflow (UC-DM-06/UC-DM-07).

- **AF-3: Partial Payment Received.** A partial payment is received during the final demand period. The system recalculates the outstanding amount, re-evaluates the debt category, and if the remaining amount falls to a lower category, adjusts the available enforcement actions accordingly.

**Exception Flows:**

- **EF-1: Proof of Delivery Not Obtained.** At step 3, if proof of delivery cannot be obtained (returned mail, invalid address), the system flags the case for alternative service methods. The DMO must arrange alternative delivery (e.g., physical visit, publication) before enforcement actions can proceed.

- **EF-2: Taxpayer Deceased/Dissolved.** During processing, the system or DMO identifies that the taxpayer (NP) is deceased or (LP) has been dissolved. The case is routed to a specialised workflow for estate/successor claims.

**Postconditions:**

- A final or immediate demand notice has been generated and dispatched with proof of delivery tracked.

- The case is eligible for enforcement action after the response period expires.

- All legal fees have been posted to the taxpayer's ledger.

- The audit trail captures the complete demand escalation history.

### UC-DM-05: Classify and Rank Debt Cases by Risk

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary), Debt Management Officer (review), Senior Debt Officer (strategy adjustment) |  |
| **Priority** | Must |  |
| **Frequency** | Daily batch + real-time on case changes |  |
| **Related Requirements** | DM-FR-006, DM-FR-007, DM-FR-008, DM-FR-004, DM-FR-017 |  |
| **Related Business Rules** | BR-DM-014 (risk scoring weights), BR-DM-015 (risk profile components), BR-DM-016 (enforcement exclusion rules) |  |

**Preconditions:**

- Active debt cases exist in the system.

- Risk scoring criteria and weights are configured by the System Administrator.

- Taxpayer compliance history and demographic data are available from ORS/ClickHouse and the taxpayer register.

**Main Success Scenario:**

- The system executes the risk scoring batch process (daily, configurable schedule) or is triggered by a case change event (new case, balance change, enforcement outcome).

- For each active debt case, the system evaluates the configurable risk criteria per DM-FR-006: size of debt (weighted), age of debt from oldest overdue item (weighted), number of revenue types involved (weighted), taxpayer compliance history — previous debt cases, enforcement responses, payment patterns (weighted), taxpayer type — NP vs. LP (weighted), and economic sector (weighted).

- The system calculates a composite risk score (0–100 scale) based on the weighted criteria per BR-DM-014.

- The system ranks all active cases by risk score and updates the ranking on each case record.

- The system generates individual risk profiles per DM-FR-007 showing: current debt amount and breakdown, historical debt patterns (recidivism indicator), compliance track record, enforcement response history, and payment behaviour analysis.

- The system generates aggregate risk profiles by taxpayer segment (NP/LP, economic sector, geographic region) for management information and strategy planning.

- The system evaluates enforcement inclusion/exclusion rules per BR-DM-016: cases with pending objections are excluded from standard enforcement, cases with active compliant instalment agreements are excluded, and cases matching other configured exclusion criteria are flagged.

- The system updates worklist sort orders to reflect the new risk rankings.

- The system logs the scoring batch completion with statistics: cases scored, distribution by risk tier, exclusions applied.

**Alternative Flows:**

- **AF-1: Real-Time Re-scoring on Balance Change.** When a payment is received or additional liability is posted, the system immediately re-evaluates the affected case's risk score and category per DM-FR-017, without waiting for the daily batch. Worklist position is updated in real time.

- **AF-2: Senior Officer Strategy Review.** A Senior Debt Officer reviews the aggregate risk profiles and adjusts scoring weights or exclusion rules via the administration interface. The system re-runs scoring for all active cases with the updated parameters and provides a before/after comparison.

- **AF-3: SAS VIYA Analytics Integration.** For advanced risk profiling per DM-FR-007, the system exports case data to SAS VIYA for predictive analytics (probability of voluntary payment, optimal enforcement strategy). Results are imported back as supplementary risk indicators on the case.

**Exception Flows:**

- **EF-1: Incomplete Data for Scoring.** At step 2, if a required data element is unavailable (e.g., economic sector not classified for the taxpayer), the system applies a default/neutral value for the missing criterion and flags the case as "incomplete scoring data" for data quality follow-up.

- **EF-2: Scoring Configuration Error.** At step 2, if the scoring weights do not sum correctly or a required criterion has no weight assigned, the system aborts the batch, logs the configuration error, and alerts the System Administrator.

**Postconditions:**

- All active debt cases have been scored and ranked by risk.

- Individual and aggregate risk profiles are updated and available.

- Enforcement exclusions have been applied per configured rules.

- Worklists reflect the updated risk-based prioritisation.

### UC-DM-06: Create Instalment Agreement

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Taxpayer (primary — portal submission), Debt Management Officer (counter submission), System |  |
| **Priority** | Must |  |
| **Frequency** | ~20–50/day |  |
| **Related Requirements** | DM-FR-021, DM-FR-022, DM-FR-025, DM-FR-028 |  |
| **Related Business Rules** | BR-DM-017 (instalment eligibility), BR-DM-018 (single plan rule), BR-DM-019 (instalment interest rate), BR-DM-020 (minimum instalment amount) |  |

**Preconditions:**

- The taxpayer has outstanding debts recorded in ORS/ClickHouse.

- The taxpayer does not have an active instalment agreement per DM-FR-022 / BR-DM-018.

- The instalment agreement module is operational and configured with approval parameters.

- Instalment interest rates are configured per DM-FR-025.

**Main Success Scenario:**

- The Taxpayer logs in to the self-service portal and navigates to the "Instalment Agreement" section.

- The system displays the taxpayer's consolidated outstanding debts (retrieved from ORS/ClickHouse) across all tax types, showing: each tax type and period with PA, IA, and PCA components, total outstanding amount, and current debt category.

- The system validates that no active instalment agreement exists per BR-DM-018.

- The Taxpayer proposes a payment schedule: number of instalments, payment frequency (monthly, quarterly), start date, and individual instalment amounts. The system provides a schedule calculator that distributes the total debt (principal + projected interest) across the requested number of payments.

- The system calculates the interest component per DM-FR-025 using the reduced instalment interest rate applied to the outstanding principal balance at each instalment date. The total cost of the instalment plan (principal + total interest) is displayed.

- The system validates the proposed schedule against configured parameters per BR-DM-017: minimum instalment amount per BR-DM-020, maximum plan duration, and coverage of all outstanding debts.

- The Taxpayer reviews the calculated schedule (showing each instalment date, principal portion, interest portion, and remaining balance) and confirms the application.

- The system creates the instalment agreement application with status "submitted," attaches the proposed schedule, records the submission timestamp and channel (portal), and generates a confirmation receipt for the taxpayer.

- The system routes the application to the approval workflow (UC-DM-07).

- The application status is visible to the Taxpayer in real time via the portal.

**Alternative Flows:**

- **AF-1: Over-the-Counter (Paper) Submission.** The Taxpayer visits an MTCA office and submits a paper instalment application. A DMO enters the application details into the system on behalf of the taxpayer per DM-FR-021. The workflow proceeds identically from step 4 onward. The submission channel is recorded as "counter."

- **AF-2: Active Plan Exists — Rejection.** At step 3, the system detects an active instalment agreement. The system displays a rejection message: "An active instalment agreement already exists (Agreement #[ID], approved [date]). A new application may only be submitted after the current agreement is cancelled." The application cannot proceed.

- **AF-3: Schedule Exceeds Standard Parameters.** At step 6, the proposed schedule exceeds standard parameters (e.g., duration >12 months or debt >€5,000). The system accepts the application but flags it for senior officer approval per DM-FR-023, rather than auto-approval.

**Exception Flows:**

- **EF-1: No Outstanding Debts.** At step 2, if the taxpayer has no outstanding debts in ORS/ClickHouse, the system displays "No outstanding balances found. An instalment agreement is not applicable." and prevents application submission.

- **EF-2: Validation Failure.** At step 6, if the proposed schedule fails validation (e.g., instalment amount below minimum, total does not cover full debt), the system displays specific validation errors and prompts the Taxpayer to adjust the schedule.

- **EF-3: Portal Session Timeout.** At step 7, if the Taxpayer's session expires before confirmation, the system saves the draft application (incomplete status). The Taxpayer can resume from the saved draft upon next login.

**Postconditions:**

- An instalment agreement application has been created with a proposed payment schedule.

- The interest component has been calculated using the reduced instalment rate.

- The application has been routed to the approval workflow.

- Application status is visible to the Taxpayer in real time.

### UC-DM-07: Review and Approve Instalment Agreement

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Debt Management Officer (standard approval), Senior Debt Officer (elevated approval), Taxpayer (draft review) |  |
| **Priority** | Must |  |
| **Frequency** | ~20–50/day |  |
| **Related Requirements** | DM-FR-023, DM-FR-024, DM-FR-025, DM-FR-028 |  |
| **Related Business Rules** | BR-DM-021 (auto-approval parameters), BR-DM-022 (approval delegation levels), BR-DM-023 (draft decision workflow) |  |

**Preconditions:**

- An instalment agreement application has been submitted (UC-DM-06) with status "submitted."

- Approval parameters are configured: auto-approval thresholds, delegation levels per amount, and draft decision workflow rules.

- The reviewing officer is authenticated and authorised for instalment agreement approval.

**Main Success Scenario:**

- The system evaluates the application against auto-approval parameters per BR-DM-021 (e.g., debt <€5,000, duration ≤12 months, first instalment application for this taxpayer).

- If auto-approval criteria are met, the system automatically approves the agreement: status changes to "approved," the payment schedule is activated, and the taxpayer is notified (go to step 8).

- If auto-approval criteria are not met, the system routes the application to the designated approver based on BR-DM-022 delegation levels (amount-based: DMO for <€20,000, SDO for ≥€20,000).

- The reviewing officer (DMO or SDO) opens the application and reviews: taxpayer profile and compliance history (retrieved from ORS/ClickHouse), current debt composition and age, proposed payment schedule, calculated interest, and risk profile.

- The reviewing officer may alter the proposed payment schedule per DM-FR-024: adjusting instalment amounts, frequency, duration, or start date. The system recalculates interest and totals for any modifications.

- The officer generates a draft decision with the MTCA-proposed schedule and sends it to the Taxpayer for review via the configured channel (portal notification, email).

- The Taxpayer reviews the draft decision and responds: accept (application proceeds to approval) or reject (application returns to negotiation or is cancelled by the Taxpayer).

- Upon approval (automatic or manual), the system: activates the payment schedule with specific due dates and amounts, posts the instalment interest per DM-FR-025, sets the compliance monitoring per UC-DM-08, pauses any active enforcement actions against the covered debts per DM-FR-028, and generates the formal agreement document.

- The system notifies the Taxpayer of the approved agreement with the confirmed schedule.

- The system records the approval decision with: approving officer/system, date, any modifications made, and version history of all schedule changes per DM-FR-024.

**Alternative Flows:**

- **AF-1: Application Rejected.** At step 4, if the reviewing officer determines the application is not viable (taxpayer history suggests inability to comply, debt structure unsuitable), the officer rejects the application with documented reasons. The system notifies the Taxpayer and returns the debt case to the standard enforcement workflow.

- **AF-2: Counter-Proposal Negotiation.** At step 7, the Taxpayer rejects the MTCA draft. The system allows the Taxpayer to submit a counter-proposal (modified schedule). The process loops back to step 4 for officer review. Maximum iteration count is configurable (e.g., 3 rounds).

- **AF-3: Escalated Approval.** At step 3, if the debt amount exceeds the DMO's approval authority, the system routes to the SDO. The SDO reviews the DMO's recommendation and makes the final decision.

**Exception Flows:**

- **EF-1: Taxpayer Non-Response to Draft.** At step 7, if the Taxpayer does not respond within the configured period (e.g., 14 days), the system sets the application to "expired — no response" and notifies the DMO. The debt case returns to the standard workflow.

- **EF-2: Balance Change During Review.** At step 4, if additional liabilities are posted to the taxpayer's account during the review period, the system alerts the reviewer. The application scope may need to be expanded to include the new debts.

**Postconditions:**

- The instalment agreement is either approved and activated, rejected, or in draft review.

- If approved: the payment schedule is active, compliance monitoring is initiated, and enforcement is paused for covered debts.

- All decision details and schedule version history are recorded in the audit trail.

### UC-DM-08: Monitor Instalment Agreement Compliance

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary), Debt Management Officer (non-compliance review) |  |
| **Priority** | Must |  |
| **Frequency** | Continuous (event-driven on payment dates) |  |
| **Related Requirements** | DM-FR-026, DM-FR-025, DM-FR-028, DM-FR-051 |  |
| **Related Business Rules** | BR-DM-024 (compliance evaluation rules), BR-DM-025 (non-compliance grace period), BR-DM-026 (interest rate reversion trigger) |  |

**Preconditions:**

- An active, approved instalment agreement exists (UC-DM-07 completed successfully).

- Payment schedule dates and amounts are recorded in the system.

- Payment matching is operational (payments received are matched to instalment schedule).

**Main Success Scenario:**

- The system monitors each instalment due date as it arrives.

- On or before the due date, the system checks whether a payment has been received that matches or exceeds the scheduled instalment amount per BR-DM-024.

- If the payment received ≥ scheduled amount by the due date: the system marks the instalment as "paid/compliant," updates the remaining balance, recalculates interest on the reduced principal, and updates the compliance dashboard.

- The system continues monitoring the next scheduled instalment date.

- At each instalment, the system updates the agreement compliance dashboard showing: total amount paid to date, number of instalments paid vs. remaining, current compliance status (green/amber/red), and projected completion date.

- Notifications are sent to the Taxpayer confirming receipt and updated balance per DM-FR-051.

**Alternative Flows:**

- **AF-1: Late Payment Within Grace Period.** At step 2, if the due date passes without payment but payment is received within the configured grace period (e.g., 3 business days per BR-DM-025), the system treats the instalment as "late but compliant," records the late payment, and continues normal monitoring. A warning notification is sent to the Taxpayer.

- **AF-2: Partial Payment.** At step 2, a payment is received but is less than the scheduled instalment amount. The system marks the instalment as "partially paid," alerts the assigned DMO and the Taxpayer within the configured period (e.g., 3 business days per DM-FR-026). If the shortfall is not resolved within the grace period, the case follows AF-3.

- **AF-3: Non-Compliance Alert.** At step 2, if no payment or insufficient payment is received after the grace period, the system generates a non-compliance alert to: the assigned DMO (worklist notification), the Taxpayer (email/SMS/portal notification specifying the missed payment and consequences), and the supervisor (if configured for the debt amount). The agreement status changes to "non-compliant."

- **AF-4: Overpayment.** At step 2, the payment received exceeds the scheduled instalment amount. The excess is applied to the next instalment(s), reducing the remaining balance. The agreement completion date may be recalculated.

**Exception Flows:**

- **EF-1: Payment Matching Failure.** At step 2, a payment is received but cannot be automatically matched to the instalment schedule (wrong reference, lump sum covering multiple periods). The system routes the unmatched payment to the suspense account and alerts the DMO for manual allocation.

- **EF-2: Consecutive Non-Compliance.** At AF-3, if the non-compliance persists for a configurable number of consecutive instalments (e.g., 2), the system initiates automatic cancellation per UC-DM-09.

**Postconditions:**

- Instalment compliance status is updated for each scheduled payment date.

- The compliance dashboard reflects current agreement status.

- Non-compliance events are recorded and appropriate parties are notified.

- Interest continues to accrue at the reduced rate while compliant, or reverts to the standard rate upon non-compliance.

### UC-DM-09: Cancel Instalment Agreement and Create Recovery Case

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (automatic cancellation), Debt Management Officer (manual cancellation), Taxpayer (voluntary cancellation) |  |
| **Priority** | Must |  |
| **Frequency** | ~5–15/day |  |
| **Related Requirements** | DM-FR-027, DM-FR-022, DM-FR-025, DM-FR-028 |  |
| **Related Business Rules** | BR-DM-027 (auto-cancellation triggers), BR-DM-028 (interest rate reversion), BR-DM-029 (recovery case creation rules) |  |

**Preconditions:**

- An active instalment agreement exists.

- A cancellation trigger has occurred: non-compliance (automatic), officer decision (manual), or taxpayer request (voluntary).

**Main Success Scenario:**

- The system detects a cancellation trigger per BR-DM-027: the taxpayer has missed a configurable number of consecutive instalment payments after the grace period, OR a DMO initiates manual cancellation with documented justification, OR the Taxpayer requests cancellation via the portal or in-person.

- The system calculates the outstanding balance as of the cancellation date: remaining principal (original debt minus payments received), accrued interest at the reduced rate up to the cancellation date, and interest adjustment — the system reverts the interest rate from the reduced instalment rate to the standard rate from the cancellation date forward per BR-DM-028 / DM-FR-025.

- The system changes the instalment agreement status to "cancelled" with: cancellation date, reason (non-compliance/manual/voluntary), cancelling party (system/officer/taxpayer), and outstanding balance at cancellation.

- The system automatically creates a new debt recovery case per DM-FR-027 / BR-DM-029 with: the calculated outstanding balance, full history of the cancelled agreement (payments made, non-compliance events), original debt case reference, and assignment to the appropriate enforcement worklist based on the current debt category.

- The enforcement block per DM-FR-028 is lifted — the debt is now eligible for all enforcement actions appropriate to its category.

- The system notifies the Taxpayer of the agreement cancellation, the outstanding balance, and the consequences (enforcement eligibility).

- The system notifies the assigned DMO of the new recovery case.

- The system removes the previous active agreement lock per DM-FR-022, allowing the Taxpayer to submit a new instalment application in the future (subject to approval).

**Alternative Flows:**

- **AF-1: Voluntary Cancellation by Taxpayer.** The Taxpayer requests cancellation via the portal. The system requests confirmation ("This will cancel your instalment agreement and the full remaining balance will become immediately due. Confirm?"). Upon confirmation, the process proceeds from step 2.

- **AF-2: Partial Fulfilment Recognition.** At step 2, if the Taxpayer has paid a significant portion of the original debt (e.g., >75%), the DMO may choose to apply special handling: extending the agreement with modified terms rather than full cancellation. This requires SDO approval.

**Exception Flows:**

- **EF-1: Concurrent Payment Received.** At step 1, while the system is processing automatic cancellation, a payment is received that would bring the agreement back into compliance. The system checks the payment timing — if received before the cancellation is finalised, the cancellation is aborted and the agreement continues with a warning. If received after cancellation, the payment is applied to the new recovery case.

**Postconditions:**

- The instalment agreement is cancelled and recorded with full history.

- A new debt recovery case is created with the outstanding balance.

- The interest rate has reverted to the standard rate.

- The enforcement block is lifted.

- The taxpayer is eligible to submit a new instalment application.

### UC-DM-10: Escalate Debt Case for Enforcement

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (automatic escalation), Debt Management Officer (manual escalation), Senior Debt Officer (approval for high-value) |  |
| **Priority** | Must |  |
| **Frequency** | ~50–100/day |  |
| **Related Requirements** | DM-FR-015, DM-FR-016, DM-FR-017, DM-FR-018, DM-FR-019, DM-FR-020, DM-FR-039, DM-FR-050 |  |
| **Related Business Rules** | BR-DM-030 (escalation trigger rules), BR-DM-031 (enforcement action applicability by category), BR-DM-032 (worklist assignment rules) |  |

**Preconditions:**

- A debt case exists where all automated pre-enforcement stages (reminders, demands) have been exhausted without resolution.

- The case is in the appropriate workflow state for enforcement escalation.

- Enforcement action types are configured per DM-FR-039 with applicable debt size ranges, taxpayer types, and document templates.

**Main Success Scenario:**

- The system identifies debt cases eligible for enforcement escalation: final demand response period has expired without payment per BR-DM-030, and no active instalment agreement or objection exists.

- The system transitions the case workflow state from "demand exhausted" to "enforcement — pending assignment" using the FSM workflow per DM-FR-015.

- The system evaluates the applicable enforcement actions based on the debt category per BR-DM-031 and Table 4 (proportional enforcement): C2 — demand notice, refund interception; C3 — plus phone calls, payment arrangements, bank garnishing, publishing name; C4 — plus taxpayer visits, lien on assets, third-party claims, passport seizure, property seizure, bankruptcy demand; C5 — same as C4 with all actions available.

- The system assigns the case to the appropriate manual processing worklist per DM-FR-016, based on: debt category and risk score, officer specialisation (e.g., high-value cases to senior enforcement team), and workload balancing across available officers per BR-DM-032.

- The assigned DMO receives a notification with: case summary, full history of automated actions taken (retrieved from the case audit trail), recommended enforcement actions, and applicable document templates.

- The DMO reviews the case and selects the enforcement action(s) to pursue.

- The system logs the escalation event with timestamp, triggering condition, assigned officer, and available actions.

**Alternative Flows:**

- **AF-1: Immediate Escalation for High-Value Debts.** Per DM-FR-018, debts exceeding the high-value threshold bypass the standard pre-enforcement stages and are escalated immediately. The system creates a "critical" priority case, assigns it to a senior officer, and sets an SLA for telephone contact within 2 business days. The SDO approves the enforcement strategy.

- **AF-2: Telephone Contact (C3–C5).** After escalation, the DMO initiates telephone contact per DM-FR-019. The system provides a contact recording form: date/time, officer ID, TIN, contact person, method (phone/visit), outcome code (promise to pay, dispute, unable to contact, no answer, wrong number), free-text notes, and follow-up date. The contact record is attached to the debt case.

- **AF-3: Taxpayer Visit (C4–C5).** For higher-value debts, an Enforcement Officer is assigned to conduct a taxpayer visit per DM-FR-020. The visit is scheduled in the system, and upon completion, the EO files a visit report: date, location, persons present, outcome, evidence collected, and follow-up actions.

- **AF-4: Multiple Simultaneous Actions.** For C4–C5 cases, the DMO may initiate multiple enforcement actions simultaneously (e.g., bank garnishing + publishing name). The system tracks each action independently on the case timeline.

**Exception Flows:**

- **EF-1: Instalment Application Filed During Escalation.** After escalation but before enforcement action is executed, the Taxpayer submits an instalment agreement application. The system suspends enforcement progression and routes the application to UC-DM-07. Enforcement resumes only if the application is rejected or the agreement is later cancelled.

- **EF-2: Payment Received Post-Escalation.** If full payment is received after escalation but before enforcement action is executed, the system resolves the case, cancels pending enforcement, and closes the case.

- **EF-3: Officer Capacity Exceeded.** At step 4, if all officers in the assigned specialisation are at capacity, the system queues the case and alerts the supervisor. Cases continue to age and may be re-prioritised by risk score.

**Postconditions:**

- The debt case has been escalated from automated to manual enforcement processing.

- The case is assigned to an officer with appropriate specialisation and capacity.

- The applicable enforcement actions are identified based on the debt category.

- Full case history is available to the assigned officer.

## Extraction Summary

| **Metric** | **Value** |  |
| --- | --- | --- |
| **Use cases extracted** | 10 (UC-DM-01 to UC-DM-10) |  |
| **All priorities** | Must (10/10) |  |
| **STA→ORS adaptations applied** | 9 instances |  |
| **Platform-agnostic adaptations** | 1 instance (System actor description) |  |
| **Requirement IDs preserved** | All DM-FR-xxx, BR-DM-xxx, UC-DM-xx references intact |  |
| **External dependencies documented** | ORS/ClickHouse, SAS VIYA, self-service portal, postal service |  |
| **Remaining for Part 2** | UC-DM-11 to UC-DM-20 + RPT/WF/ADM use cases |  |

# 7. Use Case Model — Part 2: Debt Management Use Cases UC-DM-11 to UC-DM-20 + Reporting, Workflow, Administration

> **Extraction Step:** 07b

> **Source:** Source 1 — STA/DM Requirements Specification, Sections 6.2 (UC-DM-11 to UC-DM-20), 6.3 (Reporting), 6.4 (Workflow), 6.5 (Administration)

> **Adaptations Applied:** STA→ORS/ClickHouse reframing (rule #30–31), platform-agnostic language (rule #34), ID preservation (rule #32), UC-RPT-04 excluded (rule #5, #27)

> **Use Cases in This File:** 10 DM (UC-DM-11 to UC-DM-20) + 4 RPT + 3 WF + 3 ADM = 20 use cases

## 7.3 Debt Management Use Cases (continued)

### UC-DM-11: Process Bank Account Freezing Request

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Debt Management Officer (primary), System (automated generation), External System (bank web services) |  |
| **Priority** | Must |  |
| **Frequency** | ~10–30/day |  |
| **Related Requirements** | DM-FR-029, DM-FR-038, DM-FR-040, DM-FR-050 |  |
| **Related Business Rules** | BR-DM-033 (garnishing eligibility criteria), BR-DM-034 (bank integration protocol), BR-DM-035 (garnished amount posting rules) |  |

**Preconditions:**

- The debt case is in category C3–C5 and has been escalated for enforcement (UC-DM-10 completed).

- The enforcement action "bank account garnishing" is applicable per the debt category and approved by the DMO or SDO.

- Bank web services integration is operational per DM-FR-029.

- The taxpayer's bank account details are available (from registration data, third-party data, or prior enforcement records).

**Main Success Scenario:**

- The DMO selects "Bank Account Garnishing" as the enforcement action from the case enforcement menu.

- The system displays the taxpayer's known bank accounts (bank name, account identifiers) and the current outstanding debt amount.

- The DMO confirms the garnishing request, specifying: target bank(s), debt amount to be frozen, and legal reference.

- The system generates a formal freezing request document using the configured template per DM-FR-040, populated with: TIN, taxpayer name, bank details, debt amount, legal basis, MTCA authority reference, and effective date.

- The system transmits the freezing request to the bank via web services per BR-DM-034.

- The bank responds (synchronously or asynchronously per the integration protocol) with: success (account frozen, frozen amount), partial success (account frozen, available amount less than requested), or failure (account not found, insufficient balance, other reason).

- The system records the bank response on the debt case: request date/time, bank, response, frozen amount, response date/time.

- If successful, the system monitors for the garnished amounts to be transferred. Upon receipt, the amounts are posted to the taxpayer's account (reflected in ORS) per BR-DM-035, reducing the outstanding debt.

- Any applicable legal fees per DM-FR-038 are posted to the taxpayer's ledger.

- The system updates the case timeline and notifies the DMO of the outcome.

**Alternative Flows:**

- **AF-1: Multiple Bank Requests.** The DMO initiates garnishing requests to multiple banks simultaneously. The system tracks each request independently and consolidates the outcomes on the case.

- **AF-2: Automated Batch Garnishing.** For cases meeting configurable criteria (e.g., all C4+ cases with no response after final demand), the system can generate batch garnishing requests for SDO approval before transmission.

- **AF-3: Garnish Release.** Upon debt resolution (full payment, instalment agreement, write-off), the DMO or system generates a release request to the bank. The system transmits the release via web services and records the release confirmation.

**Exception Flows:**

- **EF-1: Bank Service Unavailable.** At step 5, if the bank web service is unreachable, the system queues the request for retry and alerts the DMO. If the service remains unavailable after the retry period, the DMO may generate a manual (paper) request.

- **EF-2: Invalid Bank Details.** At step 2, if the bank account details are invalid or outdated, the DMO can update the details or initiate a bank account inquiry through alternative channels.

- **EF-3: Objection to Garnishing.** The Taxpayer files an objection to the garnishing order. The system suspends the garnishing pending objection review per BR-DM-016 and routes the objection to UC-DM-16.

**Postconditions:**

- A freezing request has been transmitted to the bank and the response recorded.

- Garnished amounts (if any) have been posted to the taxpayer's account.

- Legal fees have been posted to the taxpayer's ledger.

- The case timeline reflects the enforcement action and outcome.

### UC-DM-12: Process Write-Off

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (automatic C1 write-off), Debt Management Officer (initiate), Senior Debt Officer (approval), System Administrator (bulk configuration) |  |
| **Priority** | Must |  |
| **Frequency** | ~50–200/day (automatic C1), ~10–20/day (manual), periodic bulk operations |  |
| **Related Requirements** | DM-FR-042, DM-FR-043, DM-FR-044, DM-FR-045, DM-FR-046 |  |
| **Related Business Rules** | BR-DM-036 (C1 auto write-off), BR-DM-037 (write-off approval delegation), BR-DM-038 (statutory collection period), BR-DM-039 (write-off evidence requirements) |  |

**Preconditions:**

- A write-off trigger exists: C1 debt (<€30) for automatic write-off, C2 debt after statutory period for passive collection write-off, or C3–C5 debt where all enforcement actions have been exhausted.

- Write-off approval delegation levels are configured per BR-DM-037.

- Write-off status and accounting codes are configured.

**Main Success Scenario (Automatic C1 Write-Off):**

- The system identifies debts classified as C1 (<€30) per DM-FR-042.

- The system automatically processes the write-off: the taxpayer account is updated via ORS (debit balance reduced by the written-off amount), a write-off transaction is posted to both the taxpayer ledger and the revenue accounts, the debt case is closed with reason "uneconomic to collect," and a write-off audit record is generated.

- No human approval is required per BR-DM-036.

- The system generates a write-off report for audit purposes.

**Alternative Flows:**

- **AF-1: Approved Write-Off (C3–C5).** A DMO initiates a write-off request for a larger debt where enforcement has been exhausted per DM-FR-043. The DMO submits the request with: supporting evidence (enforcement history, inability to collect documentation), decision rationale, and amount to be written off. The system routes the request to the appropriate approver based on amount thresholds per BR-DM-037 (e.g., DMO up to €1,000, SDO up to €20,000, Director for higher amounts). The approver reviews the evidence and either approves or rejects. If approved, the system posts the write-off transactions. If rejected, the case is returned for alternative action.

- **AF-2: C2 Passive Collection Write-Off.** C2 debts (€30–100) per DM-FR-045 that remain uncollected after automated actions are set to "passive collection" status. The system monitors for refund interception opportunities (if the taxpayer becomes entitled to a refund, it is intercepted against the debt). After the statutory collection period expires (configurable years per BR-DM-038), the system flags these debts as eligible for bulk write-off.

- **AF-3: Bulk Write-Off Operation.** Per DM-FR-044, a System Administrator configures bulk write-off criteria (e.g., all C1 debts, all C2 debts beyond statutory period). The system generates a batch list for SDO or Director approval. Upon approval, the system processes all matching debts in bulk, generating individual audit entries for each and a summary report for the approver.

- **AF-4: Annual Balancing Write-Off.** At year-end, the system identifies accounts with trivial residual balances (per system-defined limits) eligible for balancing write-off per DM-FR-044. These are processed as a bulk operation with appropriate approval.

**Exception Flows:**

- **EF-1: Write-Off Approval Rejected.** At AF-1, the approver rejects the write-off. The system returns the case to the DMO with the rejection reason. The DMO must pursue alternative enforcement actions or provide additional evidence for a renewed write-off request.

- **EF-2: Balance Change During Approval.** At AF-1, while the write-off request is pending approval, a payment is received that reduces the balance. The system alerts the approver of the balance change. If the balance is now zero, the write-off request is cancelled. If reduced, the approver may adjust the write-off amount.

**Postconditions:**

- The debt has been written off and the write-off status is recorded per DM-FR-046.

- Write-off transactions are posted to both taxpayer and revenue accounts.

- Full debt history, enforcement actions, and write-off decision are preserved for audit.

- Written-off amounts are reportable by: tax type, period, category, reason, officer, and date.

- The debt case is closed.

### UC-DM-13: Manage Debt Recovery Worklist

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Debt Management Officer (primary), Senior Debt Officer (supervision), System |  |
| **Priority** | Must |  |
| **Frequency** | Continuous (primary daily interface for debt officers) |  |
| **Related Requirements** | DM-FR-016, DM-FR-006, DM-FR-048, DM-FR-050, DM-FR-051, DM-FR-057 |  |
| **Related Business Rules** | BR-DM-032 (worklist assignment rules), BR-DM-040 (SLA definitions), BR-DM-041 (workload balancing parameters) |  |

**Preconditions:**

- The DMO is authenticated and authorised with appropriate role-based access.

- Debt cases exist in the system and are assigned to worklists.

- Risk scoring has been completed (UC-DM-05).

**Main Success Scenario:**

- The DMO logs in and opens their personal debt recovery worklist.

- The system displays the worklist sorted by risk score (highest first, default) showing for each case: case ID, TIN, taxpayer name, total debt amount, debt category (C1–C5), enforcement stage (current workflow state), risk score, age of oldest debt, SLA status (on-time/approaching/overdue), last action date and type, and next action due.

- The DMO applies filters to focus their work: by debt category, by enforcement stage, by tax type, by SLA status (e.g., only overdue items), or by custom criteria.

- The DMO selects a case to work on. The system displays the full case detail view including: taxpayer profile summary (sourced from ORS/ClickHouse), complete debt breakdown by tax type and period, chronological action history (all automated and manual actions), attached documents, and available next actions based on current workflow state and debt category.

- The DMO performs the required action (e.g., initiates phone contact per UC-DM-10 AF-2, initiates garnishing per UC-DM-11, prepares write-off per UC-DM-12) and records the outcome.

- The system updates the case status, records the action, and recalculates the worklist position.

- The DMO returns to the worklist and proceeds to the next case.

**Alternative Flows:**

- **AF-1: Supervisor View.** A Senior Debt Officer views the aggregate worklist across all officers under their supervision. The view shows: per-officer case counts and SLA compliance, aggregate statistics by debt category and enforcement stage, and cases requiring supervisor attention (approval requests, SLA breaches, high-value escalations). The SDO can reassign cases between officers.

- **AF-2: Debtors List View.** Per DM-FR-057, the DMO or SDO accesses the full debtors list showing all taxpayers with outstanding debts, filterable and sortable by: debt amount, debt age, tax type, taxpayer type, economic sector, enforcement stage, and risk score. The list supports export to Excel/CSV.

- **AF-3: Case Reassignment.** The SDO reassigns a case from one DMO to another (e.g., due to leave, workload balancing, specialisation). The system transfers the case, notifies both officers, and preserves the complete case history.

- **AF-4: Batch Action.** The DMO selects multiple cases matching criteria (e.g., all C3 cases awaiting phone contact) and initiates a batch action (e.g., generate phone contact task list, schedule batch demand notice generation).

**Exception Flows:**

- **EF-1: SLA Breach.** At step 2, if a case's SLA deadline has passed, the system highlights it in red and generates an alert to both the assigned DMO and the supervisor per DM-FR-051. The breach is recorded for management reporting.

- **EF-2: Zero Active Cases.** At step 1, if the DMO has no active cases assigned, the system displays an empty worklist and prompts the DMO to check for unassigned cases or contact their supervisor.

**Postconditions:**

- The DMO has reviewed and acted on prioritised debt cases.

- Case statuses and worklist positions are updated.

- All actions are recorded in the audit trail.

- SLA compliance is tracked and reportable.

### UC-DM-14: Generate Debt Collection Plan

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Senior Debt Officer (primary), System (data aggregation) |  |
| **Priority** | Should |  |
| **Frequency** | Monthly/quarterly (strategic planning cycle) |  |
| **Related Requirements** | DM-FR-047, DM-FR-048, DM-FR-049 |  |
| **Related Business Rules** | BR-DM-042 (plan structure requirements), BR-DM-043 (target setting methodology) |  |

**Preconditions:**

- Debt management data is available from ORS/ClickHouse per DM-FR-049.

- Risk scoring and classification data is current (UC-DM-05).

- Historical collection performance data is available for trend analysis.

**Main Success Scenario:**

- The SDO navigates to the Collection Plan module and selects the scope: local (specific office/region) or national.

- The system extracts current debt stock data from ORS/ClickHouse per DM-FR-049 and presents a baseline analysis: total outstanding debt by category (C1–C5), debt age distribution (current period, 1–3 months, 3–6 months, 6–12 months, >12 months), debt composition by tax type, taxpayer segment (NP/LP), and economic sector, and historical recovery rates by enforcement action type.

- The SDO defines the collection plan parameters: target recovery amounts by category and tax type per BR-DM-043, resource allocation (number of officers by specialisation), enforcement strategy by taxpayer segment (e.g., NP C3 — prioritise phone contact; LP C4 — prioritise garnishing), and timeline (quarterly milestones).

- The system validates the plan against available resources and historical recovery rates, flagging any targets that appear unrealistic based on prior performance.

- The SDO finalises the plan and submits for approval (if required per organisational hierarchy).

- The system saves the collection plan with version control and establishes plan-vs-actual tracking.

- The system generates a plan summary report showing: target amounts, strategy overview, resource requirements, and projected timelines.

**Alternative Flows:**

- **AF-1: Plan-vs-Actual Tracking.** On an ongoing basis, the system tracks actual recovery performance against the plan targets. The SDO can view: target vs. actual recovered amounts by category, enforcement action effectiveness (actions taken vs. amounts recovered), officer productivity metrics, and variance analysis with explanatory notes.

- **AF-2: Mid-Period Plan Adjustment.** The SDO reviews plan-vs-actual performance and adjusts targets or strategies mid-period. Changes are version-controlled with justification.

**Exception Flows:**

- **EF-1: Insufficient Historical Data.** At step 2, if historical collection data is not available (e.g., first plan period), the system provides only current stock analysis and allows the SDO to set targets manually without historical benchmarking.

**Postconditions:**

- A debt collection plan is created with defined targets, strategies, and resource allocations.

- Plan-vs-actual tracking is established.

- The plan is available for management reporting and performance monitoring.

### UC-DM-15: Appoint Agent (Warrant)

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Senior Debt Officer (primary), Debt Management Officer (initiation), System |  |
| **Priority** | Should |  |
| **Frequency** | ~5–15/month |  |
| **Related Requirements** | DM-FR-037, DM-FR-038, DM-FR-039, DM-FR-050 |  |
| **Related Business Rules** | BR-DM-044 (warrant appointment criteria), BR-DM-045 (agent commission/fee schedule), BR-DM-046 (agent activity tracking requirements) |  |

**Preconditions:**

- The debt case is in category C4–C5 and standard enforcement actions have been unsuccessful or are inappropriate.

- The enforcement action "Appoint Agent (Warrant)" is available for the debt category per DM-FR-039.

- An approved list of agents is maintained in the system.

- SDO approval authority is confirmed.

**Main Success Scenario:**

- The DMO identifies a debt case where agent appointment is appropriate (e.g., debtor unresponsive to direct enforcement, debtor assets identified but requiring specialist recovery).

- The DMO prepares a warrant appointment request: case summary, debt amount, debtor details, reason for agent appointment, and recommended agent (from the approved list).

- The request is routed to the SDO for approval.

- The SDO reviews the case, approves the appointment, and selects the agent.

- The system generates the warrant document per DM-FR-040: defining the agent's scope (specific debts, specific assets, geographic area), authority limits, commission/fee terms per BR-DM-045, and reporting requirements.

- The system records the appointment on the debt case: agent name, appointment date, warrant reference, scope, and terms.

- The assigned agent's activities are tracked in the system per BR-DM-046: actions taken (visits, negotiations, asset recovery), recovered amounts (posted to the taxpayer's account, reflected in ORS), agent reports and evidence uploaded per DM-FR-052, and agent fees/commission calculated and tracked separately per DM-FR-037.

- The system monitors the warrant period and alerts the DMO when the warrant is approaching expiry or when the agent submits a completion report.

**Alternative Flows:**

- **AF-1: Agent Reports Recovery.** The agent reports successful recovery of funds. The DMO records the recovered amount, which is posted to the taxpayer's account (reflected in ORS). The agent's commission is calculated per BR-DM-045 and recorded separately. The remaining debt (if any) is updated on the case.

- **AF-2: Warrant Termination.** The SDO terminates the warrant before completion (e.g., debtor resolves debt directly, agent non-performance). The system records the termination, finalises the agent's account (amounts recovered, commission due), and returns the case to standard enforcement.

**Exception Flows:**

- **EF-1: Agent Non-Compliance.** The agent fails to report within required timeframes. The system alerts the SDO. If non-compliance continues, the SDO may terminate the warrant and appoint a different agent.

- **EF-2: Debtor Objection to Warrant.** The debtor files an objection to the warrant appointment. The case is routed to legal review, and warrant activities may be suspended pending resolution.

**Postconditions:**

- An agent is appointed with a defined warrant scope and terms.

- Agent activities and recovered amounts are tracked on the debt case.

- Agent fees/commission are calculated and recorded separately from debt amounts.

- The warrant lifecycle (appointment, activities, completion/termination) is fully documented.

### UC-DM-16: Manage Objection to Debt

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Taxpayer (primary — submission), Debt Management Officer (review), Senior Debt Officer (decision), System |  |
| **Priority** | Must |  |
| **Frequency** | ~10–30/day |  |
| **Related Requirements** | DM-FR-008, DM-FR-028, DM-FR-015, DM-FR-050, DM-FR-052 |  |
| **Related Business Rules** | BR-DM-047 (objection eligibility criteria), BR-DM-048 (objection effect on enforcement), BR-DM-049 (undisputed amount treatment), BR-DM-050 (objection decision rules) |  |

**Preconditions:**

- The taxpayer has an active debt case with an assessed or demanded amount.

- The objection period has not expired (configurable per tax type and assessment type).

- The objection management module is operational.

**Main Success Scenario:**

- The Taxpayer submits an objection through the self-service portal (or paper submission entered by DMO): selecting the specific assessment(s) or debt amount(s) being objected to, providing grounds for the objection, specifying the disputed amount vs. undisputed amount, and uploading supporting documents per DM-FR-052.

- The system validates the objection: submission within the allowable period per BR-DM-047, all required fields completed, and at least one assessment or debt amount identified for objection.

- The system creates an objection case linked to the debt case: objection ID, date submitted, disputed amount, undisputed amount, grounds, and status "submitted."

- The system automatically excludes the disputed amount from enforcement per BR-DM-048 / DM-FR-008. The undisputed amount remains subject to the standard enforcement workflow per BR-DM-049.

- The system assigns the objection case to the appropriate DMO (or a specialised objections team) within a configurable timeframe per the Case Management Platform's objection management capability.

- The DMO reviews the objection: examining the grounds and supporting documents, reviewing the original assessment basis, and consulting with the assessing officer if applicable.

- The DMO prepares a recommendation: approve (full or partial), reject, or request additional information from the Taxpayer.

- If the recommendation requires SDO approval (e.g., above delegated authority), the case is escalated.

- The final decision is recorded: if approved (full) — the disputed debt is reversed, the debt case is updated, and if no debt remains, the case is closed; if approved (partial) — the approved portion is reversed, the remaining disputed amount is confirmed as payable and returns to the enforcement workflow; if rejected — the full disputed amount is confirmed as payable and returns to the enforcement workflow. The Taxpayer is notified of the decision with appeal rights.

- The system updates the debt case to reflect the objection outcome and resumes enforcement for any confirmed payable amounts.

**Alternative Flows:**

- **AF-1: Objection Due Date Extension.** The Taxpayer or DMO requests an extension of the objection due date. The Case Management Platform can extend the due date manually or by default business rules for a specified number of days. The extension is recorded on the objection case.

- **AF-2: Automatic Rejection.** The system automatically rejects the objection per configured rules if: the submission is past the due date (and no extension was granted), or the undisputed amount is not paid on time. The Taxpayer is notified with appeal rights.

- **AF-3: Escalation to Tax Tribunal.** If the Taxpayer disagrees with the MTCA objection decision, they may escalate to the Administrative Review Tribunal (ART). The system supports: initiating the tribunal process with standard forms, maintaining hearing calendars, attaching relevant documents, and recording the tribunal outcome. Further escalation to the Court of Appeal (COA) follows a similar process.

- **AF-4: Link to Audit Case.** If the debt originates from an audit assessment, the system links the objection case to the audit case for the reviewing officer to have full context.

**Exception Flows:**

- **EF-1: Incomplete Objection.** At step 2, if the objection is incomplete (missing grounds, missing assessment reference), the system saves the incomplete objection and notifies the Taxpayer to complete it within a configurable period. If not completed, the objection expires.

- **EF-2: Enforcement Action Already Executed.** At step 4, if an enforcement action (e.g., bank garnishing) has already been executed on the disputed amount, the system flags a conflict. The DMO reviews whether the enforcement action should be reversed pending the objection outcome.

**Postconditions:**

- An objection case is created and linked to the debt case.

- The disputed amount is excluded from enforcement pending review.

- The objection has been reviewed and decided (approved/rejected), or is pending review.

- The debt case is updated to reflect the objection outcome.

- The Taxpayer is notified of the decision with appeal rights.

### UC-DM-17: Process Default Assessment (Non-Filer)

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System (primary — auto-generated), Debt Management Officer (manual creation), Taxpayer (response/filing) |  |
| **Priority** | Should |  |
| **Frequency** | ~30–80/month (batch during compliance campaigns) |  |
| **Related Requirements** | DM-FR-054, DM-FR-055, DM-FR-056 |  |
| **Related Business Rules** | BR-DM-051 (default assessment estimation rules), BR-DM-052 (reasonableness checks), BR-DM-053 (return replacement rules) |  |

**Preconditions:**

- A taxpayer has not filed a required return after the filing due date and filing reminders have been issued without response.

- Default assessment estimation rules are configured (prior year data, industry averages).

- Reasonableness check thresholds are configured per BR-DM-052.

**Main Success Scenario:**

- The system identifies non-filers who have not responded to filing reminders: the filing due date has passed, and one or more filing reminders have been sent without the return being filed.

- The system generates a default (administrative) assessment per DM-FR-054 using configurable estimation rules per BR-DM-051: prior year return data (if available, sourced from ORS/ClickHouse), industry average for the taxpayer's sector and size, or a configurable fixed formula.

- The system applies reasonableness checks per BR-DM-052 to ensure the estimated amount is not excessively exaggerated (e.g., no more than 150% of prior year without justification).

- The default assessment is posted to the taxpayer's account (reflected in ORS) as a debit, with: assessment type flagged as "default/administrative," estimation method documented, legal reference included, and assessment notice generated and sent to the Taxpayer with objection rights and filing instructions.

- The system sets a response period (configurable) for the Taxpayer to either file the actual return or pay the assessed amount.

- The assessment enters the standard debt management workflow.

**Alternative Flows:**

- **AF-1: Manual Default Assessment.** A DMO creates a manual default assessment for cases where automated estimation is not appropriate (e.g., complex taxpayer, insufficient data for estimation). The DMO enters the assessed amount with documented justification. The assessment follows the same workflow from step 4.

- **AF-2: Taxpayer Files Return in Response.** Per DM-FR-055, the Taxpayer files the actual return in response to the default assessment. The system replaces the default assessment with the filed return: reverses the default assessment amount, posts the actual return amount, reconciles the difference, and updates the taxpayer's risk profile based on the variance between default and actual. If the filed amount is less than the default, the debt is adjusted. If the filed amount is more, the additional liability is posted.

- **AF-3: Taxpayer Files and Return Is Reliable.** The filed return passes reliability verification. The default assessment is fully replaced, and the case proceeds with the actual return figures.

- **AF-4: Taxpayer Files but Return Is Unreliable.** The filed return fails reliability verification (significant variance from expected, suspicious patterns). The system flags the return for audit review and updates the taxpayer's risk profile accordingly.

**Exception Flows:**

- **EF-1: No Estimation Data Available.** At step 2, if no prior year data or industry average is available for estimation, the system flags the case for manual assessment by a DMO.

- **EF-2: No Response — Enforcement.** Per DM-FR-056, if no filing occurs and no payment is received within the response period, the system creates a new enforced collection case on the basis of the default assessment amount and routes it to the standard enforcement workflow for the applicable debt category.

**Postconditions:**

- A default assessment has been created and posted to the taxpayer's account (reflected in ORS).

- The Taxpayer has been notified with filing and objection instructions.

- The assessment is in the debt management workflow, pending taxpayer response or enforcement.

- If the Taxpayer files, the default assessment is replaced with the actual return.

### UC-DM-18: Close Debt Recovery Case

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Debt Management Officer (primary), System (automated closure), Senior Debt Officer (approval for complex closures) |  |
| **Priority** | Must |  |
| **Frequency** | ~50–150/day |  |
| **Related Requirements** | DM-FR-053, DM-FR-050, DM-FR-046, DM-FR-052 |  |
| **Related Business Rules** | BR-DM-054 (case closure eligibility), BR-DM-055 (closure reason codes), BR-DM-056 (archival rules) |  |

**Preconditions:**

- A debt recovery case exists with all associated debts resolved (paid, written off, or otherwise concluded).

- All enforcement actions associated with the case have been concluded.

- All required documents are attached to the case.

**Main Success Scenario:**

- The system identifies cases eligible for closure per BR-DM-054: all associated debt balances are resolved (zero balance or credit), all enforcement actions have been concluded (completed, cancelled, or withdrawn), and all required documents are attached per DM-FR-052.

- For automated closure (simple cases where balance = 0 and all actions completed): the system sets the case status to "closed," records the closure with: date, reason code per BR-DM-055 (e.g., "paid in full," "written off," "instalment completed"), final outcome, and resolving event (payment ID, write-off reference, etc.).

- For manual closure (complex cases): the DMO reviews the case, confirms all conditions are met, adds any final notes or documents, and initiates closure. The system validates closure eligibility.

- The closed case becomes read-only per DM-FR-053 — searchable and viewable but no modifications permitted.

- The system archives the case per BR-DM-056 retention rules (legal minimum retention period).

- The system updates the debtors list per DM-FR-057 — the taxpayer is removed from the active debtors list (or their total debt is reduced if other cases remain open).

- If the debtor was published on the debtors list per DM-FR-030, the system removes the debtor from the published list.

- The system logs the case closure event to the audit trail.

**Alternative Flows:**

- **AF-1: Closure with Credit Balance.** At step 1, after all debts are resolved, a credit balance remains on the taxpayer's account (e.g., from over-payment or reversed assessment). The system notes the credit balance and routes it to the refund/set-off process managed via the taxpayer's account in ORS. The debt case is closed independently.

- **AF-2: Partial Closure.** If the debt case covers multiple tax types and some are resolved while others remain, the DMO may request partial closure. The system splits the case: closed portion archived, remaining debts continue in a new or updated case.

- **AF-3: SDO Approval Required.** For cases above a configurable threshold (e.g., cases with enforcement history, cases with write-off components), closure requires SDO approval. The system routes the closure request to the SDO with the full case summary.

**Exception Flows:**

- **EF-1: Outstanding Balance Detected.** At step 1, the system detects a non-zero balance that has not been resolved. The system prevents closure and alerts the DMO to resolve the remaining balance.

- **EF-2: Pending Enforcement Action.** At step 1, an enforcement action is still pending (e.g., bank garnishing request sent, response not yet received). The system prevents closure until the pending action is concluded.

- **EF-3: Reopening Closed Case.** An authorised SDO reopens a previously closed case (e.g., written-off debt becomes recoverable due to changed debtor circumstances). The system creates a new case referencing the original closed case.

**Postconditions:**

- The debt recovery case is closed and archived.

- The case is read-only, searchable, and accessible for audit purposes.

- The debtors list is updated to reflect the closure.

- Published debtor entries are removed upon debt resolution.

- The full case history is preserved per legal retention requirements.

### UC-DM-19: Configure Enforcement Action Types

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System Administrator (primary) |  |
| **Priority** | Must |  |
| **Frequency** | Periodic (initial setup + quarterly/annual reviews) |  |
| **Related Requirements** | DM-FR-039, DM-FR-040, DM-FR-041, DM-FR-015 |  |
| **Related Business Rules** | BR-DM-057 (action type configuration rules), BR-DM-058 (template management rules) |  |

**Preconditions:**

- The SA is authenticated and authorised for system configuration.

- The Case Management Platform's workflow designer is operational.

**Main Success Scenario:**

- The SA navigates to the enforcement action configuration module.

- The SA creates or edits an enforcement action type per DM-FR-039, specifying: action name and unique code, applicable debt size range (e.g., €1,000–€200,000), applicable taxpayer type (NP, LP, or both), document template reference, and specific instructions for officers executing the action.

- The system validates the configuration: no overlapping codes, debt size ranges do not conflict, and referenced templates exist.

- The SA assigns the action type to the appropriate workflow stage(s) in the Case Management Platform's workflow designer per DM-FR-015.

- The SA configures or updates the document template per DM-FR-040: creating/editing the template with merge fields (taxpayer name, TIN, debt amount, tax type, period, legal references, dates), configuring language variants (English, Maltese), setting batch generation support, and enabling preview before generation.

- The SA configures demand notice consolidation rules per DM-FR-041: separate notices per tax type (default), consolidated across selected tax types, or fully consolidated single notice.

- The system saves the configuration with version control. Changes are auditable.

- The SA tests the new/modified action type in a non-production environment before deployment.

**Alternative Flows:**

- **AF-1: Template Update.** The SA updates an existing template to reflect legislative changes. The system preserves the previous version and activates the new version from a specified effective date. Historical notices retain the template version used at generation time.

- **AF-2: Deactivate Action Type.** The SA deactivates an enforcement action type that is no longer applicable. Active cases using the action type are not affected; only new cases are prevented from using the deactivated action.

**Exception Flows:**

- **EF-1: Template Merge Field Error.** At step 5, if a merge field references a data element that does not exist, the system displays a validation error and prevents template save until corrected.

- **EF-2: Workflow Dependency.** At AF-2, if the action type being deactivated is a required step in an active workflow, the system warns the SA and requires workflow modification before deactivation.

**Postconditions:**

- Enforcement action types are configured and available for use in debt management workflows.

- Document templates are configured with merge fields and language variants.

- Demand notice consolidation rules are set.

- All configuration changes are version-controlled and auditable.

### UC-DM-20: Track and Record Enforcement Activities

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | Debt Management Officer (primary), Enforcement Officer (field activities), System |  |
| **Priority** | Must |  |
| **Frequency** | Continuous (every enforcement action recorded) |  |
| **Related Requirements** | DM-FR-019, DM-FR-020, DM-FR-050, DM-FR-051, DM-FR-052, DM-FR-005 |  |
| **Related Business Rules** | BR-DM-059 (activity logging requirements), BR-DM-060 (document attachment rules) |  |

**Preconditions:**

- A debt case is in the enforcement stage with one or more enforcement actions in progress.

- The acting officer is authenticated and authorised.

**Main Success Scenario:**

- The DMO or EO opens the debt case and navigates to the enforcement activities section.

- The officer records an enforcement activity. The system provides structured forms for each activity type:

**For telephone contacts (DM-FR-019):** date and time of contact, officer ID, TIN and contact person name, contact method (phone, mobile, video), outcome code (promise to pay — with date and amount, dispute — routed to objection, unable to contact — rescheduled, no answer, wrong number, other), free-text notes, and follow-up date and action.

**For taxpayer visits (DM-FR-020):** visit date and time, location/address, persons present (taxpayer, representative, witnesses), outcome (cooperation, partial cooperation, refused access, premises vacant, other), evidence collected (photographs, documents, asset inventory), follow-up actions required, and GPS/location (optional).

**For document service:** service date and method (personal, postal registered, posted to portal), proof of delivery reference, and recipient confirmation.

- The officer attaches any supporting documents per DM-FR-052 (scanned correspondence, evidence, photographs, court documents) with document type, description, and upload date.

- The system timestamps and records the activity immutably per DM-FR-050: actor (system or officer ID), action type, case ID, TIN, before/after state, and outcome.

- The system updates the case timeline and notifies relevant parties per DM-FR-051: the assigned officer (confirmation of recorded activity), the supervisor (if SLA-related or outcome requires escalation), and the Taxpayer (if the outcome warrants notification).

- The system preserves the complete enforcement activity history as part of the taxpayer's debt history per DM-FR-005.

**Alternative Flows:**

- **AF-1: Batch Activity Recording.** After a series of phone calls, the DMO records multiple activities in sequence. The system supports quick-entry mode for efficient batch recording.

- **AF-2: Field Activity (Offline).** The EO conducts a field visit in an area without connectivity. Activities are recorded on a mobile device and synchronised when connectivity is restored. The system records both the activity timestamp and the sync timestamp.

**Exception Flows:**

- **EF-1: Mandatory Field Missing.** At step 2, if the officer attempts to save without completing mandatory fields (e.g., outcome code), the system prevents save and highlights the missing fields.

- **EF-2: Case Status Conflict.** At step 1, if the case has been resolved or closed since the officer last viewed it (e.g., payment received), the system alerts the officer that the case status has changed and the activity may no longer be applicable.

**Postconditions:**

- All enforcement activities are recorded with timestamps and full detail.

- Supporting documents are attached to the case.

- The case timeline is updated.

- The complete activity history is preserved for audit and management reporting.

- Relevant parties are notified of activity outcomes and follow-up requirements.

## 7.4 Reporting Use Cases

> **Note:** UC-RPT-04 (Generate Revenue Reconciliation Report) is **excluded** from this specification per extraction rules #5 and #27. Revenue reconciliation is an STA-domain function managed through the ORS/ClickHouse reporting layer and is not within the scope of the Debt Management Case Management Platform.

### UC-RPT-01: View Debt Aging Dashboard

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | DMO (primary), SDO, MGR |  |
| **Priority** | Must |  |
| **Frequency** | ~100–300/day (most-used dashboard) |  |
| **Related Requirements** | RPT-FR-001, RPT-FR-007, RPT-FR-018, RPT-FR-019 |  |
| **Related Business Rules** | BR-RPT-001 (age band calculation), BR-RPT-002 (dashboard refresh frequency) |  |

**Preconditions:**

- The user is authenticated with dashboard view permissions appropriate to their role.

- ORS/ClickHouse data is synchronised and available.

- Debt aging materialised views are refreshed within the configured TTL.

**Main Success Scenario:**

- The user navigates to the Debt Aging Dashboard.

- The system renders the dashboard within 10 seconds, displaying: a bar chart showing outstanding debt distributed across age bands (0–30, 31–60, 61–90, 91–180, 181–365, >365 days), a summary table with totals per age band decomposed by PA/IA/PCA, and aggregate totals reconciled with consolidated taxpayer balances from ORS/ClickHouse.

- The user applies interactive filters: tax type (single or multiple), debt category (C1–C5), taxpayer type (NP/LP), registration office, and economic sector.

- The dashboard refreshes with filtered data.

- The user drills down from an age band to the individual debtor list, displaying: TIN, name, total debt, age band, category, enforcement stage, and risk score.

- The user can further drill down from a debtor to their full taxpayer account view in ORS.

- The system logs the dashboard access event.

**Alternative Flows:**

- **AF-1: Role-Based View.** Per RPT-FR-007, the dashboard adapts to the user's role: DMO sees their assigned portfolio only, SDO sees their team's portfolio, MGR sees the enterprise-wide view. Navigation between views is supported for users with multiple roles.

- **AF-2: Export.** The user exports the dashboard data to Excel/CSV or the visual to PDF per RPT-FR-018.

**Exception Flows:**

- **EF-1: Data Stale.** If the data cache exceeds the configured TTL, the system displays a "Data as of [timestamp]" indicator and offers a manual refresh option.

**Postconditions:**

- The user has viewed the debt aging distribution at the appropriate role level.

- Audit trail recorded.

### UC-RPT-02: Generate Debt Collection Status Report

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | DMO (primary), SDO |  |
| **Priority** | Must |  |
| **Frequency** | ~50–100/day |  |
| **Related Requirements** | RPT-FR-009, RPT-FR-015, RPT-FR-018, RPT-FR-019 |  |
| **Related Business Rules** | BR-RPT-003 (SLA status calculation), BR-RPT-004 (overdue action highlighting) |  |

**Preconditions:**

- Active debt cases exist in the system.

- The user is authenticated with report generation permissions.

- Report configuration parameters are available.

**Main Success Scenario:**

- The user navigates to the Report module and selects "Debt Collection Status Report."

- The system presents the interactive report configuration per RPT-FR-015: filters for debt category, enforcement stage, assigned officer, office, tax type, and SLA status.

- The user configures parameters and submits.

- The system generates the report showing for each active debt case: TIN, taxpayer name, debt category, current enforcement stage, assigned officer, days since last action, next scheduled action, risk score, and SLA status.

- Cases with overdue next-action dates are highlighted per BR-RPT-004.

- The user sorts by any column and drills down from a case row to the full case detail view.

- The user optionally saves the report configuration for reuse per RPT-FR-016 and/or exports per RPT-FR-018.

**Alternative Flows:**

- **AF-1: Scheduled Generation.** Per RPT-FR-017, the SDO configures the report for automatic daily/weekly generation and email distribution to a team recipient list.

**Exception Flows:**

- **EF-1: Large Result Set.** If the report exceeds 1,000 cases, results are paginated with total count displayed.

**Postconditions:**

- The collection status report has been generated and displayed or distributed.

- Report configuration optionally saved for reuse.

### UC-RPT-03: View KPI Monitoring Dashboard

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | MGR (primary), SDO |  |
| **Priority** | Must |  |
| **Frequency** | ~20–50/day |  |
| **Related Requirements** | RPT-FR-003, RPT-FR-007, RPT-FR-018 |  |
| **Related Business Rules** | BR-RPT-005 (KPI calculation methods), BR-RPT-006 (traffic light thresholds) |  |

**Preconditions:**

- KPI target values have been configured by the administrator.

- ORS/ClickHouse data supports calculation of the 12 arrears management KPIs (items 24–35).

- Dashboard refresh schedule is configured.

**Main Success Scenario:**

- The MGR navigates to the KPI Monitoring Dashboard.

- The system displays the 12 arrears management KPIs, each showing: current value (calculated from ORS data per BR-RPT-005), target value, variance (absolute and percentage), trend indicator (improving/declining/stable based on 3-month direction), sparkline for 12-month trend, and traffic light: green (≥target), amber (within 10%), red (>10% below target) per BR-RPT-006.

- The MGR selects a specific KPI for drill-down.

- The system displays KPI detail: calculation methodology, underlying data sources, period-by-period breakdown (monthly for 12 months), and contributing factors.

- The dashboard refreshes automatically at the configured interval (default: every 4 hours).

**Alternative Flows:**

- **AF-1: Target Configuration.** The SA navigates to KPI administration and configures or updates target values. Changes take effect at the next dashboard refresh. Target change history is maintained.

- **AF-2: Period Comparison.** The MGR selects "Compare Periods" to view KPIs side-by-side for two configurable periods (e.g., this quarter vs. last quarter, this year vs. last year).

**Exception Flows:**

- **EF-1: KPI Data Unavailable.** If the underlying data for a KPI is not yet populated (many KPIs are "Not Yet" available per the PDF), the system displays "Data not available" with the data source status.

**Postconditions:**

- KPI dashboard displayed with current values, targets, and trends.

- Audit trail recorded.

### UC-RPT-05: View Debtors List with Filtering

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | DMO (primary), SDO, MGR |  |
| **Priority** | Must |  |
| **Frequency** | ~200–400/day |  |
| **Related Requirements** | RPT-FR-002, RPT-FR-018, RPT-FR-019 |  |
| **Related Business Rules** | BR-RPT-009 (debtor list data freshness), BR-RPT-010 (filter combination rules) |  |

**Preconditions:**

- Debtors list data is refreshed (batch sync from ORS per RPT-FR-002).

- The user has appropriate role-based access.

**Main Success Scenario:**

- The user navigates to the Debtors List dashboard.

- The system displays all taxpayers with outstanding balances showing: TIN, name, total debt, oldest debt age, category (C1–C5), enforcement stage, risk score, and assigned officer.

- The user applies combinable filters: debt amount range, debt age, tax type, taxpayer type, economic sector (NACE), registration office, enforcement stage, risk score range, and assigned officer.

- The system returns filtered results with pagination (>1,000 records supported).

- The user sorts by any column (default: total debt descending).

- The user drills down from a debtor row to the full taxpayer account view in ORS or case detail in the Case Management Platform.

- The user exports the filtered list to Excel/CSV per RPT-FR-018.

**Alternative Flows:**

- **AF-1: Published Debtors List.** Per DM-FR-030, an SDO generates a list of debtors eligible for public name publication, filtered by: debt category ≥C3, enforcement stage past final demand, and no active objection or instalment agreement.

**Exception Flows:**

- **EF-1: Stale Data.** The system displays the data refresh timestamp. If data is older than the configured freshness threshold, a warning is shown.

**Postconditions:**

- The debtors list is displayed per the user's filter and sort criteria.

- Exports generated as requested.

## 7.5 Workflow and Case Management Use Cases

### UC-WF-01: Configure Debt Management Workflow

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System Administrator (primary) |  |
| **Priority** | Must |  |
| **Frequency** | Initial setup + periodic reviews (~2–4/year) |  |
| **Related Requirements** | WF-FR-001, WF-FR-002, WF-FR-003, WF-FR-010, INT-FR-010, NFR-025, NFR-028 |  |
| **Related Business Rules** | BR-WF-001 (FSM state transition validation), BR-WF-002 (trigger event configuration), BR-WF-003 (BPMN 2.0 export requirement) |  |

**Preconditions:**

- The SA is authenticated with workflow configuration permissions.

- The Case Management Platform's visual workflow designer is operational.

- Case type definitions exist (debt management, debt recovery, instalment, write-off, default assessment, enforcement).

**Main Success Scenario:**

- The SA opens the workflow designer for a selected case type.

- The SA configures or modifies the case lifecycle states per WF-FR-001: standard states (New → Open → In Progress → On Hold → Pending Closure → Closed) plus custom states as needed per case type.

- For each state transition, the SA defines: allowed transitions (which state can follow which), transition conditions (business rules that must be true), required fields (data that must be captured at transition), user role authorisation (who can trigger the transition), and automated actions (notifications, case assignment, document generation).

- The SA configures automatic case creation triggers per WF-FR-003: debt amount thresholds, debt age thresholds, payment events (missed, reversed), instalment events (default, cancellation), and SLA breach events.

- The SA configures SLA parameters per WF-FR-010: target resolution time per case type and debt category, escalation thresholds (warning at 75%, critical at 90%), and escalation actions (notification, reassignment, management alert).

- The system validates the workflow configuration: all states are reachable, no orphaned states exist, terminal state (Closed) is accessible from all active states, and transition rules are consistent.

- The SA saves the configuration. The system maintains version control with the ability to roll back.

- The SA exports the workflow definition in BPMN 2.0 format per INT-FR-010 / NFR-028 and verifies visual equivalence in a reference BPMN tool.

**Alternative Flows:**

- **AF-1: Clone and Modify.** The SA clones an existing workflow as the starting point for a new case type, then modifies states, transitions, and rules.

- **AF-2: Sandbox Testing.** The SA deploys the workflow to the UAT environment per NFR-027 for testing before production activation.

**Exception Flows:**

- **EF-1: Validation Failure.** At step 6, if the workflow contains unreachable states or inconsistent transitions, the system highlights the issues and prevents save until corrected.

**Postconditions:**

- The workflow configuration is saved, versioned, and active (or staged for UAT).

- BPMN 2.0 export is validated for portability.

- Automatic triggers and SLA parameters are operational.

### UC-WF-02: Manage Work Queue

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | DMO (primary), SDO (supervision) |  |
| **Priority** | Must |  |
| **Frequency** | Continuous (primary daily interface) |  |
| **Related Requirements** | WF-FR-006, WF-FR-007, WF-FR-009, WF-FR-011 |  |
| **Related Business Rules** | BR-WF-004 (queue sort priorities), BR-WF-005 (workload threshold) |  |

**Preconditions:**

- The user is authenticated with appropriate worklist access.

- Cases are assigned to queues per automatic or manual assignment rules.

- Risk scoring is current.

**Main Success Scenario:**

- The DMO opens their personal work queue (default landing page per WF-FR-009).

- The system displays assigned cases sorted by priority per BR-WF-004 (risk score descending, then queue entry date ascending): case ID, TIN, taxpayer name, debt amount, risk score, priority (High/Medium/Low), days in queue, SLA status (traffic light per WF-FR-011), last action date, and next action due.

- Overdue actions are highlighted in red; actions due within 24 hours in amber.

- The DMO applies filters: case type, debt category, SLA status, and custom criteria.

- The DMO selects a case and performs the required action via quick-action buttons (log call, schedule visit, generate letter) or full case navigation.

- After completing the action, the system updates the case status and worklist position.

- The DMO returns to the queue for the next case.

**Alternative Flows:**

- **AF-1: Team Queue View (SDO).** The SDO views the aggregate team queue showing all cases across officers with per-officer statistics: open cases, SLA compliance rate, and average resolution time.

- **AF-2: Unassigned Cases.** The SDO views cases awaiting assignment (no officer assigned) and manually assigns them per WF-FR-008.

**Exception Flows:**

- **EF-1: Officer at Capacity.** If the DMO's open case count exceeds the configured threshold per BR-WF-005, the system alerts the SDO and prevents further automatic assignment until capacity is freed.

**Postconditions:**

- Work queue displayed and actionable.

- Case actions recorded, statuses updated.

### UC-WF-03: Reassign or Delegate Case

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | SDO (primary), DMO (delegation request) |  |
| **Priority** | Must |  |
| **Frequency** | ~20–50/day |  |
| **Related Requirements** | WF-FR-008, WF-FR-004, WF-FR-009 |  |
| **Related Business Rules** | BR-WF-006 (reassignment authorisation), BR-WF-007 (bulk reassignment rules) |  |

**Preconditions:**

- Cases exist assigned to the originating officer.

- The SDO is authenticated with case reassignment permissions.

**Main Success Scenario:**

- The SDO navigates to the case reassignment interface (from the team queue or case detail).

- For single reassignment: the SDO selects the case, chooses the target officer from the available officers list (showing current workload), and enters a mandatory reassignment reason.

- The system validates: target officer has appropriate role and specialisation, target officer workload is below capacity threshold.

- The system transfers the case assignment: updates the assigned officer, records the reassignment in case history (from, to, reason, timestamp, authorising SDO), and notifies both the original and new officer.

**Alternative Flows:**

- **AF-1: Bulk Reassignment.** The SDO selects multiple cases (filtered by officer, case type, debt category, or custom criteria per WF-FR-008) and reassigns them to one or more target officers. The system distributes cases considering workload balancing per BR-WF-007.

- **AF-2: Leave Coverage.** The SDO selects all active cases for an officer going on leave and redistributes them across available team members.

- **AF-3: DMO Delegation Request.** A DMO requests delegation of a case to a colleague (e.g., language skill needed, specialist knowledge). The request is routed to the SDO for approval.

**Exception Flows:**

- **EF-1: Target Officer at Capacity.** The system warns that the target officer is at or above capacity and requires SDO confirmation to proceed.

- **EF-2: Case in Active Enforcement.** If the case has an in-progress enforcement action (e.g., pending bank garnishing response), the system warns that reassignment may disrupt the action and requires confirmation.

**Postconditions:**

- Case(s) reassigned with full audit trail.

- Both officers notified.

- Worklist positions updated for all affected officers.

## 7.6 Administration Use Cases

### UC-ADM-01: Configure Business Rules

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System Administrator (primary), Senior Tax Officer (approval for critical rule changes) |  |
| **Priority** | Must |  |
| **Frequency** | ~10–20/year (rule changes are infrequent but critical) |  |
| **Related Requirements** | NFR-025, NFR-029, INT-FR-011 |  |
| **Related Business Rules** | BR-ADM-001 (rule change audit requirements), BR-ADM-002 (rule activation policy) |  |

**Preconditions:**

- The SA is authenticated with business rule configuration permissions.

- A policy or legislative change has been documented requiring rule modification.

- The rule management interface is operational.

**Main Success Scenario:**

- The SA navigates to the Business Rules Configuration module.

- The system displays the current rule catalogue organised by domain: DM rules (debt thresholds, escalation, enforcement eligibility), reporting rules (KPI calculations, thresholds), and workflow rules (SLA, assignment, triggers). STA-domain rules (payment allocation, interest, reconciliation) are managed separately through ORS/ClickHouse and are visible as read-only external references.

- The SA selects a rule to modify or creates a new rule.

- For each rule, the SA configures: rule name and description, rule logic (decision table, conditional expression, or formula), effective date and end date, applicable scope (tax type, taxpayer type, debt category), and configurable parameter values.

- The system validates the rule: no conflicts with existing rules in the same scope and effective period, all referenced parameters exist, and decision table completeness (no gaps or overlaps).

- The SA previews the rule impact: estimated number of affected cases/accounts and before/after comparison for sample cases.

- If the rule change is classified as critical (e.g., debt category thresholds), the system routes for approval.

- Upon confirmation or approval, the system activates the rule per BR-ADM-002 (immediately or at next business day start, configurable).

- The system records the rule change with full audit trail per BR-ADM-001: previous logic/values, new logic/values, effective date, configuring user, approver, and justification.

- The rule is exportable in structured format (DMN, JSON, or spreadsheet) per NFR-029.

**Alternative Flows:**

- **AF-1: Rule Deactivation.** The SA deactivates a rule that is no longer applicable. Active cases that were processed under the old rule retain their outcomes; only new processing uses the updated rules.

- **AF-2: Rule Version Comparison.** The SA compares two versions of a rule side-by-side to review changes over time.

**Exception Flows:**

- **EF-1: Conflicting Rules.** At step 5, if the new rule conflicts with an existing active rule, the system displays the conflict and requires resolution before save.

**Postconditions:**

- Business rule configured, validated, and activated.

- Full audit trail recorded.

- Rule exportable in standard format.

### UC-ADM-02: Configure Notification Templates

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System Administrator (primary) |  |
| **Priority** | Must |  |
| **Frequency** | Initial setup + periodic updates (~5–10/year) |  |
| **Related Requirements** | WF-FR-014, WF-FR-013, WF-FR-017, NFR-017, NFR-026 |  |
| **Related Business Rules** | BR-ADM-003 (template versioning rules), BR-ADM-004 (merge field validation) |  |

**Preconditions:**

- The SA is authenticated with template management permissions.

- The template designer is operational.

- Available merge fields are documented and accessible.

**Main Success Scenario:**

- The SA navigates to the Notification Template Management module.

- The system displays existing templates organised by type: payment reminders (pre-due-date), demand notices (1st, 2nd, final), instalment payment reminders, instalment default notices, enforcement escalation notices, tax account statements, and internal staff alerts.

- The SA selects a template to edit or creates a new one.

- Using the visual template designer per NFR-026, the SA configures: static text content, merge fields (TIN, name, amounts, dates, tax type, period — validated against available data per BR-ADM-004), MTCA branding (logo, letterhead, footer), conditional sections (e.g., instalment information only shown if an instalment exists), and language variant (English and Maltese per NFR-017).

- The SA previews the template with sample data to verify rendering.

- The system validates: all merge fields reference valid data sources, both language variants are complete, and required legal text sections are present.

- The SA saves the template with version control per BR-ADM-003. The previous version is preserved. An effective date is set for the new version.

- The system records the template change with audit trail.

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

### UC-ADM-03: Manage User Roles and Permissions

| **Field** | **Value** |  |
| --- | --- | --- |
| **Actor(s)** | System Administrator (primary) |  |
| **Priority** | Must |  |
| **Frequency** | ~5–15/month (user onboarding, role changes, offboarding) |  |
| **Related Requirements** | NFR-011, NFR-014, NFR-015, INT-FR-016 |  |
| **Related Business Rules** | BR-ADM-005 (role hierarchy rules), BR-ADM-006 (permission inheritance), BR-ADM-007 (separation of duties) |  |

**Preconditions:**

- The SA is authenticated with user administration permissions.

- MITA SSO directory service is accessible per INT-FR-016.

- Role definitions exist (Case Officer, Senior Officer, Supervisor, Manager, Administrator, Auditor, System Administrator per NFR-011).

**Main Success Scenario:**

- The SA navigates to the User and Role Management module.

- For role management: the SA views existing roles with their permissions matrix (view, create, edit, approve, delete per entity type). The SA can create new roles, modify permissions on existing roles (subject to separation of duties per BR-ADM-007), and configure role hierarchy per BR-ADM-005 (e.g., Supervisor inherits Case Officer permissions per BR-ADM-006).

- For user management: the SA searches for a user from the MITA SSO directory, assigns one or more roles, configures additional user-specific settings (assigned office, specialisation, workload capacity), and optionally configures data masking level per NFR-015.

- The system validates: no conflicting role assignments violating separation of duties, user exists in the SSO directory, and all mandatory user fields are populated.

- Permission changes take effect immediately per NFR-011.

- The system records the role/user change with audit trail: previous roles, new roles, change date, and administering SA.

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

## 7.7 Use Case Summary and Coverage Matrix

### 7.7.1 Debt Management Use Cases (UC-DM-01 to UC-DM-20) — Complete

| **ID** | **Name** | **Primary Actor(s)** | **Priority** | **Requirements Covered** |  |
| --- | --- | --- | --- | --- | --- |
| UC-DM-01 | Identify and Create Debt Case (Automated) | System, DMO | Must | DM-FR-001–003, 005, 017, 018 |  |
| UC-DM-02 | Generate Payment Reminder | System | Must | DM-FR-009–011, 014, 051 |  |
| UC-DM-03 | Issue Demand Notice | System, DMO | Must | DM-FR-012, 014, 038, 040, 041 |  |
| UC-DM-04 | Issue Final/Immediate Demand Notice | System, SDO | Must | DM-FR-013, 014, 018, 038 |  |
| UC-DM-05 | Classify and Rank Debt Cases by Risk | System, DMO, SDO | Must | DM-FR-004, 006, 007, 008, 017 |  |
| UC-DM-06 | Create Instalment Agreement | Taxpayer, DMO | Must | DM-FR-021, 022, 025, 028 |  |
| UC-DM-07 | Review and Approve Instalment Agreement | DMO, SDO, Taxpayer | Must | DM-FR-023, 024, 025, 028 |  |
| UC-DM-08 | Monitor Instalment Agreement Compliance | System, DMO | Must | DM-FR-025, 026, 028, 051 |  |
| UC-DM-09 | Cancel Instalment Agreement and Create Recovery Case | System, DMO, Taxpayer | Must | DM-FR-022, 025, 027, 028 |  |
| UC-DM-10 | Escalate Debt Case for Enforcement | System, DMO, SDO | Must | DM-FR-015–020, 031–036, 039, 050 |  |
| UC-DM-11 | Process Bank Account Freezing Request | DMO, System, EXT | Must | DM-FR-029, 038, 040, 050 |  |
| UC-DM-12 | Process Write-Off | System, DMO, SDO, SA | Must | DM-FR-042–046 |  |
| UC-DM-13 | Manage Debt Recovery Worklist | DMO, SDO, System | Must | DM-FR-016, 006, 048, 050, 051, 057 |  |
| UC-DM-14 | Generate Debt Collection Plan | SDO, System | Should | DM-FR-047–049 |  |
| UC-DM-15 | Appoint Agent (Warrant) | SDO, DMO, System | Should | DM-FR-037–039, 050 |  |
| UC-DM-16 | Manage Objection to Debt | TP, DMO, SDO, System | Must | DM-FR-008, 028, 015, 050, 052 |  |
| UC-DM-17 | Process Default Assessment (Non-Filer) | System, DMO, TP | Should | DM-FR-054–056 |  |
| UC-DM-18 | Close Debt Recovery Case | DMO, System, SDO | Must | DM-FR-053, 050, 046, 052 |  |
| UC-DM-19 | Configure Enforcement Action Types | SA | Must | DM-FR-039, 040, 041, 015 |  |
| UC-DM-20 | Track and Record Enforcement Activities | DMO, EO, System | Must | DM-FR-019, 020, 050–052, 005 |  |

**DM Use Case MoSCoW Summary:** 17 Must / 3 Should / 0 Could

> **Note:** The coverage matrix in the source document shows minor priority discrepancies for UC-DM-11 (source matrix: Should; UC description: Must), UC-DM-14 (source matrix: Must; UC description: Should), UC-DM-15 (source matrix: Must; UC description: Should), UC-DM-16 (source matrix: Should; UC description: Must), UC-DM-17 (source matrix: Must; UC description: Should), and UC-DM-19 (source matrix: Should; UC description: Must). This extraction follows the priority stated in the individual use case specification as the authoritative value.

### 7.7.2 Reporting Use Cases

| **ID** | **Name** | **Primary Actor(s)** | **Priority** | **Requirements Covered** |  |
| --- | --- | --- | --- | --- | --- |
| UC-RPT-01 | View Debt Aging Dashboard | DMO, SDO, MGR | Must | RPT-FR-001, 007, 018, 019 |  |
| UC-RPT-02 | Generate Debt Collection Status Report | DMO, SDO | Must | RPT-FR-009, 015, 018, 019 |  |
| UC-RPT-03 | View KPI Monitoring Dashboard | MGR, SDO | Must | RPT-FR-003, 007, 018 |  |
| UC-RPT-05 | View Debtors List with Filtering | DMO, SDO, MGR | Must | RPT-FR-002, 018, 019 |  |

**RPT Use Case MoSCoW Summary:** 4 Must / 0 Should / 0 Could

> **Excluded:** UC-RPT-04 (Generate Revenue Reconciliation Report) — STA-domain function per rules #5, #27.

### 7.7.3 Workflow and Case Management Use Cases

| **ID** | **Name** | **Primary Actor(s)** | **Priority** | **Requirements Covered** |  |
| --- | --- | --- | --- | --- | --- |
| UC-WF-01 | Configure Debt Management Workflow | SA | Must | WF-FR-001–003, 010, INT-FR-010, NFR-025, 028 |  |
| UC-WF-02 | Manage Work Queue | DMO, SDO | Must | WF-FR-006, 007, 009, 011 |  |
| UC-WF-03 | Reassign or Delegate Case | SDO, DMO | Must | WF-FR-008, 004, 009 |  |

**WF Use Case MoSCoW Summary:** 3 Must / 0 Should / 0 Could

### 7.7.4 Administration Use Cases

| **ID** | **Name** | **Primary Actor(s)** | **Priority** | **Requirements Covered** |  |
| --- | --- | --- | --- | --- | --- |
| UC-ADM-01 | Configure Business Rules | SA | Must | NFR-025, 029, INT-FR-011 |  |
| UC-ADM-02 | Configure Notification Templates | SA | Must | WF-FR-014, 013, 017, NFR-017, 026 |  |
| UC-ADM-03 | Manage User Roles and Permissions | SA | Must | NFR-011, 014, 015, INT-FR-016 |  |

**ADM Use Case MoSCoW Summary:** 3 Must / 0 Should / 0 Could

### 7.7.5 Requirements Not Covered by Specific Use Cases

The following requirements are addressed through cross-cutting capabilities rather than dedicated use cases:

| **Requirement** | **Coverage** |  |
| --- | --- | --- |
| RPT-FR-005 (Collection Performance Dashboard) | Addressed within UC-RPT-02 and UC-WF-02 (supervisor view) |  |
| RPT-FR-006 (Multi-Stream Dashboard) | Addressed within UC-RPT-03 (KPI dashboard includes multi-stream overview) |  |
| RPT-FR-008 (Debt Aging Report) | Report variant of UC-RPT-01 dashboard with print/export |  |
| RPT-FR-011 (Instalment Compliance Report) | Generated from UC-DM-08 monitoring data; configuration via RPT-FR-015 pattern |  |
| RPT-FR-012 (Write-Off Report) | Generated from UC-DM-12 data; follows RPT-FR-015 configuration pattern |  |
| RPT-FR-013 (Staff Productivity Report) | Generated from workflow audit trail; follows RPT-FR-015 configuration pattern |  |
| RPT-FR-014 (Tax Debt Status Report) | Operational report variant of UC-RPT-05 debtors list |  |
| RPT-FR-016 (Save Configurations) | Cross-cutting capability used in UC-RPT-02 |  |
| RPT-FR-020 (SAS VIYA Risk Scores) | Data integration (INT-FR-005); displayed within UC-RPT-01/05 and case views |  |
| RPT-FR-021 (Ad-hoc Query) | Apache Superset self-service; follows pattern of UC-RPT-02 with custom fields |  |
| WF-FR-004 (Case History) | Cross-cutting within all DM use cases |  |
| WF-FR-005 (Case Re-opening) | Addressed in UC-DM-18 (EF-3: Reopening Closed Case) |  |
| WF-FR-012 (SLA Escalation) | Automated within UC-WF-01 configuration; operational in UC-WF-02 |  |
| WF-FR-015–016 (Internal Alerts, Notification Log) | Cross-cutting within all notification-generating use cases |  |
| WF-FR-017–019 (Document Generation/EDS/Postal) | Operational within UC-DM-03, UC-DM-04, UC-DM-11, UC-DM-19 |  |
| WF-FR-020 (Audit Trail) | Cross-cutting NFR; operational in all use cases |  |

### 7.7.6 DM Functional Requirements Coverage Confirmation

All 58 DM functional requirements (DM-FR-001 through DM-FR-058) are covered by at least one use case. Enforcement action types (DM-FR-031 through DM-FR-036) are addressed through the configurable enforcement framework in UC-DM-10 (alternative flow AF-4).

## Extraction Summary

| **Metric** | **Value** |  |
| --- | --- | --- |
| **DM use cases extracted (this file)** | 10 (UC-DM-11 to UC-DM-20) |  |
| **DM use cases total (Parts 1+2)** | 20 (UC-DM-01 to UC-DM-20) |  |
| **RPT use cases extracted** | 4 (UC-RPT-01, 02, 03, 05) |  |
| **RPT use cases excluded** | 1 (UC-RPT-04 — STA-only, rules #5, #27) |  |
| **WF use cases extracted** | 3 (UC-WF-01, 02, 03) |  |
| **ADM use cases extracted** | 3 (UC-ADM-01, 02, 03) |  |
| **Total use cases in this file** | 20 |  |
| **Total use cases across both parts** | 30 (20 DM + 4 RPT + 3 WF + 3 ADM) |  |
| **MoSCoW — all use cases** | 27 Must / 3 Should (DM-14, DM-15, DM-17) |  |
| **STA→ORS adaptations applied** | 14 instances |  |
| **Platform-agnostic adaptations** | 6 instances (BPM tool → Case Management Platform, vendor-specific platform → Case Management Platform, vendor → Case Management Platform, STA rules → ORS-managed, UC-STA-01 → taxpayer account view in ORS, UC-STA-08 → refund/set-off process in ORS) |  |
| **Requirement IDs preserved** | All DM-FR-xxx, BR-DM-xxx, UC-DM-xx, RPT-FR-xxx, BR-RPT-xxx, UC-RPT-xx, WF-FR-xxx, BR-WF-xxx, UC-WF-xx, NFR-xxx, INT-FR-xxx, BR-ADM-xxx, UC-ADM-xx references intact |  |
| **External dependencies documented** | ORS/ClickHouse, SAS VIYA, MITA SSO, self-service portal, bank web services, postal service, Administrative Review Tribunal, Court of Appeal, Apache Superset |  |
| **Priority discrepancies noted** | 6 (UC-DM-11, 14, 15, 16, 17, 19 — individual UC specs followed as authoritative) |  |

# 8. BUSINESS RULES CATALOGUE

This section provides the authoritative definitions for all business rules referenced throughout the requirements (Section 4) and use cases (Section 7). Each rule is defined with sufficient precision to guide implementation and testing. Rules are organised by domain and cross-referenced to their source documents, related requirements, and use cases.

**Rule Configurability Legend:**

- **Y** — Configurable by administrator through the Case Management Platform UI without code changes

- **N** — Fixed in application logic; change requires code deployment

- **P** — Partially configurable (parameters adjustable, core logic fixed)

> **Scope Note:** This catalogue covers Debt Management (BR-DM), Reporting (BR-RPT), Workflow (BR-WF), and Administration (BR-ADM) rules. STA accounting rules (BR-STA-001 to BR-STA-040) are managed externally within the ORS/ClickHouse platform and are referenced as external dependencies where applicable.

## 8.1 Debt Management Rules (BR-DM-001 to BR-DM-060)

### 8.1.1 Debt Detection and Case Creation Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-001 | Debt Detection Triggers | A debt case is created when any of the following triggers occur: liability past due date without payment, dishonoured payment reversal, cancelled instalment agreement, suspense account non-resolution after threshold period, or assessment non-payment. | trigger ∈ {past_due, dishonoured_payment, instalment_cancelled, suspense_timeout, assessment_unpaid}; each trigger independently configurable (enable/disable, timing) | PDF §5.2.1; Vendor §6.8 | Y (triggers configurable) | DM-FR-001, DM-FR-002; UC-DM-01 |  |
| BR-DM-002 | Case Creation Validation | Before creating a debt case, the system validates: no duplicate case exists for the same TIN/TXT/TXP combination, the debt amount is verified against data sourced from ORS/ClickHouse, and the debt exceeds the minimum case creation threshold. | IF exists(active_case, TIN, TXT, TXP) THEN skip OR consolidate; IF debt_amount < min_threshold THEN skip | Vendor §6.8 | Y (min threshold configurable) | DM-FR-003; UC-DM-01 |  |
| BR-DM-003 | Debt Category Thresholds (C1–C5) | Debts are classified into categories based on consolidated owed amount: C1 (<€30), C2 (€30–100), C3 (€100–1,000), C4 (€1,000–20,000), C5 (€20,000–200,000). Category determines available enforcement actions. | category = CASE WHEN amount < 30 THEN 'C1' WHEN amount < 100 THEN 'C2' WHEN amount < 1000 THEN 'C3' WHEN amount < 20000 THEN 'C4' ELSE 'C5' END | PDF §5.5 (Table 4) | Y (thresholds configurable) | DM-FR-005; UC-DM-01 |  |
| BR-DM-004 | Case Consolidation | When a new debt trigger occurs for a taxpayer with an existing active debt case, the system consolidates: the new trigger is added to the existing case, the debt amount is updated, and the category is re-evaluated. Separate cases are created only for distinct enforcement streams. | IF active_case_exists(TIN) AND same_enforcement_stream THEN consolidate ELSE create_new | Vendor §6.8 | P (consolidation criteria configurable) | DM-FR-004; UC-DM-01 |  |

### 8.1.2 Reminder and Demand Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-005 | Reminder Timing | First payment reminder is issued after a configurable number of days post-due-date (default: 7 days). Second reminder (C3–C5 only) is issued after the first reminder response period expires (default: 14 days after first reminder). | first_reminder_date = due_date + reminder_delay_days; second_reminder_date = first_reminder_date + response_period_days | PDF §5.2.1 (Figure 7) | Y (delay days and response period configurable) | DM-FR-009, DM-FR-010; UC-DM-02 |  |
| BR-DM-006 | Reminder Applicability by Category | Reminders apply to C2–C5 debts only. C1 debts route to automatic write-off. Second reminders apply to C3–C5 only (C2 receives first reminder and demand only). | Decision table: {C1: no_reminder, C2: 1st_only, C3-C5: 1st_and_2nd} | PDF §5.5 (Table 4) | Y (category-action mapping configurable) | DM-FR-010, DM-FR-011; UC-DM-02 |  |
| BR-DM-007 | Notification Channel Selection | Channel is selected based on: (1) taxpayer registered preference (if set), (2) category/priority override (high-priority cases use multiple channels), (3) default channel per notification type. | channel = taxpayer.preference ?? category_override ?? default_for_type; IF priority = 'high' THEN channels = [email, SMS, postal] | Vendor §6.6.1 | Y (channel mapping configurable) | DM-FR-014; UC-DM-02 |  |
| BR-DM-008 | Demand Notice Timing | Demand notice is issued after the reminder response period expires. For C2: after first reminder period. For C3–C5: after second reminder period. Objected amounts are excluded. | demand_eligible_date = last_reminder_date + response_period; demanded_amount = total_debt − disputed_amount | PDF §5.2.1; Vendor §6.8 | Y (timing configurable) | DM-FR-012; UC-DM-03 |  |
| BR-DM-009 | Demand Notice Content | Demand notice includes: full debt breakdown (PA/IA/PCA per tax type/period), legal basis, consequences of non-payment, appeal rights, payment deadline, and payment methods. For consolidated notices: per-tax-type breakdown within a single notice. | Mandatory content sections defined in template; merge fields validated against data model | PDF §5.2.1, §5.2.2; ITCAS RFP | P (template configurable; mandatory sections fixed) | DM-FR-041; UC-DM-03 |  |
| BR-DM-010 | Legal Fee Applicability | Legal fees are posted to the taxpayer's ledger as a separate charge when enforcement actions are initiated. Fee schedule is configurable per enforcement action type. | IF enforcement_action_initiated AND fee_applicable(action_type) THEN post_fee(amount_from_schedule, TIN, case_id) | DM-FR-038; Vendor §6.8 | Y (fee schedule configurable) | DM-FR-038; UC-DM-03, UC-DM-04 |  |
| BR-DM-011 | Final Demand Timing | Final demand is issued after the initial demand response period expires (configurable, default: 21 days). Includes explicit enforcement warning and proof of delivery requirement. | final_demand_date = demand_date + response_period; requires proof of delivery for enforcement validity | PDF §5.2.2 | Y (response period configurable) | DM-FR-013; UC-DM-04 |  |
| BR-DM-012 | Immediate Demand Triggers | Debts exceeding the high-value threshold bypass the standard reminder/demand sequence and receive immediate demand. SLA: telephone contact within 2 business days. | IF debt_amount ≥ high_value_threshold THEN issue_immediate_demand; set_SLA(phone_contact, 2_business_days) | PDF §5.6 (BR-DM-06) | Y (threshold configurable) | DM-FR-018; UC-DM-04 (AF-1) |  |
| BR-DM-013 | Enforcement Warning Content | Final demand notice includes: explicit list of enforcement actions that will follow (per debt category), specific deadline, and legal authority citations. | Template with conditional enforcement action list based on BR-DM-031 | PDF §5.2.2 | Y (template configurable) | DM-FR-013; UC-DM-04 |  |

### 8.1.3 Risk Scoring and Classification Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-014 | Risk Scoring Weights | Composite risk score (0–100) calculated from weighted criteria: debt size (w1), debt age (w2), number of revenue types (w3), compliance history (w4), taxpayer type (w5), economic sector (w6). | risk_score = Σ(criterion_score_i × weight_i) / Σ(weight_i) × 100; each criterion normalised to 0–1 before weighting | PDF §5.1; Vendor §6.8 | Y (weights and scoring bands configurable) | DM-FR-006; UC-DM-05 |  |
| BR-DM-015 | Risk Profile Components | Individual risk profile includes: current debt (amount and breakdown), historical debt patterns (recidivism count), compliance track record (on-time payment ratio), enforcement response history, and payment behaviour analysis (average days to pay). | Profile computed from ORS/ClickHouse historical data and case history; updated at each scoring batch | DM-FR-007; Vendor §6.8 | P (components fixed; display configurable) | DM-FR-007; UC-DM-05 |  |
| BR-DM-016 | Enforcement Exclusion Rules | Cases are excluded from standard enforcement when: (a) pending objection exists for disputed amount, (b) active compliant instalment agreement covers the debt, (c) case is under legal review, or (d) taxpayer deceased/dissolved (routed to special workflow). | IF objection_pending OR (instalment_active AND instalment_compliant) OR legal_review OR deceased_dissolved THEN exclude_from_enforcement | DM-FR-008, DM-FR-028; PDF §5.2.2 | P (exclusion types fixed; configurable which apply) | DM-FR-008; UC-DM-05, UC-DM-16 |  |

### 8.1.4 Instalment Agreement Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-017 | Instalment Eligibility | Instalment agreements are available for debts in categories C3–C5. The taxpayer must not have an active instalment agreement. Additional eligibility criteria are configurable (e.g., minimum debt age, taxpayer type). | IF category ∈ {C3, C4, C5} AND NOT has_active_instalment AND meets_additional_criteria THEN eligible | PDF §6.1; Vendor §6.8.2 | Y (eligibility criteria configurable) | DM-FR-021; UC-DM-06 |  |
| BR-DM-018 | Single Active Plan Rule | Only one instalment agreement may be active per taxpayer at any time. A new application is rejected if an active agreement exists. The previous agreement must be cancelled before a new one can be submitted. | IF exists(active_instalment, TIN) THEN reject_application | PDF §BR-INST-01; Vendor §6.8.2 | N | DM-FR-022; UC-DM-06 |  |
| BR-DM-019 | Instalment Interest Rate | Interest during an active, compliant instalment agreement is calculated at the reduced instalment rate (lower than the standard rate). Rate reverts to standard upon non-compliance. | Cross-references the STA interest calculation rule (BR-STA-009, managed externally in ORS/ClickHouse) | PDF §BR-INST-04; Vendor §6.6.1 | Y (rate configurable) | DM-FR-025; UC-DM-06, UC-DM-08 |  |
| BR-DM-020 | Minimum Instalment Amount | Each instalment payment must meet a configurable minimum amount. Plans with individual instalments below the minimum are rejected at validation. | IF any(instalment.amount < min_instalment_amount) THEN reject_schedule | Derived from operational viability | Y (minimum amount configurable) | DM-FR-021; UC-DM-06 |  |
| BR-DM-021 | Auto-Approval Parameters | Instalment applications meeting all of the following are auto-approved: debt below configurable threshold (default: €5,000), plan duration within configurable maximum (default: 12 months), and first application for the taxpayer. | IF debt < auto_threshold AND duration ≤ auto_max_months AND first_application THEN auto_approve ELSE route_to_officer | Vendor §6.8.2 | Y (all parameters configurable) | DM-FR-023; UC-DM-07 |  |
| BR-DM-022 | Approval Delegation Levels | Instalment agreement approval authority is delegated by amount: DMO up to €20,000, SDO up to €100,000, Director for higher amounts. | approver_role = CASE WHEN debt < 20000 THEN 'DMO' WHEN debt < 100000 THEN 'SDO' ELSE 'Director' END | Derived from organisational structure | Y (amount thresholds configurable) | DM-FR-023; UC-DM-07 |  |
| BR-DM-023 | Draft Decision Workflow | The reviewing officer generates a draft decision sent to the taxpayer for review. The taxpayer has a configurable response period (default: 14 days). Non-response results in application expiry. Maximum negotiation rounds configurable (default: 3). | draft → taxpayer_review (14 days) → accept/reject/counter; IF no_response THEN expire; IF rounds > max_rounds THEN close | Vendor §6.8.2 (DM-FR-024) | Y (response period and max rounds configurable) | DM-FR-024; UC-DM-07 |  |

### 8.1.5 Instalment Compliance and Cancellation Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-024 | Compliance Evaluation | An instalment is compliant if payment ≥ scheduled amount is received by the due date (or within the grace period per BR-DM-025). | IF payment_received ≥ scheduled_amount AND payment_date ≤ due_date + grace_days THEN compliant | DM-FR-026; Vendor §6.8.2 | P (grace period configurable; logic fixed) | DM-FR-026; UC-DM-08 |  |
| BR-DM-025 | Non-Compliance Grace Period | A configurable grace period (default: 3 business days) is allowed after each instalment due date. Payment within the grace period is marked "late but compliant." | grace_period = configurable_days (default: 3 business days) | Derived from operational fairness | Y (grace period configurable) | DM-FR-026; UC-DM-08 |  |
| BR-DM-026 | Interest Rate Reversion Trigger | Upon instalment non-compliance (missed payment after grace period), the interest rate reverts from the reduced instalment rate to the standard rate, effective from the missed payment date. | IF non_compliant THEN interest_rate = standard_rate; reversion_date = missed_payment_due_date | PDF §BR-INST-04 | N (automatic upon non-compliance) | DM-FR-025; UC-DM-08, UC-DM-09 |  |
| BR-DM-027 | Auto-Cancellation Triggers | An instalment agreement is automatically cancelled when: (a) the taxpayer misses a configurable number of consecutive payments (default: 2) after grace periods, OR (b) a DMO initiates manual cancellation with documented justification, OR (c) the taxpayer requests voluntary cancellation. | IF consecutive_missed ≥ auto_cancel_threshold THEN auto_cancel; manual and voluntary paths also supported | PDF §BR-INST-03; Vendor §6.8.2 | Y (consecutive missed threshold configurable) | DM-FR-027; UC-DM-09 |  |
| BR-DM-028 | Interest Rate Reversion on Cancellation | Upon cancellation, all remaining debt reverts to the standard interest rate from the cancellation date. Prior payments made at the reduced rate are not recalculated. | remaining_debt.interest_rate = standard_rate; effective_from = cancellation_date; prior_payments unchanged | PDF §BR-INST-04 | N | DM-FR-025; UC-DM-09 |  |
| BR-DM-029 | Recovery Case Creation on Cancellation | Upon instalment cancellation, the system automatically creates a new debt recovery case with: the calculated outstanding balance, full agreement history, and assignment to the enforcement worklist. | create_recovery_case(remaining_balance, agreement_history, original_case_ref) | DM-FR-027; Vendor §6.8.2 | N (automatic) | DM-FR-027; UC-DM-09 |  |

### 8.1.6 Enforcement Escalation Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-030 | Escalation Trigger Rules | A case is eligible for enforcement escalation when: final demand response period has expired, no active instalment agreement exists, and no pending objection covers the full amount. | IF final_demand_expired AND NOT instalment_active AND NOT (objection_pending AND disputed_amount ≥ total_debt) THEN eligible_for_enforcement | PDF §5.2.2; Vendor §6.8 | P (timing configurable; conditions fixed) | DM-FR-015; UC-DM-10 |  |
| BR-DM-031 | Enforcement Action Applicability by Category | Available enforcement actions are determined by debt category per the proportional enforcement matrix (Table 4). Higher categories unlock additional enforcement tools. | Decision table per PDF Table 4 — see Section 5.5 of pdf_extraction.md | PDF §5.5 (Table 4) | Y (matrix configurable) | DM-FR-016; UC-DM-10 |  |
| BR-DM-032 | Worklist Assignment Rules | Case assignment considers: debt category and risk score, officer specialisation (tax type expertise, enforcement type), geographic assignment, and workload balancing (current open case count per officer). | assigned_officer = match(specialisation, category, geography) WHERE workload < capacity ORDER BY workload ASC | PDF §5.7; Vendor §6.7 | Y (all criteria configurable) | DM-FR-016; UC-DM-10, UC-DM-13 |  |

### 8.1.7 Bank Garnishing Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-033 | Garnishing Eligibility | Bank account garnishing is available for debts in categories C3–C5 that have been escalated for enforcement. The garnishing amount shall not exceed the outstanding debt plus applicable legal fees. | IF category ∈ {C3, C4, C5} AND enforcement_stage ≥ 'escalated' THEN eligible; garnish_amount ≤ (debt + legal_fees) | PDF §5.5 (Table 4); Vendor §6.8 | P (category eligibility configurable) | DM-FR-029; UC-DM-11 |  |
| BR-DM-034 | Bank Integration Protocol | Garnishing requests are transmitted via secure web services (TLS 1.2+). Request format includes: TIN, taxpayer name, bank details, amount, legal reference, and MTCA authority reference. Response expected within configurable timeout. | REST/SOAP web service per bank API specification; timeout configurable (default: 30 seconds sync, 48 hours async) | Vendor §6.8; DM-FR-029 | P (protocol details per bank; timeout configurable) | DM-FR-029; UC-DM-11 |  |
| BR-DM-035 | Garnished Amount Posting | Upon receipt of garnished funds from a bank, the amount is posted to the taxpayer's account via integration with the STA module in ORS/ClickHouse, using the standard payment allocation rules (BR-STA-011, BR-STA-012 — managed externally). | post_payment(TIN, garnished_amount, source='bank_garnishing'); allocate per BR-STA-011/012 (external) | Vendor §6.8 | N | DM-FR-029; UC-DM-11 |  |

> **External Dependency:** BR-DM-035 relies on STA payment allocation rules (BR-STA-011, BR-STA-012) managed within the ORS/ClickHouse platform. The Case Management Platform sends a payment posting request via the integration interface; allocation logic is executed externally.

### 8.1.8 Write-Off Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-036 | C1 Automatic Write-Off | Debts classified as C1 (<€30) are automatically written off as "uneconomic to collect" without human intervention. A write-off transaction is posted to both taxpayer and revenue accounts. | IF category = 'C1' THEN auto_write_off; post_write_off(TIN, amount, reason='uneconomic') | PDF §5.6 (BR-DM-01) | P (C1 threshold configurable per BR-DM-003) | DM-FR-042; UC-DM-12 |  |
| BR-DM-037 | Write-Off Approval Delegation | Manual write-off approval is delegated by amount: DMO up to €1,000, SDO up to €20,000, Director for amounts above €20,000. C1 auto-write-off requires no approval. | approver = CASE WHEN amount < 1000 THEN 'DMO' WHEN amount < 20000 THEN 'SDO' ELSE 'Director' END | Derived from organisational authority | Y (thresholds configurable) | DM-FR-043; UC-DM-12 |  |
| BR-DM-038 | Statutory Collection Period | After the statutory collection period expires (configurable years), uncollected debts are eligible for write-off. C2 debts in passive collection status are bulk-written-off after this period. | IF debt_age_years ≥ statutory_period AND collection_exhausted THEN eligible_for_write_off | PDF §5.6 (BR-DM-10) | Y (statutory period configurable) | DM-FR-045; UC-DM-12 |  |
| BR-DM-039 | Write-Off Evidence Requirements | Manual write-off requests must include: enforcement history summary, evidence of inability to collect, decision rationale, and amount breakdown. Submissions without required evidence are rejected. | IF NOT has_enforcement_history OR NOT has_evidence THEN reject_submission | Vendor §6.8; DM-FR-043 | P (required evidence types configurable) | DM-FR-043; UC-DM-12 |  |

### 8.1.9 Worklist and SLA Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-040 | SLA Definitions | Each case type and debt category has a configurable SLA: target resolution time, warning threshold (default: 75% of SLA), critical threshold (default: 90%), and escalation action at each threshold. | sla_target = lookup(case_type, category); warning = target × 0.75; critical = target × 0.90; escalation action per threshold | WF-FR-010; PDF §5.1.2 | Y (all parameters configurable) | DM-FR-050; UC-DM-13 |  |
| BR-DM-041 | Workload Balancing Parameters | Maximum open cases per officer is configurable by role and specialisation. Cases are not auto-assigned to officers at or above capacity. Supervisor alerted when officer reaches 90% of capacity. | max_cases = lookup(officer.role, officer.specialisation); IF officer.open_cases ≥ max_cases THEN skip_assignment; alert_supervisor | PDF §5.7 | Y (capacity limits configurable) | DM-FR-016; UC-DM-13 |  |

### 8.1.10 Collection Planning Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-042 | Collection Plan Structure | A collection plan includes: target recovery amounts by category and tax type, resource allocation by specialisation, enforcement strategy by segment, and quarterly milestones. | Plan template with mandatory sections; validated against available resources | DM-FR-047 | P (structure fixed; targets configurable) | DM-FR-047; UC-DM-14 |  |
| BR-DM-043 | Target Setting Methodology | Recovery targets are set using: historical recovery rates by enforcement action type and category, current debt stock analysis, available officer capacity, and seasonal patterns. | target = historical_rate × current_stock × capacity_factor × seasonal_adjustment | DM-FR-049 | Y (methodology parameters configurable) | DM-FR-049; UC-DM-14 |  |

### 8.1.11 Agent (Warrant) Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-044 | Warrant Appointment Criteria | Agent appointment requires: debt in category C4–C5, standard enforcement actions exhausted or inappropriate, SDO approval, and agent from the approved register. | IF category ∈ {C4, C5} AND enforcement_exhausted AND SDO_approved THEN eligible | DM-FR-037; Vendor §6.8 | P (category requirement configurable) | DM-FR-037; UC-DM-15 |  |
| BR-DM-045 | Agent Commission/Fee Schedule | Agent fees are calculated per a configurable schedule: fixed appointment fee + percentage commission on recovered amounts. Commission rate may vary by debt category and recovery type. | agent_fee = appointment_fee + (recovered_amount × commission_rate); rates from configurable schedule | DM-FR-037 | Y (fee schedule configurable) | DM-FR-037; UC-DM-15 |  |
| BR-DM-046 | Agent Activity Tracking | Agents must report activities within configurable reporting intervals (default: weekly). Reporting includes: actions taken, amounts recovered, evidence collected, and next planned actions. Non-reporting triggers supervisor alert. | IF days_since_last_report > reporting_interval THEN alert_supervisor | DM-FR-037; Vendor §6.8 | Y (reporting interval configurable) | DM-FR-037; UC-DM-15 |  |

### 8.1.12 Objection Management Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-047 | Objection Eligibility | Objections must be submitted within the allowable period per tax type (configurable days from assessment date). Late objections are rejected unless an extension was granted. | IF submission_date ≤ assessment_date + objection_period THEN eligible ELSE reject (unless extension_granted) | DM-FR-008; ITCAS RFP | Y (period configurable per tax type) | DM-FR-008; UC-DM-16 |  |
| BR-DM-048 | Objection Effect on Enforcement | Disputed amounts are excluded from enforcement actions. Undisputed amounts remain subject to the standard enforcement workflow. If the entire amount is disputed, enforcement is suspended. The enforceable amount is **consumed from the STA accounting engine**, which nets out disputed amounts, suspended identified payments (STA-FR-046) and any asserted accounting holds — DM does not compute its own enforceable figure. | enforceable_amount = STA_enforceable_balance (= total − disputed − suspended_payments − held); IF enforceable_amount ≤ 0 THEN suspend_enforcement | PDF §BR-DM-09; DM-FR-008; STA spec STA-FR-011/046 | N | DM-FR-008; UC-DM-16 |  |
| BR-DM-049 | Undisputed Amount Treatment | The undisputed portion of a debt continues through the standard enforcement workflow without delay. Payment of the undisputed amount is required regardless of the objection. | undisputed_debt = total_debt − disputed_amount; enforce(undisputed_debt) | Derived from DM-FR-008 | N | DM-FR-008; UC-DM-16 |  |
| BR-DM-050 | Objection Decision Rules | Objection outcomes: (a) approved (full) — debt reversed entirely, (b) approved (partial) — approved portion reversed, remainder confirmed, (c) rejected — full amount confirmed as payable. All outcomes include appeal rights notification. | CASE outcome: 'approved_full' → reverse_debt; 'approved_partial' → reverse(approved_portion); 'rejected' → confirm_debt; END; notify_appeal_rights | DM-FR-008; Vendor §6.8 | P (outcome types fixed; appeal period configurable) | DM-FR-008; UC-DM-16 |  |

### 8.1.13 Default Assessment Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-051 | Default Assessment Estimation | Default assessments for non-filers are estimated using (in priority order): (1) prior year return data, (2) industry average for the taxpayer's sector and size, (3) configurable fixed formula. Estimation method is documented with the assessment. | estimated_amount = prior_year_amount ?? industry_average(sector, size) ?? fixed_formula(taxpayer_type) | PDF §5.3 (BR-DM-08) | Y (estimation parameters configurable) | DM-FR-054; UC-DM-17 |  |
| BR-DM-052 | Reasonableness Check | Default assessment amounts must pass a reasonableness check: no more than a configurable multiplier of the prior year amount (default: 150%). Amounts exceeding the threshold require DMO justification. | IF estimated > prior_year × reasonableness_multiplier THEN require_justification | PDF §5.3 (caution about exaggeration) | Y (multiplier configurable) | DM-FR-054; UC-DM-17 |  |
| BR-DM-053 | Return Replacement Rules | When a taxpayer files their actual return in response to a default assessment: (a) the default assessment is reversed, (b) the filed amount replaces it, (c) the variance is recorded, and (d) the taxpayer's risk profile is updated based on the variance. | reverse(default_assessment); post(filed_return_amount); record_variance; update_risk_profile(variance) | PDF §5.3 (DM-FR-055) | N | DM-FR-055; UC-DM-17 |  |

### 8.1.14 Case Closure and Archival Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-054 | Case Closure Eligibility | A case is eligible for closure when: all associated debt balances are resolved (zero or credit), all enforcement actions are concluded, and all required documents are attached. | IF balance ≤ 0 AND all_actions_concluded AND all_documents_attached THEN eligible_for_closure | DM-FR-053 | N (validation logic) | DM-FR-053; UC-DM-18 |  |
| BR-DM-055 | Closure Reason Codes | Standard closure reason codes: paid_in_full, written_off, instalment_completed, objection_approved, consolidated_into_other_case, administrative_closure. Custom codes may be added by SA. | closure.reason ∈ standard_codes ∪ custom_codes | DM-FR-053 | Y (reason codes configurable) | DM-FR-053; UC-DM-18 |  |
| BR-DM-056 | Case Archival Rules | Closed cases are archived and retained for a minimum of 10 years (configurable). Archived cases are read-only, searchable, and accessible for audit. | archive_after_closure; retention_period = 10 years (configurable); status = read_only | DM-FR-053; NFR-023 | Y (retention period configurable) | DM-FR-053; UC-DM-18 |  |

### 8.1.15 Enforcement Configuration and Activity Rules

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-DM-057 | Action Type Configuration | Each enforcement action type is defined with: name, code, applicable debt size range, applicable taxpayer type, document template, and officer instructions. | Configuration record per action type; validated for completeness | DM-FR-039; Vendor §6.8 | Y (fully configurable) | DM-FR-039; UC-DM-19 |  |
| BR-DM-058 | Template Management | Document templates support: merge fields (validated against data model), language variants (EN/MT), version control with effective dating, and batch generation. Historical notices retain the template version used at generation. | template.version = current; generation stores template_version_used | DM-FR-040; Vendor §6.8 | Y (templates fully configurable) | DM-FR-040; UC-DM-19 |  |
| BR-DM-059 | Activity Logging Requirements | Every enforcement activity is logged with: timestamp, actor (officer ID or 'system'), action type, case ID, TIN, outcome code, and before/after state. Logs are immutable. | log_entry = {timestamp, actor, action_type, case_id, TIN, outcome, state_change}; log is append-only | DM-FR-050 | N | DM-FR-050; UC-DM-20 |  |
| BR-DM-060 | Document Attachment Rules | Supporting documents must be categorised (type, description) and attached to the relevant case. Maximum file size: 25 MB. Supported formats: PDF, DOCX, XLSX, JPG, PNG. Documents are immutable once attached. | attachment = {file, type, description, date, officer}; max_size = 25MB; immutable_after_attach | DM-FR-052; WF-FR-018 | P (file size and formats configurable) | DM-FR-052; UC-DM-20 |  |

## 8.2 Reporting Rules (BR-RPT-001 to BR-RPT-010)

> **Data Source Note:** All reporting rules operate against data sourced from ORS/ClickHouse. Dashboard and report data is retrieved via the ORS integration interface; calculations are performed either within ORS (for pre-aggregated KPIs) or within the Case Management Platform's reporting layer (for case-specific operational views).

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-RPT-001 | Debt Age Band Calculation | Debt age is calculated as the number of days between the original due date and the current date. Age bands default to: 0–30, 31–60, 61–90, 91–180, 181–365, >365 days. | age_days = current_date − original_due_date; band assignment per configured thresholds | PDF §8.4.4 (Table 18) | Y (age band boundaries configurable) | RPT-FR-001, RPT-FR-008; UC-RPT-01 |  |
| BR-RPT-002 | Dashboard Refresh Frequency | Dashboards refresh at configurable intervals (default: 15 minutes for operational dashboards, 4 hours for KPI dashboards). Data staleness indicator displayed when cache exceeds TTL. | refresh_interval = configurable per dashboard; IF data_age > TTL THEN show_staleness_warning | ORS Implementation Plan | Y (intervals configurable) | RPT-FR-001; UC-RPT-01 |  |
| BR-RPT-003 | SLA Status Calculation | SLA status is calculated as: green (≤75% of SLA consumed), amber (75–100%), red (>100%). Based on elapsed time since case creation or last major action. | sla_consumed = elapsed_time / sla_target × 100; status = CASE WHEN ≤75 THEN green WHEN ≤100 THEN amber ELSE red END | WF-FR-010, WF-FR-011 | Y (thresholds configurable) | RPT-FR-009; UC-RPT-02 |  |
| BR-RPT-004 | Overdue Action Highlighting | Cases where the next scheduled action date has passed are highlighted in red in all worklists and reports. Actions due within 24 hours are highlighted in amber. | IF next_action_date < now THEN highlight_red; ELIF next_action_date < now + 24h THEN highlight_amber | WF-FR-009 | Y (time thresholds configurable) | RPT-FR-009; UC-RPT-02 |  |
| BR-RPT-005 | KPI Calculation Methods | Each KPI has a defined calculation formula using data from ORS/ClickHouse. Calculations are standardised to ensure consistency across dashboard views and reports. Formulas documented per KPI. | Per-KPI formula (e.g., KPI #24 = new_debt_year / total_collections × 100; KPI #26 = debt_under_instalment / total_debt × 100) | PDF §1.3 (Table 5, KPIs 24–35) | P (formulas fixed; time periods configurable) | RPT-FR-003; UC-RPT-03 |  |
| BR-RPT-006 | Traffic Light Thresholds | KPI traffic lights: green (current value ≥ target), amber (within 10% of target), red (>10% below target). Thresholds configurable per KPI. | IF value ≥ target THEN green; ELIF value ≥ target × 0.90 THEN amber; ELSE red | Derived from management reporting standards | Y (percentage thresholds configurable per KPI) | RPT-FR-003; UC-RPT-03 |  |
| BR-RPT-007 | Reconciliation Variance Threshold | Revenue reconciliation variances exceeding the configured threshold (default: €1.00) are flagged for investigation. | IF abs(mtca_total − treasury_total) > threshold THEN flag_for_investigation | PDF §BR-REV-01 | Y (threshold configurable) | RPT-FR-010; UC-RPT-04 |  |
| BR-RPT-008 | Treasury Comparison Rules | Revenue reconciliation compares MTCA-side totals (PA, IA, PCA separately) by tax type against Treasury records. Comparison performed at the configured frequency. | FOR EACH tax_type: compare(mtca_total(PA,IA,PCA), treasury_record); frequency configurable | PDF §BR-REV-01, §BR-REV-02 | P (comparison structure fixed; frequency configurable) | RPT-FR-010; UC-RPT-04 |  |
| BR-RPT-009 | Debtors List Data Freshness | The debtors list is refreshed at the configured batch synchronisation frequency (minimum: every 15 minutes for hot data). A timestamp indicates data freshness. | last_refresh = batch_sync_timestamp; IF now − last_refresh > configured_max THEN stale_warning | INT-FR-003 | Y (refresh frequency configurable) | RPT-FR-002; UC-RPT-05 |  |
| BR-RPT-010 | Filter Combination Rules | All report filters are combinable (AND logic). When no filter is applied, the report returns all records within the user's role-based access scope. Empty filter result sets display "No matching records." | WHERE filter_1 AND filter_2 AND ... AND role_scope; IF count = 0 THEN 'No matching records' | RPT-FR-015 | N (logic fixed) | RPT-FR-002, RPT-FR-015; UC-RPT-05 |  |

> **Note:** BR-RPT-007 and BR-RPT-008 relate to revenue reconciliation (UC-RPT-04), which is primarily STA-scope. These rules are included here for completeness as they define DM-relevant variance thresholds; the reconciliation use case itself (UC-RPT-04) is managed externally within the ORS/ClickHouse reporting layer.

## 8.3 Workflow Rules (BR-WF-001 to BR-WF-007)

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-WF-001 | FSM State Transition Validation | Each case state transition must follow the configured allowed transitions. Invalid transitions (e.g., New → Closed without going through intermediate states) are rejected. | IF target_state NOT IN allowed_transitions(current_state) THEN reject | WF-FR-001; Vendor §6.7 | Y (transitions configurable per case type) | WF-FR-001; UC-WF-01 |  |
| BR-WF-002 | Trigger Event Configuration | Automatic case creation triggers are independently configurable: enable/disable, timing (days after event), conditions (minimum amount, tax type scope), and target case type. | Per-trigger configuration: {event_type, enabled, delay_days, conditions, target_case_type} | WF-FR-003 | Y (fully configurable) | WF-FR-003; UC-WF-01 |  |
| BR-WF-003 | BPMN 2.0 Export Requirement | All workflow definitions must be exportable in valid BPMN 2.0 XML. Export includes: process flows, decision points, timer events, user tasks, and service tasks. | Export validates against OMG BPMN 2.0 XML schema; import test in reference tool required | INT-FR-010; NFR-028 | N (requirement) | INT-FR-010; UC-WF-01 |  |
| BR-WF-004 | Work Queue Sort Priorities | Default work queue sort: (1) risk score descending, (2) SLA urgency descending, (3) debt amount descending, (4) queue entry date ascending. Users can override sort order per session. | ORDER BY risk_score DESC, sla_urgency DESC, debt_amount DESC, entry_date ASC | WF-FR-006 | Y (default sort order configurable) | WF-FR-006; UC-WF-02 |  |
| BR-WF-005 | Workload Capacity Threshold | Maximum open cases per officer is configurable. Auto-assignment skips officers at or above capacity. Supervisor alerted at 90% capacity. | IF officer.open_cases ≥ max_capacity THEN skip_assignment; IF officer.open_cases ≥ max_capacity × 0.90 THEN alert_supervisor | WF-FR-007; PDF §5.7 | Y (capacity configurable per role) | WF-FR-007; UC-WF-02 |  |
| BR-WF-006 | Reassignment Authorisation | Single case reassignment requires SDO authorisation. Bulk reassignment requires SDO authorisation with mandatory justification. All reassignments are audited. | reassignment requires role ≥ SDO; bulk_reassignment requires SDO + justification; audit all | WF-FR-008 | P (authorisation level configurable) | WF-FR-008; UC-WF-03 |  |
| BR-WF-007 | Bulk Reassignment Rules | Bulk reassignment distributes cases considering: target officers' current workload, specialisation match, and equitable distribution. Officer-specific maximum loads are respected. | Distribution algorithm: round_robin(filtered_officers) WHERE workload < capacity AND specialisation_match | WF-FR-008 | Y (distribution parameters configurable) | WF-FR-008; UC-WF-03 |  |

## 8.4 Administration Rules (BR-ADM-001 to BR-ADM-007)

| **Rule ID** | **Name** | **Description** | **Rule Logic** | **Source** | **Config.** | **Related Reqs / UCs** |  |
| --- | --- | --- | --- | --- | --- | --- | --- |
| BR-ADM-001 | Rule Change Audit Requirements | Every business rule change is logged with: previous value, new value, effective date, configuring user, approver (if applicable), justification, and timestamp. Audit entries are immutable. | audit_log.append({rule_id, old_value, new_value, effective_date, user, approver, justification, timestamp}); immutable | NFR-023, NFR-025 | N | NFR-025; UC-ADM-01 |  |
| BR-ADM-002 | Rule Activation Policy | Rule changes take effect at the configured activation time: immediately (default for non-critical rules), or at next business day start (for rules affecting batch processing). Critical rules may require STO approval before activation. | IF rule.criticality = 'high' THEN require_STO_approval; activation = next_business_day ELSE activation = immediate | NFR-025 | Y (activation timing configurable per rule type) | NFR-025; UC-ADM-01 |  |
| BR-ADM-003 | Template Versioning Rules | Notification templates use version control with effective dating. The previous version is preserved when a new version is activated. Historical notices reference the template version used at generation. | template.versions = [v1(start, end), v2(start, null)]; generated_notice.template_version = version_at_generation_date | WF-FR-014 | N (versioning automatic) | WF-FR-014; UC-ADM-02 |  |
| BR-ADM-004 | Merge Field Validation | All merge fields in templates must reference valid data model elements. Templates with invalid merge field references cannot be saved. | FOR EACH merge_field IN template: IF NOT exists_in_data_model(merge_field) THEN reject_save | WF-FR-014 | N (validation automatic) | WF-FR-014; UC-ADM-02 |  |
| BR-ADM-005 | Role Hierarchy Rules | Roles follow a hierarchy: System Administrator > Manager > Supervisor > Senior Officer > Case Officer > Auditor (read-only). Higher roles inherit all permissions of lower roles unless explicitly restricted. | effective_permissions(role) = role.own_permissions ∪ inherited_permissions(role.parent) | NFR-011 | Y (hierarchy configurable) | NFR-011; UC-ADM-03 |  |
| BR-ADM-006 | Permission Inheritance | Permission inheritance follows the role hierarchy. Explicit deny overrides inherited allow. Permissions are evaluated at runtime for each access request. | IF explicit_deny(action, entity) THEN deny; ELIF role_permits(action, entity) THEN allow; ELSE deny | NFR-011 | N (inheritance logic) | NFR-011; UC-ADM-03 |  |
| BR-ADM-007 | Separation of Duties | Certain role combinations are prohibited: (a) Case Officer + Approver for the same case type, (b) System Administrator + Auditor. Violations detected at role assignment time. | IF conflicting_roles(assigned_roles) THEN reject_assignment; display_policy_violation | NFR-011; GDPR | P (conflict pairs configurable) | NFR-011; UC-ADM-03 |  |

## 8.5 Business Rules Summary

| **Domain** | **Rule Range** | **Count** | **Configurable (Y/P)** | **Fixed (N)** |  |
| --- | --- | --- | --- | --- | --- |
| DM — Detection/Case Creation | BR-DM-001 to BR-DM-004 | 4 | 3 | 1 |  |
| DM — Reminders/Demands | BR-DM-005 to BR-DM-013 | 9 | 8 | 1 |  |
| DM — Risk Scoring | BR-DM-014 to BR-DM-016 | 3 | 2 | 1 |  |
| DM — Instalments | BR-DM-017 to BR-DM-023 | 7 | 6 | 1 |  |
| DM — Compliance/Cancellation | BR-DM-024 to BR-DM-029 | 6 | 2 | 4 |  |
| DM — Enforcement Escalation | BR-DM-030 to BR-DM-032 | 3 | 2 | 1 |  |
| DM — Bank Garnishing | BR-DM-033 to BR-DM-035 | 3 | 1 | 2 |  |
| DM — Write-Off | BR-DM-036 to BR-DM-039 | 4 | 3 | 1 |  |
| DM — Worklist/SLA | BR-DM-040 to BR-DM-041 | 2 | 2 | 0 |  |
| DM — Collection Planning | BR-DM-042 to BR-DM-043 | 2 | 1 | 1 |  |
| DM — Agent/Warrant | BR-DM-044 to BR-DM-046 | 3 | 2 | 1 |  |
| DM — Objections | BR-DM-047 to BR-DM-050 | 4 | 1 | 3 |  |
| DM — Default Assessment | BR-DM-051 to BR-DM-053 | 3 | 2 | 1 |  |
| DM — Case Closure | BR-DM-054 to BR-DM-056 | 3 | 1 | 2 |  |
| DM — Enforcement Config/Activity | BR-DM-057 to BR-DM-060 | 4 | 2 | 2 |  |
| **DM Subtotal** |  | **60** | **38** | **22** |  |
| **Reporting** | BR-RPT-001 to BR-RPT-010 | **10** | **8** | **2** |  |
| **Workflow** | BR-WF-001 to BR-WF-007 | **7** | **5** | **2** |  |
| **Administration** | BR-ADM-001 to BR-ADM-007 | **7** | **3** | **4** |  |
| **GRAND TOTAL (DM Scope)** |  | **84** | **54 (64%)** | **30 (36%)** |  |

> **Note:** STA accounting rules (BR-STA-001 to BR-STA-040, 40 rules) are excluded from this specification. They are managed within the ORS/ClickHouse platform and referenced as external dependencies where applicable (see BR-DM-019, BR-DM-035). The full system total across both platforms is 124 rules.

## 8.6 External Dependencies — STA Business Rules

The following STA rules are referenced by DM business rules but are managed externally within the ORS/ClickHouse platform:

| **Referenced STA Rule** | **Referenced By** | **Dependency Type** |  |
| --- | --- | --- | --- |
| BR-STA-009 (Interest calculation — instalment rate) | BR-DM-019 | Rate configuration — the Case Management Platform reads the applicable rate from ORS |  |
| BR-STA-011 (Payment allocation — oldest first) | BR-DM-035 | Payment posting — garnished amounts are allocated by the STA module |  |
| BR-STA-012 (Payment allocation — PA/IA/PCA order) | BR-DM-035 | Payment posting — allocation order managed in ORS |  |
| BR-STA-021 (Reconciliation frequency) | BR-RPT-008 | Scheduling — reconciliation frequency parameter sourced from ORS configuration |  |

> **Integration Note:** The Case Management Platform communicates with ORS/ClickHouse via the defined integration interfaces (see Section 10 — Interface Requirements). STA rule changes within ORS do not require code changes in the Case Management Platform, provided the integration contract is maintained.

*End of Section 8 — Business Rules Catalogue*

# 9. DATA REQUIREMENTS

## 9.1 Conceptual Data Model

The Debt Management solution operates on a conceptual data model centred on the debt recovery lifecycle. The model is read-oriented for taxpayer and financial data — sourcing this from ORS/ClickHouse — with workflow state managed locally in the Case Management Platform's database. The entities below describe the logical model; physical implementation spans two persistence layers (ClickHouse for tax data, Case Management Platform RDBMS for workflow state).

### 9.1.1 Internal DM Entities

These entities are owned and managed by the Debt Management module within the Case Management Platform.

**DebtCase** — Created when a taxpayer has outstanding liabilities past due date. A DebtCase aggregates debts across tax types for a single taxpayer, categorised by debt value (C1: <€30, C2: €30–100, C3: €100–1,000, C4: €1,000–20,000, C5: €20,000–200,000). DebtCases have a lifecycle: new → open → in-progress → on-hold → pending-closure → closed. Each case tracks the assigned officer, priority (derived from risk score), debt age, and the sequence of enforcement actions taken.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| case_id | UUID | Unique case identifier |  |
| tin | VARCHAR(10) | Taxpayer Identification Number (foreign key to ORS taxpayer data) |  |
| debt_category | ENUM(C1–C5) | System-calculated based on consolidated balance per BR-DM-001 to BR-DM-005 |  |
| status | ENUM | new, open, in-progress, on-hold, pending-closure, closed |  |
| assigned_officer | VARCHAR | Case officer user ID |  |
| priority | INTEGER | Derived from SAS VIYA risk score (0–100) |  |
| total_amount | DECIMAL | Current outstanding amount (refreshed from ORS) |  |
| oldest_debt_age | INTEGER | Days since oldest unpaid liability |  |
| created_date | TIMESTAMP | Case creation date |  |
| last_action_date | TIMESTAMP | Most recent enforcement action date |  |

**InstalmentAgreement** — A negotiated payment plan covering all outstanding debts for a taxpayer. Only one active agreement may exist per taxpayer at any time. Attributes include: schedule of payments, total amount, interest rate (lower rate applied during valid agreement), start date, approval status, and compliance status. Non-compliance triggers automatic case creation for debt recovery.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| agreement_id | UUID | Unique agreement identifier |  |
| tin | VARCHAR(10) | Taxpayer Identification Number |  |
| case_id | UUID | Linked debt case |  |
| total_amount | DECIMAL | Total agreement amount including projected interest |  |
| num_instalments | INTEGER | Number of scheduled payments |  |
| frequency | ENUM | Monthly, quarterly, custom |  |
| start_date | DATE | First instalment due date |  |
| interest_rate | DECIMAL | Reduced rate applicable during valid agreement |  |
| approval_status | ENUM | pending, approved, rejected |  |
| compliance_status | ENUM | compliant, at-risk, non-compliant, cancelled |  |
| approved_by | VARCHAR | Approving officer/supervisor |  |
| approval_date | TIMESTAMP | Date of approval decision |  |

**InstalmentPayment** — Individual payment within an InstalmentAgreement. Tracks: due date, expected amount, actual payment date, actual amount, and variance. Missed or late payments are flagged for compliance monitoring against KPI #29 (% complying with instalment arrangements).

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| payment_id | UUID | Unique instalment payment identifier |  |
| agreement_id | UUID | Parent agreement |  |
| sequence_number | INTEGER | Instalment number within the agreement |  |
| due_date | DATE | Expected payment date |  |
| expected_amount | DECIMAL | Scheduled payment amount |  |
| actual_payment_date | DATE | Date payment received (from ORS matching) |  |
| actual_amount | DECIMAL | Amount received |  |
| variance | DECIMAL | Difference between expected and actual |  |
| status | ENUM | pending, paid, late, missed |  |

**RecoveryAction** — An enforcement action taken against a debtor. The 14 action types are: demand notice, refund interception, second reminder, telephone contact, taxpayer visit, payment arrangement, bank account garnishing, name publication, lien on assets, third-party claim, passport seizure, property seizure/auction, bankruptcy/liquidation demand, and write-off. Each action records: type, initiation date, executing officer, outcome, and associated costs (legal fees tracked separately).

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| action_id | UUID | Unique action identifier |  |
| case_id | UUID | Parent debt case |  |
| action_type | ENUM | One of 14 enforcement action types |  |
| initiation_date | TIMESTAMP | Date action initiated |  |
| executing_officer | VARCHAR | Officer performing the action |  |
| outcome | ENUM | pending, successful, unsuccessful, escalated |  |
| outcome_date | TIMESTAMP | Date of outcome recording |  |
| associated_costs | DECIMAL | Legal fees and other costs |  |
| agent_id | UUID | Assigned warrant officer (if applicable) |  |
| notes | TEXT | Free-text description of action details |  |

**DemandNotice** — System-generated notification sent to a taxpayer at various escalation stages. Types include: first reminder, second reminder, demand notice, judicial letter, and warrant. Each notice records: generation date, delivery method, delivery status, response deadline, and taxpayer response.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| notice_id | UUID | Unique notice identifier |  |
| case_id | UUID | Parent debt case |  |
| tin | VARCHAR(10) | Taxpayer Identification Number |  |
| notice_type | ENUM | first_reminder, second_reminder, demand, judicial_letter, warrant |  |
| generation_date | TIMESTAMP | Date generated |  |
| delivery_method | ENUM | email, post, sms, in_app |  |
| delivery_status | ENUM | queued, sent, delivered, returned, failed |  |
| delivery_confirmation_date | TIMESTAMP | Date delivery confirmed |  |
| response_deadline | DATE | Date by which taxpayer must respond |  |
| taxpayer_response | TEXT | Response received (if any) |  |

**WriteOff** — A specific RecoveryAction that removes a debt from active collection. WriteOffs may be automatic (C1 debts <€30, uneconomic to collect) or approved (requiring supporting evidence and authorisation workflow). Supports individual and bulk processing.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| writeoff_id | UUID | Unique write-off identifier |  |
| case_id | UUID | Parent debt case |  |
| tin | VARCHAR(10) | Taxpayer Identification Number |  |
| writeoff_type | ENUM | automatic, approved |  |
| amount | DECIMAL | Written-off amount |  |
| reason | TEXT | Justification/supporting evidence reference |  |
| approved_by | VARCHAR | Approving authority |  |
| approval_date | TIMESTAMP | Date of approval |  |
| evidence_documents | ARRAY(UUID) | References to uploaded supporting documents |  |

**Worklist** — Case officer work queue managing the prioritised assignment of debt cases to officers. Supports automatic allocation based on configurable rules (round-robin, load-balanced, specialisation-based) and manual reassignment by team leads.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| worklist_id | UUID | Unique worklist entry identifier |  |
| case_id | UUID | Assigned debt case |  |
| assigned_to | VARCHAR | Officer user ID |  |
| assigned_by | ENUM | system, team_lead |  |
| assignment_date | TIMESTAMP | Date of assignment |  |
| priority | INTEGER | Queue priority (derived from case priority) |  |
| sla_deadline | TIMESTAMP | Date by which next action must be taken |  |
| status | ENUM | active, completed, reassigned |  |

**Agent** — A warrant officer or authorised representative appointed for specific enforcement actions. Tracks: appointment date, scope of authority, assigned cases, and action outcomes.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| agent_id | UUID | Unique agent identifier |  |
| name | VARCHAR | Agent name |  |
| appointment_date | DATE | Date of appointment |  |
| scope | TEXT | Scope of authority |  |
| status | ENUM | active, inactive, suspended |  |

**Document** — Generated or received document associated with any DM entity. Types include: demand notices, instalment agreements, clearance certificates, judicial letters, and supporting evidence. Documents are generated through the ADG (Automated Document Generation) subsystem and stored in the EDS (Electronic Document System).

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| document_id | UUID | Unique document identifier |  |
| entity_type | ENUM | debt_case, instalment_agreement, recovery_action, write_off |  |
| entity_id | UUID | Parent entity reference |  |
| document_type | ENUM | demand_notice, agreement, certificate, judicial_letter, evidence, other |  |
| generation_date | TIMESTAMP | Date generated or uploaded |  |
| template_id | VARCHAR | Template used for generation (if applicable) |  |
| language | ENUM | mt, en | Maltese or English |
| storage_reference | VARCHAR | EDS document reference |  |
| format | ENUM | pdf, docx, image |  |

**AuditLog** — Immutable record of every system action for compliance and accountability. Captures: timestamp, user, action type, affected entity, before/after values, and IP address. Retention period aligned with statutory requirements.

| **Attribute** | **Type** | **Description** |  |
| --- | --- | --- | --- |
| log_id | UUID | Unique log entry identifier |  |
| timestamp | TIMESTAMP | Action timestamp |  |
| user_id | VARCHAR | Acting user |  |
| action_type | VARCHAR | Create, update, delete, view, approve, reject, etc. |  |
| entity_type | VARCHAR | Affected entity type |  |
| entity_id | UUID | Affected entity ID |  |
| before_value | JSON | State before action |  |
| after_value | JSON | State after action |  |
| ip_address | VARCHAR | Client IP address |  |

### 9.1.2 External Data Entities (Accessed via ORS/ClickHouse)

These entities are **not** owned by the DM module. They are sourced read-only from ORS/ClickHouse and represent the taxpayer and financial data that DM consumes.

**Taxpayer** *(external — ORS)* — The central entity representing any natural person (NP) or legal person (LP) registered with MTCA. Every taxpayer is uniquely identified by a TIN (Taxpayer Identification Number). Key attributes consumed by DM: TIN, taxpayer type (NP/LP), name, postal address, phone, email, bank details, NACE classification, and for LPs: business name, legal representatives.

> **DM Access Pattern:** Point lookup by TIN via ClickHouse REST API. Used for case creation (UC-DM-01), demand notice generation (UC-DM-02 to UC-DM-04), and instalment agreement forms (UC-DM-06). Data freshness: Hot tier (< 5 minutes).

**TaxAccount** *(external — ORS)* — The Single Taxpayer Account record implementing the three-level structure: L1 (consolidated balance across all tax types), L2 (per-tax-type balance), L3 (per-period transaction detail). Each level maintains separate debit and credit amounts, with balances decomposed into PA (Principal Amount), IA (Interest Amount), and PCA (Penalties and Charges Amount).

> **DM Access Pattern:** The DM module reads TaxAccount data to determine the total outstanding debt for a taxpayer at case creation, to populate debt breakdowns in the case detail view, and to validate instalment agreement amounts. Accessed via Silver Layer views (taxpayer_balances, assessment_register). Data freshness: Hot tier for active case views, Warm tier for report generation.

**Transaction** *(external — ORS)* — Financial events recorded against a TaxAccount. Transaction types include: liability (assessment), payment, refund, transfer, write-off, and reversal. DM consumes transaction data for debt age calculation, payment matching against instalment schedules, and demand notice amount verification.

> **DM Access Pattern:** Range scans across tax periods for a single taxpayer's transaction history. Also used for payment event polling (new payments for instalment matching, every 5 minutes via INT-CH-05). Data freshness: Hot tier for payment matching, Warm tier for historical analysis.

**Payment** *(external — ORS)* — Funds received from taxpayers. DM monitors payments for instalment compliance tracking (matching actual payments against expected schedule) and for debt case closure triggers (when outstanding balance reaches zero).

> **DM Access Pattern:** Event-driven polling via ClickHouse (INT-CH-05, every 5 minutes) to detect new payments affecting active debt cases or instalment agreements. Reconciliation via DQ-REC-04.

**Assessment** *(external — ORS)* — Liability-creating transactions including self-assessed (filed returns), authority-assessed (audit), and default-assessed (non-filers). DM uses assessment data for default assessment review (UC-DM-17) and for computing debt case amounts.

> **DM Access Pattern:** Read at case creation and on-demand for case detail view. Data freshness: Hot tier.

### 9.1.3 Key Relationships (DM-Centric View)

┌─────────────────────────────────────────────────────────┐

│  EXTERNAL (ORS/ClickHouse — Read Only)                  │

│                                                         │

│  Taxpayer ──(1:N)──▶ TaxAccount                        │

│     │                    │                              │

│     │                    └──(1:N)──▶ Transaction        │

│     │                                    │              │

│     │                    ┌───────────────┤              │

│     │                    ▼               ▼              │

│     │               Payment         Assessment          │

└─────│───────────────────────────────────────────────────┘

│

│ TIN (lookup key)

▼

┌─────────────────────────────────────────────────────────┐

│  INTERNAL (Case Management Platform — Read/Write)       │

│                                                         │

│  DebtCase ──(1:N)──▶ RecoveryAction ──(0:1)──▶ Agent   │

│     │                                                   │

│     ├──(1:N)──▶ DemandNotice                           │

│     │                                                   │

│     ├──(0:1)──▶ InstalmentAgreement ──(1:N)──▶         │

│     │              InstalmentPayment                    │

│     │                                                   │

│     ├──(0:N)──▶ WriteOff                               │

│     │                                                   │

│     └──(1:N)──▶ Worklist                               │

│                                                         │

│  All entities ──(N)──▶ Document                        │

│  All entities ──(N)──▶ AuditLog                        │

└─────────────────────────────────────────────────────────┘

**Key cardinality rules:**

- Taxpayer (via TIN) → DebtCase: One taxpayer may have multiple debt cases over time, but typically one active case at any time

- DebtCase → RecoveryAction: Escalating sequence of enforcement actions (ordered by date)

- DebtCase → InstalmentAgreement: At most one active agreement per taxpayer at any time

- InstalmentAgreement → InstalmentPayment: One entry per scheduled payment

- RecoveryAction → Agent: Optional agent assignment for field enforcement actions

## 9.2 ORS Data Access Patterns

The DM solution consumes data from the ORS/ClickHouse platform through a read-only integration pattern. The ORS implements a medallion architecture (Bronze → Silver → Gold) across 9 Informix databases containing 5,170+ tables.

### 9.2.1 ClickHouse Data Sources Consumed by DM

| **Source Database** | **CH Prefix** | **Key Tables for DM** | **Est. Tables Used** |  |
| --- | --- | --- | --- | --- |
| Income Tax Core (1,570 tables) | it_ | Taxpayer master, assessments, payments, balances | ~80 |  |
| VAT (928 tables) | vat_ | VAT returns, payments, period balances | ~50 |  |
| ARS Accounting (461 tables) | ars_ | Payment reconciliation, revenue accounts | ~40 |  |
| Taxation Shared (219 tables) | shared_ | Tax rates, code tables, office codes, period definitions | ~30 |  |
| CTD Property Tax (471 tables) | ctd_ | Property assessments, stamp duty | ~20 |  |
| Web Income Tax (788 tables) | wit_ | E-filed returns, validation results | ~15 |  |
| VAT-Web (419 tables) | vatweb_ | Online VAT submissions | ~10 |  |
| Old VAT Charging (270 tables) | ovt_ | Historical VAT data (archive reference) | ~5 |  |
| VIES (44 tables) | vies_ | EU validation data (cross-reference) | ~3 |  |

**Total estimated ClickHouse tables consumed: ~253** (5% of the full 5,170+ table inventory)

### 9.2.2 Query Patterns by Data Freshness Tier

**Hot Tier (****<**** 5 min freshness, ~310 tables globally)**

DM usage: real-time taxpayer balance lookups for active case views, payment status checks for instalment compliance monitoring, current-period balance queries for debt case creation triggers.

Query patterns: point lookups by TIN, filtered by current tax period. Typical query: retrieve all outstanding balances for TIN X across all tax types with < 2 second response time. Supports the Debt Case Dashboard (DM-SCR-01) and real-time case detail views (DM-SCR-02).

Key tables consumed:

- mtca_ors.it_taxpayer — taxpayer master lookup

- mtca_ors.it_assessments — current period liabilities

- mtca_ors.ars_journal_entries — recent payment postings

- mtca_ors.vat_transactions — VAT period balances

**Warm Tier (****<**** 1 hour freshness, ~775 tables globally)**

DM usage: prior-period balance calculations for instalment compliance monitoring, demand notice generation triggers (checking balance thresholds), aged debt analysis for worklist prioritisation.

Query patterns: range scans across tax periods for a single taxpayer or aggregations across taxpayers for a single tax type/period. Supports instalment monitoring (UC-DM-08), demand notice generation (UC-DM-02 to UC-DM-04), and aged debtors list (UC-DM-20/DM-SCR-11).

**Cold Tier (4–24 hour freshness, ~2,065 tables globally)**

DM usage: reference data lookups (penalty rates, enforcement thresholds, office codes, NACE classifications), report parameter population, enforcement type configuration.

Query patterns: small lookups by code/ID, cached by the application layer. Supports enforcement configuration (DM-SCR-12), report parameterisation (RPT-FR-001 through RPT-FR-005), and KPI dashboard refresh.

**Archive Tier (weekly, ~2,020 tables globally)**

DM usage: historical comparison in debt management reports, year-over-year debt stock trend analysis, long-term collection effectiveness measurement.

Query patterns: batch analytical queries run during off-peak hours. Supports historical debt stock analysis in KPI dashboards.

### 9.2.3 Silver/Gold Layer Views Consumed by DM

**Silver Layer (denormalised, cross-database joins):**

- taxpayer_360 — unified taxpayer view joining registration data across all tax databases. DM uses for case creation (UC-DM-01) and demand notice recipient details (UC-DM-02 to UC-DM-04).

- taxpayer_balances — consolidated balance view per TIN across all tax types and periods. DM uses to determine total outstanding debt, debt category assignment, and instalment agreement amount validation.

- payment_history — unified payment stream from all payment channels. DM uses for instalment compliance matching (UC-DM-08, BR-DM-040) and payment event polling (INT-CH-05).

- assessment_register — all assessments across tax types. DM uses for default assessment review (UC-DM-17) and debt case amount computation.

**Gold Layer (aggregated, report-ready):**

- debt_stock_summary — current debt stock by category (C1–C5), age band, tax type, and office. Directly feeds DM-SCR-01 KPI tiles and RPT-SCR-01 Collection Status Dashboard.

- kpi_arrears_management — pre-computed KPI metrics for arrears KPIs #24–35. Feeds RPT-SCR-02 KPI Monitoring Dashboard.

- instalment_compliance — agreement compliance rates for KPIs #26, #29, #30, #34. Feeds DM-SCR-06 Instalment Monitoring screen.

### 9.2.4 Data Access Interface

All data access from the Case Management Platform to ClickHouse is via the ClickHouse HTTP REST API or JDBC connector:

**REST API Pattern (preferred for dashboard and real-time queries):**

- Endpoint: https://<clickhouse-lb>:8443/

- Authentication: X-ClickHouse-User / X-ClickHouse-Key headers

- Format: JSON (JSONEachRow) for application consumption, TabSeparated for bulk exports

- Connection pooling: managed by the Case Management Platform's datasource configuration

**JDBC Pattern (for complex joins and reporting queries):**

- Driver: com.clickhouse.jdbc.ClickHouseDriver

- Connection string: jdbc:clickhouse://<clickhouse-lb>:8443/mtca_ors?ssl=true

- Used primarily for the platform's report builder and dataset components

## 9.3 Data Quality Requirements

Data quality is critical for the DM solution because enforcement decisions (escalation sequencing, write-off authorisation, instalment approval) depend on accurate and complete source data.

### 9.3.1 Validation Rules

**At ingestion (ORS pipeline responsibility — external dependency):**

| **Rule ID** | **Description** | **Target** |  |
| --- | --- | --- | --- |
| DQ-VAL-01 | TIN format validation — all TINs conform to MOD11 check digit algorithm. Invalid TINs flagged in pipeline data quality dashboard. | < 0.01% invalid TINs in Hot tier |  |
| DQ-VAL-02 | Amount consistency — PA + IA + PCA = total amount for every transaction. Discrepancies flagged. | 100% consistency |  |
| DQ-VAL-03 | Date validity — all transaction dates within valid range (no future dates for historical transactions). | < 0.1% out-of-range dates |  |
| DQ-VAL-04 | Referential integrity — every transaction references a valid TIN, TaxType code, and TaxPeriod. Orphaned transactions flagged. | < 0.05% orphaned records |  |

**At application layer (Case Management Platform form validation):**

| **Rule ID** | **Description** | **Trigger** |  |
| --- | --- | --- | --- |
| DQ-VAL-05 | Instalment agreement amounts — total scheduled payments must equal or exceed total outstanding debt plus projected interest. | Form submission (DM-SCR-05) |  |
| DQ-VAL-06 | Debt category assignment — system-calculated category (C1–C5) based on consolidated balance must match thresholds defined in BR-DM-001 through BR-DM-005. | Case creation / balance refresh |  |
| DQ-VAL-07 | Enforcement action sequencing — actions must follow the escalation sequence defined for the debt category. Out-of-sequence actions require supervisor override. | Action form submission (DM-SCR-07) |  |
| DQ-VAL-08 | Write-off authorisation — write-off amounts must not exceed the outstanding balance for the relevant TIN/TaxType/TaxPeriod combination. | Write-off form submission (DM-SCR-08) |  |

### 9.3.2 Reconciliation Rules

| **Rule ID** | **Description** | **Frequency** | **Target** |  |
| --- | --- | --- | --- | --- |
| DQ-REC-01 | **STA balance reconciliation** — sum of all L3 transaction amounts per tax type must equal L2 balance; sum of L2 balances must equal L1 consolidated balance. | Daily (scheduled ClickHouse query) | 100% (zero tolerance) |  |
| DQ-REC-02 | **Payment-to-revenue reconciliation** — total payments in taxpayer accounts must reconcile with revenue posted to government revenue accounts. | Daily | < €100 daily variance |  |
| DQ-REC-03 | **Debt stock reconciliation** — total active debt cases in the Case Management Platform must reconcile with total outstanding debit balances in ClickHouse (excluding C1 auto-write-offs). | Weekly | < 1% variance by count, < 0.5% by value |  |
| DQ-REC-04 | **Instalment payment matching** — payments recorded in ORS against a TIN under instalment must match the expected schedule in the Case Management Platform. Mismatches trigger compliance alerts per BR-DM-040. | Daily | 100% matched within 1 business day |  |

### 9.3.3 Completeness Thresholds

| **Data Domain** | **Completeness Target** | **Measurement** |  |
| --- | --- | --- | --- |
| Taxpayer registration (TIN, name, TPT) | 100% | All taxpayers with financial transactions must have complete registration data |  |
| Tax type registration per TIN | ≥ 99.5% | Cross-check: taxpayers with transactions in a tax type must be registered for that type |  |
| Transaction history (all tax types) | ≥ 99.9% | Row count comparison between Informix source and ClickHouse Bronze layer |  |
| Payment records | ≥ 99.9% | Reconciliation against bank settlement files |  |
| Contact information (email or phone) | ≥ 85% | Required for automated notification delivery (DM-FR-016 to DM-FR-020) |  |
| NACE classification for LPs | ≥ 90% | Required for sector-based reporting and risk analysis |  |

### 9.3.4 Data Freshness SLAs (DM-Relevant)

| **Use Case** | **Required Freshness** | **ORS Tier** |  |
| --- | --- | --- | --- |
| Taxpayer balance lookup (case detail view) | < 5 minutes | Hot |  |
| Payment status check (instalment monitoring) | < 5 minutes | Hot |  |
| Debt case dashboard refresh | < 15 minutes | Hot |  |
| Report generation (operational DM reports) | < 1 hour | Warm |  |
| KPI dashboard update | < 24 hours | Cold |  |
| Historical trend reports | < 1 week | Archive |  |

## 9.4 Write-Back Requirements

The DM solution is **read-only** with respect to ClickHouse/ORS. All workflow state is persisted in the Case Management Platform's local database.

### 9.4.1 Data Written to Case Management Platform Database

| **Data Domain** | **Rationale** |  |
| --- | --- | --- |
| Debt case records (status, assignment, priority, lifecycle events) | Workflow state not present in source systems |  |
| Instalment agreement records (schedule, compliance status) | New agreements created in DM workflow |  |
| Recovery action log (all 14 enforcement action types) | Enforcement actions initiated from DM |  |
| Demand notice tracking (generation, delivery, response) | Generation and delivery status |  |
| Write-off records (approval chain, evidence references) | Write-off decisions and authorisation |  |
| User work queue assignments (officer allocation, SLA tracking) | Case officer allocation and workload management |  |
| Notification templates and schedules | Configurable by administrators |  |
| Audit log entries | Immutable action trail for all DM operations |  |

### 9.4.2 Future Write-Back to Transactional Systems

When ITCAS is operational (est. 2028–2029), workflow outcomes will need to be posted back to the transactional systems:

| **Write-Back Item** | **Target System** | **Trigger** | **ITCAS Phase** |  |
| --- | --- | --- | --- | --- |
| Confirmed write-offs | ITCAS accounting module | Write-off approval completion | Phase 2 (T0+36 weeks+) |  |
| Finalised instalment agreements | ITCAS case management | Agreement approval | Phase 2 |  |
| Enforcement action outcomes | ITCAS case management | Action completion recording | Phase 2 |  |
| Case closure events | ITCAS case management | Debt case closed | Phase 2/3 |  |

This is deferred to the ITCAS integration phase and is **not** in scope for the interim solution. The Case Management Platform's data model is designed to support export via REST API for this future integration (ref: INT-ITCAS-01 to INT-ITCAS-05 in Section 10.4).

## Cross-References

| **Section** | **Related Content** |  |
| --- | --- | --- |
| Section 4 (Functional Requirements) | DM-FR-001 to DM-FR-058 — functional requirements that drive data model |  |
| Section 7 (Use Cases) | UC-DM-01 to UC-DM-20 — use cases that define data access patterns |  |
| Section 8 (Business Rules) | BR-DM-001 to BR-DM-060 — rules governing data validation and processing |  |
| Section 10 (Interface Requirements) | System interfaces for ORS, SAS VIYA, notifications, documents |  |
| Section 6 (Non-Functional Requirements) | NFR performance and data quality standards |  |

## External Dependencies

| **Dependency** | **Type** | **Impact on DM Data** |  |
| --- | --- | --- | --- |
| ORS/ClickHouse | Read-only data source | All taxpayer, financial, and reference data |  |
| Informix databases (9) | Upstream source for ORS | Data availability dependent on ORS pipeline health |  |
| SAS VIYA | Risk scoring data | Risk scores and factor breakdowns for case prioritisation |  |
| Bank settlement files | Payment data source | Payment matching for instalment compliance |  |
| EDS (Electronic Document System) | Document storage | Storage and retrieval of generated DM documents |  |
| Apache Atlas (Data Catalogue) | Metadata reference | Table-level lineage and data dictionary for ORS tables |  |

# 10. INTERFACE REQUIREMENTS

## 10.1 User Interface — Design Principles

| **Principle ID** | **Principle** | **Description** |
| --- | --- | --- |
| UI-PRIN-01 | Responsive Design | All screens must render correctly on desktop (1920×1080 minimum), tablet (1024×768), and mobile (375×667) viewports. The Case Management Platform must provide responsive rendering natively. |
| UI-PRIN-02 | WCAG 2.1 Level AA | All screens must meet Web Content Accessibility Guidelines 2.1 at Level AA: sufficient colour contrast (4.5:1 for normal text), keyboard navigability, screen reader compatibility, and visible focus indicators. |
| UI-PRIN-03 | Bilingual Support | All user-facing labels, messages, tooltips, and generated documents must be available in both Maltese and English. Language selection stored in user profile with runtime switching via i18n resource bundles. |
| UI-PRIN-04 | Role-Based Access | Screen visibility and field editability controlled by user role. Seven roles defined: Case Officer, Senior Officer, Team Lead, Manager, Administrator, Auditor (read-only), and System Administrator. |
| UI-PRIN-05 | Consistent Navigation | Left sidebar for module selection, top bar for breadcrumb/context, main content area with tabbed sub-sections. Consistent action buttons (Save, Submit, Cancel, Print) positioned bottom-right. |
| UI-PRIN-06 | Data Density | Tables with sortable columns, inline search/filter, configurable column visibility, and export-to-Excel capability on all data grids. |

## 10.2 User Interface — DM Screens

### 10.2.1 Screen Inventory

| **Screen ID** | **Screen Name** | **Primary Actor** | **Key Functions** | **Source Reqs** |  |
| --- | --- | --- | --- | --- | --- |
| DM-SCR-01 | Debt Case Dashboard | Case Officer | Active cases, priority queue, filters by category/age/officer/status, KPI tiles | DM-FR-001–005 |  |
| DM-SCR-02 | Case Detail View | Case Officer | Full case record: taxpayer info (from ORS), debt breakdown, action history, timeline, documents | DM-FR-006–010 |  |
| DM-SCR-03 | Risk Score Panel | Case Officer | SAS VIYA risk score display, contributing factors, historical trend, manual override | DM-FR-011–015 |  |
| DM-SCR-04 | Demand Notice Generator | Case Officer | Template selection, recipient details, delivery method, preview, batch generation | DM-FR-016–020 |  |
| DM-SCR-05 | Instalment Agreement Form | Case Officer | Application capture (online/counter), schedule calculation, interest projection, approval routing | DM-FR-021–030 |  |
| DM-SCR-06 | Instalment Monitoring | Senior Officer | Compliance tracking, missed payment alerts, auto-cancellation triggers, new case creation | DM-FR-031–035 |  |
| DM-SCR-07 | Enforcement Action Form | Senior Officer | Action type selection (14 types), threshold validation, document generation, agent assignment | DM-FR-036–042 |  |
| DM-SCR-08 | Write-Off Processing | Manager | Individual/bulk write-off, approval workflow, supporting evidence upload, authorisation chain | DM-FR-043–048 |  |
| DM-SCR-09 | Default Assessment Form | Senior Officer | Auto-generation review, amount validation (exaggeration check), taxpayer notification | DM-FR-049–052 |  |
| DM-SCR-10 | Collection Planning View | Manager | Resource allocation, workload distribution, target setting by team/officer | DM-FR-053–055 |  |
| DM-SCR-11 | Debtors List | Manager | Configurable debtor listing: filters by category, age, tax type, office; export capability | DM-FR-056–058 |  |
| DM-SCR-12 | Enforcement Configuration | Administrator | Action types, debt thresholds, escalation rules, document templates, agent roles | DM-FR-036, BR-DM-001–005 |  |

**Total DM screens: 12**

### 10.2.2 Key Screen Descriptions

**DM-SCR-01: Debt Case Dashboard** — The primary DM working screen. Top row: KPI summary tiles (total active cases, total debt value, cases approaching SLA, overdue actions). Main content: sortable data grid of assigned cases with columns: TIN, taxpayer name, debt category (C1–C5), total amount, oldest debt age, risk score, current status, next action due date. Filters: category, status, officer assignment, age band, tax type. Quick actions: Open Case, Generate Demand Notice, Log Phone Call, Create Instalment Application.

> **Data sources:** Debt case data from Case Management Platform database; taxpayer name from ORS (taxpayer_360 Silver view); balance amounts refreshed from ORS (taxpayer_balances Silver view); risk score from SAS VIYA API (INT-SAS-01). KPI tiles populated from ORS Gold layer (debt_stock_summary, kpi_arrears_management).

**DM-SCR-02: Case Detail View** — Opened from the dashboard or by direct TIN search. Header: taxpayer identity panel (TIN, name, type, contact — all from ORS). Body tabs: (1) Debt Summary — outstanding amounts by tax type/period from ORS, with C1–C5 category indicator; (2) Action History — chronological list of recovery actions from Case Management Platform database; (3) Timeline — visual timeline of case lifecycle events; (4) Documents — linked documents from EDS; (5) Notes — officer notes and activity log. Right panel: risk score widget (from SAS VIYA), linked instalment agreement status, next scheduled action.

**DM-SCR-05: Instalment Agreement Form** — Multi-step form wizard. Step 1: Taxpayer identification (TIN lookup against ORS, auto-populate name/contact from taxpayer_360). Step 2: Debt summary (auto-populated from ORS taxpayer_balances — all outstanding balances by tax type/period). Step 3: Schedule configuration (number of instalments, frequency, start date, total amount — system calculates per-instalment amount with interest per BR-DM-021 to BR-DM-030). Step 4: Approval routing (auto-route based on amount thresholds: within standard parameters → auto-approve; outside parameters → supervisor approval per BR-DM-025). Step 5: Confirmation and taxpayer notification via INT-NOT-01 to INT-NOT-04.

**DM-SCR-07: Enforcement Action Form** — Used when escalating a debt case. Displays current case status, enforcement history, and available next actions (filtered by debt category escalation rules per BR-DM-001 to BR-DM-005). Officer selects action type from the 14 available types, system validates sequencing (DQ-VAL-07), generates required documents (via INT-DOC-02), and optionally assigns an agent (for field enforcement actions). Out-of-sequence actions require supervisor override with recorded justification.

**DM-SCR-08: Write-Off Processing** — Supports both individual and bulk write-off. Individual: case-level write-off with evidence upload, amount validation against ORS balance (DQ-VAL-08), and multi-level approval workflow. Bulk: filter-based selection (e.g., all C1 debts < €30), preview of affected cases with total amount, and batch approval with Administrator authorisation. All write-offs create an immutable audit trail entry.

## 10.3 User Interface — DM Reporting Screens

### 10.3.1 Screen Inventory

| **Screen ID** | **Screen Name** | **Primary Actor** | **Key Functions** | **Source Reqs** |  |
| --- | --- | --- | --- | --- | --- |
| RPT-SCR-01 | Collection Status Dashboard | Manager | Real-time KPI tiles: debt stock, collection rate, ageing, instalment compliance | RPT-FR-001–003 |  |
| RPT-SCR-02 | KPI Monitoring Dashboard | Director | Arrears management KPIs #24–35, trend charts, target vs actual, drill-down | RPT-FR-004–005 |  |
| RPT-SCR-04 | Report Configuration | Any Officer | Parameter selection (tax type, period, office, sector), delimiter config, save/load | RPT-FR-009–015 |  |
| RPT-SCR-05 | Report Viewer | Any Officer | On-screen display, print, export (PDF, Excel, CSV), scheduled delivery | RPT-FR-016–021 |  |

> **Note:** RPT-SCR-03 (Revenue Reconciliation Report) is excluded — this is an STA-only reporting screen (per extraction rule #27). DM-relevant reconciliation data appears in DQ-REC-01 to DQ-REC-04 (Section 9.3.2).

### 10.3.2 Key Reporting Screen Descriptions

**RPT-SCR-01: Collection Status Dashboard** — Real-time operational dashboard for DM management. KPI tiles: total active debt stock (by value and count), collection rate (current period vs target), debt ageing distribution (pie chart by C1–C5 category), instalment compliance rate, enforcement action success rate. All data sourced from ORS Gold layer views (debt_stock_summary, kpi_arrears_management, instalment_compliance). Drill-down from each tile to underlying case data.

**RPT-SCR-02: KPI Monitoring Dashboard** — Strategic dashboard showing the 12 arrears management KPIs (Items #24–35 from the PDF-T5 KPI framework). Each KPI displayed with: current value, target, trend (6-month sparkline), and status indicator (green/amber/red). Data sourced from ORS Gold layer (kpi_arrears_management). Drill-down to contributing data points. Export capability for management reporting.

### 10.3.3 Workflow and Administration Screens

| **Screen ID** | **Screen Name** | **Primary Actor** | **Key Functions** | **Source Reqs** |  |
| --- | --- | --- | --- | --- | --- |
| WF-SCR-01 | Workflow Configuration | Administrator | Process definition (BPMN 2.0), SLA thresholds, escalation rules, notification triggers | WF-FR-001–010 |  |
| WF-SCR-02 | Work Queue Management | Team Lead | Queue assignment, rebalancing, priority adjustment, performance monitoring | WF-FR-011–015 |  |
| WF-SCR-03 | Case Reassignment | Team Lead | Bulk reassignment, absence coverage, load balancing across officers | WF-FR-016–020 |  |
| ADM-SCR-01 | Business Rules Administration | Administrator | Rule catalogue view, rule editing (54 configurable rules), effective dating, audit trail | BR- (configurable rules) |  |
| ADM-SCR-02 | Notification Template Manager | Administrator | Template editing (Maltese/English), variable insertion, channel configuration, preview | WF-FR-006–008 |  |
| ADM-SCR-03 | User & Role Management | System Admin | User accounts, role assignment, permission matrix, access log | NFR-SEC-001–005 |  |

**Total reporting/admin screens: 10**

### 10.3.4 Total Screen Summary

| **Module** | **Screen Count** | **Screen ID Range** |  |
| --- | --- | --- | --- |
| Debt Management | 12 | DM-SCR-01 to DM-SCR-12 |  |
| Reporting | 4 | RPT-SCR-01, 02, 04, 05 |  |
| Workflow/Administration | 6 | WF-SCR-01–03, ADM-SCR-01–03 |  |
| **Total** | **22** |  |  |

> **Note:** 10 STA screens (STA-SCR-01 to STA-SCR-10) are excluded from this specification. Taxpayer account data required by DM screens is accessed as read-only views from ORS/ClickHouse rather than through dedicated STA screens. STA account viewing capability may be provided through the ORS reporting layer (Apache Superset) or through the future ITCAS system.

## 10.4 System Interfaces

### 10.4.1 ORS / ClickHouse REST API

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **Frequency** |  |
| --- | --- | --- | --- | --- | --- |
| INT-CH-01 | Read | HTTP/REST | Taxpayer account data (L1/L2/L3 balances, transactions) | Real-time, per user action |  |
| INT-CH-02 | Read | HTTP/REST | Debt stock queries (aggregate debt by category, age, tax type) | Real-time, dashboard refresh |  |
| INT-CH-03 | Read | JDBC | Report generation queries (complex joins, aggregations) | On-demand / scheduled |  |
| INT-CH-04 | Read | HTTP/REST | Reference data (tax rates, codes, periods, offices) | Cached, refreshed every 4 hours |  |
| INT-CH-05 | Read | HTTP/REST | Payment event polling (new payments for instalment matching) | Every 5 minutes |  |

**Authentication:** Service account with read-only privileges on mtca_ors database. SSL/TLS mandatory. API key rotation every 90 days.

**Error handling:** Circuit breaker pattern with 3 retry attempts, 5-second backoff. Fallback: cached data with staleness indicator displayed to user. ClickHouse health check endpoint polled every 30 seconds.

### 10.4.2 SAS VIYA Scoring API

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **Frequency** |  |
| --- | --- | --- | --- | --- | --- |
| INT-SAS-01 | Read | REST/JSON | Risk score retrieval for a specific TIN | On-demand, when case opened |  |
| INT-SAS-02 | Read | REST/JSON | Batch risk scoring for work queue prioritisation | Daily batch (overnight) |  |
| INT-SAS-03 | Read | REST/JSON | Risk factor breakdown (contributing factors to score) | On-demand, risk panel display |  |
| INT-SAS-04 | Write | REST/JSON | Case outcome feedback (for model retraining) | Event-driven, on case closure |  |

**Data flow:** ClickHouse risk data marts → SAS VIYA → risk score API → Case Management Platform. The SAS VIYA API gateway serves as the intermediary. Risk scores are numeric (0–100) with accompanying factor weights.

**SLA:** Score retrieval < 2 seconds for individual TIN, < 30 minutes for full batch.

### 10.4.3 Notification Gateway

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **Frequency** |  |
| --- | --- | --- | --- | --- | --- |
| INT-NOT-01 | Write | SMTP | Email notifications (demand notices, instalment reminders, status updates) | Event-driven |  |
| INT-NOT-02 | Write | SMS Gateway API | SMS reminders (payment due dates, instalment defaults) | Event-driven |  |
| INT-NOT-03 | Write | REST | In-app notifications (Case Management Platform notification centre) | Event-driven |  |
| INT-NOT-04 | Write | REST/Print API | Physical letter generation (demand notices, judicial letters) — queued for postal services | Batch daily |  |

**Template management:** Notification content driven by templates stored in the Case Management Platform, supporting Maltese/English, with merge variables for taxpayer name, TIN, amounts, dates, and reference numbers. Templates managed via ADM-SCR-02.

### 10.4.4 Document Generation (ADG)

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **Frequency** |  |
| --- | --- | --- | --- | --- | --- |
| INT-DOC-01 | Write/Read | REST | Demand notice document generation (PDF, templated) | Event-driven |  |
| INT-DOC-02 | Write/Read | REST | Instalment agreement document generation (PDF) | On agreement approval |  |
| INT-DOC-03 | Write/Read | REST | Tax clearance certificate generation (PDF) | On-demand |  |
| INT-DOC-04 | Read | REST | Document retrieval from EDS (Electronic Document System) | On-demand |  |

**Technology:** The Case Management Platform's built-in PDF generation for simple documents; external ADG service (Apache FOP or similar) for complex templated documents requiring precise formatting for legal compliance (judicial letters, warrant documents).

> **Note:** INT-DOC-01 corresponds to the source's INT-DOC-02 (demand notices). The source's INT-DOC-01 (TAS generation — Tax Account Statement) is excluded as an STA-only function. TAS may be provided via ORS reporting (Apache Superset) or the future ITCAS system.

### 10.4.5 External System Interfaces

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **Phase** |  |
| --- | --- | --- | --- | --- | --- |
| INT-EXT-01 | Read | REST/SFTP | Bank payment file ingestion (settlement files for payment matching) | Phase 1 |  |
| INT-EXT-02 | Write | REST | Bank account garnishing requests (automated freezing via web services) | Phase 2 |  |
| INT-EXT-03 | Read | REST | Land/property register lookup (asset identification for enforcement) | Phase 2 |  |
| INT-EXT-04 | Read | REST | Vehicle register lookup (asset identification) | Phase 2 |  |
| INT-EXT-05 | Read/Write | REST | Government CRM (MS Dynamics) — complaint/interaction tracking | Phase 3 |  |
| INT-EXT-06 | Read | REST | Taxpayer portal — self-service instalment applications, debt status queries | Phase 2 |  |
| INT-EXT-07 | Read/Write | REST | Treasury interface — revenue reconciliation, refund forecasting | Phase 3 |  |

**Phasing rationale:** Phase 1 interfaces are prerequisites for DM operation. Phase 2 interfaces support enforcement automation (bank garnishing, asset lookup) and self-service (portal). Phase 3 interfaces support broader government integration.

## 10.5 Future ITCAS Integration Points

These interfaces are **designed but not implemented** in the interim solution. They define the migration pathway to the future ITCAS system (Integrated Tax and Customs Administration System, European Dynamics contract).

| **Interface ID** | **Direction** | **Protocol** | **Purpose** | **ITCAS Phase** |  |
| --- | --- | --- | --- | --- | --- |
| INT-ITCAS-01 | Write | REST | Post confirmed write-offs to ITCAS accounting module | Phase 2 (T0+36 weeks+) |  |
| INT-ITCAS-02 | Write | REST | Post finalised instalment agreements to ITCAS | Phase 2 |  |
| INT-ITCAS-03 | Read | REST | Read taxpayer data from ITCAS (replacing ClickHouse reads) | Phase 2 |  |
| INT-ITCAS-04 | Read | Events/Kafka | Receive real-time transaction events from ITCAS | Phase 2 |  |
| INT-ITCAS-05 | Bidirectional | REST | Case management synchronisation (ITCAS CM ↔ Case Management Platform) | Phase 2/3 |  |

**Migration strategy:** A data source abstraction layer in the Case Management Platform ensures that switching from ClickHouse to ITCAS requires configuration changes (connection strings, query mapping) rather than application code changes. BPMN 2.0 process definitions can be exported from the Case Management Platform and imported into ITCAS's BPM engine, preserving workflow logic investment.

**Key design decisions for ITCAS readiness:**

- All ORS data access is routed through a configurable service layer (not hardcoded queries), enabling endpoint substitution

- Workflow processes are defined in BPMN 2.0 standard, ensuring portability

- Business rules are externalized in a configurable rule engine, not embedded in application code

- Case data schema aligns with industry-standard case management patterns for interoperability

- REST APIs follow OpenAPI 3.0 specification for documentation and contract-first development

## Cross-References

| **Section** | **Related Content** |  |
| --- | --- | --- |
| Section 4 (Functional Requirements) | DM-FR-xxx requirements defining screen and interface behaviour |  |
| Section 5 (Platform Capabilities) | CFW, WF, INT capability requirements for the Case Management Platform |  |
| Section 6 (Non-Functional Requirements) | Performance SLAs, security requirements for interfaces |  |
| Section 7 (Use Cases) | UC-DM-xx use cases driving screen and interface design |  |
| Section 8 (Business Rules) | BR-DM-xxx rules governing screen logic and validation |  |
| Section 9 (Data Requirements) | Data model and ORS access patterns underlying interfaces |  |

## External Dependencies

| **Dependency** | **Interface Group** | **Impact** |  |
| --- | --- | --- | --- |
| ORS/ClickHouse | INT-CH-01 to INT-CH-05 | Primary data source — unavailability degrades all DM screens |  |
| SAS VIYA | INT-SAS-01 to INT-SAS-04 | Risk scoring — unavailability degrades DM-SCR-03, DM-SCR-01 prioritisation |  |
| SMTP/SMS Gateway | INT-NOT-01, INT-NOT-02 | Notification delivery — unavailability delays demand notices |  |
| EDS | INT-DOC-04 | Document storage — unavailability prevents document retrieval |  |
| Bank web services | INT-EXT-01, INT-EXT-02 | Payment matching and garnishing — critical for Phases 1-2 |  |
| Land/Vehicle registers | INT-EXT-03, INT-EXT-04 | Asset identification — enforcement capability (Phase 2) |  |
| Government CRM | INT-EXT-05 | Interaction history — integration capability (Phase 3) |  |
| ITCAS (European Dynamics) | INT-ITCAS-01 to INT-ITCAS-05 | Future migration target — designed, not implemented |  |
| MITA (SSO/LDAP) | ADM-SCR-03 | User authentication and role management |  |

# Section 11: Implementation Considerations

## 11.1 Prerequisites

Successful implementation of the Debt Management module depends on the availability of foundational infrastructure and data services. These prerequisites must be satisfied before DM development commences.

### 11.1.1 ORS / ClickHouse Readiness

The Debt Management module reads all taxpayer, account, and transaction data from ORS/ClickHouse. The following ORS milestones are hard prerequisites:

| **Prerequisite** | **Required State** | **Gate** |
| --- | --- | --- |
| ORS POC validation | 60x performance improvement confirmed on representative queries | Gate G1 (March 2026) |
| Income Tax and VAT databases | Loaded in ClickHouse with Hot tier freshness (< 5 min) | Before DM Phase 1 dependency satisfied |
| All 9 Informix databases | Loaded in ClickHouse with > 99% freshness SLA compliance | Before DM Phase 2 start (Q3 2026) |
| Silver and Gold layer views | ~253 tables across 9 databases consumed; 4 Silver + 3 Gold layer views operational | Before DM Phase 2 start |
| ORS data quality thresholds | Minimum 99.5% data accuracy on balance reconciliation | Continuous |

The DM module does not directly access legacy Informix databases. All data access is mediated through ORS/ClickHouse views, ensuring a clean separation between the operational reporting layer and the case management application.

### 11.1.2 Platform Provisioning

The Case Management Platform must be deployed and configured before DM development begins:

| **Prerequisite** | **Required State** | **Timeline** |
| --- | --- | --- |
| Platform instance deployed | Development, staging, and production environments provisioned | Before DM development start |
| ClickHouse connector | Custom connector developed and validated for ORS data access | Before DM Phase 2 start |
| SAS VIYA integration | Risk scoring API operational with < 2 second response time | Before DM Phase 2 start |
| MITA infrastructure | Network access, SSO/LDAP integration, hosting environment confirmed | Before DM development start |
| BPMN modelling environment | Workflow designer available for process definition using standard BPMN 2.0 constructs | Before DM Phase 2 start |

### 11.1.3 Phase 1 Foundation (Prerequisite Dependency)

DM Phase 2 depends on the successful completion of the STA/ORS Phase 1 foundation (Q2 2026). Phase 1 delivers the core data access layer that DM workflows consume:

- Unified taxpayer account view operational across Income Tax and VAT, sourced from ORS/ClickHouse

- Core data model entities (Taxpayer, TaxAccount, TaxType, TaxPeriod, Transaction) accessible via ClickHouse integration (INT-CH-01 through INT-CH-04)

- Basic account statement generation and tax clearance certificate capabilities demonstrating end-to-end ORS data consumption

- ORS Hot tier achieving < 5 minute freshness for Income Tax and VAT tables

Phase 1 establishes that the ORS data pipeline reliably delivers taxpayer data at the quality and freshness levels required for DM case management decisions. The go/no-go gate for DM Phase 2 is predicated on Phase 1 stability in production.

**Note:** Phase 1 deliverables (STA screens, TAS generation) are outside the scope of this DM specification. They are referenced here solely as prerequisite dependencies per the STA/DM phasing strategy.

## 11.2 Phasing Strategy

The Debt Management module is implemented across two primary phases, aligned with ORS readiness milestones and ITCAS vendor timeline. Each phase delivers measurable capability and is gated by explicit go/no-go criteria.

### 11.2.1 Phase 2: Debt Management Workflows (Q3–Q4 2026 — July to December)

**Objective:** Implement the full debt management case lifecycle, instalment agreements, and automated notification workflows. This is the primary DM delivery phase and the principal FTE liberation target.

**Prerequisites:** Phase 1 stable in production; all 9 Informix databases loaded in ClickHouse; SAS VIYA risk scoring API operational.

**Deliverables:**

| **Category** | **Items** | **Specification Reference** |
| --- | --- | --- |
| DM screens | DM-SCR-01 through DM-SCR-12 (all 12 Debt Management screens) | Section 10 (Interfaces) |
| BPMN workflows | Debt case lifecycle, instalment lifecycle, enforcement escalation, write-off approval | Section 7 (Use Cases UC-DM-01 to UC-DM-20) |
| SAS VIYA integration | INT-SAS-01 through INT-SAS-04 — risk scoring, payment prediction, prioritisation, analytical feeds | Section 10.2.2 |
| Notification gateway | INT-NOT-01 through INT-NOT-04 — demand notices, reminders, instalment confirmations, escalation alerts | Section 10.2.3 |
| Document generation | INT-DOC-01 through INT-DOC-04 — demand notices, instalment agreements, write-off certificates, enforcement notices | Section 10.2.4 |
| External integrations | INT-EXT-01 (bank payment files), INT-EXT-02 (garnishing), INT-EXT-06 (self-service portal) | Section 10.2.5 |
| Business rules | All 84 business rules configured (54 configurable via UI): 60 BR-DM + 10 BR-RPT + 7 BR-WF + 7 BR-ADM | Section 8 |

**FTE Impact:** ~8–10 FTE reduction through automated debt case creation (~2.7 FTE), automated demand notice generation (~1.9 FTE), instalment agreement processing (~1.4 FTE), risk-based prioritisation (~1.3 FTE), automated reporting (~1.8 FTE), and enforcement action tracking (~0.9 FTE). See Section 11.5 for the complete FTE liberation breakdown.

**Go/No-Go Criteria:**

| **Criterion** | **Threshold** |
| --- | --- |
| ClickHouse data availability | All 9 Informix databases loaded with > 99% freshness SLA compliance |
| SAS VIYA scoring API | Response time < 2 seconds for risk score requests |
| UAT validation | Minimum 20 debt cases successfully processed end-to-end |
| Data quality | Reconciliation rules DQ-REC-01 through DQ-REC-04 passing with < 0.5% variance |
| Workflow completeness | All 4 BPMN lifecycle processes executable without manual intervention for standard cases |

### 11.2.2 Phase 3: Advanced DM and External Integration (Q1 2027 — January to March)

**Objective:** Extend enforcement capabilities with external system integrations and advanced analytical features.

**Prerequisites:** Phase 2 stable in production for 90+ days; external system APIs verified in integration testing; legal authority confirmed for automated garnishing and name publication.

**Deliverables:**

| **Category** | **Items** | **Specification Reference** |
| --- | --- | --- |
| External registers | INT-EXT-03 (property register), INT-EXT-04 (vehicle register) — asset identification for enforcement | Section 10.2.5 |
| CRM integration | INT-EXT-05 — complaint and interaction tracking | Section 10.2.5 |
| Treasury interface | INT-EXT-07 — real-time revenue reconciliation | Section 10.2.5 |
| Enhanced analytics | Payment likelihood prediction, network analysis via SAS VIYA | Section 10.2.2 |
| Bulk processing | Batch demand notices, bulk write-offs, mass case reassignment | UC-DM-15, UC-DM-18 |
| Agent management | Warrant officer appointment, case assignment, outcome tracking | UC-DM-16, UC-DM-17 |

**FTE Impact:** ~1–2 FTE additional through automated asset identification and bulk processing capabilities.

**Go/No-Go Criteria:**

| **Criterion** | **Threshold** |
| --- | --- |
| Phase 2 stability | 90+ consecutive days in production without critical incidents |
| External API readiness | Property register, vehicle register, CRM, and Treasury APIs verified in integration testing |
| Legal authority | Confirmed for automated garnishing orders and debtor name publication |

### 11.2.3 Phase 4: Full Reporting and ITCAS Preparation (Q2 2027 — April to June)

**Objective:** Complete the reporting suite covering all DM KPIs and prepare for ITCAS migration.

**Deliverables:**

| **Category** | **Items** | **Specification Reference** |  |
| --- | --- | --- | --- |
| Reporting screens | RPT-SCR-01 through RPT-SCR-04 (4 DM reporting screens) | Section 10.1.2 |  |
| Workflow administration | WF-SCR-01 through WF-SCR-03 | Section 10.1.3 |  |
| Administration screens | ADM-SCR-01 through ADM-SCR-03 | Section 10.1.3 |  |
| KPI views | Gold-layer ClickHouse views for all 12 Arrears Management KPIs | Section 11.5.2 |  |
| ITCAS migration package | BPMN 2.0 export, data migration specifications, operational runbook | Section 11.3 |  |

**Go/No-Go Criteria:** All 12 Arrears Management KPIs producing accurate data; BPMN export validated for import compatibility; ITCAS requirements verification (DcP3) completed.

### 11.2.4 Phase Summary

| **Phase** | **Timeline** | **DM Scope** | **Integrations** | **FTE Impact** | **Reqs Covered** |  |
| --- | --- | --- | --- | --- | --- | --- |
| 1 (prerequisite) | Q2 2026 | ORS foundation (not DM scope) | 4 ClickHouse | — | — |  |
| **2 (primary)** | **Q3–Q4 2026** | **12 DM screens + 4 BPMN workflows** | **SAS, notifications, docs, bank, portal** | **8–10** | **~130** |  |
| 3 | Q1 2027 | Enforcement extensions + bulk processing | Property, vehicle, CRM, Treasury | 1–2 | ~15 |  |
| 4 | Q2 2027 | 4 RPT + 3 WF + 3 ADM screens | KPI views, ITCAS prep | (reporting) | ~8 |  |
| **DM Total** | **9 months (Q3 2026 – Q2 2027)** | **22 screens** | **~25 interfaces** | **~9–12** | **~153** |  |

## 11.3 ITCAS Migration Strategy

The Debt Management interim solution is explicitly designed as a bridge to the ITCAS integrated platform. All design decisions prioritise portability and investment protection, ensuring that the transition to ITCAS is technically feasible and that no significant investment is lost.

### 11.3.1 What Migrates to ITCAS

| **Component** | **Migration Path** | **ITCAS Target** |
| --- | --- | --- |
| **BPMN process definitions** | Export as BPMN 2.0 XML → import into ITCAS BPM engine | ITCAS Case Management |
| **Business rules** | Configurable rules exported as decision tables → reimplemented in ITCAS rules engine | ITCAS Configuration Module |
| **Workflow data** (active cases, agreements) | Database export → transformation → ITCAS import via migration scripts | ITCAS Debt Management |
| **Document templates** | Template content (Maltese/English) → recreated in ITCAS document generation | ITCAS ADG |
| **Notification configurations** | Channel/template/schedule → recreated in ITCAS notification system | ITCAS Communications |
| **Report definitions** | Query logic and KPI formulas → reimplemented using ITCAS reporting | ITCAS MIS/Reporting |

### 11.3.2 What Stays / Retires

| **Component** | **Action** | **Rationale** |
| --- | --- | --- |
| **Case Management Platform** | Retire after ITCAS DM module goes live (~2028) | Interim solution replaced by integrated system |
| **ClickHouse integration layer** | Redirected to ITCAS data sources | ORS continues as analytical platform; DM reads switch to ITCAS |
| **Platform-local database** (workflow state) | Archived after data migration to ITCAS | Historical reference only |
| **SAS VIYA integration** | Retained — SAS continues as analytical layer on top of ITCAS/ClickHouse | Long-term analytical capability |

### 11.3.3 BPMN Portability

The Case Management Platform supports BPMN 2.0 process export. The following measures ensure portability to ITCAS:

- All workflow processes modelled using standard BPMN 2.0 constructs (tasks, gateways, events, timers) without platform-proprietary extensions

- Process variables use generic data types (string, number, date, boolean) — no platform-specific bindings

- Integration points abstracted behind service tasks with configurable endpoint URLs — switching from ClickHouse to ITCAS requires endpoint reconfiguration, not process redesign

- Human tasks reference roles (not specific users) — role mapping translatable to any BPM platform

- Decision logic encapsulated in external business rules (not embedded in process flows) — rules independently portable

**Limitation:** Platform-specific form bindings (UI layout, field mappings) do not export via BPMN 2.0. Forms must be rebuilt in the ITCAS platform. The DM specification mitigates this by documenting all form specifications in Section 10 (Interfaces) to serve as rebuilding blueprints.

### 11.3.4 Data Migration

Active workflow data must migrate from the Case Management Platform's local database to ITCAS:

- **Active debt cases:** case records, action histories, assigned officers, current status → mapped to ITCAS case management schema

- **Active instalment agreements:** schedules, payment history, compliance status → mapped to ITCAS instalment module

- **Pending enforcement actions:** in-progress garnishing orders, liens, agent assignments → manual verification during parallel running

- **Historical data:** completed cases, expired agreements → bulk migration for reference (lower priority, can be deferred)

**Parallel running:** A minimum 3-month parallel operation period where both the Case Management Platform and ITCAS DM modules run concurrently on the same data. Case officers work in ITCAS while the interim platform remains available for reference. Cut-over occurs when ITCAS DM achieves functional parity validated by business users.

### 11.3.5 Timeline Alignment with ITCAS

| **Milestone** | **Date (Est.)** | **DM Implication** |
| --- | --- | --- |
| ITCAS T0 (contract start) | March 2026 | DM prerequisite (Phase 1) under development concurrently |
| DcP3 (Requirements verification) | June 2026 | DM requirements shared with ITCAS vendor as input to their DM module design |
| DcP4.1 (Phase 1 design) | October 2026 | Confirm ITCAS DM architecture compatible with DM migration path |
| ITCAS Phase 2 start (Payments, Enforcement) | ~November 2026 | DM Phase 2 in production; ITCAS team can reference working system |
| ITCAS Phase 2 UAT | Est. H2 2027 | Begin parallel running preparation |
| ITCAS Phase 2 go-live | Est. H1 2028 | DM migration window opens |
| DM retirement | Est. H2 2028 | Full transition to ITCAS |

The DM interim solution provides the ITCAS vendor with a working reference implementation of debt management workflows, validated business rules, and demonstrated KPI data flows — significantly de-risking the ITCAS DM module delivery.

## 11.4 Risks and Mitigations

### 11.4.1 Risk Summary Matrix

| **#** | **Risk** | **Likelihood** | **Impact** | **Severity** | **Primary Mitigation** |  |
| --- | --- | --- | --- | --- | --- | --- |
| R1 | Data quality propagation | High | High | **Critical** | Automated reconciliation + manual verification threshold |  |
| R2 | User adoption resistance | Medium | High | **High** | Pilot users + supervisor overrides + role-specific training |  |
| R3 | Integration complexity | Medium | Medium | **Medium** | Circuit breakers + cached fallbacks + integration test environment |  |
| R4 | Platform limitations | Low–Medium | Low–Medium | **Low** | Accept for interim + workarounds + ITCAS migration |  |
| R5 | ORS readiness timing | Medium | High | **High** | Phased database dependency + ORS Gate G1 checkpoint |  |
| R6 | ITCAS coordination | Medium | Medium–High | **Medium–High** | Governance separation + shared requirements + BPMN portability |  |

### 11.4.2 R1: Data Quality Propagation (Critical)

**Risk:** Source data in legacy Informix systems contains inconsistencies, missing records, or incorrect balances that propagate through ORS/ClickHouse into the DM solution, leading to incorrect taxpayer account views, erroneous interest calculations, or invalid enforcement actions.

**Likelihood:** High — most KPI data is currently classified as "Not Yet" available, and legacy systems have operated without integrated reconciliation for decades.

**Impact:** High — incorrect enforcement actions against taxpayers could cause legal exposure, reputational damage, and taxpayer complaints.

**Mitigations:**

- Implement reconciliation rules DQ-REC-01 through DQ-REC-04 as automated daily checks with exception reporting (Section 9.3)

- Establish data quality thresholds as go/no-go criteria for each phase (Section 9.3.3)

- Create a data quality dashboard (Grafana) comparing Informix source counts against ClickHouse

- Require manual verification for all enforcement actions exceeding €1,000 during the first 6 months of operation

- Engage the Data Catalogue (Apache Atlas) to document known quality issues per table

### 11.4.3 R2: User Adoption Resistance (High)

**Risk:** MTCA staff resist the new DM system due to unfamiliarity with the platform, preference for established manual processes, or distrust of automated decisions (particularly automated write-offs and debt categorisation).

**Likelihood:** Medium — MTCA has limited experience with digital workflow systems.

**Impact:** High — low adoption undermines the 8–10 FTE liberation target and delays realisation of benefits.

**Mitigations:**

- Involve 4–6 case officers as pilot users during UAT for each phase; incorporate their feedback before rollout

- Design screens to match existing mental models (e.g., the TAS format familiar to officers)

- Provide supervisor override for all automated decisions during the first 3 months (BR-DM-060 configurable override period)

- Deliver role-specific training: 2-day hands-on workshops per role, plus self-paced e-learning in Maltese/English

- Measure adoption weekly via system usage dashboards; address drop-offs with targeted coaching

### 11.4.4 R3: Integration Complexity (Medium)

**Risk:** Multiple system integrations (ClickHouse, SAS VIYA, notification gateway, document generation, external registers) create a complex dependency chain where failure of any component degrades or blocks DM operations.

**Likelihood:** Medium — each integration is technically straightforward but the combined reliability requirement is demanding.

**Impact:** Medium — degraded service during integration outages; potential data inconsistency if partial failures are not handled correctly.

**Mitigations:**

- Implement circuit breaker pattern on all external API calls with graceful degradation (cached data + staleness warning)

- ClickHouse health check endpoint polled every 30 seconds; automatic failover to replica if primary unresponsive

- SAS VIYA scoring: cache risk scores locally with 24-hour TTL; display cached score with timestamp if API unavailable

- Notification delivery: asynchronous queue with retry logic; batch failures reported to administrators

- Integration testing environment that simulates each external system's failure modes

### 11.4.5 R4: Platform Limitations (Low)

**Risk:** The selected Case Management Platform's partial WCAG 2.1 compliance, basic case management capabilities (compared to enterprise platforms), and limited process simulation may constrain advanced functionality or require workarounds.

**Likelihood:** Low–Medium — platform evaluation identifies specific gaps in digital signatures, rule testing/simulation, and case hierarchy support.

**Impact:** Low–Medium — workarounds increase development effort by an estimated 15–20% for affected features.

**Mitigations:**

- Accept platform limitations for the interim period (12–24 months); plan full capability delivery in ITCAS

- Budget 20% contingency in development effort for accessibility remediation (CSS customisation, ARIA attributes)

- Use the platform's plugin marketplace for gaps: digital signatures, advanced charts, bulk processing

- For case hierarchy requirements (debt case → enforcement sub-cases): implement using sub-process features with parent reference fields rather than native parent-child case types

### 11.4.6 R5: ORS Readiness Timing (High)

**Risk:** The ORS ClickHouse pipeline does not achieve production-ready status on the timeline required for DM Phase 2 (Q3 2026), delaying or blocking the DM solution.

**Likelihood:** Medium — ORS depends on infrastructure procurement, MITA network access, and successful completion of pipeline rollout across all 9 databases.

**Impact:** High — DM cannot deliver value without access to taxpayer data in ClickHouse.

**Mitigations:**

- DM Phase 2 requires all 9 databases — align with ORS Phase 3 pipeline rollout

- Establish explicit ORS readiness checkpoints in the DM project plan: data availability per database at T-4 weeks before DM Phase 2 start

- Contingency: if ClickHouse data for a specific database is delayed, DM can launch with partial tax type coverage and expand incrementally

- ORS Gate G1 (60x performance validation) in March 2026 serves as the foundation go/no-go

### 11.4.7 R6: ITCAS Coordination (Medium–High)

**Risk:** The DM interim solution and ITCAS vendor implementation create competing or contradictory requirements, duplicate effort, or political tension between the parallel initiatives.

**Likelihood:** Medium — the potential for implementer reluctance and disputes regarding integration between quick-win components and ITCAS has been explicitly identified.

**Impact:** Medium–High — wasted investment, confused stakeholders, or contractual disputes with the ITCAS vendor.

**Mitigations:**

- Position DM explicitly as a proof of concept and operational bridge that de-risks ITCAS DM requirements by providing a working reference implementation

- Share DM requirements with the ITCAS vendor during DcP3 requirements verification (June 2026) as input to their DM module design

- Establish clear governance: DM owned by the STX/Data Unit; ITCAS DM owned by the vendor; joint review at quarterly steering committee

- Ensure all DM design decisions prioritise ITCAS portability (BPMN 2.0, standard APIs, no proprietary extensions)

- Document lessons learned from DM operation as formal input to ITCAS DM acceptance criteria

## 11.5 FTE Liberation Targets and Tracking

### 11.5.1 Aggregate FTE Target

The Debt Management module targets **~8–10 FTE liberation by December 2026** (end of Phase 2), contributing to the broader MTCA 50 FTE liberation programme. An additional ~1–2 FTE is targeted from Phase 3 (Q1 2027), bringing the total DM contribution to ~9–12 FTE.

### 11.5.2 FTE Liberation Breakdown by Automation Area

| **ID** | **Automation Area** | **Use Case** | **FTE Impact** | **Automation Rate** | **Phase** |  |
| --- | --- | --- | --- | --- | --- | --- |
| UC.DM.001 | Automated debt case identification and tracking | UC-DM-01, UC-DM-02 | ~2.7 FTE | 90% | 2 |  |
| UC.DM.002 | Automated demand notice generation | UC-DM-06, UC-DM-07 | ~1.9 FTE | 95% | 2 |  |
| UC.DM.003 | Instalment agreement processing | UC-DM-08, UC-DM-09 | ~1.4 FTE | 70% | 2 |  |
| UC.DM.004 | Risk-based case prioritisation | UC-DM-03, UC-DM-04 | ~1.3 FTE | 85% | 2 |  |
| UC.DM.005 | Automated reporting and dashboards | UC-RPT-01 to UC-RPT-03, UC-RPT-05 | ~1.8 FTE | 90% | 2 |  |
| UC.DM.007 | Enforcement action tracking with escalation | UC-DM-10 to UC-DM-13 | ~0.9 FTE | 60% | 2 |  |
| — | Asset identification and bulk processing | UC-DM-15, UC-DM-18 | ~1–2 FTE | 70% | 3 |  |
|  | **Total** |  | **~9–12 FTE** |  |  |  |

### 11.5.3 Quarterly FTE Liberation Checkpoints

| **Checkpoint** | **Target** | **Cumulative** | **Measurement Method** | **Gate** |  |
| --- | --- | --- | --- | --- | --- |
| Q3 2026 (Sep) | ~4–5 FTE | ~4–5 FTE | Automated case creation + demand notice generation operational; time tracking comparison | Phase 2 mid-point review |  |
| Q4 2026 (Dec) | ~4–5 FTE | ~8–10 FTE | Full DM workflow automation; pre/post automation time comparison per UC.DM use case | Phase 2 completion gate |  |
| Q1 2027 (Mar) | ~1–2 FTE | ~9–12 FTE | External integration automation + bulk processing; enforcement action time reduction | Phase 3 completion gate |  |

### 11.5.4 Measurement Methodology

FTE liberation is measured through three complementary mechanisms, aligned with PRM requirements:

- **Automation rate tracking** (PRM.016): percentage of cases processed without manual intervention, measured per automation area. The Case Management Platform records manual vs. automated processing for each case, enabling direct before/after comparison.

- **Task completion time measurement** (PRM.017): average time per task type before and after automation. Baseline measurements captured during UAT using current manual process timings; post-deployment measurements captured from platform audit logs.

- **Freed capacity reporting** (PRM.018): aggregated FTE liberation metrics reported monthly to senior management, aligned with the 50 FTE liberation programme tracking dashboard. Reports show both DM-specific liberation and contribution to the overall programme target.

- **Integration dashboard** (PRM.019): the Case Management Platform integrates with the Resource Liberation tracking dashboard, providing real-time visibility into DM automation performance alongside other liberation initiatives (ORS Priority Reports, PowerBuilder migration, process simplification).

### 11.5.5 Arrears Management KPIs

The DM module must produce data supporting all 12 Arrears Management KPIs (Items 24–35 from the MTCA KPI framework). These KPIs are the primary performance measures for debt management operations:

| **KPI #** | **Measure** | **Data Source** | **Current Availability** |  |
| --- | --- | --- | --- | --- |
| 24 | Annual new tax debt as % of total revenue collections | ORS/ClickHouse + DM case data | Not Yet |  |
| 25 | Total tax collectable debt as % of total revenue collections | ORS/ClickHouse + DM case data | Not Yet |  |
| 26 | % of tax debt that is under instalment | DM instalment agreement records | Not Yet |  |
| 27 | % of debt that is under 6 months of age | DM case created_date + debt age calculation | Not Yet |  |
| 28 | % of collectable debt that is under 2 years of age | DM case debt age analysis | Not Yet |  |
| 29 | % of taxpayers complying with agreed instalment arrangements | DM compliance_status tracking | Not Yet |  |
| 30 | % of taxpayers not complying with agreed instalment arrangements | DM compliance_status tracking (inverse of KPI #29) | Not Yet |  |
| 31 | Value of demand notes issued | DM demand notice records (INT-DOC-01) | Not Yet |  |
| 32 | Value of subsequent agreements made | DM instalment agreement total_amount | Not Yet |  |
| 33 | Revenue collected from demand notes and subsequent agreements | ORS/ClickHouse payment data linked to DM cases | Not Yet |  |
| 34 | % of instalment non-compliance requiring garnishment or stronger measures | DM enforcement escalation records | Not Yet |  |
| 35 | % of new debt resolved within six months | DM case lifecycle duration analysis | Not Yet |  |

All 12 KPIs are currently classified as "Not Yet" available. The DM module, combined with ORS/ClickHouse data, will be the primary enabler for making these KPIs operational. Gold-layer ClickHouse views for KPI computation will be delivered in Phase 4 (Q2 2027), but the underlying data capture begins in Phase 2.

### 11.5.6 Reallocation Strategy

Freed capacity from DM automation will be redirected to:

- **ITCAS testing and change management:** staff freed from manual debt management processes will participate in ITCAS User Acceptance Testing and training

- **Higher-value enforcement activities:** officers freed from routine case administration will focus on complex enforcement cases (C4/C5 categories exceeding €1,000)

- **Quality improvement:** additional capacity allocated to manual verification of automated decisions during the supervisor override period, building trust in the system

- **New structural positions:** supporting MTCA's broader organisational transformation as defined in the 50 FTE liberation programme

## 11.6 External Dependencies Summary

The following external systems and services are dependencies for DM implementation:

| **Dependency** | **Owner** | **DM Phase** | **Criticality** | **Fallback** |  |
| --- | --- | --- | --- | --- | --- |
| ORS/ClickHouse | Data Unit / STX | All | Critical | No fallback — DM cannot operate without ORS data |  |
| SAS VIYA | MTCA Analytics | 2+ | High | Cached risk scores with 24-hour TTL |  |
| MITA SSO/LDAP | MITA | All | High | Local authentication as temporary fallback |  |
| Notification gateway (email/SMS) | MTCA IT | 2+ | Medium | Manual notification with audit log |  |
| Document generation service | MTCA IT | 2+ | Medium | Template-based PDF generation as fallback |  |
| Property register | Lands Authority | 3 | Medium | Manual lookup via Lands Authority portal |  |
| Vehicle register | Transport Malta | 3 | Medium | Manual lookup via Transport Malta portal |  |
| CRM | MTCA | 3 | Low | Complaint tracking within DM case notes |  |
| Treasury interface | Treasury | 3 | Medium | Manual reconciliation via Treasury reports |  |
| ITCAS platform | European Dynamics | 4 | Medium | DM continues operating independently until ITCAS ready |  |

# 12. Traceability Matrix

This section provides bidirectional traceability linking requirements to use cases, business rules, KPIs, CMP platform requirements, and source documents. Traceability ensures that every requirement is:

- **Implementable** — mapped to at least one use case

- **Governed** — mapped to applicable business rules

- **Measurable** — linked to KPI outcomes

- **Platform-grounded** — mapped to CMP platform capabilities

- **Justified** — traced to an authoritative source

## 12.1 DM Requirements → Use Cases

This matrix maps each functional requirement to the use case(s) that implement it.

### 12.1.1 Debt Management Functional Requirements

| **Requirement** | **Use Case(s)** | **Notes** |
| --- | --- | --- |
| DM-FR-001 | UC-DM-01 | Debt identification and aggregation |
| DM-FR-002 | UC-DM-01 | Overdue debt detection |
| DM-FR-003 | UC-DM-01 (AF-1) | Threshold-based debt categorisation |
| DM-FR-004 | UC-DM-01, UC-DM-05 | Debt aging calculation |
| DM-FR-005 | UC-DM-01, UC-DM-20 | Case creation from debt detection |
| DM-FR-006 | UC-DM-05 | Risk scoring — automatic |
| DM-FR-007 | UC-DM-05 | Risk scoring — multi-factor |
| DM-FR-008 | UC-DM-05, UC-DM-16 | Risk scoring — SAS VIYA integration |
| DM-FR-009 | UC-DM-02 (AF-2) | Notification — first reminder |
| DM-FR-010 | UC-DM-02 | Notification — escalated reminder |
| DM-FR-011 | UC-DM-02 (AF-1) | Notification — multi-channel delivery |
| DM-FR-012 | UC-DM-03 | Demand notice generation |
| DM-FR-013 | UC-DM-04 | Judicial letter generation |
| DM-FR-014 | UC-DM-02, UC-DM-03, UC-DM-04 | Delivery tracking and confirmation |
| DM-FR-015 | UC-DM-10, UC-DM-16, UC-DM-19 | Enforcement escalation — automatic |
| DM-FR-016 | UC-DM-10, UC-DM-13 | Enforcement escalation — configurable |
| DM-FR-017 | UC-DM-01 (AF-4), UC-DM-05 | Proportional enforcement matching |
| DM-FR-018 | UC-DM-01 (AF-3), UC-DM-04 (AF-1), UC-DM-10 (AF-1) | Enforcement hold conditions |
| DM-FR-019 | UC-DM-10 (AF-2), UC-DM-20 | Enforcement review/approval |
| DM-FR-020 | UC-DM-10 (AF-3), UC-DM-20 | Enforcement outcome recording |
| DM-FR-021 | UC-DM-06 | Instalment — application |
| DM-FR-022 | UC-DM-06, UC-DM-09 | Instalment — calculation |
| DM-FR-023 | UC-DM-07 | Instalment — approval workflow |
| DM-FR-024 | UC-DM-07 | Instalment — approval delegation |
| DM-FR-025 | UC-DM-06, UC-DM-07, UC-DM-08, UC-DM-09 | Instalment — terms and conditions |
| DM-FR-026 | UC-DM-08 | Instalment — payment monitoring |
| DM-FR-027 | UC-DM-09 | Instalment — default detection |
| DM-FR-028 | UC-DM-06–09, UC-DM-16 | Instalment — lifecycle management |
| DM-FR-029 | UC-DM-11 | Warrant issuance |
| DM-FR-030 | UC-DM-18 | Write-off — automatic (low-value) |
| DM-FR-031 | UC-DM-10 (AF-4) | Enforcement — bank account freezing |
| DM-FR-032 | UC-DM-10 (AF-4) | Enforcement — asset seizure |
| DM-FR-033 | UC-DM-10 (AF-4) | Enforcement — departure prohibition |
| DM-FR-034 | UC-DM-10 (AF-4) | Enforcement — business closure |
| DM-FR-035 | UC-DM-10 (AF-4) | Enforcement — prosecution referral |
| DM-FR-036 | UC-DM-10 (AF-4) | Enforcement — third-party garnishment |
| DM-FR-037 | UC-DM-15 | Collection planning — strategic |
| DM-FR-038 | UC-DM-03, UC-DM-04, UC-DM-11, UC-DM-15 | Action tracking and recording |
| DM-FR-039 | UC-DM-10, UC-DM-15, UC-DM-19 | Enforcement configuration |
| DM-FR-040 | UC-DM-03, UC-DM-11, UC-DM-19 | Document generation |
| DM-FR-041 | UC-DM-03, UC-DM-19 | Legal fees tracking |
| DM-FR-042 | UC-DM-12 | Write-off — process |
| DM-FR-043 | UC-DM-12 (AF-1) | Write-off — approval workflow |
| DM-FR-044 | UC-DM-12 (AF-3, AF-4) | Write-off — partial and reversal |
| DM-FR-045 | UC-DM-12 (AF-2) | Write-off — reporting |
| DM-FR-046 | UC-DM-12, UC-DM-18 | Write-off — reactivation on payment |
| DM-FR-047 | UC-DM-14 | Default assessment generation |
| DM-FR-048 | UC-DM-13, UC-DM-14 | Default assessment — approval |
| DM-FR-049 | UC-DM-14 | Default assessment — rules |
| DM-FR-050 | UC-DM-10, UC-DM-15, UC-DM-16, UC-DM-18, UC-DM-20 | Case lifecycle — status tracking |
| DM-FR-051 | UC-DM-02, UC-DM-08, UC-DM-13, UC-DM-20 | Case lifecycle — alerts and triggers |
| DM-FR-052 | UC-DM-15, UC-DM-16, UC-DM-18, UC-DM-20 | Case lifecycle — outcome recording |
| DM-FR-053 | UC-DM-18 | Case closure |
| DM-FR-054 | UC-DM-17 | Enforcement activity — configure types |
| DM-FR-055 | UC-DM-17 (AF-2) | Enforcement activity — track |
| DM-FR-056 | UC-DM-17 (EF-2) | Enforcement activity — status conflict |
| DM-FR-057 | UC-DM-13 (AF-2), UC-DM-18 | Debtors list management |
| DM-FR-058 | UC-DM-18 | Debtors list — public publication |

**Coverage summary:** All 58 DM-FR requirements map to at least one use case. 20 DM use cases (UC-DM-01 to UC-DM-20) collectively implement the complete DM functional scope.

### 12.1.2 Reporting, Workflow, Integration → Use Cases

| **Requirement** | **Use Case(s)** | **Notes** |
| --- | --- | --- |
| RPT-FR-001 | UC-RPT-01 | Debt management dashboard |
| RPT-FR-002 | UC-RPT-05 | Debtors list report |
| RPT-FR-003 | UC-RPT-03 | KPI monitoring dashboard |
| RPT-FR-005 | UC-RPT-02 | Collection performance dashboard |
| RPT-FR-006 | UC-RPT-03 | Multi-stream dashboard |
| RPT-FR-007 | UC-RPT-01, UC-RPT-03 | Dashboard refresh and drill-down |
| RPT-FR-008 | UC-RPT-01 | Debt aging report (variant) |
| RPT-FR-009 | UC-RPT-02 | Collection status report |
| RPT-FR-011 | UC-DM-08 | Instalment compliance report (from monitoring) |
| RPT-FR-012 | UC-DM-12 | Write-off report (from write-off data) |
| RPT-FR-013 | — | Staff productivity report (from workflow audit trail) |
| RPT-FR-014 | UC-RPT-05 | Tax debt status report (variant of debtors list) |
| RPT-FR-015 | UC-RPT-02 | Report configuration — parameters |
| RPT-FR-016 | UC-RPT-02 | Report configuration — save |
| RPT-FR-018 | UC-RPT-01–05 | Report export (Excel/CSV/PDF) |
| RPT-FR-019 | UC-RPT-01, UC-RPT-02, UC-RPT-05 | Report permissions |
| RPT-FR-020 | UC-RPT-01, UC-RPT-05 | SAS VIYA risk score display (INT-FR-005) |
| RPT-FR-021 | UC-RPT-02 | Ad-hoc query (Superset self-service) |
| WF-FR-001 | UC-WF-01 | Case lifecycle definition |
| WF-FR-002 | UC-WF-01 | State transition rules |
| WF-FR-003 | UC-WF-01 | Workflow trigger configuration |
| WF-FR-004 | Cross-cutting | Case history (all DM use cases) |
| WF-FR-005 | UC-DM-18 (AF) | Case re-opening |
| WF-FR-006 | UC-WF-02 | Work queue generation |
| WF-FR-007 | UC-WF-02 | Work queue prioritisation |
| WF-FR-008 | UC-WF-03 | Case reassignment |
| WF-FR-009 | UC-WF-02, UC-WF-03 | Workload balancing |
| WF-FR-010 | UC-WF-01 | Automated task assignment |
| WF-FR-011 | UC-WF-02 | Queue filtering and sorting |
| WF-FR-012 | UC-WF-01, UC-WF-02 | SLA escalation |
| WF-FR-013 | UC-ADM-02 | Notification template management |
| WF-FR-014 | UC-ADM-02 | Notification channel configuration |
| WF-FR-015 | Cross-cutting | Internal alerts (all notification-generating UCs) |
| WF-FR-016 | Cross-cutting | Notification log (all notification-generating UCs) |
| WF-FR-017 | UC-DM-03, UC-DM-04, UC-DM-11, UC-DM-19 | Document generation |
| WF-FR-018 | UC-DM-03, UC-DM-04, UC-DM-11, UC-DM-19 | EDS integration |
| WF-FR-019 | UC-DM-03, UC-DM-04, UC-DM-11, UC-DM-19 | Postal services integration |
| WF-FR-020 | Cross-cutting | Audit trail (all use cases) |
| INT-FR-001–018 | Various | Integration requirements mapped to architecture (Section 10) |

> **Note:** RPT-FR-004 (Revenue reconciliation report) and RPT-FR-010 (Revenue reconciliation detail) excluded as STA-only per rules #5/#27. RPT-FR-017 (Report scheduling) excluded as STA-specific. All INT-FR mapped to system interface specifications in Section 10.

### 12.1.3 NFR Coverage Summary

Non-functional requirements (NFR-001 through NFR-048 in the DM specification) are cross-cutting and apply to all use cases. They are validated through system-level testing rather than individual use case scenarios.

| **NFR Category** | **NFR IDs (DM Spec)** | **Validation Approach** |
| --- | --- | --- |
| Performance | 6.1 (6 requirements) | Load testing against specified thresholds |
| Availability | 6.2 (5 requirements) | Uptime monitoring and failover testing |
| Security | 6.3 (11 requirements) | Penetration testing, RBAC verification |
| Usability | 6.4 (7 requirements) | UAT, accessibility audit |
| Data Quality | 6.5 (7 requirements) | Reconciliation testing, audit checks |
| Auditability | 6.6 (2 requirements) | Audit log completeness verification |
| Maintainability | 6.7 (6 requirements) | Code review, configuration change testing |
| Portability | 6.8 (4 requirements) | BPMN export validation, data export testing |

## 12.2 DM Requirements → Business Rules

This matrix links functional requirements to the business rules (Section 8) that govern their behaviour.

### 12.2.1 DM Functional Requirements → Business Rules

| **Requirement Group** | **Requirements** | **Governing Business Rules** |
| --- | --- | --- |
| Debt Identification & Categorisation | DM-FR-001–005 | BR-DM-001–008 (overdue thresholds, category C1–C5, aging rules) |
| Risk Scoring | DM-FR-006–008 | BR-DM-009–012 (scoring factors, weights, SAS integration rules) |
| Notifications & Escalation | DM-FR-009–014 | BR-DM-013–020 (reminder timing, escalation triggers, delivery rules) |
| Enforcement Actions | DM-FR-015–020, DM-FR-031–036 | BR-DM-021–032 (proportional enforcement, hold conditions, action sequencing) |
| Instalment Agreements | DM-FR-021–028 | BR-DM-033–044 (eligibility, calculation, approval thresholds, default rules) |
| Warrant & Legal | DM-FR-029, DM-FR-040–041 | BR-DM-045–048 (warrant issuance criteria, legal fee calculation) |
| Write-Off | DM-FR-030, DM-FR-042–046 | BR-DM-049–056 (write-off thresholds, approval authority, reactivation) |
| Default Assessment | DM-FR-047–049 | BR-DM-057–058 (assessment calculation, estimation rules) |
| Case Lifecycle & Debtors List | DM-FR-050–058 | BR-DM-059–060 (closure criteria, publication rules) |

### 12.2.2 RPT/WF/ADM Requirements → Business Rules

| **Requirement Group** | **Requirements** | **Governing Business Rules** |
| --- | --- | --- |
| Reporting Dashboards | RPT-FR-001–009, RPT-FR-011–021 | BR-RPT-001–006 (KPI calculation, threshold alerting, date range rules), BR-RPT-007–010 (scheduling rules, access permissions, data freshness) |
| Workflow Configuration | WF-FR-001–012 | BR-WF-001–005 (state transitions, SLA definitions, escalation rules) |
| Notifications & Documents | WF-FR-013–019 | BR-WF-006–007 (template selection, channel routing) |
| Administration | WF-FR-020; NFR security reqs | BR-ADM-001–007 (RBAC, audit retention, parameter governance) |

**Coverage summary:** All 58 DM-FR requirements are governed by at least one business rule group. 84 business rules total (60 BR-DM + 10 BR-RPT + 7 BR-WF + 7 BR-ADM) provide the rule base for the DM specification.

## 12.3 DM Requirements → KPIs

This matrix maps DM requirements to the 12 Arrears Management KPIs (Items 24–35 from PDF Table 5), plus the FTE liberation target. These are the KPIs directly measured by the DM solution.

### 12.3.1 Objective 3: Transform Tax Debt Management (KPI Items 24–35)

| **KPI Item** | **KPI Description** | **Contributing Requirements** |
| --- | --- | --- |
| Item 24 | New debt as % of revenue | DM-FR-001–002, RPT-FR-001, RPT-FR-003 |
| Item 25 | Total collectable debt as % of revenue | DM-FR-057, RPT-FR-001, RPT-FR-002 |
| Item 26 | Debt under instalment arrangement | DM-FR-021–028, RPT-FR-011 |
| Item 27 | Debt < 6 months age | DM-FR-004, RPT-FR-008 |
| Item 28 | Collectable debt < 2 years age | DM-FR-004, RPT-FR-001, RPT-FR-008 |
| Item 29 | Instalment compliance rate | DM-FR-026–027, RPT-FR-011 |
| Item 30 | *(Reserved — not specified in source)* | — |
| Items 31–33 | Demand notes, agreements, revenue collected | DM-FR-012–013, DM-FR-021, RPT-FR-009 |
| Item 34 | Non-compliance requiring stronger measures | DM-FR-015, DM-FR-031–036, RPT-FR-009 |
| Item 35 | New debt resolved within 6 months | DM-FR-001–005, DM-FR-050, RPT-FR-001 |

## 12.4 DM Requirements → CMP Platform Requirements

This matrix maps DM functional requirements to the CMP (Case Management Platform) capability requirements that the platform must satisfy. This is a new traceability dimension added per rule #16 to ensure that every DM requirement has a corresponding platform capability.

| **DM Requirement Domain** | **CMP Capability Group** | **CMP Requirement IDs** | **Notes** |  |
| --- | --- | --- | --- | --- |
| Debt case creation & categorisation (DM-FR-001–005) | Case Framework | CFW.001–045 | Case entity creation, state management, categorisation |  |
| Risk scoring & SAS integration (DM-FR-006–008) | Integration, External Connectors | INT.001–020, EC.001–010 | REST API for SAS VIYA, external data retrieval |  |
| Notifications & delivery (DM-FR-009–014) | Workflow, General | WF.001–050, GN.001–010 | Notification templates, multi-channel delivery, tracking |  |
| Enforcement escalation (DM-FR-015–020) | Workflow, Business Rules | WF.001–050, BR.001–015 | FSM transitions, configurable escalation rules |  |
| Instalment management (DM-FR-021–028) | Forms, Data Management | FM.001–015, DM.001–012 | Complex forms, data persistence, calculation support |  |
| Warrant & legal docs (DM-FR-029, 040–041) | Forms, Integration | FM.001–015, INT.001–020 | Document generation, EDS integration |  |
| Write-off processing (DM-FR-030, 042–046) | Workflow, Business Rules | WF.001–050, BR.001–015 | Approval workflows, configurable thresholds |  |
| Default assessment (DM-FR-047–049) | Business Rules, Data Management | BR.001–015, DM.001–012 | Estimation rules, assessment generation |  |
| Case lifecycle (DM-FR-050–058) | Case Framework, Process Monitoring | CFW.001–045, PM.001–010 | State tracking, SLA monitoring, outcome recording |  |
| Work queues & assignment (WF-FR-006–011) | Workflow, Operational Analytics | WF.001–050, OA.001–010 | Queue generation, prioritisation, workload balancing |  |
| Reporting dashboards (RPT-FR-001–021) | Integration, Operational Analytics | INT.001–020, OA.001–010 | ORS/ClickHouse query, Apache Superset integration |  |
| Performance metrics (PRM.006–010, PRM.016–019) | Process Monitoring | PM.001–010, PRM subset | FTE tracking, DM operational metrics |  |

**Coverage summary:** All DM functional requirements map to at least one CMP capability group. The 206 platform requirements across 11 subsections provide complete coverage for the 58 DM-FR + 19 RPT-FR + 20 WF-FR + 18 INT-FR functional requirements.

## 12.5 DM Requirements → Source Documents

This matrix traces each DM requirement domain to its originating source documents.

| **Requirement Domain** | **Primary Source** | **Secondary Sources** |
| --- | --- | --- |
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
| RPT-FR-001–021 (Reporting) | PDF-§6, PDF-T5 | STX-A1 (ORS); DTP-§4 |
| WF-FR-001–020 (Workflow) | PDF-§8, VP-§6.7 | CMP Evaluation; CMP Justification |
| INT-FR-001–018 (Integration) | STX-A1 (ORS); DTP-§3 | VP-§6.6–6.8; informix_clickhouse_ingestion_architecture.md |
| NFR (Non-Functional) | Project Brief; CMP Justification | DTP-§5; RFP-§3 (general requirements) |
| CS.001–006 (Contractual Safeguards) | CMP Justification §F.7 | CMP Evaluation §5.1 |
| D1.001–006 (Day 1 Requirements) | CMP Requirements §2.2 | CMP Justification |
| PRM.006–010, PRM.016–019 (Metrics) | CMP Requirements §7 | PDF-T5 (KPIs) |

### Source Document Key

| **Ref** | **Document** |
| --- | --- |
| PDF | MTCA STA/DM KPI Requirements (Menhard, February 2026) |
| RFP | ITCAS Request for Proposals |
| VP | European Dynamics Vendor Technical Proposal |
| DTP | MTCA Digital Transformation Programme |
| CMP Justification | Case Management Platform Justification Document |
| CMP Evaluation | CMP Platform Vendor Evaluation |
| CMP Requirements | Case Management Platform Requirements Specification |
| STX-A1 | STX Appendix 1: ORS Implementation Plan |

## 12.6 Requirements → ITCAS Future Capability Mapping (DM Scope)

This matrix maps DM interim solution requirements to their target ITCAS modules for migration planning.

| **Interim Domain** | **ITCAS Target Module** | **Migration Path** | **Key Requirements** |  |
| --- | --- | --- | --- | --- |
| DM (Workflow) | ITCAS Case Management + BPM Engine | BPMN 2.0 process definitions exported from the Case Management Platform, imported to ITCAS workflow engine | DM-FR-001–058; WF-FR-001–012 |  |
| DM (Business Rules) | ITCAS Business Rules Engine | Rules exported in DMN/JSON; imported to ITCAS configurable rules engine | All BR-DM-xxx; NFR portability reqs |  |
| Reporting | ITCAS MIS Module | Dashboard definitions and KPI configurations migrated; ITCAS provides native BI layer | RPT-FR-001–021 |  |
| Analytics | ITCAS Analytics (SAS VIYA integration maintained) | SAS VIYA scoring APIs re-pointed to ITCAS data sources | DM-FR-006–008; INT-FR-005 |  |
| Administration | ITCAS User Management | RBAC definitions and audit configurations migrated | NFR security reqs; BR-ADM-001–007 |  |
| Data (ORS dependency) | ITCAS Accounting Engine | Data source switch from ORS/ClickHouse to ITCAS native data layer; ORS retained for operational reporting | INT-FR-001–003 (ORS access) |  |

> **Migration safeguard:** Contractual safeguards CS.001–006 ensure that all workflow definitions (BPMN 2.0), business rules (DMN/JSON), form definitions (JSON), and data (JSON/CSV/XML) are exportable at any time, enabling ITCAS migration without vendor dependency.

# 13. Contractual Safeguards

This section defines the contractual safeguards that protect MTCA's investment in the Case Management Platform and ensure long-term strategic flexibility. These requirements are binding on the selected vendor and constitute non-negotiable conditions for platform deployment.

The safeguards address four critical risk areas: vendor lock-in prevention, data portability, platform substitution rights, and BPMN process portability. Additionally, Day 1 platform availability requirements establish implementation prerequisites that must be satisfied before DM configuration activities begin.

## 13.1 Vendor Lock-in Prevention

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CS.001 | Must | The vendor SHALL deliver Case Management Platform capabilities using a general-purpose COTS low-code platform with proven market adoption (100+ customers). |
| CS.006 | Must | MTCA reserves the right to substitute the Case Management Platform with an alternative solution if the platform fails to meet COTS market adoption criteria. |

**Rationale:** The COTS market adoption criterion (100+ customers) ensures that the selected platform has a sustainable ecosystem of developers, integrators, support resources, and community knowledge. Platforms with limited global adoption (fewer than 5 customers) present unacceptable sustainability risks for a government administration serving 700+ staff. This safeguard directly addresses the vendor lock-in risk identified in the CMP Justification Document (§F.7).

**Verification method:** Vendor must provide verifiable evidence of customer count at contract signature and annually thereafter. Customer count below the threshold triggers the platform substitution right (CS.006).

## 13.2 Data Portability

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CS.002 | Must | The vendor SHALL provide full data export capability in standard formats (JSON, CSV, XML) at any time upon MTCA request. |

**Scope of data export:** The data portability requirement covers all data stored within the Case Management Platform, including but not limited to:

- Debt management case records (all lifecycle states)

- Instalment agreement records and payment tracking data

- Enforcement action records and outcomes

- Worklist assignments and case history

- Audit trail and activity logs

- Configuration data (parameters, thresholds, templates)

- User-generated content (notes, attachments, annotations)

**Export format requirements:**

| **Data Category** | **Primary Format** | **Alternative Format** |
| --- | --- | --- |
| Structured case data | JSON | CSV |
| Configuration parameters | JSON | XML |
| Document templates | Native + JSON metadata | — |
| Audit trail | CSV | JSON |
| Bulk data (all records) | JSON | CSV |

**SLA for data export:** Export requests must be fulfilled within 5 business days for standard exports (single entity type) and 15 business days for full platform data exports. Export must be complete (no data loss) and machine-readable (parseable without proprietary tools).

## 13.3 Platform Substitution Rights

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CS.006 | Must | MTCA reserves the right to substitute the Case Management Platform with an alternative solution if the platform fails to meet COTS market adoption criteria. |

**Trigger conditions for substitution:**

- **Market adoption failure:** Platform customer count falls below the 100-customer threshold at any annual review

- **Vendor viability concern:** Vendor ceases active development, enters insolvency, or is acquired by an entity that discontinues the platform

- **Contractual non-compliance:** Vendor fails to deliver data export (CS.002), workflow export (CS.003), form export (CS.004), or business rules export (CS.005) as specified

- **Performance failure:** Platform consistently fails to meet NFR performance thresholds (Section 6) after reasonable remediation period

**Substitution procedure:** Upon triggering, MTCA may:

- Invoke CS.002–005 to extract all data, workflows, forms, and rules

- Engage alternative vendor for platform migration

- Require incumbent vendor cooperation during transition period (minimum 6 months)

- Retain all exported artefacts without licensing restrictions

**Integration with ITCAS migration:** The platform substitution right also serves as a safety net for the ITCAS migration path. If the Case Management Platform is retired in favour of ITCAS (planned H2 2028), the same export mechanisms (CS.002–005) support the migration.

## 13.4 BPMN Portability Guarantee

| **ID** | **Priority** | **Requirement** |
| --- | --- | --- |
| CS.003 | Must | The vendor SHALL provide workflow definitions exportable in BPMN 2.0 XML format. |
| CS.004 | Must | The vendor SHALL provide form definitions exportable in JSON or equivalent portable format. |
| CS.005 | Must | The vendor SHALL provide business rules exportable in documented format enabling reimplementation. |

**BPMN 2.0 compliance requirements:**

The DM solution implements 4 primary BPMN process definitions (debt case lifecycle, instalment management, enforcement escalation, write-off processing) and approximately 20 supporting sub-processes. All process definitions must be:

- **Exportable** in valid BPMN 2.0 XML that passes schema validation

- **Importable** into any BPMN 2.0-compliant engine (tested against at least 2 alternative engines)

- **Complete** including all task definitions, gateway conditions, event definitions, and data associations

- **Documented** with a mapping between platform-specific extensions and standard BPMN 2.0 elements

**Business rules portability:**

All 84 business rules (60 BR-DM + 10 BR-RPT + 7 BR-WF + 7 BR-ADM) must be exportable in a documented format. Acceptable formats include:

| **Format** | **Preference** | **Notes** |
| --- | --- | --- |
| DMN (Decision Model and Notation) | Preferred | OMG standard; directly importable into rule engines |
| JSON rule definitions | Acceptable | Must include complete rule logic, conditions, and actions |
| Documented pseudocode | Minimum | Must be sufficient for reimplementation without original platform |

**Form definition portability:**

All 12 DM screens, 4 reporting screens, and 6 WF/ADM screens (22 total) must be exportable in JSON format including:

- Field definitions (type, validation, dependencies)

- Layout structure

- Data bindings

- Conditional logic (show/hide rules)

## 13.5 Day 1 Platform Availability Requirements

Day 1 requirements establish implementation prerequisites that must be satisfied before DM configuration activities can begin. These requirements are derived from the CMP Requirements specification and are time-critical for the Phase 2 delivery timeline (Q3–Q4 2026).

### 13.5.1 Day 1 Requirements

| **ID** | **Priority** | **Requirement** | **Timeline** |  |
| --- | --- | --- | --- | --- |
| D1.001 | Must | The platform environment SHALL be accessible to authorised MTCA users within 4 weeks of T0. | T0 + 4 weeks |  |
| D1.002 | Must | The platform SHALL include operational Workflow Designer, Form Builder, and Business Rule Engine components from Day 1. | T0 |  |
| D1.003 | Must | The platform SHALL provide development, test, and staging environments from Day 1. | T0 |  |
| D1.004 | Must | The platform SHALL include initial documentation enabling MTCA staff to begin configuration activities. | T0 |  |
| D1.005 | Must | The platform SHALL include 5+ pre-configured workflow templates for common case management patterns. | T0 |  |
| D1.006 | Should | The platform SHOULD include a sandbox environment for experimentation without affecting development/test environments. | T0 |  |

> **Note:** T0 is the ITCAS contract commencement date (target: March 1, 2026). Day 1 platform availability is a prerequisite for Phase 1 (Q2 2026) platform provisioning and evaluation activities, which in turn gate Phase 2 (Q3–Q4 2026) DM implementation.

### 13.5.2 Day 1 Acceptance Test Scenarios

The following acceptance tests validate Day 1 readiness. All scenarios must pass before DM configuration activities commence.

| **Scenario** | **Test Description** | **Pass Criteria** |
| --- | --- | --- |
| DAY1-01 | Administrator login and navigation | Successful login and access to all configuration tools |
| DAY1-02 | Workflow designer accessibility | Create, save, and retrieve simple workflow |
| DAY1-03 | Form builder functionality | Create form with 5+ field types, save, and preview |
| DAY1-04 | Business rule engine operation | Create IF-THEN rule and execute test case |
| DAY1-05 | API documentation access | Access OpenAPI documentation for all endpoints |

### 13.5.3 Dependencies on Day 1 Requirements

| **DM Phase** | **Day 1 Dependency** | **Impact if Not Met** |
| --- | --- | --- |
| Phase 1 (Q2 2026) — Platform provisioning | D1.001–D1.006 | Phase 1 cannot start; cascading delay to Phase 2 |
| Phase 2 (Q3–Q4 2026) — DM implementation | All Day 1 tests passed | DM workflow configuration blocked; 8–10 FTE liberation at risk |
| Phase 3 (Q1 2027) — Extensions | Platform stable from Phase 2 | External integration work delayed |

## 13.6 Safeguard Summary Matrix

| **Safeguard** | **CS/D1 IDs** | **Risk Addressed** | **Verification Frequency** |  |
| --- | --- | --- | --- | --- |
| COTS market adoption | CS.001, CS.006 | Vendor lock-in, platform sustainability | Annual vendor review |  |
| Data portability | CS.002 | Data lock-in, migration risk | On-demand; tested quarterly |  |
| Workflow portability | CS.003 | Process lock-in, ITCAS migration | BPMN export tested bi-annually |  |
| Form portability | CS.004 | UI lock-in, platform substitution | JSON export tested bi-annually |  |
| Rules portability | CS.005 | Logic lock-in, reimplementation risk | Export tested bi-annually |  |
| Platform substitution | CS.006 | Comprehensive lock-in mitigation | Triggered by CS.001 failure |  |
| Day 1 availability | D1.001–006 | Implementation timeline risk | One-time at T0 |  |

# 14. Glossary

## 14.1 Domain-Specific Terms

| **Term** | **Definition** |
| --- | --- |
| Assessment | The determination of a taxpayer's tax liability for a specific tax type and period, either by self-assessment (taxpayer-filed return) or by authority assessment (MTCA-determined). Referenced by DM-FR-047–049 (default assessment). |
| C1–C5 Debt Categories | Five-tier debt classification based on amount thresholds: C1 (<€30, auto write-off), C2 (€30–€100, minimal collection), C3 (€100–€1,000, standard collection), C4 (€1,000–€20,000, intensive collection), C5 (>€20,000, strategic enforcement). Thresholds subject to MTCA confirmation. |
| Case | A managed unit of work in the debt management workflow, representing a taxpayer's debt situation requiring action. Cases progress through defined lifecycle states (New → Open → In-Progress → On-Hold → Pending Closure → Closed). |
| Case Management Platform | The selected low-code/no-code COTS platform used to implement DM workflow, forms, business rules, and case management functionality. Must satisfy contractual safeguards CS.001–006 including COTS market adoption (100+ customers) and full data/process portability. |
| Collectable Debt | Outstanding tax debt that has not been written off and for which there is reasonable expectation of recovery. Excludes statute-barred debt and amounts under legal dispute. |
| Collection Strategy | A configurable set of rules determining the enforcement approach for a given debt profile, based on category (C1–C5), risk score, debt age, and taxpayer characteristics. |
| COTS | Commercial Off-The-Shelf software — a pre-built software product available for purchase and use, as opposed to bespoke or custom-developed software. In this specification, the Case Management Platform must be a COTS product per CS.001. |
| Debt Aging | The classification of outstanding debt by the number of days since the payment became overdue. Used for reporting, prioritisation, and KPI calculation. |
| Default Assessment | A tax assessment generated by the system when a taxpayer fails to file a required tax return, typically based on estimated liability using available data and configurable estimation rules. |
| Demand Notice | A formal written communication to a taxpayer notifying them of outstanding tax debt and requiring payment within a specified period. Precedes judicial letter in the escalation sequence. |
| Enforcement Action | A formal collection measure applied to recover outstanding tax debt, ranging from demand notices through bank account freezing, asset seizure, departure prohibition, business closure, and prosecution referral. |
| Escalation | The progression of debt collection activities from lower-intensity measures (reminders) to higher-intensity measures (enforcement actions) based on elapsed time, debtor response, and configurable rules. |
| FSM | Fiscal Services Model — the operational framework governing MTCA's revenue collection and taxpayer accounting processes. Also used as abbreviation for Finite State Machine in workflow context. |
| Instalment Agreement | A formal arrangement between MTCA and a taxpayer allowing payment of outstanding debt in scheduled instalments over a defined period, subject to terms and conditions including interest accrual. |
| Judicial Letter | A formal legal document issued after an unresolved demand notice, constituting a legal claim for outstanding tax debt. Escalation from judicial letter leads to warrant issuance. |
| KPI (Key Performance Indicator) | A quantifiable measure of operational performance. This specification references 12 Arrears Management KPIs (Items 24–35 from PDF Table 5). |
| ORS (Operational Reporting System) | The ClickHouse-based data platform that replicates and consolidates data from MTCA's nine legacy Informix databases. The DM solution reads taxpayer balance and transaction data from ORS as its primary data source. |
| Overdue Debt | A tax liability where the payment due date has passed without full payment being received. The threshold for initiating collection actions is typically calculated from the statutory due date. |
| Proportional Enforcement | The principle that the severity and cost of enforcement actions should be proportional to the size, age, and risk profile of the outstanding debt. Prevents applying expensive enforcement measures to small debts. |
| Risk Score | A numeric score assigned to a debtor or debt case indicating the likelihood and severity of non-payment, calculated using multi-factor models integrating debt amount, age, taxpayer history, compliance patterns, and SAS VIYA predictive analytics. |
| STA View (in ORS) | The read-only taxpayer balance and transaction data accessible through the ORS/ClickHouse data layer. Provides the DM solution with consolidated taxpayer financial data without requiring direct STA system interaction. |
| Warrant | A legal instrument issued after an unresolved judicial letter, authorising specific enforcement actions against a debtor's assets. Warrants may be iterative (multiple warrants for escalating actions). |
| Work Queue | A prioritised list of debt management cases assigned to individual officers or teams, generated automatically based on workflow rules, risk scores, SLA deadlines, and workload balancing. |
| Write-Off | The removal of an outstanding debt from active collection, either automatically (for amounts below the C1 threshold) or through an approval workflow (for larger amounts). Written-off debts may be reactivated if subsequent payment is received. |

## 14.2 Acronyms

| **Acronym** | **Expansion** |
| --- | --- |
| ADG | Automated Document Generation |
| API | Application Programming Interface |
| BPMN | Business Process Model and Notation (standard version 2.0) |
| BR | Business Rule (requirement prefix) |
| C1–C5 | Debt Categories 1 through 5 (by amount threshold) |
| CDC | Change Data Capture (data synchronisation method) |
| CFW | Case Framework (CMP capability group) |
| CIT | Corporate Income Tax |
| CM | Case Management |
| CMP | Case Management Platform |
| COTS | Commercial Off-The-Shelf (software) |
| CS | Contractual Safeguard (requirement prefix) |
| CSV | Comma-Separated Values (data format) |
| D1 | Day 1 (requirement prefix for platform availability) |
| DM | Debt Management |
| DMN | Decision Model and Notation (OMG standard) |
| DMO | Debt Management Officer (operational role) |
| DTP | Digital Transformation Programme |
| EC | External Connectors (CMP capability group) |
| EDS | Electronic Document System |
| ETL | Extract, Transform, Load (data integration process) |
| FM | Forms Management (CMP capability group) |
| FR | Functional Requirement |
| FSM | Fiscal Services Model / Finite State Machine |
| FTE | Full-Time Equivalent (staff capacity measure) |
| GN | General (CMP capability group) |
| INT | Integration (requirement prefix / CMP capability group) |
| ITCAS | Integrated Tax and Customs Administration System |
| JSON | JavaScript Object Notation (data format) |
| KPI | Key Performance Indicator |
| MAGNET | Malta Government Network |
| MITA | Malta Information Technology Agency |
| MoSCoW | Must/Should/Could/Won't (prioritisation method) |
| MTCA | Malta Tax and Customs Administration |
| NFR | Non-Functional Requirement |
| OA | Operational Analytics (CMP capability group) |
| ORS | Operational Reporting System (ClickHouse-based) |
| PIT | Personal Income Tax |
| PM | Process Monitoring (CMP capability group) |
| PRM | Performance Metrics (CMP requirement prefix) |
| RBAC | Role-Based Access Control |
| REST | Representational State Transfer (API architecture) |
| RFP | Request for Proposals |
| RPT | Reporting (requirement and use case prefix) |
| SAS | Statistical Analysis System (analytics platform, now SAS VIYA) |
| SDO | Senior Debt Officer (supervisory role) |
| SLA | Service Level Agreement |
| SSO | Single Sign-On |
| STA | Single Taxpayer Account |
| STX | Short-Term Expert (IMF advisory role) |
| TIN | Taxpayer Identification Number |
| UC | Use Case |
| VAT | Value Added Tax |
| VP | Vendor Proposal |
| WF | Workflow (requirement prefix, use case prefix, CMP capability group) |
| XML | Extensible Markup Language (data format) |

# 15. Appendices

## Appendix A: Requirement Priority Summary (DM Scope)

### A.1 Functional Requirements by Domain and Priority

| **Domain** | **Must** | **Should** | **Could** | **Total** |  |
| --- | --- | --- | --- | --- | --- |
| DM (DM-FR) | 42 | 13 | 3 | 58 |  |
| Reporting (RPT-FR) | 13 | 5 | 1 | 19 |  |
| Workflow (WF-FR) | 14 | 5 | 1 | 20 |  |
| Integration (INT-FR) | 13 | 4 | 1 | 18 |  |
| **Functional Total** | **82** | **27** | **6** | **115** |  |

> **Note:** STA-FR requirements (45 total in source document) are excluded from this DM specification per rule #21. RPT-FR-004 (Revenue reconciliation report) and RPT-FR-010 (Revenue reconciliation detail) excluded as STA-only per rules #5/#27.

### A.2 Non-Functional Requirements by Priority

| **Category** | **Must** | **Should** | **Total** |  |
| --- | --- | --- | --- | --- |
| Performance (6.1) | 5 | 1 | 6 |  |
| Availability (6.2) | 4 | 1 | 5 |  |
| Security (6.3) | 9 | 2 | 11 |  |
| Usability (6.4) | 5 | 2 | 7 |  |
| Data Quality (6.5) | 6 | 1 | 7 |  |
| Auditability (6.6) | 2 | 0 | 2 |  |
| Maintainability (6.7) | 5 | 1 | 6 |  |
| Portability (6.8) | 4 | 0 | 4 |  |
| **NFR Total** | **40** | **8** | **48** |  |

> **Note:** NFR count (48) reflects merged requirements from Source 1 and CMP source (19 S1-only + 17 CMP-only + 12 merged). See Section 6 for merge decisions.

### A.3 CMP Platform Capability Requirements by Priority

| **Capability Group** | **Must** | **Should** | **Total** |  |
| --- | --- | --- | --- | --- |
| Case Framework (CFW) | 45 | 0 | 45 |  |
| Workflow (WF) | 50 | 0 | 50 |  |
| Business Rules (BR) | 15 | 0 | 15 |  |
| Forms Management (FM) | 15 | 0 | 15 |  |
| Data Management (DM) | 12 | 0 | 12 |  |
| External Connectors (EC) | 10 | 0 | 10 |  |
| Process Monitoring (PM) | 10 | 0 | 10 |  |
| Integration (INT) | 20 | 0 | 20 |  |
| Operational Analytics (OA) | 10 | 0 | 10 |  |
| General (GN) | 10 | 0 | 10 |  |
| Performance Metrics (PRM subset) | 1 | 8 | 9 |  |
| **Platform Total** | **198** | **8** | **206** |  |

> **Note:** PRM filtered to PRM.006–010 (DM operational metrics) and PRM.016–019 (FTE tracking) per rule #29.

### A.4 Contractual and Day 1 Requirements

| **Category** | **Must** | **Should** | **Total** |  |
| --- | --- | --- | --- | --- |
| Contractual Safeguards (CS) | 6 | 0 | 6 |  |
| Day 1 Requirements (D1) | 5 | 1 | 6 |  |
| **Safeguard Total** | **11** | **1** | **12** |  |

### A.5 Grand Totals (DM Specification)

| **Category** | **Must** | **Should** | **Could** | **Total** |  |
| --- | --- | --- | --- | --- | --- |
| Functional Requirements | 82 | 27 | 6 | 115 |  |
| Non-Functional Requirements | 40 | 8 | 0 | 48 |  |
| CMP Platform Requirements | 198 | 8 | 0 | 206 |  |
| Contractual/Day 1 | 11 | 1 | 0 | 12 |  |
| **Grand Total** | **331** | **44** | **6** | **381** |  |

### A.6 Supplementary Artefact Counts (DM Scope)

| **Artefact Type** | **Count** | **Notes** |
| --- | --- | --- |
| Use Cases | 30 (20 DM + 4 RPT + 3 WF + 3 ADM) | UC-RPT-04 excluded (STA-only) |
| Business Rules | 84 (60 BR-DM + 10 BR-RPT + 7 BR-WF + 7 BR-ADM) | BR-STA-xxx excluded (40 rules) |
| Internal Data Entities | 10 | DebtCase, InstalmentAgreement, InstalmentPayment, RecoveryAction, DemandNotice, WriteOff, Worklist, Agent, Document, AuditLog |
| External Data Entities (ORS) | 5 | Taxpayer, TaxAccount, Transaction, Payment, Assessment (read-only) |
| Screen Specifications | 22 (12 DM + 4 RPT + 6 WF/ADM) | 10 STA screens excluded |
| BPMN Process Definitions | 4 primary + ~20 sub-processes | Debt case lifecycle, instalment management, enforcement escalation, write-off processing |
| System Interface Groups | 6 (INT-CH, INT-SAS, INT-NOT, INT-DOC, INT-EXT, INT-ITCAS) | 35 interface specifications total |
| Implementation Phases | 3 active (Phase 2–4) | Phase 1 prerequisite only |
| Identified Risks | 6 | R1 Critical, R2/R5 High, R3/R4/R6 Medium |

## Appendix B: Open Questions and Assumptions (DM-Relevant)

The following open questions were identified during requirements analysis. They are filtered for DM relevance — STA-only questions are excluded. Each question requires resolution before the affected requirements can be finalised for implementation.

### B.1 Unspecified Numeric Targets

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-01 | KPI target values in Table 5 are marked "set target" without numeric values. | RPT-FR-003, RPT-FR-006, all KPI-linked requirements | **Open** | MTCA management to define baseline and target values for all 12 Arrears KPIs (Items 24–35). |  |
| OQ-02 | Debt category thresholds (€30, €100, €1,000, €20,000, €200,000) appear illustrative. Are these final? | DM-FR-003, BR-DM-001–008 | **Open** | MTCA to confirm whether thresholds are final policy values or placeholders. |  |
| OQ-03 | "nn thousand" for largest debts requiring immediate action — value not specified. | DM-FR-037, BR-DM-008 | **Open** | MTCA to define the C5 sub-threshold for immediate strategic action. |  |
| OQ-04 | Statutory period for collection before write-off ("n years") not specified. | DM-FR-042, BR-DM-049 | **Open** | MTCA legal team to confirm statutory limitation period. |  |

### B.2 Legal Dependencies

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-05 | Automated write-off of <€30 debts "may require specific legislative authority." What is the status? | DM-FR-030, BR-DM-049 | **Open** | Legal team to confirm whether enabling legislation is in place or planned. |  |
| OQ-07 | New Law on Tax Procedure (Stage 4, T+26 weeks) — timeline marked with "?" suggesting uncertainty. | Multiple DM requirements dependent on enforcement powers | **Open** | Legislative affairs to provide updated timeline for tax procedure law. |  |
| OQ-08 | Objection disallowed for self-assessed taxes when no return received — requires confirmation. | DM-FR-047–049 | **Open** | Legal team to confirm against current Maltese legal framework. |  |

> **Note:** OQ-06 (cross-tax-type offsetting) excluded as STA-specific.

### B.3 Data Availability

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-09 | Most KPI data classified as "Not Yet" available. Which data elements are currently missing from ORS? | RPT-FR-001–021, INT-FR-001–003 | **Partially Addressed** | ORS implementation is progressively covering all nine databases. Gap analysis needed for specific KPI data elements. |  |
| OQ-10 | ORS population timeline — is the phased database onboarding realistic for Phase 2 prerequisites? | INT-FR-001–003; ORS readiness gate G1 | **Open** | MTCA Data Unit / MITA to provide updated ORS readiness timeline aligned with DM Phase 2 (Q3 2026). |  |
| OQ-11 | NACE classification — consistently applied across all taxpayer records? | DM-FR-006–008 (risk scoring uses economic sector) | **Open** | Data quality assessment needed for NACE code completeness in ORS. |  |

### B.4 Architecture and Integration

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-12 | Integration approach between DM Case Management Platform and ITCAS not yet agreed. | INT-FR-010–011, NFR portability requirements | **Open** | MTCA/European Dynamics governance discussion needed. Critical for ITCAS migration planning. |  |

> **Note:** OQ-13, OQ-14, OQ-15 from source document are resolved or superseded: platform decisions (Case Management Platform selected, ORS/ClickHouse for data layer) have been made per CMP Evaluation and CMP Justification documents.

### B.5 Process Gaps

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-16 | Pre-enforcement activities for authority-assessed taxes (CIT assessments, customs duties)? | DM-FR-009–014 | **Open** | MTCA business team to confirm whether notification workflows differ by assessment type. |  |
| OQ-17 | Default assessment generation rules — detail beyond caution about exaggeration? | DM-FR-047–049, BR-DM-057–058 | **Open** | MTCA to provide specific estimation rules per tax type. |  |
| OQ-19 | Dispute handling — disputed amounts tracked but resolution workflow not described. | DM-FR-016 (objection to debt) | **Open** | MTCA to define whether dispute resolution is in DM scope or separate workflow. |  |

> **Note:** OQ-18 (multi-currency) and OQ-20 (TAS template variants) excluded as STA-specific.

### B.6 Document Completeness

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-21 | Chapter 8 (Conclusive Remarks) in source document marked as "preliminary" and incomplete. | General context | **Acknowledged** | Final version of source document to be provided when available. |  |
| OQ-23 | Annex 6 (MIS reporting sub system) likely contains detailed reporting specifications. | RPT-FR-001–021 | **Open** | Critical for DM reporting requirements validation. Full annex needed. |  |
| OQ-24 | PDF figure labels garbled (corrupted text from image extraction). | Process verification | **Acknowledged** | Original diagrams needed; DM spec has compensated with textual descriptions. |  |

### B.7 DM-Specific Questions Arising During Requirements Elaboration

| **ID** | **Question** | **Affected Requirements** | **Status** | **Action Required** |  |
| --- | --- | --- | --- | --- | --- |
| OQ-28 | ClickHouse materialised view refresh timing — can near-real-time be achieved for all nine databases simultaneously? | INT-FR-001–003, NFR performance requirements | **Open** | Performance testing required during ORS deployment. Phased database onboarding may be needed. |  |
| OQ-29 | Approval workflow delegation during staff absence — configurable in the Case Management Platform or requires custom development? | DM-FR-024, WF-FR-008–009 | **Open** | Platform capability validation during Phase 1 evaluation. |  |
| OQ-30 | Document generation integration — ADG vs. platform built-in forms vs. third-party template engine. | WF-FR-017–019, DM-FR-040 | **Open** | Architecture decision needed before Phase 2 implementation. |  |
| OQ-31 | SAS VIYA API availability timeline — risk scoring integration depends on SAS deployment. | DM-FR-006–008, INT-FR-005 | **Open** | SAS deployment team to confirm API availability and data format specifications. |  |

> **Note:** OQ-27 (platform form builder limitations for complex financial calculations) excluded as STA-specific concern. The DM Case Management Platform does not perform STA-level financial calculations.

### B.8 Assumptions

The following assumptions underpin the DM specification. If any assumption proves incorrect, the affected requirements may need revision.

| **ID** | **Assumption** | **Affected Scope** | **Risk if Invalid** |  |
| --- | --- | --- | --- | --- |
| ASM-01 | ORS/ClickHouse will be operational with all nine Informix databases replicated before Phase 2 (Q3 2026). | All DM requirements reading taxpayer data | DM solution has no data source; Phase 2 start delayed. |  |
| ASM-02 | The Case Management Platform will satisfy all CS.001–006 contractual safeguards, including COTS market adoption (100+ customers). | All platform-dependent requirements | Platform substitution required; significant implementation delay. |  |
| ASM-03 | MTCA will assign 8–10 FTE to the Phase 2 DM implementation team (business analysts, testers, subject matter experts). | Implementation timeline | Delivery timeline extends; parallel workstream capacity reduced. |  |
| ASM-04 | SAS VIYA will provide REST API access for risk scoring models before Phase 2 go-live. | DM-FR-006–008, INT-FR-005 | Risk scoring operates in degraded mode (rule-based only, no predictive analytics). |  |
| ASM-05 | Existing business rules (debt thresholds, escalation timing, enforcement sequencing) as documented in the source specification are accurate and current. | All BR-DM-xxx | Business rules require validation and possible update during Phase 1. |  |
| ASM-06 | The DM solution operates as an independent case management application reading from ORS, not as a module within the STA. | Entire DM architecture | Architectural redesign needed if DM is integrated into STA data layer. |  |
| ASM-07 | ITCAS contract signature (T0) occurs by March 1, 2026, enabling Day 1 platform availability requirements. | D1.001–006, Phase 1 timeline | All downstream phases shift proportionally. |  |
| ASM-08 | MTCA legal framework supports the enforcement actions specified (bank account freezing, asset seizure, departure prohibition, business closure, prosecution referral). | DM-FR-031–036 | Enforcement scope reduced; affected use cases require modification. |  |

## Appendix C: CMP Platform Requirements Cross-Reference

This appendix provides a cross-reference between the CMP capability groups (Section 5), the DM functional requirements they support, and the use cases that exercise those capabilities.

### C.1 Case Framework (CFW) — 45 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Case entity creation and lifecycle | DM-FR-001–005, DM-FR-050–053 | UC-DM-01, UC-DM-18, UC-DM-20 |
| Case categorisation and priority | DM-FR-003, DM-FR-006–008 | UC-DM-01, UC-DM-05 |
| State machine (FSM) management | DM-FR-015–020, DM-FR-050 | UC-DM-10, UC-DM-16, UC-DM-19 |
| Case history and audit trail | WF-FR-004, WF-FR-020 | Cross-cutting (all DM UCs) |
| Case search and retrieval | DM-FR-057–058 | UC-DM-13, UC-DM-18 |

### C.2 Workflow (WF) — 50 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| BPMN process definition | WF-FR-001–003 | UC-WF-01 |
| Automated task routing | WF-FR-006–007, WF-FR-010 | UC-WF-02 |
| Approval workflows | DM-FR-023–024, DM-FR-043 | UC-DM-07, UC-DM-12 |
| SLA monitoring and escalation | WF-FR-012, DM-FR-015 | UC-WF-01, UC-DM-10 |
| Notification triggers | WF-FR-013–016, DM-FR-009–014 | UC-DM-02, UC-DM-03, UC-DM-04 |
| Case reassignment and balancing | WF-FR-008–009 | UC-WF-03 |

### C.3 Business Rules (BR) — 15 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Configurable rule definitions | All BR-DM-xxx (84 rules) | Cross-cutting |
| Rule versioning and governance | NFR maintainability requirements | UC-ADM-03 |
| Decision table support | DM-FR-003 (categorisation), DM-FR-006 (scoring) | UC-DM-01, UC-DM-05 |
| Rule execution engine | DM-FR-015 (escalation), DM-FR-030 (auto write-off) | UC-DM-10, UC-DM-18 |

### C.4 Forms Management (FM) — 15 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Dynamic form builder | DM-FR-021 (instalment application), DM-FR-029 (warrant) | UC-DM-06, UC-DM-11 |
| Conditional field logic | DM-FR-039 (enforcement config), DM-FR-054 (type config) | UC-DM-19, UC-DM-17 |
| Form validation | DM-FR-022 (instalment calculation) | UC-DM-06 |
| Document generation from forms | DM-FR-040, WF-FR-017 | UC-DM-03, UC-DM-11 |

### C.5 Data Management (DM) — 12 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Data persistence and CRUD | All DM-FR (case data, instalment records, enforcement records) | Cross-cutting |
| Data import/export | CS.002 (data portability) | Migration, audit |
| Data validation rules | NFR data quality requirements | Cross-cutting |

### C.6 External Connectors (EC) — 10 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| REST API integration | INT-FR-001–003 (ORS), INT-FR-005 (SAS VIYA) | UC-DM-01, UC-DM-05 |
| External data retrieval | DM-FR-006–008 (risk scores from SAS) | UC-DM-05, UC-DM-16 |
| Third-party system connectivity | INT-FR external integrations (banking, postal, ART) | UC-DM-11, UC-DM-02 |

### C.7 Process Monitoring (PM) — 10 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Process execution tracking | DM-FR-050–052 (case lifecycle) | UC-DM-15, UC-DM-20 |
| SLA compliance monitoring | WF-FR-012 | UC-WF-01 |
| Performance dashboards | PRM.006–010 (operational metrics) | UC-RPT-03 |

### C.8 Integration (INT) — 20 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| ORS/ClickHouse connectivity | INT-FR-001–003 | Cross-cutting (data access) |
| SAS VIYA API | INT-FR-005 | UC-DM-05 (risk scoring) |
| Notification services | INT-FR-006–009 (email, SMS, portal) | UC-DM-02, UC-DM-03 |
| Document services | INT-FR-010–013 (EDS, postal) | UC-DM-03, UC-DM-11 |
| External agency integration | INT-FR-014–018 (ART, courts, banks) | UC-DM-10, UC-DM-11 |

### C.9 Operational Analytics (OA) — 10 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Work queue analytics | WF-FR-006–007, WF-FR-011 | UC-WF-02 |
| Staff productivity metrics | RPT-FR-013, PRM.016–019 | UC-RPT-03 |
| Case outcome analysis | RPT-FR-001–002 | UC-RPT-01, UC-RPT-05 |

### C.10 General (GN) — 10 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| Multi-channel notification | DM-FR-011, WF-FR-014 | UC-DM-02, UC-ADM-02 |
| Internationalisation (Maltese/English) | NFR usability requirements | Cross-cutting |
| Audit logging | WF-FR-020, NFR auditability requirements | Cross-cutting |

### C.11 Performance Metrics (PRM subset) — 9 Requirements

| **CMP Capability** | **DM Functional Requirements** | **Use Cases** |
| --- | --- | --- |
| DM operational metrics (PRM.006–010) | RPT-FR-003, RPT-FR-006 | UC-RPT-03 |
| FTE liberation tracking (PRM.016–019) | FTE target measurement | Quarterly measurement checkpoints |

## Appendix D: Source Document References

| **Ref** | **Document** | **DM-Relevant Sections** |
| --- | --- | --- |
| PDF | MTCA STA/DM KPI Requirements (Menhard, February 2026) | §4 (Debt Management), §5 (Instalments), §6 (Reporting/KPIs), §7 (Write-Off), §8 (Case Management), Table 5 (Items 24–35) |
| RFP | ITCAS Request for Proposals | §3.5.5 (Debt Management) |
| VP | European Dynamics Vendor Technical Proposal | §6.7 (Case Management), §6.8 (Debt Management), §6.8.2 (30 DM Capabilities) |
| DTP | MTCA Digital Transformation Programme | §3 (Technology Architecture), §4 (Reporting Strategy), §5 (NFR Standards) |
| CMP-J | CMP Justification Document | §F.7 (Vendor Lock-in), §5.1 (API Requirements) |
| CMP-E | CMP Platform Vendor Evaluation | §5.1 (Technical Comparison) |
| CMP-R | Case Management Platform Requirements Specification | §2.2 (Day 1), §9 (Contractual Safeguards), all capability sections |
| ORS-A | Informix-ClickHouse Ingestion Architecture | Full document (DM data access layer) |
| STX-A1 | STX Appendix 1: ORS Implementation Plan | §Consumers Layer |
| STA-SPEC | MTCA Taxpayer Accounting (Single Taxpayer Account) — Requirements Specification v1.1 (counterpart document) | STA-FR-011/021/024/033/046/048; BR-STA-009/025/040/041/044 (cross-boundary contracts) |

