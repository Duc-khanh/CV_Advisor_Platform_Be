package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CVRepository extends JpaRepository<CV, Long> {
    List<CV> findAllByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Optional<CV> findFirstByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
