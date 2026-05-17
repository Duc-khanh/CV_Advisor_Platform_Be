package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenRouterService {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.api.model:google/gemini-3.1-flash-lite}")
    private String apiModel;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterService(RestTemplateBuilder builder) {

        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(60))
                .readTimeout(Duration.ofSeconds(60))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public AiCvEvaluationResponse evaluateCv(
            AiCvEvaluationRequest request
    ) {

        validateRequest(request);

        String prompt = buildPrompt(
                request.getCvContent(),
                request.getJobDescription()
        );

        String aiResponse = callOpenRouterApi(prompt);

        AiCvEvaluationResponse.AiCvEvaluationResponseBuilder builder = AiCvEvaluationResponse.builder()
                .rawAiResponse(aiResponse);

        try {
            String jsonContent = aiResponse.replaceAll("(?s).*?```(?:json)?\\n?(.*?)\\n?```.*", "$1").trim();
            if (!jsonContent.startsWith("{")) {
                jsonContent = aiResponse.trim();
            }

            JsonNode parsed = objectMapper.readTree(jsonContent);

            builder.score(parsed.path("score").asInt(0));
            builder.summary(parsed.path("summary").asText(""));

            java.util.List<String> strengths = new java.util.ArrayList<>();
            parsed.path("strengths").forEach(n -> strengths.add(n.asText()));
            builder.strengths(strengths);

            java.util.List<String> weaknesses = new java.util.ArrayList<>();
            parsed.path("weaknesses").forEach(n -> weaknesses.add(n.asText()));
            builder.weaknesses(weaknesses);

            java.util.List<String> missingSkills = new java.util.ArrayList<>();
            parsed.path("missingSkills").forEach(n -> missingSkills.add(n.asText()));
            builder.missingSkills(missingSkills);

            java.util.List<java.util.Map<String, String>> recommendedJobs = new java.util.ArrayList<>();
            parsed.path("recommendedJobs").forEach(n -> {
                java.util.Map<String, String> job = new java.util.HashMap<>();
                job.put("title", n.path("title").asText(""));
                job.put("companyName", n.path("companyName").asText(""));
                job.put("location", n.path("location").asText(""));
                job.put("salaryRange", n.path("salaryRange").asText(""));
                recommendedJobs.add(job);
            });
            builder.recommendedJobs(recommendedJobs);

        } catch (Exception e) {
            log.warn("Không thể parse AI response thành JSON. Nguyên nhân: {}", e.getMessage());
            throw new RuntimeException("AI trả về kết quả không hợp lệ, vui lòng thử lại.");
        }

        return builder.build();
    }

    private void validateRequest(
            AiCvEvaluationRequest request
    ) {

        if (request == null) {
            throw new RuntimeException("Request không được null");
        }

        if (request.getCvContent() == null ||
                request.getCvContent().isBlank()) {

            throw new RuntimeException("CV content không được để trống");
        }

        if (request.getJobDescription() == null ||
                request.getJobDescription().isBlank()) {

            // Allow blank job description for general CV analysis
            request.setJobDescription("");
        }
    }

    private String buildPrompt(
            String cvContent,
            String jobDescription
    ) {

        String basePrompt = """
                Bạn là chuyên gia HR tuyển dụng IT senior.
                
                """;

        if (jobDescription != null && !jobDescription.isBlank()) {
            basePrompt += """
                    Hãy đánh giá CV dựa trên Job Description.

                    ====================================
                    JOB DESCRIPTION:
                    %s
                    """.formatted(jobDescription);
        } else {
            basePrompt += """
                    Hãy phân tích CV sau đây và gợi ý các công việc phù hợp.
                    """;
        }

        basePrompt += """

                ====================================
                CV:
                %s
                ====================================

                Hãy trả lời bằng tiếng Việt. BẮT BUỘC TRẢ VỀ CHUẨN JSON VỚI CẤU TRÚC SAU (KHÔNG KÈM TEXT NÀO KHÁC):
                {
                  "score": 85,
                  "summary": "Tóm tắt ngắn gọn",
                  "strengths": ["Điểm 1", "Điểm 2"],
                  "weaknesses": ["Điểm 1", "Điểm 2"],
                  "missingSkills": ["Kỹ năng 1"],
                  "recommendedJobs": [
                    {"title": "Tên job", "companyName": "Loại công ty", "location": "Remote", "salaryRange": "Thỏa thuận"}
                  ]
                }
                """.formatted(cvContent);

        return basePrompt;
    }

    private String callOpenRouterApi(String prompt) {

        List<String> fallbackModels = List.of(
                apiModel,
                "google/gemini-2.0-flash-exp:free",
                "meta-llama/llama-3.2-3b-instruct:free"
        );

        for (String model : fallbackModels) {

            try {

                log.info("Calling OpenRouter model: {}", model);

                String result =
                        executeRequest(model, prompt);

                if (result != null &&
                        !result.isBlank()) {

                    return result;
                }

            } catch (Exception e) {

                log.error(
                        "Model failed: {}",
                        model,
                        e
                );
            }
        }

        throw new RuntimeException("Không thể kết nối AI (Hết quota, rate limit hoặc Model không tồn tại).");
    }

    private String executeRequest(
            String model,
            String prompt
    ) {

        try {

            Map<String, Object> message =
                    new HashMap<>();

            message.put("role", "user");
            message.put("content", prompt);

            Map<String, Object> requestBody =
                    new HashMap<>();

            requestBody.put("model", model);

            requestBody.put(
                    "messages",
                    List.of(message)
            );

            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 1500);

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(apiKey);

            headers.set(
                    "HTTP-Referer",
                    "http://localhost:8080"
            );

            headers.set(
                    "X-Title",
                    "CvAdvisorPlatform"
            );

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(
                            requestBody,
                            headers
                    );

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            apiUrl,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            String responseBody =
                    response.getBody();

            log.info("OpenRouter Response: {}",
                    responseBody);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new RuntimeException(
                        "Response rỗng"
                );
            }

            JsonNode rootNode =
                    objectMapper.readTree(responseBody);

            JsonNode contentNode =
                    rootNode
                            .path("choices")
                            .get(0)
                            .path("message")
                            .path("content");

            if (contentNode.isMissingNode()) {

                throw new RuntimeException(
                        "Không tìm thấy content"
                );
            }

            return contentNode.asText();

        } catch (HttpClientErrorException e) {

            log.error(
                    "OpenRouter API Error: {}",
                    e.getResponseBodyAsString()
            );

            throw new RuntimeException(
                    e.getResponseBodyAsString()
            );

        } catch (Exception e) {

            log.error(
                    "System Error",
                    e
            );

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }
}