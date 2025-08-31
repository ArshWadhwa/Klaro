package org.example.service;


import org.example.entity.Comment;
import org.example.group.CommentResponse;
import org.example.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.*;
import java.util.*;

import static org.yaml.snakeyaml.events.Event.ID.Comment;

@Service
@RequiredArgsConstructor
//@RequiredArgsConstructor (from Lombok) generates a constructor with required arguments (final fields and fields marked with @NonNull).
//This allows Spring to inject CommentRepository via constructor injection, which is a recommended practice.
//So yes, it is necessary for constructor-based dependency injection when using Lombok
public class CommentService {


    private final CommentRepository commentRepository;


    public CommentResponse toResponse(Comment comment){

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setCreatedAt(comment.getCreatedAt());
        response.setContent(comment.getContent());
        response.setAuthor(comment.getAuthor().getFullName());

        return response;
    }







}

