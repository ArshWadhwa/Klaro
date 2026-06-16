-- Drop index first
DROP INDEX IF EXISTS idx_chunks_embedding_vector;

-- Drop old column and add new one with 768 dimensions for nomic-embed-text compatibility
ALTER TABLE document_chunks DROP COLUMN IF EXISTS embedding_vector;
ALTER TABLE document_chunks ADD COLUMN embedding_vector vector(768);

-- Recreate index with lists = 100
CREATE INDEX idx_chunks_embedding_vector
    ON document_chunks
    USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 100);
