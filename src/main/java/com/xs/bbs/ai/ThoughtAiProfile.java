package com.xs.bbs.ai;

import java.time.LocalDateTime;
import java.util.List;

public record ThoughtAiProfile(
        Long thoughtId,
        String summary,
        List<String> tags,
        ThoughtModerationStatus moderationStatus,
        String moderationReason,
        String llmProvider,
        String llmModel,
        String embeddingProvider,
        String embeddingModel,
        List<Double> embedding,
        String promptVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public boolean matchEligible() {
        return moderationStatus == ThoughtModerationStatus.APPROVED;
    }
}
