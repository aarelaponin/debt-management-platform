-- Intermediate layer (Blueprint Ch.8 int_* pattern; ARMS dbt_arms precedent).
-- Materialized tables owning the expensive/reused joins; rebuilt by load_seed.sh
-- (05_int_rebuild.sql) after every silver load. sta_v1 views read ONLY from here
-- (+ small dimension lookups), giving one join, one test boundary, clean lineage.

-- 1. Postings: silver_transactions ⋈ asa_trcd_map, semantics resolved once.
CREATE TABLE IF NOT EXISTS mtca_ors.int_accounting__postings
(
    tin            String,
    tax_type       LowCardinality(String),
    yofa           UInt16,
    period         String,
    txn_date       Date,
    trcd           UInt16,
    sbcd           LowCardinality(String),
    semantic       LowCardinality(String),
    component      LowCardinality(String),                 -- P / I / C
    asa_confidence LowCardinality(String),                 -- VERIFIED / APPROXIMATED
    amount         Decimal(14, 2),
    source_ref     String
) ENGINE = MergeTree ORDER BY (tin, tax_type, yofa, txn_date, trcd);

-- 2. Period balances: one row per TIN x tax type x year, decomposed + due-dated.
CREATE TABLE IF NOT EXISTS mtca_ors.int_accounting__period_balances
(
    tin              String,
    tax_type         LowCardinality(String),
    yofa             UInt16,
    balance          Decimal(14, 2),
    pa_amount        Decimal(14, 2),
    ia_amount        Decimal(14, 2),
    pca_amount       Decimal(14, 2),
    has_approximated UInt8,
    due_date         Date,
    disputed_amount  Decimal(14, 2)
) ENGINE = MergeTree ORDER BY (tin, tax_type, yofa);

-- 3. Taxpayer golden record (ARMS int_taxpayer__master pattern).
CREATE TABLE IF NOT EXISTS mtca_ors.int_taxpayer__master
(
    tin                  String,
    tpform               Enum8('I' = 1, 'C' = 2),
    name                 String,
    reg_date             Date,
    status               LowCardinality(String),
    locality             String,
    email                String,
    phone                String,
    tax_types_registered UInt8,
    open_disputed_total  Decimal(14, 2),
    credit_total         Decimal(14, 2),
    suspense_entries     UInt8
) ENGINE = MergeTree ORDER BY tin;
