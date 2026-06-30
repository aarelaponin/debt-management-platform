package com.fiscaladmin.mtca.cmbb;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.junit.Test;

import com.fiscaladmin.mtca.cmbb.service.AuthorityResolver;

/**
 * AuthorityResolver (DAS P3) — resolve an approver's rank level from their directory role-groups +
 * the role-group→level map. The directory lookup is injected (no live directory needed in unit).
 */
public class AuthorityResolverTest {

    private AuthorityResolver.GroupSource groupsOf(final String... gids) {
        return new AuthorityResolver.GroupSource() {
            @Override
            public List<String> groupsOf(String username) {
                return Arrays.asList(gids);
            }
        };
    }

    /** A FormDataDao whose mmRoleLevel query is absent (table not provisioned) → default map. */
    private FormDataDao daoNoMap() {
        FormDataDao dao = mock(FormDataDao.class);
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new RuntimeException("no such table"));
        return dao;
    }

    private static FormRow row(String group, String level) {
        FormRow r = new FormRow();
        r.setProperty("roleGroup", group);
        r.setProperty("level", level);
        return r;
    }

    @Test
    public void resolvesHighestOfMultipleGroups() {
        // default map: dm_officer→OFFICER, dm_manager→MANAGER → highest is MANAGER
        AuthorityResolver r = new AuthorityResolver(daoNoMap(), groupsOf("cmbb_user", "dm_officer", "dm_manager"));
        assertEquals("MANAGER", r.resolveLevel("someone"));
    }

    @Test
    public void defaultMapResolvesEachRole() {
        assertEquals("OFFICER", new AuthorityResolver(daoNoMap(), groupsOf("dm_officer")).resolveLevel("u"));
        assertEquals("SUPERVISOR", new AuthorityResolver(daoNoMap(), groupsOf("dm_supervisor")).resolveLevel("u"));
        assertEquals("DIRECTOR", new AuthorityResolver(daoNoMap(), groupsOf("dm_policy_admin")).resolveLevel("u"));
    }

    @Test
    public void unmappedGroupsResolveToNothing() {
        // a user only in non-authority groups has no resolvable level (gate then blocks)
        assertEquals("", new AuthorityResolver(daoNoMap(), groupsOf("cmbb_user")).resolveLevel("u"));
        assertEquals("", new AuthorityResolver(daoNoMap(), groupsOf()).resolveLevel("u"));
    }

    @Test
    public void blankUsernameResolvesToNothing() {
        assertEquals("", new AuthorityResolver(daoNoMap(), groupsOf("dm_policy_admin")).resolveLevel(""));
        assertEquals("", new AuthorityResolver(daoNoMap(), groupsOf("dm_policy_admin")).resolveLevel(null));
    }

    @Test
    public void mmRoleLevelConfigOverridesAndExtendsDefaults() {
        // config carrier maps a bespoke group to DIRECTOR and re-grades dm_officer up to SENIOR
        FormDataDao dao = mock(FormDataDao.class);
        FormRowSet rows = new FormRowSet();
        rows.add(row("dm_audit", "DIRECTOR"));
        rows.add(row("dm_officer", "SENIOR"));
        when(dao.find(anyString(), anyString(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(rows);
        AuthorityResolver r = new AuthorityResolver(dao, groupsOf("dm_officer", "dm_audit"));
        assertEquals("DIRECTOR", r.resolveLevel("u"));               // bespoke mapped group wins
        AuthorityResolver r2 = new AuthorityResolver(dao, groupsOf("dm_officer"));
        assertEquals("SENIOR", r2.resolveLevel("u"));                // default OFFICER overridden to SENIOR
    }

    @Test
    public void nullGroupSourceIsSafe() {
        AuthorityResolver r = new AuthorityResolver(daoNoMap(), new AuthorityResolver.GroupSource() {
            @Override
            public List<String> groupsOf(String username) {
                return Collections.emptyList();
            }
        });
        assertEquals("", r.resolveLevel("u"));
    }
}
