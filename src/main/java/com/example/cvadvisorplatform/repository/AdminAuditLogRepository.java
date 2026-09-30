package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> { }
