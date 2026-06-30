package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Before;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.ApprovalService;
import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;

/**
 * ApprovalService unit tests (Decision &amp; Approval Service #6, minimal slice) on the
 * generic in-memory FormDataDao fake: matrix resolution, below-band auto-pass, Pending
 * routing, guarded approve firing the DecisionEffect once, SoD self-approval block,
 * mandatory reason, reject, gate-once idempotency, duplicate-request guard.
 */
public class ApprovalServiceTest {

    private static final Pattern EQ = Pattern.compile("customProperties\\.(\\w+)\\s*=\\s*\\?(\\d+)");

    private FormDataDao dao;
    private final Map<String, List<FormRow>> store = new LinkedHashMap<>();
    private int effectRuns;
    private final List<String> effected = new ArrayList<>();

    @Before
    public void setUp() {
        store.clear();
        effectRuns = 0;
        effected.clear();
        dao = mock(FormDataDao.class);
        when(dao.load(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            String form = inv.getArgument(0);
            String id = inv.getArgument(2);
            for (FormRow r : rows(form)) {
                if (id != null && id.equals(r.getId())) {
                    return r;
                }
            }
            return null;
        });
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> query(inv.getArgument(0), inv.getArgument(2),
                        inv.getArgument(3), (Integer) inv.getArgument(7)));
        doAnswer(inv -> {
            String form = inv.getArgument(0);
            FormRowSet set = inv.getArgument(2);
            for (FormRow r : set) {
                upsert(form, r);
            }
            return null;
        }).when(dao).saveOrUpdate(anyString(), anyString(), any(FormRowSet.class));

