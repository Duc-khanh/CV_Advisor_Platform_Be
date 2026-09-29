package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewStatsResponse {
    private long todayCount;
    private long upcomingCount;
    private long pendingFeedback;
    private long completedThisMonth;
}
