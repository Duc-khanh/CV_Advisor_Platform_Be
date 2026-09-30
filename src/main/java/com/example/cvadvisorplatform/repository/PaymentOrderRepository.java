package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(String orderCode);
    Optional<PaymentOrder> findByTransferContent(String transferContent);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
