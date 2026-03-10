package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.OrganizationDTO;
import org.example.entity.Membership;
import org.example.entity.Organization;
import org.example.entity.User;
import org.example.repository.MembershipRepository;
import org.example.repository.OrganizationRepository;
import org.example.repository.UserRepository;
import org.example.security.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;

    /**
     * Ensure user has at least one active organization membership.
     * Auto-creates org + membership for legacy users who don't have one.
     */
    @Transactional
    public void ensureUserHasOrganization(User user) {
        List<Membership> memberships = membershipRepository
                .findByUserIdAndStatus(user.getId(), Membership.MembershipStatus.ACTIVE);
        
        if (memberships.isEmpty()) {
            log.info("🔧 Auto-creating organization for legacy user: {}", user.getEmail());
            String orgName = user.getFullName() + "'s Organization";
            createOrganization(orgName, user);
        }
    }

    /**
     * Create organization and assign creator as OWNER
     */
    @Transactional
    public Organization createOrganization(String name, User creator) {
        // Create organization
        Organization org = new Organization();
        org.setName(name);
        org = organizationRepository.save(org);
        
        // Create OWNER membership
        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(creator);
        membership.setRole(Membership.MembershipRole.OWNER);
        membership.setStatus(Membership.MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
        
        log.info("✅ Organization created: {} by {}", name, creator.getEmail());
        return org;
    }

    /**
     * Get current user's organization
     */
    public Organization getCurrentOrganization() {
        Long orgId = TenantContext.getOrganizationId();
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    /**
     * Get organization members with user details
     */
    public List<Membership> getOrganizationMembers() {
        Long orgId = TenantContext.getOrganizationId();
        return membershipRepository.findByOrganizationIdWithUser(orgId);
    }

    /**
     * Invite user to organization (ADMIN+ only)
     */
    @Transactional
    public Membership inviteUser(String email, Membership.MembershipRole role) {
        if (!TenantContext.isAdmin()) {
            throw new RuntimeException("Only admins can invite users");
        }
        
        Long orgId = TenantContext.getOrganizationId();
        Organization org = getCurrentOrganization();
        
        // Check plan limits
        long currentMembers = membershipRepository.countByOrganizationId(orgId);
        if (currentMembers >= org.getMaxUsers()) {
            throw new RuntimeException("Organization has reached max users limit");
        }
        
        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        // Check if already member
        if (membershipRepository.existsByUserIdAndOrganizationId(user.getId(), orgId)) {
            throw new RuntimeException("User already member of organization");
        }
        
        // Create membership
        Membership membership = new Membership();
        membership.setOrganization(org);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus(Membership.MembershipStatus.INVITED);
        membership.setInvitedBy(userRepository.findById(TenantContext.getUserId()).orElse(null));
        
        log.info("📧 User invited: {} to {} as {}", email, org.getName(), role);
        return membershipRepository.save(membership);
    }

    /**
     * Update member role (OWNER only)
     */
    @Transactional
    public Membership updateMemberRole(Long membershipId, Membership.MembershipRole newRole) {
        if (!TenantContext.isOwner()) {
            throw new RuntimeException("Only owners can update roles");
        }
        
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
        
        // Verify same organization
        if (!membership.getOrganization().getId().equals(TenantContext.getOrganizationId())) {
            throw new RuntimeException("Cannot update membership from another organization");
        }
        
        membership.setRole(newRole);
        return membershipRepository.save(membership);
    }

    /**
     * Remove member (ADMIN+ only, cannot remove self)
     */
    @Transactional
    public void removeMember(Long membershipId) {
        if (!TenantContext.isAdmin()) {
            throw new RuntimeException("Only admins can remove members");
        }
        
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Membership not found"));
        
        // Verify same organization
        if (!membership.getOrganization().getId().equals(TenantContext.getOrganizationId())) {
            throw new RuntimeException("Cannot remove membership from another organization");
        }
        
        // Prevent self-removal
        if (membership.getUser().getId().equals(TenantContext.getUserId())) {
            throw new RuntimeException("Cannot remove yourself from organization");
        }
        
        membershipRepository.delete(membership);
        log.info("🚪 Member removed: {} from {}", 
                membership.getUser().getEmail(), 
                membership.getOrganization().getName());
    }

    /**
     * Get organization usage stats
     */
    public OrganizationDTO.OrganizationStats getOrganizationStats() {
        Long orgId = TenantContext.getOrganizationId();
        Organization org = getCurrentOrganization();
        
        long activeMembers = membershipRepository.countByOrganizationId(orgId);
        long projectCount = organizationRepository.countProjects(orgId);
        
        OrganizationDTO.OrganizationStats stats = new OrganizationDTO.OrganizationStats();
        stats.setActiveMembers(activeMembers);
        stats.setMaxUsers(org.getMaxUsers());
        stats.setProjectCount(projectCount);
        stats.setMaxProjects(org.getMaxProjects());
        stats.setPlan(org.getPlan());
        
        return stats;
    }

    /**
     * Accept a pending invite — user joins the organization
     */
    @Transactional
    public Membership acceptInvite(Long membershipId, Long userId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        
        // Verify this invite belongs to the user
        if (!membership.getUser().getId().equals(userId)) {
            throw new RuntimeException("This invite does not belong to you");
        }
        
        // Verify it's actually an invite (not already active)
        if (membership.getStatus() != Membership.MembershipStatus.INVITED) {
            throw new RuntimeException("This invite is no longer pending");
        }
        
        // Accept the invite
        membership.setStatus(Membership.MembershipStatus.ACTIVE);
        membership = membershipRepository.save(membership);
        
        log.info("✅ Invite accepted: {} joined {}", 
                membership.getUser().getEmail(), 
                membership.getOrganization().getName());
        return membership;
    }

    /**
     * Decline a pending invite
     */
    @Transactional
    public void declineInvite(Long membershipId, Long userId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new RuntimeException("Invite not found"));
        
        if (!membership.getUser().getId().equals(userId)) {
            throw new RuntimeException("This invite does not belong to you");
        }
        
        if (membership.getStatus() != Membership.MembershipStatus.INVITED) {
            throw new RuntimeException("This invite is no longer pending");
        }
        
        membershipRepository.delete(membership);
        log.info("❌ Invite declined: {} declined {}", 
                membership.getUser().getEmail(), 
                membership.getOrganization().getName());
    }

    /**
     * Get pending invites for a user (by user ID)
     */
    public List<Membership> getPendingInvitesForUser(Long userId) {
        return membershipRepository.findByUserIdAndStatus(userId, Membership.MembershipStatus.INVITED);
    }
}
