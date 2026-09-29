package com.example.cvadvisorplatform.controller;

import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.example.cvadvisorplatform.dto.CareerRoadmapResponse;
import com.example.cvadvisorplatform.dto.CurrentUserUpdateRequest;
import com.example.cvadvisorplatform.model.CV;
import com.example.cvadvisorplatform.service.CurrentUserService;
import com.example.cvadvisorplatform.service.FileValidationService;
import com.example.cvadvisorplatform.service.OpenRouterService;
import com.example.cvadvisorplatform.service.PdfTextExtractorService;
import com.example.cvadvisorplatform.service.UserCvService;
import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final OpenRouterService openRouterService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final FileValidationService fileValidationService;
    private final CurrentUserService currentUserService;
    private final UserCvService userCvService;

    @PostMapping(
            value = "/evaluate-cv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AiCvEvaluationResponse> evaluateCv(

            @RequestParam("cv") MultipartFile cvFile,

            @RequestParam(value = "jobDescription", required = false, defaultValue = "")
            String jobDescription

    ) throws Exception {
        fileValidationService.validateCv(cvFile, true);
        String cvContent =
                pdfTextExtractorService.extractText(cvFile);

        AiCvEvaluationRequest request =
                new AiCvEvaluationRequest();

        request.setCvContent(cvContent);

        request.setJobDescription(jobDescription);

        AiCvEvaluationResponse response =
                openRouterService.evaluateCv(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/career-roadmap",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CareerRoadmapResponse> generateCareerRoadmap(
            @RequestParam("cv") MultipartFile cvFile,
            @RequestParam(value = "targetRole", required = false, defaultValue = "") String targetRole,
            @RequestParam(value = "desiredRoadmap", required = false, defaultValue = "") String desiredRoadmap
    ) throws Exception {
        fileValidationService.validateCv(cvFile, true);
        String cvContent = pdfTextExtractorService.extractText(cvFile);

        CareerRoadmapResponse response =
                openRouterService.generateCareerRoadmap(cvContent, targetRole, desiredRoadmap);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint: POST /api/v1/ai/parse-profile-from-cv
     * Trích xuất thông tin hồ sơ từ CV bằng AI.
     * - Nếu gửi file CV mới (multipart "cv"): dùng file đó để extract text.
     * - Nếu gửi cvId: dùng CV đã đính kèm của người dùng (phải là PDF).
     * - Nếu không gửi gì: dùng CV mới nhất của người dùng.
     */
    @PostMapping(
            value = "/parse-profile-from-cv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<CurrentUserUpdateRequest> parseProfileFromCv(
            @RequestParam(value = "cv", required = false) MultipartFile cvFile,
            @RequestParam(value = "cvId", required = false) Long cvId
    ) throws Exception {
        Long userId = currentUserService.getCurrentUser().getId();

        String cvContent;
        if (cvFile != null && !cvFile.isEmpty()) {
            // Người dùng upload file mới
            fileValidationService.validateCv(cvFile, true);
            cvContent = pdfTextExtractorService.extractText(cvFile);
        } else {
            // Dùng CV đã đính kèm trong hệ thống
            CV cv = userCvService.getOwnedCvOrLatest(userId, cvId);
            if (cv == null) {
                return ResponseEntity.badRequest().build();
            }
            cvContent = userCvService.resolveCvText(userId, cv);
            if (cvContent == null || cvContent.isBlank()) {
                return ResponseEntity.badRequest().build();
            }
        }

        CurrentUserUpdateRequest parsed = openRouterService.parseProfileFromCv(cvContent);
        return ResponseEntity.ok(parsed);
    }

//    @PostMapping(
//            value = "/candidate-fit",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    public ResponseEntity<AiCandidateFitResponse> evaluateCandidateFit(
//            @RequestParam("cv") MultipartFile cvFile,
//            @RequestParam("jobDescription") String jobDescription
//    ) throws Exception {
//
//        String cvContent = pdfTextExtractorService.extractText(cvFile);
//
//        AiCandidateFitResponse response =
//                openRouterService.evaluateCandidateFit(
//                        cvContent,
//                        jobDescription
//                );
//
//        return ResponseEntity.ok(response);
//    }
}
