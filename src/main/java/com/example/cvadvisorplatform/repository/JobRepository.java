package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobRepository extends JpaRepository<Job, Long> {

    /* ===== HR ===== */
    List<Job> findByCompanyCompanyId(Long companyId);

    long countByCompanyCompanyIdAndActiveTrue(Long companyId);

    List<Job> findByCompanyCompanyIdAndActiveTrueOrderByCreatedAtDesc(Long companyId);

    Optional<Job> findByJobIdAndCompanyCompanyId(Long jobId, Long companyId);

    List<Job> findByCompanyCompanyIdAndTitleContainingIgnoreCase(
            Long companyId, String keyword
    );

    List<Job> findByCompanyCompanyIdAndExperienceLevel(
            Long companyId, String experienceLevel
    );

    List<Job> findByCompanyCompanyIdAndTitleContainingIgnoreCaseAndExperienceLevel(
            Long companyId, String keyword, String experienceLevel
    );

    /* ===== PUBLIC (USER) ===== */
    Page<Job> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    Page<Job> findByActiveTrueAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(String keyword, Pageable pageable);

    Page<Job> findByActiveTrueAndLocationContainingIgnoreCaseOrderByCreatedAtDesc(String location, Pageable pageable);

    Page<Job> findByActiveTrueAndTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseOrderByCreatedAtDesc(
            String keyword,
            String location,
            Pageable pageable
    );

    /* ===== UPDATE VIEW COUNT ATOMICALLY ===== */
    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.viewCount = COALESCE(j.viewCount, 0) + 1 WHERE j.jobId = :id")
    void incrementViewCount(@Param("id") Long id);
}
