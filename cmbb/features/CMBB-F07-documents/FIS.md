# FIS — CMBB-F07 — Document Register + MayanConnector + ADG + Postal Tracking
Status: Accepted (T-07.1..5 5/5 PASS on jdx9 + live Mayan, 2026-06-12; full regression F02-F06 green)
CAD ref: CAD-CMBB §7 row F07

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.5) | Realised by | Test |
|---|---|---|---|
| WF-FR-017 (ADG) | "Documents generated in PDF format. Templates support: merge fields, conditional sections, MTCA letterhead, digital signatures (where applicable). Generated documents automatically attached to case record. Batch generation supported for bulk operations (e.g., monthly demand notices)." | F-cmDocGen (postProcessor → MayanConnector GENERATE): mdTemplate render → PDF (com.lowagie, bundled in wflow-core) → cmDoc row (auto-attach) + Mayan upload; bulk by caseType filter; *conditional sections + signatures = recorded refinement (Jasper letters at DMBB notices)* | T-07.2, T-07.5 |
| WF-FR-018 (EDS) | "Documents stored with: metadata (type, date, case ref, TIN), version number, upload user. Maximum file size: 25 MB. Supported formats: PDF, DOCX, XLSX, JPG, PNG. Full-text search across document metadata. Documents accessible from case view and taxpayer view." | F-cmDoc (FileUpload 25MB/format-filtered; docClass, version, uploadedBy) + postProcessor → MayanConnector PUSH (binary → Mayan, P15; register keeps mayanDocId) + DL-list_cmDoc (case/TIN filters); Mayan provides metadata indexing/search (stack live) | T-07.1 |
| WF-FR-019 (postal) | "Postal dispatch recorded in notification log and case history. Delivery confirmation updateable manually or via postal service integration. Returned mail flagged on taxpayer record. Address verification task auto-created for returned mail." | F-cmPostal (links cmNotif QUEUED_PRINT rows; manual status updates) + MayanConnector POSTAL: RETURNED → cmTask(ADDRESS_VERIFICATION, OPEN) + cmEvent + NOTIF_PENDING + cmCase.addressFlag | T-07.4 |
| (F02 deferred check) mmDocReq per state | TransitionGuard PRE_CLOSE doc requirement | cmDoc now exists → guard check live (updateSchema added — DX9-DELTAS tx-poison rule) | T-07.3 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| BR-DM-060 (categorised, ≤25MB, format whitelist, immutable once attached) | FileUpload config (25600KB, .pdf;.docx;.xlsx;.jpg;.png) + docClass required; immutability = no edit path for REGISTERED rows (UI) + Mayan versioning holds the binary |
| Required documents per state (mmDocReq) | TransitionGuard PRE_CLOSE counts cmDoc per docClass (F02 logic, now active) |

## 3. Design decisions & assumptions
1. **A1 — P15 split**: binaries live in Mayan (REST v4, token obtained at runtime from MAYAN_URL/MAYAN_USER/MAYAN_PASSWORD env; DEV autoadmin), register lives in Joget (cmDoc). Upload path: officer saves cmDoc with file → postProcessor ships to Mayan, writes back mayanDocId, status REGISTERED (FAILED kept for retry — connector failure must not lose the register row).
2. **A2 — PDF engine**: `com.lowagie` classes ship inside wflow-core (verified in 9.0.7 jar) — zero new dependencies; simple letter layout (letterhead line + rendered body). Pixel-perfect letters = joget-jasper-report at DMBB notice templates (recorded).
3. **A3 — Mayan upload**: try single-shot `POST /documents/upload/` (multipart document_type_id+file); fallback two-step create+file. Document type = "DM Case Document" (setup_mayan.py catalogue).
4. **A4 — postal integration**: manual status updates now; postal-service API = deployment integration behind the same form (recorded). Returned-mail taxpayer flag = cmCase.addressFlag (field amendment) — taxpayer-record flag proper lands with DMBB taxpayer surface.
5. **A5 — full-text search**: metadata search via DL filters + Mayan's own indexing/OCR for content (already configured in stack). 
6. No blockers — G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Document classes | mdDocClass rows (F01) | seed set | WF-FR-018 |
| Max size / formats | FileUpload config (regenerable; P "configurable" per BR-DM-060) | 25MB; pdf/docx/xlsx/jpg/png | BR-DM-060 |
| Mayan endpoint/credentials | env MAYAN_URL/MAYAN_USER/MAYAN_PASSWORD | localhost:8880 / autoadmin | P15 |
| Doc requirements per state | mmDocReq rows (F01) | none | WF-FR-018/guard |

## 5. Generation order
1. gen_forms.py: FileUpload element support → 2. forms F-cmDoc (postProcessor PUSH), F-cmDocGen (GENERATE), F-cmPostal (POSTAL) + cmCase amendment (addressFlag) → 3. datalists (companions + DL-list_cmDoc with case/TIN filters) → 4. PL-MayanConnector + DocumentService (+ MayanClient seam; unit tests with simulated client) + TransitionGuard PreClose updateSchema fix → 5. UV delta (Documents category) → 6. redeploy + T-07.x (live Mayan) + regression.
