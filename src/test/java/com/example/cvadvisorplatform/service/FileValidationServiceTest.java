package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class FileValidationServiceTest {
    private final FileValidationService service = new FileValidationService();

    @Test
    void acceptsRealPdfSignature() {
        byte[] content = new byte[]{'%', 'P', 'D', 'F', '-', '1', '.', '7'};
        MockMultipartFile file = new MockMultipartFile("cv", "resume.pdf", "application/pdf", content);
        assertDoesNotThrow(() -> service.validateCv(file, true));
    }

    @Test
    void rejectsFakePdf() {
        MockMultipartFile file = new MockMultipartFile("cv", "resume.pdf", "application/pdf", "not pdf".getBytes());
        ApiException error = assertThrows(ApiException.class, () -> service.validateCv(file, true));
        assertEquals("CV_CONTENT_INVALID", error.getCode());
    }

    @Test
    void rejectsDocWhenPdfIsRequired() {
        byte[] ole = new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0};
        MockMultipartFile file = new MockMultipartFile("cv", "resume.doc", "application/msword", ole);
        assertThrows(ApiException.class, () -> service.validateCv(file, true));
    }
}
