-- Create comment table
CREATE TABLE comment (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    author_id BIGINT NOT NULL,
    issue_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (issue_id) REFERENCES issue(id) ON DELETE CASCADE
);

-- Add indexes for performance
CREATE INDEX idx_comment_author_id ON comment(author_id);
CREATE INDEX idx_comment_issue_id ON comment(issue_id);
CREATE INDEX idx_comment_created_at ON comment(created_at);
