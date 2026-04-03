package com.xs.bbs.match;

public record UserSimilaritySnapshot(
        Long sourceUserId,
        Long targetUserId,
        int score,
        int matchedThoughtCount,
        UserVisibilityLevel visibilityLevel
) {
}
