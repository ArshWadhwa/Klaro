package org.example.group;

import lombok.Data;

@Data

public class IssueResponse {

    private Long id;
    private String title;
    private String status;
    private String priority;
    private String assignedTo;
    private String createdBy;


}
