# Mayan EDMS — Installation, Management & Administration Guide

**Purpose:** Reference implementation of the TA-RDM Document Management domain (08-document-management)
**Mayan version target:** 4.11 series (pin exact version before installation — see §4.3)
**Deployment method:** Docker Compose (single-host)
**Target OS:** Ubuntu/Debian Linux (notes for Windows/macOS in §3.3)
**Document version:** 1.0 — June 2026

---

## Table of Contents

1. [Overview & Architecture](#1-overview--architecture)
2. [System Requirements](#2-system-requirements)
3. [Prerequisites: Installing Docker](#3-prerequisites-installing-docker)
4. [Installing Mayan EDMS](#4-installing-mayan-edms)
5. [First Start & Initial Login](#5-first-start--initial-login)
6. [Day-to-Day Operations](#6-day-to-day-operations)
7. [Backup & Restore](#7-backup--restore)
8. [Upgrading](#8-upgrading)
9. [Mayan Administration Essentials](#9-mayan-administration-essentials)
10. [Configuring Mayan as the TA-RDM Reference DMS](#10-configuring-mayan-as-the-ta-rdm-reference-dms)
11. [REST API Access](#11-rest-api-access)
12. [Troubleshooting](#12-troubleshooting)
13. [Command Quick Reference](#13-command-quick-reference)

---

## 1. Overview & Architecture

### 1.1 What gets installed

Mayan EDMS is a Django (Python) application that depends on several services. The Docker Compose stack packages all of them so that nothing needs to be installed or configured natively:

| Container | Role | Data location |
|---|---|---|
| `app` (mayanedms) | Web UI, REST API, background workers (OCR, previews, indexing) | volume: document files, settings |
| `postgresql` | Relational database (all metadata, users, workflows) | volume: database files |
| `redis` | Cache and task-lock backend | ephemeral (safe to lose) |
| `rabbitmq` | Task queue broker for background workers | volume (queue state) |

### 1.2 The persistence model — read this once, carefully

- **Containers are disposable.** They are deleted and recreated on every upgrade. Nothing valuable lives inside a container.
- **Volumes are permanent.** Named Docker volumes (stored on the host under `/var/lib/docker/volumes/`) hold the database and the document files. They survive container deletion, `docker compose down`, reboots, and upgrades.
- **The only way to lose volume data** through normal commands is `docker compose down -v` (the `-v` flag deletes volumes) or explicit `docker volume rm` / `docker volume prune`. **Never use the `-v` flag** on this stack.
- Volumes protect against operational mistakes, **not** against disk failure. Backups (§7) protect against disk failure.

### 1.3 Files you own

Your entire installation is defined by two small files in one directory (e.g. `~/mayan/`):

- `docker-compose.yml` — the stack recipe (which containers, volumes, ports)
- `.env` — your configuration (passwords, version pin, port)

Keep both under version control or in your backup set. With these two files plus a database dump and the document archive, the installation can be rebuilt on any machine in minutes.

---

## 2. System Requirements

For a reference implementation / development instance:

- **CPU:** 2+ cores (4 recommended — OCR is CPU-hungry)
- **RAM:** 4 GB minimum, 8 GB comfortable
- **Storage:** SSD strongly recommended; size = documents + ~30% overhead (previews, OCR text, versions)
- **OS:** Any modern Linux distribution with Docker support. The Mayan project recommends a well-supported GNU/Linux distribution for best Docker results.
- **Network:** One free TCP port for the web UI (default 80; this guide uses **8880** to avoid colliding with Joget/Tomcat or other services).

Production sizing for a revenue administration would be a separate exercise (multi-container, external object storage, HA database) — out of scope here.

---

## 3. Prerequisites: Installing Docker

### 3.1 Ubuntu / Debian

```bash
# Install Docker Engine + Compose plugin
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Allow your user to run docker without sudo (log out/in afterwards)
sudo usermod -aG docker $USER

# Verify
docker --version
docker compose version
```

### 3.2 Sanity check

```bash
docker run --rm hello-world
```

If this prints a greeting, Docker works.

### 3.3 Windows / macOS note

Install **Docker Desktop** instead; all `docker compose` commands in this guide are identical. On Windows, run them in PowerShell or inside WSL2. Volume storage is managed by Docker Desktop's VM; the backup procedure in §7 works unchanged because it goes through containers, not host paths.

---

## 4. Installing Mayan EDMS

### 4.1 Create the installation directory and download the official files

```bash
mkdir -p ~/mayan && cd ~/mayan

curl https://gitlab.com/mayan-edms/mayan-edms/-/raw/master/docker/docker-compose.yml -O
curl https://gitlab.com/mayan-edms/mayan-edms/-/raw/master/docker/.env -O
```

### 4.2 Edit `.env` — minimum changes before first start

Open `.env` in any editor and set:

```bash
# 1. PIN THE VERSION (see 4.3). Find the variable that sets the Mayan image tag,
#    e.g.:
MAYAN_DOCKER_IMAGE_TAG=s4.11        # series-pinned; or v4.11.4 for exact pin

# 2. DATABASE PASSWORD — change from the default before first start.
#    (Changing it later requires also changing it inside PostgreSQL.)
MAYAN_DATABASE_PASSWORD=<strong-password>

# 3. WEB PORT — avoid clashing with Joget/Tomcat (8080) and anything on 80:
MAYAN_FRONTEND_HTTP_PORT=8880
```

> Variable names occasionally shift between releases — match against the
> comments inside the downloaded `.env`, which is authoritative for the
> version you pulled.

### 4.3 Why pin the version

The `latest` tag is **not** guaranteed to be the newest release, and an unpinned tag means a future `docker compose pull` could silently jump major versions. The 4.11 series changed the service argument layout and made settings handling stricter, so mixing an old Compose file with a newer image (or vice versa) breaks the stack. Rule: **the Compose file, the `.env`, and the image tag must come from the same release series.** Pin `s4.11` (latest patch of 4.11) or an exact `v4.11.x`.

### 4.4 (Recommended) Bind-mount the document storage to a visible folder

By default all storage uses named volumes (invisible "internal" storage). For a reference implementation it is convenient to see the document store as a normal folder. In `docker-compose.yml`, locate the `app` service volume mapping for `/var/lib/mayan` and replace the named volume with a host path:

```yaml
services:
  app:
    volumes:
      - /home/youruser/mayan-data:/var/lib/mayan
```

Create the folder first: `mkdir -p ~/mayan-data`. Leave the **PostgreSQL** volume as a named volume (raw DB files in a visible folder invite accidental tampering; the database is backed up via `pg_dump` anyway, §7).

If you prefer to keep everything in named volumes, skip this step — the backup script handles both layouts.

### 4.5 Launch

```bash
cd ~/mayan
docker compose --project-name mayan up --detach
```

First start downloads ~2 GB of images and runs database migrations; allow **3–10 minutes**. Watch progress with:

```bash
docker compose logs --follow app
```

When the log shows the web server accepting connections, proceed to §5.

---

## 5. First Start & Initial Login

1. Open **http://localhost:8880** (or `http://<server-ip>:8880`).
2. Mayan's *autoadmin* feature creates an `admin` account on first start and displays the generated password on the login page / in the logs:

   ```bash
   docker compose logs app | grep -i password
   ```

3. Log in, then immediately:
   - Change the admin password (top-right user menu → *User details* → password).
   - Set the system timezone and language: **System → Settings**.
4. Smoke test: upload any PDF (**Documents → New document**), confirm the preview renders and, after a minute, OCR text appears under the document's *OCR* tab. If both work, every background worker is healthy.

---

## 6. Day-to-Day Operations

All commands are run from `~/mayan` (or add `--file ~/mayan/docker-compose.yml`).

| Task | Command |
|---|---|
| Start the stack | `docker compose up -d` |
| Stop the stack (data safe) | `docker compose down` |
| Status of containers | `docker compose ps` |
| Live application logs | `docker compose logs -f app` |
| Restart just the app | `docker compose restart app` |
| Resource usage | `docker stats` |
| List volumes | `docker volume ls` |

Notes:

- Containers are configured to **restart automatically** after a host reboot — no action needed.
- `docker compose down` ≠ data loss. Only `-v` deletes volumes. Never use `-v`.
- Disk watch: `docker system df` shows space used by images/volumes. Old images from upgrades can be cleaned with `docker image prune` (safe — does not touch volumes).

---

## 7. Backup & Restore

### 7.1 What a complete backup contains

1. **Database dump** — all metadata, users, document types, workflows (via `pg_dump`; never copy raw DB volume files — they can be inconsistent mid-write).
2. **Document files** — the contents of the media volume (or bind-mounted folder).
3. **Configuration** — `docker-compose.yml` and `.env`.

### 7.2 Backup script

Save as `~/mayan-backup.sh`:

```bash
#!/bin/bash
# mayan-backup.sh — nightly backup of Mayan EDMS to the filesystem
set -euo pipefail

BACKUP_DIR="$HOME/mayan-backups"
COMPOSE_DIR="$HOME/mayan"
KEEP_DAYS=14
STAMP=$(date +%F_%H%M)

mkdir -p "$BACKUP_DIR"
cd "$COMPOSE_DIR"

# 1. Database dump (consistent snapshot; no downtime needed)
docker compose exec -T postgresql \
  pg_dump -U mayan -d mayan | gzip > "$BACKUP_DIR/mayan_db_$STAMP.sql.gz"

# 2. Document files
#    Variant A — named volume (default install). Adjust the volume name
#    after checking `docker volume ls` (e.g. mayan_app):
docker run --rm \
  -v mayan_app:/data:ro \
  -v "$BACKUP_DIR":/backup \
  alpine tar czf "/backup/mayan_files_$STAMP.tar.gz" -C /data .

#    Variant B — bind mount (§4.4): comment out Variant A and use:
# tar czf "$BACKUP_DIR/mayan_files_$STAMP.tar.gz" -C "$HOME/mayan-data" .

# 3. Configuration
cp docker-compose.yml "$BACKUP_DIR/docker-compose_$STAMP.yml"
cp .env "$BACKUP_DIR/env_$STAMP"

# 4. Rotation
find "$BACKUP_DIR" -type f -mtime +$KEEP_DAYS -delete

echo "$(date): backup OK -> $BACKUP_DIR" >> "$BACKUP_DIR/backup.log"
```

Adjust:
- `pg_dump -U mayan -d mayan` — user/database must match your `.env`.
- The volume name in Variant A — confirm with `docker volume ls`.

### 7.3 Schedule with cron

```bash
chmod +x ~/mayan-backup.sh
crontab -e
# nightly at 02:30
30 2 * * * /home/youruser/mayan-backup.sh >> /home/youruser/mayan-backups/cron.log 2>&1
```

Verify the next morning that the three files exist and have plausible sizes.

### 7.4 Off-machine copy

A backup on the same disk protects against mistakes, not hardware failure. Sync `~/mayan-backups` to a NAS, second disk, or cloud storage, e.g.:

```bash
rclone sync ~/mayan-backups remote:mayan-backups
```

### 7.5 Restore procedure (also the migration procedure)

On a clean machine with Docker installed:

```bash
# 1. Restore configuration
mkdir -p ~/mayan && cd ~/mayan
cp /backups/docker-compose_<STAMP>.yml docker-compose.yml
cp /backups/env_<STAMP> .env

# 2. Start ONLY the database
docker compose --project-name mayan up -d postgresql
sleep 15

# 3. Load the dump
gunzip -c /backups/mayan_db_<STAMP>.sql.gz | \
  docker compose exec -T postgresql psql -U mayan -d mayan

# 4. Restore document files into the media volume
docker run --rm \
  -v mayan_app:/data \
  -v /backups:/backup:ro \
  alpine sh -c "cd /data && tar xzf /backup/mayan_files_<STAMP>.tar.gz"
#    (bind-mount layout: simply untar into ~/mayan-data instead)

# 5. Start the full stack
docker compose --project-name mayan up -d
```

**Test the restore once** shortly after going into real use — an untested backup is a hypothesis, not a backup.

### 7.6 Pre-upgrade snapshot

Always run `~/mayan-backup.sh` manually immediately before any upgrade (§8).

---

## 8. Upgrading

Mayan recommends sequential upgrades — go to the latest patch of your current series before jumping to the next series. Never skip the `down` step: the stack must be taken down so migrations run on next start.

```bash
cd ~/mayan

# 0. BACKUP FIRST
~/mayan-backup.sh

# 1. Stop and remove containers (volumes are untouched — do NOT use -v)
docker compose down

# 2. Keep the old recipe for reference
mv docker-compose.yml docker-compose.yml.bck
mv .env .env.bck

# 3. Download the new release's files
curl https://gitlab.com/mayan-edms/mayan-edms/-/raw/master/docker/docker-compose.yml -O
curl https://gitlab.com/mayan-edms/mayan-edms/-/raw/master/docker/.env -O

# 4. Re-apply YOUR values into the new .env:
#    - version pin (new series tag)
#    - MAYAN_DATABASE_PASSWORD (must be the OLD password — the DB already exists!)
#    - port, and any bind-mount edits in docker-compose.yml (§4.4)

# 5. Start — migrations run automatically on first boot of the new version
docker compose --project-name mayan up -d
docker compose logs -f app     # watch migrations complete
```

Read the release notes for the target version before upgrading; some releases (e.g. 4.11) include breaking configuration changes.

---

## 9. Mayan Administration Essentials

The concepts below are the building blocks you will configure. All are managed in the web UI under **System** and **Setup** menus, and all are also available via the REST API (§11).

### 9.1 Users, groups, roles, permissions

Mayan uses a three-layer access model:

- **Users** belong to **Groups** (e.g. *Registration Officers*, *Audit Unit*).
- **Roles** aggregate groups and carry **permissions**.
- Permissions are granted either **globally** (role can do X everywhere) or as **ACLs** on specific objects (this role can view documents of this type / in this cabinet only).

Practical pattern for a tax administration pilot: one role per business function, ACL-scoped to the relevant document types — this mirrors the `data_classification` and access rules in TA-RDM BR-DM-005.

### 9.2 Document types

The central classification object. Each document belongs to exactly one **document type**, which controls: required/optional metadata, retention behaviour (trash and stub expiry), OCR language, and workflows that may attach. Configure under **Setup → Document types**.

### 9.3 Metadata types

Named fields (free text, with optional validation/lookup) attached to document types as *required* or *optional*. Required metadata is prompted at upload. Configure under **Setup → Metadata types**, then associate with document types.

### 9.4 Cabinets

Hierarchical folders for human browsing. A document can live in multiple cabinets simultaneously (they are labels, not locations). Useful as the analogue of TA-RDM's physical-file/IRD file structure.

### 9.5 Indexes

Rule-driven, **automatically maintained** hierarchies built from metadata expressions (e.g. group documents by `{{ document.metadata_value_of.tin }}`). Where cabinets are manual, indexes are computed — they are the right tool for taxpayer-centric views.

### 9.6 Workflows

State machines attached to document types: states, transitions, triggers, and actions (e.g. on transition to *Approved*, apply a tag and move into an index). This maps directly to TA-RDM document lifecycle statuses (DRAFT → UNDER_REVIEW → APPROVED → DISTRIBUTED → ARCHIVED).

### 9.7 Tags, checkouts, signatures

- **Tags** — colored labels for ad-hoc flagging.
- **Check-out** — exclusive editing lock for new-version uploads.
- **Signatures** — GPG-based document signing/verification, relevant for outbound notices.

### 9.8 OCR and full-text search

Tesseract OCR runs automatically per the document type's language setting. Add languages by listing them in the `.env` OCR/APT settings (e.g. add `tesseract-ocr-mlt` for Maltese) and recreating the app container. OCR text feeds the built-in full-text search.

### 9.9 Retention

Per document type, configure trash time periods and stub expiry. Align with the TA-RDM rule BR-DM-006 (retention ≥ 5 years per tax law) — set deletion policies accordingly or disable automatic deletion entirely for the pilot.

---

## 10. Configuring Mayan as the TA-RDM Reference DMS

This section maps TA-RDM 08-document-management constructs onto Mayan objects, so the DMS becomes a faithful physical implementation of the canonical model.

### 10.1 Concept mapping

| TA-RDM (L2 canonical) | Mayan EDMS object | Notes |
|---|---|---|
| `document` (registry row) | Document | Mayan auto-assigns UUID + internal ID; mirror `document_number` as metadata |
| `ref_document_type` | Document type | Create one Mayan document type per TA-RDM code (see 10.2) |
| `document.document_status_code` | Workflow state | One workflow implementing DRAFT→UNDER_REVIEW→APPROVED→DISTRIBUTED→ARCHIVED |
| `document_storage` | Mayan storage backend | Mayan = the `DOCUMENT_MANAGEMENT_SYSTEM` storage type in TA-RDM |
| `document_version` | Document versions | Native in Mayan; complete history retained (BR-DM-030/031/032) |
| TTT spine (TIN, tax type, tax period) | Metadata types | Required metadata on all taxpayer-facing types (see 10.3) |
| `data_classification` | ACLs + tag | Enforce CONFIDENTIAL handling via role ACLs (BR-DM-005) |
| Physical file / IRD file tracking | Cabinets | Cabinet hierarchy per office/file |
| Taxpayer 360° view | Index on TIN metadata | Auto-built per-taxpayer document tree |

### 10.2 Document types to create

Mirror `ref_document_type` codes from the canonical model:

```
TAX_RETURN, ASSESSMENT_NOTICE, PAYMENT_RECEIPT, REFUND_DECISION,
AUDIT_REPORT, COLLECTION_NOTICE, APPEAL_DECISION, CORRESPONDENCE,
CERTIFICATE, LICENSE, REGISTRATION, APPLICATION,
SUPPORTING_DOCUMENT, INTERNAL_MEMO
```

Use the code as the Mayan document type *label* (or `label (CODE)`), so API lookups remain deterministic.

### 10.3 Metadata types (the TTT spine + registry fields)

Create as metadata types and attach to document types as indicated:

| Metadata type | Validation | Required on |
|---|---|---|
| `tin` | regex per jurisdiction TIN format | all taxpayer-facing types |
| `tax_type_code` | lookup list mirroring `tax_type` (VAT, CIT, PIT, WHT…) | TAX_RETURN, ASSESSMENT_NOTICE, PAYMENT_RECEIPT, REFUND_DECISION |
| `tax_period_code` | regex e.g. `^\d{4}(-(0[1-9]|1[0-2]|Q[1-4]))?$` | same as above |
| `document_number` | regex `^DOC-\d{4}-\d{8}$` | all (mirrors BR-DM-001 uniqueness in L2) |
| `document_direction` | lookup: INBOUND, OUTBOUND, INTERNAL | all |
| `source_reference` | free text (L2 PK of originating record, e.g. assessment_id) | system-generated documents |

> Uniqueness of `document_number` is enforced in the L2 database, not in
> Mayan — Mayan metadata has no unique constraint. The L2 `document` table
> remains the registry of record; Mayan is the storage/lifecycle engine.

### 10.4 Index for the taxpayer 360° view

Create an index *Taxpayers* with a template like:

```
Level 1: {{ document.metadata_value_of.tin }}
Level 2: {{ document.metadata_value_of.tax_type_code }}
Level 3: {{ document.metadata_value_of.tax_period_code }}
```

Every uploaded document then automatically appears under its TIN → tax type → period node — the DMS-side expression of the TTT principle.

### 10.5 Integration pattern with the L2 model

Recommended flow for the reference implementation:

1. Business event in the L2 system produces/receives a file.
2. Caller `POST`s the file to Mayan's API with document type + metadata (TTT keys, document_number).
3. Mayan returns its document UUID.
4. The L2 `document_storage` row stores: `storage_type_code = 'DOCUMENT_MANAGEMENT_SYSTEM'`, `storage_reference = <mayan UUID>`, plus format and checksum.
5. Retrieval, previews, versions, OCR text, and workflow state are then served by Mayan via UI or API.

This keeps the canonical model authoritative for registry semantics while delegating binary storage, rendering, OCR, and lifecycle mechanics to the DMS — exactly the separation the domain model intends.

---

## 11. REST API Access

- Full REST API at `http://<host>:8880/api/v4/`
- Interactive documentation (Swagger UI): `http://<host>:8880/api/v4/swagger/ui/`
- Authentication: token-based. Obtain a token:

```bash
curl -s -X POST http://localhost:8880/api/v4/auth/token/obtain/ \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "<password>"}'
```

Upload a document (two-step: create document, then upload file into it — consult the Swagger UI for the exact endpoints of your pinned version, as the API evolved across 4.x):

```bash
TOKEN=<token>
curl -s -X POST http://localhost:8880/api/v4/documents/ \
  -H "Authorization: Token $TOKEN" \
  -d "document_type_id=<id>"
# then POST the file to the returned document's file upload endpoint,
# then POST metadata values to /documents/<id>/metadata/
```

For scripted configuration (creating all document types, metadata types, and the index from §10 in one pass), a Python setup script using the API is the cleanest approach — it makes the Mayan configuration itself reproducible and versionable alongside the TA-RDM YAML specifications.

---

## 12. Troubleshooting

| Symptom | Likely cause | Action |
|---|---|---|
| Web UI not reachable after install | App still migrating, or port conflict | `docker compose logs -f app`; check `ss -tlnp \| grep 8880` |
| Login page shows no admin password | Autoadmin already consumed (password shown once) | `docker compose logs app \| grep -i password`; if lost, reset via `docker compose exec app /opt/mayan-edms/bin/mayan-edms.py changepassword admin` |
| Uploads stuck at "processing" / no previews | Background workers or broker problem | `docker compose ps` (all containers Up?); `docker compose restart` |
| OCR produces nothing | Language pack missing or OCR disabled for the doc type | Check document type OCR settings; add tesseract language via `.env` |
| "Connection refused" to database in logs | postgresql container down or wrong password after edit | `docker compose ps`; never change `MAYAN_DATABASE_PASSWORD` after init without changing it in PostgreSQL too |
| Disk filling up | Old images, orphaned previews | `docker system df`; `docker image prune`; review retention settings |
| Stack broken after upgrade | Compose/.env/image version mismatch | Restore `.bck` files, `docker compose down && up -d`, retry upgrade reading release notes |
| Everything is on fire | — | You have backups (§7). Restore per §7.5 on a clean directory. |

When asking for help (Mayan forum) or debugging, capture: `docker compose ps`, the last 200 lines of `docker compose logs app`, and your (password-redacted) `.env`.

---

## 13. Command Quick Reference

```bash
# --- lifecycle (run inside ~/mayan) ---
docker compose up -d                  # start
docker compose down                   # stop (NEVER add -v)
docker compose ps                     # status
docker compose logs -f app            # live logs
docker compose restart app            # restart web/app only

# --- inspection ---
docker volume ls                      # list volumes
docker stats                          # CPU/RAM per container
docker system df                      # disk usage

# --- maintenance ---
~/mayan-backup.sh                     # manual backup (also before upgrades)
docker image prune                    # clean old images (safe)
docker compose exec app /opt/mayan-edms/bin/mayan-edms.py changepassword admin

# --- database access (read-only inspection) ---
docker compose exec postgresql psql -U mayan -d mayan
```

---

*End of document.*
