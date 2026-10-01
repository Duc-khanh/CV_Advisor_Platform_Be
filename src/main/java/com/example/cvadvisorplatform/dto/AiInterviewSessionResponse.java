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
public class AiInterviewSessionResponse {
    private String sessionTitle;
    private String targetRole;
    private String experienceLevel;
    private String interviewType;
    private String overview;
    private List<AiInterviewQuestionDto> questions;
}
