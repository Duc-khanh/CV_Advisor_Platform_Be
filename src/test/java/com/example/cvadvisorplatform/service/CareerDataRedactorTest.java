package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.User;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CareerDataRedactorTest {
    private final CareerDataRedactor redactor = new CareerDataRedactor();

    @Test
    void removesCandidateIdentifiersAndContactDetails() {
        User user = new User();
        user.setFullName("Nguyen Van A");
        user.setEmail("candidate@example.com");
        user.setPhone("0987654321");
        user.setPersonalLink("https://example.com/me");
        String source = "Nguyen Van A candidate@example.com 0987654321 "
                + "https://example.com/me Java Spring Boot developer";

        String result = redactor.redact(source, user);

        assertThat(result).doesNotContain(
                "Nguyen Van A", "candidate@example.com", "0987654321", "https://example.com/me");
        assertThat(result).contains("Java Spring Boot developer");
    }
}
