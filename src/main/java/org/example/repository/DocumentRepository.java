package org.example.repository;

import org.example.entity.Document;
import org.example.entity.Project;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Find all documents uploaded by a specific user
    List<Document> findByUploadedBy(User user);

    // Find all documents ordered by upload date (newest first)
    List<Document> findAllByOrderByUploadedAtDesc();

    // Find documents by user ordered by date
    List<Document> findByUploadedByOrderByUploadedAtDesc(User user);

    // Search documents by filename
    List<Document> findByFileNameContainingIgnoreCase(String fileName);


  List<Document> findByProject(Project project);

// Find documents by project ID
List<Document> findByProjectId(Long projectId);

// Find documents by project and ordered by date
List<Document> findByProjectOrderByUploadedAtDesc(Project project);

// Find documents by project ID ordered by date
List<Document> findByProjectIdOrderByUploadedAtDesc(Long projectId);

    // Check if document exists
    boolean existsById(Long id);

    // Optional: Find by public ID (Cloudinary)
    Optional<Document> findByPublicId(String publicId);
}