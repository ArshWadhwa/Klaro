package org.example.controller;

import org.example.entity.Comment;
import org.example.entity.Issue;
import org.example.group.CommentResponse;
import org.example.group.CreateCommentRequest;
import org.example.repository.CommentRepository;
import org.example.repository.IssueRepository;
import org.example.repository.UserRepository;
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
@CrossOrigin(origins = {
        "http://localhost:3001",
        "https://1e27-2405-201-5803-9887-f09f-e037-ca69-f5e6.ngrok-free.app"
})

@RequiredArgsConstructor

@RequestMapping("/comments")
public class CommentController {

    @Autowired private CommentService commentService;
    @Autowired private IssueService issueService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private IssueRepository issueRepository;
    @Autowired private UserRepository userRepository;


    @PostMapping("/issue/{issueId}")
    public CommentResponse addComment(@PathVariable Long issueId, @RequestBody CreateCommentRequest request,
                                       @AuthenticationPrincipal Object principal){
        Issue issue= issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        // Get user from email (principal is String email now)
        String email = principal.toString();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
