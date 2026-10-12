package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.exception.ApiException;
import com.example.cvadvisorplatform.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentControllerTest {

    private PaymentService paymentService;
    private PaymentController controller;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        controller = new PaymentController(paymentService);
        ReflectionTestUtils.setField(controller, "webhookSecret", "test-secret");
    }

    @Test
    void acceptsCassoSecureTokenHeader() {
        Map<String, Object> payload = Map.of("error", 0, "data", java.util.List.of());
        Map<String, Object> expected = Map.of("success", true);
        when(paymentService.processWebhook(payload)).thenReturn(expected);

        Map<String, Object> response = controller.handleWebhook("test-secret", null, payload);

        assertEquals(expected, response);
        verify(paymentService).processWebhook(payload);
    }

    @Test
    void keepsLegacyWebhookHeaderCompatible() {
        Map<String, Object> payload = Map.of("error", 0, "data", java.util.List.of());
        when(paymentService.processWebhook(payload)).thenReturn(Map.of("success", true));

        controller.handleWebhook(null, "test-secret", payload);

        verify(paymentService).processWebhook(payload);
    }

    @Test
    void rejectsInvalidWebhookSecret() {
        ApiException error = assertThrows(
                ApiException.class,
                () -> controller.handleWebhook("wrong-secret", null, Map.of("error", 0))
        );

        assertEquals("INVALID_WEBHOOK_SECRET", error.getCode());
    }
}
