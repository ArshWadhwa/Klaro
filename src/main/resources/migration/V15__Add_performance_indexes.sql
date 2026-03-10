-- ===============================
-- V15: Add Performance Indexes
-- Purpose: Improve query performance for frequently accessed columns
-- Created: 2026-01-23
-- ===============================

-- Index for users.email (used in authentication - findByEmail)
-- This will drastically improve login performance (199ms -> <10ms)
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Index for groups.owner_id (used in findByOwner)
-- This will improve group listing by owner (3128ms -> <50ms)
CREATE INDEX IF NOT EXISTS idx_groups_owner_id ON groups(owner_id);

-- Index for groups.invite_code (used in join group functionality)
CREATE INDEX IF NOT EXISTS idx_groups_invite_code ON groups(invite_code);

-- Index for group_members.user_id (used in findGroupsByMemberId)
-- This will improve finding groups where user is a member
CREATE INDEX IF NOT EXISTS idx_group_members_user_id ON group_members(user_id);

-- Index for group_members.group_id (used in findMembersByGroupId)
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members(group_id);

-- Composite index for group_members (user_id, group_id) for JOIN queries
CREATE INDEX IF NOT EXISTS idx_group_members_user_group ON group_members(user_id, group_id);

-- Index for projects.created_by (used in finding user's projects)
CREATE INDEX IF NOT EXISTS idx_projects_created_by ON projects(created_by);

-- Index for projects.group_id (used in finding projects in a group)
CREATE INDEX IF NOT EXISTS idx_projects_group_id ON projects(group_id);

-- Index for issue.project_id (used in finding issues in a project)
CREATE INDEX IF NOT EXISTS idx_issue_project_id ON issue(project_id);

-- Index for issue.created_by (used in finding issues created by user)
CREATE INDEX IF NOT EXISTS idx_issue_created_by ON issue(created_by);

-- Index for issue.assigned_to (used in finding issues assigned to user)
CREATE INDEX IF NOT EXISTS idx_issue_assigned_to ON issue(assigned_to);

-- Index for issue.status (used in filtering issues by status)
CREATE INDEX IF NOT EXISTS idx_issue_status ON issue(status);

-- Composite index for issue (project_id, status) for common filtered queries
CREATE INDEX IF NOT EXISTS idx_issue_project_status ON issue(project_id, status);

-- Index for comment.issue_id (used in finding comments for an issue)
CREATE INDEX IF NOT EXISTS idx_comment_issue_id ON comment(issue_id);

-- Index for comment.author_id (used in finding comments by author)
CREATE INDEX IF NOT EXISTS idx_comment_author_id ON comment(author_id);

-- Index for notification.recipient (used in finding notifications for a user)
CREATE INDEX IF NOT EXISTS idx_notification_recipient ON notification(recipient);

-- Index for documents.uploaded_by (used in finding documents uploaded by user)
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents(uploaded_by);

-- Index for chat_messages.document_id (used in finding chat history for a document)
CREATE INDEX IF NOT EXISTS idx_chat_messages_document_id ON chat_messages(document_id);

-- Index for refresh_token.user_id (used in token refresh)
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON refresh_token(user_id);

-- Index for refresh_token.token (used in validating refresh tokens)
CREATE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_token(token);
