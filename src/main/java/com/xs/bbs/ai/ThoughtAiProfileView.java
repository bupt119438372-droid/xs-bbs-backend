package com.xs.bbs.ai;

import java.time.LocalDateTime;
import java.util.List;

public record ThoughtAiProfileView(
        Long thoughtId,
        String summary,
        List<String> tags,
        ThoughtModerationStatus moderationStatus,
        String moderationReason,
        String llmProvider,
        String llmModel,
        String embeddingProvider,
        String embeddingModel,
        boolean embeddingReady,
        String promptVersion,
        LocalDateTime updatedAt
) {
}
