package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.service.PublicJobService;
import lombok.RequiredArgsConstructor;
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
            @RequestParam(required = false) String location
    ) {
        return publicJobService.getPublicJobs(keyword, location);
    }
    @GetMapping("/{id}")
    public JobPublicResponse getJobById(@PathVariable Long id) {
        return publicJobService.getJobById(id);
    }
}
