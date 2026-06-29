package com.fiscaladmin.mtca.cmbb.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Document register + ADG + postal tracking core (CMBB-F07, WF-FR-017..019).
 * Binaries go to Mayan via the {@link MayanClient} seam (P15); the Joget
 * register row (cmDoc) always survives a connector failure (status=FAILED,
 * retryable) — losing the register is the failure mode this design prevents.
 */
public class DocumentService {

    public static final String F_DOC = "cmDoc";
    public static final String F_GEN = "cmDocGen";
    public static final String F_POSTAL = "cmPostal";
    public static final String F_CASE = "cmCase";
    public static final String F_TASK = "cmTask";
    public static final String F_NOTIF = "cmNotif";

    private final FormDataDao dao;
    private final MayanClient mayan;
    private final CaseEventWriter events;
    private final MmConfigService mm;
    /** Joget upload root (wflow/app_formuploads) — overridable for tests. */
    private final File uploadRoot;

    public DocumentService(FormDataDao dao, MayanClient mayan, CaseEventWriter events,
                           MmConfigService mm, File uploadRoot) {
        this.dao = dao;
        this.mayan = mayan;
        this.events = events;
        this.mm = mm;
        this.uploadRoot = uploadRoot;
    }

    /** PUSH: ship an uploaded register row's binary to Mayan (WF-FR-018). */
    public String push(String docId, String actor) {
        FormRow d = dao.load(F_DOC, F_DOC, docId);
        if (d == null) {
            return "register row not found";
        }
        if (!DeadlineService.prop(d, "mayanDocId").isEmpty()) {
            return "already registered";
        }
        String fileName = DeadlineService.prop(d, "file");
        if (fileName.isEmpty()) {
            d.setProperty("status", "PENDING");
            save(F_DOC, d);
            return "no file attached (register-only row)";
        }
        try {
            File f = new File(new File(new File(uploadRoot, F_DOC), docId), fileName);
            byte[] content = Files.readAllBytes(f.toPath());
            String label = DeadlineService.prop(d, "docClass") + " "
                    + DeadlineService.prop(d, "caseRef") + " " + fileName;
            String mayanId = mayan.upload(label.trim(), fileName, content);
            d.setProperty("mayanDocId", mayanId);
            d.setProperty("status", "REGISTERED");
            d.setProperty("uploadedBy", actor);
            save(F_DOC, d);
            appendDocEvent(d, "DOC_REGISTERED", actor, fileName, mayanId);
            return "registered mayan:" + mayanId;
        } catch (Exception e) {
            d.setProperty("status", "FAILED");
            save(F_DOC, d);
            return "FAILED: " + e.getMessage();
        }
    }

    /** GENERATE: ADG — render template to PDF, attach to case(s), push (WF-FR-017). */
    public String generate(String genId, String actor, LocalDateTime now) {
        FormRow g = dao.load(F_GEN, F_GEN, genId);
        if (g == null) {
            return "order not found";
        }
        String templateCode = DeadlineService.prop(g, "templateCode");
        DispatchService renderer = new DispatchService(dao);
        FormRow tpl = renderer.template(templateCode, "en", now);
        if (tpl == null) {
            g.setProperty("result", "REJECTED: template not found " + templateCode);
            save(F_GEN, g);
            return DeadlineService.prop(g, "result");
        }
        List<FormRow> scope = resolveScope(g);
        int ok = 0, failed = 0;
        for (FormRow c : scope) {
            String fileName = templateCode + "-"
                    + DeadlineService.prop(c, "caseRef").replace("/", "-") + ".pdf";
            try {
                byte[] pdf = renderPdf(
                        DispatchService.render(DeadlineService.prop(tpl, "subject"), c, now),
                        DispatchService.render(DeadlineService.prop(tpl, "body"), c, now));
                String mayanId = mayan.upload("ADG " + fileName, fileName, pdf);
                FormRow d = new FormRow();
                d.setId(UUID.randomUUID().toString());
                d.setProperty("caseId", c.getId());
                d.setProperty("caseRef", DeadlineService.prop(c, "caseRef"));
                d.setProperty("tin", DeadlineService.prop(c, "tin"));
                d.setProperty("docClass", DeadlineService.prop(g, "docClass"));
                d.setProperty("description", "ADG: " + templateCode);
                d.setProperty("version", "1");
                d.setProperty("source", "GENERATED");
                d.setProperty("status", "REGISTERED");
                d.setProperty("mayanDocId", mayanId);
                d.setProperty("uploadedBy", actor);
                d.setProperty("file", fileName);
                save(F_DOC, d);
                appendDocEvent(d, "DOC_GENERATED", actor, fileName, mayanId);
                ok++;
            } catch (Exception e) {
                failed++;
            }
        }
        String result = ok + " document(s) generated"
                + (failed > 0 ? ", " + failed + " failed" : "");
        g.setProperty("result", result);
        save(F_GEN, g);
        return result;
    }

