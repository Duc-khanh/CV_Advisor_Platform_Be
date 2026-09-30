package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewUserAnswerDto {
    private Integer questionId;
    private String question;
    private String category;
    private String userAnswer;
}
