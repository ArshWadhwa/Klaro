package org.example.repository;


import org.example.entity.Notification;
import org.hibernate.boot.archive.internal.JarProtocolArchiveDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long > {
    List<Notification> findByRecipientOrderByCreatedAtDesc(String recipient);

    //findByRecipientOrderByCreatedAtDesc(...) → ye fetch karta h saaare notifications , latest sabse pehle

    long countByRecipientAndReadFalse(String recipient);
    //countByRecipientAndReadFalse(...) → jo unread notification hai hamre paas ye unka count show karega...



}
