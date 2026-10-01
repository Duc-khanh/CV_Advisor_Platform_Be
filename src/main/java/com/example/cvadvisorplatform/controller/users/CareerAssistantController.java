package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.CareerAssistantRequest;
import com.example.cvadvisorplatform.dto.CareerAssistantResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.CareerAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/career-assistant")
@RequiredArgsConstructor
public class CareerAssistantController {
    private final CareerAssistantService careerAssistantService;

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CareerAssistantResponse> chat(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CareerAssistantRequest request
    ) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        Long userId = principal.getUser().getUserId();
        return ResponseEntity.ok(careerAssistantService.chat(userId, request, null));
    }

    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CareerAssistantResponse> chatWithCv(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("request") CareerAssistantRequest request,
            @RequestPart(value = "cv", required = false) MultipartFile cv
    ) {
        if (principal == null) throw new RuntimeException("Unauthorized");
        Long userId = principal.getUser().getUserId();
        return ResponseEntity.ok(careerAssistantService.chat(userId, request, cv));
    }
}
