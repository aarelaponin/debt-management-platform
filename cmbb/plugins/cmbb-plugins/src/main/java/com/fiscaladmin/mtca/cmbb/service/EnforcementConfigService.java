package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * EnforcementConfigService — DMBB-F08 enforcement-config administration (DM-FR-039/040/041).
 *
 * VALIDATE (DM-FR-039 "testable before production"): every enabled mdInstrument is consistent —
 * required attributes present, docTemplate resolves to an active mdTemplate, costRecorded
 * instruments have an mdLegalFee. RENDER (DM-FR-040 preview + DM-FR-041 consolidation): merge an
 * mdTemplate against a case's data and report how many notices would be generated under the
 * consolidation rule. Pure read/compute — never sends a notice or creates a document.
 */
public class EnforcementConfigService {

    public static final String F_INSTRUMENT = "mdInstrument";
    public static final String F_TEMPLATE = "mdTemplate";
    public static final String F_LEGALFEE = "mdLegalFee";
    public static final String F_NOTICERULE = "mdNoticeRule";
    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    public static final String F_LINE = "dmLine";
    private static final int FETCH_ALL = 100000;

    public static class Result {
        public boolean valid = true;
        public int issueCount = 0;
        public int warnCount = 0;
        public final List<String> issues = new ArrayList<>();

        @Override
        public String toString() {
            return "valid=" + valid + " issues=" + issueCount + " warnings=" + warnCount;
        }
    }

    public static class Preview {
        public int noticeCount = 0;
        public String subject = "";
        public String body = "";
        public String detail = "";
    }

    private final FormDataDao dao;

