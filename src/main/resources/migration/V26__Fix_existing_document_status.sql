-- Set existing documents with extracted text to COMPLETED (they were processed before Temporal)
UPDATE documents SET processing_status = 'COMPLETED' WHERE extracted_text IS NOT NULL AND processing_status IS NULL;
UPDATE documents SET processing_status = 'COMPLETED' WHERE extracted_text IS NOT NULL AND processing_status = 'PENDING';

-- Set documents without text to FAILED
UPDATE documents SET processing_status = 'FAILED' WHERE extracted_text IS NULL AND processing_status IS NULL;
