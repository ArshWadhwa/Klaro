-- Make organization_id nullable in groups, issue, and projects tables
-- (Tenant system disabled - organization context is no longer required)

ALTER TABLE groups ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE issue ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE projects ALTER COLUMN organization_id DROP NOT NULL;
