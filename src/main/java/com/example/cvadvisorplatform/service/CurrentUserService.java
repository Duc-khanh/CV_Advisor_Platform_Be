package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CurrentUserResponse;
import com.example.cvadvisorplatform.dto.CurrentUserUpdateRequest;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.UserRepository;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.cvadvisorplatform.dto.ChangePasswordRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserService {

    private final CloudinaryService cloudinaryService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    public void changePassword(ChangePasswordRequest request) {
        if (request == null || request.getCurrentPassword() == null || request.getNewPassword() == null) {
            throw new RuntimeException("Vui lòng điền đầy đủ mật khẩu hiện tại và mật khẩu mới");
        }

        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Mật khẩu mới phải có tối thiểu 6 ký tự");
        }

        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Xác nhận mật khẩu mới không trùng khớp");
        }

        User user = getAuthenticatedUser();
        User freshUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), freshUser.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không chính xác");
        }

        freshUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(freshUser);
        log.info("Người dùng ID {} đổi mật khẩu thành công.", freshUser.getUserId());
    }

    public CurrentUserResponse getCurrentUser() {
        User user = getAuthenticatedUser();
        // Load fresh user details from DB to make sure we have all updated fields
        User freshUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        return mapToResponse(freshUser);
    }

    public CurrentUserResponse updateCurrentUser(
            CurrentUserUpdateRequest data,
            MultipartFile avatar
    ) {
        User user = getAuthenticatedUser();
        User freshUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (data.getFullName() != null && !data.getFullName().isBlank()) {
            freshUser.setFullName(data.getFullName());
        }

        if (data.getEmail() != null && !data.getEmail().isBlank()) {
            String newEmail = data.getEmail().trim().toLowerCase();
            // Chỉ cập nhật email nếu là email của chính user hoặc chưa bị dùng bởi user khác
            boolean emailBelongsToSelf = newEmail.equalsIgnoreCase(freshUser.getEmail());
            boolean emailTaken = !emailBelongsToSelf && userRepository.existsByEmail(newEmail);
            if (!emailTaken) {
                freshUser.setEmail(newEmail);
            } else {
                log.warn("Bỏ qua cập nhật email '{}' vì đã tồn tại trong hệ thống.", newEmail);
            }
        }

        if (avatar != null && !avatar.isEmpty()) {
            String avatarUrl = uploadAvatar(avatar);
            freshUser.setAvatar(avatarUrl);
        }

        // Cập nhật thông tin profile cơ bản
        if (data.getPhone() != null) freshUser.setPhone(data.getPhone());
        if (data.getHeadline() != null) freshUser.setHeadline(data.getHeadline());
        if (data.getLocation() != null) freshUser.setLocation(data.getLocation());
        if (data.getBio() != null) freshUser.setBio(data.getBio());
        if (data.getBirthday() != null) freshUser.setBirthday(data.getBirthday());
        if (data.getGender() != null) freshUser.setGender(data.getGender());
        if (data.getPersonalLink() != null) freshUser.setPersonalLink(data.getPersonalLink());

        // Cập nhật thông tin profile có cấu trúc (JSON strings)
        if (data.getSkills() != null) {
            freshUser.setSkills(writeJson(data.getSkills()));
        }
        if (data.getEducation() != null) {
            freshUser.setEducation(writeJson(data.getEducation()));
        }
        if (data.getExperience() != null) {
            freshUser.setExperience(writeJson(data.getExperience()));
        }
        if (data.getProjects() != null) {
            freshUser.setProjects(writeJson(data.getProjects()));
        }

        // Lưu thông tin vào database
        User savedUser = userRepository.save(freshUser);

        // Cập nhật lại đối tượng user bên trong Security Context Principal để đồng bộ
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Cập nhật thông tin người dùng trong principal nếu có setter hoặc load lại
        // Tuy nhiên do JwtFilter sẽ load lại mỗi request, việc lưu DB là quan trọng nhất.
        
        return mapToResponse(savedUser);
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
        List<String> skillsList = readJson(user.getSkills(), new TypeReference<List<String>>() {});
        List<Object> eduList = readJson(user.getEducation(), new TypeReference<List<Object>>() {});
        List<Object> expList = readJson(user.getExperience(), new TypeReference<List<Object>>() {});
        List<Object> projList = readJson(user.getProjects(), new TypeReference<List<Object>>() {});

        return new CurrentUserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getRoleName(),
                user.isEnabled(),
                user.getAvatar(),
                user.getAvatar(),
                user.getPhone(),
                user.getHeadline(),
                user.getLocation(),
                user.getBio(),
                user.getBirthday(),
                user.getGender(),
                user.getPersonalLink(),
                skillsList != null ? skillsList : new ArrayList<>(),
                eduList != null ? eduList : new ArrayList<>(),
                expList != null ? expList : new ArrayList<>(),
                projList != null ? projList : new ArrayList<>()
        );
    }

    private String writeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi đối tượng sang JSON string", e);
            return null;
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("Lỗi khi chuyển đổi JSON string sang đối tượng", e);
            return null;
        }
    }

    private String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return cloudinaryService.uploadFile(file, "cv_platform/avatars");
    }
}
