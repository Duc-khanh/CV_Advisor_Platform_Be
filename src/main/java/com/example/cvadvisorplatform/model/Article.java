package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter @Setter
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String description;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private String category;

    @Column
    private String imageUrl;

    @Column
    private String readTime;

    @Column(nullable = false)
    private Integer viewsCount = 0;

    @Column(nullable = false)
    private Integer likesCount = 0;

    @Column(nullable = false)
    private String status = "PUBLISHED"; // DRAFT, PUBLISHED, HIDDEN

    @Column(nullable = false)
    private boolean isPinned = false;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
}
