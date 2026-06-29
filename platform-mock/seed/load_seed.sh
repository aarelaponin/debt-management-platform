#!/bin/bash
# Apply DDL + load seed CSVs into the mtca-clickhouse container. Idempotent (truncates silver first).
set -euo pipefail
DIR=$(cd "$(dirname "$0")" && pwd)
ENVF="$DIR/../.env"; [ -f "$ENVF" ] && set -a && source "$ENVF" && set +a
CH() { docker exec -i mtca-clickhouse clickhouse-client -u mtca --password "${CLICKHOUSE_PASSWORD:?set in platform-mock/.env}" "$@"; }

echo "== DDL"
for f in "$DIR"/../ddl/01_silver.sql "$DIR"/../ddl/02_intermediate.sql "$DIR"/../ddl/03_sta_v1_views.sql "$DIR"/../ddl/04_users.sql; do
  echo "  applying $(basename "$f")"; CH -n < "$f"
done

echo "== seed"
[ -d "$DIR/out" ] || { echo "run generate_seed.py first"; exit 1; }
for t in silver_taxpayer silver_assessments silver_transactions silver_receipts silver_objections silver_suspense; do
  CH --query "TRUNCATE TABLE mtca_ors.$t"
  CH --query "INSERT INTO mtca_ors.$t FORMAT CSVWithNames" < "$DIR/out/$t.csv"
  echo "  $t: $(CH --query "SELECT count() FROM mtca_ors.$t") rows"
done

echo "== rebuild intermediate (dbt-run stand-in)"
CH -n < "$DIR"/../ddl/05_int_rebuild.sql
for t in int_accounting__postings int_accounting__period_balances int_taxpayer__master; do
  echo "  $t: $(CH --query "SELECT count() FROM mtca_ors.$t") rows"
done

echo "== smoke"
CH --query "SELECT debt_category, count() AS tins, round(sum(total_enforceable),2) AS enforceable FROM sta_v1.debt_priority_queue GROUP BY debt_category ORDER BY debt_category FORMAT PrettyCompact"
echo "OK."
