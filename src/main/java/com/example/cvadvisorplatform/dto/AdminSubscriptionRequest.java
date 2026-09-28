package com.example.cvadvisorplatform.dto;

import com.example.cvadvisorplatform.model.SubscriptionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AdminSubscriptionRequest(
        @NotBlank String planCode,
        SubscriptionStatus status,
        @Min(0) Integer bonusCredits,
        boolean resetPeriod
) { }
