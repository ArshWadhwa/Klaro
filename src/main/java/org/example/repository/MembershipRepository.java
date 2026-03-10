package org.example.repository;

import org.example.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.status = :status")
    List<Membership> findByUserIdAndStatus(Long userId, Membership.MembershipStatus status);
    
    @Query("SELECT m FROM Membership m WHERE m.user.id = :userId AND m.organization.id = :organizationId")
    Optional<Membership> findByUserIdAndOrganizationId(Long userId, Long organizationId);
    
    @Query("SELECT m FROM Membership m JOIN FETCH m.user WHERE m.organization.id = :organizationId AND m.status = 'ACTIVE' ORDER BY m.role, m.joinedAt")
    List<Membership> findByOrganizationIdWithUser(Long organizationId);
    
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.organization.id = :organizationId AND m.status = 'ACTIVE'")
    long countByOrganizationId(Long organizationId);
    
    boolean existsByUserIdAndOrganizationId(Long userId, Long organizationId);
}
