package com.example.cvadvisorplatform.dto;

import java.time.LocalDateTime;

public record AiUsageLogResponse(
        Long id,
        Long userId,
        String email,
        String feature,
        String status,
        String model,
        int chargedCredits,
        int estimatedInputTokens,
        Integer inputTokens,
        Integer outputTokens,
        String errorCode,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) { }
