package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewQuestionDto {
    private Integer id;
    private String category;
    private String question;
    private String hint;
    private String competencyEvaluated;
}
