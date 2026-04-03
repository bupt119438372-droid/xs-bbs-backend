package com.xs.bbs.ai;

import java.util.List;

public record DailyInsightPrompt(
        Long userId,
        String latestThought,
        String latestSummary,
        List<String> latestTags,
        List<String> themes,
        String resonanceSummary
) {
}
