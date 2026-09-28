package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.User;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class CareerDataRedactor {
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)(?:\\+?84|0)[\\s.()-]*(?:\\d[\\s.()-]*){8,10}(?!\\d)");
    private static final Pattern URL = Pattern.compile(
            "(?i)\\b(?:https?://|www\\.)\\S+");
    private static final Pattern IDENTIFIER_LINE = Pattern.compile(
            "(?im)^\\s*(?:họ tên|ho ten|full name|name|email|e-mail|điện thoại|dien thoai|phone|mobile|"
                    + "địa chỉ|dia chi|address|ngày sinh|ngay sinh|date of birth|dob|"
                    + "linkedin|github|website|portfolio)\\s*[:|-].*$");

    public String redact(String value, User user) {
        if (value == null || value.isBlank()) return "";
        String result = IDENTIFIER_LINE.matcher(value).replaceAll("[REDACTED_CONTACT]");
        result = EMAIL.matcher(result).replaceAll("[REDACTED_EMAIL]");
        result = PHONE.matcher(result).replaceAll("[REDACTED_PHONE]");
        result = URL.matcher(result).replaceAll("[REDACTED_URL]");
        result = replaceLiteral(result, user.getFullName(), "[CANDIDATE_NAME]");
        result = replaceLiteral(result, user.getEmail(), "[REDACTED_EMAIL]");
        result = replaceLiteral(result, user.getPhone(), "[REDACTED_PHONE]");
        result = replaceLiteral(result, user.getPersonalLink(), "[REDACTED_URL]");
        return result;
    }

    private String replaceLiteral(String source, String target, String replacement) {
        if (target == null || target.isBlank()) return source;
        return Pattern.compile(Pattern.quote(target), Pattern.CASE_INSENSITIVE)
                .matcher(source).replaceAll(replacement);
    }
}
