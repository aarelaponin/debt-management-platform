package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * DebtorsListService — DMBB-F11 registry extract (DM-FR-058).
 *
 * The website-publication lifecycle (qualify → publish → remove on resolution) is F07's
 * dmDebtorPub + mdPublishRule + PUBLISH/RELEASE sweep. This service adds only the registry
 * EXTRACT: gather the currently PUBLISHED dmDebtorPub entries and produce the file/API hand-off
 * record (count, total, delimited payload). The transport to the external registry is bound at
 * deployment; this is the audited extract record. Resolved debtors are excluded automatically
 * because F07's RELEASE sweep flips them to REMOVED.
 */
public class DebtorsListService {

    public static final String F_EXTRACT = "dmDebtorsExtract";
    public static final String F_PUB = "dmDebtorPub";
    private static final int FETCH_ALL = 100000;

    private final FormDataDao dao;
    private final CaseEventWriter events;

    public DebtorsListService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
        for (String f : new String[]{F_EXTRACT, F_PUB}) {
            dao.updateSchema(f, f, new FormRowSet());
        }
    }

    public String extract(String extractId, String actor, LocalDateTime now) {
        FormRow x = dao.load(F_EXTRACT, F_EXTRACT, extractId);
        if (x == null) {
            return "no extract " + extractId;
        }
        if (!p(x, "status").isEmpty()) {
            return "already processed (" + p(x, "status") + ")";
        }
        String format = p(x, "format").isEmpty() ? "CSV" : p(x, "format");
        FormRowSet pubs = dao.find(F_PUB, F_PUB, "WHERE e.customProperties.status = ?1",
                new Object[]{"PUBLISHED"}, "debtAmount", Boolean.FALSE, 0, FETCH_ALL);
        int count = 0;
        double total = 0;
        StringBuilder payload = new StringBuilder();
        if ("CSV".equalsIgnoreCase(format)) {
            payload.append("tin,debtorName,debtAmount\n");
        }
        if (pubs != null) {
            for (FormRow pub : pubs) {
                String tin = p(pub, "tin");
                String name = p(pub, "debtorName");
                double amt = num(p(pub, "debtAmount"));
                total += amt;
                count++;
                if ("JSON".equalsIgnoreCase(format)) {
                    payload.append(payload.length() == 0 ? "[" : ",")
                           .append("{\"tin\":\"").append(esc(tin)).append("\",\"name\":\"")
                           .append(esc(name)).append("\",\"amount\":").append(fmt(amt)).append('}');
                } else {
                    payload.append(tin).append(',').append(name).append(',').append(fmt(amt)).append('\n');
                }
            }
        }
        if ("JSON".equalsIgnoreCase(format)) {
            payload.append(payload.length() == 0 ? "[]" : "]");
        }

        x.setProperty("debtorCount", String.valueOf(count));
        x.setProperty("totalAmount", fmt(total));
        x.setProperty("payload", payload.toString());
        x.setProperty("status", "GENERATED");
        x.setProperty("generatedDate", now.toLocalDate().toString());
        x.setProperty("result", "GENERATED " + count + " debtors, total " + fmt(total)
                + " → " + (p(x, "registry").isEmpty() ? "registry" : p(x, "registry")) + " (" + format + ")");
        save(x);
        events.append("EXTRACT-" + extractId, "DEBTORS_EXTRACT_GENERATED", actor, "", "",
                "registry extract " + count + " debtors total " + fmt(total),
                "\"count\":\"" + count + "\",\"total\":\"" + fmt(total) + "\",\"format\":\""
                        + esc(format) + "\"");
        return "GENERATED (" + count + " debtors, total " + fmt(total) + ")";
    }

    private void save(FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(F_EXTRACT, F_EXTRACT, set);
    }

    private static String esc(String s) {
        return CaseEventWriter.esc(s);
    }

    private static String p(FormRow r, String id) {
        return DeadlineService.prop(r, id);
    }

    private static double num(String s) {
        try {
            return (s == null || s.isEmpty()) ? 0 : Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String fmt(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(Math.round(d * 100.0) / 100.0);
    }
}
