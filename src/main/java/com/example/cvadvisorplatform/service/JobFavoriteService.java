package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobFavorite;
import com.example.cvadvisorplatform.repository.JobFavoriteRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Quan trọng

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobFavoriteService {

    private final JobFavoriteRepository repository;
    private final JobRepository jobRepository;

    public boolean isFavorited(Long userId, Long jobId) {
        return repository.existsByUserIdAndJob_JobId(userId, jobId);
    }

    @Transactional
    public void toggleFavorite(Long userId, Long jobId) {

        if (repository.existsByUserIdAndJob_JobId(userId, jobId)) {
            repository.deleteByUserIdAndJob_JobId(userId, jobId);
            return;
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Không tìm thấy công việc với ID: " + jobId)
                );

        JobFavorite favorite = new JobFavorite();
        favorite.setUserId(userId);
        favorite.setJob(job);

        repository.save(favorite);
    }

    @Transactional(readOnly = true)
    public List<JobPublicResponse> getListFavorites(Long userId) {
        return repository.findAllByUserId(userId)
                .stream()
                .map(JobFavorite::getJob)
                .map(this::mapToJobPublicResponse)
                .toList();
    }


    private JobPublicResponse mapToJobPublicResponse(Job job) {
        JobPublicResponse dto = new JobPublicResponse();

        dto.setJobId(job.getJobId());
        dto.setTitle(job.getTitle());
        dto.setCompanyName(job.getCompany().getCompanyName());
        dto.setLocation(job.getLocation());


        dto.setJobType(job.getJobType());
        dto.setSalaryRange(job.getSalaryRange());
        dto.setExperienceLevel(job.getExperienceLevel());

        dto.setDescription(job.getDescription());
        dto.setCandidateRequirements(job.getCandidateRequirements());
        dto.setRequiredSkills(job.getRequiredSkills());

        dto.setVacancies(job.getVacancies());
        dto.setViewCount(job.getViewCount());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setExpiredAt(job.getExpiredAt());

        return dto;
    }

}
