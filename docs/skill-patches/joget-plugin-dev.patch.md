# Patch — joget-plugin-dev

Anchored to "Critical Rules (Non-Negotiable)".

---

## Critical Rules — ADD a subsection: External JDBC drivers in the OSGi bundle (ClickHouse)

> ### Embedded JDBC drivers — pin the transport, watch the shaded deps
> The `clickhouse-jdbc-0.6.x-all` (shaded) driver embedded in the bundle ships its own relocated Apache
> HttpClient5. Its INSERT/failover path engages `PoolingHttpClientConnectionManager`, whose `<clinit>`
> references `org.slf4j.LoggerFactory`. This bundle imports packages as `Import-Package: !*,<explicit
> list>` + `DynamicImport-Package: *`, and the dynamic import does **not** resolve `org.slf4j` at
> class-init → `ExceptionInInitializerError: NoClassDefFoundError: org/slf4j/LoggerFactory`. That error
> marks the JTA tx rollback-only (API HTTP 500) **and permanently poisons the shaded HTTP-client class
> for the whole JVM** — so the first ClickHouse call that touches the Apache pool makes every later
> ClickHouse call in that JVM fail (it passed once on a fresh JVM, failed on every repeat).
>
> **Rule:** build every ClickHouse `Connection` with the lightweight transport pinned, on BOTH read and
> write gateways:
> ```java
> Properties p = new Properties();
> if (user != null) p.setProperty("user", user);
> if (pass != null) p.setProperty("password", pass);
> p.setProperty("http_connection_provider", "HTTP_URL_CONNECTION");   // never the Apache pool
> try (Connection c = DriverManager.getConnection(url, p)) { ... }
> ```
> Alternatives (embed `slf4j-api`, or static `Import-Package: org.slf4j`) risk binding conflicts /
> bundle-resolution failure; the provider pin is the low-risk fix. A symptom that this bit you: an engine
> that works once after a restart and 500s on the second run in the same JVM.
>
> **General rule:** a shaded "-all" driver may carry transitive deps the OSGi layer can't see; prefer a
> connection mode that avoids the heavy path, and test the engine **twice in one JVM**, not once.
