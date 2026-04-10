package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppliedCandidateResponse {

    private Long applicationId;

    // Candidate
    private Long userId;

    // Application
    private String status;
    private LocalDateTime appliedAt;
    private String cvFileUrl;

    // Job
    private Long jobId;
    private String jobTitle;
    private String location;
    private String jobType;
    private String salaryRange;
}
