package com.example.cvadvisorplatform.controller.admin;


import com.example.cvadvisorplatform.dto.AdminUserCreateRequest;
import com.example.cvadvisorplatform.dto.AdminUserResponse;
import com.example.cvadvisorplatform.dto.AdminUserUpdateRequest;
import com.example.cvadvisorplatform.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

//    @GetMapping
//    public List<AdminUserResponse> getAll() {
//        return service.getAllUsers();
//    }
    @GetMapping
    public Page<AdminUserResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getAllUsers(search, role, enabled, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserResponse detail(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PostMapping(consumes = "multipart/form-data")
    public AdminUserResponse create(
            @RequestPart("data") AdminUserCreateRequest data,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        return service.createUser(data, avatar);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public AdminUserResponse update(
            @PathVariable Long id,
            @RequestPart("data") AdminUserUpdateRequest data,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar
    ) {
        return service.updateUser(id, data, avatar);
    }

    @PutMapping("/{id}/toggle-status")
    public void toggle(@PathVariable Long id) {
        service.toggleStatus(id);
    }
}
