package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FileValidationService {
    private static final long DEFAULT_MAX_CV_SIZE = 10L * 1024 * 1024;
    private static final Set<String> CV_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final SystemSettingService systemSettingService;

    public void validateCv(MultipartFile file, boolean pdfOnly) {
        if (file == null || file.isEmpty()) invalid("CV_EMPTY", "File CV không được để trống.");
        long maxSizeBytes = systemSettingService != null ? systemSettingService.getMaxCvFileSize() : DEFAULT_MAX_CV_SIZE;
        if (file.getSize() > maxSizeBytes) {
            long maxMb = maxSizeBytes / (1024 * 1024);
            invalid("CV_TOO_LARGE", "File CV không được vượt quá " + maxMb + " MB.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!CV_EXTENSIONS.contains(ext) || (pdfOnly && !"pdf".equals(ext)))
            invalid("CV_TYPE_INVALID", pdfOnly ? "Chỉ chấp nhận file PDF." : "Chỉ chấp nhận PDF, DOC hoặc DOCX.");
        try {
            byte[] header = file.getInputStream().readNBytes(8);
            boolean pdf = starts(header, new int[]{0x25, 0x50, 0x44, 0x46});
            boolean doc = starts(header, new int[]{0xD0, 0xCF, 0x11, 0xE0});
            boolean docx = starts(header, new int[]{0x50, 0x4B, 0x03, 0x04});
            if ((pdfOnly && !pdf) || (!pdf && !doc && !docx)) invalid("CV_CONTENT_INVALID", "Nội dung file không đúng định dạng.");
        } catch (IOException ex) {
            invalid("CV_READ_FAILED", "Không thể đọc file CV.");
        }
    }

    private boolean starts(byte[] source, int[] signature) {
        if (source.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if ((source[i] & 0xff) != signature[i]) return false;
        return true;
    }
    private void invalid(String code, String message) {
        throw new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
