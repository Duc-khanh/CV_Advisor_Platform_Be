package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.repository.JobFavoriteRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicJobService {

    private final JobRepository jobRepository;
    private final JobFavoriteRepository jobFavoriteRepository;

    public Page<JobPublicResponse> getPublicJobs(String keyword, String location, Long userId, Pageable pageable) {
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String cleanLocation = (location != null && !location.trim().isEmpty()) ? location.trim() : null;

        Page<Job> jobs = jobRepository.searchPublicJobs(cleanKeyword, cleanLocation, pageable);
        return jobs.map(job -> mapToPublicResponse(job, userId));
    }

    private JobPublicResponse mapToPublicResponse(Job job, Long userId) {
        JobPublicResponse dto = new JobPublicResponse();

        dto.setJobId(job.getJobId());
        dto.setTitle(job.getTitle());
        if (job.getCompany() != null) {
            dto.setCompanyName(job.getCompany().getCompanyName());
        }

        dto.setLocation(job.getLocation());
        dto.setSalaryRange(job.getSalaryRange());
        dto.setJobType(job.getJobType());
        dto.setExperienceLevel(job.getExperienceLevel());
        dto.setDescription(job.getDescription());
        dto.setCandidateRequirements(job.getCandidateRequirements());

        dto.setRequiredSkills(job.getRequiredSkills());
        dto.setViewCount(job.getViewCount());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setExpiredAt(job.getExpiredAt());

        dto.setImageUrl(job.getImageUrl());
        if (job.getCompany() != null && job.getCompany().getLogoUrl() != null && !job.getCompany().getLogoUrl().isBlank()) {
            dto.setCompanyLogo(job.getCompany().getLogoUrl());
        } else {
            dto.setCompanyLogo(job.getImageUrl());
        }

        // Kiểm tra trạng thái yêu thích nếu user đã đăng nhập
        if (userId != null) {
            dto.setFavorite(jobFavoriteRepository.existsByUserIdAndJob_JobId(userId, job.getJobId()));
        } else {
            dto.setFavorite(false);
        }

        return dto;
    }

    @Transactional
    public JobPublicResponse getJobById(Long id, Long userId) {
        jobRepository.incrementViewCount(id);

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        return mapToPublicResponse(job, userId);
    }

    public List<JobPublicResponse> getActiveJobsByCompanyId(Long companyId, Long userId) {
        List<Job> jobs = jobRepository.findByCompanyCompanyIdAndActiveTrueOrderByCreatedAtDesc(companyId);
        return jobs.stream()
                .map(job -> mapToPublicResponse(job, userId))
                .toList();
    }
}