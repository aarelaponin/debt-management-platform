package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.MatrixValidator;

/** MatrixValidator (DAS P5) — the pure mmAuthority consistency gate. */
public class MatrixValidatorTest {

    private static Map<String, String> band(String min, String max, String level, String body, String quorum) {
        Map<String, String> m = new LinkedHashMap<String, String>();
        m.put("amountMin", min);
        m.put("amountMax", max);
        m.put("level", level);
        m.put("bodyType", body);
        m.put("quorum", quorum);
        return m;
    }

    @SafeVarargs
    private static List<Map<String, String>> bands(Map<String, String>... b) {
        List<Map<String, String>> l = new ArrayList<Map<String, String>>();
        for (Map<String, String> x : b) {
            l.add(x);
        }
        return l;
    }

    private static boolean anyIssue(MatrixValidator.Result r, String needle) {
        for (String i : r.issues) {
            if (i.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void cleanContiguousMatrixIsValid() {
        MatrixValidator.Result r = MatrixValidator.validate(bands(
                band("0", "5000", "SUPERVISOR", "SINGLE", ""),
                band("5000.01", "20000", "MANAGER", "SINGLE", ""),
                band("20000.01", "", "DIRECTOR", "COLLEGIAL", "2")), "2026-06-30");
        assertTrue(r.issues.toString(), r.valid);
    }

    @Test
    public void overlapIsFlagged() {
        MatrixValidator.Result r = MatrixValidator.validate(bands(
                band("0", "10000", "SUPERVISOR", "SINGLE", ""),
                band("5000", "20000", "MANAGER", "SINGLE", "")), "2026-06-30");
        assertFalse(r.valid);
        assertTrue(anyIssue(r, "overlap"));
    }

    @Test
    public void internalGapIsFlagged() {
        MatrixValidator.Result r = MatrixValidator.validate(bands(
                band("0", "5000", "SUPERVISOR", "SINGLE", ""),
                band("9000", "20000", "MANAGER", "SINGLE", "")), "2026-06-30");
        assertTrue(anyIssue(r, "gap"));
    }

    @Test
    public void unknownLevelIsFlagged() {
        MatrixValidator.Result r = MatrixValidator.validate(bands(
                band("0", "", "ARCHON", "SINGLE", "")), "2026-06-30");
        assertTrue(anyIssue(r, "unknown level"));
    }

    @Test
    public void chainMustAscendAndHaveTwoSteps() {
        assertTrue(anyIssue(MatrixValidator.validate(bands(
                band("0", "", "DIRECTOR,SUPERVISOR", "CHAIN", "")), "2026-06-30"), "not ascending"));
        assertTrue(anyIssue(MatrixValidator.validate(bands(
                band("0", "", "SUPERVISOR", "CHAIN", "")), "2026-06-30"), ">= 2 levels"));
        assertTrue(MatrixValidator.validate(bands(
                band("0", "", "SUPERVISOR,DIRECTOR", "CHAIN", "")), "2026-06-30").valid);
    }

    @Test
    public void collegialNeedsQuorumOfAtLeastTwo() {
        assertTrue(anyIssue(MatrixValidator.validate(bands(
                band("0", "", "DIRECTOR", "COLLEGIAL", "1")), "2026-06-30"), "quorum must be >= 2"));
        assertTrue(MatrixValidator.validate(bands(
                band("0", "", "DIRECTOR", "COLLEGIAL", "2")), "2026-06-30").valid);
    }

    @Test
    public void minGreaterThanMaxIsFlagged() {
        assertTrue(anyIssue(MatrixValidator.validate(bands(
                band("9000", "5000", "SUPERVISOR", "SINGLE", "")), "2026-06-30"), "amountMin > amountMax"));
    }

    @Test
    public void effectiveDatingPreventsFalseOverlap() {
        // an expired band and its replacement share the amount range but only one is effective today
        Map<String, String> old = band("0", "", "OFFICER", "SINGLE", "");
        old.put("effectiveTo", "2000-01-01");
        Map<String, String> cur = band("0", "", "DIRECTOR", "SINGLE", "");
        cur.put("effectiveFrom", "2020-01-01");
        MatrixValidator.Result r = MatrixValidator.validate(bands(old, cur), "2026-06-30");
        assertFalse("expired band not effective -> no overlap with the current one", anyIssue(r, "overlap"));
    }

    @Test
    public void emptyMatrixIsFlagged() {
        assertEquals(false, MatrixValidator.validate(bands(), "2026-06-30").valid);
    }
}
