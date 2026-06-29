# FIS — DMBB-F07 — Enforcement actions (recovery instruments + agents + fees)
Status: Accepted (T-16.1..8 8/8 PASS on jdx9, 2026-06-13; 97/97 unit; full regression run_t02..t15 green). DMBB slice S4 (F07) — opens S4.
CAD ref: CAD-DMBB §7 row F07 (recovery_action + instrument execution + GarnishingConnector stub + costs + agents); DM-FR-029…038, BR-DM-031…035 / 044…046. Builds on F05 (enforcement gate + ENFORCEMENT_TRIGGERED), F06 (holds), CMBB-F08 (holds/decisions), F01 (mdInstrument config carries all 14 Malta instruments).

## 0. Approach — full breadth, config-driven (one engine, one action record)
The 14 Malta recovery instruments already live as **mdInstrument** config (F01): each row carries
`minCategory`, `executionMode` (ADMINISTRATIVE / FIELD / JUDICIAL), `authorityLevel`
(OFFICER / SUPERVISOR / DIRECTOR), `proportionalityMinAmount`, `docTemplate`, `costRecorded`,
`disputeHoldSensitive`. F07 therefore models **all** action types (DM-FR-029…038) through ONE
generic action record **dmAction** + ONE engine **EnforcementActionEngine** (13th) that branches on
the instrument's `executionMode` — not 14 bespoke engines. The distinct lifecycles are captured by
the execution-mode branch plus a few type-specific fields (auction proceeds, external/court refs).

Five engine modes (a single new plugin, mode on the trigger form / postProcessor):
1. **INITIATE** (`dmAction` create) — proportional gating (BR-DM-031: category rank ≥ `minCategory`
   AND amount ≥ `proportionalityMinAmount`), enforcement eligibility (the F05 contract: no full-amount
   objection, and — when the instrument is `disputeHoldSensitive` — no active ENFORCEMENT_SUPPRESS
   hold), garnish cap (BR-DM-033: amount ≤ debt + legal fees), document generation (docRef from
   `docTemplate`), execution branch by `executionMode`:
   - **ADMINISTRATIVE** → record the administrative measure; **BANK_GARNISH** additionally runs the
     **GarnishingConnector** stub (DM-FR-029, BR-DM-034): transmit a freezing request, record the bank
     response (SUCCESS / PARTIAL / FAILURE), and on SUCCESS/PARTIAL post the garnished amount as an
     informational recovery writeback (BR-DM-035, ≤ cap); **PUBLISH_NAME** adds a `dmDebtorPub` entry
     (DM-FR-030); **LIEN / THIRD_PARTY / REFUND_INTERCEPT** record the register/order request + confirmation.
   - **JUDICIAL** → generate the court/legal submission, record SUBMITTED, track outcome (PASSPORT
     departure prohibition DM-FR-033, BANKRUPTCY demand DM-FR-035, JUDICIAL_LETTER).
   - **FIELD** → assign field execution (VISIT, PROPERTY_SEIZURE+auction DM-FR-034, AGENT_WARRANT routes
     to the agent-appointment path).
   Post the legal fee (DM-FR-038) when `costRecorded` — a separate **dmCharge** record linked to the
   case (chargeType LEGAL_FEE), informational (ledger posting is external, SAD I-4). Emit
   `ENFORCEMENT_ACTION_INITIATED` (+ instrument-specific events) or `ENFORCEMENT_ACTION_BLOCKED`.
2. **AGENT_APPOINT** (`dmAgent` create, DM-FR-037) — BR-DM-044 (category C4–C5), appointment fee from
   `mdAgentFee`, status APPOINTED, emit `AGENT_APPOINTED` + a LEGAL_FEE/appointment dmCharge.
3. **AGENT_REPORT** (`dmAgentRpt` create, BR-DM-046) — record recovered amount, compute commission
   (BR-DM-045: recovered × `commissionRate`), accumulate `recoveredTotal`/`commissionTotal` on the
   appointment, post recovery writeback, emit `AGENT_REPORTED`.
