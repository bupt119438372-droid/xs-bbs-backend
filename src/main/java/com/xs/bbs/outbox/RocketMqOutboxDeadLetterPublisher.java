package com.xs.bbs.outbox;

import com.xs.bbs.config.OutboxProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rocketmq")
@Component
public class RocketMqOutboxDeadLetterPublisher implements OutboxDeadLetterPublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final OutboxProperties outboxProperties;

    public RocketMqOutboxDeadLetterPublisher(RocketMQTemplate rocketMQTemplate, OutboxProperties outboxProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.outboxProperties = outboxProperties;
    }

    @Override
    public void publish(OutboxDeadLetterMessage message) {
        rocketMQTemplate.convertAndSend(outboxProperties.getDeadLetterTopic(), message);
    }
}
