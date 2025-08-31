package org.example.controller;

import org.example.group.CreateGroupRequest;
import org.example.group.GroupResponse;
import org.example.service.AuthService;
import org.example.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
//
@RequestMapping("/groups")

public class GroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<?> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can create groups
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can create groups");
                }
                
                GroupResponse response = groupService.createGroup(request, email);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getUserGroups(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                List<GroupResponse> groups = groupService.getGroupsByUser(email);
                return ResponseEntity.ok(groups);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllGroups(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can view all groups
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can view all groups");
                }
                
                List<GroupResponse> groups = groupService.getAllGroups();
                return ResponseEntity.ok(groups);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupById(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                GroupResponse group = groupService.getGroupById(groupId);
                return ResponseEntity.ok(group);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchGroups(
            @RequestParam String query,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                List<GroupResponse> groups = groupService.searchGroups(query);
                return ResponseEntity.ok(groups);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<?> addMemberToGroup(
            @PathVariable Long groupId,
            @RequestParam String memberEmail,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can add members to groups
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can add members to groups");
                }
                
                GroupResponse response = groupService.addMemberToGroup(groupId, memberEmail, email);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{groupId}/members/batch")
    public ResponseEntity<?> addMultipleMembersToGroup(
            @PathVariable Long groupId,
            @RequestBody List<String> memberEmails,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can add members to groups
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can add members to groups");
                }
                
                GroupResponse response = groupService.addMultipleMembersToGroup(groupId, memberEmails, email);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}/members")
    public ResponseEntity<?> removeMemberFromGroup(
            @PathVariable Long groupId,
            @RequestParam String memberEmail,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                
                // Only admins can remove members from groups
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only admins can remove members from groups");
                }
                
                GroupResponse response = groupService.removeMemberFromGroup(groupId, memberEmail, email);
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                groupService.deleteGroup(groupId, email);
                return ResponseEntity.ok("Group deleted successfully");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
} 