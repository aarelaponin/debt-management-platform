package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.EscalationService;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/** EscalationService — DMBB-F04 ladder walk / category gate / idempotency (T-13 unit). */
public class EscalationServiceTest {

    private FormDataDao dao;
    private FormRow caseRow;
    private FormRow dmDebtRow;
    private final List<FormRow> strategies = new ArrayList<FormRow>();
    private final List<FormRow> workflows = new ArrayList<FormRow>(); // ADR-004 dmWorkflow carrier
    private final List<FormRow> steps = new ArrayList<FormRow>();
    private final List<FormRow> events = new ArrayList<FormRow>();
    private final List<FormRow> lines = new ArrayList<FormRow>();
    private final List<FormRow> debtTx = new ArrayList<FormRow>(); // dmDebt DM lifecycle (ADR-003 #6)

    @Before
    public void setUp() {
        dao = mock(FormDataDao.class);
        events.clear();
        lines.clear();
        caseRow = row("caseType", "DM", "category", "C6", "segment", "", "currentState", "OPEN");
        caseRow.setId("case-1");
        caseRow.setDateCreated(Date.from(LocalDateTime.now().minusDays(60)
                .atZone(ZoneId.systemDefault()).toInstant()));
        dmDebtRow = row("debtCategory", "C6", "stage", "Identified", "lastStepSeq", "0");
        dmDebtRow.setId("case-1");
        strategies.clear();
        strategies.add(row("code", "STD-MLT", "segment", "ALL", "categoryFloor", "C2",
                "version", "1", "active", "true"));
        // ADR-004: the workflow catalogue the escalation engine now resolves against.
        // W-DEFAULT (tax=ANY) is the default ladder; W-VAT (tax=VAT) is more specific.
        workflows.clear();
        workflows.add(row("code", "W-DEFAULT", "tenant", "MLT", "taxType", "", "segment", "",
                "industry", "", "categoryFloor", "C2", "validFrom", "2020-01-01", "validTo", "",
                "status", "Active", "version", "1"));
        workflows.add(row("code", "W-VAT", "tenant", "MLT", "taxType", "VAT", "segment", "",
                "industry", "", "categoryFloor", "C2", "validFrom", "2020-01-01", "validTo", "",
                "status", "Active", "version", "1"));
        steps.clear();
        steps.add(step("1", "Reminder", "7", "TPL-REMINDER"));
        steps.add(step("2", "Demand notice", "14", "TPL-DEMAND"));
        steps.add(step("3", "Final demand", "21", "TPL-FINAL"));
        // ADR-003 #6: the dmDebt DM lifecycle the migrated stage guard reads (mirrors the real
        // strategy stepNames in mmEscStep.csv).
        debtTx.clear();
        String[][] de = {{"Identified", "Reminder"}, {"Reminder", "Demand notice"},
            {"Demand notice", "Final demand"}, {"Final demand", "Bank garnishing"},
            {"Bank garnishing", "Field enforcement"}};
        for (String[] x : de) {
            debtTx.add(row("entity", "dmDebt", "scope", "DM", "fromStatus", x[0], "toStatus", x[1]));
        }
        // VAT override (ADR-003 §5 step 3): skips Reminder — Identified goes straight to Demand notice.
        String[][] vat = {{"Identified", "Demand notice"}, {"Demand notice", "Final demand"},
            {"Final demand", "Bank garnishing"}, {"Bank garnishing", "Field enforcement"}};
        for (String[] x : vat) {
            debtTx.add(row("entity", "dmDebt", "scope", "VAT", "fromStatus", x[0], "toStatus", x[1]));
        }

        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    String form = inv.getArgument(0);
                    FormRowSet out = new FormRowSet();
                    if ("cmCase".equals(form)) {
                        out.add(caseRow);
                    } else if ("mmStrategy".equals(form)) {
                        out.addAll(strategies);
                    } else if ("dmWorkflow".equals(form)) {
                        out.addAll(workflows);
                    } else if ("mmEscStep".equals(form)) {
                        out.addAll(steps);
                    } else if ("mmState".equals(form)) {
                        out.add(row("code", "CLOSED", "isTerminal", "true"));
                    } else if ("dmLine".equals(form)) {
                        out.addAll(lines);
                    } else if ("cmEvent".equals(form)) {
                        out.addAll(events); // CaseEventWriter chain reads
                    } else if ("mmEntityTransition".equals(form)) {
                        // scope-aware (params = {entity, scope}); lets VAT vs DM resolve correctly
                        Object[] ps = (Object[]) inv.getArgument(3);
                        String sc = ps != null && ps.length > 1 ? String.valueOf(ps[1]) : "";
                        for (FormRow r : debtTx) {
                            if (sc.equals(r.getProperty("scope"))) {
                                out.add(r);
                            }
                        }
                    }
                    return out;
                });
        when(dao.count(eq("mmEntityTransition"), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    Object[] ps = (Object[]) inv.getArgument(3);
                    String sc = ps != null && ps.length > 1 ? String.valueOf(ps[1]) : "";
                    long n = 0;
                    for (FormRow r : debtTx) {
                        if (sc.equals(r.getProperty("scope"))) {
                            n++;
                        }
                    }
                    return n;
                });
        when(dao.load(eq("dmDebt"), eq("dmDebt"), anyString())).thenReturn(dmDebtRow);
        doAnswer(inv -> {
            if ("cmEvent".equals(inv.getArgument(0))) {
                events.addAll((FormRowSet) inv.getArgument(2));
            }
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
    }

    private EscalationService svc() {
        return new EscalationService(dao, new MmConfigService(dao), new CaseEventWriter(dao));
    }

    private long ev(String type) {
        return events.stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    @Test
    public void walksAllDueStepsAndEmitsNotices() {
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(1, t.cases);
        assertEquals(3, t.escalated);   // cum 7,21,42 all <= 60 elapsed days
        assertEquals(0, t.skipped);
        assertEquals("3", dmDebtRow.getProperty("lastStepSeq"));
        assertEquals("Final demand", dmDebtRow.getProperty("stage"));
        assertEquals(3, ev("DEBT_ESCALATED"));
        assertEquals(3, ev("NOTIF_PENDING"));
    }

    @Test
    public void categoryFloorSkipsC1() {
        caseRow.setProperty("category", "C1");
        dmDebtRow.setProperty("debtCategory", "C1");
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(1, t.cases);
        assertEquals(0, t.escalated);
        assertEquals(1, t.skipped);
        assertEquals(0, ev("DEBT_ESCALATED"));
    }

    @Test
    public void idempotentNoRefire() {
        EscalationService svc = svc();
        svc.sweep("tester", LocalDateTime.now());
        long after1 = events.size();
        EscalationService.Tally t2 = svc.sweep("tester", LocalDateTime.now());
        assertEquals(0, t2.escalated);
        assertEquals(after1, events.size()); // nothing new fired
    }

    @Test
    public void onlyDueStepsFire() {
        // case 20 days old -> only steps with cum<=20 (step1 cum7, step2 cum21>20) => 1 step
        caseRow.setDateCreated(Date.from(LocalDateTime.now().minusDays(20)
                .atZone(ZoneId.systemDefault()).toInstant()));
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(1, t.escalated);
        assertEquals("Reminder", dmDebtRow.getProperty("stage"));
    }

    @Test
    public void vatSkipsReminderViaLifecycle() {
        // same strategy/timing, but the VAT lifecycle has no Identified->Reminder, so the
        // Reminder slot is consumed silently and Demand notice applies in its own slot (cum21).
        caseRow.setProperty("taxType", "VAT");
        caseRow.setDateCreated(Date.from(LocalDateTime.now().minusDays(25)
                .atZone(ZoneId.systemDefault()).toInstant())); // Demand notice (cum21) due, Final demand (cum42) not
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(1, t.escalated);                              // only Demand notice fired (Reminder skipped)
        assertEquals("Demand notice", dmDebtRow.getProperty("stage"));
        assertEquals("2", dmDebtRow.getProperty("lastStepSeq"));   // Reminder slot (seq1) consumed, not applied
        assertEquals(1, ev("DEBT_ESCALATED"));                     // a DM case at 25d would fire 2
    }

    @Test
    public void enforcementStepTriggersWhenEligible() {
        steps.add(enf("4", "Bank garnishing", "14", "BANK_GARNISH")); // cum 7+14+21+14=56 <= 60
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(4, t.escalated);
        assertEquals("4", dmDebtRow.getProperty("lastStepSeq"));
        assertEquals(1, ev("ENFORCEMENT_TRIGGERED"));
        assertEquals(0, ev("ENFORCEMENT_BLOCKED"));
    }

    @Test
    public void enforcementBlockedByFullObjection() {
        steps.add(enf("4", "Bank garnishing", "14", "BANK_GARNISH"));
        // full-amount objection: disputed >= enforceable on the case's lines (BR-DM-030)
        lines.add(row("caseId", "case-1", "enforceable", "1000", "disputed", "1000"));
        EscalationService.Tally t = svc().sweep("tester", LocalDateTime.now());
        assertEquals(3, t.escalated);                 // notices fired; enforcement held
        assertEquals("3", dmDebtRow.getProperty("lastStepSeq"));
        assertEquals(1, ev("ENFORCEMENT_BLOCKED"));
        assertEquals(0, ev("ENFORCEMENT_TRIGGERED"));
    }

    private static FormRow enf(String seq, String name, String triggerDays, String instrument) {
        return row("strategyCode", "STD-MLT", "seq", seq, "stepName", name,
                "triggerDays", triggerDays, "instrument", instrument, "noticeTemplate", "TPL-GARNISH");
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }

    private static FormRow step(String seq, String name, String triggerDays, String tpl) {
        return row("strategyCode", "STD-MLT", "seq", seq, "stepName", name,
                "triggerDays", triggerDays, "noticeTemplate", tpl);
    }
}
