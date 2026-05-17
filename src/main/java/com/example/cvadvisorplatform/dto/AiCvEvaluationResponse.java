package com.example.cvadvisorplatform.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCvEvaluationResponse {
    private String rawAiResponse;
    private Integer score;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> missingSkills;
    private String summary;
    private List<Map<String, String>> recommendedJobs;
}
