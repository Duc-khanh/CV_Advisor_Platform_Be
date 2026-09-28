package com.example.cvadvisorplatform.dto;

import lombok.Data;

@Data
public class AiCvRewriteRequest {
    private Long cvId;
    private String text;
    private String tone;
}