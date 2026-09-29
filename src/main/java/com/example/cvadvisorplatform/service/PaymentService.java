package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.PaymentOrderResponse;
import com.example.cvadvisorplatform.exception.ApiException;
import com.example.cvadvisorplatform.model.AiPlan;
import com.example.cvadvisorplatform.model.PaymentOrder;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.model.UserSubscription;
import com.example.cvadvisorplatform.repository.AiPlanRepository;
import com.example.cvadvisorplatform.repository.PaymentOrderRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import com.example.cvadvisorplatform.repository.UserSubscriptionRepository;
import com.example.cvadvisorplatform.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final AiPlanRepository planRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final AiQuotaService aiQuotaService;

    public static final String BANK_ID = "MB";
    public static final String BANK_NAME = "Ngân hàng TMCP Quân Đội (MB Bank)";
    public static final String ACCOUNT_NO = "686825062005";
    public static final String ACCOUNT_NAME = "NGUYEN DUC KHANH";

    @Transactional
    public PaymentOrderResponse createOrder(String planCode) {
        User user = currentUser();
        if (planCode == null || planCode.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PLAN", "Mã gói dịch vụ không được để trống.");
        }
        if ("FREE".equalsIgnoreCase(planCode.trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_DOWNGRADE_FREE", "Gói Free là gói mặc định, không cần thanh toán.");
        }

        AiPlan targetPlan = planRepository.findByCodeIgnoreCase(planCode.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Không tìm thấy gói AI: " + planCode));

        if (!targetPlan.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLAN_INACTIVE", "Gói AI hiện đang tạm ngưng phục vụ.");
        }

        // Kiểm tra hạ cấp hoặc gói hiện tại
        UserSubscription currentSub = subscriptionRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (currentSub != null && currentSub.getPlan() != null) {
            if (currentSub.getPlan().getCode().equalsIgnoreCase(targetPlan.getCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_ACTIVE", "Bạn đang sử dụng gói " + targetPlan.getName() + " rồi.");
            }
            if (targetPlan.getMonthlyPrice().compareTo(currentSub.getPlan().getMonthlyPrice()) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DOWNGRADE_NOT_ALLOWED", "Bạn chỉ có thể nâng cấp lên gói có thứ hạng cao hơn gói hiện tại.");
            }
        }

        // Hủy các đơn hàng PENDING cũ của user nếu có
        paymentOrderRepository.findTopByUser_UserIdAndStatusOrderByCreatedAtDesc(user.getUserId(), "PENDING")
                .ifPresent(oldOrder -> {
                    oldOrder.setStatus("CANCELLED");
                    paymentOrderRepository.save(oldOrder);
                });

        // Tạo mã đơn hàng độc nhất dạng CVxxxxxx (VD: CV389142)
        String orderCode = "CV" + (int)((Math.random() * 900000) + 100000);
        while (paymentOrderRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode = "CV" + (int)((Math.random() * 900000) + 100000);
        }

        PaymentOrder order = new PaymentOrder();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setPlanCode(targetPlan.getCode());
        order.setAmount(targetPlan.getMonthlyPrice());
        order.setTransferContent(orderCode);
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());
        paymentOrderRepository.save(order);

        log.info("Khởi tạo đơn hàng thanh toán nâng cấp AI: orderCode={}, user={}, plan={}, amount={}",
                orderCode, user.getEmail(), targetPlan.getCode(), targetPlan.getMonthlyPrice());

        return toResponse(order, targetPlan);
    }

    @Transactional(readOnly = true)
    public PaymentOrderResponse getOrderStatus(String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));
        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    @Transactional
    public Map<String, Object> processWebhook(Map<String, Object> payload) {
        log.info("Nhận Webhook ngân hàng / Casso / PayOS: {}", payload);
        if (payload == null || payload.isEmpty()) {
            return Map.of("success", false, "message", "Payload trống");
        }

        // 1. Trường hợp Webhook từ Casso: { data: [ { description, amount, tid } ] }
        if (payload.containsKey("data") && payload.get("data") instanceof List<?> list) {
            int processedCount = 0;
            for (Object item : list) {
                if (item instanceof Map<?, ?> tx) {
                    String desc = Objects.toString(tx.get("description"), "");
                    BigDecimal amount = parseAmount(tx.get("amount"));
                    String tid = Objects.toString(tx.get("tid"), "");
                    if (processTransaction(desc, amount, tid)) {
                        processedCount++;
                    }
                }
            }
            return Map.of("success", true, "processed", processedCount);
        }

        // 2. Trường hợp Webhook từ PayOS: { code: "00", data: { description, amount, reference } }
        if (payload.containsKey("data") && payload.get("data") instanceof Map<?, ?> dataMap) {
            String desc = Objects.toString(dataMap.get("description"), "");
            BigDecimal amount = parseAmount(dataMap.get("amount"));
            String ref = Objects.toString(dataMap.get("reference"), Objects.toString(dataMap.get("orderCode"), ""));
            boolean success = processTransaction(desc, amount, ref);
            return Map.of("success", success);
        }

        // 3. Trường hợp Webhook trực tiếp: { orderCode, amount }
        String desc = Objects.toString(payload.get("transferContent"), Objects.toString(payload.get("description"), Objects.toString(payload.get("orderCode"), "")));
        BigDecimal amount = parseAmount(payload.get("amount"));
        String ref = Objects.toString(payload.get("transactionRef"), "DIRECT");
        boolean success = processTransaction(desc, amount, ref);
        return Map.of("success", success);
    }

    @Transactional
    public PaymentOrderResponse simulatePayment(String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));

        if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
            AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
            return toResponse(order, plan);
        }

        // Kích hoạt gói dịch vụ AI cho user
        aiQuotaService.activatePlanForUser(order.getUser(), order.getPlanCode());

        order.setStatus("SUCCESS");
        order.setCompletedAt(LocalDateTime.now());
        order.setTransactionRef("SIMULATED_" + System.currentTimeMillis());
        paymentOrderRepository.save(order);

        log.info("Giả lập thanh toán thành công cho đơn hàng: orderCode={}, user={}, plan={}",
                orderCode, order.getUser().getEmail(), order.getPlanCode());

        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    private boolean processTransaction(String description, BigDecimal amount, String ref) {
        if (description == null || description.isBlank()) return false;

        // Trích xuất mã CVxxxxxx từ nội dung chuyển khoản
        Pattern pattern = Pattern.compile("CV[0-9]{6}", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(description);
        String foundCode = null;
        if (matcher.find()) {
            foundCode = matcher.group().toUpperCase();
        }

        PaymentOrder order = null;
        if (foundCode != null) {
            order = paymentOrderRepository.findByOrderCode(foundCode).orElse(null);
        }
        if (order == null) {
            order = paymentOrderRepository.findByTransferContent(description.trim()).orElse(null);
        }

        if (order != null && "PENDING".equalsIgnoreCase(order.getStatus())) {
            // Kiểm tra số tiền chuyển phải >= giá trị đơn hàng
            if (amount != null && amount.compareTo(order.getAmount()) >= 0) {
                aiQuotaService.activatePlanForUser(order.getUser(), order.getPlanCode());
                order.setStatus("SUCCESS");
                order.setCompletedAt(LocalDateTime.now());
                order.setTransactionRef(ref);
                paymentOrderRepository.save(order);

                log.info("Xác nhận thanh toán tự động thành công qua Webhook: orderCode={}, amount={}, user={}, plan={}",
                        order.getOrderCode(), amount, order.getUser().getEmail(), order.getPlanCode());
                return true;
            } else {
                log.warn("Đơn hàng {} nhận được số tiền {} nhỏ hơn giá trị đơn hàng {}",
                        order.getOrderCode(), amount, order.getAmount());
            }
        }
        return false;
    }

    private BigDecimal parseAmount(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        try {
            return new BigDecimal(obj.toString().trim());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private PaymentOrderResponse toResponse(PaymentOrder order, AiPlan plan) {
        String planName = plan != null ? plan.getName() : order.getPlanCode();
        long amountVal = order.getAmount() != null ? order.getAmount().longValue() : 0L;
        String encodedContent = URLEncoder.encode(order.getTransferContent(), StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(ACCOUNT_NAME, StandardCharsets.UTF_8);
        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                BANK_ID, ACCOUNT_NO, amountVal, encodedContent, encodedName);

        return new PaymentOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getPlanCode(),
                planName,
                order.getAmount(),
                order.getTransferContent(),
                BANK_ID,
                BANK_NAME,
                ACCOUNT_NO,
                ACCOUNT_NAME,
                qrUrl,
                order.getStatus(),
                order.getCreatedAt(),
                order.getCompletedAt()
        );
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Vui lòng đăng nhập.");
        return userRepository.findById(principal.getUser().getUserId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Tài khoản không tồn tại."));
    }
}
