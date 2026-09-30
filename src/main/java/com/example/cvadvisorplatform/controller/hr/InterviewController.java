package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.InterviewFeedbackRequest;
import com.example.cvadvisorplatform.dto.InterviewRequest;
import com.example.cvadvisorplatform.dto.InterviewResponse;
import com.example.cvadvisorplatform.dto.InterviewStatsResponse;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/hr/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    // ─── Helper: lấy companyId từ principal ─────────────────────────────────

    private Long extractCompanyId(UserPrincipal principal) {
        if (principal == null || principal.getUser().getCompany() == null) {
            throw new RuntimeException("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }
        return principal.getUser().getCompany().getCompanyId();
    }

    private Company extractCompany(UserPrincipal principal) {
        if (principal == null || principal.getUser().getCompany() == null) {
            throw new RuntimeException("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }
        return principal.getUser().getCompany();
    }

    // ─── GET /api/hr/interviews ──────────────────────────────────────────────
    // Danh sách lịch phỏng vấn (phân trang, filter status)
    @GetMapping
    public ResponseEntity<?> getInterviews(
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(required = false)     String status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
            Page<InterviewResponse> result = interviewService.getInterviews(companyId, status, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── GET /api/hr/interviews/calendar ────────────────────────────────────
    // Danh sách gọn theo khoảng ngày (dành cho Calendar view)
    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt   = to.atTime(23, 59, 59);
            List<InterviewResponse> result = interviewService.getInterviewsForCalendar(companyId, fromDt, toDt);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── GET /api/hr/interviews/stats ──────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            InterviewStatsResponse result = interviewService.getStats(companyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── GET /api/hr/interviews/{id} ────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            InterviewResponse result = interviewService.getInterviewById(id, companyId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── POST /api/hr/interviews ─────────────────────────────────────────────
    // Tạo lịch phỏng vấn mới
    @PostMapping
    public ResponseEntity<?> create(
            @RequestBody InterviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            Company company = extractCompany(principal);
            InterviewResponse result = interviewService.createInterview(companyId, company, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── PUT /api/hr/interviews/{id} ────────────────────────────────────────
    // Cập nhật thông tin lịch
    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody InterviewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            InterviewResponse result = interviewService.updateInterview(id, companyId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── PATCH /api/hr/interviews/{id}/status ────────────────────────────────
    // Chỉ đổi trạng thái lịch
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            InterviewResponse result = interviewService.updateStatus(id, companyId, status);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── POST /api/hr/interviews/{id}/feedback ───────────────────────────────
    // Lưu kết quả đánh giá + tự động cập nhật trạng thái ứng viên
    @PostMapping("/{id}/feedback")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @RequestBody InterviewFeedbackRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            InterviewResponse result = interviewService.submitFeedback(id, companyId, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ─── DELETE /api/hr/interviews/{id} ──────────────────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        try {
            Long companyId = extractCompanyId(principal);
            interviewService.deleteInterview(id, companyId);
            return ResponseEntity.ok(Map.of("message", "Đã xóa lịch phỏng vấn thành công."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
