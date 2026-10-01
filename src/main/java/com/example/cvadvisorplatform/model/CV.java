package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv")
@Getter @Setter
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cvId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String fileName;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String cvText;

    private String storedFileName;

    private Long fileSize;

    private String mimeType;

    private LocalDateTime createdAt = LocalDateTime.now();
}

