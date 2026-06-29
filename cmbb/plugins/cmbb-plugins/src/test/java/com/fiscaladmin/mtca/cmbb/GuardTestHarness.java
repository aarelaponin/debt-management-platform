package com.fiscaladmin.mtca.cmbb;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.mockito.invocation.InvocationOnMock;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.CaseRefGenerator;
import com.fiscaladmin.mtca.cmbb.service.GuardContext;
import com.fiscaladmin.mtca.cmbb.service.MmConfigService;

/**
 * In-memory mm/cm store behind a mocked FormDataDao — one fake instead of
 * per-test stub forests (statement-importer test style, condensed).
 */
public class GuardTestHarness {

    public final FormDataDao dao = mock(FormDataDao.class);
    public FormRow caseRow;
    public FormRow typeRow;
    public final List<FormRow> states = new ArrayList<>();
    public final List<String[]> transitions = new ArrayList<>(); // {type, from, to}
    public final List<FormRow> otherCases = new ArrayList<>();
    public final List<FormRow> events = new ArrayList<>();
    public long openTasks = 0;
    // F03 allocation fixtures
    public final java.util.Map<String, FormRow> cases = new java.util.HashMap<>();
    public final List<FormRow> officers = new ArrayList<>();
    public final List<FormRow> allocPolicies = new ArrayList<>();
    public final List<FormRow> coiRules = new ArrayList<>();
    public final java.util.Map<String, FormRow> reassignOrders = new java.util.HashMap<>();
    public final List<FormRow> slaRows = new ArrayList<>();
    public final List<FormRow> calendars = new ArrayList<>();
    public final java.util.Map<String, FormRow> deadlines = new java.util.HashMap<>();
    public final List<FormRow> notifRules = new ArrayList<>();
    public final List<FormRow> templates = new ArrayList<>();
    public final List<FormRow> notifs = new ArrayList<>();
    public final List<FormRow> alerts = new ArrayList<>();
    public final java.util.Map<String, FormRow> docs = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> genOrders = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> postalRows = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> tasks = new java.util.HashMap<>();
    // F08 decisions / holds / links / pending-info fixtures
    public final java.util.Map<String, FormRow> holds = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> holdReleases = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> decisionRows = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> links = new java.util.LinkedHashMap<>();
    public final java.util.Map<String, FormRow> infoRequests = new java.util.HashMap<>();
    public final java.util.Map<String, FormRow> infoResponses = new java.util.HashMap<>();
    public final List<FormRow> authorities = new ArrayList<>();
    public final List<FormRow> linkTypes = new ArrayList<>();

