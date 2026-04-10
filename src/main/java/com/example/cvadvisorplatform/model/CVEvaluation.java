package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cv_evaluation")
@Getter @Setter
public class CVEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long evaluationId;

    @ManyToOne
    @JoinColumn(name = "cv_id", nullable = false)
    private CV cv;

    private Integer score;

    @Lob
    private String strengths;

    @Lob
    private String weaknesses;

    @Lob
    private String suggestions;

    private LocalDateTime evaluatedAt = LocalDateTime.now();
}

