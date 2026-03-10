package org.example.service;

import org.example.entity.Issue;
import org.example.group.DashboardAnalytics;
import org.example.repository.IssueRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private UserRepository userRepository;

    // Method to get dashboard analytics
    public DashboardAnalytics getDashboardAnalytics() {
        DashboardAnalytics analytics = new DashboardAnalytics();
        
        // Get total counts
        analytics.setTotalIssues(issueRepository.count());
        analytics.setTotalProjects(projectRepository.count());
        analytics.setTotalUsers(userRepository.count());
        
        // Get all issues for analysis
        List<Issue> allIssues = issueRepository.findAll();
        
        // Count issues by status
        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("TO_DO", allIssues.stream()
            .filter(i -> i.getStatus() == Issue.Status.TO_DO)
            .count());
        statusMap.put("IN_PROGRESS", allIssues.stream()
            .filter(i -> i.getStatus() == Issue.Status.IN_PROGRESS)
            .count());
        statusMap.put("DONE", allIssues.stream()
            .filter(i -> i.getStatus() == Issue.Status.DONE)
            .count());
        analytics.setIssuesByStatus(statusMap);
        
        // Count issues by priority
        Map<String, Long> priorityMap = new HashMap<>();
        priorityMap.put("LOW", allIssues.stream()
            .filter(i -> i.getPriority() == Issue.Priority.LOW)
            .count());
        priorityMap.put("MEDIUM", allIssues.stream()
            .filter(i -> i.getPriority() == Issue.Priority.MEDIUM)
            .count());
        priorityMap.put("HIGH", allIssues.stream()
            .filter(i -> i.getPriority() == Issue.Priority.HIGH)
            .count());
        analytics.setIssuesByPriority(priorityMap);
        
        // Count issues by type
        Map<String, Long> typeMap = new HashMap<>();
        typeMap.put("BUG", allIssues.stream()
            .filter(i -> i.getType() == Issue.IssueType.BUG)
            .count());
        typeMap.put("FEATURE", allIssues.stream()
            .filter(i -> i.getType() == Issue.IssueType.FEATURE)
            .count());
        typeMap.put("TASK", allIssues.stream()
            .filter(i -> i.getType() == Issue.IssueType.TASK)
            .count());
        analytics.setIssuesByType(typeMap);
        
        return analytics;
    }
}
