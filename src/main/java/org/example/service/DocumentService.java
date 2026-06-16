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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

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
        Document saved = self.saveDocumentRecord(file, userEmail, null);
        publishDocumentEvent(saved.getId(), userEmail, "UPLOADED");
        return toDocumentResponse(saved);
    }

    // Upload document to a project
    @Transactional
    public DocumentResponse uploadDocumentToProject(MultipartFile file, String userEmail, Long projectId) throws IOException {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        Document saved = self.saveDocumentRecord(file, userEmail, project);
        publishDocumentEvent(saved.getId(), userEmail, "UPLOADED");
        return toDocumentResponse(saved);
    }

    /**
     * Trigger re-processing of an existing document's RAG index.
     * Useful after changing chunk parameters or recovering from a FAILED state.
     */
    @Transactional
    public DocumentResponse reprocessDocument(Long documentId, String userEmail) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        document.setProcessingStatus("PROCESSING");
        documentRepository.save(document);
        publishDocumentEvent(documentId, userEmail, "REPROCESS");
        logger.info("🔄 Reprocess event published for document {}", documentId);
        return toDocumentResponse(document);
    }

    /**
     * Trigger asynchronous RAG processing for the document.
     */
    private void publishDocumentEvent(Long documentId, String userEmail, String eventType) {
        logger.info("🔄 Submitting document {} for async processing (type: {})", documentId, eventType);
        executorService.submit(() -> {
            try {
                self.processDocumentBackground(documentId);
            } catch (Exception e) {
                logger.error("❌ Async document processing thread failed for document {}", documentId, e);
            }
        });
    }

    /**
     * Asynchronous RAG processing execution.
     * Generates AI summary and indexes chunks with embeddings.
     */
    @Transactional
    public void processDocumentBackground(Long documentId) {
        try {
            // 1. Generate summary if not present
            documentRepository.findById(documentId).ifPresent(doc -> {
                if (doc.getAiSummary() == null || doc.getAiSummary().isBlank()) {
                    logger.info("🤖 Generating AI summary for document {} in the background...", documentId);
                    String aiSummary = generateAiSummaryOfDocument(doc.getExtractedText(), doc.getFileName());
                    doc.setAiSummary(sanitizeForPostgres(aiSummary));
                    documentRepository.save(doc);
                }
            });

            // 2. Run RAG indexing
            ragService.processDocumentForRAGById(documentId);

            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setProcessingStatus("COMPLETED");
                documentRepository.save(doc);
            });
            logger.info("✅ Async RAG processing complete for document {}", documentId);
        } catch (Exception e) {
            logger.error("❌ Async RAG processing failed for document {}: {}", documentId, e.getMessage(), e);
            documentRepository.findById(documentId).ifPresent(doc -> {
                doc.setProcessingStatus("FAILED");
                documentRepository.save(doc);
            });
        }
    }

    private String generateAiSummaryOfDocument(String extractedText, String fileName) {
        try {
            // Keep the text size reasonable for the summary prompt, e.g. first 40,000 chars
            String truncated = extractedText.length() > 40000 
                    ? extractedText.substring(0, 40000) + "\n...[truncated for summary]" 
                    : extractedText;
            
            String prompt = String.format(
                    "Analyze the following document text (from file '%s') and generate a high-quality summary.\n" +
                    "Include: the main topics covered, the overall structure of the document, the total estimated number of questions or sections (if it contains questions/practice sets), and key details.\n\n" +
                    "=== DOCUMENT TEXT ===\n%s\n=== END DOCUMENT TEXT ===",
                    fileName, truncated
            );
            
            org.example.group.AIRequest request = new org.example.group.AIRequest(prompt);
            org.example.group.AIResponse response = openRouterAiService.generateContent(request);
            return response.getAiSuggestion();
        } catch (Exception e) {
            logger.warn("Could not generate AI summary for doc: {}. Falling back to default summary. Error: {}", fileName, e.getMessage());
            return pdfExtractionService.getSummary(extractedText); // fallback to first 500 chars
        }
    }

    /**
     * Save document metadata + extracted text to DB.
     * This transaction commits BEFORE RAG runs (called via self proxy).
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
        document.setAiSummary(null); // Will be generated in the background thread
        document.setPageCount(pdfExtractionService.getPageCount(file));
        document.setProcessingStatus("PROCESSING");

        Document saved = documentRepository.save(document);
        logger.info("✅ Document {} saved ({} chars extracted). Background thread will generate summary and index RAG.", saved.getId(), extractedText.length());
        return saved;
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

    private String getSystemPromptForIntent(QueryIntent intent) {
        switch (intent) {
            case EXHAUSTIVE_EXTRACTION:
                return "You are an expert exam preparation assistant and document analyst with perfect recall.\n" +
                       "Your goal is to extract EVERY piece of specific information matching the user's request: all numeric values, all frequency bands (GHz/MHz/KHz), all definitions, all lists, all tables, all protocols with their specs, and all comparisons.\n" +
                       "Be completely exhaustive — missing a single value is unacceptable. Format numeric data in clean markdown tables.\n" +
                       "Every fact, value, or frequency you extract MUST be accompanied by its source citation pointing to the page number where it was found (e.g. \"[Page X]\" or \"(Page X)\") based on the block headers.";
            case SUMMARY:
                return "You are a summarization assistant.\n" +
                       "Create a structured, clear, and comprehensive summary of the provided document content.\n" +
                       "Organize the summary into logical sections (e.g. main concepts, architecture, technical specifications, and key takeaways) using Markdown headers, bullet points, and tables.\n" +
                       "Cite the key pages (e.g. \"[Page X]\") when summarizing specific sections based on the block headers.";
            case FLASHCARD_GENERATION:
                return "You are a study aid generator.\n" +
                       "Generate a set of clear, high-quality question-and-answer study flashcards based on the provided document context.\n" +
                       "Each flashcard should test an important concept, term, definition, or numeric specification.\n" +
                       "Format the output as a clean Markdown list of \"Front (Question): ...\" and \"Back (Answer): ...\".\n" +
                       "Include page citations (e.g. \"[Page X]\") on the back of each card to reference the source based on the block headers.";
            case MCQ_GENERATION:
                return "You are an exam generator.\n" +
                       "Create a set of multiple-choice questions (MCQs) based on the provided document context to test the user's knowledge.\n" +
                       "Each question must have exactly 4 options (A, B, C, D) and a clear, correct answer key with a brief explanation.\n" +
                       "Include page citations (e.g. \"[Page X]\") in the explanation to point to the source content based on the block headers.";
            case COMPARISON:
                return "You are an analytical assistant.\n" +
                       "Compare and contrast the technologies, protocols, or concepts requested by the user based on the provided document context.\n" +
                       "Create a structured comparison, highlighting key differences, advantages, disadvantages, and specifications. Use a Markdown table for side-by-side specification comparisons wherever possible.\n" +
                       "Include page citations (e.g. \"[Page X]\") for every claim you make based on the block headers.";
            case FACT_LOOKUP:
            default:
                return "You are a precise fact-lookup assistant.\n" +
                       "Answer the user's question using ONLY the provided document context. Do not make assumptions or extrapolate.\n" +
                       "If the answer is not in the provided excerpts, say so honestly.\n" +
                       "You MUST cite the page numbers of the source context blocks (e.g. \"[Page X]\") for every fact you state based on the block headers.";
        }
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
            QueryIntent intent = ragService.detectIntent(userMessage);

            // Build context
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("You are answering questions based on the provided document context.\n");
            
            // Inject document summary if available to give high-level catalog knowledge
            if (document.getAiSummary() != null && !document.getAiSummary().isBlank()) {
                contextBuilder.append("=== DOCUMENT SUMMARY ===\n");
                contextBuilder.append(document.getAiSummary()).append("\n");
                contextBuilder.append("=== END SUMMARY ===\n\n");
            }

            if (relevantContext != null && !relevantContext.isBlank()) {
                // ✅ RAG chunks available — use them (efficient, accurate)
                contextBuilder.append("=== RELEVANT DOCUMENT EXCERPTS ===\n");
                contextBuilder.append(relevantContext);
                contextBuilder.append("\n=== END EXCERPTS ===\n\n");
                logger.info("📚 Using RAG context ({} chars) for document {}", relevantContext.length(), documentId);
            } else {
                // ⚠️ No chunks yet — use up to 20000 chars of full text as fallback
                String fullText = document.getExtractedText();
                String truncated = fullText.length() > 20000 ? fullText.substring(0, 20000) + "\n...[truncated — document is still being indexed]" : fullText;
                contextBuilder.append("=== DOCUMENT CONTENT (partial) ===\n");
                contextBuilder.append(truncated);
                contextBuilder.append("\n=== END CONTENT ===\n\n");
                logger.warn("⚠️ No RAG chunks for document {} — using up-to-20000 char fallback", documentId);
            }

            // Add recent conversation history (last 8 messages for better continuity)
            List<ChatMessage> history = chatMessageRepository.findByDocumentOrderByCreatedAtAsc(document);
            int historyStart = Math.max(0, history.size() - 8);
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
                String systemPrompt = getSystemPromptForIntent(intent);
                org.example.group.AIResponse response = openRouterAiService.generateContent(contextBuilder.toString(), systemPrompt);
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