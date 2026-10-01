package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/jobs/favorite")
@RequiredArgsConstructor
public class JobFavoriteController {

    private final JobFavoriteService service;

    /**
     * Thêm công việc vào danh sách yêu thích.
     * Trả về 409 Conflict nếu công việc đã có trong danh sách yêu thích.
     */
    @PostMapping("/add/{jobId}")
    public ResponseEntity<?> addFavorite(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa xác thực người dùng"));
        }

        try {
            service.addFavorite(principal.getUser().getUserId(), jobId);
            return ResponseEntity.ok(Map.of("message", "Đã thêm vào danh sách yêu thích"));
        } catch (RuntimeException e) {
            if ("ALREADY_FAVORITED".equals(e.getMessage())) {
                return ResponseEntity.status(409)
                        .body(Map.of("message", "Công việc đã có trong danh sách việc làm yêu thích"));
            }
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi hệ thống"));
        }
    }

    /**
     * Xóa công việc khỏi danh sách yêu thích.
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Chưa xác thực người dùng"));
        }

        try {
            service.removeFavorite(principal.getUser().getUserId(), jobId);
            return ResponseEntity.ok(Map.of("message", "Đã bỏ khỏi danh sách yêu thích"));
        } catch (RuntimeException e) {
            if ("NOT_FAVORITED".equals(e.getMessage())) {
                return ResponseEntity.status(404)
                        .body(Map.of("message", "Công việc không có trong danh sách yêu thích"));
            }
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi hệ thống"));
        }
    }

    /**
     * Kiểm tra trạng thái yêu thích của một công việc.
     */
    @GetMapping("/{jobId}/status")
    public ResponseEntity<Boolean> checkFavoriteStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.ok(false);
        }

        return ResponseEntity.ok(
                service.isFavorited(principal.getUser().getUserId(), jobId)
        );
    }

    /**
     * Lấy toàn bộ danh sách công việc yêu thích của user hiện tại.
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllFavorites(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Chưa xác thực người dùng");
        }

        return ResponseEntity.ok(
                service.getListFavorites(principal.getUser().getUserId())
        );
    }
}
