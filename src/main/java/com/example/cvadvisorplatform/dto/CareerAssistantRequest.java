package com.example.cvadvisorplatform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CareerAssistantRequest {
    private String conversationId;
    private String message;
    private String actionType;
    private Context context = new Context();
    private List<HistoryMessage> history = new ArrayList<>();

    @Data
    public static class Context {
        private String currentPage;
        private Long currentJobId;
        private Long applicationId;
    }

    @Data
    public static class HistoryMessage {
        private String role;
        private String content;
    }
}
