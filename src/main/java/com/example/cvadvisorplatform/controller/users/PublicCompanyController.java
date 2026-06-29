package com.example.cvadvisorplatform.controller.users;

import com.example.cvadvisorplatform.dto.CompanyPublicResponse;
import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.security.UserPrincipal;
import com.example.cvadvisorplatform.service.PublicCompanyService;
import com.example.cvadvisorplatform.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/public/companies")
@RequiredArgsConstructor
public class PublicCompanyController {

    private final PublicCompanyService publicCompanyService;
    private final PublicJobService publicJobService;

    @GetMapping("/top")
    public List<CompanyPublicResponse> getTopCompanies() {
        return publicCompanyService.getTopCompanies();
    }

    @GetMapping("/{id}")
    public CompanyPublicResponse getCompanyById(@PathVariable Long id) {
        return publicCompanyService.getCompanyById(id);
    }

    @GetMapping("/{id}/jobs")
    public List<JobPublicResponse> getCompanyJobs(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = (principal != null) ? principal.getUser().getUserId() : null;
        return publicJobService.getActiveJobsByCompanyId(id, userId);
    }
}
