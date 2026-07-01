package com.fiscaladmin.mtca.cmbb.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;

/**
 * DebtIdentificationService — DMBB-F03 (DM-FR-001/002, BR-DM-001..004) + ADR-004 §14
 * case constitution. Scans sta_v1 (GoldMartScanner), and for each TIN constitutes the
 * debt case(s) by the WORKFLOW that governs each tax line: a tax governed by a
 * tax-specific workflow (its selector pins taxType) forms its OWN single-tax case
 * (principal/interest/penalty/periods for that tax only); taxes governed by a
 * tax-agnostic workflow (or none) consolidate into ONE multi-tax case. Categorises
 * from the Gold band (no recompute), dedups (skip a TIN that already has a non-terminal
 * DM case), creates cmCase + dmDebt(1:1) + dmLine(1:many) via FormDataDao (P3), records
 * the governing workflow on dmDebt, appends CASE_IDENTIFIED, and starts the
 * cmCaseEnvelope through the injected ProcessStarter (testable without Joget's engine).
 */
public class DebtIdentificationService {

    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_LINE = "dmLine";
    public static final String F_STATE = "mmState";
    private static final String CLASS_NAME = DebtIdentificationService.class.getName();
    private static final int FETCH_ALL = 100000;
    /** DM-FR-018 high-value fast-track: config carrier + resolved policy (loaded per identify run). */
    private static final String F_POLICY = "mdContactPolicy";
    private BigDecimal highValueThreshold = null; // null => fast-track disabled (no active policy)
    private long telephoneSlaDays = 2;            // business days to first telephone contact

    /** Starts the case envelope for a created case (Joget impl injected at runtime). */
    public interface ProcessStarter {
        void start(String caseId, String assignee) throws Exception;
    }

    public static class Tally {
        public int created = 0;
        public int skipped = 0;
        public int failed = 0;

        @Override
        public String toString() {
            return "created=" + created + " skipped=" + skipped + " failed=" + failed;
        }
    }

    /** One constituted case: its lines, the tax it is pinned to ("" = consolidated), its workflow. */
    private static final class Group {
        final String taxType;       // single tax for a tax-specific case; "" for a consolidated case
        final String workflowCode;  // governing workflow (resolved)
        final List<GoldMartScanner.GoldLine> lines = new ArrayList<GoldMartScanner.GoldLine>();

        Group(String taxType, String workflowCode) {
            this.taxType = taxType;
            this.workflowCode = workflowCode;
        }
    }

    private final FormDataDao dao;
    private final GoldMartScanner scanner;
    private final CaseEventWriter events;
    private final ProcessStarter starter;
    private final WorkflowService workflows; // ADR-004 constitution resolver
    private final TenantContext tenants;     // ADR-004 invisible tenant scope

    public DebtIdentificationService(FormDataDao dao, GoldMartScanner scanner,
                                     CaseEventWriter events, ProcessStarter starter) {
        this.dao = dao;
        this.scanner = scanner;
        this.events = events;
        this.starter = starter;
        this.workflows = new WorkflowService(dao);
        this.tenants = new TenantContext(dao);
    }

