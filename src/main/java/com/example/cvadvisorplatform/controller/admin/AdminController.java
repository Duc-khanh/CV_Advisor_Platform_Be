package com.example.cvadvisorplatform.controller.admin;

import com.example.cvadvisorplatform.dto.AdminCompanyCreateRequest;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Industry;
import com.example.cvadvisorplatform.repository.CompanyRepository;
import com.example.cvadvisorplatform.repository.IndustryRepository;
import com.example.cvadvisorplatform.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;
    private final AdminDashboardService adminDashboardService;
    private final com.example.cvadvisorplatform.service.SystemSettingService systemSettingService;
    private final com.example.cvadvisorplatform.service.OpenRouterService openRouterService;

    @GetMapping("/settings")
    public java.util.Map<String, Object> getSettings() {
        return systemSettingService.getSettings();
    }

    @PostMapping("/settings")
    public java.util.Map<String, Object> saveSettings(@RequestBody java.util.Map<String, Object> newSettings) {
        return systemSettingService.saveSettings(newSettings);
    }

    @PostMapping("/settings/test-ai")
    public java.util.Map<String, Object> testAi(@RequestBody java.util.Map<String, String> body) {
        String model = body != null ? body.get("model") : null;
        return openRouterService.testModelDirectly(model);
    }

    @GetMapping("/dashboard")
    public String admin() {
        return "ADMIN DASHBOARD";
    }

    @GetMapping("/dashboard/stats")
    public java.util.Map<String, Object> getDashboardStats() {
        return adminDashboardService.getDashboardStats();
    }

    @GetMapping("/stats/overview")
    public java.util.Map<String, Object> getDetailedStats(@RequestParam(defaultValue = "30d") String range) {
        return adminDashboardService.getDetailedStats(range);
    }

    @GetMapping("/industries")
    public List<Industry> getAllIndustries() {
        return industryRepository.findAll();
    }

    @PostMapping("/companies")
    public Company createCompany(@RequestBody AdminCompanyCreateRequest request) {
        Industry industry = industryRepository.findByIndustryName(request.getIndustryName())
                .orElseGet(() -> {
                    Industry newInd = new Industry();
                    newInd.setIndustryName(request.getIndustryName());
                    return industryRepository.save(newInd);
                });

        Company company = new Company();
        company.setCompanyName(request.getCompanyName());
        company.setIndustry(industry);
        company.setAddress(request.getAddress() != null ? request.getAddress() : "");
        company.setDescription(request.getDescription() != null ? request.getDescription() : "");
        return companyRepository.save(company);
    }
}

