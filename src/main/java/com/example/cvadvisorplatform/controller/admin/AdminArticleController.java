package com.example.cvadvisorplatform.controller.admin;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.ArticleService;
import com.example.cvadvisorplatform.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final FileStorageService fileStorageService;

    @GetMapping
    public List<ArticleResponse> getArticles() {
        return articleService.getArticlesAdmin();
    }

    @PostMapping
    public ArticleResponse createArticle(
            @RequestBody ArticleCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return articleService.createArticle(request, principal.getUser().getUserId());
    }

    @PutMapping("/{id}")
    public ArticleResponse updateArticle(
            @PathVariable Long id,
            @RequestBody ArticleUpdateRequest request
    ) {
        return articleService.updateArticle(id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteArticle(@PathVariable Long id) {
        articleService.deleteArticle(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Xóa bài viết thành công");
        return response;
    }

    @PostMapping("/upload-image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeArticleImage(file);
        Map<String, String> response = new HashMap<>();
        response.put("imageUrl", url);
        return response;
    }
}
