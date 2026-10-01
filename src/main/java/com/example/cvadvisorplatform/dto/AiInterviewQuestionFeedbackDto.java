package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInterviewQuestionFeedbackDto {
    private Integer questionId;
    private String question;
    private String userAnswer;
    private Integer score;
    private String comment;
    private String modelAnswer;
}
