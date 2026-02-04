package org.example.controller;

import org.example.group.CreateGroupRequest;
import org.example.group.GroupMemberResponse;
import org.example.group.GroupResponse;
import org.example.service.AuthService;
import org.example.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
//
@CrossOrigin(origins = {
        "http://localhost:3001",
        "https://1e27-2405-201-5803-9887-f09f-e037-ca69-f5e6.ngrok-free.app"
})

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

    // Get invite code for a group
    @GetMapping("/{groupId}/invite-code")
    public ResponseEntity<?> getInviteCode(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);

                String inviteCode = groupService.getGroupInviteCode(groupId, email);
                return ResponseEntity.ok(Map.of(
                        "inviteCode", inviteCode,
                        "groupId", groupId.toString()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Regenerate invite code
    @PostMapping("/{groupId}/regenerate-invite-code")
    public ResponseEntity<?> regenerateInviteCode(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);

                String newCode = groupService.regenerateInviteCode(groupId, email);
                return ResponseEntity.ok(Map.of(
                        "inviteCode", newCode,
                        "message", "Invite code regenerated successfully"
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Join group by code
    @PostMapping("/join-by-code")
    public ResponseEntity<?> joinByCode(
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);

                String inviteCode = request.get("inviteCode");
                if (inviteCode == null || inviteCode.trim().isEmpty()) {
                    return ResponseEntity.badRequest().body("Invite code is required");
                }

                GroupResponse group = groupService.joinGroupByCode(inviteCode.trim(), email);
                return ResponseEntity.ok(group);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization header");
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

    @GetMapping("/{groupId}/member-emails")
    public ResponseEntity<List<String>> getGroupMemberEmails(@PathVariable Long groupId) {
        List<String> emails = groupService.getGroupMemberEmails(groupId);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<?> getGroupMembers(
            @PathVariable Long groupId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);

                // Only admins can view group members
                if (!authService.isAdmin(token)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Only admins can view group members");
                }

                List<GroupMemberResponse> members = groupService.getGroupMembers(groupId);
                return ResponseEntity.ok(members);
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing or invalid authorization header");

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