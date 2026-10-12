package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.PaymentOrderResponse;
import com.example.cvadvisorplatform.exception.ApiException;
import com.example.cvadvisorplatform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.payment.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.payment.simulation-enabled:false}")
    private boolean simulationEnabled;

    // ==========================================
    // 1. DÀNH CHO NGƯỜI DÙNG (USER / ME)
    // ==========================================

    /**
     * Tạo đơn hàng nâng cấp gói AI mới
     */
    @PostMapping("/api/me/ai-usage/orders")
    public PaymentOrderResponse createOrder(@RequestBody Map<String, String> body) {
        String planCode = body != null ? body.get("planCode") : null;
        return paymentService.createOrder(planCode);
    }

    /**
     * Lấy danh sách lịch sử đơn hàng thanh toán của người dùng hiện tại
     */
    @GetMapping("/api/me/ai-usage/orders")
    public Page<PaymentOrderResponse> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentService.getMyOrders(pageable);
    }

    /**
     * Lắng nghe / tra cứu trạng thái đơn hàng (Polling từ frontend)
     */
    @GetMapping("/api/me/ai-usage/orders/{orderCode}/status")
    public PaymentOrderResponse getOrderStatus(@PathVariable String orderCode) {
        return paymentService.getOrderStatus(orderCode);
    }

    /**
     * Hủy đơn hàng đang chờ thanh toán
     */
    @PostMapping("/api/me/ai-usage/orders/{orderCode}/cancel")
    public PaymentOrderResponse cancelOrder(@PathVariable String orderCode) {
        return paymentService.cancelOrder(orderCode);
    }

    // ==========================================
    // 2. WEBHOOK TỰ ĐỘNG (PUBLIC / CASSO / PAYOS)
    // ==========================================

    /**//kdjdhdhhdh
     * Health check cổng Webhook
     */
    @GetMapping("/api/public/payments/webhook")
    public Map<String, Object> webhookHealth() {
        return Map.of(
                "status", "ACTIVE",
                "service", "CvAdvisorPlatform Payment Webhook",
                "message", "Cổng Webhook đang hoạt động và sẵn sàng nhận thông báo chuyển khoản tự động từ Casso / PayOS!"
        );
    }

    /**
     * Tiếp nhận biến động số dư và tự động kích hoạt gói AI
     */
    @PostMapping("/api/public/payments/webhook")
    public Map<String, Object> handleWebhook(
            @RequestHeader(value = "secure-token", required = false) String cassoSecureToken,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String legacySecret,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestBody Map<String, Object> payload) {
        String providedSecret = cassoSecureToken != null && !cassoSecureToken.isBlank()
                ? cassoSecureToken
                : (legacySecret != null && !legacySecret.isBlank() ? legacySecret : apiKey);
        verifyWebhookSecret(providedSecret);
        return paymentService.processWebhook(payload);
    }

    // ==========================================
    // 3. GIẢ LẬP THANH TOÁN (DEMO / TEST / DEV)
    // ==========================================

    /**
     * Giả lập thanh toán thành công (chỉ hoạt động khi app.payment.simulation-enabled=true)
     */
    @PostMapping("/api/public/payments/simulate")
    public PaymentOrderResponse simulatePayment(@RequestParam String orderCode) {
        if (!simulationEnabled) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ENDPOINT_DISABLED", "Endpoint giả lập thanh toán đã bị tắt.");
        }
        return paymentService.simulatePayment(orderCode);
    }

    // ==========================================
    // 4. QUẢN TRỊ VIÊN (ADMIN DASHBOARD)
    // ==========================================

    /**
     * Quản trị viên xem và lọc toàn bộ đơn hàng thanh toán
     */
    @GetMapping("/api/admin/payments/orders")
    public Page<PaymentOrderResponse> adminGetOrders(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentService.adminGetOrders(search, status, pageable);
    }

    /**
     * Quản trị viên duyệt thủ công đơn hàng thanh toán
     */
    @PostMapping("/api/admin/payments/orders/{orderCode}/approve")
    public PaymentOrderResponse adminApproveOrder(@PathVariable String orderCode) {
        return paymentService.adminApproveOrder(orderCode);
    }

    /**
     * Xác thực Webhook Secret chống giả mạo
     */
    private void verifyWebhookSecret(String providedSecret) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "WEBHOOK_NOT_CONFIGURED", "Webhook thanh toán chưa được cấu hình Secret trên hệ thống.");
        }
        byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = providedSecret == null
                ? new byte[0]
                : providedSecret.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_WEBHOOK_SECRET", "Webhook secret không hợp lệ.");
        }
    }
}
