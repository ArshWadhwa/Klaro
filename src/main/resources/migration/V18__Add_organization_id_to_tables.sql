-- ===============================
-- V18: Add organization_id to existing tables
-- Purpose: Tenant isolation
-- Created: 2026-01-31
-- ===============================

-- Add organization_id to groups table
ALTER TABLE groups ADD COLUMN organization_id BIGINT;

-- Add organization_id to projects table
ALTER TABLE projects ADD COLUMN organization_id BIGINT;

-- Add organization_id to issue table
ALTER TABLE issue ADD COLUMN organization_id BIGINT;

-- Add organization_id to comment table
ALTER TABLE comment ADD COLUMN organization_id BIGINT;

-- Add organization_id to documents table
ALTER TABLE documents ADD COLUMN organization_id BIGINT;

-- Add organization_id to notification table
ALTER TABLE notification ADD COLUMN organization_id BIGINT;

-- Update existing data to use default organization
UPDATE groups SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;
UPDATE projects SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;
UPDATE issue SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;
UPDATE comment SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;
UPDATE documents SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;
UPDATE notification SET organization_id = (SELECT id FROM organizations WHERE slug = 'default-org') WHERE organization_id IS NULL;

-- Add NOT NULL constraint after data migration
ALTER TABLE groups ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE projects ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE issue ALTER COLUMN organization_id SET NOT NULL;

-- Add foreign keys
ALTER TABLE groups ADD CONSTRAINT fk_groups_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    
ALTER TABLE projects ADD CONSTRAINT fk_projects_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    
ALTER TABLE issue ADD CONSTRAINT fk_issue_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    
ALTER TABLE comment ADD CONSTRAINT fk_comment_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    
ALTER TABLE documents ADD CONSTRAINT fk_documents_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
    
ALTER TABLE notification ADD CONSTRAINT fk_notification_organization 
    FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- Add indexes for performance
CREATE INDEX idx_groups_organization_id ON groups(organization_id);
CREATE INDEX idx_projects_organization_id ON projects(organization_id);
CREATE INDEX idx_issue_organization_id ON issue(organization_id);
CREATE INDEX idx_comment_organization_id ON comment(organization_id);
CREATE INDEX idx_documents_organization_id ON documents(organization_id);
CREATE INDEX idx_notification_organization_id ON notification(organization_id);
