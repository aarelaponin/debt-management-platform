package com.fiscaladmin.mtca.cmbb.service;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;

/**
 * TenantContext — ADR-004 multi-tenancy. The tenant is a PERVASIVE but INVISIBLE
 * scope: it is never a screen the user navigates and never a field they pick. It
 * is resolved from the user's profile (the admin-maintained {@code mdUserTenant}
 * mapping username → tenantCode) and stamped onto cases and config behind the
 * scenes. When a user has no mapping the platform default tenant applies, so a
 * single-tenant deployment needs no per-user configuration at all.
 *
 * <p>This is a pure read helper: it does not write. Callers (e.g. the case-open
 * guard) stamp the resolved tenant onto their own row.</p>
 */
public class TenantContext {

    public static final String F_USER_TENANT = "mdUserTenant";
    /** Platform default when a user carries no explicit tenant mapping. */
    public static final String DEFAULT_TENANT = "MLT";

    private final FormDataDao dao;

    public TenantContext(FormDataDao dao) {
        this.dao = dao;
    }

    /**
     * The tenant that governs the given user, resolved from the profile mapping.
     * Falls back to {@link #DEFAULT_TENANT} when the user is unmapped, blank, or
     * the mapping table is absent — so resolution never returns blank.
     */
    public String resolve(String username) {
        if (username == null || username.trim().isEmpty()) {
            return DEFAULT_TENANT;
        }
        try {
            dao.updateSchema(F_USER_TENANT, F_USER_TENANT, new FormRowSet());
            FormRowSet rows = dao.find(F_USER_TENANT, F_USER_TENANT,
                    "WHERE e.customProperties.username = ?1 AND e.customProperties.active = ?2",
                    new Object[]{username.trim(), "true"},
                    "dateCreated", Boolean.FALSE, 0, 1);
            if (rows != null && !rows.isEmpty()) {
                String tenant = rows.get(0).getProperty("tenantCode");
                if (tenant != null && !tenant.trim().isEmpty()) {
                    return tenant.trim();
                }
            }
        } catch (Exception ignored) {
            // mapping table not yet present, or a transient read issue → safe default
        }
        return DEFAULT_TENANT;
    }
}
