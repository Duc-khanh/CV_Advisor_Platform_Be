package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobApplicationEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobApplicationEvaluationRepository extends JpaRepository<JobApplicationEvaluation, Long> {

    Optional<JobApplicationEvaluation> findByJobApplication_Id(Long applicationId);

    List<JobApplicationEvaluation> findByJobApplication_IdIn(List<Long> applicationIds);
}
