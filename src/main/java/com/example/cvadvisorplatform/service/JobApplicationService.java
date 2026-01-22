package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AppliedJobResponse;
import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
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

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final JobApplicationRepository repository;
    private final JobRepository jobRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    public void apply(Long userId, Long jobId, MultipartFile cv) throws IOException {

        /* ===== VALIDATE FILE ===== */
        if (cv == null || cv.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn file CV");
        }

        if (cv.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("File CV vượt quá 5MB");
        }

        if (!ALLOWED_TYPES.contains(cv.getContentType())) {
            throw new RuntimeException("Chỉ cho phép file PDF / DOC / DOCX");
        }

        /* ===== CHẶN ỨNG TUYỂN TRÙNG ===== */
        if (repository.existsByUserIdAndJob_JobId(userId, jobId)) {
            throw new RuntimeException("Bạn đã ứng tuyển công việc này");
        }

        /* ===== KIỂM TRA JOB ===== */
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job không tồn tại"));

        /* ===== LƯU FILE ===== */
        String fileName = UUID.randomUUID() + "_" + cv.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir, "cv");

        Files.createDirectories(uploadPath);
        Files.copy(cv.getInputStream(), uploadPath.resolve(fileName));

        /* ===== LƯU DB ===== */
        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setJob(job);
        app.setCvFile(fileName);
        app.setFileSize(cv.getSize());


        repository.save(app);
    }
    // Sửa đổi phương thức này
    public List<AppliedJobResponse> getApplicationsByUserId(Long userId, String status) {
        List<JobApplication> applications;

        // Logic lọc: Nếu status không rỗng và không phải là "ALL"
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            applications = repository.findAllByUserIdAndStatusOrderByIdDesc(userId, status);
        } else {
            applications = repository.findAllByUserIdOrderByIdDesc(userId);
        }

        return applications.stream().map(app -> {
            AppliedJobResponse res = new AppliedJobResponse();
            res.setApplicationId(app.getId());
            res.setStatus(app.getStatus());
            res.setApplyDate(app.getAppliedAt());
            res.setCvFileUrl(app.getCvFile());

            Job job = app.getJob();
            if (job != null) {
                JobPublicResponse jobRes = new JobPublicResponse();
                jobRes.setJobId(job.getJobId());
                jobRes.setTitle(job.getTitle());
                jobRes.setCompanyName(job.getCompany().getCompanyName());
                jobRes.setLocation(job.getLocation());
                jobRes.setJobType(job.getJobType());
                jobRes.setSalaryRange(job.getSalaryRange());
                res.setJob(jobRes);
            }
            return res;
        }).toList();
    }

}


