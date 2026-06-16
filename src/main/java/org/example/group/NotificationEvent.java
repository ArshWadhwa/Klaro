package org.example.group;

/**
 * Event published when a notification needs to be created.
 * Consumed by NotificationConsumer to persist to DB asynchronously.
 */
public class NotificationEvent {

    private String recipientEmail;
    private String message;
    private String eventType; // ISSUE_UPDATED | COMMENT_ADDED | MEMBER_ADDED | GENERAL

    public NotificationEvent() {}

    public NotificationEvent(String recipientEmail, String message, String eventType) {
        this.recipientEmail = recipientEmail;
        this.message = message;
        this.eventType = eventType;
    }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    @Override
    public String toString() {
        return "NotificationEvent{recipient='" + recipientEmail + "', eventType='" + eventType + "'}";
    }
}
