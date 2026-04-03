package com.xs.bbs.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    PROCESSED,
    FAILED,
    DEAD
}
