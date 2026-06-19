package org.example.service;

import org.example.entity.Document;
import org.example.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RAGEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(RAGEvaluationService.class);

    @Autowired
    private RAGService ragService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentService documentService;

    public static class TestCase {
        public final String query;
        public final List<String> expectedKeywords;
        public final List<Integer> expectedPages;

        public TestCase(String query, List<String> expectedKeywords, List<Integer> expectedPages) {
            this.query = query;
            this.expectedKeywords = expectedKeywords;
            this.expectedPages = expectedPages;
        }
    }

    public static class EvalResult {
        public String query;
        public boolean pageMatched;
        public double keywordScore;
        public List<Integer> retrievedPages;
        public List<Integer> expectedPages;
        public String responseSnippet;
        public long durationMs;
    }

    public static class EvalReport {
        public double overallPageAccuracy;
        public double averageKeywordRecall;
        public int totalTests;
        public int passedTests;
        public List<EvalResult> results;
    }

    private final List<TestCase> testSuite = List.of(
            new TestCase("What topology offers high fault tolerance and redundancy?", 
                    List.of("mesh", "redundancy", "tolerance"), List.of(20)),
            new TestCase("In which topology do all devices share the same communication medium?", 
                    List.of("bus", "medium", "share"), List.of(20)),
            new TestCase("The presentation layer lies between which layers?", 
                    List.of("presentation", "session", "application"), List.of(19)),
            new TestCase("What is a collision domain in networking?", 
                    List.of("collision", "domain", "segment"), List.of(20)),
            new TestCase("list down last 10 ques with solns in detail", 
                    List.of("topology", "star", "bus", "mesh"), List.of(20, 21))
    );

    public EvalReport evaluateDocumentRAG(Long documentId, String userEmail) {
        documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        List<EvalResult> results = new ArrayList<>();
        int passedCount = 0;

        for (TestCase tc : testSuite) {
            long start = System.currentTimeMillis();
            EvalResult res = new EvalResult();
            res.query = tc.query;
            res.expectedPages = tc.expectedPages;

            try {
                // 1. Run RAG context retrieval
                String context = ragService.getRelevantContext(documentId, tc.query);
                
                // Parse page numbers retrieved from context block headers
                List<Integer> retrieved = extractPagesFromContext(context);
                res.retrievedPages = retrieved;

                // Check if any expected page matches retrieved pages
                boolean pageMatch = tc.expectedPages.stream().anyMatch(retrieved::contains);
                res.pageMatched = pageMatch;

                // 2. Call AI service to get response and evaluate keyword recall
                org.example.group.ChatMessageResponse aiRes = documentService.sendMessage(documentId, tc.query, true, userEmail);
                String responseText = aiRes.getMessage().toLowerCase(Locale.ROOT);
                res.responseSnippet = aiRes.getMessage().length() > 200 
                        ? aiRes.getMessage().substring(0, 200) + "..." 
                        : aiRes.getMessage();

                int keywordMatches = 0;
                for (String kw : tc.expectedKeywords) {
                    if (responseText.contains(kw.toLowerCase(Locale.ROOT))) {
                        keywordMatches++;
                    }
                }
                res.keywordScore = tc.expectedKeywords.isEmpty() ? 1.0 : (double) keywordMatches / tc.expectedKeywords.size();

                if (pageMatch && res.keywordScore >= 0.5) {
                    passedCount++;
                }

            } catch (Exception e) {
                logger.error("Error evaluating test case '{}': {}", tc.query, e.getMessage());
                res.responseSnippet = "Error: " + e.getMessage();
                res.retrievedPages = Collections.emptyList();
            }

            res.durationMs = System.currentTimeMillis() - start;
            results.add(res);
        }

        EvalReport report = new EvalReport();
        report.totalTests = testSuite.size();
        report.passedTests = passedCount;
        report.results = results;
        report.overallPageAccuracy = (double) results.stream().filter(r -> r.pageMatched).count() / testSuite.size();
        report.averageKeywordRecall = results.stream().mapToDouble(r -> r.keywordScore).average().orElse(0.0);

        return report;
    }

    private List<Integer> extractPagesFromContext(String context) {
        if (context == null || context.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> pages = new ArrayList<>();
        // Match markers like "Page X" or "Pages X-Y" in block headers
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("Page\\s+(\\d+)|Pages\\s+(\\d+)-(\\d+)").matcher(context);
        while (m.find()) {
            if (m.group(1) != null) {
                pages.add(Integer.parseInt(m.group(1)));
            } else {
                pages.add(Integer.parseInt(m.group(2)));
                pages.add(Integer.parseInt(m.group(3)));
            }
        }
        return pages;
    }
}
