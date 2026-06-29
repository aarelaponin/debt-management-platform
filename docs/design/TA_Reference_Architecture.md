# Reference Architecture for Modern Tax Administration

*Building Sustainable Digital Platforms for a Digital Age*

**International Monetary Fund**  
March 2026

---

## Table of Contents

- **PART I — VISION AND FOUNDATIONS**
  - [Chapter 1: Introduction](#chapter-1-introduction)
    - [1.1 Purpose and Scope](#11-purpose-and-scope)
    - [1.2 Target Audience](#12-target-audience)
    - [1.3 How to Use This Document](#13-how-to-use-this-document)
    - [1.4 Relationship to Key Frameworks](#14-relationship-to-key-frameworks)
  - [Chapter 2: From Digitalization to Digital Transformation](#chapter-2-from-digitalization-to-digital-transformation)
    - [2.1 Three Stages of Digital Maturity](#21-three-stages-of-digital-maturity)
    - [2.2 Why Process Automation Is No Longer Enough](#22-why-process-automation-is-no-longer-enough)
    - [2.3 Tax Administration 3.0: The Seamless Taxation Vision](#23-tax-administration-30-the-seamless-taxation-vision)
    - [2.4 Rules as Code: Closing the Legislation-to-Execution Gap](#24-rules-as-code-closing-the-legislation-to-execution-gap)
    - [2.5 The Vision: Tax as a Digital Platform](#25-the-vision-tax-as-a-digital-platform)
  - [Chapter 3: The Case for Architectural Renewal](#chapter-3-the-case-for-architectural-renewal)
    - [3.1 Why Traditional Tax Systems Fail](#31-why-traditional-tax-systems-fail)
    - [3.2 Sustainability and Complexity Reduction as Design Imperatives](#32-sustainability-and-complexity-reduction-as-design-imperatives)
    - [3.3 The Bespoke Software Trap](#33-the-bespoke-software-trap)
    - [3.4 Domain Independence: The Structural Key](#34-domain-independence-the-structural-key)
  - [Chapter 4: Architecture Principles](#chapter-4-architecture-principles)
    - [4.1 Openness and Vendor Neutrality](#41-openness-and-vendor-neutrality)
    - [4.2 Domain Autonomy and Modularity](#42-domain-autonomy-and-modularity)
    - [4.3 Interoperability and API-First Design](#43-interoperability-and-api-first-design)
    - [4.4 Sustainability and Complexity Reduction](#44-sustainability-and-complexity-reduction)
    - [4.5 Security and Privacy by Design](#45-security-and-privacy-by-design)
    - [4.6 Data Ownership and Portability](#46-data-ownership-and-portability)
    - [4.7 User-Centered Design](#47-user-centered-design)
    - [4.8 Observability and Maintainability](#48-observability-and-maintainability)
    - [4.9 Policy as Code](#49-policy-as-code)
    - [4.10 Cloud-Native and Elastic Scaling](#410-cloud-native-and-elastic-scaling)
- **PART II — FUNCTIONAL ARCHITECTURE**
  - [Chapter 5: Tax Administration Capability Map](#chapter-5-tax-administration-capability-map)
    - [5.1 Overview](#51-overview)
    - [5.2 Capability Dependencies and Workflows](#52-capability-dependencies-and-workflows)
  - [Chapter 6: Five-Domain Architecture Overview](#chapter-6-five-domain-architecture-overview)
    - [6.1 Why Five Domains](#61-why-five-domains)
    - [6.2 Domain Boundaries and Responsibilities](#62-domain-boundaries-and-responsibilities)
    - [6.3 Cross-Domain Integration and Event-Driven Architecture](#63-cross-domain-integration-and-event-driven-architecture)
- **PART III — DOMAIN ARCHITECTURE**
  - [Chapter 7: Domain 1 — Data Capture Platform](#chapter-7-domain-1-data-capture-platform)
    - [7.1 Purpose and Scope](#71-purpose-and-scope)
    - [7.2 Key Design Decisions](#72-key-design-decisions)
    - [7.3 Taxpayer Channels](#73-taxpayer-channels)
    - [7.4 E-Invoicing and Real-Time Reporting](#74-e-invoicing-and-real-time-reporting)
    - [7.5 Rules Engine and Automated Assessment](#75-rules-engine-and-automated-assessment)
    - [7.6 Unified API Layer](#76-unified-api-layer)
    - [7.7 Integration with National Infrastructure](#77-integration-with-national-infrastructure)
  - [Chapter 8: Domain 2 — Data Platform](#chapter-8-domain-2-data-platform)
    - [8.1 Purpose and Scope](#81-purpose-and-scope)
    - [8.2 Relationship to TADPRA](#82-relationship-to-tadpra)
    - [8.3 CQRS Pattern](#83-cqrs-pattern)
    - [8.4 Medallion Architecture](#84-medallion-architecture)
    - [8.5 Data Ingestion](#85-data-ingestion)
    - [8.6 Data Products](#86-data-products)
    - [8.7 Implementation Tiers (per TADPRA)](#87-implementation-tiers-per-tadpra)
    - [8.8 Data Governance](#88-data-governance)
  - [Chapter 9: Domain 3 — Risk Management System](#chapter-9-domain-3-risk-management-system)
    - [9.1 Purpose and Scope](#91-purpose-and-scope)
    - [9.2 Key Design Decisions](#92-key-design-decisions)
    - [9.3 Risk Indicator Framework](#93-risk-indicator-framework)
    - [9.4 Machine Learning Pipeline](#94-machine-learning-pipeline)
    - [9.5 Network Analysis](#95-network-analysis)
    - [9.6 Taxpayer Risk Profiles](#96-taxpayer-risk-profiles)
    - [9.7 Explainability](#97-explainability)
    - [9.8 Rules as Code](#98-rules-as-code)
  - [Chapter 10: Domain 4 — Case Management Platform](#chapter-10-domain-4-case-management-platform)
    - [10.1 Purpose and Scope](#101-purpose-and-scope)
    - [10.2 Low-Code/No-Code Approach](#102-low-codeno-code-approach)
    - [10.3 Configurable Workflows](#103-configurable-workflows)
    - [10.4 Task Allocation](#104-task-allocation)
    - [10.5 Document Management](#105-document-management)
    - [10.6 SLA/KPI Monitoring](#106-slakpi-monitoring)
    - [10.7 Why Low-Code Minimizes Bespoke](#107-why-low-code-minimizes-bespoke)
  - [Chapter 11: Domain 5 — External Integration Platform](#chapter-11-domain-5-external-integration-platform)
    - [11.1 Purpose and Scope](#111-purpose-and-scope)
    - [11.2 Key Design Decisions](#112-key-design-decisions)
    - [11.3 Domestic Integration](#113-domestic-integration)
    - [11.4 EU Integration](#114-eu-integration)
    - [11.5 International Data Exchange](#115-international-data-exchange)
    - [11.6 Third-Party Providers](#116-third-party-providers)
    - [11.7 Security](#117-security)
  - [Chapter 12: Cross-Domain Integration](#chapter-12-cross-domain-integration)
    - [12.1 Event-Driven Architecture](#121-event-driven-architecture)
    - [12.2 Integration Patterns](#122-integration-patterns)
    - [12.3 Compliance Lifecycle](#123-compliance-lifecycle)
    - [12.4 API Standards](#124-api-standards)
- **PART IV — CROSS-FUNCTIONAL REQUIREMENTS**
  - [Chapter 13: Development Standards](#chapter-13-development-standards)
    - [Version Control](#version-control)
    - [CI/CD Pipelines](#cicd-pipelines)
    - [Test Coverage Requirements](#test-coverage-requirements)
    - [OpenAPI Specifications for All APIs](#openapi-specifications-for-all-apis)
    - [Code Quality Audits](#code-quality-audits)
    - [Technology Stack](#technology-stack)
  - [Chapter 14: Deployment and Operations](#chapter-14-deployment-and-operations)
    - [Container Packaging (OCI-Compliant)](#container-packaging-oci-compliant)
    - [Kubernetes Orchestration](#kubernetes-orchestration)
    - [Multi-Environment Support](#multi-environment-support)
    - [Backup and Restore](#backup-and-restore)
    - [Deployment Documentation](#deployment-documentation)
    - [Rollback Capability](#rollback-capability)
    - [Graceful Shutdown](#graceful-shutdown)
  - [Chapter 15: Security Architecture](#chapter-15-security-architecture)
    - [Transport Security (TLS 1.3)](#transport-security-tls-13)
    - [Identity and Access Management (OAuth 2.0 / OIDC)](#identity-and-access-management-oauth-20-oidc)
    - [Data Encryption at Rest](#data-encryption-at-rest)
    - [Input/Output Sanitization](#inputoutput-sanitization)
    - [Security Event Logging](#security-event-logging)
    - [Key Rotation](#key-rotation)
    - [Vulnerability Scanning](#vulnerability-scanning)
    - [Software Bill of Materials (SBOM)](#software-bill-of-materials-sbom)
    - [Disaster Recovery](#disaster-recovery)
  - [Chapter 16: Quality and Performance](#chapter-16-quality-and-performance)
    - [HTML5/CSS3 for Web UIs](#html5css3-for-web-uis)
    - [Accessibility (WCAG 2.1 AA)](#accessibility-wcag-21-aa)
    - [Localization Support](#localization-support)
    - [OpenAPI Documentation](#openapi-documentation)
    - [Functional Test Coverage](#functional-test-coverage)
    - [Compliance Test Mock Implementations](#compliance-test-mock-implementations)
    - [SLA Targets by Domain](#sla-targets-by-domain)
- **PART V — GOVERNANCE AND IMPLEMENTATION**
  - [Chapter 17: Sourcing Strategy](#chapter-17-sourcing-strategy)
    - [17.1 Build vs Buy vs Configure Decision Framework](#171-build-vs-buy-vs-configure-decision-framework)
    - [17.2 Vendor Lock-In Prevention](#172-vendor-lock-in-prevention)
    - [17.3 Bespoke Footprint KPI](#173-bespoke-footprint-kpi)
  - [Chapter 18: Organizational Alignment](#chapter-18-organizational-alignment)
    - [Domain-Aligned Teams with Product Owners](#domain-aligned-teams-with-product-owners)
    - [Enterprise Architecture Governance Board](#enterprise-architecture-governance-board)
    - [Data Management Office (DMO)](#data-management-office-dmo)
    - [New Skills Required](#new-skills-required)
    - [Change Management for Product Mindset](#change-management-for-product-mindset)
  - [Chapter 19: Implementation Roadmap](#chapter-19-implementation-roadmap)
    - [19.1 Why Big-Bang Fails](#191-why-big-bang-fails)
    - [19.2 Phased Approach (Lower Risk)](#192-phased-approach-lower-risk)
  - [Chapter 20: EU Accession Conformance](#chapter-20-eu-accession-conformance)
    - [Required Changes for EU Conformance](#required-changes-for-eu-conformance)
    - [Conformance Checklist](#conformance-checklist)
  - [Annex A: Glossary](#annex-a-glossary)
  - [Annex B: GovStack Conformance Traceability Matrix](#annex-b-govstack-conformance-traceability-matrix)

---

---

# PART I — VISION AND FOUNDATIONS

## Chapter 1: Introduction

### 1.1 Purpose and Scope

This document defines a reference architecture for modern tax administration digital platforms. It provides architectural guidance for tax administrations undertaking digital transformation — not merely digitalization of existing processes, but a fundamental reimagining of how taxation works in a digital economy. The architecture is domain-separated, vendor-neutral, and designed for long-term sustainability.

The reference architecture serves as both a strategic roadmap and a tactical implementation guide. It is intended for use by tax administrations at any stage of digital maturity, from those building greenfield systems to those undertaking legacy modernization. The architecture is flexible enough to accommodate different national contexts, tax structures, and revenue models while maintaining core principles of interoperability, sustainability, and user-centered design.

### 1.2 Target Audience

This document is written for:

- Enterprise architects and IT leaders responsible for strategic technology decisions

- CIOs, CTOs, and IT directors of tax administrations

- IMF, World Bank, and bilateral technical advisors guiding digital transformation programs

- System integrators and software vendors building tax administration solutions

- Policy makers and government leaders shaping tax administration strategy

The document assumes familiarity with enterprise architecture concepts, software engineering principles, and cloud computing. Tax-domain specifics are explained throughout, but readers without prior tax experience may benefit from supplementary materials on modern tax administration (particularly OECD Tax Administration 3.0 principles).

### 1.3 How to Use This Document

The document is organized in five parts, each serving a different purpose:

- Part I establishes vision and principles. It explains the journey from digitalization to true digital transformation and articulates the architectural principles guiding this reference architecture.

- Part II maps tax administration functional capabilities and introduces the five-domain architecture model.

- Part III details each of the five architectural domains: design decisions, component technologies, and integration patterns.

- Part IV specifies cross-functional requirements: security, scalability, observability, and governance.

- Part V covers organizational governance, implementation roadmaps, and case studies.

Each domain chapter follows a consistent structure:

- Purpose and strategic rationale

- Design decisions and trade-offs

- Component technologies and sourcing options

- Integration points with other domains

- Deployment and operational patterns

### 1.4 Relationship to Key Frameworks

This architecture is explicitly aligned with and builds upon several authoritative frameworks:

GovStack Architecture Specification (v2.1.0) provides a foundational approach to modular digital government services. This reference architecture adopts GovStack's nine core principles and Conformance Framework Requirements (CFRs), mapping each of our domain chapters to corresponding GovStack sections. GovStack's building-block approach validates our domain separation strategy and provides interoperability guidance for integrating with other government digital platforms.

The OECD Tax Administration 3.0 vision articulates a seamless, integrated approach to taxation where compliance is embedded in taxpayers' natural business processes. This architecture operationalizes TA 3.0 by introducing the event-driven, data-at-source model described in OECD publications. We reference TA 3.0 throughout to show how architectural decisions support the OECD strategic vision.

The IMF Digital Public Financial Management (PFM) Guidelines provide a three-pillar framework for government financial management transformation. Tax administration is a critical revenue-collection pillar within this framework. This architecture aligns with IMF PFM principles on data standardization, audit trails, and integration with broader government financial systems.

The Tax Administration Digital Platform Reference Architecture (TADPRA) from developing market technical networks provides parallel guidance on data platforms for tax administration. We reference TADPRA where our approach converges or diverges, explaining architectural choices.

## Chapter 2: From Digitalization to Digital Transformation

### 2.1 Three Stages of Digital Maturity

Digital maturity in tax administration progresses through three distinct stages, each building on the previous:

Digitization is the conversion of paper-based processes to electronic form. Examples include scanning documents into databases, replacing paper filing with electronic filing portals, and replacing handwritten ledgers with digital accounting systems. Digitization is necessary but not sufficient. A digitized process is still fundamentally the same process — scanning a poor form does not make it a good form.

Digitalization is the automation of existing processes using digital technology. Examples include workflow automation that routes documents through approval chains, automated calculation of taxes based on filed returns, and integration of multiple systems to reduce manual data entry. Digitalization creates efficiency gains within existing process models. However, it does not question the underlying process design. Automating a broken process creates a fast broken process.

Digital Transformation is the reimagining of tax administration for a fundamentally digital society. Rather than automating existing processes, transformation changes the nature of the interaction. Examples include: data flowing directly from taxpayers' financial systems into the tax system without manual filing; real-time compliance feedback embedded in taxpayers' accounting software; machine-learning driven risk detection that replaces traditional audit selection; and seamless integration with cross-government services. Transformation is a change in business model, not just technology.

Most tax administrations today are at TA 2.0 — they have digitalized their existing processes. They operate Integrated Tax Administration Systems (ITAS) that automate legacy workflows. These systems provide value but hit a ceiling. A monolithic ITAS cannot easily support real-time data ingestion, machine-learning driven analytics at scale, or seamless citizen experience. This architecture addresses the limitations of TA 2.0 by enabling TA 3.0 digital transformation.

Figure 2.1: Evolution from Digitization to Digital Transformation

### 2.2 Why Process Automation Is No Longer Enough

Traditional ITAS implementations achieve efficiency gains through process automation but encounter structural limitations that automation alone cannot overcome. A monolithic system designed for periodic (e.g., annual) tax filing and processing cannot support event-driven, real-time taxation. A system designed with batch processing at its core struggles when required to provide real-time analytics. A tightly coupled system cannot evolve tax law without extensive regression testing and deployment risk.

The core limitation is architectural coupling. In TA 2.0 systems, all tax capabilities — filing, assessment, accounting, audit, collection — are tightly integrated within a single application. Changes to one capability cascade through the system. Adding a new data source requires changes to multiple subsystems. Implementing a new tax law feature requires coordinating modifications across calculation, reporting, and audit engines. Each change introduces risk and complexity.

Moreover, modern taxation cannot be achieved through legacy process automation. Real-time compliance monitoring requires the tax system to subscribe to events from taxpayers' business systems (accounting software, banking systems, point-of-sale systems) and to react in near-real-time. Machine-learning driven risk detection requires the ability to ingest, store, and analyze large volumes of granular transaction data. These capabilities require fundamentally different architectural patterns than traditional batch-oriented ITAS.

### 2.3 Tax Administration 3.0: The Seamless Taxation Vision

The OECD has articulated a vision called Tax Administration 3.0 (TA 3.0) that redefines modern taxation. In TA 3.0, tax compliance is embedded in taxpayers' natural business processes. Taxpayers use their existing accounting software, banking systems, and enterprise systems as normal. Tax information flows to the tax administration automatically, continuously, and in standardized formats. Compliance is achieved through design, not through enforcement.

In practical terms, TA 3.0 means: A business records a sale in its accounting software, and the tax information is simultaneously available to the tax administration. A taxpayer makes a payment through their bank, and the payment is matched and recorded instantly. An employer pays an employee, and the wage withholding information is recorded. A company imports goods, and customs and tax information flows through integrated channels. Compliance happens in real-time, not in annual filing cycles.

TA 3.0 requires architectural foundations that TA 2.0 systems do not provide. It requires an event-driven architecture where data flows continuously from sources to the tax system. It requires standardized data models and interfaces across different types of taxpayers and business systems. It requires the ability to process and analyze this data in real-time. It requires APIs that allow third-party systems to query tax information and receive compliance guidance. This reference architecture provides these foundations.

### 2.4 Rules as Code: Closing the Legislation-to-Execution Gap

Traditional tax systems have a critical weakness: the gap between legislation and execution. Tax laws are written in natural language (often ambiguous legal language) in tax codes. These laws are then manually interpreted by software engineers and encoded into business logic in code. This process introduces errors, inconsistencies, and interpretation drift.

Rules as Code is a discipline that closes this gap by expressing tax law in machine-consumable format (formal rules that can be executed by software). Instead of describing a tax rule in prose ("A taxpayer must report gross income from all sources"), it is expressed as executable code. The advantage is not just automation but precision: the rule, once validated, is executed identically every time.

Modern tax systems should treat tax rules as a distinct architectural artifact — a Rules Engine that is separate from business process logic. Rules are authored by tax policy experts (with validation by legal and technical teams), versioned, tested, and deployed independently of the broader system. This enables rapid implementation of tax law changes without requiring code changes to operational systems.

### 2.5 The Vision: Tax as a Digital Platform

This reference architecture does not describe a monolithic software system, but rather a digital platform. A platform is fundamentally different from a system: it enables an ecosystem of participants. Users of a platform are not just employees of the tax administration but also taxpayers, advisors, government agencies, auditors, and international partners.

In this vision, the tax administration provides:

- APIs through which business systems can declare transactions and query tax obligations

- Data services through which authorized parties can access (anonymized or aggregated) compliance and economic data

- Microservices that other government agencies can use (e.g., a withholding service for payroll processing)

- Developer tools, documentation, and sandboxes to enable third-party integration

- A rule engine that policy makers can update without involving software engineers

This platform approach creates a digital ecosystem around tax administration. Rather than every business maintaining its own interface to the tax system, businesses connect through standardized APIs. Rather than the tax administration maintaining proprietary audit tools, auditors can build specialized tools that query the platform's data services. Rather than international cooperation requiring custom point-to-point integrations, countries can exchange information through standardized platform interfaces.

## Chapter 3: The Case for Architectural Renewal

### 3.1 Why Traditional Tax Systems Fail

The typical lifecycle of a traditional Integrated Tax Administration System (ITAS) follows a predictable pattern: A new ITAS is implemented with much investment and initial enthusiasm. It works well for its first 5-10 years, achieving efficiency and improving taxpayer experience. Success leads to feature creep: new tax types are added, special provisions are introduced, tax law changes accumulate, international reporting requirements are layered on. Each change requires modifications to the core system.

As complexity grows, change becomes riskier. Modifications to one module may have unforeseen effects on others. The organization becomes fearful of making changes. The code evolves workarounds and patches rather than clean solutions. Performance degrades as the system accumulates data and becomes slower. The organization looks for workarounds — external tools, shadow systems, manual interventions — to address problems the core system cannot efficiently handle.

By year 10-15, the traditional ITAS enters a crisis state. It has become so complex that replacement is being discussed. But replacement is difficult because the system handles so many edge cases and special provisions that its replacement must be perfect. The organization invests in a multi-year replacement program, only to discover that the replacement project (expected to take 3 years) actually takes 7-10 years and costs 2-3 times the original estimate. During this transition period, the organization is essentially maintaining two systems — the legacy ITAS and the replacement — which doubles operational cost.

This 15-20 year replacement cycle is wasteful and disruptive. It is endemic to monolithic ITAS architecture. The core problem is coupling: because all capabilities are tightly integrated, cannot upgrade one component without potentially affecting all others.

### 3.2 Sustainability and Complexity Reduction as Design Imperatives

This reference architecture is organized around two overriding design imperatives: Sustainability and Complexity Reduction. These imperatives should guide every architectural decision.

Sustainability means the system improves over time rather than degrading. A sustainable system is one where adding a new feature makes the system slightly less complex than before (because the feature enables simplification of existing workarounds). A sustainable system is one where tax law changes can be implemented in weeks, not months. A sustainable system is one where replacing a component (e.g., upgrading a database system or moving to a new cloud provider) can be done without massive organizational disruption.

Complexity reduction is the path to sustainability. Complexity is measured by several metrics: the amount of bespoke code written specifically for tax logic (as opposed to using COTS or open-source components), the number of integration points between components, the time required to implement a change to tax law, the number of manual workarounds and shadow systems. Success is measured by driving these metrics downward.

These imperatives are not academic ideals but practical necessities. Countries with limited IT budgets cannot afford the 15-20 year ITAS replacement cycle. Developing market tax administrations need systems that can evolve rapidly to support new revenue initiatives or respond to changing economic conditions.

### 3.3 The Bespoke Software Trap

Every line of custom code written for a tax system is a maintenance liability. Custom code is code that must be maintained, debugged, tested, and updated by the organization itself. As the organization's staff changes, knowledge of custom code is lost. As the organization's priorities shift, the custom code becomes more difficult to justify maintaining.

The alternative to custom code is to use Commercial Off-The-Shelf (COTS) or open-source software where possible, configured to the organization's specific needs. COTS software has the advantage that a vendor maintains it, improvements are shared with other users, and the organization can avoid the burden of maintenance.

However, tax administration is complex and highly customizable. A system designed for one country's tax structure may not fit another country's structure. This temptation is to customize COTS software extensively for local needs. But when COTS software is heavily customized, it becomes almost as burdensome as custom software — you have the complexity of customization plus the complexity of vendor upgrades.

The principle to follow is: Bespoke only where genuinely necessary. If a COTS or open-source solution can be configured to meet 90% of a requirement, use it. Only develop custom code for the remaining 10% that is truly unique to your tax administration. This minimizes the long-term maintenance burden.

### 3.4 Domain Independence: The Structural Key

The most important architectural decision in this reference architecture is domain independence. Tax administration has four fundamentally different operational modes that should not be forced into a single system:

Data Capture and Filing: The processes through which taxpayer data enters the system. This is a high-volume, relatively predictable process (e.g., annual individual income tax filing). It requires infrastructure that can handle spikes (filing deadlines) and systems that ensure receipt and processing accuracy.

Analytics and Compliance Monitoring: The processes through which the tax administration analyzes taxpayer compliance and detects patterns. This requires analytical databases, machine learning, and statistical tools. It requires the ability to store and process large volumes of granular transaction data.

Risk Detection and Audit Selection: The processes through which the tax administration identifies high-risk taxpayers or transactions and selects them for audit. This requires specialized risk modeling and scoring systems.

Case Work and Enforcement: The processes through which the tax administration investigates cases, issues audit reports, assesses penalties, and collects outstanding debts. This is inherently complex and requires flexibility to handle edge cases.

Each of these operational modes has different technology requirements, different scaling patterns, different release cycles, and different sourcing options. Forcing them into a single monolithic system creates unnecessary coupling and complexity.

By establishing domain independence, the organization can:

- Choose the best technology for each domain (e.g., real-time analytics tools for the analytics domain, case management software for the case work domain)

- Scale each domain independently (e.g., adding more analytics infrastructure without affecting filing systems)

- Update and deploy changes to each domain on different schedules

- Replace a domain component without affecting others

This architecture is designed to enable this domain independence while maintaining the necessary integrations to function as a coherent system.

## Chapter 4: Architecture Principles

This chapter articulates the ten core principles guiding this reference architecture. Each principle is stated clearly, justified with rationale, and traced to corresponding GovStack Architecture Specification principles.

### 4.1 Openness and Vendor Neutrality

Principle: The architecture should not lock the organization into a single vendor or technology. Components should be replaceable, and the organization should be able to migrate between vendors without disrupting operations.

Rationale: Tax administrations operate for decades. During that time, vendor landscapes change, companies are acquired, technologies become obsolete. An architecture that depends on a single vendor's proprietary formats, APIs, or technologies creates long-term lock-in risk. When the vendor raises prices, discontinues support, or pursues a direction incompatible with the tax administration's needs, the organization is trapped. Openness means the organization is never trapped. It can migrate components incrementally to alternative vendors if needed.

Implementation: Prefer open standards (e.g., SQL databases, HTTP APIs, JSON data formats) over proprietary alternatives. Use open-source software where mature options exist. Where using commercial software, include exit clauses in licensing agreements and maintain the ability to export data in standard formats. Design APIs using open standards like OpenAPI/Swagger.

GovStack Alignment: GovStack 3.1 Openness and Vendor Neutrality.

### 4.2 Domain Autonomy and Modularity

Principle: The architecture should be organized into distinct, loosely-coupled domains. Each domain should be independently deployable and replaceable.

Rationale: Monolithic systems where all components are tightly integrated create complexity. When everything depends on everything else, changing one component requires testing and validating the entire system. Domain autonomy breaks this coupling. When domains are loosely coupled, changes to one domain have minimal impact on others. The organization can deploy updates to one domain without affecting others. As needs change, the organization can replace a domain without disrupting operations.

Implementation: Design the system with clear domain boundaries. Establish explicit contracts (APIs) between domains. Use asynchronous messaging (publish/subscribe) rather than synchronous calls where possible, to reduce real-time coupling. Avoid shared databases across domains; each domain should own its data.

GovStack Alignment: GovStack 3.2 Domain Autonomy and Modularity.

### 4.3 Interoperability and API-First Design

Principle: All interactions with the tax system should occur through well-defined APIs. The organization should prioritize APIs over user interfaces (though both should exist).

Rationale: User interfaces are specific to particular use cases and particular groups of users. APIs are general-purpose and can support multiple use cases. When the tax system provides rich APIs, third-party developers can build specialized tools on top of the platform. Taxpayers can integrate their business systems with the tax system. Auditors can build specialized analysis tools. Governments can query tax data to support other policy objectives. An API-first approach multiplies the value of the platform.

Implementation: Define comprehensive APIs for all tax capabilities. Use industry standards (REST, GraphQL, or gRPC) for API design. Version APIs carefully to maintain backward compatibility. Provide comprehensive API documentation and developer support. Maintain an API sandbox for developers to test integrations.

GovStack Alignment: GovStack 3.3 Interoperability and API-First Design.

### 4.4 Sustainability and Complexity Reduction

Principle: Every architectural decision should be evaluated for its impact on long-term sustainability. Prefer solutions that reduce complexity over solutions that are initially faster to implement.

Rationale: The cost of a system is not its implementation cost but its total cost of ownership over its lifetime. A system that is quickly implemented but creates long-term maintenance burden is actually more expensive than a system that takes longer to implement but is sustainable. Complexity reduction is the path to lower total cost of ownership and to better ability to adapt to changing needs.

Implementation: Measure complexity using concrete metrics: lines of bespoke code, number of integration points, time to implement a change to tax law. Set targets for reducing these metrics. When making architectural decisions, prefer options that reduce these metrics even if they increase initial implementation effort.

GovStack Alignment: GovStack 3.4 Sustainability and Complexity Reduction.

### 4.5 Security and Privacy by Design

Principle: Security and privacy should be built into the architecture from the start, not added as an afterthought. The system should assume zero trust and authenticate and authorize every request.

Rationale: Tax administrations hold highly sensitive information about taxpayers' financial affairs, personal information, and business details. Breaches of tax data can harm taxpayers and undermine trust in government. Security and privacy are not optional; they are fundamental requirements. Building security in from the start is more effective and less costly than retrofitting it later.

Implementation: Use zero-trust architecture principles. Require authentication and authorization for all access to data and services. Encrypt data in transit and at rest. Log all access for audit purposes. Conduct regular security assessments. Maintain separation of duties so that no single person or system has excessive access.

GovStack Alignment: GovStack 3.5 Security and Privacy by Design.

### 4.6 Data Ownership and Portability

Principle: The tax administration should own its data, not a vendor. Data should be exportable in standard formats.

Rationale: Data is the organization's core asset. If a vendor controls the data format or the only way to access the data is through the vendor's proprietary interface, the organization is dependent on that vendor. If the organization decides to migrate to a different system, it may find that exporting data is difficult or expensive. By maintaining data ownership and portability, the organization preserves its ability to migrate systems independently.

Implementation: Use standard data formats (SQL databases, XML/JSON files, standard EDI formats) rather than proprietary formats. Maintain automated exports of critical data. Include data export capabilities as a contractual requirement in vendor agreements. Design systems so that data can be migrated between systems without extensive manual work.

GovStack Alignment: GovStack 3.6 Data Ownership and Portability.

### 4.7 User-Centered Design

Principle: The system should be designed based on the needs of actual users, not on technical preferences or organizational structures.

Rationale: A system that is technologically perfect but difficult for users to use will not be used effectively. User frustration leads to workarounds, shadow systems, and data quality problems. User-centered design ensures that the system solves actual problems for actual users. Tax administrations should conduct user research, usability testing, and user feedback collection as part of the design process.

Implementation: Involve actual users in the design process (taxpayers, tax officers, auditors, taxpayer services staff). Conduct usability testing with users. Gather feedback on new features before release. Prioritize simplicity and clarity in user interfaces. Provide multiple access channels (web, mobile, API) to accommodate different user preferences.

GovStack Alignment: GovStack 3.7 User-Centered Design.

### 4.8 Observability and Maintainability

Principle: The system should be designed to be observable. Operators should have clear visibility into system behavior, performance, and health. The system should be maintainable — operators should be able to debug problems without access to source code.

Rationale: Tax systems operate continuously and must be reliable. When problems occur, they must be diagnosed and resolved quickly. If the only way to diagnose a problem is to review source code, the organization is dependent on developers. By building observability into the system, operators can monitor system health, detect problems early, and resolve problems quickly with minimal developer involvement.

Implementation: Implement comprehensive logging of system events. Collect metrics on system performance. Provide dashboards that operators can use to monitor system health. Use structured logging (e.g., JSON-formatted logs) that can be automatically analyzed. Implement alerting systems that notify operators of potential problems.

GovStack Alignment: GovStack 3.8 Observability and Maintainability.

### 4.9 Policy as Code

Principle: Tax policy should be expressed as code, separate from operational system logic. Policy should be authored by domain experts, not by software engineers.

Rationale: Tax law is complex and changes frequently. If tax law is encoded in operational system logic (e.g., embedded in assessment calculation code), then every change to tax law requires software engineers to modify code, test it, and deploy it. This is slow and error-prone. By separating policy (tax law) from operations (how the system processes data), policy experts can define and update tax law using specialized tools, without requiring software engineer involvement.

Implementation: Implement a Rules Engine that can execute machine-readable tax rules. Provide tools for policy experts to define and test rules. Maintain versioning of rules so that historical compliance can be analyzed under the rules that were in effect at the time. Separate rule definitions from rule execution infrastructure.

GovStack Alignment: GovStack 3.9 Policy as Code.

### 4.10 Cloud-Native and Elastic Scaling

Principle: The architecture should be designed for cloud-native deployment. Components should scale elastically in response to demand.

Rationale: Tax administrations experience highly variable demand. Filing seasons, year-end compliance activities, and audit cycles create demand peaks. Traditional on-premises systems must be provisioned for peak demand, which means they are underutilized most of the time. Cloud-native systems can scale elastically — adding resources when demand increases and releasing them when demand decreases. This reduces cost and enables better resource utilization.

Implementation: Design systems to be stateless where possible (so they can scale horizontally). Use containerization (Docker) and orchestration (Kubernetes) to manage deployment and scaling. Adopt managed cloud services where available (databases, storage, analytics) rather than managing infrastructure manually. Use auto-scaling policies to automatically scale systems based on demand.

GovStack Alignment: GovStack alignment with cloud-native principles.

---

# PART II — FUNCTIONAL ARCHITECTURE

## Chapter 5: Tax Administration Capability Map

### 5.1 Overview

All tax administrations, regardless of country context, revenue structure, or tax types administered, need the same core set of functional capabilities. These capabilities represent what the tax administration does, independent of how (through what systems or processes) it does them.

This chapter identifies and describes twelve core capability areas that form the functional foundation of tax administration. These capabilities should inform the design of the organizational structure, the system architecture, and the governance model. Understanding these capabilities is a prerequisite for effective architectural design.

The following table lists the twelve core capabilities. Each capability area encompasses multiple sub-capabilities that will be detailed in subsequent chapters. In the five-domain architecture model (described in Chapter 6), each capability is assigned to one or more domains based on its technical and operational characteristics.

| # | Capability Area | Description |
| --- | --- | --- |
| 1 | Registration & Identification | Processes for taxpayer registration, identity verification, classification, and maintenance of unique identifiers across all tax types. |
| 2 | Filing & Returns | Systems for filing and submission of tax returns, declarations, and supporting documents; supports multiple filing channels and formats. |
| 3 | Assessment | Tax calculation, assessment determination, and generation of assessment notices based on filed returns and third-party data. |
| 4 | Accounting/Payments/Refunds | Financial accounting of tax receipts, payment processing, ledger management, reconciliation, and refund issuance. |
| 5 | Audit & Verification | Risk-based audit selection, audit execution, evidence management, and generation of audit reports and findings. |
| 6 | Enforced Collection | Collection of outstanding liabilities through enforcement actions, garnishment, asset recovery, and third-party collection. |
| 7 | Legal Proceedings | Case management for disputes, appeals, administrative proceedings, penalties, and prosecution of tax crimes. |
| 8 | Compliance Risk Management | Analytics-driven identification of compliance risks, stratification, segmentation, and risk-scored case prioritization. |
| 9 | Taxpayer Services | Support channels including helpdesk, self-service portals, correspondence management, and proactive taxpayer guidance. |
| 10 | International Cooperation | Exchange of information with foreign tax authorities, country-by-country reporting compliance, and treaty administration. |
| 11 | Supporting Processes | Internal operations including HR, procurement, budgeting, inventory, and general administration. |
| 12 | Security Management | Authentication, authorization, audit logging, access control, and compliance with security standards and regulations. |

Table 5.1: Tax Administration Core Capability Areas

### 5.2 Capability Dependencies and Workflows

These twelve capability areas do not operate in isolation. They form an integrated system where outputs of one capability become inputs to another. Understanding these dependencies is critical for designing effective integration between domains.

The lifecycle of a typical compliance case illustrates these dependencies: A taxpayer registers (Registration & Identification), becomes obligated to file returns (Filing & Returns). The tax administration assesses the return (Assessment), records the assessed amount in accounting systems (Accounting/Payments), and monitors whether the taxpayer pays (Payments). Meanwhile, the system performs compliance risk analysis (Compliance Risk Management) to determine the taxpayer's risk profile. Based on risk scoring, the system selects the taxpayer for audit (Audit & Verification). The auditor conducts examination and generates findings (Audit & Verification). The taxpayer appeals the audit findings (Legal Proceedings). If tax is not paid, the administration issues enforcement notices (Enforced Collection). Throughout this lifecycle, the taxpayer may contact the administration (Taxpayer Services) for guidance or assistance. Supporting processes (Supporting Processes) ensure the organization can execute these activities (HR, procurement, etc.). Security Management (Security Management) ensures that data and systems are protected throughout.

## Chapter 6: Five-Domain Architecture Overview

### 6.1 Why Five Domains

The five-domain architecture model represents the core structural innovation of this reference architecture. Rather than designing a monolithic system to handle all tax administration capabilities, the architecture deliberately separates tax administration into five loosely-coupled domains. Each domain optimizes for different technical requirements, scaling patterns, deployment cycles, and sourcing approaches.

The rationale for five domains (rather than a different number) emerges from analysis of tax administration workflows and technical requirements:

Data Ingestion: All tax administration begins with data from taxpayers or third parties. This data must be collected, validated, and stored. The data ingestion domain handles high-volume data collection during filing periods and continuous collection of third-party data (banking data, customs data, employment data). It requires infrastructure optimized for write-heavy operations, consistency guarantees, and audit trails.

Compliance Analytics: Once data is in the system, the administration must analyze it to understand taxpayer compliance patterns and detect anomalies. This requires analytical capabilities (OLAP, machine learning, statistical analysis) that are fundamentally different from transactional capabilities. The compliance analytics domain optimizes for read-heavy, exploratory queries and statistical analysis.

Operational Tax Processing: The administration must apply tax rules to taxpayer data to calculate obligations, generate assessments, process payments, and produce reports. This is the core operational domain where tax law is executed. It must be consistent, auditable, and compliant with tax law.

Case and Audit Management: Beyond operational processing, the administration must manage individual compliance cases — audits, appeals, enforcement actions, disputes. This is inherently complex, involving document management, workflow management, and complex business logic. It requires different technology than high-volume transactional processing.

Integration and APIs: Finally, the tax administration must integrate with external systems (banking, customs, accounting software) and provide APIs for other government agencies and taxpayers. This requires an integration layer that can manage complex transformation, security, and reliability requirements.

These five domains emerged from analysis of technical requirements, organizational structures, and deployment patterns in successful tax administrations. The domains represent a balance between separation of concerns and practical feasibility of integration.

Figure 6.1: Five-Domain Architecture Overview

### 6.2 Domain Boundaries and Responsibilities

Each domain has distinct boundaries and responsibilities:

The Data Ingestion Domain is responsible for receipt, validation, and storage of all data entering the tax system. Sources include: taxpayer filings (returns, declarations, payments), third-party data (banking transactions, employment data, customs data), and government data (social security data, company registration data). This domain guarantees data quality, maintains audit trails, and ensures that data is correctly classified and stored for access by other domains.

The Compliance Analytics Domain receives data from the Data Ingestion Domain and performs analysis to identify compliance patterns, detect anomalies, and support risk-based compliance strategies. This domain uses advanced analytics, machine learning, and statistical techniques. It produces compliance risk scores that feed into the Operational Processing Domain for use in audit selection, examination planning, and targeted compliance activities.

The Operational Tax Processing Domain is where tax rules are applied to taxpayer data. This domain receives raw or pre-processed data from Data Ingestion, receives risk scores and guidance from Compliance Analytics, and applies tax law to calculate obligations, generate assessments, process payments, and produce required tax reports. This domain must be consistent, fast, and reliable.

The Case and Audit Management Domain manages individual compliance cases and audit workload. Cases include routine audits, complex investigations, appeals, enforcement actions, and disputes. This domain provides workflows, document management, evidence handling, and case tracking. It integrates with the Operational Processing Domain (to access taxpayer data and assessment information) and produces outputs that feed back to Operational Processing (e.g., audit findings that adjust assessments).

The Integration and APIs Domain provides the interface between the tax system and external systems. It handles integration with banking systems, customs systems, accounting software vendors, and other government agencies. It exposes tax system capabilities through APIs that other systems can use. It handles security, authentication, data transformation, and reliability requirements of integration.

### 6.3 Cross-Domain Integration and Event-Driven Architecture

While domains are loosely coupled, they are not independent. They must integrate to deliver coherent tax administration. Integration occurs through several mechanisms:

Event-Driven Communication: When significant events occur in one domain (e.g., a taxpayer files a return in the Data Ingestion Domain, or a compliance risk score is generated in Compliance Analytics), the domain publishes an event. Other domains that need to react to that event subscribe to it. For example: Data Ingestion publishes a "Return Filed" event. The Operational Processing Domain subscribes to this event and reacts by scheduling assessment. The Compliance Analytics Domain subscribes to this event and includes the filed return in its analysis. This event-driven approach decouples domains while enabling them to react to important business events.

Shared Data Access: Domains maintain their own databases (to ensure autonomy) but provide query APIs through which other domains can access necessary data. For example, Case Management queries Operational Processing for assessment details when working on an appeal case.

The Compliance Lifecycle represents the integration choreography. A taxpayer files a return → Data Ingestion receives and validates the return → Operational Processing calculates the assessment → Compliance Analytics analyzes the taxpayer and return for compliance risk → If risk exceeds threshold, the taxpayer is selected for audit → Case Management opens an audit case → The auditor investigates → The Case Management Domain may request additional data from Data Ingestion or reassess from Operational Processing if findings warrant → Finally, the assessment is finalized (possibly modified based on audit findings) → Payment is collected → The case is closed. This entire lifecycle involves all domains acting in coordination.

Figure 6.2: Compliance Lifecycle Integration

Figure 6.2 illustrates how compliance work flows through the five domains. Each domain adds value: Data Ingestion ensures data quality, Compliance Analytics identifies risk, Operational Processing calculates tax correctly, Case Management handles complex cases and audits, and Integration ensures external systems can participate. The domains are separate but coordinated through events and APIs.

This cross-domain integration enables the organization to:

- Deploy domain updates independently: If Compliance Analytics improves its risk models, that deployment does not affect Operational Processing

- Scale domains independently: If Filing volume increases, the Data Ingestion Domain can scale without affecting audit infrastructure

- Replace domains incrementally: If the organization decides to replace its Compliance Analytics with a different technology, it can do so without touching other domains

- Evolve tax law easily: Tax law changes typically affect Operational Processing (tax rules). By separating this domain, changes to tax law do not disrupt data ingestion, analytics, or case management

---

# PART III — DOMAIN ARCHITECTURE

## Chapter 7: Domain 1 — Data Capture Platform

### 7.1 Purpose and Scope

The Data Capture Platform is the taxpayer-facing domain responsible for all data acquisition at source. It serves as the primary interface through which taxpayers and their representatives interact with the tax administration system. This domain accepts returns, invoices, payments, registrations, and all other data entering the tax ecosystem. It operates under the highest volume and strictest availability requirements, as any disruption directly affects taxpayer compliance and revenue collection.

### 7.2 Key Design Decisions

The Data Capture Platform is built around several critical design principles:

- Unified API layer as the single integration point: All channels—portal, B2B API, mobile, and counter service—connect through the same underlying API. This ensures consistency, simplifies maintenance, and allows rapid addition of new channels without duplicating business logic.

- Separation of channels from business logic: Channel-specific code (portal UI, mobile app handling, B2B protocol translation) is isolated from validation and processing logic. Each channel can evolve independently, and new channels can be added by implementing the channel adapter without touching core business logic.

- Stateless request processing for horizontal scaling: Each request contains all necessary context; no session state is maintained in memory. This enables horizontal scaling: requests can be routed to any available instance without session affinity constraints. The pattern simplifies load balancing and disaster recovery.

### 7.3 Taxpayer Channels

The Data Capture Platform supports multiple channels optimized for different taxpayer segments and use cases:

- Responsive Portal: A web-based interface built on responsive design (HTML5, CSS3, JavaScript framework) optimized for individuals and small-to-medium enterprises. Features low-code/no-code components for rapid form customization to support tax law changes. Supports multi-language, accessibility (WCAG 2.1 AA), and works across desktop and mobile browsers.

- Comprehensive B2B API: Full-featured REST API with OpenAPI 3.0 documentation. SDKs provided in Java, C#, JavaScript, and Python to reduce integration friction for enterprise partners, accounting software vendors, and tax service providers. API supports both synchronous (immediate response) and asynchronous (callback-based) patterns.

- Mobile App: Native or cross-platform mobile application optimized for individual taxpayers. Streamlined for quick filing on smartphones, with offline capability for draft storage. Integrates with mobile identity platforms for seamless authentication.

- Counter Service Integration: API endpoint for walk-in taxpayer services at government offices. Allows counter staff to assist taxpayers in filing and submission using centralized business logic, ensuring consistency across all counter locations.

### 7.4 E-Invoicing and Real-Time Reporting

The Data Capture Platform incorporates advanced e-invoicing capabilities to support modern tax compliance requirements:

- Real-time invoice validation and registration: As invoices are submitted through any channel, they are immediately validated against business rules (GST registration, invoice format, amount thresholds). Valid invoices are registered in the system within seconds. This enables real-time compliance monitoring and reduces opportunities for fraud.

- Pre-funding mechanism for VAT (Ukraine-specific): Incorporates support for Ukraine's Value Added Tax pre-funding rules, allowing taxpayers to declare VAT liabilities in real time and triggering pre-funding obligations as defined in tax law. This feature is configurable for other jurisdictions with similar VAT schemes.

- Support for ViDA DRR convergence path: Designed to transition toward the EU's VAT in the Digital Age (ViDA) requirements and Digital Reporting Requirements (DRR). As EU regulations evolve, the platform can expand its e-invoicing capabilities without architectural redesign.

- EN16931 format alignment roadmap: While currently accepting formats common in the jurisdiction, the platform has a roadmap to support EN16931 (the European e-invoicing standard) as a primary exchange format. This prepares the tax administration for cross-border e-invoicing and EU integration.

### 7.5 Rules Engine and Automated Assessment

The Data Capture Platform incorporates a rules engine for validation and auto-assessment:

- Configuration-driven tax rules, not hard-coded: Tax validation rules are defined in a configuration language (not embedded in application code). This allows tax officers and compliance specialists to update rules in response to legislative changes without requiring code deployment, compilation, or a full regression test cycle.

- Self-assessment validation at filing time: As a taxpayer files a return, the rules engine validates self-assessment calculations in real time. This provides immediate feedback on errors, reducing downstream correction cases and improving taxpayer compliance.

- Automatic cross-matching with third-party data: The system automatically cross-matches filed data against third-party sources (banking data, employment records, customs data, invoices from other taxpayers). Mismatches are flagged for review by compliance officers, prioritizing high-value discrepancies.

- Pre-populated returns based on available data: Using data already held in the system (prior filings, third-party data, government data), the platform pre-populates portions of returns. Taxpayers validate and correct pre-filled information rather than entering all data from scratch. This reduces taxpayer burden and error rates.

### 7.6 Unified API Layer

The core of the Data Capture Platform is a comprehensive Unified API Layer comprising 14 microservice groups, each aligned to the tax reference data model:

- 14 Microservice Groups: Each group manages a logical domain within tax administration (taxpayer registration, returns management, payment processing, invoicing, reporting, etc.). Services are deployed and scaled independently based on demand.

- API Gateway: Sits in front of all microservices. Handles rate limiting (protecting against abuse), authentication (verifying caller identity), request routing, response transformation, and circuit breaker patterns (graceful degradation if downstream services are unavailable). The gateway maintains a consistent interface for all consumers.

- OpenAPI 3.0 Specification: All endpoints are formally documented using OpenAPI 3.0. This enables automated client SDK generation, API documentation that stays synchronized with implementation, and contract testing between consumers and providers.

- Versioning Strategy: APIs use URI-based versioning (e.g., /api/v1/, /api/v2/). Backward compatibility is guaranteed: each major version remains available for two prior versions, giving consumers time to migrate. New versions introduce breaking changes carefully, with deprecation periods and migration guides.

### 7.7 Integration with National Infrastructure

The Data Capture Platform integrates with national identity and registry systems to establish taxpayer identity and verify information:

- eID for Authentication: Authentication uses the national electronic identity (eID) credential, building on the GovStack Identity Building Block. This ensures strong authentication, supports multi-factor options, and aligns with government digital transformation initiatives. eID integration enables single sign-on across all tax platforms.

- National Registries: The platform queries national registries for verification: business registry (to verify company registration, ownership, and status), population registry (to verify individual identity and residency), property registry (to verify real estate holdings), and vehicle registry (for vehicle-related tax obligations). Registry queries are performed at taxpayer registration to establish baseline identity.

- Treasury Integration: Payment processing is integrated with the national treasury system. Payments received via any channel are immediately reported to the treasury, enabling real-time revenue reporting and reconciliation. Refunds are processed through the treasury system, ensuring proper fund flows and compliance with government accounting standards.

- Single Sign-On (SSO): Once authenticated through eID, a taxpayer can access the tax portal, request a certificate from the company registry, or access social security records without re-authenticating. This seamless cross-government experience reduces friction and improves citizen satisfaction.

## Chapter 8: Domain 2 — Data Platform

### 8.1 Purpose and Scope

The Data Platform is the analytical foundation of the tax administration system. It consolidates data from all other domains and external sources, applying data quality rules, deduplication, and standardization. This domain enables data-driven compliance management by providing a single source of truth for all organizational data. It serves data products to Risk Management and Case Management domains, ensuring that compliance decisions are based on complete, accurate information.

### 8.2 Relationship to TADPRA

This domain implements the Tax Administration Data Platform Reference Architecture (TADPRA), a GovStack standard that defines baseline design patterns, technology selection frameworks, and implementation tiers. TADPRA provides proven approaches for building scalable, maintainable data platforms in tax administrations. Our implementation follows TADPRA principles while adapting to the specific needs of this jurisdiction.

### 8.3 CQRS Pattern

The Data Platform implements the Command/Query Responsibility Segregation (CQRS) pattern to separate write operations from read operations:

- Operational Database (Write Optimized): PostgreSQL manages all transactional writes—returns being filed, payments being processed, assessments being calculated. This database is optimized for ACID transactions and real-time consistency, using row-oriented storage and transactional locks.

- Analytical Database (Read Optimized): ClickHouse or Doris manages complex analytical queries—revenue trends, compliance patterns, population stratification. This database is optimized for columnar storage, supporting massive parallel queries across billions of records. Queries typically complete in sub-10-second timeframes for pre-aggregated datasets.

- Separation Benefit: By separating operational and analytical workloads, each database can be optimized for its specific access pattern. Analytical queries do not lock operational records, and operational transactions are not delayed by long-running analytical queries.

### 8.4 Medallion Architecture

Data flows through three layers in the Medallion Architecture, each adding value:

- Bronze Layer (Raw Data): Captures data exactly as it arrives from source systems—full history, no transformation, no aggregation. Retains data quality issues and duplicates as originally received. Serves as permanent record and source of truth for reprocessing if transformation logic changes. Implemented as a data lake in object storage (S3-compatible).

- Silver Layer (Cleansed Data): Applies data quality transformations—deduplication, format normalization, standardization of business concepts, entity resolution (matching duplicate taxpayer records across filing channels). Missing values are imputed using domain logic. Results are stored in columnar format (Parquet) for efficient querying. Data lineage is tracked to enable audit trails.

- Gold Layer (Business Logic): Applies tax-specific business logic—aggregations, tax calculations, segmentation, data products for specific use cases. Results are pre-aggregated, summary tables ready for immediate consumption. Examples include: Taxpayer 360 profiles, compliance metrics by segment, revenue forecasting models, cross-matching results.

- Data Build Tool (dbt): Transformations across all three layers are implemented using dbt, a transformation framework that provides version control, testing, documentation, and lineage for SQL-based transformations. dbt ensures reproducibility and enables rapid iteration on transformation logic.

### 8.5 Data Ingestion

The Data Platform supports multiple ingestion patterns to handle diverse data sources:

- Batch Ingestion: Legacy systems and periodic reports (e.g., monthly reconciliation files from state banks) are ingested on a batch schedule. Typically runs overnight to minimize impact on production systems. Used for stable, low-frequency data.

- Change Data Capture (CDC): Captures only changed records from operational databases, rather than re-extracting entire tables. Enables near-real-time synchronization of operational data to the analytical database. Implemented using database-native CDC tools (PostgreSQL logical decoding, MySQL binlog, etc.).

- Event Streaming: Real-time events from the Data Capture Platform (e.g., "Return Filed," "Payment Received") are streamed to the Data Platform using a message broker (Kafka, RabbitMQ). Events trigger immediate processing—data is extracted from the event, validated, and loaded into the Bronze layer within milliseconds.

- API-Based Ingestion: External data providers (banks, utilities, credit bureaus, real estate registries) expose REST APIs that the Data Platform queries on a schedule or trigger basis. Responses are normalized and loaded into Bronze layer.

### 8.6 Data Products

The Data Platform produces data products that serve the organization's compliance mission:

- Taxpayer 360-Degree Profile: Comprehensive view of each taxpayer consolidating all available data—registration history, filings history, payment history, third-party data (banking, employment, property), audit history. Used by case managers and compliance analysts to understand taxpayer context.

- Compliance Metrics by Segment: Breakdowns of compliance indicators (filing rates, payment rates, reporting accuracy) by taxpayer segment (size, industry, geography). Supports resource allocation decisions and policy analysis.

- Revenue Forecasting Models: Machine learning models trained on historical filings, payments, and economic indicators to forecast future tax revenue. Used by treasury and planning teams for budgeting.

- Cross-Matching Results: Identified discrepancies between filed data and third-party data, ranked by materiality. Feeds into risk scoring and case selection.

- Industry Benchmarks: Statistical summaries of taxpayer behaviors grouped by industry—average revenue, typical deduction rates, wage expense ratios. Used to identify outlier taxpayers for audit.

- Self-Service Analytics Portal: Web-based tool allowing authorized tax officers to build ad-hoc reports and dashboards. Provides access to Gold and Silver layers with row-level security (each officer sees only their district's data or assigned cases). Enables data-driven decision making at all organizational levels.

### 8.7 Implementation Tiers (per TADPRA)

TADPRA defines three implementation tiers based on organizational capacity:

- Tier 1 MVP (< 500 staff): Minimum viable platform with Operational Reporting and Statistical functionality. Built in approximately 3 months. Focus: establish Bronze and Silver layers, enable basic reporting.

- Tier 2 Standard (500–2,500 staff): Full Data Warehouse with basic machine learning. Built over approximately 12 months. Includes Gold layer, advanced data products, and initial ML models for risk prediction.

- Tier 3 Enterprise (> 2,500 staff): Real-time everything plus advanced AI/ML. Built over 15–18 months. Includes streaming ingestion, advanced predictive models, and autonomous compliance systems.

### 8.8 Data Governance

Proper data governance ensures data quality, security, and compliance with privacy regulations:

- Data Management Office (DMO): Centralized organization with responsibility for all data governance activities. Reports to the Commissioner or Director-General. Staffed with data architects, engineers, analysts, and compliance specialists.

- Four Governance Pillars: (1) Governance—policies, standards, metadata management, data dictionary; (2) Engineering—infrastructure, platforms, tools; (3) Quality—profiling, validation, monitoring, data quality SLAs; (4) Analytics—analytics strategy, advanced models, data science.

- Chief Data Officer (CDO): Executive responsible for enterprise data strategy, reports directly to leadership. Sits on steering committees to ensure data considerations influence organizational decisions.

- Data Classification: All data is classified by sensitivity (public, internal, confidential, restricted). Classification determines storage location, encryption requirements, access controls, and retention policies.

- Data Lineage Tracking: Every dataset knows its source, transformations applied, downstream dependencies. Enables audit trails ("where did this revenue number come from?") and impact analysis ("if we change this transformation, which reports are affected?").

- Retention Policies: Data retention is defined by regulation, business need, and audit requirements. Bronze layer retains full history indefinitely (or per legal requirements). Silver and Gold layers follow more aggressive retention (typically 7–10 years). Automated deletion protects privacy and reduces storage costs.

## Chapter 9: Domain 3 — Risk Management System

### 9.1 Purpose and Scope

The Risk Management System is the analytical brain of the tax administration. It continuously monitors compliance patterns, identifies taxpayers and transactions requiring intervention, and produces risk scores that drive workload prioritization in Case Management. The system combines rules-based indicators with machine learning models to detect both known risk patterns and emerging anomalies.

### 9.2 Key Design Decisions

The Risk Management System is built around principles that emphasize configurability and explainability:

- Configuration-driven (not programmed): Tax officers and compliance specialists define risk indicators through a no-code configuration UI, not by writing code. This enables rapid adaptation to new compliance challenges without programmer involvement. A compliance officer can add a new indicator within hours, not weeks.

- ML models retrain automatically: Historical audit outcomes continuously feed back into model training. The system automatically retrains models weekly (or on a configured schedule) using the latest available data. Model performance is validated before deployment.

- Explainability is a first-class requirement: Every risk score must be accompanied by an explanation of the top contributing factors in language understandable by a compliance officer (not a data scientist). This supports the taxpayer right to understand administrative decisions and enables officers to justify audit selections.

### 9.3 Risk Indicator Framework

The system manages hundreds of risk indicators organized by compliance area:

- Indicator Categories: Indicators are grouped by compliance area (registration, filing timeliness, reporting accuracy, payment compliance). Each area has 10–30 indicators, enabling fine-grained assessment of taxpayer compliance status.

- Indicator Specification: Each indicator defines: data sources (which datasets to query), calculation logic (SQL or formula), thresholds (when an indicator is "abnormal"), and reliability weight (how much this indicator contributes to overall risk). Specifications are versioned to support historical analysis.

- No-Code Configuration UI: A web interface allows compliance specialists to define new indicators. The specialist specifies: indicator name, description, data sources, calculation (using a formula builder, not SQL), thresholds. The system generates the underlying SQL query automatically.

### 9.4 Machine Learning Pipeline

Predictive models enhance rule-based indicators by learning patterns from historical data:

- Training on Historical Audit Outcomes: Models are trained on audits completed in prior years, using taxpayer characteristics and filed data as features, and audit findings (adjustment amount, fraud indicators) as labels. A decision tree or gradient boosting model learns which combinations of features predict high-value audit findings.

- Validation with Hold-Out Sets: The system reserves 20% of historical data (by year) for validation. Model performance is evaluated on hold-out data to estimate how well the model will perform on future taxpayers it has never seen.

- Continuous Retraining Cycle: After each week (or configured interval), the system retrains all models using all available historical data. This ensures that models incorporate the latest compliance patterns.

- Model Registry with Versioning: All trained models are stored in a versioned registry. This enables rollback if a new model performs worse than the prior version, and enables A/B testing of new models.

- A/B Testing of New Models: A new model can be deployed in "advisory mode"—it generates risk scores visible only to analysts, not used for official case selection. Performance is compared against the production model over several weeks. Only after demonstrating improvement is the new model promoted to production.

### 9.5 Network Analysis

Advanced analytics detect organized non-compliance schemes by analyzing relationships among taxpayers:

- Graph Database of Relationships: Stores a graph of taxpayer relationships—ownership (company A owns company B), transactions (company A invoiced company B), shared representatives (both companies have the same accountant or director). Enables pattern detection across networks.

- Carousel Fraud Detection: Identifies patterns typical of VAT carousel fraud—rapid chains of invoices circulating through multiple companies, with the final company disappearing. Graph algorithms detect these patterns automatically.

- Missing Trader Detection: Identifies companies that issue many invoices, claim input VAT, then disappear before tax liabilities come due. Historical patterns of missing traders inform scoring of similar, current companies.

- Coordinated Non-Compliance Detection: Identifies groups of companies whose behavior is statistically unlikely to be independent—for example, a cluster of companies all claiming identical deduction rates, all using the same intermediary. Network analysis flags these anomalies.

- Community Detection Algorithms: Algorithms like Louvain community detection identify natural clusters in the relationship graph—groups of related companies that should be audited together. Results inform case assignment and investigation strategy.

### 9.6 Taxpayer Risk Profiles

Individual taxpayers are assigned composite risk profiles:

- Composite Scoring: A taxpayer's risk score combines rule-based indicators (weighted by reliability), ML model predictions (weighted by accuracy), and network indicators (modified by relationships to other high-risk entities). The composite score is typically on a 0–100 scale.

- Near-Real-Time Updates: As new data arrives (a new filing, a bank transaction), the risk score for the affected taxpayer is recalculated. Significant score movements trigger alerts for compliance managers.

- Risk Segmentation: Taxpayers are segmented into risk tiers (low, medium, high, critical) based on scores. Each tier receives different treatment: low-risk taxpayers receive pre-filled returns and minimal verification; high-risk taxpayers are selected for detailed audits.

- Resource Allocation: Risk tiers drive resource allocation—compliance resources are concentrated on high-risk taxpayers where audit ROI is highest.

### 9.7 Explainability

Explaining risk scores is essential for both taxpayer rights and officer decision support:

- Top Contributing Factors: For every risk score, the system identifies the top 3–5 factors driving the score and presents them in plain language. Example: "Medium risk (score 58) due to: (1) Revenue growth of 120% exceeds industry average by 35% (contributes +20 points), (2) Inventory turnover ratio below industry median (contributes +15 points), (3) No filings submitted in prior year (contributes +10 points)."

- Taxpayer Rights: In jurisdictions with strong administrative procedure codes, taxpayers have a legal right to understand administrative decisions. Providing plain-language explanations of risk scores fulfills this requirement and builds trust.

- Officer Decision Support: Audit officers use explanations to determine investigation strategy—if the top factor is revenue growth, they focus on revenue substantiation; if the top factor is missing prior filing, they investigate prior-year compliance.

### 9.8 Rules as Code

Tax legislation is encoded as machine-consumable rules, eliminating interpretation drift:

- Compliance Logic as APIs: Rather than embedding tax rules in application code, they are defined as APIs. An API called "AssessEligibility" determines if a taxpayer qualifies for a tax benefit; "CalculateLiability" computes tax owed. These APIs accept structured input and return structured output.

- Single Source of Truth: Tax rules are defined once, then used everywhere. Payroll system uses the same rules as audit module. This prevents situations where two departments calculate tax differently based on outdated rule versions.

- Legislation Versioning: As tax law changes, new rule versions are defined. Prior versions remain available to support reprocessing historical data or analyzing prior-year liabilities. This supports audit trails and dispute resolution.

## Chapter 10: Domain 4 — Case Management Platform

### 10.1 Purpose and Scope

The Case Management Platform handles all structured back-office work in the tax administration. It manages audit cases, taxpayer service requests, enforcement actions, objections and appeals, and internal administrative processes. The platform is the central system for all workflow-driven operations, ensuring consistent procedures and enabling workload balancing across the organization.

### 10.2 Low-Code/No-Code Approach

Case Management is implemented on a commercial low-code platform (e.g., OutSystems, Mendix, ServiceNow), enabling rapid customization without extensive custom code:

- Tax Officer Configuration: Tax officers with domain expertise (but not programming expertise) configure workflows, case types, and business rules using visual tools. They define branching logic, SLA rules, and assignment algorithms without writing code.

- Dramatically Reduced Bespoke Code: A traditional bespoke ITAS has 10,000+ lines of custom application code. A low-code Case Management implementation typically has < 5,000 lines (primarily custom integrations and very specialized logic), a 50% reduction. This reduces maintenance burden and speeds feature development.

- Rapid Adaptation to Change: When a procedure changes (e.g., appeal workflow is modified by new legislation), the workflow is updated in the configuration UI within hours. No code review, no deployment pipeline, no testing regression cycle (though functional testing is still required).

### 10.3 Configurable Workflows

Each case type (audit, refund review, collection, appeal) has a configurable workflow:

- Visual Workflow Designer: A drag-and-drop interface allows definition of workflow states, transitions, and conditions. A tax administrator defines: "Start in Review state. If documentation is complete, transition to Analysis. If documentation is incomplete, transition to Pending with a 10-day timer. If 10 days pass without response, auto-close."

- Rich Branching: Conditional branching based on case attributes, risk scores, or rule outcomes. Example: "If case risk score > 75, route to Specialist queue. Otherwise, route to Generalist queue."

- Escalation and Parallel Processing: Cases can split into parallel sub-workflows (e.g., an audit case might initiate concurrent document collection and risk analysis tasks) and rejoin. Escalation rules automatically move cases up the hierarchy if not completed by a deadline.

- Workflow Versioning: New case types use the latest workflow version. Existing cases in progress continue on their original workflow version, preventing mid-process disruption if the workflow is later refined.

### 10.4 Task Allocation

The platform intelligently allocates case tasks to optimize workload balance and quality:

- Skill-Based Routing: Each task specifies required skills (e.g., "audit experience with VAT returns"). The system routes the task to an officer with those skills who is currently available, or places it in a queue if no skilled officer is available.

- Workload Balancing: Tasks are allocated to minimize queue length and officer idle time. If one team is overloaded and another is idle, the system can rebalance work (if skills overlap).

- Conflict-of-Interest Detection: The system maintains a conflict-of-interest matrix (e.g., an officer cannot audit her spouse or a company where she has financial interest). When a case is assigned, conflicts are automatically checked and the case is re-routed if necessary.

- Priority Queuing: Statutory deadlines, risk scores, and case complexity determine priority. High-priority cases (near deadline, high-risk) are always at the front of queues.

### 10.5 Document Management

All case evidence and communication is managed in a permanent record:

- Centralized Document Store: All documents related to a case (taxpayer submissions, correspondence, audit working papers, officer findings) are stored in a centralized, searchable repository.

- Version Control: Documents support versioning—multiple versions of a return or analysis can be stored with dates and user tracking, enabling audit trails.

- Full-Text Search: All documents are indexed and searchable by content. Auditors can search across all historical cases for similar patterns.

- Confidentiality Handling: Sensitive information (taxpayer personal data, officer annotations) is marked for restricted access. Row-level security enforces who can view specific documents.

### 10.6 SLA/KPI Monitoring

The platform provides real-time monitoring of service levels and performance:

- Response Time SLAs: Taxpayer-facing cases (refund requests, objections) have target response times (e.g., 30 days). The system tracks actual response time and alerts managers when an SLA is at risk of breach.

- Case Aging Alerts: Cases approaching statutory deadlines trigger alerts—red flags for approaching 180-day audit deadline, green flags for completed cases being stored.

- Management Dashboards: Executive dashboards show cases by status, average age by status, backlog by team, and SLA compliance rate. Managers can drill down from dashboard to individual cases.

- Performance Metrics: Metrics tracked by office, team, and individual officer—cases closed per month, average case duration, error rates (cases requiring revision), quality scores from taxpayer surveys.

### 10.7 Why Low-Code Minimizes Bespoke

The low-code approach significantly reduces bespoke code compared to traditional ITAS development:

- Every Workflow is Configuration, Not Code: A workflow is configuration, versioned and stored in the low-code platform's database. It is not code that requires deployment pipelines, compilation, and testing. Changes can be promoted from test to production within hours.

- Platform Upgrades Do Not Break Workflows: When the low-code platform releases a new version, existing workflows continue to work without modification. A new platform version introduces new capabilities but maintains backward compatibility. Bespoke ITAS often breaks during version upgrades, requiring extensive regression testing and code fixes.

- Multiple Vendors Support the Same Paradigm: The low-code market is mature with many vendors (OutSystems, Mendix, ServiceNow, Microsoft Power Platform, Appian). If the organization wants to switch platforms, the migration effort is lower because workflows and data models are defined using similar abstractions.

- Estimated 80% Reduction in Custom Development: Compared to a bespoke ITAS with 50,000+ lines of custom code, a low-code Case Management platform achieves the same functionality with < 5,000 lines, primarily for custom integrations (e.g., connecting to proprietary legacy systems). This 80% reduction dramatically reduces maintenance burden, time-to-market for new features, and total cost of ownership.

## Chapter 11: Domain 5 — External Integration Platform

### 11.1 Purpose and Scope

The External Integration Platform handles all cross-boundary data exchange. It manages integration with domestic government agencies (national registries, treasury, social security, customs), EU integration (ViDA, VIES), international data exchange (CRS, CbCR), and third-party data providers (banks, utilities, credit bureaus). By centralizing external integration, this domain isolates external complexity from core tax operations, enabling the other domains to focus on their primary responsibilities.

### 11.2 Key Design Decisions

The External Integration Platform applies the Information Mediator pattern from GovStack to manage cross-boundary communication:

- Information Mediator Pattern: All external communication is routed through this domain. Internal domains never communicate directly with external systems. This enables centralized policy enforcement, security, and audit logging.

- Protocol Translation: External systems speak different protocols (SOAP web services, REST APIs, SFTP file-based) and use different data formats (XML, JSON, proprietary). The Integration Platform translates between external protocols and the internal event/API model used by other domains.

- Security Server: Mutual authentication and encryption are enforced at the boundary. The security server handles certificate management, request signing, response verification, and non-repudiation for all cross-boundary communication.

### 11.3 Domestic Integration

The platform integrates with national government systems that provide context for tax administration:

- National Registries: (1) Business Registry—company registration, ownership, business classification, legal status; (2) Population Registry—individual identification, residency, family relationships; (3) Property Registry—real estate ownership, property classification, valuation; (4) Vehicle Registry—vehicle ownership, registration status. Registry queries are performed during taxpayer registration and periodically verified.

- Treasury Integration: Payments received by the tax authority are immediately reported to the treasury (national finance ministry). Refunds processed by the tax authority are funded through the treasury. This integration enables real-time revenue reporting and proper fund accountability.

- Social Security Integration: Employment data (wages, employment status, pension contributions) from the social security administration is periodic ingested into the Data Platform. This data is used to verify income reported by taxpayers and identify underreporting.

- Customs Integration: Import/export transaction data from customs is periodically ingested. Used to verify reported international transactions and identify underreported foreign income.

- Other Government Agencies: Integration points with other agencies as needed (e.g., higher education for student status verification, health service for practitioner registration).

### 11.4 EU Integration

The platform prepares for and supports integration with EU-level tax systems and standards:

- ViDA Digital Reporting Requirements: The EU's VAT in the Digital Age (ViDA) initiative requires real-time reporting of intra-EU B2B transactions. The platform supports this by tracking cross-border transactions and reporting them to EU repositories. Implementation roadmap: Phase 1 (months 0–12) = collect data and prepare schemas; Phase 2 (months 12–24) = implement reporting feeds; Phase 3 (months 24–36) = integrate with EU platforms.

- VIES (VAT Information Exchange System): The platform queries VIES (maintained by the European Commission) to verify VAT registration numbers of EU trading partners. Used during invoice validation and taxpayer registration.

- EN16931 E-Invoice Format Alignment: Currently, the platform accepts multiple invoice formats. Roadmap includes progressive standardization to EN16931 (the European e-invoice standard), enabling seamless cross-border B2B transactions.

- Platform Deemed-Supplier Rules: Under ViDA, certain digital platforms (marketplaces, payment processors) are designated as "deemed suppliers" and have VAT obligations. The platform implements rules to identify and track deemed-supplier transactions.

- Single VAT Registration Reforms: The EU is piloting single VAT registration for cross-border traders, allowing a company registered in one member state to report VAT for all member states through a single registration. The platform accommodates this by tracking multi-state obligations.

### 11.5 International Data Exchange

The platform supports international cooperation on tax information exchange:

- OECD Common Reporting Standard (CRS): Financial institutions report foreign financial account information to tax authorities. The tax authority receives CRS reports from foreign countries regarding residents who hold accounts abroad. This data is loaded into the Data Platform and used for compliance risk scoring.

- Country-by-Country Reporting (CbCR): Large multinational enterprises file CbCR reports detailing profit allocation across countries. Received reports are matched with domestic transfer pricing documentation. Discrepancies trigger audit investigations.

- Mutual Administrative Assistance: Tax authorities can request information from peer countries about specific taxpayers. Requests are routed through the Integration Platform and tracked for compliance with bilateral treaties.

- Spontaneous and Automatic Exchange: Automatic exchange of information with partner countries (e.g., annual CRS exchange). Spontaneous exchange triggered by risk analysis ("we found a taxpayer with significant undisclosed income from country X; notifying country X authorities").

### 11.6 Third-Party Providers

The platform integrates with commercial data providers to supplement internal data:

- Banks: Transaction data and account information used to verify taxpayer-reported income and identify undisclosed transactions. APIs provided by banks enable real-time query (and, increasingly, continuous push of transaction data).

- Utilities: Address verification (is a taxpayer actually living at the declared address?) and consumption patterns (does reported household income match consumption levels?). Utility data can flag inconsistencies worth investigating.

- Payment Platforms: Data from payment processors (e.g., mobile money, e-commerce platforms) shows transaction volumes and patterns for merchants. Cross-matched against reported revenue.

- Credit Bureaus: Credit history and credit scores of taxpayers can indicate financial stress (correlates with increased compliance risk) or undisclosed income (credit limit increased; suspected hidden income).

- Real Estate Registries: Property records show real estate transactions, valuations, and rentals. Cross-matched against reported rental income (if a taxpayer owns 10 rental properties but reports no rental income, a flag is raised).

### 11.7 Security

Cross-boundary communication requires the highest security standards:

- Mutual TLS for All Cross-Boundary Communication: Both tax administration and external system authenticate using X.509 certificates. Communication is encrypted with TLS 1.3. Certificates are rotated quarterly.

- Message Signing and Non-Repudiation: Critical messages (e.g., payment confirmations, audit notices sent to external parties) are cryptographically signed. The signature proves the message originated from the tax authority and has not been altered. Digital signatures enable non-repudiation (sender cannot later deny sending the message).

- Audit Trail of All Data Exchanges: Every API call, file transfer, or message sent or received across the boundary is logged with timestamp, parties, content hash, and outcome. Logs are retained for 7–10 years to support audit and dispute resolution.

- Data Classification Enforcement at Boundary: Data leaving the tax authority is classified and encrypted according to sensitivity level. Restricted data (e.g., taxpayer personal identifiers) are never transmitted outside the domain; instead, anonymized or hashed identifiers are used.

## Chapter 12: Cross-Domain Integration

### 12.1 Event-Driven Architecture

Domains communicate primarily through events, enabling loose coupling while maintaining coordination:

- Event Bus (Kafka/RabbitMQ): A message broker serves as the central nervous system. When an event occurs in one domain, it is published to the event bus. Other domains that care about this event subscribe to it.

- Event Schema Registry with Versioning: All event types are defined in a schema registry (e.g., Apache Avro or JSON Schema). Each event type has a name, fields, and version. Multiple versions can coexist to support gradual migration of consumers.

- At-Least-Once Delivery with Idempotent Consumers: The message broker guarantees that each event is delivered at least once. Consumers must be idempotent—processing the same event twice produces the same result as processing it once. Example: if the "Payment Received" event triggers a ledger entry, the consumer must check if the ledger entry already exists before creating it again.

### 12.2 Integration Patterns

Different integration patterns serve different use cases:

- Pub/Sub (Asynchronous Notifications): Data Capture publishes "Return Filed" event. Data Platform, Risk Management, and Case Management all subscribe. Each updates its local state independently. No waiting or blocking.

- Request/Response (Synchronous Queries): Case Management queries Operational Tax Processing: "What is the taxpayer's current assessment?" Response is returned synchronously. Used when immediate information is required.

- Batch Sync (Bulk Operations): Nightly batch jobs synchronize bulk data—data warehouse refresh, periodic data quality reporting, end-of-month reconciliation. Scheduled at off-peak hours.

### 12.3 Compliance Lifecycle

The cross-domain compliance lifecycle represents the full customer journey through the tax system. The lifecycle flow from filing to resolution uses all five domains in a coordinated sequence: A taxpayer files a return through Data Capture → Data Capture validates and stores the return → Data Capture publishes "Return Filed" event → Data Platform subscribes and ingests the return into Silver layer, enriches with third-party data, produces a Taxpayer Profile update → Risk Management subscribes to "Return Filed," recalculates taxpayer risk score, detects anomalies → If risk exceeds threshold, Risk Management publishes "High-Risk Taxpayer Detected" event → Case Management subscribes, creates an audit case, assigns it to an auditor → Auditor investigates using Case Management tools, queries Taxpayer Profile from Data Platform, possibly runs validation rules through Risk Management → Auditor produces findings → Case Management publishes "Audit Complete" event → Data Platform ingests audit findings → Operational Tax Processing recalculates assessment based on audit findings → Payment is collected → Case is closed. This lifecycle demonstrates how domains coordinate to deliver end-to-end compliance management. Reference Figure 6.2 (Compliance Lifecycle Integration) embedded in Chapter 6.

### 12.4 API Standards

All inter-domain and external APIs follow consistent standards:

- REST with OpenAPI 3.0 Specification: All APIs are RESTful (using HTTP methods: GET for queries, POST for create, PUT/PATCH for updates, DELETE for removal). APIs are formally documented with OpenAPI 3.0 specification.

- JSON as Default Data Format: Requests and responses use JSON. Avoids XML complexity and aligns with modern API practices.

- OAuth 2.0 / OpenID Connect Authentication: APIs use standard, industry-proven authentication. OAuth 2.0 handles authorization (what can a caller do). OpenID Connect adds identity layer (who is the caller). Supports both service-to-service (client credentials) and user-initiated (authorization code) flows.

- Semantic Versioning: API versions follow semantic versioning (major.minor.patch). Major version increment indicates breaking changes. Minor version is backward-compatible enhancements. Patch is bug fixes.

- Backward Compatibility Guaranteed for 2 Major Versions: Current production API is v3. v1 and v2 remain available for legacy clients. v0 is deprecated and will be decommissioned. This gives clients time to migrate without forced disruption.

---

# PART IV — CROSS-FUNCTIONAL REQUIREMENTS

## Chapter 13: Development Standards

Development and deployment of the architecture must follow comprehensive standards to ensure quality, consistency, and maintainability across all domains. This chapter outlines the minimal set of standards; each domain may define additional standards specific to its technology stack.

### Version Control

All source code, configuration files, and infrastructure definitions must be version controlled using Git. Repository policies include: main branch is production-grade code, all changes require peer review via pull requests before merge, commit messages follow conventional commits format (feat:, fix:, refactor:, docs:, etc.), and tags mark all production releases.

### CI/CD Pipelines

Every code commit triggers automated build, test, and deployment pipelines. Pipelines enforce: automated compilation and linking, automated test execution (unit tests, integration tests), code quality analysis using static analysis tools, and security scanning (dependency vulnerabilities, secret detection). Artifacts passing all quality gates are automatically deployed to staging. Production deployments require explicit approval from authorized personnel.

### Test Coverage Requirements

Test coverage targets vary by component type: core business logic (tax rules, assessment calculations, risk algorithms) must have ≥90% code coverage with unit tests. Integration points (APIs, database queries, external system integration) must have ≥80% coverage with integration tests. UI components must have ≥70% coverage with functional tests. Coverage reports are generated on every build and tracked over time; regression in coverage blocks merges.

### OpenAPI Specifications for All APIs

Every API endpoint is formally documented in OpenAPI 3.0 format. Documentation includes: endpoint path and HTTP method, request parameters (path, query, body) with type and validation rules, response schemas for success and error cases, authentication requirements, rate limits, and examples. OpenAPI specs are version-controlled and generated from code annotations (not manually maintained). Client SDKs are auto-generated from OpenAPI specs in target languages (Java, Python, JavaScript, C#).

### Code Quality Audits

Code is continuously analyzed for quality issues using tools like SonarQube, Checkmarx, or similar. Quality gates include: complexity metrics (cyclomatic complexity < 15 per function), duplication (no code block duplicated > 3 times), security hotspots (potential security issues flagged for review), and design violations (overly large classes, too many parameters). Code not meeting quality gates cannot be merged.

### Technology Stack

Preferred languages are selected from the top 25 by usage (TIOBE Index): Java, Python, C#, JavaScript/TypeScript, Go, SQL, etc. Rationale: large talent pool, proven track record, extensive libraries and tools, strong community support. Exceptions require architecture board approval. Open-source licensing preference: Apache 2.0, MIT, GPL-compatible licenses are preferred. Proprietary or restricted licenses require licensing review.

Reference: GovStack CFR 6.1 (Common Functional Requirements for Development) provides additional guidance on development practices, tooling recommendations, and quality metrics.

## Chapter 14: Deployment and Operations

The architecture is designed for continuous deployment and zero-downtime operations. This chapter defines the operational standards that enable reliable, scalable, production-grade tax administration systems.

### Container Packaging (OCI-Compliant)

Every application component is packaged as an OCI-compliant container image (Docker). Container images contain the application, dependencies, runtime environment, and configuration defaults. Images are immutable once built and tagged, ensuring that production exactly matches tested artifact. Container registries store and version all images. Images are regularly scanned for security vulnerabilities.

### Kubernetes Orchestration

Deployed containers are orchestrated using Kubernetes. Kubernetes provides: automatic scheduling of containers across a cluster of machines; horizontal auto-scaling (adding more replicas of a service as load increases); health checking and self-healing (replacing failed containers); rolling updates (deploying new versions without downtime); and sophisticated networking and service discovery.

### Multi-Environment Support

The deployment pipeline supports four environments: Development (for active feature development), Test (for QA and integration testing), Staging (mirrors production, used for final validation and performance testing), and Production (live tax administration system). Each environment has: separate database instances, separate external integrations (e.g., test APIs for treasury, test certificates for cross-border), and separate backup and recovery policies.

### Backup and Restore

Data is backed up continuously. Backup policies include: full backup daily (e.g., nightly), incremental backups hourly, transaction logs archived to separate storage for point-in-time recovery. Recovery Time Objective (RTO) targets: critical data recoverable within 4 hours; non-critical data within 24 hours. Recovery Point Objective (RPO) target: maximum 1 hour of data loss. Backup restoration is tested quarterly to ensure backups are usable.

### Deployment Documentation

Every deployment includes comprehensive documentation: release notes (features, fixes, known issues), deployment runbook (step-by-step instructions for operators), rollback procedure (how to revert to prior version), and post-deployment validation (checklist of health checks to verify success). Documentation is version-controlled alongside code.

### Rollback Capability

If a production deployment introduces critical issues, the system must rollback to the prior version within minutes. Rollback is automated: prior version containers are re-deployed, databases are recovered from transaction logs if needed. Rollback is tested during every deployment to staging to ensure it works when needed in production.

### Graceful Shutdown

Services must support graceful shutdown: when a service receives a termination signal, it stops accepting new requests, completes in-flight requests, and closes database connections cleanly. This ensures no data loss or corruption during rolling deployments or maintenance. Graceful shutdown timeout is configurable but typically 30 seconds.

Reference: GovStack CFR 6.2 (Common Functional Requirements for Deployment and Operations) provides additional detail on infrastructure, monitoring, incident response, and service level agreements.

## Chapter 15: Security Architecture

Security is a cross-cutting concern affecting all domains and components. This chapter defines the security standards and architecture that protect tax administration systems and taxpayer data.

### Transport Security (TLS 1.3)

All inter-service communication and external communication is encrypted using TLS 1.3. TLS is configured to support strong cipher suites only (no legacy weak ciphers). TLS certificates are obtained from trusted certificate authorities and rotated before expiration (automated certificate management). Certificate pinning is used for high-value integration points (e.g., treasury, national registries) to prevent man-in-the-middle attacks.

### Identity and Access Management (OAuth 2.0 / OIDC)

User and service authentication uses industry-standard OAuth 2.0 and OpenID Connect. All tax officers authenticate through OIDC (using eID credentials where possible). API consumers authenticate using OAuth 2.0 client credentials. Multi-factor authentication (MFA) is required for all personnel with production access. Session tokens expire after configurable periods (typically 8–12 hours for user sessions, minutes for API tokens).

### Data Encryption at Rest

Sensitive data (taxpayer PII, tax return details, authentication credentials) is encrypted at rest using AES-256. Encryption keys are managed separately from encrypted data—keys are stored in a hardware security module (HSM) or key management service (KMS), not embedded in code. Key rotation is performed annually at minimum.

### Input/Output Sanitization

All untrusted input (from taxpayer submissions, external systems, user forms) is validated and sanitized before processing. Validation includes: type checking, format validation, range checking, and rejection of unexpected characters. Output is escaped appropriately for the context (HTML escaping for web pages, SQL escaping for database queries) to prevent injection attacks.

### Security Event Logging

All security-relevant events are logged: authentication success/failure, privilege escalation, access to sensitive data, configuration changes, API calls to external systems, encryption key operations. Logs include: timestamp, user/service identity, action, result, and data involved (or hash of sensitive data). Logs are centralized and retained for 7–10 years to support audit and forensic investigation.

### Key Rotation

Encryption keys, API keys, and credentials are rotated on a regular schedule (annually for encryption keys, quarterly for API keys, immediately when compromise is suspected). Old keys are retained in the KMS to support decryption of historical data but marked as deprecated.

### Vulnerability Scanning

Automated vulnerability scanning is performed continuously: dependencies are scanned for known CVEs using tools like Dependabot or Snyk; container images are scanned before deployment; and penetration testing is conducted annually by external security specialists. Any critical or high-severity vulnerabilities trigger emergency patching.

### Software Bill of Materials (SBOM)

Every deployable artifact includes an SBOM listing all direct and transitive dependencies, versions, and license information. SBOMs are generated automatically during the build process using tools like CycloneDX. This enables rapid identification of affected systems when vulnerabilities are discovered in dependencies.

### Disaster Recovery

Comprehensive disaster recovery procedures ensure business continuity: data is replicated to a geographically separate data center; RPO and RTO targets are defined and regularly tested; disaster recovery drills are conducted quarterly; and a disaster recovery runbook is maintained and updated. Critical functions are designed to survive data center failure with minimal interruption.

Reference: GovStack CFR 6.5 (Common Functional Requirements for Security) provides comprehensive security requirements including threat modeling, access controls, audit trails, and incident response procedures.

## Chapter 16: Quality and Performance

High-quality tax administration systems must combine excellent user experience, accessibility, and reliable performance. This chapter defines quality and performance standards.

### HTML5/CSS3 for Web UIs

All web-based user interfaces (tax portal, compliance officer dashboards, analytics tools) are built using modern web standards: HTML5 for structure, CSS3 for styling, and JavaScript frameworks (React, Vue, Angular) for interactivity. Legacy technologies (Flash, Java applets) are not used. UI code is version-controlled and tested.

### Accessibility (WCAG 2.1 AA)

All public-facing interfaces and internal tools meet WCAG 2.1 Level AA accessibility standards. This ensures that tax systems are usable by all taxpayers regardless of disability: screen reader compatible (for blind users), keyboard navigable (for users unable to use a mouse), color-not-only for information (for colorblind users), and captions for videos (for deaf users). Automated accessibility testing is part of the CI/CD pipeline.

### Localization Support

Systems support multiple languages. Text is externalized from code into message catalogs. Forms, reports, and error messages are translated into all required languages. Date, time, currency, and number formats are localized to user's locale. Right-to-left (RTL) languages are supported in the UI layout. Localization is not an afterthought but designed in from the start.

### OpenAPI Documentation

All APIs are comprehensively documented using OpenAPI 3.0 specifications. Documentation is hosted in a developer portal where external and internal partners can explore APIs, view examples, and download SDKs. Documentation is auto-generated from code and kept up-to-date.

### Functional Test Coverage

Core business workflows have comprehensive functional test coverage: end-to-end tests verify complete scenarios (taxpayer registers, files a return, receives assessment, pays, audit case is opened and resolved), regression tests catch regressions in prior functionality, and performance tests verify that workflows complete within SLA targets.

### Compliance Test Mock Implementations

External system dependencies (national registries, treasury, banks) are mocked for testing. Mock implementations return realistic test data, enabling thorough testing without requiring access to production external systems. Mocks support fault injection (simulating registry unavailability, slow responses) to test error handling.

### SLA Targets by Domain

Service level agreements (SLAs) define expected availability and performance:

- Data Capture Platform: 99.9% availability (maximum 43 minutes/month downtime); sub-second response time (< 1 second p95) for filing operations.

- Data Platform: 99.5% availability; sub-10-second response time (< 10 seconds p95) for complex analytical queries.

- Risk Management: 99.5% availability; risk score calculations complete within 1 hour of data arrival.

- Case Management: 99.5% availability; workflow operations complete within seconds.

- External Integration: 99.9% availability for critical integrations (treasury, national registries); 99% for secondary integrations (banks, utilities).

Reference: GovStack CFR 6.4 (Common Functional Requirements for Quality and Performance) provides additional requirements on usability, performance monitoring, and quality assurance processes.

---

# PART V — GOVERNANCE AND IMPLEMENTATION

## Chapter 17: Sourcing Strategy

The architecture spans a range of sourcing options: bespoke development, commercial off-the-shelf (COTS) software, open-source platforms, and configuration of low-code platforms. Each domain has an optimal sourcing approach based on competitive differentiation, commodity vs. unique characteristics, and organizational capacity.

### 17.1 Build vs Buy vs Configure Decision Framework

| Domain | Build | Buy | Configure | Rationale |
| --- | --- | --- | --- | --- |
| Data Capture Platform | Bespoke API layer (high differentiation, unique business logic) | N/A | Portal built on low-code (rapid evolution, minimal code) | API layer is a competitive asset requiring deep tax knowledge. Portal benefits from low-code configurability. |
| Data Platform | Custom transformation logic (business-specific rules) | N/A | Open-source (ClickHouse/Doris + dbt + Airflow) | Use proven open-source components; data transformations are custom. |
| Risk Management System | Custom ML/analytical components (organization-specific models) | N/A | Rules engine configuration (low-code rules definition) | Core ML models are competitive differentiators. Rules are configured, not coded. |
| Case Management Platform | Minimal (< 5% of code) | Commercial low-code COTS (OutSystems, Mendix, ServiceNow) | Workflows, forms, dashboards via low-code UI | Case management is not a differentiator; COTS + low-code reduces bespoke code and maintenance. |
| External Integration Platform | Custom adapters for unique integrations | N/A | Open-source (Information Mediator + adapters) | Integration patterns are standard (Information Mediator); adapters are custom. |

### 17.2 Vendor Lock-In Prevention

While the architecture uses both open-source and commercial components, several strategies mitigate vendor lock-in risk:

- Multi-Vendor Strategy: Core capabilities are not dependent on a single vendor. For example, the Data Platform can run on ClickHouse or Doris; Case Management can run on OutSystems, Mendix, or ServiceNow. If a vendor becomes problematic, alternatives exist.

- API Contracts as Vendor Boundary: Interfaces between the tax authority and vendors are defined by API contracts (OpenAPI specs). As long as the vendor honors the contract, the internals can change. The tax authority can switch vendors without disrupting other domains.

- Data Portability Requirements: All commercial contracts include data portability clauses. If a relationship with a vendor ends, the tax authority can export all data in standard formats (CSV, JSON, Parquet) within 30 days.

### 17.3 Bespoke Footprint KPI

The organization tracks custom code as a Key Performance Indicator (KPI) to resist scope creep and bespoke development:

- Definition: Bespoke Footprint = total lines of custom code written by the organization / total lines of code in production. Target: < 20% bespoke across the entire platform.

- Tracking: Calculated quarterly by domain using automated source code analysis. Results reported to the steering committee.

- Examples: (1) Data Capture Platform: 50,000 lines (30,000 bespoke API layer + 20,000 low-code portal). Bespoke Footprint = 30,000 / 50,000 = 60%. (2) Case Management: 200,000 lines (10,000 custom integration code + 190,000 low-code platform code). Bespoke Footprint = 10,000 / 200,000 = 5%. (3) Overall: 2,000,000 lines total code, 300,000 bespoke = 15% footprint (within target).

- Governance: If bespoke footprint creeps above 25%, triggers a review. Why is code being written instead of configured or purchased? Are we losing discipline?

## Chapter 18: Organizational Alignment

Technical architecture must be accompanied by organizational design that supports product-oriented, domain-driven delivery. This chapter outlines the organizational changes required.

### Domain-Aligned Teams with Product Owners

Instead of organizing teams by function (all developers, all QA, all operations), teams are organized by domain. Each domain (Data Capture, Data Platform, Risk Management, etc.) has a cross-functional team: product owner, developers, testers, and operations engineers. The product owner is accountable for the domain's mission and roadmap. Teams can make decisions independently within domain boundaries, enabling velocity. Teams coordinate through APIs and events rather than shared databases or shared code.

### Enterprise Architecture Governance Board

A governance board meets monthly to review: new projects and their architectural fit with the reference architecture, cross-domain integration proposals (ensuring loose coupling), technology selections (new languages, frameworks, databases), and architectural debt (paying down technical debt vs. feature development). The board is chaired by the CTO and includes representatives from each domain.

### Data Management Office (DMO)

As described in Chapter 8, a dedicated Data Management Office is responsible for data governance, data quality, and data analytics. The DMO reports to the Commissioner/Director-General and ensures that the organization's data is treated as a strategic asset.

### New Skills Required

Building and operating this architecture requires skills different from traditional ITAS teams:

- Data Engineering: Skills in data pipelines, ETL tools, data quality, data warehousing. Hire or train data engineers to build and maintain the Data Platform.

- ML Operations (MLOps): Skills in model training, validation, deployment, monitoring. Hire ML engineers to build and maintain predictive models in Risk Management.

- Platform Engineering: Skills in Kubernetes, CI/CD, infrastructure-as-code, cloud operations. Hire platform engineers to operate the underlying infrastructure.

- API Design: Skills in RESTful design, OpenAPI specs, API versioning. API design workshops for all developers.

- Tax and Compliance Domain Expertise: Critical that new technology teams include tax professionals who understand the business. Avoid "black box" AI/ML systems by embedding domain knowledge.

### Change Management for Product Mindset

The architecture embodies a shift from a project mindset ("build and hand off") to a product mindset ("owned and evolved"). This cultural change is significant: Staff accustomed to hierarchical project structures must adapt to cross-functional teams making autonomous decisions. Success requires: executive sponsorship and communication that this is the desired culture, training and coaching of team leads, patience through an initial productivity dip as teams adapt, and celebration of successes in the new model.

## Chapter 19: Implementation Roadmap

### 19.1 Why Big-Bang Fails

History of tax administration modernization worldwide shows a clear pattern: big-bang replacement of legacy ITAS with new systems often fails catastrophically. Failures cost billions and result in revenue disruption. Examples include:

- Failed ITAS Replacements: Several countries (UK, US states, EU members) have abandoned or drastically scaled back bespoke ITAS replacements after 5+ years and billions in spending. Common causes: underestimated complexity, inability to test all edge cases in old legislation, lack of iterative validation with business users.

- Revenue Disruption Risk: If the new system has a critical bug, revenue collection halts. Taxpayers cannot file, payments cannot be received, assessments cannot be generated. Rebuilding taxpayer confidence after such disruption takes years.

- Regulation Risk: A big-bang deployment means a long period of dual-maintenance (keeping old system running while building new system). Regulatory changes during this period must be implemented in both systems, consuming resources.

### 19.2 Phased Approach (Lower Risk)

Instead, the architecture is deployed in phases, adding new capabilities gradually while maintaining revenue collection continuity. Each phase should be 3–6 months, enabling rapid delivery and learning.

| Phase | Focus | Activities | Deliverable |
| --- | --- | --- | --- |
| Phase 0 (Months 1–6) | Data Platform MVP | Build Bronze and Silver layers. Ingest data from legacy system. No disruption to taxpayer-facing services. Zero revenue impact. | Data warehouse operational with historical data, enabling analytics pilots. |
| Phase 1 (Months 4–12, parallel with Phase 0) | Risk Management (Advisory) | Build risk scoring models, indicator framework, visualization dashboards. Generate risk scores but do NOT use for case selection yet. Compliance officers view scores for analysis only. | Risk scores in use by compliance officers for analysis and audit planning, not enforcement. |
| Phase 2 (Months 8–18) | Case Management Pilot | Implement one case type (e.g., appeals) on low-code Case Management platform. One regional office pilots new workflows. Continue managing other cases in legacy system. | One case type migrated; proof that low-code Case Management works. |
| Phase 3 (Months 12–24) | Data Capture API Layer | Build Data Capture API layer. First, integrate with existing portals via new API. Later, launch new portal for new taxpayers. Phase old portal gradually. | New taxpayers file through new API/portal. Existing taxpayers gradually migrated. Legacy portal phased out. |
| Phase 4 (Months 18–30) | External Integration | Months 18–24: Integrate with domestic registries and treasury (Information Mediator pattern). Months 24–30: Expand to EU integrations (ViDA, VIES). | Integration with all critical external systems; EU data flows operational. |

## Chapter 20: EU Accession Conformance

For countries with EU accession on their agenda, this architecture includes provisions to support convergence with EU tax administration standards and requirements. This chapter maps the five required changes from EU VAT directives to architectural impacts.

### Required Changes for EU Conformance

| Requirement | Change | Domain(s) | Architectural Impact |
| --- | --- | --- | --- |
| 1. Real-Time Reporting Scope (ViDA) | Expand from B2C to real-time reporting of B2B intra-EU transactions. All invoices issued to EU trading partners must be reported in real time. | Data Capture + External Integration | Data Capture detects intra-EU invoices and publishes real-time events. External Integration routes to EU reporting platform (daily, then hourly, then real-time). |
| 2. E-Invoice Format (EN16931) | Transition from jurisdiction-specific invoice formats to EN16931 European standard. All invoices must be compatible with EN16931 schema. | Data Capture + Data Platform | Data Capture validates incoming invoices against EN16931. Data Platform transforms legacy formats to EN16931 during ingestion (Bronze → Silver). |
| 3. Pre-Funding Mechanism Reform | Change from traditional VAT pre-funding (Ukraine-specific) to EU model. VAT is pre-funded at filing time (not at end-of-quarter). | Data Capture + Risk Management | Data Capture triggers pre-funding calculation on each invoice. Risk Management adjusts VAT risk indicators based on pre-funding compliance. |
| 4. Invoice Blocking Rules | Implement automated blocking of invoices that fail to meet EU compliance rules (e.g., invalid VAT registration, amount exceeds threshold without authorization). | Data Capture | Data Capture rules engine rejects non-compliant invoices, providing real-time feedback to filer. |
| 5. Platform Deemed-Supplier Rules | Digital marketplaces and payment platforms are treated as "deemed suppliers" with own VAT obligations. Rules determine when platform vs. merchant is liable for VAT. | Risk Management + External Integration | Risk Management identifies platform transactions and applies deemed-supplier rules. External Integration coordinates with EU platform registry. |

### Conformance Checklist

Implementation teams use this checklist to verify EU conformance readiness before go-live:

- Real-time reporting: (1) Data Capture detects intra-EU invoices. (2) External Integration publishes events to EU platform. (3) Daily batch reconciliation validates completeness.

- EN16931 e-invoices: (1) Invoice validation schema accepts EN16931 format. (2) Transformation rules convert legacy formats to EN16931. (3) Test cases validate transformation accuracy.

- Pre-funding reform: (1) Filing process calculates and collects pre-funding on submission. (2) Risk Management monitors pre-funding compliance. (3) Test data set covers multiple pre-funding scenarios.

- Invoice blocking: (1) Rules engine enforces blocking conditions. (2) Real-time feedback provided to filer. (3) Appeals process defined for blocked invoices.

- Deemed-supplier rules: (1) Platform transaction identification implemented. (2) Liability rules applied correctly. (3) Reporting to EU platform operational.

ANNEXES

## Annex A: Glossary

| Term | Definition |
| --- | --- |
| Building Block (GovStack) | Reusable, modular component providing a specific capability (e.g., Identity, Registration, Information Mediator). Part of the GovStack ecosystem. |
| CQRS | Command/Query Responsibility Segregation. Architectural pattern separating write operations (commands) from read operations (queries). Enables optimization of each for its workload. |
| Domain | A bounded context with clear responsibilities and interfaces. One of five domains: Data Capture, Data Platform, Risk Management, Case Management, External Integration. |
| Domain-Driven Design (DDD) | Software design approach organizing systems around business domains rather than technical layers. Aligns code structure with organizational structure. |
| Event-Driven Architecture | Architectural style where components communicate through events (notifications of state changes) rather than direct calls. Enables loose coupling. |
| Medallion Architecture | Data warehouse pattern with three layers: Bronze (raw data), Silver (cleansed data), Gold (business logic applied). Each layer adds value. |
| Microservices | Architectural approach decomposing applications into small, independently deployable services, each with specific responsibility. Enables scaling and evolution of individual services. |
| OpenAPI Specification | Machine-readable standard for describing REST APIs. Enables automated client SDK generation, API documentation, and contract testing. |
| TADPRA | Tax Administration Data Platform Reference Architecture. GovStack standard defining baseline design for scalable, maintainable data platforms in tax administrations. |
| ViDA | VAT in the Digital Age. EU initiative modernizing VAT administration through digital reporting, e-invoicing, and real-time transparency. |
| VIES | VAT Information Exchange System. EU system enabling cross-border VAT registration verification. |

## Annex B: GovStack Conformance Traceability Matrix

This matrix maps chapters in this Reference Architecture to corresponding requirements in the GovStack specification, demonstrating how this architecture aligns with GovStack standards.

| GovStack Requirement | Architecture Section | Conformance |
| --- | --- | --- |
| GovStack CFR 6.1 (Development Standards) | Chapter 13: Development Standards | Fully conformant. Adopts Git version control, CI/CD pipelines, test coverage requirements, OpenAPI specs, code quality audits. |
| GovStack CFR 6.2 (Deployment and Operations) | Chapter 14: Deployment and Operations | Fully conformant. Uses OCI containers, Kubernetes orchestration, multi-environment support, backup/restore, rollback capability. |
| GovStack CFR 6.4 (Quality and Performance) | Chapter 16: Quality and Performance | Fully conformant. Specifies WCAG 2.1 AA accessibility, HTML5/CSS3, OpenAPI documentation, SLAs per domain, functional test coverage. |
| GovStack CFR 6.5 (Security) | Chapter 15: Security Architecture | Fully conformant. Implements TLS 1.3 transport security, OAuth 2.0/OIDC authentication, AES-256 data encryption, input/output sanitization, security logging. |
| GovStack Building Block: Identity | Chapter 7.7 (eID Integration) | Conforms. Uses eID for authentication, building on GovStack Identity Building Block. Supports single sign-on. |
| GovStack Building Block: Information Mediator | Chapter 11 (External Integration Platform) | Conforms. Implements Information Mediator pattern for all cross-boundary data exchange, security server, protocol translation. |
| GovStack TADPRA (Data Platform Reference Architecture) | Chapter 8 (Data Platform) | Conforms. Implements TADPRA medallion architecture, three implementation tiers, data governance framework, Data Management Office. |

End of Reference Architecture Document
