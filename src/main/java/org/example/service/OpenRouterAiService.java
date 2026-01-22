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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
@Service
public class OpenRouterAiService {
    private static final Logger logger = LoggerFactory.getLogger(OpenRouterAiService.class);

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.site.url}")
    private String siteUrl; // e.g., http://localhost:8080 or your app's URL

    @Value("${openrouter.site.name}")
    private String siteName; // e.g., YourAppName

    private static final String OPENROUTER_API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public AIResponse generateContent(AIRequest request) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("model", "meta-llama/llama-3.3-70b-instruct:free");

            ArrayNode messages = mapper.createArrayNode();

            ObjectNode system = mapper.createObjectNode();
            system.put("role", "system");
            system.put("content", "You are an AI assistant helping users understand a document.");
            messages.add(system);

            ObjectNode user = mapper.createObjectNode();
            user.put("role", "user");
            user.put("content", request.getIssueDescription());
            messages.add(user);

            root.set("messages", messages);

            RequestBody body = RequestBody.create(
                    mapper.writeValueAsString(root),
                    MediaType.get("application/json")
            );

            Request httpRequest = new Request.Builder()
                    .url(OPENROUTER_API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("HTTP-Referer", siteUrl)
                    .addHeader("X-Title", siteName)
                    .post(body)
                    .build();

            try (Response response = client.newCall(httpRequest).execute()) {

                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    throw new ResponseStatusException(
                            HttpStatus.valueOf(response.code()),
                            "OpenRouter API failed: " + errorBody
                    );
                }

                String responseBody = response.body().string();
                JsonNode rootNode = mapper.readTree(responseBody);
                String output = rootNode
                        .path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

                return new AIResponse(output);
            }

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to generate AI response",
                    e
            );
        }
    }
}
