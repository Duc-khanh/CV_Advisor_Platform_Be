package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.exception.AiQuotaExceededException;
import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import com.example.cvadvisorplatform.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiQuotaServiceTest {
    private UserSubscriptionRepository subscriptions;
    private AiUsageLogRepository logs;
    private AiQuotaService service;
    private UserSubscription subscription;

    @BeforeEach
    void setUp() {
        subscriptions = mock(UserSubscriptionRepository.class);
        logs = mock(AiUsageLogRepository.class);
        UserRepository users = mock(UserRepository.class);
        service = new AiQuotaService(subscriptions, mock(AiPlanRepository.class), logs, users);
        User user = new User();
        user.setUserId(1L);
        user.setEmail("user@example.com");
        user.setPassword("hash");
        Role role = new Role();
        role.setRoleName("USER");
        user.setRole(role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, new UserPrincipal(user).getAuthorities()));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        AiPlan plan = new AiPlan();
        plan.setMonthlyCredits(20);
        plan.setMonthlyTokenLimit(50_000L);
        plan.setRequestsPerMinute(5);
        subscription = new UserSubscription();
        subscription.setUser(user);
        subscription.setPlan(plan);
        subscription.setPeriodStart(LocalDateTime.now());
        subscription.setPeriodEnd(LocalDateTime.now().plusDays(1));
        when(subscriptions.findByUser_UserId(1L)).thenReturn(Optional.of(subscription));
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    @Test
    void reservesFeatureCredits() {
        service.reserve(AiFeature.CV_EVALUATION, 100, 200);
        assertEquals(5, subscription.getUsedCredits());
        verify(logs).save(any(AiUsageLog.class));
    }

    @Test
    void rejectsWhenCreditsAreExhausted() {
        subscription.setUsedCredits(18);
        assertThrows(AiQuotaExceededException.class,
                () -> service.reserve(AiFeature.CV_EVALUATION, 100, 200));
        verify(logs, never()).save(any());
    }

    @Test
    void refundsCreditsAndSetsChargedCreditsToZeroOnFailure() {
        AiQuotaService.Reservation reservation = new AiQuotaService.Reservation(10L, 1L, 5);
        subscription.setUsedCredits(5);
        AiUsageLog log = new AiUsageLog();
        log.setId(10L);
        log.setStatus(AiUsageStatus.RESERVED);
        log.setChargedCredits(5);
        when(logs.findById(10L)).thenReturn(Optional.of(log));

        service.fail(reservation, "PROVIDER_ERROR");

        assertEquals(0, subscription.getUsedCredits());
        assertEquals(0, log.getChargedCredits());
        assertEquals(AiUsageStatus.FAILED, log.getStatus());
        assertEquals("PROVIDER_ERROR", log.getErrorCode());
    }
}
