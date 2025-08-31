package org.example.repository;

import org.example.entity.Group;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Group> findByOwner(User owner);
    
    @Query("SELECT g FROM Group g JOIN g.members m WHERE m = :user")
    List<Group> findByMember(@Param("user") User user);
    
    @Query("SELECT g FROM Group g WHERE g.owner = :user OR :user MEMBER OF g.members")
    List<Group> findByOwnerOrMember(@Param("user") User user);
    
    @Query("SELECT g FROM Group g WHERE g.name LIKE %:searchTerm% OR g.description LIKE %:searchTerm%")
    List<Group> searchGroups(@Param("searchTerm") String searchTerm);
} 