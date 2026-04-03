package com.xs.bbs.outbox;

import com.xs.bbs.config.OutboxProperties;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rocketmq")
@Component
public class RocketMqOutboxRelay implements OutboxRelay {

    private final RocketMQTemplate rocketMQTemplate;
    private final OutboxProperties outboxProperties;

    public RocketMqOutboxRelay(RocketMQTemplate rocketMQTemplate, OutboxProperties outboxProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.outboxProperties = outboxProperties;
    }

    @Override
    public OutboxRelayResult relay(OutboxEventEntity event) {
        OutboxEventMessage message = new OutboxEventMessage(
                event.getId(),
                event.getEventType(),
                event.getPayloadJson()
        );
        String shardKey = buildShardKey(event);
        SendResult ignored = rocketMQTemplate.syncSendOrderly(outboxProperties.getTopic(), message, shardKey);
        return OutboxRelayResult.published(
                "published-to-rocketmq(topic=%s,eventId=%s,shardKey=%s)"
                        .formatted(outboxProperties.getTopic(), event.getId(), shardKey)
        );
    }

    private String buildShardKey(OutboxEventEntity event) {
        return event.getAggregateType() + ":" + event.getAggregateId();
    }
}