    /** POSTAL: dispatch logging + returned-mail actions (WF-FR-019). */
    public String postal(String postalId, String actor) {
        FormRow p = dao.load(F_POSTAL, F_POSTAL, postalId);
        if (p == null) {
            return "row not found";
        }
        String status = DeadlineService.prop(p, "status");
        String processed = DeadlineService.prop(p, "processed");
        FormRow c = caseByRef(DeadlineService.prop(p, "caseRef"));
        if ("DISPATCHED".equals(status) && !processed.contains("DISPATCHED")) {
            String notifId = DeadlineService.prop(p, "notifId");
            FormRow n = notifId.isEmpty() ? null : dao.load(F_NOTIF, F_NOTIF, notifId);
            if (n != null) {
                n.setProperty("status", "DISPATCHED");
                save(F_NOTIF, n);
            }
            if (c != null) {
                events.append(c.getId(), "POSTAL_DISPATCHED", actor, "", "",
                        "postal dispatch " + DeadlineService.prop(p, "method"), null);
            }
            p.setProperty("processed", processed + "DISPATCHED;");
            save(F_POSTAL, p);
            return "dispatch recorded";
        }
        if ("RETURNED".equals(status) && !processed.contains("RETURNED")) {
            if (c != null) {
                c.setProperty("addressFlag", "RETURNED_MAIL");
                save(F_CASE, c);
                FormRow t = new FormRow();
                t.setId(UUID.randomUUID().toString());
                t.setProperty("caseId", c.getId());
                t.setProperty("taskType", "ADDRESS_VERIFICATION");
                t.setProperty("title", "Verify taxpayer address (returned mail) — "
                        + DeadlineService.prop(c, "caseRef"));
                t.setProperty("assignee", DeadlineService.prop(c, "assignee"));
                t.setProperty("status", "OPEN");
                save(F_TASK, t);
                events.append(c.getId(), "POSTAL_RETURNED", actor, "", "",
                        "returned mail — address verification task created", null);
                events.append(c.getId(), "NOTIF_PENDING", actor, "", "",
                        "returned mail: address verification required",
                        "\"recipient\":\"" + CaseEventWriter.esc(
                                DeadlineService.prop(c, "assignee")) + "\"");
            }
            p.setProperty("processed", processed + "RETURNED;");
            save(F_POSTAL, p);
            return "returned-mail actions created";
        }
        return "no action (status=" + status + ")";
    }

    /** Simple ADG letter: letterhead + subject + body (FIS A2). */
    static byte[] renderPdf(String subject, String body) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document();
        PdfWriter.getInstance(doc, out);
        doc.open();
        doc.add(new Paragraph("MALTA TAX AND CUSTOMS ADMINISTRATION",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(subject, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(body, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        doc.close();
        return out.toByteArray();
    }

    private List<FormRow> resolveScope(FormRow g) {
        List<FormRow> scope = new ArrayList<FormRow>();
        String caseRef = DeadlineService.prop(g, "caseRef");
        if (!caseRef.isEmpty()) {
            FormRow c = caseByRef(caseRef);
            if (c != null) {
                scope.add(c);
            }
            return scope;
        }
        String type = DeadlineService.prop(g, "filterCaseType");
        if (type.isEmpty()) {
            return scope;
        }
        Set<String> terminals = mm.allTerminalCodes();
        FormRowSet rows = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.caseType = ?1", new Object[]{type},
                "dateCreated", Boolean.FALSE, null, null);
        if (rows != null) {
            for (FormRow c : rows) {
                if (!terminals.contains(DeadlineService.prop(c, "currentState"))) {
                    scope.add(c);
                }
            }
        }
        return scope;
    }

    private FormRow caseByRef(String caseRef) {
        if (caseRef.isEmpty()) {
            return null;
        }
        FormRowSet rows = dao.find(F_CASE, F_CASE,
                "WHERE e.customProperties.caseRef = ?1", new Object[]{caseRef},
                "dateCreated", Boolean.FALSE, 0, 1);
        return rows == null || rows.isEmpty() ? null : rows.get(0);
    }

    private void appendDocEvent(FormRow d, String type, String actor,
                                String fileName, String mayanId) {
        String caseId = DeadlineService.prop(d, "caseId");
        if (!caseId.isEmpty()) {
            events.append(caseId, type, actor, "", "",
                    type + ": " + fileName, "\"mayanDocId\":\""
                            + CaseEventWriter.esc(mayanId) + "\",\"docClass\":\""
                            + CaseEventWriter.esc(DeadlineService.prop(d, "docClass")) + "\"");
        }
    }

    private void save(String form, FormRow row) {
        FormRowSet set = new FormRowSet();
        set.add(row);
        dao.saveOrUpdate(form, form, set);
    }
}
