package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
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

    /* ===== PUBLIC (USER) SEARCH ===== */
    @Query("SELECT DISTINCT j FROM Job j " +
           "LEFT JOIN j.company c " +
           "WHERE j.active = true " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(j.jobType) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     EXISTS (SELECT 1 FROM j.requiredSkills s WHERE LOWER(s) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
           "     LOWER(CAST(j.description AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "     LOWER(CAST(j.candidateRequirements AS string)) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:location IS NULL OR :location = '' OR " +
           "     LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) " +
           "ORDER BY j.createdAt DESC")
    Page<Job> searchPublicJobs(
            @Param("keyword") String keyword,
            @Param("location") String location,
            Pageable pageable
    );

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
