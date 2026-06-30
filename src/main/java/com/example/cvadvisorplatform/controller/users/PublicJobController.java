package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    @GetMapping
    public List<JobPublicResponse> getPublicJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return publicJobService.getPublicJobs(keyword, location, userId);
    }

    @GetMapping("/{id}")
    public JobPublicResponse getJobById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return publicJobService.getJobById(id, userId);
    }
}
