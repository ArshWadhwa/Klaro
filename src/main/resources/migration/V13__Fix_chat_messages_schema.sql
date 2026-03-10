-- Fix chat_messages table to match entity expectations
ALTER TABLE chat_messages 
    DROP COLUMN IF EXISTS user_id,
    DROP COLUMN IF EXISTS timestamp,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN role TYPE VARCHAR(20),
    ALTER COLUMN document_id SET NOT NULL;

-- Fix documents table to match entity expectations
ALTER TABLE documents 
    RENAME COLUMN uploaded_by_id TO uploaded_by;

-- Make publicId NOT NULL in documents table
ALTER TABLE documents 
    ALTER COLUMN public_id SET NOT NULL;

-- Update index
DROP INDEX IF EXISTS idx_chat_timestamp;
CREATE INDEX IF NOT EXISTS idx_chat_created_at ON chat_messages(created_at);
