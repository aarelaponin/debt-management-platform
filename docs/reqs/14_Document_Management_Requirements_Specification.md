# DOCUMENT MANAGEMENT SYSTEM — SUMMARY REQUIREMENTS SPECIFICATION

**Module 14 — Document Management Services (Platform Component)**

---

# 1. EXECUTIVE SUMMARY

## 1.1 Purpose

Every layer of the architecture assumes a Document Management System without specifying it. The TA Reference Architecture (TARA) builds Domain 4 on "a centralized, searchable repository" with versioning, full-text search, and confidentiality handling (Ch. 10.5). The Generic Case Management Framework's document-and-evidence service (GCMF §3.3-9) is explicitly defined as a thin layer "over the platform DMS". Eleven module specifications levy document requirements — capture, required-document sets, generation from templates, outbound logging, retention, working papers, evidence bundles — against a component that no document in the suite defines. The TA-RDM L2 even carries the canonical data model for it (`08-document-management.yaml`: document, document_version, document_storage, correspondence, communication_template, internal_file, file_movement_log) with no requirements counterpart.

This document closes that gap: a **summary requirements specification** for the Document Management Services (DMS) platform component, compiled from the document-related requirements scattered across Modules 00–13, normalised into one consistent component, and anchored to the TA-RDM document-management domain.

## 1.2 Positioning

The DMS is a **platform service, not a business module**: like the GCMF/CMBB, it sits beneath the building blocks and is consumed by all of them. It owns the *document* lifecycle (capture → classify → store → version → retrieve → retain → dispose) and the *correspondence* lifecycle (generate → dispatch → prove delivery → log). It does **not** own business content or decisions: what a working paper says belongs to Audit; what a notice demands belongs to Debt; whether a decision is signed belongs to the deciding module's authority rules — the DMS provides the storage, generation, integrity, confidentiality, and evidentiary mechanics they all share.

Boundaries with adjacent components: the **CMBB document service** is the case-file *view* over the DMS (case-scoped registration, required-class checks per state); **Module 11** owns the channels through which documents arrive and notifications travel — the DMS receives, stores, and logs; **Module 10** consumes DMS events; the **L3 warehouse** receives document metadata (never content) for analytics.

## 1.3 Document Conventions

Requirement IDs `DMS-FR-nnn`; business rules `BR-DMS-nnn`; MoSCoW per the suite standard; **[Configurable]** markers name their governing parameter. As a summary specification, requirements are stated at component level; channel- and module-specific elaborations remain in their source modules (traced in Appendix A).

---

# 2. BUSINESS CONTEXT — THE IMPLICIT COMPONENT, MADE EXPLICIT

## 2.1 Where the suite already assumes the DMS

| Source | What it assumes (representative requirements) |
|---|---|
| TARA Ch. 10.5 | Centralised searchable store; versioning with user/date tracking; full-text search across historical cases; confidentiality marking with row-level enforcement |
| GCMF §3.1/§3.3 | `case_document` register "over the platform DMS": versions, confidentiality classes, in/out correspondence linkage; immutable evidence record |
| 01 Registration | Required/optional document sets per type (REG-FR-035); capture via upload/scan/counter with constraints (036); storage with retention metadata, retrievable in review and audit (037); missing-documents loop (038); certificate generation from configurable multilingual templates (067); outbound notification/certificate log with delivery status (070) |
| 02 Returns | Supporting attachments/schedules; OCR/structured capture of scanned paper returns (RET-FR-014); per-type document generation workflows (037); evidence on administrative assessment (027) |
| 04 Refunds | Claim evidence; pending-information document loop; decision documents; payment-instruction artefacts |
| 05 Accounting | Posting evidence references; statement and certificate generation; immutable trails |
| 07 Audit | Working papers as versioned case documents; evidence collection with chain of custody; findings/decision documents; QA review records |
| 08 Debt | Notice generation at every escalation step from templates; service/delivery proof; enforcement instruments; agreement documents |
| 09 Appeals (v1.1) | Submissions and hearing records; versioned, exportable litigation case-file bundles (APL-FR-065); decision instruments; precedent linkage to documents |
| 11 Services | Inbound correspondence registration and routing; outbound notification dispatch and delivery status; appointment and request artefacts |
| 12 Additional | ICX confidentiality-walled exchange content with usage restrictions; GSM security instruments; VDP disclosure agreements; TRR acknowledgement artefacts |
| TA-RDM L2 | The full canonical data model (document, version, storage, correspondence, template, internal_file, file_movement_log) awaiting its requirements counterpart |

