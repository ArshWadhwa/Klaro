package org.example.service;

import org.example.entity.Issue;
import org.example.entity.Project;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.group.AIRequest;
import org.example.group.AIResponse;
import org.example.group.CreateIssueRequest;
import org.example.group.IssueResponse;
import org.example.repository.IssueRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IssueService {

    @Autowired
    private IssueRepository issueRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OpenRouterAiService aiService;
    @Autowired
    private NotificationService notificationService;

    public IssueResponse createIssue(CreateIssueRequest request,String currentUserEmail) {
        User creator = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
        }

        Issue issue = new Issue();
        issue.setTitle(request.getTitle());
        issue.setDescription(request.getDescription());
        issue.setPriority(request.getPriority());
        issue.setStatus(request.getStatus());
        issue.setType(request.getType());
        issue.setCreatedBy(creator);
        issue.setAssignedTo(assignee);
        issue.setProject(project);

        Issue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    public IssueResponse toResponse(Issue issue) {
        IssueResponse response = new IssueResponse();
        response.setId(issue.getId());
        response.setTitle(issue.getTitle());
        response.setDescription(issue.getDescription());  // ADD THIS
        response.setCreatedBy(issue.getCreatedBy().getFullName());
        response.setStatus(issue.getStatus().name());
        response.setPriority(issue.getPriority().name());
        response.setType(issue.getType().name());  // ADD THIS
        response.setAssignedTo(issue.getAssignedTo() != null ? issue.getAssignedTo().getFullName() : null);
        response.setCreatedAt(issue.getCreatedAt());  // ADD THIS
        response.setProjectId(issue.getProject().getId());  // ADD THIS if needed
        return response;
    }

    public boolean canUserCreateIssueInProject(String userEmail, Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() == Role.ROLE_ADMIN) return true;

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (project.getGroup() != null) {
            return project.getGroup().getMembers()
                    .stream()
                    .anyMatch(m -> m.getId().equals(user.getId()));
        }

        return project.getCreatedBy().getId().equals(user.getId());
    }


    // Method to get AI recommendation for an issue
    public AIResponse getAIRecommendationForIssue(Long issueId) {
        // Find the issue by ID
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        
        // Create a prompt for AI with issue details
        String prompt = String.format(
            "Analyze this issue and provide recommendations:\n\n" +
            "Title: %s\n" +
            "Type: %s\n" +
            "Priority: %s\n" +
            "Description: %s\n\n" +
            "Please provide:\n" +
            "1. Brief analysis of the issue\n" +
            "2. Suggested approach to solve it\n" +
            "3. Estimated complexity (Low/Medium/High)\n" +
            "4. Potential risks or things to watch out for",
            issue.getTitle(),
            issue.getType(),
            issue.getPriority(),
            issue.getDescription()
        );
        
        // Call AI service to get recommendation
        AIRequest aiRequest = new AIRequest();
        aiRequest.setIssueDescription(prompt);
        
        try {
            return aiService.generateContent(aiRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get AI recommendation: " + e.getMessage());
        }
    }

    public IssueResponse updateIssueStatus(
            Long issueId,
            Issue.Status newStatus,
            String currentUserEmail
    ) {

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

//        // 🔐 Permission check (VERY IMPORTANT)
//        boolean canUpdate =
//                issue.getCreatedBy().getId().equals(user.getId()) ||
//                        (issue.getAssignedTo() != null &&
//                                issue.getAssignedTo().getId().equals(user.getId()));
//
//        if (!canUpdate) {
//            throw new RuntimeException("You are not allowed to update this issue");
//        }

        // ✅ Update status
        issue.setStatus(newStatus);
        Issue updated = issueRepository.save(issue);

        // 🔔 Notify assignee
        if (updated.getAssignedTo() != null) {
            notificationService.createNotification(
                    updated.getAssignedTo().getEmail(),
                    "Issue '" + updated.getTitle() + "' moved to " + newStatus.name()
            );
        }

        return toResponse(updated);
    }


    // Method to update issue priority
    public IssueResponse updateIssuePriority(Long issueId, String newPriority, String currentUserEmail) {
        // Find the issue
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        
        // Convert string to Priority enum
        Issue.Priority priority;
        try {
            priority = Issue.Priority.valueOf(newPriority.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid priority. Use: LOW, MEDIUM, or HIGH");
        }
        
        // Update the priority
        issue.setPriority(priority);
        Issue updated = issueRepository.save(issue);
        
        // Send notification to assignee if priority changed
        if (issue.getAssignedTo() != null) {
            String message = String.format(
                "Priority of issue '%s' was changed to %s",
                issue.getTitle(),
                priority.name()
            );
            notificationService.createNotification(issue.getAssignedTo().getEmail(), message);
        }
        
        return toResponse(updated);
    }

}




