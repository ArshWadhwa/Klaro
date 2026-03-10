-- Re-enable organization-scoped system
-- Step 1: Backfill any NULL organization_id rows with the first org in the system
-- (for any data created while tenant system was disabled)

UPDATE groups SET organization_id = (SELECT id FROM organizations LIMIT 1) WHERE organization_id IS NULL;
UPDATE issue SET organization_id = (SELECT id FROM organizations LIMIT 1) WHERE organization_id IS NULL;
UPDATE projects SET organization_id = (SELECT id FROM organizations LIMIT 1) WHERE organization_id IS NULL;

-- Step 2: Re-add NOT NULL constraints
ALTER TABLE groups ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE issue ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE projects ALTER COLUMN organization_id SET NOT NULL;

-- Step 3: Remove global unique constraint on group name (if exists)
-- Group names should be unique per organization, not globally
ALTER TABLE groups DROP CONSTRAINT IF EXISTS groups_name_key;

-- Step 4: Add unique constraint per organization for group names
ALTER TABLE groups ADD CONSTRAINT uk_group_name_per_org UNIQUE (name, organization_id);
