package com.xs.bbs.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventProcessor {

    private final OutboxEventService outboxEventService;
    private final OutboxRelay outboxRelay;
    private final OutboxDeadLetterPublisher outboxDeadLetterPublisher;

    public OutboxEventProcessor(
            OutboxEventService outboxEventService,
            OutboxRelay outboxRelay,
            OutboxDeadLetterPublisher outboxDeadLetterPublisher
    ) {
        this.outboxEventService = outboxEventService;
        this.outboxRelay = outboxRelay;
        this.outboxDeadLetterPublisher = outboxDeadLetterPublisher;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:2000}", initialDelayString = "${app.outbox.fixed-delay-ms:2000}")
    public void processPendingEvents() {
        for (OutboxEventEntity event : outboxEventService.findPendingBatch()) {
            if (!outboxEventService.markProcessing(event.getId())) {
                continue;
            }
            try {
                OutboxRelayResult result = outboxRelay.relay(event);
                if (result.nextStatus() == OutboxEventStatus.PUBLISHED) {
                    outboxEventService.markPublished(event.getId(), result.resultSummary());
                } else {
                    outboxEventService.markProcessed(event.getId(), result.resultSummary());
                }
            } catch (Exception exception) {
                if (outboxEventService.recordFailure(event.getId(), exception) == OutboxEventStatus.DEAD) {
                    outboxDeadLetterPublisher.publish(new OutboxDeadLetterMessage(
                            event.getId(),
                            event.getEventType(),
                            event.getAggregateType(),
                            event.getAggregateId(),
                            event.getPayloadJson(),
                            event.getRetryCount() + 1,
                            exception.getMessage(),
                            java.time.LocalDateTime.now()
                    ));
                }
            }
        }
    }
}
