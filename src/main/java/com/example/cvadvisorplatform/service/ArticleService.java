package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.*;
import org.springframework.data.domain.Page;
import java.util.List;

public interface ArticleService {
    Page<ArticleResponse> getArticlesPublic(String category, String query, int page, int size, String sortBy, Long currentUserId);
    List<ArticleResponse> getArticlesAdmin();
    ArticleResponse getArticleDetail(Long articleId, Long currentUserId);
    ArticleResponse createArticle(ArticleCreateRequest request, Long authorId);
    ArticleResponse updateArticle(Long articleId, ArticleUpdateRequest request);
    void deleteArticle(Long articleId);
    
    boolean toggleLike(Long articleId, Long userId);
    boolean toggleBookmark(Long articleId, Long userId);
    ArticleCommentResponse addComment(Long articleId, Long userId, ArticleCommentRequest request);
    List<ArticleCommentResponse> getComments(Long articleId);
    Double rateArticle(Long articleId, Long userId, ArticleRatingRequest request);
    
    List<ArticleResponse> getTrendingArticles(Long currentUserId);
    List<Object[]> getPopularTags();
    List<ArticleResponse> getBookmarkedArticles(Long userId);
}
