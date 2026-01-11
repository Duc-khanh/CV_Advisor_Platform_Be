package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
@Embeddable
@Getter @Setter
public class JobSkillId implements Serializable {
    private Long jobId;
    private Integer skillId;
}
