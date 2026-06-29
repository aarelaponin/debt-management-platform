# FIS — CMBB-F06 — Notification Dispatcher + Templates + Alerts + Outbound Log
Status: Accepted (T-06.1..5 7/7 PASS on jdx9, 2026-06-12; full regression F02-F05 green)
CAD ref: CAD-CMBB §7 row F06

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.4) | Realised by | Test |
|---|---|---|---|
| WF-FR-013 | "Each notification type has configurable default channel(s). Taxpayer channel preference respected where available. Delivery status tracked per channel: sent, delivered, failed, bounced. Failed delivery triggers fallback to next configured channel. Postal letter generates print-ready PDF queued for dispatch." | mmNotifRule (channelDefault list, respectPreference) + PL-NotificationDispatcher channel adapters (DEV: simulated transport with status; LETTER queues cmNotif status=QUEUED_PRINT — PDF rendering = F07 ADG) + fallback loop | T-06.3 |
| WF-FR-014 | "Templates configurable by administrator with: merge fields (TIN, name, amounts, dates), static text, MTCA branding. Each template available in Maltese and English. Template versioning with effective dating. Preview function before deployment." | F-mdTemplate (code+language+version+effectiveFrom; subject/body with #field# merge syntax) + dispatcher render; *preview = console form view of rendered sample (UI); branding = template content* | T-06.2 |
| WF-FR-015 | "In-app notification panel shows unread count badge. Alerts categorised by type and priority. Alert preferences configurable per officer (which alerts, which channels). Read/unread status tracked. Alert history retained for 90 days minimum." | F-cmAlert (recipient, alertType, read flag) + DL-list_cmAlert_my (#currentUser, unread first) + unread count query; *per-officer preferences = mdOfficerProfile.alertPrefs (carrier added); retention = ops policy note* | T-06.1, T-06.5 |
| WF-FR-016 | "Notification log entry created for every outbound communication. Failed notifications flagged for manual review. Notification linked to originating case. Log searchable by: TIN, date range, channel, status. Retention period: 7 years minimum." | F-cmNotif (recipient, channel, summary, status, caseId, eventId) + DL-list_cmNotif (filters TIN/channel/status) + linkage to cmEvent | T-06.1, T-06.4 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| Idempotent dispatch (no event mutated, no duplicate sends) | Dispatcher processes NOTIF_PENDING events lacking a cmNotif/cmAlert row with eventId=event.id |
| Correspondence suppression (E1) | Before taxpayer dispatch: active cmHold with scope CORRESPONDENCE_SUPPRESS blocks send (status=SUPPRESSED); table absent pre-F08 ⇒ check skipped, recorded |
| Consolidation per TIN (E3, mmNotifRule.consolidationMode) | Deferred to DMBB notice volume (single-event dispatch now); recorded, not silent |

## 3. Design decisions & assumptions
1. **A1 — trigger**: cmDispatchRun form postProcessor → DISPATCH mode (same pattern as F05 sweep; cron-able, auditable, result written back).
2. **A2 — two notification classes from one event stream**: NOTIF_PENDING with `recipient` payload → internal alert (cmAlert + optional EMAIL); rules in mmNotifRule matching (caseType, eventType) → taxpayer notification (template render → cmNotif per channel).
3. **A3 — transport adapters are DEV-simulated**: SENT for EMAIL/SMS/PORTAL, QUEUED_PRINT for LETTER, FAILED for the SIMFAIL test channel (drives the fallback test). Real SMTP/SMS gateways are deployment config behind the same adapter seam (plugin properties) — interface is the deliverable, recorded.
4. **A4 — merge fields**: `#tin#, #taxpayerName#, #caseRef#, #amountAtStake#, #date#` resolved from the case row; unknown fields left visible (template QA aid).
5. **A5 — language selection**: 'en' default; per-taxpayer language arrives with DMBB taxpayer data (preference hook documented). Template lookup = (code, language, effectiveFrom<=now, max version).
6. **A6 — alert preferences carrier**: mdOfficerProfile gains `alertPrefs` (comma list of suppressed alertTypes) — amendment.
7. No blockers — G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Channel(s) per notification type + fallback order | mmNotifRule.channelDefault (comma list) | EMAIL | WF-FR-013 |
| Templates (ML/EN, versioned, effective-dated) | mdTemplate rows | TEST seed pair | WF-FR-014 |
| Officer alert preferences | mdOfficerProfile.alertPrefs | (none suppressed) | WF-FR-015 |
| Retention periods (90d alerts / 7y log) | ops policy (no auto-purge implemented — recorded) | — | WF-FR-015/016 |

## 5. Generation order
1. forms/F-mdTemplate, F-cmNotif, F-cmAlert, F-cmDispatchRun (postProcessor) + mdOfficerProfile amendment (alertPrefs) → 2. datalists (companions + DL-list_cmAlert_my JDBC + DL-list_cmNotif filters) → 3. PL-NotificationDispatcher (bundle) + unit tests → 4. UV delta (Notifications category) → 5. seeds (mdTemplate en/mt pair, mmNotifRule TEST rule) → 6. redeploy + T-06.x + regression.
