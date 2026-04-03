package com.xs.bbs.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.bbs.audit.AuditRecordService;
import com.xs.bbs.ai.ThoughtAiProfileEntity;
import com.xs.bbs.config.AiProperties;
import com.xs.bbs.match.ThoughtTextAnalyzer;
import com.xs.bbs.thought.ThoughtPost;
import com.xs.bbs.thought.ThoughtRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ThoughtAiService {

    private static final Logger log = LoggerFactory.getLogger(ThoughtAiService.class);

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };
    private static final TypeReference<List<Double>> DOUBLE_LIST = new TypeReference<>() {
    };

    private final ThoughtAiProfileMapper thoughtAiProfileMapper;
    private final ThoughtRepository thoughtRepository;
    private final LlmGateway llmGateway;
    private final EmbeddingGateway embeddingGateway;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final AuditRecordService auditRecordService;
    private final ThoughtTextAnalyzer thoughtTextAnalyzer;

    public ThoughtAiService(
            ThoughtAiProfileMapper thoughtAiProfileMapper,
            ThoughtRepository thoughtRepository,
            LlmGateway llmGateway,
            EmbeddingGateway embeddingGateway,
            ObjectMapper objectMapper,
            AiProperties aiProperties,
            AuditRecordService auditRecordService,
            ThoughtTextAnalyzer thoughtTextAnalyzer
    ) {
        this.thoughtAiProfileMapper = thoughtAiProfileMapper;
        this.thoughtRepository = thoughtRepository;
        this.llmGateway = llmGateway;
        this.embeddingGateway = embeddingGateway;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.auditRecordService = auditRecordService;
        this.thoughtTextAnalyzer = thoughtTextAnalyzer;
    }

    public Optional<ThoughtAiProfile> findByThoughtId(Long thoughtId) {
        return Optional.ofNullable(thoughtAiProfileMapper.selectById(thoughtId))
                .map(this::toDomain);
    }

    @Transactional
    public ThoughtAiProfile ensureProfile(ThoughtPost thought) {
        return findByThoughtId(thought.id()).orElseGet(() -> createProfile(thought));
    }

    public ThoughtAiProfileView getView(Long thoughtId) {
        ThoughtPost thought = thoughtRepository.findById(thoughtId)
                .orElseThrow(() -> new IllegalArgumentException("thought does not exist"));
        return toView(ensureProfile(thought));
    }

    public List<ThoughtAiProfileView> getViewsByUserId(Long userId) {
        return thoughtRepository.findByUserId(userId).stream()
                .map(this::ensureProfile)
                .map(this::toView)
                .toList();
    }

    @Transactional
    public ThoughtAiProfileView overrideModeration(
            Long thoughtId,
            Long operatorUserId,
            ThoughtModerationStatus status,
            String reason
    ) {
        ThoughtPost thought = thoughtRepository.findById(thoughtId)
                .orElseThrow(() -> new IllegalArgumentException("thought does not exist"));
        ThoughtAiProfileEntity entity = thoughtAiProfileMapper.selectById(thoughtId);
        if (entity == null) {
            ensureProfile(thought);
            entity = thoughtAiProfileMapper.selectById(thoughtId);
        }
        if (entity == null) {
            throw new IllegalStateException("thought ai profile does not exist");
        }
        ThoughtModerationStatus previousStatus = ThoughtModerationStatus.valueOf(entity.getModerationStatus());
        entity.setModerationStatus(status.name());
        String moderationReason = truncate(blankToDefault(reason, defaultReason(status)), 255);
        entity.setModerationReason(moderationReason);
        entity.setUpdatedAt(LocalDateTime.now());
        thoughtAiProfileMapper.updateById(entity);
        auditRecordService.recordAdminDecision(thoughtId, operatorUserId, previousStatus, status, moderationReason);
        return toView(toDomain(entity));
    }

    public boolean allowsMatching(ThoughtPost thought) {
        return thought.allowRecommendation() && ensureProfile(thought).matchEligible();
    }

    public boolean allowsPublicDisplay(ThoughtPost thought) {
        return thought.publicVisible() && ensureProfile(thought).matchEligible();
    }

    private ThoughtAiProfile createProfile(ThoughtPost thought) {
        ThoughtAnalysisGeneration analysis = analyzeThoughtSafely(thought);
        EmbeddingGeneration embedding = embedSafely(thought);
        String summary = normalizeSummary(analysis.summary(), thought.content());
        List<String> tags = normalizeTags(analysis.tags());
        ThoughtModerationStatus moderationStatus = normalizeModerationStatus(analysis.moderationStatus());
        String moderationReason = truncate(blankToDefault(analysis.moderationReason(), "模型未给出审核原因"), 255);
        List<Double> embeddingVector = normalizeEmbedding(embedding.vector());

        ThoughtAiProfileEntity entity = new ThoughtAiProfileEntity();
        entity.setThoughtId(thought.id());
        entity.setSummary(summary);
        entity.setTagsJson(writeJson(tags));
        entity.setModerationStatus(moderationStatus.name());
        entity.setModerationReason(moderationReason);
        entity.setLlmProvider(blankToDefault(analysis.provider(), aiProperties.getProviderName()));
        entity.setLlmModel(blankToDefault(analysis.model(), aiProperties.getChatModel()));
        entity.setEmbeddingProvider(blankToDefault(embedding.provider(), aiProperties.getProviderName()));
        entity.setEmbeddingModel(blankToDefault(embedding.model(), aiProperties.getEmbeddingModel()));
        entity.setEmbeddingJson(writeJson(embeddingVector));
        entity.setPromptVersion(aiProperties.getPromptVersion());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        try {
            thoughtAiProfileMapper.insert(entity);
            auditRecordService.recordAiDecision(thought.id(), moderationStatus, moderationReason);
            return toDomain(entity);
        } catch (DuplicateKeyException ignore) {
            return findByThoughtId(thought.id())
                    .orElseThrow(() -> new IllegalStateException("thought ai profile was inserted concurrently but cannot be read"));
        }
    }

    private ThoughtAnalysisGeneration analyzeThoughtSafely(ThoughtPost thought) {
        try {
            return llmGateway.analyzeThought(new ThoughtAnalysisPrompt(
                    thought.id(),
                    thought.content(),
                    aiProperties.getMaxTags(),
                    aiProperties.getPromptVersion()
            ));
        } catch (RuntimeException exception) {
            log.warn("AI thought analysis degraded for thoughtId={}: {}", thought.id(), exception.getMessage());
            return new ThoughtAnalysisGeneration(
                    buildFallbackSummary(thought.content()),
                    buildFallbackTags(thought.content()),
                    ThoughtModerationStatus.REVIEW,
                    "AI 服务暂不可用，系统已自动转入人工复核。",
                    "fallback",
                    "rule-based-review"
            );
        }
    }

    private EmbeddingGeneration embedSafely(ThoughtPost thought) {
        try {
            return embeddingGateway.embed(thought.content());
        } catch (RuntimeException exception) {
            log.warn("AI embedding degraded for thoughtId={}: {}", thought.id(), exception.getMessage());
            return new EmbeddingGeneration(
                    List.of(),
                    "fallback",
                    "embedding-unavailable"
            );
        }
    }

    private ThoughtAiProfile toDomain(ThoughtAiProfileEntity entity) {
        return new ThoughtAiProfile(
                entity.getThoughtId(),
                entity.getSummary(),
                readStringList(entity.getTagsJson()),
                ThoughtModerationStatus.valueOf(entity.getModerationStatus()),
                entity.getModerationReason(),
                entity.getLlmProvider(),
                entity.getLlmModel(),
                entity.getEmbeddingProvider(),
                entity.getEmbeddingModel(),
                readDoubleList(entity.getEmbeddingJson()),
                entity.getPromptVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ThoughtAiProfileView toView(ThoughtAiProfile profile) {
        return new ThoughtAiProfileView(
                profile.thoughtId(),
                profile.summary(),
                profile.tags(),
                profile.moderationStatus(),
                profile.moderationReason(),
                profile.llmProvider(),
                profile.llmModel(),
                profile.embeddingProvider(),
                profile.embeddingModel(),
                !profile.embedding().isEmpty(),
                profile.promptVersion(),
                profile.updatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to serialize thought ai profile", exception);
        }
    }

    private List<String> readStringList(String value) {
        try {
            return objectMapper.readValue(value, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to deserialize tags json", exception);
        }
    }

    private List<Double> readDoubleList(String value) {
        try {
            return objectMapper.readValue(value, DOUBLE_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to deserialize embedding json", exception);
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength);
    }

    private String normalizeSummary(String summary, String fallbackContent) {
        String resolved = blankToDefault(summary, fallbackContent);
        return truncate(resolved, 255);
    }

    private String buildFallbackSummary(String content) {
        if (content == null || content.isBlank()) {
            return "系统已接收该念头，等待 AI 恢复后继续分析。";
        }
        String normalized = content.trim();
        String prefix = normalized.length() > 42 ? normalized.substring(0, 42) : normalized;
        return truncate("待 AI 复核：" + prefix, 255);
    }

    private List<String> buildFallbackTags(String content) {
        List<String> inferredTags = thoughtTextAnalyzer.tokens(content).stream()
                .filter(token -> token.length() >= 2)
                .limit(Math.max(1, aiProperties.getMaxTags() - 1L))
                .toList();
        if (inferredTags.isEmpty()) {
            return List.of("待AI分析");
        }
        return List.copyOf(inferredTags);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of("自我整理");
        }
        List<String> normalized = tags.stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .distinct()
                .limit(aiProperties.getMaxTags())
                .collect(Collectors.toList());
        return normalized.isEmpty() ? List.of("自我整理") : List.copyOf(normalized);
    }

    private ThoughtModerationStatus normalizeModerationStatus(ThoughtModerationStatus moderationStatus) {
        return moderationStatus == null ? ThoughtModerationStatus.REVIEW : moderationStatus;
    }

    private List<Double> normalizeEmbedding(List<Double> vector) {
        return vector == null ? List.of() : List.copyOf(vector);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String defaultReason(ThoughtModerationStatus status) {
        return switch (status) {
            case APPROVED -> "运营复核后通过，可进入公开流与匹配。";
            case REVIEW -> "需要继续人工复核，暂缓公开分发。";
            case REJECTED -> "运营复核后限制展示，不进入公开流与匹配。";
        };
    }
}
