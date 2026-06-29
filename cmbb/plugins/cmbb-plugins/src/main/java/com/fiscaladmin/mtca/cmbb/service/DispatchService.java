package com.fiscaladmin.mtca.cmbb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * NotificationDispatcher core (CMBB-F06, WF-FR-013..016).
 * Consumes NOTIF_PENDING events idempotently (an event is handled when a
 * cmAlert/cmNotif row references its eventId — events are never mutated):
 *  - payload with `recipient`  -> internal alert (cmAlert, officer prefs honoured)
 *  - mmNotifRule(eventType)    -> taxpayer notification: template render
 *    (versioned, effective-dated, language) -> channel chain with fallback,
 *    every attempt logged in cmNotif (WF-FR-016).
 * Transport adapters are DEV-simulated behind one seam (FIS A3):
 * SIMFAIL->FAILED, LETTER->QUEUED_PRINT, else SENT.
 */
public class DispatchService {

    public static final String F_EVENT = "cmEvent";
    public static final String F_CASE = "cmCase";
    public static final String F_NOTIF = "cmNotif";
    public static final String F_ALERT = "cmAlert";
    public static final String F_RULE = "mmNotifRule";
    public static final String F_TEMPLATE = "mdTemplate";
    public static final String F_OFFICER = "mdOfficerProfile";
    public static final String F_HOLD = "cmHold";
    static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final FormDataDao dao;

    public DispatchService(FormDataDao dao) {
        this.dao = dao;
    }

    public String dispatch(String actor, LocalDateTime now) {
        // reads do NOT auto-create tables and a missing relation poisons the
        // whole JTA tx on Postgres — ensure owned tables exist first
        dao.updateSchema(F_NOTIF, F_NOTIF, new FormRowSet());
        dao.updateSchema(F_ALERT, F_ALERT, new FormRowSet());
        // F08: cmHold now exists — ensure the relation before the suppression
        // read (a missing relation poisons the JTA tx; DX9-DELTAS).
        dao.updateSchema(F_HOLD, F_HOLD, new FormRowSet());
        FormRowSet pending = dao.find(F_EVENT, F_EVENT,
                "WHERE e.customProperties.eventType = ?1", new Object[]{"NOTIF_PENDING"},
                "dateCreated", Boolean.FALSE, null, null);
        int alerts = 0, notifs = 0, skipped = 0, suppressed = 0;
        if (pending != null) {
            for (FormRow ev : pending) {
                if (handled(ev.getId())) {
                    skipped++;
                    continue;
                }
                String payload = DeadlineService.prop(ev, "payload");
                String caseId = DeadlineService.prop(ev, "caseId");
                FormRow c = dao.load(F_CASE, F_CASE, caseId);
                String recipient = DeadlineService.jsonStr(payload, "recipient");
                String alertType = DeadlineService.jsonStr(payload, "alertType");
                if (alertType.isEmpty()) {
                    alertType = DeadlineService.jsonStr(payload, "reason");
                }
                if (recipient.isEmpty() && !DeadlineService.jsonStr(payload, "alertType").isEmpty()) {
                    recipient = "dm_supervisor"; // supervisor alerts without explicit recipient
                }
                if (!recipient.isEmpty()) {
                    alerts += internalAlert(ev, c, recipient, alertType, payload, now);
                }
                if (c != null) {
                    notifs += ruleDispatch(ev, c, now);
                    suppressed += 0;
                }
                if (recipient.isEmpty() && c == null) {
                    // orphan event: log a FAILED notif row so it is not rescanned forever
                    log(caseId, ev.getId(), "", "", "", "", "orphan NOTIF_PENDING", "FAILED", now);
                }
            }
        }
        return "alerts=" + alerts + " notifs=" + notifs + " skippedHandled=" + skipped;
    }

    private boolean handled(String eventId) {
        Long a = dao.count(F_ALERT, F_ALERT,
                "WHERE e.customProperties.eventId = ?1", new Object[]{eventId});
        Long n = dao.count(F_NOTIF, F_NOTIF,
                "WHERE e.customProperties.eventId = ?1", new Object[]{eventId});
        return (a != null && a > 0) || (n != null && n > 0);
    }

    private int internalAlert(FormRow ev, FormRow c, String recipient,
                              String alertType, String payload, LocalDateTime now) {
        boolean suppressedByPref = false;
        FormRow officer = dao.load(F_OFFICER, F_OFFICER, recipient);
        if (officer != null) {
            String prefs = DeadlineService.prop(officer, "alertPrefs");
            for (String p : prefs.split("[,;]")) {
                if (!p.trim().isEmpty() && alertType.contains(p.trim())) {
                    suppressedByPref = true;
                    break;
                }
            }
        }
        FormRow a = new FormRow();
        a.setId(UUID.randomUUID().toString());
        a.setProperty("caseId", ev.getProperty("caseId"));
        a.setProperty("eventId", ev.getId());
        a.setProperty("recipient", recipient);
        a.setProperty("alertType", alertType);
        a.setProperty("priority", c == null ? "" : DeadlineService.prop(c, "priority"));
        a.setProperty("message", suppressedByPref
                ? "(suppressed by officer preference)" : summary(payload, c));
        a.setProperty("isRead", suppressedByPref ? "true" : "");
        save(F_ALERT, a);
        return suppressedByPref ? 0 : 1;
    }

