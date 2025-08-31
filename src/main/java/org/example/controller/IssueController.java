package org.example.controller;


import org.example.entity.Issue;
import org.example.group.CreateIssueRequest;
import org.example.group.IssueResponse;
import org.example.repository.IssueRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.IssueService;
import org.example.entity.User;
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

}
