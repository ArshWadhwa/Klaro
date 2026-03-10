package org.example.config;

import org.example.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Startup initializer to warmup HuggingFace model on application start.
 * This prevents cold-start delays when processing first document.
 */
@Component
public class StartupInitializer implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StartupInitializer.class);
    
    private final EmbeddingService embeddingService;
    
    public StartupInitializer(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }
    
    @Override
    public void run(String... args) {
        logger.info("🚀 Running startup initialization...");
        
        // Warmup HuggingFace model in background
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5s after app starts
                embeddingService.warmupModel();
            } catch (Exception e) {
                logger.error("Failed to warmup model", e);
            }
        }).start();
        
        logger.info("✅ Startup initialization complete");
    }
}
