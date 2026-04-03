package com.xs.bbs.ai;

import java.util.List;

public record ThoughtAnalysisGeneration(
        String summary,
        List<String> tags,
        ThoughtModerationStatus moderationStatus,
        String moderationReason,
        String provider,
        String model
) {
}
