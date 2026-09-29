package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.InterviewFeedbackRequest;
import com.example.cvadvisorplatform.dto.InterviewRequest;
import com.example.cvadvisorplatform.dto.InterviewResponse;
import com.example.cvadvisorplatform.dto.InterviewStatsResponse;
import com.example.cvadvisorplatform.model.Company;
import com.example.cvadvisorplatform.model.Interview;
import com.example.cvadvisorplatform.model.JobApplication;
import com.example.cvadvisorplatform.repository.InterviewRepository;
import com.example.cvadvisorplatform.repository.JobApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository applicationRepository;
    private final MailService mailService;

    // â”€â”€â”€ READ â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Láº¥y danh sÃ¡ch lá»‹ch phá»ng váº¥n cÃ³ phÃ¢n trang + filter status.
     */
    @Transactional(readOnly = true)
    public Page<InterviewResponse> getInterviews(Long companyId, String status, Pageable pageable) {
        Page<Interview> page;

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            page = interviewRepository.findAllByCompanyAndStatus(companyId, status.toUpperCase(), pageable);
        } else {
            page = interviewRepository.findAllByCompany(companyId, pageable);
        }

        return page.map(this::toResponse);
    }

    /**
     * Láº¥y danh sÃ¡ch lá»‹ch theo khoáº£ng ngÃ y (dÃ¹ng cho Calendar view).
     */
    @Transactional(readOnly = true)
    public List<InterviewResponse> getInterviewsForCalendar(Long companyId, LocalDateTime from, LocalDateTime to) {
        return interviewRepository
                .findAllByCompanyAndDateRange(companyId, from, to)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Láº¥y chi tiáº¿t 1 lá»‹ch phá»ng váº¥n (kiá»ƒm tra quyá»n company).
     */
    @Transactional(readOnly = true)
    public InterviewResponse getInterviewById(Long id, Long companyId) {
        Interview interview = interviewRepository.findByIdAndCompany(id, companyId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y lá»‹ch phá»ng váº¥n."));
        return toResponse(interview);
    }

    /**
     * Lấy số liệu thống kê lịch phỏng vấn cho Dashboard HR.
     */
    @Transactional(readOnly = true)
    public InterviewStatsResponse getStats(Long companyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime now = LocalDateTime.now();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        long todayCount = interviewRepository.countToday(companyId, startOfDay, endOfDay);
        long upcomingCount = interviewRepository.countUpcoming(companyId, now);
        long pendingFeedback = interviewRepository.countPendingFeedback(companyId, now);
        long completedThisMonth = interviewRepository.countCompletedThisMonth(companyId, startOfMonth, endOfMonth);

        return InterviewStatsResponse.builder()
                .todayCount(todayCount)
                .upcomingCount(upcomingCount)
                .pendingFeedback(pendingFeedback)
                .completedThisMonth(completedThisMonth)
                .build();
    }

    // â”€â”€â”€ CREATE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Táº¡o lá»‹ch phá»ng váº¥n má»›i vÃ  gá»­i email thÃ´ng bÃ¡o cho á»©ng viÃªn.
     */
    @Transactional
    public InterviewResponse createInterview(Long companyId, Company company, InterviewRequest req) {
        // Validate Ä‘Æ¡n á»©ng tuyá»ƒn thuá»™c vá» company nÃ y
        JobApplication application = applicationRepository
                .findByIdAndCompanyId(req.getApplicationId(), companyId)
                .orElseThrow(() -> new RuntimeException(
                        "KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n á»©ng tuyá»ƒn hoáº·c báº¡n khÃ´ng cÃ³ quyá»n truy cáº­p."));

        Interview interview = new Interview();
        interview.setApplication(application);
        interview.setCompany(company);
        fillInterviewFromRequest(interview, req);
        interview.setStatus("SCHEDULED");

        Interview saved = interviewRepository.save(interview);
        log.info("[Interview] Created id={} for application={} company={}", saved.getId(), req.getApplicationId(), companyId);

        // Đồng bộ trạng thái đơn ứng tuyển sang INTERVIEW để ứng viên theo dõi
        application.setStatus("INTERVIEW");
        applicationRepository.save(application);
        log.info("[Interview] Synced application id={} status to INTERVIEW", application.getId());

        // Gửi email thông báo cho ứng viên (async, không ảnh hưởng response)
        try {
            String candidateEmail = (application.getUser() != null) ? application.getUser().getEmail() : null;
            String candidateName  = (application.getUser() != null) ? application.getUser().getFullName() : "Ứng viên";
            String jobTitle       = (application.getJob() != null)  ? application.getJob().getTitle() : "Vị trí ứng tuyển";
            String companyName    = (company != null && company.getCompanyName() != null) ? company.getCompanyName() : "Công ty";

            log.info("[Interview] Sending invitation email to candidate: email={}, name={}, job={}", candidateEmail, candidateName, jobTitle);

            if (candidateEmail != null && !candidateEmail.isBlank()) {
                mailService.sendInterviewInvitation(
                        candidateEmail, candidateName, jobTitle, companyName,
                        saved.getRoundName(), saved.getStartTime(), saved.getEndTime(),
                        saved.getInterviewType(), saved.getLocationOrLink(),
                        saved.getInterviewerName(), saved.getNotes()
                );
            } else {
                log.warn("[Interview] Candidate email is null or empty for applicationId={}", application.getId());
            }
        } catch (Exception e) {
            log.error("[Interview] Failed to trigger invitation email: {}", e.getMessage(), e);
        }

        return toResponse(saved);
    }

    // â”€â”€â”€ UPDATE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Cáº­p nháº­t thÃ´ng tin lá»‹ch phá»ng váº¥n.
     */
    @Transactional
    public InterviewResponse updateInterview(Long id, Long companyId, InterviewRequest req) {
        Interview interview = interviewRepository.findByIdAndCompany(id, companyId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y lá»‹ch phá»ng váº¥n."));

        fillInterviewFromRequest(interview, req);
        Interview saved = interviewRepository.save(interview);
        log.info("[Interview] Updated id={}", id);

        return toResponse(saved);
    }

    /**
     * Thay Ä‘á»•i tráº¡ng thÃ¡i lá»‹ch (SCHEDULED / COMPLETED / CANCELLED / RESCHEDULED).
     */
    @Transactional
    public InterviewResponse updateStatus(Long id, Long companyId, String status) {
        Interview interview = interviewRepository.findByIdAndCompany(id, companyId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y lá»‹ch phá»ng váº¥n."));

        String upperStatus = status.toUpperCase();
        List<String> validStatuses = List.of("SCHEDULED", "COMPLETED", "CANCELLED", "RESCHEDULED");
        if (!validStatuses.contains(upperStatus)) {
            throw new RuntimeException("Tráº¡ng thÃ¡i khÃ´ng há»£p lá»‡: " + status);
        }

        interview.setStatus(upperStatus);
        Interview saved = interviewRepository.save(interview);
        log.info("[Interview] Status changed id={} -> {}", id, upperStatus);

        return toResponse(saved);
    }

    // â”€â”€â”€ FEEDBACK â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * LÆ°u káº¿t quáº£ Ä‘Ã¡nh giÃ¡ sau phá»ng váº¥n vÃ  cáº­p nháº­t tráº¡ng thÃ¡i á»©ng viÃªn náº¿u cáº§n.
     */
    @Transactional
    public InterviewResponse submitFeedback(Long id, Long companyId, InterviewFeedbackRequest req) {
        Interview interview = interviewRepository.findByIdAndCompany(id, companyId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y lá»‹ch phá»ng váº¥n."));

        interview.setRating(req.getRating());
        interview.setFeedback(req.getFeedback());
        interview.setStrengths(req.getStrengths());
        interview.setImprovements(req.getImprovements());
        interview.setNextAction(req.getNextAction());
        interview.setStatus("COMPLETED");

        // Äá»•i tráº¡ng thÃ¡i Ä‘Æ¡n á»©ng tuyá»ƒn dá»±a theo quyáº¿t Ä‘á»‹nh HR
        if (req.getNextAction() != null) {
            JobApplication app = interview.getApplication();
            switch (req.getNextAction().toUpperCase()) {
                case "ACCEPT"     -> app.setStatus("ACCEPTED");
                case "REJECT"     -> app.setStatus("REJECTED");
                case "NEXT_ROUND" -> app.setStatus("INTERVIEW"); // giá»¯ nguyÃªn Ä‘á»ƒ táº¡o vÃ²ng má»›i
            }
            applicationRepository.save(app);
        }

        Interview saved = interviewRepository.save(interview);
        log.info("[Interview] Feedback saved id={} nextAction={}", id, req.getNextAction());

        return toResponse(saved);
    }

    // â”€â”€â”€ DELETE â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * XÃ³a lá»‹ch phá»ng váº¥n (kiá»ƒm tra quyá»n).
     */
    @Transactional
    public void deleteInterview(Long id, Long companyId) {
        Interview interview = interviewRepository.findByIdAndCompany(id, companyId)
                .orElseThrow(() -> new RuntimeException("KhÃ´ng tÃ¬m tháº¥y lá»‹ch phá»ng váº¥n."));
        interviewRepository.delete(interview);
        log.info("[Interview] Deleted id={}", id);
    }

    // â”€â”€â”€ PRIVATE HELPERS â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private void fillInterviewFromRequest(Interview interview, InterviewRequest req) {
        if (req.resolveRoundName() != null) interview.setRoundName(req.resolveRoundName());
        if (req.resolveInterviewType() != null) interview.setInterviewType(req.resolveInterviewType());
        if (req.resolveLocationOrLink() != null) interview.setLocationOrLink(req.resolveLocationOrLink());
        if (req.resolveStartTime() != null) interview.setStartTime(req.resolveStartTime());
        interview.setEndTime(req.resolveEndTime());
        if (req.getInterviewerName()  != null) interview.setInterviewerName(req.getInterviewerName());
        if (req.getInterviewerEmail() != null) interview.setInterviewerEmail(req.getInterviewerEmail());
        if (req.getNotes()          != null) interview.setNotes(req.getNotes());
    }

    /**
     * Chuyá»ƒn entity Interview â†’ DTO InterviewResponse.
     */
    private InterviewResponse toResponse(Interview iv) {
        InterviewResponse r = new InterviewResponse();
        r.setId(iv.getId());
        r.setStatus(iv.getStatus());
        r.setRoundName(iv.getRoundName());
        r.setInterviewType(iv.getInterviewType());
        r.setLocationOrLink(iv.getLocationOrLink());
        r.setStartTime(iv.getStartTime());
        r.setEndTime(iv.getEndTime());
        r.setInterviewerName(iv.getInterviewerName());
        r.setInterviewerEmail(iv.getInterviewerEmail());
        r.setNotes(iv.getNotes());
        r.setRating(iv.getRating());
        r.setFeedback(iv.getFeedback());
        r.setStrengths(iv.getStrengths());
        r.setImprovements(iv.getImprovements());
        r.setNextAction(iv.getNextAction());
        r.setCreatedAt(iv.getCreatedAt());
        r.setUpdatedAt(iv.getUpdatedAt());

        // ThÃ´ng tin á»©ng viÃªn
        if (iv.getApplication() != null) {
            JobApplication app = iv.getApplication();
            r.setApplicationId(app.getId());

            if (app.getUser() != null) {
                r.setUserId(app.getUser().getUserId());
                r.setCandidateName(app.getUser().getFullName());
                r.setCandidateEmail(app.getUser().getEmail());
                r.setCandidatePhone(app.getUser().getPhone());
            }

            if (app.getJob() != null) {
                r.setJobId(app.getJob().getJobId());
                r.setJobTitle(app.getJob().getTitle());

                if (app.getJob().getCompany() != null) {
                    r.setCompanyName(app.getJob().getCompany().getCompanyName());
                }
            }
        }

        return r;
    }
}