        // cmApproval lifecycle (DEFAULT scope) — the StatusManager guard reads these
        put("mmEntityTransition", row("entity", "cmApproval", "scope", "DEFAULT",
                "fromStatus", "Pending", "toStatus", "Approved"), "tr-ap-1");
        put("mmEntityTransition", row("entity", "cmApproval", "scope", "DEFAULT",
                "fromStatus", "Pending", "toStatus", "Rejected"), "tr-ap-2");
        put("mmEntityTransition", row("entity", "cmApproval", "scope", "DEFAULT",
                "fromStatus", "Pending", "toStatus", "Returned"), "tr-ap-3");
        // authority matrix: INSTALMENT_PLAN materiality > 5000 -> dm_supervisor
        put("mdApprovalPolicy", row("actionType", "INSTALMENT_PLAN", "threshold", "5000",
                "authorityRole", "dm_supervisor"), "pol-1");
    }

    private ApprovalService svc() {
        Map<String, ApprovalService.DecisionEffect> effects = new HashMap<>();
        effects.put("INSTALMENT_PLAN", (entity, recordId, actor, now) -> {
            effectRuns++;
            effected.add(recordId);
            return "finalised " + recordId;
        });
        return new ApprovalService(dao, new CaseEventWriter(dao), effects);
    }

    @Test
    public void belowBand_autoPasses_noRequest() {
        String r = svc().request("dmInstAgr", "agr-1", "INSTALMENT_PLAN", 4000,
                "clerk", "case-1", "clerk", LocalDateTime.now());
        assertTrue(r, r.startsWith("AUTO"));
        assertEquals(1, effectRuns);
        assertEquals(0, rows("cmApproval").size());
        assertEquals(1, ev("APPROVAL_NOT_REQUIRED"));
    }

    @Test
    public void aboveBand_createsPending_noEffect() {
        String r = svc().request("dmInstAgr", "agr-2", "INSTALMENT_PLAN", 6000,
                "clerk", "case-2", "clerk", LocalDateTime.now());
        assertTrue(r, r.startsWith("PENDING"));
        assertEquals(0, effectRuns);
        assertEquals(1, rows("cmApproval").size());
        FormRow ap = rows("cmApproval").get(0);
        assertEquals("Pending", ap.getProperty("status"));
        assertEquals("dm_supervisor", ap.getProperty("requiredAuthority"));
        assertEquals(1, ev("APPROVAL_REQUESTED"));
    }

    @Test
    public void approve_firesEffect_writesReasonedRecord() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-3", "INSTALMENT_PLAN", 6000, "clerk", "case-3", "clerk", LocalDateTime.now());
        String apId = rows("cmApproval").get(0).getId();
        String r = s.decide(apId, "boss", "approve", "within delegated authority", LocalDateTime.now());
        assertTrue(r, r.startsWith("APPROVED"));
        assertEquals("Approved", prop("cmApproval", apId, "status"));
        assertEquals("boss", prop("cmApproval", apId, "decidedBy"));
        assertEquals(1, effectRuns);
        assertEquals("agr-3", effected.get(0));
        assertEquals(1, ev("APPROVAL_DECISION"));
        assertTrue("guarded transition audited", ev("STATUS_CHANGED") >= 1);
    }

    @Test
    public void sod_blocksSelfApproval() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-4", "INSTALMENT_PLAN", 6000, "clerk", "case-4", "clerk", LocalDateTime.now());
        String apId = rows("cmApproval").get(0).getId();
        String r = s.decide(apId, "clerk", "approve", "trying to self-approve", LocalDateTime.now());
        assertTrue(r, r.startsWith("SoD"));
        assertEquals("Pending", prop("cmApproval", apId, "status"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_SOD_BLOCKED"));
    }

    @Test
    public void reason_isMandatory() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-5", "INSTALMENT_PLAN", 6000, "clerk", "case-5", "clerk", LocalDateTime.now());
        String apId = rows("cmApproval").get(0).getId();
        String r = s.decide(apId, "boss", "approve", "   ", LocalDateTime.now());
        assertEquals("reason required", r);
        assertEquals("Pending", prop("cmApproval", apId, "status"));
        assertEquals(0, effectRuns);
    }

    @Test
    public void reject_blocks_noEffect() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-6", "INSTALMENT_PLAN", 6000, "clerk", "case-6", "clerk", LocalDateTime.now());
        String apId = rows("cmApproval").get(0).getId();
        String r = s.decide(apId, "boss", "reject", "duration too long", LocalDateTime.now());
        assertEquals("REJECTED", r);
        assertEquals("Rejected", prop("cmApproval", apId, "status"));
        assertEquals(0, effectRuns);
    }

    @Test
    public void gateOnce_secondDecideIsNoop() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-7", "INSTALMENT_PLAN", 6000, "clerk", "case-7", "clerk", LocalDateTime.now());
        String apId = rows("cmApproval").get(0).getId();
        s.decide(apId, "boss", "approve", "ok", LocalDateTime.now());
        String r2 = s.decide(apId, "boss", "approve", "again", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("already decided"));
        assertEquals(1, effectRuns);
    }

    @Test
    public void duplicateRequest_isGuarded() {
        ApprovalService s = svc();
        s.request("dmInstAgr", "agr-8", "INSTALMENT_PLAN", 6000, "clerk", "case-8", "clerk", LocalDateTime.now());
        String r2 = s.request("dmInstAgr", "agr-8", "INSTALMENT_PLAN", 6000, "clerk", "case-8", "clerk", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("already pending"));
        assertEquals(1, rows("cmApproval").size());
    }

    // ---------------- fake store ----------------

    private FormRowSet query(String form, String cond, Object[] params, Integer limit) {
        FormRowSet out = new FormRowSet();
        List<String[]> preds = new ArrayList<>();
        if (cond != null) {
            Matcher m = EQ.matcher(cond);
            while (m.find()) {
                preds.add(new String[]{m.group(1), m.group(2)});
            }
        }
        for (FormRow r : rows(form)) {
            boolean ok = true;
            for (String[] pr : preds) {
                Object want = params[Integer.parseInt(pr[1]) - 1];
                if (!String.valueOf(want).equals(r.getProperty(pr[0]))) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(r);
            }
            if (limit != null && limit > 0 && out.size() >= limit) {
                break;
            }
        }
        return out;
    }

    private List<FormRow> rows(String form) {
        return store.computeIfAbsent(form, k -> new ArrayList<>());
    }

    private void put(String form, FormRow r, String id) {
        r.setId(id);
        rows(form).add(r);
    }

    private void upsert(String form, FormRow r) {
        List<FormRow> list = rows(form);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() != null && list.get(i).getId().equals(r.getId())) {
                list.set(i, r);
                return;
            }
        }
        list.add(r);
    }

    private String prop(String form, String id, String field) {
        for (FormRow r : rows(form)) {
            if (id.equals(r.getId())) {
                return r.getProperty(field);
            }
        }
        return null;
    }

    private long ev(String type) {
        return rows("cmEvent").stream().filter(e -> type.equals(e.getProperty("eventType"))).count();
    }

    private static FormRow row(String... kv) {
        FormRow r = new FormRow();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            r.setProperty(kv[i], kv[i + 1]);
        }
        return r;
    }
}
