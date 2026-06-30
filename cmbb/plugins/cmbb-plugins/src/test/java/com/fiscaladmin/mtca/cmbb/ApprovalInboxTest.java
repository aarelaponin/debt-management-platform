package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.ApprovalInbox;

/** ApprovalInbox (DAS P3b) — the pure "approvals mine to decide" eligibility rule. */
public class ApprovalInboxTest {

    private static Map<String, Object> ap(String id, String required, String requestedBy,
                                          String delegatedTo, String status) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("requiredLevel", required);
        m.put("requestedBy", requestedBy);
        m.put("delegatedTo", delegatedTo);
        m.put("status", status);
        return m;
    }

    private static List<Map<String, Object>> rows(Map<String, Object>... r) {
        List<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> x : r) {
            l.add(x);
        }
        return l;
    }

    private static List<String> ids(List<Map<String, Object>> rows) {
        List<String> out = new ArrayList<String>();
        for (Map<String, Object> r : rows) {
            out.add((String) r.get("id"));
        }
        return out;
    }

    @Test
    public void noResolvableAuthoritySeesEmptyInbox() {
        List<Map<String, Object>> in = rows(ap("a1", "SUPERVISOR", "clerk", "", "Pending"));
        assertEquals(0, ApprovalInbox.eligible(in, "nobody", "").size());      // unresolved level
        assertEquals(0, ApprovalInbox.eligible(in, "nobody", "JUNK").size());  // unknown level
    }

    @Test
    public void includesPendingAtOrBelowMyRankFromAnotherRequester() {
        List<Map<String, Object>> in = rows(ap("a1", "SUPERVISOR", "clerk", "", "Pending"));
        List<Map<String, Object>> out = ApprovalInbox.eligible(in, "boss", "DIRECTOR");
        assertEquals(ids(out).toString(), 1, out.size());
        assertTrue(ids(out).contains("a1"));
    }

    @Test
    public void excludesRequestsThatOutrankMe() {
        List<Map<String, Object>> in = rows(ap("a1", "DIRECTOR", "clerk", "", "Pending"));
        assertEquals(0, ApprovalInbox.eligible(in, "officer1", "OFFICER").size());
    }

    @Test
    public void excludesMyOwnRequests_fourEyes() {
        List<Map<String, Object>> in = rows(ap("a1", "SUPERVISOR", "boss", "", "Pending"));
        assertEquals(0, ApprovalInbox.eligible(in, "boss", "DIRECTOR").size());
    }

    @Test
    public void delegatedRequestShownOnlyToTheDelegate() {
        List<Map<String, Object>> toOther = rows(ap("a1", "SUPERVISOR", "clerk", "deputy", "Pending"));
        assertEquals(0, ApprovalInbox.eligible(toOther, "boss", "DIRECTOR").size());
        List<Map<String, Object>> toMe = rows(ap("a1", "SUPERVISOR", "clerk", "boss", "Pending"));
        assertEquals(1, ApprovalInbox.eligible(toMe, "boss", "DIRECTOR").size());
    }

    @Test
    public void excludesNonPending() {
        List<Map<String, Object>> in = rows(
                ap("a1", "SUPERVISOR", "clerk", "", "Approved"),
                ap("a2", "SUPERVISOR", "clerk", "", "Pending"));
        List<Map<String, Object>> out = ApprovalInbox.eligible(in, "boss", "DIRECTOR");
        assertEquals(1, out.size());
        assertTrue(ids(out).contains("a2"));
    }

    @Test
    public void blankUsernameAndNullRowsAreSafe() {
        assertEquals(0, ApprovalInbox.eligible(rows(ap("a1", "OFFICER", "x", "", "Pending")), "", "DIRECTOR").size());
        assertEquals(0, ApprovalInbox.eligible(null, "boss", "DIRECTOR").size());
    }
}