4. **SWEEP** (`cmEnfActionRun`, sub-mode) — PUBLISH (generate the debtor list for eligible cases
   DM-FR-030: category ≥ C3 past final demand, no active objection/instalment, per `mdPublishRule`),
   AGENT_ALERT (BR-DM-046: agents with days-since-last-report > interval → `AGENT_OVERDUE_ALERT`),
   RELEASE (on resolved cases: lift liens/garnish and unpublish, DM-FR-030/031 release), CONFIRM
   (poll pending external/judicial actions). `asOf` time-travel like F04/F06.

## 1. Traceability (acceptance criteria verbatim from docs/reqs/08-Debt_Management-Requirements_v1.1.md)
| FR / BR | AC (verbatim, abbreviated) | Realised by | Test |
|---|---|---|---|
| DM-FR-029 | "Officer or automated workflow initiates garnish → system generates freezing request with: TIN, bank details, debt amount, legal reference → request transmitted via web service → bank response (success/failure/partial) recorded → garnished amounts posted to taxpayer account upon receipt." | dmAction(BANK_GARNISH) → EnforcementActionEngine INITIATE → GarnishingConnector stub (transmit + response) → recovered writeback | T-16.1 |
| BR-DM-033 | "Bank account garnishing is available for debts in categories C3–C5 that have been escalated for enforcement. The garnishing amount shall not exceed the outstanding debt plus applicable legal fees." | proportional gate (C3–C5 via minCategory) + amount cap (≤ debt + legal fees) | T-16.1, T-16.2 |
| BR-DM-034 | "Garnishing requests are transmitted via secure web services… Request format includes: TIN, taxpayer name, bank details, amount, legal reference… Response expected within configurable timeout." | GarnishingConnector builds the request payload + records response status; transport stubbed (DEV), timeout config recorded | T-16.1 |
| BR-DM-035 | "Upon receipt of garnished funds from a bank, the amount is posted to the taxpayer's account… using the standard payment allocation rules (… managed externally)." | recovered amount recorded on dmAction + GARNISH_CONFIRMED event; allocation external (D-SAD, informational writeback) | T-16.1 |
| BR-DM-031 | "Available enforcement actions are determined by debt category per the proportional enforcement matrix (Table 4). Higher categories unlock additional enforcement tools." | mdInstrument.minCategory + proportionalityMinAmount gate; ineligible instrument REJECTED | T-16.2 |
| DM-FR-033/034/035 | departure prohibition / property seizure+auction / bankruptcy demand — "Legal document generated… submitted to relevant authority → confirmation recorded… Status tracked on case timeline." | JUDICIAL/FIELD execution branch: doc generated, SUBMITTED, outcome tracked; auction proceeds field | T-16.4 |
| DM-FR-037 / BR-DM-044 | "Agent appointed with defined scope → agent activities logged → recovered amounts posted… agent fees/commission calculated and tracked separately." / appointment requires C4–C5, SDO approval, approved register | dmAgent appoint (AGENT_APPOINT, C4–C5 gate) + dmAgentRpt (AGENT_REPORT) | T-16.5 |
| BR-DM-045 | "Agent fees are calculated per a configurable schedule: fixed appointment fee + percentage commission on recovered amounts." | mdAgentFee (appointmentFee + commissionRate); commission = recovered × rate accumulated | T-16.5 |
| BR-DM-046 | "Agents must report activities within configurable reporting intervals… Non-reporting triggers supervisor alert." | cmEnfActionRun AGENT_ALERT sweep → AGENT_OVERDUE_ALERT | T-16.6 |
| DM-FR-038 / BR-DM-010 | "Legal fees per enforcement action posted to taxpayer account with: fee type, amount, associated case ID, date posted. Fees visible… under separate charge category." | dmCharge (chargeType LEGAL_FEE) per costRecorded instrument, linked caseId, LEGAL_FEE_POSTED event | T-16.1, T-16.7 |
| DM-FR-030 | "Publishing of debtor names… for debt categories C3–C5 where configured criteria are met. Publication list generated automatically. Debtor removed from list upon debt resolution." | cmEnfActionRun PUBLISH/RELEASE sweep → dmDebtorPub (publishedDate/removedDate), per mdPublishRule | T-16.8 |

## 2. Design decisions
1. **A1 — one action record, executionMode branch.** Full breadth without 14 engines: dmAction is
   instrument-agnostic; `EnforcementActionService.execute()` switches on `mdInstrument.executionMode`.
   New country instruments are config (mdInstrument rows), not code — P14.