## 2.2 Objectives

| # | Objective | Measure |
|---|---|---|
| O1 | One store, one truth: every document exists once, referenced everywhere | No module-local document silos; all case files resolve to DMS references |
| O2 | Evidentiary integrity: any document's authenticity, version chain, and access history are provable | Hash-verified content; immutable version and access logs |
| O3 | Generation as configuration: notices, certificates, decisions from versioned multilingual templates | New template/language deployed without code |
| O4 | Provable service of documents: dispatch, channel, and delivery status logged for every legal communication | Outbound log complete; delivery-dependent deadlines computable |
| O5 | Confidentiality by class, including hard walls | ICX-class enforcement verified; access purpose-logged where required |
| O6 | Lawful retention end-to-end, including legal holds | Disposal only per policy; holds override schedules; certificates of destruction |

## 2.3 Scope

**In:** the document repository and object model; capture/ingestion across channels; classification and required-document sets; template-based generation; correspondence (inbound registration, outbound dispatch and delivery proof); versioning and integrity; search and retrieval; confidentiality and access control; retention, archival, legal hold and disposal; physical/internal file tracking; interfaces to CMBB, modules, channels, and e-signature/e-delivery services.

**Out:** business content rules (owned by modules); channel UX (Module 11); e-signature *authority* rules (deciding modules; the DMS integrates the signing service and stores signed artefacts); records-management of non-tax corporate documents (HR, procurement — enterprise systems per Module 00 boundaries); the courts', partners', and taxpayers' own repositories.

---

# 3. FUNCTIONAL REQUIREMENTS

## 3.1 Repository and Object Model

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-001 | The system shall maintain a single logical document repository in which every document is a uniquely identified object with system metadata (id, number, type, direction in/out/internal, status, dates received/created/distributed) per the TA-RDM document entity. | Must | Every stored document carries the canonical metadata set; identifiers are unique and stable. | Anchor: TA-RDM 08 :: document. |
| DMS-FR-002 | The system shall anchor every document to the administration's core references: party (TIN) and, where obligation-scoped, tax account/tax type and tax period; and to its business context via typed references (case type + case id, decision id, return id, etc.). | Must | Documents are retrievable by TIN, obligation, and case; context references are typed, not free text. | TTT discipline; GCMF case_document is a view over this. |
| DMS-FR-003 | The system shall separate document metadata from content storage, supporting configurable storage backends (database, object store, file system) per document class, transparent to consumers. | Must | Content location is a storage reference; backend change does not affect consumers. [Configurable: storage_policy] | TA-RDM document_storage. |
| DMS-FR-004 | The system shall maintain a configurable document-type catalogue defining, per type: classification scheme, required metadata, confidentiality default, retention class, format constraints, and generation template linkage. | Must | New document types are configuration; type rules drive capture, access, and retention behaviour. [Configurable: document_types] | |

