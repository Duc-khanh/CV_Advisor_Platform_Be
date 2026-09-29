package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import org.springframework.data.repository.query.Param;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobApplicationRepository
        extends JpaRepository<JobApplication, Long> {

    // check đã apply chưa
    boolean existsByUser_UserIdAndJob_JobId(
            Long userId,
            Long jobId
    );

    // lấy application theo user với JOIN FETCH để tránh N+1 Query
    @Query("""
        SELECT ja
        FROM JobApplication ja
        LEFT JOIN FETCH ja.job j
        LEFT JOIN FETCH j.company
        WHERE ja.user.userId = :userId
        ORDER BY ja.id DESC
    """)
    List<JobApplication> findAllByUserWithJobAndCompany(
            @Param("userId") Long userId
    );

    // filter theo status với JOIN FETCH để tránh N+1 Query
    @Query("""
        SELECT ja
        FROM JobApplication ja
        LEFT JOIN FETCH ja.job j
        LEFT JOIN FETCH j.company
        WHERE ja.user.userId = :userId
          AND ja.status = :status
        ORDER BY ja.id DESC
    """)
    List<JobApplication> findAllByUserAndStatusWithJobAndCompany(
            @Param("userId") Long userId,
            @Param("status") String status
    );

    // HR lấy toàn bộ ứng viên của company với phân trang và nạp trước quan hệ (Fix N+1)
    @Query(value = """
        SELECT ja
        FROM JobApplication ja
        LEFT JOIN FETCH ja.user
        LEFT JOIN FETCH ja.job
        WHERE ja.job.company.companyId = :companyId
    """, countQuery = """
        SELECT COUNT(ja)
        FROM JobApplication ja
        WHERE ja.job.company.companyId = :companyId
    """)
    Page<JobApplication> findAllByCompanyId(
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    @Query("""
        SELECT ja
        FROM JobApplication ja
        LEFT JOIN FETCH ja.user u
        LEFT JOIN FETCH ja.job j
        WHERE ja.id = :applicationId
          AND j.company.companyId = :companyId
    """)
    Optional<JobApplication> findByIdAndCompanyId(
            @Param("applicationId") Long applicationId,
            @Param("companyId") Long companyId
    );

    Optional<JobApplication> findByIdAndUser_UserId(Long applicationId, Long userId);
}
