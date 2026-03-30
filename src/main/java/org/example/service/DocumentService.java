package org.example.service;

import org.example.entity.Project;
import org.example.entity.ChatMessage;
import org.example.entity.Document;
import org.example.entity.User;
import org.example.group.ChatMessageResponse;
import org.example.group.DocumentResponse;
import org.example.repository.ChatMessageRepository;
import org.example.repository.DocumentChunkRepository;
import org.example.repository.DocumentRepository;
import org.example.repository.ProjectRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PDFExtractionService pdfExtractionService;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private OpenRouterAiService openRouterAiService;

    @Autowired
    private RAGService ragService;


    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    @Lazy
    private DocumentService self;

    /**
     * PostgreSQL text/varchar cannot store NUL (\u0000) bytes.
     * PDF extraction can occasionally introduce them, so strip before persistence.
     */
    private String sanitizeForPostgres(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\u0000", "");
    }

    private DocumentResponse toDocumentResponse(Document document) {

        DocumentResponse response = new DocumentResponse();

        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setFileUrl(document.getFileUrl());
        response.setFileSize(document.getFileSize());
        response.setPageCount(document.getPageCount());
        response.setSummary(document.getAiSummary());
        response.setProcessingStatus(document.getProcessingStatus());
        response.setUploadedAt(document.getUploadedAt());

        if (document.getUploadedBy() != null) {
            response.setUploadedByEmail(document.getUploadedBy().getEmail());
        }

          // Add project info if exists
    if (document.getProject() != null) {
        response.setProjectId(document.getProject().getId());
        response.setProjectName(document.getProject().getName());
    }
    
    

        return response;
    }


//    public void chatWithDocument()

    public List<DocumentResponse> getUserDocuments(String userEmail){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()-> new RuntimeException("User not found"));

        return documentRepository.findByUploadedBy(user)
                .stream()
                .map(this:: toDocumentResponse)
                .collect(Collectors.toList());

    }

    public DocumentResponse getDocumentById(Long documentId ,String userEmail){

        Document document = documentRepository.findById(documentId)
                .orElseThrow(()->new RuntimeException("Doc not found"));

        if (!document.getUploadedBy().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized");
        }

        return toDocumentResponse(document);

    }





    // Generic upload (without project)
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String userEmail) throws IOException {
        // Call through proxy so REQUIRES_NEW on saveDocumentRecord is actually applied.
        Document saved = self.saveDocumentRecord(file, userEmail, null);
        triggerRAG(saved);
        return toDocumentResponse(saved);
    }

    // Upload document to a project (classic synchronous)
    @Transactional
    public DocumentResponse uploadDocumentToProject(MultipartFile file, String userEmail, Long projectId) throws IOException {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        // Call through proxy so REQUIRES_NEW on saveDocumentRecord is actually applied.
        Document saved = self.saveDocumentRecord(file, userEmail, project);
        triggerRAG(saved);
        return toDocumentResponse(saved);
    }

    /**
     * Step 1: Save document metadata + extracted text to DB.
     * This transaction commits BEFORE RAG runs, so RAG can reload a fresh entity.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Document saveDocumentRecord(MultipartFile file, String userEmail, Project project) throws IOException {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> uploadResult = cloudinaryService.uploadPDF(file);

        Document document = new Document();
        document.setFileName(sanitizeForPostgres(file.getOriginalFilename()));
        document.setFileUrl(sanitizeForPostgres((String) uploadResult.get("secure_url")));
        document.setPublicId(sanitizeForPostgres((String) uploadResult.get("public_id")));
        document.setFileSize(file.getSize());
        document.setUploadedBy(user);
        document.setProject(project);
        document.setUploadedAt(LocalDateTime.now());

        String extractedText = sanitizeForPostgres(pdfExtractionService.extractText(file));
        document.setExtractedText(extractedText);
        document.setAiSummary(sanitizeForPostgres(pdfExtractionService.getSummary(extractedText)));
        document.setPageCount(pdfExtractionService.getPageCount(file));
        document.setProcessingStatus("PROCESSING");

        Document saved = documentRepository.save(document);
        logger.info("✅ Document {} saved ({}chars extracted). Starting RAG...", saved.getId(), extractedText.length());
        return saved;
    }

    /**
     * Step 2: Trigger RAG chunking. Document is already committed in DB.
     * Called after saveDocumentRecord completes its own transaction.
     */
    private void triggerRAG(Document saved) {
        try {
            ragService.processDocumentForRAG(saved);
            // Update status to COMPLETED inside RAG's own transaction
            documentRepository.findById(saved.getId()).ifPresent(doc -> {
                doc.setProcessingStatus("COMPLETED");
                documentRepository.save(doc);
            });
            logger.info("✅ RAG complete for document {}", saved.getId());
        } catch (Exception e) {
            logger.error("❌ RAG failed for document {}: {}", saved.getId(), e.getMessage(), e);
            documentRepository.findById(saved.getId()).ifPresent(doc -> {
                doc.setProcessingStatus("FAILED");
                documentRepository.save(doc);
            });
        }
    }

