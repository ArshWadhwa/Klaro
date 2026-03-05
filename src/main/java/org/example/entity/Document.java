package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
@Entity
@Table(name = "documents")
@Data
@lombok.EqualsAndHashCode(exclude = {"chunks", "chatMessages", "uploadedBy", "organization", "project"})
public class Document {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;


        private String fileName;
        private String fileUrl; // S3 or local path
        private Long fileSize;
        private Integer pageCount;

        @Column(columnDefinition = "TEXT")
        private String extractedText; // Full PDF text

        @Column(nullable = false)
        private String publicId;

        @Lob
        @Column(columnDefinition = "TEXT")
        private String aiSummary; // AI-generated summary

        @ToString.Exclude // Prevent circular reference in toString()
        @ManyToOne
        @JoinColumn(name = "uploaded_by")
        private User uploadedBy;

        @ToString.Exclude // Multi-tenant: Organization (nullable for backward compatibility)
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "organization_id")
        private Organization organization;

    @ToString.Exclude // Multi-tenant: Project (nullable for backward compatibility)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Cascade delete: When document is deleted, delete all its chunks too
    @ToString.Exclude
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks;

    // Cascade delete: When document is deleted, delete all chat messages too
    @ToString.Exclude
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages;

    @Column(name = "processing_status")
    private String processingStatus; // PENDING, PROCESSING, PROCESSED, FAILED        // Getter and setter
        public String getProcessingStatus() { return processingStatus; }
        public void setProcessingStatus(String status) { this.processingStatus = status; }


        private LocalDateTime uploadedAt;
        private LocalDateTime updatedAt;
    }
