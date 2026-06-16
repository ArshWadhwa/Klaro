package org.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DISABLED: Kafka consumer for notifications has been disabled.
 * Notifications now persist synchronously in NotificationService.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class NotificationConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationConsumer.class);

    // Kafka consumer functionality disabled - see NotificationService for synchronous persistence
}
