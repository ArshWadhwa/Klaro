package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service to generate text embeddings using HuggingFace Inference API.
 * Uses the sentence-transformers/all-MiniLM-L6-v2 model (384-dim vectors).
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Value("${huggingface.api.key}")
    private String hfApiKey;

    private static final String HF_API_URL =
            "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // Increased to 60s
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)    // Increased to 120s
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 🔥 Warmup method - Call this to pre-load the HuggingFace model
     * Run this once when app starts to avoid cold-start delays
     */
    public void warmupModel() {
        logger.info("🔥 Warming up HuggingFace model...");
        try {
            // Send a dummy request to load the model
            float[] embedding = getEmbedding("test warmup");
            logger.info("✅ HuggingFace model warmed up successfully! Embedding dims: {}", embedding.length);
        } catch (Exception e) {
            logger.warn("⚠️ Failed to warmup HuggingFace model - model will load on first use: {}", e.getMessage());
        }
    }

    /**
     * Generate embedding for a single text string.
     * Returns a float array (384 dimensions for MiniLM-L6-v2).
     * 
     * Handles HuggingFace model loading delays (first request takes 2-5 min)
     */
    public float[] getEmbedding(String text) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // HuggingFace expects: {"inputs": "text here"}
                ObjectNode requestBody = mapper.createObjectNode();
                requestBody.put("inputs", text);

                RequestBody body = RequestBody.create(
                        mapper.writeValueAsString(requestBody),
                        MediaType.get("application/json")
                );

                Request request = new Request.Builder()
                        .url(HF_API_URL)
                        .addHeader("Authorization", "Bearer " + hfApiKey)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "No body";
                        
                        // Check if it's HTML error (model loading)
                        if (errorBody.contains("<!doctype html>") || errorBody.contains("<html>")) {
                            logger.warn("⏳ HuggingFace model is loading... Attempt {}/{} - waiting 30s", 
                                    retryCount + 1, maxRetries);
                            
                            if (retryCount < maxRetries - 1) {
                                Thread.sleep(30000); // Wait 30 seconds for model to load
                                retryCount++;
                                continue;
                            } else {
                                throw new RuntimeException(
                                    "HuggingFace model failed to load after " + maxRetries + " attempts. " +
                                    "Model is cold-starting - please try again in 2-3 minutes."
                                );
                            }
                        }
                        
                        logger.error("❌ HuggingFace API Error - Status: {}, Body: {}", response.code(), errorBody);
                        
                        // If rate limited (HTTP 429 or 503), throw retryable error
                        if (response.code() == 429 || response.code() == 503) {
                            logger.warn("⚠️ Rate limited or service unavailable - will retry");
                            throw new IOException("HuggingFace rate limit/unavailable: " + errorBody);
                        }
                        
                        throw new RuntimeException("HuggingFace API failed: " + errorBody);
                    }

                    String responseBody = response.body().string();
                    
                    // Double-check it's not HTML
                    if (responseBody.contains("<!doctype html>") || responseBody.contains("<html>")) {
                        logger.warn("⏳ Received HTML instead of JSON - model loading. Retry {}/{}", 
                                retryCount + 1, maxRetries);
                        
                        if (retryCount < maxRetries - 1) {
                            Thread.sleep(30000);
                            retryCount++;
                            continue;
                        } else {
                            throw new RuntimeException(
                                "HuggingFace model not ready. Please try again in 2-3 minutes."
                            );
                        }
                    }
                    
                    logger.debug("✅ HuggingFace response received: {} bytes", responseBody.length());
                    
                    JsonNode rootNode = mapper.readTree(responseBody);

                    // Response is a flat array of floats: [0.1, -0.2, 0.3, ...]
                    float[] embedding = new float[rootNode.size()];
                    for (int i = 0; i < rootNode.size(); i++) {
                        embedding[i] = (float) rootNode.get(i).asDouble();
                    }

                    logger.debug("✅ Generated embedding with {} dimensions", embedding.length);
                    return embedding;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for HuggingFace model", e);
            } catch (IOException e) {
                logger.error("❌ Failed to get embedding from HuggingFace: {}", e.getMessage());
                
                // Retry on network errors
                if (retryCount < maxRetries - 1) {
                    retryCount++;
                    logger.warn("🔄 Retrying... attempt {}/{}", retryCount + 1, maxRetries);
                    try {
                        Thread.sleep(5000); // Wait 5s before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                    continue;
                }
                
                throw new RuntimeException("Embedding generation failed after " + maxRetries + " attempts", e);
            }
        }
        
        throw new RuntimeException("Failed to generate embedding after " + maxRetries + " attempts");
    }

    /**
     * Generate embeddings for multiple texts in a single batch call.
     * More efficient than calling one by one.
     */
    public List<float[]> getEmbeddingsBatch(List<String> texts) {
        try {
            // HuggingFace expects: {"inputs": ["text1", "text2", ...]}
            ObjectNode requestBody = mapper.createObjectNode();
            ArrayNode inputsArray = mapper.createArrayNode();
            for (String text : texts) {
                inputsArray.add(text);
            }
            requestBody.set("inputs", inputsArray);

            RequestBody body = RequestBody.create(
                    mapper.writeValueAsString(requestBody),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(HF_API_URL)
                    .addHeader("Authorization", "Bearer " + hfApiKey)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    logger.error("HuggingFace Batch API Error - Status: {}, Body: {}", response.code(), errorBody);
                    throw new RuntimeException("HuggingFace API failed: " + errorBody);
                }

                String responseBody = response.body().string();
                JsonNode rootNode = mapper.readTree(responseBody);

                // Response is array of arrays: [[0.1, -0.2, ...], [0.3, 0.4, ...]]
                List<float[]> embeddings = new ArrayList<>();
                for (JsonNode embNode : rootNode) {
                    float[] embedding = new float[embNode.size()];
                    for (int i = 0; i < embNode.size(); i++) {
                        embedding[i] = (float) embNode.get(i).asDouble();
                    }
                    embeddings.add(embedding);
                }

                logger.info("Generated {} embeddings in batch", embeddings.size());
                return embeddings;
            }
        } catch (IOException e) {
            logger.error("Failed to get batch embeddings from HuggingFace", e);
            throw new RuntimeException("Batch embedding generation failed", e);
        }
    }

    /**
     * Serialize float array to JSON string for database storage.
     */
    public String serializeEmbedding(float[] embedding) {
        try {
            return mapper.writeValueAsString(embedding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize embedding", e);
        }
    }

    /**
     * Deserialize JSON string back to float array.
     */
    public float[] deserializeEmbedding(String json) {
        try {
            JsonNode node = mapper.readTree(json);
            float[] embedding = new float[node.size()];
            for (int i = 0; i < node.size(); i++) {
                embedding[i] = (float) node.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize embedding", e);
        }
    }

    /**
     * Calculate cosine similarity between two vectors.
     * Returns a value between -1 and 1 (1 = most similar).
     */
    public double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA.length != vecB.length) {
            throw new IllegalArgumentException("Vectors must be same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += vecA[i] * vecA[i];
            normB += vecB[i] * vecB[i];
        }

        if (normA == 0 || normB == 0) return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
