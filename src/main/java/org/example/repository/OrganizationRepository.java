package org.example.repository;

import org.example.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    
    Optional<Organization> findBySlug(String slug);
    
    boolean existsBySlug(String slug);
    
    @Query("SELECT o FROM Organization o WHERE o.id = :id AND o.isActive = true")
    Optional<Organization> findActiveById(Long id);
    
    @Query("SELECT COUNT(m) FROM Membership m WHERE m.organization.id = :organizationId AND m.status = 'ACTIVE'")
    long countActiveMembers(Long organizationId);
    
    @Query("SELECT COUNT(p) FROM Project p WHERE p.organization.id = :organizationId")
    long countProjects(Long organizationId);
}
