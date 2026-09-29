package com.example.cvadvisorplatform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SETTINGS_FILE_PATH = "config/system_settings.json";

    private Map<String, Object> cachedSettings = null;

    public synchronized Map<String, Object> getSettings() {
        if (cachedSettings != null) {
            return new HashMap<>(cachedSettings);
        }

        File file = new File(SETTINGS_FILE_PATH);
        if (file.exists()) {
            try {
                cachedSettings = objectMapper.readValue(file, new TypeReference<Map<String, Object>>() {});
                return new HashMap<>(cachedSettings);
            } catch (IOException e) {
                log.warn("Không thể đọc file config {}, khởi tạo cấu hình mặc định: {}", SETTINGS_FILE_PATH, e.getMessage());
            }
        }

        cachedSettings = getDefaultSettings();
        saveSettings(cachedSettings);
        return new HashMap<>(cachedSettings);
    }

    public synchronized Map<String, Object> saveSettings(Map<String, Object> newSettings) {
        try {
            Map<String, Object> current = cachedSettings != null ? cachedSettings : getDefaultSettings();
            current.putAll(newSettings);
            cachedSettings = current;

            Path path = Paths.get(SETTINGS_FILE_PATH);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), cachedSettings);
            log.info("Đã lưu cấu hình hệ thống thành công vào {}", SETTINGS_FILE_PATH);
        } catch (IOException e) {
            log.error("Lỗi khi ghi file cấu hình hệ thống: {}", e.getMessage());
        }
        return getSettings();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAiSettings() {
        return (Map<String, Object>) getSettings().getOrDefault("ai", Collections.emptyMap());
    }

    public String getAiStrategy() {
        return (String) getAiSettings().getOrDefault("strategy", "free_first");
    }

    public String getPrimaryFreeModel() {
        return (String) getAiSettings().getOrDefault("primaryFreeModel", "openrouter/free");
    }

    public String getFallbackModel() {
        return (String) getAiSettings().getOrDefault("fallbackModel", "google/gemini-2.5-flash-lite");
    }

    public double getTemperature() {
        Object val = getAiSettings().get("temperature");
        if (val instanceof Number n) return n.doubleValue();
        return 0.4;
    }

    public int getMaxTokens() {
        Object val = getAiSettings().get("maxTokens");
        if (val instanceof Number n) return n.intValue();
        return 4096;
    }

    @SuppressWarnings("unchecked")
    public long getMaxCvFileSize() {
        Map<String, Object> storage = (Map<String, Object>) getSettings().getOrDefault("storage", Collections.emptyMap());
        Object val = storage.get("maxCvFileSizeMb");
        if (val instanceof Number n) return n.longValue() * 1024 * 1024;
        return 10L * 1024 * 1024;
    }

    @SuppressWarnings("unchecked")
    public boolean isMaintenanceMode() {
        Map<String, Object> general = (Map<String, Object>) getSettings().getOrDefault("general", Collections.emptyMap());
        return Boolean.TRUE.equals(general.get("maintenanceMode"));
    }

    @SuppressWarnings("unchecked")
    public String getMaintenanceMessage() {
        Map<String, Object> general = (Map<String, Object>) getSettings().getOrDefault("general", Collections.emptyMap());
        return (String) general.getOrDefault("maintenanceMessage", "Hệ thống đang tiến hành nâng cấp hạ tầng. Xin vui lòng quay lại sau ít phút!");
    }

    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> defaults = new LinkedHashMap<>();

        // 1. AI Settings (Hỗ trợ Free Models kết hợp Fallback Gemini & Llama)
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("strategy", "free_first"); // "free_first", "gemini_only", "free_only"
        ai.put("primaryFreeModel", "openrouter/free");
        ai.put("fallbackModel", "google/gemini-2.5-flash-lite");
        ai.put("availableFreeModels", List.of(
                Map.of("id", "openrouter/free", "name", "OpenRouter Free Router (Tự động chọn mô hình miễn phí khả dụng)", "provider", "OpenRouter"),
                Map.of("id", "google/gemma-4-31b-it:free", "name", "Google Gemma 4 31B (Miễn phí - Thông minh)", "provider", "Google / OpenRouter"),
                Map.of("id", "liquid/lfm-2.5-2.6b:free", "name", "Liquid LFM 2.5 (Miễn phí - Siêu tốc)", "provider", "LiquidAI / OpenRouter"),
                Map.of("id", "google/gemini-2.5-flash-lite", "name", "Google Gemini 2.5 Flash Lite (Siêu nhanh, khuyên dùng)", "provider", "Google / OpenRouter"),
                Map.of("id", "google/gemini-2.5-flash", "name", "Google Gemini 2.5 Flash (Chính xác cao, chuyên sâu)", "provider", "Google / OpenRouter"),
                Map.of("id", "meta-llama/llama-3.1-8b-instruct", "name", "Meta LLaMA 3.1 8B (Tiết kiệm, ổn định)", "provider", "Meta / OpenRouter"),
                Map.of("id", "meta-llama/llama-3.3-70b-instruct", "name", "Meta LLaMA 3.3 70B (Mạnh mẽ, toàn diện)", "provider", "Meta / OpenRouter")
        ));
        ai.put("temperature", 0.4);
        ai.put("topP", 0.9);
        ai.put("maxTokens", 4096);
        ai.put("safetyLevel", "BLOCK_LOW");
        ai.put("autoFallbackOnQuota", true);
        defaults.put("ai", ai);

        // 2. Storage Settings
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("maxCvFileSizeMb", 10);
        storage.put("maxAvatarFileSizeMb", 3);
        storage.put("allowedExtensions", List.of(".pdf", ".docx", ".doc"));
        storage.put("storagePath", "uploads/cv");
        storage.put("autoCleanupDays", 90);
        defaults.put("storage", storage);

        // 3. Security Settings
        Map<String, Object> security = new LinkedHashMap<>();
        security.put("jwtExpirationHours", 24);
        security.put("passwordMinLength", 8);
        security.put("requireSpecialChar", false);
        security.put("maxLoginAttempts", 5);
        security.put("lockoutDurationMinutes", 15);
        defaults.put("security", security);

        // 4. Email Settings
        Map<String, Object> email = new LinkedHashMap<>();
        email.put("smtpHost", "smtp.gmail.com");
        email.put("smtpPort", 587);
        email.put("senderName", "CV Advisor Platform");
        email.put("senderEmail", "no-reply@cvadvisor.vn");
        email.put("notifyCandidateOnApply", true);
        email.put("notifyHrOnNewCandidate", true);
        email.put("notifyOnAccountAction", true);
        defaults.put("email", email);

        // 5. General & Maintenance Settings
        Map<String, Object> general = new LinkedHashMap<>();
        general.put("platformName", "CV Advisor Platform");
        general.put("supportEmail", "support@cvadvisor.vn");
        general.put("hotline", "1900 6868");
        general.put("maintenanceMode", false);
        general.put("maintenanceMessage", "Hệ thống đang tiến hành nâng cấp hạ tầng định kỳ. Xin quý khách vui lòng quay lại sau ít phút!");
        defaults.put("general", general);

        // 6. Payment Settings (VietQR)
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("bankId", "MB");
        payment.put("bankName", "Ngân hàng TMCP Quân Đội (MB Bank)");
        payment.put("accountNo", "686825062005");
        payment.put("accountName", "NGUYEN DUC KHANH");
        defaults.put("payment", payment);

        return defaults;
    }
}
