package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.AppliedCandidateResponse;
import com.example.cvadvisorplatform.dto.UpdateApplicationStatusRequest;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class CompanyApplicationController {

    private final JobApplicationService service;

    @GetMapping
    public List<AppliedCandidateResponse> getAllApplications(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null || principal.getUser().getCompany() == null) {
            throw new RuntimeException("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }
        Long companyId = principal.getUser().getCompany().getCompanyId();
        return service.getAllCandidatesByCompany(companyId);
    }

    @PutMapping("/{applicationId}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        if (principal == null || principal.getUser().getCompany() == null) {
            return ResponseEntity.status(403).body("Bạn chưa đăng nhập hoặc không thuộc công ty nào.");
        }

        Long companyId = principal.getUser().getCompany().getCompanyId();

        try {
            service.updateApplicationStatus(
                    applicationId,
                    companyId,
                    status
            );
            return ResponseEntity.ok().body("Cập nhật trạng thái thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
