package com.example.cvadvisorplatform.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Request body khi lưu kết quả đánh giá sau phỏng vấn.
 */
@Getter
@Setter
public class InterviewFeedbackRequest {

    private Double rating;          // 0.5 – 5.0
    private String feedback;        // Nhận xét tổng quan
    private String strengths;       // Điểm mạnh
    private String improvements;    // Điểm cần cải thiện
    private String nextAction;      // ACCEPT | REJECT | NEXT_ROUND
}
