package org.example.group;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class GroupResponse {

    private Long id;
    private String name;
    private String description;
    private String ownerName;
    private String ownerEmail;
    private List<GroupMemberResponse> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int memberCount;
    private int projectCount;
} 