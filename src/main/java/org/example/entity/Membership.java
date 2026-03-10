package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "memberships", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MembershipRole role = MembershipRole.MEMBER;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt = LocalDateTime.now();

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    public enum MembershipRole {
        OWNER, ADMIN, MEMBER
    }

    public enum MembershipStatus {
        ACTIVE, INVITED, SUSPENDED
    }

    public boolean isOwner() {
        return role == MembershipRole.OWNER;
    }

    public boolean isAdmin() {
        return role == MembershipRole.ADMIN || role == MembershipRole.OWNER;
    }

    public boolean isActive() {
        return status == MembershipStatus.ACTIVE;
    }
}
