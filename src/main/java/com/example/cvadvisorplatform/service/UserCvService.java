package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCvRewriteRequest;
import com.example.cvadvisorplatform.dto.AiCvRewriteResponse;
import com.example.cvadvisorplatform.dto.CvRequest;
import com.example.cvadvisorplatform.dto.CvResponse;
import com.example.cvadvisorplatform.model.CV;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.CVRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCvService {

    private final CVRepository cvRepository;
    private final UserRepository userRepository;
    private final OpenRouterService openRouterService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final FileValidationService fileValidationService;

    @Value("${cv.private-upload-dir:private-uploads/cv}")
    private String privateUploadDir;

    public CvResponse uploadCv(Long userId, MultipartFile file) {
        fileValidationService.validateCv(file, false);

        String originalName = file.getOriginalFilename() == null ? "cv.pdf" : Paths.get(file.getOriginalFilename()).getFileName().toString();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!Set.of("pdf", "doc", "docx").contains(extension)) throw new RuntimeException("Chỉ hỗ trợ file PDF, DOC hoặc DOCX");

        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        String storedName = UUID.randomUUID() + "." + extension;
        Path directory = Paths.get(privateUploadDir).toAbsolutePath().normalize();
        Path target = directory.resolve(storedName).normalize();
        if (!target.startsWith(directory)) throw new RuntimeException("Tên file không hợp lệ");
        try {
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Không thể lưu file CV", ex);
        }

        CV cv = new CV();
        cv.setUser(user);
        cv.setFileName(originalName);
        cv.setCvText("");
        cv.setStoredFileName(storedName);
        cv.setFileSize(file.getSize());
        cv.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        cv.setCreatedAt(LocalDateTime.now());
        return mapToResponse(cvRepository.save(cv));
    }

    public CvFile getCvFile(Long userId, Long cvId) {
        CV cv = getOwnedCv(userId, cvId);
        if (cv.getStoredFileName() == null) throw new RuntimeException("CV này không có file đính kèm");
        Path directory = Paths.get(privateUploadDir).toAbsolutePath().normalize();
        Path file = directory.resolve(cv.getStoredFileName()).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) throw new RuntimeException("Không tìm thấy file CV");
        try {
            return new CvFile(new UrlResource(file.toUri()), cv.getFileName(), cv.getMimeType(), Files.size(file));
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Không thể đọc file CV", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Không thể xác định kích thước file CV", ex);
        }
    }

    private CV getOwnedCv(Long userId, Long cvId) {
        CV cv = cvRepository.findById(cvId).orElseThrow(() -> new RuntimeException("CV not found"));
        if (!cv.getUser().getUserId().equals(userId)) throw new RuntimeException("Bạn không có quyền truy cập CV này");
        return cv;
    }

    public CV getOwnedCvOrLatest(Long userId, Long cvId) {
        if (cvId != null) return getOwnedCv(userId, cvId);
        return cvRepository.findFirstByUser_UserIdOrderByCreatedAtDesc(userId).orElse(null);
    }

    public String resolveCvText(Long userId, CV cv) {
        if (cv == null) return "";
        if (cv.getUser() == null || !cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền truy cập CV này");
        }
        if (cv.getCvText() != null && !cv.getCvText().isBlank()) return cv.getCvText();
        if (cv.getStoredFileName() == null) return "";
        String name = cv.getFileName() == null ? "" : cv.getFileName().toLowerCase(Locale.ROOT);
        String type = cv.getMimeType() == null ? "" : cv.getMimeType().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".pdf") && !type.contains("pdf")) {
            throw new RuntimeException("Vui lòng sử dụng CV định dạng PDF để phân tích");
        }
        return extractAndStoreCvText(cv);
    }

    private String extractAndStoreCvText(CV cv) {
        Path directory = Paths.get(privateUploadDir).toAbsolutePath().normalize();
        Path file = directory.resolve(cv.getStoredFileName()).normalize();
        if (!file.startsWith(directory) || !Files.isRegularFile(file)) {
            throw new RuntimeException("Không tìm thấy file CV để phân tích");
        }
        return readPdfAndStore(cv, file);
    }

    private String readPdfAndStore(CV cv, Path file) {
        try (java.io.InputStream input = Files.newInputStream(file)) {
            String text = pdfTextExtractorService.extractText(input);
            if (text == null || text.isBlank()) throw new RuntimeException("CV PDF không có nội dung văn bản");
            cv.setCvText(text);
            cvRepository.save(cv);
            return text;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể trích xuất nội dung CV", ex);
        }
    }

    public record CvFile(Resource resource, String fileName, String mimeType, long size) { }
    public CvResponse saveCv(Long userId, CvRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CV cv = new CV();
        cv.setUser(user);
        cv.setFileName(request.getFileName() != null ? request.getFileName() : "Untitled CV");
        cv.setCvText(request.getCvText());
        cv.setCreatedAt(LocalDateTime.now());

        CV savedCv = cvRepository.save(cv);
        return mapToResponse(savedCv);
    }

    public CvResponse updateCv(Long userId, Long cvId, CvRequest request) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this CV");
        }

        cv.setFileName(request.getFileName() != null ? request.getFileName() : cv.getFileName());
        if (request.getCvText() != null) {
            cv.setCvText(request.getCvText());
        }

        CV updatedCv = cvRepository.save(cv);
        return mapToResponse(updatedCv);
    }

    public List<CvResponse> getAllCvsByUser(Long userId) {
        List<CV> cvs = cvRepository.findAllByUser_UserIdOrderByCreatedAtDesc(userId);
        return cvs.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public CvResponse getCvById(Long userId, Long cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to view this CV");
        }

        return mapToResponse(cv);
    }

    public AiCvRewriteResponse rewriteCvText(Long userId, AiCvRewriteRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new RuntimeException("Nội dung cần AI chỉnh sửa không được để trống");
        }

        String cvContext = "";
        if (request.getCvId() != null) {
            CV cv = cvRepository.findById(request.getCvId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy CV"));

            if (!cv.getUser().getUserId().equals(userId)) {
                throw new RuntimeException("Bạn không có quyền sử dụng CV này");
            }
            cvContext = cv.getCvText();
        }

        String rewrittenText = openRouterService.rewriteCvText(
                request.getText(),
                request.getTone(),
                cvContext
        );
        return new AiCvRewriteResponse(rewrittenText);
    }
    public void deleteCv(Long userId, Long cvId) {
        CV cv = cvRepository.findById(cvId)
                .orElseThrow(() -> new RuntimeException("CV not found"));

        if (!cv.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to delete this CV");
        }

        if (cv.getStoredFileName() != null) {
            Path directory = Paths.get(privateUploadDir).toAbsolutePath().normalize();
            Path file = directory.resolve(cv.getStoredFileName()).normalize();
            if (file.startsWith(directory)) {
                try { Files.deleteIfExists(file); } catch (IOException ignored) { }
            }
        }
        cvRepository.delete(cv);
    }

    private CvResponse mapToResponse(CV cv) {
        CvResponse response = new CvResponse();
        response.setCvId(cv.getCvId());
        response.setFileName(cv.getFileName());
        response.setCvText(cv.getCvText());
        response.setFileUrl(cv.getStoredFileName() == null ? null : "/api/user/cvs/" + cv.getCvId() + "/file");
        response.setFileSize(cv.getFileSize());
        response.setMimeType(cv.getMimeType());
        response.setCreatedAt(cv.getCreatedAt());
        return response;
    }
}