    public Tally identify(String minAmount, String actor) {
        dao.updateSchema(F_CASE, F_CASE, new FormRowSet());
        dao.updateSchema(F_DEBT, F_DEBT, new FormRowSet());
        dao.updateSchema(F_LINE, F_LINE, new FormRowSet());
        dao.updateSchema(F_POLICY, F_POLICY, new FormRowSet());                              // DM-FR-018
        dao.updateSchema(DeadlineService.F_DEADLINE, DeadlineService.F_DEADLINE, new FormRowSet());
        loadFastTrackPolicy();                                                                // DM-FR-018
        Tally t = new Tally();
        Set<String> taken = tinsWithOpenDmCase();
        String tenant = tenants.resolve(actor);
        LocalDateTime now = LocalDateTime.now();
        for (GoldMartScanner.GoldDebtor d : scanner.scan(minAmount)) {
            if (d.tin == null || d.tin.isEmpty() || taken.contains(d.tin)) {
                t.skipped++;
                continue;
            }
            List<GoldMartScanner.GoldLine> lines = scanner.lines(d.tin);
            Map<String, Group> groups = constitute(tenant, d, lines, now);
            boolean anyStarted = false;
            for (Group g : groups.values()) {
                String caseId = UUID.randomUUID().toString();
                String amount = sumEnforceable(g.lines, d);
                boolean critical = isHighValue(amount); // DM-FR-018 fast-track
                saveCase(caseId, d, g, amount);
                saveDebt(caseId, d, g, amount);
                int n = saveLines(caseId, g.lines);
                events.append(caseId, "CASE_IDENTIFIED", actor, "", "",
                        "auto-identified from Gold (" + n + " line(s)"
                                + (g.taxType.isEmpty() ? ", consolidated" : ", tax " + g.taxType) + ")",
                        "\"tin\":\"" + CaseEventWriter.esc(d.tin) + "\",\"category\":\""
                                + CaseEventWriter.esc(d.debtCategory) + "\",\"workflow\":\""
                                + CaseEventWriter.esc(g.workflowCode) + "\"");
                if (critical) {
                    // DM-FR-018: high-value debt -> CRITICAL priority + first telephone contact within
                    // telephoneSlaDays business days. The deadline is a standard cmDeadline clock the
                    // DeadlineService sweep monitors/escalates like any other.
                    createTelephoneDeadline(caseId, now);
                    events.append(caseId, "CASE_PRIORITISED", actor, "", "",
                            "high-value fast-track: CRITICAL priority + first telephone contact within "
                                    + telephoneSlaDays + " business day(s)",
                            "\"amountAtStake\":\"" + CaseEventWriter.esc(amount)
                                    + "\",\"threshold\":\"" + CaseEventWriter.esc(
                                    highValueThreshold == null ? "" : highValueThreshold.toPlainString())
                                    + "\",\"clockCode\":\"TELEPHONE_CONTACT\"");
                }
                try {
                    starter.start(caseId, "admin");
                    t.created++;
                    anyStarted = true;
                } catch (Exception e) {
                    LogUtil.warn(CLASS_NAME, "processStart failed for case " + caseId
                            + " (TIN " + d.tin + "): " + e.getMessage());
                    t.failed++;
                }
            }
            if (anyStarted) {
                taken.add(d.tin);
            }
        }
        return t;
    }

    /**
     * ADR-004 §14 constitution: bucket a TIN's Gold lines into the case(s) that will carry
     * them. A line whose tax resolves to a tax-specific workflow joins that tax's own case;
     * every other line joins the single consolidated case. Order is preserved (LinkedHashMap)
     * so the consolidated case (if any) and per-tax cases come out deterministically.
     */
    private Map<String, Group> constitute(String tenant, GoldMartScanner.GoldDebtor d,
            List<GoldMartScanner.GoldLine> lines, LocalDateTime now) {
        LinkedHashMap<String, Group> groups = new LinkedHashMap<String, Group>();
        for (GoldMartScanner.GoldLine ln : lines) {
            FormRow wf = workflows.resolve(tenant, ln.taxType, "", "", d.debtCategory, now);
            boolean taxSpecific = wf != null && !DeadlineService.prop(wf, "taxType").isEmpty();
            String key;
            Group g;
            if (taxSpecific) {
                key = "TAX::" + ln.taxType;
                g = groups.get(key);
                if (g == null) {
                    g = new Group(ln.taxType, DeadlineService.prop(wf, "code"));
                    groups.put(key, g);
                }
            } else {
                key = "CONSOLIDATED";
                g = groups.get(key);
                if (g == null) {
                    // the consolidated case is governed by the tax-agnostic resolution
                    FormRow cwf = workflows.resolve(tenant, "", "", "", d.debtCategory, now);
                    g = new Group("", cwf == null ? "" : DeadlineService.prop(cwf, "code"));
                    groups.put(key, g);
                }
            }
            g.lines.add(ln);
        }
        if (groups.isEmpty()) {
            // a debtor with no lines still yields one (empty) consolidated case
            FormRow cwf = workflows.resolve(tenant, "", "", "", d.debtCategory, now);
            groups.put("CONSOLIDATED", new Group("", cwf == null ? "" : DeadlineService.prop(cwf, "code")));
        }
        return groups;
    }

