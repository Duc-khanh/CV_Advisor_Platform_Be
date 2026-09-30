package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_usage_log", indexes = {
        @Index(name = "idx_ai_usage_user_created", columnList = "user_id,created_at"),
        @Index(name = "idx_ai_usage_status", columnList = "status")
})
@Getter @Setter
public class AiUsageLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AiFeature feature;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiUsageStatus status;
    @Column(length = 120)
    private String model;
    @Column(nullable = false)
    private Integer chargedCredits;
    @Column(nullable = false)
    private Integer estimatedInputTokens;
    private Integer inputTokens;
    private Integer outputTokens;
    @Column(length = 60)
    private String errorCode;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;
}
