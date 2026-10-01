package com.example.cvadvisorplatform.config;

import com.example.cvadvisorplatform.model.AiPlan;
import com.example.cvadvisorplatform.repository.AiPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class AiPlanSeeder implements ApplicationRunner {
    private final AiPlanRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("FREE", "Free", BigDecimal.ZERO, 20, 50_000L, 5);
        seed("BASIC", "Basic", new BigDecimal("99000"), 200, 500_000L, 20);
        seed("PRO", "Pro", new BigDecimal("299000"), 1000, 2_500_000L, 60);
    }

    private void seed(String code, String name, BigDecimal price, int credits, long tokens, int rpm) {
        if (repository.findByCodeIgnoreCase(code).isPresent()) return;
        AiPlan plan = new AiPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setMonthlyPrice(price);
        plan.setMonthlyCredits(credits);
        plan.setMonthlyTokenLimit(tokens);
        plan.setRequestsPerMinute(rpm);
        repository.save(plan);
    }
}
