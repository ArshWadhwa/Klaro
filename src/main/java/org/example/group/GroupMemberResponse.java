package org.example.group;

import lombok.Data;

@Data
public class GroupMemberResponse {

    private Long id;
    private String fullName;
    private String email;
    private boolean owner;
} 