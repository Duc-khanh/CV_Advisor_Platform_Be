package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/user/articles")
@RequiredArgsConstructor
public class UserArticleController {

    private final ArticleService articleService;

    @PostMapping("/{id}/like")
    public boolean toggleLike(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return articleService.toggleLike(id, principal.getUser().getUserId());
    }

    @PostMapping("/{id}/bookmark")
    public boolean toggleBookmark(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return articleService.toggleBookmark(id, principal.getUser().getUserId());
    }

    @PostMapping("/{id}/comment")
    public ArticleCommentResponse addComment(
            @PathVariable Long id,
            @RequestBody ArticleCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return articleService.addComment(id, principal.getUser().getUserId(), request);
    }

    @PostMapping("/{id}/rate")
    public Double rateArticle(
            @PathVariable Long id,
            @RequestBody ArticleRatingRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return articleService.rateArticle(id, principal.getUser().getUserId(), request);
    }

    @GetMapping("/bookmarked")
    public List<ArticleResponse> getBookmarked(@AuthenticationPrincipal UserPrincipal principal) {
        return articleService.getBookmarkedArticles(principal.getUser().getUserId());
    }
}
