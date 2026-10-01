package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewSummaryResponse {
    private Long id;
    private String roundName;
    private String interviewType;
    private String locationOrLink;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String interviewerName;
    private String interviewerEmail;
    private String notes;
    private String status;
}
