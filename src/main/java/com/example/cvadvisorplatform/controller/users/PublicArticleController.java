package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final ArticleService articleService;

    @GetMapping
    public Page<ArticleResponse> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(defaultValue = "newest") String sortBy,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return articleService.getArticlesPublic(category, query, page, size, sortBy, userId);
    }

    @GetMapping("/trending")
    public List<ArticleResponse> getTrending(@AuthenticationPrincipal UserPrincipal principal) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return articleService.getTrendingArticles(userId);
    }

    @GetMapping("/tags")
    public List<Map<String, Object>> getPopularTags() {
        List<Object[]> tagsData = articleService.getPopularTags();
        return tagsData.stream().map(row -> {
            Map<String, Object> map = new HashMap<>();
            map.put("name", row[0]);
            map.put("count", row[1]);
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ArticleResponse getArticleDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return articleService.getArticleDetail(id, userId);
    }

    @GetMapping("/{id}/comments")
    public List<ArticleCommentResponse> getComments(@PathVariable Long id) {
        return articleService.getComments(id);
    }
}
