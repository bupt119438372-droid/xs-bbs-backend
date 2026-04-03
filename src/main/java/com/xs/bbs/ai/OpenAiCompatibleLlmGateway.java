package com.xs.bbs.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.bbs.config.AiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@ConditionalOnProperty(prefix = "app.ai", name = "mode", havingValue = "openai-compatible")
@Component
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public OpenAiCompatibleLlmGateway(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))
                                .build()
                ))
                .build();
    }

    @Override
    public DailyInsightGeneration generateDailyInsight(DailyInsightPrompt prompt) {
        String systemPrompt = """
                你是一个中文成长社区的启发文案助手。
                你必须只输出一个 JSON 对象，不要输出 markdown，不要输出解释。
                JSON schema:
                {
                  "headline": "一句话提醒，20-40字",
                  "interpretation": "一段100-180字的解读",
                  "actions": ["行动建议1", "行动建议2", "行动建议3"]
                }
                """;
        String userPrompt = """
                请基于以下上下文生成每日启发：
                userId: %s
                latestThought: %s
                latestSummary: %s
                latestTags: %s
                resonanceSummary: %s
                """.formatted(
                prompt.userId(),
                prompt.latestThought(),
                prompt.latestSummary(),
                prompt.latestTags(),
                prompt.resonanceSummary()
        );
        String content = invokeChat(systemPrompt, userPrompt);
        try {
            return objectMapper.readValue(extractJsonObject(content), DailyInsightGeneration.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to parse daily insight json", exception);
        }
    }

    @Override
    public ThoughtAnalysisGeneration analyzeThought(ThoughtAnalysisPrompt prompt) {
        String systemPrompt = """
                你是一个中文社区内容理解与审核助手。
                你必须只输出一个 JSON 对象，不要输出 markdown，不要输出解释。
                moderationStatus 只能是 APPROVED、REVIEW、REJECTED。
                JSON schema:
                {
                  "summary": "一句话摘要，20-50字",
                  "tags": ["标签1", "标签2", "标签3"],
                  "moderationStatus": "APPROVED",
                  "moderationReason": "一句审核原因"
                }
                """;
        String userPrompt = """
                请分析以下念头内容：
                thoughtId: %s
                content: %s
                promptVersion: %s
                最多输出 %s 个标签。
                """.formatted(prompt.thoughtId(), prompt.content(), prompt.promptVersion(), prompt.maxTags());
        String content = invokeChat(systemPrompt, userPrompt);
        try {
            JsonNode jsonNode = objectMapper.readTree(extractJsonObject(content));
            List<String> tags = objectMapper.convertValue(jsonNode.path("tags"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return new ThoughtAnalysisGeneration(
                    jsonNode.path("summary").asText(prompt.content()),
                    tags.stream().limit(prompt.maxTags()).toList(),
                    ThoughtModerationStatus.valueOf(jsonNode.path("moderationStatus").asText("REVIEW")),
                    jsonNode.path("moderationReason").asText("模型未给出审核原因"),
                    aiProperties.getProviderName(),
                    aiProperties.getChatModel()
            );
        } catch (Exception exception) {
            throw new IllegalArgumentException("failed to parse thought analysis json", exception);
        }
    }

    private String invokeChat(String systemPrompt, String userPrompt) {
        URI uri = URI.create(buildUrl(aiProperties.getChatBaseUrl(), aiProperties.getChatPath()));
        RestClient.RequestBodySpec request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON);
        if (aiProperties.getChatApiKey() != null && !aiProperties.getChatApiKey().isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, bearerToken(aiProperties.getChatApiKey()));
        }
        OpenAiChatResponse response = request.body(new OpenAiChatRequest(
                        aiProperties.getChatModel(),
                        List.of(
                                new OpenAiMessage("system", systemPrompt),
                                new OpenAiMessage("user", userPrompt)
                        ),
                        0.2D,
                        new ResponseFormat("json_object")
                ))
                .retrieve()
                .body(OpenAiChatResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalArgumentException("chat completion returned empty response");
        }
        return response.choices().getFirst().message().content();
    }

    private String buildUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.ai.chat-base-url is not configured");
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String bearerToken(String token) {
        return token == null || token.isBlank() ? "" : "Bearer " + token;
    }

    private String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start < 0 || end < start) {
            throw new IllegalArgumentException("model response is not a json object: " + content);
        }
        return content.substring(start, end + 1);
    }

    private record OpenAiChatRequest(
            String model,
            List<OpenAiMessage> messages,
            Double temperature,
            ResponseFormat response_format
    ) {
    }

    private record OpenAiMessage(String role, String content) {
    }

    private record ResponseFormat(String type) {
    }

    private record OpenAiChatResponse(List<Choice> choices) {
    }

    private record Choice(OpenAiMessage message) {
    }
}
