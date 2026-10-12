package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.PaymentOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderCode(String orderCode);
    Optional<PaymentOrder> findByTransferContent(String transferContent);
    Optional<PaymentOrder> findTopByUser_UserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    List<PaymentOrder> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    Page<PaymentOrder> findByUser_UserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        SELECT p FROM PaymentOrder p
        WHERE (:status IS NULL OR p.status = :status)
          AND (:search IS NULL OR LOWER(p.orderCode) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.user.email) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.user.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(p.planCode) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.createdAt DESC
    """)
    Page<PaymentOrder> searchOrdersAdmin(@Param("search") String search,
                                         @Param("status") String status,
                                         Pageable pageable);
}
