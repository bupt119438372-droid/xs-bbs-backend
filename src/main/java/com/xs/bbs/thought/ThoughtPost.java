package com.xs.bbs.thought;

import java.time.LocalDateTime;

public record ThoughtPost(
        Long id,
        Long userId,
        String content,
        ThoughtDegree degree,
        boolean allowRecommendation,
        boolean publicVisible,
        LocalDateTime createdAt
) {
}
