package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.MmConfigService;
import com.fiscaladmin.mtca.cmbb.service.StatusManager;

/**
 * Unit tests for the config-driven {@link StatusManager} decision logic (ADR-003)
 * — scope resolution (wholesale shadowing), legal/illegal transitions, validNext,
 * terminal. Uses an in-memory {@link MmConfigService} so no DB/Joget runtime is
 * needed. Mirrors the live acceptance run_t27 (T-27.1..4).
 */
public class StatusManagerTest {

    /** In-memory config: keys are "entity|scope". */
    static class FakeCfg extends MmConfigService {
        final Map<String, FormRowSet> tx = new HashMap<String, FormRowSet>();
        final Map<String, Set<String>> terminal = new HashMap<String, Set<String>>();

        FakeCfg() { super(null); }

        void edge(String entity, String scope, String from, String to) {
            String k = entity + "|" + scope;
            FormRowSet rs = tx.get(k);
            if (rs == null) { rs = new FormRowSet(); tx.put(k, rs); }
            FormRow r = new FormRow();
            r.setProperty("fromStatus", from);
            r.setProperty("toStatus", to);
            rs.add(r);
        }

        void term(String entity, String scope, String... codes) {
            terminal.put(entity + "|" + scope, new HashSet<String>(Arrays.asList(codes)));
        }

        @Override public boolean entityScopeHasRules(String entity, String scope) {
            FormRowSet rs = tx.get(entity + "|" + scope);
            return rs != null && !rs.isEmpty();
        }
        @Override public FormRowSet entityTransitions(String entity, String scope) {
            FormRowSet rs = tx.get(entity + "|" + scope);
            return rs == null ? new FormRowSet() : rs;
        }
        @Override public Set<String> entityTerminalStatuses(String entity, String scope) {
            Set<String> t = terminal.get(entity + "|" + scope);
            return t == null ? new HashSet<String>() : t;
        }
    }

    private FakeCfg cfg;
    private StatusManager sm;

    @Before
    public void setup() {
        cfg = new FakeCfg();
        // dmAction (DM)
        cfg.edge("dmAction", "DM", "INITIATED", "EXECUTED");
        cfg.edge("dmAction", "DM", "INITIATED", "SUBMITTED");
        cfg.edge("dmAction", "DM", "INITIATED", "BLOCKED");
        cfg.edge("dmAction", "DM", "INITIATED", "REFERRED");
        cfg.edge("dmAction", "DM", "SUBMITTED", "CONFIRMED");
        // dmWriteOff (DM)
        cfg.edge("dmWriteOff", "DM", "SUBMITTED", "UNDER_REVIEW");
        cfg.edge("dmWriteOff", "DM", "UNDER_REVIEW", "APPROVED");
        cfg.term("dmWriteOff", "DM", "POSTED", "REJECTED");
        // dmDebt: DM has a Reminder step; VAT shadows it (Identified -> Demand directly)
        cfg.edge("dmDebt", "DM", "Identified", "Reminder");
        cfg.edge("dmDebt", "DM", "Reminder", "Demand");
        cfg.edge("dmDebt", "VAT", "Identified", "Demand");
        cfg.edge("dmDebt", "VAT", "Demand", "Final demand");
        sm = new StatusManager(null, cfg, null);
    }

    private static List<String> chain(String... s) { return Arrays.asList(s); }

    @Test  // T-27.1
    public void legalAllowed_illegalRejected() {
        assertTrue(sm.canTransition("dmWriteOff", chain("DM"), "SUBMITTED", "UNDER_REVIEW"));
        assertFalse(sm.canTransition("dmWriteOff", chain("DM"), "REJECTED", "ACTIVE"));
    }

    @Test  // T-27.2
    public void validNext_matchesConfig() {
        Set<String> next = sm.validNext("dmAction", chain("DM"), "INITIATED");
        assertEquals(new HashSet<String>(Arrays.asList("EXECUTED", "SUBMITTED", "BLOCKED", "REFERRED")),
                new HashSet<String>(next));
    }

    @Test  // T-27.3
    public void terminalHasNoNext() {
        assertTrue(sm.isTerminal("dmWriteOff", chain("DM"), "POSTED"));
        assertTrue(sm.validNext("dmWriteOff", chain("DM"), "POSTED").isEmpty());
    }

    @Test  // T-27.4 — per-tax wholesale shadowing
    public void vatScopeShadowsDm() {
        // most-specific scope with rules wins
        assertEquals("VAT", sm.resolveScope("dmDebt", chain("VAT", "DM")));
        // VAT lifecycle: Identified -> Demand is legal; DM requires Reminder first
        assertTrue(sm.canTransition("dmDebt", chain("VAT", "DM"), "Identified", "Demand"));
        assertFalse(sm.canTransition("dmDebt", chain("DM"), "Identified", "Demand"));
        // a scope with no rules falls through to the next (PIT -> DM)
        assertEquals("DM", sm.resolveScope("dmDebt", chain("PIT", "DM")));
        // nothing configured -> DEFAULT fallback
        assertEquals("DEFAULT", sm.resolveScope("dmAgent", chain("PIT")));
    }
}
