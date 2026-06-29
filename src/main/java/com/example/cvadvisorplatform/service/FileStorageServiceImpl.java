package com.example.cvadvisorplatform.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private final CloudinaryService cloudinaryService;

    @Override
    public String storeJobImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Nếu có Cloudinary, sử dụng Cloudinary làm bộ lưu trữ đám mây
        if (cloudinaryService.isConfigured()) {
            return cloudinaryService.uploadFile(file, "cv_platform/jobs");
        }

        // 2. Chế độ dự phòng Local Fallback
        try {
            Path targetDir = Paths.get(uploadDir, "jobs");
            Files.createDirectories(targetDir);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = targetDir.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/jobs/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Upload ảnh lên bộ lưu trữ cục bộ thất bại", e);
        }
    }
}
