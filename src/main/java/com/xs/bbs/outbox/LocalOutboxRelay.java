package com.xs.bbs.outbox;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!rocketmq")
@Component
public class LocalOutboxRelay implements OutboxRelay {

    private final OutboxEventDispatcher outboxEventDispatcher;

    public LocalOutboxRelay(OutboxEventDispatcher outboxEventDispatcher) {
        this.outboxEventDispatcher = outboxEventDispatcher;
    }

    @Override
    public OutboxRelayResult relay(OutboxEventEntity event) throws Exception {
        OutboxEventType eventType = OutboxEventType.valueOf(event.getEventType());
        String result = outboxEventDispatcher.dispatch(eventType, event.getPayloadJson());
        return OutboxRelayResult.processed(result);
    }
}
