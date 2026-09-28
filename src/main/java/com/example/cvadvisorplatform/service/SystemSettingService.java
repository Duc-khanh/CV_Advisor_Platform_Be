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
        return (String) getAiSettings().getOrDefault("primaryFreeModel", "meta-llama/llama-3.3-70b-instruct:free");
    }

    public String getFallbackModel() {
        return (String) getAiSettings().getOrDefault("fallbackModel", "google/gemini-1.5-flash");
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

        // 1. AI Settings (Hỗ trợ Free Models kết hợp Fallback Gemini)
        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("strategy", "free_first"); // "free_first", "gemini_only", "free_only"
        ai.put("primaryFreeModel", "meta-llama/llama-3.3-70b-instruct:free");
        ai.put("fallbackModel", "google/gemini-1.5-flash");
        ai.put("availableFreeModels", List.of(
                Map.of("id", "meta-llama/llama-3.3-70b-instruct:free", "name", "Meta LLaMA 3.3 70B (Miễn phí - Thông minh)", "provider", "Meta / OpenRouter"),
                Map.of("id", "google/gemma-2-9b-it:free", "name", "Google Gemma 2 9B (Miễn phí - Siêu tốc)", "provider", "Google / OpenRouter"),
                Map.of("id", "deepseek/deepseek-r1:free", "name", "DeepSeek R1 Reasoning (Miễn phí - Suy luận sâu)", "provider", "DeepSeek / OpenRouter"),
                Map.of("id", "qwen/qwen-2.5-72b-instruct:free", "name", "Qwen 2.5 72B (Miễn phí - Đa ngôn ngữ)", "provider", "Alibaba / OpenRouter"),
                Map.of("id", "mistralai/mistral-7b-instruct:free", "name", "Mistral 7B Instruct (Miễn phí)", "provider", "Mistral AI")
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

        return defaults;
    }
}
