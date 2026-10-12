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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
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
    private final NotificationService notificationService;
    private final MailService mailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int ORDER_EXPIRATION_MINUTES = 30;

    @Value("${app.payment.bank-id:MB}")
    private String bankId;

    @Value("${app.payment.bank-name:Ngân hàng TMCP Quân Đội (MB Bank)}")
    private String bankName;

    @Value("${app.payment.account-no:}")
    private String accountNo;

    @Value("${app.payment.account-name:}")
    private String accountName;

    /**
     * 1. Tạo đơn hàng nâng cấp gói AI mới cho người dùng
     */
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

        // Kiểm tra nâng cấp so với gói hiện tại
        UserSubscription currentSub = subscriptionRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (currentSub != null && currentSub.getPlan() != null) {
            if (currentSub.getPlan().getCode().equalsIgnoreCase(targetPlan.getCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ALREADY_ACTIVE", "Bạn đang sử dụng gói " + targetPlan.getName() + " rồi.");
            }
            if (targetPlan.getMonthlyPrice().compareTo(currentSub.getPlan().getMonthlyPrice()) <= 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "DOWNGRADE_NOT_ALLOWED", "Bạn chỉ có thể nâng cấp lên gói có thứ hạng cao hơn gói hiện tại.");
            }
        }

        // Tự động hủy các đơn hàng PENDING cũ của user
        paymentOrderRepository.findTopByUser_UserIdAndStatusOrderByCreatedAtDesc(user.getUserId(), "PENDING")
                .ifPresent(oldOrder -> {
                    oldOrder.setStatus("CANCELLED");
                    paymentOrderRepository.save(oldOrder);
                });

        // Tạo mã đơn hàng bảo mật ngẫu nhiên dạng CVxxxxxx
        String orderCode = generateUniqueOrderCode();

        LocalDateTime now = LocalDateTime.now();
        PaymentOrder order = new PaymentOrder();
        order.setOrderCode(orderCode);
        order.setUser(user);
        order.setPlanCode(targetPlan.getCode());
        order.setAmount(targetPlan.getMonthlyPrice());
        order.setTransferContent(orderCode);
        order.setStatus("PENDING");
        order.setCreatedAt(now);
        order.setExpiresAt(now.plusMinutes(ORDER_EXPIRATION_MINUTES));
        paymentOrderRepository.save(order);

        log.info("Khởi tạo đơn hàng nâng cấp AI thành công: orderCode={}, user={}, plan={}, amount={}",
                orderCode, user.getEmail(), targetPlan.getCode(), targetPlan.getMonthlyPrice());

        return toResponse(order, targetPlan);
    }

    /**
     * 2. Tra cứu trạng thái đơn hàng (Có bảo vệ quyền sở hữu & kiểm tra hết hạn)
     */
    @Transactional
    public PaymentOrderResponse getOrderStatus(String orderCode) {
        User user = currentUser();
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
        if (!isAdmin && (order.getUser() == null || !Objects.equals(order.getUser().getUserId(), user.getUserId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền xem thông tin đơn hàng này.");
        }

        // Tự động cập nhật EXPIRED nếu quá thời hạn thanh toán
        if ("PENDING".equalsIgnoreCase(order.getStatus()) && order.getExpiresAt() != null && order.getExpiresAt().isBefore(LocalDateTime.now())) {
            order.setStatus("EXPIRED");
            paymentOrderRepository.save(order);
        }

        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    /**
     * 3. Lấy danh sách lịch sử đơn hàng thanh toán của người dùng hiện tại
     */
    @Transactional(readOnly = true)
    public Page<PaymentOrderResponse> getMyOrders(Pageable pageable) {
        User user = currentUser();
        Page<PaymentOrder> orders = paymentOrderRepository.findByUser_UserIdOrderByCreatedAtDesc(user.getUserId(), pageable);
        return orders.map(order -> {
            AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
            return toResponse(order, plan);
        });
    }

    /**
     * 4. Hủy đơn hàng đang chờ thanh toán
     */
    @Transactional
    public PaymentOrderResponse cancelOrder(String orderCode) {
        User user = currentUser();
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));

        boolean isAdmin = user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName());
        if (!isAdmin && (order.getUser() == null || !Objects.equals(order.getUser().getUserId(), user.getUserId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền thao tác trên đơn hàng này.");
        }

        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_CANCEL", "Chỉ có thể hủy đơn hàng đang ở trạng thái chờ thanh toán.");
        }

        order.setStatus("CANCELLED");
        paymentOrderRepository.save(order);
        log.info("Đơn hàng #{} đã được hủy bởi user {}", orderCode, user.getEmail());

        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    /**
     * 5. Xử lý Webhook tự động từ Casso / PayOS / Ngân hàng
     */
    @Transactional
    public Map<String, Object> processWebhook(Map<String, Object> payload) {
        log.info("Tiếp nhận Webhook thanh toán: {}", payload);
        if (payload == null || payload.isEmpty()) {
            return Map.of("success", false, "message", "Payload trống");
        }

        // Trường hợp 1: Webhook từ Casso: { data: [ { description, amount, tid } ] }
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

        // Trường hợp 2: Webhook từ PayOS: { code: "00", data: { description, amount, reference } }
        if (payload.containsKey("data") && payload.get("data") instanceof Map<?, ?> dataMap) {
            String desc = Objects.toString(dataMap.get("description"), "");
            BigDecimal amount = parseAmount(dataMap.get("amount"));
            String ref = Objects.toString(dataMap.get("reference"), Objects.toString(dataMap.get("orderCode"), ""));
            boolean success = processTransaction(desc, amount, ref);
            return Map.of("success", success);
        }

        // Trường hợp 3: Webhook trực tiếp: { orderCode/transferContent, amount, transactionRef }
        String desc = Objects.toString(payload.get("transferContent"),
                Objects.toString(payload.get("description"), Objects.toString(payload.get("orderCode"), "")));
        BigDecimal amount = parseAmount(payload.get("amount"));
        String ref = Objects.toString(payload.get("transactionRef"), "DIRECT");
        boolean success = processTransaction(desc, amount, ref);
        return Map.of("success", success);
    }

    /**
     * 6. Giả lập thanh toán (Dành cho Development / Test / Demo)
     */
    @Transactional
    public PaymentOrderResponse simulatePayment(String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));

        if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
            AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
            return toResponse(order, plan);
        }

        completePaymentAndNotify(order, "SIMULATED_" + System.currentTimeMillis());

        log.info("Giả lập thanh toán thành công cho đơn hàng: orderCode={}, user={}, plan={}",
                orderCode, order.getUser().getEmail(), order.getPlanCode());

        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    /**
     * 7. Quản trị viên tra cứu toàn bộ đơn hàng thanh toán
     */
    @Transactional(readOnly = true)
    public Page<PaymentOrderResponse> adminGetOrders(String search, String status, Pageable pageable) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) ? status.trim().toUpperCase() : null;

        Page<PaymentOrder> page = paymentOrderRepository.searchOrdersAdmin(cleanSearch, cleanStatus, pageable);
        return page.map(order -> {
            AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
            return toResponse(order, plan);
        });
    }

    /**
     * 8. Quản trị viên duyệt thanh toán thủ công (Manual Approve)
     */
    @Transactional
    public PaymentOrderResponse adminApproveOrder(String orderCode) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Không tìm thấy đơn hàng: " + orderCode));

        if ("SUCCESS".equalsIgnoreCase(order.getStatus())) {
            AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
            return toResponse(order, plan);
        }

        completePaymentAndNotify(order, "ADMIN_APPROVED_" + System.currentTimeMillis());
        log.info("Admin đã duyệt thủ công thành công đơn hàng #{}", orderCode);

        AiPlan plan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        return toResponse(order, plan);
    }

    /**
     * Helper: Khớp mã đơn hàng và xử lý giao dịch
     */
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

        if (order != null) {
            // Cho phép kích hoạt nếu đang PENDING hoặc EXPIRED (khách chuyển tiền trễ)
            if ("PENDING".equalsIgnoreCase(order.getStatus()) || "EXPIRED".equalsIgnoreCase(order.getStatus())) {
                if (amount != null && amount.compareTo(order.getAmount()) >= 0) {
                    completePaymentAndNotify(order, ref);
                    log.info("Xác nhận thanh toán tự động thành công qua Webhook: orderCode={}, amount={}, user={}, plan={}",
                            order.getOrderCode(), amount, order.getUser().getEmail(), order.getPlanCode());
                    return true;
                } else {
                    log.warn("Đơn hàng {} nhận được số tiền {} nhỏ hơn giá trị đơn hàng {}",
                            order.getOrderCode(), amount, order.getAmount());
                }
            }
        }
        return false;
    }

    /**
     * Helper: Kích hoạt gói AI, cập nhật trạng thái đơn và gửi thông báo + email
     */
    private void completePaymentAndNotify(PaymentOrder order, String transactionRef) {
        // 1. Kích hoạt gói AI
        aiQuotaService.activatePlanForUser(order.getUser(), order.getPlanCode());

        // 2. Cập nhật trạng thái đơn hàng
        order.setStatus("SUCCESS");
        order.setCompletedAt(LocalDateTime.now());
        order.setTransactionRef(transactionRef);
        paymentOrderRepository.save(order);

        AiPlan targetPlan = planRepository.findByCodeIgnoreCase(order.getPlanCode()).orElse(null);
        String planName = targetPlan != null ? targetPlan.getName() : order.getPlanCode();
        Integer monthlyCredits = targetPlan != null ? targetPlan.getMonthlyCredits() : null;

        // 3. Gửi thông báo hệ thống (In-App Notification)
        try {
            notificationService.createNotification(
                    order.getUser().getUserId(),
                    "PAYMENT",
                    "Nâng cấp gói AI thành công!",
                    "Gói " + planName + " đã được kích hoạt thành công cho tài khoản của bạn. Chúc bạn có trải nghiệm tuyệt vời cùng CvAdvisor!",
                    "/ai-pricing",
                    "Xem gói AI",
                    order.getId()
            );
        } catch (Exception e) {
            log.warn("Không thể tạo thông báo in-app cho đơn #{}: {}", order.getOrderCode(), e.getMessage());
        }

        // 4. Gửi Email xác nhận thanh toán
        try {
            mailService.sendPaymentSuccessConfirmation(
                    order.getUser().getEmail(),
                    order.getUser().getFullName(),
                    order.getOrderCode(),
                    planName,
                    order.getAmount(),
                    monthlyCredits
            );
        } catch (Exception e) {
            log.warn("Không thể gửi email xác nhận thanh toán cho đơn #{}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    private String generateUniqueOrderCode() {
        String code;
        int attempts = 0;
        do {
            code = "CV" + (100000 + SECURE_RANDOM.nextInt(900000));
            attempts++;
            if (attempts > 50) {
                code = "CV" + (System.currentTimeMillis() % 1000000);
            }
        } while (paymentOrderRepository.findByOrderCode(code).isPresent());
        return code;
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
        if (accountNo == null || accountNo.isBlank() || accountName == null || accountName.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PAYMENT_NOT_CONFIGURED", "Thông tin tài khoản nhận thanh toán chưa được cấu hình.");
        }
        String planName = plan != null ? plan.getName() : order.getPlanCode();
        long amountVal = order.getAmount() != null ? order.getAmount().longValue() : 0L;
        String encodedContent = URLEncoder.encode(order.getTransferContent(), StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String qrUrl = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankId, accountNo, amountVal, encodedContent, encodedName);

        return new PaymentOrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getPlanCode(),
                planName,
                order.getAmount(),
                order.getTransferContent(),
                bankId,
                bankName,
                accountNo,
                accountName,
                qrUrl,
                order.getStatus(),
                order.getCreatedAt(),
                order.getCompletedAt(),
                order.getExpiresAt()
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
