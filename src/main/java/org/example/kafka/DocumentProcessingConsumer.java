package org.example.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DISABLED: Kafka consumer for document processing has been disabled.
 * Document processing now happens synchronously in DocumentService.
 */
@Component
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true")
public class DocumentProcessingConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    // Kafka consumer functionality disabled - see DocumentService for synchronous processing
}
