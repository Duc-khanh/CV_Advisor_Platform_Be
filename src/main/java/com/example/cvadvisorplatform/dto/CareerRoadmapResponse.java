package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerRoadmapResponse {
    
    private String summary;
    private List<RoadmapPhase> roadmap;
    private List<MarketTrend> marketTrends;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoadmapPhase {
        private String title;
        private String description;
        private List<String> skills;
        private String duration;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketTrend {
        private String topic;
        private String demand;
        private String salary;
    }
}
