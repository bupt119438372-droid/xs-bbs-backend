package com.xs.bbs.outbox;

public record OutboxEventMessage(
        Long eventId,
        String eventType,
        String payloadJson
) {
}
