package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.dto.AppliedCandidateResponse;
import com.example.cvadvisorplatform.model.JobApplicationEvaluation;
import com.example.cvadvisorplatform.repository.JobApplicationEvaluationRepository;
import com.example.cvadvisorplatform.dto.InterviewSummaryResponse;
import com.example.cvadvisorplatform.model.Interview;
import com.example.cvadvisorplatform.repository.InterviewRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.cvadvisorplatform.dto.AppliedJobResponse;
import com.example.cvadvisorplatform.dto.JobPublicResponse;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
    private final FileStorageService fileStorageService;
    private final UserCvService userCvService;
    private final FileValidationService fileValidationService;
    private final JobApplicationEvaluationRepository evaluationRepository;
    private final JobApplicationEvaluationService evaluationService;
    private final InterviewRepository interviewRepository;


    // APPLY JOB
    @Transactional
    public void apply(Long userId,
                      Long jobId,
                      MultipartFile cv,
                      Long cvId) throws IOException {

        boolean hasUploadedFile = cv != null && !cv.isEmpty();
        boolean hasSavedCv = cvId != null;

        if (hasUploadedFile == hasSavedCv) {
            throw new RuntimeException("Vui lòng chọn một CV đã lưu hoặc tải lên một file CV mới");
        }

        if (repository.existsByUser_UserIdAndJob_JobId(userId, jobId)) {
            throw new RuntimeException("Bạn đã ứng tuyển công việc này");
        }

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Job không tồn tại"));
        if (!Boolean.TRUE.equals(job.getActive())) {
            throw new RuntimeException("Công việc không còn nhận hồ sơ");
        }
        if (job.getExpiredAt() != null && !job.getExpiredAt().isAfter(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Công việc đã hết hạn");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User không tồn tại"));

        String cvUrl;
        long cvFileSize;

        if (hasSavedCv) {
            UserCvService.CvFile savedCv = userCvService.getCvFile(userId, cvId);
            validateCv(savedCv.mimeType(), savedCv.size());
            try (var input = savedCv.resource().getInputStream()) {
                cvUrl = fileStorageService.storeCvFile(input.readAllBytes(), savedCv.fileName());
            }
            cvFileSize = savedCv.size();
        } else {
            fileValidationService.validateCv(cv, false);
            cvUrl = fileStorageService.storeCvFile(cv);
            cvFileSize = cv.getSize();
        }

        JobApplication app = new JobApplication();

        app.setUser(user);
        app.setJob(job);
        app.setCvFile(cvUrl);
        app.setFileSize(cvFileSize);

        JobApplication savedApp = repository.save(app);

        // Gửi email xác nhận (chạy ngầm)
        mailService.sendApplicationConfirmation(
                user.getEmail(),
                user.getFullName(),
                job.getTitle(),
                job.getCompany().getCompanyName()
        );

        // Tự động phân tích & đánh giá độ phù hợp của ứng viên bằng AI chạy ngầm
        try {
            StringBuilder jdBuilder = new StringBuilder();
            if (job.getTitle() != null && !job.getTitle().isBlank()) {
                jdBuilder.append("Vị trí tuyển dụng: ").append(job.getTitle()).append("\n");
            }
            if (job.getDescription() != null && !job.getDescription().isBlank()) {
                jdBuilder.append("Mô tả công việc: ").append(job.getDescription()).append("\n");
            }
            if (job.getCandidateRequirements() != null && !job.getCandidateRequirements().isBlank()) {
                jdBuilder.append("Yêu cầu ứng viên: ").append(job.getCandidateRequirements()).append("\n");
            }
            if (job.getRequiredSkills() != null && !job.getRequiredSkills().isEmpty()) {
                jdBuilder.append("Kỹ năng yêu cầu: ").append(String.join(", ", job.getRequiredSkills())).append("\n");
            }
            String jd = jdBuilder.toString().trim();
            if (jd.isBlank() && job.getTitle() != null) {
                jd = job.getTitle();
            }

            final String finalJd = jd;
            final Long appId = savedApp.getId();

            log.info("Lập lịch đánh giá AI ngầm cho đơn ứng tuyển ID: {}", appId);
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        log.info("Giao dịch đã commit. Bắt đầu kích hoạt đánh giá AI ngầm cho đơn ứng tuyển ID: {}", appId);
                        evaluationService.evaluateCandidateFitAsync(appId, finalJd);
                    }
                });
            } else {
                log.info("Không có giao dịch hoạt động. Kích hoạt trực tiếp đánh giá AI ngầm cho đơn ứng tuyển ID: {}", appId);
                evaluationService.evaluateCandidateFitAsync(appId, finalJd);
            }
        } catch (Exception e) {
            log.error("Lỗi khi lập lịch đánh giá AI ngầm cho đơn ứng tuyển ID {}: {}", savedApp.getId(), e.getMessage());
        }
    }

    private void validateCv(String contentType, long fileSize) {
        if (fileSize <= 0) {
            throw new RuntimeException("File CV không được để trống");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new RuntimeException("File CV vượt quá 5MB");
        }
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new RuntimeException("Chỉ cho phép file PDF / DOC / DOCX");
        }
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
                    repository.findAllByUserAndStatusWithJobAndCompany(
                            userId,
                            status
                    );

        } else {

            applications =
                    repository.findAllByUserWithJobAndCompany(userId);
        }

        List<Long> appIds = applications.stream().map(JobApplication::getId).toList();
        Map<Long, Interview> latestInterviewMap = new HashMap<>();
        if (!appIds.isEmpty()) {
            List<Interview> interviews = interviewRepository.findByApplication_IdIn(appIds);
            for (Interview iv : interviews) {
                Long appId = iv.getApplication().getId();
                Interview existing = latestInterviewMap.get(appId);
                if (existing == null || (iv.getStartTime() != null && existing.getStartTime() != null && iv.getStartTime().isAfter(existing.getStartTime()))) {
                    latestInterviewMap.put(appId, iv);
                }
            }
        }

        return applications.stream().map(app -> {

            AppliedJobResponse res =
                    new AppliedJobResponse();

            res.setApplicationId(app.getId());
            res.setStatus(app.getStatus());
            res.setApplyDate(app.getAppliedAt());
            res.setCvFileUrl("/api/user/jobs/apply/" + app.getId() + "/cv");

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

            Interview iv = latestInterviewMap.get(app.getId());
            if (iv != null) {
                InterviewSummaryResponse ivDto = InterviewSummaryResponse.builder()
                        .id(iv.getId())
                        .roundName(iv.getRoundName())
                        .interviewType(iv.getInterviewType())
                        .locationOrLink(iv.getLocationOrLink())
                        .startTime(iv.getStartTime())
                        .endTime(iv.getEndTime())
                        .interviewerName(iv.getInterviewerName())
                        .interviewerEmail(iv.getInterviewerEmail())
                        .notes(iv.getNotes())
                        .status(iv.getStatus())
                        .build();
                res.setInterview(ivDto);
            }

            return res;

        }).toList();
    }

    // HR GET ALL CANDIDATES
    public Page<AppliedCandidateResponse>
    getAllCandidatesByCompany(Long companyId, Pageable pageable) {

        Page<JobApplication> applications =
                repository.findAllByCompanyId(companyId, pageable);

        List<Long> appIds = applications.getContent().stream()
                .map(JobApplication::getId)
                .toList();

        Map<Long, JobApplicationEvaluation> evalMap = appIds.isEmpty()
                ? Map.of()
                : evaluationRepository.findByJobApplication_IdIn(appIds)
                        .stream()
                        .collect(Collectors.toMap(
                                eval -> eval.getJobApplication().getId(),
                                eval -> eval,
                                (existing, replacement) -> existing
                        ));

        return applications.map(app -> {

            AppliedCandidateResponse res =
                    new AppliedCandidateResponse();

            res.setApplicationId(app.getId());
            res.setStatus(app.getStatus());
            res.setAppliedAt(app.getAppliedAt());
            res.setCvFileUrl("/api/hr/applications/" + app.getId() + "/cv");

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

            // AI FIT (Tải sẵn kết quả đánh giá AI đã lưu)
            JobApplicationEvaluation eval = evalMap.get(app.getId());
            if (eval != null) {
                res.setAiFit(AiCandidateFitResponse.builder()
                        .score(eval.getScore())
                        .summary(eval.getSummary())
                        .strengths(eval.getStrengths())
                        .weaknesses(eval.getWeaknesses())
                        .recommendations(eval.getRecommendations())
                        .build());
            }

            return res;

        });
    }

    // UPDATE STATUS
    @Transactional
    public void updateApplicationStatus(
            Long applicationId,
            Long companyId,
            String status
    ) {

        if (status == null || status.isBlank()) {
            throw new RuntimeException("Trạng thái không được để trống");
        }

        List<String> validStatus = List.of(
                "PENDING",
                "REVIEWED",
                "REVIEWING",
                "INTERVIEW",
                "ACCEPTED",
                "REJECTED"
        );

        String normalizedStatus = status.trim().toUpperCase();

        if (!validStatus.contains(normalizedStatus)) {
            throw new RuntimeException("Trạng thái không hợp lệ: " + status);
        }

        JobApplication application =
                repository.findById(applicationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Không tìm thấy đơn ứng tuyển #" + applicationId
                                ));

        if (application.getJob() == null || application.getJob().getCompany() == null) {
            throw new RuntimeException("Đơn ứng tuyển không hợp lệ (không gắn với công việc hoặc công ty)");
        }

        Long appCompanyId = application.getJob().getCompany().getCompanyId();
        if (companyId != null && !companyId.equals(appCompanyId)) {
            throw new RuntimeException("Bạn không có quyền cập nhật đơn này (thuộc công ty khác)");
        }

        application.setStatus(normalizedStatus);
        repository.save(application);
        log.info("HR công ty #{} đã cập nhật trạng thái đơn ứng tuyển #{} sang {}", companyId, applicationId, normalizedStatus);

        // Gửi email thông báo cho ứng viên nếu trúng tuyển hoặc từ chối
        try {
            if (application.getUser() != null && application.getUser().getEmail() != null) {
                String candidateEmail = application.getUser().getEmail();
                String candidateName = application.getUser().getFullName() != null ? application.getUser().getFullName() : "Ứng viên";
                String jobTitle = application.getJob() != null ? application.getJob().getTitle() : "Vị trí ứng tuyển";
                String compName = (application.getJob() != null && application.getJob().getCompany() != null)
                        ? application.getJob().getCompany().getCompanyName()
                        : "Công ty";

                mailService.sendApplicationStatusUpdate(
                        candidateEmail,
                        candidateName,
                        jobTitle,
                        compName,
                        normalizedStatus
                );
            }
        } catch (Exception e) {
            log.error("Không thể gửi email cập nhật trạng thái ứng tuyển: {}", e.getMessage());
        }
    }


    public JobApplication getApplicationByIdAndCompanyId(Long applicationId, Long companyId) {
        return repository.findByIdAndCompanyId(applicationId, companyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển hoặc ứng viên không thuộc công ty của bạn"));
    }

    public ApplicationCv getCvForCompany(Long applicationId, Long companyId) {
        return openApplicationCv(getApplicationByIdAndCompanyId(applicationId, companyId));
    }

    public ApplicationCv getCvForUser(Long applicationId, Long userId) {
        JobApplication app = repository.findByIdAndUser_UserId(applicationId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển"));
        return openApplicationCv(app);
    }

    private ApplicationCv openApplicationCv(JobApplication app) {
        InputStream stream = fileStorageService.openCvFile(app.getCvFile());
        return new ApplicationCv(stream, fileStorageService.cvFileName(app.getCvFile()));
    }

    public record ApplicationCv(InputStream stream, String fileName) { }
}
