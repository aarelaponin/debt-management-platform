-- DMBB userview role model (DEV directory seed) — gates userview categories via GroupPermission
-- and drives per-role landing pages (a user lands on the first category they may see).
-- Mirrors the existing dm_supervisor / cmbb_user DEV seeds. Idempotent.
--
-- Roles (RPT-FR-007 three tiers):
--   dm_officer       — case workers (single-stream): officer1, officer2
--   dm_policy_admin  — department management + configuration
--   dm_manager       — senior management (top tier, KPI/multi-stream dashboards)
-- admin is a member of ALL roles so the demo/superuser (and the render-based acceptance
-- tests that hit pages as admin) continue to see every category.

-- organizationid is nullable and the existing DEV groups (cmbb_user, dm_supervisor) use NULL;
-- an empty string violates the FK to dir_organization, so leave it NULL.
INSERT INTO dir_group (id, name, description, organizationid) VALUES
  ('dm_officer',      'dm_officer',      'DM case workers (DEV seed)',            NULL),
  ('dm_policy_admin', 'dm_policy_admin', 'DM department mgmt + config (DEV seed)', NULL),
  ('dm_manager',      'dm_manager',      'DM senior management (DEV seed)',        NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO dir_user_group (groupid, userid) VALUES
  ('dm_officer',      'admin'),
  ('dm_policy_admin', 'admin'),
  ('dm_manager',      'admin'),
  ('dm_officer',      'officer1'),
  ('dm_officer',      'officer2')
ON CONFLICT DO NOTHING;
