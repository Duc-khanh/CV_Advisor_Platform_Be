package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewEvaluationResponse {
    private Integer overallScore;
    private String rating;
    private String summaryFeedback;
    private Map<String, String> starAnalysis;
    private List<String> strengths;
    private List<String> improvements;
    private List<AiInterviewQuestionFeedbackDto> questionFeedbacks;
}
