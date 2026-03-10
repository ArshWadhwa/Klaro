package org.example.group;

import lombok.Data;
import org.example.entity.Issue;

@Data
public class UpdateIssueStatusRequest {
    private Issue.Status status;

    // TO_DO, IN_PROGRESS, DONE
}
