package org.example.group;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IssueResponse {
    private Long id;
    private String title;
    private String description;
    private String type;
    private String status;
    private String priority;
    private String assignedTo;
    private Long assignedToId;
    private String createdBy;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;
}
