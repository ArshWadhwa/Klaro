package org.example.controller;

import org.example.group.ChatRequest;
import org.example.group.ChatMessageResponse;
import org.example.group.DocumentResponse;
import org.example.group.MessageResponse;
import org.example.repository.DocumentRepository;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.example.service.DocumentService;
import org.example.service.EmbeddingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = {
        "http://localhost:3001",
        "https://1e27-2405-201-5803-9887-f09f-e037-ca69-f5e6.ngrok-free.app"
})
@RequestMapping("/documents")
public class DocumentController {

    private static final org.slf4j.Logger logger = 
        org.slf4j.LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * ======================================
     * Upload document (Synchronous)
     * ======================================
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            // 🔐 Extract user
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            // Synchronous processing: upload, extract, chunk, embed
            DocumentResponse response =
                    documentService.uploadDocumentToProject(file, userEmail, projectId);

            return ResponseEntity.ok(
                    Map.of(
                            "documentId", response.getId(),
                            "status", "COMPLETED",
                            "message", "Document uploaded and processed successfully"
                    )
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * Check processing status
     */
    @GetMapping("/{documentId}/status")
    public ResponseEntity<?> getProcessingStatus(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            authService.getEmailFromToken(token); // validation only

            return documentRepository.findById(documentId)
                    .map(document -> ResponseEntity.ok(
                            Map.of(
                                    "documentId", documentId,
                                    "status", document.getProcessingStatus(),
                                    "fileName", document.getFileName()
                            )
                    ))
                    .orElseThrow(() -> new RuntimeException("Document not found"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectDocs(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            List<DocumentResponse> docs =
                    documentService.getProjectDocuments(projectId, userEmail);

            return ResponseEntity.ok(docs);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserDocuments(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            List<DocumentResponse> documents =
                    documentService.getUserDocuments(userEmail);

            return ResponseEntity.ok(documents);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentById(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            DocumentResponse document =
                    documentService.getDocumentById(documentId, userEmail);

            return ResponseEntity.ok(document);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            documentService.deleteDocument(documentId, userEmail);

            return ResponseEntity.ok(
                    new MessageResponse("Document deleted successfully")
            );

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/{documentId}/chat")
    public ResponseEntity<?> chatWithDocument(
            @PathVariable Long documentId,
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            // Check document exists (allow chat even if still processing - fallback to full text)
            documentRepository.findById(documentId)
                    .orElseThrow(() -> new RuntimeException("Document not found"));

            ChatMessageResponse response =
                    documentService.sendMessage(
                            documentId,
                            request.getMessage(),
                            request.isAiMode(),
                            userEmail
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/{documentId}/chat/history")
    public ResponseEntity<?> getChatHistory(
            @PathVariable Long documentId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String token = authHeader.substring(7);
            String userEmail = authService.getEmailFromToken(token);

            List<ChatMessageResponse> history =
                    documentService.getChatHistory(documentId, userEmail);

            return ResponseEntity.ok(history);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new org.example.group.ErrorResponse(e.getMessage()));
        }
    }

    /**
     * 🧪 Test HuggingFace API connection
     */
    @GetMapping("/test-embedding")
    public ResponseEntity<?> testEmbedding() {
        try {
            logger.info("🧪 Testing HuggingFace API...");
            float[] embedding = embeddingService.getEmbedding("test connection");
            
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "HuggingFace API is working!",
                "embeddingDimensions", embedding.length,
                "sampleValues", new float[]{embedding[0], embedding[1], embedding[2]}
            ));
        } catch (Exception e) {
            logger.error("❌ HuggingFace API test failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage(),
                    "hint", "Check your HuggingFace API key or wait 2-3 minutes for model to load"
                ));
        }
    }
}
