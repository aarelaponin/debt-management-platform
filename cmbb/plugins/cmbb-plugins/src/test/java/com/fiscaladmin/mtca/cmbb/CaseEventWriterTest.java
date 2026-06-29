package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;

import org.joget.apps.form.model.FormRow;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;

/** Hash chain semantics — T-02.3: genesis prevHash="", hash=SHA-256(payload+prevHash). */
public class CaseEventWriterTest {

    @Test
    public void chainIsRecomputableAcrossAppends() {
        GuardTestHarness h = new GuardTestHarness();
        CaseEventWriter writer = new CaseEventWriter(h.dao);

        writer.append("case-1", "CASE_CREATED", "tester", "", "NEW", "case created", null);
        writer.append("case-1", "CASE_OPENED", "tester", "NEW", "OPEN", "case opened", null);

        FormRow first = h.events.get(0);
        FormRow second = h.events.get(1);

        assertEquals("", first.getProperty("prevHash")); // genesis
        assertEquals(first.getProperty("hash"), second.getProperty("prevHash"));

        // verification script semantics: recompute SHA-256(payload + prevHash)
        for (FormRow ev : h.events) {
            String recomputed = CaseEventWriter.sha256(
                    ev.getProperty("payload") + ev.getProperty("prevHash"));
            assertEquals(recomputed, ev.getProperty("hash"));
        }
    }

    @Test
    public void newWriterPicksUpChainFromStore() {
        GuardTestHarness h = new GuardTestHarness();
        new CaseEventWriter(h.dao)
                .append("case-1", "CASE_CREATED", "tester", "", "NEW", "created", null);
        // fresh writer (separate guard run) must chain from the stored row
        new CaseEventWriter(h.dao)
                .append("case-1", "STATE_CHANGED", "tester", "OPEN", "PENDING", "pending", null);

        assertEquals(h.events.get(0).getProperty("hash"),
                h.events.get(1).getProperty("prevHash"));
    }
}
