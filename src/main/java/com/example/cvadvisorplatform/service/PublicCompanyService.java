package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CompanyPublicResponse;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.repository.CompanyRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicCompanyService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;

    public List<CompanyPublicResponse> getTopCompanies() {
        List<Company> companies = companyRepository.findAll();

        return companies.stream()
                .map(company -> {
                    long activeJobCount = jobRepository.countByCompanyCompanyIdAndActiveTrue(company.getCompanyId());
                    CompanyPublicResponse dto = mapToResponse(company);
                    dto.setJobCount(activeJobCount);
                    return dto;
                })
                .sorted((c1, c2) -> {
                    // Prioritize companies with more active jobs
                    int compareJobs = Long.compare(c2.getJobCount(), c1.getJobCount());
                    if (compareJobs != 0) {
                        return compareJobs;
                    }
                    // If same job count, prioritize new companies (higher ID or creation date)
                    return c2.getCompanyId().compareTo(c1.getCompanyId());
                })
                .collect(Collectors.toList());
    }

    public CompanyPublicResponse getCompanyById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công ty với ID: " + id));
        CompanyPublicResponse dto = mapToResponse(company);
        dto.setJobCount(jobRepository.countByCompanyCompanyIdAndActiveTrue(id));
        return dto;
    }

    private CompanyPublicResponse mapToResponse(Company company) {
        CompanyPublicResponse dto = new CompanyPublicResponse();
        dto.setCompanyId(company.getCompanyId());
        dto.setCompanyName(company.getCompanyName());
        dto.setLogoUrl(company.getLogoUrl());
        dto.setAddress(company.getAddress());
        dto.setDescription(company.getDescription());
        dto.setWebsiteUrl(company.getWebsiteUrl());
        dto.setEmail(company.getEmail());
        dto.setPhone(company.getPhone());
        
        // Generate a stable rating based on companyId (between 4.3 and 4.9)
        double baseRating = 4.3;
        double addedRating = (company.getCompanyId() % 7) * 0.1;
        dto.setRating(Math.round((baseRating + addedRating) * 10.0) / 10.0);
        
        return dto;
    }
}
