package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Local embedding service using Ollama.
 *
 * Model:
 * nomic-embed-text
 *
 * Ollama must be running locally:
 * ollama serve
 *
 * Pull model first:
 * ollama pull nomic-embed-text
 */
@Service
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    private static final String OLLAMA_URL = "http://localhost:11434/api/embeddings";

    private static final String MODEL_NAME = "nomic-embed-text";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Warm up local embedding model.
     */
    public void warmupModel() {
        logger.info("🔥 Warming up Ollama embedding model...");

        try {
            float[] embedding = getEmbedding("warmup");

            logger.info(
                    "✅ Ollama embedding model ready! Dimensions: {}",
                    embedding.length);

        } catch (Exception e) {
            logger.error(
                    "❌ Failed to warmup Ollama model",
                    e);
        }
    }

    /**
     * Generate embedding for single text.
     */
    public float[] getEmbedding(String text) {

        try {

            String requestJson = """
                    {
                      "model": "%s",
                      "prompt": %s
                    }
                    """.formatted(
                    MODEL_NAME,
                    mapper.writeValueAsString(text));

            RequestBody body = RequestBody.create(
                    requestJson,
                    MediaType.get("application/json"));

            Request request = new Request.Builder()
                    .url(OLLAMA_URL)
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {

                    String errorBody = response.body() != null
                            ? response.body().string()
                            : "No body";

                    logger.error(
                            "❌ Ollama API Error - Status: {}, Body: {}",
                            response.code(),
                            errorBody);

                    throw new RuntimeException(
                            "Ollama API failed: " + errorBody);
                }

                String responseBody = response.body().string();

                JsonNode rootNode = mapper.readTree(responseBody);

                JsonNode embeddingNode = rootNode.get("embedding");

                if (embeddingNode == null) {
                    throw new RuntimeException(
                            "No embedding returned from Ollama");
                }

                float[] embedding = new float[embeddingNode.size()];

                for (int i = 0; i < embeddingNode.size(); i++) {
                    embedding[i] = (float) embeddingNode.get(i).asDouble();
                }

                logger.debug(
                        "✅ Generated embedding with {} dimensions",
                        embedding.length);

                return embedding;
            }

        } catch (IOException e) {

            logger.error(
                    "❌ Failed to generate embedding",
                    e);

            throw new RuntimeException(
                    "Embedding generation failed",
                    e);
        }
    }

    /**
     * Generate embeddings for multiple texts.
     */
    public List<float[]> getEmbeddingsBatch(List<String> texts) {

        List<float[]> embeddings = new ArrayList<>();

        for (String text : texts) {
            embeddings.add(getEmbedding(text));
        }

        logger.info(
                "✅ Generated {} embeddings",
                embeddings.size());

        return embeddings;
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