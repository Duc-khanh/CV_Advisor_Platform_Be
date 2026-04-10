package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class JobPublicResponse {

    private Long jobId;
    private String title;
    private String companyName;
    private String location;
    private String jobType;
    private String salaryRange;
    private String experienceLevel;

    private String description;

    // 👉 THÊM TRƯỜNG NÀY
    private String candidateRequirements;

    private List<String> requiredSkills;
    private Integer vacancies;
    private Integer viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;

}
