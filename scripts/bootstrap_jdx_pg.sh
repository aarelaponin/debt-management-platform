#!/bin/bash
# bootstrap_jdx_pg.sh — stand up a Joget DX9 Enterprise instance on PostgreSQL.
# Encodes the jdx9 lessons (see docs/ops/BOOTSTRAP.md §1): shk* schema preload,
# OpenJDK 21 pin + ignite flag, directory seeding, glowroot port.
#
# Usage: bootstrap_jdx_pg.sh <name> <http_port> <shutdown_port> <glowroot_port> <db_name> [tarball] [schema_source_db]
# Example: bootstrap_jdx_pg.sh jdx10 8090 8020 4020 jwdb_int
set -euo pipefail

NAME=${1:?name}; HTTP=${2:?http port}; SHUT=${3:?shutdown port}; GLOW=${4:?glowroot port}; DB=${5:?db name}
TARBALL=${6:-$HOME/dev_install/joget_src/joget/joget-enterprise-linux-9.0.7.tar.gz}
SCHEMA_SRC=${7:-jwdb_gam}
DBUSER=joget_${DB#jwdb_}
JAVA21=/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home
PGHOST=localhost; PGADMIN=$(whoami)
VER=$(basename "$TARBALL" .tar.gz | sed 's/joget-enterprise-linux-//')
N=${NAME#jdx}
TGT=$HOME/joget-enterprise-linux-$VER-$N

[ -e "$TGT" ] && { echo "ERROR: $TGT already exists"; exit 1; }
for p in "$HTTP" "$SHUT" "$GLOW"; do
  lsof -nP -i :"$p" -sTCP:LISTEN >/dev/null 2>&1 && { echo "ERROR: port $p in use"; exit 1; }
done
[ -d "$JAVA21" ] || { echo "ERROR: OpenJDK 21 not at $JAVA21 (brew install openjdk@21)"; exit 1; }

echo "== 1/8 extract"
TMP=$(mktemp -d); tar xzf "$TARBALL" -C "$TMP"
mv "$TMP"/* "$TGT"; rmdir "$TMP"
TC=$(ls -d "$TGT"/apache-tomcat-*)

echo "== 2/8 postgresql jdbc driver"
DRIVER=$(ls "$HOME"/joget-enterprise-linux-*/apache-tomcat-*/lib/postgresql-*.jar 2>/dev/null | head -1)
[ -n "$DRIVER" ] || { echo "ERROR: no postgresql jar found in existing installs"; exit 1; }
cp "$DRIVER" "$TC/lib/"

echo "== 3/8 server.xml ports (shutdown $SHUT, http $HTTP)"
SX="$TC/conf/server.xml"
sed -i '' "s/<Server port=\"8005\"/<Server port=\"$SHUT\"/" "$SX"
awk -v p="$HTTP" 'BEGIN{done=0} /<Connector port="8080" protocol="HTTP\/1.1"/ && !done {sub(/port="8080"/, "port=\"" p "\""); done=1} {print}' "$SX" > "$SX.tmp" && mv "$SX.tmp" "$SX"

echo "== 4/8 datasource + tomcat.sh"
cat > "$TGT/wflow/app_datasource-default.properties" << EOF
workflowUser=$DBUSER
workflowPassword=$DBUSER
workflowDriver=org.postgresql.Driver
workflowUrl=jdbc\\:postgresql\\://localhost\\:5432/$DB?currentSchema\\=public
profileName=
EOF
echo "currentProfile=default" > "$TGT/wflow/app_datasource.properties"
cat > "$TGT/tomcat.sh" << EOF
#!/bin/sh
# Pinned to OpenJDK 21 (Shark engine breaks on JDK 17+ default; see BOOTSTRAP.md)
export JAVA_HOME=$JAVA21
export JAVA_OPTS="-Xmx768M -Dwflow.ignite=true -Dwflow.asyncRequestTimeout=5000 --add-opens=java.base/java.nio=ALL-UNNAMED -Dfile.encoding=UTF-8 -Dwflow.home=./wflow/ -javaagent:./wflow/wflow-cluster.jar -javaagent:./wflow/aspectjweaver-1.9.22.jar -javaagent:./wflow/glowroot/glowroot.jar"
$(basename "$TC")/bin/catalina.sh \$*
EOF
chmod +x "$TGT/tomcat.sh" "$TC"/bin/*.sh
echo "{\"web\":{\"port\":$GLOW,\"bindAddress\":\"0.0.0.0\"}}" > "$TGT/wflow/glowroot/admin.json"

echo "== 5/8 database $DB (user $DBUSER)"
psql -h $PGHOST -U "$PGADMIN" -d postgres -q -c \
  "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname='$DBUSER') THEN CREATE ROLE $DBUSER LOGIN PASSWORD '$DBUSER'; END IF; END \$\$;"
psql -h $PGHOST -U "$PGADMIN" -d postgres -tc "SELECT 1 FROM pg_database WHERE datname='$DB'" | grep -q 1 || \
  createdb -h $PGHOST -U "$PGADMIN" -O "$DBUSER" "$DB"
psql -h $PGHOST -U "$PGADMIN" -d "$DB" -q -c "GRANT ALL ON SCHEMA public TO $DBUSER;"

echo "== 6/8 preload Joget PG structure (incl. shk*) from $SCHEMA_SRC"
pg_dump -h $PGHOST -U "$PGADMIN" -d "$SCHEMA_SRC" --schema-only --no-owner --no-privileges -T 'app_fd_*' \
  | psql -h $PGHOST -U "$DBUSER" -d "$DB" -q
SHK=$(psql -h $PGHOST -U "$DBUSER" -d "$DB" -tc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name LIKE 'shk%'" | tr -d ' ')
[ "$SHK" -ge 60 ] || { echo "ERROR: shk* preload failed ($SHK tables)"; exit 1; }

echo "== 7/8 seed directory (roles + admin/admin)"
psql -h $PGHOST -U "$PGADMIN" -d "$SCHEMA_SRC" -c "\copy (SELECT * FROM dir_role) TO '/tmp/_bjp_roles.tsv'"
psql -h $PGHOST -U "$DBUSER" -d "$DB" -q -c "\copy dir_role FROM '/tmp/_bjp_roles.tsv'"
psql -h $PGHOST -U "$PGADMIN" -d "$SCHEMA_SRC" -c "\copy (SELECT * FROM dir_user WHERE username='admin') TO '/tmp/_bjp_admin.tsv'"
psql -h $PGHOST -U "$DBUSER" -d "$DB" -q -c "\copy dir_user FROM '/tmp/_bjp_admin.tsv'"
psql -h $PGHOST -U "$DBUSER" -d "$DB" -q -c "UPDATE dir_user SET password='21232f297a57a5a743894a0e4a801fc3' WHERE username='admin';"
psql -h $PGHOST -U "$DBUSER" -d "$DB" -q -c "INSERT INTO dir_user_role (userid, roleid) VALUES ('admin','ROLE_ADMIN') ON CONFLICT DO NOTHING;"
rm -f /tmp/_bjp_roles.tsv /tmp/_bjp_admin.tsv

echo "== 8/8 start + verify"
( cd "$TGT" && ./tomcat.sh start >/dev/null 2>&1 )
for i in $(seq 1 30); do
  CODE=$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://localhost:$HTTP/jw/web/login" 2>/dev/null || true)
  [ "$CODE" = "200" ] && break
  sleep 10
done
[ "$CODE" = "200" ] || { echo "ERROR: instance did not come up (last HTTP $CODE); see $TC/logs/catalina.out"; exit 1; }
echo "OK: http://localhost:$HTTP/jw (admin/admin)"

AJP=$((SHUT + 10))
cat << EOF

---- paste under 'instances:' in ~/.joget/instances.yaml ----
  $NAME:
    name: $NAME
    enabled: true
    version: "$VER"
    environment: development
    description: "MTCA instance $NAME - Joget $VER (PostgreSQL)"
    owner: "Dev Team"
    installation_path: $TGT
    tomcat:
      http_port: $HTTP
      https_port: 8443
      shutdown_port: $SHUT
      ajp_port: $AJP
      url: http://localhost:$HTTP/jw
      server_xml_pattern: apache-tomcat-*/conf/server.xml
    database:
      type: postgresql
      name: $DB
      postgresql_instance: pg1
      user: $DBUSER
      password_env: JOGET_$(echo "${DB#jwdb_}" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD
    glowroot:
      enabled: true
      port: $GLOW
      admin_json_pattern: wflow/glowroot/agent-*/admin.json
    credentials:
      username: admin
      password_env: $(echo "$NAME" | tr '[:lower:]' '[:upper:]')_PASSWORD
---- add to instance-manager .env ----
JOGET_$(echo "${DB#jwdb_}" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD=$DBUSER
$(echo "$NAME" | tr '[:lower:]' '[:upper:]')_PASSWORD=admin
EOF
