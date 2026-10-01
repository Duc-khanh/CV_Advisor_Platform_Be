package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiUsageResponse;
import com.example.cvadvisorplatform.exception.AiQuotaExceededException;
import com.example.cvadvisorplatform.exception.ApiException;
import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuotaService {
    private final UserSubscriptionRepository subscriptionRepository;
    private final AiPlanRepository planRepository;
    private final AiUsageLogRepository usageLogRepository;
    private final UserRepository userRepository;
    private final Map<Long, Deque<Long>> recentRequests = new ConcurrentHashMap<>();

    @Transactional
    public Reservation reserve(AiFeature feature, int inputEstimate, int requestedOutput) {
        return reserve(null, feature, inputEstimate, requestedOutput);
    }

    @Transactional
    public Reservation reserve(User user, AiFeature feature, int inputEstimate, int requestedOutput) {
        if (user == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                user = userRepository.findById(principal.getUser().getUserId()).orElse(null);
            }
        }

        // Tác vụ nền tự động của hệ thống (Background / Async) không có user context
        if (user == null) {
            log.info("Gọi AI cho tác vụ nền hệ thống (không trừ quota người dùng) cho tính năng: {}", feature);
            return null;
        }

        UserSubscription subscription = getOrCreate(user);
        resetPeriodIfNeeded(subscription);
        AiPlan plan = subscription.getPlan();
        validateAllowance(subscription, inputEstimate, requestedOutput);
        enforceRateLimit(user.getUserId(), plan.getRequestsPerMinute());
        return reserveCredits(user, subscription, feature, inputEstimate);
    }

    private Reservation reserveCredits(User user, UserSubscription sub, AiFeature feature, int estimate) {
        int cost = sub.getPlan().creditsFor(feature);
        checkCredits(sub, cost);
        sub.setUsedCredits(sub.getUsedCredits() + cost);
        return saveReservation(user, feature, estimate, cost);
    }

    private void checkCredits(UserSubscription s, int c) {
        int a=s.getPlan().getMonthlyCredits()+s.getBonusCredits();
        if(Integer.compare(s.getUsedCredits()+c,a)==1) quotaError();
    }
    private void quotaError() {
        throw new AiQuotaExceededException("AI_CREDITS_EXHAUSTED","AI credits exhausted.");
    }

    private Reservation saveReservation(User user, AiFeature feature, int estimate, int cost) {
        AiUsageLog log = new AiUsageLog();
        log.setUser(user);
        log.setFeature(feature);
        log.setStatus(AiUsageStatus.RESERVED);
        log.setChargedCredits(cost);
        log.setEstimatedInputTokens(estimate);
        usageLogRepository.save(log);
        return new Reservation(log.getId(), user.getUserId(), cost);
    }

    private void validateAllowance(UserSubscription sub, int input, int output) {
        AiPlan plan = sub.getPlan();
        if (sub.getStatus() != SubscriptionStatus.ACTIVE || !plan.isActive())
            throw new AiQuotaExceededException("AI_SUBSCRIPTION_INACTIVE", "AI subscription is inactive.");
        if (input > plan.getMaxInputTokens() || output > plan.getMaxOutputTokens())
            throw new AiQuotaExceededException("AI_REQUEST_TOO_LARGE", "Request exceeds plan token limits.");
        long used = sub.getInputTokens() + sub.getOutputTokens();
        if (used + input + output > plan.getMonthlyTokenLimit())
            throw new AiQuotaExceededException("AI_TOKEN_LIMIT_EXCEEDED", "Monthly token limit exceeded.");
    }

    @Transactional
    public void complete(Reservation reservation, String model, int inputTokens, int outputTokens) {
        if (reservation == null) return;
        AiUsageLog log = usageLogRepository.findById(reservation.usageLogId())
                .orElseThrow(() -> new IllegalStateException("AI usage reservation not found"));
        if (log.getStatus() != AiUsageStatus.RESERVED) return;
        UserSubscription sub = subscriptionRepository.findByUser_UserId(reservation.userId())
                .orElseThrow(() -> new IllegalStateException("AI subscription not found"));
        sub.setInputTokens(sub.getInputTokens() + Math.max(0, inputTokens));
        sub.setOutputTokens(sub.getOutputTokens() + Math.max(0, outputTokens));
        log.setModel(model);
        log.setInputTokens(Math.max(0, inputTokens));
        log.setOutputTokens(Math.max(0, outputTokens));
        log.setStatus(AiUsageStatus.SUCCESS);
        log.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public void fail(Reservation reservation, String errorCode) {
        if (reservation == null) return;
        AiUsageLog log = usageLogRepository.findById(reservation.usageLogId())
                .orElseThrow(() -> new IllegalStateException("AI usage reservation not found"));
        if (log.getStatus() != AiUsageStatus.RESERVED) return;
        UserSubscription sub = subscriptionRepository.findByUser_UserId(reservation.userId())
                .orElseThrow(() -> new IllegalStateException("AI subscription not found"));
        sub.setUsedCredits(Math.max(0, sub.getUsedCredits() - reservation.credits()));
        log.setChargedCredits(0);
        log.setStatus(AiUsageStatus.FAILED);
        log.setErrorCode(errorCode == null ? "AI_PROVIDER_ERROR" : errorCode);
        log.setCompletedAt(LocalDateTime.now());
    }

    @Transactional
    public AiUsageResponse getCurrentUsage() {
        return toResponse(getOrCreate(currentUser()));
    }

    @Transactional
    public AiUsageResponse upgradePlan(String planCode) {
        User user = currentUser();
        return activatePlanForUser(user, planCode);
    }

    @Transactional
    public AiUsageResponse activatePlanForUser(User user, String planCode) {
        if (planCode == null || planCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "Mã gói dịch vụ AI không được để trống.");
        }
        if ("FREE".equalsIgnoreCase(planCode.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DOWNGRADE_FREE", "Gói Free là gói mặc định, không thể nâng cấp về gói này.");
        }

        AiPlan targetPlan = planRepository.findByCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Không tìm thấy gói AI: " + planCode));

        if (!targetPlan.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLAN_INACTIVE", "Gói AI hiện đang tạm ngưng phục vụ.");
        }

        UserSubscription sub = getOrCreate(user);
        if (sub.getPlan() != null) {
            if (sub.getPlan().getCode().equalsIgnoreCase(targetPlan.getCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_ACTIVE", "Bạn đang sử dụng gói " + targetPlan.getName() + " rồi.");
            }
            if (targetPlan.getMonthlyPrice().compareTo(sub.getPlan().getMonthlyPrice()) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DOWNGRADE_NOT_ALLOWED", "Bạn chỉ có thể nâng cấp lên gói có thứ hạng cao hơn gói hiện tại.");
            }
        }

        sub.setPlan(targetPlan);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        startNewPeriod(sub);
        subscriptionRepository.save(sub);

        log.info("Người dùng ID={} ({}) đã kích hoạt thành công gói AI: {}", user.getUserId(), user.getEmail(), targetPlan.getCode());
        return toResponse(sub);
    }

    public AiUsageResponse toResponse(UserSubscription sub) {
        AiPlan plan = sub.getPlan();
        int bonus = sub.getBonusCredits() != null ? sub.getBonusCredits() : 0;
        int used = sub.getUsedCredits() != null ? sub.getUsedCredits() : 0;
        int monthlyCredits = plan != null && plan.getMonthlyCredits() != null ? plan.getMonthlyCredits() : 0;
        int remaining = Math.max(0, monthlyCredits + bonus - used);
        long monthlyLimit = plan != null && plan.getMonthlyTokenLimit() != null ? plan.getMonthlyTokenLimit() : 0L;
        long inputTokens = sub.getInputTokens() != null ? sub.getInputTokens() : 0L;
        long outputTokens = sub.getOutputTokens() != null ? sub.getOutputTokens() : 0L;
        String status = sub.getStatus() != null ? sub.getStatus().name() : SubscriptionStatus.ACTIVE.name();
        String planCode = plan != null ? plan.getCode() : "FREE";
        String planName = plan != null ? plan.getName() : "Free";
        String email = sub.getUser() != null ? sub.getUser().getEmail() : null;
        return new AiUsageResponse(sub.getId(), sub.getUser().getUserId(), email, planCode, planName,
                status, monthlyCredits, bonus, used,
                remaining, monthlyLimit, inputTokens, outputTokens,
                sub.getPeriodStart(), sub.getPeriodEnd());
    }

    private UserSubscription getOrCreate(User user) {
        return subscriptionRepository.findByUser_UserId(user.getUserId()).orElseGet(() -> {
            AiPlan free = planRepository.findByCodeIgnoreCase("FREE")
                    .orElseThrow(() -> new IllegalStateException("FREE AI plan is not configured"));
            UserSubscription sub = new UserSubscription();
            sub.setUser(user);
            sub.setPlan(free);
            startNewPeriod(sub);
            return subscriptionRepository.save(sub);
        });
    }
    private void resetPeriodIfNeeded(UserSubscription sub) {
        if (sub.getPeriodEnd() == null || !sub.getPeriodEnd().isAfter(LocalDateTime.now())) startNewPeriod(sub);
    }
    private void startNewPeriod(UserSubscription sub) {
        LocalDateTime now = LocalDateTime.now();
        sub.setPeriodStart(now);
        sub.setPeriodEnd(now.plusMonths(1));
        sub.setUsedCredits(0);
        sub.setInputTokens(0L);
        sub.setOutputTokens(0L);
    }
    private void enforceRateLimit(Long userId, int limit) {
        long cutoff = System.currentTimeMillis() - 60_000L;
        Deque<Long> requests = recentRequests.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (requests) {
            while (!requests.isEmpty() && requests.peekFirst() < cutoff) requests.removeFirst();
            if (requests.size() >= Math.max(1, limit))
                throw new AiQuotaExceededException("AI_RATE_LIMITED", "Too many AI requests.");
            requests.addLast(System.currentTimeMillis());
        }
    }
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
        return userRepository.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Account not found."));
    }
    public record Reservation(Long usageLogId, Long userId, int credits) { }
}
