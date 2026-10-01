package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "article_ratings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "article_id"})
    }
)
@Getter @Setter
public class ArticleRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ratingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false)
    private Integer ratingValue; // 1 to 5 stars
}
