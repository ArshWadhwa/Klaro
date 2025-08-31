package org.example.group;


import lombok.Data;
import org.apache.catalina.User;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private String author;
    private LocalDateTime createdAt;

}
