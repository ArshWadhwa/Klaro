package org.example.group;

import org.example.entity.Issue;
import jakarta.annotation.Priority;
import jakarta.transaction.Status;
import lombok.Data;

import java.time.LocalDateTime;


@Data
public class CreateIssueRequest {

    private Long projectId;
    private String title;
    private String description;
    private Issue.Priority priority;

    private Issue.Status status;
    private Issue.IssueType type;
    private Long assigneeId;

}
