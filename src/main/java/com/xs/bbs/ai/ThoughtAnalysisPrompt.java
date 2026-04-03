package com.xs.bbs.ai;

public record ThoughtAnalysisPrompt(
        Long thoughtId,
        String content,
        int maxTags,
        String promptVersion
) {
}
