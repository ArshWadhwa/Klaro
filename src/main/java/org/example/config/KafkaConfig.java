package org.example.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * DISABLED: Kafka configuration has been disabled.
 * Document processing and notifications now use synchronous persistence.
 * 
 * To re-enable Kafka, set kafka.enabled=true in application.properties
 */
@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class KafkaConfig {
    // Kafka beans disabled - see DocumentService and NotificationService for synchronous processing
}

