package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobFavoriteRepository extends JpaRepository<JobFavorite, Long> {

    boolean existsByUserIdAndJob_JobId(Long userId, Long jobId);

    void deleteByUserIdAndJob_JobId(Long userId, Long jobId);
    List<JobFavorite> findAllByUserId(Long userId);
}

