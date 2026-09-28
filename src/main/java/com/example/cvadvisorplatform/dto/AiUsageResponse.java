package com.example.cvadvisorplatform.dto;

import java.time.LocalDateTime;

public record AiUsageResponse(
        Long subscriptionId,
        Long userId,
        String userEmail,
        String planCode,
        String planName,
        String status,
        int allocatedCredits,
        int bonusCredits,
        int usedCredits,
        int remainingCredits,
        long tokenLimit,
        long inputTokens,
        long outputTokens,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
) { }
