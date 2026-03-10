package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.OrganizationDTO;
import org.example.entity.Membership;
import org.example.entity.Organization;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.OrganizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    /**
     * GET /api/organizations/current - Get current user's organization
     */
    @GetMapping("/current")
    public ResponseEntity<OrganizationDTO.OrganizationResponse> getCurrentOrganization() {
        Organization org = organizationService.getCurrentOrganization();
        return ResponseEntity.ok(mapToResponse(org));
    }

    /**
     * GET /api/organizations/members - Get organization members
     */
    @GetMapping("/members")
    public ResponseEntity<List<OrganizationDTO.MembershipResponse>> getMembers() {
        List<Membership> memberships = organizationService.getOrganizationMembers();
        List<OrganizationDTO.MembershipResponse> response = memberships.stream()
                .map(this::mapToMembershipResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/organizations/invite - Invite user to organization (ADMIN+)
     */
    @PostMapping("/invite")
    public ResponseEntity<OrganizationDTO.MembershipResponse> inviteUser(
            @RequestBody OrganizationDTO.InviteRequest request) {
        
        Membership.MembershipRole role = Membership.MembershipRole.valueOf(request.getRole().toUpperCase());
        Membership membership = organizationService.inviteUser(request.getEmail(), role);
        
        return ResponseEntity.ok(mapToMembershipResponse(membership));
    }

    /**
     * PATCH /api/organizations/members/{id}/role - Update member role (OWNER only)
     */
    @PatchMapping("/members/{id}/role")
    public ResponseEntity<OrganizationDTO.MembershipResponse> updateRole(
            @PathVariable Long id,
            @RequestBody OrganizationDTO.UpdateRoleRequest request) {
        
        Membership.MembershipRole role = Membership.MembershipRole.valueOf(request.getRole().toUpperCase());
        Membership membership = organizationService.updateMemberRole(id, role);
        
        return ResponseEntity.ok(mapToMembershipResponse(membership));
    }

    /**
     * DELETE /api/organizations/members/{id} - Remove member (ADMIN+)
     */
    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id) {
        organizationService.removeMember(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/organizations/stats - Get organization usage stats
     */
    @GetMapping("/stats")
    public ResponseEntity<OrganizationDTO.OrganizationStats> getStats() {
        return ResponseEntity.ok(organizationService.getOrganizationStats());
    }

    /**
     * GET /api/organizations/my-invites - Get pending invites for current user
     * This endpoint works even WITHOUT an active org membership
     */
    @GetMapping("/my-invites")
    public ResponseEntity<List<OrganizationDTO.PendingInviteResponse>> getMyPendingInvites() {
        User user = getCurrentUser();
        List<Membership> invites = organizationService.getPendingInvitesForUser(user.getId());
        List<OrganizationDTO.PendingInviteResponse> response = invites.stream()
                .map(this::mapToPendingInviteResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/organizations/accept-invite/{membershipId} - Accept an invite
     * This endpoint works even WITHOUT an active org membership
     */
    @PostMapping("/accept-invite/{membershipId}")
    public ResponseEntity<?> acceptInvite(@PathVariable Long membershipId) {
        User user = getCurrentUser();
        Membership membership = organizationService.acceptInvite(membershipId, user.getId());
        return ResponseEntity.ok(mapToMembershipResponse(membership));
    }

    /**
     * POST /api/organizations/decline-invite/{membershipId} - Decline an invite
     */
    @PostMapping("/decline-invite/{membershipId}")
    public ResponseEntity<?> declineInvite(@PathVariable Long membershipId) {
        User user = getCurrentUser();
        organizationService.declineInvite(membershipId, user.getId());
        return ResponseEntity.ok("Invite declined");
    }

    // Helper methods
    private OrganizationDTO.OrganizationResponse mapToResponse(Organization org) {
        OrganizationDTO.OrganizationResponse response = new OrganizationDTO.OrganizationResponse();
        response.setId(org.getId());
        response.setName(org.getName());
        response.setSlug(org.getSlug());
        response.setPlan(org.getPlan());
        response.setMaxUsers(org.getMaxUsers());
        response.setMaxProjects(org.getMaxProjects());
        response.setIsActive(org.getIsActive());
        return response;
    }

    private OrganizationDTO.MembershipResponse mapToMembershipResponse(Membership m) {
        OrganizationDTO.MembershipResponse response = new OrganizationDTO.MembershipResponse();
        response.setId(m.getId());
        response.setUserEmail(m.getUser().getEmail());
        response.setRole(m.getRole().name());
        response.setStatus(m.getStatus().name());
        response.setInvitedByEmail(m.getInvitedBy() != null ? m.getInvitedBy().getEmail() : null);
        response.setCreatedAt(m.getJoinedAt().toString());
        return response;
    }

    private OrganizationDTO.PendingInviteResponse mapToPendingInviteResponse(Membership m) {
        OrganizationDTO.PendingInviteResponse response = new OrganizationDTO.PendingInviteResponse();
        response.setMembershipId(m.getId());
        response.setOrganizationId(m.getOrganization().getId());
        response.setOrganizationName(m.getOrganization().getName());
        response.setRole(m.getRole().name());
        response.setInvitedByEmail(m.getInvitedBy() != null ? m.getInvitedBy().getEmail() : null);
        response.setInvitedAt(m.getJoinedAt().toString());
        return response;
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof User) {
            email = ((User) principal).getEmail();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not authenticated"));
    }
}
