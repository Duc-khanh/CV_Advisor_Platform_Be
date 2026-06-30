package com.example.cvadvisorplatform.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCandidateFitResponse {

    private Integer score;

    private String summary;

    private List<String> strengths;

    private List<String> weaknesses;

    private String recommendations;

    private String rawAiResponse;
}