package org.example.controller;


import org.example.entity.Notification;
import org.example.service.AuthService;
import org.example.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController

@RequestMapping("/notifications")
public class NotificationController {

    private NotificationService notificationService;
    @Autowired 
    private AuthService authService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/username")
    public ResponseEntity<?> getAllNotifications(@RequestHeader("Authorization") String authHeader){
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                String fullName = authService.getFullNameFromToken(token);
                
                List<Notification> notifications = notificationService.getNotification(fullName);
                return ResponseEntity.ok(notifications);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                long count = notificationService.getUnreadCount(email);
                return ResponseEntity.ok(count);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);

        return ResponseEntity.ok("Marked as read");
    }

}
