package com.example.cvadvisorplatform.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CvResponse {
    private Long cvId;
    private String fileName;
    private String cvText;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
    private LocalDateTime createdAt;
}
