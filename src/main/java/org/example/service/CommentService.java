package org.example.service;


import org.example.entity.Comment;
import org.example.entity.Issue;
import org.example.entity.Organization;
import org.example.group.CommentResponse;
import org.example.repository.CommentRepository;
import org.example.repository.IssueRepository;
import org.example.repository.OrganizationRepository;
import org.example.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.*;
import java.util.*;

import static org.yaml.snakeyaml.events.Event.ID.Comment;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private IssueRepository issueRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;

    /**
     * Create comment on an issue
     */
    public Comment createComment(Long issueId, String content, org.example.entity.User author) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));
        
        Comment comment = new Comment();
        comment.setContent(content);
        comment.setAuthor(author);
        comment.setIssue(issue);
        
        return commentRepository.save(comment);
    }

    public CommentResponse toResponse(Comment comment){

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setContent(comment.getContent());
        response.setAuthor(comment.getAuthor().getFullName());

        return response;
    }
}