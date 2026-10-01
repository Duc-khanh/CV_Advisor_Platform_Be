package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.JobCreateRequest;
import com.example.cvadvisorplatform.dto.JobResponse;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HrJobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;



    /* ================= LẤY HR ĐANG LOGIN ================= */
    private User getCurrentHr() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Company getHrCompany() {
        Company company = getCurrentHr().getCompany();
        if (company == null) {
            throw new RuntimeException("HR chưa được gán công ty");
        }
        return company;
    }

    /* ================= HR XEM JOB CỦA CÔNG TY ================= */
    public List<JobResponse> getMyCompanyJobs() {
        Company company = getHrCompany();

        return jobRepository
                .findByCompanyCompanyId(company.getCompanyId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ================= THÊM JOB ================= */
    @Transactional
    public JobResponse createJob(JobCreateRequest request, MultipartFile image) {
        Company company = getHrCompany();

        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setDescription(request.getDescription());
        job.setCandidateRequirements(request.getCandidateRequirements());

        job.setLocation(request.getLocation());
        job.setSalaryRange(request.getSalaryRange());
        job.setJobType(request.getJobType());
        job.setVacancies(request.getVacancies());
        job.setExpiredAt(request.getExpiredAt());
        job.setRequiredSkills(request.getRequiredSkills());

        job.setActive(true);
        job.setCompany(company);

        // ✅ XỬ LÝ ẢNH
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeJobImage(image);
            job.setImageUrl(imageUrl);
        }

        jobRepository.save(job);
        return mapToResponse(job);
    }


    /* ================= SỬA JOB ================= */
    @Transactional
    public JobResponse updateJob(Long jobId, JobCreateRequest request, MultipartFile image) {
        Company company = getHrCompany();

        Job job = jobRepository
                .findByJobIdAndCompanyCompanyId(jobId, company.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Job không tồn tại"));

        job.setTitle(request.getTitle());
        job.setExperienceLevel(request.getExperienceLevel());
        job.setDescription(request.getDescription());
        job.setCandidateRequirements(request.getCandidateRequirements());

        job.setLocation(request.getLocation());
        job.setSalaryRange(request.getSalaryRange());
        job.setJobType(request.getJobType());
        job.setVacancies(request.getVacancies());
        job.setExpiredAt(request.getExpiredAt());
        job.setRequiredSkills(request.getRequiredSkills());

        // ✅ UPDATE ẢNH (nếu có)
        if (image != null && !image.isEmpty()) {
            String imageUrl = fileStorageService.storeJobImage(image);
            job.setImageUrl(imageUrl);
        }

        jobRepository.save(job);
        return mapToResponse(job);
    }


    /* ================= XÓA JOB ================= */
    @Transactional
    public void deleteJob(Long jobId) {
        Company company = getHrCompany();

        Job job = jobRepository
                .findByJobIdAndCompanyCompanyId(jobId, company.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Job không tồn tại"));

        jobRepository.delete(job);
    }

    /* ================= TÌM / LỌC JOB ================= */
    public List<JobResponse> getMyCompanyJobs(String keyword, String experienceLevel) {
        Company company = getHrCompany();
        Long companyId = company.getCompanyId();

        List<Job> jobs;

        if (keyword != null && !keyword.isBlank()
                && experienceLevel != null && !experienceLevel.isBlank()) {

            jobs = jobRepository
                    .findByCompanyCompanyIdAndTitleContainingIgnoreCaseAndExperienceLevel(
                            companyId, keyword, experienceLevel
                    );

        } else if (keyword != null && !keyword.isBlank()) {

            jobs = jobRepository
                    .findByCompanyCompanyIdAndTitleContainingIgnoreCase(
                            companyId, keyword
                    );

        } else if (experienceLevel != null && !experienceLevel.isBlank()) {

            jobs = jobRepository
                    .findByCompanyCompanyIdAndExperienceLevel(
                            companyId, experienceLevel
                    );

        } else {
            jobs = jobRepository.findByCompanyCompanyId(companyId);
        }

        return jobs.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /* ================= MAP ENTITY → DTO ================= */
    private JobResponse mapToResponse(Job job) {
        JobResponse dto = new JobResponse();

        dto.setJobId(job.getJobId());
        dto.setTitle(job.getTitle());
        dto.setExperienceLevel(job.getExperienceLevel());
        dto.setDescription(job.getDescription());

        dto.setCompanyName(job.getCompany().getCompanyName());

        dto.setLocation(job.getLocation());
        dto.setSalaryRange(job.getSalaryRange());
        dto.setJobType(job.getJobType());

        dto.setVacancies(job.getVacancies());
        dto.setActive(job.getActive());
        dto.setExpiredAt(job.getExpiredAt());
        dto.setViewCount(job.getViewCount());

        dto.setRequiredSkills(job.getRequiredSkills());
        dto.setCreatedAt(job.getCreatedAt());
        dto.setCandidateRequirements(job.getCandidateRequirements());
        dto.setImageUrl(job.getImageUrl());
        if (job.getCompany() != null && job.getCompany().getLogoUrl() != null && !job.getCompany().getLogoUrl().isBlank()) {
            dto.setCompanyLogo(job.getCompany().getLogoUrl());
        } else {
            dto.setCompanyLogo(job.getImageUrl());
        }


        return dto;
    }
}
