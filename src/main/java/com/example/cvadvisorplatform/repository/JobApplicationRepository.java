package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserIdAndJob_JobId(Long userId, Long jobId);
    List<JobApplication> findAllByUserIdOrderByIdDesc(Long userId);


    List<JobApplication> findAllByUserIdAndStatusOrderByIdDesc(Long userId, String status);
        @Query("""
        SELECT ja FROM JobApplication ja
        JOIN ja.job j
        JOIN j.company c
        WHERE c.companyId = :companyId
        ORDER BY ja.appliedAt DESC
    """)
        List<JobApplication> findAllByCompanyId(Long companyId);




}

