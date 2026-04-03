package com.xs.bbs.outbox;

import com.xs.bbs.thought.ThoughtDegree;

import java.time.LocalDateTime;

public record ThoughtPublishedPayload(
        Long thoughtId,
        Long userId,
        String content,
        ThoughtDegree degree,
        LocalDateTime createdAt
) {
}
