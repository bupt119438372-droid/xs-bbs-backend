package com.xs.bbs.ai;

import com.xs.bbs.config.AiProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression(
        "'${app.ai.mode:compatible}' == 'compatible' || '${app.ai.mode:compatible}' == 'openai-compatible'"
)
public class AiConfigurationValidator {

    private final AiProperties aiProperties;

    public AiConfigurationValidator(AiProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    @PostConstruct
    public void validate() {
        requireValue(aiProperties.getChatBaseUrl(), "app.ai.chat-base-url");
        requireValue(aiProperties.getEmbeddingBaseUrl(), "app.ai.embedding-base-url");
        requireValue(aiProperties.getChatApiKey(), "app.ai.chat-api-key or XS_BBS_AI_API_KEY");
        requireValue(aiProperties.getEmbeddingApiKey(), "app.ai.embedding-api-key or XS_BBS_AI_API_KEY");
    }

    private void requireValue(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(propertyName + " must be configured when app.ai.mode=compatible");
        }
    }
}