## 3.2 Capture and Ingestion

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-005 | The system shall capture documents through all configured channels — self-service upload, officer attach, counter scan, system generation, channel ingestion (e-mail/API where configured) — enforcing per-type format, size, and count constraints. | Must | Capture works per channel; constraints are enforced with clear errors. [Configurable: capture_rules] | Consolidates REG-FR-036, RET-FR-014, 11 intake. |
| DMS-FR-006 | The system shall run configurable ingestion processing on capture: malware scanning (mandatory), format validation, and — where configured per type — OCR/structured extraction of scanned content with extracted data passed to the requesting module. | Must (OCR Should) | Infected/invalid files are rejected and logged; OCR output is delivered to the configured consumer with confidence indicators. [Configurable: ingestion_pipeline] | RET-FR-014 OCR generalised. |
| DMS-FR-007 | The system shall support required-document-set definitions per business context (registration type, claim type, case state) and report set completeness to the owning module, powering missing-document loops. | Must | Completeness is computable per context; the missing-document request/provision loop operates without restarting the business process. [Configurable: required_sets] | REG-FR-035/038 generalised; GCMF pending-information. |
| DMS-FR-008 | The system shall register inbound correspondence (paper-scanned or electronic) with source, channel, receipt date/time, and routing to the responsible unit/case, issuing a registration reference. | Must | Every inbound item is registered, referenced, and routed; receipt date supports statutory timeliness rules. | TA-RDM correspondence; 11 §4.4 intake. |

## 3.3 Generation, Templates and Signature

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-009 | The system shall generate documents (certificates, notices, decisions, statements, agreements, acknowledgements) from a versioned, multilingual template catalogue, merging structured data supplied by the requesting module. | Must | Generation uses the template version valid at generation time; languages per configuration; output is stored as a first-class document with its data snapshot. [Configurable: templates, languages] | REG-FR-067; debt notices; decision instruments. |
| DMS-FR-010 | The system shall integrate an electronic signature/seal service for document classes requiring it, storing the signed artefact and verification data; signing authority rules remain with the requesting module. | Must | Signature-required classes cannot issue unsigned; signatures are verifiable from the stored artefact. [Configurable: signature_classes] | GCMF decisions; WF e-sign requirements. |
| DMS-FR-011 | The system shall support bundle assembly: a structured, versioned compilation of selected case documents into a single exportable artefact (e.g., litigation case file, audit file, EOIR response package). | Must | Bundles are generated from document references, versioned, and exportable; later additions create new bundle versions. | APL-FR-065; ICX responses. |

## 3.4 Outbound Correspondence and Proof of Service

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-012 | The system shall dispatch outbound documents through the channel framework (portal inbox, e-mail, SMS link, postal print-file, e-delivery service) per taxpayer channel preference and per-type service rules. | Must | Dispatch follows configured channel rules; postal output produces print/mail files where configured. [Configurable: dispatch_rules] | Channels owned by Module 11. |
| DMS-FR-013 | The system shall log every outbound communication with recipient, channel, content reference, dispatch time, and delivery status (sent/delivered/read/failed/returned), and expose delivery events to deadline computation (e.g., deemed-service rules). | Must | The outbound log is complete; delivery-dependent deadlines (appeal windows, payment terms) are computable from logged events. [Configurable: deemed_service_rules] | REG-FR-070 generalised; debt notice service; GCMF deadline engine consumer. |
| DMS-FR-014 | The system shall handle failed and returned correspondence with configurable retry/alternate-channel/exception-case behaviour. | Should | Failures follow configuration; persistent failures raise exception cases to the owning module. [Configurable: failure_handling] | |

## 3.5 Versioning, Integrity and Evidence

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-015 | The system shall version documents: stored content is immutable; changes create versions with author, time, and reason; the chain is complete and navigable. | Must | No in-place modification is possible; version history is reconstructable. | TA-RDM document_version; TARA 10.5. |
| DMS-FR-016 | The system shall compute and store a cryptographic hash per content object at capture/generation and support integrity verification on demand. | Must | Any content object can be verified against its hash; mismatches are alertable events. | Evidentiary integrity (O2). |
| DMS-FR-017 | The system shall log every access to confidential-class documents (who, when, and — for restricted classes — recorded purpose), tamper-evidently. | Must | Access logs are complete for configured classes and themselves protected. [Configurable: access_logging_classes] | ICX treaty review requirement. |
| DMS-FR-018 | The system shall support chain-of-custody metadata for evidence-class documents (source, collector, collection time/method, transfer history). | Should | Evidence documents carry custody records suitable for audit findings and litigation bundles. | Audit/litigation evidence. |

