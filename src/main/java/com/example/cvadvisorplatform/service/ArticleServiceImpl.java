package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleCommentRepository commentRepository;
    private final ArticleLikeRepository likeRepository;
    private final ArticleBookmarkRepository bookmarkRepository;
    private final ArticleRatingRepository ratingRepository;
    private final UserRepository userRepository;

    @Override
    public Page<ArticleResponse> getArticlesPublic(String category, String query, int page, int size, String sortBy, Long currentUserId) {
        Sort sort;
        if ("views".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "viewsCount");
        } else if ("likes".equalsIgnoreCase(sortBy)) {
            sort = Sort.by(Sort.Direction.DESC, "likesCount");
        } else {
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Handle "Tất cả" category as null
        String categoryFilter = ("Tất cả".equalsIgnoreCase(category) || category == null || category.trim().isEmpty()) ? null : category;
        String queryFilter = (query == null || query.trim().isEmpty()) ? null : query;

        Page<Article> articles = articleRepository.findPublishedArticles(categoryFilter, queryFilter, pageable);
        return articles.map(article -> mapToResponse(article, currentUserId));
    }

    @Override
    public List<ArticleResponse> getArticlesAdmin() {
        List<Article> articles = articleRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return articles.stream()
                .map(article -> mapToResponse(article, null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ArticleResponse getArticleDetail(Long articleId, Long currentUserId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + articleId));

        // Increment view count
        article.setViewsCount(article.getViewsCount() + 1);
        articleRepository.save(article);

        return mapToResponse(article, currentUserId);
    }

    @Override
    @Transactional
    public ArticleResponse createArticle(ArticleCreateRequest request, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tác giả với ID: " + authorId));

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setImageUrl(request.getImageUrl());
        article.setReadTime(request.getReadTime());
        article.setPinned(request.isPinned());
        article.setStatus(request.getStatus() != null ? request.getStatus() : "PUBLISHED");
        article.setAuthor(author);

        Article saved = articleRepository.save(article);
        return mapToResponse(saved, null);
    }

    @Override
    @Transactional
    public ArticleResponse updateArticle(Long articleId, ArticleUpdateRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết với ID: " + articleId));

        article.setTitle(request.getTitle());
        article.setDescription(request.getDescription());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        if (request.getImageUrl() != null) {
            article.setImageUrl(request.getImageUrl());
        }
        article.setReadTime(request.getReadTime());
        article.setPinned(request.isPinned());
        article.setStatus(request.getStatus());

        Article saved = articleRepository.save(article);
        return mapToResponse(saved, null);
    }

    @Override
    @Transactional
    public void deleteArticle(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new RuntimeException("Không tìm thấy bài viết với ID: " + articleId);
        }
        articleRepository.deleteById(articleId);
    }

    @Override
    @Transactional
    public boolean toggleLike(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Optional<ArticleLike> existing = likeRepository.findByUserUserIdAndArticleArticleId(userId, articleId);
        boolean liked;
        if (existing.isPresent()) {
            likeRepository.delete(existing.get());
            article.setLikesCount(Math.max(0, article.getLikesCount() - 1));
            liked = false;
        } else {
            ArticleLike like = new ArticleLike();
            like.setUser(user);
            like.setArticle(article);
            likeRepository.save(like);
            article.setLikesCount(article.getLikesCount() + 1);
            liked = true;
        }
        articleRepository.save(article);
        return liked;
    }

    @Override
    @Transactional
    public boolean toggleBookmark(Long articleId, Long userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        Optional<ArticleBookmark> existing = bookmarkRepository.findByUserUserIdAndArticleArticleId(userId, articleId);
        boolean bookmarked;
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            bookmarked = false;
        } else {
            ArticleBookmark bookmark = new ArticleBookmark();
            bookmark.setUser(user);
            bookmark.setArticle(article);
            bookmarkRepository.save(bookmark);
            bookmarked = true;
        }
        return bookmarked;
    }

    @Override
    @Transactional
    public ArticleCommentResponse addComment(Long articleId, Long userId, ArticleCommentRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        ArticleComment comment = new ArticleComment();
        comment.setArticle(article);
        comment.setUser(user);
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());

        ArticleComment saved = commentRepository.save(comment);
        
        return new ArticleCommentResponse(
                saved.getCommentId(),
                user.getUserId(),
                user.getFullName(),
                user.getAvatar(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Override
    public List<ArticleCommentResponse> getComments(Long articleId) {
        List<ArticleComment> comments = commentRepository.findByArticleArticleIdOrderByCreatedAtDesc(articleId);
        return comments.stream().map(c -> new ArticleCommentResponse(
                c.getCommentId(),
                c.getUser().getUserId(),
                c.getUser().getFullName(),
                c.getUser().getAvatar(),
                c.getContent(),
                c.getCreatedAt()
        )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Double rateArticle(Long articleId, Long userId, ArticleRatingRequest request) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài viết"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (request.getRatingValue() < 1 || request.getRatingValue() > 5) {
            throw new RuntimeException("Giá trị đánh giá phải nằm trong khoảng từ 1 đến 5 sao");
        }

        Optional<ArticleRating> existing = ratingRepository.findByUserUserIdAndArticleArticleId(userId, articleId);
        if (existing.isPresent()) {
            ArticleRating rating = existing.get();
            rating.setRatingValue(request.getRatingValue());
            ratingRepository.save(rating);
        } else {
            ArticleRating rating = new ArticleRating();
            rating.setUser(user);
            rating.setArticle(article);
            rating.setRatingValue(request.getRatingValue());
            ratingRepository.save(rating);
        }

        Double avg = ratingRepository.getAverageRating(articleId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public List<ArticleResponse> getTrendingArticles(Long currentUserId) {
        List<Article> articles = articleRepository.findTop4ByStatusOrderByViewsCountDesc("PUBLISHED");
        return articles.stream()
                .map(article -> mapToResponse(article, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Object[]> getPopularTags() {
        return articleRepository.countArticlesByCategory();
    }

    @Override
    public List<ArticleResponse> getBookmarkedArticles(Long userId) {
        List<ArticleBookmark> bookmarks = bookmarkRepository.findByUserUserIdOrderByBookmarkIdDesc(userId);
        return bookmarks.stream()
                .map(b -> mapToResponse(b.getArticle(), userId))
                .collect(Collectors.toList());
    }

    private ArticleResponse mapToResponse(Article article, Long currentUserId) {
        ArticleResponse res = new ArticleResponse();
        res.setArticleId(article.getArticleId());
        res.setTitle(article.getTitle());
        res.setDescription(article.getDescription());
        res.setContent(article.getContent());
        res.setCategory(article.getCategory());
        res.setImageUrl(article.getImageUrl());
        res.setReadTime(article.getReadTime());
        res.setViewsCount(article.getViewsCount());
        res.setLikesCount(article.getLikesCount());
        res.setStatus(article.getStatus());
        res.setPinned(article.isPinned());
        res.setCreatedAt(article.getCreatedAt());

        if (article.getAuthor() != null) {
            res.setAuthorName(article.getAuthor().getFullName());
            res.setAuthorAvatar(article.getAuthor().getAvatar());
        }

        // Get comments count
        res.setCommentsCount(commentRepository.countByArticleArticleId(article.getArticleId()));

        // Get average rating
        Double avgRating = ratingRepository.getAverageRating(article.getArticleId());
        res.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);

        // Get user-specific interaction
        if (currentUserId != null) {
            res.setLiked(likeRepository.existsByUserUserIdAndArticleArticleId(currentUserId, article.getArticleId()));
            res.setBookmarked(bookmarkRepository.existsByUserUserIdAndArticleArticleId(currentUserId, article.getArticleId()));
            
            Optional<ArticleRating> userRatingOpt = ratingRepository.findByUserUserIdAndArticleArticleId(currentUserId, article.getArticleId());
            res.setUserRating(userRatingOpt.map(ArticleRating::getRatingValue).orElse(null));
        } else {
            res.setLiked(false);
            res.setBookmarked(false);
            res.setUserRating(null);
        }

        return res;
    }
}
