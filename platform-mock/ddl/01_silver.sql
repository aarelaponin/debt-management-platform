-- Mock data platform: silver layer (legacy-equivalent grain, per Legacy-DB-Findings)
-- Loaded on first container start (docker-entrypoint-initdb.d) or via load_seed.sh.

CREATE DATABASE IF NOT EXISTS mtca_ors;
CREATE DATABASE IF NOT EXISTS sta_v1;

-- Taxpayer register (irdnew:taxpayer-shaped)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_taxpayer
(
    tin           String,                                    -- e.g. 123456M (legacy format)
    tpform        Enum8('I' = 1, 'C' = 2),                   -- individual / company
    name          String,
    reg_date      Date,
    status        Enum8('ACTIVE' = 1, 'DECEASED' = 2, 'DEREGISTERED' = 3),
    locality      String,
    email         String,
    phone         String,
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY tin;

-- Assessments (liability-creating events; self/authority/default per Module 05 §9.1)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_assessments
(
    assessment_id String,
    tin           String,
    tax_type      LowCardinality(String),
    yofa          UInt16,                                    -- year of assessment
    period        String,                                    -- '2024' or '2024-Q1'
    kind          Enum8('SELF' = 1, 'AUTHORITY' = 2, 'DEFAULT' = 3),
    caution_flag  UInt8 DEFAULT 0,
    amount        Decimal(14, 2),
    due_date      Date,
    assessed_at   DateTime,
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (tin, tax_type, yofa);

-- Accounting transaction log (ars:tplg-shaped; signed amounts: +liability, -payment/credit)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_transactions
(
    tin           String,
    tax_type      LowCardinality(String),
    yofa          UInt16,
    period        String,
    trcd          UInt16,                                    -- legacy transaction code
    sbcd          LowCardinality(String),
    amount        Decimal(14, 2),
    txn_date      Date,
    source_ref    String,
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (tin, tax_type, yofa, txn_date, trcd);

-- Receipts (ars:lcph/lcpd-shaped; stus 8/9 = rejected/deleted)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_receipts
(
    rcno          UInt64,
    rcdt          Date,
    stus          UInt8,
    tin           String,                                    -- '' when unidentified
    ptyp          LowCardinality(String),
    yerr          UInt16,
    amount        Decimal(14, 2),
    channel       LowCardinality(String),
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (rcdt, rcno);

-- Objections / disputes (irdnew:objectionref-shaped; drives disputed-amount exclusion)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_objections
(
    objection_id  String,
    tin           String,
    tax_type      LowCardinality(String),
    yofa          UInt16,
    amount        Decimal(14, 2),
    status        Enum8('OPEN' = 1, 'UPHELD' = 2, 'REJECTED' = 3),
    filed_date    Date,
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (tin, tax_type, yofa);

-- Credit / suspense entries (Module 05 §9.1 CreditAccount/SuspenseEntry, STA-FR-046)
CREATE TABLE IF NOT EXISTS mtca_ors.silver_suspense
(
    entry_id      String,
    tin           String,                                    -- '' = unidentified
    kind          Enum8('UNIDENTIFIED' = 1, 'CREDIT' = 2, 'SUSPENDED' = 3),
    amount        Decimal(14, 2),
    received_date Date,
    reference     String,
    _extracted_at DateTime DEFAULT now()
) ENGINE = MergeTree ORDER BY (kind, received_date);

-- ASA semantic map: legacy code -> accounting meaning. THE adaptor-as-data (D-SAD-11/12).
CREATE TABLE IF NOT EXISTS mtca_ors.asa_trcd_map
(
    trcd       UInt16,
    sbcd       LowCardinality(String),
    semantic   Enum8('LIABILITY' = 1, 'PAYMENT' = 2, 'INTEREST' = 3, 'PENALTY' = 4,
                     'ADJUSTMENT' = 5, 'JOURNAL' = 6, 'WRITEOFF' = 7),
    component  Enum8('P' = 1, 'I' = 2, 'C' = 3),             -- PA / IA / PCA bucket
    confidence Enum8('VERIFIED' = 1, 'APPROXIMATED' = 2)
) ENGINE = MergeTree ORDER BY (trcd, sbcd);

-- Map rows: 10 assessment posting; 230/231 estimated assessments; 20/G flushed TRIS payment;
-- 60 interest accrual (statement-run derived); 70 penalty; 47/49 receipt adjustments; 98 journal; 99 write-off.
TRUNCATE TABLE mtca_ors.asa_trcd_map;
INSERT INTO mtca_ors.asa_trcd_map VALUES
(10,  '',  'LIABILITY',  'P', 'VERIFIED'),
(230, '',  'LIABILITY',  'P', 'APPROXIMATED'),
(231, '',  'LIABILITY',  'P', 'APPROXIMATED'),
(20,  'G', 'PAYMENT',    'P', 'VERIFIED'),
(20,  '',  'PAYMENT',    'P', 'VERIFIED'),
(60,  '',  'INTEREST',   'I', 'APPROXIMATED'),
(70,  '',  'PENALTY',    'C', 'VERIFIED'),
(47,  'A', 'ADJUSTMENT', 'P', 'APPROXIMATED'),
(49,  'X', 'ADJUSTMENT', 'P', 'APPROXIMATED'),
(98,  '',  'JOURNAL',    'P', 'APPROXIMATED'),
(99,  '',  'WRITEOFF',   'P', 'VERIFIED');

-- Outcome writeback landing (Blueprint §14.2.2; idempotent on (case_id, outcome_date))
CREATE TABLE IF NOT EXISTS mtca_ors.fact_case_outcomes
(
    case_id      String,
    outcome_date Date,
    tin          String,
    outcome_type LowCardinality(String),                     -- resolution/action/state-transition
    outcome_code LowCardinality(String),
    amount       Decimal(14, 2) DEFAULT 0,
    officer      String,
    detail       String,
    received_at  DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(received_at) ORDER BY (case_id, outcome_date, outcome_type, outcome_code);
