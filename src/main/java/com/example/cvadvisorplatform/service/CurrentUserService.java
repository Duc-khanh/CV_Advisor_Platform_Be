package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CurrentUserResponse;
import com.example.cvadvisorplatform.dto.CurrentUserUpdateRequest;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    public CurrentUserResponse getCurrentUser() {
        User user = getAuthenticatedUser();
        return mapToResponse(user);
    }

    public CurrentUserResponse updateCurrentUser(
            CurrentUserUpdateRequest data,
            MultipartFile avatar
    ) {
        User user = getAuthenticatedUser();

        if (data.getFullName() != null && !data.getFullName().isBlank()) {
            user.setFullName(data.getFullName());
        }

        if (data.getEmail() != null && !data.getEmail().isBlank()) {
            user.setEmail(data.getEmail());
        }

        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = uploadAvatar(avatar);
            user.setAvatar(avatarUrl);
        }

        return mapToResponse(user);
    }

    /**
     * Lấy User hiện tại từ SecurityContext
     */
    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new RuntimeException("Unauthorized");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return principal.getUser();
    }

    private CurrentUserResponse mapToResponse(User user) {
        return new CurrentUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleName(),
                user.isEnabled(),
                user.getAvatar()
        );
    }

    private String uploadAvatar(MultipartFile file) {
        return "/uploads/" + file.getOriginalFilename();
    }


}
