package com.xs.bbs.ai;

import com.xs.bbs.config.AiProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@ConditionalOnExpression(
        "'${app.ai.mode:compatible}' == 'compatible' || '${app.ai.mode:compatible}' == 'openai-compatible'"
)
@Component
public class CompatibleEmbeddingGateway implements EmbeddingGateway {

    private final AiProperties aiProperties;
    private final RestClient restClient;

    public CompatibleEmbeddingGateway(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
        this.restClient = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofMillis(aiProperties.getRequestTimeoutMs()))
                                .build()
                ))
                .build();
    }

    @Override
    public EmbeddingGeneration embed(String input) {
        URI uri = URI.create(buildUrl(aiProperties.getEmbeddingBaseUrl(), aiProperties.getEmbeddingPath()));
        RestClient.RequestBodySpec request = restClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON);
        if (aiProperties.getEmbeddingApiKey() != null && !aiProperties.getEmbeddingApiKey().isBlank()) {
            request.header(HttpHeaders.AUTHORIZATION, bearerToken(aiProperties.getEmbeddingApiKey()));
        }
        OpenAiEmbeddingResponse response = request.body(new OpenAiEmbeddingRequest(aiProperties.getEmbeddingModel(), input))
                .retrieve()
                .body(OpenAiEmbeddingResponse.class);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalArgumentException("embedding api returned empty response");
        }
        return new EmbeddingGeneration(
                List.copyOf(response.data().getFirst().embedding()),
                aiProperties.getProviderName(),
                aiProperties.getEmbeddingModel()
        );
    }

    private String buildUrl(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.ai.embedding-base-url is not configured");
        }
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }

    private String bearerToken(String token) {
        return token == null || token.isBlank() ? "" : "Bearer " + token;
    }

    private record OpenAiEmbeddingRequest(String model, String input) {
    }

    private record OpenAiEmbeddingResponse(List<EmbeddingData> data) {
    }

    private record EmbeddingData(List<Double> embedding) {
    }
}
