package com.fiscaladmin.mtca.cmbb;

import java.math.BigDecimal;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

import com.fiscaladmin.mtca.cmbb.service.DeadlineService;

/**
 * CaseContextLoadBinder — form LOAD binder that prefills a new instalment / payment form
 * from the debtor whose TIN is on the URL (the worklist "Set up instalment plan" / "Record
 * payment" actions pass ?tin=). In ADD mode it resolves the debtor's primary open DM case
 * and returns tin + debtCaseId + totalDebt + debtCategory so the officer SEES the debt before
 * acting. In EDIT mode (a real record id) it defers entirely to the normal load — existing
 * agreements/payments open unchanged.
 */
public class CaseContextLoadBinder extends WorkflowFormBinder {

    public static final String F_CASE = "cmCase";
    public static final String F_DEBT = "dmDebt";
    private static final int FETCH_ALL = 100000;

    @Override
    public String getName() {
        return "DMBB Case Context Load Binder";
    }

    @Override
    public String getLabel() {
        return "DMBB Case Context Load Binder";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "[]";
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        FormRowSet existing = super.load(element, primaryKey, formData);
        boolean hasRecord = existing != null && !existing.isEmpty();
        boolean isAdd = primaryKey == null || primaryKey.isEmpty() || "null".equalsIgnoreCase(primaryKey);
        if (hasRecord || !isAdd) {
            return existing; // edit/view — untouched
        }
        String tin = formData == null ? null : formData.getRequestParameter("tin");
        if (tin == null || tin.trim().isEmpty()) {
            return existing;
        }
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            return existing;
        }
        FormRow caseRow = resolvePrimaryCase(dao, tin.trim());
        if (caseRow == null) {
            return existing;
        }
        FormRow dd = dao.load(F_DEBT, F_DEBT, caseRow.getId());
        FormRow out = new FormRow();
        out.setProperty("tin", tin.trim());
        out.setProperty("debtCaseId", caseRow.getId());
        out.setProperty("totalDebt", dd == null ? "" : DeadlineService.prop(dd, "consolidatedAmount"));
        out.setProperty("debtCategory", dd == null ? "" : DeadlineService.prop(dd, "debtCategory"));
        FormRowSet rs = new FormRowSet();
        rs.add(out);
        return rs;
    }

    private FormRow resolvePrimaryCase(FormDataDao dao, String tin) {
        FormRowSet cases = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.tin = ?1 AND e.customProperties.caseType = ?2",
                new Object[]{tin, "DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return null;
        }
        FormRow best = null;
        BigDecimal bestAmt = new BigDecimal("-1");
        for (FormRow c : cases) {
            if ("CLOSED".equalsIgnoreCase(DeadlineService.prop(c, "currentState"))) {
                continue;
            }
            FormRow dd = dao.load(F_DEBT, F_DEBT, c.getId());
            BigDecimal amt;
            try {
                String v = dd == null ? "" : DeadlineService.prop(dd, "consolidatedAmount");
                amt = (v == null || v.isEmpty()) ? BigDecimal.ZERO : new BigDecimal(v);
            } catch (NumberFormatException e) {
                amt = BigDecimal.ZERO;
            }
            if (amt.compareTo(bestAmt) > 0) {
                bestAmt = amt;
                best = c;
            }
        }
        return best;
    }
}
