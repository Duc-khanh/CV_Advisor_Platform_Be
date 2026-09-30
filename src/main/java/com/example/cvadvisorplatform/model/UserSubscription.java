package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscription")
@Getter @Setter
public class UserSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private AiPlan plan;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    @Column(nullable = false)
    private LocalDateTime periodStart;
    @Column(nullable = false)
    private LocalDateTime periodEnd;
    @Column(nullable = false)
    private Integer usedCredits = 0;
    @Column(nullable = false)
    private Integer bonusCredits = 0;
    @Column(nullable = false)
    private Long inputTokens = 0L;
    @Column(nullable = false)
    private Long outputTokens = 0L;
    @Version
    private Long version;
}