## 3.6 Search and Retrieval

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-019 | The system shall provide metadata search across all index dimensions (TIN, obligation, case, type, dates, status, direction) with access-control-filtered results. | Must | Searches return only what the searcher may see, within the performance envelope. | |
| DMS-FR-020 | The system shall provide full-text search over textual content (native and OCR-extracted) for authorised roles, scoped by confidentiality class. | Should | FTS spans historical content; restricted classes are excluded for unauthorised roles. | TARA 10.5 cross-case search. |
| DMS-FR-021 | The system shall present a complete case-file view per case (all documents, versions, correspondence, bundle membership) as the standard officer interface, consumed by the CMBB document service. | Must | The case file is complete, ordered, and exportable; it is a view over the repository, not a copy. | GCMF §3.3-9. |

## 3.7 Confidentiality and Access Control

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-022 | The system shall enforce a configurable confidentiality classification per document (default by type, override by authorised action), controlling visibility at document and search level. | Must | Classification drives access uniformly across retrieval paths. [Configurable: confidentiality_classes] | |
| DMS-FR-023 | The system shall support hard-wall classes whose content is accessible exclusively to named role groups regardless of case or unit access (e.g., exchange-of-information content), with purpose-recorded access. | Must | Wall classes are invisible and irretrievable outside the wall; access is purpose-logged. | ICX wall (12 §4.2). |
| DMS-FR-024 | The system shall propagate restrictions to derivatives: versions, OCR text, search indexes, and bundles inherit the most restrictive contributing class. | Must | No derivative leaks a restricted original. | |

## 3.8 Retention, Archival, Legal Hold and Disposal

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-025 | The system shall apply configurable retention schedules per document class and business context (e.g., taxpayer-record classes retained per registration retention rules), computing disposal eligibility dates. | Must | Every document has a derivable retention end; schedule changes re-compute prospectively. [Configurable: retention_schedules] | REG-FR-037/053 generalised. |
| DMS-FR-026 | The system shall support legal/administrative holds (litigation, audit, investigation) that suspend disposal for the held scope until released, with hold history retained. | Must | Held documents cannot be disposed of; holds and releases are recorded decisions. | Litigation/ICX necessity. |
| DMS-FR-027 | The system shall support archival tiering (online → archive storage) per policy, preserving retrievability and integrity verification of archived content. | Should | Archived documents remain retrievable within configured service levels; hashes still verify. [Configurable: archival_policy] | |
| DMS-FR-028 | The system shall execute disposal as an authorised, logged process producing a destruction record (metadata retained, content destroyed), excluding held and walled exceptions per policy. | Must | Disposal only via the authorised process; destruction records are permanent. | O6. |

## 3.9 Physical and Internal Files

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-029 | The system shall register physical/internal files (paper dossiers) with identifying metadata, location, and linkage to the electronic record. | Should | Physical files are registered and findable; hybrid (paper+electronic) case files are coherent. | TA-RDM internal_file. |
| DMS-FR-030 | The system shall track physical file movements (checkout, transfer, return) with custodian and timestamps. | Could | Movement history answers "where is the file and who has it". | TA-RDM file_movement_log; Archetype A/C relevance. |

## 3.10 Interfaces and Events

