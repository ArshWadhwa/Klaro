package org.example.controller;


import org.example.entity.Issue;
import org.example.entity.User;
import org.example.group.AIResponse;
import org.example.group.CreateIssueRequest;
import org.example.group.IssueResponse;
import org.example.group.UpdateIssuePriorityRequest;
import org.example.group.UpdateIssueStatusRequest;
import org.example.repository.IssueRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.IssueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:3001")
@RequestMapping("/api/issues")
public class IssueController {
    @Autowired private IssueService issueService;
    @Autowired private IssueRepository issueRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthService authService;

@PostMapping
    public ResponseEntity<?> createIssue(
        @RequestBody CreateIssueRequest request,
        @RequestHeader("Authorization") String authHeader
        ){
    try {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = authService.getEmailFromToken(token);
            
            // Check if user can create issues in this project
            boolean canCreateIssue = authService.isAdmin(token) || 
                                   issueService.canUserCreateIssueInProject(email, request.getProjectId());
            
            if (!canCreateIssue) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You don't have permission to create issues in this project");
            }
            
            IssueResponse response = issueService.createIssue(request, email);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
    } catch (RuntimeException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}

@GetMapping("/projects/{projectId}/issues")
    public List<IssueResponse> getIssueByProject(@PathVariable Long projectId){
    List<Issue> issues = issueRepository.findByProjectId(projectId);
    return issues.stream().map(issueService::toResponse).toList();
}



@GetMapping("/issues")
    public List<IssueResponse> getIssueByStatus(@RequestParam(required=false) String status){
    if(status!=null){
        Issue.Status st;

        try {
            st = Issue.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status value: " + status);
        }

        return issueRepository.findByStatus(st)
                .stream()
                .map(issueService::toResponse)
                .toList();
    }
    return issueRepository
            .findAll()
            .stream()
            .map(issueService:: toResponse)
            .toList();
}


    @GetMapping("/issues/assigned-to/me")
    public ResponseEntity<?> issueAssignedToMe(@RequestHeader("Authorization") String authHeader){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                User currentUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                List<Issue> issues = issueRepository.findByAssignedTo(currentUser);
                List<IssueResponse> response = issues.stream().map(issueService::toResponse).toList();
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Get AI recommendation for an issue
    @GetMapping("/{issueId}/ai-recommendation")
    public ResponseEntity<?> getAIRecommendation(
            @PathVariable Long issueId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Get AI recommendation
                AIResponse recommendation = issueService.getAIRecommendationForIssue(issueId);
                return ResponseEntity.ok(recommendation);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Update issue status
    @PatchMapping("/{issueId}/status")
    public ResponseEntity<?> updateIssueStatus(
            @PathVariable Long issueId,
            @RequestBody UpdateIssueStatusRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Update status
                IssueResponse response = issueService.updateIssueStatus(issueId, request.getStatus(), email);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Update issue priority
    @PatchMapping("/{issueId}/priority")
    public ResponseEntity<?> updateIssuePriority(
            @PathVariable Long issueId,
            @RequestBody UpdateIssuePriorityRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Update priority
                IssueResponse response = issueService.updateIssuePriority(issueId, request.getPriority(), email);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Search and filter issues
    @GetMapping("/search")
    public ResponseEntity<?> searchIssues(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Get all issues
                List<Issue> issues = issueRepository.findAll();
                
                // Filter by status if provided
                if (status != null && !status.isEmpty()) {
                    Issue.Status statusEnum = Issue.Status.valueOf(status.toUpperCase());
                    issues = issues.stream()
                            .filter(i -> i.getStatus() == statusEnum)
                            .toList();
                }
                
                // Filter by priority if provided
                if (priority != null && !priority.isEmpty()) {
                    Issue.Priority priorityEnum = Issue.Priority.valueOf(priority.toUpperCase());
                    issues = issues.stream()
                            .filter(i -> i.getPriority() == priorityEnum)
                            .toList();
                }
                
                // Filter by type if provided
                if (type != null && !type.isEmpty()) {
                    Issue.IssueType typeEnum = Issue.IssueType.valueOf(type.toUpperCase());
                    issues = issues.stream()
                            .filter(i -> i.getType() == typeEnum)
                            .toList();
                }
                
                // Filter by project if provided
                if (projectId != null) {
                    issues = issues.stream()
                            .filter(i -> i.getProject().getId().equals(projectId))
                            .toList();
                }
                
                // Filter by assignee if provided
                if (assigneeId != null) {
                    issues = issues.stream()
                            .filter(i -> i.getAssignedTo() != null && i.getAssignedTo().getId().equals(assigneeId))
                            .toList();
                }
                
                // Convert to response
                List<IssueResponse> response = issues.stream()
                        .map(issueService::toResponse)
                        .toList();
                
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid filter value: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}