    private int ruleDispatch(FormRow ev, FormRow c, LocalDateTime now) {
        // rules match the ORIGINATING event type carried in the payload reason
        // when present; NOTIF_PENDING itself is matchable for generic rules
        FormRowSet rules = dao.find(F_RULE, F_RULE,
                "WHERE e.customProperties.caseType = ?1", new Object[]{
                        DeadlineService.prop(c, "caseType")},
                "dateCreated", Boolean.FALSE, null, null);
        int sent = 0;
        if (rules == null) {
            return 0;
        }
        for (FormRow rule : rules) {
            String trigger = DeadlineService.prop(rule, "eventType");
            if (!trigger.equals("NOTIF_PENDING")
                    && !DeadlineService.prop(ev, "payload").contains(trigger)) {
                continue;
            }
            if (correspondenceSuppressed(c.getId())) {
                log(c.getId(), ev.getId(), DeadlineService.prop(c, "tin"),
                        DeadlineService.prop(c, "tin"), "",
                        DeadlineService.prop(rule, "template"),
                        "suppressed by active hold", "SUPPRESSED", now);
                continue;
            }
            FormRow tpl = template(DeadlineService.prop(rule, "template"), "en", now);
            String subject = tpl == null ? "(no template)"
                    : render(DeadlineService.prop(tpl, "subject"), c, now);
            String[] channels = DeadlineService.prop(rule, "channelDefault").split("[,;]");
            for (String ch : channels) {
                String channel = ch.trim().isEmpty() ? "EMAIL" : ch.trim();
                String status = transport(channel);
                log(c.getId(), ev.getId(), DeadlineService.prop(c, "tin"),
                        DeadlineService.prop(c, "tin"), channel,
                        tpl == null ? "" : DeadlineService.prop(tpl, "code"),
                        subject, status, now);
                if (!"FAILED".equals(status)) { // fallback only on failure (WF-FR-013)
                    sent++;
                    break;
                }
            }
        }
        return sent;
    }

    /** Template lookup: code+language, active, effectiveFrom<=now, highest version. */
    FormRow template(String code, String language, LocalDateTime now) {
        FormRowSet rows = dao.find(F_TEMPLATE, F_TEMPLATE,
                "WHERE e.customProperties.code = ?1 AND e.customProperties.language = ?2"
                        + " AND e.customProperties.active = ?3",
                new Object[]{code, language, "true"}, "version", Boolean.TRUE, null, null);
        if (rows == null) {
            return null;
        }
        FormRow best = null;
        long bestV = -1;
        for (FormRow t : rows) {
            String eff = DeadlineService.prop(t, "effectiveFrom");
            if (!eff.isEmpty() && eff.compareTo(now.toLocalDate().toString()) > 0) {
                continue; // not yet effective
            }
            long v = DeadlineService.parseLong(DeadlineService.prop(t, "version"), 0);
            if (v > bestV) {
                bestV = v;
                best = t;
            }
        }
        return best;
    }

    /** Merge-field render: #tin# #taxpayerName# #caseRef# #amountAtStake# #date#. */
    static String render(String text, FormRow c, LocalDateTime now) {
        return DeadlineService.nz(text)
                .replace("#tin#", DeadlineService.prop(c, "tin"))
                .replace("#taxpayerName#", DeadlineService.prop(c, "taxpayerName"))
                .replace("#caseRef#", DeadlineService.prop(c, "caseRef"))
                .replace("#amountAtStake#", DeadlineService.prop(c, "amountAtStake"))
                .replace("#date#", now.toLocalDate().toString());
    }

    /** DEV transport seam (FIS A3): real gateways are deployment config. */
    static String transport(String channel) {
        if ("SIMFAIL".equalsIgnoreCase(channel)) {
            return "FAILED";
        }
        if ("LETTER".equalsIgnoreCase(channel)) {
            return "QUEUED_PRINT"; // print-ready PDF = F07 ADG
        }
        return "SENT";
    }

    /** E1 suppression — ENABLED AT F08: cmHold exists (HoldConnector) and
     *  dispatch() now updateSchema(F_HOLD)s before this read. An active
     *  CORRESPONDENCE_SUPPRESS hold blocks taxpayer correspondence. */
    static final boolean HOLD_CHECK_ENABLED = true;

    private boolean correspondenceSuppressed(String caseId) {
        if (!HOLD_CHECK_ENABLED) {
            return false;
        }
        Long n = dao.count(F_HOLD, F_HOLD,
                "WHERE e.customProperties.caseId = ?1 AND e.customProperties.scope = ?2"
                        + " AND e.customProperties.status = ?3",
                new Object[]{caseId, "CORRESPONDENCE_SUPPRESS", "ACTIVE"});
        return n != null && n > 0;
    }

    private String summary(String payload, FormRow c) {
        String reason = DeadlineService.jsonStr(payload, "reason");
        if (!reason.isEmpty()) {
            return reason;
        }
        return c == null ? "notification" : "case " + DeadlineService.prop(c, "caseRef");
    }

    private void log(String caseId, String eventId, String tin, String recipient,
                     String channel, String template, String summary, String status,
                     LocalDateTime now) {
        FormRow n = new FormRow();
        n.setId(UUID.randomUUID().toString());
        n.setProperty("caseId", caseId);
        n.setProperty("eventId", eventId);
        n.setProperty("tin", tin);
        n.setProperty("recipient", recipient);
        n.setProperty("channel", channel);
        n.setProperty("template", template);
        n.setProperty("summary", summary.length() > 200 ? summary.substring(0, 200) : summary);
        n.setProperty("status", status);
        n.setProperty("sentAt", ISO.format(now));
        save(F_NOTIF, n);
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
