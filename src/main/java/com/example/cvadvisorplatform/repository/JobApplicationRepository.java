package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    boolean existsByUserIdAndJob_JobId(Long userId, Long jobId);
    List<JobApplication> findAllByUserIdOrderByIdDesc(Long userId);

    // Tìm theo trạng thái và sắp xếp mới nhất
    List<JobApplication> findAllByUserIdAndStatusOrderByIdDesc(Long userId, String status);

}

