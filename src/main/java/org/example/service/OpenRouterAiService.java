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
            "meta-llama/llama-4-scout-17b-16e-instruct",

    };

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    public AIResponse generateContent(AIRequest request) {
        return generateContent(request.getIssueDescription(),
                "You are a helpful AI assistant. Answer questions based on the provided document context concisely and accurately.");
    }

    public AIResponse generateContent(String userContent, String systemPrompt) {
        Exception lastException = null;

        for (String model : GROQ_MODELS) {
            try {
                logger.info("🤖 Trying Groq model: {}", model);
                String result = callGroq(model, userContent, systemPrompt);
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
        return callGroq(model, userContent,
                "You are a helpful AI assistant. Answer questions based on the provided document context concisely and accurately.");
    }

    private String callGroq(String model, String userContent, String systemPrompt) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);

        // Lower temperature from 0.7 to 0.2 for factual extraction/lookup queries (less
        // creativity, more accuracy)
        double temp = 0.7;
        if (systemPrompt.contains("exhaustive") || systemPrompt.contains("perfect recall")
                || systemPrompt.contains("precise fact-lookup")) {
            temp = 0.2;
        }
        root.put("temperature", temp);
        root.put("max_tokens", maxTokens);
        root.put("stream", false);

        ArrayNode messages = mapper.createArrayNode();

        ObjectNode system = mapper.createObjectNode();
        system.put("role", "system");
        system.put("content", systemPrompt);
        messages.add(system);

        ObjectNode user = mapper.createObjectNode();
        user.put("role", "user");
        user.put("content", userContent);
        messages.add(user);

        root.set("messages", messages);

        RequestBody body = RequestBody.create(
                mapper.writeValueAsString(root),
                MediaType.get("application/json; charset=utf-8"));

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
