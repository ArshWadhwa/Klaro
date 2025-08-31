-- Create notification table
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    recipient VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX idx_notification_recipient ON notification(recipient);
CREATE INDEX idx_notification_read ON notification(read);
CREATE INDEX idx_notification_created_at ON notification(created_at);
