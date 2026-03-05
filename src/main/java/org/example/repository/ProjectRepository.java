package org.example.repository;

import org.example.entity.User;
import org.example.entity.Project;
import org.example.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProjectRepository extends JpaRepository<Project,Long> {

    List<Project> findByCreatedBy(User user);
    
    List<Project> findByCreatedByEmail(String email);

    List<Project> findByGroup(Group group);

    List<Project> findByGroupId(Long groupId);

    // 🎯 Multi-tenant: Find projects by organization
    List<Project> findByOrganizationId(Long organizationId);

    @Query("SELECT p FROM Project p WHERE p.group IN (SELECT g FROM Group g JOIN g.members m WHERE m = :user)")
    List<Project> findByGroupMembersContaining(@Param("user") User user);

    // Fetch project with group members eagerly to avoid LazyInitializationException
    @Query("SELECT p FROM Project p " +
           "LEFT JOIN FETCH p.group g " +
           "LEFT JOIN FETCH g.members " +
           "WHERE p.id = :projectId")
    java.util.Optional<Project> findByIdWithGroupMembers(@Param("projectId") Long projectId);

    // Check if a user has access to a project (created it, member of group, or no group)
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Project p " +
           "LEFT JOIN p.group g " +
           "LEFT JOIN g.members m " +
           "WHERE p.id = :projectId AND " +
           "(p.createdBy.id = :userId OR m.id = :userId OR p.group IS NULL)")
    boolean userHasAccessToProject(@Param("projectId") Long projectId, @Param("userId") Long userId);

}
