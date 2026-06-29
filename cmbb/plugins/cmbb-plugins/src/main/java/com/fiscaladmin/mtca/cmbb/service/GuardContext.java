package com.fiscaladmin.mtca.cmbb.service;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * Everything a guard phase needs, constructor-injected for testability
 * (phases never reach for AppUtil themselves).
 */
public class GuardContext {

    public static final String F_CASE = "cmCase";
    public static final String F_TASK = "cmTask";
    public static final String F_DOC = "cmDoc";

    private final FormDataDao dao;
    private final MmConfigService mm;
    private final CaseEventWriter events;
    private final CaseRefGenerator refs;
    private final String caseId;
    private final String actor;
    private final boolean requireDecision;
    private FormRow caseRow;

    public GuardContext(FormDataDao dao, MmConfigService mm, CaseEventWriter events,
                        CaseRefGenerator refs, String caseId, String actor,
                        boolean requireDecision) {
        this.dao = dao;
        this.mm = mm;
        this.events = events;
        this.refs = refs;
        this.caseId = caseId;
        this.actor = actor;
        this.requireDecision = requireDecision;
    }

    public FormDataDao dao() { return dao; }
    public MmConfigService mm() { return mm; }
    public CaseEventWriter events() { return events; }
    public CaseRefGenerator refs() { return refs; }
    public String caseId() { return caseId; }
    public String actor() { return actor; }
    public boolean requireDecision() { return requireDecision; }

    /** The cmCase row under guard; loaded once, cached. */
    public FormRow caseRow() {
        if (caseRow == null) {
            caseRow = dao.load(F_CASE, F_CASE, caseId);
            if (caseRow == null) {
                throw new IllegalStateException("cmCase row not found: " + caseId);
            }
        }
        return caseRow;
    }

    public String prop(String fieldId) {
        String v = caseRow().getProperty(fieldId);
        return v == null ? "" : v;
    }

    public void saveCase() {
        FormRowSet set = new FormRowSet();
        set.add(caseRow());
        dao.saveOrUpdate(F_CASE, F_CASE, set);
    }
}
