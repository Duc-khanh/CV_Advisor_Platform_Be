package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.dto.AppliedCandidateResponse;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobApplicationService;
import com.example.cvadvisorplatform.service.JobApplicationEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class CompanyApplicationController {

    private final JobApplicationService service;
    private final JobApplicationEvaluationService evaluationService;

    @GetMapping
    public Page<AppliedCandidateResponse> getAllApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null || principal.getUser().getCompany() == null) {
            throw new RuntimeException("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }

        Long companyId =
                principal.getUser()
                        .getCompany()
                        .getCompanyId();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));

        return service.getAllCandidatesByCompany(companyId, pageable);
    }

    @GetMapping("/{applicationId}/cv")
    public ResponseEntity<InputStreamResource> downloadCv(
            @PathVariable Long applicationId,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null || principal.getUser().getCompany() == null) return ResponseEntity.status(403).build();
        Long companyId = principal.getUser().getCompany().getCompanyId();
        JobApplicationService.ApplicationCv cv = service.getCvForCompany(applicationId, companyId);

        String fileName = cv.fileName() != null ? cv.fileName() : "resume.pdf";
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileName.toLowerCase().endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (fileName.toLowerCase().endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        }

        ContentDisposition disposition = download
                ? ContentDisposition.attachment().filename(fileName).build()
                : ContentDisposition.inline().filename(fileName).build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(cv.stream()));
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null || principal.getUser() == null || principal.getUser().getCompany() == null) {
            return ResponseEntity.status(403)
                    .body(Map.of("message", "Bạn chưa đăng nhập hoặc không thuộc công ty nào."));
        }

        Long companyId =
                principal.getUser()
                        .getCompany()
                        .getCompanyId();

        String targetStatus = (status != null && !status.isBlank())
                ? status
                : (body != null ? body.get("status") : null);

        if (targetStatus == null || targetStatus.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Vui lòng chọn trạng thái mới cần cập nhật."));
        }

        try {

            service.updateApplicationStatus(
                    applicationId,
                    companyId,
                    targetStatus
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Cập nhật trạng thái thành công",
                    "status", targetStatus.toUpperCase()
            ));

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Lỗi khi cập nhật trạng thái"));
        }
    }

    // ================= AI FIT =================

    @PostMapping("/{applicationId}/fit")
    public ResponseEntity<?> evaluateCandidateFit(
            @PathVariable Long applicationId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        try {

            if (principal == null || principal.getUser().getCompany() == null) {
                return ResponseEntity.status(403)
                        .body("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
            }

            Long companyId = principal.getUser().getCompany().getCompanyId();

            // Tải đơn ứng tuyển trực tiếp bằng ID và Company ID (Sửa lỗi tải toàn bộ lên RAM)
            JobApplication application =
                    service.getApplicationByIdAndCompanyId(applicationId, companyId);

            String jobDescription =
                    request.get("jobDescription");

            // Ủy thác việc cache và xử lý AI cho Service
            AiCandidateFitResponse response =
                    evaluationService.evaluateCandidateFit(
                            application,
                            jobDescription
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}
