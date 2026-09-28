package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.AiUsageResponse;
import com.example.cvadvisorplatform.service.AiQuotaService;
import com.example.cvadvisorplatform.model.AiPlan;
import com.example.cvadvisorplatform.repository.AiPlanRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/ai-usage")
@RequiredArgsConstructor
public class AiUsageController {
    private final AiQuotaService quotaService;
    private final AiPlanRepository planRepository;
    @GetMapping
    public AiUsageResponse currentUsage() { return quotaService.getCurrentUsage(); }
    @GetMapping("/plans")
    public List<AiPlan> availablePlans() { return planRepository.findByActiveTrueOrderByMonthlyPriceAsc(); }
}
