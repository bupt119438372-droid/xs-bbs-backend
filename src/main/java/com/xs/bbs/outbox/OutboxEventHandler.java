package com.xs.bbs.outbox;

public interface OutboxEventHandler<T> {

    OutboxEventType eventType();

    Class<T> payloadType();

    String handle(T payload);
}
