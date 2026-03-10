-- Rename uploaded_by_id to uploaded_by in documents table
ALTER TABLE documents
    RENAME COLUMN uploaded_by_id TO uploaded_by;
