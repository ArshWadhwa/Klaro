-- Create groups table
CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

ALTER TABLE groups ADD COLUMN invite_code VARCHAR(12);

-- Make it unique (add index)
CREATE UNIQUE INDEX idx_groups_invite_code ON groups(invite_code);

-- Generate codes for existing groups
UPDATE groups SET invite_code = CONCAT('GRP', LPAD(CAST(id AS CHAR), 7, '0')) WHERE invite_code IS NULL OR invite_code = '';

-- Now make it NOT NULL (after populating)
ALTER TABLE groups MODIFY COLUMN invite_code VARCHAR(12) NOT NULL;
-- Add indexes for performance
CREATE INDEX idx_groups_owner_id ON groups(owner_id);
CREATE INDEX idx_groups_name ON groups(name);
