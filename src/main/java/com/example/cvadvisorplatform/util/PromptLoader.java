package com.example.cvadvisorplatform.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Đọc các file prompt từ thư mục .prompts/ ở gốc dự án.
 *
 * Cấu trúc thư mục:
 *   .prompts/
 *   ├── system_prompt.md
 *   ├── chain_of_thought.md
 *   └── skills.md
 *
 * Cách dùng:
 *   String content = PromptLoader.load("system_prompt.md");
 */
@Slf4j
public class PromptLoader {

    /** Thư mục .prompts nằm ở gốc dự án (working directory khi chạy app) */
    private static final String PROMPTS_DIR = ".prompts";

    /**
     * Đọc nội dung một file prompt từ thư mục .prompts/.
     *
     * @param fileName tên file, ví dụ: "system_prompt.md", "chain_of_thought.md", "skills.md"
     * @return nội dung file dưới dạng String, hoặc "" nếu không tìm thấy / lỗi đọc file
     */
    public static String load(String fileName) {
        Path filePath = Paths.get(PROMPTS_DIR, fileName).toAbsolutePath();

        if (!Files.exists(filePath)) {
            log.error("Không tìm thấy file prompt: {}. Hãy đảm bảo thư mục .prompts/ tồn tại ở gốc dự án.", filePath);
            return "";
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            log.debug("Đã đọc prompt file: {}", filePath);
            return content;
        } catch (IOException e) {
            log.error("Lỗi khi đọc file prompt {}: {}", filePath, e.getMessage(), e);
            return "";
        }
    }
}
