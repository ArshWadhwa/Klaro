package org.example.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PDFExtractionService {

    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            StringBuilder sb = new StringBuilder();
            int pageCount = document.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String pageText = stripper.getText(document);
                sb.append("[Page ").append(i).append("]\n").append(pageText).append("\n");
            }
            return sb.toString();
        }
    }

    public String getSummary(String extractedText) {

        if (extractedText == null || extractedText.isBlank()) {
            return "No content available for summary.";
        }

        // Temporary simple summary logic (first 500 chars)
        int maxLength = 500;
        return extractedText.length() > maxLength
                ? extractedText.substring(0, maxLength) + "..."
                : extractedText;
    }

    public int getPageCount(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            return document.getNumberOfPages();
        }
    }
}