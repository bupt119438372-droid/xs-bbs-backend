package com.xs.bbs.outbox;

public interface OutboxRelay {

    OutboxRelayResult relay(OutboxEventEntity event) throws Exception;
}
