-- sta_v1: the published accounting data product (contract per SAD §5.1, design authority Module 05).
-- Reads from the INTERMEDIATE layer (02_intermediate.sql / 05_int_rebuild.sql) — the Blueprint
-- Ch.8 staging->intermediate->marts pattern; the int_* SQL is the ASA reference implementation.

-- 1. taxpayer_360 (Module 05 §9.2.4)
CREATE OR REPLACE VIEW sta_v1.taxpayer_360 AS
SELECT tin, tpform, name, reg_date, status, locality, email, phone,
       tax_types_registered, open_disputed_total, credit_total, suspense_entries,
       now() AS as_of
FROM mtca_ors.int_taxpayer__master;

-- 2. account_transactions — L3: typed transaction detail with running balance (STA-FR L3, TAS)
CREATE OR REPLACE VIEW sta_v1.account_transactions AS
SELECT tin, tax_type, yofa, period, txn_date, trcd, sbcd,
       semantic, component, asa_confidence, amount,
       sum(amount) OVER (PARTITION BY tin, tax_type
                         ORDER BY txn_date, trcd
                         ROWS UNBOUNDED PRECEDING) AS running_balance,
       source_ref,
       now() AS as_of
FROM mtca_ors.int_accounting__postings;

-- 3. taxpayer_balances — L2 per TIN x tax type, PA/IA/PCA decomposition + disputed flag
--    (L1 = SUM over this view per TIN; deliberately not materialised twice, P9)
CREATE OR REPLACE VIEW sta_v1.taxpayer_balances AS
WITH dt AS (
    SELECT tin AS p_tin, tax_type AS p_tax_type,
           sumIf(amount, amount > 0) AS debit_total,
           -sumIf(amount, amount < 0) AS credit_total
    FROM mtca_ors.int_accounting__postings
    GROUP BY tin, tax_type
)
SELECT b.tin AS tin,
       b.tax_type AS tax_type,
       any(d.debit_total)                               AS debit_total,
       any(d.credit_total)                              AS credit_total,
       sum(b.balance)                                   AS balance,
       sum(b.pa_amount)                                 AS pa_amount,
       sum(b.ia_amount)                                 AS ia_amount,
       sum(b.pca_amount)                                AS pca_amount,
       if(max(b.has_approximated) = 1,
          'APPROXIMATED', 'VERIFIED')                   AS asa_confidence,
       sum(b.disputed_amount)                           AS disputed_amount,
       sum(b.balance) - sum(b.disputed_amount)          AS enforceable_balance,
       now()                                            AS as_of
FROM mtca_ors.int_accounting__period_balances b
LEFT JOIN dt d ON d.p_tin = b.tin AND d.p_tax_type = b.tax_type
GROUP BY b.tin, b.tax_type;

-- 4. payment_history (Module 05 §9.2.4; stus 8/9 excluded per legacy rule)
CREATE OR REPLACE VIEW sta_v1.payment_history AS
SELECT rcno, rcdt AS receipt_date, tin, ptyp AS payment_type, yerr AS year_ref,
       amount, channel,
       multiIf(tin = '', 'UNIDENTIFIED', 'IDENTIFIED') AS identification_status,
       now() AS as_of
FROM mtca_ors.silver_receipts
WHERE stus NOT IN (8, 9);

-- 5. assessment_register (Module 05 §9.1 Assessment)
CREATE OR REPLACE VIEW sta_v1.assessment_register AS
SELECT assessment_id, tin, tax_type, yofa, period, kind, caution_flag,
       amount, due_date, assessed_at, now() AS as_of
FROM mtca_ors.silver_assessments;

-- 6. open_credits_suspense (STA-FR-046, BR-STA-037/041)
CREATE OR REPLACE VIEW sta_v1.open_credits_suspense AS
SELECT entry_id, tin, kind, amount, received_date, reference, now() AS as_of
FROM mtca_ors.silver_suspense;

-- 7. debt_balances — gold_debt grain: TIN x tax type x year; ageing + C1..C6 banding
--    (BR-DM-003 / DM-FR-004 thresholds; category over CONSOLIDATED enforceable per TIN)
CREATE OR REPLACE VIEW sta_v1.debt_balances AS
WITH rows_ AS (
    SELECT tin, tax_type, yofa, balance, pa_amount, ia_amount, pca_amount,
           due_date, disputed_amount,
           balance - disputed_amount                            AS enforceable_amount,
           greatest(dateDiff('day', due_date, today()), 0)      AS age_days
    FROM mtca_ors.int_accounting__period_balances
    WHERE balance > 0 AND due_date > toDate('1970-01-01') AND due_date <= today()
),
totals AS (
    SELECT tin AS t_tin,
           sumIf(enforceable_amount, enforceable_amount > 0) AS tin_total_enforceable
    FROM rows_ GROUP BY tin
)
SELECT r.tin AS tin, r.tax_type AS tax_type, r.yofa AS yofa, r.balance AS balance,
       r.pa_amount AS pa_amount, r.ia_amount AS ia_amount, r.pca_amount AS pca_amount,
       r.due_date AS due_date, r.disputed_amount AS disputed_amount,
       r.enforceable_amount AS enforceable_amount, r.age_days AS age_days,
       multiIf(r.age_days < 30, '0-29', r.age_days < 90, '30-89', r.age_days < 180, '90-179',
               r.age_days < 365, '180-364', '365+')             AS age_band,
       t.tin_total_enforceable AS tin_total_enforceable,
       multiIf(t.tin_total_enforceable < 30,     'C1',
               t.tin_total_enforceable < 100,    'C2',
               t.tin_total_enforceable < 1000,   'C3',
               t.tin_total_enforceable < 20000,  'C4',
               t.tin_total_enforceable < 200000, 'C5', 'C6')    AS debt_category,
       now() AS as_of
FROM rows_ r
JOIN totals t ON t.t_tin = r.tin;

-- 8. debt_priority_queue — per TIN; risk_score NULL until SAS VIYA lands (I-7/D-SAD-07)
CREATE OR REPLACE VIEW sta_v1.debt_priority_queue AS
SELECT tin,
       any(debt_category)                       AS debt_category,
       sum(enforceable_amount)                  AS total_enforceable,
       sum(disputed_amount)                     AS total_disputed,
       max(age_days)                            AS oldest_debt_age_days,
       count(DISTINCT tax_type)                 AS tax_types_involved,
       count()                                  AS debt_lines,
       CAST(NULL AS Nullable(UInt8))            AS risk_score,
       round(log10(1 + sum(enforceable_amount)) * 20
             + least(max(age_days), 730) / 14)  AS mock_priority,  -- placeholder until SAS
       now()                                    AS as_of
FROM sta_v1.debt_balances
WHERE enforceable_amount > 0
GROUP BY tin;

-- 9. case_outcomes — readable face of the writeback fact (deduplicated)
CREATE OR REPLACE VIEW sta_v1.case_outcomes AS
SELECT case_id, outcome_date, tin, outcome_type, outcome_code, amount, officer, detail, received_at
FROM mtca_ors.fact_case_outcomes FINAL;
