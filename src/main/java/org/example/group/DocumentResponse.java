package org.example.group;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor


    public class DocumentResponse {
        private Long id;
        private String fileName;
        private String fileUrl;
        private Long fileSize;
        private Integer pageCount;
        private String summary;
        private String uploadedByName;
        private String uploadedByEmail;
        private LocalDateTime uploadedAt;
        private LocalDateTime updatedAt;
        private Long projectId;
private String projectName;

}
