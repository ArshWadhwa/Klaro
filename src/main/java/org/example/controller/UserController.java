package org.example.controller;

import org.example.entity.User;
import org.example.group.UserInfoResponse;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/available")
    public ResponseEntity<List<UserInfoResponse>> getAvailableUsers(
            @RequestParam(required = false) Long groupId
    ) {
        List<User> allUsers = userRepository.findAll();
        List<UserInfoResponse> availableUsers;

        if (groupId != null && userRepository.findGroupMemberEmails(groupId) != null) {
            // Filter out users already in the group
            Set<String> groupMemberEmails = userRepository.findGroupMemberEmails(groupId);
            availableUsers = allUsers.stream()
                    .filter(user -> !groupMemberEmails.contains(user.getEmail()))
                    .map(user -> new UserInfoResponse(
                            user.getId(),
                            user.getEmail(),
                            user.getFullName(),
                            user.getRole().toString()
                    ))
                    .collect(Collectors.toList());
        } else {
            // Return all users
            availableUsers = allUsers.stream()
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
}