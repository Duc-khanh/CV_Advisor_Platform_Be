package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.model.JobApplicationEvaluation;
import com.example.cvadvisorplatform.repository.JobApplicationEvaluationRepository;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationEvaluationService {

    private final JobApplicationEvaluationRepository evaluationRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final OpenRouterService openRouterService;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final FileStorageService fileStorageService;

    @Transactional
    public AiCandidateFitResponse evaluateCandidateFit(JobApplication application, String jobDescription) {
        // 1. Kiểm tra xem đơn ứng tuyển này đã được đánh giá chưa
        Optional<JobApplicationEvaluation> existingEval =
                evaluationRepository.findByJobApplication_Id(application.getId());

        if (existingEval.isPresent()) {
            log.info("Lấy kết quả đánh giá AI từ Database cho Application ID: {}", application.getId());
            JobApplicationEvaluation eval = existingEval.get();
            return AiCandidateFitResponse.builder()
                    .score(eval.getScore())
                    .summary(eval.getSummary())
                    .strengths(eval.getStrengths())
                    .weaknesses(eval.getWeaknesses())
                    .recommendations(eval.getRecommendations())
                    .rawAiResponse("Đã khôi phục từ Database (Cache)")
                    .build();
        }

        // 2. Nếu chưa có, tiến hành tải CV từ URL Cloudinary và gọi AI
        log.info("Không tìm thấy cache. Tiến hành gọi AI đánh giá cho Application ID: {}", application.getId());

        String cvUrl = application.getCvFile();
        if (cvUrl == null || cvUrl.isBlank()) {
            throw new RuntimeException("Không tìm thấy URL file CV cho đơn ứng tuyển ID: " + application.getId());
        }

        String cvContent;
        try {
            log.info("Đang tải CV cho Application ID: {}", application.getId());
            try (InputStream cvStream = fileStorageService.openCvFile(cvUrl)) {
                cvContent = pdfTextExtractorService.extractText(cvStream);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải hoặc trích xuất text từ CV URL cho Application ID: {}", application.getId(), e);
            throw new RuntimeException("Không thể đọc file CV để phân tích");
        }

        AiCandidateFitResponse aiResponse = openRouterService.evaluateCandidateFit(cvContent, jobDescription);

        // 3. Lưu kết quả mới vào Database
        JobApplicationEvaluation newEval = JobApplicationEvaluation.builder()
                .jobApplication(application)
                .score(aiResponse.getScore())
                .summary(aiResponse.getSummary())
                .strengths(aiResponse.getStrengths())
                .weaknesses(aiResponse.getWeaknesses())
                .recommendations(aiResponse.getRecommendations())
                .evaluatedAt(LocalDateTime.now())
                .build();

        evaluationRepository.save(newEval);
        log.info("Đã lưu kết quả đánh giá AI vào Database cho Application ID: {}", application.getId());

        return aiResponse;
    }

    /**
     * Tự động đánh giá độ phù hợp ứng viên chạy ngầm (Async) ngay khi nộp CV
     */
    @Async
    @Transactional
    public void evaluateCandidateFitAsync(Long applicationId, String jobDescription) {
        try {
            log.info("Bắt đầu tự động đánh giá AI ngầm cho đơn ứng tuyển ID: {}", applicationId);
            JobApplication application = jobApplicationRepository.findById(applicationId).orElse(null);
            if (application != null) {
                evaluateCandidateFit(application, jobDescription);
                log.info("Hoàn tất tự động đánh giá AI ngầm cho đơn ứng tuyển ID: {}", applicationId);
            } else {
                log.warn("Không tìm thấy đơn ứng tuyển ID: {} để đánh giá AI", applicationId);
            }
        } catch (Exception e) {
            log.error("Tự động đánh giá AI ngầm cho đơn ứng tuyển ID {} thất bại: {}", applicationId, e.getMessage());
        }
    }
}
