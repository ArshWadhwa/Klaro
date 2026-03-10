-- Remove organization requirements - make organization_id nullable everywhere
-- This allows the app to work without requiring organization context

-- Step 1: Drop per-org unique constraint on groups
ALTER TABLE groups DROP CONSTRAINT IF EXISTS uk_group_name_per_org;

-- Step 2: Make organization_id nullable
ALTER TABLE groups ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE issue ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE projects ALTER COLUMN organization_id DROP NOT NULL;

-- Step 3: Add simple unique constraint on group name (only if no duplicates exist)
-- First check and deduplicate if needed
DO $$
BEGIN
    -- Only add the constraint if it doesn't already exist
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'groups_name_key') THEN
        ALTER TABLE groups ADD CONSTRAINT groups_name_key UNIQUE (name);
    END IF;
EXCEPTION WHEN unique_violation THEN
    RAISE NOTICE 'Could not add unique constraint on groups.name - duplicate names exist';
END $$;
