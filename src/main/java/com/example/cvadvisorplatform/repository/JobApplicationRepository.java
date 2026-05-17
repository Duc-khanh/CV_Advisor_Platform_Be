package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    // check đã apply chưa
    boolean existsByUser_UserIdAndJob_JobId(
            Long userId,
            Long jobId
    );

    // lấy application theo user
    List<JobApplication> findAllByUser_UserIdOrderByIdDesc(
            Long userId
    );

    // filter theo status
    List<JobApplication>
    findAllByUser_UserIdAndStatusOrderByIdDesc(
            Long userId,
            String status
    );

    // HR lấy toàn bộ ứng viên của company
    @Query("""
        SELECT ja
        FROM JobApplication ja
        WHERE ja.job.company.companyId = :companyId
        ORDER BY ja.appliedAt DESC
    """)
    List<JobApplication> findAllByCompanyId(
            Long companyId
    );
}