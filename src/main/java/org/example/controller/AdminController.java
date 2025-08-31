package org.example.controller;

import org.example.entity.User;
import org.example.group.UserInfoResponse;
import org.example.repository.UserRepository;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthService authService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Only admins can view all users
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can view all users");
                }
                
                List<User> users = userRepository.findAll();
                List<UserInfoResponse> userResponses = users.stream()
                    .map(user -> new UserInfoResponse(user.getEmail(), user.getFullName(), user.getRole().toString()))
                    .collect(Collectors.toList());
                
                return ResponseEntity.ok(userResponses);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/users/available")
    public ResponseEntity<?> getAvailableUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Long groupId
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Only admins can view all users
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can view all users");
                }
                
                List<User> allUsers = userRepository.findAll();
                List<UserInfoResponse> availableUsers;
                
                if (groupId != null) {
                    // Filter out users who are already members of the specified group
                    Set<String> groupMemberEmails = userRepository.findGroupMemberEmails(groupId);
                    availableUsers = allUsers.stream()
                        .filter(user -> !groupMemberEmails.contains(user.getEmail()))
                        .map(user -> new UserInfoResponse(user.getEmail(), user.getFullName(), user.getRole().toString()))
                        .collect(Collectors.toList());
                } else {
                    // Return all users
                    availableUsers = allUsers.stream()
                        .map(user -> new UserInfoResponse(user.getEmail(), user.getFullName(), user.getRole().toString()))
                        .collect(Collectors.toList());
                }
                
                return ResponseEntity.ok(availableUsers);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                // Only admins can view system stats
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can view system statistics");
                }
                
                // You can add system statistics here
                return ResponseEntity.ok("System statistics - placeholder for analytics");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
