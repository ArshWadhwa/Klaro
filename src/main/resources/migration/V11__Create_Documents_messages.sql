-- documents table
CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    public_id VARCHAR(255),  -- Cloudinary public_id for deletion
    file_size BIGINT,
    page_count INTEGER,
    extracted_text TEXT,     -- Full PDF text extracted
    ai_summary TEXT,         -- AI-generated summary
    uploaded_by_id BIGINT REFERENCES users(id),
    uploaded_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- chat_messages table
CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,  -- 'user' or 'assistant'
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

alter table documents add column project_id BIGINT references projects(id) on delete CASCADE;


create index idx_documents_project ON documents(project_id);

ALTER TABLE documents 
ADD COLUMN IF NOT EXISTS organization_id BIGINT REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_documents_organization ON documents(organization_id);


-- Indexes for performance
CREATE INDEX idx_documents_user ON documents(uploaded_by_id);
CREATE INDEX idx_chat_document ON chat_messages(document_id);
CREATE INDEX idx_chat_timestamp ON chat_messages(created_at);