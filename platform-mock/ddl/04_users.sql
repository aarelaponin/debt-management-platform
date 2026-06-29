-- Read-only consumer role for Joget (GoldMartClient, JdbcDataListBinder, Jasper) — I-1, D-SAD-02.
-- DEV credentials; production equivalents come from the real platform's Keycloak/secret store.
CREATE USER IF NOT EXISTS sta_reader IDENTIFIED WITH sha256_password BY 'sta_reader_dev';
GRANT SELECT ON sta_v1.* TO sta_reader;
-- ClickHouse views execute with the caller's rights (SQL SECURITY INVOKER default),
-- so the reader also needs SELECT on the underlying silver schema (read-only regardless):
GRANT SELECT ON mtca_ors.* TO sta_reader;
-- Writeback service identity (REST API inserts outcomes; nothing else)
CREATE USER IF NOT EXISTS writeback_api IDENTIFIED WITH sha256_password BY 'writeback_dev';
GRANT INSERT, SELECT ON mtca_ors.fact_case_outcomes TO writeback_api;
