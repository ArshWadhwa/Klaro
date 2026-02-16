package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Group name is required")
    @Size(min = 2, max = 100, message = "Group name must be between 2 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @ToString.Exclude // Prevent circular reference in toString()
    @ManyToOne(fetch = FetchType.LAZY)  // Changed to LAZY - use JOIN FETCH in queries when needed
    @JoinColumn(name = "owner_id")
    private User owner;

    @ToString.Exclude // Multi-tenant: Organization this group belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = true)
    private Organization organization;

    @ToString.Exclude // Prevent circular reference in toString()
    @ManyToMany(fetch = FetchType.LAZY)  // Changed to LAZY - use JOIN FETCH in queries when needed
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
 // ← ADD THIS
    @JsonIgnoreProperties("groups")
    private Set<User> members = new HashSet<>();

    @ToString.Exclude // Prevent circular reference in toString()
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Project> projects;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(unique = true, nullable = false, length = 12)
    private String inviteCode;

    // Helper methods
    public void addMember(User user) {
        this.members.add(user);
    }

    public void removeMember(User user) {
        this.members.remove(user);
    }

    public boolean isMember(User user) {
        return this.members.contains(user);
    }

    public boolean isOwner(User user) {
        return this.owner.equals(user);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        Group group = (Group) o;
        return id != null && id.equals(group.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}