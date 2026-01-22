package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.JobCreateRequest;
import com.example.cvadvisorplatform.dto.JobResponse;
import com.example.cvadvisorplatform.service.HrJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final HrJobService hrJobService;

    /* ===== THÊM ===== */
    @PostMapping
    public JobResponse createJob(@RequestBody JobCreateRequest request) {
        return hrJobService.createJob(request);
    }

    /* ===== SỬA ===== */
    @PutMapping("/{id}")
    public JobResponse updateJob(
            @PathVariable Long id,
            @RequestBody JobCreateRequest request
    ) {
        return hrJobService.updateJob(id, request);
    }

    /* ===== XÓA ===== */
    @DeleteMapping("/{id}")
    public void deleteJob(@PathVariable Long id) {
        hrJobService.deleteJob(id);
    }

    /* ===== XEM / TÌM / LỌC ===== */
    @GetMapping
    public List<JobResponse> getMyJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String experienceLevel
    ) {
        return hrJobService.getMyCompanyJobs(keyword, experienceLevel);
    }
}

