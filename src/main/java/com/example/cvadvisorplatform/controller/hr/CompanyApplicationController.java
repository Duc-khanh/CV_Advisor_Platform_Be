package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.dto.AppliedCandidateResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobApplicationService;
import com.example.cvadvisorplatform.service.OpenRouterService;
import com.example.cvadvisorplatform.service.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class CompanyApplicationController {

    private final JobApplicationService service;
    private final OpenRouterService openRouterService;
    private final PdfTextExtractorService pdfTextExtractorService;

    @org.springframework.beans.factory.annotation.Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping
    public List<AppliedCandidateResponse> getAllApplications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null || principal.getUser().getCompany() == null) {
            throw new RuntimeException("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }

        Long companyId =
                principal.getUser()
                        .getCompany()
                        .getCompanyId();

        return service.getAllCandidatesByCompany(companyId);
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null || principal.getUser().getCompany() == null) {
            return ResponseEntity.status(403)
                    .body("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }

        Long companyId =
                principal.getUser()
                        .getCompany()
                        .getCompanyId();

        try {

            service.updateApplicationStatus(
                    applicationId,
                    companyId,
                    status
            );

            return ResponseEntity.ok()
                    .body("Cập nhật trạng thái thành công");

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
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

            AppliedCandidateResponse application =
                    service.getAllCandidatesByCompany(companyId)
                            .stream()
                            .filter(app -> app.getApplicationId().equals(applicationId))
                            .findFirst()
                            .orElse(null);

            if (application == null) {
                return ResponseEntity.status(404)
                        .body("Không tìm thấy ứng viên hoặc ứng viên không thuộc công ty của bạn");
            }

            String jobDescription =
                    request.get("jobDescription");

            // Đường dẫn CV
            Path cvPath = Path.of(
                    uploadDir,
                    "cv",
                    application.getCvFileUrl()
            );

            // Đọc text từ PDF
            if (!Files.exists(cvPath)) {
                return ResponseEntity.status(404)
                        .body("Không tìm thấy file CV: " + cvPath.toAbsolutePath());
            }

            String cvContent =
                    pdfTextExtractorService.extractText(
                            Files.newInputStream(cvPath)
                    );

            AiCandidateFitResponse response =
                    openRouterService.evaluateCandidateFit(
                            cvContent,
                            jobDescription
                    );

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }
}