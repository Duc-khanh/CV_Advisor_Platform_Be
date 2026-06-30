package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AppliedCandidateResponse;
import com.example.cvadvisorplatform.dto.AppliedJobResponse;
import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobApplicationService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final JobApplicationRepository repository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // APPLY JOB
    public void apply(Long userId,
                      Long jobId,
                      MultipartFile cv) throws IOException {

        if (cv == null || cv.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file CV");
        }

        if (cv.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File CV vượt quá 5MB");
        }

        if (!ALLOWED_TYPES.contains(cv.getContentType())) {
            throw new RuntimeException("Chỉ cho phép file PDF / DOC / DOCX");
        }

        if (repository.existsByUser_UserIdAndJob_JobId(userId, jobId)) {
            throw new RuntimeException("Bạn đã ứng tuyển công việc này");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Job không tồn tại"));

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User không tồn tại"));

        String fileName =
                UUID.randomUUID() + "_" + cv.getOriginalFilename();

        Path uploadPath = Paths.get(uploadDir, "cv");

        Files.createDirectories(uploadPath);

        Files.copy(
                cv.getInputStream(),
                uploadPath.resolve(fileName)
        );

        JobApplication app = new JobApplication();

        app.setUser(user);
        app.setJob(job);
        app.setCvFile(fileName);
        app.setFileSize(cv.getSize());

        repository.save(app);

        // Gửi email xác nhận (chạy ngầm)
        mailService.sendApplicationConfirmation(
                user.getEmail(),
                user.getFullName(),
                job.getTitle(),
                job.getCompany().getCompanyName()
        );
    }

    // USER APPLICATIONS
    public List<AppliedJobResponse> getApplicationsByUserId(
            Long userId,
            String status
    ) {

        List<JobApplication> applications;

        if (status != null &&
                !status.trim().isEmpty() &&
                !status.equalsIgnoreCase("ALL")) {

            applications =
                    repository.findAllByUser_UserIdAndStatusOrderByIdDesc(
                            userId,
                            status
                    );

        } else {

            applications =
                    repository.findAllByUser_UserIdOrderByIdDesc(userId);
        }

        return applications.stream().map(app -> {

            AppliedJobResponse res =
                    new AppliedJobResponse();

            res.setApplicationId(app.getId());
            res.setStatus(app.getStatus());
            res.setApplyDate(app.getAppliedAt());
            res.setCvFileUrl(app.getCvFile());

            Job job = app.getJob();

            if (job != null) {

                JobPublicResponse jobRes =
                        new JobPublicResponse();

                jobRes.setJobId(job.getJobId());
                jobRes.setTitle(job.getTitle());
                jobRes.setCompanyName(
                        job.getCompany().getCompanyName()
                );
                jobRes.setLocation(job.getLocation());
                jobRes.setJobType(job.getJobType());
                jobRes.setSalaryRange(job.getSalaryRange());

                res.setJob(jobRes);
            }

            return res;

        }).toList();
    }

    // HR GET ALL CANDIDATES
    public List<AppliedCandidateResponse>
    getAllCandidatesByCompany(Long companyId) {

        List<JobApplication> applications =
                repository.findAllByCompanyId(companyId);

        return applications.stream().map(app -> {

            AppliedCandidateResponse res =
                    new AppliedCandidateResponse();

            res.setApplicationId(app.getId());
            res.setStatus(app.getStatus());
            res.setAppliedAt(app.getAppliedAt());
            res.setCvFileUrl(app.getCvFile());

            // USER INFO
            if (app.getUser() != null) {

                res.setUserId(app.getUser().getUserId());

                res.setFullName(
                        app.getUser().getFullName()
                );

                res.setEmail(
                        app.getUser().getEmail()
                );
            }

            // JOB INFO
            Job job = app.getJob();

            if (job != null) {

                res.setJobId(job.getJobId());

                res.setJobTitle(job.getTitle());

                res.setLocation(job.getLocation());

                res.setJobType(job.getJobType());

                res.setSalaryRange(
                        job.getSalaryRange()
                );
            }

            return res;

        }).toList();
    }

    // UPDATE STATUS
    public void updateApplicationStatus(
            Long applicationId,
            Long companyId,
            String status
    ) {

        List<String> validStatus = List.of(
                "PENDING",
                "REVIEWED",
                "INTERVIEW",
                "ACCEPTED",
                "REJECTED"
        );

        if (!validStatus.contains(status.toUpperCase())) {
            throw new RuntimeException(
                    "Trạng thái không hợp lệ"
            );
        }

        JobApplication application =
                repository.findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy đơn ứng tuyển"
                                ));

        if (!application.getJob()
                .getCompany()
                .getCompanyId()
                .equals(companyId)) {

            throw new RuntimeException(
                    "Bạn không có quyền cập nhật đơn này"
            );
        }

        application.setStatus(
                status.toUpperCase()
        );

        repository.save(application);
    }
}