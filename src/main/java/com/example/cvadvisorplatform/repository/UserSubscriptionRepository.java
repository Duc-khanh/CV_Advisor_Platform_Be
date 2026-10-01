package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    Optional<UserSubscription> findByUser_UserId(Long userId);

    @Query("SELECT s FROM UserSubscription s " +
           "JOIN s.user u " +
           "JOIN s.plan p " +
           "WHERE (:search IS NULL OR :search = '' OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:planCode IS NULL OR :planCode = '' OR LOWER(p.code) = LOWER(:planCode)) " +
           "ORDER BY s.id DESC")
    Page<UserSubscription> searchSubscriptions(
            @Param("search") String search,
            @Param("planCode") String planCode,
            Pageable pageable
    );
}
