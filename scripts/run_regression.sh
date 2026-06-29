#!/usr/bin/env bash
# Canonical CMBB+DMBB regression sweep — run_t02 .. run_tNN, order-independent.
#
# Why this exists (regression-harness hygiene, 14 Jun 2026):
#   * run_t07 needs MAYAN_USER/MAYAN_PASSWORD in the environment or it falls back to
#     admin/admin and the Mayan token endpoint 400s. We source the live autoadmin pw here
#     so a sweep never re-discovers that.
#   * run_t03 / run_t08 are stateful (round-robin cursor + suppression/dispatch) and now
#     drain() running envelopes at suite start, so the sweep is order-independent.
#   * RESTART=1 (default) cold-starts Tomcat first so every sweep runs against a fresh JVM.
#     This is the proven-deterministic baseline (24/24 green). It also sidesteps a residual
#     CMBB-F09 issue: the outcome-run SHIP is not yet idempotent across repeated runs in a
#     single warm JVM (2nd run → WorkflowManagerImpl NPE / ClickHouse client re-init). Set
#     RESTART=0 to skip the restart (e.g. when chaining sweeps against an already-fresh JVM).
#
# Usage:  scripts/run_regression.sh [FROM] [TO]      (defaults 2 25; env RESTART=0 to skip)
set -u
# Derive ROOT from this script's own location (../ of scripts/) so the harness survives a repo move.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PY="/Users/aarelaponin/PycharmProjects/dev/joget-deployment-toolkit/.venv/bin/python"
JOGET="/Users/aarelaponin/joget-enterprise-linux-9.0.7-9"
FROM="${1:-2}"; TO="${2:-25}"
RESTART="${RESTART:-1}"
cd "$ROOT" || exit 2

if [ "$RESTART" = "1" ]; then
  echo "Cold-starting Tomcat for a clean JVM baseline ..."
  ( cd "$JOGET" && pkill -f "org.apache.catalina.startup.Bootstrap"; sleep 8; ./tomcat.sh start >/dev/null 2>&1 )
  for i in $(seq 1 30); do
    code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8089/jw/web/json/workflow/currentUsername 2>/dev/null)
    [ "$code" = "400" ] || [ "$code" = "200" ] && { echo "jdx9 up (${i}0s)"; break; }
    sleep 10
  done
  sleep 20  # let OSGi plugins finish installing
fi

export JDX9_PASSWORD=admin
export MAYAN_URL="http://localhost:8880"
export MAYAN_USER="admin"
export MAYAN_PASSWORD="$(docker exec mtca-mayan-pg psql -U mayan -d mayan -t -A -c \
  "SELECT password FROM autoadmin_autoadminsingleton;" 2>/dev/null | tr -d '[:space:]')"
[ -n "$MAYAN_PASSWORD" ] && echo "MAYAN_PASSWORD sourced (${MAYAN_PASSWORD:0:3}...)" \
                         || echo "WARN: MAYAN_PASSWORD empty — run_t07 will fail"

GREEN=0; FAILS=""
for t in $(seq -w "$FROM" "$TO"); do
  f=$(find cmbb dmbb -name "run_t${t}.py" 2>/dev/null | head -1)
  [ -z "$f" ] && continue
  out=$($PY "$f" 2>&1 | tail -1)
  if echo "$out" | grep -q "ALL GREEN" && ! echo "$out" | grep -q "FAILED"; then
    GREEN=$((GREEN+1)); echo "t${t}: ok    -> $out"
  else
    FAILS="$FAILS t${t}"; echo "t${t}: FAIL  -> $out"
  fi
done
echo "======================================================"
echo "GREEN=$GREEN   FAILS=${FAILS:-none}"
[ -z "$FAILS" ]
