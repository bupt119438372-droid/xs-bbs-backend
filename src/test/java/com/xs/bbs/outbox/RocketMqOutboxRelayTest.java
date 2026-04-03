package com.xs.bbs.outbox;

import com.xs.bbs.config.OutboxProperties;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RocketMqOutboxRelayTest {

    @Test
    void shouldWrapOutboxEventAndSendToConfiguredTopic() {
        RecordingRocketMQTemplate rocketMQTemplate = new RecordingRocketMQTemplate();
        OutboxProperties outboxProperties = new OutboxProperties();
        outboxProperties.setTopic("xs-bbs.outbox.test");

        RocketMqOutboxRelay relay = new RocketMqOutboxRelay(rocketMQTemplate, outboxProperties);

        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(99L);
        event.setEventType(OutboxEventType.THOUGHT_PUBLISHED.name());
        event.setAggregateType("thought");
        event.setAggregateId(1001L);
        event.setPayloadJson("{\"thoughtId\":1001}");
        event.setCreatedAt(LocalDateTime.now());

        OutboxRelayResult result = relay.relay(event);

        assertThat(rocketMQTemplate.destination).isEqualTo("xs-bbs.outbox.test");
        assertThat(rocketMQTemplate.shardKey).isEqualTo("thought:1001");
        assertThat(rocketMQTemplate.payload)
                .isEqualTo(new OutboxEventMessage(99L, OutboxEventType.THOUGHT_PUBLISHED.name(), "{\"thoughtId\":1001}"));
        assertThat(result.nextStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(result.resultSummary()).contains("shardKey=thought:1001");
    }

    private static final class RecordingRocketMQTemplate extends RocketMQTemplate {

        private String destination;
        private String shardKey;
        private Object payload;

        @Override
        public SendResult syncSendOrderly(String destination, Object payload, String hashKey) {
            this.destination = destination;
            this.shardKey = hashKey;
            this.payload = payload;
            return null;
        }
    }
}
