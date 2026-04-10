package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
@Entity
@Table(name = "company")
@Getter @Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    @Column(nullable = false)
    private String companyName;

    @ManyToOne
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;
    private String address;

    @Lob
    private String description;

    private LocalDateTime createdAt = LocalDateTime.now();
}

