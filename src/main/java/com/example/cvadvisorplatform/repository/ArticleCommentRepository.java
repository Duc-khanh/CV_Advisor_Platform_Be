package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {
    List<ArticleComment> findByArticleArticleIdOrderByCreatedAtDesc(Long articleId);
    long countByArticleArticleId(Long articleId);
}
