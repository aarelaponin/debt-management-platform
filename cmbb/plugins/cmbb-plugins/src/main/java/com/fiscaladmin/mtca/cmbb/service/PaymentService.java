package com.fiscaladmin.mtca.cmbb.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * PaymentService — DMBB payments (debt reduction). A payment is captured against a debt
 * case (the worklist "Record payment" action passes only the TIN). POST resolves the
 * debtor's primary open DM case, reduces the outstanding (dmDebt.consolidatedAmount and
 * the dmLine enforceable amounts, oldest year first), records applied + balance-after,
 * and on full settlement marks the debt Settled, closes the case (CLOSED) and releases
 * any enforcement-suppress hold. Idempotent: a payment already POSTED is not reprocessed.
 */
public class PaymentService {

    public static final String F_PAY = "dmPayment";
    public static final String F_DEBT = "dmDebt";
    public static final String F_CASE = "cmCase";
    public static final String F_LINE = "dmLine";
    public static final String F_HOLD = "cmHold";
    private static final int FETCH_ALL = 100000;

    private final FormDataDao dao;
    private final CaseEventWriter events;

    public PaymentService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
    }

    public String post(String payId, String actor, LocalDateTime now) {
        for (String f : new String[]{F_PAY, F_DEBT, F_CASE, F_LINE, F_HOLD}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
        FormRow pay = dao.load(F_PAY, F_PAY, payId);
        if (pay == null) {
            return "no payment " + payId;
        }
        if ("POSTED".equalsIgnoreCase(p(pay, "status"))) {
            return "already posted"; // idempotent
        }
        String tin = p(pay, "tin");
        String caseId = p(pay, "caseId");
        BigDecimal amount = dec(p(pay, "amount"));
        if (caseId.isEmpty() && !tin.isEmpty()) {
            FormRow primary = resolvePrimaryCase(tin);
            if (primary != null) {
                caseId = primary.getId();
            }
        }
        if (caseId.isEmpty()) {
            return reject(pay, "no open debt case for TIN " + tin);
        }
        FormRow dd = dao.load(F_DEBT, F_DEBT, caseId);
        if (dd == null) {
            return reject(pay, "case has no debt record");
        }
        if (amount.signum() <= 0) {
            return reject(pay, "payment amount must be positive");
        }
        BigDecimal outstanding = dec(p(dd, "consolidatedAmount"));
        BigDecimal applied = amount.min(outstanding);   // never over-apply
        BigDecimal balance = outstanding.subtract(applied).max(BigDecimal.ZERO);

        allocateToLines(caseId, applied);
        dd.setProperty("consolidatedAmount", balance.toPlainString());

        FormRow c = dao.load(F_CASE, F_CASE, caseId);
        if (c != null) {
            c.setProperty("amountAtStake", balance.toPlainString());
        }

        pay.setProperty("caseId", caseId);
        pay.setProperty("appliedAmount", applied.toPlainString());
        pay.setProperty("balanceAfter", balance.toPlainString());
        if (p(pay, "paymentDate").isEmpty()) {
            pay.setProperty("paymentDate", now.toLocalDate().toString());
        }
        pay.setProperty("status", "POSTED");

        event(caseId, "PAYMENT_RECEIVED", actor,
                "payment " + applied.toPlainString() + " applied; outstanding now " + balance.toPlainString(),
                "\"applied\":\"" + applied.toPlainString() + "\",\"balance\":\"" + balance.toPlainString() + "\"");

        boolean settled = balance.signum() == 0;
        if (settled) {
            dd.setProperty("stage", "Settled");
            if (c != null) {
                c.setProperty("currentState", "CLOSED");
            }
            releaseHolds(caseId, actor);
            event(caseId, "CASE_SETTLED", actor, "debt fully paid — case closed", "");
        }
        save(F_DEBT, dd);
        if (c != null) {
            save(F_CASE, c);
        }
        save(F_PAY, pay);
        return "POSTED applied=" + applied.toPlainString() + " balance=" + balance.toPlainString()
                + (settled ? " (SETTLED — case closed)" : "");
    }

    /** Reduce each line's enforceable amount, oldest year first, until the payment is consumed. */
    private void allocateToLines(String caseId, BigDecimal applied) {
        FormRowSet lines = dao.find(F_LINE, F_LINE, "WHERE e.customProperties.caseId = ?1",
                new Object[]{caseId}, "yofa", Boolean.TRUE, 0, FETCH_ALL);
        if (lines == null) {
            return;
        }
        BigDecimal left = applied;
        for (FormRow ln : lines) {
            if (left.signum() <= 0) {
                break;
            }
            BigDecimal enf = dec(p(ln, "enforceable"));
            BigDecimal cut = left.min(enf);
            ln.setProperty("enforceable", enf.subtract(cut).max(BigDecimal.ZERO).toPlainString());
            left = left.subtract(cut);
            save(F_LINE, ln);
        }
    }

    private void releaseHolds(String caseId, String actor) {
        FormRowSet holds = dao.find(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{caseId, "ENFORCEMENT_SUPPRESS", "ACTIVE"},
                "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (holds == null) {
            return;
        }
        for (FormRow h : holds) {
            h.setProperty("status", "RELEASED");
            save(F_HOLD, h);
        }
    }

    private FormRow resolvePrimaryCase(String tin) {
        FormRowSet cases = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.tin = ?1 AND e.customProperties.caseType = ?2",
                new Object[]{tin, "DM"}, "dateCreated", Boolean.FALSE, 0, FETCH_ALL);
        if (cases == null) {
            return null;
        }
        FormRow best = null;
        BigDecimal bestAmt = new BigDecimal("-1");
        for (FormRow c : cases) {
            if ("CLOSED".equalsIgnoreCase(p(c, "currentState"))) {
                continue;
            }
            FormRow dd = dao.load(F_DEBT, F_DEBT, c.getId());
            BigDecimal amt = dd == null ? BigDecimal.ZERO : dec(p(dd, "consolidatedAmount"));
            if (amt.compareTo(bestAmt) > 0) {
                bestAmt = amt;
                best = c;
            }
        }
        return best;
    }

    private String reject(FormRow pay, String reason) {
        pay.setProperty("status", "REJECTED");
        pay.setProperty("balanceAfter", reason);
        save(F_PAY, pay);
        return "REJECTED: " + reason;
    }

    private void event(String caseId, String type, String actor, String reason, String extra) {
        if (caseId != null && !caseId.isEmpty()) {
            events.append(caseId, type, actor, "", "", reason, extra);
        }
    }

    private static String p(FormRow r, String id) {
        return DeadlineService.prop(r, id);
    }

    private static BigDecimal dec(String s) {
        try {
            return (s == null || s.trim().isEmpty()) ? BigDecimal.ZERO : new BigDecimal(s.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
