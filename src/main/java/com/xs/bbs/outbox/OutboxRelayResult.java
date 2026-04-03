package com.xs.bbs.outbox;

public record OutboxRelayResult(
        OutboxEventStatus nextStatus,
        String resultSummary
) {

    public static OutboxRelayResult published(String resultSummary) {
        return new OutboxRelayResult(OutboxEventStatus.PUBLISHED, resultSummary);
    }

    public static OutboxRelayResult processed(String resultSummary) {
        return new OutboxRelayResult(OutboxEventStatus.PROCESSED, resultSummary);
    }
}
