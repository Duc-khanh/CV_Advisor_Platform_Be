package com.example.cvadvisorplatform.repository;

import com.example.cvadvisorplatform.model.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    // Lấy danh sách lịch phỏng vấn theo danh sách applicationId
    List<Interview> findByApplication_IdIn(List<Long> applicationIds);

    // Lấy tất cả lịch của company (phân trang, sắp xếp theo startTime DESC)
    @Query(value = """
        SELECT iv FROM Interview iv
        LEFT JOIN FETCH iv.application app
        LEFT JOIN FETCH app.user u
        LEFT JOIN FETCH app.job j
        WHERE iv.company.companyId = :companyId
    """, countQuery = """
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
    """)
    Page<Interview> findAllByCompany(
            @Param("companyId") Long companyId,
            Pageable pageable
    );

    // Lọc theo status
    @Query(value = """
        SELECT iv FROM Interview iv
        LEFT JOIN FETCH iv.application app
        LEFT JOIN FETCH app.user u
        LEFT JOIN FETCH app.job j
        WHERE iv.company.companyId = :companyId
          AND iv.status = :status
    """, countQuery = """
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
          AND iv.status = :status
    """)
    Page<Interview> findAllByCompanyAndStatus(
            @Param("companyId") Long companyId,
            @Param("status") String status,
            Pageable pageable
    );

    // Lấy theo khoảng ngày (dùng cho Calendar view)
    @Query("""
        SELECT iv FROM Interview iv
        LEFT JOIN FETCH iv.application app
        LEFT JOIN FETCH app.user u
        LEFT JOIN FETCH app.job j
        WHERE iv.company.companyId = :companyId
          AND iv.startTime BETWEEN :from AND :to
        ORDER BY iv.startTime ASC
    """)
    List<Interview> findAllByCompanyAndDateRange(
            @Param("companyId") Long companyId,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to
    );

    // Lấy chi tiết 1 lịch theo id + company (bảo mật)
    @Query("""
        SELECT iv FROM Interview iv
        LEFT JOIN FETCH iv.application app
        LEFT JOIN FETCH app.user u
        LEFT JOIN FETCH app.job j
        WHERE iv.id = :id
          AND iv.company.companyId = :companyId
    """)
    Optional<Interview> findByIdAndCompany(
            @Param("id")        Long id,
            @Param("companyId") Long companyId
    );

    // ─── Thống kê Dashboard ──────────────────────────────────────────────────

    @Query("""
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
          AND iv.startTime >= :startOfDay AND iv.startTime <= :endOfDay
          AND iv.status != 'CANCELLED'
    """)
    long countToday(
            @Param("companyId")  Long companyId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay")   LocalDateTime endOfDay
    );

    @Query("""
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
          AND iv.startTime >= :now
          AND iv.status IN ('SCHEDULED', 'RESCHEDULED')
    """)
    long countUpcoming(
            @Param("companyId") Long companyId,
            @Param("now")       LocalDateTime now
    );

    @Query("""
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
          AND iv.status != 'CANCELLED'
          AND iv.startTime <= :now
          AND (iv.rating IS NULL OR iv.status != 'COMPLETED')
    """)
    long countPendingFeedback(
            @Param("companyId") Long companyId,
            @Param("now")       LocalDateTime now
    );

    @Query("""
        SELECT COUNT(iv) FROM Interview iv
        WHERE iv.company.companyId = :companyId
          AND iv.status = 'COMPLETED'
          AND iv.startTime >= :startOfMonth AND iv.startTime <= :endOfMonth
    """)
    long countCompletedThisMonth(
            @Param("companyId")    Long companyId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth")   LocalDateTime endOfMonth
    );
}

