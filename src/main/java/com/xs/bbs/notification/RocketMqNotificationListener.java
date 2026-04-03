package com.xs.bbs.notification;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("rocketmq")
@Component
@RocketMQMessageListener(
        topic = "${app.notification.topic}",
        consumerGroup = "${app.notification.consumer-group}"
)
public class RocketMqNotificationListener implements RocketMQListener<NotificationDispatchPayload> {

    private final NotificationDeliveryHandler notificationDeliveryHandler;

    public RocketMqNotificationListener(NotificationDeliveryHandler notificationDeliveryHandler) {
        this.notificationDeliveryHandler = notificationDeliveryHandler;
    }

    @Override
    public void onMessage(NotificationDispatchPayload payload) {
        notificationDeliveryHandler.deliver(payload);
    }
}
