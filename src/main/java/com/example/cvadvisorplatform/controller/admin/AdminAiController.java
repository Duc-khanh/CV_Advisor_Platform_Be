package com.example.cvadvisorplatform.controller.admin;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.model.AiPlan;
import com.example.cvadvisorplatform.service.AdminAiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {
    private final AdminAiService service;

    @GetMapping("/dashboard")
    public Map<String, Long> dashboard() { return service.dashboard(); }
    @GetMapping("/plans")
    public List<AiPlan> plans() { return service.plans(); }
    @PostMapping("/plans")
    public AiPlan create(@Valid @RequestBody AdminAiPlanRequest request) { return service.createPlan(request); }
    @PutMapping("/plans/{id}")
    public AiPlan update(@PathVariable Long id, @Valid @RequestBody AdminAiPlanRequest request) {
        return service.updatePlan(id, request);
    }
    @GetMapping("/subscriptions")
    public Page<AiUsageResponse> subscriptions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String planCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.subscriptions(search, planCode, page, size);
    }
    @GetMapping("/subscriptions/{userId}")
    public AiUsageResponse subscription(@PathVariable Long userId) { return service.subscription(userId); }
    @PutMapping("/subscriptions/{userId}")
    public AiUsageResponse updateSubscription(@PathVariable Long userId,
                                              @Valid @RequestBody AdminSubscriptionRequest request) {
        return service.updateSubscription(userId, request);
    }
    @PostMapping("/subscriptions/{userId}/credits")
    public AiUsageResponse adjustCredits(@PathVariable Long userId,
                                         @Valid @RequestBody CreditAdjustmentRequest request) {
        return service.adjustCredits(userId, request);
    }
    @GetMapping("/usage")
    public Page<AiUsageLogResponse> usage(@RequestParam(required = false) Long userId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return service.usage(userId, page, size);
    }
}
