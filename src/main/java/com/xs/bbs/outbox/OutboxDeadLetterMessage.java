package com.xs.bbs.outbox;

import java.time.LocalDateTime;

public record OutboxDeadLetterMessage(
        Long eventId,
        String eventType,
        String aggregateType,
        Long aggregateId,
        String payloadJson,
        Integer retryCount,
        String reason,
        LocalDateTime deadLetteredAt
) {
}
