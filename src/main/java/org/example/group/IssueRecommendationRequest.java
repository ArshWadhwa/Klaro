package org.example.group;

import lombok.Data;

@Data
public class IssueRecommendationRequest {
    private Long issueId;
    private String issueTitle;
    private String issueDescription;
    private String issueType;  // BUG, FEATURE, TASK
    private String priority;   // LOW, MEDIUM, HIGH
}

