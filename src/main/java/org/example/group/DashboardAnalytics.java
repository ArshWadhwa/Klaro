package org.example.group;

import lombok.Data;
import java.util.Map;

@Data
public class DashboardAnalytics {
    private Long totalIssues;
    private Long totalProjects;
    private Long totalUsers;
    
    // Issues by status: {"TO_DO": 5, "IN_PROGRESS": 3, "DONE": 10}
    private Map<String, Long> issuesByStatus;
    
    // Issues by priority: {"LOW": 2, "MEDIUM": 8, "HIGH": 8}
    private Map<String, Long> issuesByPriority;
    
    // Issues by type: {"BUG": 5, "FEATURE": 7, "TASK": 6}
    private Map<String, Long> issuesByType;
}
