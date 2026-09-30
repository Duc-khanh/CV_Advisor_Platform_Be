package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppliedJobResponse {
    private Long applicationId;
    private String status;           // PENDING, ACCEPTED, REJECTED
    private LocalDateTime applyDate; // Ngày nộp hồ sơ
    private String cvFileUrl;        // Link để user có thể xem lại CV đã nộp
//    private String fullName;
//    private String email;

    // Chứa thông tin Job bên trong
    private JobPublicResponse job;

    // Chứa thông tin buổi phỏng vấn (nếu có lịch)
    private InterviewSummaryResponse interview;
}