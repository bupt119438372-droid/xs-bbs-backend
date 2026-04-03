package com.xs.bbs.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("!rocketmq")
@Component
public class LoggingOutboxDeadLetterPublisher implements OutboxDeadLetterPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingOutboxDeadLetterPublisher.class);

    @Override
    public void publish(OutboxDeadLetterMessage message) {
        log.error(
                "outbox event moved to dead letter: eventId={}, eventType={}, aggregateType={}, aggregateId={}, retryCount={}, reason={}",
                message.eventId(),
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                message.retryCount(),
                message.reason()
        );
    }
}
