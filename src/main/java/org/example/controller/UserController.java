package org.example.controller;

import org.example.entity.User;
import org.example.group.UserInfoResponse;
import org.example.repository.UserRepository;
import org.example.repository.GroupRepository;
import org.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {
        "http://localhost:3001",
        "https://1e27-2405-201-5803-9887-f09f-e037-ca69-f5e6.ngrok-free.app"
})

@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private AuthService authService;

    @GetMapping("/available")
    public ResponseEntity<?> getAvailableUsers(
            @RequestParam(required = false) Long groupId,
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = authService.getEmailFromToken(token);
                User currentUser = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                boolean isAdmin = "ROLE_ADMIN".equals(currentUser.getRole().toString());

                List<User> allowedUsers;
                if (isAdmin) {
                    allowedUsers = userRepository.findAll();
                } else {
                    // Get groups where current user is a member/owner
                    List<org.example.entity.Group> userGroups = groupRepository.findByOwnerOrMember(currentUser);
                    // Collect all members from these groups
                    Set<User> sharedUsers = new java.util.HashSet<>();
                    for (org.example.entity.Group group : userGroups) {
                        sharedUsers.addAll(group.getMembers());
                        sharedUsers.add(group.getOwner());
                    }
                    allowedUsers = new ArrayList<>(sharedUsers);
                }

                List<UserInfoResponse> availableUsers;
                if (groupId != null) {
                    // Check if group exists and if user has access to it
                    org.example.entity.Group group = groupRepository.findByIdWithMembers(groupId).orElse(null);
                    if (group == null) {
                        return ResponseEntity.badRequest().body("Group not found");
                    }
                    if (!isAdmin && !group.isOwner(currentUser) && !group.isMember(currentUser)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have access to this group");
                    }

                    Set<String> groupMemberEmails = userRepository.findGroupMemberEmails(groupId);
                    availableUsers = allowedUsers.stream()
                            .filter(user -> !groupMemberEmails.contains(user.getEmail()))
                            .map(user -> new UserInfoResponse(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getFullName(),
                                    user.getRole().toString()
                            ))
                            .collect(Collectors.toList());
                } else {
                    availableUsers = allowedUsers.stream()
                            .map(user -> new UserInfoResponse(
                                    user.getId(),
                                    user.getEmail(),
                                    user.getFullName(),
                                    user.getRole().toString()
                            ))
                            .collect(Collectors.toList());
                }
                return ResponseEntity.ok(availableUsers);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid authorization header");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}