package com.example.cvadvisorplatform.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentOrderResponse(
        Long id,
        String orderCode,
        String planCode,
        String planName,
        BigDecimal amount,
        String transferContent,
        String bankId,
        String bankName,
        String accountNo,
        String accountName,
        String qrUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
