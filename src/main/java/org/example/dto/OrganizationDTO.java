package org.example.dto;

import lombok.Data;

@Data
public class OrganizationDTO {
    
    @Data
    public static class CreateRequest {
        private String name;
    }
    
    @Data
    public static class InviteRequest {
        private String email;
        private String role; // ADMIN or MEMBER
    }
    
    @Data
    public static class UpdateRoleRequest {
        private String role; // OWNER, ADMIN, or MEMBER
    }
    
    @Data
    public static class OrganizationResponse {
        private Long id;
        private String name;
        private String slug;
        private String plan;
        private Integer maxUsers;
        private Integer maxProjects;
        private Boolean isActive;
    }
    
    @Data
    public static class MembershipResponse {
        private Long id;
        private String userEmail;
        private String role;
        private String status;
        private String invitedByEmail;
        private String createdAt;
    }
    
    @Data
    public static class OrganizationStats {
        private Long activeMembers;
        private Integer maxUsers;
        private Long projectCount;
        private Integer maxProjects;
        private String plan;
    }

    @Data
    public static class PendingInviteResponse {
        private Long membershipId;
        private Long organizationId;
        private String organizationName;
        private String role;
        private String invitedByEmail;
        private String invitedAt;
    }
}
