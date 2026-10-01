package com.example.cvadvisorplatform.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload một file (ảnh hoặc PDF) lên Cloudinary.
     *
     * @param file   MultipartFile cần upload
     * @param folder Thư mục trên Cloudinary (vd: "cv_platform/avatars")
     * @return secure_url của file đã upload
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto"
            );
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi upload file lên Cloudinary: " + e.getMessage(), e);
        }
    }

    public String uploadFile(byte[] content, String fileName, String folder) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "auto",
                    "filename_override", fileName
            );
            Map uploadResult = cloudinary.uploader().upload(content, params);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Failed to upload stored CV to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload stored CV: " + e.getMessage(), e);
        }
    }

    /**
     * Tải nội dung file từ một URL Cloudinary về dạng InputStream.
     * Dùng để đọc CV PDF đã upload lên Cloudinary.
     *
     * @param fileUrl URL public/secure của file trên Cloudinary
     * @return InputStream của file
     */
    public InputStream downloadFileAsStream(String fileUrl) {
        try {
            return new URL(fileUrl).openStream();
        } catch (IOException e) {
            log.error("Không thể tải file từ URL: {}", fileUrl, e);
            throw new RuntimeException("Không thể tải file CV từ Cloudinary URL: " + fileUrl, e);
        }
    }
}
