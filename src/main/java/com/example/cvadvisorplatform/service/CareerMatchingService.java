package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.model.Job;
import com.example.cvadvisorplatform.model.User;
import com.example.cvadvisorplatform.repository.JobRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CareerMatchingService {
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public MatchResult compare(User user, String cvText, Job job) {
        List<String> required = job.getRequiredSkills() == null
                ? List.of() : job.getRequiredSkills().stream().filter(this::hasText).toList();
        Set<String> candidateSkills = candidateSkills(user);
        String searchableCv = normalize(cvText);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        for (String skill : required) {
            String normalized = normalize(skill);
            boolean present = candidateSkills.stream().anyMatch(value ->
                    value.equals(normalized) || value.contains(normalized) || normalized.contains(value));
            if (!present && !normalized.isBlank()) present = searchableCv.contains(normalized);
            (present ? matched : missing).add(skill);
        }

        int score = required.isEmpty() ? 0 : (int) Math.round(matched.size() * 100.0 / required.size());
        List<String> unclear = required.isEmpty()
                ? List.of("Tin tuyển dụng chưa có danh sách kỹ năng có cấu trúc")
                : List.of();
        return new MatchResult(score, matched, missing, unclear);
    }

    public List<JobRecommendation> recommend(User user, String cvText) {
        LocalDateTime now = LocalDateTime.now();
        return jobRepository.findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, 50))
                .getContent().stream()
                .filter(job -> job.getExpiredAt() == null || job.getExpiredAt().isAfter(now))
                .map(job -> {
                    MatchResult match = compare(user, cvText, job);
                    int score = match.score() + locationBonus(user, job);
                    return new JobRecommendation(job, Math.min(score, 100), match);
                })
                .sorted(Comparator.comparingInt(JobRecommendation::score).reversed()
                        .thenComparing(item -> item.job().getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
    }

    private Set<String> candidateSkills(User user) {
        Set<String> skills = new LinkedHashSet<>();
        if (user.getSkills() == null || user.getSkills().isBlank()) return skills;
        try {
            List<String> parsed = objectMapper.readValue(user.getSkills(), new TypeReference<>() { });
            parsed.stream().filter(this::hasText).map(this::normalize).forEach(skills::add);
        } catch (Exception ignored) {
            for (String value : user.getSkills().split(",")) {
                if (hasText(value)) skills.add(normalize(value));
            }
        }
        return skills;
    }

    private int locationBonus(User user, Job job) {
        if (!hasText(user.getLocation()) || !hasText(job.getLocation())) return 0;
        return normalize(job.getLocation()).contains(normalize(user.getLocation())) ? 5 : 0;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) return "";
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii.toLowerCase(Locale.ROOT).replace('đ', 'd').replaceAll("\\s+", " ").trim();
    }

    public record MatchResult(
            int score,
            List<String> matchedSkills,
            List<String> missingSkills,
            List<String> unclearSkills
    ) { }

    public record JobRecommendation(Job job, int score, MatchResult match) { }
}
