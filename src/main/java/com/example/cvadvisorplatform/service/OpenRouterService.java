package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.example.cvadvisorplatform.dto.CareerRoadmapResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    public CareerRoadmapResponse generateCareerRoadmap(String cvContent, String targetRole, String desiredRoadmap) {
        String prompt = """
            Bạn là chuyên gia Mentor nghề nghiệp đa ngành.

            Bạn có khả năng tư vấn lộ trình học tập và phát triển sự nghiệp cho nhiều lĩnh vực khác nhau như:
            - Công nghệ thông tin (IT)
            - Marketing
            - Sales / Kinh doanh
            - Nhân sự (HR)
            - Kế toán / Tài chính
            - Thiết kế đồ họa / UI UX
            - Logistics / Supply Chain
            - Customer Service
            - Giáo dục
            - Quản lý / Leader / Manager
            - Fresher / Intern
            - Và nhiều ngành nghề khác

            Nhiệm vụ của bạn:
            - Tự động phân tích CV để nhận diện ngành nghề hiện tại của ứng viên
            - Kết hợp với vị trí mục tiêu để tạo roadmap phù hợp
            - Không được mặc định ứng viên thuộc ngành IT nếu CV hoặc vị trí mục tiêu không liên quan IT
            - Nếu vị trí mục tiêu thuộc ngành nào, hãy xây dựng roadmap theo đúng ngành đó
            - Nếu ứng viên chưa có nhiều kinh nghiệm, hãy tập trung vào nền tảng, kỹ năng cơ bản, dự án cá nhân, chứng chỉ và kinh nghiệm thực tế
            - Nếu CV thiếu thông tin, hãy đưa ra lộ trình hợp lý dựa trên thông tin hiện có, không tự bịa dữ liệu

            Dựa trên CV của ứng viên, vị trí mục tiêu và định hướng mong muốn, hãy tạo ra một lộ trình học tập và phát triển sự nghiệp chi tiết.

            ====================================
            CV:
            %s
            ====================================
            VỊ TRÍ MỤC TIÊU: %s
            LỘ TRÌNH MONG MUỐN: %s
            ====================================

            Hãy phân tích và trả về CHUẨN JSON VỚI CẤU TRÚC SAU (KHÔNG KÈM TEXT NÀO KHÁC):
            {
              "summary": "Tóm tắt lộ trình học tập, ngành nghề phù hợp và mục tiêu ngắn gọn",
              "roadmap": [
                {
                  "title": "Giai đoạn 1: Nền tảng...",
                  "description": "Chi tiết các việc cần làm theo đúng ngành nghề mục tiêu",
                  "skills": ["Kỹ năng A", "Kỹ năng B"],
                  "duration": "1-2 tháng"
                }
              ],
              "marketTrends": [
                {
                  "topic": "Xu hướng/Kỹ năng quan trọng trong ngành nghề mục tiêu",
                  "demand": "Nhu cầu thị trường (Cao/Trung bình/Thấp)",
                  "salary": "Mức lương tham khảo"
                }
              ]
            }
            """.formatted(cvContent, targetRole, desiredRoadmap);

        String aiResponse = callOpenRouterApi(prompt);

        CareerRoadmapResponse.CareerRoadmapResponseBuilder builder = CareerRoadmapResponse.builder();

        try {
            String jsonContent = aiResponse.replaceAll("(?s).*?```(?:json)?\\n?(.*?)\\n?```.*", "$1").trim();
            if (!jsonContent.startsWith("{")) {
                jsonContent = aiResponse.trim();
            }

            JsonNode parsed = objectMapper.readTree(jsonContent);

            builder.summary(parsed.path("summary").asText(""));

            java.util.List<CareerRoadmapResponse.RoadmapPhase> phases = new java.util.ArrayList<>();
            parsed.path("roadmap").forEach(n -> {
                CareerRoadmapResponse.RoadmapPhase phase = new CareerRoadmapResponse.RoadmapPhase();
                phase.setTitle(n.path("title").asText(""));
                phase.setDescription(n.path("description").asText(""));
                java.util.List<String> skills = new java.util.ArrayList<>();
                n.path("skills").forEach(s -> skills.add(s.asText()));
                phase.setSkills(skills);
                phase.setDuration(n.path("duration").asText(""));
                phases.add(phase);
            });
            builder.roadmap(phases);

            java.util.List<CareerRoadmapResponse.MarketTrend> trends = new java.util.ArrayList<>();
            parsed.path("marketTrends").forEach(n -> {
                CareerRoadmapResponse.MarketTrend trend = new CareerRoadmapResponse.MarketTrend();
                trend.setTopic(n.path("topic").asText(""));
                trend.setDemand(n.path("demand").asText(""));
                trend.setSalary(n.path("salary").asText(""));
                trends.add(trend);
            });
            builder.marketTrends(trends);

        } catch (Exception e) {
            log.warn("Không thể parse AI response thành JSON cho lộ trình học tập. Nguyên nhân: {}", e.getMessage());
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
        String systemPrompt = readFile("SYSTEM_PROMPT.md");
        String chainOfThought = readFile(".prompt/Chain of Thought.md");
        String skills = readFile(".agents/skills/cv-specialist/SKILL.md");

        StringBuilder basePrompt = new StringBuilder();

        if (!systemPrompt.isEmpty()) {
            basePrompt.append(systemPrompt).append("\n\n");
        } else {
            basePrompt.append("""
                Bạn là chuyên gia tuyển dụng và tư vấn nghề nghiệp đa ngành.

                Bạn có khả năng phân tích CV thuộc nhiều lĩnh vực khác nhau như:
                - Công nghệ thông tin (IT)
                - Marketing
                - Sales / Kinh doanh
                - Nhân sự (HR)
                - Kế toán / Tài chính
                - Thiết kế đồ họa / UI UX
                - Logistics / Supply Chain
                - Customer Service
                - Giáo dục
                - Quản lý / Leader / Manager
                - Fresher / Intern
                - Và nhiều ngành nghề khác

                Nhiệm vụ của bạn:
                - Tự động nhận diện ngành nghề phù hợp từ nội dung CV
                - Đánh giá kỹ năng chuyên môn theo đúng lĩnh vực của ứng viên
                - Không được mặc định CV là ngành IT nếu nội dung CV không liên quan đến IT
                - Xác định cấp độ ứng viên dựa trên kinh nghiệm, kỹ năng và dự án
                - Đưa ra nhận xét khách quan, thực tế và hữu ích
                - Gợi ý công việc phù hợp với ngành nghề và năng lực của ứng viên

                Nếu ứng viên chưa có nhiều kinh nghiệm:
                - Hãy tập trung đánh giá tiềm năng phát triển
                - Đánh giá kỹ năng nền tảng
                - Đánh giá dự án cá nhân, hoạt động học tập hoặc chứng chỉ nếu có

                Nếu CV thiếu thông tin:
                - Hãy nêu rõ phần còn thiếu
                - Không tự bịa thêm dữ liệu

                Trả lời hoàn toàn bằng tiếng Việt.

                """);
        }

        if (!chainOfThought.isEmpty()) {
            basePrompt.append("### QUY TRÌNH PHÂN TÍCH:\n")
                    .append(chainOfThought).append("\n\n");
        }

        if (!skills.isEmpty()) {
            basePrompt.append("### KỸ NĂNG CỦA BẠN:\n")
                    .append(skills).append("\n\n");
        }

        if (jobDescription != null && !jobDescription.isBlank()) {
            basePrompt.append("""
                Hãy đánh giá CV dựa trên Job Description.
                Đồng thời tự động xác định ngành nghề chính của CV và đánh giá mức độ phù hợp với vị trí đang tuyển.

                ====================================
                JOB DESCRIPTION:
                %s
                """.formatted(jobDescription));
        } else {
            basePrompt.append("""
                Hãy phân tích CV sau đây, tự động xác định lĩnh vực nghề nghiệp phù hợp và gợi ý các công việc tương ứng với chuyên môn của ứng viên.
                """);
        }

        basePrompt.append("""

            ====================================
            CV:
            %s
            ====================================

            Hãy trả lời bằng tiếng Việt. BẮT BUỘC TRẢ VỀ CHUẨN JSON VỚI CẤU TRÚC SAU (KHÔNG KÈM TEXT NÀO KHÁC):
            {
              "score": 85,
              "summary": "Tóm tắt ngắn gọn, có nêu ngành nghề phù hợp của CV",
              "strengths": ["Điểm 1", "Điểm 2"],
              "weaknesses": ["Điểm 1", "Điểm 2"],
              "missingSkills": ["Kỹ năng 1"],
              "recommendedJobs": [
                {"title": "Tên job phù hợp với ngành nghề của CV", "companyName": "Loại công ty", "location": "Remote", "salaryRange": "Thỏa thuận"}
              ]
            }
            """.formatted(cvContent));

        return basePrompt.toString();
    }
    private String buildCandidateFitPrompt(
            String cvContent,
            String jobDescription
    ) {
        return """
            Bạn là chuyên gia tuyển dụng HR có kinh nghiệm đánh giá CV theo JD.

            Nhiệm vụ:
            - So sánh CV của ứng viên với Job Description.
            - Chấm điểm mức độ phù hợp từ 0 đến 100.
            - Phân tích điểm mạnh của ứng viên so với JD.
            - Phân tích điểm còn thiếu hoặc chưa phù hợp.
            - Đưa ra khuyến nghị cho HR: nên loại, cân nhắc, phỏng vấn, hoặc rất phù hợp.

            Quy tắc:
            - Không tự bịa kinh nghiệm nếu CV không có.
            - Nếu CV thiếu thông tin, hãy nêu rõ.
            - Đánh giá khách quan, ngắn gọn, dễ hiểu.
            - Trả lời hoàn toàn bằng tiếng Việt.
            - Bắt buộc chỉ trả về JSON, không kèm markdown, không kèm giải thích bên ngoài.

            ====================================
            JOB DESCRIPTION:
            %s
            ====================================

            CV ỨNG VIÊN:
            %s
            ====================================

            Format JSON bắt buộc:
            {
              "score": 85,
              "summary": "Ứng viên phù hợp với vị trí vì...",
              "strengths": [
                "Điểm mạnh 1",
                "Điểm mạnh 2"
              ],
              "weaknesses": [
                "Điểm còn thiếu 1",
                "Điểm còn thiếu 2"
              ],
              "recommendations": "Nên đưa ứng viên vào vòng phỏng vấn kỹ thuật."
            }
            """.formatted(jobDescription, cvContent);
    }

    private String readFile(String filePath) {
        try {
            return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Không thể đọc file prompt: {}. Lỗi: {}", filePath, e.getMessage());
            return "";
        }
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
    public AiCandidateFitResponse evaluateCandidateFit(
            String cvContent,
            String jobDescription
    ) {
        if (cvContent == null || cvContent.isBlank()) {
            throw new RuntimeException("Nội dung CV không được để trống");
        }

        if (jobDescription == null || jobDescription.isBlank()) {
            throw new RuntimeException("Job Description không được để trống");
        }

        String prompt = buildCandidateFitPrompt(cvContent, jobDescription);

        String aiResponse = callOpenRouterApi(prompt);

        AiCandidateFitResponse.AiCandidateFitResponseBuilder builder =
                AiCandidateFitResponse.builder()
                        .rawAiResponse(aiResponse);

        try {
            String jsonContent = aiResponse
                    .replaceAll("(?s).*?```(?:json)?\\n?(.*?)\\n?```.*", "$1")
                    .trim();

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

            builder.recommendations(parsed.path("recommendations").asText(""));

        } catch (Exception e) {
            log.warn("Không thể parse AI Candidate Fit response. Lỗi: {}", e.getMessage());
            throw new RuntimeException("AI trả về kết quả đánh giá không hợp lệ, vui lòng thử lại.");
        }

        return builder.build();
    }
}