package org.example.service;

import org.example.entity.Document;
import org.example.entity.DocumentChunk;
import org.example.repository.DocumentChunkRepository;
import org.example.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) Service.
 *
 * Handles:
 * 1. Chunking documents into smaller pieces
 * 2. Generating & storing embeddings for each chunk
 * 3. Retrieving the most relevant chunks for a user query
 */
@Service
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    private static final int CHUNK_SIZE = 500;      // ~500 chars per chunk
    private static final int CHUNK_OVERLAP = 100;    // 100 char overlap between chunks
    private static final int TOP_K = 4;              // Return top 4 most relevant chunks
    private static final int BATCH_SIZE = 10;        // Batch size for HuggingFace API calls

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentRepository documentRepository;

    /**
     * Process a document: chunk it, generate embeddings, and store them.
     * Accepts documentId so the entity is always fresh-loaded in this new transaction.
     * Runs in its own NEW transaction so failures don't roll back the caller's document save.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentForRAG(Document documentArg) {
        // Reload document in THIS transaction to avoid detached-entity issues
        Document document = documentRepository.findById(documentArg.getId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentArg.getId()));

        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            logger.warn("No extracted text for document {}, skipping RAG processing", document.getId());
            return;
        }

        // Delete old chunks if re-processing
        documentChunkRepository.deleteByDocumentId(document.getId());
        documentChunkRepository.flush();

        // 1. Split text into chunks
        List<String> chunks = chunkText(extractedText);
        logger.info("Document {} split into {} chunks (text length: {})", document.getId(), chunks.size(), extractedText.length());

        // 2. Generate embeddings in batches and save chunks
        List<DocumentChunk> chunkEntities = new ArrayList<>();

        for (int batchStart = 0; batchStart < chunks.size(); batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, chunks.size());
            List<String> batchTexts = chunks.subList(batchStart, batchEnd);

            try {
                List<float[]> batchEmbeddings = embeddingService.getEmbeddingsBatch(batchTexts);

                for (int i = 0; i < batchTexts.size(); i++) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocument(document);
                    chunk.setChunkIndex(batchStart + i);
                    chunk.setChunkText(batchTexts.get(i));
                    chunk.setEmbedding(embeddingService.serializeEmbedding(batchEmbeddings.get(i)));
                    chunkEntities.add(chunk);
                }

                logger.debug("✅ Processed batch {}-{} for document {}", batchStart, batchEnd - 1, document.getId());

            } catch (Exception e) {
                logger.error("⚠️ Embedding failed for batch {}-{} of doc {} — saving chunks WITHOUT embeddings: {}",
                        batchStart, batchEnd - 1, document.getId(), e.getMessage());
                // Save chunks without embeddings — keyword fallback will still work
                for (int i = 0; i < batchTexts.size(); i++) {
                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setDocument(document);
                    chunk.setChunkIndex(batchStart + i);
                    chunk.setChunkText(batchTexts.get(i));
                    chunk.setEmbedding(null);
                    chunkEntities.add(chunk);
                }
            }
        }

        // 3. Save all chunks in one go
        documentChunkRepository.saveAll(chunkEntities);
        logger.info("✅ Saved {} chunks for document {}", chunkEntities.size(), document.getId());
    }

    /**
     * Retrieve the most relevant chunks for a user query using cosine similarity.
     * Falls back to keyword matching if embeddings are not available.
     */
    public String getRelevantContext(Long documentId, String userQuery) {
        List<DocumentChunk> allChunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);

        if (allChunks.isEmpty()) {
            logger.warn("No chunks found for document {}", documentId);
            return null; // Will fall back to full text in DocumentService
        }

        // Check if embeddings exist
        boolean hasEmbeddings = allChunks.stream()
                .anyMatch(c -> c.getEmbedding() != null && !c.getEmbedding().isBlank());

        List<String> topChunks;

        if (hasEmbeddings) {
            try {
                topChunks = retrieveByEmbedding(allChunks, userQuery);
                logger.info("Retrieved {} relevant chunks via embeddings for document {}", topChunks.size(), documentId);
            } catch (Exception e) {
                // If embedding fails (HuggingFace down), fall back to keyword search
                logger.warn("Embedding search failed, falling back to keyword search: {}", e.getMessage());
                topChunks = retrieveByKeyword(allChunks, userQuery);
            }
        } else {
            topChunks = retrieveByKeyword(allChunks, userQuery);
            logger.info("Retrieved {} relevant chunks via keyword fallback for document {}", topChunks.size(), documentId);
        }

        // Join top chunks into context
        return String.join("\n\n---\n\n", topChunks);
    }

    /**
     * Vector similarity search - core RAG retrieval.
     */
    private List<String> retrieveByEmbedding(List<DocumentChunk> chunks, String query) {
        // 1. Get query embedding
        float[] queryEmbedding = embeddingService.getEmbedding(query);

        // 2. Calculate similarity for each chunk
        List<Map.Entry<DocumentChunk, Double>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) continue;

            float[] chunkEmbedding = embeddingService.deserializeEmbedding(chunk.getEmbedding());
            double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
            scored.add(Map.entry(chunk, similarity));
        }

        // 3. Sort by similarity (highest first) and take top K
        return scored.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(TOP_K)
                .map(entry -> entry.getKey().getChunkText())
                .collect(Collectors.toList());
    }

    /**
     * Keyword-based fallback when embeddings are unavailable.
     */
    private List<String> retrieveByKeyword(List<DocumentChunk> chunks, String query) {
        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("\\s+");

        // Score chunks by number of matching query words
        List<Map.Entry<DocumentChunk, Integer>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String chunkLower = chunk.getChunkText().toLowerCase();
            int matchCount = 0;
            for (String word : queryWords) {
                if (word.length() > 2 && chunkLower.contains(word)) {
                    matchCount++;
                }
            }
            scored.add(Map.entry(chunk, matchCount));
        }

        return scored.stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(TOP_K)
                .map(entry -> entry.getKey().getChunkText())
                .collect(Collectors.toList());
    }
//
    /**
     * Split text into overlapping chunks for better context preservation.
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= CHUNK_SIZE) {
            chunks.add(text.trim());
            return chunks;
        }

        // Split by paragraphs first for natural boundaries
        String[] paragraphs = text.split("\n\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            String trimmedPara = para.trim();
            if (trimmedPara.isEmpty()) continue;

            // If adding this paragraph would exceed chunk size
            if (currentChunk.length() + trimmedPara.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());

                // Start new chunk with overlap from end of previous chunk
                String prev = currentChunk.toString();
                currentChunk = new StringBuilder();
                if (prev.length() > CHUNK_OVERLAP) {
                    currentChunk.append(prev.substring(prev.length() - CHUNK_OVERLAP));
                    currentChunk.append(" ");
                }
            }

            // If a single paragraph is too long, split it by sentences
            if (trimmedPara.length() > CHUNK_SIZE) {
                String[] sentences = trimmedPara.split("(?<=[.!?])\\s+");
                for (String sentence : sentences) {
                    if (currentChunk.length() + sentence.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString().trim());
                        String prev = currentChunk.toString();
                        currentChunk = new StringBuilder();
                        if (prev.length() > CHUNK_OVERLAP) {
                            currentChunk.append(prev.substring(prev.length() - CHUNK_OVERLAP));
                            currentChunk.append(" ");
                        }
                    }
                    currentChunk.append(sentence).append(" ");
                }
            } else {
                currentChunk.append(trimmedPara).append("\n\n");
            }
        }

        // Don't forget the last chunk
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }
}
