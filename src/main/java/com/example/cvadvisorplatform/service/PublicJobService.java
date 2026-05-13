package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.repository.JobFavoriteRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicJobService {

    private final JobRepository jobRepository;
    private final JobFavoriteRepository jobFavoriteRepository;

    public List<JobPublicResponse> getPublicJobs(String keyword, String location, Long userId) {

        List<Job> jobs;

        if (keyword != null && !keyword.isBlank()
                && location != null && !location.isBlank()) {

            jobs = jobRepository
                    .findByActiveTrueAndTitleContainingIgnoreCaseAndLocationContainingIgnoreCase(
                            keyword, location
                    );

        } else if (keyword != null && !keyword.isBlank()) {

            jobs = jobRepository
                    .findByActiveTrueAndTitleContainingIgnoreCase(keyword);

        } else if (location != null && !location.isBlank()) {

            jobs = jobRepository
                    .findByActiveTrueAndLocationContainingIgnoreCase(location);

        } else {
            jobs = jobRepository.findByActiveTrue();
        }

        return jobs.stream()
                .map(job -> mapToPublicResponse(job, userId))
                .toList();
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
}