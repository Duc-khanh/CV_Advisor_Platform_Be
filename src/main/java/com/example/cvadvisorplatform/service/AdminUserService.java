package com.example.cvadvisorplatform.service;


import com.example.cvadvisorplatform.dto.AdminUserCreateRequest;
import com.example.cvadvisorplatform.dto.AdminUserResponse;
import com.example.cvadvisorplatform.dto.AdminUserUpdateRequest;
import com.example.cvadvisorplatform.model.Role;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.RoleRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final String UPLOAD_DIR = "uploads/avatars/";

    /* ===== DANH SÁCH ===== */
    public List<AdminUserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).toList();
    }

    /* ===== CHI TIẾT ===== */
    public AdminUserResponse getUser(Long id) {
        return toDto(findUser(id));
    }

    /* ===== THÊM USER (CÓ AVATAR) ===== */
    public AdminUserResponse createUser(
            AdminUserCreateRequest req,
            MultipartFile avatar
    ) {
        Role role = getRole(req.getRole());

        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(role);

        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(saveAvatar(avatar));
        }

        userRepository.save(user);
        return toDto(user);
    }

    /* ===== SỬA USER (CÓ AVATAR) ===== */
    public AdminUserResponse updateUser(
            Long id,
            AdminUserUpdateRequest req,
            MultipartFile avatar
    ) {
        User user = findUser(id);

        if (req.getFullName() != null)
            user.setFullName(req.getFullName());

        if (req.getEmail() != null)
            user.setEmail(req.getEmail());

        if (req.getRole() != null)
            user.setRole(getRole(req.getRole()));

        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(saveAvatar(avatar));
        }

        userRepository.save(user);
        return toDto(user);
    }

    /* ===== KHÓA / MỞ ===== */
    public void toggleStatus(Long id) {
        User user = findUser(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }

    /* ===== HELPER ===== */
    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Role getRole(String roleName) {
        return roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    private String saveAvatar(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/avatars/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Upload avatar failed");
        }
    }

    private AdminUserResponse toDto(User user) {
        AdminUserResponse dto = new AdminUserResponse();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getRoleName());
        dto.setAvatar(user.getAvatar());
        dto.setEnabled(user.isEnabled());
        return dto;
    }
    public Page<AdminUserResponse> getAllUsers(String search, String role, Boolean enabled, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<User> spec = Specification.where(null);

        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(root.get("fullName"), "%" + search + "%"),
                            cb.like(root.get("email"), "%" + search + "%")
                    )
            );
        }

        if (role != null && !role.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("role").get("roleName"), role));
        }

        if (enabled != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("enabled"), enabled));
        }

        return userRepository.findAll(spec, pageable).map(this::toDto);
    }
}

