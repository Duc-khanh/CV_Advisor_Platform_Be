package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CVRepository extends JpaRepository<CV, Long> {
    List<CV> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
