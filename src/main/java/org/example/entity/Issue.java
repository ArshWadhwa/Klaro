package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue")
@Data
public class Issue {

    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status { TO_DO, IN_PROGRESS, DONE }
    public enum IssueType { BUG, FEATURE, TASK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private Status status = Status.TO_DO;

    @Enumerated(EnumType.STRING)
    private IssueType type = IssueType.TASK;

    @ToString.Exclude // Prevent circular reference in toString()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ToString.Exclude // Multi-tenant: Organization this issue belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    private Organization organization;

    @ToString.Exclude // Prevent circular reference in toString()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @ToString.Exclude // Prevent circular reference in toString()
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