    private void saveCase(String caseId, GoldMartScanner.GoldDebtor d, Group g, String amount) {
        FormRow c = new FormRow();
        c.setId(caseId);
        c.setProperty("caseType", "DM");
        c.setProperty("tin", d.tin);
        c.setProperty("origin", "SYSTEM");
        c.setProperty("category", d.debtCategory);
        c.setProperty("taxType", g.taxType); // single tax for a tax-specific case; blank = consolidated
        c.setProperty("amountAtStake", amount);
        c.setProperty("subjectKind", "DEBT");
        c.setProperty("subjectId", caseId);
        if (isHighValue(amount)) {
            // DM-FR-018: constitute the high-value debt as a CRITICAL-priority case. The critical
            // marker is the existing numeric `priority` field (=3, the top escalation band; a normal
            // case is unset). priorityBand/fastTrack are NOT cmCase form fields, so FormDataDao would
            // drop them on save — the durable fast-track markers are priority=3, the TELEPHONE_CONTACT
            // deadline, and the CASE_PRIORITISED event.
            c.setProperty("priority", "3");
        }
        save(F_CASE, c);
    }

    private void saveDebt(String caseId, GoldMartScanner.GoldDebtor d, Group g, String amount) {
        FormRow dd = new FormRow();
        dd.setId(caseId); // 1:1 — shares the case id
        dd.setProperty("tin", d.tin);
        dd.setProperty("debtCategory", d.debtCategory);
        dd.setProperty("stage", "Identified");
        dd.setProperty("triggerOrigin", "GOLD");
        dd.setProperty("consolidatedAmount", amount);
        dd.setProperty("workflowCode", g.workflowCode); // ADR-004: governing workflow recorded at birth
        dd.setProperty("asOf", "");
        save(F_DEBT, dd);
    }

    private int saveLines(String caseId, List<GoldMartScanner.GoldLine> lines) {
        int i = 0;
        for (GoldMartScanner.GoldLine ln : lines) {
            FormRow lr = new FormRow();
            lr.setId(caseId + "-L" + (++i));
            lr.setProperty("caseId", caseId);
            lr.setProperty("taxType", ln.taxType);
            lr.setProperty("yofa", ln.yofa);
            lr.setProperty("amount", nz(ln.amount));
            lr.setProperty("disputed", nz(ln.disputed));
            lr.setProperty("enforceable", nz(ln.enforceable));
            lr.setProperty("principal", nz(ln.principal));
            lr.setProperty("interest", nz(ln.interest));
            lr.setProperty("penalty", nz(ln.penalty));
            lr.setProperty("asOf", ln.asOf);
            save(F_LINE, lr);
        }
        return i;
    }

