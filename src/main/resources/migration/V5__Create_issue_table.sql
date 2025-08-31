-- Create issue table
CREATE TABLE issue (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(10) DEFAULT 'MEDIUM' NOT NULL,
    status VARCHAR(20) DEFAULT 'TO_DO' NOT NULL,
    type VARCHAR(10) DEFAULT 'TASK' NOT NULL,
    created_by BIGINT NOT NULL,
    assigned_to BIGINT,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    CONSTRAINT status_check CHECK (status IN ('TO_DO', 'IN_PROGRESS', 'DONE')),
    CONSTRAINT type_check CHECK (type IN ('BUG', 'FEATURE', 'TASK'))
);

-- Add indexes for performance
CREATE INDEX idx_issue_created_by ON issue(created_by);
CREATE INDEX idx_issue_assigned_to ON issue(assigned_to);
CREATE INDEX idx_issue_project_id ON issue(project_id);
CREATE INDEX idx_issue_status ON issue(status);
CREATE INDEX idx_issue_priority ON issue(priority);
CREATE INDEX idx_issue_type ON issue(type);
