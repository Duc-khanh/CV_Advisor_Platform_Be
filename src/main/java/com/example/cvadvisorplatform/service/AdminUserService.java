package com.example.cvadvisorplatform.service;


import com.example.cvadvisorplatform.dto.AdminUserCreateRequest;
import com.example.cvadvisorplatform.dto.AdminUserResponse;
import com.example.cvadvisorplatform.dto.AdminUserUpdateRequest;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Role;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.CompanyRepository;
import com.example.cvadvisorplatform.repository.RoleRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final CloudinaryService cloudinaryService;
    private final CompanyRepository companyRepository;

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir:uploads}")
    private String uploadDir;

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
        
        if (req.getCompanyId() != null) {
            Company company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            user.setCompany(company);
        }

        if ("HR".equals(role.getRoleName())) {
            user.setHrApprovalStatus("APPROVED");
        }

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

        if (req.getRole() != null) {
            Role role = getRole(req.getRole());
            user.setRole(role);
            if ("HR".equals(role.getRoleName()) && user.getHrApprovalStatus() == null) {
                user.setHrApprovalStatus("APPROVED");
            }
            if (!"HR".equals(role.getRoleName())) {
                user.setCompany(null);
            }
        }

        if (req.getCompanyId() != null) {
            Company company = companyRepository.findById(req.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            user.setCompany(company);
        }

        if (req.getPassword() != null && !req.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }

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

    /* ===== PHÊ DUYỆT NHÀ TUYỂN DỤNG ===== */
    public AdminUserResponse approveHrStatus(Long id, String status) {
        User user = findUser(id);
        if (!"HR".equals(user.getRole().getRoleName())) {
            throw new RuntimeException("Chỉ có thể phê duyệt cho tài khoản Nhà tuyển dụng (HR)");
        }

        if ("APPROVED".equalsIgnoreCase(status)) {
            user.setHrApprovalStatus("APPROVED");
            user.setEnabled(true);
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            user.setHrApprovalStatus("REJECTED");
            user.setEnabled(false);
        } else {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }

        userRepository.save(user);
        return toDto(user);
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
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Sử dụng Cloudinary nếu có cấu hình
        if (cloudinaryService.isConfigured()) {
            return cloudinaryService.uploadFile(file, "cv_platform/avatars");
        }

        // 2. Chế độ dự phòng Local Fallback
        try {
            Path targetDir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(targetDir);

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = targetDir.resolve(fileName);

            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/avatars/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Upload avatar thất bại: " + e.getMessage(), e);
        }
    }

    private AdminUserResponse toDto(User user) {
        AdminUserResponse dto = new AdminUserResponse();
        dto.setUserId(user.getUserId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().getRoleName());
        dto.setAvatar(user.getAvatar());
        dto.setAvatarUrl(user.getAvatar());
        dto.setEnabled(user.isEnabled());
        dto.setHrApprovalStatus(user.getHrApprovalStatus());
        if (user.getCompany() != null) {
            dto.setCompanyName(user.getCompany().getCompanyName());
            dto.setCompanyId(user.getCompany().getCompanyId());
        }
        return dto;
    }
    public Page<AdminUserResponse> getAllUsers(String search, String role, Boolean enabled, Boolean excludeHr, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "userId"));

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

        if (excludeHr != null && excludeHr) {
            spec = spec.and((root, query, cb) ->
                    cb.notEqual(root.get("role").get("roleName"), "HR"));
        }

        if (enabled != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("enabled"), enabled));
        }

        return userRepository.findAll(spec, pageable).map(this::toDto);
    }
}

