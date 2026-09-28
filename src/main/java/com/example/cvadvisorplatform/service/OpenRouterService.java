package com.example.cvadvisorplatform.service;

import com.example.cvadvisorplatform.dto.AiCandidateFitResponse;
import com.example.cvadvisorplatform.dto.AiCvEvaluationRequest;
import com.example.cvadvisorplatform.dto.AiCvEvaluationResponse;
import com.example.cvadvisorplatform.dto.CareerRoadmapResponse;
import com.example.cvadvisorplatform.exception.AiProviderException;
import com.example.cvadvisorplatform.model.AiFeature;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final AiQuotaService quotaService;
    private final SystemSettingService systemSettingService;

    public OpenRouterService(RestTemplateBuilder builder, AiQuotaService quotaService, SystemSettingService systemSettingService) {

        this.restTemplate = builder
                .connectTimeout(Duration.ofSeconds(30))
                .readTimeout(Duration.ofSeconds(120))
                .build();

        this.objectMapper = new ObjectMapper();
        this.quotaService = quotaService;
        this.systemSettingService = systemSettingService;
    }

    public AiCvEvaluationResponse evaluateCv(
            AiCvEvaluationRequest request
    ) {

        validateRequest(request);

        String prompt = buildPrompt(
                request.getCvContent(),
                request.getJobDescription()
        );

        String aiResponse = callOpenRouterApi(prompt, 240, AiFeature.CV_EVALUATION);

        AiCvEvaluationResponse.AiCvEvaluationResponseBuilder builder = AiCvEvaluationResponse.builder()
                .rawAiResponse(aiResponse);

        try {
            String jsonContent = cleanAndExtractJson(aiResponse);

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

            builder.recommendedJobs(java.util.List.of());

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

        String aiResponse = callOpenRouterApi(prompt, 1000, AiFeature.CAREER_ROADMAP);

        CareerRoadmapResponse.CareerRoadmapResponseBuilder builder = CareerRoadmapResponse.builder();

        try {
            String jsonContent = cleanAndExtractJson(aiResponse);

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
        String systemPrompt    = com.example.cvadvisorplatform.util.PromptLoader.load("system_prompt.md");
        String chainOfThought  = com.example.cvadvisorplatform.util.PromptLoader.load("chain_of_thought.md");
        String skills          = com.example.cvadvisorplatform.util.PromptLoader.load("skills.md");

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
              "missingSkills": ["Kỹ năng 1"]
            }
            Giữ JSON dưới 220 token: summary tối đa 2 câu và mỗi danh sách tối đa 2 mục ngắn gọn.
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

    public String rewriteCvText(String text, String tone, String cvContext) {
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Nội dung cần chỉnh sửa không được để trống");
        }

        String normalizedTone = tone == null ? "concise" : tone.trim().toLowerCase();
        String instruction = switch (normalizedTone) {
            case "star" -> "Viết lại theo phương pháp STAR, nhấn mạnh tình huống, nhiệm vụ, hành động và kết quả. Không bịa số liệu.";
            case "polish" -> "Trau chuốt thành văn phong chuyên nghiệp, rõ ràng và phù hợp với CV.";
            default -> "Viết lại ngắn gọn, súc tích, ưu tiên động từ hành động và giữ nguyên ý nghĩa.";
        };

        String context = cvContext == null || cvContext.isBlank()
                ? "Không có ngữ cảnh CV bổ sung."
                : cvContext.substring(0, Math.min(cvContext.length(), 12000));

        String prompt = """
                Bạn là chuyên gia viết CV bằng tiếng Việt.
                %s
                Chỉ trả về đoạn văn đã viết lại, không giải thích, không thêm markdown và không tự bịa thông tin.

                NGỮ CẢNH CV:
                %s

                ĐOẠN CẦN VIẾT LẠI:
                %s
                """.formatted(instruction, context, text);

        String result = callOpenRouterApi(prompt, 1000, AiFeature.CV_REWRITE).trim();
        if (result.startsWith("```") && result.endsWith("```")) {
            result = result.replaceFirst("^```(?:text)?\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }
        return result;
    }

    public String generateCareerAssistantResponse(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new RuntimeException("Prompt Career Assistant không được để trống");
        }
        return callOpenRouterApi(prompt, 800, AiFeature.CAREER_ASSISTANT);
    }

    public List<String> buildModelSequence() {
        String strategy = systemSettingService != null ? systemSettingService.getAiStrategy() : "free_first";
        String primaryFree = systemSettingService != null ? systemSettingService.getPrimaryFreeModel() : "meta-llama/llama-3.3-70b-instruct:free";
        String fallback = systemSettingService != null ? systemSettingService.getFallbackModel() : "google/gemini-1.5-flash";

        List<String> models = new ArrayList<>();
        if ("gemini_only".equalsIgnoreCase(strategy)) {
            models.add(fallback != null && !fallback.isBlank() ? fallback : "google/gemini-1.5-flash");
            models.add("google/gemini-2.0-flash-lite");
        } else if ("free_only".equalsIgnoreCase(strategy)) {
            if (primaryFree != null && !primaryFree.isBlank()) models.add(primaryFree);
            models.add("google/gemma-2-9b-it:free");
            models.add("deepseek/deepseek-r1:free");
            models.add("qwen/qwen-2.5-72b-instruct:free");
            models.add("meta-llama/llama-3.3-70b-instruct:free");
        } else {
            // "free_first" (default): Prioritize free models, then fallback to Gemini!
            if (primaryFree != null && !primaryFree.isBlank()) models.add(primaryFree);
            models.add("google/gemma-2-9b-it:free");
            models.add("deepseek/deepseek-r1:free");
            models.add(fallback != null && !fallback.isBlank() ? fallback : "google/gemini-1.5-flash");
            models.add("google/gemini-2.0-flash-lite");
        }
        return models.stream().filter(Objects::nonNull).distinct().toList();
    }

    public Map<String, Object> testModelDirectly(String model) {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        String targetModel = (model != null && !model.isBlank())
                ? model
                : (systemSettingService != null ? systemSettingService.getPrimaryFreeModel() : "meta-llama/llama-3.3-70b-instruct:free");
        try {
            String testPrompt = "Xin chào! Bạn là mô hình AI nào? Hãy trả lời trong 1 câu ngắn gọn bằng tiếng Việt rằng hệ thống đã kết nối thành công.";
            double temp = systemSettingService != null ? systemSettingService.getTemperature() : 0.4;
            ProviderResult providerResult = executeRequest(targetModel, testPrompt, 150, temp);
            long latency = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("latencyMs", latency);
            result.put("message", "Kết nối mô hình " + targetModel + " thành công!");
            result.put("sampleResponse", providerResult.content());
            result.put("model", targetModel);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("latencyMs", latency);
            result.put("message", "Lỗi kết nối mô hình " + targetModel + ": " + e.getMessage());
            result.put("model", targetModel);
        }
        return result;
    }

    private String callOpenRouterApi(String prompt, int maxTokens, AiFeature feature) {
        int estimatedInput = Math.max(1, (prompt.length() + 3) / 4);
        AiQuotaService.Reservation reservation = quotaService.reserve(feature, estimatedInput, maxTokens);
        List<String> fallbackModels = buildModelSequence();
        double temp = systemSettingService != null ? systemSettingService.getTemperature() : 0.4;

        RuntimeException lastError = null;

        for (String model : fallbackModels) {
            try {
                log.info("Calling AI model [Strategy: {}]: {}", systemSettingService != null ? systemSettingService.getAiStrategy() : "free_first", model);
                ProviderResult result = executeRequest(model, prompt, maxTokens, temp);

                if (result.content() != null && !result.content().isBlank()) {
                    if (reservation != null) {
                        quotaService.complete(reservation, model, result.inputTokens(), result.outputTokens());
                    }
                    return result.content();
                }

                lastError = new RuntimeException("Model " + model + " trả về nội dung rỗng");
            } catch (RuntimeException e) {
                lastError = e;
                log.error("AI model {} failed: {}", model, e.getMessage());
                if (e instanceof NonRetryableAiException) break;
            }
        }

        String reason = lastError != null && lastError.getMessage() != null
                ? lastError.getMessage()
                : "Không nhận được phản hồi từ nhà cung cấp";
        String normalizedReason = reason.toLowerCase();
        boolean quotaExceeded = normalizedReason.contains("more credits")
                || normalizedReason.contains("openrouter_credits")
                || normalizedReason.contains("402");
        String message = quotaExceeded
                ? "AI đã hết hạn mức sử dụng. Vui lòng nạp thêm credit OpenRouter rồi thử lại."
                : "AI tạm thời không phản hồi. Vui lòng thử lại sau.";
        if (reservation != null) {
            quotaService.fail(reservation, quotaExceeded ? "AI_PROVIDER_QUOTA" : "AI_PROVIDER_ERROR");
        }
        throw new AiProviderException(message, lastError);
    }
    private String resolveApiKey() {
        String projectKeyName = "CVADVISOR_OPENROUTER_API_KEY";
        java.nio.file.Path directory = java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        for (int level = 0; level < 4 && directory != null; level++) {
            java.nio.file.Path envFile = directory.resolve(".env");
            if (java.nio.file.Files.isRegularFile(envFile)) {
                try {
                    for (String line : java.nio.file.Files.readAllLines(envFile, java.nio.charset.StandardCharsets.UTF_8)) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith(projectKeyName + "=")) {
                            String value = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\""))
                                    || (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1).trim();
                            }
                            if (!value.isBlank()) {
                                return value;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Không thể đọc OpenRouter key từ {}: {}", envFile, e.getMessage());
                }
            }
            directory = directory.getParent();
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Chưa cấu hình OpenRouter API key");
        }
        return apiKey.trim();
    }
    private ProviderResult executeRequest(
            String model,
            String prompt,
            int maxTokens
    ) {
        return executeRequest(model, prompt, maxTokens, 0.4);
    }

    private ProviderResult executeRequest(
            String model,
            String prompt,
            int maxTokens,
            double temperature
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

            requestBody.put("temperature", temperature > 0 ? temperature : 0.4);
            requestBody.put("max_tokens", maxTokens);

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(resolveApiKey());

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

            log.info("OpenRouter request completed successfully for model: {}", model);

            if (responseBody == null ||
                    responseBody.isBlank()) {

                throw new RuntimeException(
                        "Response rỗng"
                );
            }

            JsonNode rootNode =
                    objectMapper.readTree(responseBody);

            JsonNode choices = rootNode.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Không tìm thấy choices trong phản hồi AI");
            }

            JsonNode contentNode = choices.path(0).path("message").path("content");

            if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {

                throw new RuntimeException(
                        "Không tìm thấy content"
                );
            }

            JsonNode usage = rootNode.path("usage");
            int inputTokens = usage.path("prompt_tokens").asInt(Math.max(1, (prompt.length() + 3) / 4));
            int outputTokens = usage.path("completion_tokens").asInt(Math.max(1, (contentNode.asText().length() + 3) / 4));
            return new ProviderResult(contentNode.asText(), inputTokens, outputTokens);

        } catch (HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            log.error("OpenRouter API Error: status={}", status);
            if (status == HttpStatus.TOO_MANY_REQUESTS.value()
                    || status == HttpStatus.REQUEST_TIMEOUT.value()) {
                throw new RuntimeException("OpenRouter HTTP " + status, e);
            }
            throw new NonRetryableAiException("OpenRouter HTTP " + status, e);

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
    private record ProviderResult(String content, int inputTokens, int outputTokens) { }
    private static class NonRetryableAiException extends RuntimeException {
        private NonRetryableAiException(String message, Throwable cause) { super(message, cause); }
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

        String aiResponse = callOpenRouterApi(prompt, 1000, AiFeature.CANDIDATE_FIT);

        AiCandidateFitResponse.AiCandidateFitResponseBuilder builder =
                AiCandidateFitResponse.builder()
                        .rawAiResponse(aiResponse);

        try {
            String jsonContent = cleanAndExtractJson(aiResponse);

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

    private String cleanAndExtractJson(String aiResponse) {
        if (aiResponse == null) {
            return "{}";
        }
        String jsonContent = aiResponse.replaceAll("(?s).*?```(?:json)?\\n?(.*?)\\n?```.*", "$1").trim();
        if (!jsonContent.startsWith("{") && !jsonContent.startsWith("[")) {
            int start = aiResponse.indexOf('{');
            int end = aiResponse.lastIndexOf('}');
            if (start != -1 && end != -1 && start < end) {
                jsonContent = aiResponse.substring(start, end + 1).trim();
            } else {
                start = aiResponse.indexOf('[');
                end = aiResponse.lastIndexOf(']');
                if (start != -1 && end != -1 && start < end) {
                    jsonContent = aiResponse.substring(start, end + 1).trim();
                } else {
                    jsonContent = aiResponse.trim();
                }
            }
        }
        return jsonContent;
    }
}