    public EnforcementConfigService(FormDataDao dao) {
        this.dao = dao;
        for (String f : new String[]{F_INSTRUMENT, F_TEMPLATE, F_LEGALFEE, F_NOTICERULE,
                F_CASE, F_DEBT, F_LINE}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    // ---------------- VALIDATE (DM-FR-039) ----------------

    public Result validate(String scope) {
        Result r = new Result();
        FormRowSet instruments = dao.find(F_INSTRUMENT, F_INSTRUMENT,
                "WHERE e.customProperties.enabled = ?1", new Object[]{"true"},
                "code", Boolean.TRUE, 0, FETCH_ALL);
        if (instruments == null) {
            return r;
        }
        for (FormRow ins : instruments) {
            String code = p(ins, "code");
            if (scope != null && !scope.isEmpty() && !scope.equals(code)) {
                continue;
            }
            if (p(ins, "name").isEmpty()) {
                err(r, code + ": missing name");
            }
            if (p(ins, "minCategory").isEmpty()) {
                err(r, code + ": missing minCategory (debt-size band)");
            }
            if (p(ins, "executionMode").isEmpty()) {
                err(r, code + ": missing executionMode");
            }
            String tpl = p(ins, "docTemplate");
            if (!tpl.isEmpty() && !templateActive(tpl)) {
                err(r, code + ": docTemplate '" + tpl + "' not found or inactive (DM-FR-040)");
            }
            if ("true".equalsIgnoreCase(p(ins, "costRecorded")) && !feeExists(code)) {
                err(r, code + ": records a cost but no mdLegalFee schedule (DM-FR-038)");
            }
            // DM-FR-039 recommended attributes (soft)
            if (p(ins, "taxpayerType").isEmpty() || p(ins, "instructions").isEmpty()) {
                r.warnCount++;
            }
        }
        r.valid = r.issueCount == 0;
        return r;
    }

    private void err(Result r, String msg) {
        r.issues.add(msg);
        r.issueCount++;
    }

    private boolean templateActive(String code) {
        FormRowSet rows = dao.find(F_TEMPLATE, F_TEMPLATE, "WHERE e.customProperties.code = ?1",
                new Object[]{code}, "version", Boolean.FALSE, 0, FETCH_ALL);
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (FormRow t : rows) {
            if (!"false".equalsIgnoreCase(p(t, "active"))) {
                return true;
            }
        }
        return false;
    }

    private boolean feeExists(String code) {
        Long specific = dao.count(F_LEGALFEE, F_LEGALFEE,
                "WHERE e.customProperties.instrument = ?1", new Object[]{code});
        if (specific != null && specific > 0) {
            return true;
        }
        Long def = dao.count(F_LEGALFEE, F_LEGALFEE,
                "WHERE e.customProperties.instrument = ?1", new Object[]{""});
        return def != null && def > 0;
    }

    // ---------------- RENDER (DM-FR-040 / DM-FR-041) ----------------

    public Preview render(String templateCode, String caseId, String modeOverride) {
        Preview pv = new Preview();
        FormRow tpl = templateByCode(templateCode);
        if (tpl == null) {
            pv.detail = "template '" + templateCode + "' not found";
            return pv;
        }
        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c == null) {
            pv.detail = "case '" + caseId + "' not found";
            return pv;
        }
        String mode = (modeOverride != null && !modeOverride.isEmpty())
                ? modeOverride : noticeMode(p(c, "caseType"));

        // tax-type breakdown from the case's debt lines
        Map<String, Double> byTax = new LinkedHashMap<>();
        double total = 0;
        FormRowSet lines = dao.find(F_LINE, F_LINE, "WHERE e.customProperties.caseId = ?1",
                new Object[]{caseId}, "dateCreated", Boolean.TRUE, 0, FETCH_ALL);
        if (lines != null) {
            for (FormRow l : lines) {
                double enf = num(p(l, "enforceable"));
                String tt = p(l, "taxType");
                byTax.merge(tt.isEmpty() ? "ALL" : tt, enf, Double::sum);
                total += enf;
            }
        }
        if (total == 0) {
            total = num(p(c, "amountAtStake"));
        }
        Set<String> taxTypes = byTax.isEmpty() ? new LinkedHashSet<>() : byTax.keySet();

        String renderTaxType;
        double renderAmount;
        if ("SEPARATE".equalsIgnoreCase(mode)) {
            pv.noticeCount = Math.max(1, taxTypes.size());
            renderTaxType = taxTypes.isEmpty() ? "" : taxTypes.iterator().next();
            renderAmount = taxTypes.isEmpty() ? total : byTax.get(renderTaxType);
        } else { // CONSOLIDATED_ALL / CONSOLIDATED_SELECTED
            pv.noticeCount = 1;
            renderTaxType = String.join("+", taxTypes);
            renderAmount = total;
        }

        Map<String, String> ctx = new LinkedHashMap<>();
        ctx.put("caseRef", p(c, "caseRef"));
        ctx.put("taxpayerName", p(c, "taxpayerName"));
        ctx.put("tin", p(c, "tin"));
        ctx.put("amountAtStake", fmt(renderAmount));
        ctx.put("taxType", renderTaxType);
        ctx.put("date", LocalDate.now().toString());

        pv.subject = merge(p(tpl, "subject"), ctx);
        pv.body = merge(p(tpl, "body"), ctx);
        pv.detail = "rendered " + templateCode + " mode=" + mode + " notices=" + pv.noticeCount;
        return pv;
    }

    private String noticeMode(String caseType) {
        FormRowSet rows = dao.find(F_NOTICERULE, F_NOTICERULE,
                "WHERE e.customProperties.caseType = ?1", new Object[]{caseType},
                "dateCreated", Boolean.FALSE, 0, 1);
        if (rows != null && !rows.isEmpty()) {
            String m = p(rows.get(0), "consolidationMode");
            if (!m.isEmpty()) {
                return m;
            }
        }
        return "SEPARATE";
    }

    private FormRow templateByCode(String code) {
        FormRowSet rows = dao.find(F_TEMPLATE, F_TEMPLATE, "WHERE e.customProperties.code = ?1",
                new Object[]{code}, "version", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    /** Replace #field# tokens from the merge context; unknown tokens are left as-is. */
    public static String merge(String template, Map<String, String> ctx) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        String out = template;
        for (Map.Entry<String, String> e : ctx.entrySet()) {
            out = out.replace("#" + e.getKey() + "#", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(Math.round(d * 100.0) / 100.0);
    }

    private static double num(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String p(FormRow r, String id) {
        return DeadlineService.prop(r, id);
    }
}
