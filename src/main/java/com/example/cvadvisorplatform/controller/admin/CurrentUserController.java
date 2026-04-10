package com.example.cvadvisorplatform.controller.admin;

import com.example.cvadvisorplatform.dto.CurrentUserResponse;
import com.example.cvadvisorplatform.dto.CurrentUserUpdateRequest;
import com.example.cvadvisorplatform.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService service;

    @GetMapping
    public CurrentUserResponse getProfile() {
        return service.getCurrentUser();
    }

    @PutMapping(consumes = "multipart/form-data")
    public CurrentUserResponse updateProfile(
            @RequestPart("data") CurrentUserUpdateRequest data,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        return service.updateCurrentUser(data, avatar);
    }
}
