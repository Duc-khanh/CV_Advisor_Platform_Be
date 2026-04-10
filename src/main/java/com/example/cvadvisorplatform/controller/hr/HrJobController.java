package com.example.cvadvisorplatform.controller.hr;

import com.example.cvadvisorplatform.dto.JobCreateRequest;
import com.example.cvadvisorplatform.dto.JobResponse;
import com.example.cvadvisorplatform.service.HrJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/hr/jobs")
@RequiredArgsConstructor
public class HrJobController {

    private final HrJobService hrJobService;

     @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobResponse createJob(
            @ModelAttribute JobCreateRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return hrJobService.createJob(request, image);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JobResponse updateJob(
            @PathVariable Long id,
            @ModelAttribute JobCreateRequest request,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        return hrJobService.updateJob(id, request, image);
    }

     @DeleteMapping("/{id}")
    public void deleteJob(@PathVariable Long id) {
        hrJobService.deleteJob(id);
    }


    @GetMapping
    public List<JobResponse> getMyJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String experienceLevel
    ) {
        return hrJobService.getMyCompanyJobs(keyword, experienceLevel);
    }
}

