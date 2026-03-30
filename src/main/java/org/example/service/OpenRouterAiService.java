package org.example.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.group.AIRequest;
import org.example.group.AIResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class OpenRouterAiService {

    private static final Logger logger = LoggerFactory.getLogger(OpenRouterAiService.class);

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${ai.max-tokens:3072}")
    private int maxTokens;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    // Groq free models — tried in order, first working one wins
    private static final String[] GROQ_MODELS = {
        "llama-3.3-70b-versatile",
        "llama3-8b-8192",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    };

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public AIResponse generateContent(AIRequest request) {
        Exception lastException = null;

        for (String model : GROQ_MODELS) {
            try {
                logger.info("🤖 Trying Groq model: {}", model);
                String result = callGroq(model, request.getIssueDescription());
                logger.info("✅ AI response received from Groq model: {}", model);
                return new AIResponse(result);
            } catch (Exception e) {
                logger.warn("⚠️ Groq model {} failed: {} — trying next...", model, e.getMessage());
                lastException = e;
            }
        }

        logger.error("❌ All Groq models failed. Last error: {}",
                lastException != null ? lastException.getMessage() : "unknown");
        return new AIResponse("⚠️ AI service is temporarily unavailable. Please try again in a moment.");
    }

    private String callGroq(String model, String userContent) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        root.put("temperature", 0.7);
        root.put("max_tokens", maxTokens);
        root.put("stream", false);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode system = mapper.createObjectNode();
        system.put("role", "system");
        system.put("content", "You are a helpful AI assistant. Answer questions based on the provided document context concisely and accurately.");
        messages.add(system);

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);

        root.set("messages", messages);

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(root),
                MediaType.get("application/json; charset=utf-8")
        );

        Request httpRequest = new Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Authorization", "Bearer " + groqApiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = client.newCall(httpRequest).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                logger.error("Groq [{}] HTTP {}: {}", model, response.code(), responseBody);
                throw new IOException("HTTP " + response.code() + ": " + responseBody);
            }

            JsonNode rootNode = mapper.readTree(responseBody);

            if (rootNode.has("error")) {
                String errorMsg = rootNode.path("error").path("message").asText(responseBody);
                throw new IOException("Groq API error: " + errorMsg);
            }

            JsonNode choices = rootNode.path("choices");
            if (choices.isMissingNode() || choices.isEmpty()) {
                throw new IOException("No choices in Groq response: " + responseBody);
            }

            String content = choices.get(0).path("message").path("content").asText("");
            if (content.isBlank()) {
                throw new IOException("Empty content from Groq: " + responseBody);
            }

            return content;
        }
    }
}
