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

import java.util.ArrayList;
import java.util.HashSet;
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

    @Autowired
    private NotificationService notificationService;

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
        group.setInviteCode(generateUniqueInviteCode());
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


        // ✅ FORCE FLUSH to database
        groupRepository.flush();

        // Fetch group with members to ensure they are loaded
        Group groupWithMembers = groupRepository.findByIdWithMembers(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));


        // ✅ FORCE INITIALIZE
        groupWithMembers.getMembers().size();

        System.out.println("🔍 DEBUG - Created group members: " + groupWithMembers.getMembers().size());

        return toGroupResponse(groupWithMembers);
    }


    public List<String> getGroupMemberEmails(Long groupId) {
        // ✅ Direct SQL query - sirf emails chahiye
        List<String> emails = groupRepository.findMemberEmailsByGroupId(groupId);
        System.out.println("✅ Fetched " + emails.size() + " member emails for group " + groupId);
        return emails;
    }


    public List<GroupResponse> getGroupsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ Get groups where user is owner
        List<Group> ownedGroups = groupRepository.findByOwner(user);

        // ✅ Get groups where user is member
        List<Group> memberGroups = groupRepository.findGroupsByMemberId(user.getId());

        // ✅ Combine and remove duplicates
        Set<Long> groupIds = new HashSet<>();
        List<Group> allUserGroups = new ArrayList<>();

        for (Group g : ownedGroups) {
            if (groupIds.add(g.getId())) {
                allUserGroups.add(g);
            }
        }
        for (Group g : memberGroups) {
            if (groupIds.add(g.getId())) {
                allUserGroups.add(g);
            }
        }

        return allUserGroups.stream()
                .map(group -> {
                    List<String> memberEmails = groupRepository.findMemberEmailsByGroupId(group.getId());

                    GroupResponse response = new GroupResponse();
                    response.setId(group.getId());
                    response.setName(group.getName());
                    response.setDescription(group.getDescription());
                    response.setOwnerName(group.getOwner().getFullName());
                    response.setOwnerEmail(group.getOwner().getEmail());
                    response.setCreatedAt(group.getCreatedAt());
                    response.setUpdatedAt(group.getUpdatedAt());
                    response.setMemberCount(memberEmails.size());
                    response.setProjectCount(group.getProjects() != null ? group.getProjects().size() : 0);
                    response.setMembers(new ArrayList<>()); // Empty for list view

                    return response;
                })
                .collect(Collectors.toList());
    }

    public List<GroupResponse> getAllGroups() {
        List<Group> groups = groupRepository.findAllWithMembers();
        return groups.stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get invite code for a group (admin/owner only)
     */
    public String getGroupInviteCode(Long groupId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user is owner or admin
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can view invite code");
        }

        return group.getInviteCode();
    }

    /**
     * Join a group using invite code (any authenticated user)
     */
    public GroupResponse joinGroupByCode(String inviteCode, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        // Check if user is already a member
        if (group.isMember(user)) {
            throw new RuntimeException("You are already a member of this group");
        }

        // Add user to group
        group.addMember(user);
        Group savedGroup = groupRepository.save(group);

        // Send notification
        String message = String.format("You have successfully joined the group '%s'", group.getName());
        notificationService.createNotification(user.getEmail(), message);

        // Fetch group with members
        Group groupWithMembers = groupRepository.findByIdWithMembers(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return toGroupResponse(groupWithMembers);
    }

    public String regenerateInviteCode(Long groupId, String currentUserEmail) {
        User currentUser = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        // Check if current user is owner or admin
        boolean isAdmin = authService.isAdminByEmail(currentUserEmail);
        if (!group.isOwner(currentUser) && !isAdmin) {
            throw new RuntimeException("Only group owner or admin can regenerate invite code");
        }

        String newCode = generateUniqueInviteCode();
        group.setInviteCode(newCode);
        groupRepository.save(group);

        return newCode;
    }

    // join a group using invite code

    private GroupResponse getGroupResp(String inviteCode, String userEmail){
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(()->new RuntimeException("User not fnd"));

        Group group = groupRepository.findByInviteCode(inviteCode)
                .orElseThrow(()-> new RuntimeException("group not there"));

        // Check if user is already a member
        if (group.isMember(user)) {
            throw new RuntimeException("You are already a member of this group");
        }

        // Add user to group
        group.addMember(user);
        Group savedGroup = groupRepository.save(group);

        // Send notification
        String message = String.format("You have successfully joined the group '%s'", group.getName());
        notificationService.createNotification(user.getEmail(), message);

        // Fetch group with members
        Group groupWithMembers = groupRepository.findByIdWithMembers(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return toGroupResponse(groupWithMembers);

    }



        private String generateCode(){
        String chars="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();

        for (int i = 0; i < 10; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }

        return code.toString();
    }

    // Ensure code is unique
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateCode();
        } while (groupRepository.existsByInviteCode(code)); // You'll need to add this method to repository

        return code;
    }




    public GroupResponse getGroupById(Long groupId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));


        // ✅ FORCE INITIALIZE members collection
        group.getMembers().size(); // This triggers Hibernate to load the collection

        System.out.println("🔍 DEBUG - Group ID: " + group.getId());
        System.out.println("📊 DEBUG - Members size: " + group.getMembers().size());
        System.out.println("👥 DEBUG - Members: " + group.getMembers());

        return toGroupResponse(group);
    }

    public List<GroupMemberResponse> getGroupMembers(Long groupId) {
        Group group = groupRepository.findByIdWithMembers(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return group.getMembers().stream()
                .map(member -> {
                    GroupMemberResponse memberResponse = new GroupMemberResponse();
                    memberResponse.setId(member.getId());
                    memberResponse.setFullName(member.getFullName());
                    memberResponse.setEmail(member.getEmail());
                    memberResponse.setOwner(group.isOwner(member));
                    return memberResponse;
                })
                .collect(Collectors.toList());
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

    public void addMembersTOGroup(Long groupId, List<Long> userIds){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()-> new RuntimeException("Group not found"));

                List<User> usersToAdd = userRepository.findAllById(userIds);
                group.getMembers().addAll(usersToAdd);
                groupRepository.save(group);

                for(User user: usersToAdd){
                    String message = String.format( "You have been added to the '%s' group", group.getName());
                    System.out.println("Added user: " + user.getEmail() + " to group: " + group.getName());
                    notificationService.createNotification(user.getEmail(),message);
                }

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

        // Fetch group with members to ensure they are loaded
        Group groupWithMembers = groupRepository.findByIdWithMembers(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return toGroupResponse(groupWithMembers);
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

        // Fetch group with members to ensure they are loaded
        Group groupWithMembers = groupRepository.findByIdWithMembers(savedGroup.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        return toGroupResponse(groupWithMembers);
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
        response.setInviteCode(group.getInviteCode());
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