package org.example.controller;


import org.example.group.AIRequest;
import org.example.group.AIResponse;
import org.example.service.OpenRouterAiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController

@RequestMapping("/ai")
public class AIController {

    @Autowired private OpenRouterAiService service;

    //The issue is that GeminiAiService.generateContent(request) in your
    // AIController throws a checked exception (java.io.IOException), but your
    // controller method getAiResponse does not handle or declare it.
    // In Spring controllers, you should either:
    //Catch the exception and handle it (e.g., return an error response),
    //Or declare the method with throws IOException.




    @PostMapping("/generate")
    public ResponseEntity<AIResponse> getAiResponse(@RequestBody AIRequest request) {
        AIResponse response = service.generateContent(request);
        return ResponseEntity.ok(response);
    }




}
