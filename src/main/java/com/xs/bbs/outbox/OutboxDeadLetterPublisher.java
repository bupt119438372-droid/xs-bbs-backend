package com.xs.bbs.outbox;

public interface OutboxDeadLetterPublisher {

    void publish(OutboxDeadLetterMessage message);
}
