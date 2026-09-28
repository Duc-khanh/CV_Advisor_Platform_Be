package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.CareerAssistantRequest;
import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.example.cvadvisorplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CareerAssistantContextBuilder {
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository applicationRepository;

    public AssistantContext build(Long userId, CareerAssistantRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        CareerAssistantRequest.Context input = request.getContext() == null
                ? new CareerAssistantRequest.Context() : request.getContext();

        Job job = input.getCurrentJobId() == null ? null : getAvailableJob(input.getCurrentJobId());
        JobApplication application = input.getApplicationId() == null ? null
                : applicationRepository.findByIdAndUser_UserId(input.getApplicationId(), userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn ứng tuyển thuộc tài khoản hiện tại"));

        return new AssistantContext(user, job, application, input.getCurrentPage());
    }

    private Job getAvailableJob(Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy công việc"));
        boolean expired = job.getExpiredAt() != null && !job.getExpiredAt().isAfter(LocalDateTime.now());
        if (!Boolean.TRUE.equals(job.getActive()) || expired) {
            throw new RuntimeException("Công việc không còn khả dụng");
        }
        return job;
    }

    public record AssistantContext(
            User user,
            Job job,
            JobApplication application,
            String currentPage
    ) { }
}
