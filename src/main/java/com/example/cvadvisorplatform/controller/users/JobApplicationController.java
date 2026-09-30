package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.AppliedJobResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/user/jobs/apply")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService service;

    @PostMapping("/{jobId}")
    public void apply(
            @PathVariable Long jobId,
            @AuthenticationPrincipal UserPrincipal principal, // Lấy User từ Token
            @RequestParam(value = "cv", required = false) MultipartFile cv,
            @RequestParam(value = "cvId", required = false) Long cvId
    ) throws IOException {
        // Kiểm tra xem user đã đăng nhập chưa
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }

        // Lấy userId trực tiếp từ đối tượng principal đã được JwtFilter xác thực
        Long userId = principal.getUser().getUserId();

        service.apply(userId, jobId, cv, cvId);
    }
    @GetMapping("/my-applications")
    public List<AppliedJobResponse> getMyApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "status", required = false) String status
    ) {
        Long userId = principal.getUser().getUserId();
        return service.getApplicationsByUserId(userId, status);
    }

    @GetMapping("/{applicationId}/cv")
    public ResponseEntity<InputStreamResource> downloadMyCv(
            @PathVariable Long applicationId,
            @RequestParam(value = "download", defaultValue = "false") boolean download,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.status(403).build();
        JobApplicationService.ApplicationCv cv =
                service.getCvForUser(applicationId, principal.getUser().getUserId());

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
}
