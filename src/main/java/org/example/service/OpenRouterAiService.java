package org.example.service;

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

    public AIResponse generateContent(AIRequest request) throws IOException {
        String prompt = request.getIssueDescription();
        logger.info("Generating content for prompt: {}", prompt);

        String json = """
                {
                  "model": "meta-llama/llama-3.3-70b-instruct:free",
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"2
                    }
                  ]
                }
                """.formatted(prompt);

        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));

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
                logger.error("OpenRouter API failed: {} - {}", response.code(), errorBody);
                if (response.code() == 402) {
                    throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient credits for OpenRouter API: " + errorBody);
                } else if (response.code() == 429) {
                    throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded for OpenRouter API: " + errorBody);
                }
                throw new ResponseStatusException(HttpStatus.valueOf(response.code()), "OpenRouter API failed: " + errorBody);
            }

            String responseBody = response.body().string();
            logger.debug("OpenRouter API response: {}", responseBody);
            JsonNode root = mapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (choices.isEmpty()) {
                logger.error("No choices in OpenRouter API response");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No choices returned by OpenRouter API");
            }
            String output = choices.get(0).path("message").path("content").asText();

            return new AIResponse(output);
        } catch (Exception e) {
            logger.error("Error processing OpenRouter API response: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to generate content: " + e.getMessage(), e);
        }
    }
}