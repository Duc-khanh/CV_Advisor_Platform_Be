package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.AiCvRewriteRequest;
import com.example.cvadvisorplatform.dto.AiCvRewriteResponse;
import com.example.cvadvisorplatform.dto.CvRequest;
import com.example.cvadvisorplatform.dto.CvResponse;
import com.example.cvadvisorplatform.service.CurrentUserService;
import com.example.cvadvisorplatform.service.UserCvService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import java.util.List;

@RestController
@RequestMapping("/api/user/cvs")
@RequiredArgsConstructor
public class UserCvController {

    private final UserCvService userCvService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CvResponse> uploadCv(@RequestPart("file") MultipartFile file) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.uploadCv(userId, file));
    }

    @GetMapping("/{cvId}/file")
    public ResponseEntity<Resource> getCvFile(@PathVariable Long cvId) {
        Long userId = currentUserService.getCurrentUser().getId();
        UserCvService.CvFile cvFile = userCvService.getCvFile(userId, cvId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cvFile.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(cvFile.fileName(), StandardCharsets.UTF_8).build().toString())
                .contentLength(cvFile.size())
                .body(cvFile.resource());
    }

    @PostMapping
    public ResponseEntity<CvResponse> createCv(@RequestBody CvRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.saveCv(userId, request));
    }

    @PutMapping("/{cvId}")
    public ResponseEntity<CvResponse> updateCv(
            @PathVariable Long cvId,
            @RequestBody CvRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.updateCv(userId, cvId, request));
    }

    @GetMapping
    public ResponseEntity<List<CvResponse>> getAllCvs() {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.getAllCvsByUser(userId));
    }

    @GetMapping("/{cvId}")
    public ResponseEntity<CvResponse> getCvById(@PathVariable Long cvId) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.getCvById(userId, cvId));
    }

    @PostMapping("/ai/rewrite")
    public ResponseEntity<AiCvRewriteResponse> rewriteCvText(@RequestBody AiCvRewriteRequest request) {
        Long userId = currentUserService.getCurrentUser().getId();
        return ResponseEntity.ok(userCvService.rewriteCvText(userId, request));
    }
    @DeleteMapping("/{cvId}")
    public ResponseEntity<Void> deleteCv(@PathVariable Long cvId) {
        Long userId = currentUserService.getCurrentUser().getId();
        userCvService.deleteCv(userId, cvId);
        return ResponseEntity.ok().build();
    }
}
