-- Enable pgvector extension (Supabase supports this natively)
-- Run CREATE EXTENSION manually via Supabase dashboard if this fails:
--   Dashboard → SQL Editor → "CREATE EXTENSION IF NOT EXISTS vector;"
CREATE EXTENSION IF NOT EXISTS vector;

-- Add the vector column to document_chunks
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS embedding_vector vector(384);

-- Create an IVFFlat index for fast approximate nearest-neighbor search.
-- lists=100 is a good default for up to ~1M vectors.
-- Rebuild with higher lists value if collection grows beyond 1M rows.
CREATE INDEX IF NOT EXISTS idx_chunks_embedding_vector
    ON document_chunks
    USING ivfflat (embedding_vector vector_cosine_ops)
    WITH (lists = 100);
