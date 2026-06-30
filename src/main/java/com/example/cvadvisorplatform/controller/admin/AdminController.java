package com.example.cvadvisorplatform.controller.admin;

import com.example.cvadvisorplatform.dto.AdminCompanyCreateRequest;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Industry;
import com.example.cvadvisorplatform.repository.CompanyRepository;
import com.example.cvadvisorplatform.repository.IndustryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyRepository companyRepository;
    private final IndustryRepository industryRepository;

    @GetMapping("/dashboard")
    public String admin() {
        return "ADMIN DASHBOARD";
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

