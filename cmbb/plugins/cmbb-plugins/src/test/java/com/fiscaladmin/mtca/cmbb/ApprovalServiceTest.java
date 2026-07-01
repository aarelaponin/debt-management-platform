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
 * ApprovalService unit tests (Decision &amp; Approval Service #6) on the generic in-memory
 * FormDataDao fake. Covers the deepened routing (ADR-005): mmAuthority band resolution,
 * below-band auto-pass, SINGLE Pending routing + rank gate, guarded approve firing the
 * DecisionEffect once, CHAIN sequential advance (effect only at the last step), QUORUM
 * distinct-voter accumulation (duplicate vote ignored; effect at the Nth), rank-too-low block,
 * SoD self-approval block, mandatory reason, reject, gate-once idempotency, duplicate-request guard.
 *
 * <p>Matrix (mmAuthority, actionType INSTALMENT_PLAN, by materiality band):
 * 5000.01–20000 → SINGLE SUPERVISOR; 20000.01–50000 → CHAIN SUPERVISOR→DIRECTOR;
 * 50000.01–∞ → QUORUM 2×DIRECTOR. Below 5000.01 → no band → auto-pass.
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
        // unified authority matrix (mmAuthority) — three INSTALMENT_PLAN bands, one per topology
        put("mmAuthority", row("actionType", "INSTALMENT_PLAN", "amountMin", "5000.01",
                "amountMax", "20000", "level", "SUPERVISOR", "bodyType", "SINGLE"), "auth-ia-s");
        put("mmAuthority", row("actionType", "INSTALMENT_PLAN", "amountMin", "20000.01",
                "amountMax", "50000", "level", "SUPERVISOR,DIRECTOR", "bodyType", "CHAIN"), "auth-ia-c");
        put("mmAuthority", row("actionType", "INSTALMENT_PLAN", "amountMin", "50000.01",
                "amountMax", "", "level", "DIRECTOR", "bodyType", "COLLEGIAL", "quorum", "2"), "auth-ia-q");
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

    private String req(ApprovalService s, String agr, double amount, String caseId) {
        return s.request("dmInstAgr", agr, "INSTALMENT_PLAN", amount, "clerk", caseId, "clerk",
                LocalDateTime.now());
    }

    private String apId() {
        return rows("cmApproval").get(rows("cmApproval").size() - 1).getId();
    }

    // ---------------- SINGLE band + auto-pass ----------------

    @Test
    public void belowBand_autoPasses_noRequest() {
        String r = req(svc(), "agr-1", 4000, "case-1");
        assertTrue(r, r.startsWith("AUTO"));
        assertEquals(1, effectRuns);
        assertEquals(0, rows("cmApproval").size());
        assertEquals(1, ev("APPROVAL_NOT_REQUIRED"));
    }

    @Test
    public void singleBand_createsPending_noEffect() {
        String r = req(svc(), "agr-2", 6000, "case-2");
        assertTrue(r, r.startsWith("PENDING"));
        assertEquals(0, effectRuns);
        FormRow ap = rows("cmApproval").get(0);
        assertEquals("Pending", ap.getProperty("status"));
        assertEquals("SUPERVISOR", ap.getProperty("requiredAuthority"));
        assertEquals("SINGLE", ap.getProperty("routeKind"));
        assertEquals(1, ev("APPROVAL_REQUESTED"));
    }

    @Test
    public void single_approve_firesEffect_writesReasonedRecord() {
        ApprovalService s = svc();
        req(s, "agr-3", 6000, "case-3");
        String r = s.decide(apId(), "boss", "DIRECTOR", "approve", "within delegated authority",
                LocalDateTime.now());
        assertTrue(r, r.startsWith("APPROVED"));
        assertEquals("Approved", prop("cmApproval", apId(), "status"));
        assertEquals("boss", prop("cmApproval", apId(), "decidedBy"));
        assertEquals(1, effectRuns);
        assertEquals("agr-3", effected.get(0));
        assertTrue("decision recorded", ev("APPROVAL_DECISION") >= 1);
        assertTrue("guarded transition audited", ev("STATUS_CHANGED") >= 1);
    }

    @Test
    public void rankTooLow_blocked_staysPending() {
        ApprovalService s = svc();
        req(s, "agr-r", 6000, "case-r"); // SINGLE SUPERVISOR
        String r = s.decide(apId(), "junior", "OFFICER", "approve", "I'll allow it",
                LocalDateTime.now());
        assertTrue(r, r.startsWith("rank too low"));
        assertEquals("Pending", prop("cmApproval", apId(), "status"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_RANK_BLOCKED"));
    }

    // ---------------- CHAIN ----------------

    @Test
    public void chain_advancesStepByStep_effectOnlyAtEnd() {
        ApprovalService s = svc();
        req(s, "agr-c", 30000, "case-c"); // CHAIN SUPERVISOR -> DIRECTOR
        String id = apId();
        assertEquals("CHAIN", prop("cmApproval", id, "routeKind"));
        assertEquals("SUPERVISOR", prop("cmApproval", id, "requiredLevel"));
        // step 1: a supervisor signs off -> advances, no effect yet
        String s1 = s.decide(id, "sup", "SUPERVISOR", "approve", "step 1 ok", LocalDateTime.now());
        assertTrue(s1, s1.startsWith("chain advanced"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals("DIRECTOR", prop("cmApproval", id, "requiredLevel"));
        assertEquals(0, effectRuns);
        // step 2 by the SAME person is rejected as a duplicate voter (distinct sign-off per step)
        String dup = s.decide(id, "sup", "DIRECTOR", "approve", "trying both", LocalDateTime.now());
        assertTrue(dup, dup.startsWith("duplicate voter"));
        assertEquals(0, effectRuns);
        // step 2: a director completes the chain -> effect fires once
        String s2 = s.decide(id, "dir", "DIRECTOR", "approve", "step 2 ok", LocalDateTime.now());
        assertTrue(s2, s2.startsWith("APPROVED"));
        assertEquals("Approved", prop("cmApproval", id, "status"));
        assertEquals(1, effectRuns);
    }

    @Test
    public void chain_secondStepRankEnforced() {
        ApprovalService s = svc();
        req(s, "agr-c2", 30000, "case-c2");
        String id = apId();
        s.decide(id, "sup", "SUPERVISOR", "approve", "step 1", LocalDateTime.now());
        // step 2 requires DIRECTOR; another supervisor is too junior
        String r = s.decide(id, "sup2", "SUPERVISOR", "approve", "step 2", LocalDateTime.now());
        assertTrue(r, r.startsWith("rank too low"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals(0, effectRuns);
    }

    // ---------------- QUORUM ----------------

    @Test
    public void quorum_needsDistinctVoters_effectAtN() {
        ApprovalService s = svc();
        req(s, "agr-q", 70000, "case-q"); // QUORUM 2 x DIRECTOR
        String id = apId();
        assertEquals("QUORUM", prop("cmApproval", id, "routeKind"));
        String v1 = s.decide(id, "dir1", "DIRECTOR", "approve", "vote 1", LocalDateTime.now());
        assertTrue(v1, v1.startsWith("quorum 1/2"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals(0, effectRuns);
        // the same director voting again does not count
        String dup = s.decide(id, "dir1", "DIRECTOR", "approve", "vote again", LocalDateTime.now());
        assertTrue(dup, dup.startsWith("duplicate voter"));
        assertEquals(0, effectRuns);
        // a second distinct director reaches quorum -> effect
        String v2 = s.decide(id, "dir2", "DIRECTOR", "approve", "vote 2", LocalDateTime.now());
        assertTrue(v2, v2.startsWith("APPROVED"));
        assertEquals("Approved", prop("cmApproval", id, "status"));
        assertEquals(1, effectRuns);
    }

    // ---------------- invariants ----------------

    @Test
    public void sod_blocksSelfApproval() {
        ApprovalService s = svc();
        req(s, "agr-4", 6000, "case-4");
        String r = s.decide(apId(), "clerk", "DIRECTOR", "approve", "trying to self-approve",
                LocalDateTime.now());
        assertTrue(r, r.startsWith("SoD"));
        assertEquals("Pending", prop("cmApproval", apId(), "status"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_SOD_BLOCKED"));
    }

    @Test
    public void reason_isMandatory() {
        ApprovalService s = svc();
        req(s, "agr-5", 6000, "case-5");
        String r = s.decide(apId(), "boss", "DIRECTOR", "approve", "   ", LocalDateTime.now());
        assertEquals("reason required", r);
        assertEquals("Pending", prop("cmApproval", apId(), "status"));
        assertEquals(0, effectRuns);
    }

    @Test
    public void reject_blocks_noEffect() {
        ApprovalService s = svc();
        req(s, "agr-6", 6000, "case-6");
        String r = s.decide(apId(), "boss", "DIRECTOR", "reject", "duration too long",
                LocalDateTime.now());
        assertEquals("REJECTED", r);
        assertEquals("Rejected", prop("cmApproval", apId(), "status"));
        assertEquals(0, effectRuns);
    }

    @Test
    public void gateOnce_secondDecideIsNoop() {
        ApprovalService s = svc();
        req(s, "agr-7", 6000, "case-7");
        String id = apId();
        s.decide(id, "boss", "DIRECTOR", "approve", "ok", LocalDateTime.now());
        String r2 = s.decide(id, "boss", "DIRECTOR", "approve", "again", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("already decided"));
        assertEquals(1, effectRuns);
    }

    @Test
    public void duplicateRequest_isGuarded() {
        ApprovalService s = svc();
        req(s, "agr-8", 6000, "case-8");
        String r2 = req(s, "agr-8", 6000, "case-8");
        assertTrue(r2, r2.startsWith("already pending"));
        assertEquals(1, rows("cmApproval").size());
    }

    // ---------------- escalation / timeout / delegation ----------------

    @Test
    public void sweep_escalatesOverdue_bumpsRankOneStep() {
        ApprovalService s = svc();
        req(s, "agr-e", 6000, "case-e"); // SINGLE SUPERVISOR, deadline now+2d
        String id = apId();
        String r = s.sweep(LocalDateTime.now().plusDays(3), "cron");
        assertTrue(r, r.contains("escalated=1"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals("MANAGER", prop("cmApproval", id, "requiredLevel"));
        assertEquals("1", prop("cmApproval", id, "escalations"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_ESCALATED"));
    }

    @Test
    public void sweep_timesOutAfterMaxEscalations_rejects_noEffect() {
        ApprovalService s = svc();
        req(s, "agr-t", 6000, "case-t");
        String id = apId();
        s.sweep(LocalDateTime.now().plusDays(3), "cron"); // esc1 SUPERVISOR->MANAGER
        s.sweep(LocalDateTime.now().plusDays(6), "cron"); // esc2 MANAGER->DIRECTOR
        String r = s.sweep(LocalDateTime.now().plusDays(9), "cron"); // esc==MAX -> timeout
        assertTrue(r, r.contains("timedOut=1"));
        assertEquals("Rejected", prop("cmApproval", id, "status"));
        assertEquals("DIRECTOR", prop("cmApproval", id, "requiredLevel"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_TIMEOUT"));
    }

    @Test
    public void sweep_leavesInTimeRequestsUntouched() {
        ApprovalService s = svc();
        req(s, "agr-i", 6000, "case-i"); // deadline now+2d
        String r = s.sweep(LocalDateTime.now().plusDays(1), "cron"); // before the deadline
        assertTrue(r, r.contains("escalated=0") && r.contains("timedOut=0"));
        assertEquals("SUPERVISOR", prop("cmApproval", apId(), "requiredLevel"));
        assertEquals(0, ev("APPROVAL_ESCALATED"));
    }

    @Test
    public void delegate_recordsHandoff_staysPending() {
        ApprovalService s = svc();
        req(s, "agr-d", 6000, "case-d");
        String id = apId();
        String r = s.delegate(id, "boss", "deputy", "please cover for me", LocalDateTime.now());
        assertTrue(r, r.startsWith("DELEGATED"));
        assertEquals("deputy", prop("cmApproval", id, "delegatedTo"));
        assertEquals("boss", prop("cmApproval", id, "delegatedBy"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals(1, ev("APPROVAL_DELEGATED"));
    }

    @Test
    public void delegate_requiresTargetAndReason() {
        ApprovalService s = svc();
        req(s, "agr-d2", 6000, "case-d2");
        String id = apId();
        assertEquals("delegate target required", s.delegate(id, "boss", " ", "r", LocalDateTime.now()));
        assertEquals("reason required", s.delegate(id, "boss", "deputy", " ", LocalDateTime.now()));
        assertEquals(0, ev("APPROVAL_DELEGATED"));
    }

    // ---------------- config: SLA / max-escalations / effective-dating ----------------

    @Test
    public void slaDays_fromConfig_setsDeadline() {
        ApprovalService s = svc();
        put("mmAuthority", row("actionType", "TEST_SLA", "amountMin", "0", "amountMax", "",
                "level", "SUPERVISOR", "bodyType", "SINGLE", "slaDays", "5"), "auth-sla");
        s.request("dmInstAgr", "agr-sla", "TEST_SLA", 6000, "clerk", "case-sla", "clerk",
                LocalDateTime.of(2026, 6, 1, 9, 0));
        String dl = prop("cmApproval", apId(), "deadline");
        assertTrue(dl, dl.startsWith("2026-06-06")); // 1 June + 5 SLA days
    }

    @Test
    public void maxEscalations_fromConfig_honoured() {
        ApprovalService s = svc();
        put("mmAuthority", row("actionType", "TEST_ESC", "amountMin", "0", "amountMax", "",
                "level", "SUPERVISOR", "bodyType", "SINGLE", "maxEscalations", "1"), "auth-esc");
        LocalDateTime t = LocalDateTime.of(2026, 6, 1, 9, 0);
        s.request("dmInstAgr", "agr-esc", "TEST_ESC", 6000, "clerk", "case-esc", "clerk", t);
        String id = apId();
        s.sweep(t.plusDays(3), "cron"); // esc 0 < 1 -> escalate once
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals("1", prop("cmApproval", id, "escalations"));
        String r = s.sweep(t.plusDays(6), "cron"); // esc 1 >= max 1 -> timeout
        assertTrue(r, r.contains("timedOut=1"));
        assertEquals("Rejected", prop("cmApproval", id, "status"));
    }

    @Test
    public void effectiveDating_picksTheCurrentBand() {
        ApprovalService s = svc();
        put("mmAuthority", row("actionType", "TEST_DATE", "amountMin", "0", "amountMax", "",
                "level", "OFFICER", "bodyType", "SINGLE", "effectiveTo", "2000-01-01"), "auth-old");
        put("mmAuthority", row("actionType", "TEST_DATE", "amountMin", "0", "amountMax", "",
                "level", "DIRECTOR", "bodyType", "SINGLE", "effectiveFrom", "2020-01-01"), "auth-new");
        s.request("dmInstAgr", "agr-dt", "TEST_DATE", 6000, "clerk", "case-dt", "clerk",
                LocalDateTime.now());
        // the expired band (effectiveTo 2000) is skipped; the current one (effectiveFrom 2020) wins
        assertEquals("DIRECTOR", prop("cmApproval", apId(), "requiredLevel"));
    }

    // ---------------- conflict of interest (#3) ----------------

    @Test
    public void coi_barredApprover_blocked_staysPending() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-X"), "caseCOI");
        put("mmCoi", row("caseType", "DM", "ruleType", "EXCLUDE_APPROVER",
                "expression", "*|TIN-X"), "coi-1"); // nobody may approve TIN-X's requests
        s.request("dmInstAgr", "agr-coi", "INSTALMENT_PLAN", 6000, "clerk", "caseCOI", "clerk",
                LocalDateTime.now());
        String id = apId();
        String r = s.decide(id, "boss", "DIRECTOR", "approve", "trying to approve", LocalDateTime.now());
        assertTrue(r, r.startsWith("COI"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        assertEquals(0, effectRuns);
        assertEquals(1, ev("APPROVAL_COI_BLOCKED"));
    }

    @Test
    public void coi_unbarredSubject_passes() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-Y"), "caseOK");
        put("mmCoi", row("caseType", "DM", "ruleType", "EXCLUDE_APPROVER",
                "expression", "*|TIN-X"), "coi-1"); // bars TIN-X only
        s.request("dmInstAgr", "agr-ok", "INSTALMENT_PLAN", 6000, "clerk", "caseOK", "clerk",
                LocalDateTime.now());
        String id = apId();
        String r = s.decide(id, "boss", "DIRECTOR", "approve", "ok", LocalDateTime.now());
        assertTrue(r, r.startsWith("APPROVED")); // TIN-Y is not barred
        assertEquals(1, effectRuns);
    }

    @Test
    public void coi_barNamedApproverAcrossCaseType() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-Z"), "caseZ");
        put("mmCoi", row("caseType", "DM", "ruleType", "EXCLUDE_APPROVER",
                "expression", "boss|*"), "coi-2"); // 'boss' barred for every DM subject
        s.request("dmInstAgr", "agr-z", "INSTALMENT_PLAN", 6000, "clerk", "caseZ", "clerk",
                LocalDateTime.now());
        String id = apId();
        assertTrue(s.decide(id, "boss", "DIRECTOR", "approve", "x", LocalDateTime.now()).startsWith("COI"));
        // a different approver is fine
        assertTrue(s.decide(id, "chief", "DIRECTOR", "approve", "ok", LocalDateTime.now()).startsWith("APPROVED"));
        assertEquals(1, effectRuns);
    }

    // ---------------- P4: delegation binding + lifecycle notifications ----------------

    @Test
    public void delegationBindsTheDelegate() {
        ApprovalService s = svc();
        req(s, "agr-bind", 6000, "case-bind");   // SUPERVISOR band; requester = clerk
        String id = apId();
        s.delegate(id, "boss", "deputy", "please handle", LocalDateTime.now());
        // an otherwise-eligible non-delegate is blocked; the request stays Pending
        String blocked = s.decide(id, "manager", "DIRECTOR", "approve", "I'll take it", LocalDateTime.now());
        assertTrue(blocked, blocked.contains("not the delegate"));
        assertEquals(1, ev("APPROVAL_DELEGATE_BLOCKED"));
        assertEquals("Pending", prop("cmApproval", id, "status"));
        // the named delegate decides -> proceeds
        String ok = s.decide(id, "deputy", "DIRECTOR", "approve", "handling as delegated", LocalDateTime.now());
        assertTrue(ok, ok.startsWith("APPROVED"));
        assertEquals("Approved", prop("cmApproval", id, "status"));
    }

    @Test
    public void requestNotifiesTheAssignedRole() {
        ApprovalService s = svc();
        req(s, "agr-asn", 6000, "case-asn");   // SUPERVISOR band -> assigned to dm_supervisor
        assertTrue("assignment notification queued to the role", notifWith("dm_supervisor") >= 1);
        assertTrue("assignment alertType", notifWith("APPROVAL_ASSIGNED") >= 1);
    }

    @Test
    public void delegationNotifiesTheDelegate() {
        ApprovalService s = svc();
        req(s, "agr-dn", 6000, "case-dn");
        s.delegate(apId(), "boss", "deputy", "cover me", LocalDateTime.now());
        assertTrue("the named delegate is notified", notifWith("deputy") >= 1);
        assertTrue(notifWith("APPROVAL_DELEGATED") >= 1);
    }

    @Test
    public void escalationAndTimeoutNotify() {
        ApprovalService s = svc();
        req(s, "agr-esc", 6000, "case-esc");
        s.sweep(LocalDateTime.now().plusDays(3), "cron");   // escalate 1 -> MANAGER
        assertTrue("escalation notifies the escalated-to role", notifWith("APPROVAL_ESCALATED") >= 1);
        s.sweep(LocalDateTime.now().plusDays(6), "cron");   // escalate 2 -> DIRECTOR
        s.sweep(LocalDateTime.now().plusDays(9), "cron");   // exhausted -> timeout
        assertTrue("timeout is notified", notifWith("APPROVAL_TIMEOUT") >= 1);
        assertTrue("timeout notifies the originator", notifWith("clerk") >= 1);
    }

    private long notifWith(String substr) {
        return rows("cmEvent").stream()
                .filter(e -> "NOTIF_PENDING".equals(e.getProperty("eventType")))
                .filter(e -> {
                    String pl = e.getProperty("payload");
                    return pl != null && pl.contains(substr);
                }).count();
    }

    // ---------------- P5: auto-derived COI ----------------

    @Test
    public void autoCoiBarsASecondDecisionForTheSameTaxpayer() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-AC"), "caseAC1");
        put("cmCase", row("caseType", "DM", "tin", "TIN-AC"), "caseAC2"); // same taxpayer, another case
        put("mmCoi", row("caseType", "DM", "ruleType", "EXCLUDE_DECISION_MAKER", "expression", "*"), "coi-dm-dec");
        req(s, "agr-ac1", 6000, "caseAC1");
        assertTrue(s.decide(apId(), "boss", "SUPERVISOR", "approve", "ok1", LocalDateTime.now()).startsWith("APPROVED"));
        req(s, "agr-ac2", 6000, "caseAC2");
        String ap2 = apId();
        String r2 = s.decide(ap2, "boss", "SUPERVISOR", "approve", "ok2", LocalDateTime.now());
        assertTrue(r2, r2.contains("auto-COI"));
        assertEquals(1, ev("APPROVAL_AUTOCOI_BLOCKED"));
        assertEquals("Pending", prop("cmApproval", ap2, "status"));
    }

    @Test
    public void autoCoiIsOffWithoutTheRule() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-AD"), "caseAD1");
        put("cmCase", row("caseType", "DM", "tin", "TIN-AD"), "caseAD2");
        req(s, "agr-ad1", 6000, "caseAD1");
        s.decide(apId(), "boss", "SUPERVISOR", "approve", "ok", LocalDateTime.now());
        req(s, "agr-ad2", 6000, "caseAD2");
        String r2 = s.decide(apId(), "boss", "SUPERVISOR", "approve", "ok", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("APPROVED")); // no EXCLUDE_DECISION_MAKER rule -> allowed
    }

    @Test
    public void autoCoiAllowsADifferentTaxpayer() {
        ApprovalService s = svc();
        put("cmCase", row("caseType", "DM", "tin", "TIN-AE1"), "caseAE1");
        put("cmCase", row("caseType", "DM", "tin", "TIN-AE2"), "caseAE2"); // different taxpayer
        put("mmCoi", row("caseType", "DM", "ruleType", "EXCLUDE_DECISION_MAKER", "expression", "*"), "coi-dm-dec2");
        req(s, "agr-ae1", 6000, "caseAE1");
        s.decide(apId(), "boss", "SUPERVISOR", "approve", "ok", LocalDateTime.now());
        req(s, "agr-ae2", 6000, "caseAE2");
        String r2 = s.decide(apId(), "boss", "SUPERVISOR", "approve", "ok", LocalDateTime.now());
        assertTrue(r2, r2.startsWith("APPROVED")); // a different taxpayer is fine
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
