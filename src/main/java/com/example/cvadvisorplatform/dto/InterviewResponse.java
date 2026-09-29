package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO trả về cho frontend – bao gồm đầy đủ thông tin ứng viên, job, lịch, kết quả đánh giá.
 */
@Getter
@Setter
public class InterviewResponse {

    private Long id;

    // ─── Thông tin ứng viên / đơn ứng tuyển ─────────────────────────────────
    private Long applicationId;
    private Long userId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;

    // ─── Thông tin công việc ─────────────────────────────────────────────────
    private Long jobId;
    private String jobTitle;
    private String companyName;

    // ─── Thông tin buổi phỏng vấn ────────────────────────────────────────────
    private String roundName;
    private String interviewType;
    private String locationOrLink;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String interviewerName;
    private String interviewerEmail;
    private String notes;
    private String status;

    // ─── Kết quả đánh giá ────────────────────────────────────────────────────
    private Double rating;
    private String feedback;
    private String strengths;
    private String improvements;
    private String nextAction;

    // ─── Audit ───────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Frontend Compatibility Aliases ──────────────────────────────────────
    public LocalDateTime getScheduledAt() {
        return startTime;
    }

    public LocalDateTime getEndAt() {
        return endTime;
    }

    public String getRound() {
        return roundName;
    }

    public String getType() {
        return interviewType;
    }

    public String getInterviewer() {
        return interviewerName;
    }

    public String getMeetingLink() {
        if ("ONLINE".equalsIgnoreCase(interviewType)) {
            return locationOrLink;
        }
        return (locationOrLink != null && (locationOrLink.startsWith("http://") || locationOrLink.startsWith("https://")))
                ? locationOrLink
                : null;
    }

    public String getLocation() {
        if (!"ONLINE".equalsIgnoreCase(interviewType)) {
            return locationOrLink;
        }
        return null;
    }
}

