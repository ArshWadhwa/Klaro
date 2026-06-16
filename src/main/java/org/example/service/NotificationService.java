package org.example.service;

import org.example.entity.Notification;
import org.example.entity.User;
import org.example.repository.NotificationRepository;
import org.example.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a notification and persist directly to DB.
     * Kafka has been disabled, so all notifications are now synchronous.
     */
    public void createNotification(String recipientEmail, String message) {
        createNotification(recipientEmail, message, "GENERAL");
    }

    public void createNotification(String recipientEmail, String message, String eventType) {
        try {
            logger.debug("📤 Creating notification for {}", recipientEmail);
            directDbWrite(recipientEmail, message);
        } catch (Exception e) {
            logger.error("❌ Failed to create notification for {}: {}", recipientEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to create notification", e);
        }
    }

    /**
     * Direct DB write for notifications.
     */
    public void directDbWrite(String recipientEmail, String message) {
        userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = new Notification();
        notification.setRecipient(recipientEmail);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotification(String recipient) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(recipient);
    }

    public long getUnreadCount(String recipient) {
        return notificationRepository.countByRecipientAndReadFalse(recipient);
    }

    public void markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }
}
