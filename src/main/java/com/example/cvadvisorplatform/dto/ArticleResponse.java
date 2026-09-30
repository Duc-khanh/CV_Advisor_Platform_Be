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

    public ArticleResponse(
            Long articleId,
            String title,
            String description,
            String content,
            String category,
            String imageUrl,
            String readTime,
            Integer viewsCount,
            Integer likesCount,
            String status,
            boolean isPinned,
            LocalDateTime createdAt,
            String authorName,
            String authorAvatar,
            Double averageRating,
            long commentsCount
    ) {
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.content = content;
        this.category = category;
        this.imageUrl = imageUrl;
        this.readTime = readTime;
        this.viewsCount = viewsCount;
        this.likesCount = likesCount;
        this.status = status;
        this.isPinned = isPinned;
        this.createdAt = createdAt;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.averageRating = averageRating;
        this.commentsCount = commentsCount;
        this.isLiked = false;
        this.isBookmarked = false;
        this.userRating = null;
    }
}
