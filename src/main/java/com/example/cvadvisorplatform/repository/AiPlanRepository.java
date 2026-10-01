package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.AiPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiPlanRepository extends JpaRepository<AiPlan, Long> {
    Optional<AiPlan> findByCodeIgnoreCase(String code);
    List<AiPlan> findAllByOrderByMonthlyPriceAsc();
    List<AiPlan> findByActiveTrueOrderByMonthlyPriceAsc();
}
