package org.example.repository;

import org.example.entity.Group;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
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

    // ✅ USE @EntityGraph instead of manual JOIN FETCH
    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT g FROM Group g WHERE g.id = :id")
    Optional<Group> findByIdWithMembers(@Param("id") Long id);

    // GroupRepository.java

    @Query(value = "SELECT DISTINCT g.* FROM groups g " +
            "INNER JOIN group_members gm ON g.id = gm.group_id " +
            "WHERE gm.user_id = :userId",
            nativeQuery = true)
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

    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT DISTINCT g FROM Group g WHERE g.owner = :user OR :user MEMBER OF g.members")
    List<Group> findByOwnerOrMemberWithMembers(@Param("user") User user);

    @EntityGraph(attributePaths = {"members"})
    @Query("SELECT DISTINCT g FROM Group g")
    List<Group> findAllWithMembers();

    boolean existsByInviteCode(String code);

    Optional<Group> findByInviteCode(String inviteCode);
} 