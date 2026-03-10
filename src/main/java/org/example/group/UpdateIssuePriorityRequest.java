package org.example.group;

import lombok.Data;

@Data
public class UpdateIssuePriorityRequest {
    private String priority; // LOW, MEDIUM, HIGH
}
