package com.example.cvadvisorplatform.controller;

import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.example.cvadvisorplatform.service.OpenRouterService;
import com.example.cvadvisorplatform.service.PdfTextExtractorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

        // extract text từ PDF
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
}