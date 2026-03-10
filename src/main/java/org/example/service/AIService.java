package org.example.service;


import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.example.group.OpenRouterResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // it generates summary...
    public String generateSummary(String documentText) {

        String limitedText = documentText.length() > 3000
                ? documentText.substring(0, 3000)
                : documentText;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "arcee-ai/trinity-large-preview:free"); // Change model here

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "You are a helpful assistant that summarizes documents concisely."));
        messages.add(Map.of("role", "user",
                "content", "Summarize this into 2–3 sentences:\n\n" + limitedText));

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 200);

        OpenRouterResponse response = webClient.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .block();

        return response.getChoices().get(0).getMessage().getContent();
    }


    public String chatWithDocument(String documentText,
                                   String userQuestion,
                                   List<Map<String, String>> chatHistory) {

        List<Map<String, String>> messages = new ArrayList<>();

        // system instruction
        messages.add(Map.of(
                "role", "system",
                "content", "Answer strictly based on the following document:\n\n"
                        + (documentText.length() > 2000
                        ? documentText.substring(0, 2000) + "..."
                        : documentText)
        ));

        // include last 5 messages from previous chat
        int start = Math.max(0, chatHistory.size() - 5);
        messages.addAll(chatHistory.subList(start, chatHistory.size()));

        // new user message
        messages.add(Map.of("role", "user", "content", userQuestion));

        // request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", "openai/gpt-4.1"); // Change model here
        body.put("messages", messages);
        body.put("max_tokens", 500);

        OpenRouterResponse response = webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OpenRouterResponse.class)
                .block();

        return response.getChoices().get(0).getMessage().getContent();
    }

}

