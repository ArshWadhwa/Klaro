package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cloud embedding service using Hugging Face Inference API.
 *
 * Model:
 * sentence-transformers/all-mpnet-base-v2 (768 dimensions)
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String HF_API_URL = "https://router.huggingface.co/hf-inference/models/mixedbread-ai/mxbai-embed-large-v1";

    @Value("${huggingface.api.key}")
    private String hfApiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Warm up Hugging Face embedding model.
     */
    public void warmupModel() {
        logger.info("🔥 Warming up Hugging Face embedding model...");

        try {
            float[] embedding = getEmbedding("warmup");

            logger.info(
                    "✅ Hugging Face embedding model ready! Dimensions: {}",
                    embedding.length);

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to warmup Hugging Face model",
                    e);
        }
    }

    /**
     * Generate embedding for single text with automatic loading retry.
     */
    public float[] getEmbedding(String text) {
        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                String requestJson = "{\"inputs\":" + mapper.writeValueAsString(text) + "}";

                RequestBody body = RequestBody.create(
                        requestJson,
                        MediaType.get("application/json"));

                Request request = new Request.Builder()
                        .url(HF_API_URL)
                        .header("Authorization", "Bearer " + hfApiKey)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    ResponseBody responseBodyObj = response.body();
                    String responseBody = responseBodyObj != null ? responseBodyObj.string() : "";

                    if (!response.isSuccessful()) {
                        JsonNode rootNode = mapper.readTree(responseBody);
                        if (rootNode.has("error")) {
                            String errorMsg = rootNode.get("error").asText();
                            if (errorMsg.contains("loading")) {
                                double estimatedTime = rootNode.has("estimated_time") ? rootNode.get("estimated_time").asDouble() : 5.0;
                                logger.warn("⏳ Hugging Face model is loading. Retrying in {} seconds (Attempt {}/{})", estimatedTime, attempt + 1, maxRetries);
                                attempt++;
                                Thread.sleep((long) (estimatedTime * 1000));
                                continue;
                            }
                        }
                        logger.error("❌ Hugging Face API Error - Status: {}, Body: {}", response.code(), responseBody);
                        throw new RuntimeException("Hugging Face API failed: " + responseBody);
                    }

                    JsonNode rootNode = mapper.readTree(responseBody);
                    if (rootNode.isArray()) {
                        int dims = Math.min(rootNode.size(), 768);
                        float[] embedding = new float[dims];
                        for (int i = 0; i < dims; i++) {
                            embedding[i] = (float) rootNode.get(i).asDouble();
                        }
                        logger.debug("✅ Generated embedding with {} dimensions (truncated from {})", embedding.length, rootNode.size());
                        return embedding;
                    } else {
                        throw new RuntimeException("Unexpected response format from Hugging Face: " + responseBody);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("❌ Failed to generate embedding", e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Embedding generation failed", e);
            }
        }
        throw new RuntimeException("Hugging Face model failed to load after multiple retries");
    }

    /**
     * Generate embeddings for multiple texts using HF batch inference.
     */
    public List<float[]> getEmbeddingsBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        int maxRetries = 5;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                String requestJson = "{\"inputs\":" + mapper.writeValueAsString(texts) + "}";

                RequestBody body = RequestBody.create(
                        requestJson,
                        MediaType.get("application/json"));

                Request request = new Request.Builder()
                        .url(HF_API_URL)
                        .header("Authorization", "Bearer " + hfApiKey)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    ResponseBody responseBodyObj = response.body();
                    String responseBody = responseBodyObj != null ? responseBodyObj.string() : "";

                    if (!response.isSuccessful()) {
                        JsonNode rootNode = mapper.readTree(responseBody);
                        if (rootNode.has("error")) {
                            String errorMsg = rootNode.get("error").asText();
                            if (errorMsg.contains("loading")) {
                                double estimatedTime = rootNode.has("estimated_time") ? rootNode.get("estimated_time").asDouble() : 5.0;
                                logger.warn("⏳ Hugging Face model is loading. Retrying in {} seconds (Attempt {}/{})", estimatedTime, attempt + 1, maxRetries);
                                attempt++;
                                Thread.sleep((long) (estimatedTime * 1000));
                                continue;
                            }
                        }
                        logger.error("❌ Hugging Face API Error - Status: {}, Body: {}", response.code(), responseBody);
                        throw new RuntimeException("Hugging Face API failed: " + responseBody);
                    }

                    JsonNode rootNode = mapper.readTree(responseBody);
                    if (rootNode.isArray()) {
                        List<float[]> embeddings = new ArrayList<>();
                        for (int i = 0; i < rootNode.size(); i++) {
                            JsonNode item = rootNode.get(i);
                            int dims = Math.min(item.size(), 768);
                            float[] embedding = new float[dims];
                            for (int j = 0; j < dims; j++) {
                                embedding[j] = (float) item.get(j).asDouble();
                            }
                            embeddings.add(embedding);
                        }
                        logger.info("✅ Generated {} embeddings (truncated to 768 dimensions)", embeddings.size());
                        return embeddings;
                    } else {
                        throw new RuntimeException("Unexpected response format from Hugging Face: " + responseBody);
                    }
                }
            } catch (IOException | InterruptedException e) {
                logger.error("❌ Failed to generate embeddings batch", e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new RuntimeException("Embedding batch generation failed", e);
            }
        }
        throw new RuntimeException("Hugging Face model failed to load after multiple retries");
    }

    /**
     * Serialize embedding to JSON.
     */
    public String serializeEmbedding(float[] embedding) {

        try {
            return mapper.writeValueAsString(embedding);

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to serialize embedding",
                    e);
        }
    }

    /**
     * Deserialize embedding JSON.
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

            throw new RuntimeException(
                    "Failed to deserialize embedding",
                    e);
        }
    }

    /**
     * Cosine similarity between vectors.
     */
    public double cosineSimilarity(
            float[] vecA,
            float[] vecB) {

        if (vecA.length != vecB.length) {
            throw new IllegalArgumentException(
                    "Vectors must be same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {

            dotProduct += vecA[i] * vecB[i];

            normA += vecA[i] * vecA[i];

            normB += vecB[i] * vecB[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct /
                (Math.sqrt(normA) * Math.sqrt(normB));
    }
}