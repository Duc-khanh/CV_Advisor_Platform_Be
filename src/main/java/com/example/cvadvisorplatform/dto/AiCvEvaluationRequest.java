package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiCvEvaluationRequest {

    private String cvContent;

    private String jobDescription;
}