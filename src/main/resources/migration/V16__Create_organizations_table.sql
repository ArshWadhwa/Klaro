-- ===============================
-- V16: Create Organizations Table
-- Purpose: Multi-tenant SaaS architecture
-- Created: 2026-01-31
-- ===============================

CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    logo_url VARCHAR(500),
    
    -- Subscription info
    plan VARCHAR(50) DEFAULT 'free' CHECK (plan IN ('free', 'pro', 'enterprise')),
    max_users INTEGER DEFAULT 10,
    max_projects INTEGER DEFAULT 5,
    is_active BOOLEAN DEFAULT true,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_organizations_is_active ON organizations(is_active);

-- Insert default organization for existing data
INSERT INTO organizations (name, slug, description, plan, is_active)
VALUES ('Default Organization', 'default-org', 'Auto-created for existing users', 'free', true);
