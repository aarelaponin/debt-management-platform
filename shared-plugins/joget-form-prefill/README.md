# joget-form-prefill

A **configurable form LOAD binder** for Joget DX. It pre-populates a *new* form from a
related record — by configuration only, with no per-form Java. Project-neutral: the bundle
depends only on the Joget form/commons API (`wflow-core`, `provided`), so the same JAR drops
into any Joget 8.1/9.x instance and any app.

It replaces two architecturally weak patterns: URL-parameter prefill (`?x=` → field default
`#requestParam.x#`, single value, ids leaked in links) and hand-written per-form load binders
(a new Java class + rebuild for every form). Here, one bundle is configured per form.

## How it works

On load, if the form is opening a **new** record and prefill is enabled, the binder:

1. resolves a **key** from an ordered list of sources (first non-empty wins);
2. looks up the source record via **`FormDataDao`** — `find` for the primary record, optional
   `load` for related records (**no raw SQL**);
3. applies post-find **filters** (eq/ne), orders (numeric or lexical, descending), picks the first;
4. **maps** fields onto this form (from the key, a primary field, an `alias.field` of a related
   record, or constants) and returns a synthetic row.

Opening an **existing** record (edit/view) defers entirely to the default load — untouched.
Any failure falls back to the default load; it never breaks the render.

## Build

```bash
mvn clean package          # → target/joget-form-prefill-1.0.0.jar  (runs 12 unit tests)
```

## Deploy

Copy the JAR into the instance's plugin folder (hot-reloads in ~10s), or upload via
**Manage Plugins**:

```bash
cp target/joget-form-prefill-1.0.0.jar <joget>/wflow/app_plugins/
```

It registers one plugin: `com.fiscaladmin.joget.formprefill.FormPrefillLoadBinder`.

## Configure (set the binder as a form's Load Binder)

Properties (all but the lookup essentials are optional):

| Property | Meaning |
|---|---|
| `enabled` | master on/off (default true) |
| `onlyOnAdd` | only prefill a new record (default true) |
| `keySources` | ordered `{source, name}` — `source` ∈ `requestParam`, `loginUser`, `currentField` |
| `formId` | lookup form id (its definition maps columns → fields) |
| `table` | lookup table (blank = same as `formId`) |
| `matchField` | the field that must equal the key |
| `filters` | extra `{field, op, value}` — `op` ∈ `eq` (default), `ne` |
| `orderBy` / `orderNumeric` / `pickFirst` | which row to take when several match |
| `related` | ordered `{formId, table, keyFrom, alias}` — `dao.load` by an id on the primary record |
| `mappings` | `{from, to}` — `from` is `key`, a primary field, or `alias.field`; `to` is a field on this form |
| `constants` | `{to, value}` — literal values |

> **Field names are Joget form field ids, not database columns.** Use `customerId`,
> `displayName`, … (the form's field ids), not the lowercased Postgres columns
> `c_customerid`, `c_displayname`. Getting this wrong silently prefills nothing.

### Generator opt-in (this repo's spec-driven pipeline)

`scripts/gen_forms.py` accepts `loadBinder: prefill` plus a `prefill:` block in a form
spec, and emits the binder + config automatically:

```yaml
form:
  id: dmInstAgr
  loadBinder: prefill
  prefill:
    keySources: [{source: requestParam, name: tin}]
    formId: cmCase
    matchField: tin
    filters:
      - {field: caseType, op: eq, value: DM}
      - {field: currentState, op: ne, value: CLOSED}
    orderBy: amountAtStake
    orderNumeric: true
    related: [{formId: dmDebt, keyFrom: id, alias: debt}]
    mappings:
      - {from: key, to: tin}
      - {from: id, to: debtCaseId}
      - {from: 'debt.consolidatedAmount', to: totalDebt}
```

## Proven examples

- **MTCA / DMBB** (jdx9): the instalment and payment forms prefill the debtor's open DM case
  and outstanding balance from the worklist actions (config above).
- **GAM** (jdx8, db `jwdb_gam`, banking): `demo-app-gam/` prefills a customer's name, type,
  risk and relationship manager from the `customerForm`/`customer` master — the *same JAR*,
  a different instance/database/domain, by configuration only. (`demo-app/` is the MTCA
  cross-app demo; `deploy_jdx8.py` is a thin deploy wrapper for jdx8, whose `instances.yaml`
  `url` is missing the `/jw` context.)

## Tests

`mvn test` runs 12 pure unit tests over `PrefillResolver` + `PrefillConfig` with an in-memory
`DataAccess` fake — key fallback, no-key, no-match, eq/ne filters, numeric pick-largest,
alias mapping, constants, bad-field rejection, and grid parsing. No Joget runtime needed.

## Layout

```
src/main/java/com/fiscaladmin/joget/formprefill/
  FormPrefillLoadBinder.java   # the Joget adapter (extends WorkflowFormBinder)
  PrefillResolver.java         # pure prefill logic (no Joget deps)
  PrefillConfig.java           # config model + parser
  DataAccess.java              # the FormDataDao seam (faked in tests)
  Activator.java               # registers the one plugin
```
