package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/jobs/favorite")
@RequiredArgsConstructor
public class JobFavoriteController {

    private final JobFavoriteService service;

    @PostMapping("/{jobId}")
    public ResponseEntity<Void> toggleFavorite(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        service.toggleFavorite(principal.getUser().getUserId(), jobId);
        return ResponseEntity.ok().build();
    }

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

