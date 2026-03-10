package org.example.service;


import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PDFExtractionService {

    public String extractText(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
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
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            return document.getNumberOfPages();
        }
    }
}