    public GuardTestHarness() {
        when(dao.load(eq("cmCase"), eq("cmCase"), anyString()))
                .thenAnswer(i -> cases.getOrDefault(i.getArgument(2), caseRow));
        when(dao.load(eq("cmReassign"), eq("cmReassign"), anyString()))
                .thenAnswer(i -> reassignOrders.get(i.getArgument(2)));
        when(dao.load(eq("cmDoc"), eq("cmDoc"), anyString()))
                .thenAnswer(i -> docs.get(i.getArgument(2)));
        when(dao.load(eq("cmDocGen"), eq("cmDocGen"), anyString()))
                .thenAnswer(i -> genOrders.get(i.getArgument(2)));
        when(dao.load(eq("cmPostal"), eq("cmPostal"), anyString()))
                .thenAnswer(i -> postalRows.get(i.getArgument(2)));
        when(dao.load(eq("cmHold"), eq("cmHold"), anyString()))
                .thenAnswer(i -> holds.get(i.getArgument(2)));
        when(dao.load(eq("cmHoldRelease"), eq("cmHoldRelease"), anyString()))
                .thenAnswer(i -> holdReleases.get(i.getArgument(2)));
        when(dao.load(eq("cmDecision"), eq("cmDecision"), anyString()))
                .thenAnswer(i -> decisionRows.get(i.getArgument(2)));
        when(dao.load(eq("cmLink"), eq("cmLink"), anyString()))
                .thenAnswer(i -> links.get(i.getArgument(2)));
        when(dao.load(eq("cmInfoRequest"), eq("cmInfoRequest"), anyString()))
                .thenAnswer(i -> infoRequests.get(i.getArgument(2)));
        when(dao.load(eq("cmInfoResponse"), eq("cmInfoResponse"), anyString()))
                .thenAnswer(i -> infoResponses.get(i.getArgument(2)));
        when(dao.load(eq("cmTask"), eq("cmTask"), anyString()))
                .thenAnswer(i -> tasks.get(i.getArgument(2)));
        when(dao.load(eq("mdOfficerProfile"), eq("mdOfficerProfile"), anyString()))
                .thenAnswer(i -> officers.stream()
                        .filter(o -> i.getArgument(2).equals(o.getProperty("code")))
                        .findFirst().orElse(null));
        when(dao.find(anyString(), anyString(), anyString(), any(Object[].class),
                any(), any(), any(), any()))
                .thenAnswer(this::dispatchFind);
        when(dao.count(anyString(), anyString(), anyString(), any(Object[].class)))
                .thenAnswer(this::dispatchCount);
        doAnswer(this::dispatchSave).when(dao)
                .saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));
    }

    public GuardContext ctx(boolean requireDecision) {
        return new GuardContext(dao, new MmConfigService(dao), new CaseEventWriter(dao),
                new CaseRefGenerator(dao), "case-1", "tester", requireDecision);
    }

    public static FormRow row(String... kv) {
        FormRow r = new FormRow();
        r.setId("row-" + System.nanoTime());
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }

    public void seedTestType() {
        typeRow = row("code", "TEST", "name", "Acceptance test case type", "active", "true",
                "ttScope", "PARTY", "idFormat", "TT-??????", "dedupPolicy", "");
        states.add(row("caseType", "TEST", "code", "NEW", "envelopeState", "New"));
        states.add(row("caseType", "TEST", "code", "OPEN", "envelopeState", "Open"));
        states.add(row("caseType", "TEST", "code", "CLOSED", "envelopeState", "Closed",
                "isTerminal", "true"));
        transitions.add(new String[]{"TEST", "NEW", "OPEN"});
        transitions.add(new String[]{"TEST", "OPEN", "CLOSED"});
        caseRow = row("caseType", "TEST", "tin", "100058G", "origin", "MANUAL",
                "currentState", "", "caseRef", "", "taxType", "", "taxPeriod", "");
        caseRow.setId("case-1");
    }

    private static FormRowSet rs(FormRow... rows) {
        FormRowSet s = new FormRowSet();
        for (FormRow r : rows) {
            s.add(r);
        }
        return s;
    }

    private Object dispatchFind(InvocationOnMock inv) {
        String form = inv.getArgument(0);
        String cond = inv.getArgument(2) == null ? "" : inv.getArgument(2);
        Object[] p = inv.getArgument(3);
        FormRowSet out = new FormRowSet();
        if ("mmCaseType".equals(form)) {
            if (typeRow != null && p.length > 0 && p[0].equals(typeRow.getProperty("code"))) {
                out.add(typeRow);
            }
        } else if ("mmState".equals(form)) {
            for (FormRow s : states) {
                if (cond.contains("customProperties.code")) { // envelopeOf lookup
                    if (s.getProperty("caseType").equals(p[0])
                            && p[1].equals(s.getProperty("code"))) {
                        out.add(s);
                    }
                } else if (cond.contains("envelopeState")) {
                    if (s.getProperty("caseType").equals(p[0])
                            && p[1].equals(s.getProperty("envelopeState"))) {
                        out.add(s);
                    }
                } else if (cond.contains("isTerminal")
                        && "true".equals(s.getProperty("isTerminal"))) {
                    // 2 params = per-type (F02), 1 param = all types (F03 workload)
                    if (p.length == 1 || s.getProperty("caseType").equals(p[0])) {
                        out.add(s);
                    }
                }
            }
        } else if ("mdOfficerProfile".equals(form)) {
            for (FormRow o : officers) {
                if ("true".equals(o.getProperty("active"))) {
                    out.add(o);
                }
            }
        } else if ("mmAlloc".equals(form)) {
            for (FormRow a : allocPolicies) {
                if (a.getProperty("caseType").equals(p[0])) {
                    out.add(a);
                }
            }
        } else if ("mmCoi".equals(form)) {
            for (FormRow r : coiRules) {
                if (r.getProperty("caseType").equals(p[0])) {
                    out.add(r);
                }
            }
        } else if ("mmSla".equals(form)) {
            for (FormRow s : slaRows) {
                if (s.getProperty("caseType").equals(p[0])
                        && (p.length < 2 || p[1].equals(s.getProperty("clockCode")))) {
                    out.add(s);
                }
            }
        } else if ("mmCalendar".equals(form)) {
            for (FormRow s : calendars) {
                if (s.getProperty("code").equals(p[0])) {
                    out.add(s);
                }
            }
        } else if ("cmDeadline".equals(form)) {
            for (FormRow s : deadlines.values()) {
                String st = s.getProperty("status");
                if (cond.contains("caseId")) {
                    if (s.getProperty("caseId").equals(p[0])
                            && ("RUNNING".equals(st) || "PAUSED".equals(st))) {
                        out.add(s);
                    }
                } else if ("RUNNING".equals(st) || "PAUSED".equals(st)) {
                    out.add(s);
                }
            }
        } else if ("cmReassign".equals(form)) {
            // newest order without result (engine fallback) — tests use explicit ids
        } else if ("mmTransition".equals(form)) {
            // transitionsFrom(type, from)
            for (String[] t : transitions) {
                if (t[0].equals(p[0]) && t[1].equals(p[1])) {
                    out.add(row("caseType", t[0], "fromState", t[1], "toState", t[2]));
                }
            }
        } else if ("cmCase".equals(form)) {
            List<FormRow> all = new ArrayList<>(otherCases);
            all.addAll(cases.values());
            if (caseRow != null && !all.contains(caseRow)) {
                all.add(caseRow);
            }
            if (cond.contains("assignee =")) {
                for (FormRow r : all) {
                    if (p[0].equals(r.getProperty("assignee"))) {
                        boolean ok = true;
                        if (cond.contains("caseType =") && p.length > 1
                                && !p[1].equals(r.getProperty("caseType"))) {
                            ok = false;
                        }
                        if (ok) {
                            out.add(r);
                        }
                    }
                }
            } else if (cond.contains("caseRef =")) {
                for (FormRow r : all) {
                    if (p[0].equals(r.getProperty("caseRef"))) {
                        out.add(r);
                    }
                }
            } else { // F02 dedup condition — preserve original behaviour
                for (FormRow r : otherCases) {
                    out.add(r);
                }
            }
        } else if ("cmEvent".equals(form)) {
            if (cond.contains("eventType")) {
                for (FormRow e : events) {
                    if (p[0].equals(e.getProperty("eventType"))) {
                        out.add(e);
                    }
                }
            } else if (!events.isEmpty()) {
                out.add(events.get(events.size() - 1)); // latest (desc, limit 1)
            }
        } else if ("mmNotifRule".equals(form)) {
            for (FormRow r : notifRules) {
                if (r.getProperty("caseType").equals(p[0])) {
                    out.add(r);
                }
            }
        } else if ("mdTemplate".equals(form)) {
            for (FormRow t : templates) {
                if (t.getProperty("code").equals(p[0])
                        && t.getProperty("language").equals(p[1])
                        && "true".equals(t.getProperty("active"))) {
                    out.add(t);
                }
            }
        } else if ("cmDispatchRun".equals(form)) {
            // engine fallback path — tests pass explicit ids
        } else if ("mmAuthority".equals(form)) {
            for (FormRow a : authorities) {
                if (p.length > 0 && p[0].equals(a.getProperty("actionType"))) {
                    out.add(a);
                }
            }
        } else if ("mmLinkType".equals(form)) {
            for (FormRow lt : linkTypes) {
                if (p.length > 0 && p[0].equals(lt.getProperty("code"))) {
                    out.add(lt);
                }
            }
        }
        return out; // mmDocReq etc. -> empty
    }

    private Object dispatchCount(InvocationOnMock inv) {
        String form = inv.getArgument(0);
        String cond = inv.getArgument(2) == null ? "" : inv.getArgument(2);
        Object[] p = inv.getArgument(3);
        if ("mmTransition".equals(form)) {
            long n = 0;
            for (String[] t : transitions) {
                if (t[0].equals(p[0]) && t[1].equals(p[1]) && t[2].equals(p[2])) {
                    n++;
                }
            }
            return n;
        }
        if ("cmTask".equals(form)) {
            return openTasks;
        }
        if (("cmNotif".equals(form) || "cmAlert".equals(form)) && cond.contains("eventId")) {
            List<FormRow> src2 = "cmNotif".equals(form) ? notifs : alerts;
            long n = 0;
            for (FormRow r : src2) {
                if (p[0].equals(r.getProperty("eventId"))) {
                    n++;
                }
            }
            return n;
        }
        if ("cmHold".equals(form)) { // F08: scope suppression count
            long n = 0;
            for (FormRow hRow : holds.values()) {
                if (p.length >= 3 && p[0].equals(hRow.getProperty("caseId"))
                        && p[1].equals(hRow.getProperty("scope"))
                        && p[2].equals(hRow.getProperty("status"))) {
                    n++;
                }
            }
            return n;
        }
        if ("cmDecision".equals(form)) { // F08: closure gate + decision-maker COI
            long n = 0;
            for (FormRow d : decisionRows.values()) {
                if (cond.contains("tin")) {
                    if (p[0].equals(d.getProperty("tin"))
                            && p[1].equals(d.getProperty("decidedBy"))
                            && p[2].equals(d.getProperty("decisionStatus"))) {
                        n++;
                    }
                } else if (p[0].equals(d.getProperty("caseId"))
                        && p[1].equals(d.getProperty("decisionStatus"))) {
                    n++;
                }
            }
            return n;
        }
        if ("cmDoc".equals(form) && cond.contains("docClass")) {
            long n = 0;
            for (FormRow r : docs.values()) {
                if (p[0].equals(r.getProperty("caseId"))
                        && p[1].equals(r.getProperty("docClass"))) {
                    n++;
                }
            }
            return n;
        }
        if ("cmEvent".equals(form) && cond.contains("eventType")) {
            long n = 0;
            for (FormRow e : events) {
                if (p[0].equals(e.getProperty("eventType"))) {
                    n++;
                }
            }
            return n;
        }
        if ("cmCase".equals(form)) {
            long n = 0;
            List<FormRow> all = new ArrayList<>(otherCases);
            if (caseRow != null) {
                all.add(caseRow);
            }
            for (FormRow r : all) {
                String ref = r.getProperty("caseRef");
                if (cond.contains("caseRef <>")) {
                    if (ref != null && !ref.isEmpty() && p[0].equals(r.getProperty("caseType"))) {
                        n++;
                    }
                } else if (cond.contains("caseRef =") && p[0].equals(ref)) {
                    n++;
                }
            }
            return n;
        }
        return 0L;
    }

    private Object dispatchSave(InvocationOnMock inv) {
        String form = inv.getArgument(0);
        FormRowSet rows = inv.getArgument(2);
        if ("cmEvent".equals(form)) {
            events.addAll(rows);
        }
        if ("cmDeadline".equals(form)) {
            for (FormRow r : rows) {
                deadlines.put(r.getId(), r);
            }
        }
        if ("cmNotif".equals(form)) {
            notifs.addAll(rows);
        }
        if ("cmAlert".equals(form)) {
            alerts.addAll(rows);
        }
        if ("cmDoc".equals(form)) {
            for (FormRow r : rows) {
                docs.put(r.getId(), r);
            }
        }
        if ("cmTask".equals(form)) {
            for (FormRow r : rows) {
                tasks.put(r.getId(), r);
            }
        }
        if ("cmHold".equals(form)) {
            for (FormRow r : rows) {
                holds.put(r.getId(), r);
            }
        }
        if ("cmHoldRelease".equals(form)) {
            for (FormRow r : rows) {
                holdReleases.put(r.getId(), r);
            }
        }
        if ("cmDecision".equals(form)) {
            for (FormRow r : rows) {
                decisionRows.put(r.getId(), r);
            }
        }
        if ("cmLink".equals(form)) {
            for (FormRow r : rows) {
                links.put(r.getId(), r);
            }
        }
        if ("cmInfoRequest".equals(form)) {
            for (FormRow r : rows) {
                infoRequests.put(r.getId(), r);
            }
        }
        if ("cmInfoResponse".equals(form)) {
            for (FormRow r : rows) {
                infoResponses.put(r.getId(), r);
            }
        }
        return null;
    }
}
