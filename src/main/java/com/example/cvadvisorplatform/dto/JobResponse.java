package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobResponse {

    private Long jobId;
    private String title;
    private String experienceLevel;
    private String description;

    private String companyName;

    /* ===== HIỂN THỊ USER ===== */
    private String location;
    private String salaryRange;
    private String jobType;

    /* ===== QUẢN LÝ ===== */
    private Boolean active;
    private Integer vacancies;
    private LocalDateTime expiredAt;
    private Integer viewCount;
    private String candidateRequirements;
    private String imageUrl;




    /* ===== AI ===== */
    private List<String> requiredSkills;

    private LocalDateTime createdAt;

}
