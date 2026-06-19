package org.example.service;

import org.example.entity.Document;
import org.example.entity.DocumentChunk;
import org.example.repository.DocumentChunkRepository;
import org.example.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.text.BreakIterator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval Augmented Generation) Service.
 *
 * Handles:
 * 1. Chunking documents into semantically meaningful pieces
 * 2. Generating & storing embeddings for each chunk (HuggingFace MiniLM)
 * 3. Storing vectors in pgvector column for fast ANN retrieval
 * 4. Retrieving the most relevant chunks for a user query
 * - Primary: pgvector cosine similarity (O(log n) with IVFFlat index)
 * - Fallback: in-memory cosine similarity (when pgvector not available)
 * - Last resort: keyword BM25-style matching
 */
@Service
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    // Larger chunks = more context, fewer cross-boundary splits
    private static final int CHUNK_SIZE = 1200; // was 500
    private static final int CHUNK_OVERLAP = 200; // was 100
    private static final int TOP_K_DEFAULT = 10; // was 8
    private static final int TOP_K_BROAD = 30; // was 24
    private static final int BATCH_SIZE = 10; // batch size for HuggingFace API calls

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initBackfill() {
        logger.info("Initializing startup pgvector backfiller...");
        new Thread(() -> {
            try {
                // Wait a few seconds for the database pool to warm up
                Thread.sleep(5000);
                performStartupBackfill();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Startup pgvector backfill failed", e);
            }
        }, "pgvector-backfill-thread").start();
    }

    private void performStartupBackfill() {
        if (jdbcTemplate == null) {
            logger.info("JdbcTemplate not available — skipping startup pgvector backfill");
            return;
        }

        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM document_chunks WHERE embedding IS NOT NULL AND embedding_vector IS NULL",
                    Integer.class);

            if (count == null || count == 0) {
                logger.info("✅ Startup pgvector backfill: all chunks are up to date.");
                return;
            }

            logger.info("⏳ Startup pgvector backfill: found {} chunks needing pgvector backfill.", count);

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, embedding FROM document_chunks WHERE embedding IS NOT NULL AND embedding_vector IS NULL");

            int updatedCount = 0;
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                String jsonEmbed = (String) row.get("embedding");

                try {
                    float[] vec = embeddingService.deserializeEmbedding(jsonEmbed);
                    String pgVecLiteral = toPgVectorLiteral(vec);

                    jdbcTemplate.update(
                            "UPDATE document_chunks SET embedding_vector = ?::vector WHERE id = ?",
                            pgVecLiteral, id);
                    updatedCount++;
                } catch (Exception ex) {
                    logger.warn("⚠️ Failed to backfill pgvector for chunk ID {}: {}", id, ex.getMessage());
                }
            }

            logger.info("✅ Startup pgvector backfill complete! Updated {} chunks.", updatedCount);

        } catch (Exception e) {
            logger.warn("⚠️ Startup pgvector backfill failed (extension or column may not be ready yet): {}",
                    e.getMessage());
        }
    }

    // ─────────────────────────── INDEXING ───────────────────────────

    /**
     * Entry point called by Kafka consumer — loads document by ID.
     * Runs in its own NEW transaction to avoid detached-entity issues.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentForRAGById(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        processDocumentInternal(document);
    }

    /**
     * Legacy entry point — kept for backwards compatibility with sync fallback.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDocumentForRAG(Document documentArg) {
        Document document = documentRepository.findById(documentArg.getId())
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentArg.getId()));
        processDocumentInternal(document);
    }

    private void processDocumentInternal(Document document) {
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            logger.warn("No extracted text for document {}, skipping RAG processing", document.getId());
            return;
        }

        // Delete old chunks if re-processing
        documentChunkRepository.deleteByDocumentId(document.getId());
        documentChunkRepository.flush();

        // 1. Split text into overlapping chunks
        List<String> chunks = chunkText(extractedText, document.getFileName());
        logger.info("Document {} split into {} chunks (text length: {}, chunk_size: {})",
                document.getId(), chunks.size(), extractedText.length(), CHUNK_SIZE);

        // 2. Generate embeddings in batches and build chunk entities
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
                    // Store JSON embedding for in-memory fallback
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

        // 3. Save all chunks
        documentChunkRepository.saveAll(chunkEntities);
        logger.info("✅ Saved {} chunks for document {}", chunkEntities.size(), document.getId());

        // 4. Back-fill pgvector column for fast retrieval (best-effort)
        backfillPgvector(document.getId(), chunkEntities);
    }

    /**
     * After saving chunks, update the embedding_vector column via JDBC native SQL.
     * pgvector requires casting float arrays to the vector type which JPQL cannot
     * do.
     */
    private void backfillPgvector(Long documentId, List<DocumentChunk> chunks) {
        if (jdbcTemplate == null) {
            logger.debug("JdbcTemplate not available — skipping pgvector backfill");
            return;
        }
        try {
            int updated = 0;
            for (DocumentChunk chunk : chunks) {
                if (chunk.getEmbedding() == null || chunk.getId() == null)
                    continue;
                float[] vec = embeddingService.deserializeEmbedding(chunk.getEmbedding());
                String pgVecLiteral = toPgVectorLiteral(vec);
                jdbcTemplate.update(
                        "UPDATE document_chunks SET embedding_vector = ?::vector WHERE id = ?",
                        pgVecLiteral, chunk.getId());
                updated++;
            }
            logger.info("✅ pgvector backfill: updated {} chunks for document {}", updated, documentId);
        } catch (Exception e) {
            // pgvector may not be enabled — non-fatal, in-memory fallback will work
            logger.warn("⚠️ pgvector backfill failed for document {} (extension may not be installed): {}",
                    documentId, e.getMessage());
        }
    }

    // ─────────────────────────── RETRIEVAL ───────────────────────────

    /**
     * Retrieve the most relevant chunks for a user query.
     *
     * Strategy:
     * 1. Short-document optimization: If the document is small, return full text
     * directly to guarantee 100% coverage.
     * 2. Hybrid Search: Query dense vector search and sparse keyword search in
     * parallel.
     * 3. Reciprocal Rank Fusion (RRF): Merge rankings to combine semantic meaning
     * and exact term matching.
     * 4. Neighbor Expansion: Retrieve preceding/succeeding neighboring chunks to
     * prevent context cuts.
     * 5. Contiguous Chunk Merging: Merge adjacent chunks into unified blocks to
     * keep sentences intact.
     */
    public String getRelevantContext(Long documentId, String userQuery) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            logger.warn("No extracted text for document {}", documentId);
            return null;
        }

        // Optimization: For short documents, return the full text for 100% complete
        // coverage
        if (extractedText.length() <= 60000) {
            logger.info("📄 Document {} is short ({} chars) — using full text for 100% coverage",
                    documentId, extractedText.length());
            return "[Document: " + document.getFileName() + " - Full Text]\n" + extractedText;
        }

        List<DocumentChunk> allChunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        if (allChunks.isEmpty()) {
            logger.warn("No chunks found for document {}", documentId);
            return null;
        }

        QueryIntent intent = detectIntent(userQuery);
        int topK = resolveTopK(userQuery, allChunks.size());

        // Perform advanced Hybrid Search (Vector + Keyword) with Reciprocal Rank Fusion
        // Retrieve slightly larger pool (3x topK) for candidate selection
        int candidatePoolSize = Math.min(topK * 3, allChunks.size());
        List<DocumentChunk> candidateChunks = performHybridSearch(documentId, allChunks, userQuery, candidatePoolSize, intent);

        // Apply lightweight heuristic reranker
        List<DocumentChunk> topChunks = rerankChunks(candidateChunks, userQuery, intent);
        if (topChunks.size() > topK) {
            topChunks = topChunks.subList(0, topK);
        }
        logger.info("🚀 Retrieved {} chunks via Hybrid Search (RRF) & Reranking for document {}", topChunks.size(), documentId);

        // Expand neighboring context & merge adjacent chunks
        return buildExpandedMergedContext(topChunks, allChunks, document.getFileName());
    }

    /**
     * pgvector nearest-neighbor search — runs entirely in Postgres.
     */
    private List<DocumentChunk> retrieveChunksByPgvector(Long documentId, String query, int limit, QueryIntent intent) {
        if (jdbcTemplate == null)
            return Collections.emptyList();

        float[] queryVec = embeddingService.getEmbedding(query);
        String pgVecLiteral = toPgVectorLiteral(queryVec);

        // For direct lookups, use a higher threshold. For broad queries, keep it relaxed so we don't miss context.
        double minSimilarity = (intent == QueryIntent.FACT_LOOKUP) ? 0.35 : 0.15;
        double maxDistance = 1.0 - minSimilarity;

        try {
            List<Long> chunkIds = jdbcTemplate.queryForList(
                    "SELECT id FROM document_chunks " +
                            "WHERE document_id = ? AND embedding_vector IS NOT NULL " +
                            "AND (embedding_vector <=> ?::vector) <= ? " +
                            "ORDER BY embedding_vector <=> ?::vector " +
                            "LIMIT ?",
                    Long.class,
                    documentId, pgVecLiteral, maxDistance, pgVecLiteral, limit);

            if (chunkIds.isEmpty())
                return Collections.emptyList();

            List<DocumentChunk> fetched = documentChunkRepository.findAllById(chunkIds);

            // Restore pgvector similarity order since findAllById does not preserve order
            Map<Long, DocumentChunk> chunkMap = fetched.stream()
                    .collect(Collectors.toMap(DocumentChunk::getId, c -> c));

            List<DocumentChunk> ordered = new ArrayList<>();
            for (Long id : chunkIds) {
                if (chunkMap.containsKey(id)) {
                    ordered.add(chunkMap.get(id));
                }
            }
            return ordered;
        } catch (Exception e) {
            throw new RuntimeException("pgvector query failed: " + e.getMessage(), e);
        }
    }

    /**
     * In-memory cosine similarity search as vector search fallback.
     */
    private List<DocumentChunk> scoreAndRankChunksByEmbedding(List<DocumentChunk> chunks, String query, int limit, QueryIntent intent) {
        float[] queryEmbedding = embeddingService.getEmbedding(query);
        List<Map.Entry<DocumentChunk, Double>> scored = new ArrayList<>();
        double minSimilarity = (intent == QueryIntent.FACT_LOOKUP) ? 0.35 : 0.15;

        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank())
                continue;
            float[] chunkEmbedding = embeddingService.deserializeEmbedding(chunk.getEmbedding());
            double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);
            if (similarity >= minSimilarity) {
                scored.add(Map.entry(chunk, similarity));
            }
        }

        return scored.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Sparse keyword matching ranker.
     */
    private List<DocumentChunk> scoreAndRankChunksByKeyword(List<DocumentChunk> chunks, String query, int limit) {
        String queryLower = query.toLowerCase();
        String[] queryWords = queryLower.split("\\s+");

        List<Map.Entry<DocumentChunk, Integer>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String chunkLower = chunk.getChunkText().toLowerCase();
            int matchCount = 0;
            for (String word : queryWords) {
                if (word.length() > 2 && chunkLower.contains(word)) {
                    // Weight match count by word length to prioritize specific terms
                    matchCount += word.length();
                }
            }
            scored.add(Map.entry(chunk, matchCount));
        }

        return scored.stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Perform Hybrid Search by combining Vector and Keyword search results via
     * Reciprocal Rank Fusion (RRF).
     */
    private List<DocumentChunk> performHybridSearch(Long documentId, List<DocumentChunk> allChunks, String query,
            int topK, QueryIntent intent) {
        // 1. Get Vector Search results (retrieve 2x topK for better fusion pool)
        List<DocumentChunk> vectorResults;
        try {
            vectorResults = retrieveChunksByPgvector(documentId, query, topK * 2, intent);
        } catch (Exception e) {
            logger.debug("pgvector failed, falling back to in-memory: {}", e.getMessage());
            vectorResults = scoreAndRankChunksByEmbedding(allChunks, query, topK * 2, intent);
        }

        // 2. Get Keyword Search results
        List<DocumentChunk> keywordResults = scoreAndRankChunksByKeyword(allChunks, query, topK * 2);

        // 3. Apply Reciprocal Rank Fusion (RRF)
        double rrfConstant = 60.0;
        Map<Long, Double> rrfScores = new HashMap<>();
        Map<Long, DocumentChunk> chunkMap = new HashMap<>();

        for (DocumentChunk c : allChunks) {
            chunkMap.put(c.getId(), c);
        }

        // Score vector results
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            DocumentChunk chunk = vectorResults.get(rank);
            double score = 1.0 / (rrfConstant + (rank + 1));
            rrfScores.put(chunk.getId(), score);
        }

        // Score keyword results and merge
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            DocumentChunk chunk = keywordResults.get(rank);
            double score = 1.0 / (rrfConstant + (rank + 1));
            rrfScores.merge(chunk.getId(), score, Double::sum);
        }

        // Sort by RRF score descending
        return rrfScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(entry -> chunkMap.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<DocumentChunk> rerankChunks(List<DocumentChunk> chunks, String query, QueryIntent intent) {
        if (chunks == null || chunks.isEmpty()) return chunks;

        String queryLower = query.toLowerCase(Locale.ROOT);
        String[] queryWords = queryLower.split("\\s+");
        List<String> importantWords = Arrays.stream(queryWords)
                .filter(w -> w.length() > 2)
                .collect(Collectors.toList());

        List<Map.Entry<DocumentChunk, Double>> scored = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            String textLower = chunk.getChunkText().toLowerCase(Locale.ROOT);
            double score = 0.0;

            // 1. Phrase matching: check if consecutive words from query appear in chunk
            for (int i = 0; i < queryWords.length - 1; i++) {
                String bigram = queryWords[i] + " " + queryWords[i+1];
                if (textLower.contains(bigram)) {
                    score += 15.0; // strong boost for consecutive matching terms
                }
            }

            // 2. Exact word occurrences
            int wordMatches = 0;
            for (String word : importantWords) {
                if (textLower.contains(word)) {
                    wordMatches++;
                    score += 5.0;
                    
                    // Count frequency of word
                    int index = 0;
                    while ((index = textLower.indexOf(word, index)) != -1) {
                        score += 1.0;
                        index += word.length();
                    }
                }
            }

            // Keyword density bonus
            if (!importantWords.isEmpty()) {
                double density = (double) wordMatches / importantWords.size();
                score += density * 10.0;
            }

            // 3. Intent-based content matching
            if (intent == QueryIntent.EXHAUSTIVE_EXTRACTION) {
                // Reward chunks with numeric tables, frequency units, or question marks
                if (textLower.contains("ghz") || textLower.contains("mhz") || textLower.contains("khz") || textLower.contains("hz")) {
                    score += 20.0;
                }
                if (textLower.contains("ans:") || textLower.contains("answer:")) {
                    score += 15.0;
                }
                if (textLower.contains("|") || textLower.contains("---") || textLower.contains("table")) {
                    score += 15.0;
                }
            } else if (intent == QueryIntent.MCQ_GENERATION || intent == QueryIntent.FLASHCARD_GENERATION) {
                if (textLower.contains("ans:") || textLower.contains("answer:") || textLower.contains("?") || textLower.contains("correct answer")) {
                    score += 20.0;
                }
            }

            scored.add(Map.entry(chunk, score));
        }

        // Sort by score descending
        return scored.stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Expand retrieved chunks to include neighbors and merge contiguous chunks.
     */
    private String buildExpandedMergedContext(List<DocumentChunk> retrievedChunks, List<DocumentChunk> allChunks,
            String fileName) {
        if (retrievedChunks.isEmpty())
            return null;

        // Create a map of index -> chunk for fast lookup
        Map<Integer, DocumentChunk> indexToChunkMap = allChunks.stream()
                .collect(Collectors.toMap(DocumentChunk::getChunkIndex, c -> c));

        // Collect all indices to include (retrieved + neighbors)
        Set<Integer> targetIndices = new HashSet<>();
        for (DocumentChunk rc : retrievedChunks) {
            int idx = rc.getChunkIndex();
            targetIndices.add(idx);
            // Add preceding chunk if exists
            if (idx > 0)
                targetIndices.add(idx - 1);
            // Add succeeding chunk if exists
            if (idx < allChunks.size() - 1)
                targetIndices.add(idx + 1);
        }

        // Sort the indices
        List<Integer> sortedIndices = new ArrayList<>(targetIndices);
        Collections.sort(sortedIndices);

        // Group contiguous indices
        List<List<Integer>> groups = new ArrayList<>();
        if (!sortedIndices.isEmpty()) {
            List<Integer> currentGroup = new ArrayList<>();
            currentGroup.add(sortedIndices.get(0));
            groups.add(currentGroup);

            for (int i = 1; i < sortedIndices.size(); i++) {
                int prev = sortedIndices.get(i - 1);
                int curr = sortedIndices.get(i);
                if (curr == prev + 1) {
                    currentGroup.add(curr);
                } else {
                    currentGroup = new ArrayList<>();
                    currentGroup.add(curr);
                    groups.add(currentGroup);
                }
            }
        }

        // Build context blocks, keeping track of character count
        StringBuilder sb = new StringBuilder();
        int totalChars = 0;
        int maxContextChars = 64000; // ~16000 tokens of context, easily supported by Llama 3.1 on Groq

        int blockNum = 1;
        for (List<Integer> group : groups) {
            int startPage = Integer.MAX_VALUE;
            int endPage = Integer.MIN_VALUE;
            StringBuilder groupText = new StringBuilder();

            for (int idx : group) {
                DocumentChunk c = indexToChunkMap.get(idx);
                if (c != null) {
                    String cleanText = stripHeader(c.getChunkText());
                    groupText.append(cleanText).append("\n");

                    int[] pages = parsePagesFromChunk(c.getChunkText());
                    if (pages != null) {
                        startPage = Math.min(startPage, pages[0]);
                        endPage = Math.max(endPage, pages[1]);
                    }
                }
            }

            String pageRangeStr = (startPage != Integer.MAX_VALUE && endPage != Integer.MIN_VALUE)
                    ? (startPage == endPage ? "Page " + startPage : "Pages " + startPage + "-" + endPage)
                    : "Unknown Page";

            String blockHeader = String.format("[Context Block %d - Document: %s, %s]%n", blockNum++, fileName,
                    pageRangeStr);
            String blockContent = groupText.toString().trim();
            String fullBlock = blockHeader + blockContent + "\n\n---\n\n";

            if (totalChars + fullBlock.length() > maxContextChars) {
                break;
            }

            sb.append(fullBlock);
            totalChars += fullBlock.length();
        }

        String result = sb.toString();
        if (result.endsWith("\n\n---\n\n")) {
            result = result.substring(0, result.length() - 9);
        }
        return result;
    }

    private String stripHeader(String chunkText) {
        if (chunkText == null)
            return "";
        int headerEnd = chunkText.indexOf("]\n");
        if (headerEnd != -1 && chunkText.startsWith("[")) {
            return chunkText.substring(headerEnd + 2);
        }
        return chunkText;
    }

    private int[] parsePagesFromChunk(String chunkText) {
        if (chunkText == null || !chunkText.startsWith("["))
            return null;
        int headerEnd = chunkText.indexOf("]\n");
        if (headerEnd == -1)
            return null;
        String header = chunkText.substring(1, headerEnd);
        int pagesIdx = header.indexOf("Pages: ");
        if (pagesIdx != -1) {
            String pagesStr = header.substring(pagesIdx + 7).trim();
            if (pagesStr.contains("-")) {
                String[] parts = pagesStr.split("-");
                try {
                    return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
                } catch (NumberFormatException e) {
                    // ignore
                }
            } else {
                try {
                    int page = Integer.parseInt(pagesStr);
                    return new int[] { page, page };
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return null;
    }

    public QueryIntent detectIntent(String query) {
        if (query == null || query.isBlank()) {
            return QueryIntent.FACT_LOOKUP;
        }
        String q = query.toLowerCase(Locale.ROOT);

        if (q.contains("summarize") || q.contains("summary") || q.contains("overview") || q.contains("tl;dr")
                || q.contains("abstract") || q.contains("brief")) {
            return QueryIntent.SUMMARY;
        }
        if (q.contains("flashcard") || q.contains("flash card") || q.contains("study card")) {
            return QueryIntent.FLASHCARD_GENERATION;
        }
        if (q.contains("mcq") || q.contains("multiple choice") || q.contains("multiple-choice") || q.contains("quiz")
                || q.contains("test question") || q.contains("exam question") || q.contains("practice question")) {
            return QueryIntent.MCQ_GENERATION;
        }
        if (q.contains("compare") || q.contains("difference between") || q.contains("versus") || q.contains("vs")
                || q.contains("comparison") || q.contains("distinguish")) {
            return QueryIntent.COMPARISON;
        }
        if (q.contains("all") || q.contains("list all") || q.contains("frequencies") || q.contains("frequency")
                || q.contains("formula") || q.contains("formulas") || q.contains("value") || q.contains("values")
                || q.contains("every") || q.contains("complete list") || q.contains("table") || q.contains("tables")
                || q.contains("numeric") || q.contains("number") || q.contains("numbers") || q.contains("ghz")
                || q.contains("mhz") || q.contains("khz") || q.contains("range") || q.contains("band")
                || q.contains("extract") || q.contains("exam") || q.contains("topics") || q.contains("important")
                || q.contains("key") || q.contains("define") || q.contains("definition") || q.contains("tell")
                || q.contains("share")) {
            return QueryIntent.EXHAUSTIVE_EXTRACTION;
        }
        return QueryIntent.FACT_LOOKUP;
    }

    private int resolveTopK(String query, int chunkCount) {
        QueryIntent intent = detectIntent(query);
        int desired;
        switch (intent) {
            case FACT_LOOKUP:
                desired = TOP_K_DEFAULT;
                break;
            case SUMMARY:
            case EXHAUSTIVE_EXTRACTION:
            case FLASHCARD_GENERATION:
            case MCQ_GENERATION:
            case COMPARISON:
            default:
                desired = 50; // Retrieve up to 50 chunks for comprehensive analysis
                break;
        }
        return Math.max(1, Math.min(desired, chunkCount));
    }

    /**
     * Convert float[] to pgvector literal format: "[0.1,0.2,...]"
     */
    private String toPgVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; i++) {
            sb.append(vec[i]);
            if (i < vec.length - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static class BlockWithPage {
        final String text;
        final int pageNumber;

        BlockWithPage(String text, int pageNumber) {
            this.text = text;
            this.pageNumber = pageNumber;
        }
    }

    /**
     * Split text into overlapping chunks using logical block-aware and page-aware
     * boundaries to keep questions, lists, and tables intact.
     * Prepend document metadata header to each chunk.
     */
    private List<String> chunkText(String text, String fileName) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // 1. Parse into page-aware logical blocks
        List<BlockWithPage> allBlocks = new ArrayList<>();
        Pattern pagePattern = Pattern.compile("\\[Page (\\d+)\\]");

        // Split text by page markers to process each page individually
        String[] segments = text.split("\\[Page \\d+\\]");
        Matcher matcher = pagePattern.matcher(text);
        List<Integer> pageNumbers = new ArrayList<>();
        while (matcher.find()) {
            pageNumbers.add(Integer.parseInt(matcher.group(1)));
        }

        int segmentIdx = 0;
        for (String segment : segments) {
            if (segmentIdx == 0 && segment.trim().isEmpty() && segments.length > 1) {
                // If text started with [Page 1], first segment before it is empty
                segmentIdx++;
                continue;
            }

            // Map segment to page number
            int pageNum = 1;
            if (!pageNumbers.isEmpty()) {
                int pageListIdx = Math.max(0, segmentIdx - 1);
                if (pageListIdx < pageNumbers.size()) {
                    pageNum = pageNumbers.get(pageListIdx);
                } else {
                    pageNum = pageNumbers.get(pageNumbers.size() - 1);
                }
            }

            List<String> pageBlocks = splitIntoLogicalBlocks(segment);
            for (String block : pageBlocks) {
                allBlocks.add(new BlockWithPage(block, pageNum));
            }
            segmentIdx++;
        }

        if (allBlocks.isEmpty()) {
            // Fallback if no blocks extracted
            chunks.add("[Document: " + fileName + ", Page: 1]\n" + text.trim());
            return chunks;
        }

        // 2. Chunk blocks using a sliding window
        int i = 0;

        while (i < allBlocks.size()) {
            List<BlockWithPage> chunkBlocks = new ArrayList<>();
            int currentLength = 0;

            // Collect blocks until we exceed CHUNK_SIZE
            while (i < allBlocks.size() && currentLength < CHUNK_SIZE) {
                BlockWithPage b = allBlocks.get(i);
                chunkBlocks.add(b);
                currentLength += b.text.length() + 2; // +2 for newlines
                i++;
            }

            int startPage = chunkBlocks.get(0).pageNumber;
            int endPage = chunkBlocks.get(chunkBlocks.size() - 1).pageNumber;

            String rawText = chunkBlocks.stream()
                    .map(b -> b.text)
                    .collect(Collectors.joining("\n\n"));

            String formattedText = String.format("[Document: %s, Pages: %d-%d]%n%s",
                    fileName, startPage, endPage, rawText);
            chunks.add(formattedText);

            // Slide window: backtrack up to 2 blocks or until overlap is satisfied
            if (i < allBlocks.size()) {
                int overlapChars = 0;
                int backtrackCount = 0;
                while (i > 0 && backtrackCount < 2 && overlapChars < CHUNK_OVERLAP) {
                    i--;
                    overlapChars += allBlocks.get(i).text.length();
                    backtrackCount++;
                }
            }
        }

        return chunks;
    }

    private List<String> splitIntoLogicalBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        StringBuilder currentBlock = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (currentBlock.length() > 0) {
                    blocks.add(currentBlock.toString().trim());
                    currentBlock.setLength(0);
                }
                continue;
            }

            // Check if this line starts a new question or structured item (e.g. "1.", "Q2.", "Question 3:")
            boolean isNewQuestionStart = trimmed.matches("^\\s*(\\d+|Q\\d+|Question\\s*\\d+)[\\.\\)\\:]\\s+.*");
            
            // Check if this line is part of an ongoing list, option, or answer (e.g. "a.", "Ans:", "- ", "* ")
            // We do NOT want to split options or answer keys away from their question!
            boolean isListItemOrOption = trimmed.matches("^\\s*([a-eA-E]|[\\-\\u2022\\*]|Ans|Answer)[\\.\\)\\:\\-]\\s+.*");

            // Split block if we hit a new question, or if it's normal text and block has reached significant size
            boolean isNormalParagraphSplit = currentBlock.length() > 1000 && !isListItemOrOption;

            if ((isNewQuestionStart || isNormalParagraphSplit) && currentBlock.length() > 0) {
                blocks.add(currentBlock.toString().trim());
                currentBlock.setLength(0);
            }

            if (currentBlock.length() > 0) {
                currentBlock.append("\n").append(line);
            } else {
                currentBlock.append(line);
            }
        }

        if (currentBlock.length() > 0) {
            blocks.add(currentBlock.toString().trim());
        }

        return blocks;
    }
}
