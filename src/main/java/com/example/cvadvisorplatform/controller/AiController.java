package com.example.cvadvisorplatform.controller;

import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.example.cvadvisorplatform.dto.CareerRoadmapResponse;
import com.example.cvadvisorplatform.service.OpenRouterService;
import com.example.cvadvisorplatform.service.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiController {

    private final OpenRouterService openRouterService;
    private final PdfTextExtractorService pdfTextExtractorService;

    @PostMapping(
            value = "/evaluate-cv",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AiCvEvaluationResponse> evaluateCv(

            @RequestParam("cv") MultipartFile cvFile,

            @RequestParam(value = "jobDescription", required = false, defaultValue = "")
            String jobDescription

    ) throws Exception {

        String cvContent =
                pdfTextExtractorService.extractText(cvFile);

        System.out.println("===== CV TEXT =====");
        System.out.println(cvContent);

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

        String cvContent = pdfTextExtractorService.extractText(cvFile);

        CareerRoadmapResponse response =
                openRouterService.generateCareerRoadmap(cvContent, targetRole, desiredRoadmap);

        return ResponseEntity.ok(response);
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