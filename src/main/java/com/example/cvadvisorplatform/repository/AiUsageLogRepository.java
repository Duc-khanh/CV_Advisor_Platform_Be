package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.AiUsageLog;
import com.example.cvadvisorplatform.model.AiUsageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {
    Page<AiUsageLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AiUsageLog> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByStatus(AiUsageStatus status);
}
