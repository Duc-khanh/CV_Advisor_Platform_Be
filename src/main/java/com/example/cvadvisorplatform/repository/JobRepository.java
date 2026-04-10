package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {

    /* ===== HR ===== */
    List<Job> findByCompanyCompanyId(Long companyId);

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
    List<Job> findByActiveTrue();

    List<Job> findByActiveTrueAndTitleContainingIgnoreCase(String keyword);

    List<Job> findByActiveTrueAndLocationContainingIgnoreCase(String location);

    List<Job> findByActiveTrueAndTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(
            String keyword,
            String location
    );

    /* ===== UPDATE VIEW COUNT ATOMICALLY ===== */
    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.viewCount = COALESCE(j.viewCount, 0) + 1 WHERE j.jobId = :id")
    void incrementViewCount(@Param("id") Long id);
}