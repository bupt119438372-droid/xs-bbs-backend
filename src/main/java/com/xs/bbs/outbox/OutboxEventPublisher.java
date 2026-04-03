package com.xs.bbs.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xs.bbs.thought.ThoughtPost;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OutboxEventPublisher {

    private final OutboxEventMapper outboxEventMapper;
    private final ObjectMapper objectMapper;

    public OutboxEventPublisher(OutboxEventMapper outboxEventMapper, ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.objectMapper = objectMapper;
    }

    public void publishThoughtPublished(ThoughtPost thought) {
        ThoughtPublishedPayload payload = new ThoughtPublishedPayload(
                thought.id(),
                thought.userId(),
                thought.content(),
                thought.degree(),
                thought.createdAt()
        );
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setEventType(OutboxEventType.THOUGHT_PUBLISHED.name());
        entity.setAggregateType("thought");
        entity.setAggregateId(thought.id());
        entity.setPayloadJson(writeJson(payload));
        entity.setStatus(OutboxEventStatus.PENDING.name());
        entity.setRetryCount(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setNextRetryAt(LocalDateTime.now());
        outboxEventMapper.insert(entity);
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to serialize outbox payload", exception);
        }
    }
}
