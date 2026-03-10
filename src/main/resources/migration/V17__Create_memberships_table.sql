-- ===============================
-- V17: Create Memberships Table
-- Purpose: User-Organization relationship with roles
-- Created: 2026-01-31
-- ===============================

CREATE TABLE memberships (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_id BIGINT NOT NULL,
    
    -- Role within organization (OWNER, ADMIN, MEMBER)
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER' 
        CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
    
    -- Status (ACTIVE, INVITED, SUSPENDED)
    status VARCHAR(50) DEFAULT 'ACTIVE' 
        CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED')),
    
    -- Timestamps
    joined_at TIMESTAMP DEFAULT NOW(),
    invited_by BIGINT,
    
    -- Foreign keys
    CONSTRAINT fk_memberships_user 
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_memberships_organization 
        FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_memberships_invited_by 
        FOREIGN KEY (invited_by) REFERENCES users(id) ON DELETE SET NULL,
    
    -- A user can only have ONE membership per organization
    UNIQUE(user_id, organization_id)
);

-- Indexes
CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_organization_id ON memberships(organization_id);
CREATE INDEX idx_memberships_role ON memberships(role);
CREATE INDEX idx_memberships_status ON memberships(status);
CREATE INDEX idx_memberships_org_user ON memberships(organization_id, user_id);

-- Create memberships for all existing users (assign to default org as OWNER)
INSERT INTO memberships (user_id, organization_id, role, status)
SELECT u.id, o.id, 'OWNER', 'ACTIVE'
FROM users u
CROSS JOIN organizations o
WHERE o.slug = 'default-org';
