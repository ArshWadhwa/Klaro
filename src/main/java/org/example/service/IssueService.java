package org.example.service;

import org.example.entity.Issue;
import org.example.entity.Project;
import org.example.entity.User;
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
        response.setCreatedBy(issue.getCreatedBy().getFullName());
        response.setStatus(issue.getStatus().name());
        response.setPriority(issue.getPriority().name());
        response.setAssignedTo(issue.getAssignedTo() != null ? issue.getAssignedTo().getFullName() : null);
        return response;
    }

    public boolean canUserCreateIssueInProject(String userEmail, Long projectId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        
        // User can create issues if they are a member of the project's group
        if (project.getGroup() != null) {
            return project.getGroup().getMembers().contains(user);
        }
        
        // If project has no group, only the creator can add issues
        return project.getCreatedBy().equals(user);
    }

}


