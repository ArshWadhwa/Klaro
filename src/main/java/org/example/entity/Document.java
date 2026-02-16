package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
@Entity
@Table(name = "documents")
@Data
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

        private LocalDateTime uploadedAt;
        private LocalDateTime updatedAt;
    }
