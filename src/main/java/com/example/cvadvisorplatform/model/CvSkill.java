package com.example.cvadvisorplatform.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity
@Table(name = "cv_skill")
@Getter @Setter
public class CvSkill {

    @EmbeddedId
    private CvSkillId id;

    @ManyToOne
    @MapsId("cvId")
    @JoinColumn(name = "cv_id")
    private CV cv;

    @ManyToOne
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;
}

