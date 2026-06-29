# Environment Bootstrap — Zero to Running in ~15 Minutes

**v1.0 · 11 June 2026 · Distilled from the first (jdx9) setup.** Companion to `ENVIRONMENT.md` (current state) and `DEPLOYMENT.md` (artefact deploys). Use this when standing up the next instance/machine (INT, demo, a colleague's laptop).

## 0. Decision defaults (don't re-ask these)

| Decision | Default | Override only if |
|---|---|---|
| Joget version | 9.0.7 Enterprise (DX9 target; proven combination) | new validated version exists |
| Install source | `~/dev_install/joget_src/joget/joget-enterprise-linux-<ver>.tar.gz` | — |
| Install dir | `~/joget-enterprise-linux-<ver>-<n>` (n = instance number) | — |
| Java | **OpenJDK 21 (Homebrew), pinned in tomcat.sh** — never the system default | Joget release notes say otherwise |
| JAVA_OPTS extras | `-Dwflow.ignite=true -Dwflow.asyncRequestTimeout=5000` | — |
| Database | PostgreSQL on `pg1` (localhost:5432), db `jwdb_<env>`, user `joget_<env>`, password = username (DEV only) | shared/remote DB |
| Ports | next free in each series: http 808x, shutdown 801x, ajp 802x, glowroot 401x; https 8443 shared | check `lsof` |
| Admin login | admin / admin (MD5 default hash) | non-DEV |
| Mayan | pinned series (s4.11), port 8880, config via `platform-mock/mayan/setup_mayan.py` | — |
| ClickHouse | 24.8, ports 8123/9000, db `mtca_ors`, user `mtca` | — |

## 1. Joget instance (automated)

```bash
cd 15-ITCAS/03_debt_management
./scripts/bootstrap_jdx_pg.sh <name> <http> <shutdown> <glowroot> <db> \
  [tarball] [schema_source_db]
# example (jdx9 equivalent):
./scripts/bootstrap_jdx_pg.sh jdx9 8089 8019 4019 jwdb_mtca
```

The script does, in order: extract tarball → copy PG JDBC driver → patch server.xml ports → write datasource properties → write tomcat.sh (Java 21 pin + flags) → set glowroot port → create role+db → **preload Joget PG structure incl. the 67 `shk*` Shark tables** (schema-only clone from a reference db; default `jwdb_gam`) → seed `dir_role`/admin user (admin/admin) → start Tomcat → poll until HTTP 200 → print the `instances.yaml` snippet.

**Then:** paste the printed snippet into `~/.joget/instances.yaml` (under `instances:`), add the two `.env` entries it prints, and verify YAML parses. (Or run the joget-instance-setup skill flow with these values.)

**Why each non-obvious step exists (the lessons):**
1. `shk*` preload — Joget auto-creates Hibernate tables but NOT the Shark workflow tables; the bundle ships only a MySQL script. Without preload: `relation "shkprocessstates" does not exist` + engine bean failure.
2. Java 21 pin — system JDK 22+ breaks the Shark/DODS path (`java.rmi.activation` removed); 21 + ignite flag is the proven jdx8 combination.
3. Directory seeding — a structure-cloned schema has no roles/users; Joget does not re-seed into existing tables.
4. Glowroot port — runtime agent config (`wflow/glowroot/agent-<host>/admin.json`) overrides; set it or it binds 4000 and conflicts.
5. First minutes of catalina.out show transient RootError noise — ignore; healthy = HTTP 200 on `/jw/web/login` + ≥116 tables.
6. Login verification is a browser job — DX9 injects CSRF via JS; curl gets 403 by design.

## 2. Docker stack (Mayan + ClickHouse)

```bash
cd platform-mock
cp .env.example .env                       # first time; edit if desired
docker compose --project-name mtca up -d   # first pull ~2 GB
curl -s http://localhost:8123/ping         # Ok.
# Mayan admin password (login page won't tell you; logs drown it):
docker exec mtca-mayan-pg psql -U mayan -d mayan -c \
  "SELECT password FROM autoadmin_autoadminsingleton;"
```

## 3. Mayan reference configuration (idempotent, rerunnable)

```bash
MAYAN_URL=http://localhost:8880 MAYAN_USER=admin MAYAN_PASS=<pwd> \
  python3 platform-mock/mayan/setup_mayan.py
```
Creates: 19 document types (TA-RDM + DM instruments), TTT metadata spine, attachments, Taxpayers index (links at TIN and period levels). API lessons encoded in the script: send `Accept: application/json` (else DRF returns HTML), retry on 429 throttling, index API is `/index_templates/`, root node id from template detail, doc-type linking via `.../document_types/add/`.

## 4. Smoke matrix (5 minutes)

| # | Check | Expect |
|---|---|---|
| 1 | `curl -s -o /dev/null -w '%{http_code}' localhost:<http>/jw/web/login` | 200 |
| 2 | table count in instance db | ≥116 (67 `shk*`) |
| 3 | browser login admin/admin | console |
| 4 | `curl -s localhost:8123/ping` | `Ok.` |
| 5 | Mayan UI login + upload PDF with TIN metadata | preview renders; OCR tab fills ~1 min |
| 6 | Indexes → Taxpayers → TIN | document listed |
| 7 | REST: token → search `q=<TIN>` → metadata → download | all 200 (commands in ENVIRONMENT.md §1a) |

## 5. Knowledge upstreaming (do once)

The `joget-instance-setup` skill predates the DX9/PostgreSQL lessons (§1). Worth adding to the skill: the shk-preload rule, the Java-21 pin, the directory-seeding step, and the glowroot agent-dir port location. *(Skills aren't editable from a Cowork session — apply via the skill-management flow; the §1 list is the patch content.)*
