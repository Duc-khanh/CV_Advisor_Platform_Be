package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.PaymentOrderResponse;
import com.example.cvadvisorplatform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // 1. Tạo đơn hàng nâng cấp gói AI (Người dùng đăng nhập)
    @PostMapping("/api/me/ai-usage/orders")
    public PaymentOrderResponse createOrder(@RequestBody Map<String, String> body) {
        String planCode = body != null ? body.get("planCode") : null;
        return paymentService.createOrder(planCode);
    }

    // 2. Lắng nghe trạng thái đơn hàng (Polling từ màn hình QR của frontend)
    @GetMapping("/api/me/ai-usage/orders/{orderCode}/status")
    public PaymentOrderResponse getOrderStatus(@PathVariable String orderCode) {
        return paymentService.getOrderStatus(orderCode);
    }

    // 3. Webhook tiếp nhận biến động số dư từ Casso / PayOS / Ngân hàng
    @GetMapping("/api/public/payments/webhook")
    public Map<String, Object> webhookHealth() {
        return Map.of(
                "status", "ACTIVE",
                "service", "CvAdvisorPlatform Payment Webhook",
                "message", "Cổng Webhook đang hoạt động và sẵn sàng nhận thông báo chuyển khoản tự động từ Casso / PayOS!"
        );
    }

    @PostMapping("/api/public/payments/webhook")
    public Map<String, Object> handleWebhook(@RequestBody Map<String, Object> payload) {
        return paymentService.processWebhook(payload);
    }

    // 4. Giả lập thanh toán thành công (Sandbox / Demo / Thử nghiệm)
    @PostMapping("/api/public/payments/simulate")
    public PaymentOrderResponse simulatePayment(@RequestParam String orderCode) {
        return paymentService.simulatePayment(orderCode);
    }
}
