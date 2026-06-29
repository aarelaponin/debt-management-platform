# MTCA DM — Development Environment Runbook

**v1.0 · 11 June 2026 · Status of this setup: jdx9 OPERATIONAL (login verified); Docker stack files ready (first `up` pending)**
**Deployment diagram:** `deployment-diagram.mermaid` (this folder)

## 1. Components

| Component | Where | Endpoint | Credentials (DEV) |
|---|---|---|---|
| **jdx9** — Joget DX 9.0.7 Enterprise (apps: cmbb, dmbb) | `/Users/aarelaponin/joget-enterprise-linux-9.0.7-9` | http://localhost:8089/jw | admin / admin |
| jdx9 database | PostgreSQL 16 (`pg1`, Homebrew, port 5432) | db `jwdb_mtca` | joget_mtca / joget_mtca |
| Mock data platform — ClickHouse 24.8 | Docker (`platform-mock/`) | http 8123 · native/JDBC 9000 | mtca / ${CLICKHOUSE_PASSWORD} |
| Mayan EDMS s4.11 (+pg14, redis, rabbitmq) | Docker (`platform-mock/`) | http://localhost:8880 | autoadmin on first start (see Mayan-EDMS.md §5) |
| Glowroot APM for jdx9 | bundled | http://localhost:4019 | — |

**Port map (jdx9):** HTTP 8089 · shutdown 8019 · AJP 8029 (commented) · HTTPS 8443 (shared) · Glowroot 4019.
Registered in `~/.joget/instances.yaml` (single source of truth). Pending `.env` additions for the instance-manager: `JOGET_MTCA_DB_PASSWORD=joget_mtca`, `JDX9_PASSWORD=admin`.

## 1a. Database connections (ad-hoc queries)

```bash
# Joget jdx9 (PostgreSQL 16) — SELECT freely; NEVER write to app_fd_* outside the Joget API (P3)
psql -h localhost -p 5432 -U joget_mtca -d jwdb_mtca          # password: joget_mtca
# JDBC: jdbc:postgresql://localhost:5432/jwdb_mtca?currentSchema=public  (user joget_mtca)

# Mock data platform (ClickHouse) — admin
curl -s 'http://localhost:8123/?user=mtca&password=<CLICKHOUSE_PASSWORD>&query=SELECT+1'
docker exec -it mtca-clickhouse clickhouse-client -u mtca --password <CLICKHOUSE_PASSWORD> -d mtca_ors
# Product consumer (what Joget uses; DataGrip: database sta_v1):
#   user sta_reader / sta_reader_dev — SELECT only
#   JDBC: jdbc:clickhouse://localhost:8123/sta_v1  (driver com.clickhouse.jdbc.ClickHouseDriver)
# Writeback identity: writeback_api / writeback_dev — INSERT on mtca_ors.fact_case_outcomes only
# Reload mock data: cd platform-mock/seed && python3 generate_seed.py && ./load_seed.sh

# Mayan metadata DB (inspection only — integration goes via REST API v4, P15)
docker exec -it mtca-mayan-pg psql -U mayan -d mayan
```

Mayan first-login password: the autoadmin password is often invisible in `docker compose logs` (drowned by Django warnings). Retrieve it from Mayan's DB:
`docker exec mtca-mayan-pg psql -U mayan -d mayan -c "SELECT password FROM autoadmin_autoadminsingleton;"` — login as `admin`, then change it (the row clears itself).

Joget app data API (seeds/tests): `/jw/api/form/{formId}/saveOrUpdate`, headers `api_id: API-cmbb-data` + `api_key: <from SELECT apikey FROM api_credential WHERE apiname='cmbb-dev-key'>` (key bound to API id — DX9 requirement). Loader: `scripts/load_md_seed.py`.
Health check one-liner for jdx9: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8089/jw/web/login` → `200`.
Glowroot note: agent admin.json patched to port 4019 — takes effect on next Tomcat restart (until then it logs bind-retry noise on 4000; harmless).

## 2. Start / stop

```bash
# Joget jdx9
cd /Users/aarelaponin/joget-enterprise-linux-9.0.7-9 && ./tomcat.sh start   # stop | restart
tail -f apache-tomcat-11.0.21/logs/catalina.out

# Mock platform + Mayan (Docker Desktop must be running)
cd <repo>/15-ITCAS/03_debt_management/platform-mock
cp .env.example .env          # first time only
docker compose --project-name mtca up -d
docker compose ps             # status; NEVER 'down -v' (volumes hold data)
```

## 3. Setup decisions & lessons (recorded the hard way)

1. **Java is pinned to OpenJDK 21** (Homebrew) in `tomcat.sh` — the default JDK 22 lacks classes the Shark engine path expects; 21 is the proven jdx8 combination. Do not remove the `JAVA_HOME` export or the `-Dwflow.ignite=true` flag.
2. **The PostgreSQL schema is NOT fully auto-created.** Joget auto-creates `app_*`/`dir_*`/`wf_*` (Hibernate) but the **Shark tables (`shk*`, 67 of them) must pre-exist** — the bundle only ships a MySQL script (`data/jwdb-empty.sql`). jdx9's schema was cloned structure-only from `jwdb_gam` (jdx8): `pg_dump -s --no-owner --no-privileges -T 'app_fd_*' jwdb_gam`. **For any future PG instance, load this structure before first boot.** A reusable copy: `/tmp/joget_pg_schema.sql` (regenerate from jdx8 if lost; commit a copy to `platform-mock/joget/` when convenient).
3. **Directory seeding:** fresh schema has no users/roles — `dir_role` (4 rows), `dir_user` admin row and `dir_user_role` were copied from jwdb_gam (`\copy`, not server-side COPY); admin password hash reset to default MD5 (`admin`).
4. **Mayan port 8880**, ClickHouse 8123/9000 — verified free against the estate (8080–8089 Joget range, Glowroot 4000–4019, MySQL 3306–3311, PG 5432).
5. Transient `RootError/shkprocessstates` lines during the first seconds of startup are warmup noise (jdx8 shows the same); a healthy boot ends with HTTP 200 on `/jw/web/login` and zero new engine errors after warmup.

## 4. Verification (smoke)

```bash
curl -s -o /dev/null -w '%{http_code}' http://localhost:8089/jw/web/login        # expect 200
psql -h localhost -U joget_mtca -d jwdb_mtca -tc \
  "SELECT count(*) FROM information_schema.tables WHERE table_schema='public';"   # expect ≥116
curl -s http://localhost:8123/ping                                                # expect Ok. (Docker up)
curl -s -o /dev/null -w '%{http_code}' http://localhost:8880                      # expect 200/302 (Mayan up)
```
DB-level checks for app state: per `joget-db-inspect` conventions against `jwdb_mtca`.

## 5. Backup notes

PostgreSQL: `pg_dump jwdb_mtca` (add to whatever cron covers pg1). Mayan: per `docs/Mayan-EDMS.md` §7 (pg_dump + volume tar + compose files). ClickHouse mock: disposable — rebuilt from `platform-mock/ddl` + seed at any time; no backup needed.
