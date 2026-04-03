package com.xs.bbs.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OutboxRetryIntegrationTest {

    @Autowired
    private OutboxEventMapper outboxEventMapper;

    @Autowired
    private OutboxEventService outboxEventService;

    @Test
    void shouldMoveEventToFailedWithBackoffAfterFirstFailure() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventType(OutboxEventType.THOUGHT_PUBLISHED.name());
        event.setAggregateType("thought");
        event.setAggregateId(1001L);
        event.setPayloadJson("{\"thoughtId\":1001}");
        event.setStatus(OutboxEventStatus.PROCESSING.name());
        event.setRetryCount(0);
        event.setCreatedAt(LocalDateTime.now());
        event.setNextRetryAt(LocalDateTime.now());
        outboxEventMapper.insert(event);

        OutboxEventStatus status = outboxEventService.recordFailure(event.getId(), new RuntimeException("boom"));
        OutboxEventEntity updated = outboxEventMapper.selectById(event.getId());

        assertThat(status).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.FAILED.name());
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getNextRetryAt()).isAfter(LocalDateTime.now().minusSeconds(1));
        assertThat(updated.getDeadLetterReason()).isNull();
    }

    @Test
    void shouldMoveEventToDeadAfterMaxRetryCountExceeded() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setEventType(OutboxEventType.THOUGHT_PUBLISHED.name());
        event.setAggregateType("thought");
        event.setAggregateId(1002L);
        event.setPayloadJson("{\"thoughtId\":1002}");
        event.setStatus(OutboxEventStatus.PROCESSING.name());
        event.setRetryCount(4);
        event.setCreatedAt(LocalDateTime.now());
        event.setNextRetryAt(LocalDateTime.now());
        outboxEventMapper.insert(event);

        OutboxEventStatus status = outboxEventService.recordFailure(event.getId(), new RuntimeException("still failing"));
        OutboxEventEntity updated = outboxEventMapper.selectById(event.getId());

        assertThat(status).isEqualTo(OutboxEventStatus.DEAD);
        assertThat(updated.getStatus()).isEqualTo(OutboxEventStatus.DEAD.name());
        assertThat(updated.getRetryCount()).isEqualTo(5);
        assertThat(updated.getDeadLetterReason()).isEqualTo("still failing");
        assertThat(updated.getDeadLetteredAt()).isNotNull();
        assertThat(updated.getNextRetryAt()).isNull();
    }
}
