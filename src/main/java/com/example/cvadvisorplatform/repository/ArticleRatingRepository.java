package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.ArticleRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRatingRepository extends JpaRepository<ArticleRating, Long> {
    Optional<ArticleRating> findByUserUserIdAndArticleArticleId(Long userId, Long articleId);
    boolean existsByUserUserIdAndArticleArticleId(Long userId, Long articleId);

    @Query("SELECT AVG(r.ratingValue) FROM ArticleRating r WHERE r.article.articleId = :articleId")
    Double getAverageRating(@Param("articleId") Long articleId);
}