| ID | Description | Priority | Acceptance Criteria | Notes / Config |
|----|-------------|----------|---------------------|----------------|
| DMS-FR-031 | The system shall expose a uniform document service API to all building blocks: store, retrieve, generate, register-inbound, dispatch, bundle, hold, and query — the single path for document operations (no module-local stores). | Must | All modules and the CMBB operate exclusively through the API; operations are authorised per the caller's context. | O1. |
| DMS-FR-032 | The system shall emit typed events (captured, generated, dispatched, delivered, accessed-restricted, hold-applied, disposed) with standard dimensions for consumption by the GCMF deadline engine, Module 10 KPIs, and the L3 warehouse (metadata only). | Must | Events are emitted reliably; content never leaves through the event stream. | |

---

# 4. NON-FUNCTIONAL REQUIREMENTS (component-specific additions to the suite baseline)

| ID | Requirement | Priority |
|---|---|---|
| DMS-NFR-01 | Storage shall scale to the configured horizon (reference: tens of millions of documents, multi-TB content) without architecture change; capture and retrieval shall meet the suite performance envelope at that scale. | Must |
| DMS-NFR-02 | Generated documents of record (notices, certificates, decisions) shall be produced in a long-term preservation format (PDF/A or equivalent) in addition to any working format. | Must |
| DMS-NFR-03 | Content at rest shall be encrypted; wall-class content shall be separately key-scoped such that platform administrators without wall roles cannot read it. | Must |
| DMS-NFR-04 | The repository shall be independently restorable: metadata and content backups are consistent, and integrity hashes verify after restore. | Must |
| DMS-NFR-05 | Document metadata shall be exportable in standard formats (per the suite's portability commitments) without vendor tooling. | Must |

---

# 5. REALISATION NOTE (Joget DX9)

The DMS is realised as a platform service consumed by all building blocks: TA-RDM-aligned metadata entities as Joget forms/tables (`document`, `document_version`, `correspondence`, `template` catalogues as MD forms); content in a configurable object store behind a storage-reference convention (DMS-FR-003) rather than in-table blobs; generation via the template catalogue + JasperReports for instruments of record (joget-jasper-report); and an estimated **4-plugin budget**: IngestionPipeline (scan/validate/OCR hook), DispatchConnector (channel framework + delivery status), IntegrityService (hashing/verification, hold enforcement), and SearchIndexer (FTS, class-scoped). The CMBB's document service (GCMF §3.3-9) becomes a thin case-scoped façade over DMS-FR-031, confirming the layering: DMS stores and proves; CMBB contextualises; modules mean.

---

# APPENDIX A — SOURCE TRACEABILITY (compiled-from evidence)

| DMS section | Compiled from |
|---|---|
| 3.1 Repository/model | TA-RDM 08-document-management (document, document_storage); GCMF §3.1 case_document; TARA 10.5 |
| 3.2 Capture | REG-FR-035/036/038; RET-FR-014; 11 §4.4 intake; counter/scan channels (02/11) |
| 3.3 Generation/signature | REG-FR-067; 08 notice generation; decision instruments across 04/07/09/12; WF.010–012 e-sign; APL-FR-065 bundles |
| 3.4 Correspondence/service | REG-FR-070; 08 notice service/delivery; 11 outbound dispatch; deemed-service deadline needs (09/08) |
| 3.5 Integrity/evidence | REF-FR-032-pattern immutability; 07 working papers/evidence; ICX review requirements; TARA versioning |
| 3.6 Search/case file | TARA 10.5 FTS; GCMF case-file view |
| 3.7 Confidentiality | TARA row-level security; 12 §4.2 ICX wall (ICX-FR-002/003) |
| 3.8 Retention/hold/disposal | REG-FR-037/053; litigation/audit hold necessity (09 v1.1, 07); suite data-protection commitments (00 §10) |
| 3.9 Physical files | TA-RDM internal_file, file_movement_log; Archetype A/C operating reality |
| 3.10 Interfaces/events | GCMF §3.3-9/§3.5; Module 10 semantic layer; 00 §9 interoperability |

*End of Module 14 — Document Management Services Summary Requirements Specification.*
