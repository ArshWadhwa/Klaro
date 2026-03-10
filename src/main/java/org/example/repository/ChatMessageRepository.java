package org.example.repository;

import org.example.entity.ChatMessage;
import org.example.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByDocumentOrderByCreatedAtAsc(Document document);

    List<ChatMessage> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ChatMessage m WHERE m.document.id = :documentId")
    void deleteByDocumentId(Long documentId);
}
