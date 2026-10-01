package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class ArticleCreateRequest {
    private String title;
    private String description;
    private String content;
    private String category;
    private String imageUrl;
    private String readTime;
    private boolean isPinned;
    private String status; // DRAFT, PUBLISHED, HIDDEN
}
