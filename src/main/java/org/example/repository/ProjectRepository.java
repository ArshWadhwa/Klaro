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

    @Query("SELECT p FROM Project p WHERE p.group IN (SELECT g FROM Group g JOIN g.members m WHERE m = :user)")
    List<Project> findByGroupMembersContaining(@Param("user") User user);

}