// Add this method to get documents for a specific project
public List<DocumentResponse> getProjectDocuments(Long projectId, String userEmail) {
    // Verify user exists
    userRepository.findByEmail(userEmail)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // Verify project exists
    projectRepository.findById(projectId)
        .orElseThrow(() -> new RuntimeException("Project not found"));
    

    
    return documentRepository.findByProjectIdOrderByUploadedAtDesc(projectId)
        .stream()
        .map(this::toDocumentResponse)
        .collect(Collectors.toList());
}


    @Transactional
    public void deleteDocument(Long documentId, String userEmail) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!document.getUploadedBy().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized - Only the uploader can delete this document");
        }

        // Delete children first (JPQL deletes bypass first-level cache — must flush after)
        chatMessageRepository.deleteByDocumentId(documentId);
        chatMessageRepository.flush();
        documentChunkRepository.deleteByDocumentId(documentId);
        documentChunkRepository.flush();

        // Now delete the document itself
        documentRepository.deleteById(documentId);
        documentRepository.flush();

        // Delete from Cloudinary (fire-and-forget, don't fail the whole operation)
        try {
            cloudinaryService.deleteFile(document.getPublicId());
            logger.info("✅ Deleted document {} from Cloudinary", documentId);
        } catch (Exception e) {
            logger.warn("⚠️ Could not delete document {} from Cloudinary (may already be gone): {}", documentId, e.getMessage());
        }

        logger.info("✅ Document {} deleted successfully", documentId);
    }

    /**
     * Chat with document using OpenRouter AI
     */
    /**
     * Send message - handles both normal chat and AI chat
     */
    public ChatMessageResponse sendMessage(Long documentId, String userMessage, boolean aiMode, String userEmail) {
        // 1. Get document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // 2. Get user info
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. ALWAYS save user message first
        ChatMessage userChatMessage = new ChatMessage();
        userChatMessage.setDocument(document);
        userChatMessage.setRole("user");
userChatMessage.setSenderEmail(userEmail);
        userChatMessage.setSenderName(user.getFullName());
        userChatMessage.setContent(userMessage);
        userChatMessage.setCreatedAt(LocalDateTime.now());
        ChatMessage savedUserMessage = chatMessageRepository.save(userChatMessage);

        // 4. ONLY call AI if aiMode is TRUE
        if (aiMode) {
            // Check if text is extracted
            if (document.getExtractedText() == null || document.getExtractedText().isEmpty()) {
                // Return a helpful message instead of throwing error
                ChatMessage aiChatMessage = new ChatMessage();
                aiChatMessage.setDocument(document);
                aiChatMessage.setRole("assistant");
                aiChatMessage.setSenderEmail("ai@system");
                aiChatMessage.setContent("⏳ Document text is still being extracted. Please wait a moment and try again.");
                aiChatMessage.setCreatedAt(LocalDateTime.now());
                ChatMessage savedAi = chatMessageRepository.save(aiChatMessage);
                return new ChatMessageResponse(
                        savedAi.getId(),
                        savedAi.getContent(),
                        "ai@system",
                        "AI",
                        "AI",
                        savedAi.getCreatedAt()
                );
            }

            // 🎯 RAG: Retrieve only relevant chunks instead of full document
            String relevantContext = ragService.getRelevantContext(documentId, userMessage);

            // Build context
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("You are an AI assistant helping users understand a PDF document.\n");
            contextBuilder.append("Answer the user's question based ONLY on the relevant document excerpts below.\n");
            contextBuilder.append("If the answer is not in the provided excerpts, say so honestly.\n\n");

            if (relevantContext != null && !relevantContext.isBlank()) {
                // ✅ RAG chunks available — use them (efficient, accurate)
                contextBuilder.append("=== RELEVANT DOCUMENT EXCERPTS ===\n");
                contextBuilder.append(relevantContext);
                contextBuilder.append("\n=== END EXCERPTS ===\n\n");
                logger.info("📚 Using RAG context ({} chars) for document {}", relevantContext.length(), documentId);
            } else {
                // ⚠️ No chunks yet — use first 3000 chars of full text as fallback
                String fullText = document.getExtractedText();
                String truncated = fullText.length() > 3000 ? fullText.substring(0, 3000) + "\n...[truncated]" : fullText;
                contextBuilder.append("=== DOCUMENT CONTENT (partial) ===\n");
                contextBuilder.append(truncated);
                contextBuilder.append("\n=== END CONTENT ===\n\n");
                logger.warn("⚠️ No RAG chunks for document {} — using truncated full text fallback", documentId);
            }

            // Add recent conversation history (last 4 messages only)
            List<ChatMessage> history = chatMessageRepository.findByDocumentOrderByCreatedAtAsc(document);
            int historyStart = Math.max(0, history.size() - 4);
            if (historyStart < history.size() - 1) { // skip if only the current message
                contextBuilder.append("Recent conversation:\n");
                for (int i = historyStart; i < history.size() - 1; i++) {
                    ChatMessage msg = history.get(i);
                    contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
                }
                contextBuilder.append("\n");
            }

            contextBuilder.append("User question: ").append(userMessage);

            // Call AI
            String aiResponse;
            try {
                org.example.group.AIRequest aiRequest = new org.example.group.AIRequest();
                aiRequest.setIssueDescription(contextBuilder.toString());
                org.example.group.AIResponse response = openRouterAiService.generateContent(aiRequest);
                aiResponse = response.getAiSuggestion();
            } catch (Exception e) {
                aiResponse = "Sorry, AI service unavailable. Error: " + e.getMessage();
            }

            // Save AI response
            ChatMessage aiChatMessage = new ChatMessage();
            aiChatMessage.setDocument(document);
            aiChatMessage.setRole("assistant");
            aiChatMessage.setSenderEmail("ai@system");
            aiChatMessage.setContent(aiResponse);
            aiChatMessage.setCreatedAt(LocalDateTime.now());
            ChatMessage savedAiMessage = chatMessageRepository.save(aiChatMessage);

            // Return AI message response
            return new ChatMessageResponse(
                    savedAiMessage.getId(),
                    aiResponse,
                    "ai@system",
                    "AI",
                    "AI",
                    savedAiMessage.getCreatedAt()
            );
        }

        // 5. Normal chat - just return the saved user message (NO AI)
        String userName = user.getFullName() + " " + user.getEmail();
        return new ChatMessageResponse(
                savedUserMessage.getId(),
                userMessage,
                userEmail,
                userName,
                "USER",
                savedUserMessage.getCreatedAt()
        );
    }

    /**
     * Get chat history for a document
     */
    public List<ChatMessageResponse> getChatHistory(Long documentId, String userEmail) {
        // 1. Get document and validate access
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));


        // Removed ownership check to allow collaboration

        // 2. Get chat history
        List<ChatMessage> messages = chatMessageRepository.findByDocumentOrderByCreatedAtAsc(document);

        // 3. Convert to response DTOs
        return messages.stream()
                .map(msg -> new ChatMessageResponse(
                        msg.getId(),
                        msg.getContent(),                           // message
                        msg.getSenderEmail(),   // Use STORED email, not request user
                        msg.getSenderName(),    // Use STORED name
                        msg.getRole().toUpperCase(),                             // messageType (USER/ASSISTANT)
                        msg.getCreatedAt()
                ))
                .toList();
    }
}