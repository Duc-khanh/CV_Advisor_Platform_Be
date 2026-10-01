package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.*;
import com.example.cvadvisorplatform.exception.ApiException;
import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminAiService {
    private final AiPlanRepository planRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AiUsageLogRepository usageRepository;
    private final UserRepository userRepository;
    private final AdminAuditLogRepository auditRepository;
    private final AiQuotaService quotaService;

    public List<AiPlan> plans() {
        return planRepository.findAllByOrderByMonthlyPriceAsc();
    }

    @Transactional
    public AiPlan createPlan(AdminAiPlanRequest request) {
        if (planRepository.findByCodeIgnoreCase(request.code()).isPresent())
            throw new ApiException(HttpStatus.CONFLICT, "PLAN_CODE_EXISTS", "Plan code already exists.");
        AiPlan plan = new AiPlan();
        apply(plan, request);
        planRepository.save(plan);
        audit("CREATE_PLAN", "AI_PLAN", plan.getId(), plan.getCode());
        return plan;
    }

    @Transactional
    public AiPlan updatePlan(Long id, AdminAiPlanRequest request) {
        AiPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Plan not found."));
        planRepository.findByCodeIgnoreCase(request.code())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ApiException(HttpStatus.CONFLICT, "PLAN_CODE_EXISTS", "Plan code already exists."); });
        apply(plan, request);
        audit("UPDATE_PLAN", "AI_PLAN", id, request.code());
        return plan;
    }

    @Transactional
    public AiUsageResponse updateSubscription(Long userId, AdminSubscriptionRequest request) {
        User user = findUser(userId);
        AiPlan plan = planRepository.findByCodeIgnoreCase(request.planCode())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Plan not found."));
        UserSubscription sub = getOrCreate(user, plan);
        sub.setPlan(plan);
        sub.setStatus(request.status() == null ? SubscriptionStatus.ACTIVE : request.status());
        if (request.bonusCredits() != null) sub.setBonusCredits(request.bonusCredits());
        if (request.resetPeriod()) reset(sub);
        audit("UPDATE_SUBSCRIPTION", "USER", userId, plan.getCode());
        return quotaService.toResponse(sub);
    }

    @Transactional
    public AiUsageResponse adjustCredits(Long userId, CreditAdjustmentRequest request) {
        User user = findUser(userId);
        AiPlan free = planRepository.findByCodeIgnoreCase("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE AI plan is not configured"));
        UserSubscription sub = getOrCreate(user, free);
        int updated = sub.getBonusCredits() + request.amount();
        if (updated < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ADJUSTMENT", "Bonus credits cannot be negative.");
        sub.setBonusCredits(updated);
        audit("ADJUST_AI_CREDITS", "USER", userId, request.amount() + ": " + request.reason());
        return quotaService.toResponse(sub);
    }

    @Transactional
    public AiUsageResponse subscription(Long userId) {
        User user = findUser(userId);
        AiPlan free = planRepository.findByCodeIgnoreCase("FREE")
                .orElseThrow(() -> new IllegalStateException("FREE AI plan is not configured"));
        UserSubscription sub = getOrCreate(user, free);
        return quotaService.toResponse(sub);
    }

    @Transactional(readOnly = true)
    public Page<AiUsageResponse> subscriptions(String search, String planCode, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<UserSubscription> subs = subscriptionRepository.searchSubscriptions(
                (search != null && !search.isBlank()) ? search.trim() : null,
                (planCode != null && !planCode.isBlank()) ? planCode.trim() : null,
                pageable
        );
        return subs.map(quotaService::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<AiUsageLogResponse> usage(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<AiUsageLog> logs = userId == null
                ? usageRepository.findAllByOrderByCreatedAtDesc(pageable)
                : usageRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);
        return logs.map(this::usageResponse);
    }

    public Map<String, Long> dashboard() {
        return Map.of("plans", planRepository.count(), "subscriptions", subscriptionRepository.count(),
                "requests", usageRepository.count(), "successfulRequests", usageRepository.countByStatus(AiUsageStatus.SUCCESS),
                "failedRequests", usageRepository.countByStatus(AiUsageStatus.FAILED));
    }

    private void apply(AiPlan p, AdminAiPlanRequest r) {
        p.setCode(r.code().trim().toUpperCase(Locale.ROOT));
        p.setName(r.name().trim());
        p.setMonthlyPrice(r.monthlyPrice());
        p.setMonthlyCredits(r.monthlyCredits());
        p.setMonthlyTokenLimit(r.monthlyTokenLimit());
        p.setMaxInputTokens(r.maxInputTokens());
        p.setMaxOutputTokens(r.maxOutputTokens());
        p.setRequestsPerMinute(r.requestsPerMinute());
        p.setCvEvaluationCredits(r.cvEvaluationCredits());
        p.setCareerRoadmapCredits(r.careerRoadmapCredits());
        p.setCvRewriteCredits(r.cvRewriteCredits());
        p.setCareerAssistantCredits(r.careerAssistantCredits());
        p.setCandidateFitCredits(r.candidateFitCredits());
        p.setActive(r.active());
    }

    private AiUsageLogResponse usageResponse(AiUsageLog log) {
        int charged = (log.getStatus() == AiUsageStatus.FAILED) ? 0 : (log.getChargedCredits() != null ? log.getChargedCredits() : 0);
        return new AiUsageLogResponse(log.getId(), log.getUser().getUserId(), log.getUser().getEmail(),
                log.getFeature().name(), log.getStatus().name(), log.getModel(), charged,
                log.getEstimatedInputTokens(), log.getInputTokens(), log.getOutputTokens(), log.getErrorCode(),
                log.getCreatedAt(), log.getCompletedAt());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private UserSubscription getOrCreate(User user, AiPlan plan) {
        return subscriptionRepository.findByUser_UserId(user.getUserId()).orElseGet(() -> {
            UserSubscription sub = new UserSubscription();
            sub.setUser(user);
            sub.setPlan(plan);
            reset(sub);
            return subscriptionRepository.save(sub);
        });
    }

    private void reset(UserSubscription sub) {
        LocalDateTime now = LocalDateTime.now();
        sub.setPeriodStart(now);
        sub.setPeriodEnd(now.plusMonths(1));
        sub.setUsedCredits(0);
        sub.setInputTokens(0L);
        sub.setOutputTokens(0L);
    }

    private void audit(String action, String targetType, Long targetId, String details) {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(principal.getUser());
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        auditRepository.save(log);
    }
}
