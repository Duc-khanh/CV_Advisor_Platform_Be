package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentResponse {
    private Long commentId;
    private Long userId;
    private String userFullName;
    private String userAvatar;
    private String content;
    private LocalDateTime createdAt;
}
