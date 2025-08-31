package org.example.group;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIResponse {
    private String aiSuggestion;

    // No-args constructor for Jackson
    public AIResponse() {}

    // Constructor with JsonCreator for explicit JSON mapping
    @JsonCreator
    public AIResponse(@JsonProperty("aiSuggestion") String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }

    public String getAiSuggestion() {
        return aiSuggestion;
    }

    public void setAiSuggestion(String aiSuggestion) {
        this.aiSuggestion = aiSuggestion;
    }
}