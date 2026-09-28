package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.*;
import com.example.cvadvisorplatform.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final JobApplicationEvaluationRepository jobApplicationEvaluationRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats() {
        List<User> users = userRepository.findAll();
        List<Company> companies = companyRepository.findAll();
        long totalJobs = jobRepository.count();
        long totalApplications = jobApplicationRepository.count();
        List<AiUsageLog> aiLogs = aiUsageLogRepository.findAll();

        long totalUsers = users.size();
        long totalCandidates = users.stream()
                .filter(u -> u.getRole() != null && "USER".equalsIgnoreCase(u.getRole().getRoleName()))
                .count();
        long totalHr = users.stream()
                .filter(u -> u.getRole() != null && "HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .count();
        long totalCompanies = companies.size();
        long totalAiRequests = aiLogs.size();
        long successfulAi = aiLogs.stream()
                .filter(l -> l.getStatus() == AiUsageStatus.SUCCESS)
                .count();
        double aiSuccessRate = totalAiRequests > 0
                ? Math.round(((double) successfulAi / totalAiRequests) * 100.0)
                : 100.0;

        // 1. Biểu đồ miền: Tăng trưởng người dùng theo ngày
        Map<String, Long> userByDateMap = users.stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().format(DATE_FMT),
                        TreeMap::new,
                        Collectors.counting()
                ));
        List<Map<String, Object>> userGrowth = userByDateMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("date", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();

        // 2. Biểu đồ tròn: Phân bổ vai trò người dùng
        Map<String, Long> roleMap = users.stream()
                .filter(u -> u.getRole() != null)
                .collect(Collectors.groupingBy(
                        u -> {
                            String r = u.getRole().getRoleName();
                            if ("USER".equalsIgnoreCase(r)) return "Ứng viên (Candidate)";
                            if ("HR".equalsIgnoreCase(r)) return "Nhà tuyển dụng (HR)";
                            if ("ADMIN".equalsIgnoreCase(r)) return "Quản trị viên (Admin)";
                            return r;
                        },
                        Collectors.counting()
                ));
        List<Map<String, Object>> roleDistribution = roleMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .toList();

        // 3. Biểu đồ cột: Mức độ sử dụng tính năng AI
        Map<String, Long> featureMap = new LinkedHashMap<>();
        featureMap.put("Đánh giá CV", 0L);
        featureMap.put("Lộ trình sự nghiệp", 0L);
        featureMap.put("Viết lại CV", 0L);
        featureMap.put("Trợ lý nghề nghiệp", 0L);
        featureMap.put("Độ phù hợp ứng viên", 0L);

        for (AiUsageLog logItem : aiLogs) {
            if (logItem.getFeature() != null) {
                switch (logItem.getFeature()) {
                    case CV_EVALUATION -> featureMap.merge("Đánh giá CV", 1L, Long::sum);
                    case CAREER_ROADMAP -> featureMap.merge("Lộ trình sự nghiệp", 1L, Long::sum);
                    case CV_REWRITE -> featureMap.merge("Viết lại CV", 1L, Long::sum);
                    case CAREER_ASSISTANT -> featureMap.merge("Trợ lý nghề nghiệp", 1L, Long::sum);
                    case CANDIDATE_FIT -> featureMap.merge("Độ phù hợp ứng viên", 1L, Long::sum);
                }
            }
        }
        List<Map<String, Object>> aiFeatureUsage = featureMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();

        // 4. Biểu đồ tròn: Doanh nghiệp theo lĩnh vực ngành nghề
        Map<String, Long> indMap = companies.stream()
                .map(c -> c.getIndustry() != null && c.getIndustry().getIndustryName() != null
                        ? c.getIndustry().getIndustryName()
                        : "Khác")
                .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
        List<Map<String, Object>> industryDistribution = indMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalCandidates", totalCandidates);
        result.put("totalHr", totalHr);
        result.put("totalCompanies", totalCompanies);
        result.put("totalJobs", totalJobs);
        result.put("totalApplications", totalApplications);
        result.put("totalAiRequests", totalAiRequests);
        result.put("aiSuccessRate", aiSuccessRate);
        result.put("userGrowth", userGrowth);
        result.put("roleDistribution", roleDistribution);
        result.put("aiFeatureUsage", aiFeatureUsage);
        result.put("industryDistribution", industryDistribution);

        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetailedStats(String range) {
        LocalDateTime cutoff;
        if ("7d".equalsIgnoreCase(range)) {
            cutoff = LocalDateTime.now().minusDays(7);
        } else if ("90d".equalsIgnoreCase(range)) {
            cutoff = LocalDateTime.now().minusDays(90);
        } else if ("all".equalsIgnoreCase(range)) {
            cutoff = LocalDateTime.of(2020, 1, 1, 0, 0);
        } else {
            cutoff = LocalDateTime.now().minusDays(30);
        }

        List<User> allUsers = userRepository.findAll();
        List<User> filteredUsers = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && !u.getCreatedAt().isBefore(cutoff))
                .toList();

        List<JobApplication> allApps = jobApplicationRepository.findAll();
        List<JobApplication> filteredApps = allApps.stream()
                .filter(a -> a.getAppliedAt() != null && !a.getAppliedAt().isBefore(cutoff))
                .toList();

        List<AiUsageLog> allAiLogs = aiUsageLogRepository.findAll();
        List<AiUsageLog> filteredAiLogs = allAiLogs.stream()
                .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(cutoff))
                .toList();

        List<Job> allJobs = jobRepository.findAll();

        long funnelJobs = allJobs.stream().filter(j -> Boolean.TRUE.equals(j.getActive())).count();
        long funnelViews = allJobs.stream().mapToLong(j -> j.getViewCount() != null ? j.getViewCount() : 0).sum();
        long funnelApplications = filteredApps.size();
        long funnelProcessed = filteredApps.stream().filter(a -> a.getStatus() != null && !"PENDING".equalsIgnoreCase(a.getStatus())).count();
        long funnelAccepted = filteredApps.stream().filter(a -> a.getStatus() != null && ("ACCEPTED".equalsIgnoreCase(a.getStatus()) || "INTERVIEW".equalsIgnoreCase(a.getStatus()))).count();

        List<Map<String, Object>> recruitmentFunnel = new ArrayList<>();
        Map<String, Object> f1 = new HashMap<>(); f1.put("stage", "Tin đang tuyển"); f1.put("count", funnelJobs); f1.put("color", "#3b82f6"); recruitmentFunnel.add(f1);
        Map<String, Object> f2 = new HashMap<>(); f2.put("stage", "Lượt xem tin"); f2.put("count", Math.max(funnelViews, funnelApplications * 3)); f2.put("color", "#6366f1"); recruitmentFunnel.add(f2);
        Map<String, Object> f3 = new HashMap<>(); f3.put("stage", "Hồ sơ ứng tuyển"); f3.put("count", funnelApplications); f3.put("color", "#8b5cf6"); recruitmentFunnel.add(f3);
        Map<String, Object> f4 = new HashMap<>(); f4.put("stage", "HR đã xử lý"); f4.put("count", funnelProcessed); f4.put("color", "#ec4899"); recruitmentFunnel.add(f4);
        Map<String, Object> f5 = new HashMap<>(); f5.put("stage", "Trúng tuyển/Phỏng vấn"); f5.put("count", funnelAccepted); f5.put("color", "#10b981"); recruitmentFunnel.add(f5);

        List<JobApplicationEvaluation> evals = jobApplicationEvaluationRepository.findAll();
        long scoreExcellent = 0;
        long scoreGood = 0;
        long scoreAverage = 0;
        long scoreNeedImprovement = 0;

        for (JobApplicationEvaluation ev : evals) {
            if (ev.getScore() != null) {
                int s = ev.getScore();
                if (s >= 80) scoreExcellent++;
                else if (s >= 65) scoreGood++;
                else if (s >= 50) scoreAverage++;
                else scoreNeedImprovement++;
            }
        }

        List<Map<String, Object>> scoreDistribution = new ArrayList<>();
        Map<String, Object> s1 = new HashMap<>(); s1.put("range", "Xuất sắc (80-100%)"); s1.put("count", scoreExcellent); s1.put("color", "#10b981"); scoreDistribution.add(s1);
        Map<String, Object> s2 = new HashMap<>(); s2.put("range", "Khá tốt (65-79%)"); s2.put("count", scoreGood); s2.put("color", "#3b82f6"); scoreDistribution.add(s2);
        Map<String, Object> s3 = new HashMap<>(); s3.put("range", "Trung bình (50-64%)"); s3.put("count", scoreAverage); s3.put("color", "#f59e0b"); scoreDistribution.add(s3);
        Map<String, Object> s4 = new HashMap<>(); s4.put("range", "Cần cải thiện (<50%)"); s4.put("count", scoreNeedImprovement); s4.put("color", "#ef4444"); scoreDistribution.add(s4);

        Map<String, Long> jobTitleApps = filteredApps.stream()
                .filter(a -> a.getJob() != null && a.getJob().getTitle() != null)
                .collect(Collectors.groupingBy(a -> a.getJob().getTitle(), Collectors.counting()));

        List<Map<String, Object>> topJobApplications = jobTitleApps.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("jobTitle", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();

        Map<String, Long> aiSuccessByDate = filteredAiLogs.stream()
                .filter(l -> l.getStatus() == AiUsageStatus.SUCCESS && l.getCreatedAt() != null)
                .collect(Collectors.groupingBy(l -> l.getCreatedAt().format(DATE_FMT), TreeMap::new, Collectors.counting()));

        Map<String, Long> aiFailByDate = filteredAiLogs.stream()
                .filter(l -> l.getStatus() != AiUsageStatus.SUCCESS && l.getCreatedAt() != null)
                .collect(Collectors.groupingBy(l -> l.getCreatedAt().format(DATE_FMT), TreeMap::new, Collectors.counting()));

        Set<String> allAiDates = new TreeSet<>(aiSuccessByDate.keySet());
        allAiDates.addAll(aiFailByDate.keySet());

        List<Map<String, Object>> aiTimeline = allAiDates.stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", d);
                    m.put("success", aiSuccessByDate.getOrDefault(d, 0L));
                    m.put("failed", aiFailByDate.getOrDefault(d, 0L));
                    return m;
                })
                .toList();

        long totalAi = filteredAiLogs.size();
        long successAi = filteredAiLogs.stream().filter(l -> l.getStatus() == AiUsageStatus.SUCCESS).count();
        double aiErrorRate = totalAi > 0 ? Math.round(((double)(totalAi - successAi) / totalAi) * 1000.0) / 10.0 : 0.0;

        long totalTokens = filteredAiLogs.stream()
                .mapToLong(l -> {
                    long inp = l.getInputTokens() != null ? l.getInputTokens() : (l.getEstimatedInputTokens() != null ? l.getEstimatedInputTokens() : 0);
                    long out = l.getOutputTokens() != null ? l.getOutputTokens() : 0;
                    return inp + out;
                }).sum();

        double avgLatency = 1.6;
        List<Long> latencies = filteredAiLogs.stream()
                .filter(l -> l.getCreatedAt() != null && l.getCompletedAt() != null)
                .map(l -> Duration.between(l.getCreatedAt(), l.getCompletedAt()).toMillis())
                .filter(ms -> ms > 0 && ms < 60000)
                .toList();
        if (!latencies.isEmpty()) {
            avgLatency = Math.round((latencies.stream().mapToLong(Long::longValue).average().orElse(1600.0) / 100.0)) / 10.0;
        }

        Map<String, Long> candidateGrowth = filteredUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getRole() != null && "USER".equalsIgnoreCase(u.getRole().getRoleName()))
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().format(DATE_FMT), TreeMap::new, Collectors.counting()));

        Map<String, Long> hrGrowth = filteredUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getRole() != null && "HR".equalsIgnoreCase(u.getRole().getRoleName()))
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().format(DATE_FMT), TreeMap::new, Collectors.counting()));

        Set<String> allUserDates = new TreeSet<>(candidateGrowth.keySet());
        allUserDates.addAll(hrGrowth.keySet());

        List<Map<String, Object>> userTrend = allUserDates.stream()
                .map(d -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("date", d);
                    m.put("candidate", candidateGrowth.getOrDefault(d, 0L));
                    m.put("hr", hrGrowth.getOrDefault(d, 0L));
                    return m;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", range);
        result.put("totalUsersPeriod", filteredUsers.size());
        result.put("totalApplicationsPeriod", filteredApps.size());
        result.put("totalAiRequestsPeriod", totalAi);
        result.put("aiErrorRate", aiErrorRate);
        result.put("totalTokensUsed", totalTokens);
        result.put("avgLatencySec", avgLatency);
        result.put("recruitmentFunnel", recruitmentFunnel);
        result.put("scoreDistribution", scoreDistribution);
        result.put("topJobApplications", topJobApplications);
        result.put("aiTimeline", aiTimeline);
        result.put("userTrend", userTrend);

        return result;
    }

}
