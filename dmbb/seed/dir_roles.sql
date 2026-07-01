-- DMBB userview role model (DEV directory seed) — gates userview categories via GroupPermission
-- and drives per-role landing pages (a user lands on the first category they may see).
-- Mirrors the existing dm_supervisor / cmbb_user DEV seeds. Idempotent.
--
-- Roles (RPT-FR-007 three tiers):
--   dm_officer       — case workers (single-stream): officer1 (+ officer2 also works cases)
--   dm_policy_admin  — department management + configuration
--   dm_manager       — senior management (top tier, KPI/multi-stream dashboards)
-- admin is a member of ALL roles so the demo/superuser (and the render-based acceptance
-- tests that hit pages as admin) continue to see every category.
--
-- 3-user deployment model (unlicensed Enterprise = 3 users max): admin=superuser (all);
-- officer2=MANAGER (dm_officer + dm_manager + dm_supervisor → Operations + Dashboards + Approvals
-- + Approvals MI, but NOT the admin-only config/legal tiers); officer1=officer (Operations only).
-- To make officer2 loginable for a live manager demo (DEV only):
--   UPDATE dir_user SET password=md5('officer2') WHERE username='officer2';   (login officer2/officer2)

-- organizationid is nullable and the existing DEV groups (cmbb_user, dm_supervisor) use NULL;
-- an empty string violates the FK to dir_organization, so leave it NULL.
--   dm_collection_admin — operational config tier (ADR-004 §7 Collection settings)
--   dm_legal_admin       — legislative config tier (ADR-004 §7 Legal & reference)
INSERT INTO dir_group (id, name, description, organizationid) VALUES
  ('dm_officer',          'dm_officer',          'DM case workers (DEV seed)',                 NULL),
  ('dm_policy_admin',     'dm_policy_admin',     'DM department mgmt + config (DEV seed)',      NULL),
  ('dm_manager',          'dm_manager',          'DM senior management (DEV seed)',            NULL),
  ('dm_collection_admin', 'dm_collection_admin', 'DM operational config — Collection settings', NULL),
  ('dm_legal_admin',      'dm_legal_admin',      'DM legislative config — Legal & reference',   NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO dir_user_group (groupid, userid) VALUES
  ('dm_officer',          'admin'),
  ('dm_policy_admin',     'admin'),
  ('dm_manager',          'admin'),
  ('dm_collection_admin', 'admin'),
  ('dm_legal_admin',      'admin'),
  ('dm_officer',          'officer1'),
  ('dm_officer',          'officer2'),
  ('dm_manager',          'officer2'),
  ('dm_supervisor',       'officer2')
ON CONFLICT DO NOTHING;
