package com.example.cvadvisorplatform.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AdminAiPlanRequest(
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0.0") BigDecimal monthlyPrice,
        @NotNull @Min(0) Integer monthlyCredits,
        @NotNull @Min(1) Long monthlyTokenLimit,
        @NotNull @Min(1) Integer maxInputTokens,
        @NotNull @Min(1) Integer maxOutputTokens,
        @NotNull @Min(1) @Max(120) Integer requestsPerMinute,
        @NotNull @Min(0) Integer cvEvaluationCredits,
        @NotNull @Min(0) Integer careerRoadmapCredits,
        @NotNull @Min(0) Integer cvRewriteCredits,
        @NotNull @Min(0) Integer careerAssistantCredits,
        @NotNull @Min(0) Integer candidateFitCredits,
        boolean active
) { }
