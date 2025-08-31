package org.example.repository;

import org.example.entity.Issue;
import org.example.entity.Project;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueRepository extends JpaRepository<Issue,Long> {

    List<Issue> findByProject(Project project);

    List<Issue> findByProjectId(Long projectId);

    List<Issue> findByStatus(Issue.Status status);

    List<Issue> findByAssignedTo(User user);

    List<Issue> findByCreatedBy(User user);

}
