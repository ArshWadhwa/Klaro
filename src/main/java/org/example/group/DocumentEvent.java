package org.example.group;

/**
 * Event published when a document is uploaded or needs reprocessing.
 * Consumed by DocumentProcessingConsumer to run RAG asynchronously.
 */
public class DocumentEvent {

    private Long documentId;
    private String triggerEmail;
    private String eventType; // UPLOADED | REPROCESS

    public DocumentEvent() {}

    public DocumentEvent(Long documentId, String triggerEmail, String eventType) {
        this.documentId = documentId;
        this.triggerEmail = triggerEmail;
        this.eventType = eventType;
    }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getTriggerEmail() { return triggerEmail; }
    public void setTriggerEmail(String triggerEmail) { this.triggerEmail = triggerEmail; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    @Override
    public String toString() {
        return "DocumentEvent{documentId=" + documentId + ", eventType='" + eventType + "'}";
    }
}
