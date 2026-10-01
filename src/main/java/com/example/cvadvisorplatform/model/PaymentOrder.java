package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Getter @Setter
public class PaymentOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 32)
    private String planCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 64)
    private String transferContent;

    @Column(nullable = false, length = 20)
    private String status = "PENDING"; // PENDING, SUCCESS, EXPIRED, CANCELLED

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime completedAt;

    private String transactionRef;
}
