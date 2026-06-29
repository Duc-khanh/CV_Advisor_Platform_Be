package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    // For public space: only published articles
    @Query("SELECT a FROM Article a WHERE a.status = 'PUBLISHED' " +
           "AND (:category IS NULL OR a.category = :category) " +
           "AND (:query IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(a.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Article> findPublishedArticles(
            @Param("category") String category,
            @Param("query") String query,
            Pageable pageable
    );

    // Get pinned articles that are published
    List<Article> findByIsPinnedTrueAndStatus(String status);

    // Get top trending published articles (based on views count)
    List<Article> findTop4ByStatusOrderByViewsCountDesc(String status);

    // Group categories with article counts
    @Query("SELECT a.category, COUNT(a) FROM Article a WHERE a.status = 'PUBLISHED' GROUP BY a.category")
    List<Object[]> countArticlesByCategory();
}
