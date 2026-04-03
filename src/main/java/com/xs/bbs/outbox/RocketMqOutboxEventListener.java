package com.xs.bbs.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rocketmq")
@Component
@RocketMQMessageListener(
        topic = "${app.outbox.topic}",
        consumerGroup = "${app.outbox.consumer-group}"
)
public class RocketMqOutboxEventListener implements RocketMQListener<OutboxEventMessage> {

    private static final Logger log = LoggerFactory.getLogger(RocketMqOutboxEventListener.class);

    private final OutboxEventDispatcher outboxEventDispatcher;
    private final OutboxEventService outboxEventService;
    private final OutboxDeadLetterPublisher outboxDeadLetterPublisher;

    public RocketMqOutboxEventListener(
            OutboxEventDispatcher outboxEventDispatcher,
            OutboxEventService outboxEventService,
            OutboxDeadLetterPublisher outboxDeadLetterPublisher
    ) {
        this.outboxEventDispatcher = outboxEventDispatcher;
        this.outboxEventService = outboxEventService;
        this.outboxDeadLetterPublisher = outboxDeadLetterPublisher;
    }

    @Override
    public void onMessage(OutboxEventMessage message) {
        if (!outboxEventService.markConsumingPublished(message.eventId())) {
            log.info("skip duplicated or stale outbox message: eventId={}", message.eventId());
            return;
        }
        try {
            OutboxEventType eventType = OutboxEventType.valueOf(message.eventType());
            String result = outboxEventDispatcher.dispatch(eventType, message.payloadJson());
            outboxEventService.markProcessed(message.eventId(), result);
            log.info("outbox event consumed: eventId={}, eventType={}, result={}", message.eventId(), eventType, result);
        } catch (Exception exception) {
            OutboxEventStatus status = outboxEventService.recordFailure(message.eventId(), exception);
            if (status == OutboxEventStatus.DEAD) {
                OutboxEventEntity event = outboxEventService.findById(message.eventId());
                if (event != null) {
                    outboxDeadLetterPublisher.publish(new OutboxDeadLetterMessage(
                            event.getId(),
                            event.getEventType(),
                            event.getAggregateType(),
                            event.getAggregateId(),
                            event.getPayloadJson(),
                            event.getRetryCount(),
                            event.getDeadLetterReason(),
                            event.getDeadLetteredAt()
                    ));
                }
            }
            log.warn("outbox event consume failed and scheduled for retry: eventId={}", message.eventId(), exception);
        }
    }
}
