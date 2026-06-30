package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;
    @Column
    private String avatar;
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;
    @Column
    private String phone;

    @Column
    private String headline;

    @Column
    private String location;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String birthday;

    @Column
    private String gender;

    @Column
    private String personalLink;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String skills;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String experience;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String education;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String projects;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 20)
    private String hrApprovalStatus; // PENDING, APPROVED, REJECTED (null for normal users)

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


}

