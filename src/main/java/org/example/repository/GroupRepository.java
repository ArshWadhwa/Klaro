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

    // 🎯 Per-organization unique group name check
    boolean existsByNameAndOrganizationId(String name, Long organizationId);

    // 🥇 LEVEL 1: DTO Projection + JOIN FETCH (BEST for immediate member loading)
    // Explicit JOIN control, DISTINCT avoids duplication, predictable SQL
    @Query("""
        SELECT DISTINCT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members
        WHERE g.owner = :owner
    """)
    List<Group> findByOwner(@Param("owner") User owner);

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m = :user")
    List<Group> findByMember(@Param("user") User user);

    // 🥇 LEVEL 1: Optimized with JOIN FETCH
    @Query("""
        SELECT DISTINCT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members
        WHERE g.owner = :user OR :user MEMBER OF g.members
    """)
    List<Group> findByOwnerOrMember(@Param("user") User user);

    @Query("SELECT g FROM Group g WHERE g.name LIKE %:searchTerm% OR g.description LIKE %:searchTerm%")
    List<Group> searchGroups(@Param("searchTerm") String searchTerm);

    // 🥇 LEVEL 1: JOIN FETCH for single group lookup
    @Query("""
        SELECT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members
        WHERE g.id = :id
    """)
    Optional<Group> findByIdWithMembers(@Param("id") Long id);

    // 🥇 LEVEL 1: Optimized with JOIN FETCH to avoid N+1 queries
    @Query("""
        SELECT DISTINCT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members m
        WHERE m.id = :userId
    """)
    List<Group> findGroupsByMemberId(@Param("userId") Long userId);

    // ✅ NATIVE QUERY to fetch members
    @Query(value = "SELECT u.* FROM users u " +
            "INNER JOIN group_members gm ON u.id = gm.user_id " +
            "WHERE gm.group_id = :groupId",
            nativeQuery = true)
    List<User> findMembersByGroupId(@Param("groupId") Long groupId);

    @Query(value = "SELECT u.email FROM users u " +
            "INNER JOIN group_members gm ON u.id = gm.user_id " +
            "WHERE gm.group_id = :groupId",
            nativeQuery = true)
    List<String> findMemberEmailsByGroupId(@Param("groupId") Long groupId);

    List<Group> findByMembersContaining(User member);

    List<Group> findByOwnerOrMembers(User owner, User member);

    // 🥇 LEVEL 1: JOIN FETCH for comprehensive group loading
    @Query("""
        SELECT DISTINCT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members
        WHERE g.owner = :user OR :user MEMBER OF g.members
    """)
    List<Group> findByOwnerOrMemberWithMembers(@Param("user") User user);

    // 🥇 LEVEL 1: JOIN FETCH for all groups
    @Query("""
        SELECT DISTINCT g
        FROM Group g
        JOIN FETCH g.owner
        LEFT JOIN FETCH g.members
    """)
    List<Group> findAllWithMembers();

    boolean existsByInviteCode(String code);

    Optional<Group> findByInviteCode(String inviteCode);
} 