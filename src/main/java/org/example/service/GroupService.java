package org.example.service;

import org.example.entity.Group;
import org.example.entity.User;
import org.example.entity.Project;
import org.example.group.CreateGroupRequest;
import org.example.group.GroupMemberResponse;
import org.example.group.GroupResponse;
import org.example.repository.GroupRepository;
import org.example.repository.UserRepository;
import org.example.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private AuthService authService;

    public GroupResponse createGroup(CreateGroupRequest request, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if group name already exists
        if (groupRepository.existsByName(request.getName())) {
            throw new RuntimeException("Group name already exists");
        }

        // Create new group
        Group group = new Group();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setOwner(currentUser);
        group.addMember(currentUser); // Owner is automatically a member

        // Add members by email
        Set<String> addedEmails = request.getMemberEmails().stream()
                .map(email -> email.trim().toLowerCase())
                .collect(Collectors.toSet());

        List<User> existingUsers = userRepository.findAll().stream()
                .filter(user -> addedEmails.contains(user.getEmail().toLowerCase()))
                .collect(Collectors.toList());

        for (User user : existingUsers) {
            group.addMember(user);
        }

        Group savedGroup = groupRepository.save(group);
        return toGroupResponse(savedGroup);
    }

    public List<GroupResponse> getGroupsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Group> groups = groupRepository.findByOwnerOrMember(user);
        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getAllGroups() {
        List<Group> groups = groupRepository.findAll();
        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return toGroupResponse(group);
    }

    public GroupResponse getGroupByName(String groupName) {
        Group group = groupRepository.findByName(groupName)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        return toGroupResponse(group);
    }

    public List<GroupResponse> searchGroups(String searchTerm) {
        List<Group> groups = groupRepository.searchGroups(searchTerm);
        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse addMemberToGroup(Long groupId, String memberEmail, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user has permission (owner or admin)
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can add members");
        }

        User newMember = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + memberEmail));

        if (group.isMember(newMember)) {
            throw new RuntimeException("User is already a member of this group");
        }

        group.addMember(newMember);
        Group savedGroup = groupRepository.save(group);
        return toGroupResponse(savedGroup);
    }

    public GroupResponse addMultipleMembersToGroup(Long groupId, List<String> memberEmails, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user has permission (owner or admin)
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can add members");
        }

        int addedCount = 0;
        StringBuilder errors = new StringBuilder();

        for (String memberEmail : memberEmails) {
            try {
                User newMember = userRepository.findByEmail(memberEmail.trim())
                        .orElseThrow(() -> new RuntimeException("User not found: " + memberEmail));

                if (!group.isMember(newMember)) {
                    group.addMember(newMember);
                    addedCount++;
                } else {
                    errors.append("User ").append(memberEmail).append(" is already a member. ");
                }
            } catch (RuntimeException e) {
                errors.append("Error with ").append(memberEmail).append(": ").append(e.getMessage()).append(". ");
            }
        }

        Group savedGroup = groupRepository.save(group);
        
        if (errors.length() > 0 && addedCount == 0) {
            throw new RuntimeException("Failed to add any members: " + errors.toString());
        }
        
        return toGroupResponse(savedGroup);
    }

    public GroupResponse removeMemberFromGroup(Long groupId, String memberEmail, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user has permission (owner or admin)
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can remove members");
        }

        User memberToRemove = userRepository.findByEmail(memberEmail)
                .orElseThrow(() -> new RuntimeException("User not found: " + memberEmail));

        if (!group.isMember(memberToRemove)) {
            throw new RuntimeException("User is not a member of this group");
        }

        if (group.isOwner(memberToRemove)) {
            throw new RuntimeException("Cannot remove group owner");
        }

        group.removeMember(memberToRemove);
        Group savedGroup = groupRepository.save(group);
        return toGroupResponse(savedGroup);
    }

    public void deleteGroup(Long groupId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("Current user not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user has permission (owner or admin)
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can delete the group");
        }

        groupRepository.delete(group);
    }

    private GroupResponse toGroupResponse(Group group) {
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setOwnerName(group.getOwner().getFullName());
        response.setOwnerEmail(group.getOwner().getEmail());
        response.setCreatedAt(group.getCreatedAt());
        response.setUpdatedAt(group.getUpdatedAt());
        response.setMemberCount(group.getMembers().size());
        response.setProjectCount(group.getProjects() != null ? group.getProjects().size() : 0);

        // Convert members to DTOs
        List<GroupMemberResponse> memberResponses = group.getMembers().stream()
                .map(member -> {
                    GroupMemberResponse memberResponse = new GroupMemberResponse();
                    memberResponse.setId(member.getId());
                    memberResponse.setFullName(member.getFullName());
                    memberResponse.setEmail(member.getEmail());
                    memberResponse.setOwner(group.isOwner(member));
                    return memberResponse;
                })
                .collect(Collectors.toList());

        response.setMembers(memberResponses);
        return response;
    }
} 