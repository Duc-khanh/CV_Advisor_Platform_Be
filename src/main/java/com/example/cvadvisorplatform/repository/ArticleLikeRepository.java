package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {
    Optional<ArticleLike> findByUserUserIdAndArticleArticleId(Long userId, Long articleId);
    boolean existsByUserUserIdAndArticleArticleId(Long userId, Long articleId);
    long countByArticleArticleId(Long articleId);
}
