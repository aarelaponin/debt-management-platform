package com.fiscaladmin.mtca.cmbb;

import java.time.LocalDateTime;
import java.util.Map;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRowSet;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultApplicationPlugin;
import org.joget.workflow.util.WorkflowUtil;

import com.fiscaladmin.mtca.cmbb.service.CaseEventWriter;
import com.fiscaladmin.mtca.cmbb.service.PaymentService;

/**
 * PaymentEngine — DMBB payments. Form post-processor on dmPayment create (mode POST):
 * resolves the debtor's case from the TIN, reduces the outstanding debt, and on full
 * settlement closes the case and releases enforcement holds (PaymentService).
 */
public class PaymentEngine extends DefaultApplicationPlugin {

    private static final String CLASS_NAME = PaymentEngine.class.getName();
    private static final String F_PAY = "dmPayment";

    @Override
    public String getName() {
        return "DMBB Payment Engine";
    }

    @Override
    public String getVersion() {
        return "8.1-SNAPSHOT";
    }

    @Override
    public String getDescription() {
        return "Records a payment against a debt case and reduces the outstanding (DMBB payments).";
    }

    @Override
    public String getLabel() {
        return "DMBB Payment Engine";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(),
                "/properties/paymentEngine.json", null, true, null);
    }

    @Override
    public Object execute(Map properties) {
        FormDataDao dao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        if (dao == null) {
            throw new RuntimeException("PaymentEngine: formDataDao bean not available");
        }
        String actor = WorkflowUtil.getCurrentUsername();
        String payId = resolveId(dao, properties);
        if (payId == null) {
            LogUtil.warn(CLASS_NAME, "POST: no dmPayment resolvable — skipped");
            return null;
        }
        PaymentService svc = new PaymentService(dao, new CaseEventWriter(dao));
        String result = svc.post(payId, actor, LocalDateTime.now());
        LogUtil.info(CLASS_NAME, "POST " + payId + ": " + result);
        return null;
    }

    /** A just-created payment has a blank status (managed field dropped by the data API). */
    private String resolveId(FormDataDao dao, Map properties) {
        String configured = getPropertyString("recordId");
        if (configured != null && !configured.isEmpty() && !configured.startsWith("#")) {
            return configured;
        }
        Object fromMap = properties.get("recordId");
        if (fromMap instanceof String && !((String) fromMap).isEmpty()
                && !((String) fromMap).startsWith("#")) {
            return (String) fromMap;
        }
        dao.updateSchema(F_PAY, F_PAY, new FormRowSet());
        FormRowSet rows = dao.find(F_PAY, F_PAY,
                "WHERE e.customProperties.status IS NULL OR e.customProperties.status = ?1",
                new Object[]{""}, "dateCreated", Boolean.TRUE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0).getId();
    }
}
