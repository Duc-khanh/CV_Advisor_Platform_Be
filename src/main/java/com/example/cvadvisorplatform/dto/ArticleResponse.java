package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Long articleId;
    private String title;
    private String description;
    private String content;
    private String category;
    private String imageUrl;
    private String readTime;
    private Integer viewsCount;
    private Integer likesCount;
    private String status;
    private boolean isPinned;
    private LocalDateTime createdAt;
    
    // Author info
    private String authorName;
    private String authorAvatar;
    
    // Interaction metadata
    private Double averageRating;
    private long commentsCount;
    private boolean isLiked;
    private boolean isBookmarked;
    private Integer userRating;
}
