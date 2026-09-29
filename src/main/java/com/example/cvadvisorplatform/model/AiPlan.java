package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(name = "ai_plan")
@Getter @Setter
public class AiPlan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 40)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice = BigDecimal.ZERO;
    @Column(nullable = false)
    private Integer monthlyCredits = 20;
    @Column(nullable = false)
    private Long monthlyTokenLimit = 50_000L;
    @Column(nullable = false)
    private Integer maxInputTokens = 12_000;
    @Column(nullable = false)
    private Integer maxOutputTokens = 4_096;
    @Column(nullable = false)
    private Integer requestsPerMinute = 5;
    @Column(nullable = false)
    private Integer cvEvaluationCredits = 5;
    @Column(nullable = false)
    private Integer careerRoadmapCredits = 5;
    @Column(nullable = false)
    private Integer cvRewriteCredits = 1;
    @Column(nullable = false)
    private Integer careerAssistantCredits = 2;
    @Column(nullable = false)
    private Integer candidateFitCredits = 3;
    @Column(nullable = false)
    private boolean active = true;

    public int creditsFor(AiFeature feature) {
        return switch (feature) {
            case CV_EVALUATION -> cvEvaluationCredits;
            case CAREER_ROADMAP -> careerRoadmapCredits;
            case CV_REWRITE -> cvRewriteCredits;
            case CAREER_ASSISTANT -> careerAssistantCredits;
            case CANDIDATE_FIT -> candidateFitCredits;
            case PROFILE_PARSE -> cvEvaluationCredits; // Same quota as CV_EVALUATION
        };
    }
}