    /** Sum of a group's enforceable amounts; falls back to the TIN total for an empty group. */
    private static String sumEnforceable(List<GoldMartScanner.GoldLine> lines,
            GoldMartScanner.GoldDebtor d) {
        if (lines == null || lines.isEmpty()) {
            return nz(d.totalEnforceable);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (GoldMartScanner.GoldLine ln : lines) {
            total = total.add(dec(ln.enforceable));
        }
        return total.toPlainString();
    }

    /** DM-FR-018: true when fast-track is enabled and the amount exceeds the high-value threshold. */
    private boolean isHighValue(String amount) {
        return highValueThreshold != null && dec(amount).compareTo(highValueThreshold) > 0;
    }

    /** Load the active mdContactPolicy row (first governs); leaves fast-track disabled on any miss. */
    private void loadFastTrackPolicy() {
        highValueThreshold = null;
        telephoneSlaDays = 2;
        try {
            // Load all rows (proven null-condition pattern) and filter active in Java — the HQL
            // customProperties condition silently returns empty for a brand-new form's flag.
            FormRowSet rows = dao.find(F_POLICY, F_POLICY, null, null,
                    "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
            if (rows != null) {
                for (FormRow r : rows) {
                    if (!"true".equalsIgnoreCase(nz(r.getProperty("active")))) {
                        continue;
                    }
                    String t = nz(r.getProperty("highValueThreshold"));
                    if (t.isEmpty()) {
                        continue;
                    }
                    highValueThreshold = dec(t);
                    try {
                        long days = Long.parseLong(nz(r.getProperty("telephoneSlaDays")).trim());
                        if (days > 0) {
                            telephoneSlaDays = days;
                        }
                    } catch (NumberFormatException ignored) { /* keep default 2 */ }
                    break; // first active row with a threshold governs
                }
            }
        } catch (Exception e) {
            LogUtil.warn(CLASS_NAME, "fast-track policy load failed (fast-track disabled): " + e.getMessage());
            highValueThreshold = null;
        }
    }

    /** DM-FR-018: create the TELEPHONE_CONTACT deadline (business-day clock) for a critical case. */
    private void createTelephoneDeadline(String caseId, LocalDateTime now) {
        FormRow cal = new FormRow();
        cal.setProperty("workingDayMode", "WORKING"); // weekend-aware business days
        cal.setProperty("holidays", "");
        LocalDateTime due = DeadlineService.addDays(now, telephoneSlaDays, cal);
        long span = Math.max(1, Duration.between(now, due).toMinutes());
        FormRow dl = new FormRow();
        dl.setId(UUID.randomUUID().toString());
        dl.setProperty("caseId", caseId);
        dl.setProperty("clockCode", "TELEPHONE_CONTACT");
        dl.setProperty("startedAt", DeadlineService.ISO.format(now));
        dl.setProperty("dueAt", DeadlineService.ISO.format(due));
        dl.setProperty("warnAt", DeadlineService.ISO.format(now.plusMinutes(span * 75 / 100)));
        dl.setProperty("critAt", DeadlineService.ISO.format(now.plusMinutes(span * 90 / 100)));
        dl.setProperty("status", "RUNNING");
        dl.setProperty("pausedDays", "0");
        dl.setProperty("escalationLevel", "0");
        save(DeadlineService.F_DEADLINE, dl);
    }

    private static BigDecimal dec(String s) {
        try {
            return (s == null || s.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** TINs that already have a non-terminal DM case (dedup / consolidation). */
    private Set<String> tinsWithOpenDmCase() {
        Set<String> terminal = terminalStateCodes();
        Set<String> tins = new HashSet<String>();
        FormRowSet rows = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.caseType = ?1", new Object[]{"DM"},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (rows != null) {
            for (FormRow r : rows) {
                String state = nz(r.getProperty("currentState"));
                if (!terminal.contains(state)) {
                    String tin = r.getProperty("tin");
                    if (tin != null && !tin.isEmpty()) {
                        tins.add(tin);
                    }
                }
            }
        }
        return tins;
    }

    private Set<String> terminalStateCodes() {
        dao.updateSchema(F_STATE, F_STATE, new FormRowSet());
        FormRowSet rows = dao.find(F_STATE, F_STATE,
                "WHERE e.customProperties.isTerminal = ?1", new Object[]{"true"},
                "code", Boolean.FALSE, 0, FETCH_ALL);
        Set<String> codes = new HashSet<String>();
        if (rows != null) {
            for (FormRow r : rows) {
                String code = r.getProperty("code");
                if (code != null && !code.isEmpty()) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }

    private static String nz(String s) {
        return (s == null || s.isEmpty()) ? "0" : s;
    }
}
