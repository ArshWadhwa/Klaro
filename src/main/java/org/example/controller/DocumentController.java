package org.example.controller;


import org.example.group.ChatRequest;
import org.example.group.ChatResponse;
import org.example.group.ChatMessageResponse;
import org.example.group.DocumentResponse;
import org.example.group.MessageResponse;
import org.example.service.AuthService;
import org.example.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3001")
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuthService authService;

    /**
     * Upload document
     * POST /documents/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            DocumentResponse response = documentService.uploadDocument(file, userEmail);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get user's documents
     * GET /documents
     */
    @GetMapping
    public ResponseEntity<?> getUserDocuments(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            List<DocumentResponse> documents = documentService.getUserDocuments(userEmail);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get document by ID
     * GET /documents/{documentId}
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentById(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            DocumentResponse document = documentService.getDocumentById(documentId, userEmail);
            return ResponseEntity.ok(document);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Delete document
     * DELETE /documents/{documentId}
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            documentService.deleteDocument(documentId, userEmail);
            return ResponseEntity.ok(new MessageResponse("Document deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Chat with document
     * POST /documents/{documentId}/chat
     */
    @PostMapping("/{documentId}/chat")
    public ResponseEntity<?> chatWithDocument(
            @PathVariable Long documentId,
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            ChatResponse response = documentService.chat(
                    documentId,
                    request.getMessage(),
                    userEmail
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Get chat history
     * GET /documents/{documentId}/chat/history
     */
    @GetMapping("/{documentId}/chat/history")
    public ResponseEntity<?> getChatHistory(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            List<ChatMessageResponse> history = documentService.getChatHistory(documentId, userEmail);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }
}

//// Helper response classes
//class ErrorResponse {
//    private String message;
//
//    public ErrorResponse(String message) {
//        this.message = message;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//}
//
//class MessageResponse {
//    private String message;
//
//    public MessageResponse(String message) {
//        this.message = message;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//}