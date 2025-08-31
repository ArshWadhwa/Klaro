package org.example.group;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AIRequest {
    private String issueDescription;

    // No-args constructor for Jackson
    public AIRequest() {}

    // Constructor with JsonCreator for explicit JSON mapping
    @JsonCreator
    public AIRequest(@JsonProperty("issueDescription") String issueDescription) {
        this.issueDescription = issueDescription;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public void setIssueDescription(String issueDescription) {
        this.issueDescription = issueDescription;
    }
}