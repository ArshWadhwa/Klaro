-- Add project_id column to documents table to make documents project-specific
ALTER TABLE documents 
ADD COLUMN project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE;

-- Add index for performance when querying documents by project
CREATE INDEX idx_documents_project ON documents(project_id);

-- Optional: Add comment for clarity
COMMENT ON COLUMN documents.project_id IS 'Foreign key to projects table - makes documents project-specific';