2. **A2 — F07 executes what F05 authorises.** F05's gate decides *whether* an enforcement step may fire
   (ENFORCEMENT_TRIGGERED). F07 is the *execution* surface: an officer (or the triggered step) creates a
   dmAction; the engine re-checks the same eligibility (objection + ENFORCEMENT_SUPPRESS hold, the latter
   only when `disputeHoldSensitive`) so an action can never bypass a live instalment/objection hold.
3. **A3 — connectors are recorded stubs (DEV).** Bank WS (DM-FR-029), asset/real-estate registers
   (DM-FR-031), courts (DM-FR-033/035) and the national debt registry (DM-FR-030) are external systems
   not present in DEV. F07 records the request payload + a simulated/pending response and emits the
   audit event; live transport is a deployment-time binding (recorded in §3). GarnishingConnector is the
   reference implementation of the stub contract (success/partial/failure).
4. **A4 — postings are informational writebacks (SAD I-4).** Garnished amounts (BR-DM-035), agent
   recoveries (DM-FR-037) and legal fees (DM-FR-038) are *recorded* on the DMBB side (dmAction.recovered,
   dmCharge) and emit events; the authoritative ledger posting/allocation stays in STA/ORS. DMBB never
   recomputes the balance (D-SAD-01/04).
5. **A5 — proportional matrix = data.** BR-DM-031 Table 4 is realised by `mdInstrument.minCategory` +
   `proportionalityMinAmount` (already seeded F01); the engine reads them, no hard-coded matrix.

## 3. Deferred / assumptions (recorded, not dropped)
- Live external transport for every connector (bank WS protocol per bank, court e-filing, asset-register
  APIs, national debt registry) — DEV records request+response; binding at deployment (config: endpoint,
  TLS, timeout BR-DM-034 default 30s sync / 48h async).
- Auction full lifecycle (schedule → bid → sale → distribution, DM-FR-034) is modelled to SOLD +
  saleProceeds + distribution event; the bidding subsystem is out of scope (recorded).
- Temporary business closure (DM-FR-036, "Could") and lien-on-assets register *query* UI (DM-FR-031) are
  represented as instrument rows + generic action records; their bespoke screens are a later console feature.
- STA debtor's-debtor posting pattern (DM-FR-032 → BR-STA-044) is recorded as a THIRD_PARTY action +
  event; the payer-side/transfer-to posting is the STA engine's (external).
- Legal-fee *ledger* posting is external; DMBB records dmCharge (the hand-off record).

## 4. Configurables → carriers
- Proportional matrix → `mdInstrument.minCategory`, `proportionalityMinAmount` (F01).
- Legal-fee schedule (DM-FR-038/BR-DM-010) → `mdLegalFee` (instrument → feeAmount, chargeCategory).
- Agent fee schedule (BR-DM-045) → `mdAgentFee` (category → appointmentFee, commissionRate).
- Publication criteria (DM-FR-030) → `mdPublishRule` (minAmount, minAgeDays, priorNoticeRequired, registry).
- Agent reporting interval (BR-DM-046) → `mdAgentFee.reportingIntervalDays` (or engine DEV default 7).
- Bank WS timeout (BR-DM-034) → engine property (DEV default), recorded.

## 5. Generation order
1. gen_forms: F-dmAction, F-dmAgent, F-dmAgentRpt (child), F-dmCharge, F-dmDebtorPub, F-cmEnfActionRun,
   + MD carriers F-mdLegalFee, F-mdAgentFee, F-mdPublishRule → dmbb/generated/forms.
2. gen_datalists (forms) → companions.
3. gen_userview: ALL dmbb UV-deltas (F01..F07) → dmbbConsole gains **Enforcement** + **Enforcement config** categories.
4. Plugin: EnforcementActionService + GarnishingConnector + EnforcementActionEngine (modes INITIATE /
   AGENT_APPOINT / AGENT_REPORT / SWEEP) + enforcementActionEngine.json; Activator 12→13; unit tests. Build.
5. Deploy cmbb (JAR) + dmbb; seed mdLegalFee/mdAgentFee/mdPublishRule (MLT) via API-dmbb-data. run_t16 + full regression run_t02..t15.
