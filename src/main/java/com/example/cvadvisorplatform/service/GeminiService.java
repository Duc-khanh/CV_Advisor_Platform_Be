package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

// @Service // Commented out to switch to OpenRouter
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiService(RestTemplateBuilder builder) {
        this.restTemplate = builder.connectTimeout(Duration.ofSeconds(30)).readTimeout(Duration.ofSeconds(30))
                .build();

        this.objectMapper = new ObjectMapper();
    }

    public AiCvEvaluationResponse evaluateCv(AiCvEvaluationRequest request) {

        String prompt = buildPrompt(
                request.getCvContent(),
                request.getJobDescription()
        );

        String aiResponse = callGeminiApi(prompt);

        return AiCvEvaluationResponse.builder()
                .rawAiResponse(aiResponse)
                .build();
    }

    private String buildPrompt(String cvContent, String jobDescription) {

        return """
                Bạn là chuyên gia HR tuyển dụng.

                Hãy đánh giá CV dựa trên Job Description.

                =========================
                JOB DESCRIPTION:
                %s

                =========================
                CV:
                %s

                =========================

                Yêu cầu:
                1. Đánh giá mức độ phù hợp
                2. Điểm mạnh
                3. Điểm yếu
                4. Kỹ năng còn thiếu
                5. Gợi ý cải thiện CV
                6. Chấm điểm trên thang 100
                """
                .formatted(jobDescription, cvContent);
    }

    private String callGeminiApi(String prompt) {

        try {

            String finalUrl = apiUrl + "?key=" + apiKey;

            System.out.println("Gemini URL: " + finalUrl);

            // Build Request Body
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(content));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            finalUrl,
                            HttpMethod.POST,
                            entity,
                            String.class
                    );

            String responseBody = response.getBody();

            System.out.println("Gemini Response: ");
            System.out.println(responseBody);

            if (responseBody == null) {
                return "Gemini API trả về dữ liệu rỗng.";
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);

            JsonNode candidatesNode = rootNode.path("candidates");

            if (!candidatesNode.isArray() || candidatesNode.isEmpty()) {
                return "Không tìm thấy candidates trong response.";
            }

            JsonNode textNode = candidatesNode
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text");

            if (textNode.isMissingNode()) {
                return "Không đọc được nội dung phản hồi từ Gemini.";
            }

            return textNode.asText();

        } catch (HttpClientErrorException e) {

            System.out.println("Gemini API Error:");
            System.out.println(e.getResponseBodyAsString());

            return "Gemini API lỗi: " + e.getResponseBodyAsString();

        } catch (Exception e) {

            e.printStackTrace();

            return "Lỗi hệ thống khi gọi Gemini API: " + e.getMessage();
        }
    }
}