package com.fiscaladmin.mtca.cmbb.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Case linkage service (CMBB-F08, GCMF §3.3-10 / enrichment E4).
 * Validates a typed link against mmLinkType.targetCaseTypes, resolves the
 * target by caseRef and writes the reciprocal row so the case graph is
 * navigable both ways. The reciprocal carries result=RECIPROCAL so the engine's
 * unprocessed-row resolver never reprocesses it. Constructor-injected.
 */
public class LinkService {

    public static final String F_LINK = "cmLink";
    public static final String F_LINKTYPE = "mmLinkType";
    public static final String F_CASE = "cmCase";

    private final FormDataDao dao;
    private final CaseEventWriter events;

    public LinkService(FormDataDao dao, CaseEventWriter events) {
        this.dao = dao;
        this.events = events;
    }

    public String link(String linkId, String actor) {
        dao.updateSchema(F_LINK, F_LINK, new FormRowSet());
        FormRow link = dao.load(F_LINK, F_LINK, linkId);
        if (link == null) {
            return "link not found: " + linkId;
        }
        if (!DeadlineService.prop(link, "result").isEmpty()) {
            return "no-op: already processed";
        }
        String linkType = DeadlineService.prop(link, "linkType");
        String fromCaseRef = DeadlineService.prop(link, "fromCaseRef");
        String toCaseRef = DeadlineService.prop(link, "toCaseRef");
        String fromCaseId = DeadlineService.prop(link, "fromCaseId");
        if (fromCaseId.isEmpty()) {
            fromCaseId = fromCaseRef;
        }

        List<String> targets = permittedTargets(linkType);
        FormRow toCase = caseByRef(toCaseRef);
        String toCaseId = toCase == null ? "" : toCase.getId();
        String toType = toCase == null ? "" : DeadlineService.prop(toCase, "caseType");

        // target-type validation: blank list = any; only enforceable when the
        // target case is resolvable (cross-BB links may precede the target).
        if (toCase != null && !targets.isEmpty() && !targets.contains(toType)) {
            String result = "REJECTED: " + linkType + " not permitted to type " + toType;
            link.setProperty("result", result);
            save(F_LINK, link);
            return result;
        }

        link.setProperty("toCaseId", toCaseId);
        boolean reciprocal = toCase != null;
        link.setProperty("reciprocal", String.valueOf(reciprocal));
        link.setProperty("result", "OK");
        save(F_LINK, link);
        events.append(fromCaseId, "CASE_LINKED", actor, "", "",
                "linked " + linkType + " -> " + toCaseRef,
                "\"linkType\":\"" + CaseEventWriter.esc(linkType) + "\""
                        + ",\"toCaseRef\":\"" + CaseEventWriter.esc(toCaseRef) + "\"");

        if (reciprocal) {
            FormRow rec = new FormRow();
            rec.setId(UUID.randomUUID().toString());
            rec.setProperty("fromCaseId", toCaseId);
            rec.setProperty("fromCaseRef", toCaseRef);
            rec.setProperty("linkType", linkType);
            rec.setProperty("toCaseRef", fromCaseRef);
            rec.setProperty("toCaseId", fromCaseId);
            rec.setProperty("reciprocal", "true");
            rec.setProperty("result", "RECIPROCAL");
            rec.setProperty("note", "reciprocal of " + linkId);
            save(F_LINK, rec);
            events.append(toCaseId, "CASE_LINKED", actor, "", "",
                    "linked " + linkType + " -> " + fromCaseRef,
                    "\"linkType\":\"" + CaseEventWriter.esc(linkType) + "\""
                            + ",\"toCaseRef\":\"" + CaseEventWriter.esc(fromCaseRef) + "\"");
        }
        return reciprocal ? "OK (reciprocal written)" : "OK (target not yet present)";
    }

    private List<String> permittedTargets(String linkType) {
        FormRowSet rows = dao.find(F_LINKTYPE, F_LINKTYPE,
                "WHERE e.customProperties.code = ?1", new Object[]{linkType},
                "dateCreated", Boolean.FALSE, 0, 1);
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String csv = DeadlineService.prop(rows.get(0), "targetCaseTypes");
        if (csv.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        for (String t : Arrays.asList(csv.split("[,;]"))) {
            if (!t.trim().isEmpty()) {
                out.add(t.trim());
            }
        }
        return out;
    }

    private FormRow caseByRef(String caseRef) {
        if (caseRef == null || caseRef.isEmpty()) {
            return null;
        }
        FormRowSet rows = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.caseRef = ?1", new Object[]{caseRef},
                "dateCreated", Boolean.FALSE, 0, 1);
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
