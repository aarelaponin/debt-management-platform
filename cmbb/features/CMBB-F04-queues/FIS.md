# FIS — CMBB-F04 — Work Queues + Personal Worklist
Status: Accepted (T-04.1..5 6/6 PASS on jdx9, 2026-06-12; F02/F03 regression green)
CAD ref: CAD-CMBB §7 row F04

## 1. Traceability
| FR | AC (verbatim from M08 §4.13.2) | Realised by | Test |
|---|---|---|---|
| WF-FR-006 | "Work queue shows: case ID, TIN, taxpayer name, debt amount, risk score, priority (High/Medium/Low), days in queue, SLA status. Default sort by priority descending, then by queue entry date ascending. Queue refreshes in real time. Filter by case type, debt category, SLA status." | DL-list_queue (JdbcDataListBinder: computed priority from mdPriorityBand, days-in-queue, BR-WF-004 ordering; filters caseType/category/slaStatus) + cmCase field amendments | T-04.1, T-04.2 |
| WF-FR-009 | "Personal work list is the default landing page for case officers. Overdue actions highlighted in red. Actions due within 24 hours highlighted in amber. List sortable and filterable. Quick-action buttons for common activities (e.g., log call, schedule visit, generate letter)." | DL-list_worklist (JDBC, `assignee = #currentUser#`; red/amber HTML on nextActionDue) + UV delta (worklist = first menu, Queues category) | T-04.3, T-04.4 |

## 2. Business rules in scope
| BR | Enforcement point |
|---|---|
| BR-WF-004 (sort: risk DESC, SLA urgency DESC, debt amount DESC, entry date ASC; user may override per session) | ORDER BY in DL-list_queue SQL; column sort headers provide the session override |

## 3. Design decisions & assumptions
1. **A1 — JDBC binder is the sanctioned raw-SQL slot** (P3 mechanism ladder: raw SQL allowed in JdbcDataListBinder/Jasper). Computed columns (priority band, days in queue, highlight class) live in the queue SQL, not in plugins.
2. **A2 — cmCase carries queue columns as data**: amendments `taxpayerName`, `riskScore` (numeric), `nextActionDue` (date), `slaStatus` (managed by F05's DeadlineEngine; blank renders as `—` until F05). taxpayerName/riskScore arrive auto-filled from Gold when DMBB-F03 creates cases; manual creation leaves them officer-entered/blank.
3. **A3 — priority thresholds are config**: new MD form `mdPriorityBand` {code H/M/L, name, minScore}; queue SQL joins it (`riskScore >= minScore`, highest band wins). Seed: H≥70, M≥40, L≥0. Blank riskScore ⇒ M (documented default).
4. **A4 — "real time" refresh**: render-time computation + manual refresh on DEV; native auto-refresh interval is a userview/theme capability, recorded as OPEN (not silently dropped) — revisit with the theme item from DX9-DELTAS.
5. **A5 — quick-action buttons**: row action "Open case" now; log-call/schedule-visit/generate-letter become row actions when their carriers exist (cmNote exists → quick note action included; visits=task (F02 cmTask), letters=F07 ADG). Partial delivery recorded per row action.
6. **A6 — unit queue scope**: organisational-unit queue = join assignee→mdOfficerProfile.unit; type queues = caseType filter on the same datalist (one parameterised surface, not N copies).
7. No blockers — G2 may pass.

## 4. Configuration parameters introduced
| Parameter | Carrier | Default | Source FR |
|---|---|---|---|
| Priority bands (score thresholds) | mdPriorityBand rows | H≥70, M≥40, L≥0 | WF-FR-006 |
| Default queue sort | DL definition (regenerable artefact; per-session override via column headers) | BR-WF-004 order | BR-WF-004 |
| Amber window for due actions | DL-list_worklist SQL constant `24h` — promoted to mmSla at F05 | 24h | WF-FR-009 |

## 5. Generation order
1. forms/F-mdPriorityBand → 2. F-cmCase amendment (4 queue fields) → 3. datalists: DL-list_queue + DL-list_worklist (JDBC binder — gen_datalists.py extension: `binder: jdbc` + sql), list_mdPriorityBand companion → 4. UV delta (Queues category; worklist first = officer landing) → 5. seeds (mdPriorityBand) → 6. redeploy + T-04.x. No plugin, no XPDL change.
