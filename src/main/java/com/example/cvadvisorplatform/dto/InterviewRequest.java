package com.example.cvadvisorplatform.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request body khi tạo mới / cập nhật lịch phỏng vấn.
 * Hỗ trợ alias linh hoạt giữa frontend và backend.
 */
@Getter
@Setter
public class InterviewRequest {

    private Long applicationId;

    @JsonAlias({"round", "round_name"})
    private String roundName;

    @JsonAlias({"type", "interview_type"})
    private String interviewType;   // ONLINE | OFFLINE

    @JsonAlias({"location", "meetingLink", "meeting_link", "location_or_link"})
    private String locationOrLink;

    @JsonAlias({"scheduledAt", "start_time"})
    private LocalDateTime startTime;

    @JsonAlias({"endAt", "end_time"})
    private LocalDateTime endTime;

    @JsonAlias({"interviewer", "interviewer_name"})
    private String interviewerName;

    @JsonAlias({"interviewer_email"})
    private String interviewerEmail;

    private String notes;

    public void setLocationOrLink(String val) {
        if (val != null && !val.trim().isEmpty()) {
            this.locationOrLink = val.trim();
        } else if (this.locationOrLink == null) {
            this.locationOrLink = val;
        }
    }

    public LocalDateTime resolveStartTime() {
        return startTime;
    }

    public LocalDateTime resolveEndTime() {
        if (endTime != null && startTime != null && endTime.isAfter(startTime)) {
            return endTime;
        }
        if (startTime != null) {
            // Mặc định thời lượng phỏng vấn là 60 phút nếu không truyền endTime hoặc endTime không hợp lệ
            return startTime.plusMinutes(60);
        }
        return endTime;
    }

    public String resolveInterviewType() {
        return interviewType != null ? interviewType.toUpperCase() : "ONLINE";
    }

    public String resolveRoundName() {
        return (roundName != null && !roundName.isBlank()) ? roundName : "Vòng 1 - Phỏng vấn Sơ loại";
    }

    public String resolveLocationOrLink() {
        return (locationOrLink != null && !locationOrLink.trim().isEmpty()) ? locationOrLink.trim() : null;
    }
}