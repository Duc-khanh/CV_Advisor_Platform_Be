package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobCreateRequest {

    private String title;
    private String experienceLevel;
    private String description;

    /* ===== HIỂN THỊ USER ===== */
    private String location;
    private String salaryRange;
    private String jobType;
    private String candidateRequirements;


    /* ===== QUẢN LÝ ===== */
    private Integer vacancies;
    private LocalDateTime expiredAt;

    /* ===== AI ===== */
    private List<String> requiredSkills;
}
