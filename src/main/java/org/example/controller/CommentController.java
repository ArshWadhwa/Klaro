package org.example.controller;

import org.example.entity.Comment;
import org.example.entity.Issue;
import org.example.group.CommentResponse;
import org.example.group.CreateCommentRequest;
import org.example.repository.CommentRepository;
import org.example.repository.IssueRepository;
import org.example.service.AuthService;
import org.example.service.CommentService;
import org.example.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.example.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor

@RequestMapping("/comments")
public class CommentController {

    @Autowired private CommentService commentService;
    @Autowired private IssueService issueService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private IssueRepository issueRepository;


    @PostMapping("/issue/{issueId}")
    public CommentResponse addComment(@PathVariable Long issueId , @RequestBody CreateCommentRequest request,@AuthenticationPrincipal User user){
        Issue issue= issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        Comment comment=new Comment();
        comment.setContent(request.getContent());
        comment.setIssue(issue);
        comment.setId(issueId);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());

        return commentService.toResponse(commentRepository.save(comment));



    }
@GetMapping("/issue/{issueId}")
    public List<CommentResponse> getResponse(@PathVariable Long issueId){
        return commentRepository.findByIssueId(issueId)
                .stream()
                .map(commentService::toResponse)
                .toList();
}

}
