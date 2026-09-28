package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_audit_log")
@Getter @Setter
public class AdminAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_user_id", nullable = false)
    private User admin;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 40)
    private String targetType;
    @Column(nullable = false)
    private Long targetId;
    @Column(length = 1000)
    private String details;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
