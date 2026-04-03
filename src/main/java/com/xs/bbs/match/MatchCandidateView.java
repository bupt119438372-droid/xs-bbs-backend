package com.xs.bbs.match;

public record MatchCandidateView(
        Long targetUserId,
        String displayName,
        String previewThought,
        double finalScore,
        int userSimilarity,
        UserVisibilityLevel visibilityLevel
) {
}
