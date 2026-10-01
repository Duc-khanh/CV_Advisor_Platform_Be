package com.example.cvadvisorplatform.dto;

import com.example.cvadvisorplatform.model.Notification;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String message;
    private String link;
    private String actionText;

    @JsonProperty("isRead")
    private boolean isRead;

    private Long referenceId;
    private LocalDateTime createdAt;

    @JsonProperty("read")
    public boolean getRead() {
        return isRead;
    }

    public static NotificationResponse fromEntity(Notification entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .link(entity.getLink())
                .actionText(entity.getActionText())
                .isRead(entity.isRead())
                .referenceId(entity.getReferenceId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

