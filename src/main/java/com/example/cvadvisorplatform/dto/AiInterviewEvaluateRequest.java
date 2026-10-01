package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewEvaluateRequest {
    private String targetRole;
    private String experienceLevel;
    private String cvSummary;
    private List<AiInterviewUserAnswerDto> answers;
}
