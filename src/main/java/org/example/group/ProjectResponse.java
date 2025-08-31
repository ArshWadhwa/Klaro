package org.example.group;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String createdBy;
    private String groupName;
    private LocalDateTime createdAt;
}
