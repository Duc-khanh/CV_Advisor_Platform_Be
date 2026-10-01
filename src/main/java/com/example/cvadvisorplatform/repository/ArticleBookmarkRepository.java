package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.ArticleBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleBookmarkRepository extends JpaRepository<ArticleBookmark, Long> {
    Optional<ArticleBookmark> findByUserUserIdAndArticleArticleId(Long userId, Long articleId);
    boolean existsByUserUserIdAndArticleArticleId(Long userId, Long articleId);
    List<ArticleBookmark> findByUserUserIdOrderByBookmarkIdDesc(Long userId);
}
