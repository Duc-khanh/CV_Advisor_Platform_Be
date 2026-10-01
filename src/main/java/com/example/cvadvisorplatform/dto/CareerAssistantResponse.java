package com.example.cvadvisorplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CareerAssistantResponse {
    private String conversationId;
    private String type;
    private String message;
    @Builder.Default
    private Map<String, Object> data = new LinkedHashMap<>();
    @Builder.Default
    private List<Action> actions = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Action {
        private String type;
        private String label;
        private Long jobId;
        private String path;
    }
}
