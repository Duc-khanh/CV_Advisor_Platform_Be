package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicJobService {

    private final JobRepository jobRepository;

    public List<JobPublicResponse> getPublicJobs(String keyword, String location) {

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
                .map(this::mapToPublicResponse)
                .toList();
    }

    private JobPublicResponse mapToPublicResponse(Job job) {
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

        return dto;
    }

    @Transactional
    public JobPublicResponse getJobById(Long id) {
        jobRepository.incrementViewCount(id);

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc với ID: " + id));

        return mapToPublicResponse(job);
    }
}