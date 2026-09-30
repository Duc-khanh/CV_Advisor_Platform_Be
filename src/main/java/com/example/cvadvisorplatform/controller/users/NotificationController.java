package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.NotificationResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean unreadOnly
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUser().getUserId();
        List<NotificationResponse> list = notificationService.getNotifications(userId, type, unreadOnly);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.ok(Collections.singletonMap("unreadCount", 0L));
        }
        Long userId = principal.getUser().getUserId();
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Collections.singletonMap("unreadCount", unreadCount));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUser().getUserId();
        NotificationResponse updated = notificationService.markAsRead(userId, id);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUser().getUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã đánh dấu tất cả thông báo là đã đọc"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUser().getUserId();
        notificationService.deleteNotification(userId, id);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa thông báo thành công"));
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<Map<String, String>> clearAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId = principal.getUser().getUserId();
        notificationService.clearAll(userId);
        return ResponseEntity.ok(Collections.singletonMap("message", "Đã xóa toàn bộ thông báo"));
    }
}
