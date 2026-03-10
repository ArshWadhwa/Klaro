package org.example.repository;

import org.example.entity.Document;
import org.example.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(Document document);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM DocumentChunk c WHERE c.document.id = :documentId")
    void deleteByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);
}
