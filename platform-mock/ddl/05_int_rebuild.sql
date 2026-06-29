-- Rebuild the intermediate layer from silver (idempotent; run after every seed load).
-- Stands in for `dbt run --select intermediate` on the real platform.

TRUNCATE TABLE mtca_ors.int_accounting__postings;
INSERT INTO mtca_ors.int_accounting__postings
SELECT t.tin            AS tin,
       t.tax_type       AS tax_type,
       t.yofa           AS yofa,
       t.period         AS period,
       t.txn_date       AS txn_date,
       t.trcd           AS trcd,
       t.sbcd           AS sbcd,
       coalesce(toString(m.semantic), 'UNMAPPED')   AS semantic,
       coalesce(toString(m.component), 'P')         AS component,
       coalesce(toString(m.confidence), 'APPROXIMATED') AS asa_confidence,
       t.amount         AS amount,
       t.source_ref     AS source_ref
FROM mtca_ors.silver_transactions t
LEFT JOIN mtca_ors.asa_trcd_map m ON m.trcd = t.trcd AND m.sbcd = t.sbcd;

TRUNCATE TABLE mtca_ors.int_accounting__period_balances;
INSERT INTO mtca_ors.int_accounting__period_balances
WITH disputes AS (
    SELECT tin AS d_tin, tax_type AS d_tax_type, yofa AS d_yofa,
           sum(amount) AS disputed_amount
    FROM mtca_ors.silver_objections WHERE status = 'OPEN'
    GROUP BY tin, tax_type, yofa
),
due AS (
    SELECT tin AS a_tin, tax_type AS a_tax_type, yofa AS a_yofa,
           min(due_date) AS due_date
    FROM mtca_ors.silver_assessments
    GROUP BY tin, tax_type, yofa
)
SELECT p.tin                                        AS tin,
       p.tax_type                                   AS tax_type,
       p.yofa                                       AS yofa,
       sum(p.amount)                                AS balance,
       sumIf(p.amount, p.component = 'P')           AS pa_amount,
       sumIf(p.amount, p.component = 'I')           AS ia_amount,
       sumIf(p.amount, p.component = 'C')           AS pca_amount,
       max(p.asa_confidence = 'APPROXIMATED')       AS has_approximated,
       coalesce(any(d2.due_date), toDate('1970-01-01')) AS due_date,
       coalesce(any(o.disputed_amount), 0)          AS disputed_amount
FROM mtca_ors.int_accounting__postings p
LEFT JOIN due d2     ON d2.a_tin = p.tin AND d2.a_tax_type = p.tax_type AND d2.a_yofa = p.yofa
LEFT JOIN disputes o ON o.d_tin = p.tin AND o.d_tax_type = p.tax_type AND o.d_yofa = p.yofa
GROUP BY p.tin, p.tax_type, p.yofa;

TRUNCATE TABLE mtca_ors.int_taxpayer__master;
INSERT INTO mtca_ors.int_taxpayer__master
WITH disp AS (
    SELECT tin AS d_tin, sum(amount) AS open_disputed_total
    FROM mtca_ors.silver_objections WHERE status = 'OPEN' GROUP BY tin
),
cred AS (
    SELECT tin AS c_tin,
           sumIf(amount, kind = 'CREDIT') AS credit_total,
           count() AS suspense_entries
    FROM mtca_ors.silver_suspense WHERE tin != '' GROUP BY tin
),
ttypes AS (
    SELECT tin AS t_tin, uniqExact(tax_type) AS tax_types_registered
    FROM mtca_ors.silver_assessments GROUP BY tin
)
SELECT tp.tin       AS tin,
       tp.tpform    AS tpform,
       tp.name      AS name,
       tp.reg_date  AS reg_date,
       toString(tp.status) AS status,
       tp.locality  AS locality,
       tp.email     AS email,
       tp.phone     AS phone,
       coalesce(any(tt.tax_types_registered), 0) AS tax_types_registered,
       coalesce(any(d3.open_disputed_total), 0)  AS open_disputed_total,
       coalesce(any(c.credit_total), 0)          AS credit_total,
       coalesce(any(c.suspense_entries), 0)      AS suspense_entries
FROM mtca_ors.silver_taxpayer tp
LEFT JOIN ttypes tt ON tt.t_tin = tp.tin
LEFT JOIN disp d3   ON d3.d_tin = tp.tin
LEFT JOIN cred c    ON c.c_tin = tp.tin
GROUP BY tp.tin, tp.tpform, tp.name, tp.reg_date, tp.status, tp.locality, tp.email, tp.phone;
