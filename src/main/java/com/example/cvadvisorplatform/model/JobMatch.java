package com.example.cvadvisorplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "job_match",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cv_id", "job_id"})
)
@Getter
@Setter
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long matchId;

    @ManyToOne
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    private Integer matchPercent;

    @Lob
    private String aiComment;

    private LocalDateTime matchedAt = LocalDateTime.now();
}

