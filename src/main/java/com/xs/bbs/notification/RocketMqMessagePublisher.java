package com.xs.bbs.notification;

import com.xs.bbs.config.NotificationProperties;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rocketmq")
@Component
public class RocketMqMessagePublisher implements MessagePublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final NotificationProperties notificationProperties;

    public RocketMqMessagePublisher(RocketMQTemplate rocketMQTemplate, NotificationProperties notificationProperties) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.notificationProperties = notificationProperties;
    }

    @Override
    public void publish(NotificationDispatchPayload payload) {
        rocketMQTemplate.convertAndSend(notificationProperties.getTopic(), payload);
    }
}
